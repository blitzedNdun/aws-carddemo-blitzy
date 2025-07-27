/**
 * Validator implementation for ValidAccountId annotation
 * 
 * This validator implements the business logic for validating account IDs
 * according to the 11-digit numeric format specified in COBOL CVACT01Y copybook.
 * 
 * Validation Logic:
 * - Checks for null/empty values based on allowEmpty configuration
 * - Validates exactly 11 digit length requirement
 * - Ensures all characters are numeric (0-9)
 * - Supports strict and lenient numeric validation modes
 * 
 * Error handling matches COBOL patterns from COBIL00C program for consistency.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
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
 * Jakarta Bean Validation validator for account ID format validation.
 * 
 * This validator implements the validation logic for the {@link ValidAccountId} annotation,
 * ensuring account IDs conform to the 11-digit numeric format as specified in the
 * COBOL account record structure.
 * 
 * The validator performs the following checks:
 * 1. Null/empty validation based on allowEmpty configuration
 * 2. Length validation (exactly 11 characters)
 * 3. Numeric format validation (digits 0-9 only)
 * 4. Leading/trailing whitespace validation
 * 
 * Thread Safety:
 * This validator is thread-safe and can be used concurrently across multiple validation
 * operations. The Pattern objects are compiled once during initialization and are
 * immutable after creation.
 * 
 * Performance Considerations:
 * - Pre-compiled regex patterns for optimal performance
 * - Early return for null/empty checks to avoid unnecessary processing
 * - Efficient string length checks before pattern matching
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 1.0
 */
public class ValidAccountIdValidator implements ConstraintValidator<ValidAccountId, String> {

    /**
     * Required length for account IDs as specified in COBOL CVACT01Y copybook.
     * ACCT-ID PIC 9(11) defines exactly 11 numeric digits.
     */
    private static final int REQUIRED_ACCOUNT_ID_LENGTH = 11;

    /**
     * Pre-compiled regex pattern for strict numeric validation.
     * Matches exactly 11 consecutive digits with no other characters.
     */
    private static final Pattern STRICT_NUMERIC_PATTERN = Pattern.compile("^\\d{11}$");

    /**
     * Pre-compiled regex pattern for lenient numeric validation.
     * Allows leading/trailing whitespace but requires 11 consecutive digits in the middle.
     */
    private static final Pattern LENIENT_NUMERIC_PATTERN = Pattern.compile("^\\s*\\d{11}\\s*$");

    /**
     * Configuration flag indicating whether empty values are allowed.
     * Set during validator initialization from annotation parameters.
     */
    private boolean allowEmpty;

    /**
     * Configuration flag indicating whether strict numeric validation is required.
     * Set during validator initialization from annotation parameters.
     */
    private boolean strictNumeric;

    /**
     * Error message for empty account ID validation failures.
     * Set during validator initialization from annotation parameters.
     */
    private String emptyMessage;

    /**
     * Error message for format validation failures.
     * Set during validator initialization from annotation parameters.
     */
    private String formatMessage;

    /**
     * Initializes the validator with configuration from the ValidAccountId annotation.
     * 
     * This method is called once by the Jakarta Bean Validation framework when
     * the validator is first created. It extracts configuration parameters from
     * the annotation and stores them for use during validation.
     * 
     * @param constraintAnnotation the ValidAccountId annotation instance containing configuration
     */
    @Override
    public void initialize(ValidAccountId constraintAnnotation) {
        // Extract configuration from annotation
        this.allowEmpty = constraintAnnotation.allowEmpty();
        this.strictNumeric = constraintAnnotation.strictNumeric();
        this.emptyMessage = constraintAnnotation.emptyMessage();
        this.formatMessage = constraintAnnotation.formatMessage();
    }

    /**
     * Validates the account ID value according to the configured rules.
     * 
     * This method implements the core validation logic matching the COBOL account ID
     * validation patterns from the COBIL00C program. It performs comprehensive
     * validation including null checks, length validation, and format validation.
     * 
     * Validation Flow:
     * 1. Check for null/empty values and handle based on allowEmpty configuration
     * 2. Validate exact length requirement (11 characters)
     * 3. Apply numeric format validation (strict or lenient mode)
     * 4. Return validation result with appropriate error messaging
     * 
     * Error Message Strategy:
     * - Uses custom error messages from annotation configuration
     * - Disables default constraint violation messages
     * - Provides specific error messages for different failure modes
     * 
     * @param value the account ID string to validate (may be null)
     * @param context the validation context for error message reporting
     * @return true if the account ID is valid according to all configured rules, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and empty values
        if (value == null || value.trim().isEmpty()) {
            if (allowEmpty) {
                return true; // Empty values are explicitly allowed
            } else {
                // Build custom error message for empty values matching COBOL pattern
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(emptyMessage)
                       .addConstraintViolation();
                return false;
            }
        }

        // Validate account ID format using appropriate pattern
        boolean isValidFormat;
        String valueToValidate = value;

        if (strictNumeric) {
            // Strict mode: No whitespace allowed, exactly 11 digits
            isValidFormat = STRICT_NUMERIC_PATTERN.matcher(value).matches();
        } else {
            // Lenient mode: Allow leading/trailing whitespace
            isValidFormat = LENIENT_NUMERIC_PATTERN.matcher(value).matches();
            valueToValidate = value.trim(); // Use trimmed value for further validation
        }

        // Additional length check for clarity and early detection
        if (valueToValidate.length() != REQUIRED_ACCOUNT_ID_LENGTH) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(formatMessage)
                   .addConstraintViolation();
            return false;
        }

        // Final format validation
        if (!isValidFormat) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(formatMessage)
                   .addConstraintViolation();
            return false;
        }

        // Additional validation to ensure all characters are digits
        // This provides extra safety even though regex should catch this
        for (int i = 0; i < valueToValidate.length(); i++) {
            char ch = valueToValidate.charAt(i);
            if (!Character.isDigit(ch)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(formatMessage)
                       .addConstraintViolation();
                return false;
            }
        }

        return true; // All validations passed
    }

    /**
     * Validates a numeric string for account ID format requirements.
     * 
     * This is a utility method that can be used for programmatic validation
     * outside of the Jakarta Bean Validation framework. It applies the same
     * validation logic as the main isValid method but without the context
     * dependency.
     * 
     * @param accountId the account ID string to validate
     * @return true if the account ID meets the 11-digit numeric format requirement
     */
    public static boolean isValidAccountIdFormat(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }

        String trimmedValue = accountId.trim();
        
        // Check length
        if (trimmedValue.length() != REQUIRED_ACCOUNT_ID_LENGTH) {
            return false;
        }

        // Check if all characters are digits
        return STRICT_NUMERIC_PATTERN.matcher(trimmedValue).matches();
    }

    /**
     * Normalizes an account ID string by removing leading/trailing whitespace.
     * 
     * This utility method provides consistent account ID normalization that can
     * be used throughout the application for data processing and storage.
     * 
     * @param accountId the account ID string to normalize (may be null)
     * @return the normalized account ID string, or null if input was null
     */
    public static String normalizeAccountId(String accountId) {
        if (accountId == null) {
            return null;
        }
        return accountId.trim();
    }

    /**
     * Formats an account ID for display purposes.
     * 
     * This utility method can be used to format account IDs consistently
     * across the application, potentially adding separators or other
     * formatting for improved readability while maintaining the underlying
     * 11-digit format.
     * 
     * Currently returns the account ID as-is, but can be extended to add
     * formatting such as: 12345-67890-1
     * 
     * @param accountId the account ID string to format
     * @return formatted account ID string suitable for display
     */
    public static String formatAccountIdForDisplay(String accountId) {
        if (accountId == null || !isValidAccountIdFormat(accountId)) {
            return accountId; // Return as-is if invalid
        }
        
        // Future enhancement: Could add formatting like 12345-67890-1
        // For now, return the normalized value
        return normalizeAccountId(accountId);
    }
}