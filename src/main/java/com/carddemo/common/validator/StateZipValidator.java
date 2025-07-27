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
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Jakarta Bean Validation implementation for US state codes and cross-validation
 * of state/ZIP code combinations for geographic consistency.
 * 
 * <p>This validator implements the exact validation logic from the original COBOL system
 * based on the CSLKPCDY.cpy copybook, which contains comprehensive lookup tables for:</p>
 * <ul>
 *   <li>Valid US state and territory codes (VALID-US-STATE-CODE condition)</li>
 *   <li>Valid state and ZIP code prefix combinations (VALID-US-STATE-ZIP-CD2-COMBO condition)</li>
 * </ul>
 * 
 * <p><strong>Validation Modes:</strong></p>
 * <ul>
 *   <li><strong>Field-level validation</strong>: Validates only the state code when applied to a String field</li>
 *   <li><strong>Class-level validation</strong>: Performs cross-validation of state and ZIP code when applied to a class</li>
 * </ul>
 * 
 * <p><strong>Geographic Consistency Validation:</strong></p>
 * <p>When performing cross-validation, the validator ensures that the first two digits of the ZIP code
 * are geographically consistent with the state code based on USPS postal zones. This prevents
 * data entry errors such as entering a California ZIP code with a New York state code.</p>
 * 
 * <p><strong>COBOL Integration:</strong></p>
 * <p>This validator maintains exact functional equivalence with the original COBOL validation routines:</p>
 * <ul>
 *   <li>State code validation mirrors COBOL 88-level condition VALID-US-STATE-CODE</li>
 *   <li>State-ZIP validation mirrors COBOL 88-level condition VALID-US-STATE-ZIP-CD2-COMBO</li>
 *   <li>Field name resolution follows COBOL CUSTREC.cpy structure conventions</li>
 *   <li>Error messages provide equivalent detail to original COBOL validation messages</li>
 * </ul>
 * 
 * <p><strong>Performance Optimization:</strong></p>
 * <p>The validator uses efficient Set-based lookups from ValidationConstants for O(1) validation
 * performance, avoiding linear searches through validation arrays that would be typical
 * in direct COBOL-to-Java translations.</p>
 * 
 * @since 1.0
 * @see ValidStateZip The constraint annotation this validator implements
 * @see ValidationConstants Constants class containing COBOL-derived lookup tables
 */
public class StateZipValidator implements ConstraintValidator<ValidStateZip, Object> {

    private static final Logger LOGGER = Logger.getLogger(StateZipValidator.class.getName());

    // Validator configuration from annotation parameters
    private boolean validateZipPrefix;
    private boolean allowEmpty;

    // Field name constants for class-level validation
    // Based on COBOL CUSTREC.cpy field naming conventions
    private static final String[] STATE_FIELD_NAMES = {
        "stateCode",           // Standard Java naming
        "state",               // Common abbreviated form  
        "custAddrStateCd",     // Direct COBOL field name mapping from CUST-ADDR-STATE-CD
        "custAddrStateCode",   // Alternate COBOL mapping
        "addressStateCode",    // Alternative addressing pattern
        "addressState"         // Simplified addressing pattern
    };

    private static final String[] ZIP_FIELD_NAMES = {
        "zipCode",             // Standard Java naming
        "zip",                 // Common abbreviated form
        "custAddrZip",         // Direct COBOL field name mapping from CUST-ADDR-ZIP
        "addressZipCode",      // Alternative addressing pattern
        "addressZip",          // Simplified addressing pattern
        "postalCode"           // Alternative postal terminology
    };

    /**
     * Initializes the validator with annotation parameters.
     * 
     * @param constraintAnnotation the constraint annotation instance containing configuration
     */
    @Override
    public void initialize(ValidStateZip constraintAnnotation) {
        this.validateZipPrefix = constraintAnnotation.validateZipPrefix();
        this.allowEmpty = constraintAnnotation.allowEmpty();
        
        LOGGER.log(Level.FINE, "StateZipValidator initialized with validateZipPrefix={0}, allowEmpty={1}",
                  new Object[]{validateZipPrefix, allowEmpty});
    }

    /**
     * Performs validation logic for state codes and state-ZIP combinations.
     * 
     * <p>The validation behavior depends on the target object type:</p>
     * <ul>
     *   <li><strong>String objects</strong>: Field-level validation of state code only</li>
     *   <li><strong>Other objects</strong>: Class-level validation with state-ZIP cross-validation</li>
     * </ul>
     * 
     * @param value the object to validate (String for field-level, any object for class-level)
     * @param context the validation context for building custom error messages
     * @return true if validation passes, false if validation fails
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Handle null values according to allowEmpty configuration
        if (value == null) {
            if (allowEmpty) {
                LOGGER.log(Level.FINE, "Validation passed: null value allowed when allowEmpty=true");
                return true;
            } else {
                addConstraintViolation(context, "State code is required for validation");
                return false;
            }
        }

        // Field-level validation: value is a String representing state code
        if (value instanceof String) {
            return validateStateCode((String) value, context);
        }

        // Class-level validation: value is an object containing state and ZIP fields
        return validateStateZipCombination(value, context);
    }

    /**
     * Validates a single state code against the COBOL-derived state lookup table.
     * 
     * <p>This method replicates the COBOL 88-level condition VALID-US-STATE-CODE
     * from CSLKPCDY.cpy, validating against all US states, territories, and military postal codes.</p>
     * 
     * @param stateCode the state code to validate (2-character string)
     * @param context the validation context for error message building
     * @return true if state code is valid, false otherwise
     */
    private boolean validateStateCode(String stateCode, ConstraintValidatorContext context) {
        // Handle empty/blank values according to allowEmpty configuration
        if (stateCode.trim().isEmpty()) {
            if (allowEmpty) {
                LOGGER.log(Level.FINE, "Validation passed: empty state code allowed when allowEmpty=true");
                return true;
            } else {
                addConstraintViolation(context, "State code cannot be blank");
                return false;
            }
        }

        // Normalize state code to uppercase for validation (COBOL equivalent behavior)
        String normalizedStateCode = stateCode.trim().toUpperCase();
        
        // Validate state code length (must be exactly 2 characters like COBOL PIC X(02))
        if (normalizedStateCode.length() != 2) {
            String errorMessage = String.format(
                "Invalid state code '%s'. State code must be exactly 2 characters", 
                stateCode
            );
            addConstraintViolation(context, errorMessage);
            return false;
        }

        // Validate against COBOL-derived state code lookup table
        if (!ValidationConstants.VALID_STATE_CODES.contains(normalizedStateCode)) {
            String errorMessage = String.format(
                "Invalid state code '%s'. Must be a valid US state, territory, or military postal code", 
                normalizedStateCode
            );
            addConstraintViolation(context, errorMessage);
            return false;
        }

        LOGGER.log(Level.FINE, "State code validation passed for: {0}", normalizedStateCode);
        return true;
    }

    /**
     * Validates state and ZIP code combination for geographic consistency.
     * 
     * <p>This method performs comprehensive cross-validation:</p>
     * <ol>
     *   <li>Extracts state and ZIP code values from the object using reflection</li>
     *   <li>Validates state code format and lookup table presence</li>
     *   <li>Validates ZIP code format using regex pattern</li>
     *   <li>Validates geographic consistency using COBOL-derived state-ZIP lookup table</li>
     * </ol>
     * 
     * <p>The geographic validation replicates the COBOL 88-level condition
     * VALID-US-STATE-ZIP-CD2-COMBO from CSLKPCDY.cpy lines 1073-1313.</p>
     * 
     * @param target the object containing state and ZIP code fields
     * @param context the validation context for error message building
     * @return true if state-ZIP combination is valid, false otherwise
     */
    private boolean validateStateZipCombination(Object target, ConstraintValidatorContext context) {
        try {
            // Extract state code and ZIP code values using field name resolution
            String stateCode = extractFieldValue(target, STATE_FIELD_NAMES);
            String zipCode = extractFieldValue(target, ZIP_FIELD_NAMES);

            LOGGER.log(Level.FINE, "Extracted values - stateCode: {0}, zipCode: {1}", 
                      new Object[]{stateCode, zipCode});

            // Handle missing field values
            if (stateCode == null && zipCode == null) {
                if (allowEmpty) {
                    LOGGER.log(Level.FINE, "Validation passed: both state and ZIP null with allowEmpty=true");
                    return true;
                } else {
                    addConstraintViolation(context, "State code and ZIP code are required for validation");
                    return false;
                }
            }

            // Validate state code if present
            if (stateCode != null && !stateCode.trim().isEmpty()) {
                if (!validateStateCode(stateCode, context)) {
                    return false; // State code validation failed, error already added
                }

                // Normalize state code for further processing
                stateCode = stateCode.trim().toUpperCase();
            } else if (!allowEmpty) {
                addConstraintViolation(context, "State code is required for state-ZIP validation");
                return false;
            }

            // Validate ZIP code format and geographic consistency if ZIP prefix validation enabled
            if (validateZipPrefix && zipCode != null && !zipCode.trim().isEmpty()) {
                return validateZipCodeFormatAndGeography(stateCode, zipCode, context);
            } else if (!allowEmpty && (zipCode == null || zipCode.trim().isEmpty())) {
                addConstraintViolation(context, "ZIP code is required for state-ZIP validation");
                return false;
            }

            LOGGER.log(Level.FINE, "State-ZIP validation completed successfully");
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during state-ZIP validation: " + e.getMessage(), e);
            addConstraintViolation(context, "Internal validation error occurred");
            return false;
        }
    }

    /**
     * Validates ZIP code format and geographic consistency with state code.
     * 
     * <p>This method performs two levels of ZIP code validation:</p>
     * <ol>
     *   <li><strong>Format validation</strong>: Ensures ZIP code matches standard format (5 or 9 digits)</li>
     *   <li><strong>Geographic validation</strong>: Validates first 2 digits against state using COBOL lookup table</li>
     * </ol>
     * 
     * @param stateCode the validated state code (2-character, uppercase)
     * @param zipCode the ZIP code to validate (5 or 9 digits with optional hyphen)
     * @param context the validation context for error message building
     * @return true if ZIP code format and geography are valid, false otherwise
     */
    private boolean validateZipCodeFormatAndGeography(String stateCode, String zipCode, 
                                                    ConstraintValidatorContext context) {
        String trimmedZipCode = zipCode.trim();
        
        // Validate ZIP code format using compiled regex pattern
        if (!ValidationConstants.ZIP_CODE_PATTERN.matcher(trimmedZipCode).matches()) {
            String errorMessage = String.format(
                "Invalid ZIP code format '%s'. Must be 5 digits or 5+4 format (e.g., 12345 or 12345-6789)", 
                zipCode
            );
            addConstraintViolation(context, errorMessage);
            return false;
        }

        // Extract first 2 digits for geographic validation (equivalent to COBOL substring operation)
        String zipPrefix = trimmedZipCode.length() >= 2 ? trimmedZipCode.substring(0, 2) : trimmedZipCode;
        
        // Validate geographic consistency using COBOL-derived state-ZIP combination lookup
        if (stateCode != null && !stateCode.isEmpty()) {
            String stateZipCombination = stateCode + zipPrefix;
            
            if (!ValidationConstants.VALID_STATE_ZIP_COMBINATIONS.contains(stateZipCombination)) {
                String errorMessage = String.format(
                    "State code '%s' and ZIP code prefix '%s' combination is not geographically valid. " +
                    "ZIP code '%s' is not valid for state '%s'", 
                    stateCode, zipPrefix, trimmedZipCode, stateCode
                );
                addConstraintViolation(context, errorMessage);
                return false;
            }
        }

        LOGGER.log(Level.FINE, "ZIP code validation passed for: {0} with state: {1}", 
                  new Object[]{trimmedZipCode, stateCode});
        return true;
    }

    /**
     * Extracts field value from object using reflection with multiple field name attempts.
     * 
     * <p>This method implements flexible field name resolution to support various naming conventions:</p>
     * <ul>
     *   <li>Standard Java camelCase naming (stateCode, zipCode)</li>
     *   <li>Abbreviated forms (state, zip)</li>
     *   <li>COBOL-derived naming (custAddrStateCd, custAddrZip)</li>
     *   <li>Alternative addressing patterns (addressStateCode, addressZipCode)</li>
     * </ul>
     * 
     * <p>This approach ensures compatibility with various DTO and entity class designs
     * while maintaining the exact validation behavior from the original COBOL system.</p>
     * 
     * @param target the object from which to extract the field value
     * @param fieldNames array of field names to attempt, in priority order
     * @return the field value as a String, or null if no matching field found
     */
    private String extractFieldValue(Object target, String[] fieldNames) {
        Class<?> clazz = target.getClass();
        
        // Attempt field extraction using each possible field name
        for (String fieldName : fieldNames) {
            try {
                Field field = findField(clazz, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(target);
                    
                    if (fieldValue != null) {
                        String stringValue = fieldValue.toString();
                        LOGGER.log(Level.FINE, "Successfully extracted field '{0}' with value: {1}", 
                                  new Object[]{fieldName, stringValue});
                        return stringValue;
                    }
                }
            } catch (IllegalAccessException e) {
                LOGGER.log(Level.WARNING, "Cannot access field '" + fieldName + "': " + e.getMessage());
            } catch (SecurityException e) {
                LOGGER.log(Level.WARNING, "Security exception accessing field '" + fieldName + "': " + e.getMessage());
            }
        }
        
        LOGGER.log(Level.FINE, "No matching field found for names: {0}", java.util.Arrays.toString(fieldNames));
        return null;
    }

    /**
     * Finds a field in the class hierarchy using case-insensitive matching.
     * 
     * <p>This method searches through the class hierarchy to find fields, supporting
     * inheritance patterns common in JPA entities and DTOs. The search is case-insensitive
     * to handle various naming conventions gracefully.</p>
     * 
     * @param clazz the class to search for the field
     * @param fieldName the name of the field to find
     * @return the Field object if found, null otherwise
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        
        // Search through class hierarchy
        while (currentClass != null && currentClass != Object.class) {
            try {
                // First attempt: exact name match
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Second attempt: case-insensitive match
                for (Field field : currentClass.getDeclaredFields()) {
                    if (field.getName().equalsIgnoreCase(fieldName)) {
                        return field;
                    }
                }
            }
            
            currentClass = currentClass.getSuperclass();
        }
        
        return null;
    }

    /**
     * Adds a custom constraint violation message to the validation context.
     * 
     * <p>This method disables the default constraint violation and adds a custom message,
     * providing detailed, contextual error information that matches the specificity
     * of the original COBOL validation error reporting.</p>
     * 
     * @param context the constraint validator context
     * @param message the custom error message to add
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        
        LOGGER.log(Level.FINE, "Added constraint violation: {0}", message);
    }
}