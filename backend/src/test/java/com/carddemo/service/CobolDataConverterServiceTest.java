/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Comprehensive unit test class for CobolDataConverterService that validates COBOL data type
 * conversion utilities, ensuring exact precision and behavioral parity with mainframe COBOL.
 * 
 * This test suite covers critical conversion scenarios required for the CardDemo system 
 * migration from COBOL/CICS to Java/Spring Boot, including:
 * - COMP-3 packed decimal to BigDecimal conversions with exact precision
 * - PIC clause interpretation and formatting for various COBOL data types
 * - Sign handling for positive/negative values in different formats
 * - Decimal point positioning based on V clauses and scale specifications
 * - DISPLAY numeric formatting matching COBOL output behavior
 * - Error handling and edge cases for invalid data scenarios
 * 
 * All tests validate that conversions preserve COBOL precision and rounding behavior,
 * which is critical for financial calculations in the credit card management system.
 * The test scenarios are designed to match real-world COBOL data patterns found in
 * the legacy mainframe system.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CobolDataConverterService.class})
public class CobolDataConverterServiceTest {

    @Autowired
    private CobolDataConverterService cobolDataConverterService;

    /**
     * Tests COMP-3 packed decimal conversion to BigDecimal with various scales and precision scenarios.
     * 
     * COMP-3 (packed decimal) is a critical COBOL data type used extensively in financial calculations.
     * This test ensures that the Java conversion produces identical results to COBOL COMP-3 arithmetic.
     * 
     * Test scenarios cover:
     * - Positive and negative values
     * - Various scales (0, 2, 4 decimal places)
     * - Different byte lengths
     * - Boundary values and edge cases
     */
    @ParameterizedTest
    @CsvSource({
        // Format: packedHex, scale, expectedValue, description
        "'1234C', 2, 12.34, 'Positive 2-decimal COMP-3'",
        "'1234D', 2, -12.34, 'Negative 2-decimal COMP-3'", 
        "'000123C', 0, 123, 'Positive integer COMP-3'",
        "'000123D', 0, -123, 'Negative integer COMP-3'",
        "'00000C', 2, 0.00, 'Zero value COMP-3'",
        "'999999C', 2, 9999.99, 'Maximum 4-digit COMP-3'",
        "'000001C', 4, 0.0001, 'Smallest positive 4-decimal'",
        "'123456789C', 2, 1234567.89, 'Large 7-digit COMP-3'"
    })
    void testFromComp3ToBigDecimal_ValidInputs(String packedHex, int scale, String expectedValue, String description) {
        // Convert hex string to byte array for COMP-3 data
        byte[] packedData = hexStringToByteArray(packedHex);
        BigDecimal expected = new BigDecimal(expectedValue);
        
        BigDecimal result = cobolDataConverterService.fromComp3ToBigDecimal(packedData, scale);
        
        assertThat(result).isNotNull();
        assertThat(result.compareTo(expected)).isEqualTo(0);
        assertThat(result.scale()).isEqualTo(scale);
        
        // Verify precision is preserved exactly as COBOL would handle it
        assertThat(result.stripTrailingZeros().scale()).isLessThanOrEqualTo(scale);
    }

    /**
     * Tests error handling for invalid COMP-3 packed decimal data.
     * 
     * COBOL COMP-3 has strict format requirements, and invalid data should be detected
     * and handled appropriately with clear error messages.
     */
    @Test
    void testFromComp3ToBigDecimal_InvalidInputs() {
        // Test null input
        assertThatThrownBy(() -> cobolDataConverterService.fromComp3ToBigDecimal(null, 2))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test empty array
        assertThatThrownBy(() -> cobolDataConverterService.fromComp3ToBigDecimal(new byte[0], 2))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test invalid sign nibble (not C or D)
        byte[] invalidSign = hexStringToByteArray("1234E");
        assertThatThrownBy(() -> cobolDataConverterService.fromComp3ToBigDecimal(invalidSign, 2))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test negative scale
        byte[] validData = hexStringToByteArray("1234C");
        assertThatThrownBy(() -> cobolDataConverterService.fromComp3ToBigDecimal(validData, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Tests PIC X (alphanumeric) string conversion with length validation and trimming.
     * 
     * COBOL PIC X fields are fixed-length and often contain trailing spaces that need
     * to be handled correctly during conversion to Java String objects.
     */
    @ParameterizedTest
    @CsvSource({
        "'HELLO     ', 10, 'HELLO', 'Trailing spaces trimmed'",
        "'WORLD', 10, 'WORLD', 'No padding needed'",
        "'', 5, '', 'Empty string'",
        "'   ', 5, '', 'Only spaces becomes empty'",
        "'TEST123', 10, 'TEST123', 'Alphanumeric content'",
        "'A', 1, 'A', 'Single character exact fit'"
    })
    void testConvertPicString_ValidInputs(String input, int maxLength, String expected, String description) {
        String result = cobolDataConverterService.convertPicString(input, maxLength);
        
        assertThat(result).isEqualTo(expected);
        assertThat(result.length()).isLessThanOrEqualTo(maxLength);
    }

    /**
     * Tests PIC X string conversion error handling for length violations.
     */
    @Test
    void testConvertPicString_InvalidInputs() {
        // Test string exceeding maximum length
        assertThatThrownBy(() -> cobolDataConverterService.convertPicString("TOOLONGSTRING", 5))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test negative maximum length
        assertThatThrownBy(() -> cobolDataConverterService.convertPicString("TEST", -1))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test null input with positive max length should be handled gracefully
        String result = cobolDataConverterService.convertPicString(null, 10);
        assertThat(result).isEmpty();
    }

    /**
     * Tests conversion to Java types based on COBOL PIC clause specifications.
     * 
     * This is the main conversion method that interprets various COBOL PIC clause formats
     * and converts data to appropriate Java types. Critical for maintaining data type
     * compatibility between COBOL and Java implementations.
     */
    @ParameterizedTest
    @CsvSource({
        "'HELLO', 'PIC X(10)', 'java.lang.String', 'Alphanumeric to String'",
        "'12345', 'PIC 9(5)', 'java.lang.Long', 'Unsigned numeric to Long'", 
        "'123.45', 'PIC S9(3)V99', 'java.math.BigDecimal', 'Signed decimal to BigDecimal'",
        "'-456.78', 'PIC S9(3)V99', 'java.math.BigDecimal', 'Negative decimal to BigDecimal'",
        "'0', 'PIC 9(1)', 'java.lang.Long', 'Single digit to Long'",
        "'999999999', 'PIC 9(9)', 'java.lang.Long', 'Large integer to Long'",
        "'00001.0000', 'PIC 9(5)V9(4)', 'java.math.BigDecimal', 'Zero-padded decimal'"
    })
    void testConvertToJavaType_ValidPicClauses(String inputValue, String picClause, String expectedClass, String description) {
        Object result = cobolDataConverterService.convertToJavaType(inputValue, picClause);
        
        assertThat(result).isNotNull();
        assertThat(result.getClass().getName()).isEqualTo(expectedClass);
        
        // Verify specific conversions based on type
        if (result instanceof String) {
            assertThat(result.toString()).isNotEmpty();
        } else if (result instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) result;
            assertThat(decimal.precision()).isGreaterThan(0);
        } else if (result instanceof Long) {
            Long longValue = (Long) result;
            assertThat(longValue).isNotNull();
        }
    }

    /**
     * Tests error handling for invalid PIC clause specifications.
     */
    @Test
    void testConvertToJavaType_InvalidPicClauses() {
        // Test invalid PIC clause format
        assertThatThrownBy(() -> cobolDataConverterService.convertToJavaType("123", "INVALID"))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test null PIC clause
        assertThatThrownBy(() -> cobolDataConverterService.convertToJavaType("123", null))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test empty PIC clause
        assertThatThrownBy(() -> cobolDataConverterService.convertToJavaType("123", ""))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test value that doesn't match PIC clause
        assertThatThrownBy(() -> cobolDataConverterService.convertToJavaType("ABC", "PIC 9(3)"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Tests precision preservation functionality that ensures BigDecimal values maintain
     * COBOL-compatible precision and rounding behavior.
     * 
     * This is critical for financial calculations where exact decimal precision must be
     * maintained to match COBOL computational results.
     */
    @ParameterizedTest
    @CsvSource({
        "123.456, 2, 123.46, 'Round to 2 decimal places'",
        "123.454, 2, 123.45, 'Round down at midpoint'", 
        "123.455, 2, 123.46, 'Round up at midpoint'",
        "100.000, 2, 100.00, 'Preserve trailing zeros'",
        "0.1, 4, 0.1000, 'Extend scale with zeros'",
        "999.999, 0, 1000, 'Round to integer'",
        "-123.456, 2, -123.46, 'Negative number rounding'"
    })
    void testPreservePrecision_VariousScales(String inputValue, int scale, String expectedValue, String description) {
        BigDecimal input = new BigDecimal(inputValue);
        BigDecimal expected = new BigDecimal(expectedValue);
        
        BigDecimal result = cobolDataConverterService.preservePrecision(input, scale);
        
        assertThat(result).isNotNull();
        assertThat(result.scale()).isEqualTo(scale);
        assertThat(result.compareTo(expected)).isEqualTo(0);
    }

    /**
     * Tests precision preservation with edge cases and null handling.
     */
    @Test
    void testPreservePrecision_EdgeCases() {
        // Test null input - should return zero with specified scale
        BigDecimal result = cobolDataConverterService.preservePrecision(null, 2);
        assertThat(result).isEqualTo(BigDecimal.ZERO.setScale(2));
        
        // Test zero input
        BigDecimal zero = BigDecimal.ZERO;
        result = cobolDataConverterService.preservePrecision(zero, 4);
        assertThat(result.scale()).isEqualTo(4);
        assertThat(result.compareTo(BigDecimal.ZERO)).isEqualTo(0);
        
        // Test very large number
        BigDecimal large = new BigDecimal("999999999.999999");
        result = cobolDataConverterService.preservePrecision(large, 2);
        assertThat(result.scale()).isEqualTo(2);
        assertThat(result.compareTo(new BigDecimal("1000000000.00"))).isEqualTo(0);
    }

    /**
     * Tests display formatting functionality for both currency and plain decimal formats.
     * 
     * This ensures that BigDecimal values are formatted correctly for user display,
     * matching the formatting behavior expected from COBOL DISPLAY statements.
     */
    @Test
    void testFormatToDisplay_CurrencyFormatting() {
        BigDecimal amount = new BigDecimal("1234.56");
        
        // Test US currency formatting
        String result = cobolDataConverterService.formatToDisplay(amount, Locale.US, true);
        assertThat(result).contains("$");
        assertThat(result).contains("1,234.56");
        
        // Test plain decimal formatting  
        result = cobolDataConverterService.formatToDisplay(amount, Locale.US, false);
        assertThat(result).isEqualTo("1234.56");
        
        // Test with null locale (should default appropriately)
        result = cobolDataConverterService.formatToDisplay(amount, null, false);
        assertThat(result).isNotNull();
        assertThat(result).contains("1234.56");
    }

    /**
     * Tests convenience formatting methods for common use cases.
     */
    @Test  
    void testFormatToDisplay_ConvenienceMethods() {
        BigDecimal amount = new BigDecimal("987.65");
        
        // Test default currency formatting (US locale)
        String currencyResult = cobolDataConverterService.formatToDisplay(amount);
        assertThat(currencyResult).contains("$");
        assertThat(currencyResult).contains("987.65");
        
        // Test plain decimal formatting
        String plainResult = cobolDataConverterService.formatToDisplayPlain(amount);
        assertThat(plainResult).isEqualTo("987.65");
        
        // Test with zero amount
        BigDecimal zero = BigDecimal.ZERO;
        currencyResult = cobolDataConverterService.formatToDisplay(zero);
        assertThat(currencyResult).contains("$");
        assertThat(currencyResult).contains("0.00");
    }

    /**
     * Tests formatting with null input handling.
     */
    @Test
    void testFormatToDisplay_NullHandling() {
        // Test null input - should format as zero
        String result = cobolDataConverterService.formatToDisplay(null);
        assertThat(result).contains("$");
        assertThat(result).contains("0.00");
        
        result = cobolDataConverterService.formatToDisplayPlain(null);
        assertThat(result).isEqualTo("0.00");
    }

    /**
     * Tests COBOL field validation functionality that ensures data conforms to
     * COBOL field specifications before conversion.
     */
    @ParameterizedTest
    @CsvSource({
        "'HELLO', 'PIC X(10)', true, 'Valid alphanumeric field'",
        "'12345', 'PIC 9(5)', true, 'Valid numeric field'",
        "'123.45', 'PIC S9(3)V99', true, 'Valid signed decimal'",
        "'TOOLONG', 'PIC X(5)', false, 'String too long for field'",
        "'ABC', 'PIC 9(3)', false, 'Non-numeric in numeric field'",
        "'', 'PIC X(10)', true, 'Empty string in alphanumeric field'"
    })
    void testValidateCobolField_VariousInputs(String value, String picClause, boolean expectedValid, String description) {
        boolean result = cobolDataConverterService.validateCobolField(value, picClause);
        assertThat(result).isEqualTo(expectedValid);
    }

    /**
     * Tests batch conversion functionality for processing multiple COBOL fields
     * efficiently in a single transaction.
     */
    @Test
    void testConvertMultipleFields_ValidInputs() {
        Object[] values = {"HELLO", "12345", "123.45"};
        String[] picClauses = {"PIC X(10)", "PIC 9(5)", "PIC S9(3)V99"};
        
        Object[] results = cobolDataConverterService.convertMultipleFields(values, picClauses);
        
        assertThat(results).hasSize(3);
        assertThat(results[0]).isInstanceOf(String.class);
        assertThat(results[1]).isInstanceOf(Long.class);
        assertThat(results[2]).isInstanceOf(BigDecimal.class);
        
        assertThat(results[0].toString()).isEqualTo("HELLO");
        assertThat((Long) results[1]).isEqualTo(12345L);
        assertThat(((BigDecimal) results[2]).compareTo(new BigDecimal("123.45"))).isEqualTo(0);
    }

    /**
     * Tests batch conversion error handling.
     */
    @Test
    void testConvertMultipleFields_InvalidInputs() {
        // Test null arrays
        assertThatThrownBy(() -> cobolDataConverterService.convertMultipleFields(null, new String[]{"PIC X(5)"}))
            .isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> cobolDataConverterService.convertMultipleFields(new Object[]{"TEST"}, null))
            .isInstanceOf(IllegalArgumentException.class);
        
        // Test mismatched array lengths
        Object[] values = {"TEST1", "TEST2"};
        String[] picClauses = {"PIC X(5)"};
        assertThatThrownBy(() -> cobolDataConverterService.convertMultipleFields(values, picClauses))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Helper method to convert hex string representation to byte array for COMP-3 testing.
     * 
     * This utility converts hex strings like "1234C" to byte arrays for testing COMP-3
     * packed decimal conversion. Each pair of hex digits becomes one byte.
     * 
     * @param hexString hex representation of packed decimal data
     * @return byte array containing the packed decimal data
     */
    private byte[] hexStringToByteArray(String hexString) {
        // Pad with leading zero if odd length (common in COMP-3 with sign nibble)
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }
        
        int length = hexString.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                 + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}
