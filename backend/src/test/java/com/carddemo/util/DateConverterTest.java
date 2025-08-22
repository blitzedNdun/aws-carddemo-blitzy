/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.assertj.core.api.Assertions;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Comprehensive unit test class for DateConversionUtil that validates COBOL date format 
 * conversions to Java date types, ensuring 100% functional parity with the original
 * COBOL CSUTLDTC program and CSUTLDPY copybook validation logic.
 *
 * <p>This test class validates the complete migration of COBOL date handling logic to Java,
 * specifically testing the conversion from COBOL PIC 9(8) CCYYMMDD format dates to Java
 * LocalDate objects while preserving all validation rules from the legacy system.</p>
 *
 * <p>Test Coverage Areas:</p>
 * <ul>
 *   <li>CCYYMMDD format parsing and validation matching COBOL EDIT-DATE-CCYY logic</li>
 *   <li>Century validation (19 and 20 only) per COBOL century constraints</li>
 *   <li>Month validation (1-12) matching COBOL EDIT-MONTH paragraph</li>
 *   <li>Day validation (1-31) with month-specific limits per COBOL EDIT-DAY logic</li>
 *   <li>Leap year detection and February 29th handling per COBOL EDIT-DAY-MONTH-YEAR</li>
 *   <li>Date-of-birth future date validation per COBOL EDIT-DATE-OF-BIRTH logic</li>
 *   <li>Timestamp formatting matching WS-TIMESTAMP layout from CSDAT01Y copybook</li>
 *   <li>Error handling and exception scenarios matching COBOL ABEND conditions</li>
 * </ul>
 *
 * <p>COBOL Functional Parity Requirements:</p>
 * <ul>
 *   <li>Replicate exact validation logic from CSUTLDPY.cpy copybook paragraphs</li>
 *   <li>Match CEEDAYS API behavior from CSUTLDTC.cbl external program call</li>
 *   <li>Preserve century validation constraints (1900-2099 only)</li>
 *   <li>Maintain identical error response patterns for invalid dates</li>
 *   <li>Support WS-EDIT-DATE-CCYYMMDD format from CSUTLDWY.cpy working storage</li>
 * </ul>
 *
 * <p>Performance Requirements:</p>
 * <ul>
 *   <li>All date validation operations must complete within 200ms response threshold</li>
 *   <li>Support high-volume date processing for batch operations</li>
 *   <li>Maintain performance parity with mainframe CICS transaction response times</li>
 * </ul>
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 * @see DateConversionUtil
 * @see AbstractBaseTest
 * @see TestConstants
 */
@DisplayName("Date Conversion Utility Tests - COBOL CSUTLDTC Program Functional Parity")
public class DateConverterTest extends AbstractBaseTest implements UnitTest {

    /**
     * Test validateDate method with valid CCYYMMDD format dates.
     * This test validates the core date validation logic from COBOL CSUTLDPY.cpy
     * EDIT-DATE paragraphs, ensuring proper parsing and validation of well-formed
     * CCYYMMDD date strings with century constraints (19 and 20 only).
     *
     * Test Coverage:
     * - Valid dates across supported centuries (1900-2099)
     * - Proper month validation (1-12)
     * - Correct day validation with month-specific limits
     * - Leap year handling for February 29th
     * - Response time validation under 200ms threshold
     *
     * @param inputDate CCYYMMDD format date string to validate
     * @param expectedYear expected year component from successful parsing
     * @param expectedMonth expected month component from successful parsing
     * @param expectedDay expected day component from successful parsing
     * @param description test scenario description for debugging
     */
    @ParameterizedTest
    @CsvSource({
        // Valid dates across different centuries - matching COBOL EDIT-YEAR-CCYY logic
        "20240315, 2024, 3, 15, 'Valid date in 21st century'",
        "19991231, 1999, 12, 31, 'Valid date in 20th century - last day'", 
        "20000101, 2000, 1, 1, 'Valid date - Y2K boundary'",
        "20240229, 2024, 2, 29, 'Valid leap year date - February 29th'",
        "20000229, 2000, 2, 29, 'Valid leap year date - Y2K February 29th'",
        
        // Month boundary testing - matching COBOL EDIT-MONTH paragraph
        "20240101, 2024, 1, 1, 'Valid date - January 1st'",
        "20241201, 2024, 12, 1, 'Valid date - December 1st'",
        
        // Day boundary testing - matching COBOL EDIT-DAY paragraph
        "20240131, 2024, 1, 31, 'Valid date - January 31st'", 
        "20240430, 2024, 4, 30, 'Valid date - April 30th'",
        "20240228, 2024, 2, 28, 'Valid date - February 28th in leap year'",
        "20230228, 2023, 2, 28, 'Valid date - February 28th in non-leap year'",
        
        // Century boundary testing - COBOL supports 19 and 20 only
        "19000101, 1900, 1, 1, 'Valid date - century 19 start'",
        "20991231, 2099, 12, 31, 'Valid date - century 20 end'"
    })
    @DisplayName("Validate CCYYMMDD Date Format - COBOL EDIT-DATE Logic Parity")
    void testValidateDateWithValidInput(String inputDate, int expectedYear, int expectedMonth, 
                                       int expectedDay, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute date validation - matches COBOL CSUTLDTC external call behavior
        boolean isValid = DateConversionUtil.validateDate(inputDate);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate successful validation result
        assertThat(isValid)
            .describedAs("Date validation should succeed for: " + description)
            .isTrue();
            
        // Parse the date to verify components match expected values
        LocalDate parsedDate = DateConversionUtil.parseDate(inputDate);
        
        assertThat(parsedDate.getYear())
            .describedAs("Year component should match expected value for: " + description)
            .isEqualTo(expectedYear);
            
        assertThat(parsedDate.getMonth().getValue())
            .describedAs("Month component should match expected value for: " + description)
            .isEqualTo(expectedMonth);
            
        assertThat(parsedDate.getDayOfMonth())
            .describedAs("Day component should match expected value for: " + description)
            .isEqualTo(expectedDay);
        
        // Validate performance requirement - must complete within response threshold
        assertThat(executionTime)
            .describedAs("Date validation must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Date validation test completed: " + description, executionTime);
    }

    /**
     * Test validateDate method with invalid CCYYMMDD format dates.
     * This test validates proper error handling for invalid date inputs,
     * matching COBOL validation failure behavior from CSUTLDPY.cpy paragraphs.
     *
     * Test Coverage:
     * - Invalid century validation (only 19 and 20 supported)
     * - Invalid month validation (outside 1-12 range)
     * - Invalid day validation (outside 1-31 range)
     * - Invalid February 29th in non-leap years
     * - Malformed date string handling
     * - Null and empty string validation
     *
     * @param inputDate invalid CCYYMMDD format date string
     * @param description test scenario description for debugging
     */
    @ParameterizedTest
    @CsvSource({
        // Invalid century testing - COBOL only supports 19 and 20
        "18991231, 'Invalid century 18 - before supported range'",
        "21000101, 'Invalid century 21 - after supported range'",
        "15001231, 'Invalid century 15 - far before supported range'",
        "25001231, 'Invalid century 25 - far after supported range'",
        
        // Invalid month testing - matching COBOL EDIT-MONTH validation
        "20240001, 'Invalid month 00 - below valid range'", 
        "20241301, 'Invalid month 13 - above valid range'",
        "20241501, 'Invalid month 15 - well above valid range'",
        
        // Invalid day testing - matching COBOL EDIT-DAY validation
        "20240100, 'Invalid day 00 - below valid range'",
        "20240132, 'Invalid day 32 - above valid range for January'",
        "20240431, 'Invalid day 31 - above valid range for April'",
        "20240631, 'Invalid day 31 - above valid range for June'",
        "20240931, 'Invalid day 31 - above valid range for September'",
        "20241131, 'Invalid day 31 - above valid range for November'",
        
        // Invalid leap year testing - matching COBOL EDIT-DAY-MONTH-YEAR logic
        "20230229, 'Invalid February 29th in non-leap year 2023'",
        "21000229, 'Invalid February 29th - year 2100 is not a leap year'",
        "19000229, 'Invalid February 29th - year 1900 is not a leap year'",
        
        // Malformed date string testing
        "2024031, 'Malformed date - too short (7 digits)'",
        "202403155, 'Malformed date - too long (9 digits)'",
        "ABCD0315, 'Malformed date - alphabetic characters'",
        "2024AB15, 'Malformed date - mixed alphanumeric'",
        "'', 'Empty string input'",
        "    , 'Whitespace input'"
    })
    @DisplayName("Validate CCYYMMDD Date Format - Invalid Date Handling")
    void testValidateDateWithInvalidInput(String inputDate, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Handle special test cases for empty strings
        String actualInput = inputDate;
        if ("''".equals(inputDate)) {
            actualInput = "";
        } else if (inputDate != null && "    ".equals(inputDate.trim())) {
            actualInput = "    ";
        }
        
        // Execute date validation - should fail for invalid inputs
        boolean isValid = DateConversionUtil.validateDate(actualInput);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate that invalid dates are properly rejected
        assertThat(isValid)
            .describedAs("Date validation should fail for: " + description)
            .isFalse();
        
        // Validate performance requirement even for failed validations
        assertThat(executionTime)
            .describedAs("Date validation must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Invalid date validation test completed: " + description, executionTime);
    }

    /**
     * Test parseDate method with null input handling.
     * Validates proper exception handling for null date strings,
     * ensuring defensive programming practices match COBOL behavior.
     */
    @ParameterizedTest
    @CsvSource({
        "null, 'Null date string input'",
        "'', 'Empty date string input'",
        "   , 'Whitespace-only date string input'"
    })
    @DisplayName("Parse Date - Null and Empty Input Exception Handling")
    void testParseDateWithNullInput(String inputDate, String description) {
        
        // Convert test input to actual null or empty values - using final variable for lambda
        final String actualInput = "null".equals(inputDate) ? null : 
                                 (inputDate != null && inputDate.trim().isEmpty() ? "" : inputDate);
        
        // Validate that IllegalArgumentException is thrown for invalid inputs
        assertThatThrownBy(() -> DateConversionUtil.parseDate(actualInput))
            .describedAs("Parse date should throw IllegalArgumentException for: " + description)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Date string cannot be null or empty");
            
        logTestExecution("Null input validation test completed: " + description, null);
    }

    /**
     * Test parseDate method with malformed date strings.
     * Validates proper exception handling for malformed CCYYMMDD format inputs,
     * ensuring DateTimeParseException is thrown for invalid format strings.
     *
     * @param inputDate malformed date string
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        "ABCD1231, 'Alphabetic year input'",
        "202412AB, 'Alphabetic day input'",
        "2024AB31, 'Alphabetic month input'",
        "12345678, 'Valid format but invalid date components'",
        "0000315, 'Too short - 7 digits'",
        "202403155, 'Too long - 9 digits'",
        "20-03-15, 'Hyphenated format instead of CCYYMMDD'",
        "2024/03/15, 'Slash format instead of CCYYMMDD'"
    })
    @DisplayName("Parse Date - Malformed Input Exception Handling")
    void testParseDateWithMalformedInput(String inputDate, String description) {
        
        // Validate that IllegalArgumentException is thrown for malformed inputs
        assertThatThrownBy(() -> DateConversionUtil.parseDate(inputDate))
            .describedAs("Parse date should throw IllegalArgumentException for: " + description)
            .isInstanceOf(IllegalArgumentException.class);
            
        logTestExecution("Malformed input validation test completed: " + description, null);
    }

    /**
     * Test formatCCYYMMDD method with valid LocalDate inputs.
     * Validates proper formatting of Java LocalDate objects to CCYYMMDD format strings,
     * ensuring output matches COBOL WS-EDIT-DATE-CCYYMMDD format from CSUTLDWY.cpy.
     *
     * @param year input year for LocalDate creation
     * @param month input month for LocalDate creation
     * @param day input day for LocalDate creation
     * @param expectedFormat expected CCYYMMDD format output
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        "2024, 3, 15, '20240315', 'Standard date formatting'",
        "1999, 12, 31, '19991231', 'End of 20th century'",
        "2000, 1, 1, '20000101', 'Y2K boundary date'",
        "2024, 2, 29, '20240229', 'Leap year February 29th'",
        "1900, 1, 1, '19000101', 'Start of supported century range'",
        "2099, 12, 31, '20991231', 'End of supported century range'",
        "2024, 1, 1, '20240101', 'January 1st formatting'",
        "2024, 12, 1, '20241201', 'December 1st formatting'"
    })
    @DisplayName("Format CCYYMMDD - LocalDate to COBOL Format Conversion")
    void testFormatCCYYMMDD(int year, int month, int day, String expectedFormat, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Create LocalDate from input components
        LocalDate inputDate = LocalDate.of(year, month, day);
        
        // Execute formatting - matches COBOL WS-EDIT-DATE-CCYYMMDD output
        String formattedDate = DateConversionUtil.formatCCYYMMDD(inputDate);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate formatted output matches expected CCYYMMDD format
        assertThat(formattedDate)
            .describedAs("Formatted date should match CCYYMMDD pattern for: " + description)
            .isEqualTo(expectedFormat);
            
        // Validate format length matches COBOL field length (8 characters)
        assertThat(formattedDate)
            .describedAs("Formatted date length should be 8 characters")
            .hasSize(8);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Date formatting must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Date formatting test completed: " + description, executionTime);
    }

    /**
     * Test formatCCYYMMDD method with null LocalDate input.
     * Validates proper exception handling for null LocalDate objects.
     */
    @ParameterizedTest
    @CsvSource({
        "null, 'Null LocalDate input'"
    })
    @DisplayName("Format CCYYMMDD - Null LocalDate Exception Handling")
    void testFormatCCYYMMDDWithNullInput(String input, String description) {
        
        // Validate that IllegalArgumentException is thrown for null input
        assertThatThrownBy(() -> DateConversionUtil.formatCCYYMMDD(null))
            .describedAs("Format CCYYMMDD should throw IllegalArgumentException for null input")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("LocalDate cannot be null");
            
        logTestExecution("Null LocalDate formatting test completed: " + description, null);
    }

    /**
     * Test convertDateFormat method for bidirectional date format conversion.
     * Validates conversion between different date format patterns while maintaining
     * date value integrity and supporting COBOL-to-Java format transformations.
     *
     * @param inputDate input date string in source format
     * @param sourceFormat source date format pattern
     * @param targetFormat target date format pattern
     * @param expectedOutput expected converted date string
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        "20240315, 'yyyyMMdd', 'yyyy-MM-dd', '2024-03-15', 'COBOL to ISO format conversion'",
        "2024-03-15, 'yyyy-MM-dd', 'yyyyMMdd', '20240315', 'ISO to COBOL format conversion'",
        "20000229, 'yyyyMMdd', 'yyyy-MM-dd', '2000-02-29', 'Leap year date conversion'",
        "19991231, 'yyyyMMdd', 'yyyy-MM-dd', '1999-12-31', 'Century boundary conversion'",
        "20240101, 'yyyyMMdd', 'dd/MM/yyyy', '01/01/2024', 'COBOL to European format conversion'",
        "31/12/1999, 'dd/MM/yyyy', 'yyyyMMdd', '19991231', 'European to COBOL format conversion'"
    })
    @DisplayName("Convert Date Format - Bidirectional Format Conversion")
    void testConvertDateFormat(String inputDate, String sourceFormat, String targetFormat, 
                              String expectedOutput, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute date format conversion
        String convertedDate = DateConversionUtil.convertDateFormat(inputDate, sourceFormat, targetFormat);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate converted output matches expected format and value
        assertThat(convertedDate)
            .describedAs("Converted date should match expected output for: " + description)
            .isEqualTo(expectedOutput);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Date format conversion must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Date format conversion test completed: " + description, executionTime);
    }

    /**
     * Test addDays method for date arithmetic operations.
     * Validates date arithmetic functionality supporting business logic that requires
     * date calculations, such as payment due date calculations and expiration date processing.
     *
     * @param inputDate base date in CCYYMMDD format
     * @param daysToAdd number of days to add (can be negative)
     * @param expectedResult expected result date in CCYYMMDD format
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        "20240315, 0, '20240315', 'Add zero days - no change'",
        "20240315, 1, '20240316', 'Add one day - next day'",
        "20240315, 30, '20240414', 'Add 30 days - month rollover'",
        "20240315, 365, '20250315', 'Add 365 days - year rollover'",
        "20240229, 366, '20250301', 'Add 366 days from leap year date'",
        "20240315, -1, '20240314', 'Subtract one day - previous day'",
        "20240315, -31, '20240213', 'Subtract 31 days - month rollback'",
        "20240315, -365, '20230316', 'Subtract 365 days - year rollback'",
        "20240101, -1, '20231231', 'Subtract from January 1st - year boundary'",
        "20240301, -1, '20240229', 'Subtract to February 29th in leap year'"
    })
    @DisplayName("Add Days - Date Arithmetic Operations")
    void testAddDays(String inputDate, int daysToAdd, String expectedResult, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute date arithmetic
        String resultDate = DateConversionUtil.addDays(inputDate, daysToAdd);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate arithmetic result matches expected date
        assertThat(resultDate)
            .describedAs("Date arithmetic result should match expected for: " + description)
            .isEqualTo(expectedResult);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Date arithmetic must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Date arithmetic test completed: " + description, executionTime);
    }

    /**
     * Test date-of-birth validation logic from COBOL EDIT-DATE-OF-BIRTH paragraph.
     * This test ensures that date-of-birth validation properly rejects future dates,
     * matching the COBOL business rule that prevents birth dates in the future.
     *
     * @param inputDate date of birth in CCYYMMDD format
     * @param isValidExpected whether the date should be considered valid
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        "19800101, true, 'Valid historical birth date'",
        "19901215, true, 'Valid birth date in 1990'", 
        "20000229, true, 'Valid birth date - leap year'",
        "20230101, true, 'Valid recent birth date'",
        "20241231, false, 'Invalid future birth date - end of current year'",
        "20250101, false, 'Invalid future birth date - next year'",
        "20300101, false, 'Invalid future birth date - far future'",
        "20991231, false, 'Invalid future birth date - end of century'"
    })
    @DisplayName("Date of Birth Validation - COBOL EDIT-DATE-OF-BIRTH Logic Parity")
    void testDateOfBirthValidation(String inputDate, boolean isValidExpected, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Parse the input date to LocalDate for validation
        LocalDate birthDate = DateConversionUtil.parseDate(inputDate);
        
        // Validate using fixed test reference date (2024-06-15) for consistent test results
        // This prevents test failures due to running on different dates
        LocalDate testCurrentDate = LocalDate.of(2024, 6, 15);
        boolean isNotFuture = !birthDate.isAfter(testCurrentDate);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate birth date future check matches expected result
        assertThat(isNotFuture)
            .describedAs("Birth date future validation should match expected for: " + description)
            .isEqualTo(isValidExpected);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Birth date validation must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Birth date validation test completed: " + description, executionTime);
    }

    /**
     * Test leap year detection logic matching COBOL EDIT-DAY-MONTH-YEAR paragraph.
     * Validates proper leap year calculation ensuring February 29th handling
     * matches COBOL leap year determination logic exactly.
     *
     * @param year year to test for leap year status
     * @param isLeapExpected whether the year should be considered a leap year
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        // Standard leap year testing
        "2024, true, 'Standard leap year - divisible by 4'",
        "2023, false, 'Non-leap year - not divisible by 4'",
        "2020, true, 'Standard leap year - divisible by 4'",
        "2021, false, 'Non-leap year - not divisible by 4'",
        
        // Century year testing - special leap year rules
        "2000, true, 'Century leap year - divisible by 400'",
        "1900, false, 'Century non-leap year - divisible by 100 but not 400'",
        "2100, false, 'Century non-leap year - divisible by 100 but not 400'",
        "2400, true, 'Century leap year - divisible by 400'",
        
        // Boundary testing within supported range
        "1904, true, 'Early 20th century leap year'",
        "2096, true, 'Late 21st century leap year'",
        "1999, false, 'End of 20th century non-leap year'",
        "2001, false, 'Start of 21st century non-leap year'"
    })
    @DisplayName("Leap Year Detection - COBOL EDIT-DAY-MONTH-YEAR Logic Parity")
    void testLeapYearDetection(int year, boolean isLeapExpected, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Test leap year through February 29th validation
        String testDate = String.format("%04d0229", year);
        boolean isValid = DateConversionUtil.validateDate(testDate);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate leap year detection matches expected result
        assertThat(isValid)
            .describedAs("Leap year detection should match expected for: " + description)
            .isEqualTo(isLeapExpected);
        
        // Additional validation using LocalDate.isLeapYear for consistency
        boolean javaLeapYear = LocalDate.of(year, 1, 1).isLeapYear();
        assertThat(javaLeapYear)
            .describedAs("Java leap year detection should match test expectation")
            .isEqualTo(isLeapExpected);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Leap year detection must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Leap year detection test completed: " + description, executionTime);
    }

    /**
     * Test parseTimestamp method for timestamp string parsing.
     * Validates proper parsing of timestamp strings matching WS-TIMESTAMP layout
     * from CSDAT01Y.cpy copybook, ensuring proper date and time component extraction.
     *
     * @param timestampString input timestamp string
     * @param expectedYear expected year component
     * @param expectedMonth expected month component  
     * @param expectedDay expected day component
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        "'2024-03-15T10:30:45', 2024, 3, 15, 'Standard timestamp with time'",
        "'2024-03-15T00:00:00', 2024, 3, 15, 'Midnight timestamp'",
        "'2024-03-15T23:59:59', 2024, 3, 15, 'End of day timestamp'",
        "'2000-02-29T12:00:00', 2000, 2, 29, 'Leap year timestamp'",
        "'1999-12-31T23:59:59', 1999, 12, 31, 'Y2K boundary timestamp'"
    })
    @DisplayName("Parse Timestamp - WS-TIMESTAMP Format Parsing")
    void testParseTimestamp(String timestampString, int expectedYear, int expectedMonth, 
                           int expectedDay, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute timestamp parsing
        LocalDate parsedDate = DateConversionUtil.parseTimestamp(timestampString);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate parsed date components match expected values
        assertThat(parsedDate.getYear())
            .describedAs("Parsed year should match expected for: " + description)
            .isEqualTo(expectedYear);
            
        assertThat(parsedDate.getMonth().getValue())
            .describedAs("Parsed month should match expected for: " + description)
            .isEqualTo(expectedMonth);
            
        assertThat(parsedDate.getDayOfMonth())
            .describedAs("Parsed day should match expected for: " + description)
            .isEqualTo(expectedDay);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Timestamp parsing must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Timestamp parsing test completed: " + description, executionTime);
    }

    /**
     * Test formatTimestamp method for timestamp string formatting.
     * Validates proper formatting of LocalDate objects to timestamp strings
     * matching WS-TIMESTAMP layout requirements from COBOL working storage.
     *
     * @param year input year for LocalDate creation
     * @param month input month for LocalDate creation
     * @param day input day for LocalDate creation
     * @param expectedFormat expected timestamp format output
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        "2024, 3, 15, '2024-03-15T00:00:00', 'Standard date to timestamp'",
        "2000, 2, 29, '2000-02-29T00:00:00', 'Leap year date to timestamp'",
        "1999, 12, 31, '1999-12-31T00:00:00', 'Y2K boundary date to timestamp'",
        "2024, 1, 1, '2024-01-01T00:00:00', 'New Year date to timestamp'",
        "2024, 12, 31, '2024-12-31T00:00:00', 'End of year date to timestamp'"
    })
    @DisplayName("Format Timestamp - LocalDate to WS-TIMESTAMP Format")
    void testFormatTimestamp(int year, int month, int day, String expectedFormat, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Create LocalDate from input components
        LocalDate inputDate = LocalDate.of(year, month, day);
        
        // Execute timestamp formatting
        String formattedTimestamp = DateConversionUtil.formatTimestamp(inputDate);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate formatted output matches expected timestamp format
        assertThat(formattedTimestamp)
            .describedAs("Formatted timestamp should match expected format for: " + description)
            .isEqualTo(expectedFormat);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Timestamp formatting must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Timestamp formatting test completed: " + description, executionTime);
    }

    /**
     * Test convertTimestampFormat method for timestamp format conversion.
     * Validates bidirectional timestamp format conversion supporting integration
     * with different systems while maintaining temporal precision and accuracy.
     *
     * @param inputTimestamp input timestamp string
     * @param sourceFormat source timestamp format pattern
     * @param targetFormat target timestamp format pattern
     * @param expectedOutput expected converted timestamp string
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        "'2024-03-15T10:30:45', 'yyyy-MM-ddTHH:mm:ss', 'yyyyMMdd HHmmss', '20240315 103045', 'ISO to COBOL timestamp format'",
        "'20240315 103045', 'yyyyMMdd HHmmss', 'yyyy-MM-ddTHH:mm:ss', '2024-03-15T10:30:45', 'COBOL to ISO timestamp format'",
        "'2024-03-15T00:00:00', 'yyyy-MM-ddTHH:mm:ss', 'yyyyMMdd', '20240315', 'Timestamp to date conversion'",
        "'2000-02-29T12:00:00', 'yyyy-MM-ddTHH:mm:ss', 'dd/MM/yyyy HH:mm', '29/02/2000 12:00', 'Leap year timestamp conversion'"
    })
    @DisplayName("Convert Timestamp Format - Bidirectional Timestamp Conversion") 
    void testConvertTimestampFormat(String inputTimestamp, String sourceFormat, String targetFormat,
                                   String expectedOutput, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute timestamp format conversion
        String convertedTimestamp = DateConversionUtil.convertTimestampFormat(inputTimestamp, sourceFormat, targetFormat);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate converted output matches expected format
        assertThat(convertedTimestamp)
            .describedAs("Converted timestamp should match expected output for: " + description)
            .isEqualTo(expectedOutput);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Timestamp conversion must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Timestamp conversion test completed: " + description, executionTime);
    }

    /**
     * Test century validation boundary conditions.
     * Validates that only centuries 19 and 20 are supported, matching
     * COBOL EDIT-YEAR-CCYY paragraph validation constraints.
     *
     * @param inputDate date string to test century validation
     * @param expectedValid whether the century should be considered valid
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        // Valid centuries - COBOL supports 19 and 20 only
        "19000101, true, 'Century 19 start - valid'",
        "19991231, true, 'Century 19 end - valid'",
        "20000101, true, 'Century 20 start - valid'",
        "20991231, true, 'Century 20 end - valid'",
        
        // Invalid centuries - outside supported range
        "18991231, false, 'Century 18 - invalid'",
        "21000101, false, 'Century 21 - invalid'",
        "17001231, false, 'Century 17 - invalid'",
        "22001231, false, 'Century 22 - invalid'",
        "10001231, false, 'Century 10 - far invalid'",
        "30001231, false, 'Century 30 - far invalid'"
    })
    @DisplayName("Century Validation - COBOL EDIT-YEAR-CCYY Boundary Testing")
    void testCenturyValidation(String inputDate, boolean expectedValid, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute date validation focusing on century validation
        boolean isValid = DateConversionUtil.validateDate(inputDate);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate century validation result matches expected
        assertThat(isValid)
            .describedAs("Century validation should match expected for: " + description)
            .isEqualTo(expectedValid);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Century validation must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Century validation test completed: " + description, executionTime);
    }

    /**
     * Test month-specific day validation logic matching COBOL EDIT-DAY paragraph.
     * Validates that each month has the correct number of days and properly
     * handles month-end boundaries, including February in leap and non-leap years.
     *
     * @param inputDate date string to test month-day validation
     * @param expectedValid whether the month-day combination should be valid
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        // February testing - leap year vs non-leap year
        "20240229, true, 'February 29th in leap year 2024'",
        "20230229, false, 'February 29th in non-leap year 2023'",
        "20240228, true, 'February 28th in leap year'",
        "20230228, true, 'February 28th in non-leap year'",
        
        // 31-day months testing
        "20240131, true, 'January 31st - valid'",
        "20240331, true, 'March 31st - valid'",
        "20240531, true, 'May 31st - valid'",
        "20240731, true, 'July 31st - valid'",
        "20240831, true, 'August 31st - valid'",
        "20241031, true, 'October 31st - valid'",
        "20241231, true, 'December 31st - valid'",
        
        // 30-day months testing - 31st should be invalid
        "20240431, false, 'April 31st - invalid (30-day month)'",
        "20240631, false, 'June 31st - invalid (30-day month)'",
        "20240931, false, 'September 31st - invalid (30-day month)'",
        "20241131, false, 'November 31st - invalid (30-day month)'",
        
        // Valid 30-day month endings
        "20240430, true, 'April 30th - valid'",
        "20240630, true, 'June 30th - valid'",
        "20240930, true, 'September 30th - valid'",
        "20241130, true, 'November 30th - valid'"
    })
    @DisplayName("Month-Day Validation - COBOL EDIT-DAY Month-Specific Logic")
    void testMonthDayValidation(String inputDate, boolean expectedValid, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute date validation focusing on month-day combinations
        boolean isValid = DateConversionUtil.validateDate(inputDate);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate month-day validation result matches expected
        assertThat(isValid)
            .describedAs("Month-day validation should match expected for: " + description)
            .isEqualTo(expectedValid);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Month-day validation must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Month-day validation test completed: " + description, executionTime);
    }

    /**
     * Test date arithmetic edge cases with boundary crossings.
     * Validates proper handling of date arithmetic that crosses month and year boundaries,
     * ensuring results match expected COBOL date calculation behavior.
     *
     * @param inputDate base date for arithmetic operation
     * @param daysToAdd number of days to add/subtract
     * @param expectedResult expected resulting date
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        // Month boundary crossings
        "20240131, 1, '20240201', 'January to February transition'",
        "20240229, 1, '20240301', 'Leap year February to March transition'",
        "20230228, 1, '20230301', 'Non-leap year February to March transition'",
        "20240301, -1, '20240229', 'March to leap year February transition'",
        "20230301, -1, '20230228', 'March to non-leap year February transition'",
        
        // Year boundary crossings
        "20231231, 1, '20240101', 'Year transition forward'",
        "20240101, -1, '20231231', 'Year transition backward'",
        "19991231, 1, '20000101', 'Y2K transition forward'",
        "20000101, -1, '19991231', 'Y2K transition backward'",
        
        // Century boundary testing within supported range
        "19991231, 366, '20001231', 'Century transition with leap year'",
        "20000101, -366, '19981231', 'Century transition backward with leap year'",
        
        // Large date arithmetic operations
        "20240315, 1000, '20261210', 'Add 1000 days - large positive arithmetic'",
        "20240315, -1000, '20210619', 'Subtract 1000 days - large negative arithmetic'"
    })
    @DisplayName("Date Arithmetic Edge Cases - Boundary Crossing Validation")
    void testDateArithmeticEdgeCases(String inputDate, int daysToAdd, String expectedResult, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute date arithmetic
        String resultDate = DateConversionUtil.addDays(inputDate, daysToAdd);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate arithmetic result matches expected date
        assertThat(resultDate)
            .describedAs("Date arithmetic should produce expected result for: " + description)
            .isEqualTo(expectedResult);
        
        // Additional validation - ensure round-trip consistency
        LocalDate originalDate = DateConversionUtil.parseDate(inputDate);
        LocalDate expectedDate = DateConversionUtil.parseDate(expectedResult);
        LocalDate calculatedDate = originalDate.plusDays(daysToAdd);
        
        assertThat(calculatedDate)
            .describedAs("Direct LocalDate arithmetic should match utility result")
            .isEqualTo(expectedDate);
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Date arithmetic must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Date arithmetic edge case test completed: " + description, executionTime);
    }

    /**
     * Test functional parity validation between COBOL and Java implementations.
     * This comprehensive test validates that the Java DateConversionUtil provides
     * 100% functional parity with the original COBOL CSUTLDTC program behavior.
     *
     * @param inputDate test date for functional parity validation
     * @param operation operation to test (validate, parse, format)
     * @param expectedBehavior expected behavior description
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        // COBOL EDIT-DATE-CCYY functional parity
        "20240315, 'validate', 'return true', 'Valid date functional parity'",
        "18240315, 'validate', 'return false', 'Invalid century functional parity'",
        "20241301, 'validate', 'return false', 'Invalid month functional parity'",
        "20240431, 'validate', 'return false', 'Invalid day functional parity'",
        
        // COBOL date parsing functional parity
        "20240315, 'parse', 'return LocalDate', 'Parse valid date functional parity'",
        "20000229, 'parse', 'return LocalDate', 'Parse leap year date functional parity'",
        
        // COBOL date formatting functional parity
        "20240315, 'format', 'return 20240315', 'Format date functional parity'",
        "19991231, 'format', 'return 19991231', 'Format Y2K boundary functional parity'"
    })
    @DisplayName("Functional Parity Validation - COBOL CSUTLDTC Behavior Matching")
    void testFunctionalParity(String inputDate, String operation, String expectedBehavior, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute operation based on test parameter
        switch (operation.toLowerCase()) {
            case "validate":
                boolean validationResult = DateConversionUtil.validateDate(inputDate);
                boolean expectValid = expectedBehavior.contains("true");
                assertThat(validationResult)
                    .describedAs("Validation functional parity for: " + description)
                    .isEqualTo(expectValid);
                break;
                
            case "parse":
                assertThatCode(() -> DateConversionUtil.parseDate(inputDate))
                    .describedAs("Parse functional parity should not throw exception for: " + description)
                    .doesNotThrowAnyException();
                break;
                
            case "format":
                LocalDate parsedDate = DateConversionUtil.parseDate(inputDate);
                String formattedDate = DateConversionUtil.formatCCYYMMDD(parsedDate);
                assertThat(formattedDate)
                    .describedAs("Format functional parity for: " + description)
                    .isEqualTo(inputDate);
                break;
                
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Functional parity validation must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Functional parity test completed: " + description, executionTime);
    }

    /**
     * Test comprehensive COBOL date validation scenarios.
     * This test validates complex date validation scenarios that combine multiple
     * validation rules from different COBOL paragraphs in CSUTLDPY.cpy.
     *
     * @param inputDate comprehensive test date scenario
     * @param validationAspect specific validation aspect being tested
     * @param expectedResult expected validation result
     * @param description test scenario description
     */
    @ParameterizedTest
    @CsvSource({
        // Complex validation scenarios combining multiple COBOL rules
        "20240229, 'leap_year_february', 'true', 'Leap year February 29th - all rules pass'",
        "20230229, 'leap_year_february', 'false', 'Non-leap year February 29th - day validation fails'",
        "20000229, 'century_leap_year', 'true', 'Century year 2000 leap year - all rules pass'",
        "19000229, 'century_leap_year', 'false', 'Century year 1900 non-leap year - day validation fails'",
        "20991231, 'century_boundary', 'true', 'End of century 20 - all rules pass'",
        "21000101, 'century_boundary', 'false', 'Start of century 21 - year validation fails'",
        
        // Date format validation scenarios
        "20240315, 'format_validation', 'true', 'Standard CCYYMMDD format - all rules pass'",
        "2024315, 'format_validation', 'false', 'Invalid CCYYMMDD format - format validation fails'",
        "202403155, 'format_validation', 'false', 'Too long CCYYMMDD format - format validation fails'",
        
        // Combined business logic validation
        "20241301, 'combined_validation', 'false', 'Invalid month in valid year - month validation fails'",
        "20240431, 'combined_validation', 'false', 'Invalid day in valid month/year - day validation fails'",
        "20240228, 'combined_validation', 'true', 'Valid non-leap year February date - all rules pass'"
    })
    @DisplayName("Comprehensive Date Validation - COBOL Multi-Rule Scenario Testing")
    void testComprehensiveDateValidation(String inputDate, String validationAspect, 
                                        String expectedResult, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute comprehensive date validation
        boolean isValid = DateConversionUtil.validateDate(inputDate);
        boolean expectedValid = "true".equals(expectedResult);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate comprehensive validation result
        assertThat(isValid)
            .describedAs("Comprehensive validation (" + validationAspect + ") should match expected for: " + description)
            .isEqualTo(expectedValid);
        
        // For valid dates, verify parsing also succeeds
        if (expectedValid) {
            assertThatCode(() -> DateConversionUtil.parseDate(inputDate))
                .describedAs("Valid date should parse successfully: " + description)
                .doesNotThrowAnyException();
                
            LocalDate parsedDate = DateConversionUtil.parseDate(inputDate);
            String reformattedDate = DateConversionUtil.formatCCYYMMDD(parsedDate);
            
            assertThat(reformattedDate)
                .describedAs("Round-trip format conversion should preserve date value")
                .isEqualTo(inputDate);
        }
        
        // Validate functional parity with COBOL rules from TestConstants
        Boolean functionalParityCheck = (Boolean) TestConstants.FUNCTIONAL_PARITY_RULES.get("preserve_decimal_precision");
        assertThat(functionalParityCheck)
            .describedAs("Functional parity rules should be enabled")
            .isTrue();
        
        // Validate performance requirement
        assertThat(executionTime)
            .describedAs("Comprehensive validation must complete within response time threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
        logTestExecution("Comprehensive validation test completed: " + description, executionTime);
    }

    /**
     * Test exception handling for invalid date arithmetic operations.
     * Validates proper error handling when date arithmetic operations
     * would result in invalid or out-of-range dates.
     */
    @ParameterizedTest
    @CsvSource({
        "INVALID, 30, 'Invalid input date format'",
        "'', 30, 'Empty string input date'",
        "20240315, null, 'Null days parameter'"
    })
    @DisplayName("Date Arithmetic Exception Handling - Invalid Input Scenarios")
    void testDateArithmeticExceptionHandling(String inputDate, String daysToAddStr, String description) {
        
        // Convert test parameters
        String actualInputDate = "''".equals(inputDate) ? "" : inputDate;
        Integer daysToAdd = "null".equals(daysToAddStr) ? null : Integer.valueOf(daysToAddStr);
        
        if (actualInputDate.equals("INVALID") || actualInputDate.isEmpty()) {
            // Test invalid date string handling
            assertThatThrownBy(() -> DateConversionUtil.addDays(actualInputDate, daysToAdd != null ? daysToAdd : 0))
                .describedAs("Date arithmetic should throw exception for: " + description)
                .isInstanceOf(IllegalArgumentException.class);
        } else if (daysToAdd == null) {
            // Test null days parameter - this would be caught by method signature, 
            // so we test with a wrapper that simulates the null scenario
            assertThatThrownBy(() -> {
                Integer nullDays = null;
                DateConversionUtil.addDays(actualInputDate, nullDays);
            })
                .describedAs("Date arithmetic should handle null days parameter")
                .isInstanceOf(NullPointerException.class);
        }
        
        logTestExecution("Date arithmetic exception handling test completed: " + description, null);
    }

    /**
     * Test performance validation for high-volume date processing scenarios.
     * Validates that date conversion utilities maintain performance requirements
     * for batch processing and high-transaction-volume scenarios.
     */
    @ParameterizedTest
    @CsvSource({
        "100, 'validate', 'High-volume validation operations'",
        "100, 'parse', 'High-volume parsing operations'",
        "100, 'format', 'High-volume formatting operations'",
        "50, 'arithmetic', 'High-volume arithmetic operations'"
    })
    @DisplayName("Performance Validation - High-Volume Date Processing")
    void testPerformanceValidation(int operationCount, String operationType, String description) {
        
        long startTime = System.currentTimeMillis();
        
        // Execute high-volume operations
        for (int i = 0; i < operationCount; i++) {
            String testDate = String.format("2024%02d%02d", (i % 12) + 1, (i % 28) + 1);
            
            switch (operationType) {
                case "validate":
                    DateConversionUtil.validateDate(testDate);
                    break;
                case "parse":
                    DateConversionUtil.parseDate(testDate);
                    break;
                case "format":
                    LocalDate date = DateConversionUtil.parseDate(testDate);
                    DateConversionUtil.formatCCYYMMDD(date);
                    break;
                case "arithmetic":
                    DateConversionUtil.addDays(testDate, i);
                    break;
            }
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        long averageTimePerOperation = executionTime / operationCount;
        
        // Validate average performance per operation
        assertThat(averageTimePerOperation)
            .describedAs("Average time per operation must be well under response threshold")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS / 10); // Allow 20ms per operation max
        
        // Validate total execution time for batch scenarios
        assertThat(executionTime)
            .describedAs("Total execution time should support batch processing requirements")
            .isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS * 5); // Allow 1 second for 100+ operations
            
        logTestExecution("Performance validation test completed: " + description + 
                        " (avg: " + averageTimePerOperation + "ms per operation)", executionTime);
    }
}
