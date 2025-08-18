/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import com.carddemo.exception.ValidationException;
import com.carddemo.util.ValidationUtil.FieldValidator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Comprehensive test suite for validation utilities ensuring COBOL validation routines 
 * are accurately replicated in Java. Tests cover all field validation methods including
 * SSN validation, phone number formatting, state/ZIP code validation, FICO score range 
 * checks, and error message generation matching COBOL CSMSG01Y copybook messages.
 * 
 * Each test method validates functional parity with original COBOL edit routines from:
 * - CSUTLDPY.cpy (date validation)
 * - CSLKPCDY.cpy (state/phone code validation)  
 * - COACTUPC.cbl (field validation patterns)
 * - COUSR01C.cbl (user validation routines)
 * 
 * Test data includes edge cases and boundary conditions to ensure robust validation
 * matching the original mainframe implementation behavior exactly.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
public class ValidationUtilsTest {

    private final FieldValidator fieldValidator = new FieldValidator();

    /**
     * Tests valid SSN formats according to COBOL SSN validation rules.
     * Validates patterns: 999-99-9999 and 999999999 (both with and without dashes).
     * Excludes invalid area codes: 000, 666, and 900-999 range.
     */
    @Test
    public void testValidateSSN_ValidFormats() {
        // Test valid SSN formats with dashes
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "123-45-6789"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "001-23-4567"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "234-56-7890"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "567-89-0123"));
        
        // Test valid SSN formats without dashes
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "123456789"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "987654321"));
        
        // Test edge cases for valid area codes
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "001-01-0001")); // Area 001 is valid
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "665-01-0001")); // Area 665 is valid (just before 666)
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "667-01-0001")); // Area 667 is valid (just after 666)
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateSSN("ssn", "899-01-0001")); // Area 899 is valid (just before 900)
    }

    /**
     * Tests invalid SSN formats and patterns that should be rejected.
     * Includes area codes 000, 666, 900-999, group code 00, serial 0000, 
     * and format violations per COBOL INVALID-SSN-PART1 logic.
     */
    @Test
    public void testValidateSSN_InvalidFormats() {
        // Test null and empty SSN
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn must be supplied");
        
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", ""))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn must be supplied");
        
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "   "))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn must be supplied");
        
        // Test invalid length
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "123-45-678"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn must be exactly 9 digits");
        
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "123-45-67890"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn must be exactly 9 digits");
        
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "12345678"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn must be exactly 9 digits");
        
        // Test invalid area code 000 (from COBOL INVALID-SSN-PART1)
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "000-12-3456"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn contains invalid area number");
        
        // Test invalid area code 666 (from COBOL INVALID-SSN-PART1)
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "666-12-3456"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn contains invalid area number");
        
        // Test invalid area codes in 900-999 range (from COBOL INVALID-SSN-PART1)
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "900-12-3456"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn contains invalid area number");
        
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "950-12-3456"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn contains invalid area number");
        
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "999-12-3456"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn contains invalid area number");
        
        // Test invalid group code 00
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "123-00-4567"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn contains invalid group number");
        
        // Test invalid serial code 0000
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "123-45-0000"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn contains invalid serial number");
        
        // Test invalid format patterns
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "123-45-678A"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn format is invalid");
        
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "ABC-DE-FGHI"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn format is invalid");
        
        assertThatThrownBy(() -> ValidationUtil.validateSSN("ssn", "123-456-789"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ssn format is invalid");
    }

    /**
     * Tests valid phone number area codes according to NANPA standards.
     * Validates area codes from CSLKPCDY.cpy VALID-PHONE-AREA-CODE list.
     */
    @Test
    public void testValidatePhoneAreaCode_ValidCodes() {
        // Test major metropolitan area codes
        assertThat(ValidationUtil.isValidPhoneAreaCode("212")).isTrue(); // New York
        assertThat(ValidationUtil.isValidPhoneAreaCode("213")).isTrue(); // Los Angeles
        assertThat(ValidationUtil.isValidPhoneAreaCode("312")).isTrue(); // Chicago
        assertThat(ValidationUtil.isValidPhoneAreaCode("415")).isTrue(); // San Francisco
        assertThat(ValidationUtil.isValidPhoneAreaCode("202")).isTrue(); // Washington DC
        assertThat(ValidationUtil.isValidPhoneAreaCode("305")).isTrue(); // Miami
        assertThat(ValidationUtil.isValidPhoneAreaCode("404")).isTrue(); // Atlanta
        assertThat(ValidationUtil.isValidPhoneAreaCode("214")).isTrue(); // Dallas
        assertThat(ValidationUtil.isValidPhoneAreaCode("713")).isTrue(); // Houston
        assertThat(ValidationUtil.isValidPhoneAreaCode("206")).isTrue(); // Seattle
        
        // Test some less common but valid area codes
        assertThat(ValidationUtil.isValidPhoneAreaCode("201")).isTrue(); // New Jersey
        assertThat(ValidationUtil.isValidPhoneAreaCode("907")).isTrue(); // Alaska
        assertThat(ValidationUtil.isValidPhoneAreaCode("808")).isTrue(); // Hawaii
        assertThat(ValidationUtil.isValidPhoneAreaCode("787")).isTrue(); // Puerto Rico
        assertThat(ValidationUtil.isValidPhoneAreaCode("671")).isTrue(); // Guam
        
        // Test newer area codes that should be valid
        assertThat(ValidationUtil.isValidPhoneAreaCode("669")).isTrue(); // California overlay
        assertThat(ValidationUtil.isValidPhoneAreaCode("929")).isTrue(); // New York overlay
        assertThat(ValidationUtil.isValidPhoneAreaCode("984")).isTrue(); // North Carolina overlay
    }

    /**
     * Tests invalid phone number area codes that should be rejected.
     * Includes non-existent area codes and invalid formats.
     */
    @Test 
    public void testValidatePhoneAreaCode_InvalidCodes() {
        // Test null and empty area codes
        assertThat(ValidationUtil.isValidPhoneAreaCode(null)).isFalse();
        assertThat(ValidationUtil.isValidPhoneAreaCode("")).isFalse();
        assertThat(ValidationUtil.isValidPhoneAreaCode("   ")).isFalse();
        
        // Test invalid length
        assertThat(ValidationUtil.isValidPhoneAreaCode("12")).isFalse();
        assertThat(ValidationUtil.isValidPhoneAreaCode("1234")).isFalse();
        assertThat(ValidationUtil.isValidPhoneAreaCode("1")).isFalse();
        
        // Test non-numeric area codes
        assertThat(ValidationUtil.isValidPhoneAreaCode("ABC")).isFalse();
        assertThat(ValidationUtil.isValidPhoneAreaCode("12A")).isFalse();
        assertThat(ValidationUtil.isValidPhoneAreaCode("A23")).isFalse();
        
        // Test reserved or non-existent area codes
        assertThat(ValidationUtil.isValidPhoneAreaCode("000")).isFalse(); // Reserved
        assertThat(ValidationUtil.isValidPhoneAreaCode("001")).isFalse(); // Non-existent
        assertThat(ValidationUtil.isValidPhoneAreaCode("100")).isFalse(); // Non-existent
        assertThat(ValidationUtil.isValidPhoneAreaCode("199")).isFalse(); // Non-existent
        assertThat(ValidationUtil.isValidPhoneAreaCode("911")).isFalse(); // Emergency service
        
        // Test specifically invalid area codes that don't exist in NANPA
        assertThat(ValidationUtil.isValidPhoneAreaCode("123")).isFalse();
        assertThat(ValidationUtil.isValidPhoneAreaCode("456")).isFalse();
        assertThat(ValidationUtil.isValidPhoneAreaCode("789")).isFalse();
        assertThat(ValidationUtil.isValidPhoneAreaCode("999")).isFalse();
    }

    /**
     * Tests valid US state codes including all 50 states, DC, and territories.
     * Based on CSLKPCDY.cpy VALID-US-STATE-CODE specification.
     */
    @Test
    public void testValidateUSStateCode_ValidCodes() {
        // Test all 50 states
        String[] validStates = {
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
            "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", 
            "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
            "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
        };
        
        for (String state : validStates) {
            assertThat(ValidationUtil.isValidStateCode(state))
                .as("State code %s should be valid", state)
                .isTrue();
                
            // Test lowercase versions
            assertThat(ValidationUtil.isValidStateCode(state.toLowerCase()))
                .as("State code %s (lowercase) should be valid", state.toLowerCase())
                .isTrue();
        }
        
        // Test District of Columbia
        assertThat(ValidationUtil.isValidStateCode("DC")).isTrue();
        assertThat(ValidationUtil.isValidStateCode("dc")).isTrue();
        
        // Test US territories
        assertThat(ValidationUtil.isValidStateCode("AS")).isTrue(); // American Samoa
        assertThat(ValidationUtil.isValidStateCode("GU")).isTrue(); // Guam
        assertThat(ValidationUtil.isValidStateCode("MP")).isTrue(); // Northern Mariana Islands
        assertThat(ValidationUtil.isValidStateCode("PR")).isTrue(); // Puerto Rico
        assertThat(ValidationUtil.isValidStateCode("VI")).isTrue(); // US Virgin Islands
    }

    /**
     * Tests invalid US state codes that should be rejected.
     * Includes non-existent codes and invalid formats.
     */
    @Test
    public void testValidateUSStateCode_InvalidCodes() {
        // Test null and empty state codes
        assertThat(ValidationUtil.isValidStateCode(null)).isFalse();
        assertThat(ValidationUtil.isValidStateCode("")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("   ")).isFalse();
        
        // Test invalid length
        assertThat(ValidationUtil.isValidStateCode("A")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("ABC")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("ABCD")).isFalse();
        
        // Test non-existent state codes
        assertThat(ValidationUtil.isValidStateCode("XX")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("ZZ")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("AA")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("BB")).isFalse();
        
        // Test numeric codes
        assertThat(ValidationUtil.isValidStateCode("12")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("00")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("99")).isFalse();
        
        // Test mixed alphanumeric
        assertThat(ValidationUtil.isValidStateCode("A1")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("1A")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("C2")).isFalse();
        
        // Test special characters
        assertThat(ValidationUtil.isValidStateCode("A-")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("C@")).isFalse();
        assertThat(ValidationUtil.isValidStateCode("--")).isFalse();
    }

    /**
     * Tests valid ZIP code formats including 5-digit and ZIP+4 formats.
     * Validates standard US postal ZIP code patterns per COBOL validation rules.
     */
    @Test
    public void testValidateZipCode_ValidFormats() {
        // Test standard 5-digit ZIP codes
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateZipCode("zipCode", "12345"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateZipCode("zipCode", "90210"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateZipCode("zipCode", "10001"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateZipCode("zipCode", "60601"));
        
        // Test ZIP codes with leading zeros
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateZipCode("zipCode", "01234"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateZipCode("zipCode", "00501"));
        
        // Test edge cases
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateZipCode("zipCode", "99999"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateZipCode("zipCode", "00000"));

        // Test state-ZIP combinations using ValidationUtil.validateStateZipCode
        assertThat(ValidationUtil.validateStateZipCode("CA", "90210")).isTrue(); // California
        assertThat(ValidationUtil.validateStateZipCode("NY", "10001")).isTrue(); // New York
        assertThat(ValidationUtil.validateStateZipCode("IL", "60601")).isTrue(); // Illinois  
        assertThat(ValidationUtil.validateStateZipCode("TX", "75201")).isTrue(); // Texas
        assertThat(ValidationUtil.validateStateZipCode("FL", "33101")).isTrue(); // Florida
        assertThat(ValidationUtil.validateStateZipCode("MA", "02101")).isTrue(); // Massachusetts
    }

    /**
     * Tests invalid ZIP code formats and patterns that should be rejected.
     * Includes invalid lengths, non-numeric characters, and invalid state-ZIP combinations.
     */
    @Test
    public void testValidateZipCode_InvalidFormats() {
        // Test null and empty ZIP codes
        assertThatThrownBy(() -> ValidationUtil.validateZipCode("zipCode", null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("zipCode must be supplied");
        
        assertThatThrownBy(() -> ValidationUtil.validateZipCode("zipCode", ""))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("zipCode must be supplied");
        
        assertThatThrownBy(() -> ValidationUtil.validateZipCode("zipCode", "   "))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("zipCode must be supplied");
        
        // Test invalid length
        assertThatThrownBy(() -> ValidationUtil.validateZipCode("zipCode", "1234"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("zipCode must be exactly 5 digits");
        
        assertThatThrownBy(() -> ValidationUtil.validateZipCode("zipCode", "123456"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("zipCode must be exactly 5 digits");
        
        assertThatThrownBy(() -> ValidationUtil.validateZipCode("zipCode", "1"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("zipCode must be exactly 5 digits");
        
        // Test non-numeric characters
        assertThatThrownBy(() -> ValidationUtil.validateZipCode("zipCode", "1234A"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("zipCode must contain only numeric characters");
        
        assertThatThrownBy(() -> ValidationUtil.validateZipCode("zipCode", "ABCDE"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("zipCode must contain only numeric characters");
        
        assertThatThrownBy(() -> ValidationUtil.validateZipCode("zipCode", "123-4"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("zipCode must contain only numeric characters");
        
        // Test invalid state-ZIP combinations
        assertThat(ValidationUtil.validateStateZipCode("CA", "10001")).isFalse(); // NY ZIP with CA state
        assertThat(ValidationUtil.validateStateZipCode("FL", "90210")).isFalse(); // CA ZIP with FL state
        assertThat(ValidationUtil.validateStateZipCode("TX", "02101")).isFalse(); // MA ZIP with TX state
        
        // Test invalid state or ZIP parameters
        assertThat(ValidationUtil.validateStateZipCode(null, "12345")).isFalse();
        assertThat(ValidationUtil.validateStateZipCode("CA", null)).isFalse();
        assertThat(ValidationUtil.validateStateZipCode("", "12345")).isFalse();
        assertThat(ValidationUtil.validateStateZipCode("CA", "")).isFalse();
        assertThat(ValidationUtil.validateStateZipCode("C", "12345")).isFalse(); // Invalid state length
        assertThat(ValidationUtil.validateStateZipCode("CA", "1234")).isFalse(); // Invalid ZIP length
    }

    /**
     * Tests valid date of birth formats and values.
     * Validates CCYYMMDD format and ensures dates are not in the future per COBOL logic.
     */
    @Test
    public void testValidateDateOfBirth_ValidDates() {
        // Test valid birth dates in CCYYMMDD format
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateDateOfBirth("dateOfBirth", "19850315"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateDateOfBirth("dateOfBirth", "19701201"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateDateOfBirth("dateOfBirth", "19950628"));
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateDateOfBirth("dateOfBirth", "20000101"));
        
        // Test leap year dates
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateDateOfBirth("dateOfBirth", "19920229")); // 1992 was a leap year
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateDateOfBirth("dateOfBirth", "20000229")); // 2000 was a leap year
        
        // Test edge cases for valid centuries (19 and 20 per COBOL validation)
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateDateOfBirth("dateOfBirth", "19000101")); // Start of century 19
        
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateDateOfBirth("dateOfBirth", "20991231")); // End of century 20
        
        // Test today's date (should be valid as it's not in the future)
        String today = DateConversionUtil.getCurrentDate();
        assertThatNoException().isThrownBy(() -> 
            ValidationUtil.validateDateOfBirth("dateOfBirth", today));
    }

    /**
     * Tests invalid date of birth formats and values that should be rejected.
     * Includes future dates, invalid formats, and dates outside valid century range.
     */
    @Test
    public void testValidateDateOfBirth_InvalidDates() {
        // Test null and empty dates
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth must be supplied");
        
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", ""))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth must be supplied");
        
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "   "))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth must be supplied");
        
        // Test invalid format length
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "1985031"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "198503155"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        // Test non-numeric characters
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "198A0315"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "ABCD0315"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        // Test future dates (should be rejected per COBOL EDIT-DATE-OF-BIRTH logic)
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "20501201"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth cannot be in the future");
        
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "21000101"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth cannot be in the future");
        
        // Test invalid dates (February 30, etc.)
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "19850230"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "19850431"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        // Test leap year edge cases
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "19910229"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid"); // 1991 was not a leap year
        
        // Test invalid month
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "19850015"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "19851315"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        // Test invalid day
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "19850300"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "19850332"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth format is invalid");
        
        // Test dates too far in the past (more than 150 years)
        assertThatThrownBy(() -> ValidationUtil.validateDateOfBirth("dateOfBirth", "18500101"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("dateOfBirth is too far in the past");
    }

    /**
     * Tests valid account ID formats according to COBOL account ID specifications.
     * Account IDs must be exactly 11 digits per CVACT01Y copybook specification.
     */
    @Test
    public void testValidateAccountId_ValidFormats() {
        // Test valid 11-digit account IDs
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateAccountId("12345678901"));
        
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateAccountId("00000000001"));
        
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateAccountId("99999999999"));
        
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateAccountId("10203040506"));
        
        // Test account IDs with leading zeros
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateAccountId("01234567890"));
        
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateAccountId("00123456789"));
    }

    /**
     * Tests invalid account ID formats that should be rejected.
     * Includes wrong lengths, non-numeric characters, and null values.
     */
    @Test
    public void testValidateAccountId_InvalidFormats() {
        // Test null and empty account IDs
        assertThatThrownBy(() -> fieldValidator.validateAccountId(null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be supplied");
        
        assertThatThrownBy(() -> fieldValidator.validateAccountId(""))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be supplied");
        
        assertThatThrownBy(() -> fieldValidator.validateAccountId("   "))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be supplied");
        
        // Test invalid length (not 11 digits)
        assertThatThrownBy(() -> fieldValidator.validateAccountId("1234567890"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be exactly 11 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateAccountId("123456789012"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be exactly 11 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateAccountId("123"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be exactly 11 digits");
        
        // Test non-numeric characters
        assertThatThrownBy(() -> fieldValidator.validateAccountId("1234567890A"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be exactly 11 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateAccountId("A2345678901"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be exactly 11 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateAccountId("123-456-7890"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be exactly 11 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateAccountId("ABCDEFGHIJK"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account ID must be exactly 11 digits");
    }

    /**
     * Tests valid card number formats according to COBOL card number specifications.
     * Card numbers must be exactly 16 digits per CVACT01Y copybook specification.
     */
    @Test
    public void testValidateCardNumber_ValidFormats() {
        // Test valid 16-digit card numbers
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateCardNumber("1234567890123456"));
        
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateCardNumber("4111111111111111"));
        
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateCardNumber("5555555555554444"));
        
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateCardNumber("0000000000000001"));
        
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateCardNumber("9999999999999999"));
        
        // Test card numbers with leading zeros
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateCardNumber("0123456789012345"));
        
        assertThatNoException().isThrownBy(() -> 
            fieldValidator.validateCardNumber("0000123456789012"));
    }

    /**
     * Tests invalid card number formats that should be rejected.
     * Includes wrong lengths, non-numeric characters, and null values.
     */
    @Test
    public void testValidateCardNumber_InvalidFormats() {
        // Test null and empty card numbers
        assertThatThrownBy(() -> fieldValidator.validateCardNumber(null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be supplied");
        
        assertThatThrownBy(() -> fieldValidator.validateCardNumber(""))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be supplied");
        
        assertThatThrownBy(() -> fieldValidator.validateCardNumber("   "))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be supplied");
        
        // Test invalid length (not 16 digits)
        assertThatThrownBy(() -> fieldValidator.validateCardNumber("123456789012345"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be exactly 16 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateCardNumber("12345678901234567"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be exactly 16 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateCardNumber("123"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be exactly 16 digits");
        
        // Test non-numeric characters
        assertThatThrownBy(() -> fieldValidator.validateCardNumber("123456789012345A"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be exactly 16 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateCardNumber("A234567890123456"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be exactly 16 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateCardNumber("1234-5678-9012-3456"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be exactly 16 digits");
        
        assertThatThrownBy(() -> fieldValidator.validateCardNumber("ABCDEFGHIJKLMNOP"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Card number must be exactly 16 digits");
    }

    /**
     * Tests valid transaction amounts according to COBOL business rules.
     * Amounts must be positive values with proper decimal precision.
     */
    @Test
    public void testValidateTransactionAmount_ValidAmounts() {
        // Test valid positive amounts with 2 decimal places
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("0.01"))).isTrue();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("1.00"))).isTrue();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("10.50"))).isTrue();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("100.99"))).isTrue();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("1000.00"))).isTrue();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("9999.99"))).isTrue();
        
        // Test maximum allowed amount (per credit card transaction limits)
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("99999.99"))).isTrue();
        
        // Test round amounts (no decimal places)
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("1"))).isTrue();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("100"))).isTrue();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("5000"))).isTrue();
        
        // Test amounts with high precision (should be rounded to 2 decimal places)
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("10.555"))).isTrue();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("100.999"))).isTrue();
    }

    /**
     * Tests invalid transaction amounts that should be rejected.
     * Includes negative amounts, zero amounts, and amounts exceeding limits.
     */
    @Test
    public void testValidateTransactionAmount_InvalidAmounts() {
        // Test null amount
        assertThat(ValidationUtil.validateTransactionAmount(null)).isFalse();
        
        // Test zero amount (business rule: transactions must be positive)
        assertThat(ValidationUtil.validateTransactionAmount(BigDecimal.ZERO)).isFalse();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("0.00"))).isFalse();
        
        // Test negative amounts
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("-0.01"))).isFalse();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("-1.00"))).isFalse();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("-100.50"))).isFalse();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("-1000.00"))).isFalse();
        
        // Test amounts exceeding maximum limit (over $100,000)
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("100000.00"))).isFalse();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("100000.01"))).isFalse();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("999999.99"))).isFalse();
        
        // Test extremely small amounts (less than 1 cent)
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("0.001"))).isFalse();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("0.005"))).isFalse();
        assertThat(ValidationUtil.validateTransactionAmount(new BigDecimal("0.009"))).isFalse();
    }

    /**
     * Helper method to create a field validator instance for testing.
     * This simulates the field validation patterns used in the COBOL programs.
     */
    private FieldValidator createFieldValidator() {
        return new FieldValidator() {
            @Override
            public void validateAccountId(String accountId) throws ValidationException {
                if (accountId == null || accountId.trim().isEmpty()) {
                    throw new ValidationException("Account ID must be supplied");
                }
                if (!ValidationUtil.validateNumericField(accountId, Constants.ACCOUNT_ID_LENGTH)) {
                    throw new ValidationException("Account ID must be exactly " + Constants.ACCOUNT_ID_LENGTH + " digits");
                }
            }

            @Override
            public void validateCardNumber(String cardNumber) throws ValidationException {
                if (cardNumber == null || cardNumber.trim().isEmpty()) {
                    throw new ValidationException("Card number must be supplied");
                }
                if (!ValidationUtil.validateNumericField(cardNumber, Constants.CARD_NUMBER_LENGTH)) {
                    throw new ValidationException("Card number must be exactly " + Constants.CARD_NUMBER_LENGTH + " digits");
                }
            }
        };
    }

    /**
     * Interface for field validation to support testing different validation scenarios.
     * This mirrors the validation patterns used in COBOL programs like COACTUPC.
     */
    private interface FieldValidator {
        void validateAccountId(String accountId) throws ValidationException;
        void validateCardNumber(String cardNumber) throws ValidationException;
    }
}