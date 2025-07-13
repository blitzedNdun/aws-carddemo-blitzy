/*
 * PhoneNumberValidator.java
 * 
 * Jakarta Bean Validation constraint validator for North American phone numbers
 * with area code verification using NANP (North American Numbering Plan) area codes.
 * 
 * This validator implements the validation logic for the @ValidPhoneNumber annotation,
 * using the area code lookup table from COBOL copybook CSLKPCDY.cpy to ensure
 * phone numbers have valid area codes according to the North American Numbering Plan.
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
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Jakarta Bean Validation constraint validator for North American phone numbers
 * with area code verification using the NANP (North American Numbering Plan) area codes.
 * 
 * This validator implements the validation logic for the @ValidPhoneNumber annotation,
 * using the area code lookup table from COBOL copybook CSLKPCDY.cpy (VALID-PHONE-AREA-CODE condition)
 * to ensure phone numbers have valid area codes according to the North American Numbering Plan.
 * 
 * <p>Validation Process:</p>
 * <ol>
 *   <li>Parse phone number from various formats to extract digits</li>
 *   <li>Validate phone number length (10 digits or 11 digits with country code 1)</li>
 *   <li>Extract area code (first 3 digits) and exchange code (next 3 digits)</li>
 *   <li>Validate area code against CSLKPCDY.cpy lookup table</li>
 *   <li>Validate exchange code according to NANP rules</li>
 *   <li>Generate appropriate error message for validation failures</li>
 * </ol>
 * 
 * <p>NANP Validation Rules:</p>
 * <ul>
 *   <li>Phone numbers must be 10 digits (area code + exchange + number)</li>
 *   <li>Country code 1 is optional for international format</li>
 *   <li>Area code must be from valid area code list (CSLKPCDY.cpy)</li>
 *   <li>Area code cannot start with 0 or 1</li>
 *   <li>Exchange code cannot start with 0 or 1</li>
 * </ul>
 * 
 * @see ValidPhoneNumber
 * @see ConstraintValidator
 * @since CardDemo v1.0
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    

    
    /**
     * Regular expression pattern for matching various phone number formats.
     * Supports formats like:
     * - (123) 456-7890
     * - 123-456-7890
     * - 123.456.7890
     * - 123 456 7890
     * - 1234567890
     * - +1 (123) 456-7890
     * - 1-123-456-7890
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(?:\\+?1[-. ]?)?(?:\\(?([0-9]{3})\\)?[-. ]?)?([0-9]{3})[-. ]?([0-9]{4})$"
    );
    
    /**
     * Pattern to extract only digits from a phone number string.
     */
    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("\\D");
    
    // Configuration fields from the annotation
    private boolean allowNull;
    private boolean allowEmpty;
    private boolean strictAreaCodeValidation;
    private boolean allowInternationalFormat;
    
    /**
     * Initialize the validator with configuration from the annotation.
     * 
     * @param annotation the ValidPhoneNumber annotation instance
     */
    @Override
    public void initialize(ValidPhoneNumber annotation) {
        this.allowNull = annotation.allowNull();
        this.allowEmpty = annotation.allowEmpty();
        this.strictAreaCodeValidation = annotation.strictAreaCodeValidation();
        this.allowInternationalFormat = annotation.allowInternationalFormat();
    }
    
    /**
     * Validate the phone number value.
     * 
     * @param value the phone number value to validate
     * @param context the validation context
     * @return true if the phone number is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values
        if (value == null) {
            return allowNull;
        }
        
        // Handle empty strings
        if (value.trim().isEmpty()) {
            return allowEmpty;
        }
        
        // First, check if the format generally matches a phone number pattern
        Matcher formatMatcher = PHONE_PATTERN.matcher(value.trim());
        if (!formatMatcher.matches()) {
            buildConstraintViolation(context, "Phone number format is invalid");
            return false;
        }
        
        // Extract digits from the phone number
        String digitsOnly = DIGITS_ONLY_PATTERN.matcher(value.trim()).replaceAll("");
        
        // Validate phone number length
        if (digitsOnly.length() < 10 || digitsOnly.length() > 11) {
            buildConstraintViolation(context, "Phone number must be 10 digits or 11 digits with country code");
            return false;
        }
        
        // Handle country code
        String phoneNumber;
        if (digitsOnly.length() == 11) {
            if (!allowInternationalFormat) {
                buildConstraintViolation(context, "International format phone numbers are not allowed");
                return false;
            }
            // Check if it starts with country code 1
            if (!digitsOnly.startsWith("1")) {
                buildConstraintViolation(context, "International phone numbers must start with country code 1");
                return false;
            }
            phoneNumber = digitsOnly.substring(1); // Remove country code
        } else {
            phoneNumber = digitsOnly;
        }
        
        // Extract area code and exchange code
        String areaCode = phoneNumber.substring(0, 3);
        String exchangeCode = phoneNumber.substring(3, 6);
        
        // Validate area code format (cannot start with 0 or 1)
        if (areaCode.startsWith("0") || areaCode.startsWith("1")) {
            buildConstraintViolation(context, "Area code cannot start with 0 or 1");
            return false;
        }
        
        // Validate exchange code format (cannot start with 0 or 1)
        if (exchangeCode.startsWith("0") || exchangeCode.startsWith("1")) {
            buildConstraintViolation(context, "Exchange code cannot start with 0 or 1");
            return false;
        }
        
        // Validate area code against lookup table if strict validation is enabled
        if (strictAreaCodeValidation && !ValidationConstants.VALID_AREA_CODES.contains(areaCode)) {
            buildConstraintViolation(context, 
                String.format("Area code %s is not a valid North American area code", areaCode));
            return false;
        }
        
        return true;
    }
    
    /**
     * Build a constraint violation with a custom message.
     * 
     * @param context the validation context
     * @param message the custom error message
     */
    private void buildConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
    
    /**
     * Check if an area code is valid according to the CSLKPCDY.cpy lookup table.
     * 
     * @param areaCode the 3-digit area code to validate
     * @return true if the area code is valid, false otherwise
     */
    public static boolean isValidAreaCode(String areaCode) {
        return areaCode != null && 
               areaCode.length() == 3 && 
               ValidationConstants.VALID_AREA_CODES.contains(areaCode);
    }
    
    /**
     * Get the set of all valid area codes from the CSLKPCDY.cpy lookup table.
     * 
     * @return an immutable set of valid area codes
     */
    public static Set<String> getValidAreaCodes() {
        return ValidationConstants.VALID_AREA_CODES;
    }
    
    /**
     * Normalize a phone number to a standard format (removes all non-digit characters).
     * 
     * @param phoneNumber the phone number to normalize
     * @return the normalized phone number with digits only, or null if input is null
     */
    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return DIGITS_ONLY_PATTERN.matcher(phoneNumber.trim()).replaceAll("");
    }
    
    /**
     * Format a phone number in standard display format (123) 456-7890.
     * 
     * @param phoneNumber the phone number to format (digits only)
     * @return the formatted phone number, or the original string if formatting fails
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        String digitsOnly = normalizePhoneNumber(phoneNumber);
        if (digitsOnly == null) {
            return phoneNumber;
        }
        
        // Handle 11-digit number with country code
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
            digitsOnly = digitsOnly.substring(1);
        }
        
        // Format 10-digit number
        if (digitsOnly.length() == 10) {
            return String.format("(%s) %s-%s", 
                digitsOnly.substring(0, 3),
                digitsOnly.substring(3, 6),
                digitsOnly.substring(6));
        }
        
        return phoneNumber; // Return original if cannot format
    }
}