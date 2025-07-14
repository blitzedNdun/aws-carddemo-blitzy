package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base request DTO providing common fields and structure for all API request DTOs
 * in the CardDemo microservices architecture. This class ensures consistent request
 * patterns across all 36 Spring Boot microservices, supporting distributed transaction
 * tracking, user context management, and comprehensive audit compliance.
 * 
 * Design Principles:
 * - Correlation ID propagation for distributed tracing across service boundaries
 * - User context preservation for authentication and authorization
 * - Request metadata for audit trail and compliance requirements
 * - JSON serialization consistency using Jackson annotations
 * - Stateless microservice architecture support through request-level context
 * 
 * Integration with Cloud-Native Architecture:
 * - Spring Cloud Gateway request routing and correlation
 * - JWT authentication context propagation
 * - Redis session management integration
 * - Prometheus metrics collection support
 * - ELK Stack logging correlation
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
public abstract class BaseRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique correlation identifier for tracking distributed transactions across
     * microservice boundaries. This ID enables end-to-end request tracing through
     * Spring Cloud Gateway routing, service-to-service communications, and audit
     * logging. Generated automatically if not provided by the client.
     * 
     * Used for:
     * - OpenTelemetry distributed tracing correlation
     * - Prometheus metrics aggregation
     * - ELK Stack log correlation
     * - Spring Cloud Gateway request routing
     * - Cross-service transaction coordination
     */
    @JsonProperty("correlation_id")
    private String correlationId;

    /**
     * Authenticated user identifier extracted from JWT token claims.
     * This field supports role-based access control, audit logging,
     * and business logic authorization across all microservices.
     * 
     * Integration points:
     * - Spring Security JWT authentication context
     * - @PreAuthorize method-level security
     * - Audit logging and compliance tracking
     * - Business rule validation and ownership checks
     * - User activity monitoring and analytics
     */
    @JsonProperty("user_id")
    private String userId;

    /**
     * Distributed session identifier linking to Redis session store.
     * Enables stateless microservice architecture while preserving
     * pseudo-conversational behavior equivalent to CICS terminal storage.
     * 
     * Session management features:
     * - Redis cluster session persistence
     * - TTL-based session expiration
     * - Cross-service session sharing
     * - Session timeout handling
     * - Stateless service design support
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * Request creation timestamp for audit trail, performance monitoring,
     * and timeout management. Automatically set to current time if not
     * provided by the client application.
     * 
     * Monitoring and audit applications:
     * - Request processing time measurement
     * - SLA compliance monitoring (< 200ms at 95th percentile)
     * - Audit trail chronological ordering
     * - Request timeout detection
     * - Performance trend analysis
     */
    @JsonProperty("request_timestamp")
    private Instant requestTimestamp;

    /**
     * Default constructor initializing common request metadata.
     * Automatically generates correlation ID and sets request timestamp
     * to support comprehensive request tracking and audit requirements.
     */
    protected BaseRequestDto() {
        this.correlationId = UUID.randomUUID().toString();
        this.requestTimestamp = Instant.now();
    }

    /**
     * Constructor with explicit correlation ID for request correlation
     * across service boundaries. Used when correlation ID is provided
     * by API Gateway or upstream services for distributed tracing.
     * 
     * @param correlationId Unique identifier for request correlation
     */
    protected BaseRequestDto(String correlationId) {
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.requestTimestamp = Instant.now();
    }

    /**
     * Full constructor for complete request context initialization.
     * Used by service-to-service communications where complete user
     * context and session information must be propagated.
     * 
     * @param correlationId Unique identifier for request correlation
     * @param userId Authenticated user identifier from JWT token
     * @param sessionId Redis session store identifier
     */
    protected BaseRequestDto(String correlationId, String userId, String sessionId) {
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.userId = userId;
        this.sessionId = sessionId;
        this.requestTimestamp = Instant.now();
    }

    /**
     * Retrieves the correlation ID for distributed transaction tracking.
     * This identifier enables end-to-end request tracing across all
     * microservices in the CardDemo architecture.
     * 
     * @return Unique correlation identifier for request tracking
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Sets the correlation ID for distributed transaction tracking.
     * Used by Spring Cloud Gateway and inter-service communications
     * to maintain request correlation across service boundaries.
     * 
     * @param correlationId Unique identifier for request correlation
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Retrieves the authenticated user identifier for authorization
     * and audit purposes. This value is extracted from JWT token
     * claims and used throughout the microservices ecosystem.
     * 
     * @return User identifier from authentication context
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the authenticated user identifier for request context.
     * Used by Spring Security integration to propagate user identity
     * across service calls and enable role-based access control.
     * 
     * @param userId User identifier from JWT authentication
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Retrieves the session identifier for distributed session management.
     * Links to Redis session store for stateless microservice architecture
     * while maintaining user context across service boundaries.
     * 
     * @return Session identifier for Redis session store
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session identifier for distributed session management.
     * Used by Spring Session integration to maintain user state
     * across stateless microservice interactions.
     * 
     * @param sessionId Session identifier for Redis session store
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Retrieves the request creation timestamp for audit and monitoring.
     * Used for performance measurement, audit trail chronology,
     * and SLA compliance verification.
     * 
     * @return Request creation timestamp
     */
    public Instant getRequestTimestamp() {
        return requestTimestamp;
    }

    /**
     * Sets the request creation timestamp for audit trail.
     * Typically set automatically during object construction,
     * but can be overridden for testing or batch processing scenarios.
     * 
     * @param requestTimestamp Request creation timestamp
     */
    public void setRequestTimestamp(Instant requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    /**
     * Validates that required request context is present for processing.
     * Ensures correlation ID is available for distributed tracing and
     * request timestamp is set for audit compliance.
     * 
     * @return true if request has valid context for processing
     */
    @JsonIgnore
    public boolean isValidRequestContext() {
        return correlationId != null && !correlationId.trim().isEmpty()
                && requestTimestamp != null;
    }

    /**
     * Validates that user authentication context is present.
     * Ensures user ID is available for authorization and audit logging.
     * 
     * @return true if request has authenticated user context
     */
    @JsonIgnore
    public boolean hasAuthenticatedUser() {
        return userId != null && !userId.trim().isEmpty();
    }

    /**
     * Validates that session context is present for stateful operations.
     * Ensures session ID is available for Redis session store integration.
     * 
     * @return true if request has valid session context
     */
    @JsonIgnore
    public boolean hasValidSession() {
        return sessionId != null && !sessionId.trim().isEmpty();
    }

    /**
     * Creates a copy of this request with the same correlation context
     * but without user-specific data. Used for service-to-service
     * communications where correlation must be preserved but user
     * context may need to be re-established.
     * 
     * @return New request instance with correlation context preserved
     */
    public BaseRequestDto createCorrelatedRequest() {
        return new BaseRequestDto(this.correlationId) {};
    }

    /**
     * Provides string representation of the base request context
     * for logging and debugging purposes. Excludes sensitive user
     * information while including key identifiers for correlation.
     * 
     * @return String representation of request context
     */
    @Override
    public String toString() {
        return String.format("BaseRequestDto{correlationId='%s', userId='%s', sessionId='%s', requestTimestamp=%s}",
                correlationId,
                userId != null ? "[PROTECTED]" : null,
                sessionId != null ? "[PROTECTED]" : null,
                requestTimestamp);
    }

    /**
     * Equality comparison based on correlation ID for request deduplication
     * and caching scenarios. Two requests with the same correlation ID
     * are considered equal for idempotency purposes.
     * 
     * @param obj Object to compare for equality
     * @return true if objects have the same correlation ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BaseRequestDto that = (BaseRequestDto) obj;
        return correlationId != null ? correlationId.equals(that.correlationId) : that.correlationId == null;
    }

    /**
     * Hash code based on correlation ID for consistent hashing
     * in collections and caching mechanisms.
     * 
     * @return Hash code based on correlation ID
     */
    @Override
    public int hashCode() {
        return correlationId != null ? correlationId.hashCode() : 0;
    }
}