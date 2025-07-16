package com.carddemo.common.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Audit Information DTO for comprehensive transaction audit trail tracking.
 * 
 * This DTO provides complete audit information including user context, operation details,
 * timestamps, and correlation information required for compliance with SOX, PCI DSS, and GDPR
 * regulations in the CardDemo cloud-native microservices architecture.
 * 
 * Key Features:
 * - User context tracking for authentication and authorization audit
 * - Operation type and source system identification for complete transaction tracking
 * - Correlation and session IDs for distributed microservice transaction tracing
 * - IP address and user agent tracking for security audit requirements
 * - Comprehensive timestamp information for compliance and monitoring
 * - JSON serialization support for ELK stack integration and real-time monitoring
 * 
 * Integration Points:
 * - Spring Security audit events for authentication and authorization tracking
 * - Spring Boot Actuator metrics for performance and compliance monitoring
 * - ELK stack (Elasticsearch, Logstash, Kibana) for centralized audit log analysis
 * - Prometheus metrics for real-time audit event monitoring
 * - OpenTelemetry distributed tracing for cross-service audit correlation
 * 
 * Compliance Support:
 * - SOX 404: Immutable audit trail with comprehensive user and operation tracking
 * - PCI DSS: Cardholder data access monitoring with user context and IP tracking
 * - GDPR: Data subject access tracking with processing activity correlation
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since Spring Boot 3.2.x
 */
public class AuditInfo {
    
    /**
     * User identifier performing the audited operation.
     * This field captures the authenticated user ID from Spring Security context
     * for complete user accountability and access control validation.
     */
    @JsonProperty("user_id")
    private String userId;
    
    /**
     * Type of operation being performed (CREATE, READ, UPDATE, DELETE, LOGIN, etc.).
     * This field categorizes the business operation for audit trail analysis
     * and compliance reporting requirements.
     */
    @JsonProperty("operation_type")
    private String operationType;
    
    /**
     * Timestamp when the audited operation occurred.
     * This field provides precise timing information for audit trail chronology
     * and regulatory compliance tracking using ISO 8601 format.
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    /**
     * Correlation identifier for distributed transaction tracking.
     * This field enables tracing of operations across multiple microservices
     * using OpenTelemetry trace correlation and Spring Cloud Sleuth integration.
     */
    @JsonProperty("correlation_id")
    private String correlationId;
    
    /**
     * Session identifier for user session tracking.
     * This field links audit events to specific user sessions managed by
     * Redis session store for comprehensive session lifecycle monitoring.
     */
    @JsonProperty("session_id")
    private String sessionId;
    
    /**
     * Source system or microservice generating the audit event.
     * This field identifies the specific Spring Boot microservice or system component
     * that initiated the audited operation for service-level audit analysis.
     */
    @JsonProperty("source_system")
    private String sourceSystem;
    
    /**
     * IP address of the client performing the operation.
     * This field captures the client's network location for security monitoring,
     * fraud detection, and geographic access pattern analysis.
     */
    @JsonProperty("ip_address")
    private String ipAddress;
    
    /**
     * User agent string from the client request.
     * This field provides browser/client information for security analysis,
     * device fingerprinting, and client compatibility tracking.
     */
    @JsonProperty("user_agent")
    private String userAgent;
    
    /**
     * Additional contextual information about the operation.
     * This field stores supplementary details such as affected resources,
     * operation parameters, or business-specific context for comprehensive audit tracking.
     */
    @JsonProperty("operation_context")
    private String operationContext;
    
    /**
     * Operation result status (SUCCESS, FAILURE, PARTIAL, etc.).
     * This field indicates the outcome of the audited operation for
     * success rate monitoring and error analysis.
     */
    @JsonProperty("operation_result")
    private String operationResult;
    
    /**
     * Duration of the operation in milliseconds.
     * This field tracks operation performance for SLA monitoring and
     * compliance with 200ms response time requirements.
     */
    @JsonProperty("operation_duration_ms")
    private Long operationDurationMs;
    
    /**
     * Default constructor for Jackson deserialization and Spring framework integration.
     */
    public AuditInfo() {
        // Initialize with current timestamp by default
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Convenience constructor for creating audit info with essential fields.
     * 
     * @param userId User identifier performing the operation
     * @param operationType Type of operation being performed
     * @param correlationId Correlation identifier for distributed tracing
     */
    public AuditInfo(String userId, String operationType, String correlationId) {
        this();
        this.userId = userId;
        this.operationType = operationType;
        this.correlationId = correlationId;
    }
    
    /**
     * Gets the user identifier performing the audited operation.
     * 
     * @return User ID from Spring Security authentication context
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Sets the user identifier performing the audited operation.
     * 
     * @param userId User ID from Spring Security authentication context
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * Gets the type of operation being performed.
     * 
     * @return Operation type (CREATE, READ, UPDATE, DELETE, etc.)
     */
    public String getOperationType() {
        return operationType;
    }
    
    /**
     * Sets the type of operation being performed.
     * 
     * @param operationType Operation type (CREATE, READ, UPDATE, DELETE, etc.)
     */
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    /**
     * Gets the timestamp when the audited operation occurred.
     * 
     * @return Operation timestamp in ISO 8601 format
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Sets the timestamp when the audited operation occurred.
     * 
     * @param timestamp Operation timestamp in ISO 8601 format
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Gets the correlation identifier for distributed transaction tracking.
     * 
     * @return Correlation ID for OpenTelemetry trace correlation
     */
    public String getCorrelationId() {
        return correlationId;
    }
    
    /**
     * Sets the correlation identifier for distributed transaction tracking.
     * 
     * @param correlationId Correlation ID for OpenTelemetry trace correlation
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    /**
     * Gets the session identifier for user session tracking.
     * 
     * @return Session ID from Redis session store
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Sets the session identifier for user session tracking.
     * 
     * @param sessionId Session ID from Redis session store
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Gets the source system or microservice generating the audit event.
     * 
     * @return Source system identifier
     */
    public String getSourceSystem() {
        return sourceSystem;
    }
    
    /**
     * Sets the source system or microservice generating the audit event.
     * 
     * @param sourceSystem Source system identifier
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
    
    /**
     * Gets the IP address of the client performing the operation.
     * 
     * @return Client IP address for security monitoring
     */
    public String getIpAddress() {
        return ipAddress;
    }
    
    /**
     * Sets the IP address of the client performing the operation.
     * 
     * @param ipAddress Client IP address for security monitoring
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    /**
     * Gets the user agent string from the client request.
     * 
     * @return User agent for client identification and security analysis
     */
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Sets the user agent string from the client request.
     * 
     * @param userAgent User agent for client identification and security analysis
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    /**
     * Gets additional contextual information about the operation.
     * 
     * @return Operation context and supplementary details
     */
    public String getOperationContext() {
        return operationContext;
    }
    
    /**
     * Sets additional contextual information about the operation.
     * 
     * @param operationContext Operation context and supplementary details
     */
    public void setOperationContext(String operationContext) {
        this.operationContext = operationContext;
    }
    
    /**
     * Gets the operation result status.
     * 
     * @return Operation result (SUCCESS, FAILURE, PARTIAL, etc.)
     */
    public String getOperationResult() {
        return operationResult;
    }
    
    /**
     * Sets the operation result status.
     * 
     * @param operationResult Operation result (SUCCESS, FAILURE, PARTIAL, etc.)
     */
    public void setOperationResult(String operationResult) {
        this.operationResult = operationResult;
    }
    
    /**
     * Gets the duration of the operation in milliseconds.
     * 
     * @return Operation duration for performance monitoring
     */
    public Long getOperationDurationMs() {
        return operationDurationMs;
    }
    
    /**
     * Sets the duration of the operation in milliseconds.
     * 
     * @param operationDurationMs Operation duration for performance monitoring
     */
    public void setOperationDurationMs(Long operationDurationMs) {
        this.operationDurationMs = operationDurationMs;
    }
    
    /**
     * Creates a string representation of the audit information for logging and debugging.
     * 
     * @return Formatted string with key audit fields
     */
    @Override
    public String toString() {
        return String.format(
            "AuditInfo{userId='%s', operationType='%s', timestamp=%s, correlationId='%s', " +
            "sessionId='%s', sourceSystem='%s', ipAddress='%s', operationResult='%s', " +
            "operationDurationMs=%d}",
            userId, operationType, timestamp, correlationId, sessionId, sourceSystem, 
            ipAddress, operationResult, operationDurationMs
        );
    }
    
    /**
     * Checks equality based on correlation ID and timestamp for duplicate detection.
     * 
     * @param obj Object to compare for equality
     * @return True if objects are equal based on audit criteria
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AuditInfo that = (AuditInfo) obj;
        
        if (correlationId != null ? !correlationId.equals(that.correlationId) : that.correlationId != null) {
            return false;
        }
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) {
            return false;
        }
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) {
            return false;
        }
        return operationType != null ? operationType.equals(that.operationType) : that.operationType == null;
    }
    
    /**
     * Generates hash code based on correlation ID and timestamp for consistent hashing.
     * 
     * @return Hash code for audit information instance
     */
    @Override
    public int hashCode() {
        int result = correlationId != null ? correlationId.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (operationType != null ? operationType.hashCode() : 0);
        return result;
    }
}