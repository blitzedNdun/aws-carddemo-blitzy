/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.Map;

import com.carddemo.test.TestConstants;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.UnitTest;

/**
 * Comprehensive unit test class for COBOL string formatting utilities that validates
 * conversion between COBOL PIC clause formatted strings and Java strings.
 * 
 * This test suite ensures 100% functional parity with COBOL string operations
 * including handling of FILLER fields, alphanumeric padding, fixed-length field
 * formatting, and COBOL JUSTIFIED RIGHT clause behavior. All tests validate exact
 * compatibility with COBOL MOVE semantics and string display patterns.
 * 
 * Test Coverage:
 * - PIC X clause alphanumeric field formatting with space padding
 * - PIC 9 clause numeric field formatting with zero padding
 * - JUSTIFIED RIGHT clause handling with left padding
 * - Fixed-length field boundary maintenance and record layout preservation
 * - FILLER field placeholder processing and generation
 * - String truncation and padding logic with configurable characters
 * - Edited numeric field formatting with decimal points and commas
 * - Trailing space preservation for COBOL display compatibility
 * - Bidirectional conversion between COBOL and Java string formats
 * - PIC clause parsing and component extraction
 * 
 * Performance Requirements:
 * - All tests must complete within performance thresholds
 * - Validates string formatting efficiency for high-volume processing
 * - Ensures memory-efficient operations for large datasets
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@DisplayName("COBOL String Formatter Test Suite")
public class CobolStringFormatterTest extends AbstractBaseTest implements UnitTest {

    /**
     * Tests alphanumeric field formatting according to COBOL PIC X clause specifications.
     * Validates space padding, truncation, and exact field length maintenance for
     * alphanumeric data fields matching COBOL MOVE semantics.
     *
     * Test scenarios include:
     * - Short strings requiring right padding with spaces
     * - Exact length strings requiring no modification
     * - Long strings requiring truncation to field length
     * - Null and empty string handling
     * - Boundary conditions and edge cases
     *
     * @param input the input string value to format
     * @param length the target PIC X field length
     * @param expected the expected formatted result
     */
    @ParameterizedTest
    @CsvSource({
        "'Hello', 10, 'Hello     '",  // Short string - pad with spaces
        "'Test', 4, 'Test'",          // Exact length - no change
        "'Testing123', 5, 'Testi'",   // Long string - truncate
        "'', 3, '   '",               // Empty string - all spaces
        "'A', 1, 'A'",                // Single character exact
        "'AB', 1, 'A'",               // Single character truncate
        "'   ', 5, '     '",          // Spaces only - extend with spaces
        "'Data', 8, 'Data    '",      // COCOM01Y.cpy pattern - 8 chars
        "'Test User Name', 25, 'Test User Name          '", // COCOM01Y.cpy pattern - 25 chars
        "'1234', 4, '1234'"           // Numeric in alphanumeric field
    })
    @DisplayName("PIC X Alphanumeric Field Formatting")
    void testFormatAlphanumericField(String input, int length, String expected) {
        // Test the formatAlphanumericField method
        String result = CobolStringFormatter.formatAlphanumericField(input, length);
        
        assertThat(result)
            .describedAs("PIC X(%d) formatting of '%s'", length, input)
            .isEqualTo(expected)
            .hasSize(length);
            
        // Validate that result maintains fixed field boundary
        assertThat(result.length())
            .describedAs("Field length must match PIC X specification")
            .isEqualTo(length);
            
        logTestExecution("PIC X formatting test passed for input: " + input, null);
    }

    /**
     * Tests numeric field formatting according to COBOL PIC 9 clause specifications.
     * Validates zero padding, numeric cleaning, and exact field length maintenance
     * for numeric data fields matching COBOL numeric MOVE semantics.
     *
     * @param input the input numeric string to format
     * @param length the target PIC 9 field length
     * @param expected the expected formatted result
     */
    @ParameterizedTest
    @CsvSource({
        "'123', 5, '00123'",          // Short number - pad with zeros
        "'456', 3, '456'",            // Exact length - no change
        "'123456789', 5, '56789'",    // Long number - truncate left
        "'', 4, '0000'",              // Empty - all zeros
        "'0', 1, '0'",                // Single zero exact
        "'ABC123DEF', 5, '00123'",    // Mixed - extract numbers only
        "'12.34', 4, '1234'",         // Decimal - strip non-numeric
        "'000123', 6, '000123'",      // Leading zeros preserved
        "'999999999', 9, '999999999'", // COCOM01Y.cpy pattern - 9 digits
        "'11111111111', 11, '11111111111'" // COCOM01Y.cpy pattern - 11 digits
    })
    @DisplayName("PIC 9 Numeric Field Formatting")
    void testFormatNumericField(String input, int length, String expected) {
        // Test the formatNumericField method
        String result = CobolStringFormatter.formatNumericField(input, length);
        
        assertThat(result)
            .describedAs("PIC 9(%d) formatting of '%s'", length, input)
            .isEqualTo(expected)
            .hasSize(length)
            .matches("\\d{" + length + "}", "Result must contain only digits");
            
        logTestExecution("PIC 9 formatting test passed for input: " + input, null);
    }

    /**
     * Tests COBOL JUSTIFIED RIGHT clause formatting behavior.
     * Validates right-alignment with left space padding to maintain exact
     * COBOL JUSTIFIED RIGHT semantics for display and processing.
     *
     * @param input the input string to justify
     * @param length the target field length
     * @param expected the expected right-justified result
     */
    @ParameterizedTest
    @CsvSource({
        "'Hello', 10, '     Hello'",  // Right justify with left padding
        "'Test', 4, 'Test'",          // Exact length - no change
        "'Testing123', 5, 'ing123'",  // Long string - truncate from left
        "'', 3, '   '",               // Empty string - all spaces
        "'A', 5, '    A'",            // Single character right justify
        "'123', 6, '   123'",         // Numeric right justify
        "'Data', 8, '    Data'",      // Standard right alignment
        "'Right', 10, '     Right'",  // Common justification case
        "'X', 1, 'X'",                // Boundary case - single char
        "'TooLongForField', 8, 'ForField'" // Truncate keeping rightmost
    })
    @DisplayName("COBOL JUSTIFIED RIGHT Clause Handling")
    void testHandleJustifiedRight(String input, int length, String expected) {
        // Test the handleJustifiedRight method
        String result = CobolStringFormatter.handleJustifiedRight(input, length);
        
        assertThat(result)
            .describedAs("JUSTIFIED RIGHT formatting of '%s' to length %d", input, length)
            .isEqualTo(expected)
            .hasSize(length);
            
        // Validate right alignment behavior
        if (input != null && !input.isEmpty() && input.length() <= length) {
            assertThat(result)
                .describedAs("Result must end with original content when right-justified")
                .endsWith(input.length() <= length ? input : input.substring(input.length() - length));
        }
        
        logTestExecution("JUSTIFIED RIGHT test passed for input: " + input, null);
    }

    /**
     * Tests fixed-length field formatting with configurable padding characters.
     * Validates maintenance of COBOL record layout field boundaries with
     * custom padding characters for different field types.
     *
     * @param input the input string value
     * @param length the fixed field length
     * @param padChar the padding character to use
     * @param expected the expected formatted result
     */
    @ParameterizedTest
    @CsvSource({
        "'Hello', 8, ' ', 'Hello   '",     // Space padding (default)
        "'123', 5, '0', '12300'",          // Zero padding for numeric
        "'Test', 6, '*', 'Test**'",        // Custom character padding
        "'', 4, '-', '----'",              // Empty with custom padding
        "'LongText', 4, 'X', 'Long'",      // Truncation with custom pad
        "'AB', 2, ' ', 'AB'",              // Exact length - no padding needed
        "'Data', 10, '_', 'Data______'",   // Underscore padding
        "'Short', 12, '.', 'Short.......'", // Period padding
        "'Field', 3, '#', 'Fie'",          // Truncate longer string
        "'A', 7, '0', 'A000000'"           // Single char with zero pad
    })
    @DisplayName("Fixed-Length Field Boundary Maintenance")
    void testFormatFixedLength(String input, int length, char padChar, String expected) {
        // Test the formatFixedLength method
        String result = CobolStringFormatter.formatFixedLength(input, length, padChar);
        
        assertThat(result)
            .describedAs("Fixed-length formatting of '%s' to length %d with pad '%c'", input, length, padChar)
            .isEqualTo(expected)
            .hasSize(length);
            
        // Validate padding character usage
        if (input != null && input.length() < length) {
            long padCount = result.chars().filter(ch -> ch == padChar).count();
            long expectedPadCount = length - Math.min(input.length(), length);
            assertThat(padCount)
                .describedAs("Padding character count must match expected")
                .isGreaterThanOrEqualTo(expectedPadCount);
        }
        
        logTestExecution("Fixed-length formatting test passed", null);
    }

    /**
     * Tests FILLER field placeholder generation and processing.
     * Validates creation of FILLER content with specified characters
     * for COBOL record layout placeholder fields.
     *
     * @param fieldLength the length of the FILLER field
     * @param fillChar the character to use for filling
     * @param expected the expected FILLER content
     */
    @ParameterizedTest
    @CsvSource({
        "5, ' ', '     '",            // Standard space FILLER
        "3, '0', '000'",              // Zero FILLER
        "10, '*', '**********'",      // Asterisk FILLER
        "1, 'X', 'X'",                // Single character FILLER
        "0, ' ', ''",                 // Zero-length FILLER
        "8, '-', '--------'",         // Dash FILLER
        "15, '.', '...............'", // Period FILLER for alignment
        "20, '_', '____________________'", // Underscore FILLER
        "4, '#', '####'",             // Hash FILLER
        "12, '=', '============'"     // Equals FILLER
    })
    @DisplayName("FILLER Field Placeholder Processing")
    void testHandleFillerFields(int fieldLength, char fillChar, String expected) {
        // Test the handleFillerFields method
        String result = CobolStringFormatter.handleFillerFields(fieldLength, fillChar);
        
        assertThat(result)
            .describedAs("FILLER field of length %d with character '%c'", fieldLength, fillChar)
            .isEqualTo(expected)
            .hasSize(fieldLength);
            
        // Validate all characters are the specified fill character
        if (fieldLength > 0) {
            assertThat(result.chars().allMatch(ch -> ch == fillChar))
                .describedAs("All characters in FILLER must be the fill character")
                .isTrue();
        }
        
        logTestExecution("FILLER field test passed for length: " + fieldLength, null);
    }

    /**
     * Tests general string padding functionality with various characters.
     * Validates the padString method which provides right-padding capabilities
     * for COBOL string formatting operations.
     *
     * @param input the string to pad
     * @param length the target length
     * @param padChar the padding character
     * @param expected the expected padded result
     */
    @ParameterizedTest
    @CsvSource({
        "'Test', 8, ' ', 'Test    '",      // Standard space padding
        "'123', 6, '0', '123000'",         // Zero padding for numbers
        "'ABC', 5, '*', 'ABC**'",          // Custom character padding
        "'', 4, '-', '----'",              // Empty string padding
        "'Full', 4, ' ', 'Full'",          // Exact length - no padding
        "'TooLong', 3, 'X', 'Too'",        // Truncation when too long
        "'Pad', 10, '_', 'Pad_______'",    // Underscore padding
        "'Short', 12, '.', 'Short.......'", // Period padding
        "'X', 7, '0', 'X000000'",          // Single char with zeros
        "'Data', 6, '#', 'Data##'"         // Hash padding
    })
    @DisplayName("String Padding Logic Validation")
    void testPadString(String input, int length, char padChar, String expected) {
        // Test the padString method
        String result = CobolStringFormatter.padString(input, length, padChar);
        
        assertThat(result)
            .describedAs("Padding '%s' to length %d with '%c'", input, length, padChar)
            .isEqualTo(expected)
            .hasSize(expected.length()); // May be truncated
            
        logTestExecution("String padding test passed", null);
    }

    /**
     * Tests string truncation functionality with length validation.
     * Validates the truncateString method maintains proper truncation behavior
     * for COBOL field overflow handling and string length control.
     *
     * @param input the string to truncate
     * @param maxLength the maximum allowed length
     * @param expected the expected truncated result
     */
    @ParameterizedTest
    @CsvSource({
        "'Hello World', 5, 'Hello'",       // Standard truncation
        "'Test', 4, 'Test'",               // Exact length - no truncation
        "'Short', 10, 'Short'",            // Shorter than max - no truncation
        "'', 3, ''",                       // Empty string - no change
        "'A', 1, 'A'",                     // Single character exact
        "'AB', 1, 'A'",                    // Single character truncate
        "'Very Long String', 8, 'Very Lon'", // Long string truncation
        "'1234567890', 7, '1234567'",      // Numeric truncation
        "'Special@#$', 6, 'Specia'",       // Special characters
        "'Trailing Spaces   ', 10, 'Trailing '"  // Truncate with spaces
    })
    @DisplayName("String Truncation Logic Validation")
    void testTruncateString(String input, int maxLength, String expected) {
        // Test the truncateString method
        String result = CobolStringFormatter.truncateString(input, maxLength);
        
        assertThat(result)
            .describedAs("Truncating '%s' to max length %d", input, maxLength)
            .isEqualTo(expected);
            
        assertThat(result.length())
            .describedAs("Truncated string length must not exceed maximum")
            .isLessThanOrEqualTo(maxLength);
            
        logTestExecution("String truncation test passed", null);
    }

    /**
     * Tests edited numeric field formatting with decimal points and commas.
     * Validates COBOL edit pattern processing for display formatting including
     * zero suppression, decimal placement, and thousands separators.
     *
     * @param input the numeric value to format
     * @param editPattern the COBOL edit pattern
     * @param expected the expected formatted result
     */
    @ParameterizedTest
    @CsvSource({
        "'123.45', 'ZZZ.99', '123.45'",       // Basic decimal formatting
        "'1000', 'Z,ZZZ', '1,000'",           // Thousands separator
        "'0.50', 'Z.99', '0.50'",             // Zero suppression with decimal
        "'', 'ZZZ.99', ''",                   // Empty input
        "'12345', 'ZZ,ZZZ', '12,345'",        // Standard thousands format
        "'100.00', 'ZZZ.99', '100.00'",       // Standard currency format
        "'5', 'Z9', '5'",                     // Simple zero suppression
        "'0', 'Z', ''",                       // Complete zero suppression
        "'999999', 'ZZZ,ZZZ', '999,999'",     // Full field thousands
        "'75.5', 'ZZ.99', '75.50'"            // Decimal padding
    })
    @DisplayName("Edited Numeric Field Formatting")
    void testFormatEditedNumeric(String input, String editPattern, String expected) {
        // Test the formatEditedNumeric method
        String result = CobolStringFormatter.formatEditedNumeric(input, editPattern);
        
        assertThat(result)
            .describedAs("Edited numeric formatting of '%s' with pattern '%s'", input, editPattern)
            .isEqualTo(expected);
            
        // Validate pattern application
        if (editPattern.contains(",") && !result.isEmpty() && result.length() > 3) {
            // Check for comma formatting when pattern includes commas
            assertThat(result)
                .describedAs("Result should contain comma separators when pattern specifies")
                .matches(".*\\d,\\d.*");
        }
        
        logTestExecution("Edited numeric formatting test passed", null);
    }

    /**
     * Tests trailing space preservation for COBOL display compatibility.
     * Validates that trailing spaces are maintained exactly as specified
     * in COBOL field definitions for proper display formatting.
     *
     * @param input the input string with potential trailing spaces
     * @param fieldLength the original COBOL field length
     * @param expected the expected result with preserved spaces
     */
    @ParameterizedTest
    @CsvSource({
        "'Hello   ', 10, 'Hello     '",     // Preserve and extend trailing spaces
        "'Test', 8, 'Test    '",            // Add trailing spaces to reach length
        "'Data     ', 12, 'Data        '",  // Extend existing trailing spaces
        "'', 5, '     '",                   // Empty string - all spaces
        "'Full      ', 6, 'Full  '",        // Truncate but preserve pattern
        "'Short', 10, 'Short    '",         // Standard space padding
        "'X', 3, 'X  '",                    // Single character with spaces
        "'NoSpaces', 10, 'NoSpaces  '",     // Add spaces when none exist
        "'Exact', 5, 'Exact'",              // Exact length - no change
        "'TooLong   ', 5, 'TooLo'"          // Truncate preserving intent
    })
    @DisplayName("Trailing Space Preservation for COBOL Compatibility")
    void testPreserveTrailingSpaces(String input, int fieldLength, String expected) {
        // Test the preserveTrailingSpaces method
        String result = CobolStringFormatter.preserveTrailingSpaces(input, fieldLength);
        
        assertThat(result)
            .describedAs("Preserving trailing spaces for '%s' with field length %d", input, fieldLength)
            .isEqualTo(expected)
            .hasSize(fieldLength);
            
        // Validate space preservation
        if (expected.endsWith(" ")) {
            assertThat(result)
                .describedAs("Result must preserve trailing space pattern")
                .endsWith(" ");
        }
        
        logTestExecution("Trailing space preservation test passed", null);
    }

    /**
     * Tests conversion from COBOL string format to Java string format.
     * Validates removal of trailing spaces and COBOL-specific formatting
     * while preserving essential string content for Java processing.
     *
     * @param input the COBOL-formatted string
     * @param expected the expected Java string result
     */
    @ParameterizedTest
    @CsvSource({
        "'Hello     ', 'Hello'",           // Remove trailing spaces
        "'Test', 'Test'",                  // No change needed
        "'   Leading', '   Leading'",      // Preserve leading spaces
        "'Data    ', 'Data'",              // Standard trailing space removal
        "'', ''",                          // Empty string unchanged
        "'   ', ''",                       // All spaces become empty
        "'Mixed   Data   ', 'Mixed   Data'", // Remove only trailing spaces
        "'SingleSpace ', 'SingleSpace'",   // Single trailing space removal
        "'Multiple     ', 'Multiple'",     // Multiple trailing spaces
        "'Numbers123   ', 'Numbers123'"    // Numeric with trailing spaces
    })
    @DisplayName("COBOL to Java String Conversion")
    void testConvertToJavaString(String input, String expected) {
        // Test the convertToJavaString method
        String result = CobolStringFormatter.convertToJavaString(input);
        
        assertThat(result)
            .describedAs("Converting COBOL string '%s' to Java format", input)
            .isEqualTo(expected);
            
        // Validate no trailing spaces in result
        if (!expected.isEmpty()) {
            assertThat(result)
                .describedAs("Java string should not have trailing spaces")
                .doesNotEndWith(" ");
        }
        
        logTestExecution("COBOL to Java conversion test passed", null);
    }

    /**
     * Tests conversion from Java string format to COBOL string format.
     * Validates proper padding to fixed field length with space characters
     * to maintain COBOL record layout requirements.
     *
     * @param input the Java string to convert
     * @param length the target COBOL field length
     * @param expected the expected COBOL-formatted result
     */
    @ParameterizedTest
    @CsvSource({
        "'Hello', 10, 'Hello     '",       // Pad to COBOL field length
        "'Test', 4, 'Test'",               // Exact length match
        "'VeryLongString', 8, 'VeryLong'", // Truncate to field length
        "'', 5, '     '",                  // Empty becomes spaces
        "'Data', 8, 'Data    '",           // Standard padding
        "'X', 1, 'X'",                     // Single character exact
        "'Short', 12, 'Short       '",     // Extended padding
        "'Numbers123', 10, 'Numbers12'",   // Numeric truncation
        "'Spaces   ', 8, 'Spaces  '",      // Preserve some trailing spaces
        "'Field', 6, 'Field '"             // Standard field conversion
    })
    @DisplayName("Java to COBOL String Conversion")
    void testConvertToCobolString(String input, int length, String expected) {
        // Test the convertToCobolString method
        String result = CobolStringFormatter.convertToCobolString(input, length);
        
        assertThat(result)
            .describedAs("Converting Java string '%s' to COBOL format length %d", input, length)
            .isEqualTo(expected)
            .hasSize(length);
            
        // Validate COBOL format requirements
        if (input != null && input.length() < length) {
            assertThat(result)
                .describedAs("COBOL string must be padded with spaces")
                .endsWith(" ");
        }
        
        logTestExecution("Java to COBOL conversion test passed", null);
    }

    /**
     * Tests PIC clause parsing and component extraction functionality.
     * Validates correct interpretation of COBOL PIC clauses to extract
     * field type, length, and formatting information.
     */
    @ParameterizedTest
    @CsvSource({
        "'PIC X(10)', 'alphanumeric', 10",     // Standard alphanumeric
        "'PIC 9(5)', 'numeric', 5",            // Standard numeric
        "'PIC S9(8)', 'signed_numeric', 8",    // Signed numeric
        "'PIC X(04)', 'alphanumeric', 4",      // COCOM01Y.cpy pattern
        "'PIC X(08)', 'alphanumeric', 8",      // COCOM01Y.cpy pattern
        "'PIC X(25)', 'alphanumeric', 25",     // COCOM01Y.cpy pattern
        "'PIC 9(09)', 'numeric', 9",           // COCOM01Y.cpy pattern
        "'PIC 9(11)', 'numeric', 11",          // COCOM01Y.cpy pattern
        "'PIC 9(16)', 'numeric', 16",          // COCOM01Y.cpy pattern
        "'PIC XXX', 'alphanumeric', 3",        // Repeated characters
        "'PIC 999', 'numeric', 3",             // Repeated digits
        "'PIC S999', 'signed_numeric', 3",     // Repeated signed digits
        "'', 'unknown', 0",                    // Empty clause
        "'INVALID', 'unknown', 0"              // Invalid clause
    })
    @DisplayName("PIC Clause Parsing and Information Extraction")
    void testParsePicClause(String picClause, String expectedType, int expectedLength) {
        // Test the parsePicClause method
        Map<String, Object> result = CobolStringFormatter.parsePicClause(picClause);
        
        assertThat(result)
            .describedAs("Parsing PIC clause '%s'", picClause)
            .isNotNull()
            .containsKey("type")
            .containsKey("length");
            
        assertThat(result.get("type"))
            .describedAs("PIC clause type extraction")
            .isEqualTo(expectedType);
            
        assertThat(result.get("length"))
            .describedAs("PIC clause length extraction")
            .isEqualTo(expectedLength);
            
        logTestExecution("PIC clause parsing test passed for: " + picClause, null);
    }

    /**
     * Tests null value handling across all formatting methods.
     * Validates that null inputs are handled gracefully and consistently
     * across all COBOL string formatting operations.
     */
    @ParameterizedTest
    @CsvSource({
        "5, '     '",     // formatAlphanumericField with null
        "3, '000'",       // formatNumericField with null  
        "4, '    '",      // handleJustifiedRight with null
        "6, '      '"     // preserveTrailingSpaces with null
    })
    @DisplayName("Null Value Handling Validation")
    void testNullValueHandling(int length, String expected) {
        // Test null handling in formatAlphanumericField
        String result1 = CobolStringFormatter.formatAlphanumericField(null, length);
        assertThat(result1)
            .describedAs("formatAlphanumericField should handle null input")
            .hasSize(length);
            
        // Test null handling in handleJustifiedRight
        String result2 = CobolStringFormatter.handleJustifiedRight(null, length);
        assertThat(result2)
            .describedAs("handleJustifiedRight should handle null input")
            .hasSize(length);
            
        // Test null handling in preserveTrailingSpaces
        String result3 = CobolStringFormatter.preserveTrailingSpaces(null, length);
        assertThat(result3)
            .describedAs("preserveTrailingSpaces should handle null input")
            .hasSize(length);
            
        // Test null handling in convertToJavaString
        String result4 = CobolStringFormatter.convertToJavaString(null);
        assertThat(result4)
            .describedAs("convertToJavaString should handle null input")
            .isEqualTo("");
            
        logTestExecution("Null value handling validation passed", null);
    }

    /**
     * Tests error conditions and invalid parameter handling.
     * Validates that appropriate exceptions are thrown for invalid inputs
     * including negative lengths and excessive field sizes.
     */
    @DisplayName("Error Condition and Exception Handling")
    void testErrorConditions() {
        // Test negative length handling
        assertThatThrownBy(() -> CobolStringFormatter.formatAlphanumericField("test", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid field length: -1");
            
        assertThatThrownBy(() -> CobolStringFormatter.formatNumericField("123", -5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid field length: -5");
            
        assertThatThrownBy(() -> CobolStringFormatter.handleJustifiedRight("test", -10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid field length: -10");
            
        // Test excessive length handling
        int excessiveLength = 40000;
        assertThatThrownBy(() -> CobolStringFormatter.formatFixedLength("test", excessiveLength, ' '))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid field length: " + excessiveLength);
            
        assertThatThrownBy(() -> CobolStringFormatter.handleFillerFields(excessiveLength, '*'))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid FILLER field length: " + excessiveLength);
            
        // Test truncateString negative length
        assertThatThrownBy(() -> CobolStringFormatter.truncateString("test", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Length cannot be negative: -1");
            
        logTestExecution("Error condition validation passed", null);
    }

    /**
     * Tests COBOL field patterns from copybook analysis.
     * Validates formatting for actual field patterns found in COBOL copybooks
     * to ensure exact compatibility with existing data structures.
     */
    @DisplayName("COBOL Copybook Field Pattern Validation")
    void testCobolCopybookPatterns() {
        // Test patterns from COCOM01Y.cpy
        
        // Transaction ID pattern (PIC X(04))
        String tranId = CobolStringFormatter.formatAlphanumericField("CC00", 4);
        assertThat(tranId)
            .describedAs("COBOL transaction ID formatting")
            .isEqualTo("CC00")
            .hasSize(4);
            
        // Program name pattern (PIC X(08))
        String programName = CobolStringFormatter.formatAlphanumericField("COSGN00C", 8);
        assertThat(programName)
            .describedAs("COBOL program name formatting")
            .isEqualTo("COSGN00C")
            .hasSize(8);
            
        // User ID pattern (PIC X(08))
        String userId = CobolStringFormatter.formatAlphanumericField(TestConstants.TEST_USER_ID, 8);
        assertThat(userId)
            .describedAs("COBOL user ID formatting")
            .isEqualTo("TESTUSER")
            .hasSize(8);
            
        // Customer ID pattern (PIC 9(09))
        String custId = CobolStringFormatter.formatNumericField("123456789", 9);
        assertThat(custId)
            .describedAs("COBOL customer ID formatting")
            .isEqualTo("123456789")
            .hasSize(9)
            .matches("\\d{9}");
            
        // Account ID pattern (PIC 9(11))
        String acctId = CobolStringFormatter.formatNumericField("12345", 11);
        assertThat(acctId)
            .describedAs("COBOL account ID formatting")
            .isEqualTo("00000012345")
            .hasSize(11)
            .matches("\\d{11}");
            
        // Card number pattern (PIC 9(16))
        String cardNum = CobolStringFormatter.formatNumericField("1234567890123456", 16);
        assertThat(cardNum)
            .describedAs("COBOL card number formatting")
            .isEqualTo("1234567890123456")
            .hasSize(16)
            .matches("\\d{16}");
            
        // Customer name patterns (PIC X(25))
        String firstName = CobolStringFormatter.formatAlphanumericField("John", 25);
        assertThat(firstName)
            .describedAs("COBOL customer first name formatting")
            .isEqualTo("John                     ")
            .hasSize(25);
            
        logTestExecution("COBOL copybook pattern validation passed", null);
    }

    /**
     * Tests title and message formatting from COBOL copybooks.
     * Validates formatting for screen titles and messages found in
     * COTTL01Y.cpy and CSMSG01Y.cpy copybooks.
     */
    @DisplayName("COBOL Title and Message Formatting")
    void testCobolTitleAndMessageFormatting() {
        // Test patterns from COTTL01Y.cpy (PIC X(40) values)
        String title1 = CobolStringFormatter.formatAlphanumericField(
            "      AWS Mainframe Modernization       ", 40);
        assertThat(title1)
            .describedAs("COBOL title formatting from COTTL01Y")
            .hasSize(40)
            .isEqualTo("      AWS Mainframe Modernization       ");
            
        String title2 = CobolStringFormatter.formatAlphanumericField(
            "              CardDemo                  ", 40);
        assertThat(title2)
            .describedAs("COBOL CardDemo title formatting")
            .hasSize(40)
            .isEqualTo("              CardDemo                  ");
            
        // Test patterns from CSMSG01Y.cpy (PIC X(50) values)
        String thankYouMsg = CobolStringFormatter.formatAlphanumericField(
            "Thank you for using CardDemo application...      ", 50);
        assertThat(thankYouMsg)
            .describedAs("COBOL thank you message formatting")
            .hasSize(50)
            .isEqualTo("Thank you for using CardDemo application...      ");
            
        String invalidKeyMsg = CobolStringFormatter.formatAlphanumericField(
            "Invalid key pressed. Please see below...         ", 50);
        assertThat(invalidKeyMsg)
            .describedAs("COBOL invalid key message formatting")
            .hasSize(50)
            .isEqualTo("Invalid key pressed. Please see below...         ");
            
        // Test trailing space preservation for display messages
        String preservedMsg = CobolStringFormatter.preserveTrailingSpaces(
            "Thank you for using CardDemo application...", 50);
        assertThat(preservedMsg)
            .describedAs("Message with preserved trailing spaces")
            .hasSize(50)
            .endsWith(" ");
            
        logTestExecution("COBOL title and message formatting validation passed", null);
    }

    /**
     * Tests boundary conditions and maximum field length handling.
     * Validates behavior at field length limits and edge cases to ensure
     * robust handling of maximum COBOL field sizes and unusual inputs.
     */
    @DisplayName("Boundary Conditions and Edge Cases")
    void testBoundaryConditions() {
        // Test zero-length fields
        assertThatCode(() -> CobolStringFormatter.formatAlphanumericField("test", 0))
            .describedAs("Zero-length alphanumeric field should not throw")
            .doesNotThrowAnyException();
            
        String zeroLengthResult = CobolStringFormatter.formatAlphanumericField("test", 0);
        assertThat(zeroLengthResult)
            .describedAs("Zero-length field should return empty string")
            .isEmpty();
            
        // Test maximum reasonable field length (within bounds)
        int maxReasonableLength = 1000;
        String maxFieldResult = CobolStringFormatter.formatAlphanumericField("test", maxReasonableLength);
        assertThat(maxFieldResult)
            .describedAs("Maximum field length formatting")
            .hasSize(maxReasonableLength)
            .startsWith("test")
            .endsWith(" ");
            
        // Test single character operations
        String singleChar = CobolStringFormatter.formatAlphanumericField("X", 1);
        assertThat(singleChar)
            .describedAs("Single character exact match")
            .isEqualTo("X")
            .hasSize(1);
            
        // Test empty field with zero length
        String emptyField = CobolStringFormatter.handleFillerFields(0, ' ');
        assertThat(emptyField)
            .describedAs("Zero-length FILLER field")
            .isEmpty()
            .hasSize(0);
            
        // Test FILLER with single character
        String singleFiller = CobolStringFormatter.handleFillerFields(1, '*');
        assertThat(singleFiller)
            .describedAs("Single character FILLER")
            .isEqualTo("*")
            .hasSize(1);
            
        logTestExecution("Boundary condition validation passed", null);
    }

    /**
     * Tests complex integration scenarios combining multiple formatting operations.
     * Validates end-to-end COBOL string processing workflows that combine
     * multiple formatting operations in realistic usage patterns.
     */
    @DisplayName("Complex Integration Scenarios")
    void testComplexIntegrationScenarios() {
        // Scenario 1: Complete COBOL record formatting simulation
        // Simulate formatting a complete COBOL record with multiple fields
        
        // Format transaction ID (PIC X(04))
        String tranId = CobolStringFormatter.formatAlphanumericField("CT00", 4);
        
        // Format program name (PIC X(08))
        String progName = CobolStringFormatter.formatAlphanumericField("COTRN00C", 8);
        
        // Format user ID (PIC X(08))
        String userId = CobolStringFormatter.formatAlphanumericField("TESTUSER", 8);
        
        // Format customer ID (PIC 9(09))
        String custId = CobolStringFormatter.formatNumericField("123456789", 9);
        
        // Format account ID (PIC 9(11))
        String acctId = CobolStringFormatter.formatNumericField("12345678901", 11);
        
        // Format customer name fields (PIC X(25) each)
        String firstName = CobolStringFormatter.formatAlphanumericField("John", 25);
        String middleName = CobolStringFormatter.formatAlphanumericField("A", 25);
        String lastName = CobolStringFormatter.formatAlphanumericField("Doe", 25);
        
        // Validate complete record formatting
        assertThat(tranId + progName + userId + custId + acctId + firstName + middleName + lastName)
            .describedAs("Complete COBOL record formatting")
            .hasSize(4 + 8 + 8 + 9 + 11 + 25 + 25 + 25); // Total: 115 characters
            
        // Scenario 2: Round-trip conversion validation
        String originalCobol = "Test Data           ";  // 20 characters with trailing spaces
        String javaString = CobolStringFormatter.convertToJavaString(originalCobol);
        String backToCobol = CobolStringFormatter.convertToCobolString(javaString, 20);
        
        assertThat(backToCobol)
            .describedAs("Round-trip conversion should preserve format")
            .hasSize(20)
            .startsWith("Test Data");
            
        // Scenario 3: Complex PIC clause processing
        Map<String, Object> picX10 = CobolStringFormatter.parsePicClause("PIC X(10)");
        String formattedX10 = CobolStringFormatter.formatAlphanumericField("Hello", 
            (Integer) picX10.get("length"));
        assertThat(formattedX10)
            .describedAs("PIC clause-driven formatting")
            .hasSize(10)
            .isEqualTo("Hello     ");
            
        logTestExecution("Complex integration scenario validation passed", null);
    }

    /**
     * Tests performance characteristics of string formatting operations.
     * Validates that formatting operations complete within acceptable time
     * thresholds for high-volume transaction processing requirements.
     */
    @DisplayName("Performance Validation for High-Volume Processing")
    void testPerformanceCharacteristics() {
        long startTime = System.currentTimeMillis();
        
        // Simulate high-volume formatting operations
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            // Test various formatting operations
            CobolStringFormatter.formatAlphanumericField("TestData" + i, 20);
            CobolStringFormatter.formatNumericField(String.valueOf(i), 10);
            CobolStringFormatter.handleJustifiedRight("Value" + i, 15);
            CobolStringFormatter.formatFixedLength("Field" + i, 12, ' ');
            CobolStringFormatter.handleFillerFields(5, '*');
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Validate performance meets requirements
        long maxAcceptableTime = 100L; // 100ms for 1000 operations
        assertThat(executionTime)
            .describedAs("High-volume formatting should complete within acceptable time")
            .isLessThan(maxAcceptableTime);
            
        logTestExecution("Performance validation completed: " + iterations + " operations", executionTime);
    }

    /**
     * Tests COBOL-specific validation rules and functional parity requirements.
     * Validates that formatting results match COBOL behavior patterns
     * according to functional parity rules from TestConstants.
     */
    @DisplayName("COBOL Functional Parity Validation")
    void testCobolFunctionalParity() {
        // Validate functional parity rules from TestConstants
        Map<String, Object> parityRules = TestConstants.FUNCTIONAL_PARITY_RULES;
        
        // Test field length validation requirement
        Boolean validateFieldLengths = (Boolean) parityRules.get("validate_field_lengths");
        if (validateFieldLengths) {
            String result = CobolStringFormatter.formatAlphanumericField("Test", 10);
            assertThat(result)
                .describedAs("Field length validation must be enforced")
                .hasSize(10);
        }
        
        // Test COBOL string patterns with validation thresholds
        Map<String, Object> thresholds = TestConstants.VALIDATION_THRESHOLDS;
        Integer stringLengthVariance = (Integer) thresholds.get("string_length_variance");
        
        // Validate zero variance requirement (exact length matching)
        String formattedField = CobolStringFormatter.formatFixedLength("Data", 8, ' ');
        assertThat(Math.abs(formattedField.length() - 8))
            .describedAs("String length variance must be within threshold")
            .isEqualTo(stringLengthVariance); // Should be 0
            
        // Test COBOL COMP-3 pattern compatibility
        Map<String, Object> comp3Patterns = TestConstants.COBOL_COMP3_PATTERNS;
        String positivePattern = (String) comp3Patterns.get("positive_pattern");
        String zeroPattern = (String) comp3Patterns.get("zero_pattern");
        
        // Format monetary values maintaining COBOL precision
        String formattedPositive = CobolStringFormatter.formatEditedNumeric(positivePattern, "ZZZ.99");
        String formattedZero = CobolStringFormatter.formatEditedNumeric(zeroPattern, "ZZZ.99");
        
        assertThat(formattedPositive)
            .describedAs("Positive monetary pattern formatting")
            .contains("123.45");
            
        assertThat(formattedZero)
            .describedAs("Zero monetary pattern formatting")
            .contains("0.00");
            
        logTestExecution("COBOL functional parity validation passed", null);
    }

    /**
     * Tests regex pattern validation for formatted string results.
     * Validates that formatted strings match expected patterns for
     * COBOL field validation and compatibility checking.
     */
    @DisplayName("Regex Pattern Validation for Formatted Strings")
    void testRegexPatternValidation() {
        // Test numeric field patterns
        String numericField = CobolStringFormatter.formatNumericField("12345", 8);
        Pattern numericPattern = Pattern.compile("\\d{8}");
        assertThat(numericPattern.matcher(numericField).matches())
            .describedAs("Numeric field must match digits-only pattern")
            .isTrue();
            
        // Test alphanumeric field patterns
        String alphaField = CobolStringFormatter.formatAlphanumericField("Test123", 10);
        Pattern alphaPattern = Pattern.compile(".{10}"); // Any 10 characters
        assertThat(alphaPattern.matcher(alphaField).matches())
            .describedAs("Alphanumeric field must match length pattern")
            .isTrue();
            
        // Test justified right patterns
        String justifiedField = CobolStringFormatter.handleJustifiedRight("ABC", 6);
        assertThat(justifiedField)
            .describedAs("Justified right must end with original content")
            .endsWith("ABC")
            .matches("^\\s*ABC$"); // Starts with optional spaces, ends with ABC
            
        // Test FILLER field patterns
        String fillerField = CobolStringFormatter.handleFillerFields(5, '*');
        Pattern fillerPattern = Pattern.compile("\\*{5}");
        assertThat(fillerPattern.matcher(fillerField).matches())
            .describedAs("FILLER field must match repetition pattern")
            .isTrue();
            
        logTestExecution("Regex pattern validation passed", null);
    }

    /**
     * Tests stream processing integration for bulk string formatting operations.
     * Validates that CobolStringFormatter methods work correctly with Java streams
     * for processing collections of data efficiently.
     */
    @DisplayName("Stream Processing Integration Validation")
    void testStreamProcessingIntegration() {
        // Test stream processing of multiple values
        java.util.List<String> testInputs = java.util.List.of(
            "Value1", "Value2", "Value3", "Value4", "Value5"
        );
        
        // Process collection using streams with formatAlphanumericField
        java.util.List<String> formattedResults = testInputs.stream()
            .map(value -> CobolStringFormatter.formatAlphanumericField(value, 10))
            .collect(java.util.stream.Collectors.toList());
            
        assertThat(formattedResults)
            .describedAs("Stream processing should format all values")
            .hasSize(testInputs.size())
            .allSatisfy(result -> {
                assertThat(result).hasSize(10);
            });
            
        // Test stream processing with justified right formatting
        java.util.List<String> justifiedResults = testInputs.stream()
            .map(value -> CobolStringFormatter.handleJustifiedRight(value, 12))
            .collect(java.util.stream.Collectors.toList());
            
        assertThat(justifiedResults)
            .describedAs("Stream justified right processing")
            .hasSize(testInputs.size())
            .allSatisfy(result -> {
                assertThat(result).hasSize(12);
            });
            
        // Test parallel stream processing
        long parallelStart = System.currentTimeMillis();
        java.util.List<String> parallelResults = testInputs.parallelStream()
            .map(value -> CobolStringFormatter.formatNumericField(value.replaceAll("[^0-9]", ""), 8))
            .collect(java.util.stream.Collectors.toList());
        long parallelTime = System.currentTimeMillis() - parallelStart;
        
        assertThat(parallelResults)
            .describedAs("Parallel stream processing should work correctly")
            .hasSize(testInputs.size());
            
        logTestExecution("Stream processing integration validation passed", parallelTime);
    }

    /**
     * Tests comprehensive COBOL format validation scenarios.
     * Validates complete COBOL formatting workflows including multiple
     * operations, error recovery, and format consistency checking.
     */
    @DisplayName("Comprehensive COBOL Format Validation")
    void testComprehensiveCobolFormatValidation() {
        // Test complete record layout formatting
        StringBuilder recordLayout = new StringBuilder();
        
        // Build a complete COBOL record using various field types
        recordLayout.append(CobolStringFormatter.formatAlphanumericField("HEADER", 8));    // Record header
        recordLayout.append(CobolStringFormatter.formatNumericField("12345", 9));          // Customer ID
        recordLayout.append(CobolStringFormatter.formatAlphanumericField("John", 25));     // First name
        recordLayout.append(CobolStringFormatter.formatAlphanumericField("Doe", 25));      // Last name
        recordLayout.append(CobolStringFormatter.formatNumericField("123456789012345", 16)); // Card number
        recordLayout.append(CobolStringFormatter.handleFillerFields(10, ' '));             // FILLER space
        recordLayout.append(CobolStringFormatter.handleJustifiedRight("END", 5));          // Right-justified end marker
        
        String completeRecord = recordLayout.toString();
        int expectedLength = 8 + 9 + 25 + 25 + 16 + 10 + 5; // 98 characters
        
        assertThat(completeRecord)
            .describedAs("Complete COBOL record formatting")
            .hasSize(expectedLength)
            .startsWith("HEADER  ")
            .contains("000012345")  // Padded customer ID
            .contains("John                     ") // Padded first name
            .endsWith("  END");     // Right-justified end marker
            
        // Test format consistency across multiple operations
        String value1 = CobolStringFormatter.formatAlphanumericField("Test", 8);
        String value2 = CobolStringFormatter.formatAlphanumericField("Test", 8);
        assertThat(value1)
            .describedAs("Formatting consistency across calls")
            .isEqualTo(value2);
            
        // Test bidirectional conversion consistency
        String originalValue = "Sample Data     ";
        String javaConverted = CobolStringFormatter.convertToJavaString(originalValue);
        String cobolConverted = CobolStringFormatter.convertToCobolString(javaConverted, originalValue.length());
        
        assertThat(cobolConverted)
            .describedAs("Bidirectional conversion should preserve essential format")
            .hasSize(originalValue.length())
            .startsWith("Sample Data");
            
        // Test PIC clause-driven formatting workflow
        Map<String, Object> picInfo = CobolStringFormatter.parsePicClause("PIC X(15)");
        String picFormatted = CobolStringFormatter.formatAlphanumericField("Dynamic", 
            (Integer) picInfo.get("length"));
        assertThat(picFormatted)
            .describedAs("PIC clause-driven formatting workflow")
            .hasSize(15)
            .isEqualTo("Dynamic        ");
            
        logTestExecution("Comprehensive COBOL format validation passed", null);
    }

    /**
     * Tests COBOL-specific edge cases and special character handling.
     * Validates handling of special COBOL scenarios including LOW-VALUES,
     * HIGH-VALUES equivalent processing, and special character preservation.
     */
    @DisplayName("COBOL Special Character and Edge Case Handling")
    void testCobolSpecialCharacterHandling() {
        // Test COBOL SPACES equivalent (all spaces)
        String spacesField = CobolStringFormatter.formatAlphanumericField("   ", 5);
        assertThat(spacesField)
            .describedAs("COBOL SPACES equivalent formatting")
            .isEqualTo("     ")
            .matches("^\\s+$");
            
        // Test COBOL LOW-VALUES equivalent (empty string)
        String lowValuesField = CobolStringFormatter.formatAlphanumericField("", 4);
        assertThat(lowValuesField)
            .describedAs("COBOL LOW-VALUES equivalent formatting")
            .isEqualTo("    ")
            .hasSize(4);
            
        // Test special character preservation
        String specialChars = CobolStringFormatter.formatAlphanumericField("@#$%^&*()", 12);
        assertThat(specialChars)
            .describedAs("Special character preservation")
            .startsWith("@#$%^&*()")
            .hasSize(12);
            
        // Test numeric special cases
        String zeroField = CobolStringFormatter.formatNumericField("000", 5);
        assertThat(zeroField)
            .describedAs("Zero-only numeric formatting")
            .isEqualTo("00000")
            .matches("^0+$");
            
        // Test mixed content cleaning for numeric fields
        String mixedContent = CobolStringFormatter.formatNumericField("A1B2C3", 6);
        assertThat(mixedContent)
            .describedAs("Mixed content cleaning for numeric")
            .isEqualTo("000123")
            .matches("\\d{6}");
            
        // Test justified right with special characters
        String justifiedSpecial = CobolStringFormatter.handleJustifiedRight("$$$", 8);
        assertThat(justifiedSpecial)
            .describedAs("Justified right with special characters")
            .isEqualTo("     $$$")
            .endsWith("$$$");
            
        logTestExecution("COBOL special character handling validation passed", null);
    }

    /**
     * Tests validation threshold compliance and COBOL precision requirements.
     * Validates that string formatting operations meet the validation thresholds
     * defined in TestConstants for COBOL functional parity.
     */
    @DisplayName("Validation Threshold Compliance Testing")
    void testValidationThresholdCompliance() {
        // Get validation thresholds from TestConstants
        Map<String, Object> thresholds = TestConstants.VALIDATION_THRESHOLDS;
        
        // Test string length variance threshold (should be 0)
        Integer lengthVarianceThreshold = (Integer) thresholds.get("string_length_variance");
        
        String[] testValues = {"Test", "Data", "Value", "Field", "String"};
        int targetLength = 10;
        
        for (String value : testValues) {
            String formatted = CobolStringFormatter.formatAlphanumericField(value, targetLength);
            int lengthVariance = Math.abs(formatted.length() - targetLength);
            
            assertThat(lengthVariance)
                .describedAs("String length variance for '%s' must meet threshold", value)
                .isEqualTo(lengthVarianceThreshold); // Should be exactly 0
        }
        
        // Test format strict requirements
        Boolean formatStrict = (Boolean) thresholds.get("date_format_strict");
        if (formatStrict) {
            // Validate strict format compliance
            String strictFormatted = CobolStringFormatter.formatFixedLength("STRICT", 8, ' ');
            assertThat(strictFormatted)
                .describedAs("Strict format compliance")
                .hasSize(8)
                .isEqualTo("STRICT  ");
        }
        
        // Test numeric overflow checking compliance
        Boolean numericOverflowCheck = (Boolean) thresholds.get("numeric_overflow_check");
        if (numericOverflowCheck) {
            // Test behavior with numeric overflow scenarios
            String overflowResult = CobolStringFormatter.formatNumericField("999999999999", 8);
            assertThat(overflowResult)
                .describedAs("Numeric overflow handling")
                .hasSize(8)
                .matches("\\d{8}");
        }
        
        logTestExecution("Validation threshold compliance verified", null);
    }
}
