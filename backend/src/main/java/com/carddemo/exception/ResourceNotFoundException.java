package com.carddemo.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Exception thrown when requested resources are not found in the database,
 * equivalent to VSAM NOTFND condition. Used for missing accounts, cards,
 * customers, or transactions. Includes resource type and identifier for
 * precise error reporting.
 * 
 * Common scenarios:
 * - Account not found by account number
 * - Card not found by card number
 * - Customer not found by customer ID
 * - Transaction not found by transaction ID
 * - User not found by user ID
 * 
 * Maps to HTTP 404 Not Found status code.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Type of resource that was not found (e.g., "Account", "Card", "Customer")
     */
    private final String resourceType;
    
    /**
     * Identifier used to search for the resource
     */
    private final String resourceId;
    
    /**
     * Additional search criteria that were used
     */
    private final Map<String, Object> searchCriteria;
    
    /**
     * Constructs a ResourceNotFoundException with resource type and ID.
     * 
     * @param resourceType the type of resource that was not found
     * @param resourceId the identifier used to search for the resource
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(buildMessage(resourceType, resourceId, null));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.searchCriteria = new HashMap<>();
    }
    
    /**
     * Constructs a ResourceNotFoundException with resource type, ID, and custom message.
     * 
     * @param resourceType the type of resource that was not found
     * @param resourceId the identifier used to search for the resource
     * @param message the detail message
     */
    public ResourceNotFoundException(String resourceType, String resourceId, String message) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.searchCriteria = new HashMap<>();
    }
    
    /**
     * Constructs a ResourceNotFoundException with resource type, ID, message, and cause.
     * 
     * @param resourceType the type of resource that was not found
     * @param resourceId the identifier used to search for the resource
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public ResourceNotFoundException(String resourceType, String resourceId, String message, Throwable cause) {
        super(message, cause);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.searchCriteria = new HashMap<>();
    }
    
    /**
     * Constructs a ResourceNotFoundException with search criteria.
     * 
     * @param resourceType the type of resource that was not found
     * @param resourceId the identifier used to search for the resource
     * @param searchCriteria additional search criteria that were used
     */
    public ResourceNotFoundException(String resourceType, String resourceId, Map<String, Object> searchCriteria) {
        super(buildMessage(resourceType, resourceId, searchCriteria));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.searchCriteria = searchCriteria != null ? new HashMap<>(searchCriteria) : new HashMap<>();
    }
    
    /**
     * Gets the type of resource that was not found.
     * 
     * @return the resource type
     */
    public String getResourceType() {
        return resourceType;
    }
    
    /**
     * Gets the identifier used to search for the resource.
     * 
     * @return the resource identifier
     */
    public String getResourceId() {
        return resourceId;
    }
    
    /**
     * Gets the additional search criteria that were used.
     * 
     * @return map of search criteria
     */
    public Map<String, Object> getSearchCriteria() {
        return new HashMap<>(searchCriteria);
    }
    
    /**
     * Adds search criteria to the exception for detailed error reporting.
     * 
     * @param key the search criteria key
     * @param value the search criteria value
     * @return this exception instance for method chaining
     */
    public ResourceNotFoundException withSearchCriteria(String key, Object value) {
        this.searchCriteria.put(key, value);
        return this;
    }
    
    /**
     * Builds a standardized error message based on resource type, ID, and search criteria.
     * 
     * @param resourceType the type of resource
     * @param resourceId the resource identifier
     * @param searchCriteria additional search criteria
     * @return formatted error message
     */
    private static String buildMessage(String resourceType, String resourceId, Map<String, Object> searchCriteria) {
        StringBuilder sb = new StringBuilder();
        sb.append(resourceType != null ? resourceType : "Resource");
        sb.append(" not found");
        
        if (resourceId != null && !resourceId.isEmpty()) {
            sb.append(" with ID: ").append(resourceId);
        }
        
        if (searchCriteria != null && !searchCriteria.isEmpty()) {
            sb.append(" using criteria: ").append(searchCriteria);
        }
        
        return sb.toString();
    }
    
    @Override
    public String getMessage() {
        // If a custom message was provided, use it; otherwise, build the standard message
        String customMessage = super.getMessage();
        if (customMessage != null && !customMessage.equals(buildMessage(resourceType, resourceId, searchCriteria))) {
            return customMessage;
        }
        return buildMessage(resourceType, resourceId, searchCriteria);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ResourceNotFoundException that = (ResourceNotFoundException) obj;
        return Objects.equals(resourceType, that.resourceType) &&
               Objects.equals(resourceId, that.resourceId) &&
               Objects.equals(searchCriteria, that.searchCriteria);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(resourceType, resourceId, searchCriteria);
    }
}