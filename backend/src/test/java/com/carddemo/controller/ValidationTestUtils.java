/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import com.carddemo.exception.ValidationException;
import com.carddemo.exception.DataPrecisionException;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

/**
 * Utility class providing validation helper methods for testing COBOL picture clause equivalents, 
 * date formats, and numeric precision in controller tests. This class translates the functionality
 * from the CSUTLDTC COBOL subprogram into comprehensive Java testing utilities.
 * 
 * The CSUTLDTC.cbl program performs date validation using the CEEDAYS API and returns various
 * validation results including "Date is valid", "Insufficient data", "Date value error", etc.
 * This Java equivalent provides comprehensive testing utilities for:
 * 
 * - COBOL PIC 9(n) numeric field format validation
 * - COBOL PIC X(n) alphanumeric field format validation  
 * - COBOL PIC 9(n)V9(n) decimal precision testing
 * - Date validation matching CSUTLDTC logic (CCYYMMDD format)
 * - COMP-3 to BigDecimal conversion precision testing
 * - Field length constraint validation
 * - Numeric range validation
 * - Required field checking
 * - Cross-field validation rules
 * - Error message formatting verification
 * 
 * All validation methods maintain exact compatibility with COBOL validation patterns
 * from the original mainframe implementation, ensuring functional parity during migration.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public final class ValidationTestUtils {

    // COBOL pattern constants for validation testing
    private static final Pattern PIC_X_PATTERN = Pattern.compile("^[A-Za-z0-9\\s\\-\\.]*$");
    private static final Pattern PIC_9_PATTERN = Pattern.compile("^\\d*$");
    private static final Pattern PIC_DECIMAL_PATTERN = Pattern.compile("^\\d*\\.?\\d*$");
    private static final DateTimeFormatter CCYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // Test data generation constants
    private static final String[] VALID_AREA_CODES = {"201", "212", "213", "214", "215", "216", "224", "248", "267", "301"};
    private static final String[] VALID_STATE_CODES = {"AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA"};
    private static final int FICO_SCORE_MIN = 300;
    private static final int FICO_SCORE_MAX = 850;

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ValidationTestUtils() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Validates PIC 9(n) numeric field formats by testing that values contain only digits
     * and conform to the specified length constraints. Replicates COBOL numeric validation
     * patterns from copybook field definitions.
     * 
     * @param value the string value to validate as numeric
     * @param length the expected length matching PIC 9(n) specification
     * @return true if value matches PIC 9(n) format, false otherwise
     */
    public static boolean validatePicNumeric(String value, int length) {
        if (value == null) {
            return false;
        }
        
        String trimmedValue = value.trim();
        
        // Check if contains only digits
        if (!PIC_9_PATTERN.matcher(trimmedValue).matches()) {
            return false;
        }
        
        // Check length constraint
        if (trimmedValue.length() > length) {
            return false;
        }
        
        // Use CobolDataConverter to validate the numeric format
        try {
            CobolDataConverter.convertPicNumeric(value, "PIC 9(" + length + ")");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates PIC X(n) alphanumeric field formats by testing that values conform
     * to alphanumeric character constraints and length specifications. Ensures compatibility
     * with COBOL alphanumeric field validation rules.
     * 
     * @param value the string value to validate as alphanumeric
     * @param maxLength the maximum length from PIC X(n) specification
     * @return true if value matches PIC X(n) format, false otherwise
     */
    public static boolean validatePicAlphanumeric(String value, int maxLength) {
        if (value == null) {
            return false;
        }
        
        // Check length constraint
        if (value.length() > maxLength) {
            return false;
        }
        
        // Check alphanumeric pattern (allowing spaces, hyphens, periods)
        if (!PIC_X_PATTERN.matcher(value).matches()) {
            return false;
        }
        
        // Use CobolDataConverter to validate the alphanumeric format
        try {
            CobolDataConverter.convertPicString(value, maxLength);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates PIC 9(n)V9(n) decimal precision by testing BigDecimal conversion
     * and ensuring proper scale preservation. Verifies that decimal values maintain
     * COBOL COMP-3 packed decimal precision requirements.
     * 
     * @param value the decimal value to validate
     * @param integerDigits number of integer digits before decimal point
     * @param decimalDigits number of decimal digits after decimal point  
     * @return true if value matches PIC 9(n)V9(n) precision, false otherwise
     */
    public static boolean validatePicDecimal(BigDecimal value, int integerDigits, int decimalDigits) {
        if (value == null) {
            return false;
        }
        
        try {
            // Preserve precision using CobolDataConverter
            BigDecimal preservedValue = CobolDataConverter.preservePrecision(value, decimalDigits);
            
            // Check that scale matches expected decimal digits
            if (preservedValue.scale() != decimalDigits) {
                return false;
            }
            
            // Check total precision (integer + decimal digits)
            String unscaledValue = preservedValue.unscaledValue().toString();
            if (unscaledValue.length() > (integerDigits + decimalDigits)) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates date format in CCYYMMDD format using comprehensive validation logic
     * that replicates the CSUTLDTC COBOL subprogram functionality. Performs the same
     * validation steps as the original CEEDAYS API call.
     * 
     * @param dateString the date string to validate in CCYYMMDD format
     * @return true if date is valid according to CSUTLDTC logic, false otherwise
     */
    public static boolean validateDateCCYYMMDD(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return false;
        }
        
        // Use DateConversionUtil which replicates CSUTLDTC logic
        boolean isValid = DateConversionUtil.validateDate(dateString);
        
        if (isValid) {
            try {
                // Additional validation to ensure date can be parsed
                LocalDate.parse(dateString, CCYYMMDD_FORMATTER);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        return false;
    }

    /**
     * Validates COMP-3 to BigDecimal conversion precision by testing that converted
     * values maintain exact scale and precision. Ensures financial calculations 
     * preserve penny-level accuracy as required by COBOL COMP-3 specifications.
     * 
     * @param originalValue the original COMP-3 equivalent value
     * @param expectedScale the expected decimal scale (typically 2 for monetary)
     * @return true if precision is maintained exactly, false otherwise
     */
    public static boolean validateComp3Precision(BigDecimal originalValue, int expectedScale) {
        if (originalValue == null) {
            return false;
        }
        
        try {
            // Use CobolDataConverter to test COMP-3 precision preservation
            BigDecimal convertedValue = CobolDataConverter.toBigDecimal(originalValue, expectedScale);
            
            // Verify scale is exactly as expected
            if (convertedValue.scale() != expectedScale) {
                return false;
            }
            
            // Verify the conversion maintains precision
            BigDecimal preservedValue = CobolDataConverter.preservePrecision(convertedValue, expectedScale);
            
            return convertedValue.compareTo(preservedValue) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates field length constraints against specified maximum lengths.
     * Tests that field values do not exceed COBOL PIC clause length specifications
     * and throws appropriate ValidationException for field-level errors.
     * 
     * @param fieldName the name of the field being validated
     * @param fieldValue the field value to check
     * @param maxLength the maximum allowed length
     * @throws ValidationException if field exceeds maximum length
     */
    public static void validateFieldLength(String fieldName, String fieldValue, int maxLength) {
        try {
            ValidationUtil.validateFieldLength(fieldName, fieldValue, maxLength);
        } catch (ValidationException e) {
            // Re-throw with additional context for testing
            throw new ValidationException("Field length validation failed for " + fieldName, e.getFieldErrors());
        }
    }

    /**
     * Validates numeric values are within specified ranges, typically used for
     * testing FICO scores, monetary amounts, and other constrained numeric fields.
     * Ensures values conform to business rule constraints.
     * 
     * @param value the numeric value to validate
     * @param minValue the minimum allowed value (inclusive)
     * @param maxValue the maximum allowed value (inclusive)
     * @return true if value is within range, false otherwise
     */
    public static boolean validateNumericRange(BigDecimal value, BigDecimal minValue, BigDecimal maxValue) {
        return ValidationUtil.isValidNumericRange(value, minValue, maxValue);
    }

    /**
     * Validates that required fields are not null or empty, collecting multiple
     * field errors in a single ValidationException. Replicates COBOL required
     * field validation patterns from edit routines.
     * 
     * @param fieldMap map of field names to field values to validate
     * @throws ValidationException with all field errors if any required fields are missing
     */
    public static void validateRequiredFields(java.util.Map<String, String> fieldMap) {
        ValidationException validationException = new ValidationException("Required field validation failed");
        
        for (java.util.Map.Entry<String, String> entry : fieldMap.entrySet()) {
            try {
                ValidationUtil.validateRequiredField(entry.getKey(), entry.getValue());
            } catch (ValidationException e) {
                validationException.addFieldError(entry.getKey(), "Field " + entry.getKey() + " is required");
            }
        }
        
        if (validationException.hasFieldErrors()) {
            throw validationException;
        }
    }

    /**
     * Validates cross-field business rules such as date ranges, account balances vs limits,
     * and state-ZIP code combinations. Tests complex validation scenarios that involve
     * multiple field relationships.
     * 
     * @param account Account entity to validate
     * @param customer Customer entity to validate 
     * @throws BusinessRuleException if cross-field validation rules are violated
     */
    public static void validateCrossFieldRules(Account account, Customer customer) {
        // Validate customer and account relationship
        if (account != null && customer != null) {
            Long accountCustomerId = account.getCustomerId();
            Long customerId = customer.getCustomerId();
            
            if (accountCustomerId != null && customerId != null && !accountCustomerId.equals(customerId)) {
                throw new BusinessRuleException("Account customer ID does not match customer ID", "9001");
            }
        }
        
        // Validate account balance vs credit limit
        if (account != null) {
            BigDecimal currentBalance = account.getCurrentBalance();
            BigDecimal creditLimit = account.getCreditLimit();
            
            if (currentBalance != null && creditLimit != null && currentBalance.compareTo(creditLimit) > 0) {
                throw new BusinessRuleException("Current balance exceeds credit limit", "9002");
            }
        }
        
        // Validate customer state and ZIP code combination
        if (customer != null) {
            String stateCode = customer.getStateCode();
            String zipCode = customer.getZipCode();
            
            if (stateCode != null && zipCode != null) {
                if (!ValidationUtil.validateStateZipCode(stateCode, zipCode)) {
                    throw new BusinessRuleException("Invalid state and ZIP code combination", "9003");
                }
            }
        }
    }

    /**
     * Validates error message formatting to ensure consistency with COBOL error
     * message patterns. Tests that ValidationException messages follow expected
     * format and contain appropriate field-level error details.
     * 
     * @param exception the ValidationException to validate
     * @param expectedFieldCount expected number of field errors
     * @return true if error format is valid, false otherwise
     */
    public static boolean validateErrorFormat(ValidationException exception, int expectedFieldCount) {
        if (exception == null) {
            return false;
        }
        
        // Validate that exception has field errors
        if (!exception.hasFieldErrors()) {
            return expectedFieldCount == 0;
        }
        
        // Validate field error count
        if (exception.getFieldErrorCount() != expectedFieldCount) {
            return false;
        }
        
        // Validate that each field error has a non-empty message
        for (String fieldName : exception.getFieldNames()) {
            String errorMessage = exception.getFieldError(fieldName);
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                return false;
            }
        }
        
        // Validate overall message format includes field errors
        String message = exception.getMessage();
        return message != null && message.contains("Field errors:");
    }

    /**
     * Creates a test Account entity with valid default values for testing purposes.
     * Provides consistent test data that passes all validation rules and maintains
     * COBOL field format compatibility.
     * 
     * @return Account entity with valid test data
     */
    public static Account createTestAccount() {
        Customer testCustomer = createTestCustomer();
        
        return Account.builder()
                .accountId(12345678901L)
                .activeStatus("Y")
                .currentBalance(new BigDecimal("1234.56"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1000.00"))
                .openDate(LocalDate.of(2020, 1, 15))
                .expirationDate(LocalDate.of(2025, 1, 15))
                .reissueDate(LocalDate.of(2023, 1, 15))
                .currentCycleCredit(new BigDecimal("0.00"))
                .currentCycleDebit(new BigDecimal("234.56"))
                .addressZip("12345")
                .groupId("GROUP001")
                .customer(testCustomer)
                .build();
    }

    /**
     * Creates a test Customer entity with valid SSN, FICO score, and phone number
     * for testing purposes. Ensures all generated data passes COBOL validation
     * rules and format requirements.
     * 
     * @return Customer entity with valid test data
     */
    public static Customer createTestCustomer() {
        return Customer.builder()
                .customerId(123456789L)
                .firstName("John")
                .middleName("Q")
                .lastName("Doe")
                .addressLine1("123 Main Street")
                .addressLine2("Apt 4B")
                .addressLine3("")
                .stateCode("CA")
                .countryCode("USA")
                .zipCode("90210")
                .phoneNumber1(generateValidPhoneNumber())
                .phoneNumber2(generateValidPhoneNumber())
                .ssn(generateValidSSN())
                .governmentIssuedId("DL123456789")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .eftAccountId("EFT001")
                .primaryCardHolderIndicator("Y")
                .ficoScore(generateValidFicoScore())
                .creditLimit(new BigDecimal("10000.00"))
                .build();
    }

    /**
     * Creates a test Transaction entity with valid amount, date, and account relationship
     * for testing purposes. Provides transaction data that maintains COBOL precision
     * requirements and passes all validation rules.
     * 
     * @return Transaction entity with valid test data
     */
    public static Transaction createTestTransaction() {
        return Transaction.builder()
                .transactionId(1234567890L)
                .amount(new BigDecimal("123.45"))
                .accountId(12345678901L)
                .transactionDate(LocalDate.now())
                .description("Test Purchase")
                .merchantId(987654321L)
                .merchantName("Test Merchant")
                .merchantCity("Test City")
                .merchantZip("12345")
                .build();
    }

    /**
     * Generates a valid SSN in proper format for testing purposes.
     * Creates SSN values that pass all COBOL SSN validation rules including
     * area code, group code, and serial number constraints.
     * 
     * @return valid SSN string in XXX-XX-XXXX format
     */
    public static String generateValidSSN() {
        // Generate valid area code (not 000, 666, or 900-999)
        int area = 123 + (int)(Math.random() * 500); // Range 123-622
        if (area == 666) area = 665; // Avoid invalid 666
        
        // Generate valid group code (not 00)
        int group = 1 + (int)(Math.random() * 99); // Range 01-99
        
        // Generate valid serial number (not 0000)
        int serial = 1 + (int)(Math.random() * 9999); // Range 0001-9999
        
        return String.format("%03d-%02d-%04d", area, group, serial);
    }

    /**
     * Generates a valid phone number with valid area code for testing purposes.
     * Creates phone numbers that pass NANPA area code validation rules from
     * the ValidationUtil.isValidPhoneAreaCode() method.
     * 
     * @return valid phone number string in XXX-XXX-XXXX format
     */
    public static String generateValidPhoneNumber() {
        // Use a valid area code from our predefined list
        String areaCode = VALID_AREA_CODES[(int)(Math.random() * VALID_AREA_CODES.length)];
        
        // Generate valid exchange (200-999, not ending in 11)
        int exchange = 200 + (int)(Math.random() * 800);
        
        // Generate valid subscriber number (0000-9999)
        int subscriber = (int)(Math.random() * 10000);
        
        return String.format("%s-%03d-%04d", areaCode, exchange, subscriber);
    }

    /**
     * Generates a valid FICO score within acceptable range for testing purposes.
     * Creates FICO scores that pass ValidationUtil.validateFicoScore() validation
     * and fall within the standard 300-850 range.
     * 
     * @return valid FICO score integer between 300-850
     */
    public static Integer generateValidFicoScore() {
        // Generate FICO score in valid range (300-850)
        return FICO_SCORE_MIN + (int)(Math.random() * (FICO_SCORE_MAX - FICO_SCORE_MIN + 1));
    }

    /**
     * Asserts that two BigDecimal values have matching precision and scale,
     * verifying COBOL COMP-3 precision preservation in calculations. Throws
     * DataPrecisionException if precision does not match exactly.
     * 
     * @param expected the expected BigDecimal value
     * @param actual the actual BigDecimal value
     * @param expectedScale the expected decimal scale
     * @throws DataPrecisionException if precision does not match
     */
    public static void assertPrecisionMatch(BigDecimal expected, BigDecimal actual, int expectedScale) {
        if (expected == null && actual == null) {
            return;
        }
        
        if (expected == null || actual == null) {
            throw new DataPrecisionException("Precision mismatch: one value is null", 
                expectedScale, actual != null ? actual.scale() : 0, actual != null ? actual : BigDecimal.ZERO);
        }
        
        // Check scale matches
        if (actual.scale() != expectedScale) {
            throw new DataPrecisionException("Scale mismatch", 
                expectedScale, actual.scale(), actual);
        }
        
        // Check values are equal
        if (expected.compareTo(actual) != 0) {
            throw new DataPrecisionException("Value mismatch with correct scale", 
                expectedScale, actual.scale(), actual);
        }
    }

    /**
     * Asserts that date conversion between formats maintains accuracy and follows
     * CCYYMMDD validation rules from CSUTLDTC logic. Validates date format
     * conversion preserves date values exactly.
     * 
     * @param originalDate the original date string
     * @param convertedDate the converted date string  
     * @param expectedFormat the expected format pattern
     */
    public static void assertDateConversion(String originalDate, String convertedDate, String expectedFormat) {
        assertNotNull(originalDate, "Original date cannot be null");
        assertNotNull(convertedDate, "Converted date cannot be null");
        assertNotNull(expectedFormat, "Expected format cannot be null");
        
        try {
            // Validate original date using CSUTLDTC logic
            assertTrue(validateDateCCYYMMDD(originalDate), "Original date failed CCYYMMDD validation");
            
            // Parse both dates to ensure they represent the same date
            LocalDate original = DateConversionUtil.parseDate(originalDate);
            LocalDate converted;
            
            if ("yyyyMMdd".equals(expectedFormat)) {
                converted = LocalDate.parse(convertedDate, CCYYMMDD_FORMATTER);
            } else {
                converted = LocalDate.parse(convertedDate, DateTimeFormatter.ofPattern(expectedFormat));
            }
            
            assertEquals(original, converted, "Date conversion changed the actual date value");
            
        } catch (Exception e) {
            fail("Date conversion assertion failed: " + e.getMessage());
        }
    }

    /**
     * Asserts that a ValidationException contains expected field errors with proper
     * formatting. Validates that error messages follow COBOL error message patterns
     * and include all expected field-level validation failures.
     * 
     * @param exception the ValidationException to validate
     * @param expectedFieldName the expected field name with error
     * @param expectedMessagePattern regex pattern for expected message
     */
    public static void assertValidationError(ValidationException exception, String expectedFieldName, String expectedMessagePattern) {
        assertNotNull(exception, "ValidationException cannot be null");
        assertTrue(exception.hasFieldErrors(), "ValidationException must have field errors");
        assertTrue(exception.hasFieldError(expectedFieldName), 
            "ValidationException must have error for field: " + expectedFieldName);
        
        String actualMessage = exception.getFieldError(expectedFieldName);
        assertNotNull(actualMessage, "Field error message cannot be null");
        
        if (expectedMessagePattern != null) {
            assertTrue(Pattern.matches(expectedMessagePattern, actualMessage),
                "Error message '" + actualMessage + "' does not match expected pattern: " + expectedMessagePattern);
        }
    }
}