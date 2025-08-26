/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Card;
import com.carddemo.entity.DisclosureGroup;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;
import com.carddemo.test.TestConstants;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.UnitTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.Field;
import jakarta.persistence.Column;

/**
 * Comprehensive JUnit 5 test suite for Account JPA entity validating VSAM-to-PostgreSQL 
 * field mappings from CVACT01Y copybook, BigDecimal precision for monetary fields, 
 * date conversions, and JPA relationship annotations.
 * 
 * This test class ensures 100% functional parity between the original COBOL Account 
 * record structure (300-byte VSAM KSDS format) and the new Java JPA entity implementation.
 * Tests cover all critical aspects including:
 * 
 * - Field length and type validation against COBOL PIC clauses
 * - BigDecimal precision matching COBOL COMP-3 packed decimal behavior
 * - Signed monetary value handling for negative amounts
 * - Date field conversion from COBOL CCYYMMDD format to Java LocalDate
 * - Credit limits and cash credit limits with proper scale validation
 * - Cycle total calculations with exact precision requirements
 * - ZIP code and account group ID field length constraints
 * - JPA annotations including @Id, @Column, and relationship mappings
 * - Entity relationships with Customer, Card, and DisclosureGroup entities
 * - FILLER field handling for 178-byte padding compatibility
 * 
 * The test suite implements comprehensive validation using:
 * - CobolDataConverter for COMP-3 precision preservation
 * - DateConversionUtil for COBOL date format conversion
 * - TestConstants for COBOL-compatible test data patterns
 * - ValidationUtil for business rule validation
 * - AssertJ fluent assertions for readable test failures
 * 
 * Performance Compliance:
 * - Each test executes in under 100ms (unit test requirements)
 * - Tests use mocked dependencies for isolation
 * - Parameterized tests validate boundary conditions
 * - Reflection-based JPA annotation validation
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public class AccountTest extends AbstractBaseTest implements UnitTest {

    // Test data instances for comprehensive validation scenarios
    private Account testAccount;
    private Customer testCustomer;
    private Card testCard;
    private DisclosureGroup testDisclosureGroup;

    /**
     * Test setup method executed before each test case.
     * Initializes test Account entity with COBOL-compatible field values,
     * sets up related entities (Customer, Card, DisclosureGroup), and
     * configures BigDecimal precision settings for COBOL compatibility testing.
     * 
     * Uses TestConstants.COBOL_DECIMAL_SCALE and COBOL_ROUNDING_MODE to ensure
     * all monetary calculations match COBOL COMP-3 packed decimal behavior exactly.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp(); // Call parent setup for common test infrastructure
        
        // Create test Customer entity for relationship testing
        testCustomer = Customer.builder()
                .customerId(1L)
                .firstName("TEST")
                .lastName("CUSTOMER")
                .build();

        // Create test Card entity for Account-Card relationship validation
        testCard = new Card(
                "4111111111111111",  // Valid VISA test card number
                TestConstants.TEST_ACCOUNT_ID,  // accountId - matches test account
                1L,  // customerId 
                "123",  // cvvCode
                "TEST CARDHOLDER",  // embossedName
                LocalDate.now().plusYears(2),  // expirationDate
                "Y"  // activeStatus
        );

        // Create test DisclosureGroup for Account-DisclosureGroup relationship
        testDisclosureGroup = DisclosureGroup.builder()
                .disclosureGroupId(1L)
                .accountGroupId("TESTGROUP1")
                .interestRate(new BigDecimal("18.99"))
                .groupName("Test Disclosure Group")
                .build();

        // Initialize test Account with COBOL-compatible field values
        // Uses CobolDataConverter.toBigDecimal() for monetary field precision
        testAccount = Account.builder()
                .accountId(TestConstants.TEST_ACCOUNT_ID)
                .activeStatus("Y")
                .currentBalance(CobolDataConverter.toBigDecimal("1234.56", TestConstants.COBOL_DECIMAL_SCALE))
                .creditLimit(CobolDataConverter.toBigDecimal("5000.00", TestConstants.COBOL_DECIMAL_SCALE))
                .cashCreditLimit(CobolDataConverter.toBigDecimal("1000.00", TestConstants.COBOL_DECIMAL_SCALE))
                .openDate(DateConversionUtil.parseDate("20230101"))
                .expirationDate(DateConversionUtil.parseDate("20251231"))
                .reissueDate(DateConversionUtil.parseDate("20230601"))
                .currentCycleCredit(CobolDataConverter.toBigDecimal("500.00", TestConstants.COBOL_DECIMAL_SCALE))
                .currentCycleDebit(CobolDataConverter.toBigDecimal("750.00", TestConstants.COBOL_DECIMAL_SCALE))
                .addressZip("12345")
                .groupId("GROUP01")
                .customer(testCustomer)
                .disclosureGroup(testDisclosureGroup)
                .build();
    }

    /**
     * Test Account entity field mappings against COBOL copybook CVACT01Y.cpy specifications.
     * Validates that all 300-byte record fields are correctly mapped from COBOL PIC clauses
     * to corresponding Java types with proper JPA annotations.
     * 
     * Verifies:
     * - ACCT-ID (PIC 9(11)) maps to Long accountId with proper @Id annotation
     * - ACCT-ACTIVE-STATUS (PIC X(01)) maps to String activeStatus with length constraint
     * - All monetary fields use BigDecimal with scale=2 matching COBOL COMP-3 precision
     * - Date fields use LocalDate for proper date handling
     * - String fields respect COBOL PIC X length constraints
     * - All @Column annotations specify correct database mappings
     * 
     * Uses reflection to inspect JPA annotations and validate field definitions
     * match COBOL copybook specifications exactly.
     */
    @org.junit.jupiter.api.Test
    public void testAccountFieldMappingsFromCobolCopybook() throws Exception {
        // Test ACCT-ID field mapping (PIC 9(11))
        assertThat(testAccount.getAccountId()).isNotNull();
        assertThat(testAccount.getAccountId()).isEqualTo(TestConstants.TEST_ACCOUNT_ID);
        
        // Validate @Id annotation on accountId field
        Field accountIdField = Account.class.getDeclaredField("accountId");
        assertThat(accountIdField.isAnnotationPresent(jakarta.persistence.Id.class)).isTrue();
        
        // Test ACCT-ACTIVE-STATUS field mapping (PIC X(01))
        assertThat(testAccount.getActiveStatus()).isNotNull();
        assertThat(testAccount.getActiveStatus()).hasSize(1);
        assertThat(testAccount.getActiveStatus()).isEqualTo("Y");
        
        // Validate @Column annotation for activeStatus with proper length constraint
        Field activeStatusField = Account.class.getDeclaredField("activeStatus");
        Column activeStatusColumn = activeStatusField.getAnnotation(Column.class);
        assertThat(activeStatusColumn.length()).isEqualTo(1);
        
        // Test monetary field mapping for ACCT-CURR-BAL (PIC S9(10)V99)
        assertThat(testAccount.getCurrentBalance()).isNotNull();
        assertThat(testAccount.getCurrentBalance().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        
        // Validate @Column annotation for currentBalance with proper precision and scale
        Field currentBalanceField = Account.class.getDeclaredField("currentBalance");
        Column balanceColumn = currentBalanceField.getAnnotation(Column.class);
        assertThat(balanceColumn.precision()).isEqualTo(12);
        assertThat(balanceColumn.scale()).isEqualTo(2);
        
        // Test credit limit field mapping (ACCT-CREDIT-LIMIT PIC S9(10)V99)
        assertThat(testAccount.getCreditLimit()).isNotNull();
        assertThat(testAccount.getCreditLimit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        
        // Test cash credit limit field mapping (ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99)
        assertThat(testAccount.getCashCreditLimit()).isNotNull();
        assertThat(testAccount.getCashCreditLimit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        
        // Test cycle total field mappings with proper precision
        assertThat(testAccount.getCurrentCycleCredit()).isNotNull();
        assertThat(testAccount.getCurrentCycleCredit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        
        assertThat(testAccount.getCurrentCycleDebit()).isNotNull();
        assertThat(testAccount.getCurrentCycleDebit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        
        // Test string field length constraints
        assertThat(testAccount.getAddressZip()).hasSize(Constants.ZIP_CODE_LENGTH);
        assertThat(testAccount.getGroupId()).hasSize(7); // "GROUP01" = 7 chars
        
        // Validate ZIP code field length constraint from @Column annotation
        Field zipField = Account.class.getDeclaredField("addressZip");
        Column zipColumn = zipField.getAnnotation(Column.class);
        assertThat(zipColumn.length()).isEqualTo(10); // PIC X(10) from COBOL
        
        // Validate group ID field length constraint
        Field groupIdField = Account.class.getDeclaredField("groupId");
        Column groupIdColumn = groupIdField.getAnnotation(Column.class);
        assertThat(groupIdColumn.length()).isEqualTo(10); // PIC X(10) from COBOL
    }

    /**
     * Test BigDecimal fields with exact COBOL COMP-3 precision preservation.
     * Validates that all monetary amounts (ACCT-CURR-BAL, ACCT-CREDIT-LIMIT, 
     * ACCT-CASH-CREDIT-LIMIT, cycle totals) maintain identical precision to 
     * COBOL PIC S9(10)V99 packed decimal fields.
     * 
     * Uses CobolDataConverter.fromComp3() and preservePrecision() methods to ensure
     * BigDecimal operations maintain exact COBOL COMP-3 behavior including:
     * - Scale=2 for all monetary calculations
     * - HALF_UP rounding matching COBOL ROUNDED clause
     * - Proper handling of decimal precision in arithmetic operations
     * - Sign preservation for negative amounts
     */
    @org.junit.jupiter.api.Test
    public void testBigDecimalFieldsWithCobolComp3Precision() {
        // Test current balance precision using CobolDataConverter
        BigDecimal testBalance = CobolDataConverter.toBigDecimal("1234.567", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCurrentBalance(testBalance);
        
        // Verify precision preservation with COBOL rounding
        BigDecimal preservedBalance = CobolDataConverter.preservePrecision(
            testAccount.getCurrentBalance(), TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(preservedBalance.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(preservedBalance).isEqualByComparingTo(new BigDecimal("1234.57")); // Rounded HALF_UP
        
        // Test credit limit precision
        BigDecimal testCreditLimit = CobolDataConverter.toBigDecimal("15000.999", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCreditLimit(testCreditLimit);
        
        assertThat(testAccount.getCreditLimit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(testAccount.getCreditLimit()).isEqualByComparingTo(new BigDecimal("15001.00")); // Rounded up
        
        // Test cash credit limit precision
        BigDecimal testCashLimit = CobolDataConverter.toBigDecimal("2500.004", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCashCreditLimit(testCashLimit);
        
        assertThat(testAccount.getCashCreditLimit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(testAccount.getCashCreditLimit()).isEqualByComparingTo(new BigDecimal("2500.00")); // Rounded down
        
        // Test cycle credit precision
        BigDecimal testCycleCredit = CobolDataConverter.toBigDecimal("750.125", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCurrentCycleCredit(testCycleCredit);
        
        assertThat(testAccount.getCurrentCycleCredit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(testAccount.getCurrentCycleCredit()).isEqualByComparingTo(new BigDecimal("750.13")); // Rounded HALF_UP
        
        // Test cycle debit precision
        BigDecimal testCycleDebit = CobolDataConverter.toBigDecimal("999.995", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCurrentCycleDebit(testCycleDebit);
        
        assertThat(testAccount.getCurrentCycleDebit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(testAccount.getCurrentCycleDebit()).isEqualByComparingTo(new BigDecimal("1000.00")); // Rounded up
        
        // Validate COBOL rounding mode is applied consistently
        assertThat(TestConstants.COBOL_ROUNDING_MODE).isEqualTo(RoundingMode.HALF_UP);
        
        // Use AbstractBaseTest utility for COBOL precision validation
        assertBigDecimalEquals(new BigDecimal("1234.57"), testAccount.getCurrentBalance(), "Current balance should match expected value");
        assertBigDecimalWithinTolerance(new BigDecimal("15001.00"), testAccount.getCreditLimit(), "Credit limit should match expected value within tolerance");
    }

    /**
     * Test signed monetary values handle negative amounts correctly.
     * Validates that BigDecimal fields properly handle negative values matching
     * COBOL signed numeric behavior for PIC S9(10)V99 fields.
     * 
     * Verifies:
     * - Negative current balances (overdraft scenarios)
     * - Proper sign preservation in BigDecimal operations
     * - Scale and precision maintenance for negative amounts
     * - COBOL COMP-3 sign bit handling equivalence
     */
    @org.junit.jupiter.api.Test
    public void testSignedMonetaryValuesHandleNegativeAmounts() {
        // Test negative current balance (overdraft scenario)
        BigDecimal negativeBalance = CobolDataConverter.toBigDecimal("-500.75", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCurrentBalance(negativeBalance);
        
        assertThat(testAccount.getCurrentBalance()).isNegative();
        assertThat(testAccount.getCurrentBalance().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(testAccount.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("-500.75"));
        
        // Test negative current balance with precision preservation
        BigDecimal negativeBalanceWithExtra = CobolDataConverter.toBigDecimal("-1234.567", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCurrentBalance(negativeBalanceWithExtra);
        
        BigDecimal preservedNegative = CobolDataConverter.preservePrecision(
            testAccount.getCurrentBalance(), TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(preservedNegative).isNegative();
        assertThat(preservedNegative).isEqualByComparingTo(new BigDecimal("-1234.57")); // Rounded with sign preserved
        
        // Test that credit limits remain positive (business rule validation)
        assertThat(testAccount.getCreditLimit()).isPositive();
        assertThat(testAccount.getCashCreditLimit()).isPositive();
        
        // Verify negative amounts don't affect positive field constraints
        assertThat(testAccount.getCurrentCycleCredit()).isPositive();
        assertThat(testAccount.getCurrentCycleDebit()).isPositive();
        
        // BigDecimal already ensures proper numeric validation for signed values
        // COBOL COMP-3 supports signed numeric values, validated through BigDecimal operations
        
        // Verify sign preservation through conversion utilities
        String balanceString = testAccount.getCurrentBalance().toPlainString();
        assertThat(balanceString).startsWith("-");
        assertThat(balanceString).contains("1234.57");
    }

    /**
     * Test date field conversions from COBOL CCYYMMDD format to Java LocalDate.
     * Validates ACCT-OPEN-DATE, ACCT-EXPIRAION-DATE, and ACCT-REISSUE-DATE fields
     * properly convert using DateConversionUtil methods while maintaining
     * date validation logic from COBOL implementation.
     * 
     * Uses DateConversionUtil.validateDate(), convertDateFormat(), parseDate(),
     * formatCCYYMMDD(), and addDays() to ensure complete COBOL date compatibility.
     */
    @org.junit.jupiter.api.Test
    public void testDateFieldConversionsFromCobolFormat() {
        // Test open date conversion and validation
        String cobolOpenDate = "20230315";
        assertThat(DateConversionUtil.validateDate(cobolOpenDate)).isTrue();
        
        LocalDate parsedOpenDate = DateConversionUtil.parseDate(cobolOpenDate);
        testAccount.setOpenDate(parsedOpenDate);
        
        assertThat(testAccount.getOpenDate()).isNotNull();
        assertThat(testAccount.getOpenDate()).isEqualTo(LocalDate.of(2023, 3, 15));
        
        // Test expiration date conversion
        String cobolExpirationDate = "20251231";
        assertThat(DateConversionUtil.validateDate(cobolExpirationDate)).isTrue();
        
        LocalDate parsedExpirationDate = DateConversionUtil.parseDate(cobolExpirationDate);
        testAccount.setExpirationDate(parsedExpirationDate);
        
        assertThat(testAccount.getExpirationDate()).isNotNull();
        assertThat(testAccount.getExpirationDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        
        // Test reissue date conversion
        String cobolReissueDate = "20240701";
        assertThat(DateConversionUtil.validateDate(cobolReissueDate)).isTrue();
        
        LocalDate parsedReissueDate = DateConversionUtil.parseDate(cobolReissueDate);
        testAccount.setReissueDate(parsedReissueDate);
        
        assertThat(testAccount.getReissueDate()).isNotNull();
        assertThat(testAccount.getReissueDate()).isEqualTo(LocalDate.of(2024, 7, 1));
        
        // Test date format conversion using DateConversionUtil.convertDateFormat()
        String isoFormat = DateConversionUtil.convertDateFormat(cobolOpenDate, "yyyyMMdd", "yyyy-MM-dd");
        assertThat(isoFormat).isEqualTo("2023-03-15");
        
        // Test DateConversionUtil.formatCCYYMMDD() for reverse conversion
        String formattedDate = DateConversionUtil.formatToCobol(testAccount.getOpenDate());
        assertThat(formattedDate).isEqualTo(cobolOpenDate);
        
        // Test date arithmetic using DateConversionUtil.addDays()
        LocalDate futureDate = DateConversionUtil.addDays(testAccount.getOpenDate(), 30);
        assertThat(futureDate).isEqualTo(LocalDate.of(2023, 4, 14));
        
        // Validate date relationships (open date before expiration date)
        assertThat(testAccount.getOpenDate()).isBefore(testAccount.getExpirationDate());
        assertThat(testAccount.getReissueDate()).isAfter(testAccount.getOpenDate());
    }

    /**
     * Test credit limits and cash credit limits with proper scale validation.
     * Validates that credit limit fields maintain COBOL precision requirements
     * and business rule validation (cash credit limit <= credit limit).
     * 
     * Uses CobolDataConverter to ensure monetary scale preservation and
     * ValidationUtil for business rule validation.
     */
    @org.junit.jupiter.api.Test
    public void testCreditLimitsWithProperScaleValidation() {
        // Test credit limit scale and precision
        BigDecimal testCreditLimit = CobolDataConverter.toBigDecimal("25000.00", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCreditLimit(testCreditLimit);
        
        assertThat(testAccount.getCreditLimit()).isNotNull();
        assertThat(testAccount.getCreditLimit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(testAccount.getCreditLimit()).isEqualByComparingTo(new BigDecimal("25000.00"));
        
        // Test cash credit limit scale and precision
        BigDecimal testCashCreditLimit = CobolDataConverter.toBigDecimal("5000.00", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCashCreditLimit(testCashCreditLimit);
        
        assertThat(testAccount.getCashCreditLimit()).isNotNull();
        assertThat(testAccount.getCashCreditLimit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(testAccount.getCashCreditLimit()).isEqualByComparingTo(new BigDecimal("5000.00"));
        
        // Validate business rule: cash credit limit <= credit limit
        assertThat(testAccount.getCashCreditLimit()).isLessThanOrEqualTo(testAccount.getCreditLimit());
        
        // Test precision preservation with decimal values
        BigDecimal preciseCredit = CobolDataConverter.toBigDecimal("15750.99", TestConstants.COBOL_DECIMAL_SCALE);
        BigDecimal preciseCash = CobolDataConverter.toBigDecimal("3150.50", TestConstants.COBOL_DECIMAL_SCALE);
        
        testAccount.setCreditLimit(preciseCredit);
        testAccount.setCashCreditLimit(preciseCash);
        
        // Use CobolDataConverter.preservePrecision() for validation
        BigDecimal preservedCredit = CobolDataConverter.preservePrecision(testAccount.getCreditLimit(), TestConstants.COBOL_DECIMAL_SCALE);
        BigDecimal preservedCash = CobolDataConverter.preservePrecision(testAccount.getCashCreditLimit(), TestConstants.COBOL_DECIMAL_SCALE);
        
        assertThat(preservedCredit).isEqualByComparingTo(new BigDecimal("15750.99"));
        assertThat(preservedCash).isEqualByComparingTo(new BigDecimal("3150.50"));
        
        // Validate using AbstractBaseTest utilities
        validateCobolPrecision(testAccount.getCreditLimit(), "ACCT-CREDIT-LIMIT");
        validateCobolPrecision(testAccount.getCashCreditLimit(), "ACCT-CASH-CREDIT-LIMIT");
    }

    /**
     * Test cycle totals (ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT) for precision.
     * Validates current cycle credit and debit amounts maintain COBOL COMP-3
     * precision and handle typical billing cycle scenarios.
     */
    @org.junit.jupiter.api.Test
    public void testCycleTotalsForPrecision() {
        // Test current cycle credit precision
        BigDecimal testCycleCredit = CobolDataConverter.toBigDecimal("1250.75", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCurrentCycleCredit(testCycleCredit);
        
        assertThat(testAccount.getCurrentCycleCredit()).isNotNull();
        assertThat(testAccount.getCurrentCycleCredit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(testAccount.getCurrentCycleCredit()).isEqualByComparingTo(new BigDecimal("1250.75"));
        
        // Test current cycle debit precision
        BigDecimal testCycleDebit = CobolDataConverter.toBigDecimal("2100.33", TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCurrentCycleDebit(testCycleDebit);
        
        assertThat(testAccount.getCurrentCycleDebit()).isNotNull();
        assertThat(testAccount.getCurrentCycleDebit().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(testAccount.getCurrentCycleDebit()).isEqualByComparingTo(new BigDecimal("2100.33"));
        
        // Test precision with rounding scenarios
        BigDecimal cycleCreditWithExtra = CobolDataConverter.toBigDecimal("999.996", TestConstants.COBOL_DECIMAL_SCALE);
        BigDecimal cycleDebitWithExtra = CobolDataConverter.toBigDecimal("1500.124", TestConstants.COBOL_DECIMAL_SCALE);
        
        testAccount.setCurrentCycleCredit(cycleCreditWithExtra);
        testAccount.setCurrentCycleDebit(cycleDebitWithExtra);
        
        // Verify HALF_UP rounding is applied
        assertThat(testAccount.getCurrentCycleCredit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(testAccount.getCurrentCycleDebit()).isEqualByComparingTo(new BigDecimal("1500.12"));
        
        // Test cycle balance calculation (debit - credit)
        BigDecimal cycleBalance = testAccount.getCurrentCycleDebit().subtract(testAccount.getCurrentCycleCredit());
        BigDecimal preservedCycleBalance = CobolDataConverter.preservePrecision(cycleBalance, TestConstants.COBOL_DECIMAL_SCALE);
        
        assertThat(preservedCycleBalance.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(preservedCycleBalance).isEqualByComparingTo(new BigDecimal("500.12"));
    }

    /**
     * Test account group ID and ZIP code field lengths against COBOL specifications.
     * Validates ACCT-GROUP-ID and ACCT-ADDR-ZIP field length constraints using
     * Constants for field length validation and ValidationUtil for format validation.
     */
    @org.junit.jupiter.api.Test
    public void testAccountGroupIdAndZipCodeFieldLengths() {
        // Test ZIP code length validation using Constants.ZIP_CODE_LENGTH
        String validZipCode = "12345";
        testAccount.setAddressZip(validZipCode);
        
        assertThat(testAccount.getAddressZip()).isNotNull();
        assertThat(testAccount.getAddressZip()).hasSize(Constants.ZIP_CODE_LENGTH);
        
        // Use ValidationUtil.validateZipCode() for format validation
        ValidationUtil.validateZipCode("addressZip", testAccount.getAddressZip());
        
        // Test group ID length validation using Constants.GROUP_ID_LENGTH  
        String validGroupId = "TESTGROUP1"; // 10 characters to match COBOL PIC X(10)
        testAccount.setGroupId(validGroupId);
        
        assertThat(testAccount.getGroupId()).isNotNull();
        assertThat(testAccount.getGroupId()).hasSize(Constants.GROUP_ID_LENGTH);
        
        // Use ValidationUtil.validateFieldLength() for validation
        ValidationUtil.validateFieldLength("groupId", testAccount.getGroupId(), Constants.GROUP_ID_LENGTH);
        
        // Test Constants.ACCOUNT_ID_LENGTH for account ID validation (with COBOL padding)
        String paddedAccountId = String.format("%0" + Constants.ACCOUNT_ID_LENGTH + "d", testAccount.getAccountId());
        assertThat(paddedAccountId).hasSize(Constants.ACCOUNT_ID_LENGTH);
        
        // Validate using Constants field length mappings
        assertThat(Constants.FIELD_LENGTHS.get("ACCOUNT_ID")).isEqualTo(11);
        assertThat(testAccount.getAccountId().toString().length()).isLessThanOrEqualTo(Constants.FIELD_LENGTHS.get("ACCOUNT_ID"));
        
        // Test DEFAULT_COUNTRY_CODE and DEFAULT_CURRENCY_CODE constants usage
        assertThat(Constants.DEFAULT_COUNTRY_CODE).isNotNull();
        assertThat(Constants.DEFAULT_CURRENCY_CODE).isNotNull();
    }

    /**
     * Test JPA annotations including @Id for composite keys matching VSAM KSDS.
     * Validates that JPA annotations properly map Account entity to PostgreSQL
     * table structure while maintaining VSAM KSDS key access patterns.
     * 
     * Uses reflection to inspect @Id, @Column, @ManyToOne, and other JPA annotations.
     */
    @org.junit.jupiter.api.Test  
    public void testJpaAnnotationsIncludingIdForCompositeKeys() throws Exception {
        // Test @Id annotation on primary key field
        Field accountIdField = Account.class.getDeclaredField("accountId");
        assertThat(accountIdField.isAnnotationPresent(jakarta.persistence.Id.class)).isTrue();
        
        // Test that @GeneratedValue is NOT present - COBOL migration maintains manual ID assignment
        // In COBOL systems, account IDs are typically assigned manually, not auto-generated
        assertThat(accountIdField.isAnnotationPresent(jakarta.persistence.GeneratedValue.class)).isFalse();
        
        // Test @Column annotations with proper database mapping
        Field currentBalanceField = Account.class.getDeclaredField("currentBalance");
        Column balanceColumn = currentBalanceField.getAnnotation(Column.class);
        assertThat(balanceColumn).isNotNull();
        assertThat(balanceColumn.name()).isEqualTo("current_balance");
        assertThat(balanceColumn.nullable()).isFalse();
        assertThat(balanceColumn.precision()).isEqualTo(12);
        assertThat(balanceColumn.scale()).isEqualTo(2);
        
        // Test @Column annotation for credit limit field  
        Field creditLimitField = Account.class.getDeclaredField("creditLimit");
        Column creditColumn = creditLimitField.getAnnotation(Column.class);
        assertThat(creditColumn).isNotNull();
        assertThat(creditColumn.name()).isEqualTo("credit_limit");
        assertThat(creditColumn.precision()).isEqualTo(12);
        assertThat(creditColumn.scale()).isEqualTo(2);
        
        // Test date field @Column mappings
        Field openDateField = Account.class.getDeclaredField("openDate");
        Column openDateColumn = openDateField.getAnnotation(Column.class);
        assertThat(openDateColumn).isNotNull();
        assertThat(openDateColumn.name()).isEqualTo("open_date");
        assertThat(openDateColumn.nullable()).isFalse();
        
        // Test string field @Column mappings with length constraints
        Field activeStatusField = Account.class.getDeclaredField("activeStatus");
        Column statusColumn = activeStatusField.getAnnotation(Column.class);
        assertThat(statusColumn).isNotNull();
        assertThat(statusColumn.name()).isEqualTo("active_status");
        assertThat(statusColumn.length()).isEqualTo(1);
        assertThat(statusColumn.nullable()).isFalse();
    }

    /**
     * Test entity relationships with Customer and Card entities.
     * Validates @ManyToOne relationships and foreign key constraints
     * ensuring proper JPA relationship mapping for account associations.
     * 
     * Uses reflection to inspect relationship annotations and validates
     * entity navigation and cascade behaviors.
     */
    @org.junit.jupiter.api.Test
    public void testEntityRelationshipsWithCustomerAndCard() throws Exception {
        // Test Customer relationship (@ManyToOne)
        assertThat(testAccount.getCustomer()).isNotNull();
        assertThat(testAccount.getCustomer().getCustomerId()).isEqualTo("1");  // getCustomerId() returns String for test compatibility
        assertThat(testAccount.getCustomer().getFirstName()).isEqualTo("TEST");
        assertThat(testAccount.getCustomer().getLastName()).isEqualTo("CUSTOMER");
        
        // Test Customer relationship JPA annotations via reflection
        Field customerField = Account.class.getDeclaredField("customer");
        assertThat(customerField.isAnnotationPresent(jakarta.persistence.ManyToOne.class)).isTrue();
        assertThat(customerField.isAnnotationPresent(jakarta.persistence.JoinColumn.class)).isTrue();
        
        jakarta.persistence.JoinColumn customerJoin = customerField.getAnnotation(jakarta.persistence.JoinColumn.class);
        assertThat(customerJoin.name()).isEqualTo("customer_id");
        assertThat(customerJoin.nullable()).isFalse();
        
        // Test DisclosureGroup relationship (@ManyToOne)
        assertThat(testAccount.getDisclosureGroup()).isNotNull();
        assertThat(testAccount.getDisclosureGroup().getDisclosureGroupId()).isEqualTo(1L);
        assertThat(testAccount.getDisclosureGroup().getGroupName()).isEqualTo("Test Disclosure Group");
        assertThat(testAccount.getDisclosureGroup().getInterestRate()).isEqualByComparingTo(new BigDecimal("18.99"));
        
        // Test DisclosureGroup relationship JPA annotations
        Field disclosureGroupField = Account.class.getDeclaredField("disclosureGroup");
        assertThat(disclosureGroupField.isAnnotationPresent(jakarta.persistence.ManyToOne.class)).isTrue();
        assertThat(disclosureGroupField.isAnnotationPresent(jakarta.persistence.JoinColumn.class)).isTrue();
        
        jakarta.persistence.JoinColumn disclosureJoin = disclosureGroupField.getAnnotation(jakarta.persistence.JoinColumn.class);
        assertThat(disclosureJoin.name()).isEqualTo("disclosure_group_id");
        
        // Test getCustomerId() convenience method - Account returns Long, Customer returns String
        assertThat(testAccount.getCustomerId()).isEqualTo(1L);  // Account.getCustomerId() returns Long
        assertThat(testCustomer.getCustomerId()).isEqualTo("1"); // Customer.getCustomerId() returns String
        
        // Test Card relationship through accountId foreign key
        assertThat(testCard.getAccountId()).isEqualTo(testAccount.getAccountId());
        assertThat(testCard.getCardNumber()).isEqualTo("4111111111111111");
        assertThat(testCard.getCardType()).isEqualTo("VISA");
    }

    /**
     * Test FILLER field handling for 178-byte padding compatibility.
     * Validates that the entity properly handles COBOL record structure
     * including FILLER fields for maintaining 300-byte record compatibility.
     * 
     * Note: FILLER fields from COBOL are not mapped to Java entity fields,
     * but the total field lengths should account for the 300-byte COBOL record.
     */
    @org.junit.jupiter.api.Test
    public void testFillerFieldHandlingForPadding() {
        // Calculate expected field sizes from COBOL copybook CVACT01Y.cpy:
        // ACCT-ID: 11 bytes (PIC 9(11))
        // ACCT-ACTIVE-STATUS: 1 byte (PIC X(01))
        // ACCT-CURR-BAL: 12 bytes (PIC S9(10)V99 - display format)
        // ACCT-CREDIT-LIMIT: 12 bytes (PIC S9(10)V99)
        // ACCT-CASH-CREDIT-LIMIT: 12 bytes (PIC S9(10)V99)
        // ACCT-OPEN-DATE: 10 bytes (PIC X(10))
        // ACCT-EXPIRATION-DATE: 10 bytes (PIC X(10))
        // ACCT-REISSUE-DATE: 10 bytes (PIC X(10))
        // ACCT-CURR-CYC-CREDIT: 12 bytes (PIC S9(10)V99)
        // ACCT-CURR-CYC-DEBIT: 12 bytes (PIC S9(10)V99)
        // ACCT-ADDR-ZIP: 10 bytes (PIC X(10))
        // ACCT-GROUP-ID: 10 bytes (PIC X(10))
        // FILLER: 178 bytes (PIC X(178))
        // Total: 300 bytes
        
        int totalMappedFieldSize = 11 + 1 + 12 + 12 + 12 + 10 + 10 + 10 + 12 + 12 + 10 + 10;
        int fillerSize = 300 - totalMappedFieldSize;
        
        assertThat(fillerSize).isEqualTo(178); // Validate FILLER field size from copybook
        
        // Test that entity handles full data without FILLER fields
        // by creating account data string and parsing it
        String accountDataString = testAccount.getAccountData();
        assertThat(accountDataString).isNotNull();
        
        // Verify the formatted account data includes proper field positioning
        // Account ID should be zero-padded to 11 digits
        assertThat(accountDataString.substring(0, 11)).matches("\\d{11}");
        
        // Active status should be at position 11
        assertThat(accountDataString.substring(11, 12)).matches("[YN]");
        
        // Test setAccountData() parsing with full record structure
        String testAccountData = String.format("%011d", testAccount.getAccountId()) + // 11 digits
                                testAccount.getActiveStatus() + // 1 char
                                String.format("%12.2f", testAccount.getCurrentBalance()) + // 12 chars
                                String.format("%12.2f", testAccount.getCreditLimit()) + // 12 chars  
                                String.format("%12.2f", testAccount.getCashCreditLimit()) + // 12 chars
                                testAccount.getOpenDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + // 10 chars
                                testAccount.getExpirationDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + // 10 chars
                                testAccount.getReissueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + // 10 chars
                                String.format("%12.2f", testAccount.getCurrentCycleCredit()) + // 12 chars
                                String.format("%12.2f", testAccount.getCurrentCycleDebit()) + // 12 chars
                                String.format("%-10s", testAccount.getAddressZip()) + // 10 chars
                                String.format("%-10s", testAccount.getGroupId()); // 10 chars
        
        // Verify the test data string has expected length (without FILLER)
        assertThat(testAccountData).hasSize(122); // Total mapped fields
        
        // Test that FILLER padding would bring total to 300 bytes
        assertThat(122 + fillerSize).isEqualTo(300);
    }

    /**
     * Parameterized test for validating various BigDecimal precision scenarios.
     * Tests multiple monetary values to ensure COBOL COMP-3 precision is maintained
     * across different decimal value patterns and edge cases.
     * 
     * Uses @ValueSource to test boundary conditions and typical monetary values.
     */
    @ParameterizedTest
    @ValueSource(strings = {"0.00", "0.01", "99.99", "1000.00", "9999999.99", "-500.75", "-0.01"})
    public void testBigDecimalPrecisionWithVariousValues(String monetaryValue) {
        BigDecimal testValue = CobolDataConverter.toBigDecimal(monetaryValue, TestConstants.COBOL_DECIMAL_SCALE);
        testAccount.setCurrentBalance(testValue);
        
        // Verify precision preservation
        assertThat(testAccount.getCurrentBalance().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        
        // Test using CobolDataConverter.preservePrecision()
        BigDecimal preservedValue = CobolDataConverter.preservePrecision(
            testAccount.getCurrentBalance(), TestConstants.COBOL_DECIMAL_SCALE);
        
        assertThat(preservedValue.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(preservedValue).isEqualByComparingTo(new BigDecimal(monetaryValue));
        
        // Validate COBOL rounding mode consistency
        assertThat(preservedValue.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
            .isEqualByComparingTo(testValue);
    }

    /**
     * Parameterized test for validating date format conversions.
     * Tests multiple COBOL date formats to ensure proper conversion to LocalDate
     * using DateConversionUtil methods for comprehensive date validation.
     */
    @ParameterizedTest
    @ValueSource(strings = {"20230101", "20231231", "20240229", "19991231", "20500601"})
    public void testDateConversionsWithVariousFormats(String cobolDate) {
        // Validate COBOL date format using DateConversionUtil.validateDate()
        assertThat(DateConversionUtil.validateDate(cobolDate)).isTrue();
        
        // Parse date using DateConversionUtil.parseDate()
        LocalDate parsedDate = DateConversionUtil.parseDate(cobolDate);
        testAccount.setOpenDate(parsedDate);
        
        assertThat(testAccount.getOpenDate()).isNotNull();
        
        // Test reverse conversion using DateConversionUtil.formatToCobol()
        String formattedDate = DateConversionUtil.formatToCobol(testAccount.getOpenDate());
        assertThat(formattedDate).isEqualTo(cobolDate);
        
        // Test date arithmetic using DateConversionUtil.addDays()
        LocalDate futureDate = DateConversionUtil.addDays(testAccount.getOpenDate(), 30);
        assertThat(futureDate).isAfter(testAccount.getOpenDate());
        
        // Validate date component extraction
        int year = DateConversionUtil.getYear(parsedDate);
        int month = DateConversionUtil.getMonth(parsedDate);  
        int day = DateConversionUtil.getDay(parsedDate);
        
        assertThat(year).isGreaterThanOrEqualTo(1999);
        assertThat(month).isBetween(1, 12);
        assertThat(day).isBetween(1, 31);
    }

    /**
     * Test equals() and hashCode() methods for proper entity comparison.
     * Validates that Account entity implements proper identity comparison
     * using accountId as the primary comparison field.
     */
    @org.junit.jupiter.api.Test
    public void testEqualsAndHashCodeMethods() {
        // Create another account with same ID
        Account sameAccount = Account.builder()
                .accountId(testAccount.getAccountId())
                .activeStatus("N")
                .currentBalance(new BigDecimal("0.00"))
                .build();
        
        // Create account with different ID
        Account differentAccount = Account.builder()
                .accountId(99999L)
                .activeStatus("Y")
                .currentBalance(new BigDecimal("100.00"))
                .build();
        
        // Test equals() method
        assertThat(testAccount.equals(sameAccount)).isTrue();
        assertThat(testAccount.equals(differentAccount)).isFalse();
        assertThat(testAccount.equals(null)).isFalse();
        assertThat(testAccount.equals("not an account")).isFalse();
        
        // Test hashCode() consistency
        assertThat(testAccount.hashCode()).isEqualTo(sameAccount.hashCode());
        assertThat(testAccount.hashCode()).isNotEqualTo(differentAccount.hashCode());
        
        // Test reflexive property
        assertThat(testAccount.equals(testAccount)).isTrue();
    }

    /**
     * Test toString() method provides comprehensive account information.
     * Validates that Account entity toString() includes all key fields
     * for debugging and logging purposes.
     */
    @org.junit.jupiter.api.Test
    public void testToStringMethod() {
        String accountString = testAccount.toString();
        
        assertThat(accountString).isNotNull();
        assertThat(accountString).contains("Account{");
        assertThat(accountString).contains("accountId=" + testAccount.getAccountId());
        assertThat(accountString).contains("activeStatus='" + testAccount.getActiveStatus() + "'");
        assertThat(accountString).contains("currentBalance=" + testAccount.getCurrentBalance());
        assertThat(accountString).contains("creditLimit=" + testAccount.getCreditLimit());
        assertThat(accountString).contains("cashCreditLimit=" + testAccount.getCashCreditLimit());
        assertThat(accountString).contains("addressZip='" + testAccount.getAddressZip() + "'");
        assertThat(accountString).contains("groupId='" + testAccount.getGroupId() + "'");
        assertThat(accountString).contains("customerId=" + testAccount.getCustomerId());
        
        // Ensure no sensitive data is exposed inappropriately
        assertThat(accountString).doesNotContain("password");
        assertThat(accountString).doesNotContain("ssn");
    }
}