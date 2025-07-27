/*
 * CardNumberValidator.java
 *
 * Implementation class for credit card number validation using Luhn algorithm.
 * Provides the core validation logic for the ValidCardNumber annotation, ensuring
 * credit card numbers meet industry checksum standards while maintaining exact
 * compatibility with COBOL validation routines.
 *
 * This validator implements the Luhn algorithm exactly as it would have been
 * implemented in the original COBOL validation routines, providing:
 * - 16-digit format validation with exact pattern matching
 * - Luhn algorithm checksum verification for mathematical validity
 * - COBOL-equivalent null and empty value handling (LOW-VALUES processing)
 * - Consistent error messaging structure for React frontend consumption
 * - Integration with Jakarta Bean Validation framework
 *
 * The Luhn algorithm implementation follows the standard industry approach:
 * 1. Starting from the rightmost digit, double every second digit
 * 2. If doubling results in a two-digit number, add the digits together
 * 3. Sum all digits including the non-doubled ones
 * 4. If the total sum is divisible by 10, the number is valid
 *
 * This approach replicates the mathematical precision that would have been
 * implemented in COBOL using COMP-3 arithmetic operations with exact decimal
 * precision maintained throughout the calculation process.
 *
 * Copyright (c) 2024 CardDemo Application
 */
package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Jakarta Bean Validation constraint validator for credit card numbers.
 * 
 * Implements comprehensive credit card number validation combining format
 * verification with mathematical checksum validation using the Luhn algorithm.
 * This validator maintains exact compatibility with COBOL validation patterns
 * while providing enhanced security through checksum verification.
 * 
 * <h3>Validation Process:</h3>
 * <ol>
 *   <li><b>Null/Empty Handling:</b> Follows COBOL LOW-VALUES semantics</li>
 *   <li><b>Format Validation:</b> Ensures exactly 16 numeric digits</li>
 *   <li><b>Pattern Matching:</b> Uses compiled regex for optimal performance</li>
 *   <li><b>Luhn Algorithm:</b> Verifies mathematical checksum validity</li>
 * </ol>
 * 
 * <h3>COBOL Compatibility:</h3>
 * The implementation preserves the exact validation logic that would have been
 * used in the original COBOL card validation routines, including:
 * - Same numeric precision and decimal handling
 * - Equivalent loop iteration and arithmetic operations
 * - Identical error conditions and validation flow
 * - Compatible null value processing (COBOL LOW-VALUES → Java null/empty)
 * 
 * <h3>Error Message Integration:</h3>
 * Provides structured error messages compatible with React frontend validation
 * displays, following the established pattern for consistent user experience
 * across all form validation scenarios.
 * 
 * @see ValidCardNumber
 * @see ValidationConstants#CARD_NUMBER_PATTERN
 */
public class CardNumberValidator implements ConstraintValidator<ValidCardNumber, String> {

    /**
     * Initializes the validator instance.
     * 
     * This method is called once by the Jakarta Bean Validation framework
     * when the validator is instantiated. Currently no initialization
     * parameters are required, but the method is retained for future
     * extensibility and framework compatibility.
     * 
     * @param constraintAnnotation the annotation instance containing validation parameters
     */
    @Override
    public void initialize(ValidCardNumber constraintAnnotation) {
        // No initialization required - validator uses stateless validation logic
        // All required constants are provided by ValidationConstants class
        // Retained for Jakarta Bean Validation framework compatibility
    }

    /**
     * Validates a credit card number using format and Luhn algorithm verification.
     * 
     * This method implements the complete validation logic equivalent to what
     * would have been implemented in COBOL validation routines, including:
     * 
     * <h4>COBOL LOW-VALUES Processing:</h4>
     * In COBOL, uninitialized or empty fields contain LOW-VALUES (binary zeros).
     * This validator treats null and empty strings as equivalent to COBOL LOW-VALUES,
     * considering them valid (allowing optional fields) but not performing
     * further validation on them.
     * 
     * <h4>16-Digit Format Validation:</h4>
     * Uses the compiled CARD_NUMBER_PATTERN regex for optimal performance,
     * ensuring exactly 16 consecutive digits with no additional characters.
     * This matches the COBOL CARD-NUM PIC X(16) field specification exactly.
     * 
     * <h4>Luhn Algorithm Implementation:</h4>
     * The checksum calculation follows these steps:
     * <ol>
     *   <li>Process digits from right to left (reverse iteration)</li>
     *   <li>Double every second digit (even positions in reverse)</li>
     *   <li>If doubling produces a two-digit result, sum the digits</li>
     *   <li>Sum all processed digits</li>
     *   <li>Check if total sum is divisible by 10</li>
     * </ol>
     * 
     * This approach replicates the precision and mathematical operations
     * that would have been performed in COBOL using COMP-3 packed decimal
     * arithmetic with exact decimal preservation.
     * 
     * <h4>Performance Considerations:</h4>
     * - Uses compiled regex pattern for format validation (O(1) pattern reuse)
     * - Single-pass iteration for Luhn calculation (O(n) where n=16)
     * - Early termination on format validation failure
     * - Minimal object allocation during validation process
     * 
     * @param cardNumber the credit card number string to validate
     * @param context the constraint validator context for error message customization
     * @return true if the card number is valid (null/empty) or passes all validation checks,
     *         false if format or Luhn algorithm validation fails
     * 
     * @see ValidationConstants#CARD_NUMBER_PATTERN
     */
    @Override
    public boolean isValid(String cardNumber, ConstraintValidatorContext context) {
        // Handle null and empty values equivalent to COBOL LOW-VALUES processing
        // In COBOL, LOW-VALUES (binary zeros) in optional fields are considered valid
        // This allows for optional card number fields in forms and API requests
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return true; // Allow null/empty values (equivalent to COBOL LOW-VALUES)
        }

        // First validation: 16-digit format verification using compiled regex pattern
        // This matches the COBOL CARD-NUM PIC X(16) field specification exactly
        // and ensures only numeric characters are present with exact length requirement
        if (!ValidationConstants.CARD_NUMBER_PATTERN.matcher(cardNumber).matches()) {
            // Custom error message can be added here if needed for specific format failures
            // Currently uses the default annotation message for consistency
            return false;
        }

        // Second validation: Luhn algorithm checksum verification
        // This implements the mathematical validation that would have been performed
        // in COBOL using COMP-3 packed decimal arithmetic with exact precision
        return isValidLuhnChecksum(cardNumber);
    }

    /**
     * Implements the Luhn algorithm for credit card number checksum validation.
     * 
     * This method replicates the exact mathematical operations that would have
     * been implemented in COBOL validation routines using COMP-3 packed decimal
     * arithmetic. The algorithm provides mathematical verification that the
     * credit card number is not only properly formatted but also mathematically
     * valid according to industry standards.
     * 
     * <h4>Algorithm Details:</h4>
     * The Luhn algorithm (also known as the "modulus 10" algorithm) works by:
     * <ol>
     *   <li><b>Right-to-Left Processing:</b> Start from the rightmost digit</li>
     *   <li><b>Alternating Doubling:</b> Double every second digit (even positions when counting from right)</li>
     *   <li><b>Digit Sum Reduction:</b> If doubling produces 10 or greater, sum the digits (e.g., 16 → 1+6 = 7)</li>
     *   <li><b>Total Summation:</b> Add all processed digits together</li>
     *   <li><b>Modulus Check:</b> Valid if the total sum is divisible by 10</li>
     * </ol>
     * 
     * <h4>COBOL Implementation Equivalence:</h4>
     * This Java implementation maintains exact equivalence to COBOL arithmetic:
     * <pre>
     * COBOL Equivalent Logic:
     * MOVE 0 TO WS-CHECKSUM-TOTAL
     * PERFORM VARYING WS-INDEX FROM 16 BY -1 UNTIL WS-INDEX < 1
     *     MOVE CARD-NUM(WS-INDEX:1) TO WS-DIGIT
     *     IF WS-POSITION-FLAG = 'Y'
     *         MULTIPLY WS-DIGIT BY 2 GIVING WS-DOUBLED-DIGIT
     *         IF WS-DOUBLED-DIGIT > 9
     *             COMPUTE WS-DOUBLED-DIGIT = WS-DOUBLED-DIGIT - 9
     *         END-IF
     *         ADD WS-DOUBLED-DIGIT TO WS-CHECKSUM-TOTAL
     *     ELSE
     *         ADD WS-DIGIT TO WS-CHECKSUM-TOTAL
     *     END-IF
     *     MOVE 'Y' TO WS-POSITION-FLAG IF WS-POSITION-FLAG = 'N'
     *     MOVE 'N' TO WS-POSITION-FLAG IF WS-POSITION-FLAG = 'Y'
     * END-PERFORM
     * IF FUNCTION MOD(WS-CHECKSUM-TOTAL, 10) = 0
     *     MOVE 'VALID' TO WS-CARD-STATUS
     * ELSE
     *     MOVE 'INVALID' TO WS-CARD-STATUS
     * END-IF
     * </pre>
     * 
     * <h4>Example Calculation:</h4>
     * For card number 4532015112830366:
     * <pre>
     * Digits:    4 5 3 2 0 1 5 1 1 2 8 3 0 3 6 6
     * Positions: 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 (from right)
     * Double:    - Y - Y - Y - Y - Y - Y - Y - Y (even positions)
     * Result:    4 1 3 4 0 2 5 2 1 4 8 6 0 6 3 6
     * Sum: 4+1+3+4+0+2+5+2+1+4+8+6+0+6+3+6 = 55
     * 55 % 10 ≠ 0, so this would be invalid
     * </pre>
     * 
     * <h4>Performance Optimization:</h4>
     * - Single-pass iteration with minimal object allocation
     * - Integer arithmetic only (no floating-point operations)
     * - Early bounds checking with fixed 16-digit assumption
     * - Efficient modulus operation for final validation
     * 
     * @param cardNumber the 16-digit card number string (format already verified)
     * @return true if the card number passes Luhn algorithm validation, false otherwise
     */
    private boolean isValidLuhnChecksum(String cardNumber) {
        int checksum = 0;
        boolean isEvenPosition = false; // Track position for alternating doubling (from right)

        // Process digits from right to left (reverse iteration)
        // This matches the COBOL PERFORM VARYING ... BY -1 loop structure
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            // Extract individual digit - equivalent to COBOL CARD-NUM(WS-INDEX:1)
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            // Double every second digit when counting from the right
            // This matches COBOL alternating flag logic with WS-POSITION-FLAG
            if (isEvenPosition) {
                digit *= 2;
                
                // If doubling results in a two-digit number, add the digits together
                // This replaces COBOL's "SUBTRACT 9" optimization with explicit digit sum
                // Both approaches are mathematically equivalent:
                // - COBOL: IF WS-DOUBLED-DIGIT > 9 THEN SUBTRACT 9
                // - Java: IF digit > 9 THEN digit = (digit / 10) + (digit % 10)
                // Example: 16 → COBOL: 16-9=7, Java: 1+6=7 (same result)
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10);
                }
            }

            // Add processed digit to running total
            // Equivalent to COBOL ADD WS-DIGIT TO WS-CHECKSUM-TOTAL
            checksum += digit;
            
            // Toggle position flag for next iteration
            // Equivalent to COBOL alternating WS-POSITION-FLAG between 'Y' and 'N'
            isEvenPosition = !isEvenPosition;
        }

        // Luhn algorithm validation: checksum must be divisible by 10
        // Equivalent to COBOL IF FUNCTION MOD(WS-CHECKSUM-TOTAL, 10) = 0
        return (checksum % 10) == 0;
    }
}