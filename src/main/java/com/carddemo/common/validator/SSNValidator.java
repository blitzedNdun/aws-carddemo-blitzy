/*
 * SSNValidator.java
 *
 * Implementation class for Social Security Number validation logic that maintains
 * exact compatibility with COBOL customer data processing patterns from COACTUPC.cbl.
 * 
 * This validator implements the same SSN validation rules found in the original
 * COBOL application, ensuring behavioral consistency during the technology stack 
 * transformation from IBM COBOL/CICS to Java Spring Boot.
 *
 * Validation Rules (from COBOL COACTUPC.cbl lines 117-147):
 * - First 3 digits (area number): Cannot be 000, 666, or 900-999
 * - Middle 2 digits (group number): Must be 01-99 (cannot be 00)  
 * - Last 4 digits (serial number): Must be 0001-9999 (cannot be 0000)
 * - Supports both formatted (XXX-XX-XXXX) and unformatted (XXXXXXXXX) input
 * - Input normalization removes hyphens and spaces before validation
 *
 * Copyright (c) 2024 CardDemo Application
 */
package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation implementation for Social Security Number format validation.
 * 
 * This validator ensures SSN fields meet Social Security Administration standards
 * while maintaining exact compatibility with COBOL customer record processing patterns.
 * It handles input normalization, format validation, and business rule enforcement
 * using the same logic structure as the original COBOL COACTUPC.cbl program.
 * 
 * The validator supports both formatted (XXX-XX-XXXX) and unformatted (XXXXXXXXX)
 * input formats, automatically normalizing input by removing hyphens and spaces
 * before applying validation rules.
 * 
 * Validation Process:
 * 1. Input normalization - Remove formatting characters
 * 2. Format validation - Ensure exactly 9 digits
 * 3. Business rule validation - Apply SSA rules for each part
 * 4. Error message generation - Provide specific failure reasons
 * 
 * Thread Safety: This validator is stateless and thread-safe.
 * Performance: Compiled regex patterns are cached for optimal performance.
 * 
 * @see ValidSSN for the constraint annotation definition
 * @see ValidationConstants#SSN_PATTERN for the regex pattern used
 */
public class SSNValidator implements ConstraintValidator<ValidSSN, String> {

    // Cache the constraint annotation for access to custom error messages
    private ValidSSN constraintAnnotation;
    
    // Pre-compiled patterns for input normalization and validation
    private static final Pattern FORMATTING_CHARS_PATTERN = Pattern.compile("[\\s\\-]");
    private static final Pattern FORMATTED_SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$");
    
    /**
     * Initializes the validator with the constraint annotation.
     * Called once during constraint creation to cache annotation values.
     * 
     * @param constraintAnnotation The ValidSSN annotation instance containing
     *                           custom error messages and configuration flags
     */
    @Override
    public void initialize(ValidSSN constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }

    /**
     * Validates a Social Security Number string against SSA standards and COBOL business rules.
     * 
     * This method implements the complete SSN validation logic equivalent to the
     * COBOL validation routines in COACTUPC.cbl, including:
     * - Input normalization (removing hyphens and spaces)
     * - Format validation (9-digit numeric pattern)
     * - Business rule validation (area, group, and serial number rules)
     * - Detailed error message generation for failed validations
     * 
     * @param ssnValue The SSN string to validate (may be null, empty, formatted, or unformatted)
     * @param context The validation context for adding custom error messages
     * @return true if the SSN is valid according to all rules, false otherwise
     */
    @Override
    public boolean isValid(String ssnValue, ConstraintValidatorContext context) {
        // Handle null values based on annotation configuration
        if (ssnValue == null) {
            return constraintAnnotation.allowNull();
        }
        
        // Handle empty/blank values based on annotation configuration
        if (ssnValue.trim().isEmpty()) {
            return constraintAnnotation.allowEmpty();
        }
        
        // Normalize input by removing formatting characters (hyphens and spaces)
        // This allows both XXX-XX-XXXX and XXXXXXXXX formats to be processed
        String normalizedSSN = normalizeSSNInput(ssnValue.trim());
        
        // Perform basic format validation using the compiled pattern
        if (!ValidationConstants.SSN_PATTERN.matcher(normalizedSSN).matches()) {
            // Custom error message for basic format failures
            buildCustomErrorMessage(context, constraintAnnotation.message());
            return false;
        }
        
        // Apply business rule validation equivalent to COBOL logic
        return validateSSNBusinessRules(normalizedSSN, context);
    }
    
    /**
     * Normalizes SSN input by removing formatting characters.
     * 
     * Supports multiple input formats:
     * - Formatted: "123-45-6789" -> "123456789"
     * - Unformatted: "123456789" -> "123456789" 
     * - With spaces: "123 45 6789" -> "123456789"
     * - Mixed formatting: "123-45 6789" -> "123456789"
     * 
     * This normalization ensures consistent processing regardless of input format,
     * matching the COBOL behavior where SSN is stored as a 9-digit numeric field.
     * 
     * @param input The raw SSN input string
     * @return The normalized SSN string with only digits
     */
    private String normalizeSSNInput(String input) {
        if (input == null) {
            return "";
        }
        
        // Remove all hyphens and spaces to normalize the input
        return FORMATTING_CHARS_PATTERN.matcher(input).replaceAll("");
    }
    
    /**
     * Validates SSN business rules equivalent to COBOL COACTUPC.cbl validation logic.
     * 
     * Implements the three-part validation structure from COBOL:
     * - WS-EDIT-US-SSN-PART1: Area number validation (000, 666, 900-999 invalid)
     * - WS-EDIT-US-SSN-PART2: Group number validation (00 invalid)
     * - WS-EDIT-US-SSN-PART3: Serial number validation (0000 invalid)
     * 
     * Each validation failure generates a specific error message matching the
     * COBOL error message patterns for consistency with the original application.
     * 
     * @param normalizedSSN The 9-digit normalized SSN string
     * @param context The validation context for adding custom error messages
     * @return true if all business rules pass, false if any rule fails
     */
    private boolean validateSSNBusinessRules(String normalizedSSN, ConstraintValidatorContext context) {
        // Extract the three parts as defined in COBOL WS-EDIT-US-SSN structure
        String areaPart = normalizedSSN.substring(0, 3);      // First 3 digits
        String groupPart = normalizedSSN.substring(3, 5);     // Middle 2 digits  
        String serialPart = normalizedSSN.substring(5, 9);    // Last 4 digits
        
        // Convert to integers for numeric validation (equivalent to COBOL PIC 9(n) handling)
        int areaNumber = Integer.parseInt(areaPart);
        int groupNumber = Integer.parseInt(groupPart);
        int serialNumber = Integer.parseInt(serialPart);
        
        // Validate area number (first 3 digits) - COBOL 88-level condition INVALID-SSN-PART1
        if (!isValidAreaNumber(areaNumber)) {
            buildCustomErrorMessage(context, constraintAnnotation.firstPartMessage());
            return false;
        }
        
        // Validate group number (middle 2 digits) - equivalent to COBOL WS-EDIT-US-SSN-PART2 validation
        if (!isValidGroupNumber(groupNumber)) {
            buildCustomErrorMessage(context, constraintAnnotation.secondPartMessage());
            return false;
        }
        
        // Validate serial number (last 4 digits) - equivalent to COBOL WS-EDIT-US-SSN-PART3 validation
        if (!isValidSerialNumber(serialNumber)) {
            buildCustomErrorMessage(context, constraintAnnotation.thirdPartMessage());
            return false;
        }
        
        // All validation rules passed
        return true;
    }
    
    /**
     * Validates the SSN area number (first 3 digits) according to SSA rules.
     * 
     * Implements the COBOL 88-level condition INVALID-SSN-PART1 logic:
     * - 000: Invalid (never assigned)
     * - 666: Invalid (reserved for non-SSN purposes)  
     * - 900-999: Invalid (reserved for future use)
     * 
     * This matches the exact validation logic from COACTUPC.cbl lines 121-123.
     * 
     * @param areaNumber The area number as an integer (0-999)
     * @return true if the area number is valid, false otherwise
     */
    private boolean isValidAreaNumber(int areaNumber) {
        // COBOL logic: 88 INVALID-SSN-PART1 VALUES 0, 666, 900 THRU 999
        return areaNumber != 0 && areaNumber != 666 && !(areaNumber >= 900 && areaNumber <= 999);
    }
    
    /**
     * Validates the SSN group number (middle 2 digits) according to SSA rules.
     * 
     * Implements the COBOL validation equivalent for WS-EDIT-US-SSN-PART2:
     * - 00: Invalid (never used as a group number)
     * - 01-99: Valid group numbers
     * 
     * @param groupNumber The group number as an integer (0-99)
     * @return true if the group number is valid, false otherwise
     */
    private boolean isValidGroupNumber(int groupNumber) {
        // Group number cannot be 00, must be 01-99
        return groupNumber >= 1 && groupNumber <= 99;
    }
    
    /**
     * Validates the SSN serial number (last 4 digits) according to SSA rules.
     * 
     * Implements the COBOL validation equivalent for WS-EDIT-US-SSN-PART3:
     * - 0000: Invalid (never used as a serial number)
     * - 0001-9999: Valid serial numbers
     * 
     * @param serialNumber The serial number as an integer (0-9999)
     * @return true if the serial number is valid, false otherwise
     */
    private boolean isValidSerialNumber(int serialNumber) {
        // Serial number cannot be 0000, must be 0001-9999
        return serialNumber >= 1 && serialNumber <= 9999;
    }
    
    /**
     * Builds a custom error message and adds it to the validation context.
     * 
     * This method replaces the default constraint message with a specific
     * error message that provides detailed information about the validation failure.
     * The error messages match the COBOL message patterns for consistency
     * with the original application behavior.
     * 
     * @param context The validation context to modify
     * @param message The custom error message to add
     */
    private void buildCustomErrorMessage(ConstraintValidatorContext context, String message) {
        // Disable the default constraint violation to replace with custom message
        context.disableDefaultConstraintViolation();
        
        // Add the custom error message to the validation context
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}