/*
 * Copyright (c) 2024 CardDemo Application
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Base response DTO providing common fields and structure for all API response DTOs
 * with success status, error message, and timestamp information supporting consistent
 * response patterns across microservices.
 * 
 * This class serves as the foundation for all REST API responses in the CardDemo system,
 * ensuring consistent response structure and audit trail capabilities across all 36
 * Spring Boot microservices. It replaces traditional COBOL COMMAREA structures with
 * modern JSON-based response patterns while preserving essential audit information.
 * 
 * Design Rationale:
 * - Provides standardized HTTP-equivalent response semantics replacing CICS transaction responses
 * - Includes comprehensive audit trail information for debugging and compliance
 * - Uses Jackson annotations for optimal JSON serialization performance
 * - Supports correlation IDs for distributed tracing across microservice boundaries
 * - Maintains null-safe JSON output through JsonInclude configuration
 * 
 * Usage Pattern:
 * All specific response DTOs should extend this base class to inherit common response
 * metadata while adding domain-specific data fields. This ensures consistent API
 * behavior across all CardDemo microservices.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponseDto {

    /**
     * Success status indicator for the API operation.
     * 
     * Replaces traditional COBOL return codes with boolean success/failure semantics.
     * True indicates successful operation completion, false indicates business logic
     * errors or validation failures. This field provides HTTP-equivalent status
     * information in JSON format for consistent client-side error handling.
     */
    private boolean success;

    /**
     * Error message providing detailed failure information.
     * 
     * Contains human-readable error descriptions when success is false, similar to
     * COBOL message structures found in CSMSG01Y and CSMSG02Y copybooks.
     * Supports internationalization and provides detailed context for troubleshooting.
     * Null when operation succeeds to optimize JSON payload size.
     */
    private String errorMessage;

    /**
     * Response generation timestamp for audit trail and debugging purposes.
     * 
     * Records the exact moment when the response was created, essential for:
     * - Audit trail compliance (SOX, PCI DSS requirements)
     * - Distributed system debugging and performance analysis
     * - Session timeout management equivalent to CICS terminal storage
     * - Compliance reporting and regulatory examination support
     * 
     * Uses LocalDateTime for precise timestamp representation with ISO-8601 formatting.
     */
    private LocalDateTime timestamp;

    /**
     * Correlation ID for request tracking and distributed tracing.
     * 
     * Enables end-to-end transaction tracking across multiple microservices,
     * replacing CICS transaction IDs with modern distributed system correlation.
     * Essential for:
     * - Performance monitoring and bottleneck identification
     * - Security audit trails and incident investigation
     * - Distributed debugging across service boundaries
     * - Compliance reporting with request/response correlation
     */
    private String correlationId;

    /**
     * Default constructor initializing timestamp to current system time.
     * 
     * Automatically sets the response timestamp to ensure accurate audit trail
     * information for all API responses. The timestamp reflects the exact moment
     * of response creation for precise audit and debugging capabilities.
     */
    public BaseResponseDto() {
        this.timestamp = LocalDateTime.now();
        this.success = true; // Default to success, explicitly set to false for errors
    }

    /**
     * Constructor for successful responses with correlation ID.
     * 
     * Creates a successful response with specified correlation ID for distributed
     * tracing support. Used when operation completes successfully and correlation
     * tracking is required across microservice boundaries.
     * 
     * @param correlationId Unique identifier for request correlation and tracing
     */
    public BaseResponseDto(String correlationId) {
        this();
        this.correlationId = correlationId;
    }

    /**
     * Constructor for error responses with detailed error information.
     * 
     * Creates an error response with failure status, error message, and correlation ID.
     * Used when business logic errors or validation failures occur, providing
     * comprehensive error context for client applications and audit systems.
     * 
     * @param errorMessage Descriptive error message for client consumption
     * @param correlationId Unique identifier for request correlation and tracing
     */
    public BaseResponseDto(String errorMessage, String correlationId) {
        this();
        this.success = false;
        this.errorMessage = errorMessage;
        this.correlationId = correlationId;
    }

    /**
     * Static factory method for creating successful responses.
     * 
     * Provides a fluent API for creating successful response instances with
     * proper correlation tracking. Ensures consistent success response creation
     * across all microservices with automatic timestamp generation.
     * 
     * @param correlationId Unique identifier for request correlation
     * @return BaseResponseDto instance configured for success
     */
    public static BaseResponseDto success(String correlationId) {
        return new BaseResponseDto(correlationId);
    }

    /**
     * Static factory method for creating error responses.
     * 
     * Provides a fluent API for creating error response instances with
     * comprehensive error information and correlation tracking. Ensures
     * consistent error response structure across all microservices.
     * 
     * @param errorMessage Descriptive error message
     * @param correlationId Unique identifier for request correlation
     * @return BaseResponseDto instance configured for error
     */
    public static BaseResponseDto error(String errorMessage, String correlationId) {
        return new BaseResponseDto(errorMessage, correlationId);
    }

    /**
     * Returns the success status of the API operation.
     * 
     * @return true if operation completed successfully, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the API operation.
     * 
     * @param success true for successful operations, false for failures
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Returns the error message for failed operations.
     * 
     * @return error message string, null for successful operations
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for failed operations.
     * 
     * @param errorMessage descriptive error message, null for successful operations
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        // Automatically set success to false when error message is provided
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            this.success = false;
        }
    }

    /**
     * Returns the response generation timestamp.
     * 
     * @return LocalDateTime representing when the response was created
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the response generation timestamp.
     * 
     * Typically used for testing purposes or when specific timestamp control is needed.
     * In normal operation, timestamp is automatically set during construction.
     * 
     * @param timestamp LocalDateTime for response creation time
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the correlation ID for request tracking.
     * 
     * @return correlation ID string for distributed tracing
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Sets the correlation ID for request tracking.
     * 
     * @param correlationId unique identifier for request correlation across services
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Returns a formatted timestamp string for human-readable display.
     * 
     * Provides ISO-8601 formatted timestamp string suitable for logging,
     * debugging, and audit trail purposes. Uses consistent date format
     * across all microservices for operational consistency.
     * 
     * @return formatted timestamp string in ISO-8601 format
     */
    public String getFormattedTimestamp() {
        if (timestamp != null) {
            return timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return null;
    }

    /**
     * Utility method to check if the response represents an error condition.
     * 
     * Provides convenient boolean check for error conditions without requiring
     * clients to check both success flag and error message presence.
     * 
     * @return true if response contains error information, false otherwise
     */
    public boolean hasError() {
        return !success || (errorMessage != null && !errorMessage.trim().isEmpty());
    }

    /**
     * Utility method to set error condition with automatic success flag management.
     * 
     * Provides atomic operation to set both error message and success status,
     * ensuring consistent error state management across all response instances.
     * 
     * @param errorMessage descriptive error message for the failure condition
     */
    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = false;
    }

    /**
     * Returns string representation of the response for logging and debugging.
     * 
     * Provides comprehensive string representation including all response metadata
     * for debugging, logging, and audit purposes. Formats key information in
     * human-readable format while preserving correlation IDs for tracing.
     * 
     * @return formatted string representation of the response
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BaseResponseDto{");
        sb.append("success=").append(success);
        if (errorMessage != null) {
            sb.append(", errorMessage='").append(errorMessage).append('\'');
        }
        if (timestamp != null) {
            sb.append(", timestamp=").append(getFormattedTimestamp());
        }
        if (correlationId != null) {
            sb.append(", correlationId='").append(correlationId).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Equals method for proper comparison of response objects.
     * 
     * Implements proper equality comparison based on all response fields,
     * essential for testing and caching scenarios in microservices architecture.
     * 
     * @param obj object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BaseResponseDto that = (BaseResponseDto) obj;
        
        if (success != that.success) return false;
        if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        return correlationId != null ? correlationId.equals(that.correlationId) : that.correlationId == null;
    }

    /**
     * Hash code method for proper object hashing in collections.
     * 
     * Implements consistent hash code generation based on all response fields,
     * supporting proper collection behavior and caching strategies.
     * 
     * @return computed hash code for the response object
     */
    @Override
    public int hashCode() {
        int result = (success ? 1 : 0);
        result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (correlationId != null ? correlationId.hashCode() : 0);
        return result;
    }
}