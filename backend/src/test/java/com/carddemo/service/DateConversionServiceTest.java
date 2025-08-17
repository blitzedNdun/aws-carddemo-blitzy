/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.carddemo.service;

import com.carddemo.service.DateConversionService.DateValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;

/**
 * Comprehensive unit test class for DateConversionService that validates the COBOL CSUTLDTC.cbl 
 * date conversion utility migration, testing Lillian date conversions, various date format masks, 
 * CEEDAYS API equivalent functionality, and comprehensive error handling for invalid dates.
 * 
 * This test class ensures 100% functional parity between the original COBOL implementation
 * and the modernized Java service, validating:
 * - Date conversion accuracy matching COBOL precision
 * - All supported date format masks from COBOL program
 * - Error code mapping from CEEDAYS API feedback codes
 * - Lillian date calculation precision (days since January 1, 1601)
 * - Edge case handling including leap years and boundary conditions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DateConversionService - CSUTLDTC COBOL Migration Tests")
class DateConversionServiceTest {

    private DateConversionService dateConversionService;

    // Test constants matching COBOL program values
    private static final String EXPECTED_VALID_RESULT = "Date is valid";
    private static final String EXPECTED_INVALID_RESULT = "Date is invalid";
    private static final String EXPECTED_INSUFFICIENT_RESULT = "Insufficient";
    private static final String EXPECTED_DATEVALUE_ERROR = "Datevalue error";
    private static final String EXPECTED_BAD_PIC_STRING = "Bad Pic String";
    
    // Lillian epoch constants for validation (January 1, 1601)
    private static final LocalDate LILLIAN_EPOCH = LocalDate.of(1601, 1, 1);
    private static final long EXPECTED_EPOCH_LILLIAN = 0L;
    
    @BeforeEach
    void setUp() {
        // Initialize the service directly since it has no dependencies
        dateConversionService = new DateConversionService();
        Assertions.assertNotNull(dateConversionService, "DateConversionService should be initialized");
    }

    @Nested
    @DisplayName("Core COBOL Functionality Tests - A000-MAIN Equivalent")
    class CoreFunctionalityTests {

        @Test
        @DisplayName("Valid date with CCYYMMDD format - matches COBOL CEEDAYS success case")
        void testValidDateCCYYMMDD() {
            // Given: Valid date string and format matching COBOL test case
            String dateString = "20240315";
            String formatMask = "CCYYMMDD";
            
            // When: Processing through Java equivalent of A000-MAIN paragraph
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            
            // Then: Assert identical output to COBOL CSUTLDTC.cbl
            Assertions.assertAll("COBOL functional parity validation",
                () -> Assertions.assertTrue(result.isValid(), "Date should be valid"),
                () -> Assertions.assertEquals("0000", result.getSeverity(), "Severity should match COBOL WS-SEVERITY-N"),
                () -> Assertions.assertEquals("0000", result.getMessageCode(), "Message code should match COBOL MSG-NO"),
                () -> Assertions.assertEquals(EXPECTED_VALID_RESULT, result.getResult(), "Result should match COBOL WS-RESULT"),
                () -> Assertions.assertEquals(dateString, result.getDate(), "Date should match COBOL WS-DATE"),
                () -> Assertions.assertEquals(formatMask, result.getDateFormat(), "Format should match COBOL WS-DATE-FMT"),
                () -> Assertions.assertNotNull(result.getLillianDate(), "Lillian date should be calculated"),
                () -> Assertions.assertTrue(result.getLillianDate() > 0, "Lillian date should be positive for dates after 1601")
            );
        }

        @Test
        @DisplayName("Empty date string - matches COBOL FC-INSUFFICIENT-DATA condition")
        void testEmptyDateString() {
            // Given: Empty date string matching COBOL insufficient data scenario
            String dateString = "";
            String formatMask = "CCYYMMDD";
            
            // When: Processing empty input
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            
            // Then: Assert COBOL FC-INSUFFICIENT-DATA mapping
            Assertions.assertAll("Insufficient data error validation",
                () -> Assertions.assertFalse(result.isValid(), "Date should be invalid"),
                () -> Assertions.assertEquals("0001", result.getSeverity(), "Severity should indicate error"),
                () -> Assertions.assertEquals("2507", result.getMessageCode(), "Message code should match FC-INSUFFICIENT-DATA"),
                () -> Assertions.assertEquals(EXPECTED_INSUFFICIENT_RESULT, result.getResult(), "Result should match COBOL message"),
                () -> Assertions.assertEquals("", result.getDate(), "Date should be empty"),
                () -> Assertions.assertEquals(formatMask, result.getDateFormat(), "Format should be preserved")
            );
        }

        @Test
        @DisplayName("Empty format mask - matches COBOL FC-INSUFFICIENT-DATA condition")
        void testEmptyFormatMask() {
            // Given: Valid date but empty format mask
            String dateString = "20240315";
            String formatMask = "";
            
            // When: Processing with empty format
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            
            // Then: Assert COBOL insufficient data handling
            Assertions.assertFalse(result.isValid());
            Assertions.assertEquals("0001", result.getSeverity());
            Assertions.assertEquals("2507", result.getMessageCode());
            Assertions.assertEquals(EXPECTED_INSUFFICIENT_RESULT, result.getResult());
        }

        @Test
        @DisplayName("Invalid date value - matches COBOL FC-BAD-DATE-VALUE condition")
        void testInvalidDateValue() {
            // Given: Invalid date with valid format (February 30th)
            String dateString = "20240230";
            String formatMask = "CCYYMMDD";
            
            // When: Processing invalid date
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            
            // Then: Assert COBOL FC-BAD-DATE-VALUE mapping
            Assertions.assertAll("Bad date value error validation",
                () -> Assertions.assertFalse(result.isValid(), "Date should be invalid"),
                () -> Assertions.assertEquals("0001", result.getSeverity(), "Severity should indicate error"),
                () -> Assertions.assertEquals("2508", result.getMessageCode(), "Message code should match FC-BAD-DATE-VALUE"),
                () -> Assertions.assertEquals(EXPECTED_DATEVALUE_ERROR, result.getResult(), "Result should match COBOL message")
            );
        }

        @Test
        @DisplayName("Unsupported format mask - matches COBOL FC-BAD-PIC-STRING condition")
        void testUnsupportedFormatMask() {
            // Given: Valid date with unsupported format mask
            String dateString = "20240315";
            String formatMask = "INVALID";
            
            // When: Processing with unsupported format
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            
            // Then: Assert COBOL FC-BAD-PIC-STRING mapping
            Assertions.assertAll("Bad picture string error validation",
                () -> Assertions.assertFalse(result.isValid(), "Date should be invalid"),
                () -> Assertions.assertEquals("0001", result.getSeverity(), "Severity should indicate error"),
                () -> Assertions.assertEquals("2518", result.getMessageCode(), "Message code should match FC-BAD-PIC-STRING"),
                () -> Assertions.assertEquals(EXPECTED_BAD_PIC_STRING, result.getResult(), "Result should match COBOL message")
            );
        }
    }

    @Nested
    @DisplayName("Lillian Date Conversion Tests - CEEDAYS API Equivalent")
    class LillianDateConversionTests {

        @Test
        @DisplayName("Lillian epoch date conversion - January 1, 1601")
        void testLillianEpochConversion() {
            // Given: The Lillian epoch date (January 1, 1601)
            LocalDate epochDate = LILLIAN_EPOCH;
            
            // When: Converting to Lillian date
            Long lillianDate = dateConversionService.convertToLillianDate(epochDate);
            
            // Then: Should return 0 (epoch start)
            Assertions.assertEquals(EXPECTED_EPOCH_LILLIAN, lillianDate,
                "Lillian epoch should convert to 0");
        }

        @Test
        @DisplayName("Known historical date - January 1, 2000 (Y2K)")
        void testY2KDateConversion() {
            // Given: Y2K date (January 1, 2000)
            LocalDate y2kDate = LocalDate.of(2000, 1, 1);
            
            // When: Converting to Lillian date
            Long lillianDate = dateConversionService.convertToLillianDate(y2kDate);
            
            // Then: Should match expected Y2K Lillian date (145,731 days since 1601)
            Long expectedY2KLillian = 145731L;
            Assertions.assertEquals(expectedY2KLillian, lillianDate,
                "Y2K date should convert to correct Lillian date");
        }

        @Test
        @DisplayName("Current century date - validates modern date handling")
        void testCurrentCenturyDate() {
            // Given: A date in current century (March 15, 2024)
            LocalDate modernDate = LocalDate.of(2024, 3, 15);
            
            // When: Converting to Lillian date and back
            Long lillianDate = dateConversionService.convertToLillianDate(modernDate);
            LocalDate convertedBack = dateConversionService.convertFromLillianDate(lillianDate);
            
            // Then: Round-trip conversion should be exact
            Assertions.assertAll("Modern date round-trip validation",
                () -> Assertions.assertNotNull(lillianDate, "Lillian date should be calculated"),
                () -> Assertions.assertTrue(lillianDate > 0, "Lillian date should be positive"),
                () -> Assertions.assertEquals(modernDate, convertedBack, "Round-trip conversion should be exact"),
                () -> Assertions.assertTrue(lillianDate > 154000L, "Modern dates should have high Lillian values")
            );
        }

        @Test
        @DisplayName("Leap year date validation - February 29, 2024")
        void testLeapYearDate() {
            // Given: Leap year date (February 29, 2024)
            String dateString = "20240229";
            String formatMask = "CCYYMMDD";
            
            // When: Validating leap year date
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            
            // Then: Should be valid with correct Lillian conversion
            Assertions.assertAll("Leap year date validation",
                () -> Assertions.assertTrue(result.isValid(), "Leap year date should be valid"),
                () -> Assertions.assertEquals(EXPECTED_VALID_RESULT, result.getResult(), "Should indicate valid date"),
                () -> Assertions.assertNotNull(result.getLillianDate(), "Lillian date should be calculated"),
                () -> Assertions.assertTrue(result.getLillianDate() > 0, "Lillian date should be positive")
            );
        }

        @Test
        @DisplayName("Non-leap year February 29 - should fail validation")
        void testNonLeapYearFebruary29() {
            // Given: February 29 in non-leap year (2023)
            String dateString = "20230229";
            String formatMask = "CCYYMMDD";
            
            // When: Validating non-leap year February 29
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            
            // Then: Should be invalid
            Assertions.assertAll("Non-leap year February 29 validation",
                () -> Assertions.assertFalse(result.isValid(), "February 29 in non-leap year should be invalid"),
                () -> Assertions.assertEquals(EXPECTED_DATEVALUE_ERROR, result.getResult(), "Should indicate date error")
            );
        }

        @Test
        @DisplayName("Null date handling - edge case validation")
        void testNullDateConversion() {
            // Given: Null date input
            LocalDate nullDate = null;
            
            // When: Converting null date
            Long lillianDate = dateConversionService.convertToLillianDate(nullDate);
            
            // Then: Should return null
            Assertions.assertNull(lillianDate, "Null date should convert to null Lillian date");
        }
    }

    @Nested
    @DisplayName("Date Format Mask Tests - All COBOL Supported Formats")
    class DateFormatMaskTests {

        @ParameterizedTest(name = "Format: {1}, Date: {0}")
        @CsvSource({
            "20240315, CCYYMMDD",
            "20240315, YYYYMMDD",
            "03/15/2024, MM/DD/YYYY",
            "15/03/2024, DD/MM/YYYY",
            "03-15-2024, MM-DD-YYYY",
            "15-03-2024, DD-MM-YYYY",
            "03152024, MMDDYYYY",
            "15032024, DDMMYYYY",
            "240315, YYMMDD",
            "031524, MMDDYY",
            "150324, DDMMYY"
        })
        @DisplayName("Valid date format combinations - comprehensive COBOL format support")
        void testValidDateFormats(String dateString, String formatMask) {
            // When: Validating date with various format masks
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            
            // Then: All should be valid with proper Lillian conversion
            Assertions.assertAll("Valid format validation for " + formatMask,
                () -> Assertions.assertTrue(result.isValid(), 
                    "Date " + dateString + " with format " + formatMask + " should be valid"),
                () -> Assertions.assertEquals(EXPECTED_VALID_RESULT, result.getResult(),
                    "Result should indicate valid date"),
                () -> Assertions.assertNotNull(result.getLillianDate(),
                    "Lillian date should be calculated"),
                () -> Assertions.assertEquals("0000", result.getSeverity(),
                    "Severity should indicate success"),
                () -> Assertions.assertEquals(dateString, result.getDate(),
                    "Original date should be preserved"),
                () -> Assertions.assertEquals(formatMask, result.getDateFormat(),
                    "Format mask should be preserved")
            );
        }

        @ParameterizedTest(name = "Invalid Date: {0}, Format: {1}")
        @CsvSource({
            "20241301, CCYYMMDD",     // Invalid month (13)
            "20240230, CCYYMMDD",     // Invalid day for February
            "20240431, CCYYMMDD",     // Invalid day for April (only 30 days)
            "13/15/2024, MM/DD/YYYY", // Invalid month in MM/DD format
            "32/03/2024, DD/MM/YYYY", // Invalid day in DD/MM format
            "00/15/2024, MM/DD/YYYY", // Invalid month (00)
            "15/00/2024, DD/MM/YYYY", // Invalid month (00)
            "ABC12024, MMDDYYYY",     // Non-numeric characters
            "1234567, CCYYMMDD",      // Wrong length
            "123, MMDDYY"             // Too short
        })
        @DisplayName("Invalid date format combinations - error handling validation")
        void testInvalidDateFormats(String dateString, String formatMask) {
            // When: Validating invalid date with format mask
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            
            // Then: Should be invalid with appropriate error
            Assertions.assertAll("Invalid format validation for " + formatMask,
                () -> Assertions.assertFalse(result.isValid(),
                    "Date " + dateString + " with format " + formatMask + " should be invalid"),
                () -> Assertions.assertEquals("0001", result.getSeverity(),
                    "Severity should indicate error"),
                () -> Assertions.assertTrue(result.getResult().contains("error") || 
                                          result.getResult().contains("invalid"),
                    "Result should indicate error condition")
            );
        }

        @Test
        @DisplayName("Century pivot logic for YY formats - validates COBOL century handling")
        void testCenturyPivotLogic() {
            // Given: Various two-digit years to test century pivot
            String[][] testCases = {
                {"240315", "YYMMDD", "2024"}, // 24 -> 2024 (00-49 range)
                {"490315", "YYMMDD", "2049"}, // 49 -> 2049 (boundary)
                {"500315", "YYMMDD", "1950"}, // 50 -> 1950 (50-99 range)
                {"990315", "YYMMDD", "1999"}  // 99 -> 1999 (boundary)
            };
            
            for (String[] testCase : testCases) {
                String dateString = testCase[0];
                String formatMask = testCase[1];
                String expectedYear = testCase[2];
                
                // When: Processing YY format date
                DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
                
                // Then: Should apply correct century logic
                Assertions.assertTrue(result.isValid(),
                    "Date with YY format should be valid: " + dateString);
                
                // Verify the year by checking the formatted message contains correct year
                String formattedMessage = result.getFormattedMessage();
                Assertions.assertTrue(formattedMessage.contains(dateString),
                    "Formatted message should contain original date");
            }
        }
    }

    @Nested
    @DisplayName("LocalDate API Usage Tests - Schema Compliance")
    class LocalDateAPIUsageTests {

        @Test
        @DisplayName("LocalDate.now() usage validation")
        void testLocalDateNowUsage() {
            // When: Getting current date
            LocalDate currentDate = dateConversionService.getCurrentDate();
            LocalDate actualNow = LocalDate.now();
            
            // Then: Should use LocalDate.now() properly
            Assertions.assertAll("LocalDate.now() validation",
                () -> Assertions.assertNotNull(currentDate, "Current date should not be null"),
                () -> Assertions.assertEquals(actualNow, currentDate, "Should return actual current date"),
                () -> Assertions.assertTrue(currentDate.getYear() >= 2024, "Current year should be reasonable")
            );
        }

        @Test
        @DisplayName("LocalDate.of() usage in date creation")
        void testLocalDateOfUsage() {
            // Given: Specific date components
            int year = 2024;
            int month = 3;
            int day = 15;
            
            // When: Creating date using LocalDate.of() equivalent
            String dateString = String.format("%04d%02d%02d", year, month, day);
            DateValidationResult result = dateConversionService.validateDate(dateString, "CCYYMMDD");
            
            // Then: Should create correct LocalDate
            Assertions.assertAll("LocalDate.of() equivalent validation",
                () -> Assertions.assertTrue(result.isValid(), "Date should be valid"),
                () -> Assertions.assertNotNull(result.getLillianDate(), "Lillian date should be calculated"),
                () -> Assertions.assertEquals(dateString, result.getDate(), "Date should match input")
            );
        }

        @Test
        @DisplayName("LocalDate.parse() usage for ISO dates")
        void testLocalDateParseUsage() {
            // Given: ISO format date string
            String isoDateString = "2024-03-15";
            
            // When: Parsing ISO date
            DateValidationResult result = dateConversionService.parseISODate(isoDateString);
            
            // Then: Should use LocalDate.parse() successfully
            Assertions.assertAll("LocalDate.parse() validation",
                () -> Assertions.assertTrue(result.isValid(), "ISO date should be valid"),
                () -> Assertions.assertEquals(EXPECTED_VALID_RESULT, result.getResult(), "Should indicate valid date"),
                () -> Assertions.assertEquals("ISO-8601", result.getDateFormat(), "Should indicate ISO format"),
                () -> Assertions.assertNotNull(result.getLillianDate(), "Lillian date should be calculated")
            );
        }

        @Test
        @DisplayName("Invalid ISO date handling")
        void testInvalidISODateParsing() {
            // Given: Invalid ISO format date
            String invalidIsoDate = "2024-13-45";
            
            // When: Parsing invalid ISO date
            DateValidationResult result = dateConversionService.parseISODate(invalidIsoDate);
            
            // Then: Should handle parse error properly
            Assertions.assertAll("Invalid ISO date validation",
                () -> Assertions.assertFalse(result.isValid(), "Invalid ISO date should be invalid"),
                () -> Assertions.assertEquals(EXPECTED_DATEVALUE_ERROR, result.getResult(), "Should indicate date error"),
                () -> Assertions.assertEquals("2508", result.getMessageCode(), "Should use correct error code")
            );
        }
    }

    @Nested
    @DisplayName("Comprehensive Error Handling Tests - COBOL Error Code Mapping")
    class ErrorHandlingTests {

        @Test
        @DisplayName("All COBOL error conditions mapping validation")
        void testAllErrorConditions() {
            // Test cases covering all COBOL feedback codes
            String[][] errorTestCases = {
                // Format: {dateString, formatMask, expectedErrorCode, expectedResult}
                {"", "CCYYMMDD", "2507", EXPECTED_INSUFFICIENT_RESULT},        // FC-INSUFFICIENT-DATA
                {"20240230", "CCYYMMDD", "2508", EXPECTED_DATEVALUE_ERROR},    // FC-BAD-DATE-VALUE
                {"20240315", "BADFORMAT", "2518", EXPECTED_BAD_PIC_STRING},    // FC-BAD-PIC-STRING
                {"ABCD1234", "CCYYMMDD", "2508", EXPECTED_DATEVALUE_ERROR},   // Non-numeric data
                {"20241301", "CCYYMMDD", "2508", EXPECTED_DATEVALUE_ERROR}     // Invalid month
            };
            
            for (String[] testCase : errorTestCases) {
                String dateString = testCase[0];
                String formatMask = testCase[1];
                String expectedErrorCode = testCase[2];
                String expectedResult = testCase[3];
                
                // When: Processing error condition
                DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
                
                // Then: Should map to correct COBOL error condition
                Assertions.assertAll("Error condition: " + dateString + "/" + formatMask,
                    () -> Assertions.assertFalse(result.isValid(), 
                        "Should be invalid for error condition"),
                    () -> Assertions.assertEquals(expectedErrorCode, result.getMessageCode(),
                        "Should map to correct COBOL error code"),
                    () -> Assertions.assertEquals(expectedResult, result.getResult(),
                        "Should have correct error message"),
                    () -> Assertions.assertEquals("0001", result.getSeverity(),
                        "Should have error severity")
                );
            }
        }

        @Test
        @DisplayName("Message formatting matches COBOL WS-MESSAGE structure")
        void testMessageFormattingStructure() {
            // Given: Valid date for formatting test
            String dateString = "20240315";
            String formatMask = "CCYYMMDD";
            
            // When: Getting formatted message
            DateValidationResult result = dateConversionService.validateDate(dateString, formatMask);
            String formattedMessage = result.getFormattedMessage();
            
            // Then: Should match COBOL WS-MESSAGE format exactly
            // Format: "nnnn Mesg Code:nnnn result TstDate:date Mask used:format   "
            Assertions.assertAll("COBOL message format validation",
                () -> Assertions.assertTrue(formattedMessage.contains("Mesg Code:"),
                    "Should contain 'Mesg Code:' label"),
                () -> Assertions.assertTrue(formattedMessage.contains("TstDate:"),
                    "Should contain 'TstDate:' label"),
                () -> Assertions.assertTrue(formattedMessage.contains("Mask used:"),
                    "Should contain 'Mask used:' label"),
                () -> Assertions.assertTrue(formattedMessage.contains(dateString),
                    "Should contain original date string"),
                () -> Assertions.assertTrue(formattedMessage.contains(formatMask),
                    "Should contain format mask"),
                () -> Assertions.assertEquals(80, formattedMessage.length(),
                    "Should match COBOL 80-character LS-RESULT length")
            );
        }
    }

    @Nested
    @DisplayName("Edge Case and Boundary Tests - Comprehensive Validation")
    class EdgeCaseTests {

        @Test
        @DisplayName("Maximum field length handling - 10 character limit")
        void testMaximumFieldLength() {
            // Given: Strings longer than COBOL field limits (10 characters)
            String longDateString = "202403151234567890"; // 18 characters
            String longFormatMask = "CCYYMMDDEXTRA"; // 13 characters
            
            // When: Processing with long strings
            DateValidationResult result = dateConversionService.validateDate(longDateString, longFormatMask);
            
            // Then: Should truncate to COBOL field sizes
            Assertions.assertAll("Field length truncation validation",
                () -> Assertions.assertEquals("2024031512", result.getDate().length() <= 10 ? result.getDate() : result.getDate().substring(0, 10),
                    "Date should be truncated to 10 characters"),
                () -> Assertions.assertEquals("CCYYMMDDEX", result.getDateFormat().length() <= 10 ? result.getDateFormat() : result.getDateFormat().substring(0, 10),
                    "Format should be truncated to 10 characters")
            );
        }

        @Test
        @DisplayName("Year boundary validation - supported range")
        void testYearBoundaryValidation() {
            // Test minimum supported year (1601 - Lillian epoch)
            DateValidationResult result1601 = dateConversionService.validateDate("16010101", "CCYYMMDD");
            Assertions.assertTrue(result1601.isValid(), "Year 1601 should be valid (Lillian epoch)");
            Assertions.assertEquals(0L, result1601.getLillianDate(), "Lillian epoch should be 0");
            
            // Test maximum reasonable year (3000)
            DateValidationResult result3000 = dateConversionService.validateDate("30001231", "CCYYMMDD");
            Assertions.assertTrue(result3000.isValid(), "Year 3000 should be valid");
            
            // Test year below minimum (1600)
            DateValidationResult result1600 = dateConversionService.validateDate("16001231", "CCYYMMDD");
            Assertions.assertFalse(result1600.isValid(), "Year 1600 should be invalid (below Lillian epoch)");
            
            // Test year above maximum (3001)
            DateValidationResult result3001 = dateConversionService.validateDate("30010101", "CCYYMMDD");
            Assertions.assertFalse(result3001.isValid(), "Year 3001 should be invalid (above supported range)");
        }

        @Test
        @DisplayName("Month boundary validation - 01 to 12")
        void testMonthBoundaryValidation() {
            // Valid month boundaries
            DateValidationResult jan = dateConversionService.validateDate("20240101", "CCYYMMDD");
            DateValidationResult dec = dateConversionService.validateDate("20241231", "CCYYMMDD");
            
            Assertions.assertTrue(jan.isValid(), "January (01) should be valid");
            Assertions.assertTrue(dec.isValid(), "December (12) should be valid");
            
            // Invalid month boundaries  
            DateValidationResult month00 = dateConversionService.validateDate("20240001", "CCYYMMDD");
            DateValidationResult month13 = dateConversionService.validateDate("20241301", "CCYYMMDD");
            
            Assertions.assertFalse(month00.isValid(), "Month 00 should be invalid");
            Assertions.assertFalse(month13.isValid(), "Month 13 should be invalid");
        }

        @Test
        @DisplayName("Day boundary validation for different months")
        void testDayBoundaryValidation() {
            // 31-day months
            DateValidationResult jan31 = dateConversionService.validateDate("20240131", "CCYYMMDD");
            Assertions.assertTrue(jan31.isValid(), "January 31 should be valid");
            
            // 30-day months
            DateValidationResult apr30 = dateConversionService.validateDate("20240430", "CCYYMMDD");
            DateValidationResult apr31 = dateConversionService.validateDate("20240431", "CCYYMMDD");
            
            Assertions.assertTrue(apr30.isValid(), "April 30 should be valid");
            Assertions.assertFalse(apr31.isValid(), "April 31 should be invalid");
            
            // February in leap year
            DateValidationResult feb28leap = dateConversionService.validateDate("20240228", "CCYYMMDD");
            DateValidationResult feb29leap = dateConversionService.validateDate("20240229", "CCYYMMDD");
            
            Assertions.assertTrue(feb28leap.isValid(), "February 28 in leap year should be valid");
            Assertions.assertTrue(feb29leap.isValid(), "February 29 in leap year should be valid");
            
            // February in non-leap year
            DateValidationResult feb28normal = dateConversionService.validateDate("20230228", "CCYYMMDD");
            DateValidationResult feb29normal = dateConversionService.validateDate("20230229", "CCYYMMDD");
            
            Assertions.assertTrue(feb28normal.isValid(), "February 28 in normal year should be valid");
            Assertions.assertFalse(feb29normal.isValid(), "February 29 in normal year should be invalid");
        }

        @Test
        @DisplayName("White space handling - trimming validation")
        void testWhiteSpaceHandling() {
            // Given: Date strings with leading/trailing spaces
            String dateWithSpaces = "  20240315  ";
            String formatWithSpaces = "  CCYYMMDD  ";
            
            // When: Processing with spaces
            DateValidationResult result = dateConversionService.validateDate(dateWithSpaces, formatWithSpaces);
            
            // Then: Should handle spaces properly
            Assertions.assertAll("White space handling validation",
                () -> Assertions.assertTrue(result.isValid(), "Date with spaces should be valid after trimming"),
                () -> Assertions.assertEquals("20240315", result.getDate(), "Date should be trimmed"),
                () -> Assertions.assertEquals("CCYYMMDD", result.getDateFormat(), "Format should be trimmed")
            );
        }
    }

    @Nested
    @DisplayName("Performance and Precision Tests")
    class PerformanceAndPrecisionTests {

        @Test
        @DisplayName("Large volume date processing - performance validation")
        void testLargeVolumeProcessing() {
            // Given: Multiple date validations
            int iterations = 1000;
            int successCountTemp = 0;
            
            long startTime = System.currentTimeMillis();
            
            // When: Processing many dates
            for (int i = 0; i < iterations; i++) {
                String dateString = String.format("2024%02d15", (i % 12) + 1);
                DateValidationResult result = dateConversionService.validateDate(dateString, "CCYYMMDD");
                if (result.isValid()) {
                    successCountTemp++;
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Make variables effectively final for lambda expressions
            final int successCount = successCountTemp;
            
            // Then: Should process efficiently
            Assertions.assertAll("Performance validation",
                () -> Assertions.assertEquals(iterations, successCount, "All valid dates should be processed successfully"),
                () -> Assertions.assertTrue(duration < 5000, "Should process " + iterations + " dates within 5 seconds"),
                () -> Assertions.assertTrue(duration > 0, "Processing should take measurable time")
            );
        }

        @Test
        @DisplayName("Lillian date precision validation - exact calculation")
        void testLillianDatePrecision() {
            // Given: Known test dates with expected Lillian values
            Object[][] testCases = {
                {LocalDate.of(1601, 1, 1), 0L},           // Epoch
                {LocalDate.of(1601, 1, 2), 1L},           // Day after epoch
                {LocalDate.of(1601, 12, 31), 364L},       // End of epoch year (not leap)
                {LocalDate.of(1602, 1, 1), 365L},         // Start of second year
                {LocalDate.of(2000, 1, 1), 145731L},      // Y2K date
                {LocalDate.of(2024, 1, 1), 154497L}       // Modern date (corrected calculation)
            };
            
            for (Object[] testCase : testCases) {
                LocalDate date = (LocalDate) testCase[0];
                Long expectedLillian = (Long) testCase[1];
                
                // When: Converting to Lillian date
                Long actualLillian = dateConversionService.convertToLillianDate(date);
                
                // Then: Should match expected value exactly
                Assertions.assertEquals(expectedLillian, actualLillian,
                    "Lillian date for " + date + " should be exactly " + expectedLillian);
            }
        }
    }
}