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
import java.util.regex.Pattern;

/**
 * Validator implementation for the {@link ValidStateZip} constraint annotation.
 * 
 * <p>This validator performs two types of validation:</p>
 * <ul>
 *   <li><strong>Field-level validation</strong>: When applied to a String field, validates only the state code</li>
 *   <li><strong>Class-level validation</strong>: When applied to a class, validates both state code and state-ZIP combination</li>
 * </ul>
 * 
 * <p><strong>State Code Validation:</strong></p>
 * <p>Validates against the complete list of US states, territories, and military postal codes
 * from the COBOL copybook CSLKPCDY.cpy. Includes all 50 US states, District of Columbia (DC),
 * and US territories (AS, GU, MP, PR, VI).</p>
 * 
 * <p><strong>State-ZIP Cross-Validation:</strong></p>
 * <p>When performing class-level validation, ensures that the first two digits of the ZIP code
 * are valid for the given state based on the official USPS ZIP code allocation. This prevents
 * invalid combinations like "CA 10001" (California with New York ZIP prefix).</p>
 * 
 * <p><strong>Field Discovery for Class-Level Validation:</strong></p>
 * <p>The validator looks for the following field names in order of preference:</p>
 * <ul>
 *   <li><strong>State fields</strong>: "stateCode", "state", "custAddrStateCd"</li>
 *   <li><strong>ZIP fields</strong>: "zipCode", "zip", "custAddrZip"</li>
 * </ul>
 * 
 * <p><strong>Validation Logic:</strong></p>
 * <ol>
 *   <li>If allowEmpty is true and value is null/empty, validation passes</li>
 *   <li>State code is validated against VALID_STATE_CODES set</li>
 *   <li>If validateZipPrefix is true and ZIP is available, cross-validation is performed</li>
 *   <li>ZIP format is validated using ZIP_CODE_PATTERN (5 or 9 digits)</li>
 *   <li>State-ZIP combination is validated against VALID_STATE_ZIP_COMBINATIONS</li>
 * </ol>
 * 
 * <p><strong>Error Messages:</strong></p>
 * <p>The validator provides specific error messages based on the type of validation failure:</p>
 * <ul>
 *   <li>Invalid state code with details about valid codes</li>
 *   <li>Invalid ZIP format with format requirements</li>
 *   <li>Invalid state-ZIP combination with geographic mismatch details</li>
 *   <li>Missing required fields for cross-validation</li>
 * </ul>
 * 
 * @see ValidStateZip The constraint annotation
 * @see ValidationConstants Constants used for validation
 * @since 1.0
 */
public class ValidStateZipValidator implements ConstraintValidator<ValidStateZip, Object> {

    private ValidStateZip constraintAnnotation;

    /**
     * Initializes the validator with the constraint annotation parameters.
     * 
     * @param constraintAnnotation the constraint annotation instance
     */
    @Override
    public void initialize(ValidStateZip constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }

    /**
     * Validates the given value according to the constraint configuration.
     * 
     * <p>For String values (field-level validation), only state code validation is performed.
     * For Object values (class-level validation), both state and ZIP validation is performed.</p>
     * 
     * @param value the value to validate (String for field-level, Object for class-level)
     * @param context the constraint validator context for error message customization
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Handle null and empty values based on allowEmpty setting
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
            return constraintAnnotation.allowEmpty();
        }

        // Field-level validation (String value = state code only)
        if (value instanceof String) {
            return validateStateCode((String) value, context);
        }

        // Class-level validation (Object = perform state and ZIP cross-validation)
        return validateStateZipCombination(value, context);
    }

    /**
     * Validates a state code against the list of valid US states and territories.
     * 
     * @param stateCode the state code to validate
     * @param context the validation context for error messages
     * @return true if the state code is valid, false otherwise
     */
    private boolean validateStateCode(String stateCode, ConstraintValidatorContext context) {
        if (stateCode == null || stateCode.trim().isEmpty()) {
            if (!constraintAnnotation.allowEmpty()) {
                buildErrorMessage(context, "State code cannot be empty");
                return false;
            }
            return true;
        }

        String normalizedState = stateCode.trim().toUpperCase();
        
        if (!ValidationConstants.VALID_STATE_CODES.contains(normalizedState)) {
            buildErrorMessage(context, 
                String.format("Invalid state code '%s'. Must be a valid US state, territory, or military postal code", 
                    normalizedState));
            return false;
        }

        return true;
    }

    /**
     * Validates state and ZIP code combination for class-level validation.
     * 
     * @param target the target object containing state and ZIP fields
     * @param context the validation context for error messages
     * @return true if the state-ZIP combination is valid, false otherwise
     */
    private boolean validateStateZipCombination(Object target, ConstraintValidatorContext context) {
        String stateCode = extractStateCode(target);
        String zipCode = extractZipCode(target);

        // First validate the state code
        if (!validateStateCodeValue(stateCode, context)) {
            return false;
        }

        // If ZIP prefix validation is enabled, validate ZIP and cross-validation
        if (constraintAnnotation.validateZipPrefix()) {
            if (!validateZipCodeFormat(zipCode, context)) {
                return false;
            }

            // Only perform state-ZIP combination validation if both values are non-empty
            if (stateCode != null && !stateCode.trim().isEmpty() && 
                zipCode != null && !zipCode.trim().isEmpty()) {
                return validateStateZipPrefix(stateCode, zipCode, context);
            }
        }

        return true;
    }

    /**
     * Validates a state code value directly.
     * 
     * @param stateCode the state code to validate
     * @param context the validation context
     * @return true if valid, false otherwise
     */
    private boolean validateStateCodeValue(String stateCode, ConstraintValidatorContext context) {
        if (stateCode == null || stateCode.trim().isEmpty()) {
            if (!constraintAnnotation.allowEmpty()) {
                buildErrorMessage(context, "State code is required for state-ZIP validation");
                return false;
            }
            return true;
        }

        String normalizedState = stateCode.trim().toUpperCase();
        
        if (!ValidationConstants.VALID_STATE_CODES.contains(normalizedState)) {
            buildErrorMessage(context, 
                String.format("Invalid state code '%s'. Must be a valid US state, territory, or military postal code", 
                    normalizedState));
            return false;
        }

        return true;
    }

    /**
     * Validates ZIP code format using the standard ZIP pattern.
     * 
     * @param zipCode the ZIP code to validate
     * @param context the validation context
     * @return true if the format is valid, false otherwise
     */
    private boolean validateZipCodeFormat(String zipCode, ConstraintValidatorContext context) {
        if (zipCode == null || zipCode.trim().isEmpty()) {
            if (!constraintAnnotation.allowEmpty()) {
                buildErrorMessage(context, "ZIP code is required for state-ZIP validation");
                return false;
            }
            return true;
        }

        String normalizedZip = zipCode.trim();
        
        if (!ValidationConstants.ZIP_CODE_PATTERN.matcher(normalizedZip).matches()) {
            buildErrorMessage(context, 
                String.format("Invalid ZIP code format '%s'. Must be 5 digits or 5+4 format (e.g., 12345 or 12345-6789)", 
                    normalizedZip));
            return false;
        }

        return true;
    }

    /**
     * Validates that the state and ZIP code prefix combination is geographically valid.
     * 
     * @param stateCode the state code
     * @param zipCode the ZIP code
     * @param context the validation context
     * @return true if the combination is valid, false otherwise
     */
    private boolean validateStateZipPrefix(String stateCode, String zipCode, ConstraintValidatorContext context) {
        String normalizedState = stateCode.trim().toUpperCase();
        String normalizedZip = zipCode.trim();
        
        // Extract first 2 digits of ZIP code for prefix validation
        String zipPrefix = normalizedZip.length() >= 2 ? normalizedZip.substring(0, 2) : normalizedZip;
        
        // Create state-ZIP combination for lookup
        String stateZipCombo = normalizedState + zipPrefix;
        
        if (!ValidationConstants.VALID_STATE_ZIP_COMBINATIONS.contains(stateZipCombo)) {
            buildErrorMessage(context, 
                String.format("State code '%s' and ZIP code prefix '%s' combination is not geographically valid", 
                    normalizedState, zipPrefix));
            return false;
        }

        return true;
    }

    /**
     * Extracts the state code from the target object using reflection.
     * 
     * @param target the target object
     * @return the state code value, or null if not found
     */
    private String extractStateCode(Object target) {
        // Field names to check in order of preference
        String[] stateFieldNames = {"stateCode", "state", "custAddrStateCd"};
        
        for (String fieldName : stateFieldNames) {
            String value = extractFieldValue(target, fieldName);
            if (value != null) {
                return value;
            }
        }
        
        return null;
    }

    /**
     * Extracts the ZIP code from the target object using reflection.
     * 
     * @param target the target object
     * @return the ZIP code value, or null if not found
     */
    private String extractZipCode(Object target) {
        // Field names to check in order of preference
        String[] zipFieldNames = {"zipCode", "zip", "custAddrZip"};
        
        for (String fieldName : zipFieldNames) {
            String value = extractFieldValue(target, fieldName);
            if (value != null) {
                return value;
            }
        }
        
        return null;
    }

    /**
     * Extracts a field value from the target object using reflection.
     * 
     * @param target the target object
     * @param fieldName the field name to extract
     * @return the field value as String, or null if not found or not accessible
     */
    private String extractFieldValue(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value != null ? value.toString() : null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Field not found or not accessible - this is expected for optional fields
            return null;
        }
    }

    /**
     * Builds a custom error message and disables the default constraint violation.
     * 
     * @param context the constraint validator context
     * @param message the custom error message
     */
    private void buildErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}