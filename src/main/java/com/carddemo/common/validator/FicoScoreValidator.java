/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.validator;

import com.carddemo.common.validator.ValidationConstants;
import com.carddemo.common.validator.ValidFicoScore;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Jakarta Bean Validation constraint validator for FICO credit score validation.
 * 
 * This validator ensures that FICO credit scores fall within the standard 300-850 range
 * as specified in the CardDemo application functional requirements. The implementation
 * maintains exact compatibility with the original COBOL credit score validation logic
 * from the COACTUPC.cbl program.
 * 
 * <p>FICO Score Validation Rules:</p>
 * <ul>
 *   <li>Valid range: 300-850 (inclusive) - matches industry standard</li>
 *   <li>Null values are permitted for customers without established credit history</li>
 *   <li>Non-null values must be numeric integers within the valid range</li>
 *   <li>Zero values are treated as invalid (not within acceptable range)</li>
 *   <li>Negative values are rejected</li>
 * </ul>
 * 
 * <p>COBOL Compatibility:</p>
 * This validator preserves the exact validation behavior from the original COBOL program
 * COACTUPC.cbl, including the PIC 9(03) field constraints and business rule validation
 * that was defined in the WS-EDIT-FICO-SCORE-FLGS section.
 * 
 * <p>Technical Implementation:</p>
 * <ul>
 *   <li>Integrates with Spring Boot validation framework</li>
 *   <li>Uses ValidationConstants.MIN_FICO_SCORE and MAX_FICO_SCORE for range validation</li>
 *   <li>Supports custom error messages via ConstraintValidatorContext</li>
 *   <li>Handles nullable values according to annotation configuration</li>
 * </ul>
 * 
 * <p>Usage Example:</p>
 * <pre>
 * public class Customer {
 *     {@literal @}ValidFicoScore
 *     private Integer ficoScore;
 *     
 *     // Constructor, getters, and setters
 * }
 * </pre>
 * 
 * <p>Error Messages:</p>
 * The validator provides business-appropriate error messages that guide users
 * toward providing valid FICO scores within the acceptable range.
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since CardDemo v1.0
 */
public class FicoScoreValidator implements ConstraintValidator<ValidFicoScore, Integer> {
    
    /**
     * Minimum allowed FICO score value.
     * Populated from annotation configuration during initialization.
     */
    private int minScore;
    
    /**
     * Maximum allowed FICO score value.
     * Populated from annotation configuration during initialization.
     */
    private int maxScore;
    
    /**
     * Whether null values are allowed.
     * Populated from annotation configuration during initialization.
     */
    private boolean allowNullValues;
    
    /**
     * Custom error message template.
     * Populated from annotation configuration during initialization.
     */
    private String messageTemplate;
    
    /**
     * Initializes the validator with configuration from the ValidFicoScore annotation.
     * 
     * This method is called by the Jakarta Bean Validation framework during
     * constraint validator initialization. It extracts the configuration values
     * from the annotation and prepares the validator for use.
     * 
     * @param constraintAnnotation the ValidFicoScore annotation instance containing
     *                           configuration parameters such as min, max, nullable, 
     *                           and message template
     */
    @Override
    public void initialize(ValidFicoScore constraintAnnotation) {
        // Extract configuration from annotation
        this.minScore = constraintAnnotation.min();
        this.maxScore = constraintAnnotation.max();
        this.allowNullValues = constraintAnnotation.nullable();
        this.messageTemplate = constraintAnnotation.message();
        
        // Validate configuration consistency with ValidationConstants
        if (this.minScore != ValidationConstants.MIN_FICO_SCORE) {
            throw new IllegalArgumentException(
                String.format("Annotation min value (%d) does not match ValidationConstants.MIN_FICO_SCORE (%d)", 
                             this.minScore, ValidationConstants.MIN_FICO_SCORE));
        }
        
        if (this.maxScore != ValidationConstants.MAX_FICO_SCORE) {
            throw new IllegalArgumentException(
                String.format("Annotation max value (%d) does not match ValidationConstants.MAX_FICO_SCORE (%d)", 
                             this.maxScore, ValidationConstants.MAX_FICO_SCORE));
        }
        
        // Validate that min is less than max
        if (this.minScore >= this.maxScore) {
            throw new IllegalArgumentException(
                String.format("Minimum FICO score (%d) must be less than maximum FICO score (%d)", 
                             this.minScore, this.maxScore));
        }
    }
    
    /**
     * Performs the actual FICO score validation.
     * 
     * This method implements the core validation logic that mirrors the original
     * COBOL credit score validation from the COACTUPC.cbl program. It ensures
     * that FICO scores are within the acceptable range while supporting nullable
     * values for customers without established credit history.
     * 
     * <p>Validation Logic:</p>
     * <ol>
     *   <li>Check if null values are allowed and handle null case</li>
     *   <li>Validate that the score is within the acceptable range (300-850)</li>
     *   <li>Provide appropriate error messages for validation failures</li>
     * </ol>
     * 
     * @param value the FICO score value to validate (may be null)
     * @param context the validation context for error message customization
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // Handle null values based on annotation configuration
        if (value == null) {
            // If null values are allowed, validation passes
            if (allowNullValues) {
                return true;
            } else {
                // Null values not allowed - create custom error message
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "FICO credit score is required and cannot be null")
                    .addConstraintViolation();
                return false;
            }
        }
        
        // Additional validation for edge cases - check zero first
        if (value == 0) {
            // Zero is technically within PIC 9(03) range but not a valid FICO score
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("FICO credit score cannot be zero - valid range is %d to %d", 
                             ValidationConstants.MIN_FICO_SCORE, 
                             ValidationConstants.MAX_FICO_SCORE))
                .addConstraintViolation();
            return false;
        }
        
        // Validate that the score is within the acceptable range
        // This matches the COBOL validation logic from COACTUPC.cbl
        if (value < ValidationConstants.MIN_FICO_SCORE || value > ValidationConstants.MAX_FICO_SCORE) {
            // Score is out of range - create detailed error message
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("FICO credit score must be between %d and %d, but was %d", 
                             ValidationConstants.MIN_FICO_SCORE, 
                             ValidationConstants.MAX_FICO_SCORE, 
                             value))
                .addConstraintViolation();
            return false;
        }
        
        // All validation checks passed
        return true;
    }
    
    /**
     * Validates a FICO score value against the standard range without annotation context.
     * 
     * This utility method provides a way to validate FICO scores programmatically
     * without requiring annotation processing. It uses the same validation logic
     * as the main isValid method but with simplified error handling.
     * 
     * @param ficoScore the FICO score to validate
     * @return true if the score is valid (within 300-850 range), false otherwise
     */
    public static boolean isValidFicoScore(Integer ficoScore) {
        if (ficoScore == null) {
            return true; // Default to allowing null values
        }
        
        return ficoScore >= ValidationConstants.MIN_FICO_SCORE 
               && ficoScore <= ValidationConstants.MAX_FICO_SCORE 
               && ficoScore != 0;
    }
    
    /**
     * Provides a human-readable description of the FICO score range.
     * 
     * This method returns a standardized description of the acceptable FICO score
     * range that can be used in user interfaces, documentation, or error messages.
     * 
     * @return a string describing the valid FICO score range
     */
    public static String getFicoScoreRangeDescription() {
        return String.format("FICO credit scores must be between %d and %d", 
                           ValidationConstants.MIN_FICO_SCORE, 
                           ValidationConstants.MAX_FICO_SCORE);
    }
    
    /**
     * Validates and normalizes a FICO score string input.
     * 
     * This utility method handles string-to-integer conversion with validation,
     * providing a convenient way to process FICO score inputs from user interfaces
     * or external systems.
     * 
     * @param ficoScoreString the string representation of a FICO score
     * @return the parsed and validated FICO score, or null if invalid
     * @throws NumberFormatException if the string cannot be parsed as an integer
     */
    public static Integer parseAndValidateFicoScore(String ficoScoreString) {
        if (ficoScoreString == null || ficoScoreString.trim().isEmpty()) {
            return null;
        }
        
        try {
            Integer score = Integer.parseInt(ficoScoreString.trim());
            return isValidFicoScore(score) ? score : null;
        } catch (NumberFormatException e) {
            // Invalid number format
            return null;
        }
    }
}