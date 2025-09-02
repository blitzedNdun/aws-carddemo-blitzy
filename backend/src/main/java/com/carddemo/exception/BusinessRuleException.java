package com.carddemo.exception;

/**
 * Exception for business rule violations that replaces COBOL ABEND routines.
 * Thrown when business logic constraints are violated such as insufficient funds,
 * invalid account status, or transaction limits exceeded. Maintains error codes
 * compatible with legacy CICS ABEND codes.
 * 
 * Common business violations include:
 * - Insufficient balance for transactions
 * - Credit limit exceeded
 * - Invalid card status (expired, blocked, cancelled)
 * - Account restrictions and holds
 * - Daily/monthly transaction limits exceeded
 * - Invalid merchant category restrictions
 * 
 * Maps to CICS ABEND code '9999' for critical business rule errors.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class BusinessRuleException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 4-character error code matching COBOL ABEND-CODE format
     */
    private final String errorCode;
    
    /**
     * Business rule name that was violated
     */
    private final String ruleName;
    
    /**
     * Details about the violation
     */
    private final String violationDetails;
    
    /**
     * Entity affected by the business rule violation
     */
    private final String affectedEntity;
    
    /**
     * Constructs a BusinessRuleException with a message and error code.
     * 
     * @param message the detail message
     * @param errorCode the 4-character COBOL-compatible error code
     */
    public BusinessRuleException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.ruleName = null;
        this.violationDetails = null;
        this.affectedEntity = null;
    }
    
    /**
     * Constructs a BusinessRuleException with comprehensive details.
     * 
     * @param message the detail message
     * @param errorCode the 4-character COBOL-compatible error code
     * @param ruleName the name of the business rule that was violated
     * @param violationDetails specific details about the violation
     * @param affectedEntity the entity affected by the violation
     */
    public BusinessRuleException(String message, String errorCode, String ruleName, 
                                String violationDetails, String affectedEntity) {
        super(message);
        this.errorCode = errorCode;
        this.ruleName = ruleName;
        this.violationDetails = violationDetails;
        this.affectedEntity = affectedEntity;
    }
    
    /**
     * Constructs a BusinessRuleException with a message, error code, and cause.
     * 
     * @param message the detail message
     * @param errorCode the 4-character COBOL-compatible error code
     * @param cause the cause of this exception
     */
    public BusinessRuleException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.ruleName = null;
        this.violationDetails = null;
        this.affectedEntity = null;
    }
    
    /**
     * Gets the 4-character error code compatible with COBOL ABEND-CODE format.
     * 
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the name of the business rule that was violated.
     * 
     * @return the rule name, or null if not specified
     */
    public String getRuleName() {
        return ruleName;
    }
    
    /**
     * Gets specific details about the violation.
     * 
     * @return the violation details, or null if not specified
     */
    public String getViolationDetails() {
        return violationDetails;
    }
    
    /**
     * Gets the entity affected by the business rule violation.
     * 
     * @return the affected entity, or null if not specified
     */
    public String getAffectedEntity() {
        return affectedEntity;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        
        if (errorCode != null) {
            sb.append(" [Error Code: ").append(errorCode).append("]");
        }
        
        if (ruleName != null) {
            sb.append(" [Rule: ").append(ruleName).append("]");
        }
        
        if (violationDetails != null) {
            sb.append(" [Details: ").append(violationDetails).append("]");
        }
        
        if (affectedEntity != null) {
            sb.append(" [Entity: ").append(affectedEntity).append("]");
        }
        
        return sb.toString();
    }
}