package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementation class for FICO credit score validation. Validates credit scores within 
 * the 300-850 range while supporting nullable values and providing appropriate error 
 * messaging for out-of-range scores.
 * 
 * <p>This validator implements the {@link ValidFicoScore} constraint annotation to
 * provide comprehensive FICO score validation logic equivalent to COBOL credit scoring
 * validation routines found in COACTVWC.cbl and COACTUPC.cbl.</p>
 * 
 * <p><strong>Validation Rules:</strong></p>
 * <ul>
 *   <li>Null values are valid when allowNull is true (default)</li>
 *   <li>Zero values are invalid when allowZero is false (default)</li>
 *   <li>FICO scores must be between min and max values (default 300-850)</li>
 *   <li>Uses ValidationConstants for consistent range validation</li>
 * </ul>
 * 
 * <p><strong>COBOL Equivalent:</strong></p>
 * <p>This validator replaces the validation logic for CUST-FICO-CREDIT-SCORE
 * field (PIC 9(03)) found in CUSTREC.cpy and processed in customer update programs.</p>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 * @see ValidFicoScore
 * @see ValidationConstants#MIN_FICO_SCORE
 * @see ValidationConstants#MAX_FICO_SCORE
 */
public class FicoScoreValidator implements ConstraintValidator<ValidFicoScore, Integer> {

    private int min;
    private int max;
    private boolean allowNull;
    private boolean allowZero;

    /**
     * Initializes the validator with annotation parameters.
     * 
     * @param constraintAnnotation the ValidFicoScore annotation instance
     */
    @Override
    public void initialize(ValidFicoScore constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
        this.allowNull = constraintAnnotation.allowNull();
        this.allowZero = constraintAnnotation.allowZero();
    }

    /**
     * Validates the FICO credit score value according to business rules.
     * 
     * <p>Validation logic:</p>
     * <ol>
     *   <li>Null values: valid if allowNull is true</li>
     *   <li>Zero values: valid only if allowZero is true</li>
     *   <li>Range validation: value must be between min and max (inclusive)</li>
     * </ol>
     * 
     * @param value the FICO score value to validate
     * @param context the validation context for error message customization
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // Handle null values
        if (value == null) {
            return allowNull;
        }

        // Handle zero values
        if (value == 0) {
            if (!allowZero) {
                if (context != null) {
                    buildCustomErrorMessage(context, 
                        "FICO score cannot be zero - use null for customers without credit history");
                }
                return false;
            } else {
                // Zero is allowed, so it's valid regardless of range
                return true;
            }
        }

        // Range validation
        if (value < min || value > max) {
            // For range validation, let the default annotation message be used
            // The annotation can provide custom messages via the message() parameter
            return false;
        }

        return true;
    }

    /**
     * Builds a custom error message for validation failures.
     * 
     * @param context the validation context (must not be null)
     * @param message the custom error message
     */
    private void buildCustomErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}