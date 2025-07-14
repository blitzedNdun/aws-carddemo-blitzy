/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jakarta Bean Validation implementation for state and ZIP code cross-validation.
 * <p>
 * This validator implements the {@link ValidStateZip} constraint annotation to provide
 * comprehensive validation of US state codes and ZIP code combinations using lookup tables
 * derived from the CSLKPCDY.cpy COBOL copybook.
 * </p>
 * <p>
 * The validator performs multiple levels of validation:
 * </p>
 * <ul>
 *   <li><strong>State Code Validation:</strong> Validates the state code against the complete
 *       list of valid US state codes from CSLKPCDY.cpy, including all 50 states, Washington DC,
 *       and US territories (AS, GU, MP, PR, VI)</li>
 *   <li><strong>ZIP Code Format Validation:</strong> Ensures ZIP codes conform to the standard
 *       5-digit or 9-digit (with hyphen) format using the ZIP_CODE_PATTERN regex</li>
 *   <li><strong>Geographic Consistency Validation:</strong> Cross-validates state and ZIP code
 *       combinations using the state+ZIP prefix lookup table from CSLKPCDY.cpy to ensure
 *       geographic accuracy</li>
 * </ul>
 * <p>
 * <strong>COBOL Equivalence:</strong>
 * </p>
 * <p>
 * This validator maintains exact functional equivalence with the COBOL validation routines:
 * </p>
 * <ul>
 *   <li>Replaces CSLKPCDY.cpy VALID-US-STATE-CODE 88-level condition validation</li>
 *   <li>Replaces CSLKPCDY.cpy VALID-US-STATE-ZIP-CD2-COMBO 88-level condition validation</li>
 *   <li>Maintains the same validation logic flow and error detection as original COBOL</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>
 * {@code
 * @ValidStateZip
 * public class CustomerAddressDto {
 *     private String state;
 *     private String zipCode;
 *     // getters and setters
 * }
 * 
 * @ValidStateZip(stateField = "stateCode", zipCodeField = "postalCode", strictValidation = false)
 * public class AlternateAddressDto {
 *     private String stateCode;
 *     private String postalCode;
 *     // getters and setters
 * }
 * }
 * </pre>
 * <p>
 * <strong>Validation Process:</strong>
 * </p>
 * <ol>
 *   <li>Extract state and ZIP code values from the annotated object using reflection</li>
 *   <li>Validate state code against ValidationConstants.VALID_STATE_CODES</li>
 *   <li>Validate ZIP code format against ValidationConstants.ZIP_CODE_PATTERN</li>
 *   <li>If strictValidation is enabled, check state+ZIP prefix combination against
 *       ValidationConstants.VALID_STATE_ZIP_COMBINATIONS</li>
 *   <li>Generate detailed error messages for any validation failures</li>
 * </ol>
 * <p>
 * <strong>Error Messages:</strong>
 * </p>
 * <ul>
 *   <li>"State code 'XX' is not a valid US state code" - Invalid state code</li>
 *   <li>"ZIP code 'XXXXX' has invalid format" - Invalid ZIP code format</li>
 *   <li>"ZIP code 'XXXXX' is not valid for state 'XX'" - Invalid state/ZIP combination</li>
 *   <li>"State and ZIP code fields are required for validation" - Missing field values</li>
 * </ul>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 * @see ValidStateZip
 * @see ValidationConstants
 */
public class StateZipValidator implements ConstraintValidator<ValidStateZip, Object> {
    
    private static final Logger LOGGER = Logger.getLogger(StateZipValidator.class.getName());
    
    // Configuration from annotation
    private String stateFieldName;
    private String zipCodeFieldName;
    private boolean strictValidation;
    
    /**
     * Initializes the validator with configuration from the {@link ValidStateZip} annotation.
     * <p>
     * This method is called by the validation framework before any validation calls
     * to configure the validator instance with the annotation parameters.
     * </p>
     * 
     * @param constraintAnnotation the ValidStateZip annotation containing configuration
     */
    @Override
    public void initialize(ValidStateZip constraintAnnotation) {
        this.stateFieldName = constraintAnnotation.stateField();
        this.zipCodeFieldName = constraintAnnotation.zipCodeField();
        this.strictValidation = constraintAnnotation.strictValidation();
        
        LOGGER.log(Level.FINE, "StateZipValidator initialized with stateField={0}, zipCodeField={1}, strictValidation={2}",
                new Object[]{stateFieldName, zipCodeFieldName, strictValidation});
    }
    
    /**
     * Validates the state and ZIP code combination for the given object.
     * <p>
     * This method performs comprehensive validation of state codes and ZIP codes
     * according to the rules defined in CSLKPCDY.cpy COBOL copybook. It validates
     * both individual field formats and geographic consistency.
     * </p>
     * 
     * @param value the object being validated (typically a DTO or entity with address fields)
     * @param context the constraint validator context for building error messages
     * @return true if validation passes, false if any validation rule fails
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Null objects are considered valid (use @NotNull for null checks)
        if (value == null) {
            return true;
        }
        
        try {
            // Extract field values using reflection
            String stateCode = getFieldValue(value, stateFieldName);
            String zipCode = getFieldValue(value, zipCodeFieldName);
            
            // Skip validation if both fields are null or empty
            if (isNullOrEmpty(stateCode) && isNullOrEmpty(zipCode)) {
                return true;
            }
            
            // Disable default constraint violation message
            context.disableDefaultConstraintViolation();
            
            boolean isValid = true;
            
            // Validate state code
            if (!isNullOrEmpty(stateCode)) {
                isValid &= validateStateCode(stateCode, context);
            }
            
            // Validate ZIP code format
            if (!isNullOrEmpty(zipCode)) {
                isValid &= validateZipCodeFormat(zipCode, context);
            }
            
            // Perform cross-validation if both fields are present and strict validation is enabled
            if (strictValidation && !isNullOrEmpty(stateCode) && !isNullOrEmpty(zipCode) && isValid) {
                isValid &= validateStateZipCombination(stateCode, zipCode, context);
            }
            
            return isValid;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during state/ZIP validation: " + e.getMessage(), e);
            context.buildConstraintViolationWithTemplate(
                    "Internal validation error: " + e.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }
    
    /**
     * Validates the state code against the list of valid US state codes.
     * <p>
     * This method checks the provided state code against the comprehensive list
     * of valid US state codes from CSLKPCDY.cpy, including all 50 states,
     * Washington DC, and US territories.
     * </p>
     * 
     * @param stateCode the 2-character state code to validate
     * @param context the constraint validator context for building error messages
     * @return true if the state code is valid, false otherwise
     */
    private boolean validateStateCode(String stateCode, ConstraintValidatorContext context) {
        // Normalize state code to uppercase for lookup
        String normalizedStateCode = stateCode.trim().toUpperCase();
        
        // Check if state code is exactly 2 characters
        if (normalizedStateCode.length() != 2) {
            context.buildConstraintViolationWithTemplate(
                    String.format("State code '%s' must be exactly 2 characters", stateCode))
                    .addPropertyNode(stateFieldName)
                    .addConstraintViolation();
            return false;
        }
        
        // Check against valid state codes from CSLKPCDY.cpy
        if (!ValidationConstants.VALID_STATE_CODES.contains(normalizedStateCode)) {
            context.buildConstraintViolationWithTemplate(
                    String.format("State code '%s' is not a valid US state code", normalizedStateCode))
                    .addPropertyNode(stateFieldName)
                    .addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates the ZIP code format using the standard ZIP code pattern.
     * <p>
     * This method ensures the ZIP code conforms to either the 5-digit format (XXXXX)
     * or the 9-digit format with hyphen (XXXXX-XXXX) as defined in the
     * ValidationConstants.ZIP_CODE_PATTERN.
     * </p>
     * 
     * @param zipCode the ZIP code to validate
     * @param context the constraint validator context for building error messages
     * @return true if the ZIP code format is valid, false otherwise
     */
    private boolean validateZipCodeFormat(String zipCode, ConstraintValidatorContext context) {
        String trimmedZipCode = zipCode.trim();
        
        // Validate ZIP code format using pattern from ValidationConstants
        if (!ValidationConstants.ZIP_CODE_PATTERN.matcher(trimmedZipCode).matches()) {
            context.buildConstraintViolationWithTemplate(
                    String.format("ZIP code '%s' has invalid format (expected XXXXX or XXXXX-XXXX)", zipCode))
                    .addPropertyNode(zipCodeFieldName)
                    .addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates the geographic consistency between state code and ZIP code.
     * <p>
     * This method performs cross-validation to ensure that the ZIP code prefix
     * is geographically consistent with the state code using the lookup table
     * from CSLKPCDY.cpy VALID-US-STATE-ZIP-CD2-COMBO condition.
     * </p>
     * 
     * @param stateCode the normalized state code
     * @param zipCode the ZIP code
     * @param context the constraint validator context for building error messages
     * @return true if the state/ZIP combination is valid, false otherwise
     */
    private boolean validateStateZipCombination(String stateCode, String zipCode, ConstraintValidatorContext context) {
        // Normalize inputs
        String normalizedStateCode = stateCode.trim().toUpperCase();
        String normalizedZipCode = zipCode.trim();
        
        // Extract first 2 digits of ZIP code for lookup
        String zipPrefix = extractZipPrefix(normalizedZipCode);
        if (zipPrefix == null) {
            // This should have been caught by format validation, but handle gracefully
            context.buildConstraintViolationWithTemplate(
                    String.format("Cannot extract ZIP code prefix from '%s'", zipCode))
                    .addPropertyNode(zipCodeFieldName)
                    .addConstraintViolation();
            return false;
        }
        
        // Build state+ZIP prefix combination for lookup (e.g., "CA90", "NY10")
        String stateZipCombination = normalizedStateCode + zipPrefix;
        
        // Check against valid state-ZIP combinations from CSLKPCDY.cpy
        if (!ValidationConstants.VALID_STATE_ZIP_COMBINATIONS.contains(stateZipCombination)) {
            context.buildConstraintViolationWithTemplate(
                    String.format("ZIP code '%s' is not valid for state '%s'", zipCode, normalizedStateCode))
                    .addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    /**
     * Extracts the first 2 digits from a ZIP code for state-ZIP combination lookup.
     * <p>
     * This method handles both 5-digit and 9-digit ZIP code formats and extracts
     * the first 2 digits which are used for geographic validation against the
     * state-ZIP combination lookup table.
     * </p>
     * 
     * @param zipCode the ZIP code (5 or 9 digits)
     * @return the first 2 digits of the ZIP code, or null if extraction fails
     */
    private String extractZipPrefix(String zipCode) {
        if (zipCode == null || zipCode.length() < 2) {
            return null;
        }
        
        // Handle both XXXXX and XXXXX-XXXX formats
        String numericPart = zipCode.split("-")[0]; // Get part before hyphen
        
        if (numericPart.length() >= 2) {
            return numericPart.substring(0, 2);
        }
        
        return null;
    }
    
    /**
     * Extracts a field value from an object using reflection.
     * <p>
     * This method attempts to get the field value by first trying direct field access,
     * then falling back to getter method access. It handles both public fields and
     * private fields with getter methods.
     * </p>
     * 
     * @param object the object to extract the field value from
     * @param fieldName the name of the field to extract
     * @return the field value as a String, or null if the field doesn't exist or is null
     */
    private String getFieldValue(Object object, String fieldName) {
        try {
            // Try direct field access first
            Field field = findField(object.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(object);
                return value != null ? value.toString() : null;
            }
            
            // Try getter method access
            Method getter = findGetter(object.getClass(), fieldName);
            if (getter != null) {
                Object value = getter.invoke(object);
                return value != null ? value.toString() : null;
            }
            
            LOGGER.log(Level.WARNING, "Field '{0}' not found in class {1}", 
                    new Object[]{fieldName, object.getClass().getName()});
            return null;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error accessing field '" + fieldName + "': " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Finds a field in the class hierarchy.
     * <p>
     * This method searches for a field with the given name in the provided class
     * and its superclasses, supporting inheritance scenarios.
     * </p>
     * 
     * @param clazz the class to search in
     * @param fieldName the name of the field to find
     * @return the Field object, or null if not found
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * Finds a getter method for the specified field name.
     * <p>
     * This method looks for getter methods using standard Java naming conventions:
     * getFieldName() for regular fields and isFieldName() for boolean fields.
     * </p>
     * 
     * @param clazz the class to search in
     * @param fieldName the name of the field to find a getter for
     * @return the getter Method object, or null if not found
     */
    private Method findGetter(Class<?> clazz, String fieldName) {
        String capitalizedFieldName = capitalize(fieldName);
        
        try {
            // Try getFieldName() pattern
            return clazz.getMethod("get" + capitalizedFieldName);
        } catch (NoSuchMethodException e) {
            try {
                // Try isFieldName() pattern for boolean fields
                return clazz.getMethod("is" + capitalizedFieldName);
            } catch (NoSuchMethodException ex) {
                return null;
            }
        }
    }
    
    /**
     * Capitalizes the first character of a string.
     * <p>
     * This utility method is used to convert field names to the format expected
     * by Java getter method naming conventions.
     * </p>
     * 
     * @param str the string to capitalize
     * @return the capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Checks if a string is null or empty (after trimming).
     * <p>
     * This utility method provides consistent null and empty string checking
     * throughout the validator implementation.
     * </p>
     * 
     * @param str the string to check
     * @return true if the string is null or empty after trimming, false otherwise
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}