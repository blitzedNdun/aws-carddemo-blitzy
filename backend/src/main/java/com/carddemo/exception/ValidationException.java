package com.carddemo.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Exception for field-level validation failures that replicates COBOL edit routine
 * error handling. Supports multiple field errors with detailed messages for each field.
 * Maps to BMS screen field highlighting patterns using error attributes equivalent to DFHRED.
 * 
 * Validation types include:
 * - Date format validation (CCYYMMDD format)
 * - SSN format validation (999-99-9999)
 * - Phone format validation
 * - Numeric range validation (e.g., FICO scores 300-850)
 * - Required field validation
 * - Field length and format validation
 * 
 * Field highlighting flags are equivalent to COBOL FLG-fieldname-NOT-OK patterns.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class ValidationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Map of field names to their corresponding error messages
     */
    private final Map<String, String> fieldErrors;
    
    /**
     * Constructs a ValidationException with a single message.
     * 
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
    }
    
    /**
     * Constructs a ValidationException with a message and field errors.
     * 
     * @param message the detail message
     * @param fieldErrors map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? new HashMap<>(fieldErrors) : new HashMap<>();
    }
    
    /**
     * Constructs a ValidationException with a message, field errors, and cause.
     * 
     * @param message the detail message
     * @param fieldErrors map of field names to error messages
     * @param cause the cause of this exception
     */
    public ValidationException(String message, Map<String, String> fieldErrors, Throwable cause) {
        super(message, cause);
        this.fieldErrors = fieldErrors != null ? new HashMap<>(fieldErrors) : new HashMap<>();
    }
    
    /**
     * Adds a field-level validation error.
     * 
     * @param fieldName the name of the field with the error
     * @param errorMessage the error message for the field
     */
    public void addFieldError(String fieldName, String errorMessage) {
        this.fieldErrors.put(fieldName, errorMessage);
    }
    
    /**
     * Adds multiple field errors from a map.
     * 
     * @param errors map of field names to error messages
     */
    public void addFieldErrors(Map<String, String> errors) {
        if (errors != null) {
            this.fieldErrors.putAll(errors);
        }
    }
    
    /**
     * Gets an unmodifiable view of the field errors.
     * 
     * @return unmodifiable map of field names to error messages
     */
    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }
    
    /**
     * Checks if there are any field-level validation errors.
     * 
     * @return true if there are field errors, false otherwise
     */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
    
    /**
     * Checks if a specific field has a validation error.
     * 
     * @param fieldName the field name to check
     * @return true if the field has an error, false otherwise
     */
    public boolean hasFieldError(String fieldName) {
        return fieldErrors.containsKey(fieldName);
    }
    
    /**
     * Gets the error message for a specific field.
     * 
     * @param fieldName the field name
     * @return the error message for the field, or null if no error exists
     */
    public String getFieldError(String fieldName) {
        return fieldErrors.get(fieldName);
    }
    
    /**
     * Clears all field-level validation errors.
     */
    public void clearFieldErrors() {
        fieldErrors.clear();
    }
    
    /**
     * Gets the number of field-level validation errors.
     * 
     * @return the count of field errors
     */
    public int getFieldErrorCount() {
        return fieldErrors.size();
    }
    
    /**
     * Gets the set of field names that have validation errors.
     * 
     * @return set of field names with errors
     */
    public Set<String> getFieldNames() {
        return Collections.unmodifiableSet(fieldErrors.keySet());
    }
    
    @Override
    public String getMessage() {
        if (!hasFieldErrors()) {
            return super.getMessage();
        }
        
        String baseMessage = super.getMessage();
        String fieldErrorMessage = buildFieldErrorMessage();
        
        if (baseMessage != null && !baseMessage.isEmpty()) {
            return baseMessage + " Field errors: " + fieldErrorMessage;
        } else {
            return "Validation failed. Field errors: " + fieldErrorMessage;
        }
    }
    
    /**
     * Builds a comprehensive error message from all field errors.
     * 
     * @return formatted string containing all field error details
     */
    private String buildFieldErrorMessage() {
        if (fieldErrors.isEmpty()) {
            return "No field errors";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        boolean first = true;
        for (Map.Entry<String, String> entry : fieldErrors.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        
        sb.append("}");
        return sb.toString();
    }
}