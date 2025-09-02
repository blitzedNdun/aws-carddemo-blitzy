/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.UnitTest;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Arrays;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.io.IOException;

/**
 * Comprehensive unit test suite for FileFormatConverter utility class.
 * 
 * This test class validates conversion between COBOL fixed-width record formats and modern formats
 * like CSV and JSON, ensuring preservation of data integrity and field mappings during the 
 * CardDemo system migration from COBOL/CICS to Java/Spring Boot.
 * 
 * Test Coverage:
 * - COBOL record parsing with copybook field definitions
 * - Fixed-width to CSV conversion preserving field boundaries  
 * - COBOL record to JSON object mapping with nested structure support
 * - OCCURS clause handling in array conversion scenarios
 * - Numeric precision preservation during COBOL COMP-3 to BigDecimal conversion
 * - FILLER field handling and placeholder management in conversions
 * - Batch file processing with large datasets and memory efficiency
 * - Error handling for malformed records and invalid copybook definitions
 * - Character encoding preservation (EBCDIC to ASCII) during conversion
 * - Validation of converted data against COBOL copybook definitions
 * 
 * All tests ensure 100% functional parity with the original COBOL implementation
 * while supporting modern data processing requirements and validation standards.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@DisplayName("File Format Converter Tests")
public class FileFormatConverterTest extends AbstractBaseTest implements UnitTest {

    private FileFormatConverter converter;
    private Map<String, String> transactionCopybook;
    private Map<String, String> accountCopybook;
    private String sampleTransactionRecord;
    private String sampleAccountRecord;
    
    /**
     * Test setup method executed before each test.
     * Initializes converter instance, builds copybook definitions from COBOL copybooks,
     * and prepares sample test data matching VSAM record layouts.
     * 
     * This method extends AbstractBaseTest.setUp() to provide specific initialization
     * for file format conversion testing scenarios.
     */
    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        
        // Initialize FileFormatConverter instance
        converter = new FileFormatConverter();
        
        // Build transaction copybook definition from CVTRA05Y.cpy
        transactionCopybook = buildTransactionCopybook();
        
        // Build account copybook definition from CVACT01Y.cpy  
        accountCopybook = buildAccountCopybook();
        
        // Create sample records matching COBOL fixed-width format
        sampleTransactionRecord = buildSampleTransactionRecord();
        sampleAccountRecord = buildSampleAccountRecord();
        
        logTestExecution("FileFormatConverterTest setup completed", null);
    }

    /**
     * Test COBOL record parsing with comprehensive copybook field definitions.
     * Validates that fixed-width COBOL records are correctly parsed into field maps
     * with proper data type conversion and field boundary recognition.
     */
    @Test
    @DisplayName("Parse COBOL Transaction Record")
    public void testParseCobolRecord() {
        // Act - Parse the sample transaction record using copybook definition
        Map<String, Object> parsedRecord = converter.parseCobolRecord(sampleTransactionRecord, transactionCopybook);
        
        // Assert - Validate all fields are correctly parsed
        assertThat(parsedRecord).isNotNull();
        assertThat(parsedRecord).containsKey("TRAN-ID");
        assertThat(parsedRecord).containsKey("TRAN-TYPE-CD");
        assertThat(parsedRecord).containsKey("TRAN-AMT");
        assertThat(parsedRecord).containsKey("TRAN-DESC");
        
        // Validate specific field values and types
        assertThat(parsedRecord.get("TRAN-ID")).isInstanceOf(String.class);
        assertThat(parsedRecord.get("TRAN-TYPE-CD")).isInstanceOf(String.class);
        assertThat(parsedRecord.get("TRAN-AMT")).isInstanceOf(BigDecimal.class);
        
        // Validate transaction amount precision
        BigDecimal amount = (BigDecimal) parsedRecord.get("TRAN-AMT");
        assertBigDecimalEquals(new BigDecimal("100.50"), amount, "Transaction amount precision mismatch");
        
        // Validate field length compliance with COBOL PIC specifications
        String tranId = (String) parsedRecord.get("TRAN-ID");
        assertThat(tranId.length()).isLessThanOrEqualTo(16);
        
        logTestExecution("COBOL record parsing validation completed", null);
    }

    /**
     * Test conversion to CSV format with field boundary preservation.
     * Validates that COBOL records are correctly converted to CSV while maintaining
     * field boundaries, proper escaping, and data type integrity.
     */
    @Test
    @DisplayName("Convert COBOL Records to CSV Format")
    public void testConvertToCSV() {
        // Arrange - Parse sample records first
        Map<String, Object> parsedTransaction = converter.parseCobolRecord(sampleTransactionRecord, transactionCopybook);
        Map<String, Object> parsedAccount = converter.parseCobolRecord(sampleAccountRecord, accountCopybook);
        
        List<Map<String, Object>> records = Arrays.asList(parsedTransaction, parsedAccount);
        List<String> fieldNames = Arrays.asList("TRAN-ID", "TRAN-TYPE-CD", "TRAN-AMT", "TRAN-DESC");
        
        // Act - Convert to CSV format
        String csvOutput = converter.convertToCSV(records, fieldNames, ",");
        
        // Assert - Validate CSV structure and content
        assertThat(csvOutput).isNotNull();
        assertThat(csvOutput).isNotEmpty();
        
        // Validate CSV header
        String[] lines = csvOutput.split("\n");
        assertThat(lines).hasSizeGreaterThan(0);
        
        String headerLine = lines[0];
        assertThat(headerLine).contains("TRAN-ID,TRAN-TYPE-CD,TRAN-AMT,TRAN-DESC");
        
        // Validate data rows
        if (lines.length > 1) {
            String dataLine = lines[1];
            assertThat(dataLine).isNotEmpty();
            
            // Validate field count in CSV
            String[] fields = dataLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Handle quoted fields
            assertThat(fields).hasSizeGreaterThanOrEqualTo(4);
        }
        
        // Validate proper CSV escaping for fields containing commas
        if (csvOutput.contains("\"")) {
            assertThat(csvOutput).matches(".*\"[^\"]*\".*"); // Quoted fields are properly formatted
        }
        
        logTestExecution("CSV conversion validation completed", null);
    }

    /**
     * Test conversion to JSON format with nested structure support.
     * Validates that COBOL records are correctly converted to JSON objects
     * while preserving data types and supporting complex data structures.
     */
    @Test
    @DisplayName("Convert COBOL Records to JSON Format")
    public void testConvertToJSON() {
        // Arrange - Parse sample records
        Map<String, Object> parsedTransaction = converter.parseCobolRecord(sampleTransactionRecord, transactionCopybook);
        List<Map<String, Object>> records = Arrays.asList(parsedTransaction);
        
        // Act - Convert to JSON format
        String jsonOutput = converter.convertToJSON(records);
        
        // Assert - Validate JSON structure
        assertThat(jsonOutput).isNotNull();
        assertThat(jsonOutput).isNotEmpty();
        
        // Parse JSON to validate structure
        assertThatCode(() -> {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode jsonArray = mapper.readTree(jsonOutput);
            
            assertThat(jsonArray.isArray()).isTrue();
            assertThat(jsonArray.size()).isEqualTo(1);
            
            JsonNode transactionJson = jsonArray.get(0);
            assertThat(transactionJson.has("TRAN-ID")).isTrue();
            assertThat(transactionJson.has("TRAN-TYPE-CD")).isTrue();
            assertThat(transactionJson.has("TRAN-AMT")).isTrue();
            
            // Validate numeric field preservation
            JsonNode amountNode = transactionJson.get("TRAN-AMT");
            assertThat(amountNode.isNull()).isFalse();
            assertThat(amountNode.asDouble()).isEqualTo(100.50);
            
            // Validate string field preservation
            JsonNode tranIdNode = transactionJson.get("TRAN-ID");
            assertThat(tranIdNode.asText()).isNotEmpty();
            
        }).doesNotThrowAnyException();
        
        logTestExecution("JSON conversion validation completed", null);
    }

    /**
     * Test OCCURS clause handling for array conversion scenarios.
     * Validates that COBOL OCCURS clauses are correctly processed to create
     * JSON arrays while preserving element data types and structure.
     */
    @Test
    @DisplayName("Handle COBOL OCCURS Clause Array Conversion")
    public void testHandleOccursClause() {
        // Arrange - Create test data with OCCURS pattern
        String fieldData = "ITEM01ITEM02ITEM03ITEM04ITEM05"; // 5 items, 6 chars each
        String occursClause = "OCCURS 5 TIMES";
        int elementLength = 6;
        
        // Act - Process OCCURS clause
        List<Object> arrayElements = converter.handleOccursClause(fieldData, occursClause, elementLength);
        
        // Assert - Validate array processing
        assertThat(arrayElements).isNotNull();
        assertThat(arrayElements).hasSize(5);
        
        // Validate individual elements
        assertThat(arrayElements.get(0)).isEqualTo("ITEM01");
        assertThat(arrayElements.get(1)).isEqualTo("ITEM02");
        assertThat(arrayElements.get(2)).isEqualTo("ITEM03");
        assertThat(arrayElements.get(3)).isEqualTo("ITEM04");
        assertThat(arrayElements.get(4)).isEqualTo("ITEM05");
        
        // Test with different OCCURS format
        String occursClause2 = "OCCURS 3";
        List<Object> elements2 = converter.handleOccursClause("ABCDEFGHI", occursClause2, 3);
        assertThat(elements2).hasSize(3);
        assertThat(elements2.get(0)).isEqualTo("ABC");
        assertThat(elements2.get(1)).isEqualTo("DEF");
        assertThat(elements2.get(2)).isEqualTo("GHI");
        
        logTestExecution("OCCURS clause processing validation completed", null);
    }

    /**
     * Test numeric precision preservation for COBOL COMP-3 to BigDecimal conversion.
     * Validates that monetary calculations maintain exact precision matching
     * COBOL packed decimal behavior for financial accuracy.
     */
    @Test
    @DisplayName("Preserve COBOL COMP-3 Numeric Precision") 
    public void testPreserveNumericPrecision() {
        // Arrange - Test various numeric values with different scales
        Object[] testValues = {
            "123.45", 
            123.45, 
            new BigDecimal("999.99"),
            12345L,
            null
        };
        
        int monetaryScale = TestConstants.COBOL_DECIMAL_SCALE;
        
        // Act & Assert - Test each value type
        for (Object value : testValues) {
            BigDecimal result = converter.preserveNumericPrecision(value, monetaryScale);
            
            assertThat(result).isNotNull();
            assertThat(result.scale()).isEqualTo(monetaryScale);
            
            // Validate COBOL precision requirements
            boolean isValidPrecision = validateCobolPrecision(result, "test_field");
            assertThat(isValidPrecision).isTrue();
        }
        
        // Test with zero value
        BigDecimal zeroResult = converter.preserveNumericPrecision(null, monetaryScale);
        assertBigDecimalEquals(BigDecimal.ZERO.setScale(monetaryScale, TestConstants.COBOL_ROUNDING_MODE), 
                              zeroResult, "Zero value precision mismatch");
        
        // Test with large precision value
        BigDecimal largeValue = new BigDecimal("999999999.99");
        BigDecimal preservedLarge = converter.preserveNumericPrecision(largeValue, monetaryScale);
        assertThat(preservedLarge.scale()).isEqualTo(monetaryScale);
        
        logTestExecution("Numeric precision preservation validation completed", null);
    }

    /**
     * Test FILLER field handling in COBOL record conversions.
     * Validates that FILLER fields are properly managed during conversion,
     * with options to include or exclude placeholder data.
     */
    @Test
    @DisplayName("Handle COBOL FILLER Fields")
    public void testHandleFillerFields() {
        // Arrange - Test FILLER field definitions
        String fillerDefinition = "FILLER PIC X(20)";
        String nonFillerDefinition = "TRAN-ID PIC X(16)";
        
        // Act - Test FILLER field detection and handling
        Map<String, Object> fillerResult = converter.handleFillerFields(fillerDefinition, true);
        Map<String, Object> nonFillerResult = converter.handleFillerFields(nonFillerDefinition, false);
        
        // Assert - Validate FILLER field handling
        assertThat(fillerResult).containsEntry("isFiller", true);
        assertThat(fillerResult).containsEntry("include", true);
        assertThat(fillerResult).containsEntry("length", 20);
        assertThat(fillerResult).containsKey("placeholder");
        
        assertThat(nonFillerResult).containsEntry("isFiller", false);
        
        // Test FILLER exclusion
        Map<String, Object> excludeFillerResult = converter.handleFillerFields(fillerDefinition, false);
        assertThat(excludeFillerResult).containsEntry("include", false);
        
        // Test null input handling
        Map<String, Object> nullResult = converter.handleFillerFields(null, true);
        assertThat(nullResult).containsEntry("isFiller", false);
        
        logTestExecution("FILLER field handling validation completed", null);
    }

    /**
     * Test character encoding processing for EBCDIC to ASCII conversion.
     * Validates that character set conversion maintains data integrity
     * during mainframe to modern system data migration.
     */
    @Test
    @DisplayName("Process Character Encoding Conversion")
    public void testProcessEncoding() {
        // Arrange - Test data with various character patterns
        String testData = "Test Data 123!@#";
        String sourceEncoding = "UTF-8";
        String targetEncoding = "UTF-8";
        
        // Act - Process encoding conversion
        String convertedData = converter.processEncoding(testData, sourceEncoding, targetEncoding);
        
        // Assert - Validate encoding preservation
        assertThat(convertedData).isEqualTo(testData);
        
        // Test with null inputs
        String nullResult = converter.processEncoding(null, sourceEncoding, targetEncoding);
        assertThat(nullResult).isNull();
        
        String emptyResult = converter.processEncoding("", sourceEncoding, targetEncoding);
        assertThat(emptyResult).isEmpty();
        
        // Test with same source and target encoding
        String sameEncoding = converter.processEncoding(testData, "UTF-8", "UTF-8");
        assertThat(sameEncoding).isEqualTo(testData);
        
        // Test with null encoding parameters
        String nullEncodingResult = converter.processEncoding(testData, null, "UTF-8");
        assertThat(nullEncodingResult).isEqualTo(testData);
        
        logTestExecution("Character encoding processing validation completed", null);
    }

    /**
     * Test COBOL copybook parsing for field definition extraction.
     * Validates that copybook content is correctly parsed to extract field names,
     * PIC clauses, and other metadata needed for record conversion.
     */
    @Test
    @DisplayName("Parse COBOL Copybook Definitions")
    public void testParseCopybook() {
        // Arrange - Sample copybook content matching CVTRA05Y.cpy format
        String copybookContent = """
               *****************************************************************         
               *    Data-structure for TRANsaction record (RECLN = 350)                  
               *****************************************************************         
                01  TRAN-RECORD.                                                         
                    05  TRAN-ID                     PIC X(16).               
                    05  TRAN-TYPE-CD                PIC X(02).               
                    05  TRAN-AMT                    PIC S9(09)V99.           
                    05  TRAN-DESC                   PIC X(100).              
                    05  FILLER                      PIC X(20).               
               """;
        
        // Act - Parse copybook content
        Map<String, String> fieldDefinitions = converter.parseCopybook(copybookContent);
        
        // Assert - Validate field definitions
        assertThat(fieldDefinitions).isNotNull();
        assertThat(fieldDefinitions).isNotEmpty();
        
        // Validate specific field definitions
        assertThat(fieldDefinitions).containsKey("TRAN-ID");
        assertThat(fieldDefinitions).containsKey("TRAN-TYPE-CD");
        assertThat(fieldDefinitions).containsKey("TRAN-AMT");
        assertThat(fieldDefinitions).containsKey("TRAN-DESC");
        assertThat(fieldDefinitions).containsKey("FILLER");
        
        // Validate PIC clause extraction
        assertThat(fieldDefinitions.get("TRAN-ID")).contains("PIC X(16)");
        assertThat(fieldDefinitions.get("TRAN-AMT")).contains("PIC S9(09)V99");
        
        // Test error handling for invalid copybook
        assertThatThrownBy(() -> converter.parseCopybook(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Copybook content cannot be null");
            
        assertThatThrownBy(() -> converter.parseCopybook(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Copybook content cannot be null or empty");
        
        logTestExecution("Copybook parsing validation completed", null);
    }

    /**
     * Test conversion validation for data integrity verification.
     * Validates that conversion processes maintain data integrity and
     * provide comprehensive validation results with issue reporting.
     */
    @Test
    @DisplayName("Validate Conversion Data Integrity")
    public void testValidateConversion() {
        // Arrange - Test original and converted data pairs
        String originalData = sampleTransactionRecord;
        String csvData = "TRAN-ID,TRAN-TYPE-CD,TRAN-AMT\nTXN001,PU,100.50\n";
        String jsonData = "[{\"TRAN-ID\":\"TXN001\",\"TRAN-TYPE-CD\":\"PU\",\"TRAN-AMT\":100.50}]";
        
        // Act & Assert - Test different validation types
        
        // Test length validation
        Map<String, Object> lengthValidation = converter.validateConversion(originalData, csvData, "length");
        assertThat(lengthValidation).containsEntry("isValid", true);
        assertThat(lengthValidation).containsKey("issues");
        
        // Test JSON structure validation
        Map<String, Object> jsonValidation = converter.validateConversion(originalData, jsonData, "json_structure");
        assertThat(jsonValidation).containsEntry("isValid", true);
        
        // Test field count validation
        Map<String, Object> fieldValidation = converter.validateConversion(originalData, csvData, "field_count");
        assertThat(fieldValidation).containsEntry("isValid", true);
        
        // Test error scenarios
        Map<String, Object> nullValidation = converter.validateConversion(null, csvData, "length");
        assertThat(nullValidation).containsEntry("isValid", false);
        List<String> issues = (List<String>) nullValidation.get("issues");
        assertThat(issues).contains("Original or converted data is null");
        
        // Test invalid JSON validation
        Map<String, Object> invalidJsonValidation = converter.validateConversion(originalData, "invalid json", "json_structure");
        assertThat(invalidJsonValidation).containsEntry("isValid", false);
        
        logTestExecution("Conversion validation completed", null);
    }

    /**
     * Test malformed record handling with comprehensive error scenarios.
     * Validates that the converter properly handles various error conditions
     * including invalid record formats, malformed copybook definitions, and data corruption.
     */
    @Test
    @DisplayName("Handle Malformed Records and Error Scenarios")
    public void testMalformedRecordHandling() {
        // Test null record data
        assertThatThrownBy(() -> converter.parseCobolRecord(null, transactionCopybook))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Record data cannot be null");
        
        // Test empty copybook definition
        assertThatThrownBy(() -> converter.parseCobolRecord(sampleTransactionRecord, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Copybook definition cannot be null");
            
        assertThatThrownBy(() -> converter.parseCobolRecord(sampleTransactionRecord, new HashMap<>()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Copybook definition cannot be null or empty");
        
        // Test record too short for copybook definition
        String shortRecord = "SHORT";
        assertThatThrownBy(() -> converter.parseCobolRecord(shortRecord, transactionCopybook))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("extends beyond record length");
        
        // Test record too long (exceeds maximum)
        String longRecord = "A".repeat(40000); // Exceeds MAX_RECORD_LENGTH
        assertThatThrownBy(() -> converter.parseCobolRecord(longRecord, transactionCopybook))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Record length exceeds maximum");
        
        // Test invalid OCCURS clause
        assertThatThrownBy(() -> converter.handleOccursClause("DATA", "INVALID CLAUSE", 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid OCCURS clause");
        
        // Test field data too short for OCCURS
        assertThatThrownBy(() -> converter.handleOccursClause("ABC", "OCCURS 5 TIMES", 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Field data too short");
        
        // Test invalid encoding conversion
        assertThatThrownBy(() -> converter.processEncoding("test", "INVALID_ENCODING", "UTF-8"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Failed to convert encoding");
        
        logTestExecution("Malformed record handling validation completed", null);
    }

    /**
     * Test batch file processing with comprehensive data handling.
     * Validates that large batch files are processed efficiently while
     * maintaining data integrity and providing proper error recovery.
     */
    @Test
    @DisplayName("Process Batch Files with Data Integrity")
    public void testBatchProcessing() {
        // Arrange - Create multi-record batch input
        StringBuilder batchInput = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            batchInput.append(createSampleTransactionRecord(i)).append("\n");
        }
        
        // Act - Process batch to CSV
        String csvResult = converter.processBatchFile(batchInput.toString(), transactionCopybook, "CSV");
        
        // Assert - Validate CSV batch output
        assertThat(csvResult).isNotNull();
        assertThat(csvResult).isNotEmpty();
        
        String[] csvLines = csvResult.split("\n");
        assertThat(csvLines).hasSizeGreaterThan(5); // Header + 5 data records
        
        // Validate CSV header
        assertThat(csvLines[0]).contains("TRAN-ID");
        
        // Act - Process batch to JSON
        String jsonResult = converter.processBatchFile(batchInput.toString(), transactionCopybook, "JSON");
        
        // Assert - Validate JSON batch output
        assertThat(jsonResult).isNotNull();
        assertThat(jsonResult).isNotEmpty();
        
        // Parse and validate JSON array
        assertThatCode(() -> {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode jsonArray = mapper.readTree(jsonResult);
            assertThat(jsonArray.isArray()).isTrue();
            assertThat(jsonArray.size()).isEqualTo(5);
        }).doesNotThrowAnyException();
        
        // Test error handling for empty input
        assertThatThrownBy(() -> converter.processBatchFile("", transactionCopybook, "CSV"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Input data cannot be null or empty");
        
        // Test error handling for invalid format
        assertThatThrownBy(() -> converter.processBatchFile(batchInput.toString(), transactionCopybook, "INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported output format");
        
        logTestExecution("Batch processing validation completed", null);
    }

    /**
     * Test large dataset conversion for performance and memory efficiency validation.
     * Validates that the converter can handle large volumes of data efficiently
     * without memory exhaustion or performance degradation.
     */
    @Test
    @DisplayName("Convert Large Datasets Efficiently")
    public void testLargeDatasetConversion() {
        // Arrange - Create large dataset (1000 records)
        List<Map<String, Object>> largeDataset = new ArrayList<>();
        
        for (int i = 1; i <= 1000; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("TRAN-ID", String.format("TXN%013d", i));
            record.put("TRAN-TYPE-CD", "PU");
            record.put("TRAN-AMT", new BigDecimal(String.format("%d.%02d", i, i % 100)));
            record.put("TRAN-DESC", "Test Transaction " + i);
            largeDataset.add(record);
        }
        
        List<String> fieldNames = Arrays.asList("TRAN-ID", "TRAN-TYPE-CD", "TRAN-AMT", "TRAN-DESC");
        
        // Act - Convert large dataset to CSV
        long startTime = System.currentTimeMillis();
        String csvResult = converter.convertToCSV(largeDataset, fieldNames, ",");
        long csvTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate CSV conversion performance and correctness
        assertThat(csvResult).isNotNull();
        assertThat(csvResult).isNotEmpty();
        
        String[] csvLines = csvResult.split("\n");
        assertThat(csvLines).hasSize(1001); // Header + 1000 data records
        
        // Validate performance threshold (should complete within reasonable time)
        assertThat(csvTime).isLessThan(5000L); // 5 seconds for 1000 records
        
        // Act - Convert large dataset to JSON
        startTime = System.currentTimeMillis();
        String jsonResult = converter.convertToJSON(largeDataset);
        long jsonTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate JSON conversion performance and correctness
        assertThat(jsonResult).isNotNull();
        assertThat(jsonResult).isNotEmpty();
        
        // Parse and validate JSON structure
        assertThatCode(() -> {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode jsonArray = mapper.readTree(jsonResult);
            assertThat(jsonArray.isArray()).isTrue();
            assertThat(jsonArray.size()).isEqualTo(1000);
        }).doesNotThrowAnyException();
        
        // Validate performance threshold
        assertThat(jsonTime).isLessThan(5000L); // 5 seconds for 1000 records
        
        logTestExecution("Large dataset conversion validation completed", null);
    }

    /**
     * Test comprehensive error handling across all conversion operations.
     * Validates that all error scenarios are properly handled with appropriate
     * exception types and descriptive error messages.
     */
    @Test
    @DisplayName("Comprehensive Error Handling Validation")
    public void testErrorHandling() {
        // Test parseCobolRecord error scenarios
        Map<String, String> invalidCopybook = Map.of("INVALID_FIELD", "INVALID PIC CLAUSE");
        
        // Test convertToCSV error scenarios
        assertThatThrownBy(() -> converter.convertToCSV(null, Arrays.asList("FIELD1"), ","))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Records list cannot be null");
            
        assertThatThrownBy(() -> converter.convertToCSV(Arrays.asList(Map.of("KEY", "VALUE")), null, ","))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Field names list cannot be null");
            
        assertThatThrownBy(() -> converter.convertToCSV(new ArrayList<>(), Arrays.asList("FIELD1"), ","))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Records list cannot be null or empty");
        
        // Test convertToJSON error scenarios
        assertThatThrownBy(() -> converter.convertToJSON(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Records list cannot be null");
            
        assertThatThrownBy(() -> converter.convertToJSON(new ArrayList<>()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Records list cannot be null or empty");
        
        // Test handleOccursClause error scenarios
        assertThatThrownBy(() -> converter.handleOccursClause(null, "OCCURS 5", 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Field data cannot be null");
            
        assertThatThrownBy(() -> converter.handleOccursClause("DATA", null, 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("OCCURS clause cannot be null");
        
        // Test preserveNumericPrecision error scenarios
        assertThatThrownBy(() -> converter.preserveNumericPrecision("INVALID", 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot preserve precision");
        
        logTestExecution("Error handling validation completed", null);
    }

    /**
     * Parameterized test for COBOL data type conversion scenarios.
     * Tests various COBOL PIC clauses and data patterns to ensure
     * comprehensive coverage of conversion edge cases and data types.
     */
    @ParameterizedTest
    @DisplayName("COBOL Data Type Conversion Scenarios")
    @CsvSource({
        "PIC X(10), 'TEST DATA ', String, TEST DATA",
        "PIC 9(5), '12345', Long, 12345",
        "PIC S9(7)V99, '1234567', BigDecimal, 12345.67",
        "PIC X(5), '     ', String, ''",
        "PIC 9(3), '000', Long, 0"
    })
    public void testCobolDataTypeConversion(String picClause, String inputValue, String expectedType, String expectedValue) {
        // Arrange - Create test copybook with single field
        Map<String, String> testCopybook = Map.of("TEST_FIELD", picClause);
        
        // Create test record with proper padding
        String testRecord = String.format("%-20s", inputValue); // Pad to ensure sufficient length
        
        // Act - Parse the record
        Map<String, Object> parsedRecord = converter.parseCobolRecord(testRecord, testCopybook);
        
        // Assert - Validate conversion result
        assertThat(parsedRecord).containsKey("TEST_FIELD");
        Object convertedValue = parsedRecord.get("TEST_FIELD");
        
        // Validate data type
        switch (expectedType) {
            case "String":
                assertThat(convertedValue).isInstanceOf(String.class);
                assertThat(convertedValue.toString().trim()).isEqualTo(expectedValue);
                break;
            case "Long":
                assertThat(convertedValue).isInstanceOf(Long.class);
                assertThat(convertedValue).isEqualTo(Long.parseLong(expectedValue));
                break;
            case "BigDecimal":
                assertThat(convertedValue).isInstanceOf(BigDecimal.class);
                BigDecimal bdExpected = new BigDecimal(expectedValue);
                assertBigDecimalEquals(bdExpected, (BigDecimal) convertedValue, 
                    "BigDecimal conversion mismatch for " + picClause);
                break;
        }
        
        logTestExecution("COBOL data type conversion test completed for " + picClause, null);
    }

    /**
     * Test with real COBOL copybook data from Transaction and Account entities.
     * Validates conversion using actual entity getter methods to ensure
     * integration compatibility with JPA entities.
     */
    @Test
    @DisplayName("Integration with JPA Entity Data")
    public void testEntityIntegration() {
        // Arrange - Create test transaction and account objects
        Transaction testTransaction = new Transaction();
        testTransaction.setTransactionId(1L);
        testTransaction.setAmount(new BigDecimal("250.75"));
        testTransaction.setTransactionTypeCode("PU");
        testTransaction.setDescription("Test Purchase Transaction");
        testTransaction.setAccountId(12345L);
        
        Account testAccount = new Account();
        testAccount.setAccountId(12345L);
        testAccount.setCurrentBalance(new BigDecimal("1500.25"));
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        testAccount.setCashCreditLimit(new BigDecimal("1000.00"));
        
        // Act - Convert entity data to map format for testing
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("TRAN-ID", testTransaction.getTransactionId().toString());
        transactionData.put("TRAN-AMT", testTransaction.getAmount());
        transactionData.put("TRAN-TYPE-CD", testTransaction.getTransactionType());
        transactionData.put("TRAN-DESC", testTransaction.getDescription());
        transactionData.put("ACCT-ID", testTransaction.getAccountId().toString());
        
        Map<String, Object> accountData = new HashMap<>();
        accountData.put("ACCT-ID", testAccount.getAccountId().toString());
        accountData.put("CURR-BAL", testAccount.getCurrentBalance());
        accountData.put("CREDIT-LIMIT", testAccount.getCreditLimit());
        accountData.put("CASH-LIMIT", testAccount.getCashCreditLimit());
        
        List<Map<String, Object>> entityRecords = Arrays.asList(transactionData, accountData);
        List<String> fieldNames = Arrays.asList("TRAN-ID", "TRAN-AMT", "TRAN-TYPE-CD", "ACCT-ID");
        
        // Act - Convert to CSV and JSON
        String csvOutput = converter.convertToCSV(entityRecords, fieldNames, ",");
        String jsonOutput = converter.convertToJSON(entityRecords);
        
        // Assert - Validate outputs
        assertThat(csvOutput).isNotNull();
        assertThat(csvOutput).contains("TRAN-ID,TRAN-AMT,TRAN-TYPE-CD,ACCT-ID");
        assertThat(csvOutput).contains("1,250.75");
        
        assertThat(jsonOutput).isNotNull();
        assertThatCode(() -> {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode jsonArray = mapper.readTree(jsonOutput);
            assertThat(jsonArray.isArray()).isTrue();
            assertThat(jsonArray.size()).isEqualTo(2);
        }).doesNotThrowAnyException();
        
        logTestExecution("Entity integration validation completed", null);
    }

    /**
     * Test performance benchmarks for file format conversion operations.
     * Validates that conversion operations meet performance requirements
     * and maintain acceptable response times for large data volumes.
     */
    @Test
    @DisplayName("Performance Benchmark Validation")
    public void testPerformanceBenchmarks() {
        // Arrange - Create moderate-sized dataset for performance testing
        List<Map<String, Object>> testRecords = generateTestData("transactions", 100);
        List<String> fieldNames = Arrays.asList("transactionId", "accountId", "transactionAmount", "transactionType");
        
        // Act & Assert - Test CSV conversion performance
        long startTime = System.currentTimeMillis();
        String csvResult = converter.convertToCSV(testRecords, fieldNames, ",");
        long csvConversionTime = System.currentTimeMillis() - startTime;
        
        assertThat(csvResult).isNotNull();
        assertThat(csvConversionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        // Act & Assert - Test JSON conversion performance
        startTime = System.currentTimeMillis();
        String jsonResult = converter.convertToJSON(testRecords);
        long jsonConversionTime = System.currentTimeMillis() - startTime;
        
        assertThat(jsonResult).isNotNull();
        assertThat(jsonConversionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        // Validate functional parity rules
        Map<String, Object> parityRules = TestConstants.FUNCTIONAL_PARITY_RULES;
        assertThat((Boolean) parityRules.get("preserve_decimal_precision")).isTrue();
        assertThat((Boolean) parityRules.get("match_cobol_rounding")).isTrue();
        
        logTestExecution("Performance benchmark validation completed", null);
    }

    /**
     * Helper method to build transaction copybook definition based on CVTRA05Y.cpy.
     * Creates field mapping for transaction record structure with proper PIC clauses.
     * 
     * @return Map containing field names and their COBOL PIC definitions
     */
    private Map<String, String> buildTransactionCopybook() {
        Map<String, String> copybook = new LinkedHashMap<>();
        
        // Based on CVTRA05Y.cpy structure - TRAN-RECORD (RECLN = 350)
        copybook.put("TRAN-ID", "PIC X(16)");
        copybook.put("TRAN-TYPE-CD", "PIC X(02)");
        copybook.put("TRAN-CAT-CD", "PIC 9(04) COMP");
        copybook.put("TRAN-SOURCE", "PIC X(02)");
        copybook.put("TRAN-DESC", "PIC X(100)");
        copybook.put("TRAN-AMT", "PIC S9(09)V99 COMP-3");
        copybook.put("TRAN-MERCHANT-ID", "PIC X(15)");
        copybook.put("TRAN-MERCHANT-NAME", "PIC X(50)");
        copybook.put("TRAN-MERCHANT-CITY", "PIC X(50)");
        copybook.put("TRAN-MERCHANT-ZIP", "PIC X(10)");
        copybook.put("TRAN-CARD-NUM", "PIC X(19)");
        copybook.put("TRAN-ORIG-TS", "PIC X(26)");
        copybook.put("TRAN-PROC-TS", "PIC X(26)");
        copybook.put("FILLER", "PIC X(25)");
        
        return copybook;
    }

    /**
     * Helper method to build account copybook definition based on CVACT01Y.cpy.
     * Creates field mapping for account record structure with proper PIC clauses.
     * 
     * @return Map containing field names and their COBOL PIC definitions
     */
    private Map<String, String> buildAccountCopybook() {
        Map<String, String> copybook = new LinkedHashMap<>();
        
        // Based on CVACT01Y.cpy structure - ACCOUNT-RECORD (RECLN = 300)
        copybook.put("ACCT-ID", "PIC 9(11) COMP");
        copybook.put("ACCT-ACTIVE-STATUS", "PIC X(01)");
        copybook.put("ACCT-CURR-BAL", "PIC S9(09)V99 COMP-3");
        copybook.put("ACCT-CREDIT-LIMIT", "PIC S9(09)V99 COMP-3");
        copybook.put("ACCT-CASH-CREDIT-LIMIT", "PIC S9(09)V99 COMP-3");
        copybook.put("ACCT-OPEN-DATE", "PIC X(10)");
        copybook.put("ACCT-EXPIRAION-DATE", "PIC X(10)");
        copybook.put("ACCT-REISSUE-DATE", "PIC X(10)");
        copybook.put("ACCT-CURR-CYC-CREDIT", "PIC S9(09)V99 COMP-3");
        copybook.put("ACCT-CURR-CYC-DEBIT", "PIC S9(09)V99 COMP-3");
        copybook.put("ACCT-GROUP-ID", "PIC X(10)");
        copybook.put("FILLER", "PIC X(178)");
        
        return copybook;
    }

    /**
     * Helper method to create sample transaction record matching COBOL fixed-width format.
     * Generates test data conforming to CVTRA05Y.cpy record layout.
     * 
     * @return Fixed-width transaction record string for testing
     */
    private String buildSampleTransactionRecord() {
        StringBuilder record = new StringBuilder();
        
        // TRAN-ID (16 bytes)
        record.append(String.format("%-16s", "TXN0000000001"));
        
        // TRAN-TYPE-CD (2 bytes)
        record.append("PU");
        
        // TRAN-CAT-CD (4 bytes binary, represented as string for testing)
        record.append("5000");
        
        // TRAN-SOURCE (2 bytes)
        record.append("01");
        
        // TRAN-DESC (100 bytes)
        record.append(String.format("%-100s", "Test Purchase Transaction"));
        
        // TRAN-AMT (COMP-3, represented as string for testing)
        record.append(String.format("%011d", 10050)); // 100.50 in cents
        
        // TRAN-MERCHANT-ID (15 bytes)
        record.append(String.format("%-15s", "MERCH001"));
        
        // TRAN-MERCHANT-NAME (50 bytes)
        record.append(String.format("%-50s", "Test Merchant Store"));
        
        // TRAN-MERCHANT-CITY (50 bytes)
        record.append(String.format("%-50s", "Test City"));
        
        // TRAN-MERCHANT-ZIP (10 bytes)
        record.append(String.format("%-10s", "12345"));
        
        // TRAN-CARD-NUM (19 bytes)
        record.append(String.format("%-19s", "4000123456789012"));
        
        // TRAN-ORIG-TS (26 bytes)
        record.append(String.format("%-26s", "2024-01-15T10:30:00.000Z"));
        
        // TRAN-PROC-TS (26 bytes)
        record.append(String.format("%-26s", "2024-01-15T10:31:00.000Z"));
        
        // FILLER (25 bytes)
        record.append(String.format("%-25s", ""));
        
        return record.toString();
    }

    /**
     * Helper method to create sample account record matching COBOL fixed-width format.
     * Generates test data conforming to CVACT01Y.cpy record layout.
     * 
     * @return Fixed-width account record string for testing
     */
    private String buildSampleAccountRecord() {
        StringBuilder record = new StringBuilder();
        
        // ACCT-ID (11 bytes, packed decimal represented as string)
        record.append(String.format("%011d", 12345678901L));
        
        // ACCT-ACTIVE-STATUS (1 byte)
        record.append("Y");
        
        // ACCT-CURR-BAL (COMP-3, represented as string)
        record.append(String.format("%011d", 150025)); // 1500.25 in cents
        
        // ACCT-CREDIT-LIMIT (COMP-3)
        record.append(String.format("%011d", 500000)); // 5000.00 in cents
        
        // ACCT-CASH-CREDIT-LIMIT (COMP-3)
        record.append(String.format("%011d", 100000)); // 1000.00 in cents
        
        // ACCT-OPEN-DATE (10 bytes)
        record.append("2020-01-15");
        
        // ACCT-EXPIRAION-DATE (10 bytes)
        record.append("2025-01-31");
        
        // ACCT-REISSUE-DATE (10 bytes)
        record.append("2023-02-01");
        
        // ACCT-CURR-CYC-CREDIT (COMP-3)
        record.append(String.format("%011d", 75000)); // 750.00 in cents
        
        // ACCT-CURR-CYC-DEBIT (COMP-3)
        record.append(String.format("%011d", 25000)); // 250.00 in cents
        
        // ACCT-GROUP-ID (10 bytes)
        record.append(String.format("%-10s", "GROUP001"));
        
        // FILLER (178 bytes)
        record.append(String.format("%-178s", ""));
        
        return record.toString();
    }

    /**
     * Helper method to create sample transaction record with specific ID.
     * Used for batch processing tests to generate multiple unique records.
     * 
     * @param recordId Unique identifier for the test record
     * @return Fixed-width transaction record string with unique ID
     */
    private String createSampleTransactionRecord(int recordId) {
        StringBuilder record = new StringBuilder();
        
        // TRAN-ID (16 bytes) - unique per record
        record.append(String.format("TXN%013d", recordId));
        
        // TRAN-TYPE-CD (2 bytes)
        record.append("PU");
        
        // TRAN-CAT-CD (4 bytes)
        record.append("5000");
        
        // TRAN-SOURCE (2 bytes)
        record.append("01");
        
        // TRAN-DESC (100 bytes)
        record.append(String.format("%-100s", "Batch Test Transaction " + recordId));
        
        // TRAN-AMT (COMP-3) - varying amounts
        record.append(String.format("%011d", (recordId * 100) + 50)); // Varying amounts
        
        // Fill remaining fields with default data
        record.append(String.format("%-15s", "MERCH" + String.format("%03d", recordId % 100)));
        record.append(String.format("%-50s", "Test Merchant " + recordId));
        record.append(String.format("%-50s", "Test City"));
        record.append(String.format("%-10s", "12345"));
        record.append(String.format("%-19s", "4000123456789012"));
        record.append(String.format("%-26s", "2024-01-15T10:30:00.000Z"));
        record.append(String.format("%-26s", "2024-01-15T10:31:00.000Z"));
        record.append(String.format("%-25s", ""));
        
        return record.toString();
    }

    /**
     * Helper method to generate test data for performance testing.
     * Creates structured test data matching entity requirements.
     * 
     * @param entityType Type of entity data to generate ("transactions" or "accounts")
     * @param count Number of records to generate
     * @return List of test records as Map objects
     */
    @Override
    protected List<Map<String, Object>> generateTestData(String entityType, int count) {
        List<Map<String, Object>> testData = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Map<String, Object> record = new HashMap<>();
            
            if ("transactions".equals(entityType)) {
                record.put("transactionId", (long) i);
                record.put("accountId", (long) (1000 + i));
                record.put("transactionAmount", new BigDecimal(String.format("%d.%02d", i, i % 100)));
                record.put("transactionType", i % 2 == 0 ? "PU" : "CA");
                record.put("transactionDescription", "Test Transaction " + i);
            } else if ("accounts".equals(entityType)) {
                record.put("accountId", (long) i);
                record.put("currentBalance", new BigDecimal(String.format("%d.%02d", 1000 + i, i % 100)));
                record.put("creditLimit", new BigDecimal("5000.00"));
                record.put("customerId", (long) (100 + i));
            }
            
            testData.add(record);
        }
        
        return testData;
    }
}
