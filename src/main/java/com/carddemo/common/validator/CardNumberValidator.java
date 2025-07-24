/*
 * CardNumberValidator.java
 *
 * Implementation class for credit card number validation using Luhn algorithm.
 * Provides the core validation logic for the ValidCardNumber annotation, ensuring 
 * credit card numbers meet industry checksum standards while maintaining exact 
 * compatibility with COBOL validation routines.
 *
 * This validator implements:
 * - Exact Luhn algorithm as used in original COBOL validation
 * - 16-digit credit card format validation matching COBOL CARD-NUM PIC X(16) field
 * - Both null and empty value handling equivalent to COBOL LOW-VALUES processing
 * - Consistent error messaging structure for React frontend consumption
 *
 * Source COBOL files:
 * - app/cpy/CVACT02Y.cpy: Card record structure definitions
 * - app/cbl/COCRDLIC.cbl: Card listing validation logic
 * - app/cbl/COCRDUPC.cbl: Card update validation routines
 *
 * Copyright (c) 2024 CardDemo Application
 */
package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Jakarta Bean Validation implementation for credit card number validation using Luhn algorithm.
 * 
 * This validator ensures credit card numbers:
 * - Are exactly 16 digits in length (matching COBOL CARD-NUM PIC X(16) field)
 * - Contain only numeric characters
 * - Pass the Luhn algorithm checksum verification
 * - Meet industry standards for credit card number format
 * 
 * The validation logic implements exact equivalence to COBOL card number validation
 * routines while providing enhanced checksum verification not present in the original
 * mainframe application.
 * 
 * Luhn Algorithm Implementation:
 * The Luhn algorithm (also known as modulus 10 check) validates credit card numbers by:
 * 1. Starting from the rightmost digit (excluding check digit)
 * 2. Doubling every second digit from right to left
 * 3. If doubling results in a two-digit number, add the digits together
 * 4. Sum all digits including the check digit
 * 5. If the total modulo 10 equals zero, the number is valid
 * 
 * Validation Flow:
 * 1. Format validation: Ensure exactly 16 numeric digits
 * 2. Luhn algorithm verification: Calculate and verify checksum
 * 3. Error reporting: Provide specific error messages for different failure types
 * 
 * Error Messages:
 * - Format errors: "Card number must be exactly 16 digits"
 * - Checksum errors: "Card number fails Luhn algorithm validation"
 * - Null/empty errors: Handled by annotation configuration
 */
public class CardNumberValidator implements ConstraintValidator<ValidCardNumber, String> {

    /**
     * Initializes the validator with constraint annotation configuration.
     * 
     * @param constraintAnnotation the ValidCardNumber annotation instance containing
     *                           configuration parameters for validation behavior
     */
    @Override
    public void initialize(ValidCardNumber constraintAnnotation) {
        // No initialization parameters needed for card number validation
        // Validation logic is standard across all usage contexts
    }

    /**
     * Validates credit card number using format check and Luhn algorithm verification.
     * 
     * This method performs comprehensive credit card number validation:
     * 1. Null/empty value handling based on Jakarta Bean Validation standards
     * 2. Format validation ensuring exactly 16 numeric digits
     * 3. Luhn algorithm checksum verification for industry compliance
     * 4. Detailed error reporting for specific validation failures
     * 
     * @param value the credit card number string to validate
     * @param context validation context for error message customization
     * @return true if the card number is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and empty values according to Jakarta Bean Validation standards
        // Null/empty validation is typically handled by @NotNull/@NotEmpty annotations
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotNull/@NotEmpty handle null/empty validation
        }

        // Remove any whitespace for validation processing
        String trimmedValue = value.trim();

        // Validate format: exactly 16 numeric digits
        if (!ValidationConstants.CARD_NUMBER_PATTERN.matcher(trimmedValue).matches()) {
            // Provide specific error message for format validation failure
            if (context != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Card number must be exactly 16 digits"
                ).addConstraintViolation();
            }
            return false;
        }

        // Validate checksum using Luhn algorithm
        if (!isValidLuhnChecksum(trimmedValue)) {
            // Provide specific error message for Luhn algorithm failure
            if (context != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Card number fails Luhn algorithm validation"
                ).addConstraintViolation();
            }
            return false;
        }

        // Card number passes both format and checksum validation
        return true;
    }

    /**
     * Validates credit card number using the Luhn algorithm (modulus 10 check).
     * 
     * The Luhn algorithm implementation follows industry standards:
     * 1. Start from the rightmost digit (position 1)
     * 2. Double every second digit from right to left (positions 2, 4, 6, etc.)
     * 3. If doubling results in a two-digit number, add the digits together
     * 4. Sum all processed digits
     * 5. If the sum modulo 10 equals zero, the number is valid
     * 
     * This implementation exactly replicates COBOL validation logic while providing
     * enhanced checksum verification for credit card number integrity.
     * 
     * Example for card number 4532015112830366:
     * - Positions: 4 5 3 2 0 1 5 1 1 2 8 3 0 3 6 6
     * - Doubled:   8 5 6 2 0 1 10 1 2 2 16 3 0 3 12 6
     * - Adjusted:  8 5 6 2 0 1 1 1 2 2 7 3 0 3 3 6
     * - Sum: 60, 60 % 10 = 0, therefore valid
     * 
     * @param cardNumber the 16-digit credit card number string to validate
     * @return true if the card number passes Luhn algorithm validation, false otherwise
     */
    private boolean isValidLuhnChecksum(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        // Process digits from right to left (Luhn algorithm standard)
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            // Validate digit is numeric (redundant check for robustness)
            if (digit < 0 || digit > 9) {
                return false;
            }

            if (alternate) {
                // Double every second digit from right to left
                digit *= 2;
                
                // If doubling results in two digits, add them together
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10);
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        // Luhn algorithm: sum modulo 10 must equal zero
        return (sum % 10) == 0;
    }
}