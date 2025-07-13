/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Base Response DTO providing common fields and structure for all API response DTOs
 * in the CardDemo microservices ecosystem.
 * 
 * This class serves as the foundation for consistent response patterns across all
 * 36 Spring Boot microservices, replacing the COBOL message handling patterns
 * from CSMSG01Y.cpy and CSMSG02Y.cpy with modern JSON-based API responses.
 * 
 * Key Features:
 * - Success/error status with HTTP-equivalent semantics
 * - Standardized error message handling equivalent to COBOL ABEND-MSG structure
 * - Audit trail timestamp for compliance and debugging
 * - Correlation ID for distributed tracing across microservices
 * - Jackson annotations for consistent JSON serialization
 * 
 * Design Principles:
 * - Maintains functional equivalence with original CICS transaction response patterns
 * - Supports Spring Boot microservices architecture requirements
 * - Provides enterprise-grade audit trail and error handling capabilities
 * - Enables consistent API response formatting across all services
 * 
 * @author Blitzy Agent - CardDemo Transformation Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponseDto {
    
    /**
     * Success indicator flag following HTTP-equivalent semantics.
     * Maps conceptually to COBOL transaction completion status.
     * 
     * true  = Successful transaction execution (HTTP 2xx equivalent)
     * false = Transaction failure or business logic error (HTTP 4xx/5xx equivalent)
     */
    private boolean success;
    
    /**
     * Error message providing detailed information about transaction failures.
     * Replaces COBOL ABEND-MSG functionality with structured error reporting.
     * 
     * Contains business-friendly error descriptions for:
     * - Validation failures (equivalent to field-level COBOL edits)
     * - Business rule violations (equivalent to COBOL 88-level condition failures)
     * - System errors (equivalent to COBOL ABEND conditions)
     * 
     * Null when success = true, populated when success = false
     */
    private String errorMessage;
    
    /**
     * Response generation timestamp for audit trail and debugging purposes.
     * Provides precise timing information equivalent to CICS transaction timestamps.
     * 
     * Generated automatically at response creation time using LocalDateTime.now()
     * Formatted consistently using ISO-8601 standard for JSON serialization
     * 
     * Critical for:
     * - Financial audit trail compliance
     * - Performance monitoring and SLA verification
     * - Distributed transaction correlation and debugging
     */
    private LocalDateTime timestamp;
    
    /**
     * Correlation ID for distributed request tracking across microservices.
     * Enables end-to-end transaction tracing in the microservices architecture.
     * 
     * Generated at API Gateway level and propagated through all service calls
     * Replaces CICS task correlation with modern distributed tracing capabilities
     * 
     * Format: UUID string for unique identification across system boundaries
     * Used for:
     * - Cross-service transaction correlation
     * - Log aggregation and analysis
     * - Performance monitoring and bottleneck identification
     * - Support and troubleshooting workflows
     */
    private String correlationId;
    
    /**
     * Default constructor initializing timestamp to current time.
     * Establishes audit trail timestamp at response creation point.
     */
    public BaseResponseDto() {
        this.timestamp = LocalDateTime.now();
        this.success = true; // Default to success, set to false when errors occur
    }
    
    /**
     * Constructor for successful responses with correlation ID.
     * 
     * @param correlationId Unique identifier for request correlation
     */
    public BaseResponseDto(String correlationId) {
        this();
        this.correlationId = correlationId;
    }
    
    /**
     * Constructor for error responses with message and correlation ID.
     * 
     * @param errorMessage Detailed error description
     * @param correlationId Unique identifier for request correlation
     */
    public BaseResponseDto(String errorMessage, String correlationId) {
        this();
        this.success = false;
        this.errorMessage = errorMessage;
        this.correlationId = correlationId;
    }
    
    /**
     * Factory method for creating successful response instances.
     * Provides fluent API for success case creation.
     * 
     * @param correlationId Unique identifier for request correlation
     * @return BaseResponseDto configured for success
     */
    public static BaseResponseDto success(String correlationId) {
        return new BaseResponseDto(correlationId);
    }
    
    /**
     * Factory method for creating error response instances.
     * Provides fluent API for error case creation.
     * 
     * @param errorMessage Detailed error description
     * @param correlationId Unique identifier for request correlation
     * @return BaseResponseDto configured for error
     */
    public static BaseResponseDto error(String errorMessage, String correlationId) {
        return new BaseResponseDto(errorMessage, correlationId);
    }
    
    /**
     * Retrieves the success status indicator.
     * 
     * @return true if transaction was successful, false if error occurred
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Sets the success status indicator.
     * 
     * @param success true for successful execution, false for error conditions
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Retrieves the error message for failed transactions.
     * 
     * @return Detailed error description or null for successful transactions
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Sets the error message for failed transactions.
     * Automatically sets success flag to false when error message is provided.
     * 
     * @param errorMessage Detailed error description
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            this.success = false;
        }
    }
    
    /**
     * Retrieves the response generation timestamp.
     * 
     * @return LocalDateTime when response was created
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Sets the response generation timestamp.
     * Typically used for testing or when explicit timestamp control is required.
     * 
     * @param timestamp Response creation time
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Retrieves the correlation ID for distributed request tracking.
     * 
     * @return Unique identifier for cross-service correlation
     */
    public String getCorrelationId() {
        return correlationId;
    }
    
    /**
     * Sets the correlation ID for distributed request tracking.
     * 
     * @param correlationId Unique identifier for cross-service correlation
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    /**
     * Provides string representation for debugging and logging purposes.
     * Includes all essential fields for troubleshooting support.
     * 
     * @return Formatted string representation of the response
     */
    @Override
    public String toString() {
        return String.format(
            "BaseResponseDto{success=%s, errorMessage='%s', timestamp=%s, correlationId='%s'}",
            success,
            errorMessage,
            timestamp != null ? timestamp.toString() : "null",
            correlationId
        );
    }
    
    /**
     * Equality comparison based on all fields.
     * Supports unit testing and response validation scenarios.
     * 
     * @param obj Object to compare with
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
     * Hash code implementation consistent with equals method.
     * Enables proper usage in collections and caching scenarios.
     * 
     * @return Hash code value
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