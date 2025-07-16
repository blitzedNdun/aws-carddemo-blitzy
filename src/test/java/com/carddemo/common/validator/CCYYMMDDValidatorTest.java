package com.carddemo.common.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CCYYMMDDValidator.
 * 
 * Tests validate the exact behavior of COBOL CSUTLDPY.cpy date validation routines
 * to ensure functional equivalence in the Java implementation.
 * 
 * Test Coverage:
 * - Format validation (8-digit numeric requirement)
 * - Century validation (19xx and 20xx only)
 * - Month validation (01-12)
 * - Day validation (01-31)
 * - Leap year validation (February 29th)
 * - Month/day combination validation
 * - Null and blank value handling
 * - Error message validation
 * 
 * @since 1.0
 */
class CCYYMMDDValidatorTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    /**
     * Test DTO class for validation testing.
     */
    static class TestDTO {
        @ValidCCYYMMDD
        private String dateField;
        
        @ValidCCYYMMDD(fieldName = "Birth Date")
        private String birthDate;
        
        @ValidCCYYMMDD(allowNull = false)
        private String requiredDate;
        
        @ValidCCYYMMDD(allowBlank = false)
        private String nonBlankDate;
        
        public TestDTO(String dateField) {
            this.dateField = dateField;
        }
        
        public TestDTO(String dateField, String birthDate, String requiredDate, String nonBlankDate) {
            this.dateField = dateField;
            this.birthDate = birthDate;
            this.requiredDate = requiredDate;
            this.nonBlankDate = nonBlankDate;
        }
    }
    
    @Test
    @DisplayName("Valid dates should pass validation")
    void testValidDates() {
        String[] validDates = {
            "19800101", // January 1, 1980
            "20231231", // December 31, 2023
            "19900228", // February 28, 1990 (non-leap year)
            "20000229", // February 29, 2000 (leap year)
            "20240229", // February 29, 2024 (leap year)
            "19700430", // April 30, 1970
            "20200531"  // May 31, 2020
        };
        
        for (String validDate : validDates) {
            TestDTO dto = new TestDTO(validDate);
            Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(), "Date " + validDate + " should be valid");
        }
    }
    
    @Test
    @DisplayName("Invalid format should fail validation")
    void testInvalidFormat() {
        String[] invalidFormats = {
            "1980010",   // 7 digits
            "198001011", // 9 digits
            "19800a01",  // Contains letter
            "19-80-01",  // Contains dashes
            "abcd1234",  // Letters and numbers
            ""           // Empty string (when allowBlank=false)
        };
        
        for (String invalidFormat : invalidFormats) {
            TestDTO dto = new TestDTO(null, null, null, invalidFormat);
            Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty(), "Format " + invalidFormat + " should be invalid");
        }
    }
    
    @Test
    @DisplayName("Invalid century should fail validation")
    void testInvalidCentury() {
        String[] invalidCenturies = {
            "18801231", // 18xx century
            "21001231", // 21xx century
            "00801231", // 00xx century
            "99801231"  // 99xx century
        };
        
        for (String invalidCentury : invalidCenturies) {
            TestDTO dto = new TestDTO(invalidCentury);
            Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty(), "Century in " + invalidCentury + " should be invalid");
            
            // Check error message contains century validation
            String errorMessage = violations.iterator().next().getMessage();
            assertTrue(errorMessage.contains("Century is not valid"), 
                      "Error message should mention century validation");
        }
    }
    
    @Test
    @DisplayName("Invalid month should fail validation")
    void testInvalidMonth() {
        String[] invalidMonths = {
            "19800001", // Month 00
            "19801301", // Month 13
            "19809901"  // Month 99
        };
        
        for (String invalidMonth : invalidMonths) {
            TestDTO dto = new TestDTO(invalidMonth);
            Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty(), "Month in " + invalidMonth + " should be invalid");
            
            // Check error message contains month validation
            String errorMessage = violations.iterator().next().getMessage();
            assertTrue(errorMessage.contains("Month must be a number between 1 and 12"), 
                      "Error message should mention month validation");
        }
    }
    
    @Test
    @DisplayName("Invalid day should fail validation")
    void testInvalidDay() {
        String[] invalidDays = {
            "19800100", // Day 00
            "19800132", // Day 32
            "19800199"  // Day 99
        };
        
        for (String invalidDay : invalidDays) {
            TestDTO dto = new TestDTO(invalidDay);
            Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty(), "Day in " + invalidDay + " should be invalid");
            
            // Check error message contains day validation
            String errorMessage = violations.iterator().next().getMessage();
            assertTrue(errorMessage.contains("Day must be a number between 1 and 31"), 
                      "Error message should mention day validation");
        }
    }
    
    @Test
    @DisplayName("Invalid day-month combinations should fail validation")
    void testInvalidDayMonthCombinations() {
        String[] invalidCombinations = {
            "19800431", // April 31 (April has 30 days)
            "19800631", // June 31 (June has 30 days)
            "19800931", // September 31 (September has 30 days)
            "19801131", // November 31 (November has 30 days)
            "19800230", // February 30 (February never has 30 days)
            "19900229"  // February 29 in non-leap year
        };
        
        for (String invalidCombination : invalidCombinations) {
            TestDTO dto = new TestDTO(invalidCombination);
            Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty(), "Day-month combination " + invalidCombination + " should be invalid");
        }
    }
    
    @Test
    @DisplayName("Leap year validation should work correctly")
    void testLeapYearValidation() {
        // Valid leap years
        String[] validLeapYears = {
            "20000229", // 2000 is leap year (divisible by 400)
            "20040229", // 2004 is leap year (divisible by 4)
            "20240229"  // 2024 is leap year (divisible by 4)
        };
        
        for (String validLeapYear : validLeapYears) {
            TestDTO dto = new TestDTO(validLeapYear);
            Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty(), "Leap year date " + validLeapYear + " should be valid");
        }
        
        // Invalid leap years
        String[] invalidLeapYears = {
            "19000229", // 1900 is not leap year (divisible by 100 but not 400)
            "19010229", // 1901 is not leap year
            "20230229"  // 2023 is not leap year
        };
        
        for (String invalidLeapYear : invalidLeapYears) {
            TestDTO dto = new TestDTO(invalidLeapYear);
            Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty(), "Non-leap year date " + invalidLeapYear + " should be invalid");
            
            // Check error message mentions leap year
            String errorMessage = violations.iterator().next().getMessage();
            assertTrue(errorMessage.contains("Not a leap year"), 
                      "Error message should mention leap year validation");
        }
    }
    
    @Test
    @DisplayName("Null handling should respect allowNull setting")
    void testNullHandling() {
        // Test with allowNull=true (default)
        TestDTO dto1 = new TestDTO(null);
        Set<ConstraintViolation<TestDTO>> violations1 = validator.validate(dto1);
        assertTrue(violations1.isEmpty(), "Null should be valid when allowNull=true");
        
        // Test with allowNull=false
        TestDTO dto2 = new TestDTO(null, null, null, null);
        Set<ConstraintViolation<TestDTO>> violations2 = validator.validate(dto2);
        assertFalse(violations2.isEmpty(), "Null should be invalid when allowNull=false");
    }
    
    @Test
    @DisplayName("Blank handling should respect allowBlank setting")
    void testBlankHandling() {
        // Test with allowBlank=true (default)
        TestDTO dto1 = new TestDTO("");
        Set<ConstraintViolation<TestDTO>> violations1 = validator.validate(dto1);
        assertTrue(violations1.isEmpty(), "Blank should be valid when allowBlank=true");
        
        // Test with allowBlank=false
        TestDTO dto2 = new TestDTO(null, null, null, "   ");
        Set<ConstraintViolation<TestDTO>> violations2 = validator.validate(dto2);
        assertFalse(violations2.isEmpty(), "Blank should be invalid when allowBlank=false");
    }
    
    @Test
    @DisplayName("Custom field name should appear in error messages")
    void testCustomFieldName() {
        TestDTO dto = new TestDTO(null, "invalid", null, null);
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Invalid date should produce violation");
        
        String errorMessage = violations.iterator().next().getMessage();
        assertTrue(errorMessage.contains("Birth Date"), 
                  "Error message should contain custom field name");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "19800101", "20231231", "19900228", "20000229", "20240229",
        "19700430", "20200531", "19801031", "20211130", "19851225"
    })
    @DisplayName("Parameterized test for valid dates")
    void testValidDatesParameterized(String validDate) {
        TestDTO dto = new TestDTO(validDate);
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Date " + validDate + " should be valid");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "18801231", "21001231", "19801301", "19800001", "19800132",
        "19800100", "19800431", "19800631", "19800931", "19801131"
    })
    @DisplayName("Parameterized test for invalid dates")
    void testInvalidDatesParameterized(String invalidDate) {
        TestDTO dto = new TestDTO(invalidDate);
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Date " + invalidDate + " should be invalid");
    }
}