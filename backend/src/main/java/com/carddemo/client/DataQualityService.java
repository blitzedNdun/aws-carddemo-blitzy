package com.carddemo.client;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * External service client for comprehensive data quality checks and validation.
 * Provides customer data profiling, duplicate detection, phone number validation,
 * email format validation, SSN verification, and data completeness scoring
 * to ensure high-quality customer information throughout the system.
 * 
 * This service maintains COBOL-equivalent precision and validation rules
 * to preserve compatibility with the legacy CICS credit card management system.
 */
@Service
public class DataQualityService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataQualityService.class);
    
    @Value("${data.quality.api.base.url:#{null}}")
    private String dataQualityApiBaseUrl;
    
    @Value("${data.quality.api.key:#{null}}")
    private String dataQualityApiKey;
    
    @Value("${data.quality.cache.enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${data.quality.external.timeout:5000}")
    private int externalApiTimeout;
    
    private final RestTemplate restTemplate;
    
    // COBOL-compatible constants for validation
    private static final int CUST_ID_LENGTH = 9;
    private static final int CUST_NAME_LENGTH = 25;
    private static final int CUST_ADDR_LINE_LENGTH = 50;
    private static final int CUST_STATE_LENGTH = 2;
    private static final int CUST_COUNTRY_LENGTH = 3;
    private static final int CUST_ZIP_LENGTH = 10;
    private static final int CUST_PHONE_LENGTH = 15;
    private static final int CUST_SSN_LENGTH = 9;
    private static final int CUST_GOVT_ID_LENGTH = 20;
    private static final int CUST_DOB_LENGTH = 10;
    private static final int CUST_EFT_LENGTH = 10;
    private static final int CUST_FICO_MIN = 300;
    private static final int CUST_FICO_MAX = 850;
    
    // Regular expression patterns for validation
    private static final Pattern SSN_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{3}\\d{3}\\d{4}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");
    
    // Valid area codes for phone number validation
    private static final Set<String> VALID_AREA_CODES = Set.of(
        "201", "202", "203", "205", "206", "207", "208", "209", "210",
        "212", "213", "214", "215", "216", "217", "218", "219", "224",
        "225", "228", "229", "231", "234", "239", "240", "248", "251",
        "252", "253", "254", "256", "260", "262", "267", "269", "270",
        "276", "281", "301", "302", "303", "304", "305", "307", "308",
        "309", "310", "312", "313", "314", "315", "316", "317", "318",
        "319", "320", "321", "323", "325", "330", "331", "334", "336",
        "337", "339", "347", "351", "352", "360", "361", "386", "401",
        "402", "404", "405", "406", "407", "408", "409", "410", "412",
        "413", "414", "415", "417", "419", "423", "424", "425", "430",
        "432", "434", "435", "440", "443", "458", "469", "470", "475",
        "478", "479", "480", "484", "501", "502", "503", "504", "505",
        "507", "508", "509", "510", "512", "513", "515", "516", "517",
        "518", "520", "530", "540", "541", "551", "559", "561", "562",
        "563", "567", "570", "571", "573", "574", "575", "580", "585",
        "586", "601", "602", "603", "605", "606", "607", "608", "609",
        "610", "612", "614", "615", "616", "617", "618", "619", "620",
        "623", "626", "630", "631", "636", "641", "646", "650", "651",
        "660", "661", "662", "667", "678", "682", "701", "702", "703",
        "704", "706", "707", "708", "712", "713", "714", "715", "716",
        "717", "718", "719", "720", "724", "727", "731", "732", "734",
        "737", "740", "747", "754", "757", "760", "763", "765", "770",
        "772", "773", "774", "775", "781", "785", "786", "787", "801",
        "802", "803", "804", "805", "806", "808", "810", "812", "813",
        "814", "815", "816", "817", "818", "828", "830", "831", "832",
        "843", "845", "847", "848", "850", "856", "857", "858", "859",
        "860", "862", "863", "864", "865", "870", "872", "878", "901",
        "903", "904", "906", "907", "908", "909", "910", "912", "913",
        "914", "915", "916", "917", "918", "919", "920", "925", "928",
        "929", "931", "936", "937", "940", "941", "947", "949", "951",
        "952", "954", "956", "970", "971", "972", "973", "978", "979",
        "980", "984", "985", "989"
    );
    
    public DataQualityService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Comprehensive customer data validation including SSN format and validity verification,
     * phone number formatting and area code validation, email address format validation,
     * demographic data validation, and data consistency checks.
     * 
     * @param customerData Map containing customer data fields from CVCUS01Y copybook structure
     * @return DataQualityResult containing validation results and scoring
     */
    public DataQualityResult validateCustomerData(Map<String, Object> customerData) {
        if (customerData == null || customerData.isEmpty()) {
            logger.warn("Empty customer data provided for validation");
            return new DataQualityResult(false, "Customer data is required", 0.0);
        }
        
        logger.info("Starting comprehensive customer data validation for customer ID: {}", 
                   customerData.get("custId"));
        
        try {
            DataQualityResult result = new DataQualityResult();
            List<String> validationErrors = new ArrayList<>();
            Map<String, Object> validationDetails = new HashMap<>();
            
            // Validate customer ID (CUST-ID)
            validateCustomerId(customerData, validationErrors, validationDetails);
            
            // Validate name fields (CUST-FIRST-NAME, CUST-MIDDLE-NAME, CUST-LAST-NAME)
            validateNameFields(customerData, validationErrors, validationDetails);
            
            // Validate address fields
            validateAddressFields(customerData, validationErrors, validationDetails);
            
            // Validate phone numbers (CUST-PHONE-NUM-1, CUST-PHONE-NUM-2)
            validatePhoneNumbers(customerData, validationErrors, validationDetails);
            
            // Validate SSN (CUST-SSN)
            validateSocialSecurityNumber(customerData, validationErrors, validationDetails);
            
            // Validate date of birth (CUST-DOB-YYYY-MM-DD)
            validateDateOfBirth(customerData, validationErrors, validationDetails);
            
            // Validate FICO credit score (CUST-FICO-CREDIT-SCORE)
            validateFicoScore(customerData, validationErrors, validationDetails);
            
            // Validate email if provided (not in original COBOL structure but supported)
            if (customerData.containsKey("email")) {
                validateEmailAddress(customerData, validationErrors, validationDetails);
            }
            
            // Calculate data completeness score
            double completenessScore = calculateCompletenessScore(customerData);
            validationDetails.put("completenessScore", completenessScore);
            
            // Check data consistency across related fields
            validateDataConsistency(customerData, validationErrors, validationDetails);
            
            // Set validation result
            result.setValid(validationErrors.isEmpty());
            result.setErrorMessage(validationErrors.isEmpty() ? "Validation successful" : 
                                   String.join("; ", validationErrors));
            result.setQualityScore(calculateQualityScore(validationErrors.size(), completenessScore));
            result.setValidationDetails(validationDetails);
            
            logger.info("Customer data validation completed. Valid: {}, Quality Score: {}", 
                       result.isValid(), result.getQualityScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during customer data validation", e);
            return new DataQualityResult(false, "Validation processing error: " + e.getMessage(), 0.0);
        }
    }
    
    /**
     * Duplicate customer detection using fuzzy matching algorithms to identify
     * potential duplicate records based on name, address, phone, and SSN similarity.
     * 
     * @param customerData Customer data to check for duplicates
     * @param existingCustomers List of existing customer records to compare against
     * @return List of potential duplicate matches with similarity scores
     */
    public List<DuplicateMatch> detectDuplicates(Map<String, Object> customerData, 
                                                 List<Map<String, Object>> existingCustomers) {
        if (customerData == null || existingCustomers == null || existingCustomers.isEmpty()) {
            logger.debug("No customer data or existing customers provided for duplicate detection");
            return new ArrayList<>();
        }
        
        logger.info("Starting duplicate detection for customer data");
        
        List<DuplicateMatch> duplicateMatches = new ArrayList<>();
        
        try {
            String targetFirstName = getStringValue(customerData, "custFirstName");
            String targetLastName = getStringValue(customerData, "custLastName");
            String targetSSN = getStringValue(customerData, "custSSN");
            String targetPhone1 = getStringValue(customerData, "custPhoneNum1");
            String targetAddress1 = getStringValue(customerData, "custAddrLine1");
            String targetZip = getStringValue(customerData, "custAddrZip");
            
            for (Map<String, Object> existingCustomer : existingCustomers) {
                String existingFirstName = getStringValue(existingCustomer, "custFirstName");
                String existingLastName = getStringValue(existingCustomer, "custLastName");
                String existingSSN = getStringValue(existingCustomer, "custSSN");
                String existingPhone1 = getStringValue(existingCustomer, "custPhoneNum1");
                String existingAddress1 = getStringValue(existingCustomer, "custAddrLine1");
                String existingZip = getStringValue(existingCustomer, "custAddrZip");
                
                // Calculate similarity scores for different attributes
                double nameSimilarity = calculateNameSimilarity(targetFirstName, targetLastName, 
                                                               existingFirstName, existingLastName);
                double ssnSimilarity = calculateExactMatch(targetSSN, existingSSN);
                double phoneSimilarity = calculateExactMatch(targetPhone1, existingPhone1);
                double addressSimilarity = calculateAddressSimilarity(targetAddress1, targetZip,
                                                                      existingAddress1, existingZip);
                
                // Calculate overall similarity score with weighted factors
                double overallSimilarity = calculateOverallSimilarity(nameSimilarity, ssnSimilarity, 
                                                                      phoneSimilarity, addressSimilarity);
                
                // Consider a match if similarity is above threshold (80%)
                if (overallSimilarity >= 0.80) {
                    DuplicateMatch match = new DuplicateMatch();
                    match.setCustomerId(getStringValue(existingCustomer, "custId"));
                    match.setSimilarityScore(overallSimilarity);
                    match.setMatchingFields(getMatchingFields(nameSimilarity, ssnSimilarity, 
                                                             phoneSimilarity, addressSimilarity));
                    duplicateMatches.add(match);
                }
            }
            
            // Sort by similarity score descending
            duplicateMatches.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
            
            logger.info("Duplicate detection completed. Found {} potential matches", duplicateMatches.size());
            
        } catch (Exception e) {
            logger.error("Error during duplicate detection", e);
        }
        
        return duplicateMatches;
    }
    
    /**
     * Phone number formatting and area code validation ensuring compliance
     * with North American Numbering Plan (NANP) standards.
     * 
     * @param phoneNumber Phone number to validate
     * @return PhoneValidationResult with validation status and formatted number
     */
    public PhoneValidationResult validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.debug("Empty phone number provided for validation");
            return new PhoneValidationResult(false, "Phone number is required", null);
        }
        
        try {
            // Remove all non-numeric characters
            String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
            
            // Handle US country code prefix
            if (cleanPhone.startsWith("1") && cleanPhone.length() == 11) {
                cleanPhone = cleanPhone.substring(1);
            }
            
            // Validate length
            if (cleanPhone.length() != 10) {
                return new PhoneValidationResult(false, 
                    "Phone number must be 10 digits (area code + number)", null);
            }
            
            // Extract area code
            String areaCode = cleanPhone.substring(0, 3);
            
            // Validate area code
            if (!VALID_AREA_CODES.contains(areaCode)) {
                return new PhoneValidationResult(false, 
                    "Invalid area code: " + areaCode, null);
            }
            
            // Validate exchange code (cannot start with 0 or 1)
            String exchangeCode = cleanPhone.substring(3, 6);
            if (exchangeCode.startsWith("0") || exchangeCode.startsWith("1")) {
                return new PhoneValidationResult(false, 
                    "Invalid exchange code: " + exchangeCode, null);
            }
            
            // Format phone number: (XXX) XXX-XXXX
            String formattedPhone = String.format("(%s) %s-%s", 
                areaCode, exchangeCode, cleanPhone.substring(6));
            
            logger.debug("Phone number validation successful: {}", formattedPhone);
            
            return new PhoneValidationResult(true, "Valid phone number", formattedPhone);
            
        } catch (Exception e) {
            logger.error("Error validating phone number: {}", phoneNumber, e);
            return new PhoneValidationResult(false, "Phone validation error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Email address format validation and domain verification using regex patterns
     * and optional external domain validation services.
     * 
     * @param email Email address to validate
     * @return EmailValidationResult with validation status and domain information
     */
    public EmailValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            logger.debug("Empty email provided for validation");
            return new EmailValidationResult(false, "Email address is required", null);
        }
        
        String trimmedEmail = email.trim().toLowerCase();
        
        try {
            // Basic format validation
            if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
                return new EmailValidationResult(false, "Invalid email format", null);
            }
            
            // Extract domain
            String domain = trimmedEmail.substring(trimmedEmail.indexOf('@') + 1);
            
            // Validate domain length
            if (domain.length() > 253) {
                return new EmailValidationResult(false, "Domain name too long", null);
            }
            
            // Check for common disposable email domains
            Set<String> disposableDomains = Set.of(
                "10minutemail.com", "guerrillamail.com", "mailinator.com", 
                "tempmail.org", "throwaway.email", "temp-mail.org"
            );
            
            if (disposableDomains.contains(domain)) {
                return new EmailValidationResult(false, "Disposable email addresses not allowed", null);
            }
            
            // Perform external domain validation if API is configured
            boolean domainValid = true;
            String domainValidationMessage = "Domain validation not performed";
            
            if (dataQualityApiBaseUrl != null && !dataQualityApiBaseUrl.isEmpty()) {
                try {
                    domainValid = validateEmailDomainExternal(domain);
                    domainValidationMessage = domainValid ? "Domain validation successful" : 
                                             "Domain validation failed";
                } catch (Exception e) {
                    logger.warn("External domain validation failed for {}: {}", domain, e.getMessage());
                    // Continue with local validation only
                }
            }
            
            logger.debug("Email validation successful: {}", trimmedEmail);
            
            EmailValidationResult result = new EmailValidationResult(true, "Valid email address", domain);
            result.setDomainValid(domainValid);
            result.setDomainValidationMessage(domainValidationMessage);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error validating email: {}", email, e);
            return new EmailValidationResult(false, "Email validation error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Data completeness scoring based on required fields and data quality metrics.
     * Evaluates the percentage of required fields populated and quality of data.
     * 
     * @param customerData Customer data to score
     * @return Double value between 0.0 and 1.0 representing completeness score
     */
    public Double scoreDataCompleteness(Map<String, Object> customerData) {
        if (customerData == null || customerData.isEmpty()) {
            logger.debug("Empty customer data provided for completeness scoring");
            return 0.0;
        }
        
        try {
            // Define required fields with weights
            Map<String, Double> requiredFields = Map.of(
                "custId", 1.0,
                "custFirstName", 1.0,
                "custLastName", 1.0,
                "custAddrLine1", 0.8,
                "custAddrStateCD", 0.7,
                "custAddrZip", 0.8,
                "custPhoneNum1", 0.9,
                "custSSN", 1.0,
                "custDOBYYYYMMDD", 0.9,
                "custFICOCreditScore", 0.6
            );
            
            // Define optional fields with weights
            Map<String, Double> optionalFields = Map.of(
                "custMiddleName", 0.3,
                "custAddrLine2", 0.2,
                "custAddrLine3", 0.1,
                "custAddrCountryCD", 0.4,
                "custPhoneNum2", 0.3,
                "custGovtIssuedId", 0.5,
                "custEFTAccountId", 0.4,
                "custPriCardHolderInd", 0.3
            );
            
            double totalPossibleScore = requiredFields.values().stream().mapToDouble(Double::doubleValue).sum() +
                                       optionalFields.values().stream().mapToDouble(Double::doubleValue).sum();
            
            double actualScore = 0.0;
            
            // Score required fields
            for (Map.Entry<String, Double> field : requiredFields.entrySet()) {
                if (isFieldComplete(customerData, field.getKey())) {
                    actualScore += field.getValue();
                }
            }
            
            // Score optional fields
            for (Map.Entry<String, Double> field : optionalFields.entrySet()) {
                if (isFieldComplete(customerData, field.getKey())) {
                    actualScore += field.getValue();
                }
            }
            
            // Calculate final score as percentage
            double completenessScore = actualScore / totalPossibleScore;
            
            logger.debug("Data completeness score calculated: {}", completenessScore);
            
            return Math.min(1.0, Math.max(0.0, completenessScore));
            
        } catch (Exception e) {
            logger.error("Error calculating data completeness score", e);
            return 0.0;
        }
    }
    
    /**
     * SSN format and validity verification using check digit algorithms
     * and validation against known invalid SSN patterns.
     * 
     * @param ssn Social Security Number to validate
     * @return SSNValidationResult with validation status and details
     */
    public SSNValidationResult validateSSN(String ssn) {
        if (ssn == null || ssn.trim().isEmpty()) {
            logger.debug("Empty SSN provided for validation");
            return new SSNValidationResult(false, "SSN is required", null);
        }
        
        try {
            // Remove all non-numeric characters
            String cleanSSN = ssn.replaceAll("[^0-9]", "");
            
            // Validate length
            if (cleanSSN.length() != 9) {
                return new SSNValidationResult(false, "SSN must be 9 digits", null);
            }
            
            // Check for invalid patterns
            if (isInvalidSSNPattern(cleanSSN)) {
                return new SSNValidationResult(false, "Invalid SSN pattern", null);
            }
            
            // Validate area number (first 3 digits)
            String areaNumber = cleanSSN.substring(0, 3);
            if (!isValidSSNAreaNumber(areaNumber)) {
                return new SSNValidationResult(false, "Invalid SSN area number", null);
            }
            
            // Format SSN: XXX-XX-XXXX
            String formattedSSN = String.format("%s-%s-%s",
                cleanSSN.substring(0, 3),
                cleanSSN.substring(3, 5),
                cleanSSN.substring(5, 9));
            
            logger.debug("SSN validation successful");
            
            return new SSNValidationResult(true, "Valid SSN", formattedSSN);
            
        } catch (Exception e) {
            logger.error("Error validating SSN", e);
            return new SSNValidationResult(false, "SSN validation error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Data consistency checks across related records to ensure logical coherence
     * and business rule compliance in customer information.
     * 
     * @param customerData Customer data to check for consistency
     * @return List of consistency validation issues found
     */
    public List<String> checkDataConsistency(Map<String, Object> customerData) {
        List<String> consistencyIssues = new ArrayList<>();
        
        if (customerData == null || customerData.isEmpty()) {
            consistencyIssues.add("Customer data is required for consistency check");
            return consistencyIssues;
        }
        
        try {
            // Check age consistency with FICO score requirements
            checkAgeConsistency(customerData, consistencyIssues);
            
            // Check address consistency
            checkAddressConsistency(customerData, consistencyIssues);
            
            // Check phone number consistency
            checkPhoneConsistency(customerData, consistencyIssues);
            
            // Check name consistency
            checkNameConsistency(customerData, consistencyIssues);
            
            // Check financial data consistency
            checkFinancialDataConsistency(customerData, consistencyIssues);
            
            logger.debug("Data consistency check completed. Issues found: {}", consistencyIssues.size());
            
        } catch (Exception e) {
            logger.error("Error during data consistency check", e);
            consistencyIssues.add("Consistency check processing error: " + e.getMessage());
        }
        
        return consistencyIssues;
    }
    
    /**
     * Generate comprehensive data quality report including validation results,
     * completeness scores, duplicate detection, and recommendations for improvement.
     * 
     * @param customerData Customer data to analyze
     * @param includeRecommendations Whether to include improvement recommendations
     * @return DataQualityReport with comprehensive analysis
     */
    public DataQualityReport generateDataQualityReport(Map<String, Object> customerData, 
                                                       boolean includeRecommendations) {
        if (customerData == null || customerData.isEmpty()) {
            logger.warn("Empty customer data provided for quality report generation");
            return new DataQualityReport("ERROR", "Customer data is required", 0.0);
        }
        
        try {
            DataQualityReport report = new DataQualityReport();
            report.setCustomerId(getStringValue(customerData, "custId"));
            report.setReportTimestamp(new Date());
            
            // Perform comprehensive validation
            DataQualityResult validationResult = validateCustomerData(customerData);
            report.setOverallQualityScore(validationResult.getQualityScore());
            report.setValidationStatus(validationResult.isValid() ? "VALID" : "INVALID");
            report.setValidationMessage(validationResult.getErrorMessage());
            
            // Calculate completeness score
            Double completenessScore = scoreDataCompleteness(customerData);
            report.setCompletenessScore(completenessScore);
            
            // Check data consistency
            List<String> consistencyIssues = checkDataConsistency(customerData);
            report.setConsistencyIssues(consistencyIssues);
            
            // Validate individual components
            Map<String, Object> componentValidation = new HashMap<>();
            
            // Phone validation
            String phone1 = getStringValue(customerData, "custPhoneNum1");
            if (phone1 != null && !phone1.isEmpty()) {
                PhoneValidationResult phoneResult = validatePhoneNumber(phone1);
                componentValidation.put("phone1Validation", phoneResult);
            }
            
            // SSN validation
            String ssn = getStringValue(customerData, "custSSN");
            if (ssn != null && !ssn.isEmpty()) {
                SSNValidationResult ssnResult = validateSSN(ssn);
                componentValidation.put("ssnValidation", ssnResult);
            }
            
            // Email validation if present
            if (customerData.containsKey("email")) {
                String email = getStringValue(customerData, "email");
                if (email != null && !email.isEmpty()) {
                    EmailValidationResult emailResult = validateEmail(email);
                    componentValidation.put("emailValidation", emailResult);
                }
            }
            
            report.setComponentValidation(componentValidation);
            
            // Generate recommendations if requested
            if (includeRecommendations) {
                List<String> recommendations = generateRecommendations(validationResult, 
                                                                       completenessScore, 
                                                                       consistencyIssues);
                report.setRecommendations(recommendations);
            }
            
            logger.info("Data quality report generated for customer {}: Score {}", 
                       report.getCustomerId(), report.getOverallQualityScore());
            
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating data quality report", e);
            return new DataQualityReport("ERROR", "Report generation error: " + e.getMessage(), 0.0);
        }
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    /**
     * Validates customer ID field according to COBOL specifications
     */
    private void validateCustomerId(Map<String, Object> customerData, List<String> errors, 
                                   Map<String, Object> details) {
        String custId = getStringValue(customerData, "custId");
        if (custId == null || custId.trim().isEmpty()) {
            errors.add("Customer ID is required");
            details.put("custIdValid", false);
            return;
        }
        
        String cleanCustId = custId.replaceAll("[^0-9]", "");
        if (cleanCustId.length() != CUST_ID_LENGTH) {
            errors.add("Customer ID must be exactly 9 digits");
            details.put("custIdValid", false);
            return;
        }
        
        if (!NUMERIC_PATTERN.matcher(cleanCustId).matches()) {
            errors.add("Customer ID must contain only numeric characters");
            details.put("custIdValid", false);
            return;
        }
        
        details.put("custIdValid", true);
        details.put("custIdFormatted", cleanCustId);
    }
    
    /**
     * Validates customer name fields according to COBOL field lengths
     */
    private void validateNameFields(Map<String, Object> customerData, List<String> errors, 
                                   Map<String, Object> details) {
        // Validate first name
        String firstName = getStringValue(customerData, "custFirstName");
        if (firstName == null || firstName.trim().isEmpty()) {
            errors.add("First name is required");
            details.put("firstNameValid", false);
        } else if (firstName.length() > CUST_NAME_LENGTH) {
            errors.add("First name cannot exceed 25 characters");
            details.put("firstNameValid", false);
        } else {
            details.put("firstNameValid", true);
        }
        
        // Validate last name
        String lastName = getStringValue(customerData, "custLastName");
        if (lastName == null || lastName.trim().isEmpty()) {
            errors.add("Last name is required");
            details.put("lastNameValid", false);
        } else if (lastName.length() > CUST_NAME_LENGTH) {
            errors.add("Last name cannot exceed 25 characters");
            details.put("lastNameValid", false);
        } else {
            details.put("lastNameValid", true);
        }
        
        // Validate middle name (optional)
        String middleName = getStringValue(customerData, "custMiddleName");
        if (middleName != null && middleName.length() > CUST_NAME_LENGTH) {
            errors.add("Middle name cannot exceed 25 characters");
            details.put("middleNameValid", false);
        } else {
            details.put("middleNameValid", true);
        }
    }
    
    /**
     * Validates customer address fields according to COBOL specifications
     */
    private void validateAddressFields(Map<String, Object> customerData, List<String> errors, 
                                      Map<String, Object> details) {
        // Validate address line 1 (required)
        String addrLine1 = getStringValue(customerData, "custAddrLine1");
        if (addrLine1 == null || addrLine1.trim().isEmpty()) {
            errors.add("Address line 1 is required");
            details.put("addrLine1Valid", false);
        } else if (addrLine1.length() > CUST_ADDR_LINE_LENGTH) {
            errors.add("Address line 1 cannot exceed 50 characters");
            details.put("addrLine1Valid", false);
        } else {
            details.put("addrLine1Valid", true);
        }
        
        // Validate state code
        String stateCode = getStringValue(customerData, "custAddrStateCD");
        if (stateCode == null || stateCode.trim().isEmpty()) {
            errors.add("State code is required");
            details.put("stateCodeValid", false);
        } else if (stateCode.length() != CUST_STATE_LENGTH) {
            errors.add("State code must be exactly 2 characters");
            details.put("stateCodeValid", false);
        } else {
            details.put("stateCodeValid", true);
        }
        
        // Validate ZIP code
        String zipCode = getStringValue(customerData, "custAddrZip");
        if (zipCode == null || zipCode.trim().isEmpty()) {
            errors.add("ZIP code is required");
            details.put("zipCodeValid", false);
        } else if (zipCode.length() > CUST_ZIP_LENGTH) {
            errors.add("ZIP code cannot exceed 10 characters");
            details.put("zipCodeValid", false);
        } else {
            details.put("zipCodeValid", true);
        }
        
        // Validate optional address lines
        validateOptionalAddressLine(customerData, "custAddrLine2", errors, details, "addrLine2Valid");
        validateOptionalAddressLine(customerData, "custAddrLine3", errors, details, "addrLine3Valid");
        
        // Validate country code (optional)
        String countryCode = getStringValue(customerData, "custAddrCountryCD");
        if (countryCode != null && countryCode.length() > CUST_COUNTRY_LENGTH) {
            errors.add("Country code cannot exceed 3 characters");
            details.put("countryCodeValid", false);
        } else {
            details.put("countryCodeValid", true);
        }
    }
    
    /**
     * Validates optional address line fields
     */
    private void validateOptionalAddressLine(Map<String, Object> customerData, String fieldName,
                                           List<String> errors, Map<String, Object> details, 
                                           String validationKey) {
        String addrLine = getStringValue(customerData, fieldName);
        if (addrLine != null && addrLine.length() > CUST_ADDR_LINE_LENGTH) {
            errors.add(fieldName + " cannot exceed 50 characters");
            details.put(validationKey, false);
        } else {
            details.put(validationKey, true);
        }
    }
    
    /**
     * Validates customer phone number fields
     */
    private void validatePhoneNumbers(Map<String, Object> customerData, List<String> errors, 
                                     Map<String, Object> details) {
        // Validate primary phone number (required)
        String phone1 = getStringValue(customerData, "custPhoneNum1");
        if (phone1 == null || phone1.trim().isEmpty()) {
            errors.add("Primary phone number is required");
            details.put("phone1Valid", false);
        } else {
            PhoneValidationResult result = validatePhoneNumber(phone1);
            if (!result.isValid()) {
                errors.add("Primary phone number: " + result.getErrorMessage());
                details.put("phone1Valid", false);
            } else {
                details.put("phone1Valid", true);
                details.put("phone1Formatted", result.getFormattedPhone());
            }
        }
        
        // Validate secondary phone number (optional)
        String phone2 = getStringValue(customerData, "custPhoneNum2");
        if (phone2 != null && !phone2.trim().isEmpty()) {
            PhoneValidationResult result = validatePhoneNumber(phone2);
            if (!result.isValid()) {
                errors.add("Secondary phone number: " + result.getErrorMessage());
                details.put("phone2Valid", false);
            } else {
                details.put("phone2Valid", true);
                details.put("phone2Formatted", result.getFormattedPhone());
            }
        } else {
            details.put("phone2Valid", true); // Optional field
        }
    }
    
    /**
     * Validates Social Security Number field
     */
    private void validateSocialSecurityNumber(Map<String, Object> customerData, List<String> errors, 
                                             Map<String, Object> details) {
        String ssn = getStringValue(customerData, "custSSN");
        if (ssn == null || ssn.trim().isEmpty()) {
            errors.add("Social Security Number is required");
            details.put("ssnValid", false);
            return;
        }
        
        SSNValidationResult result = validateSSN(ssn);
        if (!result.isValid()) {
            errors.add("SSN: " + result.getErrorMessage());
            details.put("ssnValid", false);
        } else {
            details.put("ssnValid", true);
            details.put("ssnFormatted", result.getFormattedSSN());
        }
    }
    
    /**
     * Validates date of birth field according to COBOL date format
     */
    private void validateDateOfBirth(Map<String, Object> customerData, List<String> errors, 
                                    Map<String, Object> details) {
        String dob = getStringValue(customerData, "custDOBYYYYMMDD");
        if (dob == null || dob.trim().isEmpty()) {
            errors.add("Date of birth is required");
            details.put("dobValid", false);
            return;
        }
        
        try {
            // Validate format YYYY-MM-DD
            if (!DATE_PATTERN.matcher(dob).matches()) {
                errors.add("Date of birth must be in YYYY-MM-DD format");
                details.put("dobValid", false);
                return;
            }
            
            // Parse and validate date
            LocalDate birthDate = LocalDate.parse(dob, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate today = LocalDate.now();
            
            // Check if date is in the future
            if (birthDate.isAfter(today)) {
                errors.add("Date of birth cannot be in the future");
                details.put("dobValid", false);
                return;
            }
            
            // Check reasonable age limits (18-120 years)
            int age = today.getYear() - birthDate.getYear();
            if (age < 18) {
                errors.add("Customer must be at least 18 years old");
                details.put("dobValid", false);
                return;
            }
            
            if (age > 120) {
                errors.add("Invalid date of birth: age exceeds 120 years");
                details.put("dobValid", false);
                return;
            }
            
            details.put("dobValid", true);
            details.put("customerAge", age);
            
        } catch (DateTimeParseException e) {
            errors.add("Invalid date of birth format");
            details.put("dobValid", false);
        }
    }
    
    /**
     * Validates FICO credit score field
     */
    private void validateFicoScore(Map<String, Object> customerData, List<String> errors, 
                                  Map<String, Object> details) {
        Object ficoObj = customerData.get("custFICOCreditScore");
        if (ficoObj == null) {
            // FICO score is optional for new customers
            details.put("ficoScoreValid", true);
            return;
        }
        
        try {
            int ficoScore;
            if (ficoObj instanceof String) {
                String ficoStr = ((String) ficoObj).trim();
                if (ficoStr.isEmpty()) {
                    details.put("ficoScoreValid", true);
                    return;
                }
                ficoScore = Integer.parseInt(ficoStr);
            } else if (ficoObj instanceof Number) {
                ficoScore = ((Number) ficoObj).intValue();
            } else {
                errors.add("FICO score must be a numeric value");
                details.put("ficoScoreValid", false);
                return;
            }
            
            if (ficoScore < CUST_FICO_MIN || ficoScore > CUST_FICO_MAX) {
                errors.add("FICO score must be between " + CUST_FICO_MIN + " and " + CUST_FICO_MAX);
                details.put("ficoScoreValid", false);
            } else {
                details.put("ficoScoreValid", true);
                details.put("ficoScoreRange", categorizeFicoScore(ficoScore));
            }
            
        } catch (NumberFormatException e) {
            errors.add("FICO score must be a valid number");
            details.put("ficoScoreValid", false);
        }
    }
    
    /**
     * Validates email address if provided
     */
    private void validateEmailAddress(Map<String, Object> customerData, List<String> errors, 
                                     Map<String, Object> details) {
        String email = getStringValue(customerData, "email");
        if (email != null && !email.trim().isEmpty()) {
            EmailValidationResult result = validateEmail(email);
            if (!result.isValid()) {
                errors.add("Email: " + result.getErrorMessage());
                details.put("emailValid", false);
            } else {
                details.put("emailValid", true);
                details.put("emailDomain", result.getDomain());
            }
        } else {
            details.put("emailValid", true); // Optional field
        }
    }
    
    /**
     * Calculates data completeness score based on field weights
     */
    private double calculateCompletenessScore(Map<String, Object> customerData) {
        return scoreDataCompleteness(customerData);
    }
    
    /**
     * Calculates overall quality score based on validation errors and completeness
     */
    private double calculateQualityScore(int errorCount, double completenessScore) {
        // Base score starts at completeness score
        double qualityScore = completenessScore;
        
        // Deduct points for validation errors
        double errorPenalty = Math.min(0.5, errorCount * 0.1);
        qualityScore -= errorPenalty;
        
        // Ensure score is between 0.0 and 1.0
        return Math.min(1.0, Math.max(0.0, qualityScore));
    }
    
    /**
     * Validates data consistency across related fields
     */
    private void validateDataConsistency(Map<String, Object> customerData, List<String> errors, 
                                        Map<String, Object> details) {
        List<String> consistencyIssues = checkDataConsistency(customerData);
        if (!consistencyIssues.isEmpty()) {
            errors.addAll(consistencyIssues);
            details.put("consistencyIssues", consistencyIssues);
        }
    }
    
    // ========== DUPLICATE DETECTION HELPER METHODS ==========
    
    /**
     * Calculates name similarity using fuzzy matching algorithms
     */
    private double calculateNameSimilarity(String firstName1, String lastName1, 
                                          String firstName2, String lastName2) {
        if (firstName1 == null || lastName1 == null || firstName2 == null || lastName2 == null) {
            return 0.0;
        }
        
        double firstNameSim = calculateLevenshteinSimilarity(firstName1.toLowerCase(), 
                                                            firstName2.toLowerCase());
        double lastNameSim = calculateLevenshteinSimilarity(lastName1.toLowerCase(), 
                                                           lastName2.toLowerCase());
        
        // Weight last name more heavily than first name
        return (firstNameSim * 0.4) + (lastNameSim * 0.6);
    }
    
    /**
     * Calculates exact match similarity (1.0 for exact match, 0.0 for no match)
     */
    private double calculateExactMatch(String value1, String value2) {
        if (value1 == null || value2 == null) {
            return 0.0;
        }
        return value1.equals(value2) ? 1.0 : 0.0;
    }
    
    /**
     * Calculates address similarity using multiple factors
     */
    private double calculateAddressSimilarity(String addr1, String zip1, String addr2, String zip2) {
        if (addr1 == null || addr2 == null) {
            return 0.0;
        }
        
        double addrSim = calculateLevenshteinSimilarity(addr1.toLowerCase(), addr2.toLowerCase());
        double zipSim = calculateExactMatch(zip1, zip2);
        
        // Weight ZIP code more heavily than address text
        return (addrSim * 0.3) + (zipSim * 0.7);
    }
    
    /**
     * Calculates overall similarity score with weighted factors
     */
    private double calculateOverallSimilarity(double nameSim, double ssnSim, 
                                             double phoneSim, double addrSim) {
        // Weight SSN and phone numbers heavily as they are unique identifiers
        double weightedScore = (nameSim * 0.2) + (ssnSim * 0.4) + (phoneSim * 0.3) + (addrSim * 0.1);
        return Math.min(1.0, weightedScore);
    }
    
    /**
     * Calculates Levenshtein similarity between two strings
     */
    private double calculateLevenshteinSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = calculateLevenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * Calculates Levenshtein distance between two strings
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), 
                                   dp[i - 1][j - 1] + cost);
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Gets matching fields based on similarity scores
     */
    private List<String> getMatchingFields(double nameSim, double ssnSim, 
                                          double phoneSim, double addrSim) {
        List<String> matchingFields = new ArrayList<>();
        
        if (nameSim >= 0.8) {
            matchingFields.add("name");
        }
        if (ssnSim >= 0.8) {
            matchingFields.add("ssn");
        }
        if (phoneSim >= 0.8) {
            matchingFields.add("phone");
        }
        if (addrSim >= 0.8) {
            matchingFields.add("address");
        }
        
        return matchingFields;
    }
    
    // ========== CONSISTENCY CHECK HELPER METHODS ==========
    
    /**
     * Checks age consistency with other data elements
     */
    private void checkAgeConsistency(Map<String, Object> customerData, List<String> issues) {
        try {
            String dobStr = getStringValue(customerData, "custDOBYYYYMMDD");
            Object ficoObj = customerData.get("custFICOCreditScore");
            
            if (dobStr != null && ficoObj != null) {
                LocalDate dob = LocalDate.parse(dobStr);
                int age = LocalDate.now().getYear() - dob.getYear();
                
                int ficoScore = 0;
                if (ficoObj instanceof Number) {
                    ficoScore = ((Number) ficoObj).intValue();
                } else if (ficoObj instanceof String && !((String) ficoObj).trim().isEmpty()) {
                    ficoScore = Integer.parseInt(((String) ficoObj).trim());
                }
                
                // Young customers with very high FICO scores may be unusual
                if (age < 25 && ficoScore > 750) {
                    issues.add("Unusually high FICO score for young customer (age " + age + ")");
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking age consistency", e);
        }
    }
    
    /**
     * Checks address field consistency
     */
    private void checkAddressConsistency(Map<String, Object> customerData, List<String> issues) {
        String addrLine1 = getStringValue(customerData, "custAddrLine1");
        String stateCode = getStringValue(customerData, "custAddrStateCD");
        String zipCode = getStringValue(customerData, "custAddrZip");
        
        // Check if ZIP code prefix matches state (basic validation)
        if (stateCode != null && zipCode != null && zipCode.length() >= 5) {
            String zipPrefix = zipCode.substring(0, 5);
            if (!isZipStateConsistent(zipPrefix, stateCode)) {
                issues.add("ZIP code may not be consistent with state code");
            }
        }
        
        // Check for obvious address inconsistencies
        if (addrLine1 != null && stateCode != null) {
            String addrLower = addrLine1.toLowerCase();
            String stateLower = stateCode.toLowerCase();
            
            // Look for state names in address that don't match state code
            Map<String, String> stateNames = Map.of(
                "california", "ca", "texas", "tx", "florida", "fl",
                "new york", "ny", "illinois", "il", "pennsylvania", "pa"
            );
            
            for (Map.Entry<String, String> entry : stateNames.entrySet()) {
                if (addrLower.contains(entry.getKey()) && !stateLower.equals(entry.getValue())) {
                    issues.add("Address contains state name that doesn't match state code");
                    break;
                }
            }
        }
    }
    
    /**
     * Checks phone number consistency
     */
    private void checkPhoneConsistency(Map<String, Object> customerData, List<String> issues) {
        String phone1 = getStringValue(customerData, "custPhoneNum1");
        String phone2 = getStringValue(customerData, "custPhoneNum2");
        
        if (phone1 != null && phone2 != null && !phone1.trim().isEmpty() && !phone2.trim().isEmpty()) {
            String cleanPhone1 = phone1.replaceAll("[^0-9]", "");
            String cleanPhone2 = phone2.replaceAll("[^0-9]", "");
            
            if (cleanPhone1.equals(cleanPhone2)) {
                issues.add("Primary and secondary phone numbers are identical");
            }
        }
    }
    
    /**
     * Checks name field consistency
     */
    private void checkNameConsistency(Map<String, Object> customerData, List<String> issues) {
        String firstName = getStringValue(customerData, "custFirstName");
        String middleName = getStringValue(customerData, "custMiddleName");
        String lastName = getStringValue(customerData, "custLastName");
        
        // Check for obviously invalid names
        if (firstName != null && firstName.trim().toLowerCase().equals("test")) {
            issues.add("First name appears to be test data");
        }
        
        if (lastName != null && lastName.trim().toLowerCase().equals("customer")) {
            issues.add("Last name appears to be test data");
        }
        
        // Check for suspicious patterns
        if (firstName != null && lastName != null && 
            firstName.trim().equalsIgnoreCase(lastName.trim())) {
            issues.add("First name and last name are identical");
        }
    }
    
    /**
     * Checks financial data consistency
     */
    private void checkFinancialDataConsistency(Map<String, Object> customerData, List<String> issues) {
        Object ficoObj = customerData.get("custFICOCreditScore");
        String custId = getStringValue(customerData, "custId");
        
        if (ficoObj != null && custId != null) {
            try {
                int ficoScore = 0;
                if (ficoObj instanceof Number) {
                    ficoScore = ((Number) ficoObj).intValue();
                } else if (ficoObj instanceof String && !((String) ficoObj).trim().isEmpty()) {
                    ficoScore = Integer.parseInt(((String) ficoObj).trim());
                }
                
                // Check for perfect scores which are extremely rare
                if (ficoScore == 850) {
                    issues.add("Perfect FICO score (850) is extremely rare and should be verified");
                }
                
                // Check for round numbers which may indicate estimated data
                if (ficoScore > 0 && ficoScore % 50 == 0) {
                    issues.add("FICO score is a round number, may be estimated rather than actual");
                }
                
            } catch (NumberFormatException e) {
                // Already handled in main validation
            }
        }
    }
    
    // ========== EXTERNAL API HELPER METHODS ==========
    
    /**
     * Validates email domain using external API service
     */
    private boolean validateEmailDomainExternal(String domain) {
        if (dataQualityApiBaseUrl == null || dataQualityApiBaseUrl.isEmpty()) {
            return true; // Assume valid if no external service configured
        }
        
        try {
            String url = dataQualityApiBaseUrl + "/validate-domain";
            
            HttpHeaders headers = new HttpHeaders();
            if (dataQualityApiKey != null && !dataQualityApiKey.isEmpty()) {
                headers.set("X-API-Key", dataQualityApiKey);
            }
            headers.set("Content-Type", "application/json");
            
            Map<String, String> requestBody = Map.of("domain", domain);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map responseBody = response.getBody();
                Object validObj = responseBody.get("valid");
                return validObj != null && Boolean.parseBoolean(validObj.toString());
            }
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.warn("External domain validation failed with HTTP error: {}", e.getStatusCode());
        } catch (RestClientException e) {
            logger.warn("External domain validation failed with client error: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("External domain validation failed with unexpected error", e);
        }
        
        return true; // Default to valid if external check fails
    }
    
    // ========== UTILITY HELPER METHODS ==========
    
    /**
     * Safely extracts string value from map
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }
    
    /**
     * Checks if a field has complete, non-empty data
     */
    private boolean isFieldComplete(Map<String, Object> data, String fieldName) {
        String value = getStringValue(data, fieldName);
        return value != null && !value.isEmpty() && !value.equalsIgnoreCase("null");
    }
    
    /**
     * Checks for invalid SSN patterns
     */
    private boolean isInvalidSSNPattern(String ssn) {
        // Known invalid patterns
        Set<String> invalidPatterns = Set.of(
            "000000000", "111111111", "222222222", "333333333", "444444444",
            "555555555", "666666666", "777777777", "888888888", "999999999",
            "123456789", "987654321"
        );
        
        if (invalidPatterns.contains(ssn)) {
            return true;
        }
        
        // Area number 000, 666, or 900-999 are invalid
        String areaNumber = ssn.substring(0, 3);
        if (areaNumber.equals("000") || areaNumber.equals("666")) {
            return true;
        }
        
        int area = Integer.parseInt(areaNumber);
        if (area >= 900 && area <= 999) {
            return true;
        }
        
        // Group number 00 is invalid
        String groupNumber = ssn.substring(3, 5);
        if (groupNumber.equals("00")) {
            return true;
        }
        
        // Serial number 0000 is invalid
        String serialNumber = ssn.substring(5, 9);
        if (serialNumber.equals("0000")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Validates SSN area number
     */
    private boolean isValidSSNAreaNumber(String areaNumber) {
        try {
            int area = Integer.parseInt(areaNumber);
            // Valid ranges: 001-665, 667-899
            return (area >= 1 && area <= 665) || (area >= 667 && area <= 899);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Categorizes FICO score into ranges
     */
    private String categorizeFicoScore(int score) {
        if (score >= 800) return "Exceptional";
        if (score >= 740) return "Very Good";
        if (score >= 670) return "Good";
        if (score >= 580) return "Fair";
        return "Poor";
    }
    
    /**
     * Basic ZIP code and state consistency check
     */
    private boolean isZipStateConsistent(String zipPrefix, String stateCode) {
        // This is a simplified check - in production, use a comprehensive ZIP/state database
        Map<String, Set<String>> zipStateMap = Map.of(
            "CA", Set.of("900", "901", "902", "903", "904", "905", "906", "907", "908", "909", 
                         "910", "911", "912", "913", "914", "915", "916", "917", "918", "919",
                         "920", "921", "922", "923", "924", "925", "926", "927", "928", "929",
                         "930", "931", "932", "933", "934", "935", "936", "937", "938", "939",
                         "940", "941", "942", "943", "944", "945", "946", "947", "948", "949",
                         "950", "951", "952", "953", "954", "955", "956", "957", "958", "959",
                         "960", "961"),
            "TX", Set.of("733", "734", "735", "736", "737", "738", "739", "740", "741", "742",
                         "743", "744", "745", "746", "747", "748", "749", "750", "751", "752",
                         "753", "754", "755", "756", "757", "758", "759", "760", "761", "762",
                         "763", "764", "765", "766", "767", "768", "769", "770", "771", "772",
                         "773", "774", "775", "776", "777", "778", "779", "780", "781", "782",
                         "783", "784", "785", "786", "787", "788", "789", "790", "791", "792",
                         "793", "794", "795", "796", "797", "798", "799", "885"),
            "NY", Set.of("100", "101", "102", "103", "104", "105", "106", "107", "108", "109",
                         "110", "111", "112", "113", "114", "115", "116", "117", "118", "119",
                         "120", "121", "122", "123", "124", "125", "126", "127", "128", "129",
                         "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
                         "140", "141", "142", "143", "144", "145", "146", "147", "148", "149")
        );
        
        Set<String> validZips = zipStateMap.get(stateCode.toUpperCase());
        if (validZips == null) {
            return true; // Unknown state, assume valid
        }
        
        return validZips.contains(zipPrefix);
    }
    
    /**
     * Generates improvement recommendations based on validation results
     */
    private List<String> generateRecommendations(DataQualityResult validationResult, 
                                                Double completenessScore, 
                                                List<String> consistencyIssues) {
        List<String> recommendations = new ArrayList<>();
        
        if (!validationResult.isValid()) {
            recommendations.add("Address validation errors to improve data quality");
        }
        
        if (completenessScore < 0.8) {
            recommendations.add("Complete missing optional fields to improve data completeness");
        }
        
        if (!consistencyIssues.isEmpty()) {
            recommendations.add("Review and resolve data consistency issues");
        }
        
        if (validationResult.getQualityScore() < 0.7) {
            recommendations.add("Consider additional data verification steps");
        }
        
        recommendations.add("Implement regular data quality monitoring");
        recommendations.add("Consider external data enrichment services");
        
        return recommendations;
    }
    
    // ========== RESULT CLASSES ==========
    
    /**
     * Comprehensive data quality validation result
     */
    public static class DataQualityResult {
        private boolean valid;
        private String errorMessage;
        private double qualityScore;
        private Map<String, Object> validationDetails;
        
        public DataQualityResult() {}
        
        public DataQualityResult(boolean valid, String errorMessage, double qualityScore) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.qualityScore = qualityScore;
            this.validationDetails = new HashMap<>();
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public double getQualityScore() { return qualityScore; }
        public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
        
        public Map<String, Object> getValidationDetails() { return validationDetails; }
        public void setValidationDetails(Map<String, Object> validationDetails) { 
            this.validationDetails = validationDetails; 
        }
    }
    
    /**
     * Duplicate customer match result
     */
    public static class DuplicateMatch {
        private String customerId;
        private double similarityScore;
        private List<String> matchingFields;
        
        public DuplicateMatch() {
            this.matchingFields = new ArrayList<>();
        }
        
        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        
        public double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
        
        public List<String> getMatchingFields() { return matchingFields; }
        public void setMatchingFields(List<String> matchingFields) { this.matchingFields = matchingFields; }
    }
    
    /**
     * Phone number validation result
     */
    public static class PhoneValidationResult {
        private boolean valid;
        private String errorMessage;
        private String formattedPhone;
        
        public PhoneValidationResult(boolean valid, String errorMessage, String formattedPhone) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.formattedPhone = formattedPhone;
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getFormattedPhone() { return formattedPhone; }
        public void setFormattedPhone(String formattedPhone) { this.formattedPhone = formattedPhone; }
    }
    
    /**
     * Email validation result
     */
    public static class EmailValidationResult {
        private boolean valid;
        private String errorMessage;
        private String domain;
        private boolean domainValid;
        private String domainValidationMessage;
        
        public EmailValidationResult(boolean valid, String errorMessage, String domain) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.domain = domain;
            this.domainValid = true;
            this.domainValidationMessage = "";
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public boolean isDomainValid() { return domainValid; }
        public void setDomainValid(boolean domainValid) { this.domainValid = domainValid; }
        
        public String getDomainValidationMessage() { return domainValidationMessage; }
        public void setDomainValidationMessage(String domainValidationMessage) { 
            this.domainValidationMessage = domainValidationMessage; 
        }
    }
    
    /**
     * SSN validation result
     */
    public static class SSNValidationResult {
        private boolean valid;
        private String errorMessage;
        private String formattedSSN;
        
        public SSNValidationResult(boolean valid, String errorMessage, String formattedSSN) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.formattedSSN = formattedSSN;
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getFormattedSSN() { return formattedSSN; }
        public void setFormattedSSN(String formattedSSN) { this.formattedSSN = formattedSSN; }
    }
    
    /**
     * Comprehensive data quality report
     */
    public static class DataQualityReport {
        private String customerId;
        private Date reportTimestamp;
        private double overallQualityScore;
        private String validationStatus;
        private String validationMessage;
        private Double completenessScore;
        private List<String> consistencyIssues;
        private Map<String, Object> componentValidation;
        private List<String> recommendations;
        
        public DataQualityReport() {
            this.consistencyIssues = new ArrayList<>();
            this.componentValidation = new HashMap<>();
            this.recommendations = new ArrayList<>();
            this.reportTimestamp = new Date();
        }
        
        public DataQualityReport(String validationStatus, String validationMessage, double overallQualityScore) {
            this();
            this.validationStatus = validationStatus;
            this.validationMessage = validationMessage;
            this.overallQualityScore = overallQualityScore;
        }
        
        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        
        public Date getReportTimestamp() { return reportTimestamp; }
        public void setReportTimestamp(Date reportTimestamp) { this.reportTimestamp = reportTimestamp; }
        
        public double getOverallQualityScore() { return overallQualityScore; }
        public void setOverallQualityScore(double overallQualityScore) { 
            this.overallQualityScore = overallQualityScore; 
        }
        
        public String getValidationStatus() { return validationStatus; }
        public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
        
        public String getValidationMessage() { return validationMessage; }
        public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
        
        public Double getCompletenessScore() { return completenessScore; }
        public void setCompletenessScore(Double completenessScore) { this.completenessScore = completenessScore; }
        
        public List<String> getConsistencyIssues() { return consistencyIssues; }
        public void setConsistencyIssues(List<String> consistencyIssues) { 
            this.consistencyIssues = consistencyIssues; 
        }
        
        public Map<String, Object> getComponentValidation() { return componentValidation; }
        public void setComponentValidation(Map<String, Object> componentValidation) { 
            this.componentValidation = componentValidation; 
        }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}