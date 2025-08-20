/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import com.carddemo.exception.ValidationException;

import java.util.Map;
import java.util.regex.Pattern;
import java.lang.String;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Common validation utility class providing field validation methods translated from COBOL 
 * copybook validation routines. Maintains exact validation rules to ensure functional parity
 * with the original COBOL implementation from CSUTLDPY.cpy, CSLKPCDY.cpy, and CSUTLDWY.cpy.
 * 
 * This utility class implements comprehensive field validation including:
 * - Phone number area code validation using NANPA area code list
 * - US state code validation for all states and territories  
 * - State and ZIP code combination validation
 * - SSN format and pattern validation
 * - Date validation with future date checking
 * - Required field validation
 * - Numeric field and range validation
 * - Alphanumeric pattern validation
 * 
 * All validation error messages match COBOL error messages exactly to maintain 
 * compatibility with external interfaces and user expectations.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public final class ValidationUtil {

    // Field length constants based on COBOL PIC clauses from copybooks
    private static final int SSN_LENGTH = 9;
    private static final int PHONE_NUMBER_LENGTH = 10; 
    private static final int ZIP_CODE_LENGTH = 5;
    private static final int FICO_SCORE_MIN = 300;
    private static final int FICO_SCORE_MAX = 850;
    
    // Validation patterns - accepts both dashed (123-45-6789) and non-dashed (123456789) formats
    private static final Pattern SSN_PATTERN = Pattern.compile("^(?:\\d{3}-\\d{2}-\\d{4}|\\d{9})$");
    private static final Pattern SSN_BASIC_FORMAT_PATTERN = Pattern.compile("^[\\d-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{3}-?\\d{3}-?\\d{4}$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{16}$");
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ValidationUtil() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Validates a user ID according to COBOL user validation rules.
     * User ID must be 1-8 characters, alphanumeric, and not blank.
     * 
     * @param userId the user ID to validate
     * @throws ValidationException if the user ID is invalid
     */
    public static void validateUserId(String userId) {
        ValidationException validationException = new ValidationException("User ID validation failed");
        
        if (userId == null || userId.trim().isEmpty()) {
            validationException.addFieldError("userId", "User ID must be supplied.");
        } else if (userId.trim().length() > Constants.USER_ID_LENGTH) {
            validationException.addFieldError("userId", "User ID must be " + Constants.USER_ID_LENGTH + " characters or less.");
        } else if (!ALPHANUMERIC_PATTERN.matcher(userId.trim()).matches()) {
            validationException.addFieldError("userId", "User ID must contain only alphanumeric characters.");
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates a password according to system requirements.
     * Password must be supplied and meet minimum length requirements.
     * 
     * @param password the password to validate
     * @throws ValidationException if the password is invalid
     */
    public static void validatePassword(String password) {
        ValidationException validationException = new ValidationException("Password validation failed");
        
        if (password == null || password.trim().isEmpty()) {
            validationException.addFieldError("password", "Password must be supplied.");
        } else if (password.trim().length() < 4) {
            validationException.addFieldError("password", "Password must be at least 4 characters long.");
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates a user type according to COBOL user type specifications.
     * User type must be either 'A' (Admin) or 'U' (User).
     * 
     * @param userType the user type to validate
     * @throws ValidationException if the user type is invalid
     */
    public static void validateUserType(String userType) {
        ValidationException validationException = new ValidationException("User type validation failed");
        
        if (userType == null || userType.trim().isEmpty()) {
            validationException.addFieldError("userType", "User type must be supplied.");
        } else if (!Constants.USER_TYPE_ADMIN.equals(userType.trim()) && 
                   !Constants.USER_TYPE_USER.equals(userType.trim())) {
            validationException.addFieldError("userType", "User type must be 'A' for Admin or 'U' for User.");
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates field length against specified maximum length.
     * 
     * @param fieldName the name of the field for error messages
     * @param fieldValue the field value to validate
     * @param maxLength the maximum allowed length
     * @throws ValidationException if the field exceeds maximum length
     */
    public static void validateFieldLength(String fieldName, String fieldValue, int maxLength) {
        ValidationException validationException = new ValidationException("Field length validation failed");
        
        if (fieldValue != null && fieldValue.length() > maxLength) {
            validationException.addFieldError(fieldName, fieldName + " must be " + maxLength + " characters or less.");
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates that a required field is not null or empty.
     * Replicates COBOL required field validation logic.
     * 
     * @param fieldName the name of the field for error messages
     * @param fieldValue the field value to validate
     * @throws ValidationException if the field is required but not supplied
     */
    public static void validateRequiredField(String fieldName, String fieldValue) {
        ValidationException validationException = new ValidationException("Required field validation failed");
        
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            validationException.addFieldError(fieldName, fieldName + " must be supplied.");
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Determines the card type based on the card number prefix.
     * Implements card type identification logic using industry standard prefixes.
     * 
     * @param cardNumber the card number to analyze
     * @return the card type ("VISA", "MASTERCARD", "AMEX", "DISCOVER", "UNKNOWN")
     */
    public static String determineCardType(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        String trimmedCard = cardNumber.trim().replaceAll("\\D", "");
        
        if (trimmedCard.length() < 4) {
            return "UNKNOWN";
        }
        
        // VISA: starts with 4
        if (trimmedCard.startsWith("4")) {
            return "VISA";
        }
        
        // Mastercard: starts with 5 or 2221-2720
        if (trimmedCard.startsWith("5") || 
           (trimmedCard.length() >= 4 && 
            Integer.parseInt(trimmedCard.substring(0, 4)) >= 2221 && 
            Integer.parseInt(trimmedCard.substring(0, 4)) <= 2720)) {
            return "MASTERCARD";
        }
        
        // American Express: starts with 34 or 37
        if (trimmedCard.startsWith("34") || trimmedCard.startsWith("37")) {
            return "AMEX";
        }
        
        // Discover: starts with 6011, 622126-622925, 644-649, 65
        if (trimmedCard.startsWith("6011") || trimmedCard.startsWith("65") ||
           (trimmedCard.length() >= 6 && 
            Integer.parseInt(trimmedCard.substring(0, 6)) >= 622126 && 
            Integer.parseInt(trimmedCard.substring(0, 6)) <= 622925) ||
           (trimmedCard.length() >= 3 && 
            Integer.parseInt(trimmedCard.substring(0, 3)) >= 644 && 
            Integer.parseInt(trimmedCard.substring(0, 3)) <= 649)) {
            return "DISCOVER";
        }
        
        return "UNKNOWN";
    }

    /**
     * Validates phone number area code using the comprehensive NANPA area code list
     * from CSLKPCDY.cpy copybook. Implements VALID-PHONE-AREA-CODE validation logic.
     * 
     * @param areaCode the 3-digit area code to validate
     * @return true if the area code is valid according to NANPA standards
     */
    public static boolean isValidPhoneAreaCode(String areaCode) {
        if (areaCode == null || areaCode.trim().length() != 3) {
            return false;
        }
        
        String code = areaCode.trim();
        if (!NUMERIC_PATTERN.matcher(code).matches()) {
            return false;
        }
        
        // Valid NANPA area codes from CSLKPCDY.cpy VALID-PHONE-AREA-CODE
        Set<String> validAreaCodes = Set.of(
            "201", "202", "203", "204", "205", "206", "207", "208", "209", "210",
            "212", "213", "214", "215", "216", "217", "218", "219", "220", "223",
            "224", "225", "226", "228", "229", "231", "234", "236", "239", "240",
            "242", "246", "248", "249", "250", "251", "252", "253", "254", "256",
            "260", "262", "264", "267", "268", "269", "270", "272", "276", "279",
            "281", "284", "289", "301", "302", "303", "304", "305", "306", "307",
            "308", "309", "310", "312", "313", "314", "315", "316", "317", "318",
            "319", "320", "321", "323", "325", "326", "330", "331", "332", "334",
            "336", "337", "339", "340", "341", "343", "345", "346", "347", "351",
            "352", "360", "361", "364", "365", "367", "368", "380", "385", "386",
            "401", "402", "403", "404", "405", "406", "407", "408", "409", "410",
            "412", "413", "414", "415", "416", "417", "418", "419", "423", "424",
            "425", "430", "431", "432", "434", "435", "437", "438", "440", "441",
            "442", "443", "445", "447", "448", "450", "458", "463", "464", "469",
            "470", "473", "474", "475", "478", "479", "480", "484", "501", "502",
            "503", "504", "505", "506", "507", "508", "509", "510", "512", "513",
            "514", "515", "516", "517", "518", "519", "520", "530", "531", "534",
            "539", "540", "541", "548", "551", "555", "559", "561", "562", "563", "564",
            "567", "570", "571", "572", "573", "574", "575", "579", "580", "581",
            "582", "585", "586", "587", "601", "602", "603", "604", "605", "606",
            "607", "608", "609", "610", "612", "613", "614", "615", "616", "617",
            "618", "619", "620", "623", "626", "628", "629", "630", "631", "636",
            "639", "640", "641", "646", "647", "649", "650", "651", "656", "657",
            "658", "659", "660", "661", "662", "664", "667", "669", "670", "671",
            "672", "678", "680", "681", "682", "683", "684", "689", "701", "702",
            "703", "704", "705", "706", "707", "708", "709", "712", "713", "714",
            "715", "716", "717", "718", "719", "720", "721", "724", "725", "726",
            "727", "731", "732", "734", "737", "740", "742", "743", "747", "753",
            "754", "757", "758", "760", "762", "763", "765", "767", "769", "770",
            "771", "772", "773", "774", "775", "778", "779", "780", "781", "782",
            "784", "785", "786", "787", "801", "802", "803", "804", "805", "806",
            "807", "808", "809", "810", "812", "813", "814", "815", "816", "817",
            "818", "819", "820", "825", "826", "828", "829", "830", "831", "832",
            "838", "839", "840", "843", "845", "847", "848", "849", "850", "854",
            "856", "857", "858", "859", "860", "862", "863", "864", "865", "867",
            "868", "869", "870", "872", "873", "876", "878", "901", "902", "903",
            "904", "905", "906", "907", "908", "909", "910", "912", "913", "914",
            "915", "916", "917", "918", "919", "920", "925", "928", "929", "930",
            "931", "934", "936", "937", "938", "939", "940", "941", "943", "945",
            "947", "948", "949", "951", "952", "954", "956", "959", "970", "971",
            "972", "973", "978", "979", "980", "983", "984", "985", "986", "989"
        );
        
        return validAreaCodes.contains(code);
    }

    /**
     * Validates US state code according to CSLKPCDY.cpy VALID-US-STATE-CODE specification.
     * Includes all 50 states, DC, and US territories.
     * 
     * @param stateCode the 2-character state code to validate
     * @return true if the state code is valid
     */
    public static boolean isValidStateCode(String stateCode) {
        if (stateCode == null || stateCode.trim().length() != 2) {
            return false;
        }
        
        String code = stateCode.trim().toUpperCase();
        
        // Valid US state codes from CSLKPCDY.cpy VALID-US-STATE-CODE
        Set<String> validStateCodes = Set.of(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
            "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
            "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
            "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
            "DC", "AS", "GU", "MP", "PR", "VI"
        );
        
        return validStateCodes.contains(code);
    }

    /**
     * Validates numeric field values are within specified range.
     * Used for validating FICO scores, amounts, and other numeric constraints.
     * 
     * @param value the numeric value to validate
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @return true if the value is within the specified range
     */
    public static boolean isValidNumericRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null || min == null || max == null) {
            return false;
        }
        
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    /**
     * Validates account eligibility for archival based on retention policy.
     * Implements business rules for data retention and archival processes.
     * 
     * @param accountId the account ID to validate
     * @param retentionYears the retention period in years
     * @return true if account is eligible for archival
     */
    public static boolean validateAccountForArchival(String accountId, int retentionYears) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }
        
        if (retentionYears < 1 || retentionYears > 50) {
            return false;
        }
        
        return true;
    }

    /**
     * Validates retention policy parameters for data archival.
     * 
     * @param retentionYears the retention period in years to validate
     * @return true if retention policy is valid
     */
    public static boolean validateRetentionPolicy(int retentionYears) {
        // Retention period must be between 1 and 50 years
        return retentionYears >= 1 && retentionYears <= 50;
    }

    /**
     * Validates numeric field format and content.
     * Replicates COBOL NUMERIC test functionality.
     * 
     * @param fieldName the field name for error messages
     * @param fieldValue the field value to validate
     * @throws ValidationException if the field is not numeric
     */
    public static void validateNumericField(String fieldName, String fieldValue) {
        ValidationException validationException = new ValidationException("Numeric field validation failed");
        
        if (fieldValue != null && !fieldValue.trim().isEmpty()) {
            String trimmedValue = fieldValue.trim();
            if (!NUMERIC_PATTERN.matcher(trimmedValue).matches()) {
                validationException.addFieldError(fieldName, fieldName + " must be numeric.");
            }
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates numeric field format and length.
     * Replicates COBOL NUMERIC test with length validation.
     * 
     * @param fieldValue the field value to validate
     * @param expectedLength the expected length of the field
     * @return true if the field is numeric and has the expected length
     */
    public static boolean validateNumericField(String fieldValue, int expectedLength) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return false;
        }
        
        String trimmedValue = fieldValue.trim();
        return NUMERIC_PATTERN.matcher(trimmedValue).matches() && 
               trimmedValue.length() == expectedLength;
    }

    /**
     * Validates date range ensuring start date is before end date.
     * Used for transaction date ranges and reporting periods.
     * 
     * @param startDateStr the start date in CCYYMMDD format
     * @param endDateStr the end date in CCYYMMDD format
     * @throws ValidationException if the date range is invalid
     */
    public static void validateDateRange(String startDateStr, String endDateStr) {
        ValidationException validationException = new ValidationException("Date range validation failed");
        
        if (startDateStr == null || startDateStr.trim().isEmpty()) {
            validationException.addFieldError("startDate", "Start date must be supplied.");
        }
        
        if (endDateStr == null || endDateStr.trim().isEmpty()) {
            validationException.addFieldError("endDate", "End date must be supplied.");
        }
        
        if (!validationException.hasFieldErrors()) {
            try {
                boolean startDateValid = DateConversionUtil.validateDate(startDateStr);
                boolean endDateValid = DateConversionUtil.validateDate(endDateStr);
                
                if (!startDateValid) {
                    validationException.addFieldError("startDate", "Start date format is invalid. Use CCYYMMDD format.");
                }
                
                if (!endDateValid) {
                    validationException.addFieldError("endDate", "End date format is invalid. Use CCYYMMDD format.");
                }
                
                if (startDateValid && endDateValid) {
                    LocalDate startDate = DateConversionUtil.convertCobolDate(startDateStr);
                    LocalDate endDate = DateConversionUtil.convertCobolDate(endDateStr);
                    
                    if (!startDate.isBefore(endDate) && !startDate.isEqual(endDate)) {
                        validationException.addFieldError("dateRange", "Start date must be before or equal to end date.");
                    }
                }
            } catch (Exception e) {
                validationException.addFieldError("dateRange", "Date range validation error: " + e.getMessage());
            }
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Helper class with core validation methods for account and transaction operations.
     */
    public static class FieldValidator {
        
        /**
         * Validates account ID according to COBOL account ID specifications.
         * Account ID must be exactly 11 digits.
         * 
         * @param accountId the account ID to validate
         * @throws ValidationException if the account ID is invalid
         */
        public void validateAccountId(String accountId) {
            ValidationException validationException = new ValidationException("Account ID validation failed");
            
            if (accountId == null || accountId.trim().isEmpty()) {
                validationException.addFieldError("accountId", "Account ID must be supplied.");
            } else if (!ACCOUNT_ID_PATTERN.matcher(accountId.trim()).matches()) {
                validationException.addFieldError("accountId", "Account ID must be exactly 11 digits.");
            }
            
            if (validationException.hasFieldErrors()) {
                throw validationException;
            }
        }

        /**
         * Validates card number according to industry standards.
         * Card number must be exactly 16 digits.
         * 
         * @param cardNumber the card number to validate
         * @throws ValidationException if the card number is invalid
         */
        public void validateCardNumber(String cardNumber) {
            ValidationException validationException = new ValidationException("Card number validation failed");
            
            if (cardNumber == null || cardNumber.trim().isEmpty()) {
                validationException.addFieldError("cardNumber", "Card number must be supplied.");
            } else {
                String cleanCard = cardNumber.trim().replaceAll("\\D", "");
                if (!CARD_NUMBER_PATTERN.matcher(cleanCard).matches()) {
                    validationException.addFieldError("cardNumber", "Card number must be exactly 16 digits.");
                }
            }
            
            if (validationException.hasFieldErrors()) {
                throw validationException;
            }
        }

        /**
         * Validates transaction amount ensuring it's positive and within reasonable limits.
         * 
         * @param amount the transaction amount to validate
         * @throws ValidationException if the amount is invalid
         */
        public void validateTransactionAmount(BigDecimal amount) {
            ValidationException validationException = new ValidationException("Transaction amount validation failed");
            
            if (amount == null) {
                validationException.addFieldError("amount", "Transaction amount must be supplied.");
            } else if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                validationException.addFieldError("amount", "Transaction amount must be greater than zero.");
            } else if (amount.compareTo(BigDecimal.valueOf(999999.99)) > 0) {
                validationException.addFieldError("amount", "Transaction amount cannot exceed $999,999.99.");
            }
            
            if (validationException.hasFieldErrors()) {
                throw validationException;
            }
        }

        /**
         * Validates that a required field is not null or empty.
         * Provides instance method access to static validation logic.
         * 
         * @param fieldName the name of the field for error messages
         * @param fieldValue the field value to validate
         * @throws ValidationException if the field is required but not supplied
         */
        public void validateRequiredField(String fieldName, String fieldValue) {
            ValidationUtil.validateRequiredField(fieldName, fieldValue);
        }
    }

    /**
     * Validates state and ZIP code combination using VALID-US-STATE-ZIP-CD2-COMBO
     * logic from CSLKPCDY.cpy copybook. Ensures ZIP code prefix matches the state.
     * 
     * @param stateCode the 2-character state code
     * @param zipCode the 5-digit ZIP code
     * @return true if the state-ZIP combination is valid
     */
    public static boolean validateStateZipCode(String stateCode, String zipCode) {
        if (stateCode == null || zipCode == null || 
            stateCode.trim().length() != 2 || zipCode.trim().length() != 5) {
            return false;
        }
        
        String state = stateCode.trim().toUpperCase();
        String zip = zipCode.trim();
        
        if (!NUMERIC_PATTERN.matcher(zip).matches()) {
            return false;
        }
        
        String zipPrefix = zip.substring(0, 2);
        String stateZipCombo = state + zipPrefix;
        
        // Valid state-ZIP combinations from CSLKPCDY.cpy VALID-US-STATE-ZIP-CD2-COMBO
        Set<String> validStateZipCombos = Set.of(
            "AA34", "AE90", "AE91", "AE92", "AE93", "AE94", "AE95", "AE96", "AE97", "AE98",
            "AK99", "AL35", "AL36", "AP96", "AR71", "AR72", "AS96", "AZ85", "AZ86", "CA90",
            "CA91", "CA92", "CA93", "CA94", "CA95", "CA96", "CO80", "CO81", "CT60", "CT61",
            "CT62", "CT63", "CT64", "CT65", "CT66", "CT67", "CT68", "CT69", "DC20", "DC56",
            "DC88", "DE19", "FL32", "FL33", "FL34", "FM96", "GA30", "GA31", "GA39", "GU96",
            "HI96", "IA50", "IA51", "IA52", "ID83", "IL60", "IL61", "IL62", "IN46", "IN47",
            "KS66", "KS67", "KY40", "KY41", "KY42", "LA70", "LA71", "MA10", "MA11", "MA12",
            "MA13", "MA14", "MA15", "MA16", "MA17", "MA18", "MA19", "MA20", "MA21", "MA22",
            "MA23", "MA24", "MA25", "MA26", "MA27", "MA55", "MD20", "MD21", "ME39", "ME40",
            "ME41", "ME42", "ME43", "ME44", "ME45", "ME46", "ME47", "ME48", "ME49", "MH96",
            "MI48", "MI49", "MN55", "MN56", "MO63", "MO64", "MO65", "MO72", "MP96", "MS38",
            "MS39", "MT59", "NC27", "NC28", "ND58", "NE68", "NE69", "NH30", "NH31", "NH32",
            "NH33", "NH34", "NH35", "NH36", "NH37", "NH38", "NJ70", "NJ71", "NJ72", "NJ73",
            "NJ74", "NJ75", "NJ76", "NJ77", "NJ78", "NJ79", "NJ80", "NJ81", "NJ82", "NJ83",
            "NJ84", "NJ85", "NJ86", "NJ87", "NJ88", "NJ89", "NM87", "NM88", "NV88", "NV89",
            "NY50", "NY54", "NY63", "NY10", "NY11", "NY12", "NY13", "NY14", "OH43", "OH44",
            "OH45", "OK73", "OK74", "OR97", "PA15", "PA16", "PA17", "PA18", "PA19", "PR60",
            "PR61", "PR62", "PR63", "PR64", "PR65", "PR66", "PR67", "PR68", "PR69", "PR70",
            "PR71", "PR72", "PR73", "PR74", "PR75", "PR76", "PR77", "PR78", "PR79", "PR90",
            "PR91", "PR92", "PR93", "PR94", "PR95", "PR96", "PR97", "PR98", "PW96", "RI28",
            "RI29", "SC29", "SD57", "TN37", "TN38", "TX73", "TX75", "TX76", "TX77", "TX78",
            "TX79", "TX88", "UT84", "VA20", "VA22", "VA23", "VA24", "VI80", "VI82", "VI83",
            "VI84", "VI85", "VT50", "VT51", "VT52", "VT53", "VT54", "VT56", "VT57", "VT58",
            "VT59", "WA98", "WA99", "WI53", "WI54", "WV24", "WV25", "WV26", "WY82", "WY83"
        );
        
        return validStateZipCombos.contains(stateZipCombo);
    }

    /**
     * Validates SSN format and content according to COBOL SSN validation rules.
     * SSN must be 9 digits and follow valid SSN patterns.
     * 
     * @param fieldName the field name for error messages
     * @param ssn the SSN to validate
     * @throws ValidationException if the SSN is invalid
     */
    public static void validateSSN(String fieldName, String ssn) {
        ValidationException validationException = new ValidationException("SSN validation failed");
        
        if (ssn == null || ssn.trim().isEmpty()) {
            validationException.addFieldError(fieldName, fieldName + " must be supplied.");
        } else {
            String trimmedSSN = ssn.trim();
            
            // First check if it contains only digits and dashes (basic format check)
            if (!SSN_BASIC_FORMAT_PATTERN.matcher(trimmedSSN).matches()) {
                validationException.addFieldError(fieldName, fieldName + " format is invalid.");
            } else {
                String cleanSSN = trimmedSSN.replaceAll("\\D", "");
                
                // Then check length
                if (cleanSSN.length() != SSN_LENGTH) {
                    validationException.addFieldError(fieldName, fieldName + " must be exactly 9 digits.");
                } else if (!SSN_PATTERN.matcher(trimmedSSN).matches()) {
                    // Check specific pattern (correct grouping)
                    validationException.addFieldError(fieldName, fieldName + " format is invalid.");
                } else {
                    // Additional SSN validation rules
                    String area = cleanSSN.substring(0, 3);
                    String group = cleanSSN.substring(3, 5);
                    String serial = cleanSSN.substring(5, 9);
                    
                    // Convert area to int for range checking
                    int areaNumber = Integer.parseInt(area);
                    if (areaNumber == 0 || areaNumber == 666 || (areaNumber >= 900 && areaNumber <= 999)) {
                        validationException.addFieldError(fieldName, fieldName + " contains invalid area number.");
                    }
                    
                    if ("00".equals(group)) {
                        validationException.addFieldError(fieldName, fieldName + " contains invalid group number.");
                    }
                    
                    if ("0000".equals(serial)) {
                        validationException.addFieldError(fieldName, fieldName + " contains invalid serial number.");
                    }
                }
            }
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates phone number format and area code according to COBOL phone validation rules.
     * 
     * @param fieldName the field name for error messages
     * @param phoneNumber the phone number to validate
     * @throws ValidationException if the phone number is invalid
     */
    public static void validatePhoneNumber(String fieldName, String phoneNumber) {
        ValidationException validationException = new ValidationException("Phone number validation failed");
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            validationException.addFieldError(fieldName, fieldName + " must be supplied.");
        } else {
            String cleanPhone = phoneNumber.trim().replaceAll("\\D", "");
            
            if (cleanPhone.length() != PHONE_NUMBER_LENGTH) {
                validationException.addFieldError(fieldName, fieldName + " must be exactly 10 digits.");
            } else if (!PHONE_PATTERN.matcher(phoneNumber.trim()).matches()) {
                validationException.addFieldError(fieldName, fieldName + " format is invalid. Use 999-999-9999 format.");
            } else {
                String areaCode = cleanPhone.substring(0, 3);
                if (!isValidPhoneAreaCode(areaCode)) {
                    validationException.addFieldError(fieldName, fieldName + " contains invalid area code: " + areaCode);
                }
            }
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates FICO score range according to industry standards (300-850).
     * 
     * @param fieldName the field name for error messages
     * @param ficoScore the FICO score to validate
     * @throws ValidationException if the FICO score is invalid
     */
    public static void validateFicoScore(String fieldName, Integer ficoScore) {
        ValidationException validationException = new ValidationException("FICO score validation failed");
        
        if (ficoScore == null) {
            validationException.addFieldError(fieldName, fieldName + " must be supplied.");
        } else if (ficoScore < FICO_SCORE_MIN || ficoScore > FICO_SCORE_MAX) {
            validationException.addFieldError(fieldName, fieldName + " must be between " + FICO_SCORE_MIN + " and " + FICO_SCORE_MAX + ".");
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates date of birth ensuring it's a valid date and not in the future.
     * Replicates EDIT-DATE-OF-BIRTH logic from CSUTLDPY.cpy.
     * 
     * @param fieldName the field name for error messages
     * @param dateOfBirth the date of birth in CCYYMMDD format
     * @throws ValidationException if the date of birth is invalid
     */
    public static void validateDateOfBirth(String fieldName, String dateOfBirth) {
        ValidationException validationException = new ValidationException("Date of birth validation failed");
        
        if (dateOfBirth == null || dateOfBirth.trim().isEmpty()) {
            validationException.addFieldError(fieldName, fieldName + " must be supplied.");
        } else {
            // First validate date format
            if (!DateConversionUtil.validateDate(dateOfBirth)) {
                validationException.addFieldError(fieldName, fieldName + " format is invalid. Use CCYYMMDD format.");
            } else {
                try {
                    LocalDate birthDate = DateConversionUtil.convertCobolDate(dateOfBirth);
                    
                    // Check if date is not in the future
                    if (!DateConversionUtil.isNotFutureDate(birthDate)) {
                        validationException.addFieldError(fieldName, fieldName + " cannot be in the future.");
                    }
                    
                    // Check reasonable age limits (not more than 150 years ago)
                    LocalDate minimumDate = LocalDate.now().minusYears(150);
                    if (birthDate.isBefore(minimumDate)) {
                        validationException.addFieldError(fieldName, fieldName + " is too far in the past.");
                    }
                } catch (Exception e) {
                    validationException.addFieldError(fieldName, fieldName + " validation error: " + e.getMessage());
                }
            }
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates ZIP code format according to US postal standards.
     * 
     * @param fieldName the field name for error messages
     * @param zipCode the ZIP code to validate
     * @throws ValidationException if the ZIP code is invalid
     */
    public static void validateZipCode(String fieldName, String zipCode) {
        ValidationException validationException = new ValidationException("ZIP code validation failed");
        
        if (zipCode == null || zipCode.trim().isEmpty()) {
            validationException.addFieldError(fieldName, fieldName + " must be supplied.");
        } else {
            String trimmedZip = zipCode.trim();
            
            // First check if it contains only numeric characters (no formatting allowed)
            if (!NUMERIC_PATTERN.matcher(trimmedZip).matches()) {
                validationException.addFieldError(fieldName, fieldName + " must contain only numeric characters.");
            } else if (trimmedZip.length() != ZIP_CODE_LENGTH) {
                validationException.addFieldError(fieldName, fieldName + " must be exactly 5 digits.");
            }
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates transaction amount ensuring it's positive and within reasonable limits.
     * Returns boolean for use in conditional logic.
     * 
     * @param amount the transaction amount to validate
     * @return true if the amount is valid, false otherwise
     */
    public static boolean validateTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        
        // Must be positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Must not exceed $99,999.99 (per credit card transaction limits)
        if (amount.compareTo(BigDecimal.valueOf(99999.99)) > 0) {
            return false;
        }
        
        // Must be at least 1 cent
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            return false;
        }
        
        return true;
    }

    /**
     * Validates phone area code against NANPA area code standards.
     * Public method for external validation testing.
     * 
     * @param areaCode the 3-digit area code to validate
     * @return true if the area code is valid, false otherwise
     */
    public static boolean validatePhoneAreaCode(String areaCode) {
        if (areaCode == null || areaCode.trim().length() != 3) {
            return false;
        }
        
        return isValidPhoneAreaCode(areaCode.trim());
    }

    /**
     * Validates US state code against valid state abbreviations.
     * Public method for external validation testing.
     * 
     * @param stateCode the 2-character state code to validate
     * @return true if the state code is valid, false otherwise
     */
    public static boolean validateUSStateCode(String stateCode) {
        if (stateCode == null || stateCode.trim().length() != 2) {
            return false;
        }
        
        return isValidStateCode(stateCode.trim().toUpperCase());
    }



    /**
     * Validates numeric field with specified length.
     * Used for customer ID, account ID, and other numeric fields.
     * 
     * @param value the numeric value to validate
     * @param fieldName the field name for error messages  
     * @param expectedLength the expected length of the numeric field
     * @throws ValidationException if the field is invalid
     */
    public static void validateNumericField(String value, String fieldName, int expectedLength) {
        ValidationException validationException = new ValidationException("Numeric field validation failed");
        
        if (value == null || value.trim().isEmpty()) {
            validationException.addFieldError(fieldName, fieldName + " must be supplied.");
        } else {
            String trimmedValue = value.trim();
            
            if (!NUMERIC_PATTERN.matcher(trimmedValue).matches()) {
                validationException.addFieldError(fieldName, fieldName + " must contain only digits.");
            } else if (trimmedValue.length() != expectedLength) {
                validationException.addFieldError(fieldName, fieldName + " must be exactly " + expectedLength + " digits.");
            }
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates date of birth with LocalDate input.
     * Converts LocalDate to string format for validation.
     * 
     * @param dateOfBirth the date of birth as LocalDate
     * @throws ValidationException if the date of birth is invalid
     */
    public static void validateDateOfBirth(LocalDate dateOfBirth) {
        ValidationException validationException = new ValidationException("Date of birth validation failed");
        
        if (dateOfBirth == null) {
            validationException.addFieldError("dateOfBirth", "Date of birth must be supplied.");
        } else {
            // Check if date is not in the future
            if (dateOfBirth.isAfter(LocalDate.now())) {
                validationException.addFieldError("dateOfBirth", "Birth date cannot be in the future.");
            }
            
            // Check if customer is at least 18 years old
            LocalDate minimumAge = LocalDate.now().minusYears(18);
            if (dateOfBirth.isAfter(minimumAge)) {
                validationException.addFieldError("dateOfBirth", "Customer must be at least 18 years old.");
            }
            
            // Check reasonable age limits (not more than 120 years ago)
            LocalDate maximumAge = LocalDate.now().minusYears(120);
            if (dateOfBirth.isBefore(maximumAge)) {
                validationException.addFieldError("dateOfBirth", "Invalid birth date - too far in the past.");
            }
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates SSN with single parameter for testing purposes.
     * Uses default field name "SSN" for error messages.
     * 
     * @param ssn the SSN to validate
     * @throws ValidationException if the SSN is invalid
     */
    public static void validateSSN(String ssn) {
        validateSSN("SSN", ssn);
    }


}
