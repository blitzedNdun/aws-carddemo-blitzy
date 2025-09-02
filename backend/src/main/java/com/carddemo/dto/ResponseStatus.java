/*
 * ResponseStatus.java
 * 
 * Enumeration for API response status values
 * Maps to COBOL success/error patterns from CICS transaction processing
 * 
 * Used throughout the application for:
 * - Successful operations (normal CICS completion)
 * - Failed operations (COBOL ABEND or error conditions)
 */
package com.carddemo.dto;

/**
 * ResponseStatus enum defines the overall status of an API response
 * Maps to COBOL success/error patterns from CICS transaction processing
 */
public enum ResponseStatus {
    /**
     * Successful operation - equivalent to normal CICS completion
     * Indicates the request was processed successfully
     */
    SUCCESS,
    
    /**
     * Failed operation - equivalent to COBOL ABEND or error condition
     * Indicates the request encountered an error during processing
     */
    ERROR
}