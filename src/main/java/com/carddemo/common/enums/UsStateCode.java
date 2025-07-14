/*
 * Copyright 2024 CardDemo Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * US State Code enumeration converted from COBOL VALID-US-STATE-CODE 88-level condition.
 * 
 * <p>This enumeration provides comprehensive validation for US state codes including all 50 states
 * plus territories (DC, AS, GU, MP, PR, VI) as defined in the original COBOL CSLKPCDY.cpy copybook.
 * The enum maintains exact functional equivalence with the COBOL 88-level condition while providing
 * enhanced validation capabilities for modern Java applications.</p>
 * 
 * <p>The validation preserves the original COBOL cross-reference behavior for address validation
 * and supports Spring Boot validation framework integration for comprehensive field validation.
 * All state codes maintain the original 2-character format validation patterns.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Complete US state and territory code validation per COBOL VALID-US-STATE-CODE</li>
 *   <li>Cross-field validation with ZIP codes for comprehensive address validation</li>
 *   <li>Spring Boot validation framework integration with Jakarta Bean Validation</li>
 *   <li>React component state/ZIP validation pattern support</li>
 *   <li>Comprehensive error messaging for user interface feedback</li>
 * </ul>
 * 
 * <p>COBOL Pattern Conversion:</p>
 * <pre>
 * Original COBOL (CSLKPCDY.cpy):
 *   01 US-STATE-CODE-TO-EDIT  PIC X(2).
 *      88 VALID-US-STATE-CODE VALUES 'AL', 'AK', 'AZ', ... 'VI'.
 * 
 * Java Equivalent:
 *   UsStateCode.isValid(stateCode) // replaces VALID-US-STATE-CODE condition
 * </pre>
 * 
 * <p>Integration Examples:</p>
 * <ul>
 *   <li>Customer address validation in account management services</li>
 *   <li>Spring Boot form validation with @Valid annotation</li>
 *   <li>React component validation with consistent error messages</li>
 *   <li>Cross-reference validation with ZIP code combinations</li>
 * </ul>
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since CardDemo v1.0
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
    
    // Federal District and Territories
    DISTRICT_OF_COLUMBIA("DC", "District of Columbia"),
    AMERICAN_SAMOA("AS", "American Samoa"),
    GUAM("GU", "Guam"),
    NORTHERN_MARIANA_ISLANDS("MP", "Northern Mariana Islands"),
    PUERTO_RICO("PR", "Puerto Rico"),
    US_VIRGIN_ISLANDS("VI", "U.S. Virgin Islands");
    
    // Enum instance fields
    private final String code;
    private final String description;
    
    /**
     * Private constructor for enum instances.
     * 
     * @param code The 2-letter state code (e.g., "AL", "CA", "TX")
     * @param description The full state or territory name
     */
    private UsStateCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * Gets the 2-letter state code for this enum instance.
     * 
     * <p>Returns the standardized 2-character state code as defined in the original
     * COBOL VALID-US-STATE-CODE condition. All codes are uppercase and maintain
     * compatibility with postal service and government standards.</p>
     * 
     * @return The 2-letter state code (e.g., "AL", "CA", "TX")
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the full descriptive name for this state or territory.
     * 
     * <p>Returns the complete state or territory name for display purposes
     * in user interfaces and reports. Names match official postal service
     * and government designations.</p>
     * 
     * @return The full state or territory name (e.g., "Alabama", "California", "Texas")
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Validates if a given state code is valid according to COBOL VALID-US-STATE-CODE condition.
     * 
     * <p>This method replicates the exact behavior of the original COBOL 88-level condition
     * VALID-US-STATE-CODE from CSLKPCDY.cpy. It performs comprehensive validation including
     * format checking, case normalization, and cross-reference validation.</p>
     * 
     * <p>Validation includes:</p>
     * <ul>
     *   <li>Null and empty string checking</li>
     *   <li>Format validation for 2-character alpha codes</li>
     *   <li>Case-insensitive comparison with automatic normalization</li>
     *   <li>Cross-reference validation against all valid state codes</li>
     * </ul>
     * 
     * @param stateCode The state code to validate (2 characters, case-insensitive)
     * @return true if the state code is valid, false otherwise
     * @throws IllegalArgumentException if validation encounters unexpected data format
     */
    public static boolean isValid(String stateCode) {
        if (stateCode == null || stateCode.trim().isEmpty()) {
            return false;
        }
        
        // Use ValidationUtils for consistent field validation
        ValidationResult fieldValidation = ValidationUtils.validateAlphaField(stateCode, 2);
        if (!fieldValidation.isValid()) {
            return false;
        }
        
        // Normalize to uppercase for case-insensitive comparison
        String normalizedCode = stateCode.trim().toUpperCase();
        
        // Validate exact length requirement
        if (normalizedCode.length() != 2) {
            return false;
        }
        
        // Search through all enum values for matching code
        for (UsStateCode state : values()) {
            if (state.code.equals(normalizedCode)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Finds a UsStateCode enum value by its 2-letter code.
     * 
     * <p>This method provides Optional-based lookup for null-safe processing when
     * parsing state codes that may not be valid. It supports both Spring Boot
     * backend validation and React frontend validation scenarios with consistent
     * error handling patterns.</p>
     * 
     * <p>The method performs case-insensitive matching and automatic normalization
     * to uppercase, ensuring compatibility with various input formats while
     * maintaining strict validation standards.</p>
     * 
     * @param stateCode The 2-letter state code to look up (case-insensitive)
     * @return Optional containing the matching UsStateCode, or empty if not found
     */
    public static Optional<UsStateCode> fromCode(String stateCode) {
        if (stateCode == null || stateCode.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Normalize to uppercase for case-insensitive comparison
        String normalizedCode = stateCode.trim().toUpperCase();
        
        // Validate format before searching
        if (normalizedCode.length() != 2) {
            return Optional.empty();
        }
        
        // Search through all enum values for matching code
        for (UsStateCode state : values()) {
            if (state.code.equals(normalizedCode)) {
                return Optional.of(state);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Validates state code with comprehensive error messaging for user interface feedback.
     * 
     * <p>This method provides detailed validation results with specific error messages
     * for different validation failure scenarios. It supports both Spring Boot server-side
     * validation and React form validation with consistent error messaging patterns.</p>
     * 
     * <p>Validation covers:</p>
     * <ul>
     *   <li>Required field validation (blank/null checking)</li>
     *   <li>Format validation (length and character type)</li>
     *   <li>Cross-reference validation (valid state code lookup)</li>
     *   <li>Detailed error messaging for each failure type</li>
     * </ul>
     * 
     * @param stateCode The state code to validate
     * @return ValidationResult with detailed success/error information
     */
    public ValidationResult validateStateCode(String stateCode) {
        // Check for required field
        ValidationResult requiredCheck = ValidationUtils.validateRequiredField(stateCode, "stateCode");
        if (!requiredCheck.isValid()) {
            return ValidationResult.BLANK_FIELD;
        }
        
        // Validate alphabetic format with exact length
        ValidationResult formatCheck = ValidationUtils.validateAlphaField(stateCode, 2);
        if (!formatCheck.isValid()) {
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Validate exact length requirement
        String normalizedCode = stateCode.trim().toUpperCase();
        if (normalizedCode.length() != 2) {
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Validate cross-reference (state code exists in valid set)
        if (!isValid(normalizedCode)) {
            return ValidationResult.INVALID_CROSS_REFERENCE;
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Validates state code with ZIP code cross-reference validation.
     * 
     * <p>This method provides comprehensive address validation by cross-referencing
     * the state code with ZIP code combinations using the StateZipCodeCombo enum.
     * It supports complex validation scenarios where both state and ZIP codes must
     * be validated together for data integrity.</p>
     * 
     * <p>The validation process:</p>
     * <ul>
     *   <li>Basic state code validation (format and cross-reference)</li>
     *   <li>ZIP code format validation</li>
     *   <li>State-ZIP combination validation using StateZipCodeCombo</li>
     *   <li>Detailed error messaging for each validation failure</li>
     * </ul>
     * 
     * @param stateCode The state code to validate (2 characters)
     * @param zipCode The ZIP code to validate (5 or 9 digits)
     * @return ValidationResult with detailed validation status and error messages
     */
    public ValidationResult validateWithZipCode(String stateCode, String zipCode) {
        // First validate the state code independently
        ValidationResult stateValidation = validateStateCode(stateCode);
        if (!stateValidation.isValid()) {
            return stateValidation;
        }
        
        // Validate ZIP code format
        if (zipCode == null || zipCode.trim().isEmpty()) {
            return ValidationResult.BLANK_FIELD;
        }
        
        String normalizedZip = zipCode.trim();
        
        // Check basic ZIP code format (5 or 9 digits with optional dash)
        if (!normalizedZip.matches("\\d{5}(-\\d{4})?")) {
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Extract ZIP prefix (first 2 digits) for state-ZIP validation
        String zipPrefix = normalizedZip.substring(0, 2);
        String normalizedState = stateCode.trim().toUpperCase();
        
        // Use StateZipCodeCombo for cross-reference validation
        if (!StateZipCodeCombo.isValid(normalizedState, zipPrefix)) {
            return ValidationResult.INVALID_CROSS_REFERENCE;
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Gets all valid ZIP prefixes for this state from StateZipCodeCombo.
     * 
     * <p>This method provides integration with the StateZipCodeCombo enum to retrieve
     * all valid ZIP code prefixes for this state. It supports address validation
     * scenarios where the user interface needs to display valid ZIP code ranges
     * for a selected state.</p>
     * 
     * <p>The method enables:</p>
     * <ul>
     *   <li>Dynamic ZIP code validation based on selected state</li>
     *   <li>User assistance in address entry forms</li>
     *   <li>Address validation with postal service standards</li>
     *   <li>Integration with React form validation patterns</li>
     * </ul>
     * 
     * @return Array of valid ZIP prefixes for this state, or empty array if none found
     */
    public String[] getValidZipPrefixes() {
        return StateZipCodeCombo.getValidZipPrefixesForState(this.code);
    }
    
    /**
     * Checks if this state is valid for the given ZIP code prefix.
     * 
     * <p>This method provides reverse validation to check if this state code
     * is valid for a given ZIP code prefix. It supports address validation
     * scenarios where the ZIP code is entered first and the state needs to
     * be validated against it.</p>
     * 
     * @param zipPrefix The first 2 digits of the ZIP code
     * @return true if this state is valid for the ZIP prefix, false otherwise
     */
    public boolean isValidForZipPrefix(String zipPrefix) {
        if (zipPrefix == null || zipPrefix.trim().isEmpty()) {
            return false;
        }
        
        String normalizedZip = zipPrefix.trim();
        
        // Validate ZIP prefix format
        if (normalizedZip.length() != 2 || !normalizedZip.matches("\\d{2}")) {
            return false;
        }
        
        // Use StateZipCodeCombo for validation
        return StateZipCodeCombo.isValid(this.code, normalizedZip);
    }
    
    /**
     * Returns a string representation of this state code suitable for logging and debugging.
     * 
     * @return String representation including code and description
     */
    @Override
    public String toString() {
        return String.format("UsStateCode{code='%s', description='%s'}", code, description);
    }
}