package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator implementation for {@link ValidSSN} annotation.
 * 
 * This class implements the actual validation logic for Social Security Number
 * format validation based on COBOL patterns from the CardDemo application.
 * 
 * <p>Validation Logic (based on COACTUPC.cbl patterns):</p>
 * <ul>
 *   <li>Supports both formatted (XXX-XX-XXXX) and unformatted (XXXXXXXXX) input</li>
 *   <li>Validates 9-digit numeric format requirement</li>
 *   <li>Enforces COBOL-based SSN validation rules when strict mode is enabled</li>
 *   <li>Provides detailed error messages matching original COBOL patterns</li>
 * </ul>
 * 
 * <p>COBOL Pattern Mapping:</p>
 * <pre>
 * COBOL: 88 INVALID-SSN-PART1 VALUES 0, 666, 900 THRU 999
 * Java:  part1 == 0 || part1 == 666 || (part1 >= 900 && part1 <= 999)
 * 
 * COBOL: WS-EDIT-US-SSN-PART2-N (must be 01-99)
 * Java:  part2 >= 1 && part2 <= 99
 * 
 * COBOL: WS-EDIT-US-SSN-PART3-N (must be 0001-9999)
 * Java:  part3 >= 1 && part3 <= 9999
 * </pre>
 * 
 * @author Blitzy Agent
 * @since 1.0
 * @see ValidSSN
 */
public class SSNValidator implements ConstraintValidator<ValidSSN, String> {
    
    /**
     * Pattern for formatted SSN (XXX-XX-XXXX format).
     */
    private static final Pattern FORMATTED_SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");
    
    /**
     * Pattern for unformatted SSN (XXXXXXXXX format).
     */
    private static final Pattern UNFORMATTED_SSN_PATTERN = Pattern.compile("^\\d{9}$");
    
    /**
     * Pattern for extracting digits from any format.
     */
    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("\\D");
    
    /**
     * Configuration from the ValidSSN annotation.
     */
    private ValidSSN validSSN;
    
    /**
     * Flag indicating whether blank values are allowed.
     */
    private boolean allowBlank;
    
    /**
     * Flag indicating whether strict validation should be applied.
     */
    private boolean strict;
    
    /**
     * Initializes the validator with the annotation configuration.
     * 
     * @param validSSN the ValidSSN annotation instance
     */
    @Override
    public void initialize(ValidSSN validSSN) {
        this.validSSN = validSSN;
        this.allowBlank = validSSN.allowBlank();
        this.strict = validSSN.strict();
    }
    
    /**
     * Validates the SSN value according to the configured rules.
     * 
     * @param value the SSN value to validate
     * @param context the validation context
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null and blank values
        if (value == null || value.trim().isEmpty()) {
            return allowBlank;
        }
        
        // Check basic format first
        if (!isValidFormat(value)) {
            buildCustomMessage(context, validSSN.formatErrorMessage());
            return false;
        }
        
        // Extract digits for validation
        String digitsOnly = DIGITS_ONLY_PATTERN.matcher(value).replaceAll("");
        
        // Validate length
        if (digitsOnly.length() != 9) {
            buildCustomMessage(context, validSSN.formatErrorMessage());
            return false;
        }
        
        // If strict validation is disabled, only check basic format
        if (!strict) {
            return true;
        }
        
        // Perform strict validation based on COBOL patterns
        return performStrictValidation(digitsOnly, context);
    }
    
    /**
     * Checks if the SSN value matches expected format patterns.
     * 
     * @param value the SSN value to check
     * @return true if format is valid, false otherwise
     */
    private boolean isValidFormat(String value) {
        return FORMATTED_SSN_PATTERN.matcher(value).matches() || 
               UNFORMATTED_SSN_PATTERN.matcher(value).matches();
    }
    
    /**
     * Performs strict validation based on COBOL SSN validation rules.
     * 
     * @param digitsOnly the 9-digit SSN string
     * @param context the validation context
     * @return true if all strict validation rules pass, false otherwise
     */
    private boolean performStrictValidation(String digitsOnly, ConstraintValidatorContext context) {
        try {
            // Extract the three parts of the SSN
            int part1 = Integer.parseInt(digitsOnly.substring(0, 3));
            int part2 = Integer.parseInt(digitsOnly.substring(3, 5));
            int part3 = Integer.parseInt(digitsOnly.substring(5, 9));
            
            // Validate Part 1: First 3 digits
            // Based on COBOL: 88 INVALID-SSN-PART1 VALUES 0, 666, 900 THRU 999
            if (part1 == 0 || part1 == 666 || (part1 >= 900 && part1 <= 999)) {
                buildCustomMessage(context, validSSN.part1ErrorMessage());
                return false;
            }
            
            // Validate Part 2: Middle 2 digits (01-99)
            // Based on COBOL: WS-EDIT-US-SSN-PART2-N validation
            if (part2 < 1 || part2 > 99) {
                buildCustomMessage(context, validSSN.part2ErrorMessage());
                return false;
            }
            
            // Validate Part 3: Last 4 digits (0001-9999)
            // Based on COBOL: WS-EDIT-US-SSN-PART3-N validation
            if (part3 < 1 || part3 > 9999) {
                buildCustomMessage(context, validSSN.part3ErrorMessage());
                return false;
            }
            
            return true;
            
        } catch (NumberFormatException e) {
            // This should not happen given our format validation, but handle gracefully
            buildCustomMessage(context, validSSN.formatErrorMessage());
            return false;
        }
    }
    
    /**
     * Builds a custom error message and disables the default message.
     * 
     * @param context the validation context
     * @param message the custom error message
     */
    private void buildCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}