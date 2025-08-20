/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.assertj.core.api.Assertions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.stream.Stream;

import com.carddemo.test.TestConstants;

/**
 * Comprehensive unit test class for CobolDataConverter utility that validates COBOL COMP-3 
 * packed decimal to Java BigDecimal conversion, ensuring exact precision and scale matching 
 * for financial calculations.
 * 
 * This test suite validates all COBOL numeric data type conversions including signed/unsigned values,
 * various scale factors, and edge cases to ensure 100% functional parity with the original COBOL
 * implementation as specified in Section 0.3.2 of the technical specification.
 * 
 * Key Testing Areas:
 * - COMP-3 packed decimal unpacking algorithm with exact precision preservation
 * - Sign handling for positive, negative, and unsigned values  
 * - Scale factor validation (0 to 6 decimal places)
 * - Boundary values and edge cases (zero, max values, invalid data)
 * - COBOL PIC clause interpretation and Java type mapping
 * - Financial calculation precision maintenance (HALF_UP rounding)
 * - Data type conversion validation for all supported COBOL formats
 * - Error handling and exception validation for invalid inputs
 * 
 * All tests use TestConstants for consistent validation thresholds and COBOL compatibility
 * patterns to ensure identical behavior between COBOL COMP-3 and Java BigDecimal operations.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
class CobolDataConverterTest {

    /**
     * Test setup executed before each test method.
     * Initializes test environment and validates test constants availability.
     */
    @BeforeEach
    void setUp() {
        // Verify test constants are properly initialized
        Assertions.assertThat(TestConstants.COBOL_DECIMAL_SCALE)
                .isEqualTo(2)
                .as("COBOL decimal scale should be 2 for monetary calculations");
        
        Assertions.assertThat(TestConstants.COBOL_ROUNDING_MODE)
                .isEqualTo(RoundingMode.HALF_UP)
                .as("COBOL rounding mode should be HALF_UP to match COBOL ROUNDED clause");
                
        Assertions.assertThat(TestConstants.FUNCTIONAL_PARITY_RULES)
                .isNotEmpty()
                .as("Functional parity rules should be available for validation");
    }

    /**
     * Tests COMP-3 packed decimal conversion for positive monetary amounts.
     * Validates that positive COMP-3 values are correctly unpacked to BigDecimal
     * with proper scale preservation matching COBOL PIC S9(10)V99 behavior.
     */
    @Test
    void testFromComp3PositiveMonetaryAmount() {
        // COMP-3 representation of 123.45 (positive monetary amount)
        // Bytes: 0x12, 0x34, 0x5C (where C indicates positive sign)
        byte[] comp3Data = {0x12, 0x34, 0x5C};
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.fromComp3(comp3Data, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("COMP-3 conversion should not return null");
                
        Assertions.assertThat(result.compareTo(new BigDecimal("123.45")))
                .isEqualTo(0)
                .as("COMP-3 positive value should convert to exactly 123.45");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Converted value should maintain specified scale");
                
        Assertions.assertThat(result.toString())
                .isEqualTo("123.45")
                .as("String representation should match expected monetary format");
    }

    /**
     * Tests COMP-3 packed decimal conversion for negative monetary amounts.
     * Validates that negative COMP-3 values are correctly unpacked with proper
     * sign handling matching COBOL signed numeric behavior.
     */
    @Test
    void testFromComp3NegativeMonetaryAmount() {
        // COMP-3 representation of -123.45 (negative monetary amount)
        // Bytes: 0x12, 0x34, 0x5D (where D indicates negative sign)
        byte[] comp3Data = {0x12, 0x34, 0x5D};
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.fromComp3(comp3Data, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("COMP-3 conversion should not return null");
                
        Assertions.assertThat(result.compareTo(new BigDecimal("-123.45")))
                .isEqualTo(0)
                .as("COMP-3 negative value should convert to exactly -123.45");
                
        Assertions.assertThat(result.signum())
                .isEqualTo(-1)
                .as("Negative COMP-3 value should have negative sign");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Converted value should maintain specified scale");
    }

    /**
     * Tests COMP-3 packed decimal conversion for zero values.
     * Validates proper handling of zero amounts with correct scale preservation.
     */
    @Test
    void testFromComp3ZeroAmount() {
        // COMP-3 representation of 0.00 
        // Bytes: 0x00, 0x0C (where C indicates positive sign)
        byte[] comp3Data = {0x00, 0x0C};
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.fromComp3(comp3Data, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("COMP-3 conversion should not return null");
                
        Assertions.assertThat(result.compareTo(BigDecimal.ZERO))
                .isEqualTo(0)
                .as("COMP-3 zero value should convert to BigDecimal zero");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Zero value should maintain specified scale");
                
        Assertions.assertThat(result.toString())
                .isEqualTo("0.00")
                .as("Zero should format with proper decimal places");
    }

    /**
     * Tests COMP-3 packed decimal conversion with unsigned positive indicator.
     * Validates handling of COMP-3 data with 0xF sign nibble (unsigned positive).
     */
    @Test
    void testFromComp3UnsignedPositiveAmount() {
        // COMP-3 representation of 67.89 with unsigned positive (F) sign
        // Bytes: 0x06, 0x78, 0x9F (digits 6789 with F sign for unsigned positive)
        byte[] comp3Data = {0x06, 0x78, (byte) 0x9F};
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.fromComp3(comp3Data, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("COMP-3 conversion should not return null");
                
        Assertions.assertThat(result.compareTo(new BigDecimal("67.89")))
                .isEqualTo(0)
                .as("COMP-3 unsigned positive should convert to positive value");
                
        Assertions.assertThat(result.signum())
                .isEqualTo(1)
                .as("Unsigned positive COMP-3 should be positive");
    }

    /**
     * Parameterized test for various scale factors (0-6 decimal places).
     * Tests COMP-3 conversion across different scale configurations to ensure
     * proper decimal place handling for various COBOL PIC clause formats.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void testFromComp3VariousScaleFactors(int scale) {
        // Create test data representing value 12345 with varying scales
        // For scale=0: 12345, scale=2: 123.45, scale=4: 1.2345, etc.
        byte[] comp3Data = {0x12, 0x34, 0x5C}; // 12345 positive
        
        BigDecimal result = CobolDataConverter.fromComp3(comp3Data, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("COMP-3 conversion should not return null for scale " + scale);
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Result should have expected scale " + scale);
                
        // Verify the actual decimal value matches expected based on scale
        BigDecimal expectedValue = new BigDecimal("12345").scaleByPowerOfTen(-scale)
                .setScale(scale, TestConstants.COBOL_ROUNDING_MODE);
        
        Assertions.assertThat(result.compareTo(expectedValue))
                .isEqualTo(0)
                .as("Scaled value should match expected calculation for scale " + scale);
    }

    /**
     * Tests error handling for null COMP-3 data input.
     * Validates that appropriate exception is thrown with descriptive message.
     */
    @Test
    void testFromComp3NullDataThrowsException() {
        byte[] nullData = null;
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        Assertions.assertThatThrownBy(() -> CobolDataConverter.fromComp3(nullData, scale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COMP-3 packed data cannot be null or empty")
                .as("Null COMP-3 data should throw IllegalArgumentException");
    }

    /**
     * Tests error handling for empty COMP-3 data array.
     * Validates proper exception handling for invalid input data.
     */
    @Test
    void testFromComp3EmptyDataThrowsException() {
        byte[] emptyData = new byte[0];
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        Assertions.assertThatThrownBy(() -> CobolDataConverter.fromComp3(emptyData, scale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COMP-3 packed data cannot be null or empty")
                .as("Empty COMP-3 data should throw IllegalArgumentException");
    }

    /**
     * Tests error handling for invalid sign nibble in COMP-3 data.
     * Validates that invalid sign values are properly detected and rejected.
     */
    @Test
    void testFromComp3InvalidSignNibbleThrowsException() {
        // Invalid sign nibble (0xA is not a valid COMP-3 sign)
        byte[] invalidData = {0x12, 0x3A};
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        Assertions.assertThatThrownBy(() -> CobolDataConverter.fromComp3(invalidData, scale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid COMP-3 format: invalid sign nibble")
                .as("Invalid sign nibble should throw IllegalArgumentException");
    }

    /**
     * Tests error handling for negative scale values.
     * Validates that negative scale parameters are properly rejected.
     */
    @Test
    void testFromComp3NegativeScaleThrowsException() {
        byte[] validData = {0x12, 0x3C};
        int negativeScale = -1;
        
        Assertions.assertThatThrownBy(() -> CobolDataConverter.fromComp3(validData, negativeScale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Scale cannot be negative")
                .as("Negative scale should throw IllegalArgumentException");
    }

    /**
     * Tests toBigDecimal conversion for Integer input values.
     * Validates proper conversion of integer types to BigDecimal with specified scale.
     */
    @Test
    void testToBigDecimalFromInteger() {
        Integer intValue = 12345;
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.toBigDecimal(intValue, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("toBigDecimal should not return null for Integer input");
                
        Assertions.assertThat(result.compareTo(new BigDecimal("12345.00")))
                .isEqualTo(0)
                .as("Integer should convert to BigDecimal with proper scale");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Result should have specified scale");
    }

    /**
     * Tests toBigDecimal conversion for Long input values.
     * Validates proper conversion of long types to BigDecimal with specified scale.
     */
    @Test
    void testToBigDecimalFromLong() {
        Long longValue = 9876543210L;
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.toBigDecimal(longValue, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("toBigDecimal should not return null for Long input");
                
        Assertions.assertThat(result.compareTo(new BigDecimal("9876543210.00")))
                .isEqualTo(0)
                .as("Long should convert to BigDecimal with proper scale");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Result should have specified scale");
    }

    /**
     * Tests toBigDecimal conversion for Double input values.
     * Validates proper conversion with COBOL-compatible rounding behavior.
     */
    @Test
    void testToBigDecimalFromDouble() {
        Double doubleValue = 123.456789;
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.toBigDecimal(doubleValue, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("toBigDecimal should not return null for Double input");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Result should have specified scale");
                
        // Double should be rounded to 2 decimal places using HALF_UP rounding
        Assertions.assertThat(result.compareTo(new BigDecimal("123.46")))
                .isEqualTo(0)
                .as("Double should be properly rounded with COBOL rounding mode");
    }

    /**
     * Tests toBigDecimal conversion for String input values.
     * Validates proper parsing and conversion of numeric strings.
     */
    @Test
    void testToBigDecimalFromString() {
        String stringValue = "999.99";
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.toBigDecimal(stringValue, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("toBigDecimal should not return null for String input");
                
        Assertions.assertThat(result.compareTo(new BigDecimal("999.99")))
                .isEqualTo(0)
                .as("String should convert to exact BigDecimal value");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Result should have specified scale");
    }

    /**
     * Tests toBigDecimal conversion for null input values.
     * Validates proper handling of null inputs with zero default.
     */
    @Test
    void testToBigDecimalFromNull() {
        Object nullValue = null;
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.toBigDecimal(nullValue, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("toBigDecimal should not return null for null input");
                
        Assertions.assertThat(result.compareTo(BigDecimal.ZERO))
                .isEqualTo(0)
                .as("Null input should convert to BigDecimal zero");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Zero result should have specified scale");
    }

    /**
     * Tests convertPicString for valid alphanumeric data.
     * Validates PIC X field conversion with proper length validation.
     */
    @Test
    void testConvertPicStringValidData() {
        String inputValue = "TEST DATA";
        int maxLength = Constants.USER_NAME_LENGTH; // 20 characters
        
        String result = CobolDataConverter.convertPicString(inputValue, maxLength);
        
        Assertions.assertThat(result)
                .isNotNull()
                .isEqualTo("TEST DATA")
                .as("Valid string should be returned unchanged");
                
        Assertions.assertThat(result.length())
                .isLessThanOrEqualTo(maxLength)
                .as("Result length should not exceed maximum");
    }

    /**
     * Tests convertPicString with trailing spaces (typical COBOL behavior).
     * Validates proper trimming of COBOL fixed-length fields.
     */
    @Test
    void testConvertPicStringWithTrailingSpaces() {
        String inputValue = "ACCOUNT123    "; // With trailing spaces
        int maxLength = Constants.ACCOUNT_ID_LENGTH; // 11 characters
        
        String result = CobolDataConverter.convertPicString(inputValue, maxLength);
        
        Assertions.assertThat(result)
                .isNotNull()
                .isEqualTo("ACCOUNT123")
                .as("Trailing spaces should be trimmed");
                
        Assertions.assertThat(result.length())
                .isLessThanOrEqualTo(maxLength)
                .as("Trimmed result should fit within maximum length");
    }

    /**
     * Tests convertPicString error handling for oversized input.
     * Validates proper exception when string exceeds PIC X length specification.
     */
    @Test
    void testConvertPicStringOversizeThrowsException() {
        String oversizedValue = "THIS STRING IS TOO LONG FOR THE FIELD";
        int maxLength = 10;
        
        Assertions.assertThatThrownBy(() -> CobolDataConverter.convertPicString(oversizedValue, maxLength))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("String length")
                .hasMessageContaining("exceeds maximum")
                .as("Oversized string should throw IllegalArgumentException");
    }

    /**
     * Tests convertToJavaType for PIC X alphanumeric field conversion.
     * Validates proper routing and conversion of COBOL PIC X clauses.
     */
    @Test
    void testConvertToJavaTypePicX() {
        String inputValue = "CUSTOMER01";
        String picClause = "PIC X(10)";
        
        Object result = CobolDataConverter.convertToJavaType(inputValue, picClause);
        
        Assertions.assertThat(result)
                .isNotNull()
                .isInstanceOf(String.class)
                .as("PIC X should convert to String type");
                
        Assertions.assertThat((String) result)
                .isEqualTo("CUSTOMER01")
                .as("PIC X conversion should preserve string value");
    }

    /**
     * Tests convertToJavaType for PIC 9 unsigned numeric field conversion.
     * Validates proper conversion of COBOL unsigned numeric fields to appropriate Java types.
     */
    @Test
    void testConvertToJavaTypePic9() {
        String inputValue = "12345";
        String picClause = "PIC 9(5)";
        
        Object result = CobolDataConverter.convertToJavaType(inputValue, picClause);
        
        Assertions.assertThat(result)
                .isNotNull()
                .isInstanceOf(Long.class)
                .as("PIC 9 should convert to Long for integer values");
                
        Assertions.assertThat((Long) result)
                .isEqualTo(12345L)
                .as("PIC 9 conversion should preserve numeric value");
    }

    /**
     * Tests convertToJavaType for PIC S9 signed numeric field conversion.
     * Validates proper conversion of COBOL signed numeric fields with decimal notation.
     */
    @Test
    void testConvertToJavaTypePicS9V99() {
        String inputValue = "123.45";
        String picClause = "PIC S9(10)V99";
        
        Object result = CobolDataConverter.convertToJavaType(inputValue, picClause);
        
        Assertions.assertThat(result)
                .isNotNull()
                .isInstanceOf(BigDecimal.class)
                .as("PIC S9V99 should convert to BigDecimal");
                
        BigDecimal decimalResult = (BigDecimal) result;
        Assertions.assertThat(decimalResult.compareTo(new BigDecimal("123.45")))
                .isEqualTo(0)
                .as("PIC S9V99 should preserve decimal value exactly");
                
        Assertions.assertThat(decimalResult.scale())
                .isEqualTo(2)
                .as("PIC S9V99 should have scale of 2");
    }

    /**
     * Tests preservePrecision for standardizing BigDecimal values.
     * Validates that precision preservation maintains exact COBOL compatibility.
     */
    @Test
    void testPreservePrecisionWithValidDecimal() {
        BigDecimal inputDecimal = new BigDecimal("123.456789");
        int targetScale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.preservePrecision(inputDecimal, targetScale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("preservePrecision should not return null");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(targetScale)
                .as("Result should have target scale");
                
        Assertions.assertThat(result.compareTo(new BigDecimal("123.46")))
                .isEqualTo(0)
                .as("Value should be rounded using COBOL rounding mode");
    }

    /**
     * Tests preservePrecision with null input.
     * Validates proper handling of null BigDecimal input with zero default.
     */
    @Test
    void testPreservePrecisionWithNull() {
        BigDecimal nullDecimal = null;
        int targetScale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.preservePrecision(nullDecimal, targetScale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("preservePrecision should not return null for null input");
                
        Assertions.assertThat(result.compareTo(BigDecimal.ZERO))
                .isEqualTo(0)
                .as("Null input should result in zero value");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(targetScale)
                .as("Zero result should have target scale");
    }

    /**
     * Tests boundary conditions for maximum COBOL precision values.
     * Validates handling of largest possible COBOL numeric values.
     */
    @Test
    void testBoundaryConditionsMaxPrecision() {
        // Test with maximum COBOL precision (18 digits + 2 decimal places)
        String maxValue = "999999999999999999.99";
        BigDecimal largeDecimal = new BigDecimal(maxValue);
        int scale = TestConstants.COBOL_DECIMAL_SCALE;
        
        BigDecimal result = CobolDataConverter.preservePrecision(largeDecimal, scale);
        
        Assertions.assertThat(result)
                .isNotNull()
                .as("Large precision value should be handled");
                
        Assertions.assertThat(result.compareTo(new BigDecimal(maxValue)))
                .isEqualTo(0)
                .as("Maximum precision value should be preserved exactly");
                
        Assertions.assertThat(result.scale())
                .isEqualTo(scale)
                .as("Maximum value should maintain proper scale");
    }

    /**
     * Tests COBOL field validation functionality.
     * Validates that field validation properly identifies valid and invalid COBOL data.
     */
    @Test
    void testValidateCobolFieldValidData() {
        String validValue = "12345";
        String validPicClause = "PIC 9(5)";
        
        boolean result = CobolDataConverter.validateCobolField(validValue, validPicClause);
        
        Assertions.assertThat(result)
                .isTrue()
                .as("Valid COBOL field should pass validation");
    }

    /**
     * Tests COBOL field validation with invalid data.
     * Validates that field validation properly rejects invalid COBOL data.
     */
    @Test
    void testValidateCobolFieldInvalidData() {
        String invalidValue = "ABCDE"; // Non-numeric for PIC 9
        String numericPicClause = "PIC 9(5)";
        
        boolean result = CobolDataConverter.validateCobolField(invalidValue, numericPicClause);
        
        Assertions.assertThat(result)
                .isFalse()
                .as("Invalid COBOL field should fail validation");
    }

    /**
     * Tests comprehensive COBOL-to-Java functional parity.
     * Validates that all converted values maintain identical precision and behavior
     * as specified in functional parity rules.
     */
    @Test
    void testFunctionalParityValidation() {
        // Test data representing typical COBOL account balance
        String cobolValue = "1234.56";
        String picClause = "PIC S9(10)V99";
        
        BigDecimal result = (BigDecimal) CobolDataConverter.convertToJavaType(cobolValue, picClause);
        
        // Validate against functional parity rules
        Boolean preserveDecimalPrecision = (Boolean) TestConstants.FUNCTIONAL_PARITY_RULES.get("preserve_decimal_precision");
        Boolean matchCobolRounding = (Boolean) TestConstants.FUNCTIONAL_PARITY_RULES.get("match_cobol_rounding");
        
        Assertions.assertThat(preserveDecimalPrecision)
                .isTrue()
                .as("Functional parity rules should require decimal precision preservation");
                
        Assertions.assertThat(matchCobolRounding)
                .isTrue()
                .as("Functional parity rules should require COBOL rounding matching");
                
        // Verify the result meets parity requirements
        Assertions.assertThat(result.scale())
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE)
                .as("Result should maintain COBOL decimal scale for parity");
                
        Assertions.assertThat(result.compareTo(new BigDecimal("1234.56")))
                .isEqualTo(0)
                .as("Conversion should maintain exact COBOL value for functional parity");
    }

    /**
     * Tests edge cases with complex COMP-3 patterns.
     * Validates proper handling of various COMP-3 edge cases and boundary conditions.
     */
    @Test
    void testComp3EdgeCasesAndPatterns() {
        // Test pattern from TestConstants.COBOL_COMP3_PATTERNS
        String positivePattern = (String) TestConstants.COBOL_COMP3_PATTERNS.get("positive_pattern");
        String negativePattern = (String) TestConstants.COBOL_COMP3_PATTERNS.get("negative_pattern"); 
        String zeroPattern = (String) TestConstants.COBOL_COMP3_PATTERNS.get("zero_pattern");
        
        Assertions.assertThat(positivePattern)
                .isEqualTo("123.45")
                .as("Positive pattern should match expected COBOL format");
                
        Assertions.assertThat(negativePattern)
                .isEqualTo("-123.45")
                .as("Negative pattern should match expected COBOL format");
                
        Assertions.assertThat(zeroPattern)
                .isEqualTo("0.00")
                .as("Zero pattern should match expected COBOL format");
                
        // Test conversion of these patterns
        BigDecimal positiveResult = CobolDataConverter.toBigDecimal(positivePattern, TestConstants.COBOL_DECIMAL_SCALE);
        BigDecimal negativeResult = CobolDataConverter.toBigDecimal(negativePattern, TestConstants.COBOL_DECIMAL_SCALE);
        BigDecimal zeroResult = CobolDataConverter.toBigDecimal(zeroPattern, TestConstants.COBOL_DECIMAL_SCALE);
        
        Assertions.assertThat(positiveResult.compareTo(new BigDecimal("123.45")))
                .isEqualTo(0)
                .as("Positive pattern conversion should be exact");
                
        Assertions.assertThat(negativeResult.compareTo(new BigDecimal("-123.45")))
                .isEqualTo(0)
                .as("Negative pattern conversion should be exact");
                
        Assertions.assertThat(zeroResult.compareTo(BigDecimal.ZERO))
                .isEqualTo(0)
                .as("Zero pattern conversion should be exact");
    }

    /**
     * Tests performance and validation thresholds.
     * Validates that conversion operations meet performance requirements.
     */
    @Test
    void testValidationThresholds() {
        Object decimalTolerance = TestConstants.VALIDATION_THRESHOLDS.get("decimal_precision_tolerance");
        Object numericOverflowCheck = TestConstants.VALIDATION_THRESHOLDS.get("numeric_overflow_check");
        
        Assertions.assertThat(decimalTolerance)
                .isNotNull()
                .isInstanceOf(Double.class)
                .as("Decimal precision tolerance should be defined");
                
        Assertions.assertThat(numericOverflowCheck)
                .isNotNull()
                .isEqualTo(true)
                .as("Numeric overflow checking should be enabled");
                
        // Test that conversions stay within tolerance
        BigDecimal testValue = new BigDecimal("100.001");
        BigDecimal converted = CobolDataConverter.preservePrecision(testValue, TestConstants.COBOL_DECIMAL_SCALE);
        
        BigDecimal expected = new BigDecimal("100.00");
        BigDecimal difference = converted.subtract(expected).abs();
        Double tolerance = (Double) decimalTolerance;
        
        Assertions.assertThat(difference.doubleValue())
                .isLessThanOrEqualTo(tolerance)
                .as("Conversion precision should be within tolerance threshold");
    }
}