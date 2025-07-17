package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;

/**
 * Jakarta Bean Validation annotation for FICO credit score range validation.
 * 
 * Validates that FICO credit scores fall within the standard 300-850 range as specified
 * in the CardDemo application functional requirements. This annotation supports nullable
 * values to accommodate customers without established credit history.
 * 
 * <p>FICO Score Validation Rules:</p>
 * <ul>
 *   <li>Valid range: 300-850 (inclusive)</li>
 *   <li>Null values are permitted for customers without credit history</li>
 *   <li>Non-null values must be numeric and within the valid range</li>
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
 * <p>Technical Implementation:</p>
 * <ul>
 *   <li>Based on PostgreSQL NUMERIC(3) data type mapping</li>
 *   <li>Integrates with Spring Boot validation framework</li>
 *   <li>Supports both field and method level validation</li>
 *   <li>Provides business-appropriate error messages</li>
 * </ul>
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since CardDemo v1.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = FicoScoreValidator.class)
public @interface ValidFicoScore {
    
    /**
     * Default validation error message.
     * 
     * This message is displayed when FICO score validation fails.
     * The message is designed to be user-friendly and informative,
     * providing clear guidance on the expected range.
     * 
     * @return the error message template
     */
    String message() default "FICO credit score must be between 300 and 850";
    
    /**
     * Validation groups for conditional validation.
     * 
     * Allows grouping of validations for different contexts
     * such as customer creation vs. customer update scenarios.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for extensibility.
     * 
     * Can be used to associate metadata with the validation constraint,
     * such as severity levels or additional context information.
     * 
     * @return the constraint payload
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum allowed FICO score value.
     * 
     * Based on the Fair Isaac Corporation standard FICO score range.
     * This value aligns with industry standards and functional requirements.
     * 
     * @return the minimum valid FICO score (300)
     */
    int min() default 300;
    
    /**
     * Maximum allowed FICO score value.
     * 
     * Based on the Fair Isaac Corporation standard FICO score range.
     * This value aligns with industry standards and functional requirements.
     * 
     * @return the maximum valid FICO score (850)
     */
    int max() default 850;
    
    /**
     * Whether null values are allowed.
     * 
     * When true, null values pass validation, allowing for customers
     * without established credit history. When false, null values
     * will trigger validation failure.
     * 
     * @return true if null values are allowed, false otherwise
     */
    boolean nullable() default true;
}