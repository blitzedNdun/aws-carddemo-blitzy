package com.carddemo.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.text.MessageFormat;
import java.util.Objects;

/**
 * Exception for concurrent update conflicts when optimistic locking fails.
 * Equivalent to CICS READ UPDATE/REWRITE conflicts. Thrown when a record
 * has been modified by another transaction between read and update operations.
 * 
 * This exception handles scenarios where:
 * - JPA @Version field conflicts occur
 * - Multiple users attempt to update the same record simultaneously
 * - Optimistic locking detects concurrent modifications
 * - Database version mismatch during updates
 * 
 * Maps to HTTP 409 Conflict status code and provides retry guidance.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class ConcurrencyException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Type of entity that experienced the concurrency conflict
     */
    @JsonProperty("entity_type")
    private final String entityType;
    
    /**
     * Identifier of the specific entity instance
     */
    @JsonProperty("entity_id")
    private final String entityId;
    
    /**
     * Expected version number during the update attempt
     */
    @JsonProperty("expected_version")
    private final Long expectedVersion;
    
    /**
     * Actual version number found in the database
     */
    @JsonProperty("actual_version")
    private final Long actualVersion;
    
    /**
     * Guidance for retry logic
     */
    @JsonProperty("retry_guidance")
    private final String retryGuidance;
    
    /**
     * Constructs a ConcurrencyException with basic entity information.
     * 
     * @param entityType the type of entity that experienced the conflict
     * @param entityId the identifier of the entity instance
     * @param message the detail message
     */
    public ConcurrencyException(String entityType, String entityId, String message) {
        super(message);
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = null;
        this.actualVersion = null;
        this.retryGuidance = "Please refresh the data and try again.";
    }
    
    /**
     * Constructs a ConcurrencyException with version information.
     * 
     * @param entityType the type of entity that experienced the conflict
     * @param entityId the identifier of the entity instance
     * @param expectedVersion the expected version number
     * @param actualVersion the actual version number found
     */
    public ConcurrencyException(String entityType, String entityId, Long expectedVersion, Long actualVersion) {
        super(buildMessage(entityType, entityId, expectedVersion, actualVersion));
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.retryGuidance = "The record has been modified by another user. Please refresh and try again.";
    }
    
    /**
     * Constructs a ConcurrencyException with comprehensive details.
     * 
     * @param entityType the type of entity that experienced the conflict
     * @param entityId the identifier of the entity instance
     * @param expectedVersion the expected version number
     * @param actualVersion the actual version number found
     * @param message the detail message
     * @param retryGuidance specific guidance for retry logic
     */
    public ConcurrencyException(String entityType, String entityId, Long expectedVersion, 
                              Long actualVersion, String message, String retryGuidance) {
        super(message);
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.retryGuidance = retryGuidance != null ? retryGuidance : "Please refresh the data and try again.";
    }
    
    /**
     * Constructs a ConcurrencyException with a cause.
     * 
     * @param entityType the type of entity that experienced the conflict
     * @param entityId the identifier of the entity instance
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public ConcurrencyException(String entityType, String entityId, String message, Throwable cause) {
        super(message, cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = null;
        this.actualVersion = null;
        this.retryGuidance = "Please refresh the data and try again.";
    }
    
    /**
     * Gets the type of entity that experienced the concurrency conflict.
     * 
     * @return the entity type
     */
    public String getEntityType() {
        return entityType;
    }
    
    /**
     * Gets the identifier of the specific entity instance.
     * 
     * @return the entity identifier
     */
    public String getEntityId() {
        return entityId;
    }
    
    /**
     * Gets the expected version number during the update attempt.
     * 
     * @return the expected version, or null if not available
     */
    public Long getExpectedVersion() {
        return expectedVersion;
    }
    
    /**
     * Gets the actual version number found in the database.
     * 
     * @return the actual version, or null if not available
     */
    public Long getActualVersion() {
        return actualVersion;
    }
    
    /**
     * Gets the guidance for retry logic.
     * 
     * @return the retry guidance message
     */
    public String getRetryGuidance() {
        return retryGuidance;
    }
    
    /**
     * Builds a standardized error message for concurrency conflicts.
     * 
     * @param entityType the type of entity
     * @param entityId the entity identifier
     * @param expectedVersion the expected version
     * @param actualVersion the actual version
     * @return formatted error message
     */
    private static String buildMessage(String entityType, String entityId, Long expectedVersion, Long actualVersion) {
        if (expectedVersion != null && actualVersion != null) {
            return MessageFormat.format(
                "Concurrent modification detected for {0} with ID {1}. Expected version {2}, but found version {3}.",
                entityType, entityId, expectedVersion, actualVersion
            );
        } else {
            return MessageFormat.format(
                "Concurrent modification detected for {0} with ID {1}.",
                entityType, entityId
            );
        }
    }
    
    @Override
    public String getMessage() {
        String baseMessage = super.getMessage();
        
        if (baseMessage != null && !baseMessage.isEmpty()) {
            return baseMessage;
        }
        
        return buildMessage(entityType, entityId, expectedVersion, actualVersion);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConcurrencyException that = (ConcurrencyException) obj;
        return Objects.equals(entityType, that.entityType) &&
               Objects.equals(entityId, that.entityId) &&
               Objects.equals(expectedVersion, that.expectedVersion) &&
               Objects.equals(actualVersion, that.actualVersion);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(entityType, entityId, expectedVersion, actualVersion);
    }
}