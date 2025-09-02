package com.carddemo.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized error response DTO that structures error information returned by REST APIs.
 * Maintains compatibility with COBOL ABEND-DATA structure fields including error code,
 * culprit program, reason, and message. Supports field-level validation errors and
 * comprehensive error details for debugging.
 * 
 * COBOL ABEND-DATA structure compatibility:
 * - errorCode (4 chars like ABEND-CODE)
 * - culprit (8 chars like ABEND-CULPRIT)  
 * - reason (50 chars like ABEND-REASON)
 * - message (72 chars like ABEND-MSG)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class ErrorResponse {
    
    @JsonProperty("error_code")
    private String errorCode;
    
    @JsonProperty("culprit")
    private String culprit;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("field_errors")
    private Map<String, String> fieldErrors;
    
    // Default constructor
    public ErrorResponse() {
        this.fieldErrors = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor for builder pattern
    private ErrorResponse(Builder builder) {
        this.errorCode = builder.errorCode;
        this.culprit = builder.culprit;
        this.reason = builder.reason;
        this.message = builder.message;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.path = builder.path;
        this.fieldErrors = builder.fieldErrors != null ? builder.fieldErrors : new HashMap<>();
    }
    
    /**
     * Creates a new Builder instance for constructing ErrorResponse objects.
     * 
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getCulprit() {
        return culprit;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getMessage() {
        return message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
    
    // Setters
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public void setCulprit(String culprit) {
        this.culprit = culprit;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
    
    /**
     * Builder class for constructing ErrorResponse objects with method chaining.
     */
    public static class Builder {
        private String errorCode;
        private String culprit;
        private String reason;
        private String message;
        private LocalDateTime timestamp;
        private String path;
        private Map<String, String> fieldErrors;
        
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder culprit(String culprit) {
            this.culprit = culprit;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder path(String path) {
            this.path = path;
            return this;
        }
        
        public Builder fieldErrors(Map<String, String> fieldErrors) {
            this.fieldErrors = fieldErrors;
            return this;
        }
        
        public Builder addFieldError(String field, String error) {
            if (this.fieldErrors == null) {
                this.fieldErrors = new HashMap<>();
            }
            this.fieldErrors.put(field, error);
            return this;
        }
        
        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }
}