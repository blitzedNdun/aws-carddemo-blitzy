/*
 * FicoScoreValidator.java
 *
 * Implementation class for FICO credit score validation within the 300-850 range.
 * Validates credit scores while supporting nullable values and providing appropriate
 * error messaging for out-of-range scores.
 *
 * This validator enforces the business rule validation requirements specified
 * in the CardDemo system's functional requirements which mandate FICO score
 * validation with range 300-850 and numeric range checking, maintaining exact
 * validation behavior equivalent to COBOL credit score processing.
 *
 * The validation logic mirrors the original COBOL implementation from COACTUPC.cbl
 * where FICO scores were stored as PIC 9(03) fields with range validation performed
 * during account updates via the 1275-EDIT-FICO-SCORE paragraph.
 *
 * Key features:
 * - Enforces FICO score range validation within 300-850 boundaries
 * - Supports null value handling for customers without established credit scores
 * - Provides consistent validation behavior matching COBOL credit scoring logic
 * - Integrates comprehensive range checking with business-appropriate error messaging
 *
 * Copyright (c) 2024 CardDemo Application
 */
package com.carddemo.common.validator;

import com.carddemo.common.validator.ValidationConstants;
import com.carddemo.common.validator.ValidFicoScore;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Jakarta Bean Validation implementation class for FICO credit score validation.
 * 
 * This class implements the ConstraintValidator interface to provide validation
 * logic for the ValidFicoScore annotation. It validates that credit scores fall
 * within the standard FICO range of 300-850 while supporting nullable values
 * for customers without established credit history.
 * 
 * The validator implements the business rules established in the original COBOL
 * system where FICO scores were processed as 3-digit numeric fields with explicit
 * range validation. This maintains exact functional equivalence with the original
 * mainframe credit scoring logic.
 * 
 * Validation logic:
 * - Null values are considered valid (supports customers without credit history)
 * - Non-null values must be between MIN_FICO_SCORE and MAX_FICO_SCORE inclusive
 * - Provides clear, business-appropriate error messages with actual values
 * - Supports Integer and Short numeric types for flexible database integration
 * 
 * Usage:
 * This validator is automatically invoked when the @ValidFicoScore annotation
 * is applied to entity fields, DTO properties, or method parameters.
 * 
 * @since CardDemo v1.0
 * @author Blitzy Platform
 */
public class FicoScoreValidator implements ConstraintValidator<ValidFicoScore, Number> {
    
    private int minValue;
    private int maxValue;
    private String messageTemplate;
    
    /**
     * Initialize the validator with annotation parameter values.
     * 
     * This method is called by the Jakarta Bean Validation framework during
     * validator initialization. It extracts configuration parameters from the
     * ValidFicoScore annotation and uses ValidationConstants for consistent
     * range boundaries across the application.
     * 
     * The initialization establishes:
     * - Minimum FICO score boundary (default from ValidationConstants.MIN_FICO_SCORE)
     * - Maximum FICO score boundary (default from ValidationConstants.MAX_FICO_SCORE) 
     * - Custom error message template if specified in annotation
     * 
     * @param constraintAnnotation the ValidFicoScore annotation instance containing validation parameters
     */
    @Override
    public void initialize(ValidFicoScore constraintAnnotation) {
        // Use annotation parameters with ValidationConstants as fallback defaults
        this.minValue = (constraintAnnotation.min() != 300) ? 
            constraintAnnotation.min() : ValidationConstants.MIN_FICO_SCORE;
        this.maxValue = (constraintAnnotation.max() != 850) ? 
            constraintAnnotation.max() : ValidationConstants.MAX_FICO_SCORE;
        this.messageTemplate = constraintAnnotation.message();
    }
    
    /**
     * Validate the FICO credit score value against business rules.
     * 
     * Implements the core validation logic that mirrors the original COBOL
     * FICO score validation from the 1275-EDIT-FICO-SCORE paragraph in COACTUPC.cbl.
     * This ensures exact functional equivalence with the mainframe implementation.
     * 
     * Validation process:
     * 1. Null values are considered valid (supports customers without credit history)
     * 2. Non-null numeric values are converted to integer for range checking
     * 3. Values must fall within the MIN_FICO_SCORE to MAX_FICO_SCORE range (300-850)
     * 4. Type safety is enforced for Integer and Short numeric types
     * 5. Detailed error messages are provided for validation failures
     * 
     * Error handling:
     * - Unsupported number types generate specific type error messages
     * - Out-of-range values generate detailed messages with actual value and expected range
     * - Custom constraint violation messages replace default framework messages
     * 
     * @param value the FICO score value to validate (can be null)
     * @param context the validation context for error reporting and message customization
     * @return true if the value is valid according to FICO business rules, false otherwise
     */
    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        // Null values are valid - supports customers without established credit history
        // This mirrors COBOL logic where FICO score fields could be initialized to spaces/zeros
        if (value == null) {
            return true;
        }
        
        // Convert to integer for range checking with type safety validation
        int ficoScore;
        if (value instanceof Integer) {
            ficoScore = (Integer) value;
        } else if (value instanceof Short) {
            ficoScore = ((Short) value).intValue();
        } else {
            // Unsupported number type - provide helpful error message for developers
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "FICO score must be an Integer or Short value, received: " + value.getClass().getSimpleName())
                .addConstraintViolation();
            return false;
        }
        
        // Validate the FICO score is within the acceptable business range
        // Uses ValidationConstants to ensure consistency with COBOL business rules
        boolean isValidRange = ficoScore >= ValidationConstants.MIN_FICO_SCORE && 
                              ficoScore <= ValidationConstants.MAX_FICO_SCORE;
        
        if (!isValidRange) {
            // Provide specific error message with actual value and expected range
            // This matches the detailed error reporting from the original COBOL system
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("FICO credit score %d is out of range. Valid range is %d-%d", 
                    ficoScore, ValidationConstants.MIN_FICO_SCORE, ValidationConstants.MAX_FICO_SCORE))
                .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}