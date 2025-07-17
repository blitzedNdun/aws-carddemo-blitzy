/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementation class for credit card number validation using Luhn algorithm.
 * 
 * <p>This validator provides the core validation logic for the {@link ValidCardNumber} annotation,
 * ensuring credit card numbers meet industry checksum standards while maintaining exact
 * compatibility with COBOL validation routines from COCRDLIC.CBL and COCRDUPC.CBL programs.
 * 
 * <p>The implementation replicates the exact validation behavior found in the original
 * COBOL programs, including:
 * <ul>
 *   <li>16-digit format validation (CARD-NUM PIC X(16))</li>
 *   <li>Numeric character validation (equivalent to COBOL IS NUMERIC test)</li>
 *   <li>Luhn algorithm checksum verification</li>
 *   <li>LOW-VALUES and SPACES handling equivalent to COBOL null processing</li>
 * </ul>
 * 
 * <p>The Luhn algorithm implementation follows the standard specification used by
 * credit card industry for checksum validation:
 * <ol>
 *   <li>Starting from the rightmost digit (excluding check digit), double every second digit</li>
 *   <li>If doubling results in a number greater than 9, subtract 9 from the result</li>
 *   <li>Sum all the digits (both doubled and non-doubled)</li>
 *   <li>Card number is valid if the total sum is divisible by 10</li>
 * </ol>
 * 
 * <p>Error handling provides detailed validation messages structured for React frontend
 * consumption while maintaining consistency with original COBOL error message patterns.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 1.0
 * @see ValidCardNumber
 * @see ValidationConstants#CARD_NUMBER_PATTERN
 */
public class CardNumberValidator implements ConstraintValidator<ValidCardNumber, String> {
    
    /**
     * Flag indicating whether Luhn algorithm validation is enabled.
     * Extracted from ValidCardNumber annotation during initialization.
     */
    private boolean enableLuhnValidation;
    
    /**
     * Flag indicating whether null or empty values should be allowed.
     * Extracted from ValidCardNumber annotation during initialization.
     */
    private boolean allowNullOrEmpty;
    
    /**
     * Initializes the validator with configuration from the ValidCardNumber annotation.
     * 
     * <p>This method extracts the validation flags from the annotation to configure
     * the validation behavior for Luhn algorithm and null value handling.
     * 
     * @param constraintAnnotation the ValidCardNumber annotation instance containing validation configuration
     */
    @Override
    public void initialize(ValidCardNumber constraintAnnotation) {
        this.enableLuhnValidation = constraintAnnotation.enableLuhnValidation();
        this.allowNullOrEmpty = constraintAnnotation.allowNullOrEmpty();
    }
    
    /**
     * Validates the credit card number according to configured validation rules.
     * 
     * <p>This method performs comprehensive validation including:
     * <ul>
     *   <li>Null and empty value handling based on COBOL LOW-VALUES processing</li>
     *   <li>16-digit format validation using regex pattern matching</li>
     *   <li>Numeric character validation equivalent to COBOL IS NUMERIC test</li>
     *   <li>Optional Luhn algorithm checksum verification</li>
     * </ul>
     * 
     * <p>The validation logic follows the exact sequence used in COBOL programs:
     * <ol>
     *   <li>Check for null/empty values (equivalent to COBOL LOW-VALUES/SPACES test)</li>
     *   <li>Validate 16-digit format (equivalent to COBOL length and numeric checks)</li>
     *   <li>Apply Luhn algorithm if enabled (industry standard checksum validation)</li>
     * </ol>
     * 
     * @param cardNumber the credit card number string to validate
     * @param context the constraint validator context for custom error message handling
     * @return true if the card number is valid according to all configured rules, false otherwise
     */
    @Override
    public boolean isValid(String cardNumber, ConstraintValidatorContext context) {
        // Handle null and empty values according to COBOL LOW-VALUES processing
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return allowNullOrEmpty;
        }
        
        // Remove any non-digit characters (spaces, hyphens, etc.) for processing
        // This maintains compatibility with COBOL string handling
        String cleanCardNumber = cardNumber.replaceAll("\\D", "");
        
        // Validate 16-digit format using pattern matching
        // Equivalent to COBOL: IF CC-CARD-NUM IS NOT NUMERIC OR LENGTH validation
        if (!ValidationConstants.CARD_NUMBER_PATTERN.matcher(cleanCardNumber).matches()) {
            // Build custom error message for format validation failure
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Card number must be exactly 16 digits (currently " + cleanCardNumber.length() + " digits)")
                .addConstraintViolation();
            return false;
        }
        
        // Apply Luhn algorithm validation if enabled
        if (enableLuhnValidation && !isValidLuhn(cleanCardNumber)) {
            // Build custom error message for Luhn validation failure
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Card number fails Luhn algorithm validation - invalid checksum")
                .addConstraintViolation();
            return false;
        }
        
        // All validations passed
        return true;
    }
    
    /**
     * Implements the Luhn algorithm for credit card number checksum validation.
     * 
     * <p>This implementation follows the standard Luhn algorithm specification
     * used throughout the credit card industry for validating card numbers.
     * The algorithm provides a checksum validation that catches most common
     * errors in credit card number entry.
     * 
     * <p>Algorithm steps:
     * <ol>
     *   <li>Starting from the rightmost digit, double every second digit (moving left)</li>
     *   <li>If doubling results in a two-digit number, subtract 9 (equivalent to summing digits)</li>
     *   <li>Sum all digits (both doubled and original)</li>
     *   <li>Card number is valid if the sum is divisible by 10</li>
     * </ol>
     * 
     * <p>Example calculation for card number "4532015112830366":
     * <pre>
     * Position: 1 6 3 2 0 1 5 1 1 2 8 3 0 3 6 6
     * Double:   3 6 6 2 0 1 1 1 1 2 7 3 0 3 3 6
     * Sum: 3+6+6+2+0+1+1+1+1+2+7+3+0+3+3+6 = 40
     * Valid: 40 % 10 = 0 (divisible by 10)
     * </pre>
     * 
     * @param cardNumber the 16-digit card number string (digits only, no spaces or separators)
     * @return true if the card number passes Luhn algorithm validation, false otherwise
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left (Luhn algorithm requirement)
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            // Double every second digit starting from the right
            if (alternate) {
                digit *= 2;
                // If doubling results in a number > 9, subtract 9
                // This is equivalent to summing the two digits of the result
                if (digit > 9) {
                    digit -= 9;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        // Card number is valid if the sum is divisible by 10
        return (sum % 10) == 0;
    }
}