package com.carddemo.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * External service client for address validation and standardization using third-party address validation APIs.
 * Provides standardized address formatting, ZIP+4 code validation, state abbreviation validation,
 * and address deliverability verification to ensure customer address data quality and postal compliance.
 * 
 * This service replaces COBOL address validation routines while maintaining identical data quality standards
 * and validation rules as required by the mainframe migration specifications.
 */
@Service
public class AddressValidationService {

    private static final Logger logger = LoggerFactory.getLogger(AddressValidationService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${address.validation.api.url:https://api.address-validator.com}")
    private String addressValidationApiUrl;

    @Value("${address.validation.api.key}")
    private String apiKey;

    @Value("${address.validation.timeout:5000}")
    private int timeoutMs;

    @Value("${address.validation.enabled:true}")
    private boolean validationEnabled;

    // US state codes mapping for validation
    private static final Set<String> VALID_US_STATES = new HashSet<>();
    static {
        VALID_US_STATES.add("AL"); VALID_US_STATES.add("AK"); VALID_US_STATES.add("AZ"); VALID_US_STATES.add("AR");
        VALID_US_STATES.add("CA"); VALID_US_STATES.add("CO"); VALID_US_STATES.add("CT"); VALID_US_STATES.add("DE");
        VALID_US_STATES.add("FL"); VALID_US_STATES.add("GA"); VALID_US_STATES.add("HI"); VALID_US_STATES.add("ID");
        VALID_US_STATES.add("IL"); VALID_US_STATES.add("IN"); VALID_US_STATES.add("IA"); VALID_US_STATES.add("KS");
        VALID_US_STATES.add("KY"); VALID_US_STATES.add("LA"); VALID_US_STATES.add("ME"); VALID_US_STATES.add("MD");
        VALID_US_STATES.add("MA"); VALID_US_STATES.add("MI"); VALID_US_STATES.add("MN"); VALID_US_STATES.add("MS");
        VALID_US_STATES.add("MO"); VALID_US_STATES.add("MT"); VALID_US_STATES.add("NE"); VALID_US_STATES.add("NV");
        VALID_US_STATES.add("NH"); VALID_US_STATES.add("NJ"); VALID_US_STATES.add("NM"); VALID_US_STATES.add("NY");
        VALID_US_STATES.add("NC"); VALID_US_STATES.add("ND"); VALID_US_STATES.add("OH"); VALID_US_STATES.add("OK");
        VALID_US_STATES.add("OR"); VALID_US_STATES.add("PA"); VALID_US_STATES.add("RI"); VALID_US_STATES.add("SC");
        VALID_US_STATES.add("SD"); VALID_US_STATES.add("TN"); VALID_US_STATES.add("TX"); VALID_US_STATES.add("UT");
        VALID_US_STATES.add("VT"); VALID_US_STATES.add("VA"); VALID_US_STATES.add("WA"); VALID_US_STATES.add("WV");
        VALID_US_STATES.add("WI"); VALID_US_STATES.add("WY"); VALID_US_STATES.add("DC"); VALID_US_STATES.add("PR");
        VALID_US_STATES.add("VI"); VALID_US_STATES.add("GU"); VALID_US_STATES.add("AS"); VALID_US_STATES.add("MP");
    }

    // Country codes mapping for validation (ISO 3166-1 alpha-3)
    private static final Set<String> VALID_COUNTRY_CODES = new HashSet<>();
    static {
        VALID_COUNTRY_CODES.add("USA"); VALID_COUNTRY_CODES.add("CAN"); VALID_COUNTRY_CODES.add("MEX");
        VALID_COUNTRY_CODES.add("GBR"); VALID_COUNTRY_CODES.add("FRA"); VALID_COUNTRY_CODES.add("DEU");
        VALID_COUNTRY_CODES.add("ITA"); VALID_COUNTRY_CODES.add("ESP"); VALID_COUNTRY_CODES.add("JPN");
        VALID_COUNTRY_CODES.add("AUS"); VALID_COUNTRY_CODES.add("BRA"); VALID_COUNTRY_CODES.add("CHN");
        VALID_COUNTRY_CODES.add("IND"); VALID_COUNTRY_CODES.add("RUS"); VALID_COUNTRY_CODES.add("ZAF");
    }

    // ZIP code patterns for different countries
    private static final Pattern US_ZIP_PATTERN = Pattern.compile("^\\d{5}(-\\d{4})?$");
    private static final Pattern CA_POSTAL_PATTERN = Pattern.compile("^[A-Z]\\d[A-Z]\\s?\\d[A-Z]\\d$");
    private static final Pattern UK_POSTAL_PATTERN = Pattern.compile("^[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2}$");

    /**
     * Comprehensive address validation including street address formatting, city name standardization,
     * state abbreviation validation, ZIP code format validation and enhancement to ZIP+4,
     * address deliverability verification through external APIs, latitude/longitude geocoding
     * for location validation, and address type classification (residential/commercial).
     *
     * @param addressLine1 Primary street address line (max 50 chars as per COBOL copybook)
     * @param addressLine2 Secondary address line (max 50 chars)
     * @param addressLine3 Additional address line (max 50 chars)
     * @param city City name for standardization
     * @param state State abbreviation (2 chars as per COBOL copybook)
     * @param zipCode ZIP code for validation and enhancement (max 10 chars)
     * @param countryCode Country code (3 chars as per COBOL copybook)
     * @return AddressValidationResult containing standardized address and validation status
     */
    public AddressValidationResult validateAndStandardizeAddress(
            String addressLine1, String addressLine2, String addressLine3,
            String city, String state, String zipCode, String countryCode) {
        
        logger.info("Starting comprehensive address validation for: {}, {}, {} {}, {}",
                addressLine1, city, state, zipCode, countryCode);

        AddressValidationResult result = new AddressValidationResult();
        result.setInputAddress(new Address(addressLine1, addressLine2, addressLine3, city, state, zipCode, countryCode));
        result.setValidationTimestamp(LocalDateTime.now());

        try {
            // Input validation and sanitization
            if (!StringUtils.hasText(addressLine1) || !StringUtils.hasText(city) || 
                !StringUtils.hasText(state) || !StringUtils.hasText(zipCode)) {
                result.setValid(false);
                result.addValidationError("Required address fields are missing");
                return result;
            }

            // Validate individual components first
            boolean stateValid = validateState(state, countryCode);
            boolean zipValid = validateZipCode(zipCode, countryCode);
            boolean countryValid = validateCountry(countryCode);

            if (!stateValid || !zipValid || !countryValid) {
                result.setValid(false);
                if (!stateValid) result.addValidationError("Invalid state code: " + state);
                if (!zipValid) result.addValidationError("Invalid ZIP code format: " + zipCode);
                if (!countryValid) result.addValidationError("Invalid country code: " + countryCode);
                return result;
            }

            // Perform external validation if enabled
            if (validationEnabled && StringUtils.hasText(apiKey)) {
                ExternalValidationResponse externalResult = callExternalValidationApi(
                    addressLine1, addressLine2, addressLine3, city, state, zipCode, countryCode);
                
                if (externalResult != null) {
                    result.setStandardizedAddress(externalResult.getStandardizedAddress());
                    result.setValid(externalResult.isDeliverable());
                    result.setDeliverable(externalResult.isDeliverable());
                    result.setAddressType(externalResult.getAddressType());
                    result.setLatitude(externalResult.getLatitude());
                    result.setLongitude(externalResult.getLongitude());
                    result.setEnhancedZipCode(externalResult.getZipPlusFour());
                } else {
                    // Fallback to local validation
                    result = performLocalValidation(addressLine1, addressLine2, addressLine3, 
                                                 city, state, zipCode, countryCode);
                }
            } else {
                // Perform local validation only
                result = performLocalValidation(addressLine1, addressLine2, addressLine3, 
                                             city, state, zipCode, countryCode);
            }

            logger.info("Address validation completed. Valid: {}, Deliverable: {}", 
                       result.isValid(), result.isDeliverable());

        } catch (Exception e) {
            logger.error("Error during address validation: {}", e.getMessage(), e);
            result.setValid(false);
            result.addValidationError("Address validation service error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Validates ZIP code format based on country-specific patterns.
     * Supports US (5-digit and ZIP+4), Canadian postal codes, and UK postal codes.
     *
     * @param zipCode ZIP or postal code to validate
     * @param countryCode Country code for format determination
     * @return true if ZIP code format is valid for the specified country
     */
    @Cacheable(value = "zipValidation", key = "#zipCode + '_' + #countryCode")
    public boolean validateZipCode(String zipCode, String countryCode) {
        if (!StringUtils.hasText(zipCode) || !StringUtils.hasText(countryCode)) {
            return false;
        }

        String cleanZip = zipCode.trim().toUpperCase();
        String cleanCountry = countryCode.trim().toUpperCase();

        logger.debug("Validating ZIP code: {} for country: {}", cleanZip, cleanCountry);

        switch (cleanCountry) {
            case "USA":
                return US_ZIP_PATTERN.matcher(cleanZip).matches();
            case "CAN":
                return CA_POSTAL_PATTERN.matcher(cleanZip).matches();
            case "GBR":
                return UK_POSTAL_PATTERN.matcher(cleanZip).matches();
            default:
                // For other countries, basic length and character validation
                return cleanZip.length() >= 3 && cleanZip.length() <= 10 &&
                       cleanZip.matches("^[A-Z0-9\\s-]+$");
        }
    }

    /**
     * Validates state abbreviation against known state codes for the specified country.
     * Supports US states, Canadian provinces, and other country subdivisions.
     *
     * @param state State or province abbreviation (2 characters)
     * @param countryCode Country code for state validation context
     * @return true if state abbreviation is valid for the specified country
     */
    @Cacheable(value = "stateValidation", key = "#state + '_' + #countryCode")
    public boolean validateState(String state, String countryCode) {
        if (!StringUtils.hasText(state) || !StringUtils.hasText(countryCode)) {
            return false;
        }

        String cleanState = state.trim().toUpperCase();
        String cleanCountry = countryCode.trim().toUpperCase();

        logger.debug("Validating state: {} for country: {}", cleanState, cleanCountry);

        // Validate state code length (should be 2 characters as per COBOL copybook)
        if (cleanState.length() != 2) {
            return false;
        }

        switch (cleanCountry) {
            case "USA":
                return VALID_US_STATES.contains(cleanState);
            case "CAN":
                return isValidCanadianProvince(cleanState);
            default:
                // For other countries, allow any 2-character alphabetic code
                return cleanState.matches("^[A-Z]{2}$");
        }
    }

    /**
     * Checks if an address is deliverable based on external validation services
     * and internal business rules. Considers factors such as address completeness,
     * format compliance, and external API deliverability indicators.
     *
     * @param addressLine1 Primary street address
     * @param city City name
     * @param state State abbreviation
     * @param zipCode ZIP code
     * @param countryCode Country code
     * @return true if address is considered deliverable
     */
    @Cacheable(value = "deliverabilityCheck", key = "#addressLine1 + '_' + #city + '_' + #state + '_' + #zipCode")
    public boolean isAddressDeliverable(String addressLine1, String city, String state, String zipCode, String countryCode) {
        logger.debug("Checking deliverability for address: {}, {}, {} {}", addressLine1, city, state, zipCode);

        // Basic completeness check
        if (!StringUtils.hasText(addressLine1) || !StringUtils.hasText(city) || 
            !StringUtils.hasText(state) || !StringUtils.hasText(zipCode)) {
            return false;
        }

        // Validate format compliance
        if (!validateState(state, countryCode) || !validateZipCode(zipCode, countryCode)) {
            return false;
        }

        // Check for common undeliverable patterns
        String addressUpper = addressLine1.toUpperCase();
        if (addressUpper.contains("PO BOX") && !isPoBoxDeliverable(addressLine1, zipCode)) {
            return false;
        }

        if (addressUpper.contains("GENERAL DELIVERY") || 
            addressUpper.contains("HOLD FOR PICKUP") ||
            addressUpper.contains("PMBLIVE") ||
            addressUpper.contains("INSUFFICIENT")) {
            return false;
        }

        // For enhanced deliverability, call external API if available
        if (validationEnabled && StringUtils.hasText(apiKey)) {
            try {
                ExternalValidationResponse response = callExternalValidationApi(
                    addressLine1, null, null, city, state, zipCode, countryCode);
                if (response != null) {
                    return response.isDeliverable();
                }
            } catch (Exception e) {
                logger.warn("External deliverability check failed, using local validation: {}", e.getMessage());
            }
        }

        // Default to deliverable if basic validation passes
        return true;
    }

    /**
     * Enhances basic ZIP code to ZIP+4 format using external validation services.
     * Improves mail delivery precision and address standardization compliance.
     *
     * @param zipCode Basic 5-digit ZIP code
     * @param addressLine1 Street address for ZIP+4 determination
     * @param city City name
     * @param state State abbreviation
     * @return Enhanced ZIP+4 code or original ZIP code if enhancement unavailable
     */
    @Cacheable(value = "zipEnhancement", key = "#zipCode + '_' + #addressLine1")
    public String enhanceZipCode(String zipCode, String addressLine1, String city, String state) {
        if (!StringUtils.hasText(zipCode) || zipCode.length() < 5) {
            return zipCode;
        }

        // If already ZIP+4, return as-is
        if (US_ZIP_PATTERN.matcher(zipCode).matches() && zipCode.contains("-")) {
            return zipCode;
        }

        logger.debug("Enhancing ZIP code: {} for address: {}", zipCode, addressLine1);

        // Call external API for ZIP+4 enhancement
        if (validationEnabled && StringUtils.hasText(apiKey)) {
            try {
                ExternalValidationResponse response = callExternalValidationApi(
                    addressLine1, null, null, city, state, zipCode, "USA");
                if (response != null && StringUtils.hasText(response.getZipPlusFour())) {
                    return response.getZipPlusFour();
                }
            } catch (Exception e) {
                logger.warn("ZIP+4 enhancement failed: {}", e.getMessage());
            }
        }

        // Return original ZIP code if enhancement not available
        return zipCode;
    }

    /**
     * Standardizes city names using external validation services and local rules.
     * Corrects common spelling variations and abbreviations to official postal names.
     *
     * @param city Raw city name input
     * @param state State abbreviation for context
     * @param countryCode Country code for standardization rules
     * @return Standardized city name in proper case
     */
    @Cacheable(value = "cityStandardization", key = "#city + '_' + #state + '_' + #countryCode")
    public String standardizeCity(String city, String state, String countryCode) {
        if (!StringUtils.hasText(city)) {
            return city;
        }

        String cleanCity = city.trim();
        logger.debug("Standardizing city: {} for state: {}", cleanCity, state);

        // Apply basic standardization rules
        cleanCity = applyBasicCityStandardization(cleanCity);

        // Call external API for official city name
        if (validationEnabled && StringUtils.hasText(apiKey)) {
            try {
                ExternalValidationResponse response = callExternalValidationApi(
                    "123 Main St", null, null, cleanCity, state, "12345", countryCode);
                if (response != null && StringUtils.hasText(response.getStandardizedAddress().getCity())) {
                    return response.getStandardizedAddress().getCity();
                }
            } catch (Exception e) {
                logger.warn("External city standardization failed: {}", e.getMessage());
            }
        }

        return cleanCity;
    }

    /**
     * Validates country code against supported country list.
     * Ensures country code compliance with ISO 3166-1 alpha-3 standard.
     *
     * @param countryCode 3-character country code
     * @return true if country code is valid and supported
     */
    @Cacheable(value = "countryValidation", key = "#countryCode")
    public boolean validateCountry(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return false;
        }

        String cleanCountry = countryCode.trim().toUpperCase();
        logger.debug("Validating country code: {}", cleanCountry);

        // Validate format (3 alphabetic characters)
        if (!cleanCountry.matches("^[A-Z]{3}$")) {
            return false;
        }

        return VALID_COUNTRY_CODES.contains(cleanCountry);
    }

    // Private helper methods

    private AddressValidationResult performLocalValidation(
            String addressLine1, String addressLine2, String addressLine3,
            String city, String state, String zipCode, String countryCode) {
        
        AddressValidationResult result = new AddressValidationResult();
        
        // Create standardized address using local rules
        Address standardizedAddress = new Address();
        standardizedAddress.setAddressLine1(standardizeAddressLine(addressLine1));
        standardizedAddress.setAddressLine2(standardizeAddressLine(addressLine2));
        standardizedAddress.setAddressLine3(standardizeAddressLine(addressLine3));
        standardizedAddress.setCity(standardizeCity(city, state, countryCode));
        standardizedAddress.setState(state.toUpperCase());
        standardizedAddress.setZipCode(zipCode);
        standardizedAddress.setCountryCode(countryCode.toUpperCase());

        result.setStandardizedAddress(standardizedAddress);
        result.setValid(true);
        result.setDeliverable(isAddressDeliverable(addressLine1, city, state, zipCode, countryCode));
        result.setAddressType("UNKNOWN");
        result.setValidationTimestamp(LocalDateTime.now());

        return result;
    }

    private ExternalValidationResponse callExternalValidationApi(
            String addressLine1, String addressLine2, String addressLine3,
            String city, String state, String zipCode, String countryCode) {
        
        try {
            String url = addressValidationApiUrl + "/validate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("addressLine1", addressLine1);
            requestBody.put("addressLine2", addressLine2);
            requestBody.put("addressLine3", addressLine3);
            requestBody.put("city", city);
            requestBody.put("state", state);
            requestBody.put("zipCode", zipCode);
            requestBody.put("countryCode", countryCode);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<ExternalValidationResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, ExternalValidationResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                logger.warn("External validation API returned status: {}", response.getStatusCode());
                return null;
            }

        } catch (RestClientException e) {
            logger.error("Failed to call external validation API: {}", e.getMessage());
            return null;
        }
    }

    private boolean isValidCanadianProvince(String province) {
        Set<String> canadianProvinces = Set.of(
            "AB", "BC", "MB", "NB", "NL", "NS", "NT", "NU", "ON", "PE", "QC", "SK", "YT"
        );
        return canadianProvinces.contains(province);
    }

    private boolean isPoBoxDeliverable(String address, String zipCode) {
        // PO Boxes are generally deliverable if they have valid ZIP codes
        // Additional business rules can be added here
        return StringUtils.hasText(zipCode) && zipCode.length() >= 5;
    }

    private String standardizeAddressLine(String addressLine) {
        if (!StringUtils.hasText(addressLine)) {
            return addressLine;
        }

        String standardized = addressLine.trim();
        
        // Apply common standardization rules
        standardized = standardized.replaceAll("\\s+", " "); // Multiple spaces to single space
        standardized = standardized.replaceAll("(?i)\\bSTREET\\b", "ST");
        standardized = standardized.replaceAll("(?i)\\bAVENUE\\b", "AVE");
        standardized = standardized.replaceAll("(?i)\\bROAD\\b", "RD");
        standardized = standardized.replaceAll("(?i)\\bDRIVE\\b", "DR");
        standardized = standardized.replaceAll("(?i)\\bLANE\\b", "LN");
        standardized = standardized.replaceAll("(?i)\\bCOURT\\b", "CT");
        standardized = standardized.replaceAll("(?i)\\bCIRCLE\\b", "CIR");
        standardized = standardized.replaceAll("(?i)\\bBOULEVARD\\b", "BLVD");
        standardized = standardized.replaceAll("(?i)\\bAPARTMENT\\b", "APT");
        standardized = standardized.replaceAll("(?i)\\bSUITE\\b", "STE");

        return toTitleCase(standardized);
    }

    private String applyBasicCityStandardization(String city) {
        String standardized = city.trim();
        
        // Remove common prefixes/suffixes that aren't part of official names
        standardized = standardized.replaceAll("(?i)\\bCITY OF\\b", "");
        standardized = standardized.trim();
        
        return toTitleCase(standardized);
    }

    private String toTitleCase(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c) || c == '-' || c == '.') {
                nextTitleCase = true;
                result.append(c);
            } else if (nextTitleCase) {
                result.append(Character.toTitleCase(c));
                nextTitleCase = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    // Inner classes for address validation data structures

    public static class AddressValidationResult {
        private Address inputAddress;
        private Address standardizedAddress;
        private boolean valid;
        private boolean deliverable;
        private String addressType;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String enhancedZipCode;
        private LocalDateTime validationTimestamp;
        private java.util.List<String> validationErrors = new java.util.ArrayList<>();

        // Getters and setters
        public Address getInputAddress() { return inputAddress; }
        public void setInputAddress(Address inputAddress) { this.inputAddress = inputAddress; }
        
        public Address getStandardizedAddress() { return standardizedAddress; }
        public void setStandardizedAddress(Address standardizedAddress) { this.standardizedAddress = standardizedAddress; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public boolean isDeliverable() { return deliverable; }
        public void setDeliverable(boolean deliverable) { this.deliverable = deliverable; }
        
        public String getAddressType() { return addressType; }
        public void setAddressType(String addressType) { this.addressType = addressType; }
        
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
        
        public String getEnhancedZipCode() { return enhancedZipCode; }
        public void setEnhancedZipCode(String enhancedZipCode) { this.enhancedZipCode = enhancedZipCode; }
        
        public LocalDateTime getValidationTimestamp() { return validationTimestamp; }
        public void setValidationTimestamp(LocalDateTime validationTimestamp) { this.validationTimestamp = validationTimestamp; }
        
        public java.util.List<String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(java.util.List<String> validationErrors) { this.validationErrors = validationErrors; }
        
        public void addValidationError(String error) { this.validationErrors.add(error); }
    }

    public static class Address {
        private String addressLine1;
        private String addressLine2;
        private String addressLine3;
        private String city;
        private String state;
        private String zipCode;
        private String countryCode;

        public Address() {}

        public Address(String addressLine1, String addressLine2, String addressLine3, 
                      String city, String state, String zipCode, String countryCode) {
            this.addressLine1 = addressLine1;
            this.addressLine2 = addressLine2;
            this.addressLine3 = addressLine3;
            this.city = city;
            this.state = state;
            this.zipCode = zipCode;
            this.countryCode = countryCode;
        }

        // Getters and setters
        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
        
        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
        
        public String getAddressLine3() { return addressLine3; }
        public void setAddressLine3(String addressLine3) { this.addressLine3 = addressLine3; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
        
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    }

    public static class ExternalValidationResponse {
        private Address standardizedAddress;
        private boolean deliverable;
        private String addressType;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String zipPlusFour;

        // Getters and setters
        public Address getStandardizedAddress() { return standardizedAddress; }
        public void setStandardizedAddress(Address standardizedAddress) { this.standardizedAddress = standardizedAddress; }
        
        public boolean isDeliverable() { return deliverable; }
        public void setDeliverable(boolean deliverable) { this.deliverable = deliverable; }
        
        public String getAddressType() { return addressType; }
        public void setAddressType(String addressType) { this.addressType = addressType; }
        
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
        
        public String getZipPlusFour() { return zipPlusFour; }
        public void setZipPlusFour(String zipPlusFour) { this.zipPlusFour = zipPlusFour; }
    }
}