package com.carddemo.common.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Audit Information DTO containing transaction audit trail details including user context,
 * timestamps, operation type, and correlation information for compliance and tracking purposes.
 * 
 * This DTO supports comprehensive audit requirements for the CardDemo application including:
 * - SOX compliance with immutable audit trails for financial transaction tracking
 * - PCI DSS requirements for cardholder data access tracking and security monitoring
 * - GDPR data processing activity logging for regulatory compliance
 * - Distributed transaction correlation across microservices architecture
 * 
 * All audit information is captured in structured JSON format compatible with
 * ELK stack integration for centralized logging and compliance reporting.
 * 
 * The audit trail supports correlation across distributed microservices through
 * correlation IDs and session information, enabling end-to-end transaction tracking
 * and comprehensive security incident investigation capabilities.
 */
public class AuditInfo {
    
    /**
     * Unique identifier of the user performing the audited operation.
     * Maps to the authenticated user's ID from the Spring Security context.
     * Essential for SOX compliance and user accountability tracking.
     */
    @JsonProperty("user_id")
    private String userId;
    
    /**
     * Type of operation being performed for audit classification and compliance reporting.
     * Examples: LOGIN, LOGOUT, ACCOUNT_VIEW, ACCOUNT_UPDATE, TRANSACTION_CREATE, 
     * CARD_STATUS_CHANGE, USER_MANAGEMENT, ADMIN_ACCESS, etc.
     * Supports detailed audit analysis and security pattern recognition.
     */
    @JsonProperty("operation_type")
    private String operationType;
    
    /**
     * Precise timestamp when the audited operation occurred.
     * Uses LocalDateTime for consistent timestamp formatting across the application
     * and compliance with audit trail temporal requirements.
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    /**
     * Correlation identifier for tracking related operations across distributed microservices.
     * Enables end-to-end transaction tracking and audit trail correlation in the
     * cloud-native microservices architecture. Critical for debugging and compliance
     * reporting across service boundaries.
     */
    @JsonProperty("correlation_id")
    private String correlationId;
    
    /**
     * Session identifier linking the audit event to the user's active session.
     * Supports session-based audit analysis and security incident investigation.
     * Integrates with Redis-backed session management for distributed session tracking.
     */
    @JsonProperty("session_id")
    private String sessionId;
    
    /**
     * Source system or service that generated the audit event.
     * Identifies the specific microservice for distributed system audit tracking.
     * Examples: AuthenticationService, AccountViewService, TransactionService, etc.
     */
    @JsonProperty("source_system")
    private String sourceSystem;
    
    /**
     * IP address of the client making the request for security audit requirements.
     * Enables geographic analysis, suspicious activity detection, and compliance
     * with security monitoring requirements. Essential for fraud prevention
     * and regulatory audit trails.
     */
    @JsonProperty("ip_address")
    private String ipAddress;
    
    /**
     * User agent string from the client request for detailed security audit tracking.
     * Provides browser/client information for security analysis, fraud detection,
     * and compliance monitoring. Essential for identifying suspicious patterns,
     * automated bot activity, and ensuring regulatory audit trail completeness.
     * Used for PCI DSS compliance and security incident investigation.
     */
    @JsonProperty("user_agent")
    private String userAgent;
    
    /**
     * Default constructor for JSON deserialization and framework compatibility.
     * Automatically initializes timestamp to current time when audit info is created,
     * ensuring accurate temporal tracking of audit events.
     */
    public AuditInfo() {
        // Initialize timestamp to current time when audit info is created
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor with essential audit information for immediate audit event creation.
     * Provides convenient creation pattern for common audit scenarios while
     * maintaining automatic timestamp initialization.
     * 
     * @param userId The authenticated user's identifier
     * @param operationType The type of operation being audited
     * @param correlationId The correlation ID for distributed tracking
     */
    public AuditInfo(String userId, String operationType, String correlationId) {
        this();
        this.userId = userId;
        this.operationType = operationType;
        this.correlationId = correlationId;
    }
    
    /**
     * Gets the user identifier for the audited operation.
     * 
     * @return The user ID performing the operation
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Sets the user identifier for the audited operation.
     * 
     * @param userId The user ID performing the operation
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * Gets the operation type being audited.
     * 
     * @return The operation type classification
     */
    public String getOperationType() {
        return operationType;
    }
    
    /**
     * Sets the operation type being audited.
     * 
     * @param operationType The operation type classification
     */
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    /**
     * Gets the timestamp when the operation occurred.
     * 
     * @return The operation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Sets the timestamp when the operation occurred.
     * 
     * @param timestamp The operation timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Gets the correlation identifier for distributed transaction tracking.
     * 
     * @return The correlation ID
     */
    public String getCorrelationId() {
        return correlationId;
    }
    
    /**
     * Sets the correlation identifier for distributed transaction tracking.
     * 
     * @param correlationId The correlation ID
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    /**
     * Gets the session identifier for the audit event.
     * 
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Sets the session identifier for the audit event.
     * 
     * @param sessionId The session ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Gets the source system that generated the audit event.
     * 
     * @return The source system identifier
     */
    public String getSourceSystem() {
        return sourceSystem;
    }
    
    /**
     * Sets the source system that generated the audit event.
     * 
     * @param sourceSystem The source system identifier
     */
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
    
    /**
     * Gets the IP address of the client making the request.
     * 
     * @return The client IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }
    
    /**
     * Sets the IP address of the client making the request.
     * 
     * @param ipAddress The client IP address
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    /**
     * Gets the user agent string from the client request.
     * 
     * @return The client user agent string
     */
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Sets the user agent string from the client request.
     * 
     * @param userAgent The client user agent string
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    /**
     * Creates a formatted string representation of the audit information.
     * Uses the LocalDateTime.toString() method to provide consistent formatting
     * compatible with structured logging and audit trail requirements.
     * 
     * @return Formatted string containing key audit information
     */
    @Override
    public String toString() {
        return String.format("AuditInfo{userId='%s', operationType='%s', timestamp='%s', correlationId='%s', sessionId='%s', sourceSystem='%s', ipAddress='%s', userAgent='%s'}", 
            userId, operationType, timestamp != null ? timestamp.toString() : null, correlationId, sessionId, sourceSystem, ipAddress, userAgent);
    }
    
    /**
     * Formats the timestamp using LocalDateTime's built-in formatting capabilities.
     * Provides consistent timestamp representation across the application and
     * ensures compatibility with JSON serialization and ELK stack processing.
     * 
     * @return Formatted timestamp string using LocalDateTime.toString()
     */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.toString() : null;
    }
    
    /**
     * Utility method to create a copy of the current audit info with updated timestamp.
     * Useful for creating related audit events while preserving correlation context.
     * Demonstrates usage of LocalDateTime.now() for timestamp updates.
     * 
     * @return New AuditInfo instance with current timestamp
     */
    public AuditInfo withCurrentTimestamp() {
        AuditInfo copy = new AuditInfo();
        copy.setUserId(this.userId);
        copy.setOperationType(this.operationType);
        copy.setTimestamp(LocalDateTime.now());
        copy.setCorrelationId(this.correlationId);
        copy.setSessionId(this.sessionId);
        copy.setSourceSystem(this.sourceSystem);
        copy.setIpAddress(this.ipAddress);
        copy.setUserAgent(this.userAgent);
        return copy;
    }
    
    /**
     * Formats the timestamp for audit reports using LocalDateTime formatting.
     * Demonstrates usage of LocalDateTime.format() method for custom timestamp display.
     * 
     * @return ISO-formatted timestamp string for audit reporting
     */
    public String getIsoFormattedTimestamp() {
        if (timestamp == null) {
            return null;
        }
        return timestamp.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}