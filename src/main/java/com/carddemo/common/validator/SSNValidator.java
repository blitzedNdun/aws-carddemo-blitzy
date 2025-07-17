/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

import static com.carddemo.common.validator.ValidationConstants.SSN_PATTERN;

/**
 * Jakarta Bean Validation implementation for Social Security Number validation logic.
 * 
 * This validator provides comprehensive SSN format validation maintaining exact
 * behavioral compatibility with COBOL customer data processing patterns from 
 * COACTUPC.cbl while integrating seamlessly with Spring Boot validation framework.
 * 
 * <p><strong>COBOL Equivalency:</strong></p>
 * This implementation preserves the exact SSN validation logic from the COBOL
 * programs COACTUPC.cbl (lines 117-146) which defines structured validation of:
 * <ul>
 *   <li>WS-EDIT-US-SSN-PART1 PIC X(3) - Area number validation</li>
 *   <li>WS-EDIT-US-SSN-PART2 PIC X(2) - Group number validation</li> 
 *   <li>WS-EDIT-US-SSN-PART3 PIC X(4) - Serial number validation</li>
 *   <li>88-level condition INVALID-SSN-PART1 for specific forbidden values</li>
 * </ul>
 * 
 * <p><strong>Validation Rules (Social Security Administration Standards):</strong></p>
 * <ul>
 *   <li><strong>Format:</strong> Must be exactly 9 digits (CUSTREC.cpy CUST-SSN PIC 9(09))</li>
 *   <li><strong>Area Number (first 3 digits):</strong> Cannot be 000, 666, or 900-999</li>
 *   <li><strong>Group Number (middle 2 digits):</strong> Must be 01-99 (cannot be 00)</li>
 *   <li><strong>Serial Number (last 4 digits):</strong> Must be 0001-9999 (cannot be 0000)</li>
 *   <li><strong>Input Normalization:</strong> Strips hyphens, spaces, and formatting characters</li>
 * </ul>
 * 
 * <p><strong>Supported Input Formats:</strong></p>
 * <ul>
 *   <li>Formatted: XXX-XX-XXXX (e.g., "123-45-6789")</li>
 *   <li>Unformatted: XXXXXXXXX (e.g., "123456789")</li>
 *   <li>Partial formatting with spaces: "123 45 6789"</li>
 *   <li>Mixed formatting: "123-45 6789"</li>
 * </ul>
 * 
 * <p><strong>Error Message Integration:</strong></p>
 * The validator provides specific error messages matching COBOL validation patterns
 * for seamless integration with Spring Boot's BindingResult and FieldError framework,
 * supporting both field-level validation display and programmatic error handling.
 * 
 * <p><strong>Performance Characteristics:</strong></p>
 * <ul>
 *   <li>Pre-compiled regex patterns for optimal validation performance</li>
 *   <li>Minimal string manipulation with efficient normalization</li>
 *   <li>Early termination for invalid formats to reduce processing overhead</li>
 *   <li>Thread-safe implementation suitable for concurrent Spring Boot applications</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * // Basic entity field validation
 * public class Customer {
 *     &#64;ValidSSN
 *     private String socialSecurityNumber;
 * }
 * 
 * // Custom validation with specific error message
 * public class EmployeeRecord {
 *     &#64;ValidSSN(message = "Employee SSN must be valid for tax reporting")
 *     private String employeeSSN;
 * }
 * 
 * // Strict validation with custom part-specific messages
 * public class TaxForm {
 *     &#64;ValidSSN(strict = true, 
 *               part1ErrorMessage = "SSN area number invalid for tax purposes")
 *     private String taxPayerSSN;
 * }
 * 
 * // Allow blank for optional fields
 * public class OptionalContact {
 *     &#64;ValidSSN(allowBlank = true)
 *     private String emergencyContactSSN;
 * }
 * </pre>
 * 
 * @author Blitzy Agent
 * @since 1.0
 * @see ValidSSN
 * @see ValidationConstants#SSN_PATTERN
 */
public class SSNValidator implements ConstraintValidator<ValidSSN, String> {
    
    /**
     * Pre-compiled pattern for formatted SSN input (XXX-XX-XXXX).
     * Matches the common hyphenated format used in forms and documents.
     */
    private static final Pattern FORMATTED_SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");
    
    /**
     * Pre-compiled pattern for input normalization.
     * Removes all non-digit characters (hyphens, spaces, parentheses, etc.)
     * to create clean numeric string for validation.
     */
    private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("[^0-9]");
    
    /**
     * Configuration flags from the ValidSSN annotation.
     * Initialized during constraint validator initialization.
     */
    private boolean allowBlank;
    private boolean strict;
    private String part1ErrorMessage;
    private String part2ErrorMessage; 
    private String part3ErrorMessage;
    private String formatErrorMessage;
    
    /**
     * Initializes the validator with configuration from the ValidSSN annotation.
     * 
     * This method is called once by the Jakarta Bean Validation framework
     * when the validator is first instantiated for a specific constraint usage.
     * 
     * @param constraintAnnotation the ValidSSN annotation instance containing
     *                           configuration parameters for validation behavior
     */
    @Override
    public void initialize(ValidSSN constraintAnnotation) {
        this.allowBlank = constraintAnnotation.allowBlank();
        this.strict = constraintAnnotation.strict();
        this.part1ErrorMessage = constraintAnnotation.part1ErrorMessage();
        this.part2ErrorMessage = constraintAnnotation.part2ErrorMessage();
        this.part3ErrorMessage = constraintAnnotation.part3ErrorMessage();
        this.formatErrorMessage = constraintAnnotation.formatErrorMessage();
    }
    
    /**
     * Validates the provided SSN value according to configured validation rules.
     * 
     * This method implements the core validation logic equivalent to the COBOL
     * SSN validation routines from COACTUPC.cbl, providing exact behavioral
     * compatibility while supporting modern Spring Boot validation patterns.
     * 
     * <p><strong>Validation Sequence:</strong></p>
     * <ol>
     *   <li>Null/blank validation based on allowBlank flag</li>
     *   <li>Input normalization (remove formatting characters)</li>
     *   <li>Basic format validation (9-digit numeric pattern)</li>
     *   <li>Strict validation rules (if enabled):
     *       <ul>
     *         <li>Area number validation (000, 666, 900-999 forbidden)</li>
     *         <li>Group number validation (00 forbidden)</li>
     *         <li>Serial number validation (0000 forbidden)</li>
     *       </ul>
     *   </li>
     * </ol>
     * 
     * <p><strong>Error Message Strategy:</strong></p>
     * When validation fails, this method adds specific constraint violations
     * to the validation context with detailed error messages that match the
     * original COBOL validation message patterns for consistency.
     * 
     * @param value the SSN string value to validate (may be null or formatted)
     * @param context the constraint validator context for adding violation messages
     * @return true if the SSN is valid according to all configured rules, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and blank values according to allowBlank configuration
        if (value == null || value.trim().isEmpty()) {
            return allowBlank;
        }
        
        // Normalize input by removing all non-digit characters
        // This handles formatted input like "123-45-6789", "123 45 6789", etc.
        String normalizedSSN = normalizeSSNInput(value);
        
        // Validate basic 9-digit numeric format using ValidationConstants pattern
        if (!SSN_PATTERN.matcher(normalizedSSN).matches()) {
            // Add custom constraint violation with format-specific error message
            addConstraintViolation(context, formatErrorMessage);
            return false;
        }
        
        // Apply strict validation rules if enabled (matches COBOL validation logic)
        if (strict) {
            return validateStrictSSNRules(normalizedSSN, context);
        }
        
        // Basic validation passed
        return true;
    }
    
    /**
     * Normalizes SSN input by removing all non-digit characters.
     * 
     * This method provides equivalent functionality to COBOL input processing
     * routines that clean user input before validation, supporting various
     * common SSN format patterns while producing clean numeric strings.
     * 
     * <p><strong>Supported Input Transformations:</strong></p>
     * <ul>
     *   <li>"123-45-6789" → "123456789"</li>
     *   <li>"123 45 6789" → "123456789"</li>
     *   <li>"123.45.6789" → "123456789"</li>
     *   <li>"(123)45-6789" → "123456789" (handles phone-like formats)</li>
     *   <li>"123456789" → "123456789" (already clean)</li>
     * </ul>
     * 
     * @param input the raw SSN input string potentially containing formatting characters
     * @return normalized SSN string containing only digit characters
     */
    private String normalizeSSNInput(String input) {
        // Use pre-compiled pattern for efficient character removal
        return NON_DIGIT_PATTERN.matcher(input.trim()).replaceAll("");
    }
    
    /**
     * Applies strict SSN validation rules based on Social Security Administration standards.
     * 
     * This method implements the detailed validation logic equivalent to the COBOL
     * program COACTUPC.cbl validation routines, ensuring exact behavioral compatibility
     * with the original mainframe application while providing comprehensive error messaging.
     * 
     * <p><strong>COBOL Mapping:</strong></p>
     * <ul>
     *   <li>Area Number → WS-EDIT-US-SSN-PART1 (PIC X(3)) validation</li>
     *   <li>Group Number → WS-EDIT-US-SSN-PART2 (PIC X(2)) validation</li>
     *   <li>Serial Number → WS-EDIT-US-SSN-PART3 (PIC X(4)) validation</li>
     *   <li>Invalid Values → 88-level condition INVALID-SSN-PART1 logic</li>
     * </ul>
     * 
     * @param normalizedSSN the 9-digit numeric SSN string to validate
     * @param context the constraint validator context for adding specific violation messages
     * @return true if all strict validation rules pass, false if any rule fails
     */
    private boolean validateStrictSSNRules(String normalizedSSN, ConstraintValidatorContext context) {
        // Extract SSN components (equivalent to COBOL REDEFINES structure)
        String areNumber = normalizedSSN.substring(0, 3);     // WS-EDIT-US-SSN-PART1
        String groupNumber = normalizedSSN.substring(3, 5);   // WS-EDIT-US-SSN-PART2  
        String serialNumber = normalizedSSN.substring(5, 9);  // WS-EDIT-US-SSN-PART3
        
        // Validate area number (first 3 digits) - matches COBOL INVALID-SSN-PART1 condition
        if (!isValidAreaNumber(areNumber)) {
            addConstraintViolation(context, part1ErrorMessage);
            return false;
        }
        
        // Validate group number (middle 2 digits) - cannot be 00
        if (!isValidGroupNumber(groupNumber)) {
            addConstraintViolation(context, part2ErrorMessage);
            return false;
        }
        
        // Validate serial number (last 4 digits) - cannot be 0000
        if (!isValidSerialNumber(serialNumber)) {
            addConstraintViolation(context, part3ErrorMessage);
            return false;
        }
        
        // All strict validation rules passed
        return true;
    }
    
    /**
     * Validates the SSN area number (first 3 digits) according to SSA rules.
     * 
     * This method implements the exact logic from COBOL program COACTUPC.cbl
     * 88-level condition INVALID-SSN-PART1 which specifies forbidden area numbers
     * based on Social Security Administration allocation policies.
     * 
     * <p><strong>Forbidden Area Numbers:</strong></p>
     * <ul>
     *   <li>000 - Never assigned</li>
     *   <li>666 - Never assigned (reserved)</li>
     *   <li>900-999 - Never assigned (reserved for future use)</li>
     * </ul>
     * 
     * @param areaNumber the 3-digit area number string to validate
     * @return true if the area number is valid, false if forbidden by SSA rules
     */
    private boolean isValidAreaNumber(String areaNumber) {
        int areaInt = Integer.parseInt(areaNumber);
        
        // Apply COBOL validation logic: INVALID-SSN-PART1 VALUES 0, 666, 900 THRU 999
        return areaInt != 0 && areaInt != 666 && (areaInt < 900 || areaInt > 999);
    }
    
    /**
     * Validates the SSN group number (middle 2 digits) according to SSA rules.
     * 
     * The group number must be between 01 and 99 (cannot be 00) to ensure
     * valid SSN format compliance with Social Security Administration standards.
     * 
     * @param groupNumber the 2-digit group number string to validate
     * @return true if the group number is valid (01-99), false if invalid (00)
     */
    private boolean isValidGroupNumber(String groupNumber) {
        int groupInt = Integer.parseInt(groupNumber);
        
        // Group number must be between 01 and 99 (cannot be 00)
        return groupInt >= 1 && groupInt <= 99;
    }
    
    /**
     * Validates the SSN serial number (last 4 digits) according to SSA rules.
     * 
     * The serial number must be between 0001 and 9999 (cannot be 0000) to ensure
     * valid SSN format compliance with Social Security Administration standards.
     * 
     * @param serialNumber the 4-digit serial number string to validate
     * @return true if the serial number is valid (0001-9999), false if invalid (0000)
     */
    private boolean isValidSerialNumber(String serialNumber) {
        int serialInt = Integer.parseInt(serialNumber);
        
        // Serial number must be between 0001 and 9999 (cannot be 0000)
        return serialInt >= 1 && serialInt <= 9999;
    }
    
    /**
     * Adds a custom constraint violation to the validation context.
     * 
     * This method provides Spring Boot integration for detailed error messaging,
     * allowing the validation framework to capture specific violation details
     * for display in user interfaces or API error responses.
     * 
     * <p><strong>Error Message Integration:</strong></p>
     * The added constraint violations integrate seamlessly with Spring Boot's
     * BindingResult and FieldError mechanisms, supporting both programmatic
     * error handling and automatic UI error display.
     * 
     * @param context the constraint validator context to modify
     * @param message the specific error message describing the validation failure
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        // Disable default constraint violation to allow custom message
        context.disableDefaultConstraintViolation();
        
        // Add custom constraint violation with specific error message
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}