/*
 * Implementation class for state and ZIP code cross-validation.
 * 
 * This validator ensures state codes are valid US state abbreviations and that
 * state/ZIP code combinations are geographically consistent, based on the exact
 * validation logic from COBOL copybook CSLKPCDY.cpy.
 * 
 * Implements Jakarta Bean Validation ConstraintValidator interface to provide
 * seamless integration with Spring Boot validation framework while preserving
 * original COBOL business rules for address validation.
 * 
 * Conversion from COBOL validation patterns in CSLKPCDY.cpy:
 * - US-STATE-CODE-TO-EDIT (lines 1013-1069) → VALID_STATE_CODES validation
 * - US-STATE-ZIPCODE-TO-EDIT (lines 1073-1313) → VALID_STATE_ZIP_COMBINATIONS validation
 * - ZIP code format validation using pattern matching
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jakarta Bean Validation constraint validator for cross-field validation of US state codes
 * and ZIP code combinations, ensuring geographic consistency.
 * 
 * This validator implements the exact business logic from COBOL copybook CSLKPCDY.cpy
 * for validating state codes and state/ZIP code combinations:
 * 
 * <p><strong>State Code Validation:</strong></p>
 * <ul>
 *   <li>Validates against all 50 US states plus DC and territories (AS, GU, MP, PR, VI)</li>
 *   <li>Includes military postal codes (AA, AE, AP) from CSLKPCDY.cpy</li>
 *   <li>Case-insensitive validation with automatic uppercase conversion</li>
 * </ul>
 * 
 * <p><strong>ZIP Code Format Validation:</strong></p>
 * <ul>
 *   <li>Supports 5-digit ZIP codes (12345)</li>
 *   <li>Supports ZIP+4 format (12345-6789)</li>
 *   <li>Validates numeric content and proper formatting</li>
 * </ul>
 * 
 * <p><strong>State/ZIP Cross-Validation:</strong></p>
 * <ul>
 *   <li>Ensures state code and first 2 digits of ZIP code are geographically consistent</li>
 *   <li>Uses USPS ZIP code allocation patterns from CSLKPCDY.cpy lookup table</li>
 *   <li>Prevents invalid combinations like "CA" (California) with "10xxx" (New York) ZIP codes</li>
 * </ul>
 * 
 * <p><strong>Error Reporting:</strong></p>
 * The validator provides specific error messages for different validation failure scenarios:
 * <ul>
 *   <li>Invalid state code: "{stateCode} is not a valid US state code"</li>
 *   <li>Invalid ZIP format: "ZIP code must be in format 12345 or 12345-6789"</li>
 *   <li>Geographic mismatch: "State {stateCode} and ZIP {zipPrefix} combination is not valid"</li>
 *   <li>Missing fields: "State code is required" or "ZIP code is required"</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * &#64;ValidStateZip(stateField = "custAddrStateCd", zipField = "custAddrZip")
 * public class CustomerDto {
 *     private String custAddrStateCd;  // Must be valid state code (e.g., "CA", "NY", "TX")
 *     private String custAddrZip;      // Must be valid ZIP format and consistent with state
 * }
 * </pre>
 * 
 * This implementation preserves the exact COBOL validation behavior while providing
 * modern Jakarta Bean Validation integration for Spring Boot microservices.
 */
public class StateZipValidator implements ConstraintValidator<ValidStateZip, Object> {
    
    private static final Logger logger = Logger.getLogger(StateZipValidator.class.getName());
    
    // Annotation parameters stored during initialization
    private String stateFieldName;
    private String zipFieldName;
    private boolean stateOnly;
    private boolean allowEmpty;
    private String invalidStateMessage;
    private String invalidCombinationMessage;
    private String missingStateMessage;
    private String missingZipMessage;
    
    /**
     * Initializes the validator with parameters from the ValidStateZip annotation.
     * 
     * Stores annotation configuration for use during validation:
     * - Field names for state and ZIP code extraction via reflection
     * - Validation behavior flags (stateOnly, allowEmpty)
     * - Custom error message templates for different failure scenarios
     * 
     * @param constraintAnnotation the ValidStateZip annotation instance with configuration
     */
    @Override
    public void initialize(ValidStateZip constraintAnnotation) {
        this.stateFieldName = constraintAnnotation.stateField();
        this.zipFieldName = constraintAnnotation.zipField();
        this.stateOnly = constraintAnnotation.stateOnly();
        this.allowEmpty = constraintAnnotation.allowEmpty();
        this.invalidStateMessage = constraintAnnotation.invalidStateMessage();
        this.invalidCombinationMessage = constraintAnnotation.invalidCombinationMessage();
        this.missingStateMessage = constraintAnnotation.missingStateMessage();
        this.missingZipMessage = constraintAnnotation.missingZipMessage();
        
        logger.info("Initialized StateZipValidator for fields: state=" + stateFieldName + 
                   ", zip=" + zipFieldName + ", stateOnly=" + stateOnly + ", allowEmpty=" + allowEmpty);
    }
    
    /**
     * Performs the main validation logic for state and ZIP code cross-validation.
     * 
     * <p><strong>Validation Process:</strong></p>
     * <ol>
     *   <li>Extract state code and ZIP code values from the object using reflection</li>
     *   <li>Handle null/empty values based on allowEmpty flag</li>
     *   <li>Validate state code against VALID_STATE_CODES from CSLKPCDY.cpy</li>
     *   <li>Validate ZIP code format using ZIP_CODE_PATTERN</li>
     *   <li>If not stateOnly, validate state/ZIP combination consistency</li>
     *   <li>Generate appropriate error messages for validation failures</li>
     * </ol>
     * 
     * <p><strong>COBOL Equivalence:</strong></p>
     * This method replicates the validation logic from CSLKPCDY.cpy:
     * <pre>
     * COBOL: IF VALID-US-STATE-CODE
     *        THEN IF VALID-US-STATE-ZIP-CD2-COMBO
     *             THEN CONTINUE
     *             ELSE MOVE ERROR-MESSAGE...
     * Java:  if (ValidationConstants.VALID_STATE_CODES.contains(stateCode))
     *        then if (ValidationConstants.VALID_STATE_ZIP_COMBINATIONS.contains(combination))
     *             then return true
     *             else addConstraintViolation...
     * </pre>
     * 
     * @param value   the object instance being validated (containing state and ZIP fields)
     * @param context the validation context for building constraint violations
     * @return true if validation passes, false if any validation rule fails
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Handle null object - consider valid if allowEmpty is true
        if (value == null) {
            if (allowEmpty) {
                logger.fine("Validation passed: null object allowed when allowEmpty=true");
                return true;
            } else {
                logger.warning("Validation failed: null object not allowed when allowEmpty=false");
                addConstraintViolation(context, "Object cannot be null for state/ZIP validation");
                return false;
            }
        }
        
        try {
            // Extract field values using reflection
            String stateCode = extractFieldValue(value, stateFieldName);
            String zipCode = extractFieldValue(value, zipFieldName);
            
            logger.fine("Extracted values: stateCode=" + stateCode + ", zipCode=" + zipCode);
            
            // Handle null/empty values based on allowEmpty configuration
            if (isNullOrEmpty(stateCode) || isNullOrEmpty(zipCode)) {
                if (allowEmpty) {
                    logger.fine("Validation passed: empty values allowed when allowEmpty=true");
                    return true;
                }
                
                // Generate specific error messages for missing fields
                if (isNullOrEmpty(stateCode)) {
                    logger.warning("Validation failed: missing state code");
                    addConstraintViolation(context, missingStateMessage);
                    return false;
                }
                
                if (isNullOrEmpty(zipCode)) {
                    logger.warning("Validation failed: missing ZIP code");
                    addConstraintViolation(context, missingZipMessage);
                    return false;
                }
            }
            
            // Normalize state code to uppercase for consistent validation
            String normalizedStateCode = stateCode.trim().toUpperCase();
            String normalizedZipCode = zipCode.trim();
            
            // Step 1: Validate state code against VALID_STATE_CODES from CSLKPCDY.cpy
            if (!ValidationConstants.VALID_STATE_CODES.contains(normalizedStateCode)) {
                logger.warning("Validation failed: invalid state code - " + normalizedStateCode);
                String errorMessage = invalidStateMessage.replace("{stateCode}", normalizedStateCode);
                addConstraintViolation(context, errorMessage);
                return false;
            }
            
            // Step 2: Validate ZIP code format using ZIP_CODE_PATTERN
            if (!ValidationConstants.ZIP_CODE_PATTERN.matcher(normalizedZipCode).matches()) {
                logger.warning("Validation failed: invalid ZIP code format - " + normalizedZipCode);
                addConstraintViolation(context, "ZIP code must be in format 12345 or 12345-6789");
                return false;
            }
            
            // Step 3: If stateOnly flag is set, skip cross-validation
            if (stateOnly) {
                logger.fine("Validation passed: state-only validation completed successfully");
                return true;
            }
            
            // Step 4: Perform state/ZIP combination cross-validation
            return validateStateZipCombination(normalizedStateCode, normalizedZipCode, context);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Validation error during field extraction: " + e.getMessage(), e);
            addConstraintViolation(context, "Internal validation error: unable to access validation fields");
            return false;
        }
    }
    
    /**
     * Validates state and ZIP code combination for geographic consistency.
     * 
     * <p><strong>Implementation Details:</strong></p>
     * <ul>
     *   <li>Extracts first 2 digits from ZIP code for state/region matching</li>
     *   <li>Combines state code + ZIP prefix (e.g., "CA90", "NY10", "TX75")</li>
     *   <li>Validates against VALID_STATE_ZIP_COMBINATIONS from CSLKPCDY.cpy</li>
     *   <li>Handles both 5-digit and ZIP+4 formats correctly</li>
     * </ul>
     * 
     * <p><strong>COBOL Logic Equivalent:</strong></p>
     * <pre>
     * COBOL: MOVE CUST-ADDR-STATE-CD TO US-STATE-AND-FIRST-ZIP2 (1:2)
     *        MOVE CUST-ADDR-ZIP (1:2) TO US-STATE-AND-FIRST-ZIP2 (3:2)
     *        IF VALID-US-STATE-ZIP-CD2-COMBO THEN...
     * Java:  String zipPrefix = zipCode.substring(0, 2)
     *        String combination = stateCode + zipPrefix
     *        if (VALID_STATE_ZIP_COMBINATIONS.contains(combination)) then...
     * </pre>
     * 
     * @param stateCode normalized state code (uppercase, trimmed)
     * @param zipCode   normalized ZIP code (trimmed, validated format)
     * @param context   validation context for error reporting
     * @return true if state/ZIP combination is geographically valid, false otherwise
     */
    private boolean validateStateZipCombination(String stateCode, String zipCode, ConstraintValidatorContext context) {
        // Extract first 2 digits of ZIP code for state prefix validation
        // Handle both 5-digit ZIP (12345) and ZIP+4 (12345-6789) formats
        String zipPrefix;
        if (zipCode.contains("-")) {
            // ZIP+4 format: extract first 2 digits before the dash
            zipPrefix = zipCode.substring(0, 2);
        } else {
            // 5-digit ZIP format: extract first 2 digits
            zipPrefix = zipCode.substring(0, 2);
        }
        
        // Build state-ZIP combination exactly as in CSLKPCDY.cpy validation
        String stateZipCombination = stateCode + zipPrefix;
        
        logger.fine("Validating state/ZIP combination: " + stateZipCombination);
        
        // Validate against VALID_STATE_ZIP_COMBINATIONS from CSLKPCDY.cpy
        if (ValidationConstants.VALID_STATE_ZIP_COMBINATIONS.contains(stateZipCombination)) {
            logger.fine("Validation passed: valid state/ZIP combination - " + stateZipCombination);
            return true;
        } else {
            logger.warning("Validation failed: invalid state/ZIP combination - " + stateZipCombination);
            String errorMessage = invalidCombinationMessage
                .replace("{stateCode}", stateCode)
                .replace("{zipPrefix}", zipPrefix);
            addConstraintViolation(context, errorMessage);
            return false;
        }
    }
    
    /**
     * Extracts field value from an object using reflection.
     * 
     * <p><strong>Field Access Strategy:</strong></p>
     * <ol>
     *   <li>Attempt direct field access with accessibility override</li>
     *   <li>Handle security manager restrictions gracefully</li>
     *   <li>Support both public and private field access</li>
     *   <li>Provide detailed error logging for debugging</li>
     * </ol>
     * 
     * This method enables cross-field validation by accessing object fields
     * dynamically based on annotation configuration, maintaining compatibility
     * with Spring Boot's reflection-based validation framework.
     * 
     * @param object    the object containing the field to extract
     * @param fieldName the name of the field to extract
     * @return the string value of the field, or null if field is null or extraction fails
     * @throws ReflectiveOperationException if field access fails due to security or missing field
     */
    private String extractFieldValue(Object object, String fieldName) throws ReflectiveOperationException {
        Class<?> clazz = object.getClass();
        
        try {
            // Get the field from the class hierarchy
            Field field = findField(clazz, fieldName);
            
            // Make field accessible if it's private/protected
            boolean wasAccessible = field.isAccessible();
            if (!wasAccessible) {
                field.setAccessible(true);
            }
            
            try {
                // Extract field value
                Object value = field.get(object);
                return value != null ? value.toString() : null;
            } finally {
                // Restore original accessibility state
                if (!wasAccessible) {
                    field.setAccessible(false);
                }
            }
            
        } catch (NoSuchFieldException e) {
            logger.severe("Field not found: " + fieldName + " in class " + clazz.getName());
            throw new ReflectiveOperationException("Field '" + fieldName + "' not found in class " + clazz.getName(), e);
        } catch (IllegalAccessException e) {
            logger.severe("Cannot access field: " + fieldName + " in class " + clazz.getName());
            throw new ReflectiveOperationException("Cannot access field '" + fieldName + "' in class " + clazz.getName(), e);
        } catch (SecurityException e) {
            logger.severe("Security error accessing field: " + fieldName + " in class " + clazz.getName());
            throw new ReflectiveOperationException("Security error accessing field '" + fieldName + "' in class " + clazz.getName(), e);
        }
    }
    
    /**
     * Finds a field in the class hierarchy, including inherited fields.
     * 
     * <p><strong>Search Strategy:</strong></p>
     * <ul>
     *   <li>Search current class first for declared fields</li>
     *   <li>Walk up inheritance hierarchy to find inherited fields</li>
     *   <li>Include both public and private fields in search</li>
     *   <li>Stop at Object class to avoid infinite loops</li>
     * </ul>
     * 
     * This method ensures that validation works correctly with inheritance
     * hierarchies common in DTO and entity classes.
     * 
     * @param clazz     the class to search for the field
     * @param fieldName the name of the field to find
     * @return the Field object if found
     * @throws NoSuchFieldException if the field is not found in the class hierarchy
     */
    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            try {
                // Try to get the declared field from current class
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Field not found in current class, try parent class
                currentClass = currentClass.getSuperclass();
            }
        }
        
        // Field not found in entire hierarchy
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy of " + clazz.getName());
    }
    
    /**
     * Checks if a string value is null, empty, or contains only whitespace.
     * 
     * <p><strong>Null/Empty Validation:</strong></p>
     * <ul>
     *   <li>Returns true for null references</li>
     *   <li>Returns true for empty strings ("")</li>
     *   <li>Returns true for whitespace-only strings ("   ")</li>
     *   <li>Returns false for strings with non-whitespace content</li>
     * </ul>
     * 
     * This method provides consistent empty value handling across all
     * validation scenarios, matching COBOL SPACES and LOW-VALUES behavior.
     * 
     * @param value the string value to check
     * @return true if the value is null, empty, or whitespace-only; false otherwise
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Adds a constraint violation to the validation context with custom error message.
     * 
     * <p><strong>Error Message Handling:</strong></p>
     * <ul>
     *   <li>Disables default constraint violation message</li>
     *   <li>Adds custom error message with field context</li>
     *   <li>Maintains compatibility with Spring Boot validation reporting</li>
     *   <li>Supports message interpolation for dynamic content</li>
     * </ul>
     * 
     * This method ensures that validation errors provide meaningful feedback
     * to users while maintaining consistency with Jakarta Bean Validation standards.
     * 
     * @param context the validation context to add the violation to
     * @param message the custom error message to report
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        // Disable default constraint violation
        context.disableDefaultConstraintViolation();
        
        // Add custom constraint violation with specific message
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
        
        logger.fine("Added constraint violation: " + message);
    }
}