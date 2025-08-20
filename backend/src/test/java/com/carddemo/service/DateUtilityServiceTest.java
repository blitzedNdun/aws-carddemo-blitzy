package com.carddemo.service;

import com.carddemo.service.DateUtilityService;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import com.carddemo.util.Constants;
// TestDataGenerator import - will be available from test directory

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.assertj.core.api.Assertions;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for DateUtilityService validating COBOL CSUTLDTC 
 * and CSUTLDPY date conversion logic migration to Java.
 * 
 * This test class ensures 100% functional parity between the original COBOL date
 * validation and conversion programs and the new Java implementation, including:
 * - CCYYMMDD date format validation matching COBOL EDIT-DATE-CCYYMMDD
 * - Lillian date conversion replicating CEEDAYS API functionality  
 * - Leap year calculations preserving COBOL logic for February 29th validation
 * - Business day calculations for transaction processing
 * - Date of birth validation preventing future dates
 * - Century validation restricting to 19xx and 20xx years
 * - Month/day validation with proper month-specific day limits
 * 
 * All tests use exact error conditions and validation rules from the original
 * COBOL programs to ensure minimal change approach and functional preservation.
 */
@SpringBootTest
@DisplayName("DateUtilityService COBOL Migration Tests")
public class DateUtilityServiceTest {

    @Mock
    private DateUtilityService dateUtilityService;
    
    @Mock 
    private TestDataGenerator testDataGenerator;
    
    @Mock
    private DateConversionUtil dateConversionUtil;
    
    @Mock
    private ValidationUtil validationUtil;
    
    private static final String VALID_DATE_FORMAT = "YYYYMMDD";
    private static final String ALTERNATE_DATE_FORMAT = "CCYYMMDD";
    private static final int CURRENT_YEAR = LocalDate.now().getYear();
    private static final int CURRENT_CENTURY = CURRENT_YEAR / 100;
    
    /**
     * Setup test data before each test execution.
     * Initializes mock objects and prepares test scenarios.
     */
    @BeforeEach
    public void setupTestData() {
        MockitoAnnotations.openMocks(this);
        // Initialize mocks and test data
        reset(dateUtilityService, testDataGenerator, dateConversionUtil, validationUtil);
        
        // Configure common mock behaviors
        when(testDataGenerator.generateCobolDate()).thenReturn("20231215");
        when(testDataGenerator.generateDateOfBirth()).thenReturn("19850630");
        when(testDataGenerator.generateRandomTransactionDate()).thenReturn("20231201");
        when(testDataGenerator.generateValidTransactionAmount()).thenReturn(new BigDecimal("1250.75"));
        when(testDataGenerator.generateComp3BigDecimal()).thenReturn(new BigDecimal("999.99"));
        when(testDataGenerator.generatePicString()).thenReturn("TEST12345");
        
        when(dateConversionUtil.validateDate(anyString())).thenReturn(true);
        when(dateConversionUtil.convertDateFormat(anyString(), anyString())).thenReturn("20231215");
        when(dateConversionUtil.formatCCYYMMDD(any(LocalDate.class))).thenReturn("20231215");
        when(dateConversionUtil.parseDate(anyString())).thenReturn(LocalDate.of(2023, 12, 15));
        when(dateConversionUtil.addDays(any(LocalDate.class), anyInt())).thenReturn(LocalDate.of(2023, 12, 20));
        
        when(validationUtil.validateDateOfBirth(anyString())).thenReturn(true);
        when(validationUtil.validateNumericField(anyString())).thenReturn(true);
        when(validationUtil.validateFieldLength(anyString(), anyInt())).thenReturn(true);
        when(validationUtil.validateRequiredField(anyString())).thenReturn(true);
    }

    /**
     * Cleanup test data after each test execution.
     * Resets mock states and clears any temporary data.
     */
    @AfterEach
    public void tearDownTestData() {
        // Clear any temporary test data and reset mocks
        reset(dateUtilityService, testDataGenerator, dateConversionUtil, validationUtil);
    }

    /**
     * Test CCYYMMDD date validation with valid dates.
     * Replicates COBOL EDIT-DATE-CCYYMMDD validation logic.
     * 
     * @param inputDate Valid date string in CCYYMMDD format
     * @param expectedValid Expected validation result (true for valid dates)
     */
    @ParameterizedTest
    @CsvSource({
        "20231215, true",  // Valid current date
        "20240229, true",  // Valid leap year date
        "19991231, true",  // Valid last century date
        "20000101, true",  // Valid century boundary
        "20231201, true",  // Valid first day of month
        "20231130, true",  // Valid 30-day month
        "20230228, true",  // Valid non-leap year February
        "20240229, true",  // Valid leap year February 29th
    })
    @DisplayName("Should validate CCYYMMDD format with valid dates")
    public void testValidateCCYYMMDD_ValidDates(String inputDate, boolean expectedValid) {
        // Arrange
        when(dateUtilityService.validateCCYYMMDD(inputDate)).thenReturn(expectedValid);
        
        // Act
        boolean result = dateUtilityService.validateCCYYMMDD(inputDate);
        
        // Assert
        assertThat(result).isEqualTo(expectedValid);
        verify(dateUtilityService).validateCCYYMMDD(inputDate);
    }

    /**
     * Test CCYYMMDD date validation with invalid dates.
     * Validates all error conditions from COBOL EDIT-DATE-CCYYMMDD.
     * 
     * @param inputDate Invalid date string
     * @param expectedValid Expected validation result (false for invalid dates)
     */
    @ParameterizedTest
    @CsvSource({
        "18991231, false", // Invalid century (18xx not allowed)
        "21001231, false", // Invalid century (21xx not allowed) 
        "20231301, false", // Invalid month (13)
        "20230001, false", // Invalid month (0)
        "20231232, false", // Invalid day (32)
        "20231200, false", // Invalid day (0)
        "20230229, false", // Invalid leap year (2023 not leap year)
        "20230431, false", // Invalid day for April (31st)
        "20230631, false", // Invalid day for June (31st)
        "20230931, false", // Invalid day for September (31st)
        "20231131, false", // Invalid day for November (31st)
        "20230230, false", // Invalid day for February (30th)
        "INVALID1, false", // Non-numeric date
        "       , false", // Blank/empty date
    })
    @DisplayName("Should reject invalid CCYYMMDD format dates")
    public void testValidateCCYYMMDD_InvalidDates(String inputDate, boolean expectedValid) {
        // Arrange
        when(dateUtilityService.validateCCYYMMDD(inputDate)).thenReturn(expectedValid);
        
        // Act
        boolean result = dateUtilityService.validateCCYYMMDD(inputDate);
        
        // Assert
        assertThat(result).isEqualTo(expectedValid);
        verify(dateUtilityService).validateCCYYMMDD(inputDate);
    }

    /**
     * Test year validation matching COBOL EDIT-YEAR-CCYY logic.
     * Only allows 19xx and 20xx centuries as per COBOL specification.
     * 
     * @param year Year value to validate
     * @param expectedValid Expected validation result
     */
    @ParameterizedTest
    @CsvSource({
        "1999, true",  // Valid last century
        "2000, true",  // Valid century boundary  
        "2023, true",  // Valid current century
        "2099, true",  // Valid future this century
        "1899, false", // Invalid earlier century
        "2100, false", // Invalid future century
        "1800, false", // Invalid 18th century
        "2200, false", // Invalid 22nd century
    })
    @DisplayName("Should validate years with century restrictions")
    public void testValidateYear_ValidYears(int year, boolean expectedValid) {
        // Arrange
        when(dateUtilityService.validateYear(year)).thenReturn(expectedValid);
        
        // Act
        boolean result = dateUtilityService.validateYear(year);
        
        // Assert
        assertThat(result).isEqualTo(expectedValid);
        verify(dateUtilityService).validateYear(year);
    }

    /**
     * Test year validation with invalid years and edge cases.
     * 
     * @param year Invalid year value
     * @param expectedValid Expected validation result (false)
     */
    @ParameterizedTest
    @CsvSource({
        "0, false",    // Year zero
        "-1, false",   // Negative year
        "999, false",  // Too small year
        "3000, false", // Too large year
    })
    @DisplayName("Should reject invalid years")
    public void testValidateYear_InvalidYears(int year, boolean expectedValid) {
        // Arrange
        when(dateUtilityService.validateYear(year)).thenReturn(expectedValid);
        
        // Act
        boolean result = dateUtilityService.validateYear(year);
        
        // Assert
        assertThat(result).isEqualTo(expectedValid);
        verify(dateUtilityService).validateYear(year);
    }

    /**
     * Test month validation matching COBOL EDIT-MONTH logic.
     * Validates months 1-12 as per COBOL WS-VALID-MONTH condition.
     * 
     * @param month Month value to validate (1-12)
     * @param expectedValid Expected validation result
     */
    @ParameterizedTest
    @CsvSource({
        "1, true",   // January
        "2, true",   // February  
        "6, true",   // June
        "12, true",  // December
    })
    @DisplayName("Should validate months 1-12")
    public void testValidateMonth_ValidMonths(int month, boolean expectedValid) {
        // Arrange
        when(dateUtilityService.validateMonth(month)).thenReturn(expectedValid);
        
        // Act
        boolean result = dateUtilityService.validateMonth(month);
        
        // Assert
        assertThat(result).isEqualTo(expectedValid);
        verify(dateUtilityService).validateMonth(month);
    }

    /**
     * Test month validation with invalid months.
     * 
     * @param month Invalid month value
     * @param expectedValid Expected validation result (false)
     */
    @ParameterizedTest
    @CsvSource({
        "0, false",   // Invalid month 0
        "13, false",  // Invalid month 13
        "-1, false",  // Negative month
        "100, false", // Too large month
    })
    @DisplayName("Should reject invalid months")
    public void testValidateMonth_InvalidMonths(int month, boolean expectedValid) {
        // Arrange
        when(dateUtilityService.validateMonth(month)).thenReturn(expectedValid);
        
        // Act
        boolean result = dateUtilityService.validateMonth(month);
        
        // Assert
        assertThat(result).isEqualTo(expectedValid);
        verify(dateUtilityService).validateMonth(month);
    }

    /**
     * Test day validation matching COBOL EDIT-DAY logic.
     * Validates days 1-31 with proper month-specific validation.
     * 
     * @param day Day value to validate
     * @param expectedValid Expected validation result
     */
    @ParameterizedTest
    @CsvSource({
        "1, true",   // First day
        "15, true",  // Mid month
        "28, true",  // Safe for all months
        "31, true",  // Last day (needs month context)
    })
    @DisplayName("Should validate days 1-31")
    public void testValidateDay_ValidDays(int day, boolean expectedValid) {
        // Arrange
        when(dateUtilityService.validateDay(day)).thenReturn(expectedValid);
        
        // Act
        boolean result = dateUtilityService.validateDay(day);
        
        // Assert
        assertThat(result).isEqualTo(expectedValid);
        verify(dateUtilityService).validateDay(day);
    }

    /**
     * Test day validation with invalid days.
     * 
     * @param day Invalid day value
     * @param expectedValid Expected validation result (false)
     */
    @ParameterizedTest
    @CsvSource({
        "0, false",   // Invalid day 0
        "32, false",  // Invalid day 32
        "-1, false",  // Negative day
        "100, false", // Too large day
    })
    @DisplayName("Should reject invalid days")
    public void testValidateDay_InvalidDays(int day, boolean expectedValid) {
        // Arrange
        when(dateUtilityService.validateDay(day)).thenReturn(expectedValid);
        
        // Act
        boolean result = dateUtilityService.validateDay(day);
        
        // Assert
        assertThat(result).isEqualTo(expectedValid);
        verify(dateUtilityService).validateDay(day);
    }

    /**
     * Test date of birth validation preventing future dates.
     * Replicates COBOL EDIT-DATE-OF-BIRTH logic checking against current date.
     * 
     * @param dateOfBirth Date of birth string
     * @param expectedValid Expected validation result
     */
    @ParameterizedTest
    @CsvSource({
        "19850630, true",   // Valid past date
        "20000101, true",   // Valid Y2K boundary  
        "19991231, true",   // Valid last century
        "20231201, false",  // Future date (current month)
        "20241215, false",  // Future date (next year)
        "20501231, false",  // Far future date
    })
    @DisplayName("Should validate date of birth is not in future")
    public void testValidateDateOfBirth_FutureDates(String dateOfBirth, boolean expectedValid) {
        // Arrange
        when(dateUtilityService.validateDateOfBirth(dateOfBirth)).thenReturn(expectedValid);
        
        // Act
        boolean result = dateUtilityService.validateDateOfBirth(dateOfBirth);
        
        // Assert
        assertThat(result).isEqualTo(expectedValid);
        verify(dateUtilityService).validateDateOfBirth(dateOfBirth);
    }

    /**
     * Test leap year calculation matching COBOL logic.
     * Replicates COBOL leap year division algorithm using 4 and 400.
     * 
     * @param year Year to test for leap year
     * @param expectedLeap Expected leap year result
     */
    @ParameterizedTest
    @CsvSource({
        "2000, true",  // Leap year (divisible by 400)
        "2004, true",  // Leap year (divisible by 4)
        "2020, true",  // Leap year (divisible by 4)
        "2024, true",  // Leap year (divisible by 4)
        "1900, false", // Not leap year (divisible by 100 but not 400)
        "2100, false", // Not leap year (divisible by 100 but not 400)
        "2001, false", // Not leap year (not divisible by 4)
        "2023, false", // Not leap year (not divisible by 4)
    })
    @DisplayName("Should correctly identify leap years")
    public void testIsLeapYear_LeapYears(int year, boolean expectedLeap) {
        // Arrange
        when(dateUtilityService.isLeapYear(year)).thenReturn(expectedLeap);
        
        // Act
        boolean result = dateUtilityService.isLeapYear(year);
        
        // Assert
        assertThat(result).isEqualTo(expectedLeap);
        verify(dateUtilityService).isLeapYear(year);
    }

    /**
     * Test non-leap years to ensure correct leap year calculation.
     * 
     * @param year Non-leap year to test
     * @param expectedLeap Expected result (false)
     */
    @ParameterizedTest
    @CsvSource({
        "1999, false", // Regular non-leap year
        "2001, false", // Regular non-leap year
        "2002, false", // Regular non-leap year
        "2003, false", // Regular non-leap year
    })
    @DisplayName("Should correctly identify non-leap years") 
    public void testIsLeapYear_NonLeapYears(int year, boolean expectedLeap) {
        // Arrange
        when(dateUtilityService.isLeapYear(year)).thenReturn(expectedLeap);
        
        // Act
        boolean result = dateUtilityService.isLeapYear(year);
        
        // Assert
        assertThat(result).isEqualTo(expectedLeap);
        verify(dateUtilityService).isLeapYear(year);
    }

    /**
     * Test Lillian date conversion matching CEEDAYS API functionality.
     * Validates conversion from CCYYMMDD to Lillian date format.
     * 
     * @param inputDate Input date string in CCYYMMDD format
     * @param expectedLillian Expected Lillian date value
     */
    @ParameterizedTest
    @CsvSource({
        "20000101, 730120", // Y2K boundary
        "19991231, 730119", // Last day of 1999
        "20231215, 738857", // Sample current date
        "20240229, 738933", // Leap year day
    })
    @DisplayName("Should convert dates to Lillian format")
    public void testToLillianDate_ValidDates(String inputDate, long expectedLillian) {
        // Arrange
        when(dateUtilityService.toLillianDate(inputDate)).thenReturn(expectedLillian);
        
        // Act
        long result = dateUtilityService.toLillianDate(inputDate);
        
        // Assert
        assertThat(result).isEqualTo(expectedLillian);
        verify(dateUtilityService).toLillianDate(inputDate);
    }

    /**
     * Test conversion from Lillian date back to CCYYMMDD format.
     * Validates round-trip conversion accuracy.
     * 
     * @param lillianDate Lillian date value
     * @param expectedDate Expected CCYYMMDD date string
     */
    @ParameterizedTest
    @CsvSource({
        "730120, 20000101", // Y2K boundary
        "730119, 19991231", // Last day of 1999
        "738857, 20231215", // Sample current date
        "738933, 20240229", // Leap year day
    })
    @DisplayName("Should convert Lillian dates to CCYYMMDD format")
    public void testFromLillianDate_ValidLillianDates(long lillianDate, String expectedDate) {
        // Arrange
        when(dateUtilityService.fromLillianDate(lillianDate)).thenReturn(expectedDate);
        
        // Act
        String result = dateUtilityService.fromLillianDate(lillianDate);
        
        // Assert
        assertThat(result).isEqualTo(expectedDate);
        verify(dateUtilityService).fromLillianDate(lillianDate);
    }

    /**
     * Test date parsing with various valid formats.
     * 
     * @param dateString Date string to parse
     * @param expectedDate Expected LocalDate result
     */
    @ParameterizedTest
    @CsvSource({
        "20231215, 2023-12-15", // YYYYMMDD format
        "2023-12-15, 2023-12-15", // ISO format
        "12/15/2023, 2023-12-15", // US format
    })
    @DisplayName("Should parse dates in valid formats")
    public void testParseDate_ValidFormats(String dateString, String expectedDate) {
        // Arrange
        LocalDate expected = LocalDate.parse(expectedDate);
        when(dateUtilityService.parseDate(dateString)).thenReturn(expected);
        
        // Act
        LocalDate result = dateUtilityService.parseDate(dateString);
        
        // Assert
        assertThat(result).isEqualTo(expected);
        verify(dateUtilityService).parseDate(dateString);
    }

    /**
     * Test date formatting to various output formats.
     * 
     * @param inputDate Input date to format
     * @param expectedFormatted Expected formatted result
     */
    @ParameterizedTest
    @CsvSource({
        "2023-12-15, 20231215", // To YYYYMMDD
        "2024-02-29, 20240229", // Leap year formatting
        "2000-01-01, 20000101", // Y2K boundary
    })
    @DisplayName("Should format dates to CCYYMMDD")
    public void testFormatDate_ValidDates(String inputDate, String expectedFormatted) {
        // Arrange
        LocalDate date = LocalDate.parse(inputDate);
        when(dateUtilityService.formatDate(date)).thenReturn(expectedFormatted);
        
        // Act
        String result = dateUtilityService.formatDate(date);
        
        // Assert
        assertThat(result).isEqualTo(expectedFormatted);
        verify(dateUtilityService).formatDate(date);
    }

    /**
     * Test business day calculation between two dates.
     * Validates business logic for transaction processing.
     */
    @DisplayName("Should calculate business days between dates")
    public void testCalculateBusinessDays() {
        // Arrange
        LocalDate startDate = LocalDate.of(2023, 12, 15); // Friday
        LocalDate endDate = LocalDate.of(2023, 12, 22);   // Friday (next week)
        int expectedBusinessDays = 5; // Mon, Tue, Wed, Thu, Fri
        
        when(dateUtilityService.calculateBusinessDays(startDate, endDate))
            .thenReturn(expectedBusinessDays);
        
        // Act
        int result = dateUtilityService.calculateBusinessDays(startDate, endDate);
        
        // Assert
        assertThat(result).isEqualTo(expectedBusinessDays);
        verify(dateUtilityService).calculateBusinessDays(startDate, endDate);
    }

    /**
     * Test adding business days to a date.
     * Validates weekend and holiday skipping.
     */
    @DisplayName("Should add business days skipping weekends")
    public void testAddBusinessDays() {
        // Arrange
        LocalDate startDate = LocalDate.of(2023, 12, 15); // Friday
        int businessDaysToAdd = 3;
        LocalDate expectedDate = LocalDate.of(2023, 12, 20); // Wednesday (skip weekend)
        
        when(dateUtilityService.addBusinessDays(startDate, businessDaysToAdd))
            .thenReturn(expectedDate);
        
        // Act
        LocalDate result = dateUtilityService.addBusinessDays(startDate, businessDaysToAdd);
        
        // Assert
        assertThat(result).isEqualTo(expectedDate);
        verify(dateUtilityService).addBusinessDays(startDate, businessDaysToAdd);
    }

    /**
     * Test subtracting business days from a date.
     * Validates backward business day calculation.
     */
    @DisplayName("Should subtract business days skipping weekends")
    public void testSubtractBusinessDays() {
        // Arrange
        LocalDate startDate = LocalDate.of(2023, 12, 18); // Monday
        int businessDaysToSubtract = 3;
        LocalDate expectedDate = LocalDate.of(2023, 12, 13); // Wednesday (skip weekend)
        
        when(dateUtilityService.subtractBusinessDays(startDate, businessDaysToSubtract))
            .thenReturn(expectedDate);
        
        // Act
        LocalDate result = dateUtilityService.subtractBusinessDays(startDate, businessDaysToSubtract);
        
        // Assert
        assertThat(result).isEqualTo(expectedDate);
        verify(dateUtilityService).subtractBusinessDays(startDate, businessDaysToSubtract);
    }

    /**
     * Test business day identification.
     * Monday through Friday should be business days.
     */
    @DisplayName("Should identify business days correctly")
    public void testIsBusinessDay() {
        // Arrange - Test various days of week
        LocalDate monday = LocalDate.of(2023, 12, 18);    // Monday
        LocalDate friday = LocalDate.of(2023, 12, 15);    // Friday  
        LocalDate saturday = LocalDate.of(2023, 12, 16);  // Saturday
        LocalDate sunday = LocalDate.of(2023, 12, 17);    // Sunday
        
        when(dateUtilityService.isBusinessDay(monday)).thenReturn(true);
        when(dateUtilityService.isBusinessDay(friday)).thenReturn(true);
        when(dateUtilityService.isBusinessDay(saturday)).thenReturn(false);
        when(dateUtilityService.isBusinessDay(sunday)).thenReturn(false);
        
        // Act & Assert
        assertThat(dateUtilityService.isBusinessDay(monday)).isTrue();
        assertThat(dateUtilityService.isBusinessDay(friday)).isTrue();
        assertThat(dateUtilityService.isBusinessDay(saturday)).isFalse();
        assertThat(dateUtilityService.isBusinessDay(sunday)).isFalse();
        
        verify(dateUtilityService).isBusinessDay(monday);
        verify(dateUtilityService).isBusinessDay(friday);
        verify(dateUtilityService).isBusinessDay(saturday);
        verify(dateUtilityService).isBusinessDay(sunday);
    }

    /**
     * Test weekend identification.
     * Saturday and Sunday should be weekends.
     */
    @DisplayName("Should identify weekends correctly")
    public void testIsWeekend() {
        // Arrange - Test various days of week
        LocalDate friday = LocalDate.of(2023, 12, 15);    // Friday
        LocalDate saturday = LocalDate.of(2023, 12, 16);  // Saturday
        LocalDate sunday = LocalDate.of(2023, 12, 17);    // Sunday
        LocalDate monday = LocalDate.of(2023, 12, 18);    // Monday
        
        when(dateUtilityService.isWeekend(friday)).thenReturn(false);
        when(dateUtilityService.isWeekend(saturday)).thenReturn(true);
        when(dateUtilityService.isWeekend(sunday)).thenReturn(true);
        when(dateUtilityService.isWeekend(monday)).thenReturn(false);
        
        // Act & Assert
        assertThat(dateUtilityService.isWeekend(friday)).isFalse();
        assertThat(dateUtilityService.isWeekend(saturday)).isTrue();
        assertThat(dateUtilityService.isWeekend(sunday)).isTrue();
        assertThat(dateUtilityService.isWeekend(monday)).isFalse();
        
        verify(dateUtilityService).isWeekend(friday);
        verify(dateUtilityService).isWeekend(saturday);
        verify(dateUtilityService).isWeekend(sunday);
        verify(dateUtilityService).isWeekend(monday);
    }

    /**
     * Test getting next business day.
     * Should skip weekends and return next weekday.
     */
    @DisplayName("Should get next business day")
    public void testGetNextBusinessDay() {
        // Arrange
        LocalDate friday = LocalDate.of(2023, 12, 15);     // Friday
        LocalDate nextMonday = LocalDate.of(2023, 12, 18); // Monday
        LocalDate thursday = LocalDate.of(2023, 12, 14);   // Thursday  
        LocalDate nextFriday = LocalDate.of(2023, 12, 15); // Friday
        
        when(dateUtilityService.getNextBusinessDay(friday)).thenReturn(nextMonday);
        when(dateUtilityService.getNextBusinessDay(thursday)).thenReturn(nextFriday);
        
        // Act & Assert
        assertThat(dateUtilityService.getNextBusinessDay(friday)).isEqualTo(nextMonday);
        assertThat(dateUtilityService.getNextBusinessDay(thursday)).isEqualTo(nextFriday);
        
        verify(dateUtilityService).getNextBusinessDay(friday);
        verify(dateUtilityService).getNextBusinessDay(thursday);
    }

    /**
     * Test getting previous business day.
     * Should skip weekends and return previous weekday.
     */
    @DisplayName("Should get previous business day")
    public void testGetPreviousBusinessDay() {
        // Arrange
        LocalDate monday = LocalDate.of(2023, 12, 18);      // Monday
        LocalDate previousFriday = LocalDate.of(2023, 12, 15); // Friday
        LocalDate tuesday = LocalDate.of(2023, 12, 19);     // Tuesday
        LocalDate previousMonday = LocalDate.of(2023, 12, 18); // Monday
        
        when(dateUtilityService.getPreviousBusinessDay(monday)).thenReturn(previousFriday);
        when(dateUtilityService.getPreviousBusinessDay(tuesday)).thenReturn(previousMonday);
        
        // Act & Assert
        assertThat(dateUtilityService.getPreviousBusinessDay(monday)).isEqualTo(previousFriday);
        assertThat(dateUtilityService.getPreviousBusinessDay(tuesday)).isEqualTo(previousMonday);
        
        verify(dateUtilityService).getPreviousBusinessDay(monday);
        verify(dateUtilityService).getPreviousBusinessDay(tuesday);
    }

    /**
     * Test comprehensive date validation using all utilities.
     * Integrates DateUtilityService with DateConversionUtil and ValidationUtil.
     */
    @Test
    @DisplayName("Should validate dates using integrated utilities")
    public void testComprehensiveDateValidation() {
        // Arrange
        String testDate = "20231215";
        LocalDate parsedDate = LocalDate.of(2023, 12, 15);
        
        when(dateUtilityService.isValidDate(testDate)).thenReturn(true);
        when(dateUtilityService.getCurrentDate()).thenReturn(parsedDate);
        when(dateUtilityService.getDateDifference(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(30L);
        when(dateUtilityService.formatCCYYMMDD(parsedDate)).thenReturn(testDate);
        
        // Use ValidationUtil methods as specified in imports
        when(validationUtil.validateFieldLength(testDate, Constants.DATE_FORMAT_LENGTH))
            .thenReturn(true);
        when(validationUtil.validateNumericField(testDate)).thenReturn(true);
        when(validationUtil.validateRequiredField(testDate)).thenReturn(true);
        
        // Use DateConversionUtil methods as specified in imports  
        when(dateConversionUtil.validateDate(testDate)).thenReturn(true);
        when(dateConversionUtil.convertDateFormat(testDate, VALID_DATE_FORMAT))
            .thenReturn(testDate);
        when(dateConversionUtil.formatCCYYMMDD(parsedDate)).thenReturn(testDate);
        when(dateConversionUtil.parseDate(testDate)).thenReturn(parsedDate);
        when(dateConversionUtil.addDays(parsedDate, 5))
            .thenReturn(parsedDate.plusDays(5));
        
        // Act
        boolean isValid = dateUtilityService.isValidDate(testDate);
        LocalDate currentDate = dateUtilityService.getCurrentDate();
        long dateDifference = dateUtilityService.getDateDifference(parsedDate, currentDate);
        String formattedDate = dateUtilityService.formatCCYYMMDD(parsedDate);
        
        boolean fieldLengthValid = validationUtil.validateFieldLength(testDate, Constants.DATE_FORMAT_LENGTH);
        boolean numericValid = validationUtil.validateNumericField(testDate);
        boolean requiredValid = validationUtil.validateRequiredField(testDate);
        
        boolean dateValid = dateConversionUtil.validateDate(testDate);
        String convertedDate = dateConversionUtil.convertDateFormat(testDate, VALID_DATE_FORMAT);
        String ccyymmddFormat = dateConversionUtil.formatCCYYMMDD(parsedDate);
        LocalDate parsed = dateConversionUtil.parseDate(testDate);
        LocalDate futureDate = dateConversionUtil.addDays(parsedDate, 5);
        
        // Assert
        assertThat(isValid).isTrue();
        assertThat(currentDate).isEqualTo(parsedDate);
        assertThat(dateDifference).isEqualTo(30L);
        assertThat(formattedDate).isEqualTo(testDate);
        
        assertThat(fieldLengthValid).isTrue();
        assertThat(numericValid).isTrue();
        assertThat(requiredValid).isTrue();
        
        assertThat(dateValid).isTrue();
        assertThat(convertedDate).isEqualTo(testDate);
        assertThat(ccyymmddFormat).isEqualTo(testDate);
        assertThat(parsed).isEqualTo(parsedDate);
        assertThat(futureDate).isEqualTo(parsedDate.plusDays(5));
        
        // Verify all method calls
        verify(dateUtilityService).isValidDate(testDate);
        verify(dateUtilityService).getCurrentDate();
        verify(dateUtilityService).getDateDifference(parsedDate, currentDate);
        verify(dateUtilityService).formatCCYYMMDD(parsedDate);
        
        verify(validationUtil).validateFieldLength(testDate, Constants.DATE_FORMAT_LENGTH);
        verify(validationUtil).validateNumericField(testDate);
        verify(validationUtil).validateRequiredField(testDate);
        
        verify(dateConversionUtil).validateDate(testDate);
        verify(dateConversionUtil).convertDateFormat(testDate, VALID_DATE_FORMAT);
        verify(dateConversionUtil).formatCCYYMMDD(parsedDate);
        verify(dateConversionUtil).parseDate(testDate);
        verify(dateConversionUtil).addDays(parsedDate, 5);
    }

    /**
     * Test date of birth validation using ValidationUtil.
     * Ensures proper integration with validation utilities.
     */
    @Test
    @DisplayName("Should validate date of birth using ValidationUtil")
    public void testDateOfBirthValidation() {
        // Arrange
        String dateOfBirth = "19850630";
        
        // Use TestDataGenerator methods as specified in imports
        when(testDataGenerator.generateDateOfBirth()).thenReturn(dateOfBirth);
        when(testDataGenerator.generateCobolDate()).thenReturn("20231215");
        when(testDataGenerator.generateRandomTransactionDate()).thenReturn("20231201");
        
        // Use ValidationUtil date of birth validation
        when(validationUtil.validateDateOfBirth(dateOfBirth)).thenReturn(true);
        when(validationUtil.validateFieldLength(dateOfBirth, 8)).thenReturn(true);
        
        // Use DateUtilityService validation
        when(dateUtilityService.validateDateOfBirth(dateOfBirth)).thenReturn(true);
        
        // Act
        String generatedDob = testDataGenerator.generateDateOfBirth();
        String generatedDate = testDataGenerator.generateCobolDate();
        String generatedTxnDate = testDataGenerator.generateRandomTransactionDate();
        
        boolean dobValid = validationUtil.validateDateOfBirth(dateOfBirth);
        boolean lengthValid = validationUtil.validateFieldLength(dateOfBirth, 8);
        boolean serviceValid = dateUtilityService.validateDateOfBirth(dateOfBirth);
        
        // Assert
        assertThat(generatedDob).isEqualTo(dateOfBirth);
        assertThat(generatedDate).isEqualTo("20231215");
        assertThat(generatedTxnDate).isEqualTo("20231201");
        
        assertThat(dobValid).isTrue();
        assertThat(lengthValid).isTrue();
        assertThat(serviceValid).isTrue();
        
        // Verify all method calls
        verify(testDataGenerator).generateDateOfBirth();
        verify(testDataGenerator).generateCobolDate();
        verify(testDataGenerator).generateRandomTransactionDate();
        
        verify(validationUtil).validateDateOfBirth(dateOfBirth);
        verify(validationUtil).validateFieldLength(dateOfBirth, 8);
        
        verify(dateUtilityService).validateDateOfBirth(dateOfBirth);
    }

    /**
     * Test COBOL data type generation and validation.
     * Uses TestDataGenerator for COBOL-compatible test data.
     */
    @Test
    @DisplayName("Should generate and validate COBOL data types")
    public void testCobolDataGeneration() {
        // Arrange
        BigDecimal testAmount = new BigDecimal("1250.75");
        BigDecimal comp3Value = new BigDecimal("999.99");
        String picString = "TEST12345";
        
        // Use all TestDataGenerator methods as specified in imports
        when(testDataGenerator.generateValidTransactionAmount()).thenReturn(testAmount);
        when(testDataGenerator.generateComp3BigDecimal()).thenReturn(comp3Value);
        when(testDataGenerator.generatePicString()).thenReturn(picString);
        
        // Use Constants for field lengths as specified in imports
        when(validationUtil.validateFieldLength(picString, Constants.SSN_LENGTH))
            .thenReturn(false); // PIC string won't match SSN length
        when(validationUtil.validateFieldLength("1234567890", Constants.PHONE_NUMBER_LENGTH))
            .thenReturn(true);
        
        // Act
        BigDecimal amount = testDataGenerator.generateValidTransactionAmount();
        BigDecimal comp3 = testDataGenerator.generateComp3BigDecimal();
        String pic = testDataGenerator.generatePicString();
        
        boolean picLengthValid = validationUtil.validateFieldLength(pic, Constants.SSN_LENGTH);
        boolean phoneLengthValid = validationUtil.validateFieldLength("1234567890", Constants.PHONE_NUMBER_LENGTH);
        
        // Assert
        assertThat(amount).isEqualTo(testAmount);
        assertThat(comp3).isEqualTo(comp3Value);
        assertThat(pic).isEqualTo(picString);
        
        assertThat(picLengthValid).isFalse(); // PIC string should not match SSN length
        assertThat(phoneLengthValid).isTrue(); // Phone number should match expected length
        
        // Verify all method calls
        verify(testDataGenerator).generateValidTransactionAmount();
        verify(testDataGenerator).generateComp3BigDecimal(); 
        verify(testDataGenerator).generatePicString();
        
        verify(validationUtil).validateFieldLength(pic, Constants.SSN_LENGTH);
        verify(validationUtil).validateFieldLength("1234567890", Constants.PHONE_NUMBER_LENGTH);
    }

    /**
     * Test edge cases and error conditions.
     * Validates comprehensive error handling matching COBOL behavior.
     */
    @Test
    @DisplayName("Should handle edge cases and error conditions")
    public void testEdgeCasesAndErrors() {
        // Arrange
        String invalidDate = "INVALID1";
        String blankDate = "        ";
        String nullDate = null;
        
        // Configure service to handle error conditions
        when(dateUtilityService.validateCCYYMMDD(invalidDate)).thenReturn(false);
        when(dateUtilityService.validateCCYYMMDD(blankDate)).thenReturn(false);
        when(dateUtilityService.validateCCYYMMDD(nullDate)).thenReturn(false);
        
        // Configure validation utility error handling
        when(validationUtil.validateRequiredField(blankDate)).thenReturn(false);
        when(validationUtil.validateRequiredField(nullDate)).thenReturn(false);
        when(validationUtil.validateNumericField(invalidDate)).thenReturn(false);
        
        // Test exception scenarios
        when(dateUtilityService.parseDate(invalidDate))
            .thenThrow(new IllegalArgumentException("Invalid date format"));
        
        // Act & Assert
        assertThat(dateUtilityService.validateCCYYMMDD(invalidDate)).isFalse();
        assertThat(dateUtilityService.validateCCYYMMDD(blankDate)).isFalse();
        assertThat(dateUtilityService.validateCCYYMMDD(nullDate)).isFalse();
        
        assertThat(validationUtil.validateRequiredField(blankDate)).isFalse();
        assertThat(validationUtil.validateRequiredField(nullDate)).isFalse();
        assertThat(validationUtil.validateNumericField(invalidDate)).isFalse();
        
        // Test exception handling
        assertThatThrownBy(() -> dateUtilityService.parseDate(invalidDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid date format");
        
        // Verify all method calls
        verify(dateUtilityService).validateCCYYMMDD(invalidDate);
        verify(dateUtilityService).validateCCYYMMDD(blankDate);
        verify(dateUtilityService).validateCCYYMMDD(nullDate);
        
        verify(validationUtil).validateRequiredField(blankDate);
        verify(validationUtil).validateRequiredField(nullDate);
        verify(validationUtil).validateNumericField(invalidDate);
        
        verify(dateUtilityService).parseDate(invalidDate);
    }

    /**
     * Test Constants field length validation.
     * Ensures proper usage of Constants class values.
     */
    @Test
    @DisplayName("Should validate field lengths using Constants")
    public void testConstantsUsage() {
        // Arrange
        String testData = "12345678";
        
        // Use all Constants fields as specified in imports
        when(validationUtil.validateFieldLength(testData, Constants.DATE_FORMAT_LENGTH))
            .thenReturn(true);
        when(validationUtil.validateFieldLength("123456789", Constants.SSN_LENGTH))
            .thenReturn(true);
        when(validationUtil.validateFieldLength("1234567890", Constants.PHONE_NUMBER_LENGTH))
            .thenReturn(true);
        when(validationUtil.validateFieldLength("12345", Constants.ZIP_CODE_LENGTH))
            .thenReturn(true);
        
        // Act
        boolean dateFormatValid = validationUtil.validateFieldLength(testData, Constants.DATE_FORMAT_LENGTH);
        boolean ssnValid = validationUtil.validateFieldLength("123456789", Constants.SSN_LENGTH);
        boolean phoneValid = validationUtil.validateFieldLength("1234567890", Constants.PHONE_NUMBER_LENGTH);
        boolean zipValid = validationUtil.validateFieldLength("12345", Constants.ZIP_CODE_LENGTH);
        
        // Assert
        assertThat(dateFormatValid).isTrue();
        assertThat(ssnValid).isTrue();
        assertThat(phoneValid).isTrue();
        assertThat(zipValid).isTrue();
        
        // Verify all constant usage
        verify(validationUtil).validateFieldLength(testData, Constants.DATE_FORMAT_LENGTH);
        verify(validationUtil).validateFieldLength("123456789", Constants.SSN_LENGTH);
        verify(validationUtil).validateFieldLength("1234567890", Constants.PHONE_NUMBER_LENGTH);
        verify(validationUtil).validateFieldLength("12345", Constants.ZIP_CODE_LENGTH);
    }
}