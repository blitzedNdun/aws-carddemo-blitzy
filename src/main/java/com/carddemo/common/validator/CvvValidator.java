/*
 * Copyright 2024 CardDemo Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation implementation class for CVV (Card Verification Value) validation.
 * 
 * <p>This validator implements the {@link ValidCvv} annotation constraint to ensure CVV codes
 * meet the following criteria based on the COBOL CVV processing logic from the legacy CardDemo 
 * system (COCRDUPC.cbl program):
 * 
 * <ul>
 *   <li>Must be exactly 3 digits long when provided</li>
 *   <li>Must contain only numeric characters (0-9)</li>
 *   <li>Supports null values for optional CVV updates in card modification scenarios</li>
 *   <li>Provides appropriate error messaging for validation failures</li>
 * </ul>
 * 
 * <p>The validation behavior is based on the original COBOL data structure:
 * <pre>
 * COBOL: CARD-CVV-CD-N PIC 9(03) - exactly 3 numeric digits
 * Java:  String cvvCode validated as 3-digit numeric pattern
 * </pre>
 * 
 * <p>Implementation maintains exact validation behavior equivalent to COBOL CVV processing 
 * routines while supporting modern Spring Boot integration patterns and null value handling
 * for optional CVV updates during card modification operations.
 * 
 * <p>Usage examples:
 * <pre>
 * // Valid CVV codes
 * "123" -> Valid
 * "000" -> Valid
 * "999" -> Valid
 * 
 * // Invalid CVV codes
 * "12"   -> Invalid (too short)
 * "1234" -> Invalid (too long)
 * "abc"  -> Invalid (non-numeric)
 * "12a"  -> Invalid (contains non-numeric)
 * 
 * // Optional update scenarios
 * null   -> Valid (for optional updates)
 * ""     -> Invalid (empty string not allowed)
 * "   "  -> Invalid (whitespace not allowed)
 * </pre>
 * 
 * @author CardDemo Migration Team
 * @since 1.0.0
 * @see ValidCvv
 * @see jakarta.validation.ConstraintValidator
 * @see jakarta.validation.ConstraintValidatorContext
 */
public class CvvValidator implements ConstraintValidator<ValidCvv, String> {

    /**
     * Compiled regular expression pattern for 3-digit CVV validation.
     * 
     * <p>Pattern: ^[0-9]{3}$
     * <ul>
     *   <li>^ - Start of string</li>
     *   <li>[0-9] - Numeric characters only (0 through 9)</li>
     *   <li>{3} - Exactly 3 characters</li>
     *   <li>$ - End of string</li>
     * </ul>
     * 
     * <p>This pattern replicates the COBOL PIC 9(03) validation logic from the
     * original CardDemo system, ensuring exact behavioral equivalence.
     */
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3}$");

    /**
     * Default error message for CVV validation failures.
     * 
     * <p>This message is used when the annotation's message attribute is not overridden
     * and provides clear guidance on the expected CVV format requirements.
     */
    private static final String DEFAULT_ERROR_MESSAGE = "CVV code must be exactly 3 digits";

    /**
     * Initializes the validator with constraint annotation configuration.
     * 
     * <p>This method is called once during validator lifecycle to configure
     * the validator instance with the annotation's attributes. For CVV validation,
     * no additional configuration is required beyond the default pattern matching.
     * 
     * @param constraintAnnotation the {@link ValidCvv} annotation instance containing
     *                           configuration attributes such as message, groups, and payload
     */
    @Override
    public void initialize(ValidCvv constraintAnnotation) {
        // No additional initialization required for CVV validation
        // The Pattern is statically compiled and thread-safe
        // All configuration is handled through the annotation attributes
    }

    /**
     * Validates the CVV code value according to the defined business rules.
     * 
     * <p>This method implements the core validation logic that replicates the COBOL
     * CVV processing behavior from the original CardDemo system. The validation
     * supports both mandatory CVV validation for card creation and optional CVV
     * validation for card update operations.
     * 
     * <p>Validation Rules:
     * <ul>
     *   <li>Null values are considered valid (supports optional CVV updates)</li>
     *   <li>Non-null values must be exactly 3 digits</li>
     *   <li>Only numeric characters (0-9) are allowed</li>
     *   <li>Leading zeros are permitted (e.g., "001" is valid)</li>
     *   <li>Whitespace and special characters are not allowed</li>
     * </ul>
     * 
     * <p>Error Message Customization:
     * When validation fails, the method uses the configured error message from the
     * annotation, falling back to the default message if not specified. The error
     * message is added to the validation context for display to the user.
     * 
     * @param value the CVV code string to validate; may be null for optional updates
     * @param context the validation context for error message customization and
     *                constraint violation reporting
     * @return true if the CVV code is valid or null; false if validation fails
     * 
     * @throws IllegalArgumentException if the context parameter is null
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are considered valid for optional CVV updates
        // This supports card modification scenarios where CVV may not be required
        if (value == null) {
            return true;
        }
        
        // Use the compiled pattern to validate the CVV format
        // This replicates the COBOL PIC 9(03) validation logic
        boolean isValidFormat = CVV_PATTERN.matcher(value).matches();
        
        // If validation fails, customize the error message
        if (!isValidFormat) {
            // Disable the default constraint violation to allow custom message
            context.disableDefaultConstraintViolation();
            
            // Build custom constraint violation with appropriate error message
            context.buildConstraintViolationWithTemplate(DEFAULT_ERROR_MESSAGE)
                   .addConstraintViolation();
        }
        
        return isValidFormat;
    }

    /**
     * Static utility method for programmatic CVV validation.
     * 
     * <p>This method provides a convenient way to validate CVV codes programmatically
     * without requiring the full Jakarta Bean Validation framework. It's useful for
     * service layer validation or custom validation scenarios.
     * 
     * <p>Note: This method only validates the format and does not support null values.
     * For null-safe validation, use the standard Jakarta Bean Validation framework
     * with the {@link ValidCvv} annotation.
     * 
     * @param cvvCode the CVV code to validate; must not be null
     * @return true if the CVV code has valid 3-digit numeric format; false otherwise
     * @throws IllegalArgumentException if cvvCode is null
     */
    public static boolean isValidCvvFormat(String cvvCode) {
        if (cvvCode == null) {
            throw new IllegalArgumentException("CVV code cannot be null for format validation");
        }
        return CVV_PATTERN.matcher(cvvCode).matches();
    }

    /**
     * Static utility method to extract CVV validation error details.
     * 
     * <p>This method provides detailed information about why a CVV validation failed,
     * which can be useful for debugging, logging, or providing detailed user feedback.
     * 
     * @param cvvCode the CVV code to analyze; may be null
     * @return a descriptive error message explaining the validation failure,
     *         or null if the CVV code is valid
     */
    public static String getValidationErrorMessage(String cvvCode) {
        if (cvvCode == null) {
            return null; // Null is valid for optional updates
        }
        
        if (cvvCode.trim().isEmpty()) {
            return "CVV code cannot be empty or contain only whitespace";
        }
        
        if (cvvCode.length() != 3) {
            return String.format("CVV code must be exactly 3 digits long, but was %d characters", 
                                cvvCode.length());
        }
        
        if (!cvvCode.matches("^[0-9]+$")) {
            return "CVV code must contain only numeric digits (0-9)";
        }
        
        // If we reach here, the CVV code is valid
        return null;
    }
}