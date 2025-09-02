package com.carddemo.dto;

import com.carddemo.dto.MessageLevel;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO representing informational or warning messages for user feedback.
 * Maps to COBOL message display patterns from BMS screens, replacing traditional
 * ERRMSG and informational message handling patterns in the mainframe application.
 * 
 * This DTO supports multi-level message display and provides structured
 * user feedback across the React frontend and Spring Boot backend components.
 * The message levels (INFO, WARNING, ERROR) align with the original COBOL
 * message classification system while enabling modern web-based user interactions.
 */
@Data
public class MessageDto {
    
    /**
     * Message severity level using the MessageLevel enum.
     * Supports INFO, WARNING, and ERROR levels to provide appropriate
     * visual cues and user experience in the React frontend.
     * Maps directly to COBOL message classification patterns.
     */
    private MessageLevel level;
    
    /**
     * The actual message text to display to the user.
     * This field is mandatory and cannot be blank to ensure
     * meaningful user feedback is always provided.
     * Replaces COBOL ERRMSG and informational message text fields.
     */
    @NotBlank(message = "Message text cannot be blank")
    private String text;
    
    /**
     * Default constructor for MessageDto.
     * Required for JSON deserialization and Spring framework compatibility.
     */
    public MessageDto() {
    }
    
    /**
     * Convenience constructor for creating MessageDto with level and text.
     * Enables quick message creation throughout the application services.
     * 
     * @param level The message severity level (INFO, WARNING, ERROR)
     * @param text The message text to display
     */
    public MessageDto(MessageLevel level, String text) {
        this.level = level;
        this.text = text;
    }
    
    /**
     * Factory method for creating INFO level messages.
     * Provides a convenient way to create informational messages
     * that correspond to successful operations or general information.
     * 
     * @param text The informational message text
     * @return MessageDto with INFO level
     */
    public static MessageDto info(String text) {
        return new MessageDto(MessageLevel.INFO, text);
    }
    
    /**
     * Factory method for creating WARNING level messages.
     * Used for non-critical issues that require user attention
     * but don't prevent operation completion.
     * 
     * @param text The warning message text
     * @return MessageDto with WARNING level
     */
    public static MessageDto warning(String text) {
        return new MessageDto(MessageLevel.WARNING, text);
    }
    
    /**
     * Factory method for creating ERROR level messages.
     * Used for critical issues that prevent operation completion
     * and require immediate user attention or correction.
     * 
     * @param text The error message text
     * @return MessageDto with ERROR level
     */
    public static MessageDto error(String text) {
        return new MessageDto(MessageLevel.ERROR, text);
    }
}