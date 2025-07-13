package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator implementation for the {@link ValidSSN} annotation.
 * <p>
 * This validator implements the actual SSN validation logic that enforces the same business
 * rules as the original COBOL programs COACTVWC and COACTUPC. The validation logic is based
 * on the COBOL paragraph 1265-EDIT-US-SSN which validates SSN parts according to U.S. SSN
 * format requirements.
 * </p>
 * 
 * <h3>Validation Algorithm:</h3>
 * <ol>
 *   <li>Normalize input by removing formatting characters (hyphens)</li>
 *   <li>Validate that the result is exactly 9 digits</li>
 *   <li>Apply COBOL-equivalent business rules for each SSN part</li>
 *   <li>Return validation result with appropriate error context</li>
 * </ol>
 * 
 * <h3>COBOL Compatibility:</h3>
 * <p>
 * This implementation replicates the validation logic from COACTUPC.cbl lines 2431-2491,
 * ensuring exact compatibility with the original mainframe application's validation behavior.
 * The validation rules match the COBOL 88-level conditions and edit routines.
 * </p>
 * 
 * @author Blitzy CardDemo Platform
 * @version 1.0
 * @since 1.0
 * 
 * @see ValidSSN
 */
public class SSNValidator implements ConstraintValidator<ValidSSN, String> {
    
    /**
     * Pattern for validating formatted SSN input (XXX-XX-XXXX).
     * This pattern ensures the input matches the standard U.S. SSN format with hyphens.
     */
    private static final Pattern FORMATTED_SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");
    
    /**
     * Pattern for validating unformatted SSN input (XXXXXXXXX).
     * This pattern ensures the input is exactly 9 digits without any formatting.
     */
    private static final Pattern UNFORMATTED_SSN_PATTERN = Pattern.compile("^\\d{9}$");
    
    /**
     * Pattern for extracting digits from formatted SSN input.
     * This pattern removes all non-digit characters to normalize the input.
     */
    private static final Pattern DIGIT_EXTRACTION_PATTERN = Pattern.compile("[^0-9]");
    
    /**
     * Configuration flag indicating whether formatted input is required.
     * Set during initialization based on the annotation configuration.
     */
    private boolean requireFormatted;
    
    /**
     * Configuration flag indicating whether null values are allowed.
     * Set during initialization based on the annotation configuration.
     */
    private boolean allowNull;
    
    /**
     * Configuration flag indicating whether empty strings are allowed.
     * Set during initialization based on the annotation configuration.
     */
    private boolean allowEmpty;
    
    /**
     * Initializes the validator with configuration from the ValidSSN annotation.
     * <p>
     * This method is called by the validation framework to configure the validator
     * instance with the parameters specified in the annotation.
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
     * Validates the provided SSN value according to COBOL business rules.
     * <p>
     * This method implements the core validation logic that replicates the behavior
     * of the COBOL 1265-EDIT-US-SSN paragraph. It performs comprehensive validation
     * of the SSN format and business rules.
     * </p>
     * 
     * @param value the SSN value to validate (may be null)
     * @param context the validation context for error reporting
     * @return true if the SSN is valid according to business rules, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values according to configuration
        if (value == null) {
            return allowNull;
        }
        
        // Handle empty strings according to configuration
        if (value.isEmpty()) {
            return allowEmpty;
        }
        
        // Trim whitespace to handle padded input
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return allowEmpty;
        }
        
        // Validate input format before processing
        if (!isValidFormat(trimmedValue)) {
            addViolation(context, "SSN format must be XXX-XX-XXXX or XXXXXXXXX");
            return false;
        }
        
        // Normalize the input to remove formatting characters
        String normalizedSSN = normalizeSSN(trimmedValue);
        
        // Validate that normalized SSN is exactly 9 digits
        if (!UNFORMATTED_SSN_PATTERN.matcher(normalizedSSN).matches()) {
            addViolation(context, "SSN must contain exactly 9 digits");
            return false;
        }
        
        // Apply COBOL business rules validation
        return validateBusinessRules(normalizedSSN, context);
    }
    
    /**
     * Validates the input format according to configuration requirements.
     * <p>
     * This method checks whether the input matches the expected format based on
     * the requireFormatted configuration flag.
     * </p>
     * 
     * @param value the SSN value to validate format for
     * @return true if the format is valid, false otherwise
     */
    private boolean isValidFormat(String value) {
        if (requireFormatted) {
            // Only accept formatted input (XXX-XX-XXXX)
            return FORMATTED_SSN_PATTERN.matcher(value).matches();
        } else {
            // Accept both formatted and unformatted input
            return FORMATTED_SSN_PATTERN.matcher(value).matches() || 
                   UNFORMATTED_SSN_PATTERN.matcher(value).matches();
        }
    }
    
    /**
     * Normalizes the SSN input by removing all non-digit characters.
     * <p>
     * This method strips out hyphens and other formatting characters to create
     * a consistent 9-digit string for validation processing.
     * </p>
     * 
     * @param value the SSN value to normalize
     * @return the normalized SSN containing only digits
     */
    private String normalizeSSN(String value) {
        return DIGIT_EXTRACTION_PATTERN.matcher(value).replaceAll("");
    }
    
    /**
     * Validates SSN business rules based on COBOL validation logic.
     * <p>
     * This method implements the same validation rules as the COBOL program
     * COACTUPC.cbl paragraph 1265-EDIT-US-SSN, ensuring compatibility with
     * the original application's business logic.
     * </p>
     * 
     * @param normalizedSSN the 9-digit normalized SSN
     * @param context the validation context for error reporting
     * @return true if all business rules pass, false otherwise
     */
    private boolean validateBusinessRules(String normalizedSSN, ConstraintValidatorContext context) {
        // Extract SSN parts for validation (matching COBOL structure)
        String part1 = normalizedSSN.substring(0, 3);  // First 3 digits
        String part2 = normalizedSSN.substring(3, 5);  // Middle 2 digits
        String part3 = normalizedSSN.substring(5, 9);  // Last 4 digits
        
        // Validate Part 1 (Area Number) - replicating COBOL INVALID-SSN-PART1 logic
        if (!validateSSNPart1(part1, context)) {
            return false;
        }
        
        // Validate Part 2 (Group Number) - must be 01-99
        if (!validateSSNPart2(part2, context)) {
            return false;
        }
        
        // Validate Part 3 (Serial Number) - must be 0001-9999
        if (!validateSSNPart3(part3, context)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates SSN Part 1 (Area Number) according to COBOL business rules.
     * <p>
     * This method implements the COBOL validation logic for the first 3 digits
     * of the SSN, replicating the INVALID-SSN-PART1 88-level condition validation.
     * </p>
     * 
     * @param part1 the first 3 digits of the SSN
     * @param context the validation context for error reporting
     * @return true if Part 1 is valid, false otherwise
     */
    private boolean validateSSNPart1(String part1, ConstraintValidatorContext context) {
        int area = Integer.parseInt(part1);
        
        // COBOL validation: INVALID-SSN-PART1 VALUES 0, 666, 900 THRU 999
        if (area == 0 || area == 666 || (area >= 900 && area <= 999)) {
            addViolation(context, "SSN first 3 digits cannot be 000, 666, or between 900-999");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates SSN Part 2 (Group Number) according to COBOL business rules.
     * <p>
     * This method validates that the middle 2 digits are not 00, matching
     * the COBOL validation requirements for the group number portion.
     * </p>
     * 
     * @param part2 the middle 2 digits of the SSN
     * @param context the validation context for error reporting
     * @return true if Part 2 is valid, false otherwise
     */
    private boolean validateSSNPart2(String part2, ConstraintValidatorContext context) {
        int group = Integer.parseInt(part2);
        
        // Group number cannot be 00 (must be 01-99)
        if (group == 0) {
            addViolation(context, "SSN middle 2 digits cannot be 00");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates SSN Part 3 (Serial Number) according to COBOL business rules.
     * <p>
     * This method validates that the last 4 digits are not 0000, matching
     * the COBOL validation requirements for the serial number portion.
     * </p>
     * 
     * @param part3 the last 4 digits of the SSN
     * @param context the validation context for error reporting
     * @return true if Part 3 is valid, false otherwise
     */
    private boolean validateSSNPart3(String part3, ConstraintValidatorContext context) {
        int serial = Integer.parseInt(part3);
        
        // Serial number cannot be 0000 (must be 0001-9999)
        if (serial == 0) {
            addViolation(context, "SSN last 4 digits cannot be 0000");
            return false;
        }
        
        return true;
    }
    
    /**
     * Adds a custom validation violation message to the context.
     * <p>
     * This method disables the default constraint violation and adds a custom
     * message with specific details about the validation failure.
     * </p>
     * 
     * @param context the validation context
     * @param message the custom error message to add
     */
    private void addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}