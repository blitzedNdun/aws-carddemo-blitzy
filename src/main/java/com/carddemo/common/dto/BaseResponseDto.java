/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Base response DTO providing common fields and structure for all API response DTOs.
 * This class establishes consistent response patterns across all CardDemo microservices
 * by providing standard success/error status, message handling, and audit information.
 * 
 * Derived from COBOL message structures (CSMSG01Y/CSMSG02Y) but enhanced with
 * modern API response patterns including correlation IDs, timestamps, and
 * standardized error handling.
 * 
 * All response DTOs in the CardDemo application must extend this base class
 * to ensure consistent response format and proper audit trail maintenance.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponseDto {
    
    /**
     * Indicates whether the operation was successful.
     * true = successful operation, false = operation failed
     * Maps to COBOL transaction completion status from original CICS processing
     */
    private boolean success;
    
    /**
     * Error message describing any failure that occurred during processing.
     * Only populated when success = false
     * Derived from COBOL ABEND-MSG and ABEND-REASON fields for consistent error reporting
     */
    private String errorMessage;
    
    /**
     * Timestamp when the response was generated.
     * Provides audit trail and debugging information for all API responses
     * Uses LocalDateTime for precise timing without timezone complications
     */
    private LocalDateTime timestamp;
    
    /**
     * Correlation ID for request tracking and audit trail.
     * Enables distributed tracing across microservices and request correlation
     * for debugging and monitoring purposes
     */
    private String correlationId;
    
    /**
     * HTTP status code equivalent for REST API responses.
     * Maps CICS response codes to standard HTTP status codes
     */
    private Integer statusCode;
    
    /**
     * Additional descriptive message for successful operations.
     * Provides context-specific information beyond simple success/failure
     * Derived from COBOL CCDA-MSG-THANK-YOU and similar informational messages
     */
    private String message;
    
    /**
     * Operation name or transaction code for audit purposes.
     * Identifies the specific business operation that was performed
     * Maps to original CICS transaction codes (COSGN, COACTVW, etc.)
     */
    private String operation;
    
    /**
     * Default constructor initializing response with current timestamp
     */
    public BaseResponseDto() {
        this.timestamp = LocalDateTime.now();
        this.success = true; // Default to successful operation
        this.statusCode = 200; // Default HTTP OK status
    }
    
    /**
     * Constructor for successful response with message
     * 
     * @param message Success message to include in response
     */
    public BaseResponseDto(String message) {
        this();
        this.message = message;
    }
    
    /**
     * Constructor for error response
     * 
     * @param success Success status (should be false for errors)
     * @param errorMessage Error message describing the failure
     */
    public BaseResponseDto(boolean success, String errorMessage) {
        this();
        this.success = success;
        this.errorMessage = errorMessage;
        this.statusCode = success ? 200 : 400; // Set appropriate HTTP status
    }
    
    /**
     * Constructor for complete response initialization
     * 
     * @param success Success status
     * @param message Success message or error message
     * @param correlationId Request correlation ID
     * @param operation Operation name or transaction code
     */
    public BaseResponseDto(boolean success, String message, String correlationId, String operation) {
        this();
        this.success = success;
        if (success) {
            this.message = message;
            this.statusCode = 200;
        } else {
            this.errorMessage = message;
            this.statusCode = 400;
        }
        this.correlationId = correlationId;
        this.operation = operation;
    }
    
    /**
     * Gets the success status of the operation
     * 
     * @return true if operation was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Sets the success status of the operation
     * 
     * @param success true if operation was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Gets the error message describing any failure
     * 
     * @return error message or null if no error occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Sets the error message describing any failure
     * 
     * @param errorMessage error message describing the failure
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * Gets the timestamp when the response was generated
     * 
     * @return response generation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Sets the timestamp when the response was generated
     * 
     * @param timestamp response generation timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Gets the correlation ID for request tracking
     * 
     * @return correlation ID for audit trail
     */
    public String getCorrelationId() {
        return correlationId;
    }
    
    /**
     * Sets the correlation ID for request tracking
     * 
     * @param correlationId correlation ID for audit trail
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    /**
     * Gets the HTTP status code equivalent
     * 
     * @return HTTP status code
     */
    public Integer getStatusCode() {
        return statusCode;
    }
    
    /**
     * Sets the HTTP status code equivalent
     * 
     * @param statusCode HTTP status code
     */
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    /**
     * Gets the success message
     * 
     * @return success message or null if not applicable
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Sets the success message
     * 
     * @param message success message
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Gets the operation name or transaction code
     * 
     * @return operation name for audit purposes
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * Sets the operation name or transaction code
     * 
     * @param operation operation name for audit purposes
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    /**
     * Convenience method to create a successful response
     * 
     * @param message success message
     * @return BaseResponseDto with success status
     */
    public static BaseResponseDto success(String message) {
        return new BaseResponseDto(true, message, null, null);
    }
    
    /**
     * Convenience method to create a successful response with correlation ID
     * 
     * @param message success message
     * @param correlationId request correlation ID
     * @return BaseResponseDto with success status and correlation ID
     */
    public static BaseResponseDto success(String message, String correlationId) {
        return new BaseResponseDto(true, message, correlationId, null);
    }
    
    /**
     * Convenience method to create an error response
     * 
     * @param errorMessage error message describing the failure
     * @return BaseResponseDto with error status
     */
    public static BaseResponseDto error(String errorMessage) {
        return new BaseResponseDto(false, errorMessage, null, null);
    }
    
    /**
     * Convenience method to create an error response with correlation ID
     * 
     * @param errorMessage error message describing the failure
     * @param correlationId request correlation ID
     * @return BaseResponseDto with error status and correlation ID
     */
    public static BaseResponseDto error(String errorMessage, String correlationId) {
        return new BaseResponseDto(false, errorMessage, correlationId, null);
    }
    
    /**
     * Convenience method to create a response with full context
     * 
     * @param success success status
     * @param message success or error message
     * @param correlationId request correlation ID
     * @param operation operation name or transaction code
     * @return BaseResponseDto with complete context
     */
    public static BaseResponseDto create(boolean success, String message, 
                                        String correlationId, String operation) {
        return new BaseResponseDto(success, message, correlationId, operation);
    }
    
    /**
     * Checks if the response represents an error condition
     * 
     * @return true if this is an error response, false otherwise
     */
    public boolean isError() {
        return !success;
    }
    
    /**
     * Checks if the response has a correlation ID
     * 
     * @return true if correlation ID is present, false otherwise
     */
    public boolean hasCorrelationId() {
        return correlationId != null && !correlationId.trim().isEmpty();
    }
    
    /**
     * Gets the formatted timestamp as string
     * 
     * @return formatted timestamp string
     */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.toString() : null;
    }
    
    /**
     * Sets the timestamp to current time
     */
    public void updateTimestamp() {
        this.timestamp = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "BaseResponseDto{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                ", statusCode=" + statusCode +
                ", message='" + message + '\'' +
                ", operation='" + operation + '\'' +
                '}';
    }
}