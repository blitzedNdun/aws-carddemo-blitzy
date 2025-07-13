package com.carddemo.common.enums;

import java.util.Optional;
import jakarta.validation.Valid;

/**
 * State and ZIP code combination validation enum converted from COBOL VALID-US-STATE-ZIP-CD2-COMBO
 * 88-level condition for comprehensive address validation.
 * 
 * <p>This enum preserves exact COBOL cross-reference validation behavior for data integrity
 * and supports complex validation scenarios with detailed error messages for user interface 
 * feedback. The combination validation maintains compatibility with postal service address 
 * verification standards.</p>
 * 
 * <p>All 240 valid state/ZIP combinations from the original COBOL VALUES are maintained
 * with exact validation logic for Spring Boot address validation and React form validation.</p>
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since CardDemo v1.0
 */
public enum StateZipCodeCombo {
    
    // Military and Federal Postal Codes (AA, AE, AP)
    AA_34("AA", "34", "Armed Forces Americas - ZIP prefix 34"),
    AE_90("AE", "90", "Armed Forces Europe - ZIP prefix 90"),
    AE_91("AE", "91", "Armed Forces Europe - ZIP prefix 91"),
    AE_92("AE", "92", "Armed Forces Europe - ZIP prefix 92"),
    AE_93("AE", "93", "Armed Forces Europe - ZIP prefix 93"),
    AE_94("AE", "94", "Armed Forces Europe - ZIP prefix 94"),
    AE_95("AE", "95", "Armed Forces Europe - ZIP prefix 95"),
    AE_96("AE", "96", "Armed Forces Europe - ZIP prefix 96"),
    AE_97("AE", "97", "Armed Forces Europe - ZIP prefix 97"),
    AE_98("AE", "98", "Armed Forces Europe - ZIP prefix 98"),
    AP_96("AP", "96", "Armed Forces Pacific - ZIP prefix 96"),
    
    // US States and Territories
    AK_99("AK", "99", "Alaska - ZIP prefix 99"),
    AL_35("AL", "35", "Alabama - ZIP prefix 35"),
    AL_36("AL", "36", "Alabama - ZIP prefix 36"),
    AR_71("AR", "71", "Arkansas - ZIP prefix 71"),
    AR_72("AR", "72", "Arkansas - ZIP prefix 72"),
    AS_96("AS", "96", "American Samoa - ZIP prefix 96"),
    AZ_85("AZ", "85", "Arizona - ZIP prefix 85"),
    AZ_86("AZ", "86", "Arizona - ZIP prefix 86"),
    
    // California
    CA_90("CA", "90", "California - ZIP prefix 90"),
    CA_91("CA", "91", "California - ZIP prefix 91"),
    CA_92("CA", "92", "California - ZIP prefix 92"),
    CA_93("CA", "93", "California - ZIP prefix 93"),
    CA_94("CA", "94", "California - ZIP prefix 94"),
    CA_95("CA", "95", "California - ZIP prefix 95"),
    CA_96("CA", "96", "California - ZIP prefix 96"),
    
    // Colorado
    CO_80("CO", "80", "Colorado - ZIP prefix 80"),
    CO_81("CO", "81", "Colorado - ZIP prefix 81"),
    
    // Connecticut
    CT_60("CT", "60", "Connecticut - ZIP prefix 60"),
    CT_61("CT", "61", "Connecticut - ZIP prefix 61"),
    CT_62("CT", "62", "Connecticut - ZIP prefix 62"),
    CT_63("CT", "63", "Connecticut - ZIP prefix 63"),
    CT_64("CT", "64", "Connecticut - ZIP prefix 64"),
    CT_65("CT", "65", "Connecticut - ZIP prefix 65"),
    CT_66("CT", "66", "Connecticut - ZIP prefix 66"),
    CT_67("CT", "67", "Connecticut - ZIP prefix 67"),
    CT_68("CT", "68", "Connecticut - ZIP prefix 68"),
    CT_69("CT", "69", "Connecticut - ZIP prefix 69"),
    
    // District of Columbia
    DC_20("DC", "20", "District of Columbia - ZIP prefix 20"),
    DC_56("DC", "56", "District of Columbia - ZIP prefix 56"),
    DC_88("DC", "88", "District of Columbia - ZIP prefix 88"),
    
    // Delaware
    DE_19("DE", "19", "Delaware - ZIP prefix 19"),
    
    // Florida
    FL_32("FL", "32", "Florida - ZIP prefix 32"),
    FL_33("FL", "33", "Florida - ZIP prefix 33"),
    FL_34("FL", "34", "Florida - ZIP prefix 34"),
    
    // Federal States of Micronesia
    FM_96("FM", "96", "Federal States of Micronesia - ZIP prefix 96"),
    
    // Georgia
    GA_30("GA", "30", "Georgia - ZIP prefix 30"),
    GA_31("GA", "31", "Georgia - ZIP prefix 31"),
    GA_39("GA", "39", "Georgia - ZIP prefix 39"),
    
    // Guam
    GU_96("GU", "96", "Guam - ZIP prefix 96"),
    
    // Hawaii
    HI_96("HI", "96", "Hawaii - ZIP prefix 96"),
    
    // Iowa
    IA_50("IA", "50", "Iowa - ZIP prefix 50"),
    IA_51("IA", "51", "Iowa - ZIP prefix 51"),
    IA_52("IA", "52", "Iowa - ZIP prefix 52"),
    
    // Idaho
    ID_83("ID", "83", "Idaho - ZIP prefix 83"),
    
    // Illinois
    IL_60("IL", "60", "Illinois - ZIP prefix 60"),
    IL_61("IL", "61", "Illinois - ZIP prefix 61"),
    IL_62("IL", "62", "Illinois - ZIP prefix 62"),
    
    // Indiana
    IN_46("IN", "46", "Indiana - ZIP prefix 46"),
    IN_47("IN", "47", "Indiana - ZIP prefix 47"),
    
    // Kansas
    KS_66("KS", "66", "Kansas - ZIP prefix 66"),
    KS_67("KS", "67", "Kansas - ZIP prefix 67"),
    
    // Kentucky
    KY_40("KY", "40", "Kentucky - ZIP prefix 40"),
    KY_41("KY", "41", "Kentucky - ZIP prefix 41"),
    KY_42("KY", "42", "Kentucky - ZIP prefix 42"),
    
    // Louisiana
    LA_70("LA", "70", "Louisiana - ZIP prefix 70"),
    LA_71("LA", "71", "Louisiana - ZIP prefix 71"),
    
    // Massachusetts
    MA_10("MA", "10", "Massachusetts - ZIP prefix 10"),
    MA_11("MA", "11", "Massachusetts - ZIP prefix 11"),
    MA_12("MA", "12", "Massachusetts - ZIP prefix 12"),
    MA_13("MA", "13", "Massachusetts - ZIP prefix 13"),
    MA_14("MA", "14", "Massachusetts - ZIP prefix 14"),
    MA_15("MA", "15", "Massachusetts - ZIP prefix 15"),
    MA_16("MA", "16", "Massachusetts - ZIP prefix 16"),
    MA_17("MA", "17", "Massachusetts - ZIP prefix 17"),
    MA_18("MA", "18", "Massachusetts - ZIP prefix 18"),
    MA_19("MA", "19", "Massachusetts - ZIP prefix 19"),
    MA_20("MA", "20", "Massachusetts - ZIP prefix 20"),
    MA_21("MA", "21", "Massachusetts - ZIP prefix 21"),
    MA_22("MA", "22", "Massachusetts - ZIP prefix 22"),
    MA_23("MA", "23", "Massachusetts - ZIP prefix 23"),
    MA_24("MA", "24", "Massachusetts - ZIP prefix 24"),
    MA_25("MA", "25", "Massachusetts - ZIP prefix 25"),
    MA_26("MA", "26", "Massachusetts - ZIP prefix 26"),
    MA_27("MA", "27", "Massachusetts - ZIP prefix 27"),
    MA_55("MA", "55", "Massachusetts - ZIP prefix 55"),
    
    // Maryland
    MD_20("MD", "20", "Maryland - ZIP prefix 20"),
    MD_21("MD", "21", "Maryland - ZIP prefix 21"),
    
    // Maine
    ME_39("ME", "39", "Maine - ZIP prefix 39"),
    ME_40("ME", "40", "Maine - ZIP prefix 40"),
    ME_41("ME", "41", "Maine - ZIP prefix 41"),
    ME_42("ME", "42", "Maine - ZIP prefix 42"),
    ME_43("ME", "43", "Maine - ZIP prefix 43"),
    ME_44("ME", "44", "Maine - ZIP prefix 44"),
    ME_45("ME", "45", "Maine - ZIP prefix 45"),
    ME_46("ME", "46", "Maine - ZIP prefix 46"),
    ME_47("ME", "47", "Maine - ZIP prefix 47"),
    ME_48("ME", "48", "Maine - ZIP prefix 48"),
    ME_49("ME", "49", "Maine - ZIP prefix 49"),
    
    // Marshall Islands
    MH_96("MH", "96", "Marshall Islands - ZIP prefix 96"),
    
    // Michigan
    MI_48("MI", "48", "Michigan - ZIP prefix 48"),
    MI_49("MI", "49", "Michigan - ZIP prefix 49"),
    
    // Minnesota
    MN_55("MN", "55", "Minnesota - ZIP prefix 55"),
    MN_56("MN", "56", "Minnesota - ZIP prefix 56"),
    
    // Missouri
    MO_63("MO", "63", "Missouri - ZIP prefix 63"),
    MO_64("MO", "64", "Missouri - ZIP prefix 64"),
    MO_65("MO", "65", "Missouri - ZIP prefix 65"),
    MO_72("MO", "72", "Missouri - ZIP prefix 72"),
    
    // Northern Mariana Islands
    MP_96("MP", "96", "Northern Mariana Islands - ZIP prefix 96"),
    
    // Mississippi
    MS_38("MS", "38", "Mississippi - ZIP prefix 38"),
    MS_39("MS", "39", "Mississippi - ZIP prefix 39"),
    
    // Montana
    MT_59("MT", "59", "Montana - ZIP prefix 59"),
    
    // North Carolina
    NC_27("NC", "27", "North Carolina - ZIP prefix 27"),
    NC_28("NC", "28", "North Carolina - ZIP prefix 28"),
    
    // North Dakota
    ND_58("ND", "58", "North Dakota - ZIP prefix 58"),
    
    // Nebraska
    NE_68("NE", "68", "Nebraska - ZIP prefix 68"),
    NE_69("NE", "69", "Nebraska - ZIP prefix 69"),
    
    // New Hampshire
    NH_30("NH", "30", "New Hampshire - ZIP prefix 30"),
    NH_31("NH", "31", "New Hampshire - ZIP prefix 31"),
    NH_32("NH", "32", "New Hampshire - ZIP prefix 32"),
    NH_33("NH", "33", "New Hampshire - ZIP prefix 33"),
    NH_34("NH", "34", "New Hampshire - ZIP prefix 34"),
    NH_35("NH", "35", "New Hampshire - ZIP prefix 35"),
    NH_36("NH", "36", "New Hampshire - ZIP prefix 36"),
    NH_37("NH", "37", "New Hampshire - ZIP prefix 37"),
    NH_38("NH", "38", "New Hampshire - ZIP prefix 38"),
    
    // New Jersey
    NJ_70("NJ", "70", "New Jersey - ZIP prefix 70"),
    NJ_71("NJ", "71", "New Jersey - ZIP prefix 71"),
    NJ_72("NJ", "72", "New Jersey - ZIP prefix 72"),
    NJ_73("NJ", "73", "New Jersey - ZIP prefix 73"),
    NJ_74("NJ", "74", "New Jersey - ZIP prefix 74"),
    NJ_75("NJ", "75", "New Jersey - ZIP prefix 75"),
    NJ_76("NJ", "76", "New Jersey - ZIP prefix 76"),
    NJ_77("NJ", "77", "New Jersey - ZIP prefix 77"),
    NJ_78("NJ", "78", "New Jersey - ZIP prefix 78"),
    NJ_79("NJ", "79", "New Jersey - ZIP prefix 79"),
    NJ_80("NJ", "80", "New Jersey - ZIP prefix 80"),
    NJ_81("NJ", "81", "New Jersey - ZIP prefix 81"),
    NJ_82("NJ", "82", "New Jersey - ZIP prefix 82"),
    NJ_83("NJ", "83", "New Jersey - ZIP prefix 83"),
    NJ_84("NJ", "84", "New Jersey - ZIP prefix 84"),
    NJ_85("NJ", "85", "New Jersey - ZIP prefix 85"),
    NJ_86("NJ", "86", "New Jersey - ZIP prefix 86"),
    NJ_87("NJ", "87", "New Jersey - ZIP prefix 87"),
    NJ_88("NJ", "88", "New Jersey - ZIP prefix 88"),
    NJ_89("NJ", "89", "New Jersey - ZIP prefix 89"),
    
    // New Mexico
    NM_87("NM", "87", "New Mexico - ZIP prefix 87"),
    NM_88("NM", "88", "New Mexico - ZIP prefix 88"),
    
    // Nevada
    NV_88("NV", "88", "Nevada - ZIP prefix 88"),
    NV_89("NV", "89", "Nevada - ZIP prefix 89"),
    
    // New York
    NY_10("NY", "10", "New York - ZIP prefix 10"),
    NY_11("NY", "11", "New York - ZIP prefix 11"),
    NY_12("NY", "12", "New York - ZIP prefix 12"),
    NY_13("NY", "13", "New York - ZIP prefix 13"),
    NY_14("NY", "14", "New York - ZIP prefix 14"),
    NY_50("NY", "50", "New York - ZIP prefix 50"),
    NY_54("NY", "54", "New York - ZIP prefix 54"),
    NY_63("NY", "63", "New York - ZIP prefix 63"),
    
    // Ohio
    OH_43("OH", "43", "Ohio - ZIP prefix 43"),
    OH_44("OH", "44", "Ohio - ZIP prefix 44"),
    OH_45("OH", "45", "Ohio - ZIP prefix 45"),
    
    // Oklahoma
    OK_73("OK", "73", "Oklahoma - ZIP prefix 73"),
    OK_74("OK", "74", "Oklahoma - ZIP prefix 74"),
    
    // Oregon
    OR_97("OR", "97", "Oregon - ZIP prefix 97"),
    
    // Pennsylvania
    PA_15("PA", "15", "Pennsylvania - ZIP prefix 15"),
    PA_16("PA", "16", "Pennsylvania - ZIP prefix 16"),
    PA_17("PA", "17", "Pennsylvania - ZIP prefix 17"),
    PA_18("PA", "18", "Pennsylvania - ZIP prefix 18"),
    PA_19("PA", "19", "Pennsylvania - ZIP prefix 19"),
    
    // Puerto Rico
    PR_60("PR", "60", "Puerto Rico - ZIP prefix 60"),
    PR_61("PR", "61", "Puerto Rico - ZIP prefix 61"),
    PR_62("PR", "62", "Puerto Rico - ZIP prefix 62"),
    PR_63("PR", "63", "Puerto Rico - ZIP prefix 63"),
    PR_64("PR", "64", "Puerto Rico - ZIP prefix 64"),
    PR_65("PR", "65", "Puerto Rico - ZIP prefix 65"),
    PR_66("PR", "66", "Puerto Rico - ZIP prefix 66"),
    PR_67("PR", "67", "Puerto Rico - ZIP prefix 67"),
    PR_68("PR", "68", "Puerto Rico - ZIP prefix 68"),
    PR_69("PR", "69", "Puerto Rico - ZIP prefix 69"),
    PR_70("PR", "70", "Puerto Rico - ZIP prefix 70"),
    PR_71("PR", "71", "Puerto Rico - ZIP prefix 71"),
    PR_72("PR", "72", "Puerto Rico - ZIP prefix 72"),
    PR_73("PR", "73", "Puerto Rico - ZIP prefix 73"),
    PR_74("PR", "74", "Puerto Rico - ZIP prefix 74"),
    PR_75("PR", "75", "Puerto Rico - ZIP prefix 75"),
    PR_76("PR", "76", "Puerto Rico - ZIP prefix 76"),
    PR_77("PR", "77", "Puerto Rico - ZIP prefix 77"),
    PR_78("PR", "78", "Puerto Rico - ZIP prefix 78"),
    PR_79("PR", "79", "Puerto Rico - ZIP prefix 79"),
    PR_90("PR", "90", "Puerto Rico - ZIP prefix 90"),
    PR_91("PR", "91", "Puerto Rico - ZIP prefix 91"),
    PR_92("PR", "92", "Puerto Rico - ZIP prefix 92"),
    PR_93("PR", "93", "Puerto Rico - ZIP prefix 93"),
    PR_94("PR", "94", "Puerto Rico - ZIP prefix 94"),
    PR_95("PR", "95", "Puerto Rico - ZIP prefix 95"),
    PR_96("PR", "96", "Puerto Rico - ZIP prefix 96"),
    PR_97("PR", "97", "Puerto Rico - ZIP prefix 97"),
    PR_98("PR", "98", "Puerto Rico - ZIP prefix 98"),
    
    // Palau
    PW_96("PW", "96", "Palau - ZIP prefix 96"),
    
    // Rhode Island
    RI_28("RI", "28", "Rhode Island - ZIP prefix 28"),
    RI_29("RI", "29", "Rhode Island - ZIP prefix 29"),
    
    // South Carolina
    SC_29("SC", "29", "South Carolina - ZIP prefix 29"),
    
    // South Dakota
    SD_57("SD", "57", "South Dakota - ZIP prefix 57"),
    
    // Tennessee
    TN_37("TN", "37", "Tennessee - ZIP prefix 37"),
    TN_38("TN", "38", "Tennessee - ZIP prefix 38"),
    
    // Texas
    TX_73("TX", "73", "Texas - ZIP prefix 73"),
    TX_75("TX", "75", "Texas - ZIP prefix 75"),
    TX_76("TX", "76", "Texas - ZIP prefix 76"),
    TX_77("TX", "77", "Texas - ZIP prefix 77"),
    TX_78("TX", "78", "Texas - ZIP prefix 78"),
    TX_79("TX", "79", "Texas - ZIP prefix 79"),
    TX_88("TX", "88", "Texas - ZIP prefix 88"),
    
    // Utah
    UT_84("UT", "84", "Utah - ZIP prefix 84"),
    
    // Virginia
    VA_20("VA", "20", "Virginia - ZIP prefix 20"),
    VA_22("VA", "22", "Virginia - ZIP prefix 22"),
    VA_23("VA", "23", "Virginia - ZIP prefix 23"),
    VA_24("VA", "24", "Virginia - ZIP prefix 24"),
    
    // U.S. Virgin Islands
    VI_80("VI", "80", "U.S. Virgin Islands - ZIP prefix 80"),
    VI_82("VI", "82", "U.S. Virgin Islands - ZIP prefix 82"),
    VI_83("VI", "83", "U.S. Virgin Islands - ZIP prefix 83"),
    VI_84("VI", "84", "U.S. Virgin Islands - ZIP prefix 84"),
    VI_85("VI", "85", "U.S. Virgin Islands - ZIP prefix 85"),
    
    // Vermont
    VT_50("VT", "50", "Vermont - ZIP prefix 50"),
    VT_51("VT", "51", "Vermont - ZIP prefix 51"),
    VT_52("VT", "52", "Vermont - ZIP prefix 52"),
    VT_53("VT", "53", "Vermont - ZIP prefix 53"),
    VT_54("VT", "54", "Vermont - ZIP prefix 54"),
    VT_56("VT", "56", "Vermont - ZIP prefix 56"),
    VT_57("VT", "57", "Vermont - ZIP prefix 57"),
    VT_58("VT", "58", "Vermont - ZIP prefix 58"),
    VT_59("VT", "59", "Vermont - ZIP prefix 59"),
    
    // Washington
    WA_98("WA", "98", "Washington - ZIP prefix 98"),
    WA_99("WA", "99", "Washington - ZIP prefix 99"),
    
    // Wisconsin
    WI_53("WI", "53", "Wisconsin - ZIP prefix 53"),
    WI_54("WI", "54", "Wisconsin - ZIP prefix 54"),
    
    // West Virginia
    WV_24("WV", "24", "West Virginia - ZIP prefix 24"),
    WV_25("WV", "25", "West Virginia - ZIP prefix 25"),
    WV_26("WV", "26", "West Virginia - ZIP prefix 26"),
    
    // Wyoming
    WY_82("WY", "82", "Wyoming - ZIP prefix 82"),
    WY_83("WY", "83", "Wyoming - ZIP prefix 83");
    
    // Enum fields
    private final String stateCode;
    private final String zipPrefix;
    private final String description;
    
    /**
     * Private constructor for enum values.
     * 
     * @param stateCode The 2-letter state code
     * @param zipPrefix The first 2 digits of the ZIP code
     * @param description Descriptive text for the state/ZIP combination
     */
    private StateZipCodeCombo(String stateCode, String zipPrefix, String description) {
        this.stateCode = stateCode;
        this.zipPrefix = zipPrefix;
        this.description = description;
    }
    
    /**
     * Gets the 2-letter state code for this combination.
     * 
     * @return The state code (e.g., "CA", "NY", "TX")
     */
    public String getStateCode() {
        return stateCode;
    }
    
    /**
     * Gets the first 2 digits of the ZIP code for this combination.
     * 
     * @return The ZIP prefix (e.g., "90", "10", "73")
     */
    public String getZipPrefix() {
        return zipPrefix;
    }
    
    /**
     * Gets the descriptive text for this state/ZIP combination.
     * 
     * @return The description including state name and ZIP prefix
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Validates if a given state code and ZIP prefix combination is valid.
     * 
     * <p>This method preserves the exact COBOL 88-level condition behavior from
     * VALID-US-STATE-ZIP-CD2-COMBO, ensuring data integrity and compatibility
     * with postal service address verification standards.</p>
     * 
     * @param stateCode The 2-letter state code to validate
     * @param zipPrefix The first 2 digits of the ZIP code to validate
     * @return true if the combination is valid, false otherwise
     */
    public static boolean isValid(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            return false;
        }
        
        // Normalize inputs to uppercase for case-insensitive comparison
        String normalizedState = stateCode.trim().toUpperCase();
        String normalizedZip = zipPrefix.trim();
        
        // Validate format: state code must be 2 letters, ZIP prefix must be 2 digits
        if (normalizedState.length() != 2 || normalizedZip.length() != 2) {
            return false;
        }
        
        // Check if ZIP prefix contains only digits
        if (!normalizedZip.matches("\\d{2}")) {
            return false;
        }
        
        // Check if state code contains only letters
        if (!normalizedState.matches("[A-Z]{2}")) {
            return false;
        }
        
        // Check if combination exists in our valid enum values
        return fromStateZip(normalizedState, normalizedZip).isPresent();
    }
    
    /**
     * Finds a StateZipCodeCombo enum value by state code and ZIP prefix.
     * 
     * <p>This method provides Optional-based lookup for null-safe processing
     * when parsing state-ZIP combinations that may not be valid. It supports
     * both Spring Boot backend validation and React frontend validation scenarios.</p>
     * 
     * @param stateCode The 2-letter state code
     * @param zipPrefix The first 2 digits of the ZIP code
     * @return Optional containing the matching enum value, or empty if not found
     */
    public static Optional<StateZipCodeCombo> fromStateZip(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            return Optional.empty();
        }
        
        // Normalize inputs to uppercase for case-insensitive comparison
        String normalizedState = stateCode.trim().toUpperCase();
        String normalizedZip = zipPrefix.trim();
        
        // Validate format before searching
        if (normalizedState.length() != 2 || normalizedZip.length() != 2) {
            return Optional.empty();
        }
        
        // Check if ZIP prefix contains only digits
        if (!normalizedZip.matches("\\d{2}")) {
            return Optional.empty();
        }
        
        // Check if state code contains only letters
        if (!normalizedState.matches("[A-Z]{2}")) {
            return Optional.empty();
        }
        
        // Search through all enum values for matching combination
        for (StateZipCodeCombo combo : values()) {
            if (combo.stateCode.equals(normalizedState) && combo.zipPrefix.equals(normalizedZip)) {
                return Optional.of(combo);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Validates a complete state-ZIP combination string in format "SSNN".
     * 
     * <p>This method provides comprehensive validation for address processing
     * with detailed error handling for user interface feedback. It supports
     * both React form validation and Spring Boot server-side validation.</p>
     * 
     * @param stateZipCombo The 4-character state-ZIP combination (e.g., "CA90", "NY10")
     * @return true if the combination is valid, false otherwise
     */
    public static boolean isValid(String stateZipCombo) {
        if (stateZipCombo == null) {
            return false;
        }
        
        String normalized = stateZipCombo.trim().toUpperCase();
        
        // Must be exactly 4 characters
        if (normalized.length() != 4) {
            return false;
        }
        
        // Extract state code (first 2 characters) and ZIP prefix (last 2 characters)
        String stateCode = normalized.substring(0, 2);
        String zipPrefix = normalized.substring(2, 4);
        
        return isValid(stateCode, zipPrefix);
    }
    
    /**
     * Validates an address with state and ZIP code, providing detailed error information.
     * 
     * <p>This method supports complex validation scenarios with comprehensive error
     * messages for user interface feedback. It integrates with Spring Boot validation
     * framework and provides meaningful error messages for form validation.</p>
     * 
     * @param stateCode The 2-letter state code
     * @param zipCode The complete ZIP code (can be 5 or 9 digits)
     * @return ValidationResult containing validation status and error messages
     */
    public static ValidationResult validateAddress(String stateCode, String zipCode) {
        if (stateCode == null || stateCode.trim().isEmpty()) {
            return new ValidationResult(false, "State code cannot be null or empty");
        }
        
        if (zipCode == null || zipCode.trim().isEmpty()) {
            return new ValidationResult(false, "ZIP code cannot be null or empty");
        }
        
        String normalizedState = stateCode.trim().toUpperCase();
        String normalizedZip = zipCode.trim();
        
        // Validate state code format
        if (normalizedState.length() != 2) {
            return new ValidationResult(false, "State code must be exactly 2 characters");
        }
        
        if (!normalizedState.matches("[A-Z]{2}")) {
            return new ValidationResult(false, "State code must contain only letters");
        }
        
        // Validate ZIP code format (5 or 9 digits, with optional dash)
        if (!normalizedZip.matches("\\d{5}(-\\d{4})?")) {
            return new ValidationResult(false, "ZIP code must be in format NNNNN or NNNNN-NNNN");
        }
        
        // Extract ZIP prefix (first 2 digits)
        String zipPrefix = normalizedZip.substring(0, 2);
        
        // Validate the state-ZIP combination
        if (!isValid(normalizedState, zipPrefix)) {
            return new ValidationResult(false, 
                String.format("Invalid state-ZIP combination: %s-%s. State '%s' does not use ZIP codes starting with '%s'",
                    normalizedState, zipPrefix, normalizedState, zipPrefix));
        }
        
        return new ValidationResult(true, "Valid state-ZIP combination");
    }
    
    /**
     * Gets all valid ZIP prefixes for a given state.
     * 
     * <p>This method supports address validation scenarios where the user interface
     * needs to display valid ZIP code ranges for a selected state. It enables
     * dynamic validation and user assistance in address entry forms.</p>
     * 
     * @param stateCode The 2-letter state code
     * @return Array of valid ZIP prefixes for the state, or empty array if state not found
     */
    public static String[] getValidZipPrefixesForState(String stateCode) {
        if (stateCode == null) {
            return new String[0];
        }
        
        String normalizedState = stateCode.trim().toUpperCase();
        
        return java.util.Arrays.stream(values())
            .filter(combo -> combo.stateCode.equals(normalizedState))
            .map(combo -> combo.zipPrefix)
            .distinct()
            .sorted()
            .toArray(String[]::new);
    }
    
    /**
     * Gets all valid states for a given ZIP prefix.
     * 
     * <p>This method supports reverse lookup scenarios where the user interface
     * needs to determine which states use a particular ZIP code prefix. This is
     * useful for address validation and data cleansing operations.</p>
     * 
     * @param zipPrefix The first 2 digits of the ZIP code
     * @return Array of valid state codes for the ZIP prefix, or empty array if not found
     */
    public static String[] getValidStatesForZipPrefix(String zipPrefix) {
        if (zipPrefix == null) {
            return new String[0];
        }
        
        String normalizedZip = zipPrefix.trim();
        
        // Validate ZIP prefix format
        if (normalizedZip.length() != 2 || !normalizedZip.matches("\\d{2}")) {
            return new String[0];
        }
        
        return java.util.Arrays.stream(values())
            .filter(combo -> combo.zipPrefix.equals(normalizedZip))
            .map(combo -> combo.stateCode)
            .distinct()
            .sorted()
            .toArray(String[]::new);
    }
    
    /**
     * Validation result class for comprehensive address validation feedback.
     * 
     * <p>This class provides detailed validation results with status and error
     * messages for user interface feedback. It supports both React form validation
     * and Spring Boot server-side validation scenarios.</p>
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        /**
         * Creates a validation result with status and message.
         * 
         * @param valid Whether the validation passed
         * @param message The validation message (error or success)
         */
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        /**
         * Gets the validation status.
         * 
         * @return true if validation passed, false otherwise
         */
        public boolean isValid() {
            return valid;
        }
        
        /**
         * Gets the validation message.
         * 
         * @return The validation message
         */
        public String getMessage() {
            return message;
        }
        
        /**
         * Gets the validation message, or returns the default message if validation passed.
         * 
         * @param defaultMessage The default message to return if validation passed
         * @return The error message if validation failed, or the default message if passed
         */
        public String orElse(String defaultMessage) {
            return valid ? defaultMessage : message;
        }
    }
}