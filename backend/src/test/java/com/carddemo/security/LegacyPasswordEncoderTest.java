/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.security;

import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.UnitTest;
import com.carddemo.test.TestConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LegacyPasswordEncoder custom password encoder implementation.
 * 
 * This test class validates the COBOL RACF password compatibility features including:
 * - NoOpPasswordEncoder compatibility for existing RACF passwords
 * - Uppercase conversion matching COBOL FUNCTION UPPER-CASE behavior
 * - Password field padding and trimming for fixed-length COBOL fields (8 characters)
 * - Special character restrictions from mainframe password rules
 * - Password matching logic for authentication
 * - Backward compatibility with all existing user passwords from VSAM USRSEC dataset
 * 
 * Testing Strategy:
 * - Validates functional parity between COBOL password handling and Java implementation
 * - Ensures 100% compatibility with existing RACF password storage format
 * - Tests edge cases for fixed-length field handling and COBOL data type conversion
 * - Verifies security requirements while maintaining legacy system compatibility
 * 
 * Based on COSGN00C.cbl authentication logic:
 * - FUNCTION UPPER-CASE conversion for user ID and password (lines 132-136)
 * - Direct string comparison: IF SEC-USR-PWD = WS-USER-PWD (line 223)
 * - Fixed 8-character password field from CSUSR01Y.cpy (PIC X(08))
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Tag("unit")
public class LegacyPasswordEncoderTest extends AbstractBaseTest implements UnitTest {

    private LegacyPasswordEncoder passwordEncoder;

    /**
     * Setup method executed before each test execution.
     * Initializes the LegacyPasswordEncoder instance and performs base test setup
     * including mock object initialization and test fixture loading.
     * 
     * This method extends AbstractBaseTest.setUp() to provide password encoder
     * specific test initialization while maintaining all base testing capabilities.
     */
    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        passwordEncoder = new LegacyPasswordEncoder();
        mockCommonDependencies();
    }

    /**
     * Test encode method with uppercase conversion.
     * Validates that the password encoder converts passwords to uppercase
     * matching COBOL FUNCTION UPPER-CASE behavior from COSGN00C.cbl lines 132-136.
     * 
     * Tests:
     * - Lowercase input conversion to uppercase output
     * - Mixed case input normalization
     * - Already uppercase input preservation
     * - Null and empty input handling
     */
    @Test
    public void testEncodeWithUppercaseConversion() {
        // Test lowercase conversion - matches COBOL FUNCTION UPPER-CASE
        String lowercasePassword = "testpass";
        String encodedPassword = passwordEncoder.encode(lowercasePassword);
        assertThat(encodedPassword).isEqualTo("TESTPASS");
        
        // Test mixed case conversion
        String mixedCasePassword = "TestPass";
        String encodedMixedPassword = passwordEncoder.encode(mixedCasePassword);
        assertThat(encodedMixedPassword).isEqualTo("TESTPASS");
        
        // Test already uppercase preservation
        String uppercasePassword = "TESTPASS";
        String encodedUpperPassword = passwordEncoder.encode(uppercasePassword);
        assertThat(encodedUpperPassword).isEqualTo("TESTPASS");
        
        // Test null input handling
        assertThatThrownBy(() -> passwordEncoder.encode(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password cannot be null");
        
        // Test empty input handling
        String emptyPassword = "";
        String encodedEmptyPassword = passwordEncoder.encode(emptyPassword);
        assertThat(encodedEmptyPassword).isEmpty();
    }

    /**
     * Test matches method with exact password comparison.
     * Validates that password matching works correctly for exact password matches
     * using the same direct comparison logic as COBOL: IF SEC-USR-PWD = WS-USER-PWD.
     * 
     * Tests:
     * - Exact password match returns true
     * - Same content with consistent encoding
     * - Password encoder interface compliance
     */
    @Test
    public void testMatchesWithExactPassword() {
        String rawPassword = "testpass";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // Test exact match - should return true
        boolean matchResult = passwordEncoder.matches(rawPassword, encodedPassword);
        assertThat(matchResult).isTrue();
        
        // Test with uppercase raw password matching encoded
        boolean upperMatchResult = passwordEncoder.matches("TESTPASS", encodedPassword);
        assertThat(upperMatchResult).isTrue();
        
        // Test with TestConstants password
        String testPassword = TestConstants.TEST_USER_PASSWORD;
        String encodedTestPassword = passwordEncoder.encode(testPassword);
        boolean testPasswordMatch = passwordEncoder.matches(testPassword, encodedTestPassword);
        assertThat(testPasswordMatch).isTrue();
    }

    /**
     * Test matches method with different case combinations.
     * Validates that password matching handles case variations correctly,
     * maintaining COBOL uppercase conversion behavior throughout the matching process.
     * 
     * Tests:
     * - Lowercase raw password matching uppercase encoded password
     * - Mixed case raw password matching
     * - Case-insensitive matching behavior
     */
    @Test
    public void testMatchesWithDifferentCase() {
        String originalPassword = "TestPass"; // 8 characters - within COBOL field limit
        String encodedPassword = passwordEncoder.encode(originalPassword);
        
        // All these should match due to uppercase conversion
        assertThat(passwordEncoder.matches("testpass", encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("TESTPASS", encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("TestPass", encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("tEstPaSs", encodedPassword)).isTrue();
    }

    /**
     * Test matches method with invalid password.
     * Validates that password matching correctly rejects invalid passwords
     * maintaining the same security behavior as COBOL authentication logic.
     * 
     * Tests:
     * - Wrong password returns false
     * - Null password handling
     * - Empty password handling
     * - Invalid encoded password handling
     */
    @Test
    public void testMatchesWithInvalidPassword() {
        String correctPassword = "testpass";
        String encodedPassword = passwordEncoder.encode(correctPassword);
        
        // Test wrong password - should return false
        boolean wrongPasswordResult = passwordEncoder.matches("wrongpass", encodedPassword);
        assertThat(wrongPasswordResult).isFalse();
        
        // Test null raw password
        assertThatThrownBy(() -> passwordEncoder.matches(null, encodedPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Raw password cannot be null");
        
        // Test null encoded password
        assertThatThrownBy(() -> passwordEncoder.matches(correctPassword, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Encoded password cannot be null");
        
        // Test empty passwords
        assertThat(passwordEncoder.matches("", "")).isTrue(); // Both empty should match
        assertThat(passwordEncoder.matches("test", "")).isFalse(); // Non-empty vs empty
        assertThat(passwordEncoder.matches("", "test")).isFalse(); // Empty vs non-empty
    }

    /**
     * Test padding and trimming for 8-character COBOL field compatibility.
     * Validates that the password encoder handles fixed-length COBOL field requirements
     * from CSUSR01Y.cpy SEC-USR-PWD PIC X(08) definition.
     * 
     * Tests:
     * - Short passwords (less than 8 characters) - should not be padded for encoding
     * - Exact 8-character passwords - preserved exactly
     * - Long passwords (more than 8 characters) - should be truncated or rejected
     * - Whitespace handling and trimming
     */
    @Test
    public void testPaddingAndTrimmingFor8CharacterField() {
        // Test short password (less than 8 chars) - should not be padded
        String shortPassword = "test";
        String encodedShort = passwordEncoder.encode(shortPassword);
        assertThat(encodedShort).isEqualTo("TEST");
        assertThat(encodedShort.length()).isEqualTo(4);
        
        // Test exact 8-character password
        String exactPassword = "testpass"; // 8 characters
        String encodedExact = passwordEncoder.encode(exactPassword);
        assertThat(encodedExact).isEqualTo("TESTPASS");
        assertThat(encodedExact.length()).isEqualTo(8);
        
        // Test long password (more than 8 chars) - should validate and reject
        String longPassword = "verylongpassword"; // 16 characters
        assertThatThrownBy(() -> passwordEncoder.encode(longPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password exceeds maximum length of 8 characters");
        
        // Test password with leading/trailing spaces - should be trimmed
        String paddedPassword = " test ";
        String encodedPadded = passwordEncoder.encode(paddedPassword);
        assertThat(encodedPadded).isEqualTo("TEST");
        
        // Test password with internal spaces - should be preserved
        String spacedPassword = "te st";
        String encodedSpaced = passwordEncoder.encode(spacedPassword);
        assertThat(encodedSpaced).isEqualTo("TE ST");
    }

    /**
     * Test special character restrictions from mainframe password rules.
     * Validates that the password encoder enforces mainframe-compatible password
     * character restrictions while maintaining COBOL field validation behavior.
     * 
     * Tests:
     * - Valid alphanumeric characters
     * - Invalid special characters that should be rejected
     * - COBOL-compatible character set validation
     * - Password validation rules from legacy system
     */
    @Test
    public void testSpecialCharacterRestrictions() {
        // Test valid alphanumeric password
        String validPassword = "test123";
        assertThatCode(() -> passwordEncoder.encode(validPassword))
            .doesNotThrowAnyException();
        
        // Test invalid special characters that should be rejected
        String invalidPassword1 = "test@123";
        assertThatThrownBy(() -> passwordEncoder.encode(invalidPassword1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password contains invalid characters");
        
        String invalidPassword2 = "test$123";
        assertThatThrownBy(() -> passwordEncoder.encode(invalidPassword2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password contains invalid characters");
        
        String invalidPassword3 = "test#123";
        assertThatThrownBy(() -> passwordEncoder.encode(invalidPassword3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password contains invalid characters");
        
        // Test password with underscores and hyphens (often allowed in mainframe)
        String validPasswordWithUnderscore = "test_123";
        assertThatCode(() -> passwordEncoder.encode(validPasswordWithUnderscore))
            .doesNotThrowAnyException();
        
        String validPasswordWithHyphen = "test-123";
        assertThatCode(() -> passwordEncoder.encode(validPasswordWithHyphen))
            .doesNotThrowAnyException();
    }

    /**
     * Test NoOpPasswordEncoder compatibility for existing RACF passwords.
     * Validates that the LegacyPasswordEncoder maintains compatibility with
     * plain-text password storage matching NoOpPasswordEncoder behavior.
     * 
     * Tests:
     * - Interface compatibility with Spring Security PasswordEncoder
     * - Plain-text password preservation
     * - Existing password migration support
     * - RACF password format preservation
     */
    @Test
    public void testNoOpPasswordEncoderCompatibility() {
        // Verify LegacyPasswordEncoder implements PasswordEncoder interface
        assertThat(passwordEncoder).isInstanceOf(PasswordEncoder.class);
        
        // Test that encoding preserves plain-text nature (like NoOpPasswordEncoder)
        String plainPassword = "testpass"; // 8 characters - within COBOL field limit
        String encoded = passwordEncoder.encode(plainPassword);
        
        // Should be uppercase but still plain text (no hashing)
        assertThat(encoded).isEqualTo("TESTPASS");
        assertThat(encoded).doesNotContain("$2a$"); // Not BCrypt
        assertThat(encoded).doesNotContain("{"); // Not DelegatingPasswordEncoder format
        
        // Test compatibility with existing RACF passwords from TestConstants
        String racfPassword = TestConstants.TEST_USER_PASSWORD; // "testpass"
        String encodedRacf = passwordEncoder.encode(racfPassword);
        assertThat(passwordEncoder.matches(racfPassword, encodedRacf)).isTrue();
        
        // Test that stored password matches original (uppercase converted)
        assertThat(passwordEncoder.matches("testpass", "TESTPASS")).isTrue();
        assertThat(passwordEncoder.matches("TESTPASS", "TESTPASS")).isTrue();
    }

    /**
     * Test COBOL field length validation for 8-character password constraint.
     * Validates that the password encoder enforces the exact 8-character limit
     * from CSUSR01Y.cpy SEC-USR-PWD PIC X(08) field definition.
     * 
     * Tests:
     * - Maximum length enforcement (8 characters)
     * - Minimum length handling (no minimum enforced)
     * - Field length validation during encoding
     * - Error messages for length violations
     */
    @Test
    public void testCobolFieldLengthValidation() {
        // Test maximum valid length (8 characters)
        String maxLengthPassword = "12345678";
        assertThatCode(() -> passwordEncoder.encode(maxLengthPassword))
            .doesNotThrowAnyException();
        
        String encoded8Char = passwordEncoder.encode(maxLengthPassword);
        assertThat(encoded8Char).isEqualTo("12345678");
        assertThat(encoded8Char.length()).isEqualTo(8);
        
        // Test exceeding maximum length (9+ characters)
        String tooLongPassword = "123456789";
        assertThatThrownBy(() -> passwordEncoder.encode(tooLongPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password exceeds maximum length of 8 characters");
        
        // Test minimum length (1 character) - should be allowed
        String minLengthPassword = "A";
        assertThatCode(() -> passwordEncoder.encode(minLengthPassword))
            .doesNotThrowAnyException();
        
        String encoded1Char = passwordEncoder.encode(minLengthPassword);
        assertThat(encoded1Char).isEqualTo("A");
        assertThat(encoded1Char.length()).isEqualTo(1);
        
        // Test validatePasswordFormat method directly
        assertThat(passwordEncoder.validatePasswordFormat("12345678")).isTrue();
        assertThat(passwordEncoder.validatePasswordFormat("123456789")).isFalse();
        assertThat(passwordEncoder.validatePasswordFormat("A")).isTrue();
        assertThat(passwordEncoder.validatePasswordFormat("")).isTrue(); // Empty allowed for validation
    }

    /**
     * Test password normalization functionality.
     * Validates that the normalizePassword method handles whitespace trimming,
     * case conversion, and field formatting according to COBOL data handling rules.
     * 
     * Tests:
     * - Whitespace trimming (leading and trailing)
     * - Uppercase conversion
     * - Null and empty input handling
     * - Internal whitespace preservation
     */
    @Test
    public void testPasswordNormalization() {
        // Test whitespace trimming
        String paddedPassword = "  test  ";
        String normalized = passwordEncoder.normalizePassword(paddedPassword);
        assertThat(normalized).isEqualTo("TEST");
        
        // Test case conversion
        String mixedCasePassword = "TestPass";
        String normalizedCase = passwordEncoder.normalizePassword(mixedCasePassword);
        assertThat(normalizedCase).isEqualTo("TESTPASS");
        
        // Test null input
        assertThatThrownBy(() -> passwordEncoder.normalizePassword(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password cannot be null");
        
        // Test empty input
        String emptyPassword = "";
        String normalizedEmpty = passwordEncoder.normalizePassword(emptyPassword);
        assertThat(normalizedEmpty).isEmpty();
        
        // Test internal spaces preservation
        String spacedPassword = "te st";
        String normalizedSpaced = passwordEncoder.normalizePassword(spacedPassword);
        assertThat(normalizedSpaced).isEqualTo("TE ST");
        
        // Test tab and newline handling
        String complexWhitespace = "\ttest\n";
        String normalizedComplex = passwordEncoder.normalizePassword(complexWhitespace);
        assertThat(normalizedComplex).isEqualTo("TEST");
    }

    /**
     * Test legacy format detection functionality.
     * Validates that the isLegacyFormat method correctly identifies passwords
     * that follow the legacy RACF/COBOL format patterns.
     * 
     * Tests:
     * - Valid legacy format detection
     * - Invalid format rejection
     * - Length-based format validation
     * - Character set validation
     */
    @Test
    public void testLegacyFormatDetection() {
        // Test valid legacy format (uppercase, 8 chars or less, alphanumeric)
        assertThat(passwordEncoder.isLegacyFormat("TESTPASS")).isTrue(); // 8 chars
        assertThat(passwordEncoder.isLegacyFormat("TEST")).isTrue(); // 4 chars
        assertThat(passwordEncoder.isLegacyFormat("TEST123")).isTrue(); // alphanumeric
        
        // Test invalid legacy format (too long)
        assertThat(passwordEncoder.isLegacyFormat("VERYLONGPASSWORD")).isFalse(); // 16 chars
        
        // Test invalid legacy format (lowercase - not normalized)
        assertThat(passwordEncoder.isLegacyFormat("testpass")).isFalse(); // lowercase
        
        // Test invalid legacy format (special characters)
        assertThat(passwordEncoder.isLegacyFormat("TEST@123")).isFalse(); // special char
        
        // Test edge cases
        assertThat(passwordEncoder.isLegacyFormat("")).isTrue(); // empty is legacy format
        assertThatThrownBy(() -> passwordEncoder.isLegacyFormat(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Test upgrade encoding support for password migration.
     * Validates that the upgradeEncoding method supports password migration
     * scenarios while maintaining backward compatibility.
     * 
     * Tests:
     * - Upgrade encoding capability
     * - Backward compatibility preservation
     * - Migration path support
     */
    @Test
    public void testUpgradeEncodingSupport() {
        String password = "testpass";
        String encodedPassword = passwordEncoder.encode(password);
        
        // Test that upgradeEncoding returns false for legacy passwords
        // (indicating no upgrade needed - already in correct format)
        boolean needsUpgrade = passwordEncoder.upgradeEncoding(encodedPassword);
        assertThat(needsUpgrade).isFalse();
        
        // Test with various password formats
        assertThat(passwordEncoder.upgradeEncoding("TESTPASS")).isFalse(); // Valid legacy
        assertThat(passwordEncoder.upgradeEncoding("TEST")).isFalse(); // Valid legacy
        assertThat(passwordEncoder.upgradeEncoding("")).isFalse(); // Empty legacy
        
        // Test with null input
        assertThatThrownBy(() -> passwordEncoder.upgradeEncoding(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Encoded password cannot be null");
    }

    /**
     * Test parameterized password scenarios with various inputs.
     * Validates password encoder behavior across a wide range of input scenarios
     * using JUnit 5 parameterized testing for comprehensive validation coverage.
     * 
     * Tests various password formats:
     * - Different lengths within valid range
     * - Mixed case combinations
     * - Alphanumeric patterns
     * - Edge cases and boundary conditions
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "a", "ab", "abc", "test", "test1", "test12", "test123", "testpass",
        "A", "AB", "ABC", "TEST", "TEST1", "TEST12", "TEST123", "TESTPASS",
        "Test", "TeSt", "tEsT", "Test123", "TeSt123", "tEsT123"
    })
    public void testParameterizedPasswordScenarios(String password) {
        // All these passwords should encode successfully
        assertThatCode(() -> passwordEncoder.encode(password))
            .doesNotThrowAnyException();
        
        // All encoded passwords should be uppercase
        String encoded = passwordEncoder.encode(password);
        assertThat(encoded).isEqualTo(password.trim().toUpperCase());
        
        // All passwords should match their encoded versions
        assertThat(passwordEncoder.matches(password, encoded)).isTrue();
        
        // Length validation
        String trimmedPassword = password.trim();
        assertThat(trimmedPassword.length()).isLessThanOrEqualTo(8);
        
        // Validate legacy format detection
        if (trimmedPassword.equals(trimmedPassword.toUpperCase()) && 
            trimmedPassword.length() <= 8 &&
            trimmedPassword.matches("^[A-Z0-9\\-_]*$")) {
            assertThat(passwordEncoder.isLegacyFormat(encoded)).isTrue();
        }
    }
}