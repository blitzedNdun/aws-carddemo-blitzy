/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

/**
 * Custom exception class for batch processing errors in the CardDemo batch infrastructure.
 * 
 * This exception provides comprehensive error handling capabilities for batch job operations,
 * including support for retryable vs non-retryable errors, error categorization, and detailed
 * error information for proper error handling and recovery in REST endpoints.
 * 
 * Key Features:
 * - Retryable error classification for automatic retry logic
 * - Error code support for categorized error handling
 * - Detailed error messages for troubleshooting and logging
 * - Integration with Spring Boot error handling patterns
 * - Support for nested exception causes
 * 
 * Usage Examples:
 * - Job launch failures: Invalid parameters, missing job definitions
 * - Job execution errors: Database connectivity, file access issues
 * - Job control errors: Invalid state transitions, permission issues
 * - System errors: Resource exhaustion, configuration problems
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public class BatchProcessingException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Indicates whether this exception represents a retryable error condition.
     * Retryable errors are typically transient issues that may succeed on retry,
     * while non-retryable errors indicate configuration or data problems.
     */
    private final boolean retryable;

    /**
     * Optional error code for categorizing the specific type of batch processing error.
     * Used for programmatic error handling and monitoring/alerting purposes.
     */
    private final String errorCode;

    /**
     * Constructor for non-retryable batch processing exception with message only.
     * 
     * @param message Descriptive error message
     */
    public BatchProcessingException(String message) {
        super(message);
        this.retryable = false;
        this.errorCode = null;
    }

    /**
     * Constructor for batch processing exception with message and retryable flag.
     * 
     * @param message Descriptive error message
     * @param retryable Whether this error condition is retryable
     */
    public BatchProcessingException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
        this.errorCode = null;
    }

    /**
     * Constructor for batch processing exception with message, retryable flag, and error code.
     * 
     * @param message Descriptive error message
     * @param retryable Whether this error condition is retryable
     * @param errorCode Categorized error code for programmatic handling
     */
    public BatchProcessingException(String message, boolean retryable, String errorCode) {
        super(message);
        this.retryable = retryable;
        this.errorCode = errorCode;
    }

    /**
     * Constructor for batch processing exception with message and nested cause.
     * 
     * @param message Descriptive error message
     * @param cause Underlying exception that caused this error
     */
    public BatchProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = false;
        this.errorCode = null;
    }

    /**
     * Constructor for batch processing exception with message, cause, and retryable flag.
     * 
     * @param message Descriptive error message
     * @param cause Underlying exception that caused this error
     * @param retryable Whether this error condition is retryable
     */
    public BatchProcessingException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
        this.errorCode = null;
    }

    /**
     * Full constructor for batch processing exception with all parameters.
     * 
     * @param message Descriptive error message
     * @param cause Underlying exception that caused this error
     * @param retryable Whether this error condition is retryable
     * @param errorCode Categorized error code for programmatic handling
     */
    public BatchProcessingException(String message, Throwable cause, boolean retryable, String errorCode) {
        super(message, cause);
        this.retryable = retryable;
        this.errorCode = errorCode;
    }

    /**
     * Returns whether this exception represents a retryable error condition.
     * 
     * Retryable errors typically include:
     * - Temporary network connectivity issues
     * - Database connection timeouts
     * - Resource contention (locks, thread pool exhaustion)
     * - Temporary file system issues
     * 
     * Non-retryable errors typically include:
     * - Invalid job parameters or configuration
     * - Missing job definitions or beans
     * - Security/permission violations
     * - Data validation failures
     * - Business rule violations
     * 
     * @return true if the error condition may succeed on retry, false otherwise
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Returns the categorized error code for this exception, if available.
     * 
     * Common error codes include:
     * - JOB_NOT_FOUND: Job definition or bean not found
     * - INVALID_PARAMETERS: Job parameter validation failure
     * - INVALID_STATE: Job state transition error
     * - RESOURCE_ERROR: System resource exhaustion
     * - DATABASE_ERROR: Database connectivity or transaction error
     * - FILE_ERROR: File system access error
     * - SECURITY_ERROR: Authentication or authorization failure
     * 
     * @return error code string, or null if not specified
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Provides detailed string representation including retryable flag and error code.
     * 
     * @return formatted string with exception details
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        
        if (errorCode != null) {
            sb.append(" [ErrorCode: ").append(errorCode).append("]");
        }
        
        sb.append(" [Retryable: ").append(retryable).append("]");
        
        return sb.toString();
    }
}