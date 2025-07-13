package com.carddemo.common.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Audit Information DTO for comprehensive transaction audit trail tracking
 * in the CardDemo microservices architecture.
 * 
 * This DTO captures all audit-relevant information for compliance with 
 * SOX Section 404, PCI DSS, and GDPR requirements, supporting distributed
 * transaction correlation across the Spring Boot microservices ecosystem.
 * 
 * The audit information includes user context, operation details, timestamps,
 * correlation data, and security tracking information necessary for complete
 * audit trail compliance in financial transaction processing.
 */
public class AuditInfo {
    
    // Standard ISO formatter for consistent timestamp serialization
    private static final DateTimeFormatter AUDIT_TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * User identifier who performed the operation
     * Maps to Spring Security authentication context and PostgreSQL users table
     */
    @JsonProperty("user_id")
    private String userId;
    
    /**
     * Type of operation performed (CREATE, READ, UPDATE, DELETE, LOGIN, etc.)
     * Used for comprehensive audit trail categorization and compliance reporting
     */
    @JsonProperty("operation_type")
    private String operationType;
    
    /**
     * Precise timestamp when the operation occurred
     * Uses LocalDateTime for exact audit trail chronology with millisecond precision
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    /**
     * Correlation ID for distributed transaction tracking across microservices
     * Enables complete transaction flow visibility in the Spring Cloud architecture
     */
    @JsonProperty("correlation_id")
    private String correlationId;
    
    /**
     * Session identifier from Spring Session Redis store
     * Links audit events to user session context for complete audit correlation
     */
    @JsonProperty("session_id")
    private String sessionId;
    
    /**
     * Source system or microservice that initiated the operation
     * Identifies the specific Spring Boot service in the distributed architecture
     */
    @JsonProperty("source_system")
    private String sourceSystem;
    
    /**
     * Client IP address for security audit and compliance tracking
     * Captured from HTTP request headers for security incident investigation
     */
    @JsonProperty("ip_address")
    private String ipAddress;
    
    /**
     * User agent string for enhanced security audit capabilities
     * Required for security audit requirements per technical specification
     */
    @JsonProperty("user_agent")
    private String userAgent;
    
    /**
     * Additional contextual information about the operation
     * JSON-serialized metadata for flexible audit information storage
     */
    @JsonProperty("operation_context")
    private String operationContext;
    
    /**
     * Result status of the audited operation
     * SUCCESS, FAILURE, ERROR for operation outcome tracking
     */
    @JsonProperty("operation_result")
    private String operationResult;
    
    /**
     * Default constructor for JSON deserialization and framework compatibility
     */
    public AuditInfo() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor for creating audit information with core tracking data
     * 
     * @param userId The user performing the operation
     * @param operationType The type of operation being performed
     * @param correlationId The correlation ID for distributed tracking
     */
    public AuditInfo(String userId, String operationType, String correlationId) {
        this();
        this.userId = userId;
        this.operationType = operationType;
        this.correlationId = correlationId;
    }
    
    /**
     * Comprehensive constructor for creating complete audit information
     * 
     * @param userId The user performing the operation
     * @param operationType The type of operation being performed
     * @param correlationId The correlation ID for distributed tracking
     * @param sessionId The session ID from Spring Session
     * @param sourceSystem The source microservice or system
     * @param ipAddress The client IP address
     */
    public AuditInfo(String userId, String operationType, String correlationId, 
                     String sessionId, String sourceSystem, String ipAddress) {
        this(userId, operationType, correlationId);
        this.sessionId = sessionId;
        this.sourceSystem = sourceSystem;
        this.ipAddress = ipAddress;
    }
    
    // Getter and Setter methods as specified in the exports schema
    
    /**
     * Get the user identifier who performed the operation
     * 
     * @return The user ID from Spring Security context
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Set the user identifier who performed the operation
     * 
     * @param userId The user ID from Spring Security authentication
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * Get the operation type being audited
     * 
     * @return The operation type (CREATE, READ, UPDATE, DELETE, etc.)
     */
    public String getOperationType() {
        return operationType;
    }
    
    /**
     * Set the operation type being audited
     * 
     * @param operationType The type of operation for audit categorization
     */
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    /**
     * Get the timestamp when the operation occurred
     * 
     * @return LocalDateTime with precise operation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set the timestamp when the operation occurred
     * 
     * @param timestamp The operation timestamp for audit chronology
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Get the correlation ID for distributed transaction tracking
     * 
     * @return The correlation ID for microservices transaction correlation
     */
    public String getCorrelationId() {
        return correlationId;
    }
    
    /**
     * Set the correlation ID for distributed transaction tracking
     * 
     * @param correlationId The correlation ID for transaction flow tracking
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    /**
     * Get the session ID from Spring Session management
     * 
     * @return The session ID for user session correlation
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Set the session ID from Spring Session management
     * 
     * @param sessionId The session ID for audit session correlation
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Get the source system that initiated the operation
     * 
     * @return The source microservice or system identifier
     */
    public String getSourceSystem() {
        return sourceSystem;
    }
    
    /**
     * Set the source system that initiated the operation
     * 
     * @param sourceSystem The source microservice identifier
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
    
    /**
     * Get the client IP address for security audit tracking
     * 
     * @return The client IP address for security compliance
     */
    public String getIpAddress() {
        return ipAddress;
    }
    
    /**
     * Set the client IP address for security audit tracking
     * 
     * @param ipAddress The client IP address for security audit
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    /**
     * Get the user agent string for enhanced security audit
     * 
     * @return The user agent string from HTTP request headers
     */
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Set the user agent string for enhanced security audit
     * 
     * @param userAgent The user agent for security tracking
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    /**
     * Get additional operation context information
     * 
     * @return JSON-serialized operation context metadata
     */
    public String getOperationContext() {
        return operationContext;
    }
    
    /**
     * Set additional operation context information
     * 
     * @param operationContext JSON-serialized context metadata
     */
    public void setOperationContext(String operationContext) {
        this.operationContext = operationContext;
    }
    
    /**
     * Get the operation result status
     * 
     * @return The operation result (SUCCESS, FAILURE, ERROR)
     */
    public String getOperationResult() {
        return operationResult;
    }
    
    /**
     * Set the operation result status
     * 
     * @param operationResult The outcome of the audited operation
     */
    public void setOperationResult(String operationResult) {
        this.operationResult = operationResult;
    }
    
    /**
     * Get formatted timestamp string for consistent audit log output
     * Uses the imported LocalDateTime format() method as specified in external imports
     * 
     * @return Formatted timestamp string in ISO format
     */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(AUDIT_TIMESTAMP_FORMATTER) : null;
    }
    
    /**
     * Get timestamp as string using the imported toString() method
     * Provides standard LocalDateTime string representation for debugging
     * 
     * @return Standard LocalDateTime string representation
     */
    public String getTimestampAsString() {
        return timestamp != null ? timestamp.toString() : null;
    }
    
    /**
     * Create a new AuditInfo instance with current timestamp
     * Uses the imported LocalDateTime.now() method as specified in external imports
     * 
     * @return New AuditInfo with current timestamp
     */
    public static AuditInfo createWithCurrentTimestamp() {
        AuditInfo auditInfo = new AuditInfo();
        auditInfo.setTimestamp(LocalDateTime.now());
        return auditInfo;
    }
    
    /**
     * Builder pattern for convenient AuditInfo construction
     * Enables fluent API creation of audit information objects
     */
    public static class Builder {
        private AuditInfo auditInfo;
        
        public Builder() {
            this.auditInfo = new AuditInfo();
        }
        
        public Builder userId(String userId) {
            auditInfo.setUserId(userId);
            return this;
        }
        
        public Builder operationType(String operationType) {
            auditInfo.setOperationType(operationType);
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            auditInfo.setCorrelationId(correlationId);
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            auditInfo.setSessionId(sessionId);
            return this;
        }
        
        public Builder sourceSystem(String sourceSystem) {
            auditInfo.setSourceSystem(sourceSystem);
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            auditInfo.setIpAddress(ipAddress);
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            auditInfo.setUserAgent(userAgent);
            return this;
        }
        
        public Builder operationContext(String operationContext) {
            auditInfo.setOperationContext(operationContext);
            return this;
        }
        
        public Builder operationResult(String operationResult) {
            auditInfo.setOperationResult(operationResult);
            return this;
        }
        
        public AuditInfo build() {
            return auditInfo;
        }
    }
    
    /**
     * Create a Builder instance for fluent API construction
     * 
     * @return New Builder instance for AuditInfo construction
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * String representation for debugging and logging purposes
     * Includes all key audit information fields
     * 
     * @return String representation of audit information
     */
    @Override
    public String toString() {
        return String.format(
            "AuditInfo{userId='%s', operationType='%s', timestamp=%s, correlationId='%s', " +
            "sessionId='%s', sourceSystem='%s', ipAddress='%s', operationResult='%s'}",
            userId, operationType, timestamp, correlationId, sessionId, sourceSystem, 
            ipAddress, operationResult
        );
    }
    
    /**
     * Equals implementation for audit information comparison
     * Based on correlation ID and timestamp for uniqueness
     * 
     * @param obj Object to compare
     * @return true if audit information matches
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AuditInfo auditInfo = (AuditInfo) obj;
        
        if (correlationId != null ? !correlationId.equals(auditInfo.correlationId) : auditInfo.correlationId != null)
            return false;
        if (timestamp != null ? !timestamp.equals(auditInfo.timestamp) : auditInfo.timestamp != null)
            return false;
        
        return userId != null ? userId.equals(auditInfo.userId) : auditInfo.userId == null;
    }
    
    /**
     * Hash code implementation for audit information
     * Based on correlation ID, timestamp, and user ID
     * 
     * @return Hash code for audit information
     */
    @Override
    public int hashCode() {
        int result = correlationId != null ? correlationId.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        return result;
    }
}