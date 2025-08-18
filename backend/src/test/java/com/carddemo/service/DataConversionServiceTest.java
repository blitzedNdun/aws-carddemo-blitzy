/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.carddemo.service.DataConversionService;
import com.carddemo.util.CobolDataConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Comprehensive unit test class for DataConversionService that validates COBOL to Java data type conversions.
 * 
 * This test suite ensures 100% functional parity with the original COBOL implementation by validating:
 * - COMP-3 packed decimal to BigDecimal transformations with exact precision preservation
 * - EBCDIC to ASCII character encoding conversions for mainframe data migration
 * - Date validation logic equivalent to CSUTLDPY.cpy procedures
 * - Numeric precision handling for financial calculations
 * - Bulk data conversion operations for performance and consistency
 * - Edge cases, boundary values, and error handling scenarios
 * 
 * The test implementation follows the requirements specified in Section 0.1.2 of the technical
 * specification for maintaining "exact decimal precision matching in financial calculations"
 * and preserving COBOL data conversion behavior.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@DisplayName("DataConversionService Unit Tests")
class DataConversionServiceTest {

    private DataConversionService dataConversionService;
    
    // Test data constants for COMP-3 packed decimal testing
    private static final byte[] COMP3_POSITIVE_123_45 = {0x01, 0x23, 0x4C}; // 123.45 positive
    private static final byte[] COMP3_NEGATIVE_123_45 = {0x01, 0x23, 0x4D}; // 123.45 negative  
    private static final byte[] COMP3_UNSIGNED_123_45 = {0x01, 0x23, 0x4F}; // 123.45 unsigned
    private static final byte[] COMP3_ZERO = {0x00, 0x0C}; // 0.00
    private static final byte[] COMP3_MAX_VALUE = {0x99, 0x99, 0x99, 0x9C}; // 999999.99
    private static final byte[] COMP3_MIN_VALUE = {0x99, 0x99, 0x99, 0x9D}; // -999999.99
    
    // Test data constants for EBCDIC to ASCII conversion
    private static final byte[] EBCDIC_HELLO = {(byte)0xC8, (byte)0xC5, (byte)0xD3, (byte)0xD3, (byte)0xD6}; // "HELLO"
    private static final byte[] EBCDIC_NUMBERS = {(byte)0xF1, (byte)0xF2, (byte)0xF3, (byte)0xF4, (byte)0xF5}; // "12345"
    private static final byte[] EBCDIC_SPECIAL_CHARS = {(byte)0x4B, (byte)0x6B, (byte)0x50}; // ".,&"
    private static final byte[] EBCDIC_MIXED_CASE = {(byte)0xC1, (byte)0x81, (byte)0xC2, (byte)0x82}; // "AaBb"
    
    @BeforeEach
    void setUp() {
        dataConversionService = new DataConversionService();
    }

    @Nested
    @DisplayName("COMP-3 Packed Decimal Conversion Tests")
    class Comp3ConversionTests {

        @Test
        @DisplayName("Should convert positive COMP-3 packed decimal to BigDecimal with correct scale")
        void testConvertComp3ToBigDecimal_PositiveValue() {
            // Given
            String fieldName = "AMOUNT_FIELD";
            int scale = 2;
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(COMP3_POSITIVE_123_45, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal("123.45"))).isEqualTo(0);
            assertThat(result.scale()).isEqualTo(scale);
            assertThat(result.signum()).isEqualTo(1); // Positive
        }

        @Test
        @DisplayName("Should convert negative COMP-3 packed decimal with proper sign handling")
        void testConvertComp3ToBigDecimal_NegativeValue() {
            // Given
            String fieldName = "BALANCE_FIELD";
            int scale = 2;
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(COMP3_NEGATIVE_123_45, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal("-123.45"))).isEqualTo(0);
            assertThat(result.scale()).isEqualTo(scale);
            assertThat(result.signum()).isEqualTo(-1); // Negative
        }

        @Test
        @DisplayName("Should convert unsigned COMP-3 packed decimal as positive value")
        void testConvertComp3ToBigDecimal_UnsignedValue() {
            // Given
            String fieldName = "INTEREST_RATE";
            int scale = 2;
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(COMP3_UNSIGNED_123_45, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal("123.45"))).isEqualTo(0);
            assertThat(result.scale()).isEqualTo(scale);
            assertThat(result.signum()).isEqualTo(1); // Unsigned treated as positive
        }

        @Test
        @DisplayName("Should handle zero COMP-3 value correctly")
        void testConvertComp3ToBigDecimal_ZeroValue() {
            // Given
            String fieldName = "ZERO_AMOUNT";
            int scale = 2;
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(COMP3_ZERO, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(BigDecimal.ZERO)).isEqualTo(0);
            assertThat(result.scale()).isEqualTo(scale);
        }

        @Test
        @DisplayName("Should handle maximum COMP-3 value without overflow")
        void testConvertComp3ToBigDecimal_MaxValue() {
            // Given
            String fieldName = "MAX_AMOUNT";
            int scale = 2;
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(COMP3_MAX_VALUE, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal("999999.99"))).isEqualTo(0);
            assertThat(result.scale()).isEqualTo(scale);
        }

        @Test
        @DisplayName("Should handle minimum COMP-3 value without underflow")
        void testConvertComp3ToBigDecimal_MinValue() {
            // Given
            String fieldName = "MIN_AMOUNT";
            int scale = 2;
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(COMP3_MIN_VALUE, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal("-999999.99"))).isEqualTo(0);
            assertThat(result.scale()).isEqualTo(scale);
        }

        @Test
        @DisplayName("Should handle null packed data by returning zero with correct scale")
        void testConvertComp3ToBigDecimal_NullData() {
            // Given
            String fieldName = "NULL_FIELD";
            int scale = 2;
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(null, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(BigDecimal.ZERO)).isEqualTo(0);
            assertThat(result.scale()).isEqualTo(scale);
        }

        @Test
        @DisplayName("Should handle empty packed data by returning zero with correct scale")
        void testConvertComp3ToBigDecimal_EmptyData() {
            // Given
            String fieldName = "EMPTY_FIELD";
            int scale = 2;
            byte[] emptyData = new byte[0];
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(emptyData, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(BigDecimal.ZERO)).isEqualTo(0);
            assertThat(result.scale()).isEqualTo(scale);
        }

        @Test
        @DisplayName("Should throw exception for invalid COMP-3 format")
        void testConvertComp3ToBigDecimal_InvalidFormat() {
            // Given
            String fieldName = "INVALID_FIELD";
            int scale = 2;
            byte[] invalidData = {(byte)0xFF, (byte)0xFF, (byte)0xFF}; // Invalid sign nibble
            
            // When/Then
            assertThatThrownBy(() -> dataConversionService.convertComp3ToBigDecimal(invalidData, scale, fieldName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COMP-3 conversion failed");
        }

        @ParameterizedTest
        @DisplayName("Should handle various scale values correctly")
        @ValueSource(ints = {0, 1, 2, 3, 4, 5})
        void testConvertComp3ToBigDecimal_VariousScales(int scale) {
            // Given
            String fieldName = "SCALE_TEST_FIELD";
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(COMP3_POSITIVE_123_45, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.scale()).isEqualTo(scale);
            // Value should be adjusted based on scale
            if (scale == 2) {
                assertThat(result.compareTo(new BigDecimal("123.45"))).isEqualTo(0);
            }
        }
    }

    @Nested
    @DisplayName("EBCDIC to ASCII Conversion Tests")
    class EbcdicConversionTests {

        @Test
        @DisplayName("Should convert EBCDIC letters to ASCII correctly")
        void testConvertEbcdicToAscii_Letters() {
            // Given
            String fieldName = "NAME_FIELD";
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(EBCDIC_HELLO, fieldName);
            
            // Then
            assertThat(result).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("Should convert EBCDIC numbers to ASCII correctly")
        void testConvertEbcdicToAscii_Numbers() {
            // Given
            String fieldName = "ACCOUNT_NUMBER";
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(EBCDIC_NUMBERS, fieldName);
            
            // Then
            assertThat(result).isEqualTo("12345");
        }

        @Test
        @DisplayName("Should convert EBCDIC special characters to ASCII correctly")
        void testConvertEbcdicToAscii_SpecialCharacters() {
            // Given
            String fieldName = "SPECIAL_FIELD";
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(EBCDIC_SPECIAL_CHARS, fieldName);
            
            // Then
            assertThat(result).isEqualTo(".,&");
        }

        @Test
        @DisplayName("Should convert EBCDIC mixed case letters correctly")
        void testConvertEbcdicToAscii_MixedCase() {
            // Given
            String fieldName = "MIXED_CASE_FIELD";
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(EBCDIC_MIXED_CASE, fieldName);
            
            // Then
            assertThat(result).isEqualTo("AaBb");
        }

        @Test
        @DisplayName("Should handle null EBCDIC data by returning empty string")
        void testConvertEbcdicToAscii_NullData() {
            // Given
            String fieldName = "NULL_FIELD";
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(null, fieldName);
            
            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty EBCDIC data by returning empty string")
        void testConvertEbcdicToAscii_EmptyData() {
            // Given
            String fieldName = "EMPTY_FIELD";
            byte[] emptyData = new byte[0];
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(emptyData, fieldName);
            
            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should trim trailing spaces from converted ASCII string")
        void testConvertEbcdicToAscii_TrailingSpaces() {
            // Given
            String fieldName = "PADDED_FIELD";
            byte[] paddedData = {(byte)0xC8, (byte)0xC9, (byte)0x40, (byte)0x40}; // "HI  "
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(paddedData, fieldName);
            
            // Then
            assertThat(result).isEqualTo("HI");
        }

        @Test
        @DisplayName("Should handle unmapped EBCDIC characters gracefully")
        void testConvertEbcdicToAscii_UnmappedCharacters() {
            // Given
            String fieldName = "UNMAPPED_FIELD";
            byte[] unmappedData = {(byte)0x00, (byte)0x01, (byte)0x02}; // Control characters
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(unmappedData, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            // Should not throw exception even with unmapped characters
        }
    }

    @Nested
    @DisplayName("Date Validation and Conversion Tests")
    class DateValidationTests {

        @Test
        @DisplayName("Should validate and convert valid CCYYMMDD date")
        void testValidateAndConvertDate_ValidDate() {
            // Given
            String dateString = "20231225"; // Christmas 2023
            String fieldName = "BIRTH_DATE";
            
            // When
            LocalDate result = dataConversionService.validateAndConvertDate(dateString, fieldName);
            
            // Then
            assertThat(result).isEqualTo(LocalDate.of(2023, 12, 25));
        }

        @Test
        @DisplayName("Should validate century constraints (19xx and 20xx only)")
        void testValidateAndConvertDate_ValidCenturies() {
            // Test 19xx century
            LocalDate result1 = dataConversionService.validateAndConvertDate("19851231", "DATE_19XX");
            assertThat(result1).isEqualTo(LocalDate.of(1985, 12, 31));
            
            // Test 20xx century  
            LocalDate result2 = dataConversionService.validateAndConvertDate("20501231", "DATE_20XX");
            assertThat(result2).isEqualTo(LocalDate.of(2050, 12, 31));
        }

        @Test
        @DisplayName("Should reject invalid centuries (not 19 or 20)")
        void testValidateAndConvertDate_InvalidCentury() {
            // Given
            String invalidCentury = "18851231"; // 18xx century not allowed
            String fieldName = "INVALID_CENTURY";
            
            // When/Then
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate(invalidCentury, fieldName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Century is not valid");
        }

        @Test
        @DisplayName("Should validate month range (01-12)")
        void testValidateAndConvertDate_MonthValidation() {
            // Valid months should work
            assertThatCode(() -> dataConversionService.validateAndConvertDate("20230101", "JAN"))
                .doesNotThrowAnyException();
            assertThatCode(() -> dataConversionService.validateAndConvertDate("20231201", "DEC"))
                .doesNotThrowAnyException();
            
            // Invalid months should fail
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("20230001", "INVALID_MONTH"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Month must be a number between 1 and 12");
            
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("20231301", "INVALID_MONTH"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Month must be a number between 1 and 12");
        }

        @Test
        @DisplayName("Should validate day range (01-31)")
        void testValidateAndConvertDate_DayValidation() {
            // Valid days should work
            assertThatCode(() -> dataConversionService.validateAndConvertDate("20230101", "DAY_01"))
                .doesNotThrowAnyException();
            assertThatCode(() -> dataConversionService.validateAndConvertDate("20230131", "DAY_31"))
                .doesNotThrowAnyException();
            
            // Invalid days should fail
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("20230100", "INVALID_DAY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Day must be a number between 1 and 31");
            
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("20230132", "INVALID_DAY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Day must be a number between 1 and 31");
        }

        @Test
        @DisplayName("Should validate month-specific day limits")
        void testValidateAndConvertDate_MonthDayLimits() {
            // February 30th should fail
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("20230230", "FEB_30"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid for month");
            
            // April 31st should fail (30-day month)
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("20230431", "APR_31"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid for month");
        }

        @Test
        @DisplayName("Should handle leap year calculations correctly")
        void testValidateAndConvertDate_LeapYear() {
            // 2024 is a leap year - Feb 29 should be valid
            assertThatCode(() -> dataConversionService.validateAndConvertDate("20240229", "LEAP_YEAR"))
                .doesNotThrowAnyException();
            
            // 2023 is not a leap year - Feb 29 should fail
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("20230229", "NON_LEAP_YEAR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid for month");
            
            // 2000 was a leap year (divisible by 400)
            assertThatCode(() -> dataConversionService.validateAndConvertDate("20000229", "YEAR_2000"))
                .doesNotThrowAnyException();
            
            // 1900 was not a leap year (divisible by 100 but not 400)
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("19000229", "YEAR_1900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid for month");
        }

        @Test
        @DisplayName("Should reject null or empty date strings")
        void testValidateAndConvertDate_NullEmpty() {
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate(null, "NULL_DATE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
            
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("", "EMPTY_DATE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
            
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("   ", "BLANK_DATE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("Should reject malformed date strings")
        void testValidateAndConvertDate_MalformedDates() {
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("2023123", "TOO_SHORT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 digits in CCYYMMDD format");
            
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("202312345", "TOO_LONG"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 digits in CCYYMMDD format");
            
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("2023/12/25", "WITH_SLASHES"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 digits in CCYYMMDD format");
        }

        @ParameterizedTest
        @DisplayName("Should validate various valid date formats")
        @CsvSource({
            "20230101, 2023-01-01",
            "20231231, 2023-12-31", 
            "19800229, 1980-02-29",  // Leap year
            "20240229, 2024-02-29",  // Leap year
            "20230228, 2023-02-28",  // Non-leap year February
            "20230430, 2023-04-30",  // 30-day month
            "20230531, 2023-05-31"   // 31-day month
        })
        void testValidateAndConvertDate_ValidDates(String inputDate, String expectedDate) {
            LocalDate result = dataConversionService.validateAndConvertDate(inputDate, "VALID_DATE");
            assertThat(result).isEqualTo(LocalDate.parse(expectedDate));
        }
    }

    @Nested
    @DisplayName("Numeric Field Conversion Tests")
    class NumericFieldTests {

        @Test
        @DisplayName("Should convert integer numeric field with PIC 9 clause")
        void testConvertNumericField_Integer() {
            // Given
            Integer value = 12345;
            String picClause = "PIC 9(5)";
            String fieldName = "ACCOUNT_NUMBER";
            
            // When
            BigDecimal result = dataConversionService.convertNumericField(value, picClause, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal("12345"))).isEqualTo(0);
        }

        @Test
        @DisplayName("Should convert signed numeric field with PIC S9 clause")
        void testConvertNumericField_Signed() {
            // Given
            String value = "-123.45";
            String picClause = "PIC S9(5)V99";
            String fieldName = "BALANCE_AMOUNT";
            
            // When
            BigDecimal result = dataConversionService.convertNumericField(value, picClause, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal("-123.45"))).isEqualTo(0);
            assertThat(result.signum()).isEqualTo(-1);
        }

        @Test
        @DisplayName("Should handle null numeric values by returning zero")
        void testConvertNumericField_NullValue() {
            // Given
            String picClause = "PIC 9(10)V99";
            String fieldName = "NULL_AMOUNT";
            
            // When
            BigDecimal result = dataConversionService.convertNumericField(null, picClause, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(BigDecimal.ZERO)).isEqualTo(0);
        }

        @Test
        @DisplayName("Should preserve decimal precision for monetary amounts")
        void testConvertNumericField_MonetaryPrecision() {
            // Given
            String value = "1234.56";
            String picClause = "PIC S9(10)V99";
            String fieldName = "MONETARY_AMOUNT";
            
            // When
            BigDecimal result = dataConversionService.convertNumericField(value, picClause, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.scale()).isGreaterThanOrEqualTo(2);
            assertThat(result.compareTo(new BigDecimal("1234.56"))).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle large numeric values without precision loss")
        void testConvertNumericField_LargeValues() {
            // Given
            String value = "999999999999999999.99";
            String picClause = "PIC S9(18)V99";
            String fieldName = "LARGE_AMOUNT";
            
            // When
            BigDecimal result = dataConversionService.convertNumericField(value, picClause, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal("999999999999999999.99"))).isEqualTo(0);
        }

        @Test
        @DisplayName("Should throw exception for invalid numeric values")
        void testConvertNumericField_InvalidValue() {
            // Given
            String value = "invalid-number";
            String picClause = "PIC 9(5)";
            String fieldName = "INVALID_FIELD";
            
            // When/Then
            assertThatThrownBy(() -> dataConversionService.convertNumericField(value, picClause, fieldName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Numeric conversion failed");
        }
    }

    @Nested
    @DisplayName("Numeric Precision Validation Tests")
    class NumericPrecisionTests {

        @Test
        @DisplayName("Should validate precision within allowed limits")
        void testValidateNumericPrecision_ValidPrecision() {
            // Given
            BigDecimal value = new BigDecimal("123.45");
            int requiredScale = 2;
            int maxPrecision = 10;
            String fieldName = "VALID_PRECISION";
            
            // When
            BigDecimal result = dataConversionService.validateNumericPrecision(value, requiredScale, maxPrecision, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.scale()).isEqualTo(requiredScale);
            assertThat(result.precision()).isLessThanOrEqualTo(maxPrecision);
        }

        @Test
        @DisplayName("Should adjust scale to required value using COBOL rounding")
        void testValidateNumericPrecision_ScaleAdjustment() {
            // Given
            BigDecimal value = new BigDecimal("123.456789");
            int requiredScale = 2;
            int maxPrecision = 10;
            String fieldName = "SCALE_ADJUSTMENT";
            
            // When
            BigDecimal result = dataConversionService.validateNumericPrecision(value, requiredScale, maxPrecision, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.scale()).isEqualTo(requiredScale);
            // Should be rounded using HALF_UP (COBOL rounding)
            assertThat(result.compareTo(new BigDecimal("123.46"))).isEqualTo(0);
        }

        @Test
        @DisplayName("Should throw exception when precision exceeds maximum allowed")
        void testValidateNumericPrecision_ExceedsMaxPrecision() {
            // Given
            BigDecimal value = new BigDecimal("12345678901"); // 11 digits
            int requiredScale = 2;
            int maxPrecision = 10;
            String fieldName = "EXCEEDS_PRECISION";
            
            // When/Then
            assertThatThrownBy(() -> dataConversionService.validateNumericPrecision(value, requiredScale, maxPrecision, fieldName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("precision")
                .hasMessageContaining("exceeds maximum");
        }

        @Test
        @DisplayName("Should handle null values by returning zero with required scale")
        void testValidateNumericPrecision_NullValue() {
            // Given
            int requiredScale = 2;
            int maxPrecision = 10;
            String fieldName = "NULL_VALUE";
            
            // When
            BigDecimal result = dataConversionService.validateNumericPrecision(null, requiredScale, maxPrecision, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(BigDecimal.ZERO)).isEqualTo(0);
            assertThat(result.scale()).isEqualTo(requiredScale);
        }

        @ParameterizedTest
        @DisplayName("Should handle various scale requirements")
        @ValueSource(ints = {0, 1, 2, 3, 4, 5})
        void testValidateNumericPrecision_VariousScales(int scale) {
            // Given
            BigDecimal value = new BigDecimal("123.456789");
            int maxPrecision = 10;
            String fieldName = "SCALE_TEST";
            
            // When
            BigDecimal result = dataConversionService.validateNumericPrecision(value, scale, maxPrecision, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.scale()).isEqualTo(scale);
        }
    }

    @Nested
    @DisplayName("Bulk Data Conversion Tests")
    class BulkConversionTests {

        @Test
        @DisplayName("Should process multiple fields in bulk conversion successfully")
        void testConvertBulkData_MultipleFields() {
            // Given
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("accountNumber", 12345);
            dataMap.put("balance", "123.45");
            dataMap.put("customerName", "JOHN DOE");
            
            Map<String, String> schemaMap = new HashMap<>();
            schemaMap.put("accountNumber", "PIC 9(5)");
            schemaMap.put("balance", "PIC S9(10)V99");
            schemaMap.put("customerName", "PIC X(25)");
            
            String recordName = "CUSTOMER_RECORD";
            
            // When
            Map<String, Object> result = dataConversionService.convertBulkData(dataMap, schemaMap, recordName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
            assertThat(result.get("accountNumber")).isInstanceOf(BigDecimal.class);
            assertThat(result.get("balance")).isInstanceOf(BigDecimal.class);
            assertThat(result.get("customerName")).isInstanceOf(String.class);
        }

        @Test
        @DisplayName("Should handle COMP-3 fields in bulk conversion")
        void testConvertBulkData_Comp3Fields() {
            // Given
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("amount1", COMP3_POSITIVE_123_45);
            dataMap.put("amount2", COMP3_NEGATIVE_123_45);
            
            Map<String, String> schemaMap = new HashMap<>();
            schemaMap.put("amount1", "PIC S9(10)V99 COMP-3");
            schemaMap.put("amount2", "PIC S9(10)V99 COMP-3");
            
            String recordName = "COMP3_RECORD";
            
            // When
            Map<String, Object> result = dataConversionService.convertBulkData(dataMap, schemaMap, recordName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            
            BigDecimal amount1 = (BigDecimal) result.get("amount1");
            BigDecimal amount2 = (BigDecimal) result.get("amount2");
            
            assertThat(amount1.compareTo(new BigDecimal("123.45"))).isEqualTo(0);
            assertThat(amount2.compareTo(new BigDecimal("-123.45"))).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle fields without schema by passing through unchanged")
        void testConvertBulkData_NoSchema() {
            // Given
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("fieldWithSchema", 123);
            dataMap.put("fieldWithoutSchema", "unchanged");
            
            Map<String, String> schemaMap = new HashMap<>();
            schemaMap.put("fieldWithSchema", "PIC 9(3)");
            // No schema for fieldWithoutSchema
            
            String recordName = "MIXED_RECORD";
            
            // When
            Map<String, Object> result = dataConversionService.convertBulkData(dataMap, schemaMap, recordName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get("fieldWithSchema")).isInstanceOf(BigDecimal.class);
            assertThat(result.get("fieldWithoutSchema")).isEqualTo("unchanged");
        }

        @Test
        @DisplayName("Should handle empty data maps gracefully")
        void testConvertBulkData_EmptyMaps() {
            // Given
            Map<String, Object> emptyDataMap = new HashMap<>();
            Map<String, String> emptySchemaMap = new HashMap<>();
            String recordName = "EMPTY_RECORD";
            
            // When
            Map<String, Object> result = dataConversionService.convertBulkData(emptyDataMap, emptySchemaMap, recordName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle null input maps gracefully")
        void testConvertBulkData_NullMaps() {
            // Given
            String recordName = "NULL_RECORD";
            
            // When
            Map<String, Object> result = dataConversionService.convertBulkData(null, null, recordName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when field conversion fails")
        void testConvertBulkData_ConversionError() {
            // Given
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("validField", 123);
            dataMap.put("invalidField", "invalid-number");
            
            Map<String, String> schemaMap = new HashMap<>();
            schemaMap.put("validField", "PIC 9(3)");
            schemaMap.put("invalidField", "PIC 9(5)");
            
            String recordName = "ERROR_RECORD";
            
            // When/Then
            assertThatThrownBy(() -> dataConversionService.convertBulkData(dataMap, schemaMap, recordName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bulk conversion failed");
        }

        @Test
        @DisplayName("Should handle large datasets efficiently")
        void testConvertBulkData_Performance() {
            // Given
            Map<String, Object> largeDataMap = new HashMap<>();
            Map<String, String> largeSchemaMap = new HashMap<>();
            
            // Create 1000 test fields
            for (int i = 0; i < 1000; i++) {
                String fieldName = "field" + i;
                largeDataMap.put(fieldName, i * 100);
                largeSchemaMap.put(fieldName, "PIC 9(10)");
            }
            
            String recordName = "LARGE_RECORD";
            
            // When
            long startTime = System.currentTimeMillis();
            Map<String, Object> result = dataConversionService.convertBulkData(largeDataMap, largeSchemaMap, recordName);
            long endTime = System.currentTimeMillis();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1000);
            assertThat(endTime - startTime).isLessThan(5000); // Should complete within 5 seconds
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Value Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle maximum BigDecimal values without overflow")
        void testMaximumBigDecimalValues() {
            // Given
            String maxValueString = "999999999999999999.99";
            String picClause = "PIC S9(18)V99";
            String fieldName = "MAX_VALUE";
            
            // When
            BigDecimal result = dataConversionService.convertNumericField(maxValueString, picClause, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal(maxValueString))).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle minimum BigDecimal values without underflow")
        void testMinimumBigDecimalValues() {
            // Given
            String minValueString = "-999999999999999999.99";
            String picClause = "PIC S9(18)V99";
            String fieldName = "MIN_VALUE";
            
            // When
            BigDecimal result = dataConversionService.convertNumericField(minValueString, picClause, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal(minValueString))).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle very small decimal values")
        void testVerySmallDecimalValues() {
            // Given
            String smallValueString = "0.01";
            String picClause = "PIC S9(10)V99";
            String fieldName = "SMALL_VALUE";
            
            // When
            BigDecimal result = dataConversionService.convertNumericField(smallValueString, picClause, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(new BigDecimal("0.01"))).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle leap year edge cases")
        void testLeapYearEdgeCases() {
            // Test century years divisible by 400 (leap years)
            assertThatCode(() -> dataConversionService.validateAndConvertDate("20000229", "YEAR_2000"))
                .doesNotThrowAnyException();
            
            // Test century years not divisible by 400 (not leap years)
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("19000229", "YEAR_1900"))
                .isInstanceOf(IllegalArgumentException.class);
            
            // Test regular leap year
            assertThatCode(() -> dataConversionService.validateAndConvertDate("20240229", "YEAR_2024"))
                .doesNotThrowAnyException();
            
            // Test non-leap year
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate("20230229", "YEAR_2023"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle month-end boundary dates")
        void testMonthEndBoundaryDates() {
            // Test all month-end dates for a non-leap year
            String[] validMonthEnds = {
                "20230131", "20230228", "20230331", "20230430",
                "20230531", "20230630", "20230731", "20230831", 
                "20230930", "20231031", "20231130", "20231231"
            };
            
            for (String date : validMonthEnds) {
                assertThatCode(() -> dataConversionService.validateAndConvertDate(date, "MONTH_END"))
                    .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should handle COMP-3 single byte values")
        void testComp3SingleByte() {
            // Given
            byte[] singleByteComp3 = {0x0C}; // 0 positive
            String fieldName = "SINGLE_BYTE";
            int scale = 0;
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(singleByteComp3, scale, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.compareTo(BigDecimal.ZERO)).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle very long EBCDIC strings")
        void testVeryLongEbcdicStrings() {
            // Given
            byte[] longEbcdic = new byte[1000];
            Arrays.fill(longEbcdic, (byte)0xC1); // Fill with 'A's
            String fieldName = "LONG_STRING";
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(longEbcdic, fieldName);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1000);
            assertThat(result).containsOnly("A");
        }
    }

    @Nested
    @DisplayName("Integration Tests with CobolDataConverter")
    class IntegrationTests {

        @Test
        @DisplayName("Should integrate with CobolDataConverter.fromComp3()")
        void testIntegrationWithCobolDataConverter_FromComp3() {
            // Given
            String fieldName = "INTEGRATION_COMP3";
            int scale = 2;
            
            // When
            BigDecimal result = dataConversionService.convertComp3ToBigDecimal(COMP3_POSITIVE_123_45, scale, fieldName);
            
            // Then - should produce same result as direct CobolDataConverter call
            BigDecimal directResult = CobolDataConverter.fromComp3(COMP3_POSITIVE_123_45, scale);
            assertThat(result.compareTo(directResult)).isEqualTo(0);
        }

        @Test
        @DisplayName("Should integrate with CobolDataConverter.toBigDecimal()")
        void testIntegrationWithCobolDataConverter_ToBigDecimal() {
            // Given
            Object value = "123.45";
            int scale = 2;
            
            // When - test through numeric field conversion
            BigDecimal serviceResult = dataConversionService.convertNumericField(value, "PIC S9(10)V99", "INTEGRATION_TEST");
            
            // Then - should use CobolDataConverter internally
            BigDecimal directResult = CobolDataConverter.toBigDecimal(value, scale);
            assertThat(serviceResult).isNotNull();
            assertThat(directResult).isNotNull();
        }

        @Test
        @DisplayName("Should integrate with CobolDataConverter.convertPicString()")
        void testIntegrationWithCobolDataConverter_ConvertPicString() {
            // Given
            String value = "TEST STRING";
            int maxLength = 25;
            
            // When - test through bulk conversion with PIC X field
            Map<String, Object> dataMap = Map.of("testField", value);
            Map<String, String> schemaMap = Map.of("testField", "PIC X(25)");
            Map<String, Object> result = dataConversionService.convertBulkData(dataMap, schemaMap, "INTEGRATION_TEST");
            
            // Then - should use CobolDataConverter internally
            String directResult = CobolDataConverter.convertPicString(value, maxLength);
            assertThat(result.get("testField")).isEqualTo(directResult);
        }
    }

    @Nested
    @DisplayName("Error Handling and Exception Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should provide detailed error messages for COMP-3 conversion failures")
        void testComp3ConversionErrorMessages() {
            // Given
            byte[] invalidComp3 = {(byte)0xFF, (byte)0xAA}; // Invalid nibbles
            String fieldName = "ERROR_FIELD";
            int scale = 2;
            
            // When/Then
            assertThatThrownBy(() -> dataConversionService.convertComp3ToBigDecimal(invalidComp3, scale, fieldName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COMP-3 conversion failed")
                .hasMessageContaining(fieldName);
        }

        @Test
        @DisplayName("Should provide context in error messages for date validation")
        void testDateValidationErrorMessages() {
            // Given
            String invalidDate = "20240230"; // Invalid date (Feb 30)
            String fieldName = "BIRTH_DATE";
            
            // When/Then
            assertThatThrownBy(() -> dataConversionService.validateAndConvertDate(invalidDate, fieldName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid for month")
                .hasMessageContaining("30")
                .hasMessageContaining("2");
        }

        @Test
        @DisplayName("Should handle concurrent access safely")
        void testConcurrentAccess() throws InterruptedException {
            // Given
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            List<Exception> exceptions = Arrays.asList(new Exception[threadCount]);
            
            // When - run conversions concurrently
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    try {
                        BigDecimal result = dataConversionService.convertComp3ToBigDecimal(
                            COMP3_POSITIVE_123_45, 2, "CONCURRENT_TEST_" + threadIndex);
                        assertThat(result.compareTo(new BigDecimal("123.45"))).isEqualTo(0);
                    } catch (Exception e) {
                        exceptions.set(threadIndex, e);
                    }
                });
                threads[i].start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Then - no exceptions should have occurred
            for (Exception e : exceptions) {
                assertThat(e).isNull();
            }
        }

        @Test
        @DisplayName("Should handle memory constraints for large data sets")
        void testMemoryConstraints() {
            // Given - create large byte array for EBCDIC conversion
            byte[] largeEbcdic = new byte[100000];
            Arrays.fill(largeEbcdic, (byte)0xC1); // Fill with 'A's
            String fieldName = "LARGE_MEMORY_TEST";
            
            // When
            String result = dataConversionService.convertEbcdicToAscii(largeEbcdic, fieldName);
            
            // Then - should complete without memory issues
            assertThat(result).isNotNull();
            assertThat(result).hasSize(100000);
        }
    }

    // Helper methods for generating test data

    /**
     * Provides test arguments for parameterized tests with various COMP-3 values
     */
    static Stream<Arguments> comp3TestData() {
        return Stream.of(
            Arguments.of(new byte[]{0x01, 0x23, 0x4C}, 2, new BigDecimal("123.45")), // Positive
            Arguments.of(new byte[]{0x01, 0x23, 0x4D}, 2, new BigDecimal("-123.45")), // Negative
            Arguments.of(new byte[]{0x00, 0x0C}, 2, new BigDecimal("0.00")), // Zero
            Arguments.of(new byte[]{0x99, 0x9C}, 1, new BigDecimal("99.9")), // Max 3-digit
            Arguments.of(new byte[]{0x05, 0x0F}, 0, new BigDecimal("50")) // Unsigned integer
        );
    }

    /**
     * Provides test arguments for date validation edge cases  
     */
    static Stream<Arguments> dateValidationTestData() {
        return Stream.of(
            Arguments.of("20230101", true, "Valid New Year"),
            Arguments.of("20231231", true, "Valid New Year's Eve"),
            Arguments.of("20240229", true, "Valid leap year date"),
            Arguments.of("20230229", false, "Invalid non-leap year date"),
            Arguments.of("20230431", false, "Invalid April 31st"),
            Arguments.of("20231301", false, "Invalid month 13"),
            Arguments.of("20230132", false, "Invalid day 32"),
            Arguments.of("18991231", false, "Invalid century 18"),
            Arguments.of("21001231", false, "Invalid century 21")
        );
    }
}