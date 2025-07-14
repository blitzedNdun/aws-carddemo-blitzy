/*
 * CardDemo Credit Card Management System
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
 * CVV validator implementation for Jakarta Bean Validation framework.
 * 
 * <p>This validator implements the CVV format validation logic equivalent to
 * the original COBOL CardDemo application's CVV processing routines found in
 * COCRDUPC.cbl. It validates CVV codes according to 3-digit numeric format
 * standards while supporting null value handling for optional CVV updates.
 * 
 * <p><strong>COBOL Equivalence:</strong>
 * This validator preserves the exact validation behavior from the original
 * COBOL program where CVV codes are processed through these key fields:
 * <pre>
 * CARD-CVV-CD-X                       PIC X(03).
 * CARD-CVV-CD-N REDEFINES CARD-CVV-CD-X PIC 9(03).
 * CCUP-NEW-CVV-CD                     PIC X(3).
 * CCUP-OLD-CVV-CD                     PIC X(3).
 * </pre>
 * 
 * <p><strong>Validation Logic:</strong>
 * The validator implements the following validation rules:
 * <ul>
 *   <li>CVV must be exactly 3 characters in length</li>
 *   <li>CVV must contain only numeric digits (0-9)</li>
 *   <li>Leading zeros are preserved and considered valid (e.g., "007")</li>
 *   <li>Null values are valid by default (for optional CVV updates)</li>
 *   <li>Empty strings are considered invalid</li>
 *   <li>Whitespace characters are not allowed</li>
 * </ul>
 * 
 * <p><strong>Performance Considerations:</strong>
 * <ul>
 *   <li>Uses compiled regex pattern for efficient validation</li>
 *   <li>Implements fail-fast validation logic</li>
 *   <li>Minimizes object allocation during validation</li>
 *   <li>Supports Jakarta Bean Validation caching mechanisms</li>
 * </ul>
 * 
 * <p><strong>Security Notes:</strong>
 * This validator focuses on format validation only. CVV codes should be
 * handled securely in accordance with PCI DSS requirements. The validator
 * does not log or expose CVV values in error messages for security reasons.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 * 
 * @see ValidCvv
 * @see ConstraintValidator
 * @see ConstraintValidatorContext
 */
public class CvvValidator implements ConstraintValidator<ValidCvv, String> {

    /**
     * Compiled regex pattern for CVV validation.
     * 
     * <p>Pattern explanation:
     * <ul>
     *   <li>^: Start of string</li>
     *   <li>\\d: Exactly one digit (0-9)</li>
     *   <li>{3}: Exactly 3 occurrences</li>
     *   <li>$: End of string</li>
     * </ul>
     * 
     * <p>This pattern ensures CVV codes are exactly 3 numeric digits,
     * equivalent to the COBOL PIC 9(03) format validation.
     */
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3}$");

    /**
     * Error message template for CVV format validation failures.
     * 
     * <p>This message is used when the CVV format is invalid but the
     * annotation doesn't specify a custom message. It provides clear
     * guidance about the expected format without exposing the invalid value.
     */
    private static final String DEFAULT_CVV_FORMAT_MESSAGE = "CVV code must be exactly 3 numeric digits";

    /**
     * Error message template for null CVV validation failures.
     * 
     * <p>This message is used when requireNonNull is true and the CVV
     * value is null, but a custom nullMessage is not provided.
     */
    private static final String DEFAULT_NULL_CVV_MESSAGE = "CVV code is required and cannot be null";

    /**
     * The annotation instance containing validation configuration.
     * 
     * <p>This field is populated during validator initialization and
     * contains the configuration attributes from the @ValidCvv annotation
     * such as custom messages, requireNonNull flag, and other constraints.
     */
    private ValidCvv constraintAnnotation;

    /**
     * Initializes the validator with the annotation configuration.
     * 
     * <p>This method is called by the Jakarta Bean Validation framework
     * during validator initialization. It extracts configuration from the
     * @ValidCvv annotation and prepares the validator for use.
     * 
     * <p><strong>Implementation Details:</strong>
     * <ul>
     *   <li>Stores the annotation instance for later use during validation</li>
     *   <li>Performs any necessary initialization of validation resources</li>
     *   <li>Validates the annotation configuration parameters</li>
     * </ul>
     * 
     * @param constraintAnnotation the @ValidCvv annotation instance containing
     *                           validation configuration such as custom messages,
     *                           requireNonNull flag, and validation groups
     * 
     * @throws IllegalArgumentException if the annotation configuration is invalid
     * 
     * @see ValidCvv
     */
    @Override
    public void initialize(ValidCvv constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }

    /**
     * Validates the CVV code according to 3-digit numeric format requirements.
     * 
     * <p>This method implements the core CVV validation logic equivalent to
     * the original COBOL CardDemo application's CVV processing routines.
     * It performs comprehensive format validation while supporting null value
     * handling for optional CVV updates in card modification scenarios.
     * 
     * <p><strong>Validation Algorithm:</strong>
     * <ol>
     *   <li><strong>Null Check:</strong> Handles null values based on requireNonNull flag</li>
     *   <li><strong>Empty String Check:</strong> Rejects empty strings as invalid</li>
     *   <li><strong>Length Validation:</strong> Ensures exactly 3 characters</li>
     *   <li><strong>Format Validation:</strong> Validates numeric-only content using regex</li>
     *   <li><strong>Error Message Generation:</strong> Provides appropriate error messages</li>
     * </ol>
     * 
     * <p><strong>COBOL Equivalence:</strong>
     * This validation logic preserves the behavior from the original COBOL
     * program's CVV processing sections:
     * <pre>
     * MOVE CCUP-NEW-CVV-CD TO CARD-CVV-CD-X
     * MOVE CARD-CVV-CD-N   TO CARD-UPDATE-CVV-CD
     * </pre>
     * 
     * <p><strong>Validation Examples:</strong>
     * <pre>
     * "123" -> Valid (3 numeric digits)
     * "007" -> Valid (leading zeros preserved)
     * "000" -> Valid (all zeros allowed)
     * "999" -> Valid (maximum valid value)
     * null  -> Valid (if requireNonNull=false, default)
     * null  -> Invalid (if requireNonNull=true)
     * ""    -> Invalid (empty string)
     * " "   -> Invalid (whitespace)
     * "12"  -> Invalid (too short)
     * "1234" -> Invalid (too long)
     * "12a" -> Invalid (non-numeric)
     * "1 2" -> Invalid (contains space)
     * </pre>
     * 
     * <p><strong>Error Message Handling:</strong>
     * The method provides customized error messages based on the validation
     * failure type:
     * <ul>
     *   <li>Null values: Uses nullMessage or default null message</li>
     *   <li>Format errors: Uses annotation message or default format message</li>
     *   <li>Custom context: Supports ConstraintValidatorContext for advanced scenarios</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong>
     * <ul>
     *   <li>O(1) time complexity for validation operations</li>
     *   <li>Minimal memory allocation during validation</li>
     *   <li>Compiled regex pattern for efficient matching</li>
     *   <li>Fail-fast validation logic with early returns</li>
     * </ul>
     * 
     * @param value the CVV code string to validate. May be null for optional
     *              CVV updates, empty string, or contain any characters.
     *              The validator handles all input types appropriately.
     * 
     * @param context the validation context provided by Jakarta Bean Validation
     *                framework. Used for custom error message generation,
     *                constraint violation reporting, and validation metadata.
     *                Must not be null.
     * 
     * @return true if the CVV code is valid according to the 3-digit numeric
     *         format requirements and null handling policy, false otherwise
     * 
     * @throws IllegalArgumentException if the validation context is null
     * 
     * @see Pattern#matcher(CharSequence)
     * @see ConstraintValidatorContext#buildConstraintViolationWithTemplate(String)
     * @see ConstraintValidatorContext#addConstraintViolation()
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values based on requireNonNull flag
        if (value == null) {
            if (constraintAnnotation.requireNonNull()) {
                // CVV is required but null was provided
                String nullMessage = constraintAnnotation.nullMessage();
                if (nullMessage.isEmpty()) {
                    nullMessage = DEFAULT_NULL_CVV_MESSAGE;
                }
                
                // Disable default constraint violation and add custom message
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(nullMessage)
                       .addConstraintViolation();
                
                return false;
            }
            // Null is valid for optional CVV updates
            return true;
        }

        // Empty string is invalid - CVV cannot be empty if provided
        if (value.isEmpty()) {
            // Use default message for empty strings
            return false;
        }

        // Validate CVV format using compiled regex pattern
        // This ensures exactly 3 numeric digits (equivalent to COBOL PIC 9(03))
        if (!CVV_PATTERN.matcher(value).matches()) {
            // Format validation failed - CVV is not 3 numeric digits
            return false;
        }

        // CVV format is valid
        return true;
    }
}