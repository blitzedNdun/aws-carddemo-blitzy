/*
 * MessageLevel.java
 * 
 * Enumeration for message severity levels in API responses
 * Maps to COBOL message display patterns for BMS color coding
 * 
 * Used throughout the application for:
 * - Informational messages (BMS GREEN color equivalent)
 * - Warning messages (BMS YELLOW color equivalent)  
 * - Error messages (BMS RED color equivalent)
 */
package com.carddemo.dto;

/**
 * MessageLevel enum defines the severity level of messages
 * Maps to COBOL message severity patterns for user interface display
 */
public enum MessageLevel {
    /**
     * Informational messages - equivalent to BMS GREEN color
     * Used for successful operations and status updates
     */
    INFO,
    
    /**
     * Warning messages - equivalent to BMS YELLOW color
     * Used for non-critical issues that need user attention
     */
    WARNING,
    
    /**
     * Error messages - equivalent to BMS RED color
     * Used for validation errors and critical failures
     */
    ERROR
}