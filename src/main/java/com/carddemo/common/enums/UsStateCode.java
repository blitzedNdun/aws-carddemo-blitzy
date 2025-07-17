/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.enums;

import com.carddemo.common.util.ValidationUtils;
import java.util.Optional;
import java.util.Arrays;
import jakarta.validation.Valid;

/**
 * US State Code Enumeration
 * 
 * This enum converts the COBOL VALID-US-STATE-CODE 88-level condition from the 
 * CSLKPCDY copybook to Java, maintaining exact validation behavior for US state 
 * codes used in customer address validation and cross-reference checking.
 * 
 * Original COBOL structure:
 * - US-STATE-CODE-TO-EDIT PIC X(2)
 * - 88 VALID-US-STATE-CODE VALUES 'AL', 'AK', 'AZ', ... (lines 1013-1069)
 * - Includes all 50 US states plus territories (DC, AS, GU, MP, PR, VI)
 * 
 * Key features:
 * - Preserves exact COBOL validation logic for address validation
 * - Supports Spring Boot address validation framework integration
 * - Maintains 2-character format validation patterns from original system
 * - Enables cross-field validation with ZIP codes for comprehensive address verification
 * - Provides standardized validation results compatible with React form validation
 * 
 * Business Rules:
 * - State codes must be exactly 2 characters (alphabetic)
 * - Case-insensitive validation with uppercase normalization
 * - Supports cross-reference validation with ZIP code prefixes
 * - Maintains equivalence with original COBOL 88-level condition behavior
 * 
 * Integration Points:
 * - Customer address validation in account management services
 * - React component state/ZIP validation patterns
 * - Spring Boot REST API address validation endpoints
 * - PostgreSQL constraint validation for state_code columns
 * 
 * Performance characteristics:
 * - Constant-time lookup for validation operations
 * - Memory-efficient enum-based implementation
 * - Thread-safe for concurrent validation operations
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public enum UsStateCode {
    
    // US States (50 states in alphabetical order)
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
    
    // US Territories and Special Areas
    DISTRICT_OF_COLUMBIA("DC", "District of Columbia"),
    AMERICAN_SAMOA("AS", "American Samoa"),
    GUAM("GU", "Guam"),
    NORTHERN_MARIANA_ISLANDS("MP", "Northern Mariana Islands"),
    PUERTO_RICO("PR", "Puerto Rico"),
    VIRGIN_ISLANDS("VI", "Virgin Islands");
    
    // Instance fields for each enum constant
    private final String code;
    private final String description;
    
    /**
     * Private constructor for enum constants
     * 
     * @param code Two-character state code (e.g., "AL", "NY", "TX")
     * @param description Human-readable description of the state/territory
     */
    private UsStateCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * Returns the two-character state code
     * 
     * @return State code (e.g., "AL", "NY", "TX")
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Returns the human-readable description
     * 
     * @return Description of the state/territory
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Validates if a given state code is valid
     * 
     * This method replicates the exact behavior of the COBOL 88-level condition
     * VALID-US-STATE-CODE by checking if the provided state code matches any
     * of the valid US states or territories.
     * 
     * @param stateCode Two-character state code to validate
     * @return true if the state code is valid, false otherwise
     */
    public static boolean isValid(String stateCode) {
        if (stateCode == null || stateCode.length() != 2) {
            return false;
        }
        
        // Convert to uppercase for case-insensitive comparison
        String upperStateCode = stateCode.toUpperCase();
        
        // Check if state code exists in enum values
        for (UsStateCode state : values()) {
            if (state.code.equals(upperStateCode)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns an Optional containing the UsStateCode for the given code
     * 
     * This method supports null-safe processing and follows the Optional pattern
     * for handling potentially invalid state codes.
     * 
     * @param stateCode Two-character state code
     * @return Optional containing the matching UsStateCode, or empty if not found
     */
    public static Optional<UsStateCode> fromCode(String stateCode) {
        if (stateCode == null || stateCode.length() != 2) {
            return Optional.empty();
        }
        
        // Convert to uppercase for case-insensitive comparison
        String upperStateCode = stateCode.toUpperCase();
        
        // Find matching enum constant
        for (UsStateCode state : values()) {
            if (state.code.equals(upperStateCode)) {
                return Optional.of(state);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Validates state code format and cross-reference using ValidationUtils
     * 
     * This method provides comprehensive validation by leveraging the common
     * validation utilities for consistent error handling patterns across the
     * application. It performs both format validation and cross-reference
     * validation against the valid state codes.
     * 
     * @param stateCode Two-character state code to validate
     * @return ValidationResult indicating the validation outcome
     */
    public static ValidationResult validateStateCode(String stateCode) {
        // First, validate required field
        ValidationResult requiredResult = ValidationUtils.validateRequiredField(stateCode);
        if (!requiredResult.isValid()) {
            return requiredResult;
        }
        
        // Then, validate alphabetic format
        ValidationResult alphaResult = ValidationUtils.validateAlphaField(stateCode);
        if (!alphaResult.isValid()) {
            return alphaResult;
        }
        
        // Check if exactly 2 characters
        if (stateCode.trim().length() != 2) {
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Finally, validate against valid state codes
        if (!isValid(stateCode)) {
            return ValidationResult.INVALID_CROSS_REFERENCE;
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Validates state code and ZIP code combination
     * 
     * This method provides cross-field validation by checking if the state code
     * and ZIP code combination is valid according to USPS standards. It leverages
     * the StateZipCodeCombo enum for comprehensive address validation.
     * 
     * @param stateCode Two-character state code
     * @param zipCode Full ZIP code (5 digits) or ZIP prefix (2 digits)
     * @return ValidationResult indicating the validation outcome
     */
    public static ValidationResult validateStateZipCombination(String stateCode, String zipCode) {
        // First validate the state code
        ValidationResult stateResult = validateStateCode(stateCode);
        if (!stateResult.isValid()) {
            return stateResult;
        }
        
        // Validate ZIP code format
        if (zipCode == null || zipCode.trim().isEmpty()) {
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanZipCode = zipCode.trim();
        
        // Support both 5-digit ZIP codes and 2-digit ZIP prefixes
        String zipPrefix;
        if (cleanZipCode.length() == 5) {
            zipPrefix = cleanZipCode.substring(0, 2);
        } else if (cleanZipCode.length() == 2) {
            zipPrefix = cleanZipCode;
        } else {
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Validate state-ZIP combination using StateZipCodeCombo
        if (!StateZipCodeCombo.isValid(stateCode.toUpperCase(), zipPrefix)) {
            return ValidationResult.INVALID_CROSS_REFERENCE;
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Returns detailed validation error message for invalid state codes
     * 
     * This method provides comprehensive error messages for UI feedback,
     * supporting React form validation requirements and Spring Boot
     * address validation framework integration.
     * 
     * @param stateCode The state code that was validated
     * @return Detailed error message explaining why the state code is invalid
     */
    public static String getValidationErrorMessage(String stateCode) {
        if (stateCode == null || stateCode.trim().isEmpty()) {
            return "State code is required for address validation";
        }
        
        String cleanStateCode = stateCode.trim();
        
        if (cleanStateCode.length() != 2) {
            return "State code must be exactly 2 characters";
        }
        
        if (!cleanStateCode.matches("[A-Za-z]{2}")) {
            return "State code must contain only letters";
        }
        
        if (!isValid(cleanStateCode)) {
            return String.format("State code '%s' is not a valid US state or territory", cleanStateCode.toUpperCase());
        }
        
        return ""; // Valid state code
    }
    
    /**
     * Returns detailed validation error message for invalid state-ZIP combinations
     * 
     * This method provides comprehensive error messages for cross-field validation
     * failures, supporting complex address validation scenarios in customer
     * management services.
     * 
     * @param stateCode The state code that was validated
     * @param zipCode The ZIP code that was validated
     * @return Detailed error message explaining why the combination is invalid
     */
    public static String getStateZipValidationErrorMessage(String stateCode, String zipCode) {
        // First check state code validity
        String stateError = getValidationErrorMessage(stateCode);
        if (!stateError.isEmpty()) {
            return stateError;
        }
        
        if (zipCode == null || zipCode.trim().isEmpty()) {
            return "ZIP code is required for state-ZIP validation";
        }
        
        String cleanZipCode = zipCode.trim();
        
        if (cleanZipCode.length() != 2 && cleanZipCode.length() != 5) {
            return "ZIP code must be either 2 digits (prefix) or 5 digits (full ZIP)";
        }
        
        if (!cleanZipCode.matches("\\d{2}") && !cleanZipCode.matches("\\d{5}")) {
            return "ZIP code must contain only digits";
        }
        
        // If we reach here, the individual fields are valid but the combination is not
        String zipPrefix = cleanZipCode.length() == 5 ? cleanZipCode.substring(0, 2) : cleanZipCode;
        return String.format("ZIP prefix '%s' is not valid for state '%s'. Please verify the ZIP code is correct for the specified state.", 
                           zipPrefix, stateCode.toUpperCase());
    }
    
    /**
     * Returns all valid state codes as a String array
     * 
     * This method provides a convenience method for populating UI dropdowns
     * and for validation scenarios that require a complete list of valid codes.
     * 
     * @return Array of all valid state codes
     */
    public static String[] getAllStateCodes() {
        String[] codes = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            codes[i] = values()[i].getCode();
        }
        return codes;
    }
    
    /**
     * Returns all valid state descriptions as a String array
     * 
     * This method provides a convenience method for populating UI dropdowns
     * with human-readable state names for better user experience.
     * 
     * @return Array of all valid state descriptions
     */
    public static String[] getAllStateDescriptions() {
        String[] descriptions = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            descriptions[i] = values()[i].getDescription();
        }
        return descriptions;
    }
    
    /**
     * Checks if the state code represents a US territory (not one of the 50 states)
     * 
     * This method provides business logic distinction between states and territories
     * for scenarios where different processing rules may apply.
     * 
     * @return true if this is a US territory, false if it's a state
     */
    public boolean isTerritory() {
        return this == DISTRICT_OF_COLUMBIA || 
               this == AMERICAN_SAMOA || 
               this == GUAM || 
               this == NORTHERN_MARIANA_ISLANDS || 
               this == PUERTO_RICO || 
               this == VIRGIN_ISLANDS;
    }
    
    /**
     * Checks if the state code represents one of the 50 US states
     * 
     * This method provides business logic distinction between states and territories
     * for scenarios where different processing rules may apply.
     * 
     * @return true if this is one of the 50 US states, false if it's a territory
     */
    public boolean isState() {
        return !isTerritory();
    }
    
    /**
     * Returns a formatted string representation of the state code
     * 
     * This method provides a human-readable format suitable for UI display
     * and error messages, following consistent formatting patterns.
     * 
     * @return Formatted string in the format "CODE - Description"
     */
    @Override
    public String toString() {
        return String.format("%s - %s", code, description);
    }
    
    /**
     * Validates if a ZIP code is potentially valid for this state
     * 
     * This method provides state-specific ZIP code validation by checking
     * if the ZIP code prefix is valid for this particular state according
     * to USPS standards.
     * 
     * @param zipCode Full ZIP code (5 digits) to validate
     * @return true if the ZIP code could be valid for this state
     */
    public boolean isValidZipCodeForState(String zipCode) {
        if (zipCode == null || zipCode.length() != 5) {
            return false;
        }
        
        if (!zipCode.matches("\\d{5}")) {
            return false;
        }
        
        String zipPrefix = zipCode.substring(0, 2);
        return StateZipCodeCombo.isValid(this.code, zipPrefix);
    }
    
    /**
     * Returns all valid ZIP prefixes for this state
     * 
     * This method provides a convenience method for getting all valid ZIP
     * code prefixes for a specific state, useful for validation and UI
     * population scenarios.
     * 
     * @return Array of valid ZIP prefixes for this state
     */
    public String[] getValidZipPrefixes() {
        return Arrays.stream(StateZipCodeCombo.values())
            .filter(combo -> combo.getStateCode().equals(this.code))
            .map(StateZipCodeCombo::getZipPrefix)
            .toArray(String[]::new);
    }
}