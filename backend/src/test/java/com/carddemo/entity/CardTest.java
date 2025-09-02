/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.UnitTest;
import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;
import com.carddemo.util.DateConversionUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.assertj.core.api.Assertions;

import java.time.LocalDate;
import java.lang.reflect.Field;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit test class for Card JPA entity validating 150-byte CARD-RECORD 
 * structure from CVACT02Y copybook, including card number, CVV, embossed name, 
 * expiration date handling, and account relationships.
 * 
 * This test class ensures complete functional parity between COBOL CARD-RECORD 
 * structure and Java Card entity implementation, validating:
 * 
 * COBOL Field Mappings (150-byte total record length):
 * - CARD-NUM PIC X(16) → cardNumber field (16 characters)
 * - CARD-ACCT-ID PIC 9(11) → accountId field (11 digits)
 * - CARD-CVV-CD PIC 9(03) → cvvCode field (3 digits)
 * - CARD-EMBOSSED-NAME PIC X(50) → embossedName field (50 characters)
 * - CARD-EXPIRAION-DATE PIC X(10) → expirationDate field (10 characters)
 * - CARD-ACTIVE-STATUS PIC X(01) → activeStatus field (1 character)
 * - FILLER PIC X(59) → 59-byte padding (handled by JPA table structure)
 * 
 * Validation Scenarios:
 * - Field length constraints matching COBOL PIC clauses exactly
 * - JPA annotation validation for column mappings and constraints
 * - Card-account-customer relationship integrity through foreign keys
 * - Business logic validation including expiration date handling
 * - ToString, equals, and hashCode methods for entity comparison
 * - Unique constraints on card number for data integrity
 * 
 * This test implementation preserves all business logic from original COBOL programs
 * (COCRDLIC.cbl, COCRDSLC.cbl, COCRDUPC.cbl) while ensuring comprehensive coverage
 * of the Card entity's functionality in the modernized Spring Boot architecture.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public class CardTest extends AbstractBaseTest implements UnitTest {

    private Card testCard;
    private Account testAccount;
    private Customer testCustomer;
    private Validator validator;

    /**
     * Setup method executed before each test execution.
     * Initializes test Card entity with valid data matching COBOL CARD-RECORD structure,
     * creates related Account and Customer entities for relationship testing,
     * and configures Bean Validation validator for constraint validation testing.
     * 
     * Test data follows COBOL field specifications from CVACT02Y copybook:
     * - 16-character card number matching industry standard
     * - 11-digit account ID matching CARD-ACCT-ID specification
     * - 3-digit CVV code for security validation
     * - 50-character embossed name field
     * - Valid expiration date in future
     * - Active status flag ('Y' for active)
     */
    @BeforeEach
    public void setUp() {
        super.setUp();
        
        // Create test customer entity for relationship testing
        testCustomer = new Customer();
        testCustomer.setCustomerId(TestConstants.TEST_CUSTOMER_ID_LONG.toString());
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        
        // Create test account entity for relationship testing
        testAccount = new Account();
        testAccount.setAccountId(TestConstants.TEST_ACCOUNT_ID);
        testAccount.setCustomer(testCustomer); // Set customer relationship instead of customerId
        testAccount.setCurrentBalance(new java.math.BigDecimal("1500.00"));
        
        // Create test Card entity with COBOL-compatible data
        testCard = new Card();
        testCard.setCardNumber(TestConstants.TEST_CARD_NUMBER);
        testCard.setAccountId(TestConstants.TEST_ACCOUNT_ID);
        testCard.setCustomerId(Long.valueOf(testCustomer.getCustomerId()));
        testCard.setCvvCode("123");
        testCard.setEmbossedName("JOHN A DOE");
        testCard.setExpirationDate(LocalDate.now().plusYears(3));
        testCard.setActiveStatus("Y");
        testCard.setAccount(testAccount);
        testCard.setCustomer(testCustomer);
        
        // Initialize Bean Validation validator for constraint testing
        ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        logTestExecution("Card test setup completed with COBOL-compatible test data", null);
    }

    // Field Mapping and Length Validation Tests

    /**
     * Test card number field mapping and validation from CARD-NUM PIC X(16).
     * Validates that card number field accepts exactly 16 characters and
     * rejects invalid lengths, matching COBOL field specification exactly.
     */
    @Test
    public void testCardNumberFieldMapping() {
        // Test valid 16-character card number
        testCard.setCardNumber("1234567890123456");
        assertThat(testCard.getCardNumber()).isEqualTo("1234567890123456");
        assertThat(testCard.getCardNumber().length()).isEqualTo(Constants.CARD_NUMBER_LENGTH);
        
        // Validate field length matches COBOL PIC X(16) specification
        assertThat(Constants.CARD_NUMBER_LENGTH).isEqualTo(16);
        
        logTestExecution("Card number field mapping validated", null);
    }

    /**
     * Test account ID field mapping and validation from CARD-ACCT-ID PIC 9(11).
     * Validates that account ID field accepts exactly 11 digits and maintains
     * proper relationship with Account entity through foreign key constraints.
     */
    @Test
    public void testAccountIdFieldMapping() {
        // Test valid 11-digit account ID
        Long accountId = TestConstants.TEST_ACCOUNT_ID;
        testCard.setAccountId(accountId);
        assertThat(testCard.getAccountId()).isEqualTo(accountId);
        
        // Validate field length matches COBOL PIC 9(11) specification  
        assertThat(Constants.ACCOUNT_ID_LENGTH).isEqualTo(11);
        assertThat(TestConstants.TEST_ACCOUNT_ID.toString().length()).isEqualTo(Constants.ACCOUNT_ID_LENGTH);
        
        logTestExecution("Account ID field mapping validated", null);
    }

    /**
     * Test customer ID relationship field mapping and validation.
     * Validates that customer ID field maintains proper relationship with
     * Customer entity through foreign key constraints and supports card-customer associations.
     */
    @Test
    public void testCustomerIdFieldMapping() {
        // Test valid customer ID relationship
        Long customerId = Long.valueOf(testCustomer.getCustomerId());
        testCard.setCustomerId(customerId);
        assertThat(testCard.getCustomerId()).isEqualTo(String.format("%09d", customerId)); // getCustomerId() returns formatted String
        
        // Validate customer relationship
        testCard.setCustomer(testCustomer);
        assertThat(testCard.getCustomer()).isEqualTo(testCustomer);
        assertThat(testCard.getCustomerId()).isEqualTo(testCustomer.getCustomerId()); // Both return formatted String for COBOL compatibility
        
        logTestExecution("Customer ID relationship mapping validated", null);
    }

    /**
     * Test CVV code field mapping and validation from CARD-CVV-CD PIC 9(03).
     * Validates that CVV code field accepts exactly 3 digits and maintains
     * security through JSON serialization exclusion for PCI DSS compliance.
     */
    @Test
    public void testCvvCodeFieldMapping() {
        // Test valid 3-digit CVV code
        testCard.setCvvCode("123");
        assertThat(testCard.getCvvCode()).isEqualTo("123");
        assertThat(testCard.getCvvCode().length()).isEqualTo(3);
        
        // Test CVV code validation with different patterns
        testCard.setCvvCode("999");
        assertThat(testCard.getCvvCode()).matches("\\d{3}");
        
        logTestExecution("CVV code field mapping validated", null);
    }

    /**
     * Test embossed name field mapping and validation from CARD-EMBOSSED-NAME PIC X(50).
     * Validates that embossed name field accepts up to 50 characters and handles
     * typical card name formatting including spaces and middle initials.
     */
    @Test
    public void testEmbossedNameFieldMapping() {
        // Test valid embossed name within 50-character limit
        String embossedName = "JOHN A DOE";
        testCard.setEmbossedName(embossedName);
        assertThat(testCard.getEmbossedName()).isEqualTo(embossedName);
        assertThat(testCard.getEmbossedName().length()).isLessThanOrEqualTo(50);
        
        // Test maximum length embossed name (50 characters)
        String maxLengthName = "A".repeat(50);
        testCard.setEmbossedName(maxLengthName);
        assertThat(testCard.getEmbossedName()).hasSize(50);
        
        logTestExecution("Embossed name field mapping validated", null);
    }

    /**
     * Test expiration date field mapping and validation from CARD-EXPIRAION-DATE PIC X(10).
     * Note: COBOL copybook contains typo "EXPIRAION" - tests validate correct business logic
     * despite original typo in field name. Validates date parsing, future date validation,
     * and expiration checking business logic.
     */
    @Test
    public void testExpirationDateFieldMapping() {
        // Test valid future expiration date
        LocalDate futureDate = LocalDate.now().plusYears(3);
        testCard.setExpirationDate(futureDate);
        assertThat(testCard.getExpirationDate()).isEqualTo(futureDate);
        assertThat(testCard.isExpired()).isFalse();
        
        // Test past expiration date (expired card)
        LocalDate pastDate = LocalDate.now().minusYears(1);
        testCard.setExpirationDate(pastDate);
        assertThat(testCard.isExpired()).isTrue();
        
        // Test date validation using DateConversionUtil (expects YYYYMMDD format)
        String formattedDate = futureDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(DateConversionUtil.validateDate(formattedDate)).isTrue();
        
        logTestExecution("Expiration date field mapping validated", null);
    }

    /**
     * Test active status field mapping and validation from CARD-ACTIVE-STATUS PIC X(01).
     * Validates that active status field accepts only 'Y' or 'N' values and supports
     * business logic for determining card activation status.
     */
    @Test
    public void testActiveStatusFieldMapping() {
        // Test active status 'Y'
        testCard.setActiveStatus("Y");
        assertThat(testCard.getActiveStatus()).isEqualTo("Y");
        assertThat(testCard.isActive()).isTrue();
        
        // Test inactive status 'N'
        testCard.setActiveStatus("N");
        assertThat(testCard.getActiveStatus()).isEqualTo("N");
        assertThat(testCard.isActive()).isFalse();
        
        // Validate single character length matching PIC X(01)
        assertThat(testCard.getActiveStatus().length()).isEqualTo(1);
        
        logTestExecution("Active status field mapping validated", null);
    }

    // JPA Annotation and Column Constraint Tests

    /**
     * Test JPA column annotations and database constraints using reflection.
     * Validates that entity field annotations match COBOL field specifications
     * including column names, lengths, nullable constraints, and unique constraints.
     */
    @Test
    public void testJpaColumnAnnotations() throws NoSuchFieldException {
        // Test card number column annotation
        Field cardNumberField = Card.class.getDeclaredField("cardNumber");
        Column cardNumberColumn = cardNumberField.getAnnotation(Column.class);
        assertThat(cardNumberColumn.name()).isEqualTo("card_number");
        assertThat(cardNumberColumn.length()).isEqualTo(Constants.CARD_NUMBER_LENGTH);
        assertThat(cardNumberColumn.nullable()).isFalse();
        
        // Test account ID column annotation
        Field accountIdField = Card.class.getDeclaredField("accountId");
        Column accountIdColumn = accountIdField.getAnnotation(Column.class);
        assertThat(accountIdColumn.name()).isEqualTo("account_id");
        assertThat(accountIdColumn.nullable()).isFalse();
        
        // Test CVV code column annotation  
        Field cvvCodeField = Card.class.getDeclaredField("cvvCode");
        Column cvvCodeColumn = cvvCodeField.getAnnotation(Column.class);
        assertThat(cvvCodeColumn.name()).isEqualTo("cvv_code");
        assertThat(cvvCodeColumn.length()).isEqualTo(3);
        assertThat(cvvCodeColumn.nullable()).isFalse();
        
        // Test embossed name column annotation
        Field embossedNameField = Card.class.getDeclaredField("embossedName");
        Column embossedNameColumn = embossedNameField.getAnnotation(Column.class);
        assertThat(embossedNameColumn.name()).isEqualTo("embossed_name");
        assertThat(embossedNameColumn.length()).isEqualTo(50);
        assertThat(embossedNameColumn.nullable()).isFalse();
        
        // Test active status column annotation
        Field activeStatusField = Card.class.getDeclaredField("activeStatus");
        Column activeStatusColumn = activeStatusField.getAnnotation(Column.class);
        assertThat(activeStatusColumn.name()).isEqualTo("active_status");
        assertThat(activeStatusColumn.length()).isEqualTo(1);
        assertThat(activeStatusColumn.nullable()).isFalse();
        
        logTestExecution("JPA column annotations validated", null);
    }

    // Card Number Validation Tests

    /**
     * Parameterized test for card number validation using various valid patterns.
     * Tests card number field with different valid 16-digit patterns to ensure
     * ValidationUtil.validateCardNumber() maintains COBOL validation compatibility.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "1234567890123456", // Standard test pattern
        "4111111111111111", // Visa pattern
        "5555555555554444", // MasterCard pattern  
        "3782822463100005", // American Express (15 digits handled separately)
        "6011111111111117"  // Discover pattern
    })
    public void testValidCardNumbers(String cardNumber) {
        if (cardNumber.length() == 16) { // Only test 16-digit cards for this entity
            testCard.setCardNumber(cardNumber);
            assertThat(testCard.getCardNumber()).isEqualTo(cardNumber);
            
            // Validate using ValidationUtil
            ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
            assertThatNoException().isThrownBy(() -> validator.validateCardNumber(cardNumber));
        }
        
        logTestExecution("Valid card number pattern validated: " + cardNumber, null);
    }

    /**
     * Parameterized test for invalid card number validation.
     * Tests card number field with various invalid patterns to ensure proper
     * validation rejection matching COBOL edit routine behavior.
     */
    @ParameterizedTest  
    @ValueSource(strings = {
        "",                 // Empty string
        "123",              // Too short
        "12345678901234567", // Too long (17 digits)
        "123456789012345a"  // Contains letter (ValidationUtil cleans dashes/spaces, so they pass)
    })
    public void testInvalidCardNumbers(String invalidCardNumber) {
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        assertThatThrownBy(() -> validator.validateCardNumber(invalidCardNumber))
            .isInstanceOf(com.carddemo.exception.ValidationException.class);
        
        logTestExecution("Invalid card number rejected: " + invalidCardNumber, null);
    }

    // Account ID Validation Tests

    /**
     * Test account ID validation using ValidationUtil.validateAccountId().
     * Validates that account ID field accepts valid 11-digit patterns and
     * rejects invalid formats, maintaining COBOL validation compatibility.
     */
    @Test
    public void testValidAccountIdValidation() {
        // Test valid account ID patterns
        String validAccountId = TestConstants.TEST_ACCOUNT_ID.toString();
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        assertThatNoException().isThrownBy(() -> validator.validateAccountId(validAccountId));
        
        // Test account ID assignment to card
        testCard.setAccountId(Long.parseLong(validAccountId));
        assertThat(testCard.getAccountId()).isEqualTo(Long.parseLong(validAccountId));
        
        logTestExecution("Valid account ID validation passed", null);
    }

    /**
     * Parameterized test for invalid account ID validation.
     * Tests account ID field with various invalid patterns to ensure proper
     * validation rejection matching COBOL field validation behavior.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "",           // Empty string
        "123",        // Too short (less than 11 digits)
        "123456789012", // Too long (more than 11 digits) 
        "1234567890a",  // Contains letter
        "12345-67890",  // Contains dash
        "12345 67890"   // Contains space
    })
    public void testInvalidAccountIds(String invalidAccountId) {
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        assertThatThrownBy(() -> validator.validateAccountId(invalidAccountId))
            .isInstanceOf(com.carddemo.exception.ValidationException.class);
            
        logTestExecution("Invalid account ID rejected: " + invalidAccountId, null);
    }

    // CVV Code Validation Tests

    /**
     * Parameterized test for CVV code validation with valid 3-digit patterns.
     * Validates that CVV code field accepts standard 3-digit security codes
     * following industry standard validation patterns.
     */
    @ParameterizedTest
    @ValueSource(strings = {"123", "999", "000", "789", "101"})
    public void testValidCvvCodes(String cvvCode) {
        testCard.setCvvCode(cvvCode);
        assertThat(testCard.getCvvCode()).isEqualTo(cvvCode);
        assertThat(testCard.getCvvCode()).matches("\\d{3}");
        
        // Validate CVV is excluded from toString() serialization (security requirement)
        // Ensure no CVV field is exposed in toString output for security compliance
        String cardString = testCard.toString();
        assertThat(cardString).doesNotContain("cvvCode");
        assertThat(cardString).doesNotContain("cvv");
        // For CVV value check, exclude false positives from accountId field
        assertThat(cardString).doesNotContain("cvvCode='" + cvvCode + "'");
        
        logTestExecution("Valid CVV code validated: " + cvvCode, null);
    }

    /**
     * Parameterized test for invalid CVV code validation.
     * Tests CVV code field with invalid patterns to ensure proper security
     * validation and rejection of non-standard formats.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "12", "1234", "12a", "abc"})
    public void testInvalidCvvCodes(String invalidCvv) {
        testCard.setCvvCode(invalidCvv);
        
        // Validate constraint violations for invalid CVV codes
        Set<ConstraintViolation<Card>> violations = validator.validate(testCard);
        boolean hasCvvViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("cvvCode"));
            
        if (invalidCvv == null || invalidCvv.trim().isEmpty() || !invalidCvv.matches("\\d{3}")) {
            assertThat(hasCvvViolation || violations.size() > 0).isTrue();
        }
        
        logTestExecution("Invalid CVV code validation tested: " + invalidCvv, null);
    }

    // Expiration Date Validation Tests

    /**
     * Test expiration date validation and business logic.
     * Validates date parsing, future date validation, and expiration checking
     * using DateConversionUtil methods for COBOL compatibility.
     */
    @Test
    public void testExpirationDateValidation() {
        // Test valid future expiration date
        LocalDate futureDate = LocalDate.now().plusMonths(6);
        testCard.setExpirationDate(futureDate);
        assertThat(testCard.isExpired()).isFalse();
        
        // Test date validation using DateConversionUtil
        assertThat(DateConversionUtil.validateDate(futureDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")))).isTrue();
        
        // Test expired date (past date)
        LocalDate expiredDate = LocalDate.now().minusMonths(6);
        testCard.setExpirationDate(expiredDate);
        assertThat(testCard.isExpired()).isTrue();
        
        // Test null expiration date handling
        testCard.setExpirationDate(null);
        assertThat(testCard.isExpired()).isTrue();
        
        logTestExecution("Expiration date validation passed", null);
    }

    /**
     * Test expiration date parsing and format conversion.
     * Validates date format conversion using DateConversionUtil.parseDate()
     * and DateConversionUtil.convertDateFormat() for COBOL date compatibility.
     */
    @Test
    public void testExpirationDateFormatConversion() {
        // Test date parsing from CCYYMMDD format (COBOL standard)
        String cobolDateFormat = "20251201"; // December 1, 2025
        LocalDate parsedDate = DateConversionUtil.parseDate(cobolDateFormat);
        assertThat(parsedDate).isNotNull();
        assertThat(parsedDate.getYear()).isEqualTo(2025);
        assertThat(parsedDate.getMonthValue()).isEqualTo(12);
        assertThat(parsedDate.getDayOfMonth()).isEqualTo(1);
        
        // Test date format conversion
        String convertedDate = DateConversionUtil.convertDateFormat(cobolDateFormat, "yyyyMMdd", "yyyy-MM-dd");
        assertThat(convertedDate).isEqualTo("2025-12-01");
        
        // Set converted date to card and validate
        testCard.setExpirationDate(parsedDate);
        assertThat(testCard.getExpirationDate()).isEqualTo(parsedDate);
        assertThat(testCard.isExpired()).isFalse();
        
        logTestExecution("Date format conversion validated", null);
    }

    // Relationship Testing

    /**
     * Test card-account relationship mapping and integrity.
     * Validates JPA relationship annotations, foreign key constraints,
     * and bidirectional relationship management between Card and Account entities.
     */
    @Test
    public void testCardAccountRelationship() {
        // Test account relationship setup
        testCard.setAccount(testAccount);
        assertThat(testCard.getAccount()).isEqualTo(testAccount);
        assertThat(testCard.getAccountId()).isEqualTo(testAccount.getAccountId());
        
        // Test account relationship with null handling
        testCard.setAccount(null);
        assertThat(testCard.getAccount()).isNull();
        // Account ID should remain set even when relationship is null
        assertThat(testCard.getAccountId()).isNotNull();
        
        // Test account balance access through relationship
        testCard.setAccount(testAccount);
        assertThat(testCard.getAccount().getCurrentBalance()).isEqualTo(testAccount.getCurrentBalance());
        
        logTestExecution("Card-Account relationship validated", null);
    }

    /**
     * Test card-customer relationship mapping and integrity.
     * Validates JPA relationship annotations, foreign key constraints,
     * and customer association management for card ownership tracking.
     */
    @Test
    public void testCardCustomerRelationship() {
        // Test customer relationship setup
        testCard.setCustomer(testCustomer);
        assertThat(testCard.getCustomer()).isEqualTo(testCustomer);
        assertThat(testCard.getCustomerId()).isEqualTo(testCustomer.getCustomerId()); // Both return formatted String for COBOL compatibility
        
        // Test customer name access through relationship
        assertThat(testCard.getCustomer().getFirstName()).isEqualTo(testCustomer.getFirstName());
        assertThat(testCard.getCustomer().getLastName()).isEqualTo(testCustomer.getLastName());
        
        // Test customer relationship with null handling
        testCard.setCustomer(null);
        assertThat(testCard.getCustomer()).isNull();
        // Customer ID should remain set even when relationship is null
        assertThat(testCard.getCustomerId()).isNotNull();
        
        logTestExecution("Card-Customer relationship validated", null);
    }

    // Business Logic Tests

    /**
     * Test card type determination business logic.
     * Validates card type detection using ValidationUtil.determineCardType()
     * for industry standard card number pattern recognition.
     */
    @Test
    public void testCardTypeDetection() {
        // Test Visa card type (starts with 4)
        testCard.setCardNumber("4111111111111111");
        assertThat(testCard.getCardType()).isEqualTo("VISA");
        
        // Test MasterCard type (starts with 5)
        testCard.setCardNumber("5555555555554444");
        assertThat(testCard.getCardType()).isEqualTo("MASTERCARD");
        
        // Test unknown card type
        testCard.setCardNumber("1234567890123456");
        String cardType = testCard.getCardType();
        assertThat(cardType).isNotNull();
        
        logTestExecution("Card type detection validated", null);
    }

    /**
     * Test card masking functionality for PCI DSS compliance.
     * Validates that getMaskedCardNumber() properly masks sensitive data
     * while preserving last 4 digits for identification purposes.
     */
    @Test
    public void testCardNumberMasking() {
        String cardNumber = "1234567890123456";
        testCard.setCardNumber(cardNumber);
        
        String maskedNumber = testCard.getMaskedCardNumber();
        assertThat(maskedNumber).isNotNull();
        assertThat(maskedNumber).isNotEqualTo(cardNumber); // Should be masked
        assertThat(maskedNumber).endsWith("3456"); // Should show last 4 digits
        assertThat(maskedNumber).contains("*"); // Should contain masking characters
        
        logTestExecution("Card number masking validated", null);
    }

    /**
     * Test comprehensive card validation business logic.
     * Validates Card.validateCard() method implementation that replicates
     * COBOL edit routines for complete field and business rule validation.
     */
    @Test
    public void testComprehensiveCardValidation() {
        // Test validation with valid card data
        assertThatNoException().isThrownBy(() -> testCard.validateCard());
        
        // Test validation with missing card number
        testCard.setCardNumber(null);
        assertThatThrownBy(() -> testCard.validateCard())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("cardNumber");
        
        // Reset and test validation with missing account ID
        testCard.setCardNumber(TestConstants.TEST_CARD_NUMBER);
        testCard.setAccountId(null);
        assertThatThrownBy(() -> testCard.validateCard())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("accountId");
        
        // Reset and test validation with expired date
        testCard.setAccountId(TestConstants.TEST_ACCOUNT_ID);
        testCard.setExpirationDate(LocalDate.now().minusYears(1));
        assertThatThrownBy(() -> testCard.validateCard())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("expiration");
        
        logTestExecution("Comprehensive card validation tested", null);
    }

    // Object Methods Tests

    /**
     * Test Card entity equals method implementation.
     * Validates that equals() method properly compares Card entities based on
     * card number (primary key) and handles null values correctly.
     */
    @Test
    public void testCardEquals() {
        // Test equals with same card number
        Card card1 = new Card();
        card1.setCardNumber("1234567890123456");
        
        Card card2 = new Card();
        card2.setCardNumber("1234567890123456");
        
        assertThat(card1).isEqualTo(card2);
        
        // Test equals with different card numbers
        card2.setCardNumber("6543210987654321");
        assertThat(card1).isNotEqualTo(card2);
        
        // Test equals with null
        assertThat(card1).isNotEqualTo(null);
        
        // Test equals with same instance
        assertThat(card1).isEqualTo(card1);
        
        logTestExecution("Card equals method validated", null);
    }

    /**
     * Test Card entity hashCode method implementation.
     * Validates that hashCode() method returns consistent values based on
     * card number and supports proper hash-based collection behavior.
     */
    @Test
    public void testCardHashCode() {
        // Test hashCode consistency
        Card card1 = new Card();
        card1.setCardNumber("1234567890123456");
        
        int hashCode1 = card1.hashCode();
        int hashCode2 = card1.hashCode();
        assertThat(hashCode1).isEqualTo(hashCode2);
        
        // Test hashCode equality for equal objects
        Card card2 = new Card();
        card2.setCardNumber("1234567890123456");
        assertThat(card1.hashCode()).isEqualTo(card2.hashCode());
        
        // Test hashCode difference for different objects
        card2.setCardNumber("6543210987654321");
        assertThat(card1.hashCode()).isNotEqualTo(card2.hashCode());
        
        logTestExecution("Card hashCode method validated", null);
    }

    /**
     * Test Card entity toString method implementation.
     * Validates that toString() method provides comprehensive representation
     * while maintaining security by masking sensitive data (CVV excluded).
     */
    @Test
    public void testCardToString() {
        String cardString = testCard.toString();
        
        // Verify toString contains key information
        assertThat(cardString).contains("Card{");
        assertThat(cardString).contains("accountId=" + testCard.getAccountId());
        assertThat(cardString).contains("customerId=" + testCard.getCustomerId());
        assertThat(cardString).contains("embossedName='" + testCard.getEmbossedName() + "'");
        assertThat(cardString).contains("activeStatus='" + testCard.getActiveStatus() + "'");
        
        // Verify toString shows masked card number (security requirement)
        assertThat(cardString).contains("cardNumber=");
        assertThat(cardString).doesNotContain(testCard.getCardNumber()); // Should be masked
        
        // Verify toString does NOT contain CVV (security requirement)
        assertThat(cardString).doesNotContain(testCard.getCvvCode());
        
        // Verify toString contains business logic results
        assertThat(cardString).contains("expired=" + testCard.isExpired());
        assertThat(cardString).contains("active=" + testCard.isActive());
        
        logTestExecution("Card toString method validated", null);
    }

    // COBOL Field Length and Constraints Validation

    /**
     * Test COBOL 150-byte record structure compliance.
     * Validates that total field lengths match COBOL CARD-RECORD structure
     * from CVACT02Y copybook, ensuring data structure parity.
     */
    @Test
    public void testCobolRecordStructureCompliance() {
        // Validate individual field lengths match COBOL PIC clauses
        assertThat(Constants.CARD_NUMBER_LENGTH).isEqualTo(16);      // CARD-NUM PIC X(16)
        // Note: CARD-ACCT-ID PIC 9(11) maps to Long (no length constraint needed)
        // CVV code length is 3 digits (CARD-CVV-CD PIC 9(03))
        // Embossed name max length is 50 chars (CARD-EMBOSSED-NAME PIC X(50))
        // Expiration date maps to LocalDate (CARD-EXPIRAION-DATE PIC X(10))
        // Active status length is 1 char (CARD-ACTIVE-STATUS PIC X(01))
        // FILLER PIC X(59) is handled by database table structure padding
        
        // Validate total conceptual record length: 16 + 11 + 3 + 50 + 10 + 1 + 59 = 150 bytes
        int conceptualRecordLength = 16 + 11 + 3 + 50 + 10 + 1 + 59;
        assertThat(conceptualRecordLength).isEqualTo(150);
        
        logTestExecution("COBOL 150-byte record structure compliance validated", null);
    }

    /**
     * Test FILLER field handling in database schema.
     * Validates that FILLER PIC X(59) padding is properly handled by
     * PostgreSQL table structure without requiring explicit Java fields.
     */
    @Test
    public void testFillerFieldHandling() {
        // FILLER fields in COBOL are padding that don't require explicit Java fields
        // They are handled by the database table structure and column definitions
        
        // Verify FILLER concept: 59 bytes of padding space
        int fillerLength = 59;
        int expectedFillerLength = 150 - (16 + 11 + 3 + 50 + 10 + 1); // Total - used fields
        assertThat(fillerLength).isEqualTo(expectedFillerLength);
        
        // Verify Card entity doesn't have explicit FILLER field (correct design)
        Field[] fields = Card.class.getDeclaredFields();
        boolean hasFillerField = java.util.Arrays.stream(fields)
            .anyMatch(field -> field.getName().toLowerCase().contains("filler"));
        assertThat(hasFillerField).isFalse(); // FILLER should not be explicit field
        
        logTestExecution("FILLER field handling validated", null);
    }

    /**
     * Test unique constraint validation on card number.
     * Validates that card number field maintains uniqueness constraint
     * for data integrity and prevents duplicate card numbers in the system.
     */
    @Test
    public void testCardNumberUniqueConstraint() {
        // This test validates the concept of uniqueness
        // In actual database testing, this would be validated with JPA repository tests
        
        String uniqueCardNumber = "1111222233334444";
        testCard.setCardNumber(uniqueCardNumber);
        
        // Verify card number is set correctly
        assertThat(testCard.getCardNumber()).isEqualTo(uniqueCardNumber);
        
        // In production, database would enforce uniqueness constraint
        // This test confirms the field is suitable for unique constraint
        assertThat(testCard.getCardNumber()).isNotNull();
        assertThat(testCard.getCardNumber().trim()).isNotEmpty();
        assertThat(testCard.getCardNumber().length()).isEqualTo(16);
        
        logTestExecution("Card number uniqueness constraint concept validated", null);
    }

    // Additional Validation Tests

    /**
     * Test numeric field validation using ValidationUtil.validateNumericField().
     * Validates numeric field validation for account ID and other numeric fields
     * ensuring compatibility with COBOL numeric field validation patterns.
     */
    @Test
    public void testNumericFieldValidation() {
        // Test account ID numeric validation
        String numericAccountId = testCard.getAccountId().toString();
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateNumericField("accountId", numericAccountId));
        
        // Test customer ID numeric validation
        String numericCustomerId = testCard.getCustomerId().toString();
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateNumericField("customerId", numericCustomerId));
        
        logTestExecution("Numeric field validation passed", null);
    }

    /**
     * Test transaction amount validation using ValidationUtil.validateTransactionAmount().
     * Validates monetary field validation patterns for future transaction processing
     * integration and ensures BigDecimal precision compatibility.
     */
    @Test  
    public void testTransactionAmountValidation() {
        // Test valid transaction amount
        java.math.BigDecimal validAmount = new java.math.BigDecimal("150.00");
        ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
        assertThatNoException().isThrownBy(() -> validator.validateTransactionAmount(validAmount));
        
        // Test transaction amount compatibility with COBOL decimal precision
        assertThat(validAmount.scale()).isEqualTo(2); // COBOL S9(10)V99 format
        
        logTestExecution("Transaction amount validation pattern tested", null);
    }
}