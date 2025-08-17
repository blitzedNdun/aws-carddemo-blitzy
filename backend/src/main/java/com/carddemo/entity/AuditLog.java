/*
 * AuditLog.java
 * 
 * JPA entity representing audit log records for comprehensive security event tracking
 * and compliance reporting. Stores immutable audit entries with cryptographic integrity
 * validation, supporting regulatory requirements and replacing mainframe SMF security
 * records with structured audit data.
 * 
 * This entity provides enterprise-grade audit trail capabilities as outlined in the
 * Security Architecture documentation (Section 6.4), ensuring regulatory compliance
 * and security monitoring for the CardDemo application.
 */
package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity class representing audit log records for security event tracking.
 * 
 * Provides comprehensive audit logging capabilities including:
 * - Immutable audit record design with creation timestamp
 * - Cryptographic integrity validation through hash fields
 * - Support for all security event types (authentication, authorization, etc.)
 * - Regulatory compliance fields for audit trail requirements
 * - Correlation ID support for tracking related audit events
 * - JSON serialization support for audit log export and monitoring integration
 * 
 * The audit log design ensures tamper-evident logging with cryptographic hashes
 * and supports enterprise audit requirements for financial services compliance.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_username", columnList = "username"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_source_ip", columnList = "source_ip"),
    @Index(name = "idx_audit_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_audit_resource_accessed", columnList = "resource_accessed"),
    @Index(name = "idx_audit_outcome", columnList = "outcome")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditLog {

    /**
     * Primary key for the audit log record.
     * Auto-generated sequence value ensuring unique identification.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long id;

    /**
     * Username associated with the audit event.
     * Identifies the user who performed the audited action.
     * Maximum length: 50 characters
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "username", length = 50, nullable = false)
    private String username;

    /**
     * Type of audit event being logged.
     * Categories include: AUTHENTICATION, AUTHORIZATION, SESSION, TRANSACTION,
     * CONFIGURATION, DATA_ACCESS, SECURITY_VIOLATION, SYSTEM_EVENT
     * Maximum length: 50 characters
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    /**
     * Timestamp when the audit event occurred.
     * Immutable field set at record creation time.
     * Used for chronological ordering and compliance reporting.
     */
    @NotNull
    @Column(name = "timestamp", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    /**
     * Source IP address from which the audited action originated.
     * Critical for security incident investigation and access analysis.
     * Maximum length: 45 characters (supports IPv6)
     */
    @Size(max = 45)
    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    /**
     * Resource that was accessed during the audited action.
     * Examples: account numbers, transaction IDs, configuration parameters
     * Maximum length: 200 characters
     */
    @Size(max = 200)
    @Column(name = "resource_accessed", length = 200)
    private String resourceAccessed;

    /**
     * Specific action that was performed on the resource.
     * Examples: CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, AUTHORIZE
     * Maximum length: 100 characters
     */
    @Size(max = 100)
    @Column(name = "action_performed", length = 100)
    private String actionPerformed;

    /**
     * Outcome of the audited action.
     * Values: SUCCESS, FAILURE, DENIED, WARNING, ERROR
     * Critical for security monitoring and compliance reporting.
     * Maximum length: 20 characters
     */
    @Size(max = 20)
    @Column(name = "outcome", length = 20)
    private String outcome;

    /**
     * Correlation ID linking related audit events.
     * Enables tracking of multi-step transactions and complex workflows.
     * Maximum length: 100 characters
     */
    @Size(max = 100)
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    /**
     * Additional details about the audit event.
     * Free-form text providing context and supplementary information.
     * Maximum length: 1000 characters
     */
    @Size(max = 1000)
    @Column(name = "details", length = 1000)
    private String details;

    /**
     * Cryptographic integrity hash for tamper detection.
     * SHA-256 hash of critical audit fields to ensure audit trail integrity.
     * Used for forensic analysis and compliance validation.
     * Maximum length: 64 characters
     */
    @Size(max = 64)
    @Column(name = "integrity_hash", length = 64)
    private String integrityHash;

    /**
     * Session identifier associated with the audit event.
     * Links audit events to user sessions for activity correlation.
     * Maximum length: 100 characters
     */
    @Size(max = 100)
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * User agent string for web-based actions.
     * Captures browser and application information for security analysis.
     * Maximum length: 500 characters
     */
    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Risk score assigned to the audit event.
     * Numeric value indicating the security risk level of the action.
     * Range: 0-100 (0 = low risk, 100 = high risk)
     */
    @Column(name = "risk_score")
    private Integer riskScore;

    /**
     * Compliance tags for regulatory categorization.
     * Comma-separated tags for compliance framework mapping.
     * Examples: SOX, PCI-DSS, GDPR, SOC2
     * Maximum length: 200 characters
     */
    @Size(max = 200)
    @Column(name = "compliance_tags", length = 200)
    private String complianceTags;

    /**
     * Default constructor for JPA.
     */
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor for creating audit log entries with required fields.
     *
     * @param username    Username performing the action
     * @param eventType   Type of audit event
     * @param sourceIp    Source IP address
     * @param outcome     Action outcome
     */
    public AuditLog(String username, String eventType, String sourceIp, String outcome) {
        this();
        this.username = username;
        this.eventType = eventType;
        this.sourceIp = sourceIp;
        this.outcome = outcome;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getResourceAccessed() {
        return resourceAccessed;
    }

    public void setResourceAccessed(String resourceAccessed) {
        this.resourceAccessed = resourceAccessed;
    }

    public String getActionPerformed() {
        return actionPerformed;
    }

    public void setActionPerformed(String actionPerformed) {
        this.actionPerformed = actionPerformed;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIntegrityHash() {
        return integrityHash;
    }

    public void setIntegrityHash(String integrityHash) {
        this.integrityHash = integrityHash;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getComplianceTags() {
        return complianceTags;
    }

    public void setComplianceTags(String complianceTags) {
        this.complianceTags = complianceTags;
    }

    // Utility Methods

    /**
     * Pre-persist callback to set the timestamp if not already set.
     */
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    /**
     * Checks if this audit log entry has integrity hash validation.
     *
     * @return true if integrity hash is present and non-empty
     */
    public boolean hasIntegrityValidation() {
        return integrityHash != null && !integrityHash.trim().isEmpty();
    }

    /**
     * Checks if this is a security-relevant audit event.
     *
     * @return true if event type indicates security concern
     */
    public boolean isSecurityEvent() {
        return eventType != null && (
            eventType.equals("AUTHENTICATION") ||
            eventType.equals("AUTHORIZATION") ||
            eventType.equals("SECURITY_VIOLATION") ||
            eventType.equals("ACCESS_DENIED")
        );
    }

    /**
     * Checks if this audit event represents a successful action.
     *
     * @return true if outcome indicates success
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(outcome);
    }

    /**
     * Checks if this audit event represents a failed action.
     *
     * @return true if outcome indicates failure
     */
    public boolean isFailed() {
        return "FAILURE".equals(outcome) || "ERROR".equals(outcome);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id) &&
               Objects.equals(username, auditLog.username) &&
               Objects.equals(eventType, auditLog.eventType) &&
               Objects.equals(timestamp, auditLog.timestamp) &&
               Objects.equals(sourceIp, auditLog.sourceIp) &&
               Objects.equals(correlationId, auditLog.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, eventType, timestamp, sourceIp, correlationId);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", sourceIp='" + sourceIp + '\'' +
                ", resourceAccessed='" + resourceAccessed + '\'' +
                ", actionPerformed='" + actionPerformed + '\'' +
                ", outcome='" + outcome + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", riskScore=" + riskScore +
                ", hasIntegrityHash=" + hasIntegrityValidation() +
                '}';
    }
}