/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation implementation for CVV (Card Verification Value) code validation.
 * 
 * <p>This validator implements the business logic for validating CVV codes according to
 * credit card security standards while maintaining compatibility with legacy COBOL
 * CVV processing patterns from COCRDUPC.cbl.</p>
 * 
 * <p>Validation Logic:</p>
 * <ul>
 *   <li>Validates CVV codes are exactly 3 digits</li>
 *   <li>Ensures CVV contains only numeric characters (0-9)</li>
 *   <li>Handles null values based on annotation configuration</li>
 *   <li>Provides context-specific error messages</li>
 * </ul>
 * 
 * <p>COBOL Compatibility:</p>
 * <p>This validator replicates the validation logic found in the legacy COBOL program
 * COCRDUPC.cbl where CVV codes are processed as 3-digit numeric fields using PIC 9(03)
 * format. The validation ensures exact format compatibility while providing modern
 * Jakarta Bean Validation capabilities.</p>
 * 
 * @since CardDemo v2.0
 * @see ValidCvv
 */
public class CvvValidator implements ConstraintValidator<ValidCvv, String> {

    /**
     * Regular expression pattern for validating 3-digit numeric CVV codes.
     * Matches exactly 3 consecutive digits (0-9).
     */
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3}$");

    /**
     * Minimum valid CVV value (000).
     */
    private static final String MIN_CVV = "000";

    /**
     * Maximum valid CVV value (999).
     */
    private static final String MAX_CVV = "999";

    /**
     * Configuration from the @ValidCvv annotation.
     */
    private boolean allowNull;
    private String nullMessage;
    private String formatMessage;

    /**
     * Initializes the validator with configuration from the @ValidCvv annotation.
     * 
     * @param constraintAnnotation the annotation instance containing validation configuration
     */
    @Override
    public void initialize(ValidCvv constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.nullMessage = constraintAnnotation.nullMessage();
        this.formatMessage = constraintAnnotation.formatMessage();
    }

    /**
     * Validates the CVV code according to the configured rules.
     * 
     * <p>Validation Steps:</p>
     * <ol>
     *   <li>Check if value is null and handle according to allowNull configuration</li>
     *   <li>Validate the CVV format (exactly 3 digits)</li>
     *   <li>Ensure all characters are numeric</li>
     *   <li>Verify CVV is within valid range (000-999)</li>
     * </ol>
     * 
     * @param cvvCode the CVV code to validate
     * @param context the constraint validator context for custom error messages
     * @return true if the CVV code is valid, false otherwise
     */
    @Override
    public boolean isValid(String cvvCode, ConstraintValidatorContext context) {
        // Handle null values based on configuration
        if (cvvCode == null) {
            if (allowNull) {
                return true;
            } else {
                // Build custom error message for null values
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(nullMessage)
                       .addConstraintViolation();
                return false;
            }
        }

        // Handle empty or whitespace-only strings
        if (cvvCode.trim().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(formatMessage)
                   .addConstraintViolation();
            return false;
        }

        // Validate CVV format using regex pattern
        if (!CVV_PATTERN.matcher(cvvCode.trim()).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(formatMessage)
                   .addConstraintViolation();
            return false;
        }

        // Additional validation: ensure CVV is within valid numeric range
        // This replicates COBOL PIC 9(03) behavior where values must be 000-999
        try {
            int cvvNumeric = Integer.parseInt(cvvCode.trim());
            if (cvvNumeric < 0 || cvvNumeric > 999) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "CVV must be between 000 and 999")
                       .addConstraintViolation();
                return false;
            }
        } catch (NumberFormatException e) {
            // This should not occur given the regex validation above,
            // but included for defensive programming
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(formatMessage)
                   .addConstraintViolation();
            return false;
        }

        // CVV code passes all validation checks
        return true;
    }

    /**
     * Utility method to check if a CVV code has valid format without full validation context.
     * 
     * <p>This method provides a simple boolean check for CVV format validation
     * without the overhead of constraint validation context setup. Useful for
     * programmatic validation checks in business logic.</p>
     * 
     * @param cvvCode the CVV code to check
     * @return true if the CVV has valid format, false otherwise
     */
    public static boolean hasValidFormat(String cvvCode) {
        if (cvvCode == null || cvvCode.trim().isEmpty()) {
            return false;
        }

        return CVV_PATTERN.matcher(cvvCode.trim()).matches();
    }

    /**
     * Utility method to normalize CVV codes to 3-digit format with leading zeros.
     * 
     * <p>This method ensures CVV codes are properly formatted with leading zeros
     * when necessary, matching the COBOL PIC 9(03) format behavior where numeric
     * values are zero-padded to exactly 3 digits.</p>
     * 
     * @param cvvCode the CVV code to normalize
     * @return normalized 3-digit CVV code, or null if input is invalid
     */
    public static String normalizeCvv(String cvvCode) {
        if (!hasValidFormat(cvvCode)) {
            return null;
        }

        try {
            int cvvNumeric = Integer.parseInt(cvvCode.trim());
            return String.format("%03d", cvvNumeric);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Utility method to validate CVV code format for COBOL compatibility.
     * 
     * <p>This method performs validation that ensures compatibility with the
     * legacy COBOL CVV processing logic found in COCRDUPC.cbl, where CVV codes
     * are handled as PIC 9(03) fields.</p>
     * 
     * @param cvvCode the CVV code to validate
     * @return true if CVV is compatible with COBOL PIC 9(03) format
     */
    public static boolean isCobolCompatible(String cvvCode) {
        return hasValidFormat(cvvCode) && cvvCode.trim().length() == 3;
    }
}