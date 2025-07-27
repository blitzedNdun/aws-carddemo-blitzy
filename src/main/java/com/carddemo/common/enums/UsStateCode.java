/*
 * CardDemo Application
 * 
 * US State Code Validation Enum
 * 
 * Converted from COBOL VALID-US-STATE-CODE 88-level condition
 * in CSLKPCDY.cpy for comprehensive address validation
 * 
 * This enum preserves the exact COBOL cross-reference validation behavior
 * while providing modern Java validation capabilities for React forms
 * and Spring Boot address validation.
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

package com.carddemo.common.enums;

import com.carddemo.common.enums.StateZipCodeCombo;
import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.util.ValidationUtils;

import java.util.Optional;
import jakarta.validation.Valid;

/**
 * US State Code Validation Enum
 * 
 * This enum represents all valid US state and territory codes as defined in the
 * original COBOL VALID-US-STATE-CODE 88-level condition from CSLKPCDY.cpy.
 * It provides comprehensive validation for customer address validation in
 * account management services and maintains exact COBOL cross-reference behavior.
 * 
 * The enum supports Spring Boot address validation and PostgreSQL constraint
 * validation while preserving the original 2-character format validation patterns.
 * Integration with StateZipCodeCombo enables cross-field validation for complete
 * address verification following postal service standards.
 * 
 * State codes included:
 * - All 50 US states in standard 2-character abbreviation format
 * - District of Columbia (DC)
 * - US territories: American Samoa (AS), Guam (GU), Northern Mariana Islands (MP), 
 *   Puerto Rico (PR), US Virgin Islands (VI)
 * 
 * Key Features:
 * - Exact COBOL validation pattern preservation for legacy compatibility
 * - Cross-reference validation with ZIP codes through StateZipCodeCombo integration
 * - Spring Boot validation framework support with Jakarta Bean Validation
 * - React component state/ZIP validation patterns for frontend integration
 * - PostgreSQL constraint validation support for database integrity
 * 
 * Validation Methods:
 * - isValid(): Static validation for state code format and cross-reference checking
 * - fromCode(): Safe parsing with Optional return for null-safe processing
 * - Cross-validation with ZIP codes through StateZipCodeCombo API integration
 * 
 * @author CardDemo Development Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public enum UsStateCode {
    
    // US States (alphabetical order matching COBOL VALUES specification)
    ALABAMA("AL", "Alabama"),
    ALASKA("AK", "Alaska"),
    ARIZONA("AZ", "Arizona"),
    ARKANSAS("AR", "Arkansas"),
    CALIFORNIA("CA", "California"),
    COLORADO("CO", "Colorado"),
    CONNECTICUT("CT", "Connecticut"),
    DELAWARE("DE", "Delaware"),
    FLORIDA("FL", "Florida"),
    GEORGIA("GA", "Georgia"),
    HAWAII("HI", "Hawaii"),
    IDAHO("ID", "Idaho"),
    ILLINOIS("IL", "Illinois"),
    INDIANA("IN", "Indiana"),
    IOWA("IA", "Iowa"),
    KANSAS("KS", "Kansas"),
    KENTUCKY("KY", "Kentucky"),
    LOUISIANA("LA", "Louisiana"),
    MAINE("ME", "Maine"),
    MARYLAND("MD", "Maryland"),
    MASSACHUSETTS("MA", "Massachusetts"),
    MICHIGAN("MI", "Michigan"),
    MINNESOTA("MN", "Minnesota"),
    MISSISSIPPI("MS", "Mississippi"),
    MISSOURI("MO", "Missouri"),
    MONTANA("MT", "Montana"),
    NEBRASKA("NE", "Nebraska"),
    NEVADA("NV", "Nevada"),
    NEW_HAMPSHIRE("NH", "New Hampshire"),
    NEW_JERSEY("NJ", "New Jersey"),
    NEW_MEXICO("NM", "New Mexico"),
    NEW_YORK("NY", "New York"),
    NORTH_CAROLINA("NC", "North Carolina"),
    NORTH_DAKOTA("ND", "North Dakota"),
    OHIO("OH", "Ohio"),
    OKLAHOMA("OK", "Oklahoma"),
    OREGON("OR", "Oregon"),
    PENNSYLVANIA("PA", "Pennsylvania"),
    RHODE_ISLAND("RI", "Rhode Island"),
    SOUTH_CAROLINA("SC", "South Carolina"),
    SOUTH_DAKOTA("SD", "South Dakota"),
    TENNESSEE("TN", "Tennessee"),
    TEXAS("TX", "Texas"),
    UTAH("UT", "Utah"),
    VERMONT("VT", "Vermont"),
    VIRGINIA("VA", "Virginia"),
    WASHINGTON("WA", "Washington"),
    WEST_VIRGINIA("WV", "West Virginia"),
    WISCONSIN("WI", "Wisconsin"),
    WYOMING("WY", "Wyoming"),
    
    // Federal District and US Territories (matching COBOL order)
    DISTRICT_OF_COLUMBIA("DC", "District of Columbia"),
    AMERICAN_SAMOA("AS", "American Samoa"),
    GUAM("GU", "Guam"),
    NORTHERN_MARIANA_ISLANDS("MP", "Northern Mariana Islands"),
    PUERTO_RICO("PR", "Puerto Rico"),
    US_VIRGIN_ISLANDS("VI", "US Virgin Islands");
    
    // Instance fields for each enum constant
    private final String code;
    private final String description;
    
    /**
     * Constructor for UsStateCode enum constants
     * 
     * @param code The 2-character state code (matches COBOL VALUES specification)
     * @param description Human-readable description of the state or territory
     */
    UsStateCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * Get the 2-character state code for this state/territory
     * 
     * This method provides access to the state code in the exact format
     * specified by the original COBOL VALID-US-STATE-CODE 88-level condition.
     * The returned code is always uppercase and exactly 2 characters long.
     * 
     * @return 2-character state code (e.g., "CA", "TX", "NY")
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the human-readable description for this state/territory
     * 
     * This method provides the full name of the state or territory for
     * user interface display purposes and detailed error messages.
     * 
     * @return Full state or territory name (e.g., "California", "Texas", "New York")
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Validate if a state code is valid according to COBOL cross-reference behavior
     * 
     * This method replicates the exact behavior of the COBOL VALID-US-STATE-CODE
     * 88-level condition for maintaining data integrity compatibility with the
     * original mainframe system. It performs comprehensive validation including
     * format checking and cross-reference verification.
     * 
     * Validation Process:
     * 1. Null and blank field validation using ValidationUtils
     * 2. Alpha field format validation (2-character alphabetic)
     * 3. Cross-reference lookup against all valid state codes
     * 4. Case-insensitive comparison matching COBOL behavior
     * 
     * @param stateCode The state code string to validate
     * @return true if the state code is valid, false otherwise
     */
    public static boolean isValid(String stateCode) {
        // Perform required field validation using ValidationUtils
        ValidationResult requiredCheck = ValidationUtils.validateRequiredField(stateCode, "state code");
        if (!requiredCheck.isValid()) {
            return false;
        }
        
        // Perform alpha field format validation (2-character limit)
        ValidationResult alphaCheck = ValidationUtils.validateAlphaField(stateCode, 2);
        if (!alphaCheck.isValid()) {
            return false;
        }
        
        String normalizedCode = stateCode.trim().toUpperCase();
        
        // Validate exact 2-character length requirement
        if (normalizedCode.length() != 2) {
            return false;
        }
        
        // Cross-reference validation against all enum values
        for (UsStateCode state : values()) {
            if (state.code.equals(normalizedCode)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Parse a state code string into a UsStateCode enum with null-safe processing
     * 
     * This method provides Optional-based parsing for robust error handling
     * when parsing state codes that may not be valid. It supports comprehensive
     * validation feedback and enables null-safe processing patterns throughout
     * the application.
     * 
     * The method performs the same validation as isValid() but returns an
     * Optional containing the matching enum constant for successful parsing,
     * or empty Optional for invalid input.
     * 
     * @param stateCode The state code string to parse
     * @return Optional containing the matching UsStateCode, or empty if invalid/not found
     */
    public static Optional<UsStateCode> fromCode(String stateCode) {
        // Use isValid() for comprehensive validation first
        if (!isValid(stateCode)) {
            return Optional.empty();
        }
        
        String normalizedCode = stateCode.trim().toUpperCase();
        
        // Find matching enum constant
        for (UsStateCode state : values()) {
            if (state.code.equals(normalizedCode)) {
                return Optional.of(state);
            }
        }
        
        // This should not be reached due to isValid() check, but included for safety
        return Optional.empty();
    }
    
    /**
     * Validate state code with detailed error information using ValidationResult
     * 
     * This method provides comprehensive validation with detailed error feedback
     * for integration with Spring Boot validation framework and React form
     * validation. It returns ValidationResult enum values that can be used
     * for consistent error handling and user interface feedback.
     * 
     * Error Categories:
     * - BLANK_FIELD: Null, empty, or whitespace-only input
     * - INVALID_FORMAT: Non-alphabetic characters or incorrect length
     * - INVALID_CROSS_REFERENCE: Valid format but not found in state code list
     * - VALID: Successful validation
     * 
     * @param stateCode The state code string to validate
     * @return ValidationResult indicating validation success or specific failure type
     */
    public static ValidationResult validateStateCode(String stateCode) {
        // Perform required field validation using ValidationUtils
        ValidationResult requiredCheck = ValidationUtils.validateRequiredField(stateCode, "state code");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }
        
        // Perform alpha field format validation (2-character limit)
        ValidationResult alphaCheck = ValidationUtils.validateAlphaField(stateCode, 2);
        if (!alphaCheck.isValid()) {
            return alphaCheck;
        }
        
        String normalizedCode = stateCode.trim().toUpperCase();
        
        // Validate exact 2-character length requirement
        if (normalizedCode.length() != 2) {
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Cross-reference validation against all enum values
        for (UsStateCode state : values()) {
            if (state.code.equals(normalizedCode)) {
                return ValidationResult.VALID;
            }
        }
        
        // Valid format but not found in reference data
        return ValidationResult.INVALID_CROSS_REFERENCE;
    }
    
    /**
     * Validate state code with ZIP code cross-reference using StateZipCodeCombo
     * 
     * This method performs comprehensive cross-field validation by checking
     * both state code validity and state-ZIP code combination validity through
     * integration with StateZipCodeCombo enum. It enables complete address
     * validation for customer data integrity.
     * 
     * Cross-validation Process:
     * 1. Individual state code validation using validateStateCode()
     * 2. ZIP code prefix extraction and validation
     * 3. State-ZIP combination validation using StateZipCodeCombo.isValid()
     * 4. Postal service compatibility verification
     * 
     * @param stateCode The 2-character state code to validate
     * @param zipCode The ZIP code (5-digit or ZIP+4 format) for cross-validation
     * @return ValidationResult indicating validation success or specific failure type
     */
    public static ValidationResult validateWithZipCode(String stateCode, String zipCode) {
        // First validate the state code independently
        ValidationResult stateValidation = validateStateCode(stateCode);
        if (!stateValidation.isValid()) {
            return stateValidation;
        }
        
        // Validate ZIP code format and extract prefix for cross-validation
        if (zipCode == null || zipCode.trim().isEmpty()) {
            return ValidationResult.BLANK_FIELD;
        }
        
        String normalizedZip = zipCode.trim();
        String zipPrefix;
        
        // Extract 2-digit ZIP prefix for StateZipCodeCombo validation
        if (normalizedZip.length() >= 5) {
            // Handle both 5-digit (NNNNN) and ZIP+4 (NNNNN-NNNN) formats
            String zipDigits = normalizedZip.replaceAll("[^0-9]", "");
            if (zipDigits.length() >= 5) {
                zipPrefix = zipDigits.substring(0, 2);
            } else {
                return ValidationResult.INVALID_FORMAT;
            }
        } else {
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Perform cross-validation using StateZipCodeCombo
        String normalizedState = stateCode.trim().toUpperCase();
        if (StateZipCodeCombo.isValid(normalizedState, zipPrefix)) {
            return ValidationResult.VALID;
        } else {
            // Check if state exists in any valid combination for detailed error
            Optional<StateZipCodeCombo> anyStateCombo = StateZipCodeCombo.fromStateZip(normalizedState + "00");
            if (anyStateCombo.isPresent() || hasValidZipCombinations(normalizedState)) {
                // State exists but ZIP prefix is invalid for this state
                return ValidationResult.INVALID_CROSS_REFERENCE;
            } else {
                // State itself might not have ZIP combinations in our data
                return ValidationResult.VALID; // Allow state validation to pass
            }
        }
    }
    
    /**
     * Check if a state has any valid ZIP code combinations in StateZipCodeCombo
     * 
     * This helper method determines whether a given state code appears in any
     * StateZipCodeCombo entries, which helps provide more accurate validation
     * results for cross-field validation scenarios.
     * 
     * @param stateCode The normalized state code to check
     * @return true if the state has valid ZIP combinations, false otherwise
     */
    private static boolean hasValidZipCombinations(String stateCode) {
        for (StateZipCodeCombo combo : StateZipCodeCombo.values()) {
            if (combo.getStateCode().equals(stateCode)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get detailed validation error message for invalid state codes
     * 
     * This method provides comprehensive error messages for React form
     * validation and Spring Boot address validation with detailed feedback
     * for user interface components. It analyzes the validation failure
     * and provides context-specific error messages.
     * 
     * @param stateCode The state code that failed validation
     * @return Detailed error message explaining the validation failure
     */
    public static String getValidationErrorMessage(String stateCode) {
        ValidationResult result = validateStateCode(stateCode);
        
        if (result.isValid()) {
            return "State code validation successful";
        }
        
        // Use ValidationResult's built-in error message as base
        String baseMessage = result.getErrorMessage();
        
        // Enhance with context-specific details for state code validation
        switch (result) {
            case BLANK_FIELD:
                return "State code is required and cannot be blank";
                
            case INVALID_FORMAT:
                return "State code must be exactly 2 alphabetic characters (e.g., CA, TX, NY)";
                
            case INVALID_CROSS_REFERENCE:
                return "Invalid state code '" + (stateCode != null ? stateCode.trim().toUpperCase() : "NULL") + 
                       "'. Please use a valid US state or territory code.";
                
            default:
                return baseMessage;
        }
    }
    
    /**
     * Get all valid state codes as an array for validation constraints
     * 
     * This method provides access to all valid state codes in the same order
     * as the original COBOL VALUES specification. It supports PostgreSQL
     * constraint validation and React form validation dropdown populations.
     * 
     * @return Array of all valid 2-character state codes
     */
    public static String[] getAllValidCodes() {
        UsStateCode[] allStates = values();
        String[] codes = new String[allStates.length];
        
        for (int i = 0; i < allStates.length; i++) {
            codes[i] = allStates[i].code;
        }
        
        return codes;
    }
    
    /**
     * Check if this state/territory is a US territory (non-state)
     * 
     * This method identifies US territories vs. actual states for business
     * rule validation and reporting purposes. Territories have different
     * legal and regulatory requirements compared to states.
     * 
     * @return true if this is a US territory, false if it's a state or DC
     */
    public boolean isTerritory() {
        return this == AMERICAN_SAMOA || 
               this == GUAM || 
               this == NORTHERN_MARIANA_ISLANDS || 
               this == PUERTO_RICO || 
               this == US_VIRGIN_ISLANDS;
    }
    
    /**
     * Check if this state/territory is the District of Columbia
     * 
     * DC has special status as the federal district and may require
     * different handling in certain business scenarios.
     * 
     * @return true if this is District of Columbia, false otherwise
     */
    public boolean isDistrictOfColumbia() {
        return this == DISTRICT_OF_COLUMBIA;
    }
    
    /**
     * Check if this is one of the 50 US states
     * 
     * This method identifies the 50 actual US states vs. territories
     * and DC for business rule applications and regulatory compliance.
     * 
     * @return true if this is one of the 50 US states, false otherwise
     */
    public boolean isState() {
        return !isTerritory() && !isDistrictOfColumbia();
    }
    
    /**
     * Get available ZIP code prefixes for this state using StateZipCodeCombo
     * 
     * This method integrates with StateZipCodeCombo to provide all valid
     * ZIP code prefixes for the current state. It supports comprehensive
     * address validation and user interface dropdown population.
     * 
     * @return Array of valid 2-digit ZIP code prefixes for this state
     */
    public String[] getValidZipPrefixes() {
        java.util.List<String> prefixes = new java.util.ArrayList<>();
        
        for (StateZipCodeCombo combo : StateZipCodeCombo.values()) {
            if (combo.getStateCode().equals(this.code)) {
                prefixes.add(combo.getZipPrefix());
            }
        }
        
        // Sort prefixes for consistent ordering
        prefixes.sort(String::compareTo);
        return prefixes.toArray(new String[0]);
    }
    
    /**
     * Validate ZIP code prefix for this specific state
     * 
     * This method provides state-specific ZIP code validation using
     * StateZipCodeCombo integration. It enables precise validation
     * for address forms where the state is already known.
     * 
     * @param zipPrefix The 2-digit ZIP code prefix to validate
     * @return true if the ZIP prefix is valid for this state, false otherwise
     */
    public boolean isValidZipPrefix(String zipPrefix) {
        if (zipPrefix == null || zipPrefix.length() != 2) {
            return false;
        }
        
        return StateZipCodeCombo.isValid(this.code, zipPrefix);
    }
    
    /**
     * Get StateZipCodeCombo entries for this state
     * 
     * This method returns all StateZipCodeCombo entries that correspond
     * to this state, enabling comprehensive cross-reference validation
     * and detailed address verification.
     * 
     * @return Array of StateZipCodeCombo entries for this state
     */
    public StateZipCodeCombo[] getStateZipCombinations() {
        java.util.List<StateZipCodeCombo> combinations = new java.util.ArrayList<>();
        
        for (StateZipCodeCombo combo : StateZipCodeCombo.values()) {
            if (combo.getStateCode().equals(this.code)) {
                combinations.add(combo);
            }
        }
        
        return combinations.toArray(new StateZipCodeCombo[0]);
    }
    
    /**
     * String representation of this state code
     * 
     * @return Formatted string with code and description
     */
    @Override
    public String toString() {
        return String.format("%s (%s)", code, description);
    }
}