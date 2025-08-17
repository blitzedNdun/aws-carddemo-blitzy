package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.carddemo.dto.AddressDto;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Service for standardizing and validating customer addresses using USPS address verification patterns.
 * 
 * This service implements address cleansing, ZIP+4 validation, and standardization routines that 
 * replicate COBOL address editing logic from the mainframe customer data processing system.
 * 
 * Key features:
 * - Address standardization matching COBOL address handling patterns
 * - ZIP+4 validation for enhanced address accuracy
 * - USPS-style address formatting and normalization
 * - Phone number and SSN validation patterns preservation
 * - Mock implementation for address verification API integration
 * 
 * Address validation follows patterns established in CBCUS01C customer data processing,
 * maintaining compatibility with existing customer data validation rules while providing
 * enhanced standardization capabilities for the modernized system.
 */
@Service
public class AddressValidationService {

    // Street type standardization mapping (COBOL-style lookup table)
    private static final Map<String, String> STREET_TYPE_STANDARDIZATION = Map.ofEntries(
        Map.entry("ST", "STREET"),
        Map.entry("STR", "STREET"), 
        Map.entry("AVE", "AVENUE"),
        Map.entry("AV", "AVENUE"),
        Map.entry("BLVD", "BOULEVARD"),
        Map.entry("BLV", "BOULEVARD"),
        Map.entry("RD", "ROAD"),
        Map.entry("DR", "DRIVE"),
        Map.entry("LN", "LANE"),
        Map.entry("CT", "COURT"),
        Map.entry("CIR", "CIRCLE"),
        Map.entry("PL", "PLACE"),
        Map.entry("PKY", "PARKWAY"),
        Map.entry("HWY", "HIGHWAY")
    );

    // Directional abbreviation standardization (preserving COBOL abbreviation patterns)
    private static final Map<String, String> DIRECTIONAL_STANDARDIZATION = Map.of(
        "N", "NORTH",
        "S", "SOUTH", 
        "E", "EAST",
        "W", "WEST",
        "NE", "NORTHEAST",
        "NW", "NORTHWEST",
        "SE", "SOUTHEAST",
        "SW", "SOUTHWEST"
    );

    // State code validation patterns (matching COBOL ADDR-STATE-CD validation)
    private static final Set<String> VALID_STATE_CODES = Set.of(
        "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
        "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
        "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
        "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
        "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
        "DC", "PR", "VI", "GU", "AS"
    );

    // ZIP code validation patterns
    private static final Pattern ZIP_5_PATTERN = Pattern.compile("^\\d{5}$");
    private static final Pattern ZIP_9_PATTERN = Pattern.compile("^\\d{5}-\\d{4}$");
    private static final Pattern ZIP_PLUS4_PATTERN = Pattern.compile("^\\d{9}$");
    
    // Phone number validation pattern (preserving COBOL phone validation)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$|^\\d{3}-\\d{3}-\\d{4}$|^\\(\\d{3}\\)\\s?\\d{3}-\\d{4}$");
    
    // SSN validation pattern (maintaining COBOL SSN validation rules)
    private static final Pattern SSN_PATTERN = Pattern.compile("^\\d{3}-\\d{2}-\\d{4}$|^\\d{9}$");

    /**
     * Standardizes an address using USPS address verification patterns.
     * 
     * This method implements comprehensive address cleansing that replicates
     * COBOL address editing logic, including:
     * - Street type standardization
     * - Directional abbreviation expansion
     * - Case normalization
     * - Punctuation standardization
     * - Address component validation
     * 
     * @param address The address to standardize
     * @return Standardized address with USPS-compliant formatting
     * @throws IllegalArgumentException if address is null or invalid
     */
    public AddressDto standardizeAddress(AddressDto address) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }

        AddressDto standardized = new AddressDto();
        
        // Standardize address line 1 (primary address)
        standardized.setAddressLine1(standardizeAddressLine(address.getAddressLine1()));
        
        // Standardize address line 2 (secondary address - unit/suite/apt)
        standardized.setAddressLine2(standardizeSecondaryAddress(address.getAddressLine2()));
        
        // Standardize address line 3 (additional address information)
        standardized.setAddressLine3(cleanAndTrim(address.getAddressLine3()));
        
        // Standardize state code (ensure uppercase and valid)
        standardized.setStateCode(standardizeStateCode(address.getStateCode()));
        
        // Standardize country code (ensure uppercase)
        standardized.setCountryCode(standardizeCountryCode(address.getCountryCode()));
        
        // Standardize ZIP code
        standardized.setZipCode(standardizeZipCode(address.getZipCode()));

        return standardized;
    }

    /**
     * Validates and standardizes a ZIP+4 code for enhanced address accuracy.
     * 
     * Implements ZIP+4 validation patterns that support:
     * - Standard 5-digit ZIP codes
     * - ZIP+4 format validation (XXXXX-XXXX)
     * - 9-digit ZIP code without hyphen
     * - Format standardization to XXXXX-XXXX pattern
     * 
     * @param zipCode The ZIP code to validate and standardize
     * @return Standardized ZIP+4 code or original 5-digit ZIP if +4 not available
     * @throws IllegalArgumentException if ZIP code format is invalid
     */
    public String validateZipCode4(String zipCode) {
        if (!StringUtils.hasText(zipCode)) {
            throw new IllegalArgumentException("ZIP code cannot be null or empty");
        }

        String cleanZip = zipCode.trim().toUpperCase();
        
        // Validate 5-digit ZIP code
        if (ZIP_5_PATTERN.matcher(cleanZip).matches()) {
            return cleanZip;
        }
        
        // Validate ZIP+4 format (XXXXX-XXXX)
        if (ZIP_9_PATTERN.matcher(cleanZip).matches()) {
            return cleanZip;
        }
        
        // Validate 9-digit ZIP code without hyphen and format it
        if (ZIP_PLUS4_PATTERN.matcher(cleanZip).matches()) {
            return cleanZip.substring(0, 5) + "-" + cleanZip.substring(5);
        }
        
        throw new IllegalArgumentException("Invalid ZIP code format: " + zipCode);
    }

    /**
     * Formats an address for display and standardized output.
     * 
     * Provides formatted address output suitable for:
     * - Customer statements and correspondence
     * - Address labels and shipping
     * - Database storage consistency
     * - COBOL fixed-field compatibility
     * 
     * @param address The address to format
     * @return Formatted address as a multi-line string
     */
    public String formatAddress(AddressDto address) {
        if (address == null) {
            return "";
        }

        List<String> addressLines = new ArrayList<>();
        
        // Add address line 1 if present
        if (StringUtils.hasText(address.getAddressLine1())) {
            addressLines.add(address.getAddressLine1().trim());
        }
        
        // Add address line 2 if present  
        if (StringUtils.hasText(address.getAddressLine2())) {
            addressLines.add(address.getAddressLine2().trim());
        }
        
        // Add address line 3 if present
        if (StringUtils.hasText(address.getAddressLine3())) {
            addressLines.add(address.getAddressLine3().trim());
        }
        
        // Add city, state, ZIP line
        StringBuilder cityStateZip = new StringBuilder();
        if (StringUtils.hasText(address.getStateCode())) {
            cityStateZip.append(address.getStateCode());
        }
        if (StringUtils.hasText(address.getZipCode())) {
            if (cityStateZip.length() > 0) {
                cityStateZip.append(" ");
            }
            cityStateZip.append(address.getZipCode());
        }
        if (cityStateZip.length() > 0) {
            addressLines.add(cityStateZip.toString());
        }
        
        // Add country if present and not USA
        if (StringUtils.hasText(address.getCountryCode()) && 
            !"USA".equals(address.getCountryCode()) && 
            !"US".equals(address.getCountryCode())) {
            addressLines.add(address.getCountryCode());
        }

        return String.join("\n", addressLines);
    }

    /**
     * Validates an address against USPS standards and business rules.
     * 
     * Performs comprehensive address validation including:
     * - Required field validation
     * - State code validation
     * - ZIP code format validation
     * - Address component length validation
     * - COBOL field constraint compliance
     * 
     * @param address The address to validate
     * @return true if address passes all validation rules, false otherwise
     */
    public boolean validateAddress(AddressDto address) {
        if (address == null) {
            return false;
        }

        try {
            // Validate required address line 1
            if (!StringUtils.hasText(address.getAddressLine1())) {
                return false;
            }
            
            // Validate address line length constraints (COBOL PIC X(50) compatibility)
            if (address.getAddressLine1().length() > 50 ||
                (address.getAddressLine2() != null && address.getAddressLine2().length() > 50) ||
                (address.getAddressLine3() != null && address.getAddressLine3().length() > 50)) {
                return false;
            }
            
            // Validate state code if present
            if (StringUtils.hasText(address.getStateCode())) {
                if (!isValidStateCode(address.getStateCode())) {
                    return false;
                }
            }
            
            // Validate ZIP code if present
            if (StringUtils.hasText(address.getZipCode())) {
                try {
                    validateZipCode4(address.getZipCode());
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            
            // Validate country code length (COBOL PIC X(03) compatibility)
            if (address.getCountryCode() != null && address.getCountryCode().length() > 3) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Normalizes an address to ensure consistent data formatting.
     * 
     * Implements address normalization that preserves COBOL data handling patterns:
     * - Consistent case formatting
     * - Whitespace normalization
     * - Special character handling
     * - Field padding and truncation rules
     * - NULL and empty value handling
     * 
     * @param address The address to normalize
     * @return Normalized address with consistent formatting
     */
    public AddressDto normalizeAddress(AddressDto address) {
        if (address == null) {
            return new AddressDto();
        }

        AddressDto normalized = new AddressDto();
        
        // Normalize address lines with consistent formatting
        normalized.setAddressLine1(normalizeAddressLine(address.getAddressLine1()));
        normalized.setAddressLine2(normalizeAddressLine(address.getAddressLine2()));
        normalized.setAddressLine3(normalizeAddressLine(address.getAddressLine3()));
        
        // Normalize state code to uppercase
        normalized.setStateCode(normalizeCode(address.getStateCode(), 2));
        
        // Normalize country code to uppercase
        normalized.setCountryCode(normalizeCode(address.getCountryCode(), 3));
        
        // Normalize ZIP code
        normalized.setZipCode(normalizeZipCode(address.getZipCode()));

        return normalized;
    }

    // Private helper methods for address processing

    private String standardizeAddressLine(String addressLine) {
        if (!StringUtils.hasText(addressLine)) {
            return null;
        }

        String standardized = cleanAndTrim(addressLine).toUpperCase();
        
        // Standardize street types
        for (Map.Entry<String, String> entry : STREET_TYPE_STANDARDIZATION.entrySet()) {
            String pattern = "\\b" + entry.getKey() + "\\b";
            standardized = standardized.replaceAll(pattern, entry.getValue());
        }
        
        // Standardize directional abbreviations
        for (Map.Entry<String, String> entry : DIRECTIONAL_STANDARDIZATION.entrySet()) {
            String pattern = "\\b" + entry.getKey() + "\\b";
            standardized = standardized.replaceAll(pattern, entry.getValue());
        }
        
        return standardized;
    }

    private String standardizeSecondaryAddress(String secondaryAddress) {
        if (!StringUtils.hasText(secondaryAddress)) {
            return null;
        }

        String standardized = cleanAndTrim(secondaryAddress).toUpperCase();
        
        // Standardize unit/suite/apartment abbreviations
        standardized = standardized.replaceAll("\\bAPT\\b", "APARTMENT");
        standardized = standardized.replaceAll("\\bSTE\\b", "SUITE");
        standardized = standardized.replaceAll("\\bUNIT\\b", "UNIT");
        standardized = standardized.replaceAll("\\b#\\b", "NUMBER");
        
        return standardized;
    }

    private String standardizeStateCode(String stateCode) {
        if (!StringUtils.hasText(stateCode)) {
            return null;
        }
        
        String standardized = cleanAndTrim(stateCode).toUpperCase();
        
        if (!isValidStateCode(standardized)) {
            throw new IllegalArgumentException("Invalid state code: " + stateCode);
        }
        
        return standardized;
    }

    private String standardizeCountryCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return "USA"; // Default to USA for domestic addresses
        }
        
        String standardized = cleanAndTrim(countryCode).toUpperCase();
        
        // Standardize common country code variations
        if ("US".equals(standardized)) {
            return "USA";
        }
        
        return standardized;
    }

    private String standardizeZipCode(String zipCode) {
        if (!StringUtils.hasText(zipCode)) {
            return null;
        }
        
        try {
            return validateZipCode4(zipCode);
        } catch (IllegalArgumentException e) {
            return cleanAndTrim(zipCode); // Return as-is if validation fails
        }
    }

    private String normalizeAddressLine(String addressLine) {
        if (!StringUtils.hasText(addressLine)) {
            return null;
        }
        
        return cleanAndTrim(addressLine);
    }

    private String normalizeCode(String code, int maxLength) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        
        String normalized = cleanAndTrim(code).toUpperCase();
        
        if (normalized.length() > maxLength) {
            normalized = normalized.substring(0, maxLength);
        }
        
        return normalized;
    }

    private String normalizeZipCode(String zipCode) {
        if (!StringUtils.hasText(zipCode)) {
            return null;
        }
        
        String normalized = zipCode.trim();
        
        // Remove any non-digit characters except hyphen
        normalized = normalized.replaceAll("[^\\d-]", "");
        
        return normalized;
    }

    private String cleanAndTrim(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove extra whitespace and normalize
        String cleaned = input.trim().replaceAll("\\s+", " ");
        
        return cleaned.isEmpty() ? null : cleaned;
    }

    private boolean isValidStateCode(String stateCode) {
        return StringUtils.hasText(stateCode) && 
               VALID_STATE_CODES.contains(stateCode.toUpperCase().trim());
    }

    // Utility methods for customer data validation (preserving COBOL validation patterns)

    /**
     * Validates phone number format preserving COBOL phone validation patterns.
     * 
     * @param phoneNumber The phone number to validate
     * @return true if phone number is valid, false otherwise
     */
    public boolean validatePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return false;
        }
        
        String cleanPhone = phoneNumber.replaceAll("[^\\d()\\-\\s]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * Validates SSN format maintaining COBOL SSN validation rules.
     * 
     * @param ssn The SSN to validate
     * @return true if SSN is valid, false otherwise
     */
    public boolean validateSSN(String ssn) {
        if (!StringUtils.hasText(ssn)) {
            return false;
        }
        
        String cleanSSN = ssn.replaceAll("[^\\d\\-]", "");
        return SSN_PATTERN.matcher(cleanSSN).matches();
    }
}