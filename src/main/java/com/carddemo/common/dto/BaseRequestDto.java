package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Base request DTO providing common fields and structure for all API request DTOs.
 * 
 * This class serves as the foundation for consistent request patterns across all 36 
 * CardDemo microservices, supporting distributed transaction tracking, user context
 * management, and audit compliance requirements in the modernized Spring Boot architecture.
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Correlation ID for distributed transaction tracking across microservice boundaries</li>
 *   <li>User context information for audit trail and authorization decisions</li>
 *   <li>Session management integration with Redis-backed distributed sessions</li>
 *   <li>Request metadata for transaction correlation and compliance logging</li>
 *   <li>Standardized JSON serialization for consistent API request formatting</li>
 * </ul>
 * 
 * <p>This implementation replaces traditional CICS COMMAREA patterns with modern
 * REST API request structures while maintaining equivalent audit and tracking
 * capabilities for the enterprise credit card management system.</p>
 * 
 * <p>All concrete request DTOs in the CardDemo system must extend this base class
 * to ensure consistent request structure, enable distributed tracing capabilities,
 * and maintain audit compliance across the microservices architecture.</p>
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since 2024-01-01
 */
public class BaseRequestDto {
    
    /**
     * Unique correlation identifier for tracking requests across distributed microservices.
     * 
     * <p>This field enables distributed tracing, audit logging, and transaction correlation
     * across the 36 Spring Boot microservices. Automatically generated if not provided
     * to ensure every request is trackable through the system.</p>
     * 
     * <p>Used by OpenTelemetry for distributed tracing and by audit logging systems
     * for comprehensive transaction tracking equivalent to CICS transaction IDs.</p>
     */
    @JsonProperty("correlationId")
    private String correlationId;
    
    /**
     * Authenticated user identifier for authorization and audit trail purposes.
     * 
     * <p>Populated from JWT token claims during Spring Security authentication
     * filter processing. Required for all authenticated API calls in the system
     * and used by authorization services for role-based access control.</p>
     * 
     * <p>Replaces RACF user identification in the original mainframe system
     * while maintaining equivalent security and audit capabilities.</p>
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * Session identifier for distributed session management via Redis cluster.
     * 
     * <p>Enables stateless microservice architecture while maintaining user context
     * across service boundaries. Integrated with Spring Session Data Redis for
     * distributed session storage and retrieval.</p>
     * 
     * <p>Replaces CICS pseudo-conversational patterns with modern stateless
     * session management while preserving user state across request boundaries.</p>
     */
    @JsonProperty("sessionId")
    private String sessionId;
    
    /**
     * Request timestamp for audit trail and request ordering purposes.
     * 
     * <p>Automatically set during request processing to ensure accurate timing
     * information for compliance reporting, performance monitoring, and audit
     * trail maintenance. Uses ISO-8601 format for consistent time representation.</p>
     * 
     * <p>Essential for financial transaction compliance and system performance
     * analysis in the modernized microservices architecture.</p>
     */
    @JsonProperty("requestTimestamp")
    private Instant requestTimestamp;
    
    /**
     * Default constructor initializing request with correlation ID and timestamp.
     * 
     * <p>Ensures every request has trackable metadata even if not explicitly set.
     * The correlation ID is automatically generated using UUID to guarantee
     * uniqueness across distributed system boundaries.</p>
     */
    public BaseRequestDto() {
        this.correlationId = UUID.randomUUID().toString();
        this.requestTimestamp = Instant.now();
    }
    
    /**
     * Constructor with correlation ID for explicit tracking scenarios.
     * 
     * <p>Used when correlation ID needs to be propagated from upstream systems
     * or when specific correlation patterns are required for distributed
     * transaction coordination.</p>
     * 
     * @param correlationId the correlation identifier for this request
     */
    public BaseRequestDto(String correlationId) {
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.requestTimestamp = Instant.now();
    }
    
    /**
     * Gets the correlation ID for distributed transaction tracking.
     * 
     * @return the unique correlation identifier for this request
     */
    public String getCorrelationId() {
        return correlationId;
    }
    
    /**
     * Sets the correlation ID for distributed transaction tracking.
     * 
     * @param correlationId the unique correlation identifier for this request
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    /**
     * Gets the authenticated user identifier.
     * 
     * @return the user ID from authentication context
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Sets the authenticated user identifier.
     * 
     * @param userId the user ID from authentication context
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * Gets the session identifier for distributed session management.
     * 
     * @return the session ID for Redis-backed session storage
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Sets the session identifier for distributed session management.
     * 
     * @param sessionId the session ID for Redis-backed session storage
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Gets the request timestamp for audit trail purposes.
     * 
     * @return the timestamp when this request was created
     */
    public Instant getRequestTimestamp() {
        return requestTimestamp;
    }
    
    /**
     * Sets the request timestamp for audit trail purposes.
     * 
     * @param requestTimestamp the timestamp when this request was created
     */
    public void setRequestTimestamp(Instant requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }
    
    /**
     * Returns a string representation of the BaseRequestDto for debugging and logging.
     * 
     * <p>Provides essential request metadata for troubleshooting and audit purposes
     * without exposing sensitive information. Used by logging frameworks and
     * debugging tools across the microservices architecture.</p>
     * 
     * @return string representation including correlation ID, user ID, and timestamp
     */
    @Override
    public String toString() {
        return "BaseRequestDto{" +
                "correlationId='" + correlationId + '\'' +
                ", userId='" + userId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", requestTimestamp=" + requestTimestamp +
                '}';
    }
    
    /**
     * Validates that required fields are populated for request processing.
     * 
     * <p>Called by validation frameworks to ensure request integrity before
     * processing. Verifies that minimum required fields are present and
     * properly formatted for successful request processing.</p>
     * 
     * @return true if the request has minimum required fields, false otherwise
     */
    public boolean isValid() {
        return correlationId != null && !correlationId.trim().isEmpty() &&
               requestTimestamp != null;
    }
    
    /**
     * Initializes request metadata for new request instances.
     * 
     * <p>Utility method for ensuring consistent request initialization
     * across different request creation scenarios. Generates new correlation
     * ID and sets current timestamp if not already populated.</p>
     */
    public void initializeRequestMetadata() {
        if (this.correlationId == null || this.correlationId.trim().isEmpty()) {
            this.correlationId = UUID.randomUUID().toString();
        }
        if (this.requestTimestamp == null) {
            this.requestTimestamp = Instant.now();
        }
    }
}