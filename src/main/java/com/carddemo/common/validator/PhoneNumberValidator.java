/*
 * PhoneNumberValidator.java
 * 
 * Jakarta Bean Validation constraint validator for North American phone numbers
 * with area code verification using NANP (North American Numbering Plan) area codes.
 * 
 * This validator implements the validation logic for the @ValidPhoneNumber annotation,
 * using the area code lookup table from COBOL copybook CSLKPCDY.cpy to ensure
 * phone numbers have valid area codes according to the North American Numbering Plan.
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
package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Jakarta Bean Validation constraint validator for North American phone numbers
 * with area code verification using the NANP (North American Numbering Plan) area codes.
 * 
 * This validator implements the validation logic for the @ValidPhoneNumber annotation,
 * using the area code lookup table from COBOL copybook CSLKPCDY.cpy (VALID-PHONE-AREA-CODE condition)
 * to ensure phone numbers have valid area codes according to the North American Numbering Plan.
 * 
 * <p>Validation Process:</p>
 * <ol>
 *   <li>Parse phone number from various formats to extract digits</li>
 *   <li>Validate phone number length (10 digits or 11 digits with country code 1)</li>
 *   <li>Extract area code (first 3 digits) and exchange code (next 3 digits)</li>
 *   <li>Validate area code against CSLKPCDY.cpy lookup table</li>
 *   <li>Validate exchange code according to NANP rules</li>
 *   <li>Generate appropriate error message for validation failures</li>
 * </ol>
 * 
 * <p>NANP Validation Rules:</p>
 * <ul>
 *   <li>Phone numbers must be 10 digits (area code + exchange + number)</li>
 *   <li>Country code 1 is optional for international format</li>
 *   <li>Area code must be from valid area code list (CSLKPCDY.cpy)</li>
 *   <li>Area code cannot start with 0 or 1</li>
 *   <li>Exchange code cannot start with 0 or 1</li>
 * </ul>
 * 
 * @see ValidPhoneNumber
 * @see ConstraintValidator
 * @since CardDemo v1.0
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    /**
     * Set of valid North American area codes from COBOL copybook CSLKPCDY.cpy
     * This corresponds to the VALID-PHONE-AREA-CODE condition in the copybook.
     */
    private static final Set<String> VALID_AREA_CODES = new HashSet<>();
    
    static {
        // Initialize valid area codes from CSLKPCDY.cpy VALID-PHONE-AREA-CODE condition
        // These are the actual area codes from the COBOL copybook
        VALID_AREA_CODES.addAll(Set.of(
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
            "539", "540", "541", "548", "551", "559", "561", "562", "563", "564",
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
        ));
    }
    
    /**
     * Regular expression pattern for matching various phone number formats.
     * Supports formats like:
     * - (123) 456-7890
     * - 123-456-7890
     * - 123.456.7890
     * - 123 456 7890
     * - 1234567890
     * - +1 (123) 456-7890
     * - 1-123-456-7890
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(?:\\+?1[-. ]?)?(?:\\(?([0-9]{3})\\)?[-. ]?)?([0-9]{3})[-. ]?([0-9]{4})$"
    );
    
    /**
     * Pattern to extract only digits from a phone number string.
     */
    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("\\D");
    
    // Configuration fields from the annotation
    private boolean allowNull;
    private boolean allowEmpty;
    private boolean strictAreaCodeValidation;
    private boolean allowInternationalFormat;
    
    /**
     * Initialize the validator with configuration from the annotation.
     * 
     * @param annotation the ValidPhoneNumber annotation instance
     */
    @Override
    public void initialize(ValidPhoneNumber annotation) {
        this.allowNull = annotation.allowNull();
        this.allowEmpty = annotation.allowEmpty();
        this.strictAreaCodeValidation = annotation.strictAreaCodeValidation();
        this.allowInternationalFormat = annotation.allowInternationalFormat();
    }
    
    /**
     * Validate the phone number value.
     * 
     * @param value the phone number value to validate
     * @param context the validation context
     * @return true if the phone number is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values
        if (value == null) {
            return allowNull;
        }
        
        // Handle empty strings
        if (value.trim().isEmpty()) {
            return allowEmpty;
        }
        
        // Extract digits from the phone number
        String digitsOnly = DIGITS_ONLY_PATTERN.matcher(value.trim()).replaceAll("");
        
        // Validate phone number length
        if (digitsOnly.length() < 10 || digitsOnly.length() > 11) {
            buildConstraintViolation(context, "Phone number must be 10 digits or 11 digits with country code");
            return false;
        }
        
        // Handle country code
        String phoneNumber;
        if (digitsOnly.length() == 11) {
            if (!allowInternationalFormat) {
                buildConstraintViolation(context, "International format phone numbers are not allowed");
                return false;
            }
            // Check if it starts with country code 1
            if (!digitsOnly.startsWith("1")) {
                buildConstraintViolation(context, "International phone numbers must start with country code 1");
                return false;
            }
            phoneNumber = digitsOnly.substring(1); // Remove country code
        } else {
            phoneNumber = digitsOnly;
        }
        
        // Extract area code and exchange code
        String areaCode = phoneNumber.substring(0, 3);
        String exchangeCode = phoneNumber.substring(3, 6);
        
        // Validate area code format (cannot start with 0 or 1)
        if (areaCode.startsWith("0") || areaCode.startsWith("1")) {
            buildConstraintViolation(context, "Area code cannot start with 0 or 1");
            return false;
        }
        
        // Validate exchange code format (cannot start with 0 or 1)
        if (exchangeCode.startsWith("0") || exchangeCode.startsWith("1")) {
            buildConstraintViolation(context, "Exchange code cannot start with 0 or 1");
            return false;
        }
        
        // Validate area code against lookup table if strict validation is enabled
        if (strictAreaCodeValidation && !VALID_AREA_CODES.contains(areaCode)) {
            buildConstraintViolation(context, 
                String.format("Area code %s is not a valid North American area code", areaCode));
            return false;
        }
        
        // Additional format validation using regex
        Matcher matcher = PHONE_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            buildConstraintViolation(context, "Phone number format is invalid");
            return false;
        }
        
        return true;
    }
    
    /**
     * Build a constraint violation with a custom message.
     * 
     * @param context the validation context
     * @param message the custom error message
     */
    private void buildConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
    
    /**
     * Check if an area code is valid according to the CSLKPCDY.cpy lookup table.
     * 
     * @param areaCode the 3-digit area code to validate
     * @return true if the area code is valid, false otherwise
     */
    public static boolean isValidAreaCode(String areaCode) {
        return areaCode != null && 
               areaCode.length() == 3 && 
               VALID_AREA_CODES.contains(areaCode);
    }
    
    /**
     * Get the set of all valid area codes from the CSLKPCDY.cpy lookup table.
     * 
     * @return an immutable set of valid area codes
     */
    public static Set<String> getValidAreaCodes() {
        return Set.copyOf(VALID_AREA_CODES);
    }
    
    /**
     * Normalize a phone number to a standard format (removes all non-digit characters).
     * 
     * @param phoneNumber the phone number to normalize
     * @return the normalized phone number with digits only, or null if input is null
     */
    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return DIGITS_ONLY_PATTERN.matcher(phoneNumber.trim()).replaceAll("");
    }
    
    /**
     * Format a phone number in standard display format (123) 456-7890.
     * 
     * @param phoneNumber the phone number to format (digits only)
     * @return the formatted phone number, or the original string if formatting fails
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        String digitsOnly = normalizePhoneNumber(phoneNumber);
        if (digitsOnly == null) {
            return phoneNumber;
        }
        
        // Handle 11-digit number with country code
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
            digitsOnly = digitsOnly.substring(1);
        }
        
        // Format 10-digit number
        if (digitsOnly.length() == 10) {
            return String.format("(%s) %s-%s", 
                digitsOnly.substring(0, 3),
                digitsOnly.substring(3, 6),
                digitsOnly.substring(6));
        }
        
        return phoneNumber; // Return original if cannot format
    }
}