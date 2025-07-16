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
 * <p>This validator provides the core validation logic for the ValidCardNumber annotation,
 * ensuring credit card numbers meet industry checksum standards while maintaining exact
 * compatibility with COBOL validation routines from COCRDLIC.CBL and COCRDUPC.CBL.
 * 
 * <p>The validator implements the following validation rules:
 * <ul>
 *   <li>Format validation: Must be exactly 16 digits (matching COBOL CARD-NUM PIC X(16))</li>
 *   <li>Numeric validation: Must contain only digits 0-9</li>
 *   <li>Luhn algorithm validation: Must pass checksum verification</li>
 *   <li>Null/empty handling: Configurable through annotation parameters</li>
 * </ul>
 * 
 * <p>The Luhn algorithm implementation follows the industry standard specification:
 * <ol>
 *   <li>Starting from the rightmost digit, double every second digit</li>
 *   <li>If doubling results in a number greater than 9, subtract 9</li>
 *   <li>Sum all the digits</li>
 *   <li>Card number is valid if the total is divisible by 10</li>
 * </ol>
 * 
 * <p>This implementation maintains exact compatibility with COBOL card validation
 * patterns while providing structured error messages for React frontend integration.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 1.0
 */
public class CardNumberValidator implements ConstraintValidator<ValidCardNumber, String> {
    
    private boolean enableLuhnValidation;
    private boolean allowNullOrEmpty;
    
    /**
     * Initializes the validator with annotation configuration.
     * 
     * @param constraintAnnotation the annotation instance with configuration
     */
    @Override
    public void initialize(ValidCardNumber constraintAnnotation) {
        this.enableLuhnValidation = constraintAnnotation.enableLuhnValidation();
        this.allowNullOrEmpty = constraintAnnotation.allowNullOrEmpty();
    }
    
    /**
     * Validates the credit card number according to configured rules.
     * 
     * @param cardNumber the card number to validate
     * @param context the validation context
     * @return true if the card number is valid, false otherwise
     */
    @Override
    public boolean isValid(String cardNumber, ConstraintValidatorContext context) {
        // Handle null and empty values according to configuration
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return allowNullOrEmpty;
        }
        
        // Remove any whitespace and validate format
        String cleanCardNumber = cardNumber.replaceAll("\\s+", "");
        
        // Validate format using pattern from ValidationConstants
        if (!ValidationConstants.CARD_NUMBER_PATTERN.matcher(cleanCardNumber).matches()) {
            return false;
        }
        
        // Perform Luhn algorithm validation if enabled
        if (enableLuhnValidation && !isValidLuhn(cleanCardNumber)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Implements the Luhn algorithm for credit card checksum validation.
     * 
     * <p>This method replicates the exact validation logic used in COBOL card
     * validation routines, ensuring compatibility with the original mainframe
     * validation behavior.
     * 
     * <p>The algorithm works as follows:
     * <ol>
     *   <li>Starting from the rightmost digit, double every second digit</li>
     *   <li>If doubling results in a number greater than 9, subtract 9</li>
     *   <li>Sum all the digits</li>
     *   <li>Card number is valid if the total is divisible by 10</li>
     * </ol>
     * 
     * @param cardNumber the 16-digit card number to validate
     * @return true if the card number passes Luhn validation, false otherwise
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        // Valid if sum is divisible by 10
        return (sum % 10) == 0;
    }
}