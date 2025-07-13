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
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;

/**
 * Validator implementation for {@link ValidStateZip} annotation.
 * <p>
 * This validator performs state code validation and state/ZIP code cross-validation
 * based on the lookup tables from CSLKPCDY.cpy COBOL copybook, ensuring geographic
 * consistency between state codes and ZIP code prefixes.
 * </p>
 * <p>
 * The validator implements the following validation rules:
 * </p>
 * <ul>
 *   <li>Validates state codes against the complete list of valid US state codes,
 *       including all 50 states, DC, and US territories</li>
 *   <li>Cross-validates state and ZIP code combinations using the state+ZIP prefix
 *       lookup table to ensure geographic accuracy</li>
 *   <li>Provides detailed error messages for different validation failure scenarios</li>
 * </ul>
 * <p>
 * Based on COBOL copybook CSLKPCDY.cpy:
 * </p>
 * <ul>
 *   <li>US-STATE-CODE-TO-EDIT with VALID-US-STATE-CODE condition (lines 1013-1069)</li>
 *   <li>US-STATE-ZIPCODE-TO-EDIT with VALID-US-STATE-ZIP-CD2-COMBO condition (lines 1073-1313)</li>
 * </ul>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
public class ValidStateZipValidator implements ConstraintValidator<ValidStateZip, Object> {
    
    private String stateField;
    private String zipCodeField;
    private boolean strictValidation;
    
    /**
     * Valid US state codes from CSLKPCDY.cpy copybook.
     * Includes all 50 states, DC, and US territories.
     * Based on VALID-US-STATE-CODE condition (lines 1013-1069).
     */
    private static final Set<String> VALID_STATE_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
        "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
        "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
        "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
        "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
        "DC", "AS", "GU", "MP", "PR", "VI"
    )));
    
    /**
     * Valid state and ZIP code prefix combinations from CSLKPCDY.cpy copybook.
     * Based on VALID-US-STATE-ZIP-CD2-COMBO condition (lines 1073-1313).
     * Format: "StateZipPrefix" where State is 2 chars and ZipPrefix is 2 chars.
     */
    private static final Set<String> VALID_STATE_ZIP_COMBINATIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "AA34", "AE90", "AE91", "AE92", "AE93", "AE94", "AE95", "AE96", "AE97", "AE98",
        "AK99", "AL35", "AL36", "AP96", "AR71", "AR72", "AS96", "AZ85", "AZ86",
        "CA90", "CA91", "CA92", "CA93", "CA94", "CA95", "CA96", "CO80", "CO81",
        "CT60", "CT61", "CT62", "CT63", "CT64", "CT65", "CT66", "CT67", "CT68", "CT69",
        "DC20", "DC56", "DC88", "DE19", "FL32", "FL33", "FL34", "FM96",
        "GA30", "GA31", "GA39", "GU96", "HI96", "IA50", "IA51", "IA52", "ID83",
        "IL60", "IL61", "IL62", "IN46", "IN47", "KS66", "KS67", "KY40", "KY41", "KY42",
        "LA70", "LA71", "MA10", "MA11", "MA12", "MA13", "MA14", "MA15", "MA16", "MA17",
        "MA18", "MA19", "MA20", "MA21", "MA22", "MA23", "MA24", "MA25", "MA26", "MA27", "MA55",
        "MD20", "MD21", "ME39", "ME40", "ME41", "ME42", "ME43", "ME44", "ME45", "ME46", "ME47", "ME48", "ME49",
        "MH96", "MI48", "MI49", "MN55", "MN56", "MO63", "MO64", "MO65", "MO72", "MP96",
        "MS38", "MS39", "MT59", "NC27", "NC28", "ND58", "NE68", "NE69",
        "NH30", "NH31", "NH32", "NH33", "NH34", "NH35", "NH36", "NH37", "NH38",
        "NJ70", "NJ71", "NJ72", "NJ73", "NJ74", "NJ75", "NJ76", "NJ77", "NJ78", "NJ79",
        "NJ80", "NJ81", "NJ82", "NJ83", "NJ84", "NJ85", "NJ86", "NJ87", "NJ88", "NJ89",
        "NM87", "NM88", "NV88", "NV89", "NY50", "NY54", "NY63", "NY10", "NY11", "NY12", "NY13", "NY14",
        "OH43", "OH44", "OH45", "OK73", "OK74", "OR97", "PA15", "PA16", "PA17", "PA18", "PA19",
        "PR60", "PR61", "PR62", "PR63", "PR64", "PR65", "PR66", "PR67", "PR68", "PR69",
        "PR70", "PR71", "PR72", "PR73", "PR74", "PR75", "PR76", "PR77", "PR78", "PR79",
        "PR90", "PR91", "PR92", "PR93", "PR94", "PR95", "PR96", "PR97", "PR98", "PW96",
        "RI28", "RI29", "SC29", "SD57", "TN37", "TN38", "TX73", "TX75", "TX76", "TX77", "TX78", "TX79", "TX88",
        "UT84", "VA20", "VA22", "VA23", "VA24", "VI80", "VI82", "VI83", "VI84", "VI85",
        "VT50", "VT51", "VT52", "VT53", "VT54", "VT56", "VT57", "VT58", "VT59",
        "WA98", "WA99", "WI53", "WI54", "WV24", "WV25", "WV26", "WY82", "WY83"
    )));
    
    @Override
    public void initialize(ValidStateZip constraintAnnotation) {
        this.stateField = constraintAnnotation.stateField();
        this.zipCodeField = constraintAnnotation.zipCodeField();
        this.strictValidation = constraintAnnotation.strictValidation();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        try {
            String state = getFieldValue(value, stateField);
            String zipCode = getFieldValue(value, zipCodeField);
            
            // If either field is null or empty, validation passes
            // (individual field validation should handle required fields)
            if (isNullOrEmpty(state) || isNullOrEmpty(zipCode)) {
                return true;
            }
            
            // Normalize input values
            state = state.trim().toUpperCase();
            zipCode = zipCode.trim();
            
            // Validate state code format
            if (state.length() != 2) {
                addConstraintViolation(context, 
                    String.format("State code '%s' must be exactly 2 characters", state));
                return false;
            }
            
            // Validate ZIP code format (5 or 9 digits, possibly with dash)
            if (!isValidZipCodeFormat(zipCode)) {
                addConstraintViolation(context, 
                    String.format("ZIP code '%s' must be in format 12345 or 12345-1234", zipCode));
                return false;
            }
            
            // Validate state code against valid list
            if (!VALID_STATE_CODES.contains(state)) {
                addConstraintViolation(context, 
                    String.format("State code '%s' is not a valid US state code", state));
                return false;
            }
            
            // Perform strict state/ZIP combination validation if enabled
            if (strictValidation) {
                String zipPrefix = zipCode.length() >= 2 ? zipCode.substring(0, 2) : "";
                String stateZipCombo = state + zipPrefix;
                
                if (!VALID_STATE_ZIP_COMBINATIONS.contains(stateZipCombo)) {
                    addConstraintViolation(context, 
                        String.format("ZIP code '%s' is not valid for state '%s'", zipCode, state));
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            addConstraintViolation(context, 
                "State and ZIP code fields are required for validation");
            return false;
        }
    }
    
    /**
     * Retrieves the value of a field from an object using reflection.
     * Tries field access first, then getter method access.
     * 
     * @param obj the object to extract the field value from
     * @param fieldName the name of the field to extract
     * @return the field value as a String, or null if not found
     * @throws Exception if field access fails
     */
    private String getFieldValue(Object obj, String fieldName) throws Exception {
        Class<?> clazz = obj.getClass();
        
        // Try direct field access first
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            return value != null ? value.toString() : null;
        } catch (NoSuchFieldException e) {
            // Field not found, try getter method
        }
        
        // Try getter method access
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Method getter = clazz.getMethod(getterName);
            Object value = getter.invoke(obj);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            // Getter method not found or failed
        }
        
        throw new Exception("Field '" + fieldName + "' not found in class " + clazz.getName());
    }
    
    /**
     * Checks if a string is null or empty.
     * 
     * @param str the string to check
     * @return true if the string is null or empty
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Validates ZIP code format.
     * Accepts 5-digit format (12345) or 9-digit format (12345-1234).
     * 
     * @param zipCode the ZIP code to validate
     * @return true if the ZIP code format is valid
     */
    private boolean isValidZipCodeFormat(String zipCode) {
        if (zipCode == null || zipCode.trim().isEmpty()) {
            return false;
        }
        
        zipCode = zipCode.trim();
        
        // Check for 5-digit format
        if (zipCode.matches("^\\d{5}$")) {
            return true;
        }
        
        // Check for 9-digit format with dash
        if (zipCode.matches("^\\d{5}-\\d{4}$")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Adds a custom constraint violation with a specific message.
     * 
     * @param context the validation context
     * @param message the error message to add
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}