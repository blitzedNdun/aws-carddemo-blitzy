package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Jakarta Bean Validation constraint validator for FICO credit score validation.
 * 
 * This validator implements the business rules for FICO credit score validation
 * as specified in the CardDemo application functional requirements. It enforces
 * the standard 300-850 range while supporting nullable values for customers
 * without established credit history.
 * 
 * <p>Validation Logic:</p>
 * <ul>
 *   <li>Null values are accepted when nullable=true (default)</li>
 *   <li>Non-null values must be numeric integers</li>
 *   <li>Valid range is 300-850 (inclusive) based on FICO standards</li>
 *   <li>Custom error messages provide specific guidance for different failure scenarios</li>
 * </ul>
 * 
 * <p>Integration Points:</p>
 * <ul>
 *   <li>Spring Boot validation framework integration</li>
 *   <li>PostgreSQL NUMERIC(3) data type compatibility</li>
 *   <li>Customer entity field validation</li>
 *   <li>REST API request validation</li>
 * </ul>
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since CardDemo v1.0
 */
public class ValidFicoScoreValidator implements ConstraintValidator<ValidFicoScore, Integer> {
    
    private static final Logger logger = Logger.getLogger(ValidFicoScoreValidator.class.getName());
    
    /**
     * Minimum allowed FICO score value from annotation configuration.
     */
    private int minScore;
    
    /**
     * Maximum allowed FICO score value from annotation configuration.
     */
    private int maxScore;
    
    /**
     * Whether null values are allowed from annotation configuration.
     */
    private boolean allowNull;
    
    /**
     * Custom error message template from annotation configuration.
     */
    private String messageTemplate;
    
    /**
     * Initializes the validator with constraint annotation parameters.
     * 
     * This method is called by the validation framework during constraint
     * initialization. It extracts configuration parameters from the
     * {@code @ValidFicoScore} annotation instance.
     * 
     * @param constraintAnnotation the {@code @ValidFicoScore} annotation instance
     */
    @Override
    public void initialize(ValidFicoScore constraintAnnotation) {
        this.minScore = constraintAnnotation.min();
        this.maxScore = constraintAnnotation.max();
        this.allowNull = constraintAnnotation.nullable();
        this.messageTemplate = constraintAnnotation.message();
        
        // Validate configuration parameters
        if (minScore < 0 || maxScore < 0 || minScore > maxScore) {
            throw new IllegalArgumentException(
                "Invalid FICO score range configuration: min=" + minScore + ", max=" + maxScore);
        }
        
        logger.log(Level.FINE, "ValidFicoScoreValidator initialized with range [{0}-{1}], nullable={2}", 
                   new Object[]{minScore, maxScore, allowNull});
    }
    
    /**
     * Validates the FICO credit score value against business rules.
     * 
     * This method implements the core validation logic for FICO scores:
     * <ul>
     *   <li>Returns true for null values when nullable=true</li>
     *   <li>Returns false for null values when nullable=false</li>
     *   <li>Validates numeric range for non-null values</li>
     *   <li>Provides specific error messages for different failure scenarios</li>
     * </ul>
     * 
     * @param ficoScore the FICO score value to validate (may be null)
     * @param context the validation context for error message customization
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(Integer ficoScore, ConstraintValidatorContext context) {
        // Handle null values based on configuration
        if (ficoScore == null) {
            if (allowNull) {
                logger.log(Level.FINE, "FICO score validation passed: null value accepted");
                return true;
            } else {
                logger.log(Level.FINE, "FICO score validation failed: null value not allowed");
                buildCustomErrorMessage(context, "FICO credit score is required");
                return false;
            }
        }
        
        // Validate numeric range for non-null values
        if (ficoScore < minScore || ficoScore > maxScore) {
            logger.log(Level.FINE, "FICO score validation failed: value {0} outside range [{1}-{2}]", 
                       new Object[]{ficoScore, minScore, maxScore});
            
            // Provide specific error message based on the type of range violation
            String errorMessage;
            if (ficoScore < minScore) {
                errorMessage = String.format(
                    "FICO credit score %d is below the minimum value of %d", 
                    ficoScore, minScore);
            } else {
                errorMessage = String.format(
                    "FICO credit score %d exceeds the maximum value of %d", 
                    ficoScore, maxScore);
            }
            
            buildCustomErrorMessage(context, errorMessage);
            return false;
        }
        
        logger.log(Level.FINE, "FICO score validation passed: value {0} within valid range", ficoScore);
        return true;
    }
    
    /**
     * Builds a custom error message and disables the default message.
     * 
     * This method provides more specific and user-friendly error messages
     * compared to the default annotation message. It helps users understand
     * exactly what went wrong with their FICO score input.
     * 
     * @param context the validation context
     * @param message the custom error message to display
     */
    private void buildCustomErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}