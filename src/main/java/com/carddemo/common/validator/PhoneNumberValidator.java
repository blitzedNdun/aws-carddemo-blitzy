/*
 * PhoneNumberValidator.java
 * 
 * Implementation class for phone number validation with area code verification.
 * Validates phone numbers against the complete North American Numbering Plan
 * area code list while supporting multiple input formats and providing
 * comprehensive error reporting.
 * 
 * This validator implements the exact phone number validation logic from the
 * COBOL system, converting the CSLKPCDY.cpy area code lookup table to a
 * Java Set-based implementation for efficient validation while maintaining
 * complete functional equivalence with the original COBOL validation.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation implementation for validating North American phone numbers
 * with comprehensive area code verification using the NANP (North American Numbering Plan).
 * 
 * <p>This validator provides exact functional equivalence to the COBOL phone number
 * validation logic found in the original CardDemo mainframe application. It validates
 * phone numbers against the complete area code lookup table from CSLKPCDY.cpy while
 * supporting multiple input formats commonly used in North American phone numbers.</p>
 * 
 * <p><strong>COBOL Source Mapping:</strong></p>
 * <ul>
 *   <li>CSLKPCDY.cpy VALID-PHONE-AREA-CODE → ValidationConstants.VALID_AREA_CODES</li>
 *   <li>CUSTREC.cpy CUST-PHONE-NUM-1/2 PIC X(15) → Support for 15-character input format</li>
 *   <li>COBOL 88-level area code validation → Java Set.contains() lookup</li>
 * </ul>
 * 
 * <p><strong>Validation Process:</strong></p>
 * <ol>
 *   <li>Parse input string to extract numeric digits</li>
 *   <li>Handle optional country code (1) prefix</li>
 *   <li>Validate 10-digit North American format (NXX-NXX-XXXX)</li>
 *   <li>Extract 3-digit area code (NXX)</li>
 *   <li>Verify area code against CSLKPCDY.cpy lookup table</li>
 *   <li>Validate exchange code doesn't start with 0 or 1 (NANP rule)</li>
 *   <li>Ensure subscriber number is valid 4-digit sequence</li>
 * </ol>
 * 
 * <p><strong>Supported Input Formats:</strong></p>
 * <ul>
 *   <li>(123) 456-7890 - Standard US format with parentheses</li>
 *   <li>123-456-7890 - Hyphenated format</li>
 *   <li>123.456.7890 - Dot-separated format</li>
 *   <li>123 456 7890 - Space-separated format</li>
 *   <li>1234567890 - Continuous 10-digit format</li>
 *   <li>+1 (123) 456-7890 - International format with country code</li>
 *   <li>1-123-456-7890 - Full format with country code</li>
 *   <li>+1 123 456 7890 - International space-separated format</li>
 *   <li>1.123.456.7890 - Full dot-separated format</li>
 *   <li>11234567890 - Continuous 11-digit format with country code</li>
 * </ul>
 * 
 * <p><strong>NANP Validation Rules:</strong></p>
 * <ul>
 *   <li>Total digits must be exactly 10 (or 11 with country code 1)</li>
 *   <li>Area code (first 3 digits) must be in VALID_AREA_CODES Set</li>
 *   <li>Area code cannot start with 0 or 1 (already enforced by lookup table)</li>
 *   <li>Exchange code (digits 4-6) cannot start with 0 or 1</li>
 *   <li>Subscriber number (digits 7-10) can be any 4-digit combination</li>
 *   <li>Country code, if present, must be exactly 1</li>
 * </ul>
 * 
 * <p><strong>Error Message Strategy:</strong></p>
 * The validator provides specific error messages for different failure scenarios
 * to maintain the detailed validation feedback expected by the CardDemo application:
 * <ul>
 *   <li>Format errors: "Phone number format is invalid"</li>
 *   <li>Area code errors: "Area code {code} is not a valid North American area code"</li>
 *   <li>Exchange code errors: "Exchange code cannot start with 0 or 1"</li>
 *   <li>General errors: "Phone number is invalid"</li>
 * </ul>
 * 
 * @see ValidPhoneNumber
 * @see ValidationConstants#VALID_AREA_CODES
 * @see ValidationConstants#PHONE_PATTERN
 * @since CardDemo v1.0
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    /**
     * Comprehensive regex pattern for parsing various North American phone number formats.
     * Captures all common formatting variations while extracting the core numeric digits.
     * Groups: 1=country code (optional), 2=area code, 3=exchange, 4=subscriber number
     */
    private static final Pattern PHONE_FORMAT_PATTERN = Pattern.compile(
        "^\\s*" +                           // Optional leading whitespace
        "(?:\\+?1[\\s.-]?)?" +              // Optional country code 1 with separators (non-capturing)
        "(?:\\(?(\\d{3})\\)?[\\s.-]?)?" +   // Area code with optional parentheses and separators (group 1)
        "(\\d{3})" +                        // Exchange code (group 2)
        "[\\s.-]?" +                        // Optional separator
        "(\\d{4})" +                        // Subscriber number (group 3)
        "\\s*$"                             // Optional trailing whitespace
    );
    
    /**
     * Simplified pattern for extracting just numeric digits from phone number input.
     * Used as fallback when the comprehensive pattern doesn't match.
     */
    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("\\d");
    
    /**
     * Annotation configuration values set during initialization.
     */
    private boolean allowNull;
    private boolean allowEmpty;
    private boolean strictAreaCodeValidation;
    private boolean allowInternationalFormat;
    
    /**
     * Initializes the validator with configuration values from the ValidPhoneNumber annotation.
     * Sets up validation behavior based on annotation parameters.
     * 
     * @param constraintAnnotation the ValidPhoneNumber annotation instance containing configuration
     */
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.allowEmpty = constraintAnnotation.allowEmpty();
        this.strictAreaCodeValidation = constraintAnnotation.strictAreaCodeValidation();
        this.allowInternationalFormat = constraintAnnotation.allowInternationalFormat();
    }
    
    /**
     * Validates a phone number string against North American Numbering Plan requirements
     * with area code verification using the CSLKPCDY.cpy lookup table.
     * 
     * <p>This method implements the complete phone number validation logic equivalent
     * to the COBOL system, ensuring exact functional compatibility while providing
     * comprehensive error reporting through the constraint validation context.</p>
     * 
     * @param value the phone number string to validate
     * @param context the constraint validator context for error message customization
     * @return true if the phone number is valid according to NANP rules and area code lookup,
     *         false otherwise with specific error messages added to the context
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and empty values according to annotation configuration
        if (value == null) {
            return allowNull;
        }
        
        if (value.trim().isEmpty()) {
            return allowEmpty;
        }
        
        // Extract numeric digits from the input using regex matching
        String numericDigits = extractNumericDigits(value);
        
        // Validate the numeric digit count (10 or 11 with country code)
        if (!isValidDigitCount(numericDigits, context)) {
            return false;
        }
        
        // Handle country code prefix if present
        String tenDigitNumber = normalizeToTenDigits(numericDigits, context);
        if (tenDigitNumber == null) {
            return false; // Error message already set in normalizeToTenDigits
        }
        
        // Validate the 10-digit phone number structure using pattern matching
        if (!ValidationConstants.PHONE_PATTERN.matcher(tenDigitNumber).matches()) {
            buildConstraintViolation(context, "Phone number format is invalid");
            return false;
        }
        
        // Extract phone number components using regex groups
        PhoneNumberComponents components = parsePhoneComponents(tenDigitNumber);
        if (components == null) {
            buildConstraintViolation(context, "Phone number format is invalid");
            return false;
        }
        
        // Validate area code against CSLKPCDY.cpy lookup table
        if (!validateAreaCode(components.areaCode, context)) {
            return false;
        }
        
        // Validate exchange code against NANP rules
        if (!validateExchangeCode(components.exchangeCode, context)) {
            return false;
        }
        
        // All validations passed - phone number is valid
        return true;
    }
    
    /**
     * Extracts numeric digits from phone number input, handling various formatting characters.
     * Supports multiple input formats by removing all non-digit characters.
     * 
     * @param input the raw phone number input string
     * @return string containing only the numeric digits
     */
    private String extractNumericDigits(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder digits = new StringBuilder();
        Matcher digitMatcher = DIGITS_ONLY_PATTERN.matcher(input);
        
        while (digitMatcher.find()) {
            digits.append(digitMatcher.group());
        }
        
        return digits.toString();
    }
    
    /**
     * Validates that the extracted digits represent a valid North American phone number length.
     * Checks for 10 digits (domestic) or 11 digits with country code 1 (international).
     * 
     * @param digits the numeric digits extracted from input
     * @param context constraint validator context for error reporting
     * @return true if digit count is valid, false otherwise
     */
    private boolean isValidDigitCount(String digits, ConstraintValidatorContext context) {
        int length = digits.length();
        
        if (length == 10) {
            return true; // Valid domestic format
        }
        
        if (length == 11 && allowInternationalFormat) {
            return true; // Valid international format with country code
        }
        
        // Invalid digit count
        if (length < 10) {
            buildConstraintViolation(context, "Phone number is too short - must be 10 digits");
        } else if (length == 11 && !allowInternationalFormat) {
            buildConstraintViolation(context, "International format not allowed");
        } else {
            buildConstraintViolation(context, "Phone number format is invalid");
        }
        
        return false;
    }
    
    /**
     * Normalizes phone number digits to a standard 10-digit format.
     * Handles country code removal and validation for 11-digit numbers.
     * 
     * @param digits the numeric digits to normalize
     * @param context constraint validator context for error reporting
     * @return 10-digit phone number string, or null if normalization fails
     */
    private String normalizeToTenDigits(String digits, ConstraintValidatorContext context) {
        if (digits.length() == 10) {
            return digits; // Already in correct format
        }
        
        if (digits.length() == 11) {
            // Validate country code is 1
            if (!digits.startsWith("1")) {
                buildConstraintViolation(context, "Invalid country code - must be 1 for North American numbers");
                return null;
            }
            
            // Remove country code prefix
            return digits.substring(1);
        }
        
        // Invalid length - error already reported in isValidDigitCount
        return null;
    }
    
    /**
     * Parses a 10-digit phone number into its component parts.
     * Extracts area code, exchange code, and subscriber number.
     * 
     * @param tenDigitNumber the normalized 10-digit phone number
     * @return PhoneNumberComponents object with parsed parts, or null if parsing fails
     */
    private PhoneNumberComponents parsePhoneComponents(String tenDigitNumber) {
        if (tenDigitNumber == null || tenDigitNumber.length() != 10) {
            return null;
        }
        
        try {
            String areaCode = tenDigitNumber.substring(0, 3);
            String exchangeCode = tenDigitNumber.substring(3, 6);
            String subscriberNumber = tenDigitNumber.substring(6, 10);
            
            return new PhoneNumberComponents(areaCode, exchangeCode, subscriberNumber);
        } catch (Exception e) {
            return null; // Parsing failed
        }
    }
    
    /**
     * Validates the area code against the CSLKPCDY.cpy lookup table.
     * Implements the exact COBOL VALID-PHONE-AREA-CODE validation logic
     * using Java Set-based lookup for efficient performance.
     * 
     * @param areaCode the 3-digit area code to validate
     * @param context constraint validator context for error reporting
     * @return true if area code is valid according to NANP and lookup table, false otherwise
     */
    private boolean validateAreaCode(String areaCode, ConstraintValidatorContext context) {
        // Validate area code format (3 digits, not starting with 0 or 1)
        if (areaCode == null || areaCode.length() != 3) {
            buildConstraintViolation(context, "Area code must be exactly 3 digits");
            return false;
        }
        
        // Check if area code starts with 0 or 1 (invalid per NANP rules)
        char firstDigit = areaCode.charAt(0);
        if (firstDigit == '0' || firstDigit == '1') {
            buildConstraintViolation(context, "Area code cannot start with 0 or 1");
            return false;
        }
        
        // If strict validation is enabled, check against CSLKPCDY.cpy lookup table
        if (strictAreaCodeValidation) {
            if (!ValidationConstants.VALID_AREA_CODES.contains(areaCode)) {
                buildConstraintViolation(context, 
                    String.format("Area code %s is not a valid North American area code", areaCode));
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validates the exchange code according to NANP rules.
     * The exchange code (central office code) cannot start with 0 or 1.
     * 
     * @param exchangeCode the 3-digit exchange code to validate
     * @param context constraint validator context for error reporting
     * @return true if exchange code is valid, false otherwise
     */
    private boolean validateExchangeCode(String exchangeCode, ConstraintValidatorContext context) {
        // Validate exchange code format (3 digits)
        if (exchangeCode == null || exchangeCode.length() != 3) {
            buildConstraintViolation(context, "Exchange code must be exactly 3 digits");
            return false;
        }
        
        // Check if exchange code starts with 0 or 1 (invalid per NANP rules)
        char firstDigit = exchangeCode.charAt(0);
        if (firstDigit == '0' || firstDigit == '1') {
            buildConstraintViolation(context, "Exchange code cannot start with 0 or 1");
            return false;
        }
        
        return true;
    }
    
    /**
     * Builds a custom constraint violation with a specific error message.
     * Disables the default constraint violation and adds a new one with the provided message.
     * 
     * @param context the constraint validator context
     * @param message the custom error message to display
     */
    private void buildConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
    
    /**
     * Inner class to hold parsed phone number components.
     * Provides structured access to area code, exchange code, and subscriber number.
     */
    private static class PhoneNumberComponents {
        final String areaCode;
        final String exchangeCode;
        final String subscriberNumber;
        
        /**
         * Creates a new phone number components object.
         * 
         * @param areaCode the 3-digit area code
         * @param exchangeCode the 3-digit exchange code
         * @param subscriberNumber the 4-digit subscriber number
         */
        PhoneNumberComponents(String areaCode, String exchangeCode, String subscriberNumber) {
            this.areaCode = areaCode;
            this.exchangeCode = exchangeCode;
            this.subscriberNumber = subscriberNumber;
        }
    }
}