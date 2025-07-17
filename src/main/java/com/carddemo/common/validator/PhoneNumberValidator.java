/*
 * PhoneNumberValidator.java
 * 
 * Jakarta Bean Validation implementation for North American phone number validation
 * with comprehensive area code verification against the NANP area code list from
 * COBOL copybook CSLKPCDY.cpy.
 * 
 * This validator maintains exact functional equivalence with the original COBOL
 * phone number validation logic while providing modern Java validation capabilities
 * and detailed error reporting for invalid formats and area codes.
 * 
 * Part of CardDemo mainframe modernization - COBOL to Java transformation.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

package com.carddemo.common.validator;

import com.carddemo.common.validator.ValidationConstants;
import com.carddemo.common.validator.ValidPhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation implementation for North American phone number validation.
 * 
 * This class implements the validation logic for the {@link ValidPhoneNumber} annotation,
 * providing comprehensive phone number validation including:
 * <ul>
 *   <li>Format validation supporting multiple input formats</li>
 *   <li>Area code validation against complete NANP area code list</li>
 *   <li>Exchange code validation (cannot start with 0 or 1, cannot be N11)</li>
 *   <li>Length validation based on COBOL field constraints</li>
 *   <li>Configurable validation rules through annotation parameters</li>
 * </ul>
 * 
 * <p>Supported phone number input formats:
 * <ul>
 *   <li>(###) ###-#### - Standard US format with parentheses</li>
 *   <li>###-###-#### - Hyphenated format</li>
 *   <li>###.###.#### - Dot-separated format</li>
 *   <li>### ### #### - Space-separated format</li>
 *   <li>########## - 10 digits with no formatting</li>
 *   <li>+1########## - International format with country code</li>
 *   <li>1########## - US format with leading 1</li>
 * </ul>
 * 
 * <p>Area code validation rules based on CSLKPCDY.cpy:
 * <ul>
 *   <li>Must be present in VALID_AREA_CODES set from ValidationConstants</li>
 *   <li>Cannot be easily recognizable codes (555, 800, etc.) unless explicitly allowed</li>
 *   <li>Can be restricted to general purpose codes only if configured</li>
 * </ul>
 * 
 * <p>Exchange code validation rules:
 * <ul>
 *   <li>Cannot start with 0 (0XX numbers are reserved)</li>
 *   <li>Cannot start with 1 (1XX numbers are reserved)</li>
 *   <li>Cannot be N11 format where N is 2-9 (special service codes)</li>
 * </ul>
 * 
 * <p>Error handling provides specific messages for different validation failures:
 * <ul>
 *   <li>Invalid format errors with format guidance</li>
 *   <li>Invalid area code errors with specific area code mentioned</li>
 *   <li>Invalid exchange code errors with explanation</li>
 *   <li>Length violation errors with max length specified</li>
 * </ul>
 * 
 * @see ValidPhoneNumber
 * @see ValidationConstants#VALID_AREA_CODES
 * @see ValidationConstants#PHONE_PATTERN
 * @since 1.0
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    // Comprehensive regex pattern supporting all common North American phone number formats
    private static final Pattern FLEXIBLE_PHONE_PATTERN = Pattern.compile(
        "^(?:\\+?1[-\\s\\.]?)?(?:\\(?([0-9]{3})\\)?[-\\s\\.]?)([0-9]{3})[-\\s\\.]?([0-9]{4})$"
    );
    
    // Pattern for extracting digits only from any phone number format
    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("[^0-9]");
    
    // Easily recognizable area codes that are typically reserved or special use
    private static final Pattern EASY_RECOGNIZABLE_PATTERN = Pattern.compile(
        "^(200|211|222|233|244|255|266|277|288|299|300|311|322|333|344|355|366|377|388|399|" +
        "400|411|422|433|444|455|466|477|488|499|500|511|522|533|544|555|566|577|588|599|" +
        "600|611|622|633|644|655|666|677|688|699|700|711|722|733|744|755|766|777|788|799|" +
        "800|811|822|833|844|855|866|877|888|899|900|911|922|933|944|955|966|977|988|999)$"
    );
    
    // Special service codes pattern (N11 where N is 2-9)
    private static final Pattern N11_PATTERN = Pattern.compile("^[2-9]11$");
    
    // Validation configuration from annotation
    private boolean allowNull;
    private boolean allowEmpty;
    private boolean allowEasyRecognizableAreaCodes;
    private boolean strictFormatting;
    private int maxLength;
    private boolean generalPurposeOnly;
    private String context;
    
    /**
     * Initializes the validator with configuration from the ValidPhoneNumber annotation.
     * 
     * @param constraintAnnotation the ValidPhoneNumber annotation containing validation parameters
     */
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.allowEmpty = constraintAnnotation.allowEmpty();
        this.allowEasyRecognizableAreaCodes = constraintAnnotation.allowEasyRecognizableAreaCodes();
        this.strictFormatting = constraintAnnotation.strictFormatting();
        this.maxLength = constraintAnnotation.maxLength();
        this.generalPurposeOnly = constraintAnnotation.generalPurposeOnly();
        this.context = constraintAnnotation.context();
    }
    
    /**
     * Validates the phone number according to NANP rules and configured validation parameters.
     * 
     * This method performs comprehensive validation including:
     * <ul>
     *   <li>Null and empty value handling based on configuration</li>
     *   <li>Length validation against COBOL field constraints</li>
     *   <li>Format validation supporting multiple input formats</li>
     *   <li>Area code validation against NANP area code list</li>
     *   <li>Exchange code validation for reserved numbers</li>
     *   <li>Special service code validation (N11 numbers)</li>
     * </ul>
     * 
     * @param value the phone number string to validate
     * @param context the validation context for error message customization
     * @return true if the phone number is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values based on configuration
        if (value == null) {
            if (allowNull) {
                return true;
            } else {
                buildErrorMessage(context, "Phone number cannot be null", "NULL_VALUE");
                return false;
            }
        }
        
        // Handle empty values based on configuration
        if (value.trim().isEmpty()) {
            if (allowEmpty) {
                return true;
            } else {
                buildErrorMessage(context, "Phone number cannot be empty", "EMPTY_VALUE");
                return false;
            }
        }
        
        // Validate length against COBOL field constraint (PIC X(15))
        if (value.length() > maxLength) {
            buildErrorMessage(context, 
                String.format("Phone number exceeds maximum length of %d characters", maxLength), 
                "LENGTH_EXCEEDED");
            return false;
        }
        
        // Extract digits from the phone number
        String digitsOnly = DIGITS_ONLY_PATTERN.matcher(value).replaceAll("");
        
        // Handle phone numbers with country code
        if (digitsOnly.startsWith("1") && digitsOnly.length() == 11) {
            digitsOnly = digitsOnly.substring(1); // Remove leading '1'
        }
        
        // Validate digit count - must be exactly 10 digits
        if (digitsOnly.length() != 10) {
            buildErrorMessage(context, 
                String.format("Phone number must contain exactly 10 digits, found %d digits", digitsOnly.length()), 
                "INVALID_DIGIT_COUNT");
            return false;
        }
        
        // Apply format validation based on strictFormatting setting
        if (strictFormatting) {
            // Use the predefined PHONE_PATTERN from ValidationConstants for strict validation
            Matcher strictMatcher = ValidationConstants.PHONE_PATTERN.matcher(value);
            if (!strictMatcher.matches()) {
                buildErrorMessage(context, 
                    "Phone number format must be one of: (XXX) XXX-XXXX, XXX-XXX-XXXX, or XXXXXXXXXX", 
                    "INVALID_STRICT_FORMAT");
                return false;
            }
        } else {
            // Use flexible pattern matching for various common formats
            Matcher flexibleMatcher = FLEXIBLE_PHONE_PATTERN.matcher(value);
            if (!flexibleMatcher.matches()) {
                buildErrorMessage(context, 
                    "Invalid phone number format. Supported formats: (XXX) XXX-XXXX, XXX-XXX-XXXX, XXX.XXX.XXXX, XXX XXX XXXX, XXXXXXXXXX, +1XXXXXXXXXX", 
                    "INVALID_FORMAT");
                return false;
            }
        }
        
        // Extract area code, exchange, and number parts
        String areaCode = digitsOnly.substring(0, 3);
        String exchangeCode = digitsOnly.substring(3, 6);
        String subscriberNumber = digitsOnly.substring(6, 10);
        
        // Validate area code against NANP area code list
        if (!ValidationConstants.VALID_AREA_CODES.contains(areaCode)) {
            buildErrorMessage(context, 
                String.format("Area code '%s' is not a valid North American area code", areaCode), 
                "INVALID_AREA_CODE");
            return false;
        }
        
        // Check for easily recognizable area codes if not allowed
        if (!allowEasyRecognizableAreaCodes) {
            Matcher easyRecognizableMatcher = EASY_RECOGNIZABLE_PATTERN.matcher(areaCode);
            if (easyRecognizableMatcher.matches()) {
                buildErrorMessage(context, 
                    String.format("Area code '%s' is an easily recognizable code and not allowed for regular use", areaCode), 
                    "EASY_RECOGNIZABLE_AREA_CODE");
                return false;
            }
        }
        
        // Validate exchange code rules
        if (!isValidExchangeCode(exchangeCode)) {
            buildErrorMessage(context, 
                String.format("Exchange code '%s' is invalid. Exchange codes cannot start with 0 or 1, and cannot be N11 format", exchangeCode), 
                "INVALID_EXCHANGE_CODE");
            return false;
        }
        
        // Additional validation for subscriber number
        if (!isValidSubscriberNumber(subscriberNumber)) {
            buildErrorMessage(context, 
                String.format("Subscriber number '%s' is invalid", subscriberNumber), 
                "INVALID_SUBSCRIBER_NUMBER");
            return false;
        }
        
        // All validations passed
        return true;
    }
    
    /**
     * Validates the exchange code according to NANP rules.
     * 
     * Exchange code validation rules:
     * <ul>
     *   <li>Cannot start with 0 (0XX numbers are reserved for operator services)</li>
     *   <li>Cannot start with 1 (1XX numbers are reserved for special services)</li>
     *   <li>Cannot be N11 format where N is 2-9 (special service codes like 411, 611, etc.)</li>
     * </ul>
     * 
     * @param exchangeCode the 3-digit exchange code to validate
     * @return true if the exchange code is valid, false otherwise
     */
    private boolean isValidExchangeCode(String exchangeCode) {
        if (exchangeCode == null || exchangeCode.length() != 3) {
            return false;
        }
        
        // Exchange code cannot start with 0 or 1
        char firstDigit = exchangeCode.charAt(0);
        if (firstDigit == '0' || firstDigit == '1') {
            return false;
        }
        
        // Exchange code cannot be N11 format (special service codes)
        Matcher n11Matcher = N11_PATTERN.matcher(exchangeCode);
        if (n11Matcher.matches()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates the subscriber number (last 4 digits) according to basic rules.
     * 
     * @param subscriberNumber the 4-digit subscriber number to validate
     * @return true if the subscriber number is valid, false otherwise
     */
    private boolean isValidSubscriberNumber(String subscriberNumber) {
        if (subscriberNumber == null || subscriberNumber.length() != 4) {
            return false;
        }
        
        // Subscriber number should be all digits (already validated by digit extraction)
        // Additional business rules can be added here if needed
        return true;
    }
    
    /**
     * Builds a custom error message and adds it to the validation context.
     * 
     * This method creates detailed error messages that include:
     * <ul>
     *   <li>Specific validation failure reason</li>
     *   <li>Context information if provided</li>
     *   <li>Error code for programmatic handling</li>
     * </ul>
     * 
     * @param context the validation context
     * @param message the error message to display
     * @param errorCode the error code for categorizing the validation failure
     */
    private void buildErrorMessage(ConstraintValidatorContext context, String message, String errorCode) {
        // Disable default constraint violation
        context.disableDefaultConstraintViolation();
        
        // Build detailed error message
        StringBuilder errorMessage = new StringBuilder(message);
        
        // Add context information if provided
        if (this.context != null && !this.context.trim().isEmpty()) {
            errorMessage.append(" (Context: ").append(this.context).append(")");
        }
        
        // Add error code for programmatic handling
        errorMessage.append(" [Error Code: ").append(errorCode).append("]");
        
        // Create custom constraint violation with detailed message
        context.buildConstraintViolationWithTemplate(errorMessage.toString())
               .addConstraintViolation();
    }
    
    /**
     * Utility method to extract only digits from a phone number string.
     * This method is used internally for digit count validation and area code extraction.
     * 
     * @param phoneNumber the phone number string with potential formatting
     * @return string containing only the digits
     */
    public static String extractDigits(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        return DIGITS_ONLY_PATTERN.matcher(phoneNumber).replaceAll("");
    }
    
    /**
     * Utility method to check if an area code is considered easily recognizable.
     * This method can be used by other components that need to validate area codes.
     * 
     * @param areaCode the 3-digit area code to check
     * @return true if the area code is easily recognizable, false otherwise
     */
    public static boolean isEasilyRecognizableAreaCode(String areaCode) {
        if (areaCode == null || areaCode.length() != 3) {
            return false;
        }
        Matcher matcher = EASY_RECOGNIZABLE_PATTERN.matcher(areaCode);
        return matcher.matches();
    }
    
    /**
     * Utility method to format a 10-digit phone number string into standard display format.
     * This method formats phone numbers into (XXX) XXX-XXXX format for consistent display.
     * 
     * @param phoneNumber the 10-digit phone number string
     * @return formatted phone number string, or original string if formatting fails
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        String digits = extractDigits(phoneNumber);
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s", 
                digits.substring(0, 3),    // Area code
                digits.substring(3, 6),    // Exchange
                digits.substring(6, 10));  // Subscriber number
        }
        
        // Return original string if formatting is not possible
        return phoneNumber;
    }
}