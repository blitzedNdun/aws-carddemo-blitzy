/*
 * Message.java
 * 
 * Message DTO for user feedback and system messages
 * Replaces COBOL message handling patterns from CSMSG01Y
 * Used in API responses for informational, warning, and error messages
 * 
 * This class provides:
 * - Message level classification (INFO, WARNING, ERROR)
 * - Message text content for user display
 * - Field reference for field-level validation messages
 * - Standardized message structure across all REST APIs
 * 
 * Maps COBOL patterns:
 * - Message field from CSMSG01Y -> text String
 * - Message level classification -> level enum
 * - Field-level error display -> fieldName String
 */
package com.carddemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Message DTO representing user feedback messages in API responses
 * 
 * Replaces COBOL message handling from CSMSG01Y copybook with modern
 * JSON structure while maintaining functional equivalence with 
 * mainframe message display patterns.
 * 
 * Used throughout the application for:
 * - Field-level validation error messages
 * - Business rule violation notifications
 * - System status and informational messages
 * - Success confirmation messages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    
    /**
     * Message severity level for appropriate UI styling and handling
     * Maps to BMS color coding (GREEN=INFO, YELLOW=WARNING, RED=ERROR)
     */
    private MessageLevel level;
    
    /**
     * Message text content for user display
     * Maps to message field from CSMSG01Y copybook
     */
    private String text;
    
    /**
     * Optional field name reference for field-level validation messages
     * Enables client-side highlighting of specific form fields
     * Used for validation error association with input fields
     */
    private String fieldName;
    
    /**
     * Constructor for general messages without field association
     * 
     * @param level Message severity level
     * @param text Message text content
     */
    public Message(MessageLevel level, String text) {
        this.level = level;
        this.text = text;
        this.fieldName = null;
    }
    
    /**
     * Factory method for creating info messages
     * 
     * @param text Message text
     * @return Message object with INFO level
     */
    public static Message info(String text) {
        return new Message(MessageLevel.INFO, text);
    }
    
    /**
     * Factory method for creating warning messages
     * 
     * @param text Message text
     * @return Message object with WARNING level
     */
    public static Message warning(String text) {
        return new Message(MessageLevel.WARNING, text);
    }
    
    /**
     * Factory method for creating error messages
     * 
     * @param text Message text
     * @return Message object with ERROR level
     */
    public static Message error(String text) {
        return new Message(MessageLevel.ERROR, text);
    }
    
    /**
     * Factory method for creating field-specific error messages
     * Used for validation errors that need to highlight specific fields
     * 
     * @param fieldName Name of the field with validation error
     * @param text Error message text
     * @return Message object with ERROR level and field association
     */
    public static Message fieldError(String fieldName, String text) {
        return new Message(MessageLevel.ERROR, text, fieldName);
    }
    
    /**
     * Factory method for creating field-specific warning messages
     * Used for validation warnings that need to highlight specific fields
     * 
     * @param fieldName Name of the field with validation warning
     * @param text Warning message text
     * @return Message object with WARNING level and field association
     */
    public static Message fieldWarning(String fieldName, String text) {
        return new Message(MessageLevel.WARNING, text, fieldName);
    }
    
    /**
     * Checks if this message is an error message
     * 
     * @return true if message level is ERROR
     */
    public boolean isError() {
        return MessageLevel.ERROR.equals(this.level);
    }
    
    /**
     * Checks if this message is a warning message
     * 
     * @return true if message level is WARNING
     */
    public boolean isWarning() {
        return MessageLevel.WARNING.equals(this.level);
    }
    
    /**
     * Checks if this message is an informational message
     * 
     * @return true if message level is INFO
     */
    public boolean isInfo() {
        return MessageLevel.INFO.equals(this.level);
    }
    
    /**
     * Checks if this message is associated with a specific field
     * 
     * @return true if fieldName is not null and not empty
     */
    public boolean hasFieldName() {
        return this.fieldName != null && !this.fieldName.trim().isEmpty();
    }
}