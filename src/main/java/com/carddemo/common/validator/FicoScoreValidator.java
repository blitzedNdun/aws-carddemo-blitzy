package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Jakarta Bean Validation implementation for FICO credit score range validation.
 * <p>
 * This validator enforces the standard FICO credit score range of 300-850 as defined
 * in the CardDemo application business rules, while supporting nullable values for
 * customers without established credit history. The implementation maintains exact
 * equivalence with the COBOL credit scoring logic found in COACTUPC.cbl and COACTVWC.cbl.
 * </p>
 * 
 * <p>
 * <strong>Validation Rules:</strong>
 * <ul>
 *   <li>FICO scores must be within the range 300-850 (inclusive)</li>
 *   <li>Null values are permitted by default for customers without credit history</li>
 *   <li>Zero values are rejected by default and should be represented as null</li>
 *   <li>Range boundaries are configurable via annotation parameters</li>
 * </ul>
 * </p>
 * 
 * <p>
 * <strong>COBOL Equivalence:</strong>
 * This validator replaces the COBOL field validation for CUST-FICO-CREDIT-SCORE
 * (PIC 9(03)) from CUSTREC.cpy. The original COBOL programs handled FICO scores
 * as simple numeric moves without explicit range validation, relying on database
 * constraints and business logic validation at the application level.
 * </p>
 * 
 * <p>
 * <strong>Error Handling:</strong>
 * When validation fails, the validator provides business-appropriate error messages
 * that indicate the acceptable range and explain the null value option for customers
 * without credit history.
 * </p>
 * 
 * <p>
 * <strong>Thread Safety:</strong>
 * This validator is stateless and thread-safe. Multiple threads can safely use
 * the same validator instance concurrently.
 * </p>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 * @see ValidFicoScore
 * @see ValidationConstants#MIN_FICO_SCORE
 * @see ValidationConstants#MAX_FICO_SCORE
 */
public class FicoScoreValidator implements ConstraintValidator<ValidFicoScore, Integer> {
    
    /**
     * Minimum acceptable FICO score from annotation configuration.
     * Defaults to ValidationConstants.MIN_FICO_SCORE (300).
     */
    private int minScore;
    
    /**
     * Maximum acceptable FICO score from annotation configuration.
     * Defaults to ValidationConstants.MAX_FICO_SCORE (850).
     */
    private int maxScore;
    
    /**
     * Whether null values are permitted.
     * When true, null values pass validation (for customers without credit history).
     */
    private boolean allowNull;
    
    /**
     * Whether zero values are permitted.
     * When false, zero values are treated as invalid and should be null instead.
     */
    private boolean allowZero;
    
    /**
     * Initializes the validator with constraints from the ValidFicoScore annotation.
     * <p>
     * This method is called once by the Jakarta Bean Validation framework
     * before any validation operations. It extracts the validation parameters
     * from the annotation and stores them for use during validation.
     * </p>
     * 
     * @param constraintAnnotation the ValidFicoScore annotation instance containing
     *                            the validation parameters (min, max, allowNull, allowZero)
     */
    @Override
    public void initialize(ValidFicoScore constraintAnnotation) {
        // Extract configuration from annotation, using ValidationConstants as fallback
        this.minScore = constraintAnnotation.min();
        this.maxScore = constraintAnnotation.max();
        this.allowNull = constraintAnnotation.allowNull();
        this.allowZero = constraintAnnotation.allowZero();
        
        // Validate configuration consistency
        if (minScore > maxScore) {
            throw new IllegalArgumentException(
                String.format("FICO score minimum (%d) cannot be greater than maximum (%d)", 
                            minScore, maxScore));
        }
        
        // Ensure configured values align with ValidationConstants for consistency
        if (minScore != ValidationConstants.MIN_FICO_SCORE) {
            // Log warning about deviation from standard range, but allow it for flexibility
        }
        
        if (maxScore != ValidationConstants.MAX_FICO_SCORE) {
            // Log warning about deviation from standard range, but allow it for flexibility
        }
    }
    
    /**
     * Performs FICO credit score validation according to business rules.
     * <p>
     * This method validates that the provided FICO score meets all configured
     * constraints. The validation logic maintains exact equivalence with the
     * COBOL credit scoring business rules while providing enhanced error reporting.
     * </p>
     * 
     * <p>
     * <strong>Validation Logic:</strong>
     * <ol>
     *   <li>Check null value handling based on allowNull configuration</li>
     *   <li>Check zero value handling based on allowZero configuration</li>
     *   <li>Validate score falls within configured min/max range</li>
     *   <li>Generate appropriate error messages for business users</li>
     * </ol>
     * </p>
     * 
     * @param value the FICO score Integer to validate (may be null)
     * @param context the validation context for error message customization
     * @return true if the value passes validation, false otherwise
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // Handle null values according to configuration
        if (value == null) {
            return allowNull; // Pass validation if nulls are allowed, fail otherwise
        }
        
        // Handle zero values according to business rules
        if (value == 0) {
            if (!allowZero) {
                // Provide business-appropriate error message for zero values
                buildCustomErrorMessage(context, 
                    "FICO score cannot be zero - use null for customers without credit history");
                return false;
            } else {
                // Zero is explicitly allowed, pass validation without range check
                return true;
            }
        }
        
        // Validate range boundaries
        if (value < minScore || value > maxScore) {
            // Create detailed error message indicating acceptable range
            String errorMessage = String.format(
                "FICO score must be between %d and %d (inclusive), received: %d", 
                minScore, maxScore, value);
            
            // Add guidance for null values if applicable
            if (allowNull) {
                errorMessage += " - use null for customers without credit history";
            }
            
            buildCustomErrorMessage(context, errorMessage);
            return false;
        }
        
        // All validation checks passed
        return true;
    }
    
    /**
     * Builds a custom error message and adds it to the validation context.
     * <p>
     * This helper method disables the default constraint violation message
     * and replaces it with a custom message that provides more specific
     * guidance to business users about the validation failure.
     * </p>
     * 
     * @param context the validation context for error message management
     * @param message the custom error message to display
     */
    private void buildCustomErrorMessage(ConstraintValidatorContext context, String message) {
        // Disable the default constraint violation message
        context.disableDefaultConstraintViolation();
        
        // Add the custom error message
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}