package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation implementation for Social Security Number validation.
 * <p>
 * This validator implements the exact SSN validation logic from the COBOL programs COACTVWC and 
 * COACTUPC, maintaining functional equivalence with the original mainframe application. The 
 * validation replicates the 1265-EDIT-US-SSN paragraph from COACTUPC.cbl with identical business 
 * rules and error conditions.
 * </p>
 * 
 * <h3>COBOL-Equivalent Validation Rules:</h3>
 * <ul>
 *   <li><strong>Format Acceptance:</strong> Supports both formatted (XXX-XX-XXXX) and unformatted (XXXXXXXXX) input</li>
 *   <li><strong>Input Normalization:</strong> Removes hyphens and spaces for validation, preserving COBOL input handling</li>
 *   <li><strong>9-Digit Requirement:</strong> Must be exactly 9 digits after normalization (CUST-SSN PIC 9(09))</li>
 *   <li><strong>Part 1 Validation:</strong> First 3 digits cannot be 000, 666, or 900-999 (INVALID-SSN-PART1)</li>
 *   <li><strong>Part 2 Validation:</strong> Middle 2 digits must be 01-99 (cannot be 00)</li>
 *   <li><strong>Part 3 Validation:</strong> Last 4 digits must be 0001-9999 (cannot be 0000)</li>
 * </ul>
 * 
 * <h3>Input Processing:</h3>
 * <p>
 * The validator normalizes input by removing common formatting characters (hyphens and spaces),
 * allowing users to enter SSNs in either formatted or unformatted style. This matches the
 * flexible input handling in the COBOL screen processing logic.
 * </p>
 * 
 * <h3>Validation Flow:</h3>
 * <ol>
 *   <li>Check for null/empty values based on annotation configuration</li>
 *   <li>Normalize input by removing hyphens and spaces</li>
 *   <li>Validate 9-digit numeric pattern using ValidationConstants.SSN_PATTERN</li>
 *   <li>Apply three-part business rule validation matching COBOL logic</li>
 *   <li>Return detailed error messages for specific validation failures</li>
 * </ol>
 * 
 * <h3>COBOL Source Mapping:</h3>
 * <pre>
 * COBOL Code (COACTUPC.cbl lines 2431-2491):
 * 1265-EDIT-US-SSN.
 *   Format xxx-xx-xxxx
 *   Part1: should have 3 digits, not 000, 666, or 900-999
 *   Part2: should have 2 digits from 01 to 99  
 *   Part3: should have 4 digits from 0001 to 9999
 * </pre>
 * 
 * @author Blitzy CardDemo Platform
 * @version 1.0
 * @since 1.0
 * 
 * @see ValidSSN
 * @see ValidationConstants#SSN_PATTERN
 */
public class SSNValidator implements ConstraintValidator<ValidSSN, String> {
    
    /**
     * Pattern for formatted SSN input (XXX-XX-XXXX)
     */
    private static final Pattern FORMATTED_SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");
    
    /**
     * Pattern to remove formatting characters for normalization
     */
    private static final Pattern FORMATTING_CHARS_PATTERN = Pattern.compile("[\\s\\-]");
    
    /**
     * Configuration flag indicating if formatted input is required
     */
    private boolean requireFormatted;
    
    /**
     * Configuration flag indicating if null values are allowed
     */
    private boolean allowNull;
    
    /**
     * Configuration flag indicating if empty strings are allowed
     */
    private boolean allowEmpty;
    
    /**
     * Initializes the validator with configuration from the ValidSSN annotation.
     * <p>
     * This method is called by the Jakarta Bean Validation framework to configure
     * the validator instance based on the annotation parameters.
     * </p>
     * 
     * @param constraintAnnotation the ValidSSN annotation instance containing configuration
     */
    @Override
    public void initialize(ValidSSN constraintAnnotation) {
        this.requireFormatted = constraintAnnotation.requireFormatted();
        this.allowNull = constraintAnnotation.allowNull();
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }
    
    /**
     * Validates the SSN value according to COBOL-equivalent business rules.
     * <p>
     * This method implements the complete validation logic from the COBOL 1265-EDIT-US-SSN
     * paragraph, ensuring functional equivalence with the original mainframe validation.
     * The validation maintains exact compatibility with COBOL processing patterns.
     * </p>
     * 
     * @param value the SSN value to validate (may be null or empty)
     * @param context the constraint validator context for custom error messages
     * @return true if the SSN is valid according to all business rules, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values based on configuration
        if (value == null) {
            return allowNull;
        }
        
        // Handle empty values based on configuration
        if (value.trim().isEmpty()) {
            return allowEmpty;
        }
        
        // Check formatted input requirement
        if (requireFormatted && !FORMATTED_SSN_PATTERN.matcher(value).matches()) {
            setCustomErrorMessage(context, "SSN must be in formatted XXX-XX-XXXX pattern");
            return false;
        }
        
        // Normalize input by removing formatting characters (hyphens and spaces)
        String normalizedSSN = FORMATTING_CHARS_PATTERN.matcher(value.trim()).replaceAll("");
        
        // Validate 9-digit numeric pattern using ValidationConstants
        if (!ValidationConstants.SSN_PATTERN.matcher(normalizedSSN).matches()) {
            setCustomErrorMessage(context, "SSN must be exactly 9 digits");
            return false;
        }
        
        // Apply COBOL-equivalent three-part validation
        return validateSSNParts(normalizedSSN, context);
    }
    
    /**
     * Validates SSN parts according to COBOL business rules.
     * <p>
     * This method implements the exact validation logic from COACTUPC.cbl, including
     * the INVALID-SSN-PART1 condition and the validation rules for each part of the SSN.
     * </p>
     * 
     * @param normalizedSSN the normalized 9-digit SSN string
     * @param context the constraint validator context for custom error messages
     * @return true if all parts pass validation, false otherwise
     */
    private boolean validateSSNParts(String normalizedSSN, ConstraintValidatorContext context) {
        // Extract the three parts as in COBOL WS-EDIT-US-SSN structure
        String part1 = normalizedSSN.substring(0, 3);  // First 3 digits
        String part2 = normalizedSSN.substring(3, 5);  // Middle 2 digits  
        String part3 = normalizedSSN.substring(5, 9);  // Last 4 digits
        
        // Convert to integers for validation
        int part1Int = Integer.parseInt(part1);
        int part2Int = Integer.parseInt(part2);
        int part3Int = Integer.parseInt(part3);
        
        // Validate Part 1: INVALID-SSN-PART1 validation from COBOL
        // VALUES 0, 666, 900 THRU 999 are invalid
        if (part1Int == 0 || part1Int == 666 || (part1Int >= 900 && part1Int <= 999)) {
            setCustomErrorMessage(context, 
                "SSN first 3 digits cannot be 000, 666, or between 900 and 999");
            return false;
        }
        
        // Validate Part 2: Middle 2 digits must be 01-99 (cannot be 00)
        if (part2Int == 0) {
            setCustomErrorMessage(context, 
                "SSN middle 2 digits must be between 01 and 99");
            return false;
        }
        
        // Validate Part 3: Last 4 digits must be 0001-9999 (cannot be 0000)
        if (part3Int == 0) {
            setCustomErrorMessage(context, 
                "SSN last 4 digits must be between 0001 and 9999");
            return false;
        }
        
        // All validations passed
        return true;
    }
    
    /**
     * Sets a custom error message in the validation context.
     * <p>
     * This method disables the default constraint violation and adds a custom message,
     * allowing for specific error feedback that matches COBOL validation message patterns.
     * </p>
     * 
     * @param context the constraint validator context
     * @param message the custom error message to set
     */
    private void setCustomErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}