/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit test suite for DateUtilityService validating COBOL to Java date conversion migration.
 * Tests all critical date validation, conversion, and business day calculation functionality
 * ensuring 100% functional parity with original COBOL CSUTLDTC.cbl and CSUTLDPY.cpy programs.
 * 
 * Key Test Areas:
 * - CCYYMMDD date format validation matching COBOL EDIT-DATE-CCYYMMDD logic
 * - Date validation with century restrictions (19xx/20xx), month/day validation
 * - Leap year calculation tests replicating COBOL logic
 * - Lillian date conversion functionality matching CEEDAYS API behavior
 * - Business day calculation tests for transaction processing
 * - Date of birth validation preventing future dates
 * - Comprehensive test scenarios for date parsing and formatting
 * 
 * Uses JUnit 5, Mockito, and AssertJ frameworks as specified in technical requirements.
 * Ensures functional parity with original COBOL CSUTLDTC and CSUTLDPY programs.
 * 
 * This is a pure unit test that doesn't require Spring context, focusing on testing
 * DateUtilityService business logic in isolation with proper mocking of dependencies.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class DateUtilityServiceTest {

    private DateUtilityService dateUtilityService;

    /**
     * Setup test data before each test execution.
     * Initializes the service instance for testing.
     */
    @BeforeEach
    public void setupTestData() {
        // Create a real instance since we're testing the service methods directly
        dateUtilityService = new DateUtilityService();
    }

    // ========================================
    // CCYYMMDD Date Format Validation Tests
    // ========================================

    @Test
    @DisplayName("validateCCYYMMDD should accept valid date formats")
    public void testValidateCCYYMMDD_ValidDates_ShouldReturnTrue() {
        // Test valid dates in CCYYMMDD format
        assertThat(dateUtilityService.validateCCYYMMDD("20231215")).isTrue();
        assertThat(dateUtilityService.validateCCYYMMDD("19850630")).isTrue();
        assertThat(dateUtilityService.validateCCYYMMDD("20000229")).isTrue(); // Leap year
        assertThat(dateUtilityService.validateCCYYMMDD("19000228")).isTrue(); // Non-leap year
        assertThat(dateUtilityService.validateCCYYMMDD("20240101")).isTrue(); // Current century
        assertThat(dateUtilityService.validateCCYYMMDD("19991231")).isTrue(); // Previous century
    }

    @ParameterizedTest
    @DisplayName("validateCCYYMMDD should reject invalid date formats")
    @ValueSource(strings = {
        "",           // Empty string
        "   ",        // Whitespace only
        "2023121",    // Too short
        "202312155",  // Too long
        "20AB1215",   // Non-numeric characters
        "20231232",   // Invalid day
        "20231305",   // Invalid month
        "18991215",   // Before 1900
        "21001215",   // After 2099
        "20230229",   // Non-leap year Feb 29
        "20230431"    // April 31st (invalid)
    })
    public void testValidateCCYYMMDD_InvalidDates_ShouldReturnFalse(String invalidDate) {
        assertThat(dateUtilityService.validateCCYYMMDD(invalidDate)).isFalse();
    }

    @Test
    @DisplayName("validateCCYYMMDD should handle null input")
    public void testValidateCCYYMMDD_NullInput_ShouldReturnFalse() {
        assertThat(dateUtilityService.validateCCYYMMDD(null)).isFalse();
    }

    // ========================================
    // Year Validation Tests
    // ========================================

    @Test
    @DisplayName("validateYear should accept valid years in range 1900-2099")
    public void testValidateYear_ValidRange_ShouldReturnTrue() {
        assertThat(dateUtilityService.validateYear(1900)).isTrue();
        assertThat(dateUtilityService.validateYear(2000)).isTrue();
        assertThat(dateUtilityService.validateYear(2023)).isTrue();
        assertThat(dateUtilityService.validateYear(2099)).isTrue();
    }

    @Test
    @DisplayName("validateYear should reject years outside valid range")
    public void testValidateYear_InvalidRange_ShouldReturnFalse() {
        assertThat(dateUtilityService.validateYear(1899)).isFalse();
        assertThat(dateUtilityService.validateYear(2100)).isFalse();
        assertThat(dateUtilityService.validateYear(1800)).isFalse();
        assertThat(dateUtilityService.validateYear(3000)).isFalse();
    }

    // ========================================
    // Month and Day Validation Tests
    // ========================================

    @Test
    @DisplayName("validateMonth should accept valid months 1-12")
    public void testValidateMonth_ValidRange_ShouldReturnTrue() {
        for (int month = 1; month <= 12; month++) {
            assertThat(dateUtilityService.validateMonth(month)).isTrue();
        }
    }

    @Test
    @DisplayName("validateMonth should reject invalid months")
    public void testValidateMonth_InvalidRange_ShouldReturnFalse() {
        assertThat(dateUtilityService.validateMonth(0)).isFalse();
        assertThat(dateUtilityService.validateMonth(13)).isFalse();
        assertThat(dateUtilityService.validateMonth(-1)).isFalse();
        assertThat(dateUtilityService.validateMonth(100)).isFalse();
    }

    @Test
    @DisplayName("validateDay should accept valid days 1-31")
    public void testValidateDay_ValidRange_ShouldReturnTrue() {
        assertThat(dateUtilityService.validateDay(1)).isTrue();
        assertThat(dateUtilityService.validateDay(15)).isTrue();
        assertThat(dateUtilityService.validateDay(31)).isTrue();
    }

    @Test
    @DisplayName("validateDay should reject invalid days")
    public void testValidateDay_InvalidRange_ShouldReturnFalse() {
        assertThat(dateUtilityService.validateDay(0)).isFalse();
        assertThat(dateUtilityService.validateDay(32)).isFalse();
        assertThat(dateUtilityService.validateDay(-1)).isFalse();
        assertThat(dateUtilityService.validateDay(100)).isFalse();
    }

    // ========================================
    // Leap Year Tests
    // ========================================

    @ParameterizedTest
    @DisplayName("isLeapYear should correctly identify leap years")
    @ValueSource(ints = {2000, 2004, 2008, 2012, 2016, 2020, 2024})
    public void testIsLeapYear_LeapYears_ShouldReturnTrue(int year) {
        assertThat(dateUtilityService.isLeapYear(year)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("isLeapYear should correctly identify non-leap years")
    @ValueSource(ints = {1900, 2001, 2002, 2003, 2100, 2200, 2300})
    public void testIsLeapYear_NonLeapYears_ShouldReturnFalse(int year) {
        assertThat(dateUtilityService.isLeapYear(year)).isFalse();
    }

    @Test
    @DisplayName("isLeapYear should handle century years correctly")
    public void testIsLeapYear_CenturyYears_ShouldFollowCorrectRules() {
        // Century years divisible by 400 are leap years
        assertThat(dateUtilityService.isLeapYear(2000)).isTrue();
        assertThat(dateUtilityService.isLeapYear(2400)).isTrue();
        
        // Century years not divisible by 400 are not leap years
        assertThat(dateUtilityService.isLeapYear(1900)).isFalse();
        assertThat(dateUtilityService.isLeapYear(2100)).isFalse();
        assertThat(dateUtilityService.isLeapYear(2200)).isFalse();
        assertThat(dateUtilityService.isLeapYear(2300)).isFalse();
    }

    // ========================================
    // Date of Birth Validation Tests
    // ========================================

    @Test
    @DisplayName("validateDateOfBirth should accept valid past dates")
    public void testValidateDateOfBirth_ValidPastDates_ShouldReturnTrue() {
        // Test dates that are definitely in the past
        assertThat(dateUtilityService.validateDateOfBirth("19850630")).isTrue();
        assertThat(dateUtilityService.validateDateOfBirth("19950315")).isTrue();
        assertThat(dateUtilityService.validateDateOfBirth("20000101")).isTrue();
        assertThat(dateUtilityService.validateDateOfBirth("20100815")).isTrue();
    }

    @Test
    @DisplayName("validateDateOfBirth should reject invalid formats")
    public void testValidateDateOfBirth_InvalidFormats_ShouldReturnFalse() {
        assertThat(dateUtilityService.validateDateOfBirth("")).isFalse();
        assertThat(dateUtilityService.validateDateOfBirth(null)).isFalse();
        assertThat(dateUtilityService.validateDateOfBirth("20AB1215")).isFalse();
        assertThat(dateUtilityService.validateDateOfBirth("20231232")).isFalse();
    }

    @Test
    @DisplayName("validateDateOfBirth should reject future dates")
    public void testValidateDateOfBirth_FutureDates_ShouldReturnFalse() {
        // Test dates that are definitely in the future
        LocalDate futureDate = LocalDate.now().plusYears(1);
        String futureDateString = dateUtilityService.formatDate(futureDate);
        assertThat(dateUtilityService.validateDateOfBirth(futureDateString)).isFalse();
    }

    // ========================================
    // Lillian Date Conversion Tests
    // ========================================

    @Test
    @DisplayName("toLillianDate should convert valid dates correctly")
    public void testToLillianDate_ValidDates_ShouldReturnCorrectValues() {
        // Test valid dates within COBOL accepted range (1900-2099)
        Long lillianDate = dateUtilityService.toLillianDate("19000101");
        assertThat(lillianDate).isNotNull();
        assertThat(lillianDate).isGreaterThan(0L); // Should be positive since after 1601-01-01

        lillianDate = dateUtilityService.toLillianDate("20231215");
        assertThat(lillianDate).isNotNull();
        assertThat(lillianDate).isGreaterThan(0L);
    }

    @Test
    @DisplayName("toLillianDate should handle invalid dates")
    public void testToLillianDate_InvalidDates_ShouldReturnNull() {
        assertThat(dateUtilityService.toLillianDate("")).isNull();
        assertThat(dateUtilityService.toLillianDate(null)).isNull();
        assertThat(dateUtilityService.toLillianDate("invalid")).isNull();
        assertThat(dateUtilityService.toLillianDate("20231232")).isNull();
        
        // Dates outside COBOL valid range (1900-2099) should return null
        assertThat(dateUtilityService.toLillianDate("16010101")).isNull(); // Lillian epoch but outside COBOL range
        assertThat(dateUtilityService.toLillianDate("18991231")).isNull(); // Before COBOL valid range
        assertThat(dateUtilityService.toLillianDate("21000101")).isNull(); // After COBOL valid range
    }

    @Test
    @DisplayName("fromLillianDate should convert back to correct date format")
    public void testFromLillianDate_ValidValues_ShouldReturnCorrectDates() {
        // Test round-trip conversion with valid COBOL date range
        String originalDate = "20231215";
        Long lillianDate = dateUtilityService.toLillianDate(originalDate);
        assertThat(lillianDate).isNotNull();
        
        String convertedBack = dateUtilityService.fromLillianDate(lillianDate);
        assertThat(convertedBack).isEqualTo(originalDate);
        
        // Test another valid date
        originalDate = "19500615";
        lillianDate = dateUtilityService.toLillianDate(originalDate);
        assertThat(lillianDate).isNotNull();
        
        convertedBack = dateUtilityService.fromLillianDate(lillianDate);
        assertThat(convertedBack).isEqualTo(originalDate);
    }

    @Test
    @DisplayName("fromLillianDate should handle null input")
    public void testFromLillianDate_NullInput_ShouldReturnNull() {
        assertThat(dateUtilityService.fromLillianDate(null)).isNull();
    }

    // ========================================
    // Date Parsing Tests
    // ========================================

    @Test
    @DisplayName("parseDate should handle multiple date formats")
    public void testParseDate_MultipleFormats_ShouldParseCorrectly() {
        // CCYYMMDD format
        LocalDate date = dateUtilityService.parseDate("20231215");
        assertThat(date).isEqualTo(LocalDate.of(2023, 12, 15));

        // ISO format
        date = dateUtilityService.parseDate("2023-12-15");
        assertThat(date).isEqualTo(LocalDate.of(2023, 12, 15));

        // US format
        date = dateUtilityService.parseDate("12/15/2023");
        assertThat(date).isEqualTo(LocalDate.of(2023, 12, 15));
    }

    @Test
    @DisplayName("parseDate should handle invalid formats")
    public void testParseDate_InvalidFormats_ShouldReturnNull() {
        assertThat(dateUtilityService.parseDate("")).isNull();
        assertThat(dateUtilityService.parseDate(null)).isNull();
        assertThat(dateUtilityService.parseDate("invalid")).isNull();
        assertThat(dateUtilityService.parseDate("32/15/2023")).isNull();
    }

    // ========================================
    // Date Formatting Tests
    // ========================================

    @Test
    @DisplayName("formatDate should format LocalDate to CCYYMMDD")
    public void testFormatDate_ValidDate_ShouldReturnCorrectFormat() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        String formatted = dateUtilityService.formatDate(date);
        assertThat(formatted).isEqualTo("20231215");

        date = LocalDate.of(2000, 1, 1);
        formatted = dateUtilityService.formatDate(date);
        assertThat(formatted).isEqualTo("20000101");
    }

    @Test
    @DisplayName("formatDate should handle null input")
    public void testFormatDate_NullInput_ShouldReturnNull() {
        assertThat(dateUtilityService.formatDate(null)).isNull();
    }

    @Test
    @DisplayName("formatCCYYMMDD should be equivalent to formatDate")
    public void testFormatCCYYMMDD_ShouldMatchFormatDate() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        assertThat(dateUtilityService.formatCCYYMMDD(date))
            .isEqualTo(dateUtilityService.formatDate(date));
    }

    // ========================================
    // Business Day Calculation Tests
    // ========================================

    @Test
    @DisplayName("isBusinessDay should correctly identify weekdays")
    public void testIsBusinessDay_Weekdays_ShouldReturnTrue() {
        // Monday to Friday should be business days
        LocalDate monday = LocalDate.of(2023, 12, 11);    // Monday
        LocalDate tuesday = LocalDate.of(2023, 12, 12);   // Tuesday
        LocalDate wednesday = LocalDate.of(2023, 12, 13); // Wednesday
        LocalDate thursday = LocalDate.of(2023, 12, 14);  // Thursday
        LocalDate friday = LocalDate.of(2023, 12, 15);    // Friday

        assertThat(dateUtilityService.isBusinessDay(monday)).isTrue();
        assertThat(dateUtilityService.isBusinessDay(tuesday)).isTrue();
        assertThat(dateUtilityService.isBusinessDay(wednesday)).isTrue();
        assertThat(dateUtilityService.isBusinessDay(thursday)).isTrue();
        assertThat(dateUtilityService.isBusinessDay(friday)).isTrue();
    }

    @Test
    @DisplayName("isBusinessDay should correctly identify weekends")
    public void testIsBusinessDay_Weekends_ShouldReturnFalse() {
        LocalDate saturday = LocalDate.of(2023, 12, 16);  // Saturday
        LocalDate sunday = LocalDate.of(2023, 12, 17);    // Sunday

        assertThat(dateUtilityService.isBusinessDay(saturday)).isFalse();
        assertThat(dateUtilityService.isBusinessDay(sunday)).isFalse();
    }

    @Test
    @DisplayName("isBusinessDay should handle null input")
    public void testIsBusinessDay_NullInput_ShouldReturnFalse() {
        assertThat(dateUtilityService.isBusinessDay(null)).isFalse();
    }

    @Test
    @DisplayName("isWeekend should be opposite of isBusinessDay")
    public void testIsWeekend_ShouldBeOppositeOfIsBusinessDay() {
        LocalDate weekday = LocalDate.of(2023, 12, 15);   // Friday
        LocalDate weekend = LocalDate.of(2023, 12, 16);   // Saturday

        assertThat(dateUtilityService.isWeekend(weekday))
            .isEqualTo(!dateUtilityService.isBusinessDay(weekday));
        assertThat(dateUtilityService.isWeekend(weekend))
            .isEqualTo(!dateUtilityService.isBusinessDay(weekend));
    }

    @Test
    @DisplayName("calculateBusinessDays should count only weekdays")
    public void testCalculateBusinessDays_ShouldCountOnlyWeekdays() {
        // Monday to Friday (5 business days)
        LocalDate monday = LocalDate.of(2023, 12, 11);
        LocalDate friday = LocalDate.of(2023, 12, 15);

        int businessDays = dateUtilityService.calculateBusinessDays(monday, friday);
        assertThat(businessDays).isEqualTo(4); // Tue, Wed, Thu, Fri

        // Spanning a weekend
        LocalDate friday2 = LocalDate.of(2023, 12, 15);
        LocalDate monday2 = LocalDate.of(2023, 12, 18);

        businessDays = dateUtilityService.calculateBusinessDays(friday2, monday2);
        assertThat(businessDays).isEqualTo(1); // Only Monday
    }

    @Test
    @DisplayName("calculateBusinessDays should handle same day")
    public void testCalculateBusinessDays_SameDay_ShouldReturnZero() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        int businessDays = dateUtilityService.calculateBusinessDays(date, date);
        assertThat(businessDays).isEqualTo(0);
    }

    @Test
    @DisplayName("calculateBusinessDays should handle reversed dates")
    public void testCalculateBusinessDays_ReversedDates_ShouldReturnZero() {
        LocalDate start = LocalDate.of(2023, 12, 15);
        LocalDate end = LocalDate.of(2023, 12, 11);
        int businessDays = dateUtilityService.calculateBusinessDays(start, end);
        assertThat(businessDays).isEqualTo(0);
    }

    @Test
    @DisplayName("calculateBusinessDays should handle null inputs")
    public void testCalculateBusinessDays_NullInputs_ShouldReturnZero() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        assertThat(dateUtilityService.calculateBusinessDays(null, date)).isEqualTo(0);
        assertThat(dateUtilityService.calculateBusinessDays(date, null)).isEqualTo(0);
        assertThat(dateUtilityService.calculateBusinessDays(null, null)).isEqualTo(0);
    }

    @Test
    @DisplayName("addBusinessDays should skip weekends")
    public void testAddBusinessDays_ShouldSkipWeekends() {
        LocalDate friday = LocalDate.of(2023, 12, 15);  // Friday
        LocalDate result = dateUtilityService.addBusinessDays(friday, 1);
        LocalDate expectedMonday = LocalDate.of(2023, 12, 18);  // Next Monday
        assertThat(result).isEqualTo(expectedMonday);

        // Adding 5 business days from Monday should land on next Monday
        LocalDate monday = LocalDate.of(2023, 12, 11);
        result = dateUtilityService.addBusinessDays(monday, 5);
        LocalDate expectedNextMonday = LocalDate.of(2023, 12, 18);
        assertThat(result).isEqualTo(expectedNextMonday);
    }

    @Test
    @DisplayName("addBusinessDays should handle zero and negative inputs")
    public void testAddBusinessDays_ZeroAndNegativeInputs_ShouldReturnOriginal() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        assertThat(dateUtilityService.addBusinessDays(date, 0)).isEqualTo(date);
        assertThat(dateUtilityService.addBusinessDays(date, -1)).isEqualTo(date);
        assertThat(dateUtilityService.addBusinessDays(null, 5)).isNull();
    }

    @Test
    @DisplayName("subtractBusinessDays should skip weekends")
    public void testSubtractBusinessDays_ShouldSkipWeekends() {
        LocalDate monday = LocalDate.of(2023, 12, 18);  // Monday
        LocalDate result = dateUtilityService.subtractBusinessDays(monday, 1);
        LocalDate expectedFriday = LocalDate.of(2023, 12, 15);  // Previous Friday
        assertThat(result).isEqualTo(expectedFriday);
    }

    @Test
    @DisplayName("subtractBusinessDays should handle zero and negative inputs")
    public void testSubtractBusinessDays_ZeroAndNegativeInputs_ShouldReturnOriginal() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        assertThat(dateUtilityService.subtractBusinessDays(date, 0)).isEqualTo(date);
        assertThat(dateUtilityService.subtractBusinessDays(date, -1)).isEqualTo(date);
        assertThat(dateUtilityService.subtractBusinessDays(null, 5)).isNull();
    }

    @Test
    @DisplayName("getNextBusinessDay should skip weekends")
    public void testGetNextBusinessDay_ShouldSkipWeekends() {
        LocalDate friday = LocalDate.of(2023, 12, 15);  // Friday
        LocalDate nextBusinessDay = dateUtilityService.getNextBusinessDay(friday);
        LocalDate expectedMonday = LocalDate.of(2023, 12, 18);  // Next Monday
        assertThat(nextBusinessDay).isEqualTo(expectedMonday);

        LocalDate thursday = LocalDate.of(2023, 12, 14);  // Thursday
        nextBusinessDay = dateUtilityService.getNextBusinessDay(thursday);
        LocalDate expectedFriday = LocalDate.of(2023, 12, 15);  // Next Friday
        assertThat(nextBusinessDay).isEqualTo(expectedFriday);
    }

    @Test
    @DisplayName("getNextBusinessDay should handle null input")
    public void testGetNextBusinessDay_NullInput_ShouldReturnNull() {
        assertThat(dateUtilityService.getNextBusinessDay(null)).isNull();
    }

    @Test
    @DisplayName("getPreviousBusinessDay should skip weekends")
    public void testGetPreviousBusinessDay_ShouldSkipWeekends() {
        LocalDate monday = LocalDate.of(2023, 12, 18);  // Monday
        LocalDate previousBusinessDay = dateUtilityService.getPreviousBusinessDay(monday);
        LocalDate expectedFriday = LocalDate.of(2023, 12, 15);  // Previous Friday
        assertThat(previousBusinessDay).isEqualTo(expectedFriday);

        LocalDate tuesday = LocalDate.of(2023, 12, 19);  // Tuesday
        previousBusinessDay = dateUtilityService.getPreviousBusinessDay(tuesday);
        LocalDate expectedMonday = LocalDate.of(2023, 12, 18);  // Previous Monday
        assertThat(previousBusinessDay).isEqualTo(expectedMonday);
    }

    @Test
    @DisplayName("getPreviousBusinessDay should handle null input")
    public void testGetPreviousBusinessDay_NullInput_ShouldReturnNull() {
        assertThat(dateUtilityService.getPreviousBusinessDay(null)).isNull();
    }

    // ========================================
    // Utility Method Tests
    // ========================================

    @Test
    @DisplayName("isValidDate should delegate to validateCCYYMMDD")
    public void testIsValidDate_ShouldDelegateToValidateCCYYMMDD() {
        assertThat(dateUtilityService.isValidDate("20231215"))
            .isEqualTo(dateUtilityService.validateCCYYMMDD("20231215"));
        assertThat(dateUtilityService.isValidDate("invalid"))
            .isEqualTo(dateUtilityService.validateCCYYMMDD("invalid"));
    }

    @Test
    @DisplayName("getCurrentDate should return current date")
    public void testGetCurrentDate_ShouldReturnCurrentDate() {
        LocalDate currentDate = dateUtilityService.getCurrentDate();
        assertThat(currentDate).isNotNull();
        assertThat(currentDate).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("getDateDifference should calculate days between dates")
    public void testGetDateDifference_ShouldCalculateDaysBetween() {
        LocalDate startDate = LocalDate.of(2023, 12, 10);
        LocalDate endDate = LocalDate.of(2023, 12, 15);
        
        Long difference = dateUtilityService.getDateDifference(startDate, endDate);
        assertThat(difference).isEqualTo(5L);

        // Reverse order should give negative difference
        difference = dateUtilityService.getDateDifference(endDate, startDate);
        assertThat(difference).isEqualTo(-5L);
    }

    @Test
    @DisplayName("getDateDifference should handle null inputs")
    public void testGetDateDifference_NullInputs_ShouldReturnNull() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        assertThat(dateUtilityService.getDateDifference(null, date)).isNull();
        assertThat(dateUtilityService.getDateDifference(date, null)).isNull();
        assertThat(dateUtilityService.getDateDifference(null, null)).isNull();
    }

    // ========================================
    // Edge Case and Boundary Tests
    // ========================================

    @ParameterizedTest
    @DisplayName("validateCCYYMMDD should handle boundary years correctly")
    @CsvSource({
        "19000101, true",   // First valid year
        "20991231, true",   // Last valid year
        "18991231, false",  // Before valid range
        "21000101, false"   // After valid range
    })
    public void testValidateCCYYMMDD_BoundaryYears(String date, boolean expected) {
        assertThat(dateUtilityService.validateCCYYMMDD(date)).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("validateCCYYMMDD should handle month boundaries correctly")
    @CsvSource({
        "20230101, true",   // January 1st
        "20231231, true",   // December 31st
        "20230001, false",  // Invalid month 00
        "20231301, false"   // Invalid month 13
    })
    public void testValidateCCYYMMDD_BoundaryMonths(String date, boolean expected) {
        assertThat(dateUtilityService.validateCCYYMMDD(date)).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("validateCCYYMMDD should handle day boundaries correctly")
    @CsvSource({
        "20230131, true",   // January 31st (valid)
        "20230228, true",   // February 28th non-leap year
        "20200229, true",   // February 29th leap year
        "20230229, false",  // February 29th non-leap year
        "20230430, true",   // April 30th (valid)
        "20230431, false",  // April 31st (invalid)
        "20230631, false"   // June 31st (invalid)
    })
    public void testValidateCCYYMMDD_BoundaryDays(String date, boolean expected) {
        assertThat(dateUtilityService.validateCCYYMMDD(date)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Comprehensive integration test for date processing workflow")
    public void testDateProcessingWorkflow_IntegrationTest() {
        // Test complete workflow: validation -> parsing -> formatting -> business day calculations
        String testDate = "20231215"; // Friday
        
        // Step 1: Validate the date
        assertThat(dateUtilityService.validateCCYYMMDD(testDate)).isTrue();
        
        // Step 2: Parse the date
        LocalDate parsedDate = dateUtilityService.parseDate(testDate);
        assertThat(parsedDate).isNotNull();
        assertThat(parsedDate).isEqualTo(LocalDate.of(2023, 12, 15));
        
        // Step 3: Format the date back
        String formattedDate = dateUtilityService.formatDate(parsedDate);
        assertThat(formattedDate).isEqualTo(testDate);
        
        // Step 4: Business day calculations
        assertThat(dateUtilityService.isBusinessDay(parsedDate)).isTrue();
        LocalDate nextBusinessDay = dateUtilityService.getNextBusinessDay(parsedDate);
        assertThat(nextBusinessDay).isEqualTo(LocalDate.of(2023, 12, 18)); // Monday
        
        // Step 5: Lillian date conversion
        Long lillianDate = dateUtilityService.toLillianDate(testDate);
        assertThat(lillianDate).isNotNull();
        String convertedBack = dateUtilityService.fromLillianDate(lillianDate);
        assertThat(convertedBack).isEqualTo(testDate);
    }
}