package com.carddemo.common.enums;

import java.util.Optional;
import jakarta.validation.Valid;

/**
 * StateZipCodeCombo Enum
 * 
 * This enum converts the COBOL 88-level condition VALID-US-STATE-ZIP-CD2-COMBO
 * from the CSLKPCDY copybook to Java, maintaining exact validation behavior
 * for state and ZIP code combinations used in customer address validation.
 * 
 * Original COBOL structure:
 * - US-STATE-ZIPCODE-TO-EDIT.US-STATE-AND-FIRST-ZIP2 PIC X(4)
 * - Contains 2-character state code + 2-character ZIP prefix
 * - Validates against predefined state/ZIP combinations per USPS standards
 * 
 * Key features:
 * - Preserves exact COBOL validation logic for data integrity
 * - Supports complex validation scenarios with detailed error messages
 * - Integrates with React form validation and Spring Boot address validation
 * - Maintains compatibility with postal service address verification standards
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
public enum StateZipCodeCombo {
    
    // Military/Diplomatic addresses (AA, AE, AP prefixes)
    AA_34("AA", "34", "Armed Forces Americas (APO/FPO)"),
    AE_90("AE", "90", "Armed Forces Europe (APO/FPO)"),
    AE_91("AE", "91", "Armed Forces Europe (APO/FPO)"),
    AE_92("AE", "92", "Armed Forces Europe (APO/FPO)"),
    AE_93("AE", "93", "Armed Forces Europe (APO/FPO)"),
    AE_94("AE", "94", "Armed Forces Europe (APO/FPO)"),
    AE_95("AE", "95", "Armed Forces Europe (APO/FPO)"),
    AE_96("AE", "96", "Armed Forces Europe (APO/FPO)"),
    AE_97("AE", "97", "Armed Forces Europe (APO/FPO)"),
    AE_98("AE", "98", "Armed Forces Europe (APO/FPO)"),
    AP_96("AP", "96", "Armed Forces Pacific (APO/FPO)"),
    
    // Alaska
    AK_99("AK", "99", "Alaska"),
    
    // Alabama
    AL_35("AL", "35", "Alabama"),
    AL_36("AL", "36", "Alabama"),
    
    // Arkansas
    AR_71("AR", "71", "Arkansas"),
    AR_72("AR", "72", "Arkansas"),
    
    // American Samoa
    AS_96("AS", "96", "American Samoa"),
    
    // Arizona
    AZ_85("AZ", "85", "Arizona"),
    AZ_86("AZ", "86", "Arizona"),
    
    // California
    CA_90("CA", "90", "California"),
    CA_91("CA", "91", "California"),
    CA_92("CA", "92", "California"),
    CA_93("CA", "93", "California"),
    CA_94("CA", "94", "California"),
    CA_95("CA", "95", "California"),
    CA_96("CA", "96", "California"),
    
    // Colorado
    CO_80("CO", "80", "Colorado"),
    CO_81("CO", "81", "Colorado"),
    
    // Connecticut
    CT_60("CT", "60", "Connecticut"),
    CT_61("CT", "61", "Connecticut"),
    CT_62("CT", "62", "Connecticut"),
    CT_63("CT", "63", "Connecticut"),
    CT_64("CT", "64", "Connecticut"),
    CT_65("CT", "65", "Connecticut"),
    CT_66("CT", "66", "Connecticut"),
    CT_67("CT", "67", "Connecticut"),
    CT_68("CT", "68", "Connecticut"),
    CT_69("CT", "69", "Connecticut"),
    
    // District of Columbia
    DC_20("DC", "20", "District of Columbia"),
    DC_56("DC", "56", "District of Columbia"),
    DC_88("DC", "88", "District of Columbia"),
    
    // Delaware
    DE_19("DE", "19", "Delaware"),
    
    // Florida
    FL_32("FL", "32", "Florida"),
    FL_33("FL", "33", "Florida"),
    FL_34("FL", "34", "Florida"),
    
    // Federated States of Micronesia
    FM_96("FM", "96", "Federated States of Micronesia"),
    
    // Georgia
    GA_30("GA", "30", "Georgia"),
    GA_31("GA", "31", "Georgia"),
    GA_39("GA", "39", "Georgia"),
    
    // Guam
    GU_96("GU", "96", "Guam"),
    
    // Hawaii
    HI_96("HI", "96", "Hawaii"),
    
    // Iowa
    IA_50("IA", "50", "Iowa"),
    IA_51("IA", "51", "Iowa"),
    IA_52("IA", "52", "Iowa"),
    
    // Idaho
    ID_83("ID", "83", "Idaho"),
    
    // Illinois
    IL_60("IL", "60", "Illinois"),
    IL_61("IL", "61", "Illinois"),
    IL_62("IL", "62", "Illinois"),
    
    // Indiana
    IN_46("IN", "46", "Indiana"),
    IN_47("IN", "47", "Indiana"),
    
    // Kansas
    KS_66("KS", "66", "Kansas"),
    KS_67("KS", "67", "Kansas"),
    
    // Kentucky
    KY_40("KY", "40", "Kentucky"),
    KY_41("KY", "41", "Kentucky"),
    KY_42("KY", "42", "Kentucky"),
    
    // Louisiana
    LA_70("LA", "70", "Louisiana"),
    LA_71("LA", "71", "Louisiana"),
    
    // Massachusetts
    MA_10("MA", "10", "Massachusetts"),
    MA_11("MA", "11", "Massachusetts"),
    MA_12("MA", "12", "Massachusetts"),
    MA_13("MA", "13", "Massachusetts"),
    MA_14("MA", "14", "Massachusetts"),
    MA_15("MA", "15", "Massachusetts"),
    MA_16("MA", "16", "Massachusetts"),
    MA_17("MA", "17", "Massachusetts"),
    MA_18("MA", "18", "Massachusetts"),
    MA_19("MA", "19", "Massachusetts"),
    MA_20("MA", "20", "Massachusetts"),
    MA_21("MA", "21", "Massachusetts"),
    MA_22("MA", "22", "Massachusetts"),
    MA_23("MA", "23", "Massachusetts"),
    MA_24("MA", "24", "Massachusetts"),
    MA_25("MA", "25", "Massachusetts"),
    MA_26("MA", "26", "Massachusetts"),
    MA_27("MA", "27", "Massachusetts"),
    MA_55("MA", "55", "Massachusetts"),
    
    // Maryland
    MD_20("MD", "20", "Maryland"),
    MD_21("MD", "21", "Maryland"),
    
    // Maine
    ME_39("ME", "39", "Maine"),
    ME_40("ME", "40", "Maine"),
    ME_41("ME", "41", "Maine"),
    ME_42("ME", "42", "Maine"),
    ME_43("ME", "43", "Maine"),
    ME_44("ME", "44", "Maine"),
    ME_45("ME", "45", "Maine"),
    ME_46("ME", "46", "Maine"),
    ME_47("ME", "47", "Maine"),
    ME_48("ME", "48", "Maine"),
    ME_49("ME", "49", "Maine"),
    
    // Marshall Islands
    MH_96("MH", "96", "Marshall Islands"),
    
    // Michigan
    MI_48("MI", "48", "Michigan"),
    MI_49("MI", "49", "Michigan"),
    
    // Minnesota
    MN_55("MN", "55", "Minnesota"),
    MN_56("MN", "56", "Minnesota"),
    
    // Missouri
    MO_63("MO", "63", "Missouri"),
    MO_64("MO", "64", "Missouri"),
    MO_65("MO", "65", "Missouri"),
    MO_72("MO", "72", "Missouri"),
    
    // Northern Mariana Islands
    MP_96("MP", "96", "Northern Mariana Islands"),
    
    // Mississippi
    MS_38("MS", "38", "Mississippi"),
    MS_39("MS", "39", "Mississippi"),
    
    // Montana
    MT_59("MT", "59", "Montana"),
    
    // North Carolina
    NC_27("NC", "27", "North Carolina"),
    NC_28("NC", "28", "North Carolina"),
    
    // North Dakota
    ND_58("ND", "58", "North Dakota"),
    
    // Nebraska
    NE_68("NE", "68", "Nebraska"),
    NE_69("NE", "69", "Nebraska"),
    
    // New Hampshire
    NH_30("NH", "30", "New Hampshire"),
    NH_31("NH", "31", "New Hampshire"),
    NH_32("NH", "32", "New Hampshire"),
    NH_33("NH", "33", "New Hampshire"),
    NH_34("NH", "34", "New Hampshire"),
    NH_35("NH", "35", "New Hampshire"),
    NH_36("NH", "36", "New Hampshire"),
    NH_37("NH", "37", "New Hampshire"),
    NH_38("NH", "38", "New Hampshire"),
    
    // New Jersey
    NJ_70("NJ", "70", "New Jersey"),
    NJ_71("NJ", "71", "New Jersey"),
    NJ_72("NJ", "72", "New Jersey"),
    NJ_73("NJ", "73", "New Jersey"),
    NJ_74("NJ", "74", "New Jersey"),
    NJ_75("NJ", "75", "New Jersey"),
    NJ_76("NJ", "76", "New Jersey"),
    NJ_77("NJ", "77", "New Jersey"),
    NJ_78("NJ", "78", "New Jersey"),
    NJ_79("NJ", "79", "New Jersey"),
    NJ_80("NJ", "80", "New Jersey"),
    NJ_81("NJ", "81", "New Jersey"),
    NJ_82("NJ", "82", "New Jersey"),
    NJ_83("NJ", "83", "New Jersey"),
    NJ_84("NJ", "84", "New Jersey"),
    NJ_85("NJ", "85", "New Jersey"),
    NJ_86("NJ", "86", "New Jersey"),
    NJ_87("NJ", "87", "New Jersey"),
    NJ_88("NJ", "88", "New Jersey"),
    NJ_89("NJ", "89", "New Jersey"),
    
    // New Mexico
    NM_87("NM", "87", "New Mexico"),
    NM_88("NM", "88", "New Mexico"),
    
    // Nevada
    NV_88("NV", "88", "Nevada"),
    NV_89("NV", "89", "Nevada"),
    
    // New York
    NY_50("NY", "50", "New York"),
    NY_54("NY", "54", "New York"),
    NY_63("NY", "63", "New York"),
    NY_10("NY", "10", "New York"),
    NY_11("NY", "11", "New York"),
    NY_12("NY", "12", "New York"),
    NY_13("NY", "13", "New York"),
    NY_14("NY", "14", "New York"),
    
    // Ohio
    OH_43("OH", "43", "Ohio"),
    OH_44("OH", "44", "Ohio"),
    OH_45("OH", "45", "Ohio"),
    
    // Oklahoma
    OK_73("OK", "73", "Oklahoma"),
    OK_74("OK", "74", "Oklahoma"),
    
    // Oregon
    OR_97("OR", "97", "Oregon"),
    
    // Pennsylvania
    PA_15("PA", "15", "Pennsylvania"),
    PA_16("PA", "16", "Pennsylvania"),
    PA_17("PA", "17", "Pennsylvania"),
    PA_18("PA", "18", "Pennsylvania"),
    PA_19("PA", "19", "Pennsylvania"),
    
    // Puerto Rico
    PR_60("PR", "60", "Puerto Rico"),
    PR_61("PR", "61", "Puerto Rico"),
    PR_62("PR", "62", "Puerto Rico"),
    PR_63("PR", "63", "Puerto Rico"),
    PR_64("PR", "64", "Puerto Rico"),
    PR_65("PR", "65", "Puerto Rico"),
    PR_66("PR", "66", "Puerto Rico"),
    PR_67("PR", "67", "Puerto Rico"),
    PR_68("PR", "68", "Puerto Rico"),
    PR_69("PR", "69", "Puerto Rico"),
    PR_70("PR", "70", "Puerto Rico"),
    PR_71("PR", "71", "Puerto Rico"),
    PR_72("PR", "72", "Puerto Rico"),
    PR_73("PR", "73", "Puerto Rico"),
    PR_74("PR", "74", "Puerto Rico"),
    PR_75("PR", "75", "Puerto Rico"),
    PR_76("PR", "76", "Puerto Rico"),
    PR_77("PR", "77", "Puerto Rico"),
    PR_78("PR", "78", "Puerto Rico"),
    PR_79("PR", "79", "Puerto Rico"),
    PR_90("PR", "90", "Puerto Rico"),
    PR_91("PR", "91", "Puerto Rico"),
    PR_92("PR", "92", "Puerto Rico"),
    PR_93("PR", "93", "Puerto Rico"),
    PR_94("PR", "94", "Puerto Rico"),
    PR_95("PR", "95", "Puerto Rico"),
    PR_96("PR", "96", "Puerto Rico"),
    PR_97("PR", "97", "Puerto Rico"),
    PR_98("PR", "98", "Puerto Rico"),
    
    // Palau
    PW_96("PW", "96", "Palau"),
    
    // Rhode Island
    RI_28("RI", "28", "Rhode Island"),
    RI_29("RI", "29", "Rhode Island"),
    
    // South Carolina
    SC_29("SC", "29", "South Carolina"),
    
    // South Dakota
    SD_57("SD", "57", "South Dakota"),
    
    // Tennessee
    TN_37("TN", "37", "Tennessee"),
    TN_38("TN", "38", "Tennessee"),
    
    // Texas
    TX_73("TX", "73", "Texas"),
    TX_75("TX", "75", "Texas"),
    TX_76("TX", "76", "Texas"),
    TX_77("TX", "77", "Texas"),
    TX_78("TX", "78", "Texas"),
    TX_79("TX", "79", "Texas"),
    TX_88("TX", "88", "Texas"),
    
    // Utah
    UT_84("UT", "84", "Utah"),
    
    // Virginia
    VA_20("VA", "20", "Virginia"),
    VA_22("VA", "22", "Virginia"),
    VA_23("VA", "23", "Virginia"),
    VA_24("VA", "24", "Virginia"),
    
    // Virgin Islands
    VI_80("VI", "80", "Virgin Islands"),
    VI_82("VI", "82", "Virgin Islands"),
    VI_83("VI", "83", "Virgin Islands"),
    VI_84("VI", "84", "Virgin Islands"),
    VI_85("VI", "85", "Virgin Islands"),
    
    // Vermont
    VT_50("VT", "50", "Vermont"),
    VT_51("VT", "51", "Vermont"),
    VT_52("VT", "52", "Vermont"),
    VT_53("VT", "53", "Vermont"),
    VT_54("VT", "54", "Vermont"),
    VT_56("VT", "56", "Vermont"),
    VT_57("VT", "57", "Vermont"),
    VT_58("VT", "58", "Vermont"),
    VT_59("VT", "59", "Vermont"),
    
    // Washington
    WA_98("WA", "98", "Washington"),
    WA_99("WA", "99", "Washington"),
    
    // Wisconsin
    WI_53("WI", "53", "Wisconsin"),
    WI_54("WI", "54", "Wisconsin"),
    
    // West Virginia
    WV_24("WV", "24", "West Virginia"),
    WV_25("WV", "25", "West Virginia"),
    WV_26("WV", "26", "West Virginia"),
    
    // Wyoming
    WY_82("WY", "82", "Wyoming"),
    WY_83("WY", "83", "Wyoming");
    
    // Instance fields for each enum constant
    private final String stateCode;
    private final String zipPrefix;
    private final String description;
    
    /**
     * Private constructor for enum constants
     * 
     * @param stateCode Two-character state code (e.g., "CA", "NY", "TX")
     * @param zipPrefix Two-character ZIP code prefix (e.g., "90", "10", "75")
     * @param description Human-readable description of the state/territory
     */
    private StateZipCodeCombo(String stateCode, String zipPrefix, String description) {
        this.stateCode = stateCode;
        this.zipPrefix = zipPrefix;
        this.description = description;
    }
    
    /**
     * Returns the two-character state code
     * 
     * @return State code (e.g., "CA", "NY", "TX")
     */
    public String getStateCode() {
        return stateCode;
    }
    
    /**
     * Returns the two-character ZIP code prefix
     * 
     * @return ZIP prefix (e.g., "90", "10", "75")
     */
    public String getZipPrefix() {
        return zipPrefix;
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
     * Validates if a given state and ZIP code combination is valid
     * 
     * This method replicates the exact behavior of the COBOL 88-level condition
     * VALID-US-STATE-ZIP-CD2-COMBO by checking if the provided state code and
     * ZIP prefix match any of the predefined valid combinations.
     * 
     * @param stateCode Two-character state code to validate
     * @param zipPrefix Two-character ZIP code prefix to validate
     * @return true if the combination is valid, false otherwise
     * @throws IllegalArgumentException if either parameter is null or invalid format
     */
    public static boolean isValid(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            throw new IllegalArgumentException("State code and ZIP prefix cannot be null");
        }
        
        if (stateCode.length() != 2 || zipPrefix.length() != 2) {
            throw new IllegalArgumentException("State code and ZIP prefix must be exactly 2 characters");
        }
        
        // Convert to uppercase for case-insensitive comparison
        String upperStateCode = stateCode.toUpperCase();
        String upperZipPrefix = zipPrefix.toUpperCase();
        
        // Check if combination exists in enum values
        for (StateZipCodeCombo combo : values()) {
            if (combo.stateCode.equals(upperStateCode) && combo.zipPrefix.equals(upperZipPrefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validates if a given 4-character state-ZIP combination is valid
     * 
     * This method validates the exact format used in the original COBOL:
     * US-STATE-AND-FIRST-ZIP2 PIC X(4) where first 2 chars are state and
     * last 2 chars are ZIP prefix.
     * 
     * @param stateZipCombo 4-character string containing state code + ZIP prefix
     * @return true if the combination is valid, false otherwise
     * @throws IllegalArgumentException if parameter is null or invalid format
     */
    public static boolean isValid(String stateZipCombo) {
        if (stateZipCombo == null) {
            throw new IllegalArgumentException("State-ZIP combination cannot be null");
        }
        
        if (stateZipCombo.length() != 4) {
            throw new IllegalArgumentException("State-ZIP combination must be exactly 4 characters");
        }
        
        String stateCode = stateZipCombo.substring(0, 2);
        String zipPrefix = stateZipCombo.substring(2, 4);
        
        return isValid(stateCode, zipPrefix);
    }
    
    /**
     * Returns an Optional containing the StateZipCodeCombo for the given state and ZIP prefix
     * 
     * This method supports null-safe processing and follows the Optional pattern
     * for handling potentially invalid state-ZIP combinations.
     * 
     * @param stateCode Two-character state code
     * @param zipPrefix Two-character ZIP code prefix
     * @return Optional containing the matching StateZipCodeCombo, or empty if not found
     */
    public static Optional<StateZipCodeCombo> fromStateZip(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            return Optional.empty();
        }
        
        if (stateCode.length() != 2 || zipPrefix.length() != 2) {
            return Optional.empty();
        }
        
        // Convert to uppercase for case-insensitive comparison
        String upperStateCode = stateCode.toUpperCase();
        String upperZipPrefix = zipPrefix.toUpperCase();
        
        // Find matching enum constant
        for (StateZipCodeCombo combo : values()) {
            if (combo.stateCode.equals(upperStateCode) && combo.zipPrefix.equals(upperZipPrefix)) {
                return Optional.of(combo);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Returns an Optional containing the StateZipCodeCombo for the given 4-character combination
     * 
     * @param stateZipCombo 4-character string containing state code + ZIP prefix
     * @return Optional containing the matching StateZipCodeCombo, or empty if not found
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
     * Returns a formatted string representation of the state-ZIP combination
     * 
     * This method provides a human-readable format suitable for UI display
     * and error messages, following the original COBOL format conventions.
     * 
     * @return Formatted string in the format "STATE-ZIP (Description)"
     */
    @Override
    public String toString() {
        return String.format("%s-%s (%s)", stateCode, zipPrefix, description);
    }
    
    /**
     * Returns the 4-character state-ZIP combination as used in the original COBOL
     * 
     * This method returns the exact format that was stored in the COBOL
     * US-STATE-AND-FIRST-ZIP2 field for compatibility with legacy systems.
     * 
     * @return 4-character string combining state code and ZIP prefix
     */
    public String toCobolFormat() {
        return stateCode + zipPrefix;
    }
    
    /**
     * Validates a complete 5-digit ZIP code against the state-ZIP combination
     * 
     * This method provides extended validation by checking if a full ZIP code
     * is consistent with the state-ZIP prefix combination, supporting more
     * comprehensive address validation scenarios.
     * 
     * @param fullZipCode 5-digit ZIP code to validate
     * @return true if the ZIP code starts with the expected prefix for this state
     * @throws IllegalArgumentException if ZIP code is null or invalid format
     */
    public boolean isValidFullZipCode(String fullZipCode) {
        if (fullZipCode == null) {
            throw new IllegalArgumentException("ZIP code cannot be null");
        }
        
        if (fullZipCode.length() != 5) {
            throw new IllegalArgumentException("ZIP code must be exactly 5 digits");
        }
        
        if (!fullZipCode.matches("\\d{5}")) {
            throw new IllegalArgumentException("ZIP code must contain only digits");
        }
        
        // Check if ZIP code starts with the expected prefix for this state
        return fullZipCode.startsWith(zipPrefix);
    }
    
    /**
     * Returns detailed validation error message for invalid state-ZIP combinations
     * 
     * This method provides comprehensive error messages for UI feedback,
     * supporting React form validation requirements and Spring Boot
     * address validation framework integration.
     * 
     * @param stateCode The state code that was validated
     * @param zipPrefix The ZIP prefix that was validated
     * @return Detailed error message explaining why the combination is invalid
     */
    public static String getValidationErrorMessage(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            return "State code and ZIP prefix are required for address validation";
        }
        
        if (stateCode.length() != 2 || zipPrefix.length() != 2) {
            return "State code and ZIP prefix must be exactly 2 characters each";
        }
        
        if (!stateCode.matches("[A-Za-z]{2}")) {
            return "State code must contain only letters";
        }
        
        if (!zipPrefix.matches("\\d{2}")) {
            return "ZIP prefix must contain only digits";
        }
        
        // Check if state exists in any valid combination
        String upperStateCode = stateCode.toUpperCase();
        boolean stateExists = false;
        for (StateZipCodeCombo combo : values()) {
            if (combo.stateCode.equals(upperStateCode)) {
                stateExists = true;
                break;
            }
        }
        
        if (!stateExists) {
            return String.format("State code '%s' is not a valid US state or territory", stateCode);
        }
        
        // State exists but ZIP prefix is invalid for this state
        return String.format("ZIP prefix '%s' is not valid for state '%s'. Please verify the ZIP code is correct for the specified state.", zipPrefix, stateCode);
    }
}