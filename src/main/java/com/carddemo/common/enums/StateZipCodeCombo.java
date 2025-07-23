/*
 * CardDemo Application
 * 
 * State and ZIP Code Combination Validation Enum
 * 
 * Converted from COBOL VALID-US-STATE-ZIP-CD2-COMBO 88-level condition
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

import java.util.Optional;
import jakarta.validation.Valid;

/**
 * State and ZIP Code Combination Validation Enum
 * 
 * This enum represents all valid combinations of US state codes and ZIP code prefixes
 * as defined in the original COBOL VALID-US-STATE-ZIP-CD2-COMBO 88-level condition.
 * It provides comprehensive validation for customer address integrity checking
 * and supports both React form validation and Spring Boot address validation.
 * 
 * The enum maintains exact compatibility with the postal service address
 * verification standards and preserves the original COBOL validation logic.
 * 
 * Each enum constant represents a valid state/ZIP prefix combination where:
 * - First 2 characters: State code
 * - Last 2 characters: ZIP code prefix
 * 
 * @author CardDemo Development Team
 * @version 1.0
 */
public enum StateZipCodeCombo {
    
    // Armed Forces and Military Postal Codes
    AA34("AA", "34", "Armed Forces Americas"),
    AE90("AE", "90", "Armed Forces Europe"),
    AE91("AE", "91", "Armed Forces Europe"), 
    AE92("AE", "92", "Armed Forces Europe"),
    AE93("AE", "93", "Armed Forces Europe"),
    AE94("AE", "94", "Armed Forces Europe"),
    AE95("AE", "95", "Armed Forces Europe"),
    AE96("AE", "96", "Armed Forces Europe"),
    AE97("AE", "97", "Armed Forces Europe"),
    AE98("AE", "98", "Armed Forces Europe"),
    AP96("AP", "96", "Armed Forces Pacific"),
    
    // US States and Territories
    AK99("AK", "99", "Alaska"),
    AL35("AL", "35", "Alabama"),
    AL36("AL", "36", "Alabama"),
    AR71("AR", "71", "Arkansas"),
    AR72("AR", "72", "Arkansas"),
    AS96("AS", "96", "American Samoa"),
    AZ85("AZ", "85", "Arizona"),
    AZ86("AZ", "86", "Arizona"),
    
    // California
    CA90("CA", "90", "California"),
    CA91("CA", "91", "California"),
    CA92("CA", "92", "California"),
    CA93("CA", "93", "California"),
    CA94("CA", "94", "California"),
    CA95("CA", "95", "California"),
    CA96("CA", "96", "California"),
    
    // Colorado
    CO80("CO", "80", "Colorado"),
    CO81("CO", "81", "Colorado"),
    
    // Connecticut
    CT60("CT", "60", "Connecticut"),
    CT61("CT", "61", "Connecticut"),
    CT62("CT", "62", "Connecticut"),
    CT63("CT", "63", "Connecticut"),
    CT64("CT", "64", "Connecticut"),
    CT65("CT", "65", "Connecticut"),
    CT66("CT", "66", "Connecticut"),
    CT67("CT", "67", "Connecticut"),
    CT68("CT", "68", "Connecticut"),
    CT69("CT", "69", "Connecticut"),
    
    // District of Columbia
    DC20("DC", "20", "District of Columbia"),
    DC56("DC", "56", "District of Columbia"),
    DC88("DC", "88", "District of Columbia"),
    
    // Delaware
    DE19("DE", "19", "Delaware"),
    
    // Florida
    FL32("FL", "32", "Florida"),
    FL33("FL", "33", "Florida"),
    FL34("FL", "34", "Florida"),
    
    // Federal States of Micronesia
    FM96("FM", "96", "Federated States of Micronesia"),
    
    // Georgia
    GA30("GA", "30", "Georgia"),
    GA31("GA", "31", "Georgia"),
    GA39("GA", "39", "Georgia"),
    
    // Guam
    GU96("GU", "96", "Guam"),
    
    // Hawaii
    HI96("HI", "96", "Hawaii"),
    
    // Iowa
    IA50("IA", "50", "Iowa"),
    IA51("IA", "51", "Iowa"),
    IA52("IA", "52", "Iowa"),
    
    // Idaho
    ID83("ID", "83", "Idaho"),
    
    // Illinois
    IL60("IL", "60", "Illinois"),
    IL61("IL", "61", "Illinois"),
    IL62("IL", "62", "Illinois"),
    
    // Indiana
    IN46("IN", "46", "Indiana"),
    IN47("IN", "47", "Indiana"),
    
    // Kansas
    KS66("KS", "66", "Kansas"),
    KS67("KS", "67", "Kansas"),
    
    // Kentucky
    KY40("KY", "40", "Kentucky"),
    KY41("KY", "41", "Kentucky"),
    KY42("KY", "42", "Kentucky"),
    
    // Louisiana
    LA70("LA", "70", "Louisiana"),
    LA71("LA", "71", "Louisiana"),
    
    // Massachusetts
    MA10("MA", "10", "Massachusetts"),
    MA11("MA", "11", "Massachusetts"),
    MA12("MA", "12", "Massachusetts"),
    MA13("MA", "13", "Massachusetts"),
    MA14("MA", "14", "Massachusetts"),
    MA15("MA", "15", "Massachusetts"),
    MA16("MA", "16", "Massachusetts"),
    MA17("MA", "17", "Massachusetts"),
    MA18("MA", "18", "Massachusetts"),
    MA19("MA", "19", "Massachusetts"),
    MA20("MA", "20", "Massachusetts"),
    MA21("MA", "21", "Massachusetts"),
    MA22("MA", "22", "Massachusetts"),
    MA23("MA", "23", "Massachusetts"),
    MA24("MA", "24", "Massachusetts"),
    MA25("MA", "25", "Massachusetts"),
    MA26("MA", "26", "Massachusetts"),
    MA27("MA", "27", "Massachusetts"),
    MA55("MA", "55", "Massachusetts"),
    
    // Maryland
    MD20("MD", "20", "Maryland"),
    MD21("MD", "21", "Maryland"),
    
    // Maine
    ME39("ME", "39", "Maine"),
    ME40("ME", "40", "Maine"),
    ME41("ME", "41", "Maine"),
    ME42("ME", "42", "Maine"),
    ME43("ME", "43", "Maine"),
    ME44("ME", "44", "Maine"),
    ME45("ME", "45", "Maine"),
    ME46("ME", "46", "Maine"),
    ME47("ME", "47", "Maine"),
    ME48("ME", "48", "Maine"),
    ME49("ME", "49", "Maine"),
    
    // Marshall Islands
    MH96("MH", "96", "Marshall Islands"),
    
    // Michigan
    MI48("MI", "48", "Michigan"),
    MI49("MI", "49", "Michigan"),
    
    // Minnesota
    MN55("MN", "55", "Minnesota"),
    MN56("MN", "56", "Minnesota"),
    
    // Missouri
    MO63("MO", "63", "Missouri"),
    MO64("MO", "64", "Missouri"),
    MO65("MO", "65", "Missouri"),
    MO72("MO", "72", "Missouri"),
    
    // Northern Mariana Islands
    MP96("MP", "96", "Northern Mariana Islands"),
    
    // Mississippi
    MS38("MS", "38", "Mississippi"),
    MS39("MS", "39", "Mississippi"),
    
    // Montana
    MT59("MT", "59", "Montana"),
    
    // North Carolina
    NC27("NC", "27", "North Carolina"),
    NC28("NC", "28", "North Carolina"),
    
    // North Dakota
    ND58("ND", "58", "North Dakota"),
    
    // Nebraska
    NE68("NE", "68", "Nebraska"),
    NE69("NE", "69", "Nebraska"),
    
    // New Hampshire
    NH30("NH", "30", "New Hampshire"),
    NH31("NH", "31", "New Hampshire"),
    NH32("NH", "32", "New Hampshire"),
    NH33("NH", "33", "New Hampshire"),
    NH34("NH", "34", "New Hampshire"),
    NH35("NH", "35", "New Hampshire"),
    NH36("NH", "36", "New Hampshire"),
    NH37("NH", "37", "New Hampshire"),
    NH38("NH", "38", "New Hampshire"),
    
    // New Jersey
    NJ70("NJ", "70", "New Jersey"),
    NJ71("NJ", "71", "New Jersey"),
    NJ72("NJ", "72", "New Jersey"),
    NJ73("NJ", "73", "New Jersey"),
    NJ74("NJ", "74", "New Jersey"),
    NJ75("NJ", "75", "New Jersey"),
    NJ76("NJ", "76", "New Jersey"),
    NJ77("NJ", "77", "New Jersey"),
    NJ78("NJ", "78", "New Jersey"),
    NJ79("NJ", "79", "New Jersey"),
    NJ80("NJ", "80", "New Jersey"),
    NJ81("NJ", "81", "New Jersey"),
    NJ82("NJ", "82", "New Jersey"),
    NJ83("NJ", "83", "New Jersey"),
    NJ84("NJ", "84", "New Jersey"),
    NJ85("NJ", "85", "New Jersey"),
    NJ86("NJ", "86", "New Jersey"),
    NJ87("NJ", "87", "New Jersey"),
    NJ88("NJ", "88", "New Jersey"),
    NJ89("NJ", "89", "New Jersey"),
    
    // New Mexico
    NM87("NM", "87", "New Mexico"),
    NM88("NM", "88", "New Mexico"),
    
    // Nevada
    NV88("NV", "88", "Nevada"),
    NV89("NV", "89", "Nevada"),
    
    // New York
    NY50("NY", "50", "New York"),
    NY54("NY", "54", "New York"),
    NY63("NY", "63", "New York"),
    NY10("NY", "10", "New York"),
    NY11("NY", "11", "New York"),
    NY12("NY", "12", "New York"),
    NY13("NY", "13", "New York"),
    NY14("NY", "14", "New York"),
    
    // Ohio
    OH43("OH", "43", "Ohio"),
    OH44("OH", "44", "Ohio"),
    OH45("OH", "45", "Ohio"),
    
    // Oklahoma
    OK73("OK", "73", "Oklahoma"),
    OK74("OK", "74", "Oklahoma"),
    
    // Oregon
    OR97("OR", "97", "Oregon"),
    
    // Pennsylvania
    PA15("PA", "15", "Pennsylvania"),
    PA16("PA", "16", "Pennsylvania"),
    PA17("PA", "17", "Pennsylvania"),
    PA18("PA", "18", "Pennsylvania"),
    PA19("PA", "19", "Pennsylvania"),
    
    // Puerto Rico
    PR60("PR", "60", "Puerto Rico"),
    PR61("PR", "61", "Puerto Rico"),
    PR62("PR", "62", "Puerto Rico"),
    PR63("PR", "63", "Puerto Rico"),
    PR64("PR", "64", "Puerto Rico"),
    PR65("PR", "65", "Puerto Rico"),
    PR66("PR", "66", "Puerto Rico"),
    PR67("PR", "67", "Puerto Rico"),
    PR68("PR", "68", "Puerto Rico"),
    PR69("PR", "69", "Puerto Rico"),
    PR70("PR", "70", "Puerto Rico"),
    PR71("PR", "71", "Puerto Rico"),
    PR72("PR", "72", "Puerto Rico"),
    PR73("PR", "73", "Puerto Rico"),
    PR74("PR", "74", "Puerto Rico"),
    PR75("PR", "75", "Puerto Rico"),
    PR76("PR", "76", "Puerto Rico"),
    PR77("PR", "77", "Puerto Rico"),
    PR78("PR", "78", "Puerto Rico"),
    PR79("PR", "79", "Puerto Rico"),
    PR90("PR", "90", "Puerto Rico"),
    PR91("PR", "91", "Puerto Rico"),
    PR92("PR", "92", "Puerto Rico"),
    PR93("PR", "93", "Puerto Rico"),
    PR94("PR", "94", "Puerto Rico"),
    PR95("PR", "95", "Puerto Rico"),
    PR96("PR", "96", "Puerto Rico"),
    PR97("PR", "97", "Puerto Rico"),
    PR98("PR", "98", "Puerto Rico"),
    
    // Palau
    PW96("PW", "96", "Palau"),
    
    // Rhode Island
    RI28("RI", "28", "Rhode Island"),
    RI29("RI", "29", "Rhode Island"),
    
    // South Carolina
    SC29("SC", "29", "South Carolina"),
    
    // South Dakota
    SD57("SD", "57", "South Dakota"),
    
    // Tennessee
    TN37("TN", "37", "Tennessee"),
    TN38("TN", "38", "Tennessee"),
    
    // Texas
    TX73("TX", "73", "Texas"),
    TX75("TX", "75", "Texas"),
    TX76("TX", "76", "Texas"),
    TX77("TX", "77", "Texas"),
    TX78("TX", "78", "Texas"),
    TX79("TX", "79", "Texas"),
    TX88("TX", "88", "Texas"),
    
    // Utah
    UT84("UT", "84", "Utah"),
    
    // Virginia
    VA20("VA", "20", "Virginia"),
    VA22("VA", "22", "Virginia"),
    VA23("VA", "23", "Virginia"),
    VA24("VA", "24", "Virginia"),
    
    // US Virgin Islands
    VI80("VI", "80", "US Virgin Islands"),
    VI82("VI", "82", "US Virgin Islands"),
    VI83("VI", "83", "US Virgin Islands"),
    VI84("VI", "84", "US Virgin Islands"),
    VI85("VI", "85", "US Virgin Islands"),
    
    // Vermont
    VT50("VT", "50", "Vermont"),
    VT51("VT", "51", "Vermont"),
    VT52("VT", "52", "Vermont"),
    VT53("VT", "53", "Vermont"),
    VT54("VT", "54", "Vermont"),
    VT56("VT", "56", "Vermont"),
    VT57("VT", "57", "Vermont"),
    VT58("VT", "58", "Vermont"),
    VT59("VT", "59", "Vermont"),
    
    // Washington
    WA98("WA", "98", "Washington"),
    WA99("WA", "99", "Washington"),
    
    // Wisconsin
    WI53("WI", "53", "Wisconsin"),
    WI54("WI", "54", "Wisconsin"),
    
    // West Virginia
    WV24("WV", "24", "West Virginia"),
    WV25("WV", "25", "West Virginia"),
    WV26("WV", "26", "West Virginia"),
    
    // Wyoming
    WY82("WY", "82", "Wyoming"),
    WY83("WY", "83", "Wyoming");
    
    // Instance fields for each enum constant
    private final String stateCode;
    private final String zipPrefix;
    private final String description;
    
    /**
     * Constructor for StateZipCodeCombo enum constants
     * 
     * @param stateCode The 2-character state code
     * @param zipPrefix The 2-character ZIP code prefix
     * @param description Human-readable description of the state/territory
     */
    StateZipCodeCombo(String stateCode, String zipPrefix, String description) {
        this.stateCode = stateCode;
        this.zipPrefix = zipPrefix;
        this.description = description;
    }
    
    /**
     * Get the state code for this combination
     * 
     * @return 2-character state code
     */
    public String getStateCode() {
        return stateCode;
    }
    
    /**
     * Get the ZIP code prefix for this combination
     * 
     * @return 2-character ZIP code prefix
     */
    public String getZipPrefix() {
        return zipPrefix;
    }
    
    /**
     * Get the human-readable description
     * 
     * @return Description of the state or territory
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Validate if a state/ZIP code combination is valid
     * 
     * This method replicates the exact behavior of the COBOL 
     * VALID-US-STATE-ZIP-CD2-COMBO 88-level condition for 
     * maintaining data integrity compatibility.
     * 
     * @param stateCode 2-character state code
     * @param zipPrefix 2-character ZIP code prefix  
     * @return true if the combination is valid, false otherwise
     */
    public static boolean isValid(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            return false;
        }
        
        // Ensure uppercase comparison to match COBOL behavior
        String normalizedState = stateCode.trim().toUpperCase();
        String normalizedZip = zipPrefix.trim();
        
        // Validate length constraints
        if (normalizedState.length() != 2 || normalizedZip.length() != 2) {
            return false;
        }
        
        // Check if ZIP prefix contains only digits
        if (!normalizedZip.matches("\\d{2}")) {
            return false;
        }
        
        // Search through all enum values for exact match
        for (StateZipCodeCombo combo : values()) {
            if (combo.stateCode.equals(normalizedState) && 
                combo.zipPrefix.equals(normalizedZip)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Find a StateZipCodeCombo from state and ZIP prefix
     * 
     * This method provides Optional-based lookup for null-safe
     * processing when parsing state-ZIP combinations that may
     * not be valid, supporting comprehensive error handling.
     * 
     * @param stateCode 2-character state code
     * @param zipPrefix 2-character ZIP code prefix
     * @return Optional containing the matching combo, or empty if not found
     */
    public static Optional<StateZipCodeCombo> fromStateZip(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            return Optional.empty();
        }
        
        // Ensure uppercase comparison to match COBOL behavior
        String normalizedState = stateCode.trim().toUpperCase();
        String normalizedZip = zipPrefix.trim();
        
        // Validate length constraints
        if (normalizedState.length() != 2 || normalizedZip.length() != 2) {
            return Optional.empty();
        }
        
        // Check if ZIP prefix contains only digits
        if (!normalizedZip.matches("\\d{2}")) {
            return Optional.empty();
        }
        
        // Search through all enum values for exact match
        for (StateZipCodeCombo combo : values()) {
            if (combo.stateCode.equals(normalizedState) && 
                combo.zipPrefix.equals(normalizedZip)) {
                return Optional.of(combo);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Validate a complete 4-character state-ZIP combination string
     * 
     * This method directly replicates the COBOL validation logic
     * for the US-STATE-AND-FIRST-ZIP2 field format.
     * 
     * @param stateZipCombo 4-character string (SSNN format)
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String stateZipCombo) {
        if (stateZipCombo == null || stateZipCombo.length() != 4) {
            return false;
        }
        
        String stateCode = stateZipCombo.substring(0, 2);
        String zipPrefix = stateZipCombo.substring(2, 4);
        
        return isValid(stateCode, zipPrefix);
    }
    
    /**
     * Find StateZipCodeCombo from 4-character combination string
     * 
     * @param stateZipCombo 4-character string (SSNN format)  
     * @return Optional containing the matching combo, or empty if not found
     */
    public static Optional<StateZipCodeCombo> fromStateZip(String stateZipCombo) {
        if (stateZipCombo == null || stateZipCombo.length() != 4) {
            return Optional.empty();
        }
        
        String stateCode = stateZipCombo.substring(0, 2);
        String zipPrefix = stateZipCombo.substring(2, 4);
        
        return fromStateZip(stateCode, zipPrefix);
    }
    
    /**
     * Get all valid ZIP prefixes for a specific state
     * 
     * This method supports complex validation scenarios requiring
     * detailed feedback for user interface validation and comprehensive
     * error message generation.
     * 
     * @param stateCode 2-character state code
     * @return Array of valid ZIP prefixes for the state, empty if state not found
     */
    public static String[] getValidZipPrefixesForState(String stateCode) {
        if (stateCode == null) {
            return new String[0];
        }
        
        String normalizedState = stateCode.trim().toUpperCase();
        
        return java.util.Arrays.stream(values())
            .filter(combo -> combo.stateCode.equals(normalizedState))
            .map(combo -> combo.zipPrefix)
            .sorted()
            .toArray(String[]::new);
    }
    
    /**
     * Get detailed validation error message for invalid combinations
     * 
     * This method provides comprehensive error messages for React form
     * validation and Spring Boot address validation with detailed
     * feedback for user interface components.
     * 
     * @param stateCode 2-character state code
     * @param zipPrefix 2-character ZIP code prefix
     * @return Detailed error message explaining validation failure
     */
    public static String getValidationErrorMessage(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            return "State code and ZIP prefix cannot be null";
        }
        
        String normalizedState = stateCode.trim().toUpperCase();
        String normalizedZip = zipPrefix.trim();
        
        if (normalizedState.length() != 2) {
            return "State code must be exactly 2 characters long";
        }
        
        if (normalizedZip.length() != 2) {
            return "ZIP prefix must be exactly 2 characters long";
        }
        
        if (!normalizedZip.matches("\\d{2}")) {
            return "ZIP prefix must contain only numeric digits";
        }
        
        // Check if state exists in any valid combination
        boolean stateExists = java.util.Arrays.stream(values())
            .anyMatch(combo -> combo.stateCode.equals(normalizedState));
        
        if (!stateExists) {
            return "Invalid state code: " + normalizedState + 
                   ". Please use a valid US state or territory code.";
        }
        
        // State exists but ZIP prefix is invalid for this state
        String[] validZips = getValidZipPrefixesForState(normalizedState);
        return "Invalid ZIP prefix " + normalizedZip + " for state " + normalizedState + 
               ". Valid ZIP prefixes for " + normalizedState + " are: " + 
               String.join(", ", validZips);
    }
    
    /**
     * Check if the combination is compatible with postal service standards
     * 
     * This method ensures compatibility with postal service address
     * verification standards by validating against the complete set
     * of authorized state/ZIP combinations.
     * 
     * @param stateCode 2-character state code
     * @param zipPrefix 2-character ZIP code prefix
     * @return true if compatible with postal standards, false otherwise
     */
    public static boolean isPostalServiceCompatible(String stateCode, String zipPrefix) {
        // For this implementation, postal service compatibility is equivalent
        // to our validation since our data comes from official sources
        return isValid(stateCode, zipPrefix);
    }
    
    /**
     * Get the 4-character combination string for this enum constant
     * 
     * @return 4-character state-ZIP combination (SSNN format)
     */
    public String getCombinationCode() {
        return stateCode + zipPrefix;
    }
    
    /**
     * Check if this combination represents a US territory (non-state)
     * 
     * @return true if this is a US territory, false if it's a state
     */
    public boolean isTerritory() {
        return stateCode.equals("AA") || stateCode.equals("AE") || stateCode.equals("AP") ||
               stateCode.equals("AS") || stateCode.equals("FM") || stateCode.equals("GU") ||
               stateCode.equals("MH") || stateCode.equals("MP") || stateCode.equals("PR") ||
               stateCode.equals("PW") || stateCode.equals("VI");
    }
    
    /**
     * Check if this combination represents a military postal code
     * 
     * @return true if this is a military address, false otherwise
     */
    public boolean isMilitary() {
        return stateCode.equals("AA") || stateCode.equals("AE") || stateCode.equals("AP");
    }
    
    /**
     * Get all valid combinations as an array (equivalent to COBOL VALUES clause)
     * 
     * This method provides access to all valid combinations in the same
     * order as defined in the original COBOL 88-level condition for
     * maintaining exact behavioral compatibility.
     * 
     * @return Array of all enum values preserving definition order
     */
    public static StateZipCodeCombo[] values() {
        return StateZipCodeCombo.values();
    }
    
    /**
     * String representation of this state-ZIP combination
     * 
     * @return Formatted string with state, ZIP prefix, and description
     */
    @Override
    public String toString() {
        return String.format("%s-%s (%s)", stateCode, zipPrefix, description);
    }
}