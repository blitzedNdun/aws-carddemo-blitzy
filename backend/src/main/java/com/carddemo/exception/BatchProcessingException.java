package com.carddemo.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Custom exception for batch processing specific errors in the CardDemo application.
 * Handles various batch job failure scenarios including launch failures, timeouts,
 * dependency violations, and parameter errors. Provides retry guidance and detailed
 * error context for automated error recovery and audit trail compliance.
 * 
 * <p>This exception replaces COBOL JCL ABEND handling and provides Spring Batch
 * compatible error information for Kubernetes CronJob execution monitoring.</p>
 * 
 * <p>Error types supported:</p>
 * <ul>
 *   <li>LAUNCH_FAILURE - Job cannot be started due to configuration or system issues</li>
 *   <li>TIMEOUT - Job execution exceeded the 4-hour processing window</li>
 *   <li>DEPENDENCY_VIOLATION - Job dependencies not satisfied or circular dependencies</li>
 *   <li>PARAMETER_ERROR - Invalid or missing job parameters</li>
 *   <li>EXECUTION_ERROR - Runtime errors during job step processing</li>
 *   <li>RESOURCE_UNAVAILABLE - Required resources (DB, files) not available</li>
 * </ul>
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 */
public class BatchProcessingException extends RuntimeException {

    /**
     * Enumeration defining specific batch processing error types
     * that can occur during Spring Batch job execution.
     */
    public enum ErrorType {
        LAUNCH_FAILURE("BTCH001", "Failed to launch batch job", true),
        TIMEOUT("BTCH002", "Batch job execution timeout", false), 
        DEPENDENCY_VIOLATION("BTCH003", "Job dependency validation failed", true),
        PARAMETER_ERROR("BTCH004", "Invalid job parameters provided", true),
        EXECUTION_ERROR("BTCH005", "Runtime error during job execution", false),
        RESOURCE_UNAVAILABLE("BTCH006", "Required resources unavailable", true);

        private final String errorCode;
        private final String description;
        private final boolean retryable;

        ErrorType(String errorCode, String description, boolean retryable) {
            this.errorCode = errorCode;
            this.description = description;
            this.retryable = retryable;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }

    private final String jobName;
    private final ErrorType errorType;
    private final int retryCount;
    private final boolean isRetryable;
    private final Map<String, Object> jobParameters;
    private final Map<String, Object> executionContext;
    private final LocalDateTime errorTimestamp;
    private final Long jobExecutionId;
    private final String stepName;
    private final long processingWindowMinutes;

    /**
     * Constructs a BatchProcessingException with basic error information.
     * 
     * @param jobName the name of the failed batch job
     * @param errorType the specific type of batch processing error
     * @param message detailed error message
     */
    public BatchProcessingException(String jobName, ErrorType errorType, String message) {
        this(jobName, errorType, message, null, 0, new HashMap<>(), new HashMap<>());
    }

    /**
     * Constructs a BatchProcessingException with error cause information.
     * 
     * @param jobName the name of the failed batch job
     * @param errorType the specific type of batch processing error
     * @param message detailed error message
     * @param cause the underlying cause of the batch processing failure
     */
    public BatchProcessingException(String jobName, ErrorType errorType, String message, Throwable cause) {
        this(jobName, errorType, message, cause, 0, new HashMap<>(), new HashMap<>());
    }

    /**
     * Constructs a BatchProcessingException with comprehensive error context.
     * 
     * @param jobName the name of the failed batch job
     * @param errorType the specific type of batch processing error
     * @param message detailed error message
     * @param cause the underlying cause of the batch processing failure
     * @param retryCount the number of retry attempts made
     * @param jobParameters the job parameters used during execution
     * @param executionContext the job execution context at time of failure
     */
    public BatchProcessingException(String jobName, ErrorType errorType, String message, 
                                  Throwable cause, int retryCount, 
                                  Map<String, Object> jobParameters, 
                                  Map<String, Object> executionContext) {
        super(buildErrorMessage(jobName, errorType, message, retryCount), cause);
        this.jobName = jobName;
        this.errorType = errorType;
        this.retryCount = retryCount;
        this.isRetryable = errorType.isRetryable() && retryCount < getMaxRetryCount(errorType);
        this.jobParameters = new HashMap<>(jobParameters != null ? jobParameters : new HashMap<>());
        this.executionContext = new HashMap<>(executionContext != null ? executionContext : new HashMap<>());
        this.errorTimestamp = LocalDateTime.now();
        this.jobExecutionId = extractJobExecutionId(executionContext);
        this.stepName = extractStepName(executionContext);
        this.processingWindowMinutes = 240; // 4-hour window requirement
    }

    /**
     * Creates a BatchProcessingException for job launch failures.
     * 
     * @param jobName the name of the job that failed to launch
     * @param cause the underlying cause of the launch failure
     * @param jobParameters the parameters used for job launch
     * @return BatchProcessingException configured for launch failure
     */
    public static BatchProcessingException jobLaunchFailure(String jobName, Throwable cause, 
                                                           Map<String, Object> jobParameters) {
        String message = String.format("Failed to launch batch job '%s'. Check job configuration and system resources.", jobName);
        return new BatchProcessingException(jobName, ErrorType.LAUNCH_FAILURE, message, cause, 0, jobParameters, new HashMap<>());
    }

    /**
     * Creates a BatchProcessingException for job execution timeout.
     * 
     * @param jobName the name of the job that timed out
     * @param executionDurationMinutes the actual execution duration before timeout
     * @param executionContext the job execution context at timeout
     * @return BatchProcessingException configured for timeout
     */
    public static BatchProcessingException jobTimeout(String jobName, long executionDurationMinutes, 
                                                     Map<String, Object> executionContext) {
        String message = String.format("Batch job '%s' exceeded 4-hour processing window. Execution duration: %d minutes.", 
                                      jobName, executionDurationMinutes);
        return new BatchProcessingException(jobName, ErrorType.TIMEOUT, message, null, 0, new HashMap<>(), executionContext);
    }

    /**
     * Creates a BatchProcessingException for job dependency violations.
     * 
     * @param jobName the name of the job with dependency issues
     * @param dependencyDetails description of the dependency violation
     * @param retryCount the current retry attempt count
     * @return BatchProcessingException configured for dependency violation
     */
    public static BatchProcessingException dependencyViolation(String jobName, String dependencyDetails, int retryCount) {
        String message = String.format("Job dependency violation for '%s': %s", jobName, dependencyDetails);
        return new BatchProcessingException(jobName, ErrorType.DEPENDENCY_VIOLATION, message, null, retryCount, new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a BatchProcessingException for invalid job parameters.
     * 
     * @param jobName the name of the job with parameter issues
     * @param parameterErrors description of parameter validation failures
     * @param jobParameters the invalid parameters that caused the error
     * @return BatchProcessingException configured for parameter error
     */
    public static BatchProcessingException parameterError(String jobName, String parameterErrors, 
                                                         Map<String, Object> jobParameters) {
        String message = String.format("Invalid parameters for batch job '%s': %s", jobName, parameterErrors);
        return new BatchProcessingException(jobName, ErrorType.PARAMETER_ERROR, message, null, 0, jobParameters, new HashMap<>());
    }

    /**
     * Creates a BatchProcessingException for runtime execution errors.
     * 
     * @param jobName the name of the job that encountered runtime error
     * @param stepName the step where the error occurred
     * @param cause the underlying runtime exception
     * @param executionContext the job execution context at error
     * @return BatchProcessingException configured for execution error
     */
    public static BatchProcessingException executionError(String jobName, String stepName, Throwable cause, 
                                                         Map<String, Object> executionContext) {
        String message = String.format("Runtime error in batch job '%s' at step '%s': %s", 
                                      jobName, stepName, cause.getMessage());
        Map<String, Object> context = new HashMap<>(executionContext);
        context.put("failedStepName", stepName);
        return new BatchProcessingException(jobName, ErrorType.EXECUTION_ERROR, message, cause, 0, new HashMap<>(), context);
    }

    /**
     * Creates a BatchProcessingException for resource unavailability.
     * 
     * @param jobName the name of the job missing resources
     * @param resourceDetails description of unavailable resources
     * @param retryCount the current retry attempt count
     * @return BatchProcessingException configured for resource unavailability
     */
    public static BatchProcessingException resourceUnavailable(String jobName, String resourceDetails, int retryCount) {
        String message = String.format("Required resources unavailable for batch job '%s': %s", jobName, resourceDetails);
        return new BatchProcessingException(jobName, ErrorType.RESOURCE_UNAVAILABLE, message, null, retryCount, new HashMap<>(), new HashMap<>());
    }

    /**
     * Builds a comprehensive error message including job context and retry information.
     * 
     * @param jobName the name of the failed job
     * @param errorType the type of error encountered
     * @param message the base error message
     * @param retryCount the number of retry attempts
     * @return formatted error message
     */
    private static String buildErrorMessage(String jobName, ErrorType errorType, String message, int retryCount) {
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append(String.format("[%s] %s - %s", errorType.getErrorCode(), errorType.getDescription(), message));
        
        if (retryCount > 0) {
            errorMsg.append(String.format(" (Retry attempt: %d)", retryCount));
        }
        
        if (errorType.isRetryable()) {
            errorMsg.append(" - Job is eligible for retry");
        } else {
            errorMsg.append(" - Job requires manual intervention");
        }
        
        return errorMsg.toString();
    }

    /**
     * Determines the maximum retry count based on error type.
     * 
     * @param errorType the type of batch processing error
     * @return maximum number of retry attempts allowed
     */
    private static int getMaxRetryCount(ErrorType errorType) {
        return switch (errorType) {
            case LAUNCH_FAILURE -> 3;
            case DEPENDENCY_VIOLATION -> 5;
            case PARAMETER_ERROR -> 1;
            case RESOURCE_UNAVAILABLE -> 3;
            case TIMEOUT -> 0; // No retries for timeout
            case EXECUTION_ERROR -> 0; // No automatic retries for execution errors
        };
    }

    /**
     * Extracts job execution ID from execution context.
     * 
     * @param executionContext the job execution context
     * @return job execution ID or null if not available
     */
    private static Long extractJobExecutionId(Map<String, Object> executionContext) {
        if (executionContext != null && executionContext.containsKey("jobExecutionId")) {
            Object id = executionContext.get("jobExecutionId");
            if (id instanceof Long) {
                return (Long) id;
            } else if (id instanceof String) {
                try {
                    return Long.parseLong((String) id);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Extracts step name from execution context.
     * 
     * @param executionContext the job execution context
     * @return step name or null if not available
     */
    private static String extractStepName(Map<String, Object> executionContext) {
        if (executionContext != null && executionContext.containsKey("stepName")) {
            Object stepName = executionContext.get("stepName");
            return stepName != null ? stepName.toString() : null;
        }
        return null;
    }

    /**
     * Generates retry guidance based on error type and current retry count.
     * 
     * @return detailed retry guidance message
     */
    public String getRetryGuidance() {
        if (!isRetryable) {
            return switch (errorType) {
                case TIMEOUT -> "Job exceeded 4-hour processing window. Consider optimizing job performance or increasing processing window.";
                case EXECUTION_ERROR -> "Runtime error requires code review and fix before retry.";
                default -> "Maximum retry attempts exceeded. Manual intervention required.";
            };
        }

        return switch (errorType) {
            case LAUNCH_FAILURE -> String.format("Retry %d of %d. Check system resources and job configuration.", 
                                                retryCount + 1, getMaxRetryCount(errorType));
            case DEPENDENCY_VIOLATION -> String.format("Retry %d of %d. Verify prerequisite jobs completed successfully.", 
                                                      retryCount + 1, getMaxRetryCount(errorType));
            case PARAMETER_ERROR -> "Fix job parameters and retry once.";
            case RESOURCE_UNAVAILABLE -> String.format("Retry %d of %d. Verify database connectivity and file system access.", 
                                                      retryCount + 1, getMaxRetryCount(errorType));
            default -> "Job is eligible for automatic retry.";
        };
    }

    /**
     * Checks if the execution window (4 hours) has been exceeded.
     * 
     * @param executionStartTime the job execution start time
     * @return true if execution window exceeded
     */
    public boolean isExecutionWindowExceeded(LocalDateTime executionStartTime) {
        if (executionStartTime == null) {
            return false;
        }
        return executionStartTime.plusMinutes(processingWindowMinutes).isBefore(LocalDateTime.now());
    }

    // Getter methods for accessing exception details

    public String getJobName() {
        return jobName;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getErrorCode() {
        return errorType.getErrorCode();
    }

    public int getRetryCount() {
        return retryCount;
    }

    public boolean isRetryable() {
        return isRetryable;
    }

    public Map<String, Object> getJobParameters() {
        return new HashMap<>(jobParameters);
    }

    public Map<String, Object> getExecutionContext() {
        return new HashMap<>(executionContext);
    }

    public LocalDateTime getErrorTimestamp() {
        return errorTimestamp;
    }

    public Long getJobExecutionId() {
        return jobExecutionId;
    }

    public String getStepName() {
        return stepName;
    }

    public long getProcessingWindowMinutes() {
        return processingWindowMinutes;
    }

    /**
     * Gets the next retry delay in milliseconds based on exponential backoff.
     * 
     * @return delay in milliseconds before next retry attempt
     */
    public long getRetryDelayMs() {
        if (!isRetryable) {
            return -1;
        }
        // Exponential backoff: 30 seconds * 2^retryCount (capped at 5 minutes)
        long baseDelayMs = 30000; // 30 seconds
        long delayMs = baseDelayMs * (1L << retryCount);
        return Math.min(delayMs, 300000); // Cap at 5 minutes
    }

    /**
     * Creates a new BatchProcessingException for the next retry attempt.
     * 
     * @return new exception instance with incremented retry count
     */
    public BatchProcessingException createRetryException() {
        if (!isRetryable) {
            throw new IllegalStateException("Exception is not eligible for retry");
        }
        return new BatchProcessingException(jobName, errorType, getMessage(), getCause(), 
                                          retryCount + 1, jobParameters, executionContext);
    }

    @Override
    public String toString() {
        return String.format("BatchProcessingException{jobName='%s', errorType=%s, errorCode='%s', retryCount=%d, isRetryable=%s, timestamp=%s}",
                           jobName, errorType, errorType.getErrorCode(), retryCount, isRetryable, errorTimestamp);
    }
}