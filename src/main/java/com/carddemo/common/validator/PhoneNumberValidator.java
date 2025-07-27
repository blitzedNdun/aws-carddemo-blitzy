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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation implementation for North American phone number validation with area code verification.
 * 
 * <p>This validator implements comprehensive phone number validation that mirrors the COBOL validation
 * logic found in CSLKPCDY.cpy copybook. It validates phone numbers against the North American Numbering
 * Plan (NANP) area code registry and supports multiple input formats while ensuring strict compliance
 * with telecommunications standards.
 * 
 * <p>The validator performs the following validations:
 * <ul>
 *   <li>Format parsing and digit extraction from various input formats</li>
 *   <li>North American area code validation against NANP registry (CSLKPCDY.cpy VALID-PHONE-AREA-CODE)</li>
 *   <li>Exchange code validation following NANP rules (2-9 for first digit, 0-9 for second)</li>
 *   <li>Line number validation (4-digit subscriber number)</li>
 *   <li>International format handling with country code support</li>
 *   <li>Easily recognizable code filtering (555, 800 series, etc.)</li>
 * </ul>
 * 
 * <p>Supported input formats include:
 * <ul>
 *   <li>(123) 456-7890</li>
 *   <li>123-456-7890</li>
 *   <li>123.456.7890</li>
 *   <li>123 456 7890</li>
 *   <li>1234567890</li>
 *   <li>+1 (123) 456-7890</li>
 *   <li>1-123-456-7890</li>
 *   <li>+1 123 456 7890</li>
 * </ul>
 * 
 * <p>This implementation maintains exact functional equivalence with COBOL area code lookup
 * processing while providing enhanced error reporting and multiple format support for 
 * modern Java applications.
 * 
 * @see ValidPhoneNumber
 * @see ValidationConstants#VALID_AREA_CODES
 * @since 1.0
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    /**
     * Compiled regex pattern for extracting digits from phone number input.
     * Matches digits while preserving position for area code extraction.
     */
    private static final Pattern DIGIT_EXTRACTION_PATTERN = Pattern.compile("\\d");

    /**
     * Pattern for validating North American phone number format (10 digits).
     * Captures area code (group 1), exchange (group 2), and line number (group 3).
     */
    private static final Pattern NANP_FORMAT_PATTERN = Pattern.compile("^(\\d{3})(\\d{3})(\\d{4})$");

    /**
     * Pattern for validating North American phone number with country code (11 digits).
     * Captures country code (group 1), area code (group 2), exchange (group 3), and line number (group 4).
     */
    private static final Pattern NANP_WITH_COUNTRY_PATTERN = Pattern.compile("^1(\\d{3})(\\d{3})(\\d{4})$");

    /**
     * Pattern for international phone number format validation.
     * Allows country codes from 1-4 digits followed by 7-15 additional digits.
     */
    private static final Pattern INTERNATIONAL_FORMAT_PATTERN = Pattern.compile("^\\+?\\d{1,4}\\d{7,15}$");

    // Validation configuration from annotation
    private boolean allowEmpty;
    private boolean allowInternational;
    private boolean strictAreaCodeValidation;
    private boolean allowRecognizableCodes;
    private String invalidAreaCodeMessage;
    private String invalidFormatMessage;
    private String invalidExchangeMessage;

    /**
     * Initializes the validator with configuration from the ValidPhoneNumber annotation.
     * 
     * @param constraintAnnotation The ValidPhoneNumber annotation instance containing validation parameters
     */
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
        this.allowInternational = constraintAnnotation.allowInternational();
        this.strictAreaCodeValidation = constraintAnnotation.strictAreaCodeValidation();
        this.allowRecognizableCodes = constraintAnnotation.allowRecognizableCodes();
        this.invalidAreaCodeMessage = constraintAnnotation.invalidAreaCodeMessage();
        this.invalidFormatMessage = constraintAnnotation.invalidFormatMessage();
        this.invalidExchangeMessage = constraintAnnotation.invalidExchangeMessage();
    }

    /**
     * Validates a phone number string against North American Numbering Plan requirements.
     * 
     * <p>Performs comprehensive validation including:
     * <ul>
     *   <li>Format parsing and digit extraction</li>
     *   <li>Area code validation against NANP registry from CSLKPCDY.cpy</li>
     *   <li>Exchange code NANP compliance checking</li>
     *   <li>International format handling when enabled</li>
     *   <li>Custom error message generation for specific failure modes</li>
     * </ul>
     * 
     * @param value The phone number string to validate
     * @param context The constraint validator context for error message customization
     * @return true if the phone number is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and empty values according to allowEmpty setting
        if (value == null || value.trim().isEmpty()) {
            return allowEmpty;
        }

        // Extract digits from the input string, removing all formatting characters
        String digitsOnly = extractDigitsOnly(value);
        
        // Validate basic format requirements
        if (!isValidDigitCount(digitsOnly)) {
            return buildConstraintViolation(context, invalidFormatMessage);
        }

        // Parse the phone number components based on digit count
        PhoneNumberComponents components = parsePhoneNumber(digitsOnly);
        if (components == null) {
            return buildConstraintViolation(context, invalidFormatMessage);
        }

        // Validate area code against NANP registry if strict validation is enabled
        if (strictAreaCodeValidation && !isValidAreaCode(components.areaCode)) {
            return buildConstraintViolation(context, invalidAreaCodeMessage + ": " + components.areaCode);
        }

        // Validate exchange code according to NANP rules
        if (!isValidExchangeCode(components.exchangeCode)) {
            return buildConstraintViolation(context, invalidExchangeMessage + ": " + components.exchangeCode);
        }

        // Filter easily recognizable codes if not allowed
        if (!allowRecognizableCodes && isEasilyRecognizableCode(components.areaCode)) {
            return buildConstraintViolation(context, "Phone number uses easily recognizable area code: " + components.areaCode);
        }

        // Validate line number (subscriber number)
        if (!isValidLineNumber(components.lineNumber)) {
            return buildConstraintViolation(context, "Invalid line number: " + components.lineNumber);
        }

        return true;
    }

    /**
     * Extracts only numeric digits from a phone number string.
     * Removes all formatting characters including spaces, hyphens, parentheses, dots, and plus signs.
     * 
     * @param phoneNumber The formatted phone number string
     * @return String containing only numeric digits
     */
    private String extractDigitsOnly(String phoneNumber) {
        StringBuilder digits = new StringBuilder();
        Matcher matcher = DIGIT_EXTRACTION_PATTERN.matcher(phoneNumber);
        
        while (matcher.find()) {
            digits.append(matcher.group());
        }
        
        return digits.toString();
    }

    /**
     * Validates that the extracted digits conform to supported phone number lengths.
     * 
     * @param digits String containing only numeric digits
     * @return true if digit count is valid (10 for domestic, 11 for North American with country code)
     */
    private boolean isValidDigitCount(String digits) {
        int length = digits.length();
        
        // 10 digits: North American domestic format (area code + exchange + line)
        if (length == 10) {
            return true;
        }
        
        // 11 digits: North American with country code (1 + area code + exchange + line)
        if (length == 11 && allowInternational) {
            return digits.startsWith("1"); // Must start with country code 1 for North America
        }
        
        // Other lengths for international numbers if enabled
        if (allowInternational && length >= 7 && length <= 15) {
            return ValidationConstants.PHONE_PATTERN.matcher(reconstructFormattedNumber(digits)).matches();
        }
        
        return false;
    }

    /**
     * Parses phone number digits into component parts (country code, area code, exchange, line number).
     * 
     * @param digits String containing only numeric digits
     * @return PhoneNumberComponents object with parsed parts, or null if parsing fails
     */
    private PhoneNumberComponents parsePhoneNumber(String digits) {
        Matcher matcher;
        
        // Try 10-digit North American format first
        matcher = NANP_FORMAT_PATTERN.matcher(digits);
        if (matcher.matches()) {
            return new PhoneNumberComponents(
                null,                    // No country code
                matcher.group(1),        // Area code (digits 1-3)
                matcher.group(2),        // Exchange code (digits 4-6)
                matcher.group(3)         // Line number (digits 7-10)
            );
        }
        
        // Try 11-digit North American format with country code
        if (allowInternational) {
            matcher = NANP_WITH_COUNTRY_PATTERN.matcher(digits);
            if (matcher.matches()) {
                return new PhoneNumberComponents(
                    "1",                     // Country code
                    matcher.group(1),        // Area code (digits 2-4)
                    matcher.group(2),        // Exchange code (digits 5-7)
                    matcher.group(3)         // Line number (digits 8-11)
                );
            }
        }
        
        return null; // Unable to parse into recognized format
    }

    /**
     * Validates area code against the North American Numbering Plan registry.
     * Uses the VALID_AREA_CODES Set from ValidationConstants which contains all
     * valid area codes from CSLKPCDY.cpy VALID-PHONE-AREA-CODE condition.
     * 
     * @param areaCode Three-digit area code string
     * @return true if area code is found in NANP registry, false otherwise
     */
    private boolean isValidAreaCode(String areaCode) {
        if (areaCode == null || areaCode.length() != 3) {
            return false;
        }
        
        // Validate against NANP area code registry from CSLKPCDY.cpy
        return ValidationConstants.VALID_AREA_CODES.contains(areaCode);
    }

    /**
     * Validates exchange code according to North American Numbering Plan rules.
     * 
     * <p>NANP rules for exchange codes:
     * <ul>
     *   <li>First digit must be 2-9 (cannot be 0 or 1)</li>
     *   <li>Second digit can be 0-9</li>
     *   <li>Third digit can be 0-9</li>
     * </ul>
     * 
     * @param exchangeCode Three-digit exchange code string  
     * @return true if exchange code follows NANP rules, false otherwise
     */
    private boolean isValidExchangeCode(String exchangeCode) {
        if (exchangeCode == null || exchangeCode.length() != 3) {
            return false;
        }
        
        // First digit must be 2-9 according to NANP rules
        char firstDigit = exchangeCode.charAt(0);
        if (firstDigit < '2' || firstDigit > '9') {
            return false;
        }
        
        // Second and third digits can be 0-9
        char secondDigit = exchangeCode.charAt(1);
        char thirdDigit = exchangeCode.charAt(2);
        
        return (secondDigit >= '0' && secondDigit <= '9') && 
               (thirdDigit >= '0' && thirdDigit <= '9');
    }

    /**
     * Validates line number (subscriber number) format.
     * 
     * @param lineNumber Four-digit line number string
     * @return true if line number is valid 4-digit format, false otherwise
     */
    private boolean isValidLineNumber(String lineNumber) {
        if (lineNumber == null || lineNumber.length() != 4) {
            return false;
        }
        
        // All digits must be 0-9
        return lineNumber.matches("\\d{4}");
    }

    /**
     * Checks if an area code is an easily recognizable code (test numbers, service codes).
     * Based on CSLKPCDY.cpy VALID-EASY-RECOG-AREA-CODE condition which includes
     * patterns like 200, 211, 222, 555, 800, 888, etc.
     * 
     * @param areaCode Three-digit area code string
     * @return true if area code is easily recognizable, false otherwise
     */
    private boolean isEasilyRecognizableCode(String areaCode) {
        if (areaCode == null || areaCode.length() != 3) {
            return false;
        }
        
        // Check for easily recognizable patterns from CSLKPCDY.cpy
        // These include repeating digits and common service codes
        char first = areaCode.charAt(0);
        char second = areaCode.charAt(1);
        char third = areaCode.charAt(2);
        
        // All three digits the same (111, 222, 333, etc.)
        if (first == second && second == third) {
            return true;
        }
        
        // Common service codes
        if (areaCode.equals("555") || areaCode.equals("800") || areaCode.equals("888") || 
            areaCode.equals("877") || areaCode.equals("866") || areaCode.equals("855") ||
            areaCode.equals("844") || areaCode.equals("833")) {
            return true;
        }
        
        // Sequential patterns (123, 234, etc.) - simplified check
        if (Math.abs(first - second) == 1 && Math.abs(second - third) == 1) {
            return true;
        }
        
        return false;
    }

    /**
     * Reconstructs a formatted phone number string for pattern matching.
     * Used for international number validation.
     * 
     * @param digits String containing only numeric digits
     * @return Formatted phone number string
     */
    private String reconstructFormattedNumber(String digits) {
        // Simple formatting for pattern matching - preserve digits and add minimal formatting
        if (digits.length() == 10) {
            return String.format("%s-%s-%s", 
                digits.substring(0, 3), 
                digits.substring(3, 6), 
                digits.substring(6, 10));
        } else if (digits.length() == 11 && digits.startsWith("1")) {
            return String.format("1-%s-%s-%s", 
                digits.substring(1, 4), 
                digits.substring(4, 7), 
                digits.substring(7, 11));
        }
        return digits;
    }

    /**
     * Builds a custom constraint violation with a specific error message.
     * 
     * @param context The constraint validator context
     * @param message The custom error message
     * @return false (indicating validation failure)
     */
    private boolean buildConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }

    /**
     * Inner class to hold parsed phone number components.
     * Provides structured access to country code, area code, exchange code, and line number.
     */
    private static class PhoneNumberComponents {
        final String countryCode;
        final String areaCode;
        final String exchangeCode;
        final String lineNumber;

        /**
         * Creates a new PhoneNumberComponents instance.
         * 
         * @param countryCode The country code (can be null for domestic numbers)
         * @param areaCode The three-digit area code
         * @param exchangeCode The three-digit exchange code
         * @param lineNumber The four-digit line number
         */
        PhoneNumberComponents(String countryCode, String areaCode, String exchangeCode, String lineNumber) {
            this.countryCode = countryCode;
            this.areaCode = areaCode;
            this.exchangeCode = exchangeCode;
            this.lineNumber = lineNumber;
        }

        /**
         * Returns a string representation of the phone number components.
         * 
         * @return Formatted string showing all components
         */
        @Override
        public String toString() {
            return String.format("PhoneNumberComponents{countryCode='%s', areaCode='%s', exchangeCode='%s', lineNumber='%s'}", 
                countryCode, areaCode, exchangeCode, lineNumber);
        }
    }
}