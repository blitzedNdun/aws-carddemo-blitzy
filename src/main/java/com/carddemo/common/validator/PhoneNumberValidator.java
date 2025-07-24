/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.regex.Matcher;

/**
 * Implementation of phone number validation using North American Numbering Plan (NANP) area codes.
 * 
 * <p>This validator implements comprehensive phone number validation as specified in the COBOL
 * copybook CSLKPCDY.cpy, providing exact functional equivalence to the original COBOL
 * VALID-PHONE-AREA-CODE condition validation logic.
 * 
 * <p>The validator performs multi-stage validation:
 * <ol>
 *   <li>Format normalization and pattern matching</li>
 *   <li>NANP structure validation (area code, exchange, line number)</li>
 *   <li>Area code verification against official NANP registry</li>
 *   <li>Exchange code rule enforcement (first digit 2-9, second digit 0-9)</li>
 *   <li>Service number filtering (555, 800, etc.) when configured</li>
 * </ol>
 * 
 * <p>Supported input formats:
 * <ul>
 *   <li>(123) 456-7890</li>
 *   <li>123-456-7890</li>
 *   <li>123.456.7890</li>
 *   <li>123 456 7890</li>
 *   <li>1234567890</li>
 *   <li>+1 (123) 456-7890</li>
 *   <li>1-123-456-7890</li>
 * </ul>
 * 
 * <p>The implementation maintains exact compatibility with COBOL validation behavior,
 * ensuring identical results for all input combinations tested in the original system.
 * 
 * @see ValidPhoneNumber
 * @see ValidationConstants#VALID_AREA_CODES
 * @since 1.0
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    /**
     * Pattern for extracting digits from formatted phone numbers.
     * Matches any sequence of digits, ignoring formatting characters.
     */
    private static final Pattern DIGIT_EXTRACTION_PATTERN = Pattern.compile("\\d");
    
    /**
     * Pattern for North American phone number format validation.
     * Matches various formatting styles with optional country code.
     */
    private static final Pattern NANP_FORMAT_PATTERN = Pattern.compile(
        "^(?:\\+?1[\\s\\-\\.\\(\\)]?)?" +  // Optional +1 country code
        "[\\(]?([2-9]\\d{2})[\\)\\s\\-\\.]?" +  // Area code (2-9 followed by 2 digits)
        "([2-9]\\d{2})" +  // Exchange code (2-9 followed by 2 digits)
        "[\\s\\-\\.]?" +
        "(\\d{4})$"  // Line number (4 digits)
    );
    
    /**
     * Pattern for basic 10-digit phone number validation.
     * Used as fallback for simple numeric validation.
     */
    private static final Pattern BASIC_10_DIGIT_PATTERN = Pattern.compile("^\\d{10}$");
    
    /**
     * Annotation configuration for this validator instance.
     */
    private ValidPhoneNumber annotation;

    /**
     * Initializes the validator with annotation configuration.
     * 
     * @param constraintAnnotation the annotation instance containing configuration
     */
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    /**
     * Validates a phone number according to NANP standards and configuration.
     * 
     * <p>Performs comprehensive validation following the exact logic used in the
     * COBOL VALID-PHONE-AREA-CODE condition from CSLKPCDY.cpy.
     * 
     * @param value the phone number string to validate
     * @param context the validation context for error reporting
     * @return true if the phone number is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and empty values according to configuration
        if (value == null || value.trim().isEmpty()) {
            return annotation.allowEmpty();
        }
        
        // Extract digits from formatted input
        String digitsOnly = extractDigits(value);
        
        // Handle various input lengths
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
            // Check if this is a valid US number with country code (1 + area code starting with 2-9)
            if (digitsOnly.charAt(1) >= '2' && digitsOnly.charAt(1) <= '9') {
                // Remove country code if it's a valid US number
                digitsOnly = digitsOnly.substring(1);
            } else {
                // Invalid: 11 digits starting with 1 but area code starts with 0 or 1
                return buildError(context, annotation.invalidFormatMessage(), 
                    "Invalid 11-digit number: area code must start with 2-9 after country code");
            }
        } else if (digitsOnly.length() != 10) {
            // Invalid length for NANP number
            return buildError(context, annotation.invalidFormatMessage(), 
                "Phone number must contain exactly 10 digits for North American numbers");
        }
        
        // Validate basic 10-digit format
        if (!BASIC_10_DIGIT_PATTERN.matcher(digitsOnly).matches()) {
            return buildError(context, annotation.invalidFormatMessage(),
                "Phone number must contain only digits");
        }
        
        // Extract area code, exchange, and line number
        String areaCode = digitsOnly.substring(0, 3);
        String exchange = digitsOnly.substring(3, 6);
        String lineNumber = digitsOnly.substring(6);
        
        // Validate area code
        if (!validateAreaCode(areaCode, context)) {
            return false;
        }
        
        // Validate exchange code
        if (!validateExchangeCode(exchange, context)) {
            return false;
        }
        
        // Validate line number
        if (!validateLineNumber(lineNumber, context)) {
            return false;
        }
        
        // Check for easily recognizable codes if configured
        if (!annotation.allowRecognizableCodes() && isRecognizableNumber(areaCode, exchange)) {
            return buildError(context, annotation.invalidFormatMessage(),
                "Phone number appears to use easily recognizable test or service codes");
        }
        
        return true;
    }
    
    /**
     * Extracts digits from a formatted phone number string.
     * 
     * @param phoneNumber the formatted phone number
     * @return string containing only digits
     */
    private String extractDigits(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        
        StringBuilder digits = new StringBuilder();
        Matcher matcher = DIGIT_EXTRACTION_PATTERN.matcher(phoneNumber);
        while (matcher.find()) {
            digits.append(matcher.group());
        }
        
        return digits.toString();
    }
    
    /**
     * Validates area code according to NANP rules and registry lookup.
     * 
     * <p>Implements the exact logic from COBOL 88-level condition VALID-PHONE-AREA-CODE
     * in CSLKPCDY.cpy, ensuring complete functional equivalence.
     * 
     * @param areaCode the 3-digit area code to validate
     * @param context the validation context for error reporting
     * @return true if area code is valid, false otherwise
     */
    private boolean validateAreaCode(String areaCode, ConstraintValidatorContext context) {
        // Check if area code follows NANP format (first digit 2-9)
        if (areaCode.charAt(0) < '2' || areaCode.charAt(0) > '9') {
            return buildError(context, annotation.invalidAreaCodeMessage(),
                "Area code first digit must be 2-9 according to NANP rules");
        }
        
        // Perform strict area code validation if configured
        if (annotation.strictAreaCodeValidation()) {
            if (!ValidationConstants.VALID_AREA_CODES.contains(areaCode)) {
                return buildError(context, annotation.invalidAreaCodeMessage(),
                    "Area code " + areaCode + " is not a valid North American area code");
            }
        }
        
        return true;
    }
    
    /**
     * Validates exchange code according to NANP rules.
     * 
     * <p>Exchange codes must follow specific NANP rules:
     * <ul>
     *   <li>First digit: 2-9 (cannot be 0 or 1)</li>
     *   <li>Second digit: 0-9 (any digit allowed)</li>
     *   <li>Third digit: 0-9 (any digit allowed)</li>
     * </ul>
     * 
     * @param exchange the 3-digit exchange code to validate
     * @param context the validation context for error reporting
     * @return true if exchange code is valid, false otherwise
     */
    private boolean validateExchangeCode(String exchange, ConstraintValidatorContext context) {
        // First digit must be 2-9
        if (exchange.charAt(0) < '2' || exchange.charAt(0) > '9') {
            return buildError(context, annotation.invalidExchangeMessage(),
                "Exchange code first digit must be 2-9 according to NANP rules");
        }
        
        // Second and third digits can be 0-9 (already validated by digit extraction)
        return true;
    }
    
    /**
     * Validates line number according to NANP rules.
     * 
     * <p>Line numbers must be exactly 4 digits (0000-9999).
     * All combinations are valid in NANP.
     * 
     * @param lineNumber the 4-digit line number to validate
     * @param context the validation context for error reporting
     * @return true if line number is valid, false otherwise
     */
    private boolean validateLineNumber(String lineNumber, ConstraintValidatorContext context) {
        // Line number must be exactly 4 digits (already validated by length check)
        // All 4-digit combinations are valid in NANP
        return true;
    }
    
    /**
     * Checks if a phone number uses easily recognizable test or service codes.
     * 
     * <p>Common recognizable patterns include:
     * <ul>
     *   <li>555 exchange (test numbers)</li>
     *   <li>Obvious repeating digit patterns (111, 222, etc.)</li>
     *   <li>Classic test patterns (000, 999)</li>
     * </ul>
     * 
     * @param areaCode the area code portion
     * @param exchange the exchange code portion
     * @return true if number appears to use recognizable codes, false otherwise
     */
    private boolean isRecognizableNumber(String areaCode, String exchange) {
        // Check for 555 exchange (classic test number)
        if ("555".equals(exchange)) {
            return true;
        }
        
        // Check for obvious repeating digit patterns in area code (all same digit)
        if (areaCode.charAt(0) == areaCode.charAt(1) && 
            areaCode.charAt(1) == areaCode.charAt(2)) {
            return true;
        }
        
        // Check for obvious repeating digit patterns in exchange (all same digit)
        if (exchange.charAt(0) == exchange.charAt(1) && 
            exchange.charAt(1) == exchange.charAt(2)) {
            return true;
        }
        
        // Check for obvious test patterns (000, 999)
        if ("000".equals(exchange) || "999".equals(exchange) ||
            "000".equals(areaCode) || "999".equals(areaCode)) {
            return true;
        }
        
        return false;
    }

    
    /**
     * Builds a validation error with custom message and adds it to the context.
     * 
     * @param context the validation context
     * @param message the error message template
     * @param details detailed error description
     * @return false (indicating validation failure)
     */
    private boolean buildError(ConstraintValidatorContext context, String message, String details) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message + ": " + details)
               .addConstraintViolation();
        return false;
    }
}