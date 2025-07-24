package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation constraint validator for Social Security Number format validation.
 * 
 * Implements the validation logic for the {@link ValidSSN} annotation, ensuring SSN fields
 * meet the required 9-digit numeric format while supporting both formatted and unformatted
 * input patterns consistent with COBOL customer record processing.
 * 
 * This validator replicates the exact business rules from the original COBOL program
 * COACTUPC.CBL, section 1265-EDIT-US-SSN, maintaining functional equivalence during
 * the mainframe-to-Java transformation.
 * 
 * Validation Process:
 * 1. Format normalization (remove hyphens if present)
 * 2. Basic format validation (9 digits total)
 * 3. Business rule validation for each part
 * 4. Custom error message generation based on specific failure
 * 
 * @see ValidSSN
 */
public class SSNValidator implements ConstraintValidator<ValidSSN, String> {
    
    /**
     * Pattern for formatted SSN input (XXX-XX-XXXX).
     */
    private static final Pattern FORMATTED_SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");
    
    /**
     * Pattern for unformatted SSN input (9 consecutive digits).
     */
    private static final Pattern UNFORMATTED_SSN_PATTERN = Pattern.compile("^\\d{9}$");
    
    /**
     * Pattern for validating that input contains only digits and hyphens.
     */
    private static final Pattern VALID_CHARACTERS_PATTERN = Pattern.compile("^[\\d-]+$");
    
    private ValidSSN annotation;
    
    /**
     * Initializes the validator with the annotation instance.
     * 
     * @param annotation the ValidSSN annotation instance containing configuration
     */
    @Override
    public void initialize(ValidSSN annotation) {
        this.annotation = annotation;
    }
    
    /**
     * Validates the SSN value according to COBOL business rules.
     * 
     * @param value the SSN value to validate
     * @param context the validation context for custom error messages
     * @return true if valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values based on annotation configuration
        if (value == null) {
            return annotation.allowNull();
        }
        
        // Handle empty values based on annotation configuration
        if (value.trim().isEmpty()) {
            return annotation.allowEmpty();
        }
        
        // Normalize the SSN by removing hyphens
        String normalizedSSN = value.replace("-", "");
        
        // Basic format validation
        if (!isValidFormat(value)) {
            return false;
        }
        
        // Business rule validation with custom error messages
        return validateBusinessRules(normalizedSSN, context);
    }
    
    /**
     * Validates the basic format of the SSN (either XXX-XX-XXXX or XXXXXXXXX).
     * 
     * @param value the SSN value to validate
     * @return true if format is valid, false otherwise
     */
    private boolean isValidFormat(String value) {
        // Check if input contains only valid characters (digits and hyphens)
        if (!VALID_CHARACTERS_PATTERN.matcher(value).matches()) {
            return false;
        }
        
        // Check if it matches either formatted or unformatted pattern
        return FORMATTED_SSN_PATTERN.matcher(value).matches() || 
               UNFORMATTED_SSN_PATTERN.matcher(value).matches();
    }
    
    /**
     * Validates SSN business rules based on COBOL logic from COACTUPC.CBL.
     * 
     * Business Rules (from COBOL section 1265-EDIT-US-SSN):
     * - Part 1 (first 3 digits): Cannot be 000, 666, or 900-999
     * - Part 2 (middle 2 digits): Cannot be 00 (must be 01-99)
     * - Part 3 (last 4 digits): Cannot be 0000 (must be 0001-9999)
     * 
     * @param normalizedSSN the 9-digit SSN without hyphens
     * @param context the validation context for custom error messages
     * @return true if all business rules pass, false otherwise
     */
    private boolean validateBusinessRules(String normalizedSSN, ConstraintValidatorContext context) {
        // Extract the three parts of the SSN
        int part1 = Integer.parseInt(normalizedSSN.substring(0, 3));  // First 3 digits
        int part2 = Integer.parseInt(normalizedSSN.substring(3, 5));  // Middle 2 digits  
        int part3 = Integer.parseInt(normalizedSSN.substring(5, 9));  // Last 4 digits
        
        // Validate first part (XXX): Cannot be 000, 666, or 900-999
        if (part1 == 0 || part1 == 666 || (part1 >= 900 && part1 <= 999)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(annotation.firstPartMessage())
                   .addConstraintViolation();
            return false;
        }
        
        // Validate second part (XX): Cannot be 00, must be 01-99
        if (part2 == 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(annotation.secondPartMessage())
                   .addConstraintViolation();
            return false;
        }
        
        // Validate third part (XXXX): Cannot be 0000, must be 0001-9999
        if (part3 == 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(annotation.thirdPartMessage())
                   .addConstraintViolation();
            return false;
        }
        
        // All validation rules passed
        return true;
    }
}