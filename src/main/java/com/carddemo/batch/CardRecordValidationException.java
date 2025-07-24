package com.carddemo.batch;

/**
 * Custom exception for card record validation failures.
 * 
 * Used to wrap validation errors with specific context for Spring Batch
 * error handling and skip/retry policies.
 */
public class CardRecordValidationException extends Exception {
    
    /**
     * Constructs a new validation exception with the specified message.
     * 
     * @param message the detail message explaining the validation failure
     */
    public CardRecordValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new validation exception with message and cause.
     * 
     * @param message the detail message explaining the validation failure  
     * @param cause the underlying cause of the validation failure
     */
    public CardRecordValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}