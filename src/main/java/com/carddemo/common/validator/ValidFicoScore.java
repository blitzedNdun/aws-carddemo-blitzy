package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for FICO credit score range validation.
 * <p>
 * This annotation validates that FICO credit scores fall within the standard 300-850 range
 * as defined in the CardDemo application functional requirements (F-003-RQ-003).
 * Supports nullable values for customers without established credit history.
 * </p>
 * 
 * <p>
 * <strong>Business Rules:</strong>
 * <ul>
 *   <li>FICO scores must be between 300 and 850 (inclusive)</li>
 *   <li>Null values are permitted for customers without credit history</li>
 *   <li>Zero values are treated as invalid (customers should have null instead)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * <strong>Usage Examples:</strong>
 * <pre>
 * &#64;ValidFicoScore
 * private Integer ficoScore;
 * 
 * &#64;ValidFicoScore(message = "Custom FICO score validation message")
 * private Integer creditScore;
 * </pre>
 * </p>
 * 
 * <p>
 * <strong>COBOL Equivalent:</strong>
 * This annotation replaces the COBOL validation logic for CUST-FICO-CREDIT-SCORE
 * field (PIC 9(03)) found in CUSTREC.cpy and processed in COACTUPC.cbl.
 * </p>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = FicoScoreValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFicoScore {
    
    /**
     * The error message to be displayed when validation fails.
     * <p>
     * Default message follows the business rule format established in the
     * CardDemo application for field validation errors.
     * </p>
     * 
     * @return the validation error message
     */
    String message() default "FICO score must be between 300 and 850, or null for customers without credit history";
    
    /**
     * Validation groups for conditional validation scenarios.
     * <p>
     * Allows different validation rules to be applied based on the
     * business context (e.g., new customer vs. existing customer updates).
     * </p>
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for validation metadata.
     * <p>
     * Can be used to carry additional information about the validation
     * constraint, such as severity level or custom error codes.
     * </p>
     * 
     * @return the validation payload
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum acceptable FICO score value.
     * <p>
     * Based on the standard FICO scoring model range definition.
     * This value should not be modified unless business requirements change.
     * </p>
     * 
     * @return the minimum FICO score (default: 300)
     */
    int min() default 300;
    
    /**
     * Maximum acceptable FICO score value.
     * <p>
     * Based on the standard FICO scoring model range definition.
     * This value should not be modified unless business requirements change.
     * </p>
     * 
     * @return the maximum FICO score (default: 850)
     */
    int max() default 850;
    
    /**
     * Whether to allow null values.
     * <p>
     * When true, null values are considered valid (for customers without
     * established credit history). When false, null values will trigger
     * validation errors.
     * </p>
     * 
     * @return true if null values are allowed (default: true)
     */
    boolean allowNull() default true;
    
    /**
     * Whether to allow zero values.
     * <p>
     * When false, zero values are treated as invalid and should be
     * represented as null instead. This aligns with the business rule
     * that customers without credit history should have null FICO scores.
     * </p>
     * 
     * @return true if zero values are allowed (default: false)
     */
    boolean allowZero() default false;
}