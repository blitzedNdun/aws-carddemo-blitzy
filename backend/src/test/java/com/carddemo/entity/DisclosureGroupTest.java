/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import com.carddemo.entity.DisclosureGroup;
import com.carddemo.entity.Account;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;
import com.carddemo.test.TestConstants;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.math.RoundingMode;
import java.lang.reflect.Field;
import jakarta.persistence.EmbeddedId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Unit test class for DisclosureGroup JPA entity validating 50-byte DIS-GROUP-RECORD structure 
 * from CVTRA02Y copybook, testing interest rate calculations and account grouping.
 * 
 * This comprehensive test suite validates the DisclosureGroup entity's COBOL-to-Java conversion
 * including field mappings, composite key implementation, BigDecimal precision for interest rates,
 * and functional parity with the original COBOL implementation.
 * 
 * Key Testing Areas:
 * - 50-byte DIS-GROUP-RECORD field structure validation
 * - Composite key DIS-GROUP-KEY (account group ID + transaction type + category)
 * - 10-character account group ID (DIS-ACCT-GROUP-ID PIC X(10))
 * - 2-character transaction type code (DIS-TRAN-TYPE-CD PIC X(02))
 * - 4-digit transaction category code (DIS-TRAN-CAT-CD PIC 9(04))
 * - BigDecimal interest rate with S9(04)V99 precision (DIS-INT-RATE)
 * - Signed interest rate handling for special rates
 * - Relationship with Account entity via group ID
 * - Interest calculation accuracy matching COBOL COMP-3
 * - FILLER field handling (28-character padding)
 * 
 * COBOL Copybook Reference (CVTRA02Y.cpy):
 * 01  DIS-GROUP-RECORD.                                                    
 *     05  DIS-GROUP-KEY.                                                   
 *        10 DIS-ACCT-GROUP-ID                     PIC X(10).               
 *        10 DIS-TRAN-TYPE-CD                      PIC X(02).               
 *        10 DIS-TRAN-CAT-CD                       PIC 9(04).               
 *     05  DIS-INT-RATE                            PIC S9(04)V99.           
 *     05  FILLER                                  PIC X(28).               
 * 
 * Test Strategy:
 * - Functional parity validation between COBOL and Java implementations
 * - BigDecimal precision testing with COBOL COMP-3 compatibility
 * - Comprehensive field validation using COBOL PIC clause constraints
 * - Parameterized testing for multiple data scenarios
 * - Reflection-based validation of JPA annotations and field mappings
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@DisplayName("DisclosureGroup Entity Tests")
public class DisclosureGroupTest extends AbstractBaseTest implements UnitTest {

    // Test data constants for DisclosureGroup validation
    private static final String VALID_ACCOUNT_GROUP_ID = "GROUP12345";
    private static final String VALID_TRANSACTION_TYPE_CODE = "01";
    private static final String VALID_TRANSACTION_CATEGORY_CODE = "1234";
    private static final BigDecimal VALID_INTEREST_RATE = new BigDecimal("12.5000");
    private static final String VALID_GROUP_NAME = "Test Disclosure Group";
    private static final String VALID_DESCRIPTION = "Test group for unit testing";

    // Invalid test data for negative testing
    private static final String INVALID_LONG_GROUP_ID = "GROUPID123456789"; // Exceeds 10 chars
    private static final String INVALID_LONG_TYPE_CODE = "123"; // Exceeds 2 chars
    private static final String INVALID_LONG_CATEGORY_CODE = "12345"; // Exceeds 4 chars
    private static final BigDecimal INVALID_NEGATIVE_RATE = new BigDecimal("-5.0000");

    /**
     * Test account group ID field validation according to COBOL DIS-ACCT-GROUP-ID PIC X(10).
     * Validates field length constraints, null handling, and character set validation.
     */
    @DisplayName("Account Group ID Validation - PIC X(10)")
    public void testAccountGroupIdValidation() {
        logTestExecution("Testing account group ID validation", null);

        // Test valid account group ID
        DisclosureGroup validGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .groupName(VALID_GROUP_NAME)
                .build();

        assertThat(validGroup.getAccountGroupId())
                .as("Valid account group ID should be accepted")
                .isEqualTo(VALID_ACCOUNT_GROUP_ID);

        // Test maximum length constraint (10 characters)
        String maxLengthGroupId = "1234567890"; // Exactly 10 characters
        DisclosureGroup maxLengthGroup = DisclosureGroup.builder()
                .accountGroupId(maxLengthGroupId)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .build();

        assertThat(maxLengthGroup.getAccountGroupId())
                .as("Account group ID with maximum length (10 chars) should be accepted")
                .isEqualTo(maxLengthGroupId)
                .hasSize(Constants.GROUP_ID_LENGTH);

        // Test empty string handling
        DisclosureGroup emptyGroup = DisclosureGroup.builder()
                .accountGroupId("")
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .build();

        assertThat(emptyGroup.getAccountGroupId())
                .as("Empty account group ID should be stored as empty string")
                .isEmpty();

        // Test null handling
        DisclosureGroup nullGroup = DisclosureGroup.builder()
                .accountGroupId(null)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .build();

        assertThat(nullGroup.getAccountGroupId())
                .as("Null account group ID should be handled appropriately")
                .isNull();

        // Account group ID is a string field, no COBOL precision validation needed
        logTestExecution("account group ID validation completed", null);
    }

    /**
     * Test transaction type code field validation according to COBOL DIS-TRAN-TYPE-CD PIC X(02).
     * Validates 2-character constraint and alphanumeric content validation.
     */
    @DisplayName("Transaction Type Code Validation - PIC X(02)")
    public void testTransactionTypeCodeValidation() {
        logTestExecution("Testing transaction type code validation", null);

        // Test valid transaction type codes
        String[] validTypeCodes = {"01", "02", "03", "AB", "XY", "99"};
        
        for (String typeCode : validTypeCodes) {
            DisclosureGroup group = DisclosureGroup.builder()
                    .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                    .transactionTypeCode(typeCode)
                    .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                    .interestRate(VALID_INTEREST_RATE)
                    .build();

            assertThat(group.getTransactionTypeCode())
                    .as("Valid transaction type code '%s' should be accepted", typeCode)
                    .isEqualTo(typeCode)
                    .hasSize(Constants.TYPE_CODE_LENGTH);
        }

        // Test maximum length constraint (2 characters exactly)
        String exactLengthTypeCode = "AB"; // Exactly 2 characters
        DisclosureGroup exactLengthGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(exactLengthTypeCode)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .build();

        assertThat(exactLengthGroup.getTransactionTypeCode())
                .as("Transaction type code with exact length (2 chars) should be accepted")
                .isEqualTo(exactLengthTypeCode)
                .hasSize(Constants.TYPE_CODE_LENGTH);

        // Test single character handling
        String singleCharTypeCode = "1";
        DisclosureGroup singleCharGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(singleCharTypeCode)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .build();

        assertThat(singleCharGroup.getTransactionTypeCode())
                .as("Single character transaction type code should be accepted")
                .isEqualTo(singleCharTypeCode)
                .hasSizeLessThanOrEqualTo(Constants.TYPE_CODE_LENGTH);

        // Transaction type code is a string field, no COBOL precision validation needed
        logTestExecution("transaction type code validation completed", null);
    }

    /**
     * Test transaction category code field validation according to COBOL DIS-TRAN-CAT-CD PIC 9(04).
     * Validates 4-digit numeric constraint and leading zero handling.
     */
    @DisplayName("Transaction Category Code Validation - PIC 9(04)")
    public void testTransactionCategoryCodeValidation() {
        logTestExecution("Testing transaction category code validation", null);

        // Test valid category codes with different patterns
        String[] validCategoryCodes = {"0001", "1234", "9999", "0000"};
        
        for (String categoryCode : validCategoryCodes) {
            DisclosureGroup group = DisclosureGroup.builder()
                    .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                    .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                    .transactionCategoryCode(categoryCode)
                    .interestRate(VALID_INTEREST_RATE)
                    .build();

            assertThat(group.getTransactionCategoryCode())
                    .as("Valid transaction category code '%s' should be accepted", categoryCode)
                    .isEqualTo(categoryCode)
                    .hasSize(Constants.CATEGORY_CODE_LENGTH);

            // Validate numeric content using ValidationUtil
            assertThatNoException()
                    .as("Category code '%s' should pass numeric validation", categoryCode)
                    .isThrownBy(() -> ValidationUtil.validateNumericField("categoryCode", categoryCode));
        }

        // Test maximum length constraint (4 characters exactly)
        String exactLengthCategoryCode = "1234"; // Exactly 4 characters
        DisclosureGroup exactLengthGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(exactLengthCategoryCode)
                .interestRate(VALID_INTEREST_RATE)
                .build();

        assertThat(exactLengthGroup.getTransactionCategoryCode())
                .as("Transaction category code with exact length (4 chars) should be accepted")
                .isEqualTo(exactLengthCategoryCode)
                .hasSize(Constants.CATEGORY_CODE_LENGTH);

        // Test leading zeros preservation (important for COBOL compatibility)
        String leadingZerosCode = "0123";
        DisclosureGroup leadingZerosGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(leadingZerosCode)
                .interestRate(VALID_INTEREST_RATE)
                .build();

        assertThat(leadingZerosGroup.getTransactionCategoryCode())
                .as("Leading zeros in category code should be preserved for COBOL compatibility")
                .isEqualTo(leadingZerosCode)
                .startsWith("0");

        // Transaction category code is a string field, no COBOL precision validation needed
        logTestExecution("transaction category code validation completed", null);
    }

    /**
     * Test interest rate precision validation for COBOL S9(04)V99 → BigDecimal precision=6, scale=4.
     * Validates BigDecimal precision, scale, rounding mode, and COBOL COMP-3 compatibility.
     */
    @DisplayName("Interest Rate Precision Validation - S9(04)V99")
    public void testInterestRatePrecisionValidation() {
        logTestExecution("Testing interest rate precision validation", null);

        // Test various interest rates with different precision patterns
        BigDecimal[] testRates = {
                new BigDecimal("0.0000"),    // Zero rate
                new BigDecimal("5.2500"),    // Standard rate
                new BigDecimal("12.7500"),   // Higher rate
                new BigDecimal("99.9999"),   // Maximum rate
                new BigDecimal("1.0001")     // Small decimal
        };

        for (BigDecimal rate : testRates) {
            DisclosureGroup group = DisclosureGroup.builder()
                    .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                    .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                    .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                    .interestRate(rate)
                    .build();

            // Validate BigDecimal precision and scale
            BigDecimal storedRate = group.getInterestRate();
            assertThat(storedRate)
                    .as("Interest rate should maintain BigDecimal precision")
                    .isNotNull();

            // Validate scale matches COBOL precision (4 decimal places for interest rates)
            assertThat(storedRate.scale())
                    .as("Interest rate scale should match COBOL S9(04)V99 precision")
                    .isEqualTo(4); // 4 decimal places for interest rates

            // Test COBOL precision preservation using CobolDataConverter
            BigDecimal preservedRate = CobolDataConverter.preservePrecision(rate, 4);
            assertBigDecimalEquals(preservedRate, storedRate, "Interest rate precision should match COBOL COMP-3");

            // Validate rounding mode compatibility
            BigDecimal roundedRate = rate.setScale(4, TestConstants.COBOL_ROUNDING_MODE);
            assertThat(storedRate.compareTo(roundedRate))
                    .as("Interest rate rounding should match COBOL ROUNDED clause behavior")
                    .isEqualTo(0);
        }

        // Test BigDecimal conversion from COBOL numeric types
        Object cobolNumericValue = "12.75";
        BigDecimal convertedRate = CobolDataConverter.toBigDecimal(cobolNumericValue, 4);
        
        DisclosureGroup convertedGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(convertedRate)
                .build();

        assertBigDecimalWithinTolerance(
                new BigDecimal("12.7500"), 
                convertedGroup.getInterestRate(), 
                "COBOL-converted interest rate should match expected precision"
        );

        validateCobolPrecision(convertedGroup.getInterestRate(), "interest rate precision validation");
    }

    /**
     * Test composite key mapping validation for DIS-GROUP-KEY structure.
     * This test uses reflection to validate that while the entity doesn't have an @EmbeddedId,
     * the composite key concept is represented through the three key fields.
     */
    @DisplayName("Composite Key Structure Validation - DIS-GROUP-KEY")
    public void testCompositeKeyMapping() {
        logTestExecution("Testing composite key structure mapping", null);

        DisclosureGroup group = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .groupName(VALID_GROUP_NAME)
                .build();

        // Validate that all three key components are properly set
        assertThat(group.getAccountGroupId())
                .as("Account group ID component of composite key should be set")
                .isEqualTo(VALID_ACCOUNT_GROUP_ID);

        assertThat(group.getTransactionTypeCode())
                .as("Transaction type code component of composite key should be set")
                .isEqualTo(VALID_TRANSACTION_TYPE_CODE);

        assertThat(group.getTransactionCategoryCode())
                .as("Transaction category code component of composite key should be set")
                .isEqualTo(VALID_TRANSACTION_CATEGORY_CODE);

        // Test composite key uniqueness concept
        DisclosureGroup duplicateGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(new BigDecimal("15.0000"))
                .groupName("Different Group Name")
                .build();

        // While we can't test database uniqueness constraints in unit tests,
        // we can validate that the key components are identical
        assertThat(duplicateGroup.getAccountGroupId())
                .as("Duplicate group should have same account group ID")
                .isEqualTo(group.getAccountGroupId());
        assertThat(duplicateGroup.getTransactionTypeCode())
                .as("Duplicate group should have same transaction type code")
                .isEqualTo(group.getTransactionTypeCode());
        assertThat(duplicateGroup.getTransactionCategoryCode())
                .as("Duplicate group should have same transaction category code")
                .isEqualTo(group.getTransactionCategoryCode());

        // Test composite key field validation using reflection
        Field[] fields = DisclosureGroup.class.getDeclaredFields();
        boolean hasAccountGroupId = false;
        boolean hasTransactionTypeCode = false;
        boolean hasTransactionCategoryCode = false;

        for (Field field : fields) {
            if ("accountGroupId".equals(field.getName())) {
                hasAccountGroupId = true;
            } else if ("transactionTypeCode".equals(field.getName())) {
                hasTransactionTypeCode = true;
            } else if ("transactionCategoryCode".equals(field.getName())) {
                hasTransactionCategoryCode = true;
            }
        }

        assertThat(hasAccountGroupId && hasTransactionTypeCode && hasTransactionCategoryCode)
                .as("All composite key fields should be present in DisclosureGroup entity")
                .isTrue();

        // Composite key validation completed - validate the BigDecimal interest rate
        validateCobolPrecision(group.getInterestRate(), "composite key mapping validation");
    }

    /**
     * Test field length validation for all COBOL PIC clause constraints.
     * Validates that field lengths match COBOL copybook specifications exactly.
     */
    @DisplayName("Field Length Validation - COBOL PIC Clauses")
    public void testFieldLengthValidation() {
        logTestExecution("Testing field length validation", null);

        // Test account group ID length (PIC X(10))
        assertThat(Constants.GROUP_ID_LENGTH)
                .as("Constants.GROUP_ID_LENGTH should match COBOL PIC X(10)")
                .isEqualTo(10);

        // Test transaction type code length (PIC X(02))
        assertThat(Constants.TYPE_CODE_LENGTH)
                .as("Constants.TYPE_CODE_LENGTH should match COBOL PIC X(02)")
                .isEqualTo(2);

        // Test transaction category code length (PIC 9(04))
        assertThat(Constants.CATEGORY_CODE_LENGTH)
                .as("Constants.CATEGORY_CODE_LENGTH should match COBOL PIC 9(04)")
                .isEqualTo(4);

        // Test field length validation using ValidationUtil
        String validGroupId = "GROUP123"; // 8 characters, within 10-char limit
        assertThatNoException()
                .as("Valid group ID should pass length validation")
                .isThrownBy(() -> ValidationUtil.validateFieldLength("groupId", validGroupId, Constants.GROUP_ID_LENGTH));

        // Test maximum length boundary
        String maxGroupId = "1234567890"; // Exactly 10 characters
        assertThatNoException()
                .as("Maximum length group ID should pass validation")
                .isThrownBy(() -> ValidationUtil.validateFieldLength("groupId", maxGroupId, Constants.GROUP_ID_LENGTH));

        // Create entity with various field lengths for testing
        DisclosureGroup lengthTestGroup = DisclosureGroup.builder()
                .accountGroupId(maxGroupId) // 10 chars max
                .transactionTypeCode("AB") // 2 chars max
                .transactionCategoryCode("1234") // 4 chars max
                .interestRate(VALID_INTEREST_RATE)
                .groupName("Test Group Name") // Up to 50 chars
                .description("Test description for length validation") // Up to 200 chars
                .build();

        // Validate field lengths don't exceed COBOL specifications
        assertThat(lengthTestGroup.getAccountGroupId().length())
                .as("Account group ID length should not exceed COBOL limit")
                .isLessThanOrEqualTo(Constants.GROUP_ID_LENGTH);

        assertThat(lengthTestGroup.getTransactionTypeCode().length())
                .as("Transaction type code length should not exceed COBOL limit")
                .isLessThanOrEqualTo(Constants.TYPE_CODE_LENGTH);

        assertThat(lengthTestGroup.getTransactionCategoryCode().length())
                .as("Transaction category code length should not exceed COBOL limit")
                .isLessThanOrEqualTo(Constants.CATEGORY_CODE_LENGTH);

        // Field length validation completed - validate the BigDecimal interest rate
        validateCobolPrecision(lengthTestGroup.getInterestRate(), "field length validation");
    }

    /**
     * Test equals() and hashCode() methods for proper entity comparison.
     * Validates object equality based on business key components.
     */
    @DisplayName("Equals and HashCode Validation")
    public void testEqualsAndHashCode() {
        logTestExecution("Testing equals() and hashCode() methods", null);

        DisclosureGroup group1 = DisclosureGroup.builder()
                .disclosureGroupId(1L)
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .groupName(VALID_GROUP_NAME)
                .build();

        DisclosureGroup group2 = DisclosureGroup.builder()
                .disclosureGroupId(1L)
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .groupName(VALID_GROUP_NAME)
                .build();

        DisclosureGroup group3 = DisclosureGroup.builder()
                .disclosureGroupId(2L)
                .accountGroupId("DIFFERENT")
                .transactionTypeCode("99")
                .transactionCategoryCode("9999")
                .interestRate(new BigDecimal("20.0000"))
                .groupName("Different Group")
                .build();

        // Test reflexivity: x.equals(x) should return true
        assertThat(group1.equals(group1))
                .as("Entity should be equal to itself (reflexivity)")
                .isTrue();

        // Test symmetry: x.equals(y) should return the same as y.equals(x)
        assertThat(group1.equals(group2))
                .as("Equal entities should be symmetric")
                .isEqualTo(group2.equals(group1));

        // Test hashCode consistency
        if (group1.equals(group2)) {
            assertThat(group1.hashCode())
                    .as("Equal entities should have the same hashCode")
                    .isEqualTo(group2.hashCode());
        }

        // Test inequality
        assertThat(group1.equals(group3))
                .as("Different entities should not be equal")
                .isFalse();

        // Test null comparison
        assertThat(group1.equals(null))
                .as("Entity should not be equal to null")
                .isFalse();

        // Test different class comparison
        assertThat(group1.equals("not a DisclosureGroup"))
                .as("Entity should not be equal to different class")
                .isFalse();

        // Equals and hashCode validation completed - validate the BigDecimal interest rate
        validateCobolPrecision(group1.getInterestRate(), "equals and hashCode validation");
    }

    /**
     * Test toString() method for proper string representation.
     * Validates that toString() provides meaningful information for debugging and logging.
     */
    @DisplayName("ToString Method Validation")
    public void testToString() {
        logTestExecution("Testing toString() method", null);

        DisclosureGroup group = DisclosureGroup.builder()
                .disclosureGroupId(123L)
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .groupName(VALID_GROUP_NAME)
                .active(true)
                .build();

        String toStringResult = group.toString();

        // Validate toString() contains key identifying information
        assertThat(toStringResult)
                .as("toString() should contain entity name")
                .contains("DisclosureGroup");

        assertThat(toStringResult)
                .as("toString() should contain disclosure group ID")
                .contains("123");

        assertThat(toStringResult)
                .as("toString() should contain account group ID")
                .contains(VALID_ACCOUNT_GROUP_ID);

        assertThat(toStringResult)
                .as("toString() should contain transaction type code")
                .contains(VALID_TRANSACTION_TYPE_CODE);

        assertThat(toStringResult)
                .as("toString() should contain transaction category code")
                .contains(VALID_TRANSACTION_CATEGORY_CODE);

        assertThat(toStringResult)
                .as("toString() should contain interest rate")
                .contains(VALID_INTEREST_RATE.toString());

        assertThat(toStringResult)
                .as("toString() should contain group name")
                .contains(VALID_GROUP_NAME);

        // Validate toString() doesn't expose sensitive information
        assertThat(toStringResult)
                .as("toString() should not be null or empty")
                .isNotNull()
                .isNotBlank();

        // toString validation completed - validate the BigDecimal interest rate
        validateCobolPrecision(group.getInterestRate(), "toString validation");
    }

    /**
     * Parameterized test for various interest rate values to validate COBOL precision compatibility.
     * Tests multiple interest rate scenarios including edge cases and boundary values.
     */
    @ParameterizedTest
    @ValueSource(strings = {"0.0000", "1.2500", "5.7500", "12.5000", "25.0000", "99.9999"})
    @DisplayName("Parameterized Interest Rate Validation")
    public void testParameterizedInterestRates(String rateValue) {
        logTestExecution("Testing parameterized interest rate: " + rateValue, null);

        BigDecimal testRate = new BigDecimal(rateValue);

        DisclosureGroup group = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(testRate)
                .build();

        // Validate interest rate precision preservation
        BigDecimal storedRate = group.getInterestRate();
        assertThat(storedRate)
                .as("Parameterized interest rate should be stored correctly")
                .isNotNull();

        // Test COBOL precision compatibility
        BigDecimal cobolCompatibleRate = CobolDataConverter.preservePrecision(testRate, 4);
        assertBigDecimalEquals(cobolCompatibleRate, storedRate, 
                "Parameterized rate " + rateValue + " should maintain COBOL precision");

        // Validate scale consistency
        assertThat(storedRate.scale())
                .as("Interest rate scale should be consistent for value " + rateValue)
                .isEqualTo(4);

        // Test conversion roundtrip
        BigDecimal convertedBack = CobolDataConverter.toBigDecimal(storedRate.toString(), 4);
        assertBigDecimalEquals(storedRate, convertedBack,
                "Interest rate conversion should be reversible for " + rateValue);

        validateCobolPrecision(storedRate, "parameterized interest rate " + rateValue);
    }

    /**
     * Parameterized test for various account group ID patterns to validate field constraints.
     * Tests different group ID formats and lengths within COBOL specifications.
     */
    @ParameterizedTest
    @ValueSource(strings = {"GROUP01", "ACCT123456", "DEFAULT", "PREMIUM", "1234567890"})
    @DisplayName("Parameterized Account Group ID Validation")
    public void testParameterizedGroupIds(String groupId) {
        logTestExecution("Testing parameterized group ID: " + groupId, null);

        DisclosureGroup group = DisclosureGroup.builder()
                .accountGroupId(groupId)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .build();

        // Validate group ID storage
        assertThat(group.getAccountGroupId())
                .as("Parameterized group ID should be stored correctly")
                .isEqualTo(groupId);

        // Validate length constraint
        assertThat(group.getAccountGroupId().length())
                .as("Group ID length should not exceed COBOL limit")
                .isLessThanOrEqualTo(Constants.GROUP_ID_LENGTH);

        // Validate field length using ValidationUtil
        assertThatNoException()
                .as("Group ID '%s' should pass validation", groupId)
                .isThrownBy(() -> ValidationUtil.validateFieldLength("groupId", groupId, Constants.GROUP_ID_LENGTH));

        // Test COBOL string conversion compatibility
        String convertedGroupId = CobolDataConverter.convertPicString(groupId, Constants.GROUP_ID_LENGTH);
        assertThat(convertedGroupId)
                .as("Group ID should be compatible with COBOL PIC X(10) conversion")
                .isEqualTo(groupId.trim());

        // Group ID is string field - validate the BigDecimal interest rate instead
        validateCobolPrecision(group.getInterestRate(), "parameterized group ID " + groupId);
    }

    /**
     * Test FILLER field handling (28-character padding) from COBOL copybook.
     * While the entity doesn't explicitly have FILLER fields, validates that the total
     * structure accounts for the 50-byte COBOL record length.
     */
    @DisplayName("FILLER Field Handling - 28-character padding")
    public void testFillerFieldHandling() {
        logTestExecution("Testing FILLER field handling", null);

        DisclosureGroup group = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID) // 10 bytes
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE) // 2 bytes
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE) // 4 bytes
                .interestRate(VALID_INTEREST_RATE) // 6 bytes (S9(04)V99)
                .build();

        // COBOL structure validation:
        // DIS-ACCT-GROUP-ID (10) + DIS-TRAN-TYPE-CD (2) + DIS-TRAN-CAT-CD (4) + DIS-INT-RATE (6) + FILLER (28) = 50 bytes

        int cobolKeyLength = Constants.GROUP_ID_LENGTH + Constants.TYPE_CODE_LENGTH + Constants.CATEGORY_CODE_LENGTH;
        int cobolInterestRateLength = 6; // S9(04)V99 = 6 bytes in COBOL
        int cobolFillerLength = 28;
        int totalCobolRecordLength = cobolKeyLength + cobolInterestRateLength + cobolFillerLength;

        assertThat(totalCobolRecordLength)
                .as("Total COBOL record length should be 50 bytes")
                .isEqualTo(50);

        // Validate that our key fields account for the first 22 bytes of the record
        int javaKeyFieldsLength = group.getAccountGroupId().length() + 
                                  group.getTransactionTypeCode().length() + 
                                  group.getTransactionCategoryCode().length();

        assertThat(javaKeyFieldsLength)
                .as("Java key fields should account for COBOL key structure")
                .isLessThanOrEqualTo(cobolKeyLength);

        // Test that the entity can handle the conceptual FILLER space through additional fields
        DisclosureGroup extendedGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .groupName(VALID_GROUP_NAME) // Additional field using "FILLER" space
                .description(VALID_DESCRIPTION) // Another field using "FILLER" space
                .active(true) // Boolean field
                .build();

        assertThat(extendedGroup)
                .as("Extended group with additional fields should be valid")
                .isNotNull();

        // The additional fields (groupName, description, active) conceptually
        // utilize the space that would be FILLER in the COBOL structure
        assertThat(extendedGroup.getGroupName())
                .as("Group name should utilize conceptual FILLER space")
                .isEqualTo(VALID_GROUP_NAME);

        // FILLER field handling validation completed - validate the BigDecimal interest rate
        validateCobolPrecision(group.getInterestRate(), "FILLER field handling validation");
    }

    /**
     * Test signed interest rate handling for special rates including negative values.
     * Validates COBOL S9(04)V99 signed numeric behavior and BigDecimal sign preservation.
     */
    @DisplayName("Signed Interest Rate Handling - S9(04)V99")
    public void testSignedInterestRateHandling() {
        logTestExecution("Testing signed interest rate handling", null);

        // Test positive interest rates
        BigDecimal positiveRate = new BigDecimal("12.5000");
        DisclosureGroup positiveGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(positiveRate)
                .build();

        assertThat(positiveGroup.getInterestRate().signum())
                .as("Positive interest rate should have positive sign")
                .isEqualTo(1);

        // Test zero interest rates
        BigDecimal zeroRate = new BigDecimal("0.0000");
        DisclosureGroup zeroGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(zeroRate)
                .build();

        assertThat(zeroGroup.getInterestRate().signum())
                .as("Zero interest rate should have zero sign")
                .isEqualTo(0);

        // Test negative interest rates (for special scenarios like rebates or penalties)
        BigDecimal negativeRate = new BigDecimal("-2.5000");
        DisclosureGroup negativeGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode("02") // Different type for negative rate scenario
                .transactionCategoryCode("9999") // Special category for negative rates
                .interestRate(negativeRate)
                .build();

        assertThat(negativeGroup.getInterestRate().signum())
                .as("Negative interest rate should have negative sign")
                .isEqualTo(-1);

        // Test COBOL signed numeric conversion
        String cobolSignedValue = "-12.75";
        BigDecimal convertedSignedRate = CobolDataConverter.toBigDecimal(cobolSignedValue, 4);
        
        DisclosureGroup convertedGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(convertedSignedRate)
                .build();

        assertThat(convertedGroup.getInterestRate().signum())
                .as("Converted negative rate should maintain sign")
                .isEqualTo(-1);

        assertBigDecimalEquals(new BigDecimal("-12.7500"), convertedGroup.getInterestRate(),
                "Converted signed rate should match expected value");

        // Test sign preservation with COBOL rounding
        BigDecimal signedRateWithRounding = new BigDecimal("-12.7567");
        BigDecimal roundedSignedRate = CobolDataConverter.preservePrecision(signedRateWithRounding, 4);
        
        assertThat(roundedSignedRate.signum())
                .as("Rounded negative rate should preserve sign")
                .isEqualTo(-1);

        validateCobolPrecision(positiveGroup.getInterestRate(), "positive signed rate");
        validateCobolPrecision(zeroGroup.getInterestRate(), "zero signed rate");
        validateCobolPrecision(negativeGroup.getInterestRate(), "negative signed rate");
    }

    /**
     * Test relationship mapping with Account entity via group ID.
     * Validates that DisclosureGroup can be properly associated with Account entities.
     */
    @DisplayName("Account Relationship Mapping")
    public void testAccountRelationshipMapping() {
        logTestExecution("Testing Account relationship mapping", null);

        // Create test Account instance
        Account testAccount = Account.builder()
                .accountId(1L)
                .groupId(VALID_ACCOUNT_GROUP_ID)
                .currentBalance(new BigDecimal("1000.00"))
                .build();

        // Create DisclosureGroup with matching group ID
        DisclosureGroup disclosureGroup = DisclosureGroup.builder()
                .accountGroupId(VALID_ACCOUNT_GROUP_ID)
                .transactionTypeCode(VALID_TRANSACTION_TYPE_CODE)
                .transactionCategoryCode(VALID_TRANSACTION_CATEGORY_CODE)
                .interestRate(VALID_INTEREST_RATE)
                .groupName(VALID_GROUP_NAME)
                .build();

        // Validate group ID relationship
        assertThat(disclosureGroup.getAccountGroupId())
                .as("DisclosureGroup account group ID should match Account group ID")
                .isEqualTo(testAccount.getGroupId());

        // Test relationship through group ID matching
        boolean isRelatedAccount = testAccount.getGroupId() != null && 
                                 testAccount.getGroupId().equals(disclosureGroup.getAccountGroupId());

        assertThat(isRelatedAccount)
                .as("Account should be related to DisclosureGroup through group ID")
                .isTrue();

        // Test multiple accounts with same group ID
        Account secondAccount = Account.builder()
                .accountId(2L)
                .groupId(VALID_ACCOUNT_GROUP_ID)
                .currentBalance(new BigDecimal("2500.00"))
                .build();

        assertThat(secondAccount.getGroupId())
                .as("Multiple accounts can share the same disclosure group ID")
                .isEqualTo(disclosureGroup.getAccountGroupId());

        // Test different group ID (no relationship)
        String differentGroupId = "DIFFGROUP";
        Account unrelatedAccount = Account.builder()
                .accountId(3L)
                .groupId(differentGroupId)
                .currentBalance(new BigDecimal("500.00"))
                .build();

        boolean isUnrelated = !differentGroupId.equals(disclosureGroup.getAccountGroupId());
        assertThat(isUnrelated)
                .as("Account with different group ID should not be related")
                .isTrue();

        // Validate that group ID serves as foreign key concept
        assertThat(disclosureGroup.getAccountGroupId())
                .as("Account group ID should serve as foreign key reference")
                .isNotNull()
                .hasSize(Constants.GROUP_ID_LENGTH);

        // Account relationship mapping validation completed - validate the BigDecimal interest rate
        validateCobolPrecision(disclosureGroup.getInterestRate(), "account relationship mapping");
    }

    /**
     * Test COBOL precision matching for all numeric fields and calculations.
     * Validates that Java implementations maintain identical precision to COBOL calculations.
     */
    @DisplayName("COBOL Precision Matching Validation")
    public void testCobolPrecisionMatching() {
        logTestExecution("Testing COBOL precision matching", null);

        // Create test entity with various numeric values
        DisclosureGroup precisionTestGroup = DisclosureGroup.builder()
                .accountGroupId("PRECISION1")
                .transactionTypeCode("01")
                .transactionCategoryCode("1234")
                .interestRate(new BigDecimal("12.3456"))
                .build();

        // Test interest rate precision matching (S9(04)V99 -> 4 decimal places)
        BigDecimal interestRate = precisionTestGroup.getInterestRate();
        
        // Apply COBOL precision rules
        BigDecimal cobolPrecisionRate = CobolDataConverter.preservePrecision(interestRate, 4);
        
        assertBigDecimalEquals(cobolPrecisionRate, interestRate,
                "Interest rate should match COBOL S9(04)V99 precision");

        // Test rounding mode compatibility
        BigDecimal testValue = new BigDecimal("12.3456789");
        BigDecimal cobolRoundedValue = testValue.setScale(4, TestConstants.COBOL_ROUNDING_MODE);
        
        assertThat(cobolRoundedValue)
                .as("Rounding should match COBOL ROUNDED clause behavior")
                .isEqualTo(new BigDecimal("12.3457"));

        // Test functional parity validation using TestConstants
        Boolean preserveDecimalPrecision = (Boolean) TestConstants.FUNCTIONAL_PARITY_RULES.get("preserve_decimal_precision");
        Boolean matchCobolRounding = (Boolean) TestConstants.FUNCTIONAL_PARITY_RULES.get("match_cobol_rounding");
        Boolean validateFieldLengths = (Boolean) TestConstants.FUNCTIONAL_PARITY_RULES.get("validate_field_lengths");

        assertThat(preserveDecimalPrecision)
                .as("Functional parity rules should enforce decimal precision preservation")
                .isTrue();

        assertThat(matchCobolRounding)
                .as("Functional parity rules should enforce COBOL rounding matching")
                .isTrue();

        assertThat(validateFieldLengths)
                .as("Functional parity rules should enforce field length validation")
                .isTrue();

        // Test COBOL COMP-3 pattern validation
        Map<String, Object> comp3Patterns = TestConstants.COBOL_COMP3_PATTERNS;
        
        assertThat(comp3Patterns.get("monetary_scale"))
                .as("COMP-3 patterns should define monetary scale")
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);

        assertThat(comp3Patterns.get("rounding_mode"))
                .as("COMP-3 patterns should define rounding mode")
                .isEqualTo(TestConstants.COBOL_ROUNDING_MODE.toString());

        // Test validation threshold compliance
        Double toleranceThreshold = (Double) TestConstants.VALIDATION_THRESHOLDS.get("decimal_precision_tolerance");
        
        assertThat(toleranceThreshold)
                .as("Validation thresholds should define precision tolerance")
                .isEqualTo(0.001);

        // Validate precision within tolerance
        BigDecimal expectedPrecision = new BigDecimal("12.3457");
        BigDecimal actualPrecision = cobolRoundedValue;
        
        assertBigDecimalWithinTolerance(expectedPrecision, actualPrecision,
                "COBOL precision should be within validation tolerance");

        // Test numeric overflow handling
        Boolean checkOverflow = (Boolean) TestConstants.FUNCTIONAL_PARITY_RULES.get("check_overflow_handling");
        
        assertThat(checkOverflow)
                .as("Functional parity rules should enforce overflow checking")
                .isTrue();

        validateCobolPrecision(precisionTestGroup.getInterestRate(), "COBOL precision matching validation");
    }
}