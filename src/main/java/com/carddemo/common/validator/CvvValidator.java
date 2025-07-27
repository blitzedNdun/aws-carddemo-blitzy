/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation constraint validator for CVV (Card Verification Value) codes.
 * 
 * <p>This validator implements CVV format validation with 3-digit numeric pattern matching,
 * maintaining exact compatibility with legacy COBOL CVV processing routines found in
 * COCRDUPC.cbl where CVV codes are defined as PIC 9(03) format.</p>
 * 
 * <p>Validation Logic:</p>
 * <ul>
 *   <li>CVV must be exactly 3 digits (000-999)</li>
 *   <li>CVV must contain only numeric characters (0-9)</li>
 *   <li>Leading zeros are allowed and preserved (e.g., "001", "020")</li>
 *   <li>Null value handling is configurable via @ValidCvv annotation allowNull property</li>
 *   <li>Custom error messages are supported for different validation failure scenarios</li>
 * </ul>
 * 
 * <p>Legacy COBOL Compatibility:</p>
 * <p>This implementation preserves the exact validation behavior from the original COBOL
 * card update program (COCRDUPC.cbl) where CVV codes are processed as 3-digit numeric
 * fields using COBOL PIC 9(03) format. The validation maintains identical acceptance
 * criteria and error handling patterns to ensure functional equivalence during the
 * mainframe-to-Java migration.</p>
 * 
 * <p>Error Message Customization:</p>
 * <p>The validator supports custom error messages through the ValidCvv annotation
 * properties including formatMessage for format violations and nullMessage for
 * null value violations when null values are not allowed.</p>
 * 
 * <p>Thread Safety:</p>
 * <p>This validator is stateless and thread-safe, suitable for use in concurrent
 * Spring Boot applications where multiple validation requests may be processed
 * simultaneously.</p>
 * 
 * @see ValidCvv
 * @since CardDemo v2.0
 */
public class CvvValidator implements ConstraintValidator<ValidCvv, String> {

    /**
     * Pre-compiled regex pattern for 3-digit CVV validation.
     * Pattern matches exactly 3 digits (000-999) with no additional characters.
     * Using pre-compilation for optimal performance in high-volume validation scenarios.
     */
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3}$");

    /**
     * Configuration flag indicating whether null values should be treated as valid.
     * Extracted from the ValidCvv annotation during initialization.
     */
    private boolean allowNull;

    /**
     * Custom error message for null value violations.
     * Used when allowNull is false and a null value is encountered.
     */
    private String nullMessage;

    /**
     * Custom error message for format violations.
     * Used when the CVV value doesn't match the required 3-digit numeric pattern.
     */
    private String formatMessage;

    /**
     * Initializes the validator with configuration from the ValidCvv annotation.
     * 
     * <p>This method is called once during validator setup and extracts the
     * configuration parameters from the annotation to control validation behavior:</p>
     * <ul>
     *   <li>allowNull - determines if null values are acceptable</li>
     *   <li>nullMessage - custom message for null value violations</li>
     *   <li>formatMessage - custom message for format violations</li>
     * </ul>
     * 
     * <p>The configuration follows the COBOL card update processing patterns where
     * CVV updates are optional in certain scenarios but must be valid when provided.</p>
     * 
     * @param constraintAnnotation the ValidCvv annotation instance containing configuration
     */
    @Override
    public void initialize(ValidCvv constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.nullMessage = constraintAnnotation.nullMessage();
        this.formatMessage = constraintAnnotation.formatMessage();
    }

    /**
     * Validates the CVV code according to 3-digit numeric format requirements.
     * 
     * <p>Validation Process:</p>
     * <ol>
     *   <li>Check for null values and apply allowNull configuration</li>
     *   <li>Check for empty/blank strings and treat as format violations</li>
     *   <li>Validate against 3-digit numeric pattern using compiled regex</li>
     *   <li>Generate appropriate error messages for different failure scenarios</li>
     * </ol>
     * 
     * <p>COBOL Equivalence:</p>
     * <p>This validation logic maintains exact compatibility with the COBOL CVV
     * processing in COCRDUPC.cbl where CVV codes are validated as PIC 9(03) fields.
     * The validation accepts values from "000" to "999" inclusive, preserving
     * leading zeros as required by card industry standards.</p>
     * 
     * <p>Error Message Handling:</p>
     * <p>Custom validation messages are built using the ConstraintValidatorContext
     * to provide clear, actionable feedback to users. Messages are customizable
     * through the ValidCvv annotation properties to support internationalization
     * and business-specific messaging requirements.</p>
     * 
     * @param cvvValue the CVV code value to validate (may be null)
     * @param context the validation context for building custom error messages
     * @return true if the CVV is valid according to all configured rules, false otherwise
     */
    @Override
    public boolean isValid(String cvvValue, ConstraintValidatorContext context) {
        // Handle null values based on allowNull configuration
        // This supports optional CVV updates in card modification scenarios
        // as indicated in the COBOL source code patterns
        if (cvvValue == null) {
            if (allowNull) {
                return true;  // Null is explicitly allowed for optional updates
            } else {
                // Build custom constraint violation for null values
                buildCustomViolation(context, nullMessage);
                return false;
            }
        }

        // Handle empty and blank strings as format violations
        // Empty strings are not valid CVV codes even if null values are allowed
        if (cvvValue.trim().isEmpty()) {
            buildCustomViolation(context, formatMessage);
            return false;
        }

        // Validate against 3-digit numeric pattern
        // This maintains exact compatibility with COBOL PIC 9(03) format
        // where CVV must be exactly 3 digits with leading zeros preserved
        if (!CVV_PATTERN.matcher(cvvValue).matches()) {
            buildCustomViolation(context, formatMessage);
            return false;
        }

        // CVV passes all validation rules
        return true;
    }

    /**
     * Builds a custom constraint violation with the specified error message.
     * 
     * <p>This method replaces the default validation message with a custom message
     * appropriate for the specific validation failure scenario. It disables the
     * default constraint violation and creates a new one with the custom message.</p>
     * 
     * <p>Message Customization:</p>
     * <p>Custom messages support business-specific error reporting and can be
     * internationalized through standard Jakarta Bean Validation message
     * interpolation mechanisms. Messages provide clear guidance to users
     * about the specific validation requirements.</p>
     * 
     * @param context the validation context for building the violation
     * @param message the custom error message to display
     */
    private void buildCustomViolation(ConstraintValidatorContext context, String message) {
        // Disable the default constraint violation to use custom message
        context.disableDefaultConstraintViolation();
        
        // Build and add the custom constraint violation with specified message
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}