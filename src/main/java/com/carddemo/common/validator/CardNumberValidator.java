package com.carddemo.common.validator;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Jakarta Bean Validation constraint validator for credit card numbers.
 * 
 * <p>This validator implements the Luhn algorithm for credit card number
 * validation, providing industry-standard checksum verification equivalent
 * to COBOL card number validation routines while integrating with Spring
 * Boot microservices architecture.</p>
 * 
 * <p>The validator performs the following validation steps:</p>
 * <ol>
 *   <li>Null and empty value handling based on allowNull configuration</li>
 *   <li>Format normalization (removing whitespace and formatting characters)</li>
 *   <li>Length validation (default 16 digits matching COBOL PIC X(16))</li>
 *   <li>Numeric character validation</li>
 *   <li>Luhn algorithm checksum verification</li>
 * </ol>
 * 
 * <p>The Luhn algorithm implementation follows the standard specification:</p>
 * <ul>
 *   <li>Double every second digit from right to left</li>
 *   <li>Sum all digits (reducing double-digit results to single digits)</li>
 *   <li>Check if the total sum is divisible by 10</li>
 * </ul>
 * 
 * @author Blitzy agent
 * @since 1.0.0
 * @see ValidCardNumber
 * @see jakarta.validation.ConstraintValidator
 */
public class CardNumberValidator implements ConstraintValidator<ValidCardNumber, String> {
    
    /**
     * Pattern for validating numeric-only strings.
     * Compiled once for performance optimization.
     */
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");
    
    /**
     * Pattern for identifying formatting characters to remove.
     * Includes spaces, hyphens, and other common card number separators.
     */
    private static final Pattern FORMATTING_PATTERN = Pattern.compile("[\\s\\-]+");
    
    /**
     * Configuration flag for allowing null values.
     */
    private boolean allowNull;
    
    /**
     * Configuration flag for ignoring formatting characters.
     */
    private boolean ignoreFormatting;
    
    /**
     * Expected card number length after formatting removal.
     */
    private int expectedLength;
    
    /**
     * Configuration flag for performing Luhn algorithm validation.
     */
    private boolean enableLuhnCheck;
    
    /**
     * Custom error message for null values.
     */
    private String nullMessage;
    
    /**
     * Custom error message for format validation failures.
     */
    private String formatMessage;
    
    /**
     * Custom error message for Luhn algorithm validation failures.
     */
    private String luhnMessage;
    
    /**
     * Initializes the validator with configuration from the ValidCardNumber annotation.
     * 
     * <p>This method is called by the Jakarta Bean Validation framework during
     * constraint initialization and extracts all configuration parameters
     * from the annotation for use during validation.</p>
     * 
     * @param constraintAnnotation the ValidCardNumber annotation instance
     */
    @Override
    public void initialize(ValidCardNumber constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.ignoreFormatting = constraintAnnotation.ignoreFormatting();
        this.expectedLength = constraintAnnotation.expectedLength();
        this.enableLuhnCheck = constraintAnnotation.enableLuhnCheck();
        this.nullMessage = constraintAnnotation.nullMessage();
        this.formatMessage = constraintAnnotation.formatMessage();
        this.luhnMessage = constraintAnnotation.luhnMessage();
    }
    
    /**
     * Validates the credit card number according to configured rules.
     * 
     * <p>This method performs comprehensive validation including null handling,
     * format normalization, length validation, numeric validation, and
     * Luhn algorithm verification. It provides specific error messages
     * for different validation failure scenarios.</p>
     * 
     * @param value the card number to validate
     * @param context the validation context for error reporting
     * @return true if the card number is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and empty values
        if (value == null || value.trim().isEmpty()) {
            if (allowNull) {
                return true;
            } else {
                addCustomErrorMessage(context, nullMessage);
                return false;
            }
        }
        
        // Normalize the card number by removing formatting characters
        String normalizedValue = normalizeCardNumber(value);
        
        // Validate the normalized card number format
        if (!isValidFormat(normalizedValue)) {
            addCustomErrorMessage(context, formatMessage.replace("{expectedLength}", String.valueOf(expectedLength)));
            return false;
        }
        
        // Perform Luhn algorithm validation if enabled
        if (enableLuhnCheck && !isValidLuhn(normalizedValue)) {
            addCustomErrorMessage(context, luhnMessage);
            return false;
        }
        
        return true;
    }
    
    /**
     * Normalizes the card number by removing formatting characters.
     * 
     * <p>This method removes whitespace, hyphens, and other formatting
     * characters from the card number if ignoreFormatting is enabled.
     * This provides user-friendly input handling while maintaining
     * strict validation requirements.</p>
     * 
     * @param cardNumber the raw card number input
     * @return the normalized card number containing only digits
     */
    private String normalizeCardNumber(String cardNumber) {
        if (ignoreFormatting) {
            return FORMATTING_PATTERN.matcher(cardNumber).replaceAll("");
        }
        return cardNumber;
    }
    
    /**
     * Validates the card number format (length and numeric characters).
     * 
     * <p>This method checks that the normalized card number contains only
     * numeric characters and has the expected length. This validation
     * matches the COBOL validation logic for PIC X(16) fields with
     * numeric content validation.</p>
     * 
     * @param cardNumber the normalized card number
     * @return true if the format is valid, false otherwise
     */
    private boolean isValidFormat(String cardNumber) {
        // Check if the card number has the expected length
        if (cardNumber.length() != expectedLength) {
            return false;
        }
        
        // Check if the card number contains only numeric characters
        return NUMERIC_PATTERN.matcher(cardNumber).matches();
    }
    
    /**
     * Validates the card number using the Luhn algorithm.
     * 
     * <p>The Luhn algorithm (also known as the "modulus 10" algorithm) is
     * a checksum formula used to validate credit card numbers. The algorithm
     * was developed by Hans Peter Luhn and is widely used in the credit card
     * industry for basic validation.</p>
     * 
     * <p>Algorithm steps:</p>
     * <ol>
     *   <li>Starting from the rightmost digit, double every second digit</li>
     *   <li>If doubling results in a two-digit number, subtract 9</li>
     *   <li>Sum all the digits</li>
     *   <li>If the total sum is divisible by 10, the number is valid</li>
     * </ol>
     * 
     * @param cardNumber the normalized card number (digits only)
     * @return true if the card number passes Luhn validation, false otherwise
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            // Double every second digit from the right
            if (alternate) {
                digit *= 2;
                
                // If doubling results in a two-digit number, subtract 9
                // (equivalent to summing the digits: 10 -> 1+0 = 1, 11 -> 1+1 = 2, etc.)
                if (digit > 9) {
                    digit -= 9;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        // The card number is valid if the sum is divisible by 10
        return (sum % 10) == 0;
    }
    
    /**
     * Adds a custom error message to the validation context.
     * 
     * <p>This method disables the default error message and adds a custom
     * error message that provides more specific feedback about the validation
     * failure. This supports structured error messages compatible with
     * React frontend components.</p>
     * 
     * @param context the validation context
     * @param message the custom error message to add
     */
    private void addCustomErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}