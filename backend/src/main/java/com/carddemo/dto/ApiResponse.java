/*
 * ApiResponse.java
 * 
 * Generic API response wrapper DTO providing standard response envelope 
 * for all REST endpoints. Replaces CICS SEND MAP response patterns with 
 * JSON structure for the COBOL-to-Java migration.
 * 
 * This class provides:
 * - Standardized response format across all REST APIs
 * - Support for both successful and error responses  
 * - Message collection for informational and error feedback
 * - Session state management through sessionUpdates map
 * - Generic type support for various response data types
 * 
 * Maps COBOL patterns:
 * - COMMAREA session management -> sessionUpdates Map
 * - Message handling from CSMSG01Y -> messages List
 * - CICS SEND MAP patterns -> JSON response structure
 */
package com.carddemo.dto;

import com.carddemo.dto.Message;
import com.carddemo.dto.ResponseStatus;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;



/**
 * Generic API response wrapper providing standardized response envelope
 * for all REST endpoints in the CardDemo application.
 * 
 * Replaces CICS SEND MAP response patterns with modern JSON structure
 * while maintaining functional equivalence with mainframe behavior.
 * 
 * @param <T> Generic type for responseData to support various response types
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * Overall status of the API response (SUCCESS or ERROR)
     * Maps to COBOL program completion status
     */
    private ResponseStatus status;
    
    /**
     * Transaction code identifying the operation performed
     * Maps to CICS transaction codes (CC00, CT00, etc.)
     */
    private String transactionCode;
    
    /**
     * Generic response data containing operation-specific results
     * Type varies based on the specific API endpoint and operation
     */
    private T responseData;
    
    /**
     * List of messages for user feedback (info, warnings, errors)
     * Maps to COBOL message handling patterns from CSMSG01Y
     */
    private List<Message> messages;
    
    /**
     * Session state updates to be applied on the client side
     * Maps to CICS COMMAREA functionality for session management
     * Contains key-value pairs for session attribute updates
     */
    private Map<String, Object> sessionUpdates;
    
    /**
     * Default constructor initializing collections
     */
    public ApiResponse() {
        this.messages = new ArrayList<>();
        this.sessionUpdates = new HashMap<>();
    }
    
    /**
     * Constructor with status and transaction code
     * 
     * @param status Response status (SUCCESS or ERROR)
     * @param transactionCode CICS transaction code equivalent
     */
    public ApiResponse(ResponseStatus status, String transactionCode) {
        this();
        this.status = status;
        this.transactionCode = transactionCode;
    }
    
    /**
     * Constructor with all basic fields
     * 
     * @param status Response status
     * @param transactionCode Transaction identifier
     * @param responseData Generic response payload
     */
    public ApiResponse(ResponseStatus status, String transactionCode, T responseData) {
        this(status, transactionCode);
        this.responseData = responseData;
    }
    
    /**
     * Adds a message to the response message collection
     * Supports multiple messages per response matching COBOL message patterns
     * 
     * @param message Message object to add to the collection
     */
    public void addMessage(Message message) {
        if (message != null) {
            this.messages.add(message);
        }
    }
    
    /**
     * Convenience method to check if response indicates success
     * Maps to COBOL program success condition checking
     * 
     * @return true if status is SUCCESS, false otherwise
     */
    public boolean isSuccess() {
        return ResponseStatus.SUCCESS.equals(this.status);
    }
    
    /**
     * Convenience method to check if response has error conditions
     * Maps to COBOL error condition detection
     * 
     * @return true if status is ERROR, false otherwise
     */
    public boolean hasErrors() {
        return ResponseStatus.ERROR.equals(this.status);
    }
    
    /**
     * Adds a session update key-value pair
     * Maps to CICS COMMAREA field updates for session management
     * 
     * @param key Session attribute name
     * @param value Session attribute value
     */
    public void addSessionUpdate(String key, Object value) {
        if (key != null && value != null) {
            this.sessionUpdates.put(key, value);
        }
    }
    
    /**
     * Sets the status field
     * 
     * @param status ResponseStatus enum value
     */
    public void setStatus(ResponseStatus status) {
        this.status = status;
    }
    
    /**
     * Gets the status field
     * 
     * @return ResponseStatus enum value
     */
    public ResponseStatus getStatus() {
        return this.status;
    }
    
    /**
     * Sets the transaction code
     * 
     * @param transactionCode CICS transaction code equivalent
     */
    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }
    
    /**
     * Gets the transaction code
     * 
     * @return Transaction code string
     */
    public String getTransactionCode() {
        return this.transactionCode;
    }
    
    /**
     * Sets the response data
     * 
     * @param responseData Generic response payload
     */
    public void setResponseData(T responseData) {
        this.responseData = responseData;
    }
    
    /**
     * Gets the response data
     * 
     * @return Generic response payload
     */
    public T getResponseData() {
        return this.responseData;
    }
    
    /**
     * Sets the messages list
     * 
     * @param messages List of Message objects
     */
    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }
    
    /**
     * Gets the messages list
     * 
     * @return List of Message objects
     */
    public List<Message> getMessages() {
        return this.messages;
    }
    
    /**
     * Sets the session updates map
     * 
     * @param sessionUpdates Map of session attribute updates
     */
    public void setSessionUpdates(Map<String, Object> sessionUpdates) {
        this.sessionUpdates = sessionUpdates != null ? sessionUpdates : new HashMap<>();
    }
    
    /**
     * Gets the session updates map
     * 
     * @return Map of session attribute updates
     */
    public Map<String, Object> getSessionUpdates() {
        return this.sessionUpdates;
    }
}