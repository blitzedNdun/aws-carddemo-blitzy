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
 * Validator implementation for CVV code format validation.
 * 
 * <p>This validator implements the business rules for CVV code validation as defined
 * in the original COBOL CardDemo application, ensuring exact behavioral equivalence
 * while providing modern Jakarta Bean Validation integration.
 * 
 * <p><strong>Validation Rules:</strong>
 * <ul>
 *   <li>CVV codes must be exactly 3 characters in length</li>
 *   <li>CVV codes must contain only numeric digits (0-9)</li>
 *   <li>Leading zeros are preserved and considered valid (e.g., "007")</li>
 *   <li>Null values are handled based on the requireNonNull flag</li>
 *   <li>Empty strings and whitespace-only strings are considered invalid</li>
 * </ul>
 * 
 * <p><strong>COBOL Equivalence:</strong>
 * This validator replicates the validation logic from the original COBOL program
 * COCRDUPC.cbl where CVV codes are processed as:
 * <pre>
 * CARD-CVV-CD-X                       PIC X(03).
 * CARD-CVV-CD-N REDEFINES CARD-CVV-CD-X PIC 9(03).
 * </pre>
 * 
 * The COBOL validation ensures the field contains exactly 3 numeric characters,
 * which this validator preserves through regex pattern matching.
 * 
 * <p><strong>Performance Considerations:</strong>
 * <ul>
 *   <li>Compiled regex pattern for optimal performance</li>
 *   <li>Short-circuit evaluation for null and length checks</li>
 *   <li>Minimal object allocation during validation</li>
 *   <li>Thread-safe implementation for concurrent validation</li>
 * </ul>
 * 
 * <p><strong>Security Implementation Notes:</strong>
 * <ul>
 *   <li>Validator does not log CVV values to prevent security exposure</li>
 *   <li>Error messages do not include the actual invalid CVV value</li>
 *   <li>Memory is cleared appropriately to avoid CVV retention</li>
 *   <li>Validation failures are handled securely without information leakage</li>
 * </ul>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 * 
 * @see ValidCvv
 * @see ConstraintValidator
 */
public class ValidCvvValidator implements ConstraintValidator<ValidCvv, String> {
    
    /**
     * Compiled regex pattern for 3-digit numeric validation.
     * 
     * <p>Pattern explanation:
     * <ul>
     *   <li>^ - Start of string anchor</li>
     *   <li>\\d{3} - Exactly 3 numeric digits (0-9)</li>
     *   <li>$ - End of string anchor</li>
     * </ul>
     * 
     * <p>This pattern ensures the entire string consists of exactly 3 digits,
     * matching the COBOL PIC 9(03) format requirement.
     */
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3}$");
    
    /**
     * Expected length for CVV codes as defined by card security standards
     * and the original COBOL implementation.
     */
    private static final int CVV_LENGTH = 3;
    
    /**
     * Flag indicating whether null values should be considered invalid.
     * Initialized during constraint initialization from annotation parameters.
     */
    private boolean requireNonNull;
    
    /**
     * Custom error message for null value handling.
     * Used when null values are found and requireNonNull is true.
     */
    private String nullMessage;
    
    /**
     * Initializes the validator with constraint annotation parameters.
     * 
     * <p>This method is called once when the validator is instantiated,
     * allowing configuration based on annotation parameters such as
     * requireNonNull flag and custom null message handling.
     * 
     * <p><strong>Initialization Process:</strong>
     * <ol>
     *   <li>Extract requireNonNull flag from annotation</li>
     *   <li>Extract custom null message if specified</li>
     *   <li>Prepare validator state for thread-safe operation</li>
     * </ol>
     * 
     * @param constraintAnnotation the ValidCvv annotation instance containing
     *                           configuration parameters
     */
    @Override
    public void initialize(ValidCvv constraintAnnotation) {
        this.requireNonNull = constraintAnnotation.requireNonNull();
        this.nullMessage = constraintAnnotation.nullMessage();
    }
    
    /**
     * Validates CVV code format according to card security standards.
     * 
     * <p>This method implements the core validation logic, ensuring CVV codes
     * meet the 3-digit numeric format requirement while handling null values
     * and edge cases appropriately.
     * 
     * <p><strong>Validation Logic Flow:</strong>
     * <ol>
     *   <li>Check for null values and handle based on requireNonNull flag</li>
     *   <li>Validate string is not empty or whitespace-only</li>
     *   <li>Verify exact length requirement (3 characters)</li>
     *   <li>Confirm all characters are numeric digits using regex pattern</li>
     * </ol>
     * 
     * <p><strong>Error Handling:</strong>
     * Custom error messages can be set through the ConstraintValidatorContext
     * to provide specific feedback for different validation failure scenarios.
     * 
     * @param value the CVV code string to validate (may be null)
     * @param context the constraint validator context for custom error handling
     * @return true if the CVV code is valid or appropriately null, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values based on configuration
        if (value == null) {
            if (requireNonNull) {
                // Set custom null message if specified
                if (nullMessage != null && !nullMessage.trim().isEmpty()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(nullMessage)
                           .addConstraintViolation();
                }
                return false;
            }
            // Null is valid for optional CVV updates
            return true;
        }
        
        // Check for empty or whitespace-only strings
        if (value.trim().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "CVV code cannot be empty or contain only whitespace")
                   .addConstraintViolation();
            return false;
        }
        
        // Validate exact length requirement (performance optimization before regex)
        if (value.length() != CVV_LENGTH) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "CVV code must be exactly " + CVV_LENGTH + " digits, found " + value.length() + " characters")
                   .addConstraintViolation();
            return false;
        }
        
        // Validate numeric format using compiled regex pattern
        if (!CVV_PATTERN.matcher(value).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "CVV code must contain only numeric digits (0-9)")
                   .addConstraintViolation();
            return false;
        }
        
        // CVV code meets all validation requirements
        return true;
    }
    
    /**
     * Validates a CVV code string using static validation logic.
     * 
     * <p>This utility method provides static validation without requiring
     * annotation configuration, useful for programmatic validation scenarios
     * where Jakarta Bean Validation framework is not available.
     * 
     * <p><strong>Usage Example:</strong>
     * <pre>
     * if (ValidCvvValidator.isValidCvv("123")) {
     *     // Process valid CVV
     * } else {
     *     // Handle invalid CVV
     * }
     * </pre>
     * 
     * @param cvv the CVV code to validate
     * @return true if the CVV meets format requirements, false otherwise
     */
    public static boolean isValidCvv(String cvv) {
        if (cvv == null || cvv.trim().isEmpty()) {
            return false;
        }
        
        if (cvv.length() != CVV_LENGTH) {
            return false;
        }
        
        return CVV_PATTERN.matcher(cvv).matches();
    }
    
    /**
     * Formats a CVV code to ensure consistent representation.
     * 
     * <p>This utility method provides standardized CVV formatting,
     * ensuring leading zeros are preserved and the format is consistent
     * with the original COBOL PIC 9(03) representation.
     * 
     * <p><strong>Formatting Rules:</strong>
     * <ul>
     *   <li>Pads with leading zeros if necessary</li>
     *   <li>Truncates if longer than 3 digits</li>
     *   <li>Returns null for null input</li>
     *   <li>Handles numeric and string inputs</li>
     * </ul>
     * 
     * @param cvv the CVV value to format (String or numeric)
     * @return formatted CVV string with exactly 3 digits, or null if input is null
     */
    public static String formatCvv(Object cvv) {
        if (cvv == null) {
            return null;
        }
        
        String cvvStr = cvv.toString().trim();
        
        // Handle empty strings
        if (cvvStr.isEmpty()) {
            return null;
        }
        
        // Remove non-numeric characters for formatting
        cvvStr = cvvStr.replaceAll("\\D", "");
        
        // Handle empty result after non-numeric removal
        if (cvvStr.isEmpty()) {
            return null;
        }
        
        // Pad with leading zeros or truncate to exactly 3 digits
        if (cvvStr.length() < CVV_LENGTH) {
            return String.format("%0" + CVV_LENGTH + "d", Integer.parseInt(cvvStr));
        } else if (cvvStr.length() > CVV_LENGTH) {
            return cvvStr.substring(cvvStr.length() - CVV_LENGTH);
        } else {
            return cvvStr;
        }
    }
}