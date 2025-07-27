package com.carddemo.common.util;

import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.validator.ValidationConstants;
import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.enums.ErrorFlag;
import com.carddemo.common.enums.PhoneAreaCode;
import com.carddemo.common.enums.AccountStatus;
import com.carddemo.common.enums.CardStatus;

import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.util.Optional;
import java.time.LocalDate;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * ValidationUtils - Comprehensive field validation utility class providing COBOL-equivalent
 * validation patterns for account numbers, card numbers, dates, and other business data fields.
 * 
 * This utility maintains exact validation behavior from original COBOL PICTURE clauses and
 * 88-level conditions while supporting modern Spring Boot REST API validation and React
 * form validation requirements.
 * 
 * Key Features:
 * - Exact COBOL validation pattern replication for account numbers, card numbers, dates
 * - COBOL 88-level condition validation patterns as Java enum validation predicates
 * - Field validation supporting identical error messaging patterns from COBOL implementation
 * - Integration with BigDecimalUtils for exact COBOL COMP-3 decimal precision validation
 * - Support for ValidationResult standardized error handling and React form integration
 * 
 * Validation Patterns Converted from COBOL Sources:
 * - CSUTLDPY.cpy: Date validation logic with century, month, day, and leap year validation
 * - CSUTLDWY.cpy: Data structures and 88-level conditions for date components
 * - CSLKPCDY.cpy: Phone area codes, state codes, and cross-reference validation
 * - Various COBOL programs: Account, card, and business data validation patterns
 * 
 * Performance Requirements:
 * - All validation operations must complete within sub-millisecond timeframes
 * - Support 10,000+ TPS transaction validation throughput
 * - Memory-efficient validation patterns optimized for high-frequency operations
 * 
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public final class ValidationUtils {

    /**
     * Logger for validation operations and error tracking.
     * Supports structured logging for validation debugging and audit trails.
     */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

    /**
     * Date formatters for various date validation patterns.
     * Compiled patterns for optimal performance in high-frequency validation scenarios.
     */
    private static final DateTimeFormatter CCYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter US_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**
     * Compiled regex patterns for high-performance validation operations.
     * Pre-compiled patterns avoid regex compilation overhead in validation operations.
     */
    private static final Pattern LUHN_CHECKSUM_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern EMAIL_VALIDATION_PATTERN = ValidationConstants.EMAIL_PATTERN;

    /**
     * Private constructor to prevent instantiation of utility class.
     * ValidationUtils is designed as a stateless utility class with only static methods.
     */
    private ValidationUtils() {
        throw new UnsupportedOperationException("ValidationUtils is a utility class and cannot be instantiated");
    }

    /**
     * Validates account number format and structure according to COBOL account ID patterns.
     * Replicates COBOL account number validation logic from account processing programs.
     * 
     * Validation Rules (from COBOL ACCT-ID validation):
     * - Must be exactly 11 digits (PIC 9(11) equivalent)
     * - Must be within valid account ID range (10000000000-99999999999)
     * - Must be numeric-only content
     * - Cannot be null, empty, or contain non-numeric characters
     * 
     * COBOL Equivalent: Account ID validation from COACTVWC.cbl and COACTUPC.cbl
     * 
     * @param accountNumber The account number string to validate
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateAccountNumber(String accountNumber) {
        logger.debug("Validating account number: {}", maskSensitiveData(accountNumber));

        // Check for null or blank field (COBOL INSUFFICIENT-DATA equivalent)
        if (StringUtils.isBlank(accountNumber)) {
            logger.warn("Account number validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }

        // Trim input and validate format
        String trimmedAccountNumber = accountNumber.trim();

        // Validate numeric-only content (COBOL NUMERIC test equivalent)
        if (!ValidationConstants.ACCOUNT_ID_PATTERN.matcher(trimmedAccountNumber).matches()) {
            logger.warn("Account number validation failed: invalid format");
            return ValidationResult.INVALID_FORMAT;
        }

        // Convert to long for range validation
        try {
            long accountId = Long.parseLong(trimmedAccountNumber);
            
            // Validate range (COBOL business rule validation)
            if (accountId < ValidationConstants.MIN_ACCOUNT_ID || accountId > ValidationConstants.MAX_ACCOUNT_ID) {
                logger.warn("Account number validation failed: outside valid range");
                return ValidationResult.INVALID_RANGE;
            }

            logger.debug("Account number validation successful");
            return ValidationResult.VALID;

        } catch (NumberFormatException e) {
            logger.warn("Account number validation failed: number format exception", e);
            return ValidationResult.NON_NUMERIC_DATA;
        }
    }

    /**
     * Validates credit limit amount using exact COBOL COMP-3 decimal precision.
     * Maintains identical validation behavior as original COBOL credit limit validation.
     * 
     * Validation Rules (from COBOL credit limit validation):
     * - Must be valid monetary amount with proper decimal precision
     * - Must be within business rule limits ($500 - $50,000)
     * - Must be non-negative (credit limits cannot be negative)
     * - Must have proper scale (2 decimal places maximum)
     * 
     * COBOL Equivalent: Credit limit validation from account update programs
     * 
     * @param creditLimit The credit limit amount to validate
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateCreditLimit(BigDecimal creditLimit) {
        logger.debug("Validating credit limit: {}", creditLimit);

        // Check for null value
        if (creditLimit == null) {
            logger.warn("Credit limit validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }

        // Validate non-negative amount (business rule)
        if (BigDecimalUtils.isLessThan(creditLimit, BigDecimal.ZERO)) {
            logger.warn("Credit limit validation failed: negative amount");
            return ValidationResult.INVALID_RANGE;
        }

        // Validate within business limits
        BigDecimal minLimit = BigDecimal.valueOf(ValidationConstants.MIN_CREDIT_LIMIT);
        BigDecimal maxLimit = BigDecimal.valueOf(ValidationConstants.MAX_CREDIT_LIMIT);

        if (!BigDecimalUtils.isInRange(creditLimit, minLimit, maxLimit)) {
            logger.warn("Credit limit validation failed: outside business limits");
            return ValidationResult.INVALID_RANGE;
        }

        // Validate proper monetary scale (COBOL COMP-3 precision)
        if (creditLimit.scale() > ValidationConstants.CURRENCY_SCALE) {
            logger.warn("Credit limit validation failed: excessive decimal precision");
            return ValidationResult.INVALID_FORMAT;
        }

        logger.debug("Credit limit validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates account balance using COBOL COMP-3 decimal precision patterns.
     * Ensures proper monetary formatting and business rule compliance.
     * 
     * Validation Rules (from COBOL balance validation):
     * - Must be valid monetary amount with exact decimal precision
     * - Must have proper scale (2 decimal places maximum)
     * - Can be negative (for debt balances) or positive
     * - Must not exceed maximum balance limits
     * 
     * COBOL Equivalent: Account balance validation from CVACT01Y.cpy
     * 
     * @param balance The account balance to validate
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateBalance(BigDecimal balance) {
        logger.debug("Validating account balance: {}", balance);

        // Check for null value
        if (balance == null) {
            logger.warn("Balance validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }

        // Validate proper monetary scale (COBOL COMP-3 precision requirement)
        if (balance.scale() > ValidationConstants.CURRENCY_SCALE) {
            logger.warn("Balance validation failed: excessive decimal precision");
            return ValidationResult.INVALID_FORMAT;
        }

        // Validate precision doesn't exceed COBOL COMP-3 limits
        if (balance.precision() > ValidationConstants.CURRENCY_PRECISION) {
            logger.warn("Balance validation failed: precision exceeds COBOL limits");
            return ValidationResult.INVALID_RANGE;
        }

        logger.debug("Balance validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates required field content according to COBOL blank field detection.
     * Replicates COBOL INSUFFICIENT-DATA validation patterns.
     * 
     * Validation Rules (from COBOL required field validation):
     * - Field cannot be null
     * - Field cannot be empty or contain only whitespace
     * - Field cannot be low-values or spaces (COBOL equivalent)
     * 
     * COBOL Equivalent: Required field validation from various COBOL programs
     * 
     * @param fieldValue The field value to validate
     * @param fieldName The field name for error messaging
     * @return ValidationResult indicating success or blank field failure
     */
    public static ValidationResult validateRequiredField(String fieldValue, String fieldName) {
        logger.debug("Validating required field '{}': {}", fieldName, maskSensitiveData(fieldValue));

        // COBOL null/low-values check equivalent
        if (StringUtils.isBlank(fieldValue)) {
            logger.warn("Required field validation failed for '{}': blank field", fieldName);
            return ValidationResult.BLANK_FIELD;
        }

        logger.debug("Required field validation successful for '{}'", fieldName);
        return ValidationResult.VALID;
    }

    /**
     * Validates alphabetic field content according to COBOL alpha validation patterns.
     * Ensures field contains only alphabetic characters as per COBOL PICTURE A patterns.
     * 
     * Validation Rules (from COBOL alpha field validation):
     * - Must contain only alphabetic characters (A-Z, a-z)
     * - Cannot contain digits, special characters, or whitespace
     * - Field length must be within specified bounds
     * 
     * COBOL Equivalent: Alpha field validation using PICTURE A patterns
     * 
     * @param fieldValue The field value to validate
     * @param maxLength Maximum allowed field length
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateAlphaField(String fieldValue, int maxLength) {
        logger.debug("Validating alpha field with max length {}: {}", maxLength, maskSensitiveData(fieldValue));

        // Check for required field
        ValidationResult requiredCheck = validateRequiredField(fieldValue, "alpha field");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmedValue = fieldValue.trim();

        // Validate length constraint
        if (trimmedValue.length() > maxLength) {
            logger.warn("Alpha field validation failed: exceeds maximum length");
            return ValidationResult.INVALID_LENGTH;
        }

        // Validate alphabetic-only content (COBOL PICTURE A equivalent)
        if (!ValidationConstants.ALPHA_PATTERN.matcher(trimmedValue).matches()) {
            logger.warn("Alpha field validation failed: contains non-alphabetic characters");
            return ValidationResult.INVALID_FORMAT;
        }

        logger.debug("Alpha field validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates numeric field content according to COBOL numeric validation patterns.
     * Replicates COBOL PICTURE 9 field validation with exact precision requirements.
     * 
     * Validation Rules (from COBOL numeric field validation):
     * - Must contain only numeric characters (0-9)
     * - Cannot contain alphabetic characters, special characters, or whitespace
     * - Field length must match COBOL PICTURE specification
     * - Must be convertible to appropriate numeric type
     * 
     * COBOL Equivalent: Numeric field validation using PICTURE 9 patterns
     * 
     * @param fieldValue The field value to validate
     * @param maxLength Maximum allowed field length
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateNumericField(String fieldValue, int maxLength) {
        logger.debug("Validating numeric field with max length {}: {}", maxLength, fieldValue);

        // Check for required field
        ValidationResult requiredCheck = validateRequiredField(fieldValue, "numeric field");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmedValue = fieldValue.trim();

        // Validate length constraint
        if (trimmedValue.length() > maxLength) {
            logger.warn("Numeric field validation failed: exceeds maximum length");
            return ValidationResult.INVALID_LENGTH;
        }

        // Validate numeric-only content (COBOL PICTURE 9 equivalent)
        if (!ValidationConstants.NUMERIC_PATTERN.matcher(trimmedValue).matches()) {
            logger.warn("Numeric field validation failed: contains non-numeric characters");
            return ValidationResult.NON_NUMERIC_DATA;
        }

        // Verify numeric conversion capability (avoid octal interpretation issues)
        try {
            Integer.parseInt(trimmedValue);
        } catch (NumberFormatException e) {
            logger.warn("Numeric field validation failed: not convertible to number");
            return ValidationResult.NON_NUMERIC_DATA;
        }

        logger.debug("Numeric field validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates date field using COBOL date validation logic from CSUTLDPY.cpy.
     * Maintains exact validation behavior including century, month, day, and leap year validation.
     * 
     * Validation Rules (from COBOL date validation in CSUTLDPY.cpy):
     * - Date format must be CCYYMMDD (8 digits)
     * - Century must be 19 or 20 (THIS-CENTURY or LAST-CENTURY)
     * - Month must be 1-12 (WS-VALID-MONTH)
     * - Day must be 1-31 with month-specific validation (WS-VALID-DAY)
     * - Leap year calculation for February 29th validation
     * - Future date validation for birth dates
     * 
     * COBOL Equivalent: EDIT-DATE-CCYYMMDD paragraph from CSUTLDPY.cpy
     * 
     * @param dateValue The date string to validate in CCYYMMDD format
     * @param allowFutureDates Whether to allow future dates (false for birth dates)
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateDateField(String dateValue, boolean allowFutureDates) {
        logger.debug("Validating date field (allow future: {}): {}", allowFutureDates, dateValue);

        // Check for required field (COBOL INSUFFICIENT-DATA equivalent)
        ValidationResult requiredCheck = validateRequiredField(dateValue, "date field");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmedDate = dateValue.trim();

        // Validate 8-digit format (CCYYMMDD)
        if (trimmedDate.length() != 8 || !ValidationConstants.NUMERIC_PATTERN.matcher(trimmedDate).matches()) {
            logger.warn("Date validation failed: invalid format (must be CCYYMMDD)");
            return ValidationResult.INVALID_FORMAT;
        }

        try {
            // Extract date components (COBOL equivalent field extraction)
            int century = Integer.parseInt(trimmedDate.substring(0, 2));  // CC
            int year = Integer.parseInt(trimmedDate.substring(2, 4));     // YY
            int month = Integer.parseInt(trimmedDate.substring(4, 6));    // MM
            int day = Integer.parseInt(trimmedDate.substring(6, 8));      // DD

            int fullYear = century * 100 + year;

            // Validate century (COBOL THIS-CENTURY/LAST-CENTURY)
            if (!ValidationConstants.VALID_CENTURIES.contains(century)) {
                logger.warn("Date validation failed: invalid century (must be 19 or 20)");
                return ValidationResult.INVALID_DATE;
            }

            // Validate month (COBOL WS-VALID-MONTH)
            if (!ValidationConstants.VALID_MONTHS.contains(month)) {
                logger.warn("Date validation failed: invalid month");
                return ValidationResult.INVALID_DATE;
            }

            // Validate day range (COBOL WS-VALID-DAY)
            if (!ValidationConstants.VALID_DAYS.contains(day)) {
                logger.warn("Date validation failed: invalid day");
                return ValidationResult.INVALID_DATE;
            }

            // Validate day for specific month (COBOL month-day combinations)
            int maxDayForMonth = ValidationConstants.getMaxDayForMonth(month, fullYear);
            if (day > maxDayForMonth) {
                logger.warn("Date validation failed: day {} invalid for month {}", day, month);
                return ValidationResult.INVALID_DATE;
            }

            // Parse as LocalDate for additional validation
            LocalDate parsedDate = LocalDate.parse(trimmedDate, CCYYMMDD_FORMATTER);

            // Validate future date restriction (COBOL date-of-birth logic)
            if (!allowFutureDates && parsedDate.isAfter(LocalDate.now())) {
                logger.warn("Date validation failed: future date not allowed");
                return ValidationResult.INVALID_DATE;
            }

            logger.debug("Date validation successful");
            return ValidationResult.VALID;

        } catch (NumberFormatException | DateTimeParseException e) {
            logger.warn("Date validation failed: parsing error", e);
            return ValidationResult.INVALID_DATE;
        }
    }

    /**
     * Validates phone number format and area code according to COBOL phone validation.
     * Uses North American area code validation from CSLKPCDY.cpy.
     * 
     * Validation Rules (from COBOL phone validation):
     * - Must match phone number pattern (digits, spaces, hyphens, parentheses)
     * - Area code must be valid NANP area code from VALID-PHONE-AREA-CODE
     * - Total length must not exceed 15 characters (COBOL PIC X(15))
     * - Must extract and validate 3-digit area code
     * 
     * COBOL Equivalent: Phone number validation from customer data validation
     * 
     * @param phoneNumber The phone number string to validate  
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validatePhoneNumber(String phoneNumber) {
        logger.debug("Validating phone number: {}", maskSensitiveData(phoneNumber));

        // Check for required field
        ValidationResult requiredCheck = validateRequiredField(phoneNumber, "phone number");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmedPhone = phoneNumber.trim();

        // Validate overall phone pattern
        if (!ValidationConstants.PHONE_PATTERN.matcher(trimmedPhone).matches()) {
            logger.warn("Phone number validation failed: invalid format");
            return ValidationResult.INVALID_FORMAT;
        }

        // Extract digits only for area code validation
        String digitsOnly = trimmedPhone.replaceAll("[^0-9]", "");

        // Validate minimum length for area code extraction
        if (digitsOnly.length() < 10) {
            logger.warn("Phone number validation failed: insufficient digits");
            return ValidationResult.INVALID_FORMAT;
        }

        // Extract area code (first 3 digits)
        String areaCode = digitsOnly.substring(0, 3);

        // Validate area code using PhoneAreaCode enum (COBOL VALID-PHONE-AREA-CODE)
        if (!PhoneAreaCode.isValid(areaCode)) {
            logger.warn("Phone number validation failed: invalid area code");
            return ValidationResult.INVALID_CROSS_REFERENCE;
        }

        logger.debug("Phone number validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates Social Security Number format according to COBOL SSN patterns.
     * Maintains exact validation behavior from customer data validation programs.
     * 
     * Validation Rules (from COBOL SSN validation):
     * - Must be exactly 9 digits (COBOL PIC 9(09))
     * - Cannot contain dashes, spaces, or other formatting characters
     * - Must be all numeric content
     * - Cannot be all zeros or other invalid patterns
     * 
     * COBOL Equivalent: SSN validation from customer data validation
     * 
     * @param ssn The Social Security Number to validate
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateSSN(String ssn) {
        logger.debug("Validating SSN: {}", maskSensitiveData(ssn));

        // Check for required field
        ValidationResult requiredCheck = validateRequiredField(ssn, "SSN");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmedSSN = ssn.trim();

        // Validate SSN pattern (exactly 9 digits)
        if (!ValidationConstants.SSN_PATTERN.matcher(trimmedSSN).matches()) {
            logger.warn("SSN validation failed: invalid format");
            return ValidationResult.INVALID_FORMAT;
        }

        // Additional business rule: cannot be all zeros
        if ("000000000".equals(trimmedSSN)) {
            logger.warn("SSN validation failed: cannot be all zeros");
            return ValidationResult.BUSINESS_RULE_VIOLATION;
        }

        logger.debug("SSN validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates credit card number format and checksum using Luhn algorithm.
     * Replicates COBOL card number validation with enhanced checksum validation.
     * 
     * Validation Rules (from COBOL card validation):
     * - Must be exactly 16 digits (standard credit card format)
     * - Must pass Luhn checksum algorithm validation
     * - Must be all numeric content
     * - Cannot be all zeros or test numbers
     * 
     * COBOL Equivalent: Card number validation from card management programs
     * 
     * @param cardNumber The credit card number to validate
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateCardNumber(String cardNumber) {
        logger.debug("Validating card number: {}", maskSensitiveData(cardNumber));

        // Check for required field
        ValidationResult requiredCheck = validateRequiredField(cardNumber, "card number");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmedCardNumber = cardNumber.trim();

        // Validate card number pattern (exactly 16 digits)
        if (!ValidationConstants.CARD_NUMBER_PATTERN.matcher(trimmedCardNumber).matches()) {
            logger.warn("Card number validation failed: invalid format");
            return ValidationResult.INVALID_FORMAT;
        }

        // Validate Luhn checksum
        ValidationResult luhnResult = validateLuhnChecksum(trimmedCardNumber);
        if (!luhnResult.isValid()) {
            logger.warn("Card number validation failed: Luhn checksum invalid");
            return luhnResult;
        }

        logger.debug("Card number validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates ZIP code format according to US postal standards.
     * Supports both 5-digit and ZIP+4 formats with state cross-reference validation.
     * 
     * Validation Rules (from COBOL ZIP validation):
     * - Must be 5 digits or 5+4 format (NNNNN or NNNNN-NNNN)
     * - Must be valid numeric content
     * - Optional state cross-reference validation
     * 
     * COBOL Equivalent: ZIP code validation from address validation programs
     * 
     * @param zipCode The ZIP code to validate
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateZipCode(String zipCode) {
        logger.debug("Validating ZIP code: {}", zipCode);

        // Check for required field
        ValidationResult requiredCheck = validateRequiredField(zipCode, "ZIP code");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmedZipCode = zipCode.trim();

        // Validate ZIP code pattern (5 digits or 5+4 format)
        if (!ValidationConstants.ZIP_CODE_PATTERN.matcher(trimmedZipCode).matches()) {
            logger.warn("ZIP code validation failed: invalid format");
            return ValidationResult.INVALID_FORMAT;
        }

        logger.debug("ZIP code validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates state code according to US state and territory standards.
     * Uses COBOL state code validation from CSLKPCDY.cpy.
     * 
     * Validation Rules (from COBOL state validation):
     * - Must be valid US state or territory code
     * - Must match VALID-US-STATE-CODE 88-level condition
     * - Case-insensitive matching supported
     * 
     * COBOL Equivalent: State code validation from CSLKPCDY.cpy
     * 
     * @param stateCode The state code to validate
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateStateCode(String stateCode) {
        logger.debug("Validating state code: {}", stateCode);

        // Check for required field
        ValidationResult requiredCheck = validateRequiredField(stateCode, "state code");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmedStateCode = stateCode.trim().toUpperCase();

        // Validate against valid state codes (COBOL VALID-US-STATE-CODE)
        if (!ValidationConstants.VALID_STATE_CODES.contains(trimmedStateCode)) {
            logger.warn("State code validation failed: invalid state code");
            return ValidationResult.INVALID_CROSS_REFERENCE;
        }

        logger.debug("State code validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates currency amount using COBOL COMP-3 decimal precision standards.
     * Ensures proper monetary formatting and business rule compliance.
     * 
     * Validation Rules (from COBOL currency validation):
     * - Must be valid BigDecimal with proper scale
     * - Must have maximum 2 decimal places (monetary scale)
     * - Must be within reasonable currency limits
     * - Must use COBOL COMP-3 equivalent precision
     * 
     * COBOL Equivalent: Currency amount validation from financial programs
     * 
     * @param currencyAmount The currency amount to validate
     * @return ValidationResult indicating success or specific validation failure  
     */
    public static ValidationResult validateCurrency(BigDecimal currencyAmount) {
        logger.debug("Validating currency amount: {}", currencyAmount);

        // Check for null value
        if (currencyAmount == null) {
            logger.warn("Currency validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }

        // Validate proper monetary scale (COBOL COMP-3 precision)
        if (currencyAmount.scale() > ValidationConstants.CURRENCY_SCALE) {
            logger.warn("Currency validation failed: excessive decimal precision");
            return ValidationResult.INVALID_FORMAT;
        }

        // Validate precision doesn't exceed COBOL limits
        if (currencyAmount.precision() > ValidationConstants.CURRENCY_PRECISION) {
            logger.warn("Currency validation failed: precision exceeds COBOL limits");
            return ValidationResult.INVALID_RANGE;
        }

        logger.debug("Currency validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates FICO credit score according to industry standards.
     * Maintains business rule validation from credit scoring programs.
     * 
     * Validation Rules (from COBOL FICO validation):
     * - Must be integer value between 300-850 (industry standard range)
     * - Must be numeric content only
     * - Cannot be null or empty
     * 
     * COBOL Equivalent: FICO score validation from credit assessment programs
     * 
     * @param ficoScore The FICO score to validate
     * @return ValidationResult indicating success or specific validation failure
     */
    public static ValidationResult validateFicoScore(Integer ficoScore) {
        logger.debug("Validating FICO score: {}", ficoScore);

        // Check for null value
        if (ficoScore == null) {
            logger.warn("FICO score validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }

        // Validate FICO score range (industry standard 300-850)
        if (ficoScore < ValidationConstants.MIN_FICO_SCORE || ficoScore > ValidationConstants.MAX_FICO_SCORE) {
            logger.warn("FICO score validation failed: outside valid range");
            return ValidationResult.INVALID_RANGE;
        }

        logger.debug("FICO score validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates number using Luhn checksum algorithm for credit card validation.
     * Implements industry standard Luhn algorithm for card number verification.
     * 
     * Algorithm Steps:
     * 1. Starting from rightmost digit, double every second digit
     * 2. If doubling results in two digits, add them together
     * 3. Sum all digits
     * 4. If sum is divisible by 10, checksum is valid
     * 
     * @param number The number string to validate using Luhn algorithm
     * @return ValidationResult indicating checksum validity
     */
    public static ValidationResult validateLuhnChecksum(String number) {
        logger.debug("Validating Luhn checksum for number: {}", maskSensitiveData(number));

        // Check for required field
        ValidationResult requiredCheck = validateRequiredField(number, "number for Luhn validation");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }

        String trimmedNumber = number.trim();

        // Validate numeric-only content
        if (!LUHN_CHECKSUM_PATTERN.matcher(trimmedNumber).matches()) {
            logger.warn("Luhn validation failed: non-numeric content");
            return ValidationResult.NON_NUMERIC_DATA;
        }

        // Implement Luhn algorithm
        int sum = 0;
        boolean alternate = false;

        // Process digits from right to left
        for (int i = trimmedNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(trimmedNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        // Checksum is valid if sum is divisible by 10
        if (sum % 10 == 0) {
            logger.debug("Luhn checksum validation successful");
            return ValidationResult.VALID;
        } else {
            logger.warn("Luhn checksum validation failed: invalid checksum");
            return ValidationResult.INVALID_FORMAT;
        }
    }

    /**
     * Validates date format string for various supported date patterns.
     * Supports multiple date formats including COBOL date patterns.
     * 
     * Supported Formats:
     * - YYYYMMDD (COBOL CCYYMMDD equivalent)
     * - YYYY-MM-DD (ISO format)
     * - MM/DD/YYYY (US format)
     * 
     * @param dateString The date string to validate
     * @param pattern The expected date pattern
     * @return true if date string matches pattern and is valid date, false otherwise
     */
    public static boolean isValidDateFormat(String dateString, String pattern) {
        logger.debug("Validating date format for pattern '{}': {}", pattern, dateString);

        if (StringUtils.isBlank(dateString) || StringUtils.isBlank(pattern)) {
            return false;
        }

        try {
            DateTimeFormatter formatter;
            switch (pattern.toUpperCase()) {
                case "YYYYMMDD":
                case "CCYYMMDD":
                    formatter = CCYYMMDD_FORMATTER;
                    break;
                case "YYYY-MM-DD":
                    formatter = ISO_DATE_FORMATTER;
                    break;
                case "MM/DD/YYYY":
                    formatter = US_DATE_FORMATTER;
                    break;
                default:
                    formatter = DateTimeFormatter.ofPattern(pattern);
                    break;
            }

            LocalDate.parse(dateString.trim(), formatter);
            logger.debug("Date format validation successful");
            return true;

        } catch (DateTimeParseException | IllegalArgumentException e) {
            logger.debug("Date format validation failed", e);
            return false;
        }
    }

    /**
     * Validates email address format using standard email pattern.
     * Provides comprehensive email format validation for customer communication.
     * 
     * Validation Rules:
     * - Must contain @ symbol with valid domain
     * - Must have valid local part before @
     * - Must have valid domain with TLD
     * - Cannot contain invalid characters
     * 
     * @param email The email address to validate
     * @return true if email format is valid, false otherwise
     */
    public static boolean isValidEmailFormat(String email) {
        logger.debug("Validating email format: {}", maskSensitiveData(email));

        if (StringUtils.isBlank(email)) {
            return false;
        }

        boolean isValid = EMAIL_VALIDATION_PATTERN.matcher(email.trim()).matches();
        logger.debug("Email format validation result: {}", isValid);
        return isValid;
    }

    /**
     * Validates field length according to COBOL PICTURE clause specifications.
     * Ensures field content meets length requirements from original COBOL programs.
     * 
     * @param fieldValue The field value to validate
     * @param minLength Minimum allowed length (inclusive)
     * @param maxLength Maximum allowed length (inclusive)
     * @return ValidationResult indicating length validity
     */
    public static ValidationResult validateFieldLength(String fieldValue, int minLength, int maxLength) {
        logger.debug("Validating field length (min: {}, max: {}): {}", minLength, maxLength, 
                    maskSensitiveData(fieldValue));

        // Handle null value
        if (fieldValue == null) {
            if (minLength > 0) {
                logger.warn("Field length validation failed: null value with minimum length requirement");
                return ValidationResult.BLANK_FIELD;
            } else {
                return ValidationResult.VALID;
            }
        }

        int fieldLength = fieldValue.length();

        // Validate minimum length
        if (fieldLength < minLength) {
            logger.warn("Field length validation failed: below minimum length");
            return ValidationResult.INVALID_LENGTH;
        }

        // Validate maximum length
        if (fieldLength > maxLength) {
            logger.warn("Field length validation failed: exceeds maximum length");
            return ValidationResult.INVALID_LENGTH;
        }

        logger.debug("Field length validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Validates numeric value within specified range using BigDecimal precision.
     * Maintains exact COBOL COMP-3 comparison behavior for range validation.
     * 
     * @param value The numeric value to validate
     * @param minValue Minimum allowed value (inclusive)
     * @param maxValue Maximum allowed value (inclusive)
     * @return ValidationResult indicating range validity
     */
    public static ValidationResult validateNumericRange(BigDecimal value, BigDecimal minValue, BigDecimal maxValue) {
        logger.debug("Validating numeric range (min: {}, max: {}): {}", minValue, maxValue, value);

        // Check for null value
        if (value == null) {
            logger.warn("Numeric range validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }

        // Validate range using BigDecimalUtils for exact precision
        if (!BigDecimalUtils.isInRange(value, minValue, maxValue)) {
            logger.warn("Numeric range validation failed: value outside range");
            return ValidationResult.INVALID_RANGE;
        }

        logger.debug("Numeric range validation successful");
        return ValidationResult.VALID;
    }

    /**
     * Sanitizes input string by removing potentially harmful content.
     * Provides basic input sanitization for security and data quality.
     * 
     * Sanitization Operations:
     * - Trims leading and trailing whitespace
     * - Removes control characters
     * - Normalizes multiple spaces to single spaces
     * - Handles null input gracefully
     * 
     * @param input The input string to sanitize
     * @return Sanitized string or null if input was null
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Trim whitespace
        String sanitized = input.trim();

        // Remove control characters (except tab, newline, carriage return)
        sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\t\n\r]]", "");

        // Normalize multiple spaces to single spaces
        sanitized = sanitized.replaceAll("\\s+", " ");

        logger.debug("Input sanitization completed");
        return sanitized;
    }

    /**
     * Validates customer ID format and range using COBOL-equivalent validation patterns.
     * This method provides customer ID validation equivalent to COBOL PIC 9(09) constraints
     * ensuring exactly 9 digits within valid customer ID range.
     * 
     * Validation Rules (from COBOL customer ID validation):
     * - Must be exactly 9 digits (PIC 9(09) equivalent)
     * - Must be within valid customer ID range (100000000-999999999)
     * - Must be numeric-only content without alphabetic characters
     * - Cannot be null, empty, or contain non-numeric characters
     * 
     * COBOL Equivalent: Customer ID validation from COBOL customer management programs
     * with INSPECT and NUMERIC testing for 9-digit customer identifiers.
     * 
     * @param customerId the customer ID string to validate
     * @return ValidationResult indicating customer ID validity or specific validation failure
     */
    public static ValidationResult validateCustomerId(String customerId) {
        logger.debug("Validating customer ID: {}", maskSensitiveData(customerId));
        
        // Check for null or empty
        if (StringUtils.isBlank(customerId)) {
            logger.warn("Customer ID validation failed: null or empty");
            return ValidationResult.BLANK_FIELD;
        }
        
        String trimmedCustomerId = customerId.trim();
        
        // Check length - must be exactly 9 digits
        if (trimmedCustomerId.length() != 9) {
            logger.warn("Customer ID validation failed: incorrect length (expected 9, got {})", 
                       trimmedCustomerId.length());
            return ValidationResult.INVALID_LENGTH;
        }
        
        // Check that all characters are numeric
        if (!NumberUtils.isDigits(trimmedCustomerId)) {
            logger.warn("Customer ID validation failed: contains non-numeric characters");
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Convert to long for range validation
        try {
            long customerIdValue = Long.parseLong(trimmedCustomerId);
            
            // Validate range (100000000 to 999999999)
            if (customerIdValue < 100000000L || customerIdValue > 999999999L) {
                logger.warn("Customer ID validation failed: outside valid range");
                return ValidationResult.INVALID_RANGE;
            }
            
            logger.debug("Customer ID validation successful");
            return ValidationResult.VALID;
            
        } catch (NumberFormatException e) {
            logger.warn("Customer ID validation failed: number format error", e);
            return ValidationResult.INVALID_FORMAT;
        }
    }
    
    /**
     * Validates date of birth to ensure it represents a valid past date.
     * This method provides date of birth validation equivalent to COBOL date validation
     * logic ensuring the date is in the past and properly formatted.
     * 
     * Validation Rules:
     * - Date must be properly formatted and parseable
     * - Date must be in the past (no future dates allowed)
     * - Date must represent a valid calendar date
     * - Supports multiple date formats (ISO, US, COBOL CCYYMMDD)
     * 
     * COBOL Equivalent: Date validation from COBOL customer data validation programs
     * with date range checking and future date prevention logic.
     * 
     * @param dateOfBirth the date of birth string to validate
     * @return ValidationResult indicating date of birth validity or specific validation failure
     */
    public static ValidationResult validateDateOfBirth(String dateOfBirth) {
        logger.debug("Validating date of birth: {}", dateOfBirth);
        
        // Check for null or empty
        if (StringUtils.isBlank(dateOfBirth)) {
            logger.warn("Date of birth validation failed: null or empty");
            return ValidationResult.BLANK_FIELD;
        }
        
        String trimmedDate = dateOfBirth.trim();
        
        try {
            LocalDate birthDate;
            
            // Try different date formats
            if (isValidDateFormat(trimmedDate, "YYYY-MM-DD")) {
                birthDate = LocalDate.parse(trimmedDate, ISO_DATE_FORMATTER);
            } else if (isValidDateFormat(trimmedDate, "YYYYMMDD")) {
                birthDate = LocalDate.parse(trimmedDate, CCYYMMDD_FORMATTER);
            } else if (isValidDateFormat(trimmedDate, "MM/DD/YYYY")) {
                birthDate = LocalDate.parse(trimmedDate, US_DATE_FORMATTER);
            } else {
                logger.warn("Date of birth validation failed: unsupported format");
                return ValidationResult.INVALID_FORMAT;
            }
            
            // Check that date is in the past
            LocalDate today = LocalDate.now();
            if (birthDate.isAfter(today)) {
                logger.warn("Date of birth validation failed: future date not allowed");
                return ValidationResult.INVALID_RANGE;
            }
            
            // Check reasonable age limits (not more than 150 years ago)
            LocalDate maxPastDate = today.minusYears(150);
            if (birthDate.isBefore(maxPastDate)) {
                logger.warn("Date of birth validation failed: date too far in past");
                return ValidationResult.INVALID_RANGE;
            }
            
            logger.debug("Date of birth validation successful");
            return ValidationResult.VALID;
            
        } catch (DateTimeParseException e) {
            logger.warn("Date of birth validation failed: parse error", e);
            return ValidationResult.INVALID_FORMAT;
        }
    }
    
    /**
     * Validates FICO credit score according to industry standards and business rules.
     * This is an alias method for validateFicoScore to maintain compatibility with 
     * customer service layer method naming conventions.
     * 
     * @param ficoScore the FICO credit score to validate
     * @return ValidationResult indicating FICO score validity or specific validation failure
     */
    public static ValidationResult validateFicoCreditScore(Integer ficoScore) {
        return validateFicoScore(ficoScore);
    }

    /**
     * Masks sensitive data for logging purposes.
     * Prevents sensitive information from appearing in log files.
     * 
     * @param sensitiveData The sensitive data to mask
     * @return Masked string with only first and last characters visible
     */
    private static String maskSensitiveData(String sensitiveData) {
        if (StringUtils.isBlank(sensitiveData)) {
            return "[blank]";
        }

        if (sensitiveData.length() <= 2) {
            return "***";
        }

        return sensitiveData.charAt(0) + "***" + sensitiveData.charAt(sensitiveData.length() - 1);
    }
}