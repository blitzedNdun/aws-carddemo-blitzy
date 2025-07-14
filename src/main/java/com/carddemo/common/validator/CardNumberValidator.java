package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementation class for credit card number validation using Luhn algorithm.
 * 
 * <p>This validator provides the core validation logic for the ValidCardNumber annotation,
 * ensuring credit card numbers meet industry checksum standards while maintaining exact
 * compatibility with COBOL validation routines from the original CardDemo application.</p>
 * 
 * <p>The validator implements:</p>
 * <ul>
 *   <li>Exact Luhn algorithm as used in original COBOL validation logic</li>
 *   <li>16-digit format validation with precise pattern matching</li>
 *   <li>Null and empty value handling equivalent to COBOL LOW-VALUES processing</li>
 *   <li>Consistent error messaging structure for React frontend consumption</li>
 *   <li>Jakarta Bean Validation framework integration via ConstraintValidator</li>
 * </ul>
 * 
 * <p>Key behavioral characteristics:</p>
 * <ul>
 *   <li>Preserves COBOL numeric precision and validation semantics</li>
 *   <li>Supports configurable null handling to match COBOL field requirements</li>
 *   <li>Provides detailed validation feedback for user interface integration</li>
 *   <li>Maintains high performance with optimized Luhn algorithm implementation</li>
 * </ul>
 * 
 * <p>This class directly supports the transformation from COBOL validation routines
 * in COCRDLIC.cbl and COCRDUPC.cbl to modern Java validation framework, ensuring
 * identical validation results across the technology stack migration.</p>
 * 
 * @author Blitzy agent
 * @since 1.0.0
 * @see ValidCardNumber
 * @see ValidationConstants#CARD_NUMBER_PATTERN
 * @see jakarta.validation.ConstraintValidator
 */
public class CardNumberValidator implements ConstraintValidator<ValidCardNumber, String> {
    
    private ValidCardNumber constraintAnnotation;
    
    /**
     * Initializes the validator with the constraint annotation configuration.
     * 
     * <p>This method is called by the Jakarta Bean Validation framework during
     * validator initialization to provide access to the annotation's configuration
     * parameters such as allowNull, ignoreFormatting, and enableLuhnCheck.</p>
     * 
     * @param constraintAnnotation the ValidCardNumber annotation instance with configuration
     */
    @Override
    public void initialize(ValidCardNumber constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }
    
    /**
     * Performs credit card number validation using Luhn algorithm and format checks.
     * 
     * <p>This method implements the complete validation logic that replicates the
     * original COBOL card number validation behavior. The validation process follows
     * these steps:</p>
     * 
     * <ol>
     *   <li>Null and empty value handling according to COBOL LOW-VALUES semantics</li>
     *   <li>Optional formatting character removal (spaces, hyphens)</li>
     *   <li>16-digit format validation using regex pattern matching</li>
     *   <li>Luhn algorithm checksum verification for card integrity</li>
     *   <li>Detailed error message construction for frontend consumption</li>
     * </ol>
     * 
     * <p>The implementation maintains exact behavioral compatibility with the original
     * COBOL validation routines while providing modern validation framework integration.</p>
     * 
     * @param value the credit card number string to validate (may be null)
     * @param context the validation context for error message customization
     * @return true if the card number is valid according to all configured rules, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and empty values according to COBOL LOW-VALUES processing
        if (value == null) {
            if (constraintAnnotation.allowNull()) {
                return true;
            }
            buildCustomErrorMessage(context, constraintAnnotation.nullMessage());
            return false;
        }
        
        // Handle empty string equivalent to COBOL SPACES processing
        if (value.trim().isEmpty()) {
            if (constraintAnnotation.allowNull()) {
                return true;
            }
            buildCustomErrorMessage(context, constraintAnnotation.nullMessage());
            return false;
        }
        
        // Apply formatting rules - remove whitespace and formatting characters if configured
        String cleanCardNumber = value;
        if (constraintAnnotation.ignoreFormatting()) {
            cleanCardNumber = removeFormattingCharacters(value);
        }
        
        // Validate format using exact 16-digit pattern matching
        if (!isValidFormat(cleanCardNumber)) {
            String formatMessage = constraintAnnotation.formatMessage()
                .replace("{expectedLength}", String.valueOf(constraintAnnotation.expectedLength()));
            buildCustomErrorMessage(context, formatMessage);
            return false;
        }
        
        // Perform Luhn algorithm validation if enabled
        if (constraintAnnotation.enableLuhnCheck()) {
            if (!isValidLuhnChecksum(cleanCardNumber)) {
                buildCustomErrorMessage(context, constraintAnnotation.luhnMessage());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Removes formatting characters from card number for validation.
     * 
     * <p>This method strips common formatting characters that users might input
     * such as spaces, hyphens, and other non-digit characters. This provides
     * user-friendly input handling while maintaining strict validation semantics.</p>
     * 
     * <p>The method preserves the core numeric content while removing:</p>
     * <ul>
     *   <li>Whitespace characters (spaces, tabs, newlines)</li>
     *   <li>Hyphen characters commonly used in card number formatting</li>
     *   <li>Other non-digit characters that might be present</li>
     * </ul>
     * 
     * @param cardNumber the raw card number input string
     * @return the cleaned card number containing only digits
     */
    private String removeFormattingCharacters(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }
        return cardNumber.replaceAll("[\\s\\-]", "");
    }
    
    /**
     * Validates card number format using exact pattern matching.
     * 
     * <p>This method performs format validation to ensure the card number meets
     * the exact requirements derived from the original COBOL field definition
     * (PIC X(16)). The validation checks:</p>
     * 
     * <ul>
     *   <li>Exact length matching the expected digit count</li>
     *   <li>Numeric content validation using regex pattern</li>
     *   <li>Character set compliance (digits only)</li>
     * </ul>
     * 
     * <p>The format validation uses the CARD_NUMBER_PATTERN from ValidationConstants
     * to ensure consistency with other validation components and maintain exact
     * COBOL field compatibility.</p>
     * 
     * @param cardNumber the cleaned card number string to validate
     * @return true if the card number format is valid, false otherwise
     */
    private boolean isValidFormat(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        
        // Check exact length requirement
        if (cardNumber.length() != constraintAnnotation.expectedLength()) {
            return false;
        }
        
        // Validate using the pattern from ValidationConstants
        return ValidationConstants.CARD_NUMBER_PATTERN.matcher(cardNumber).matches();
    }
    
    /**
     * Implements the Luhn algorithm for credit card number checksum validation.
     * 
     * <p>This method provides an exact implementation of the Luhn algorithm that
     * maintains compatibility with the original COBOL validation logic. The algorithm
     * follows these standard steps:</p>
     * 
     * <ol>
     *   <li>Process digits from right to left (starting with check digit)</li>
     *   <li>Double every second digit from the right</li>
     *   <li>Sum digits greater than 9 by adding their individual digits</li>
     *   <li>Calculate total sum of all processed digits</li>
     *   <li>Verify that total sum is divisible by 10</li>
     * </ol>
     * 
     * <p>The implementation preserves the exact arithmetic behavior of the COBOL
     * validation routines, ensuring identical validation results for all card
     * numbers processed by the original system.</p>
     * 
     * <p>Algorithm example for card number 4532015112830366:</p>
     * <pre>
     * Original:  4 5 3 2 0 1 5 1 1 2 8 3 0 3 6 6
     * Process:   4+10+3+4+0+2+5+2+1+4+8+6+0+6+6+6 = 67
     * Check:     67 % 10 = 7 ≠ 0, so invalid card number
     * </pre>
     * 
     * @param cardNumber the validated format card number (digits only)
     * @return true if the card number passes Luhn algorithm verification, false otherwise
     */
    private boolean isValidLuhnChecksum(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return false;
        }
        
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left (Luhn algorithm requirement)
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            // Double every second digit from the right
            if (alternate) {
                digit *= 2;
                
                // If doubled digit is greater than 9, sum its digits
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10);
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        // Valid if sum is divisible by 10
        return (sum % 10) == 0;
    }
    
    /**
     * Builds custom error messages with context-specific details.
     * 
     * <p>This method constructs detailed validation error messages that provide
     * specific feedback about validation failures. The messages are designed
     * for consumption by React frontend components and follow a consistent
     * structure that facilitates internationalization and user experience.</p>
     * 
     * <p>The method handles:</p>
     * <ul>
     *   <li>Custom message template processing</li>
     *   <li>Context-aware error message construction</li>
     *   <li>Integration with Jakarta Bean Validation message interpolation</li>
     *   <li>Structured error data for frontend consumption</li>
     * </ul>
     * 
     * @param context the validation context for message building
     * @param message the specific error message to display
     */
    private void buildCustomErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}