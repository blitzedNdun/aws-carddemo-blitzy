package com.carddemo.dto;

/**
 * Field-level validation error DTO containing field name, error code, error message, 
 * and suggested correction. Used for returning structured validation error information 
 * for detailed field-level error reporting matching BMS field highlighting patterns.
 * 
 * This class provides a standardized way to represent validation errors across the
 * CardDemo application, enabling consistent error reporting and user feedback that
 * mirrors the original COBOL BMS field validation and highlighting behavior.
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class ValidationError {

    /**
     * The name of the field that failed validation
     */
    private String field;

    /**
     * Error code identifying the specific validation rule that failed
     */
    private String code;

    /**
     * Human-readable error message describing the validation failure
     */
    private String message;

    /**
     * Suggested correction or guidance for fixing the validation error
     */
    private String suggestedCorrection;

    /**
     * Default constructor for ValidationError
     */
    public ValidationError() {
    }

    /**
     * Constructor with all required fields
     * 
     * @param field the name of the field that failed validation
     * @param code error code identifying the validation rule
     * @param message human-readable error message
     * @param suggestedCorrection suggested correction for the error
     */
    public ValidationError(String field, String code, String message, String suggestedCorrection) {
        this.field = field;
        this.code = code;
        this.message = message;
        this.suggestedCorrection = suggestedCorrection;
    }

    /**
     * Constructor with required fields (without suggested correction)
     * 
     * @param field the name of the field that failed validation
     * @param code error code identifying the validation rule
     * @param message human-readable error message
     */
    public ValidationError(String field, String code, String message) {
        this(field, code, message, null);
    }

    /**
     * Gets the name of the field that failed validation
     * 
     * @return the field name
     */
    public String getField() {
        return field;
    }

    /**
     * Sets the name of the field that failed validation
     * 
     * @param field the field name
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * Gets the error code identifying the specific validation rule that failed
     * 
     * @return the error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the error code identifying the validation rule
     * 
     * @param code the error code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the human-readable error message describing the validation failure
     * 
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the human-readable error message
     * 
     * @param message the error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the suggested correction or guidance for fixing the validation error
     * 
     * @return the suggested correction
     */
    public String getSuggestedCorrection() {
        return suggestedCorrection;
    }

    /**
     * Sets the suggested correction or guidance
     * 
     * @param suggestedCorrection the suggested correction
     */
    public void setSuggestedCorrection(String suggestedCorrection) {
        this.suggestedCorrection = suggestedCorrection;
    }

    /**
     * Checks if this validation error has a suggested correction
     * 
     * @return true if suggested correction is available, false otherwise
     */
    public boolean hasSuggestedCorrection() {
        return suggestedCorrection != null && !suggestedCorrection.trim().isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationError{");
        sb.append("field='").append(field).append('\'');
        sb.append(", code='").append(code).append('\'');
        sb.append(", message='").append(message).append('\'');
        if (hasSuggestedCorrection()) {
            sb.append(", suggestedCorrection='").append(suggestedCorrection).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValidationError that = (ValidationError) o;

        if (field != null ? !field.equals(that.field) : that.field != null) return false;
        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return suggestedCorrection != null ? suggestedCorrection.equals(that.suggestedCorrection) : that.suggestedCorrection == null;
    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (suggestedCorrection != null ? suggestedCorrection.hashCode() : 0);
        return result;
    }
}