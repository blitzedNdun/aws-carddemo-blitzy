/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.util;

import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.enums.ErrorFlag;
import com.carddemo.common.enums.PhoneAreaCode;
import com.carddemo.common.enums.AccountStatus;
import com.carddemo.common.enums.CardStatus;
import com.carddemo.common.validator.ValidationConstants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ValidationUtils provides comprehensive field validation utilities maintaining identical behavior
 * to original COBOL PICTURE clause validations and 88-level conditions.
 * 
 * This class converts COBOL validation patterns from CSUTLDPY.cpy, CSUTLDWY.cpy, and various
 * COBOL programs to Java validation methods with exact functional equivalence. All validation
 * rules preserve the original COBOL behavior including error messages, validation sequences,
 * and business logic patterns.
 * 
 * Key Features:
 * - Exact COBOL PICTURE clause validation replication for all field types
 * - 88-level condition validation patterns implemented as Java enum validation predicates
 * - COBOL date validation procedures converted to Java LocalDate validation
 * - Luhn algorithm implementation for credit card number validation
 * - Comprehensive field validation supporting React form validation and Spring Boot error responses
 * - Financial amount validation using BigDecimal with exact COBOL COMP-3 precision
 * 
 * Technical Compliance:
 * - Maintains identical validation behavior to original COBOL field validation logic
 * - Supports Jakarta Bean Validation integration for Spring Boot REST API validation
 * - Provides validation result types compatible with React Hook Form error handling
 * - Implements COBOL validation message patterns for consistent user experience
 * 
 * Based on COBOL validation patterns from:
 * - CSUTLDPY.cpy: Date validation procedures with century, month, day, and leap year validation
 * - CSUTLDWY.cpy: Date validation working storage with 88-level conditions
 * - CSLKPCDY.cpy: Phone area code, state code, and state/ZIP combination validation
 * - Various COBOL programs: Field validation patterns for accounts, cards, and transactions
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public final class ValidationUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);
    
    /**
     * Date formatter for COBOL CCYYMMDD format validation
     * Based on CSUTLDPY.cpy date validation procedures
     */
    private static final DateTimeFormatter COBOL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * Pattern for COBOL numeric fields - equivalent to PIC 9(n)
     */
    private static final Pattern NUMERIC_ONLY_PATTERN = Pattern.compile("^[0-9]+$");
    
    /**
     * Pattern for COBOL alphabetic fields - equivalent to PIC A(n)
     */
    private static final Pattern ALPHABETIC_ONLY_PATTERN = Pattern.compile("^[A-Za-z]+$");
    
    /**
     * Pattern for COBOL alphanumeric fields - equivalent to PIC X(n)
     */
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[A-Za-z0-9\\s]+$");
    
    /**
     * Pattern for email validation
     */
    private static final Pattern EMAIL_VALIDATION_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    /**
     * Private constructor to prevent instantiation.
     * This utility class contains only static methods and should not be instantiated.
     */
    private ValidationUtils() {
        throw new UnsupportedOperationException("ValidationUtils is a utility class and cannot be instantiated");
    }
    
    /**
     * Validates account number format and structure.
     * 
     * Implements COBOL account ID validation equivalent to ACCOUNT-ID PIC 9(11) validation.
     * Validates that the account number is exactly 11 digits and falls within the valid
     * account ID range as defined in the original COBOL business rules.
     * 
     * @param accountNumber the account number to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateAccountNumber(String accountNumber) {
        logger.debug("Validating account number: {}", accountNumber);
        
        if (StringUtils.isBlank(accountNumber)) {
            logger.warn("Account number validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanAccountNumber = StringUtils.trim(accountNumber);
        
        // Check format - must be exactly 11 digits
        if (!ValidationConstants.ACCOUNT_ID_PATTERN.matcher(cleanAccountNumber).matches()) {
            logger.warn("Account number validation failed: invalid format - {}", cleanAccountNumber);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Check range - must be within valid account ID range
        try {
            long accountId = Long.parseLong(cleanAccountNumber);
            if (accountId < ValidationConstants.MIN_ACCOUNT_ID || accountId > ValidationConstants.MAX_ACCOUNT_ID) {
                logger.warn("Account number validation failed: out of range - {}", accountId);
                return ValidationResult.INVALID_RANGE;
            }
        } catch (NumberFormatException e) {
            logger.error("Account number validation failed: number format exception - {}", cleanAccountNumber, e);
            return ValidationResult.INVALID_FORMAT;
        }
        
        logger.debug("Account number validation successful: {}", cleanAccountNumber);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates credit limit amount using exact COBOL COMP-3 precision.
     * 
     * Implements COBOL credit limit validation equivalent to CREDIT-LIMIT PIC S9(7)V99 COMP-3
     * validation. Uses BigDecimal with MathContext.DECIMAL128 for exact precision matching
     * COBOL packed decimal arithmetic.
     * 
     * @param creditLimit the credit limit amount to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateCreditLimit(BigDecimal creditLimit) {
        logger.debug("Validating credit limit: {}", creditLimit);
        
        if (creditLimit == null) {
            logger.warn("Credit limit validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }
        
        // Validate monetary amount format
        if (!BigDecimalUtils.isValidMonetaryAmount(creditLimit)) {
            logger.warn("Credit limit validation failed: invalid monetary amount - {}", creditLimit);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Check business rule range
        BigDecimal minLimit = BigDecimalUtils.createDecimal(ValidationConstants.MIN_CREDIT_LIMIT);
        BigDecimal maxLimit = BigDecimalUtils.createDecimal(ValidationConstants.MAX_CREDIT_LIMIT);
        
        if (BigDecimalUtils.compare(creditLimit, minLimit) < 0 || 
            BigDecimalUtils.compare(creditLimit, maxLimit) > 0) {
            logger.warn("Credit limit validation failed: out of range - {}", creditLimit);
            return ValidationResult.INVALID_RANGE;
        }
        
        logger.debug("Credit limit validation successful: {}", creditLimit);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates account balance using exact COBOL COMP-3 precision.
     * 
     * Implements COBOL balance validation equivalent to ACCT-CURR-BAL PIC S9(10)V99 COMP-3
     * validation. Uses BigDecimal with MathContext.DECIMAL128 for exact precision matching
     * COBOL packed decimal arithmetic.
     * 
     * @param balance the account balance to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateBalance(BigDecimal balance) {
        logger.debug("Validating balance: {}", balance);
        
        if (balance == null) {
            logger.warn("Balance validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }
        
        // Validate monetary amount format
        if (!BigDecimalUtils.isValidMonetaryAmount(balance)) {
            logger.warn("Balance validation failed: invalid monetary amount - {}", balance);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Balance can be negative, so only check upper bound
        BigDecimal maxBalance = BigDecimalUtils.createDecimal(9999999999.99);
        if (BigDecimalUtils.compare(balance.abs(), maxBalance) > 0) {
            logger.warn("Balance validation failed: exceeds maximum absolute value - {}", balance);
            return ValidationResult.INVALID_RANGE;
        }
        
        logger.debug("Balance validation successful: {}", balance);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates required field is not blank.
     * 
     * Implements COBOL required field validation equivalent to checking for LOW-VALUES
     * or SPACES as implemented in CSUTLDPY.cpy date validation procedures.
     * 
     * @param fieldValue the field value to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateRequiredField(String fieldValue) {
        logger.debug("Validating required field: {}", fieldValue);
        
        if (StringUtils.isBlank(fieldValue)) {
            logger.warn("Required field validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        logger.debug("Required field validation successful");
        return ValidationResult.VALID;
    }
    
    /**
     * Validates alphabetic field format.
     * 
     * Implements COBOL alphabetic field validation equivalent to PIC A(n) validation.
     * Ensures field contains only alphabetic characters (A-Z, a-z).
     * 
     * @param fieldValue the field value to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateAlphaField(String fieldValue) {
        logger.debug("Validating alpha field: {}", fieldValue);
        
        if (StringUtils.isBlank(fieldValue)) {
            logger.warn("Alpha field validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanValue = StringUtils.trim(fieldValue);
        
        if (!ValidationConstants.ALPHA_PATTERN.matcher(cleanValue).matches()) {
            logger.warn("Alpha field validation failed: invalid format - {}", cleanValue);
            return ValidationResult.INVALID_FORMAT;
        }
        
        logger.debug("Alpha field validation successful: {}", cleanValue);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates numeric field format.
     * 
     * Implements COBOL numeric field validation equivalent to PIC 9(n) validation.
     * Ensures field contains only numeric characters (0-9).
     * 
     * @param fieldValue the field value to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateNumericField(String fieldValue) {
        logger.debug("Validating numeric field: {}", fieldValue);
        
        if (StringUtils.isBlank(fieldValue)) {
            logger.warn("Numeric field validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanValue = StringUtils.trim(fieldValue);
        
        if (!ValidationConstants.NUMERIC_PATTERN.matcher(cleanValue).matches()) {
            logger.warn("Numeric field validation failed: invalid format - {}", cleanValue);
            return ValidationResult.INVALID_FORMAT;
        }
        
        logger.debug("Numeric field validation successful: {}", cleanValue);
        return ValidationResult.VALID;
    }

    /**
     * Validates that a field contains only numeric characters and has the expected length.
     * Combines numeric validation with length validation for convenience.
     * 
     * @param fieldValue the field value to validate
     * @param expectedLength the expected length of the field
     * @return true if field is numeric and has the expected length, false otherwise
     */
    public static boolean validateNumericField(String fieldValue, int expectedLength) {
        logger.debug("Validating numeric field with length: {} (expected: {})", fieldValue, expectedLength);
        
        if (StringUtils.isBlank(fieldValue)) {
            logger.warn("Numeric field with length validation failed: blank field");
            return false;
        }
        
        String cleanValue = StringUtils.trim(fieldValue);
        
        // Check length first
        if (cleanValue.length() != expectedLength) {
            logger.warn("Numeric field length validation failed: expected {}, got {}", expectedLength, cleanValue.length());
            return false;
        }
        
        // Check if field contains only digits
        if (!ValidationConstants.NUMERIC_PATTERN.matcher(cleanValue).matches()) {
            logger.warn("Numeric field validation failed: non-numeric characters found");
            return false;
        }
        
        logger.debug("Numeric field validation with length passed");
        return true;
    }
    
    /**
     * Validates date field format and business rules.
     * 
     * Implements COBOL date validation equivalent to EDIT-DATE-CCYYMMDD procedures
     * from CSUTLDPY.cpy. Validates century, month, day, and leap year according to
     * original COBOL business rules.
     * 
     * @param dateValue the date value to validate (CCYYMMDD format)
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateDateField(String dateValue) {
        logger.debug("Validating date field: {}", dateValue);
        
        if (StringUtils.isBlank(dateValue)) {
            logger.warn("Date field validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanDate = StringUtils.trim(dateValue);
        
        // Check basic format - must be 8 digits
        if (cleanDate.length() != 8 || !NUMERIC_ONLY_PATTERN.matcher(cleanDate).matches()) {
            logger.warn("Date field validation failed: invalid format - {}", cleanDate);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Extract date components
        try {
            int century = Integer.parseInt(cleanDate.substring(0, 2));
            int year = Integer.parseInt(cleanDate.substring(2, 4));
            int month = Integer.parseInt(cleanDate.substring(4, 6));
            int day = Integer.parseInt(cleanDate.substring(6, 8));
            int fullYear = (century * 100) + year;
            
            // Validate century - only 19 and 20 are valid (from CSUTLDWY.cpy)
            if (!ValidationConstants.VALID_CENTURIES.contains(century)) {
                logger.warn("Date field validation failed: invalid century - {}", century);
                return ValidationResult.INVALID_ERA;
            }
            
            // Validate month
            if (!ValidationConstants.VALID_MONTHS.contains(month)) {
                logger.warn("Date field validation failed: invalid month - {}", month);
                return ValidationResult.INVALID_MONTH;
            }
            
            // Validate day for month and year
            if (!ValidationConstants.isValidDayForMonth(day, month, fullYear)) {
                logger.warn("Date field validation failed: invalid day for month/year - {}/{}/{}", day, month, fullYear);
                return ValidationResult.INVALID_DATE;
            }
            
            // Additional validation using LocalDate
            LocalDate.parse(cleanDate, COBOL_DATE_FORMATTER);
            
        } catch (DateTimeParseException | NumberFormatException e) {
            logger.error("Date field validation failed: parse exception - {}", cleanDate, e);
            return ValidationResult.INVALID_DATE;
        }
        
        logger.debug("Date field validation successful: {}", cleanDate);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates phone number format and area code.
     * 
     * Implements COBOL phone number validation equivalent to CUST-PHONE-NUM PIC X(15)
     * validation with area code cross-reference validation against VALID-PHONE-AREA-CODE
     * 88-level conditions from CSLKPCDY.cpy.
     * 
     * @param phoneNumber the phone number to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validatePhoneNumber(String phoneNumber) {
        logger.debug("Validating phone number: {}", phoneNumber);
        
        if (StringUtils.isBlank(phoneNumber)) {
            logger.warn("Phone number validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanPhone = StringUtils.trim(phoneNumber);
        
        // Check basic format
        if (!ValidationConstants.PHONE_PATTERN.matcher(cleanPhone).matches()) {
            logger.warn("Phone number validation failed: invalid format - {}", cleanPhone);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Extract area code from various formats
        String areaCode;
        if (cleanPhone.length() == 10) {
            // 1234567890 format
            areaCode = cleanPhone.substring(0, 3);
        } else if (cleanPhone.contains("-")) {
            // 123-456-7890 format
            areaCode = cleanPhone.substring(0, 3);
        } else if (cleanPhone.contains("(")) {
            // (123) 456-7890 format
            areaCode = cleanPhone.substring(1, 4);
        } else {
            logger.warn("Phone number validation failed: unrecognized format - {}", cleanPhone);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Validate area code using PhoneAreaCode enum
        if (!PhoneAreaCode.isValid(areaCode)) {
            logger.warn("Phone number validation failed: invalid area code - {}", areaCode);
            return ValidationResult.INVALID_CROSS_REFERENCE;
        }
        
        logger.debug("Phone number validation successful: {}", cleanPhone);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates Social Security Number format.
     * 
     * Implements COBOL SSN validation equivalent to CUST-SSN PIC 9(09) validation.
     * Validates that SSN is exactly 9 digits without any formatting characters.
     * 
     * @param ssn the Social Security Number to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateSSN(String ssn) {
        logger.debug("Validating SSN: {}", ssn);
        
        if (StringUtils.isBlank(ssn)) {
            logger.warn("SSN validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanSSN = StringUtils.trim(ssn);
        
        // Check format - must be exactly 9 digits
        if (!ValidationConstants.SSN_PATTERN.matcher(cleanSSN).matches()) {
            logger.warn("SSN validation failed: invalid format - {}", cleanSSN);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Additional business rule validation
        if (cleanSSN.equals("000000000") || cleanSSN.equals("123456789")) {
            logger.warn("SSN validation failed: invalid SSN pattern - {}", cleanSSN);
            return ValidationResult.BUSINESS_RULE_VIOLATION;
        }
        
        logger.debug("SSN validation successful");
        return ValidationResult.VALID;
    }
    
    /**
     * Validates credit card number format and checksum.
     * 
     * Implements credit card validation including format validation and Luhn algorithm
     * checksum validation. Validates that card number is exactly 16 digits and passes
     * the Luhn checksum test for credit card number validity.
     * 
     * @param cardNumber the credit card number to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateCardNumber(String cardNumber) {
        logger.debug("Validating card number: {}", cardNumber);
        
        if (StringUtils.isBlank(cardNumber)) {
            logger.warn("Card number validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanCardNumber = StringUtils.trim(cardNumber);
        
        // Check format - must be exactly 16 digits
        if (!ValidationConstants.CARD_NUMBER_PATTERN.matcher(cleanCardNumber).matches()) {
            logger.warn("Card number validation failed: invalid format - {}", cleanCardNumber);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Validate using Luhn algorithm
        if (!validateLuhnChecksum(cleanCardNumber)) {
            logger.warn("Card number validation failed: Luhn checksum failed - {}", cleanCardNumber);
            return ValidationResult.INVALID_FORMAT;
        }
        
        logger.debug("Card number validation successful");
        return ValidationResult.VALID;
    }
    
    /**
     * Validates ZIP code format and state combination.
     * 
     * Implements COBOL ZIP code validation equivalent to CUST-ADDR-ZIP PIC X(10)
     * validation. Supports both 5-digit and 9-digit (ZIP+4) formats.
     * 
     * @param zipCode the ZIP code to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateZipCode(String zipCode) {
        logger.debug("Validating ZIP code: {}", zipCode);
        
        if (StringUtils.isBlank(zipCode)) {
            logger.warn("ZIP code validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanZipCode = StringUtils.trim(zipCode);
        
        // Check format - must be 5 digits or 5 digits + 4 digits
        if (!ValidationConstants.ZIP_CODE_PATTERN.matcher(cleanZipCode).matches()) {
            logger.warn("ZIP code validation failed: invalid format - {}", cleanZipCode);
            return ValidationResult.INVALID_FORMAT;
        }
        
        logger.debug("ZIP code validation successful: {}", cleanZipCode);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates state code format and cross-reference.
     * 
     * Implements COBOL state code validation equivalent to CUST-ADDR-STATE-CD PIC X(02)
     * validation with cross-reference validation against VALID-US-STATE-CODE 88-level
     * conditions from CSLKPCDY.cpy.
     * 
     * @param stateCode the state code to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateStateCode(String stateCode) {
        logger.debug("Validating state code: {}", stateCode);
        
        if (StringUtils.isBlank(stateCode)) {
            logger.warn("State code validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanStateCode = StringUtils.trim(stateCode).toUpperCase();
        
        // Check format - must be exactly 2 characters
        if (cleanStateCode.length() != ValidationConstants.STATE_CODE_LENGTH) {
            logger.warn("State code validation failed: invalid length - {}", cleanStateCode);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Validate against valid state codes
        if (!ValidationConstants.isValidStateCode(cleanStateCode)) {
            logger.warn("State code validation failed: invalid state code - {}", cleanStateCode);
            return ValidationResult.INVALID_CROSS_REFERENCE;
        }
        
        logger.debug("State code validation successful: {}", cleanStateCode);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates currency amount format and range.
     * 
     * Implements COBOL currency validation using BigDecimal with exact COBOL COMP-3
     * precision. Validates monetary amounts for financial calculations with proper
     * scale and precision matching original COBOL validation.
     * 
     * @param amount the currency amount to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateCurrency(BigDecimal amount) {
        logger.debug("Validating currency amount: {}", amount);
        
        if (amount == null) {
            logger.warn("Currency validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }
        
        // Validate monetary amount format
        if (!BigDecimalUtils.isValidMonetaryAmount(amount)) {
            logger.warn("Currency validation failed: invalid monetary amount - {}", amount);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Check scale - must have exactly 2 decimal places
        if (amount.scale() > BigDecimalUtils.MONETARY_SCALE) {
            logger.warn("Currency validation failed: invalid scale - {}", amount);
            return ValidationResult.INVALID_FORMAT;
        }
        
        logger.debug("Currency validation successful: {}", amount);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates FICO credit score range.
     * 
     * Implements COBOL FICO score validation equivalent to CUST-FICO-CREDIT-SCORE PIC 9(03)
     * validation. Validates that score is within valid FICO range (300-850).
     * 
     * @param ficoScore the FICO credit score to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateFicoScore(Integer ficoScore) {
        logger.debug("Validating FICO score: {}", ficoScore);
        
        if (ficoScore == null) {
            logger.warn("FICO score validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }
        
        // Check range - must be between 300 and 850
        if (ficoScore < ValidationConstants.MIN_FICO_SCORE || ficoScore > ValidationConstants.MAX_FICO_SCORE) {
            logger.warn("FICO score validation failed: out of range - {}", ficoScore);
            return ValidationResult.INVALID_RANGE;
        }
        
        logger.debug("FICO score validation successful: {}", ficoScore);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates FICO credit score range (alias for validateFicoScore).
     * 
     * Implements COBOL FICO score validation equivalent to CUST-FICO-CREDIT-SCORE PIC 9(03)
     * validation. Validates that score is within valid FICO range (300-850).
     * 
     * @param ficoCreditScore the FICO credit score to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateFicoCreditScore(Integer ficoCreditScore) {
        return validateFicoScore(ficoCreditScore);
    }
    
    /**
     * Validates customer ID format and structure.
     * 
     * Implements COBOL customer ID validation equivalent to CUST-ID PIC 9(09) validation.
     * Validates that the customer ID is exactly 9 digits and falls within the valid
     * customer ID range as defined in the original COBOL business rules.
     * 
     * @param customerId the customer ID to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateCustomerId(String customerId) {
        logger.debug("Validating customer ID: {}", customerId);
        
        if (StringUtils.isBlank(customerId)) {
            logger.warn("Customer ID validation failed: blank field");
            return ValidationResult.BLANK_FIELD;
        }
        
        String cleanCustomerId = StringUtils.trim(customerId);
        
        // Check format - must be exactly 9 digits
        if (cleanCustomerId.length() != 9 || !NUMERIC_ONLY_PATTERN.matcher(cleanCustomerId).matches()) {
            logger.warn("Customer ID validation failed: invalid format - {}", cleanCustomerId);
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Check range - must be within valid customer ID range (1-999999999)
        try {
            long custId = Long.parseLong(cleanCustomerId);
            if (custId < 1 || custId > 999999999) {
                logger.warn("Customer ID validation failed: out of range - {}", custId);
                return ValidationResult.INVALID_RANGE;
            }
        } catch (NumberFormatException e) {
            logger.error("Customer ID validation failed: number format exception - {}", cleanCustomerId, e);
            return ValidationResult.INVALID_FORMAT;
        }
        
        logger.debug("Customer ID validation successful: {}", cleanCustomerId);
        return ValidationResult.VALID;
    }
    
    /**
     * Validates credit card number using Luhn algorithm.
     * 
     * Implements the Luhn algorithm (mod 10) for credit card number validation.
     * This is the standard algorithm used by credit card companies to validate
     * card numbers and detect simple errors in credit card numbers.
     * 
     * @param cardNumber the credit card number to validate (digits only)
     * @return true if the card number passes Luhn validation, false otherwise
     */
    public static boolean validateLuhnChecksum(String cardNumber) {
        logger.debug("Validating Luhn checksum for card number");
        
        if (StringUtils.isBlank(cardNumber)) {
            logger.warn("Luhn validation failed: blank card number");
            return false;
        }
        
        String cleanCardNumber = StringUtils.trim(cardNumber);
        
        // Check if all characters are digits
        if (!NUMERIC_ONLY_PATTERN.matcher(cleanCardNumber).matches()) {
            logger.warn("Luhn validation failed: non-numeric characters");
            return false;
        }
        
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left
        for (int i = cleanCardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cleanCardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        boolean isValid = (sum % 10 == 0);
        logger.debug("Luhn validation result: {}", isValid);
        return isValid;
    }
    
    /**
     * Validates date format according to supported patterns.
     * 
     * Validates date strings against multiple supported formats including COBOL
     * CCYYMMDD format, ISO format, and US format. Returns true if the date can
     * be parsed using any of the supported formats.
     * 
     * @param dateValue the date value to validate
     * @return true if the date format is valid, false otherwise
     */
    public static boolean isValidDateFormat(String dateValue) {
        logger.debug("Validating date format: {}", dateValue);
        
        if (StringUtils.isBlank(dateValue)) {
            logger.warn("Date format validation failed: blank value");
            return false;
        }
        
        String cleanDate = StringUtils.trim(dateValue);
        
        // Try each supported date pattern
        for (String pattern : ValidationConstants.DATE_PATTERNS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDate.parse(cleanDate, formatter);
                logger.debug("Date format validation successful with pattern: {}", pattern);
                return true;
            } catch (DateTimeParseException e) {
                // Continue to next pattern
            }
        }
        
        logger.warn("Date format validation failed: no matching pattern");
        return false;
    }
    
    /**
     * Validates email format according to RFC standards.
     * 
     * Implements email validation using a regex pattern that checks for basic
     * email format compliance. Validates local part, @ symbol, and domain part
     * according to standard email formatting rules.
     * 
     * @param email the email address to validate
     * @return true if the email format is valid, false otherwise
     */
    public static boolean isValidEmailFormat(String email) {
        logger.debug("Validating email format: {}", email);
        
        if (StringUtils.isBlank(email)) {
            logger.warn("Email format validation failed: blank value");
            return false;
        }
        
        String cleanEmail = StringUtils.trim(email);
        
        boolean isValid = EMAIL_VALIDATION_PATTERN.matcher(cleanEmail).matches();
        logger.debug("Email format validation result: {}", isValid);
        return isValid;
    }
    
    /**
     * Validates field length against maximum allowed length.
     * 
     * Implements COBOL field length validation equivalent to PIC X(n) field length
     * validation. Checks that the field value does not exceed the maximum allowed
     * length for the field type.
     * 
     * @param fieldValue the field value to validate
     * @param maxLength the maximum allowed length
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateFieldLength(String fieldValue, int maxLength) {
        logger.debug("Validating field length: {} (max: {})", fieldValue, maxLength);
        
        if (fieldValue == null) {
            logger.debug("Field length validation successful: null value");
            return ValidationResult.VALID;
        }
        
        if (fieldValue.length() > maxLength) {
            logger.warn("Field length validation failed: exceeds maximum length - {} > {}", 
                fieldValue.length(), maxLength);
            return ValidationResult.INVALID_RANGE;
        }
        
        logger.debug("Field length validation successful");
        return ValidationResult.VALID;
    }
    
    /**
     * Validates numeric value against range constraints.
     * 
     * Implements COBOL numeric range validation for fields with minimum and maximum
     * value constraints. Validates that the numeric value falls within the specified
     * range boundaries.
     * 
     * @param value the numeric value to validate
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateNumericRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        logger.debug("Validating numeric range: {} (min: {}, max: {})", value, min, max);
        
        if (value == null) {
            logger.warn("Numeric range validation failed: null value");
            return ValidationResult.BLANK_FIELD;
        }
        
        if (min != null && BigDecimalUtils.compare(value, min) < 0) {
            logger.warn("Numeric range validation failed: below minimum - {} < {}", value, min);
            return ValidationResult.INVALID_RANGE;
        }
        
        if (max != null && BigDecimalUtils.compare(value, max) > 0) {
            logger.warn("Numeric range validation failed: above maximum - {} > {}", value, max);
            return ValidationResult.INVALID_RANGE;
        }
        
        logger.debug("Numeric range validation successful");
        return ValidationResult.VALID;
    }
    
    /**
     * Sanitizes input by removing potentially harmful characters.
     * 
     * Implements input sanitization to prevent injection attacks and ensure
     * data integrity. Removes or escapes potentially dangerous characters while
     * preserving legitimate data content.
     * 
     * @param input the input string to sanitize
     * @return sanitized string with harmful characters removed
     */
    public static String sanitizeInput(String input) {
        logger.debug("Sanitizing input");
        
        if (StringUtils.isBlank(input)) {
            logger.debug("Input sanitization: blank input");
            return "";
        }
        
        // Remove potential script tags and SQL injection patterns
        String sanitized = input
            .replaceAll("(?i)<script[^>]*>.*?</script>", "")  // Remove script tags
            .replaceAll("(?i)<[^>]*>", "")                     // Remove HTML tags
            .replaceAll("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)", "") // Remove SQL keywords
            .replaceAll("['\";\\\\]", "")                      // Remove quote characters
            .trim();
        
        logger.debug("Input sanitization completed");
        return sanitized;
    }
}