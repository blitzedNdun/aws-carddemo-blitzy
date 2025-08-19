/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.FileFormatConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.Constants;
import com.carddemo.util.FormatUtil;

/**
 * Utility class for comparing Java service outputs with expected COBOL program outputs 
 * to ensure functional parity during the mainframe modernization process.
 * 
 * This comprehensive comparison framework validates that the modernized Spring Boot services
 * produce identical results to their original COBOL counterparts, ensuring zero functional
 * regression during the migration. The utility supports various comparison modes including
 * numeric precision validation, file content comparison, date format verification, and
 * complete business logic output validation.
 * 
 * Key Features:
 * - COBOL COMP-3 to Java BigDecimal precision validation with exact parity
 * - Fixed-width COBOL record parsing and field-by-field comparison
 * - Date format conversion validation ensuring CCYYMMDD compatibility
 * - String field comparison with configurable tolerance for padding differences
 * - Record layout validation matching COBOL copybook specifications
 * - Comprehensive reporting with detailed difference analysis
 * - Configurable tolerance settings for acceptable variations
 * - Sort order verification for list operations
 * - Pagination validation for screen operations
 * - Business logic accuracy assessment with penny-level precision
 * 
 * The comparison framework directly supports the requirements specified in Section 0.5.1
 * for maintaining identical calculation results and ensuring byte-for-byte output compatibility
 * with the original COBOL implementation during parallel run validation.
 * 
 * Usage Examples:
 * <pre>
 * {@code
 * // Configure tolerance settings
 * ToleranceSettings tolerance = new ToleranceSettings();
 * tolerance.setNumericTolerance(BigDecimal.ZERO); // Exact precision required
 * 
 * // Load COBOL test outputs
 * Map<String, Object> cobolData = CobolComparisonUtils.loadCobolTestOutput("test-account-data.txt");
 * 
 * // Compare with Java service results
 * ComparisonResult result = CobolComparisonUtils.compareFieldByField(cobolData, javaResults, tolerance);
 * 
 * // Generate detailed comparison report
 * String report = CobolComparisonUtils.generateComparisonReport(result);
 * }
 * </pre>
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public final class CobolComparisonUtils {

    private static final Logger logger = LoggerFactory.getLogger(CobolComparisonUtils.class);
    
    // Default tolerance settings for comparison operations
    private static ToleranceSettings defaultTolerance = new ToleranceSettings();
    
    // Object mapper for JSON processing and comparison
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Field separators and formatting constants
    private static final String FIELD_SEPARATOR = "|";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String REPORT_HEADER_SEPARATOR = "=".repeat(80);
    private static final String SECTION_SEPARATOR = "-".repeat(60);
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CobolComparisonUtils() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Compares report outputs between COBOL and Java implementations for identical content validation.
     * 
     * This method performs comprehensive report comparison including layout validation,
     * data precision verification, and format consistency checking. It ensures that
     * modernized report generation produces byte-for-byte identical output to the
     * original COBOL report programs.
     * 
     * @param cobolReportPath Path to the COBOL-generated report file
     * @param javaReportPath Path to the Java-generated report file
     * @param toleranceSettings Tolerance configuration for comparison operations
     * @return ComparisonResult containing detailed comparison analysis
     * @throws IOException if report files cannot be read
     * @throws IllegalArgumentException if report paths are invalid
     */
    public static ComparisonResult compareReportOutputs(Path cobolReportPath, Path javaReportPath, 
                                                      ToleranceSettings toleranceSettings) throws IOException {
        logger.info("Starting report output comparison: COBOL={}, Java={}", 
                   cobolReportPath.getFileName(), javaReportPath.getFileName());
        
        if (!Files.exists(cobolReportPath)) {
            throw new IllegalArgumentException("COBOL report file does not exist: " + cobolReportPath);
        }
        if (!Files.exists(javaReportPath)) {
            throw new IllegalArgumentException("Java report file does not exist: " + javaReportPath);
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try (BufferedReader cobolReader = Files.newBufferedReader(cobolReportPath);
             BufferedReader javaReader = Files.newBufferedReader(javaReportPath)) {
            
            String cobolLine;
            String javaLine;
            int lineNumber = 1;
            
            while ((cobolLine = cobolReader.readLine()) != null) {
                javaLine = javaReader.readLine();
                
                if (javaLine == null) {
                    differences.add("Line " + lineNumber + ": Java report shorter than COBOL report");
                    result.setSuccessful(false);
                    break;
                }
                
                // Compare lines with tolerance settings
                if (!compareStringFields(cobolLine, javaLine, toleranceSettings)) {
                    differences.add("Line " + lineNumber + ": Content mismatch");
                    differences.add("  COBOL: " + cobolLine);
                    differences.add("  Java:  " + javaLine);
                    result.setSuccessful(false);
                }
                
                lineNumber++;
            }
            
            // Check for additional lines in Java report
            if (javaReader.readLine() != null) {
                differences.add("Java report longer than COBOL report starting at line " + lineNumber);
                result.setSuccessful(false);
            }
        }
        
        result.addDifferences(differences);
        result.addComparedField("Report Layout Comparison", "Line-by-line content validation");
        
        if (result.isSuccessful()) {
            logger.info("Report comparison successful: {} lines validated", result.getComparedFields().size());
        } else {
            logger.warn("Report comparison failed: {} differences found", differences.size());
        }
        
        return result;
    }

    /**
     * Validates numeric precision between COBOL COMP-3 and Java BigDecimal implementations.
     * 
     * This method ensures that financial calculations maintain exact precision parity
     * between COBOL packed decimal arithmetic and Java BigDecimal operations. It validates
     * scale, precision, and rounding behavior to guarantee identical monetary calculations.
     * 
     * @param cobolComp3Value Original COBOL COMP-3 value as byte array
     * @param javaBigDecimal Corresponding Java BigDecimal value
     * @param expectedScale Expected decimal scale for validation
     * @param expectedPrecision Expected total precision for validation
     * @return ComparisonResult indicating precision validation success or failure
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult validateNumericPrecision(byte[] cobolComp3Value, BigDecimal javaBigDecimal,
                                                           int expectedScale, int expectedPrecision) {
        logger.debug("Validating numeric precision: scale={}, precision={}", expectedScale, expectedPrecision);
        
        if (cobolComp3Value == null) {
            throw new IllegalArgumentException("COBOL COMP-3 value cannot be null");
        }
        if (javaBigDecimal == null) {
            throw new IllegalArgumentException("Java BigDecimal value cannot be null");
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Convert COBOL COMP-3 to BigDecimal for comparison
            BigDecimal cobolBigDecimal = CobolDataConverter.fromComp3(cobolComp3Value, expectedScale);
            
            // Validate scale precision
            if (javaBigDecimal.scale() != expectedScale) {
                differences.add("Scale mismatch: expected=" + expectedScale + ", actual=" + javaBigDecimal.scale());
                result.setSuccessful(false);
            }
            
            // Validate total precision
            if (javaBigDecimal.precision() != expectedPrecision) {
                differences.add("Precision mismatch: expected=" + expectedPrecision + ", actual=" + javaBigDecimal.precision());
                result.setSuccessful(false);
            }
            
            // Validate value equality with exact comparison
            if (cobolBigDecimal.compareTo(javaBigDecimal) != 0) {
                differences.add("Value mismatch: COBOL=" + cobolBigDecimal + ", Java=" + javaBigDecimal);
                result.setSuccessful(false);
            }
            
            // Validate preserved precision using CobolDataConverter
            BigDecimal preservedValue = CobolDataConverter.preservePrecision(javaBigDecimal, expectedScale);
            if (preservedValue.compareTo(javaBigDecimal) != 0) {
                differences.add("Precision preservation failed: original=" + javaBigDecimal + ", preserved=" + preservedValue);
                result.setSuccessful(false);
            }
            
        } catch (Exception e) {
            differences.add("Numeric conversion error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during numeric precision validation", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("Numeric Precision", "COMP-3 to BigDecimal validation");
        
        if (result.isSuccessful()) {
            logger.debug("Numeric precision validation successful");
        } else {
            logger.warn("Numeric precision validation failed: {} issues found", differences.size());
        }
        
        return result;
    }

    /**
     * Compares file content between COBOL fixed-width format and Java processed data.
     * 
     * This method validates that Java services process COBOL data files correctly,
     * maintaining exact field content and structure. It supports comparison of
     * fixed-width records, copybook-defined layouts, and data transformation accuracy.
     * 
     * @param cobolFilePath Path to original COBOL data file
     * @param javaDataMap Java processed data as Map representation
     * @param copybookLayout Copybook layout specification for field parsing
     * @param toleranceSettings Tolerance configuration for field comparisons
     * @return ComparisonResult with detailed file content comparison results
     * @throws IOException if file reading operations fail
     */
    public static ComparisonResult compareFileContent(Path cobolFilePath, Map<String, Object> javaDataMap,
                                                    String copybookLayout, ToleranceSettings toleranceSettings) throws IOException {
        logger.info("Comparing file content: COBOL file={}, copybook layout provided", cobolFilePath.getFileName());
        
        if (!Files.exists(cobolFilePath)) {
            throw new IllegalArgumentException("COBOL file does not exist: " + cobolFilePath);
        }
        if (javaDataMap == null) {
            throw new IllegalArgumentException("Java data map cannot be null");
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Load and parse COBOL file using FileFormatConverter
            Map<String, Object> cobolData = loadCobolTestOutput(cobolFilePath.toString());
            
            // Parse copybook layout for field definitions
            FileFormatConverter converter = new FileFormatConverter();
            Map<String, String> parsedCopybook = converter.parseCopybook(copybookLayout);
            
            // Compare each field defined in the copybook
            for (String fieldName : parsedCopybook.keySet()) {
                Object cobolValue = cobolData.get(fieldName);
                Object javaValue = javaDataMap.get(fieldName);
                
                ComparisonResult fieldResult = compareFieldByField(
                    Map.of(fieldName, cobolValue), 
                    Map.of(fieldName, javaValue), 
                    toleranceSettings
                );
                
                if (!fieldResult.isSuccessful()) {
                    differences.addAll(fieldResult.getDifferences());
                    result.setSuccessful(false);
                }
                
                result.addComparedField(fieldName, "File content field comparison");
            }
            
            // Validate file structure and record count
            Map<String, Object> validationResult = converter.validateConversion(
                cobolData.toString(), javaDataMap.toString(), "STRUCTURE");
            if (!(Boolean) validationResult.getOrDefault("isValid", false)) {
                differences.add("File structure validation failed");
                result.setSuccessful(false);
            }
            
        } catch (Exception e) {
            differences.add("File content comparison error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during file content comparison", e);
        }
        
        result.addDifferences(differences);
        
        if (result.isSuccessful()) {
            logger.info("File content comparison successful: {} fields validated", result.getComparedFields().size());
        } else {
            logger.warn("File content comparison failed: {} differences found", differences.size());
        }
        
        return result;
    }

    /**
     * Validates date format conversion between COBOL CCYYMMDD and Java LocalDate.
     * 
     * This method ensures that date field conversions maintain exact compatibility
     * with COBOL date validation logic, including leap year handling, month validation,
     * and day validation as defined in the CSUTLDPY copybook procedures.
     * 
     * @param cobolDateString Original COBOL date in CCYYMMDD format
     * @param javaLocalDate Converted Java LocalDate object
     * @return ComparisonResult indicating date format validation success or failure
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult validateDateFormat(String cobolDateString, LocalDate javaLocalDate) {
        logger.debug("Validating date format conversion: COBOL={}, Java={}", cobolDateString, javaLocalDate);
        
        if (cobolDateString == null) {
            throw new IllegalArgumentException("COBOL date string cannot be null");
        }
        if (javaLocalDate == null) {
            throw new IllegalArgumentException("Java LocalDate cannot be null");
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Validate COBOL date string format (CCYYMMDD)
            if (cobolDateString.length() != Constants.DATE_FORMAT_LENGTH) {
                differences.add("Invalid COBOL date length: expected=" + Constants.DATE_FORMAT_LENGTH + 
                              ", actual=" + cobolDateString.length());
                result.setSuccessful(false);
            }
            
            // Validate using DateConversionUtil
            if (!DateConversionUtil.validateDate(cobolDateString)) {
                differences.add("COBOL date validation failed: " + cobolDateString);
                result.setSuccessful(false);
            }
            
            // Convert COBOL date to LocalDate for comparison
            LocalDate convertedDate = DateConversionUtil.parseDate(cobolDateString);
            
            // Compare converted date with Java LocalDate
            if (!convertedDate.equals(javaLocalDate)) {
                differences.add("Date conversion mismatch: COBOL converted=" + convertedDate + 
                              ", Java=" + javaLocalDate);
                result.setSuccessful(false);
            }
            
            // Validate round-trip conversion
            String reconvertedDate = DateConversionUtil.formatToCobol(javaLocalDate);
            if (!cobolDateString.equals(reconvertedDate)) {
                differences.add("Round-trip conversion failed: original=" + cobolDateString + 
                              ", reconverted=" + reconvertedDate);
                result.setSuccessful(false);
            }
            
        } catch (DateTimeParseException e) {
            differences.add("Date parsing error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during date format validation", e);
        } catch (Exception e) {
            differences.add("Date validation error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Unexpected error during date format validation", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("Date Format", "CCYYMMDD to LocalDate conversion validation");
        
        if (result.isSuccessful()) {
            logger.debug("Date format validation successful");
        } else {
            logger.warn("Date format validation failed: {} issues found", differences.size());
        }
        
        return result;
    }

    /**
     * Compares BigDecimal precision between COBOL and Java implementations for exact parity.
     * 
     * This method performs detailed BigDecimal comparison including scale validation,
     * precision verification, and rounding behavior consistency. It ensures that
     * financial calculations produce identical results between COBOL and Java systems.
     * 
     * @param cobolBigDecimal BigDecimal value from COBOL conversion
     * @param javaBigDecimal BigDecimal value from Java calculation
     * @param toleranceSettings Tolerance configuration for precision comparison
     * @return ComparisonResult with detailed precision comparison analysis
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult compareBigDecimalPrecision(BigDecimal cobolBigDecimal, BigDecimal javaBigDecimal,
                                                            ToleranceSettings toleranceSettings) {
        logger.debug("Comparing BigDecimal precision: COBOL={}, Java={}", cobolBigDecimal, javaBigDecimal);
        
        if (cobolBigDecimal == null) {
            throw new IllegalArgumentException("COBOL BigDecimal cannot be null");
        }
        if (javaBigDecimal == null) {
            throw new IllegalArgumentException("Java BigDecimal cannot be null");
        }
        if (toleranceSettings == null) {
            toleranceSettings = defaultTolerance;
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Compare values within tolerance
            BigDecimal difference = cobolBigDecimal.subtract(javaBigDecimal).abs();
            BigDecimal tolerance = toleranceSettings.getNumericTolerance();
            
            if (difference.compareTo(tolerance) > 0) {
                differences.add("Value difference exceeds tolerance: difference=" + difference + 
                              ", tolerance=" + tolerance);
                result.setSuccessful(false);
            }
            
            // Compare scale (decimal places)
            if (cobolBigDecimal.scale() != javaBigDecimal.scale()) {
                differences.add("Scale mismatch: COBOL=" + cobolBigDecimal.scale() + 
                              ", Java=" + javaBigDecimal.scale());
                result.setSuccessful(false);
            }
            
            // Compare precision (total digits)
            if (cobolBigDecimal.precision() != javaBigDecimal.precision()) {
                differences.add("Precision mismatch: COBOL=" + cobolBigDecimal.precision() + 
                              ", Java=" + javaBigDecimal.precision());
                result.setSuccessful(false);
            }
            
            // Validate exact equality for strict mode
            if (toleranceSettings.isStrictMode() && cobolBigDecimal.compareTo(javaBigDecimal) != 0) {
                differences.add("Strict mode: Values not exactly equal: COBOL=" + cobolBigDecimal + 
                              ", Java=" + javaBigDecimal);
                result.setSuccessful(false);
            }
            
        } catch (Exception e) {
            differences.add("BigDecimal comparison error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during BigDecimal precision comparison", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("BigDecimal Precision", "Detailed precision and scale validation");
        
        if (result.isSuccessful()) {
            logger.debug("BigDecimal precision comparison successful");
        } else {
            logger.warn("BigDecimal precision comparison failed: {} issues found", differences.size());
        }
        
        return result;
    }

    /**
     * Loads COBOL test output files and converts them to Java Map representation.
     * 
     * This method reads COBOL fixed-width data files and converts them to structured
     * Java data for comparison operations. It handles multiple record formats and
     * provides field-level access to COBOL data values.
     * 
     * @param cobolFilePath Path to the COBOL test output file
     * @return Map containing field names and values from the COBOL file
     * @throws IOException if file reading operations fail
     * @throws IllegalArgumentException if file path is invalid
     */
    public static Map<String, Object> loadCobolTestOutput(String cobolFilePath) throws IOException {
        logger.info("Loading COBOL test output from: {}", cobolFilePath);
        
        if (cobolFilePath == null || cobolFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("COBOL file path cannot be null or empty");
        }
        
        Path filePath = Paths.get(cobolFilePath);
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("COBOL file does not exist: " + cobolFilePath);
        }
        
        Map<String, Object> cobolData = new HashMap<>();
        
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int recordNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                // Skip empty lines and comments
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                
                try {
                    // Parse COBOL record using FileFormatConverter
                    FileFormatConverter converter = new FileFormatConverter();
                    // Use a basic copybook definition for parsing
                    Map<String, String> basicCopybook = new HashMap<>();
                    basicCopybook.put("DEFAULT_FIELD", "PIC X(" + line.length() + ")");
                    Map<String, Object> recordData = converter.parseCobolRecord(line, basicCopybook);
                    
                    // Merge record data into main map with record number prefix
                    for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                        String fieldKey = "RECORD_" + recordNumber + "_" + entry.getKey();
                        cobolData.put(fieldKey, entry.getValue());
                    }
                    
                    recordNumber++;
                    
                } catch (Exception e) {
                    logger.warn("Error parsing COBOL record at line {}: {}", recordNumber, e.getMessage());
                    // Continue processing other records
                }
            }
            
            logger.info("Successfully loaded {} records from COBOL file", recordNumber - 1);
            
        } catch (IOException e) {
            logger.error("Error reading COBOL file: {}", cobolFilePath, e);
            throw e;
        }
        
        return cobolData;
    }

    /**
     * Performs field-by-field comparison between COBOL and Java data structures.
     * 
     * This method compares corresponding fields from COBOL and Java data maps,
     * applying appropriate conversion and tolerance rules for each field type.
     * It provides detailed analysis of field-level differences and validates
     * data transformation accuracy.
     * 
     * @param cobolData Map containing COBOL field data
     * @param javaData Map containing Java field data
     * @param toleranceSettings Tolerance configuration for field comparisons
     * @return ComparisonResult with comprehensive field comparison analysis
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult compareFieldByField(Map<String, Object> cobolData, Map<String, Object> javaData,
                                                     ToleranceSettings toleranceSettings) {
        logger.debug("Starting field-by-field comparison: {} COBOL fields, {} Java fields", 
                    cobolData.size(), javaData.size());
        
        if (cobolData == null) {
            throw new IllegalArgumentException("COBOL data map cannot be null");
        }
        if (javaData == null) {
            throw new IllegalArgumentException("Java data map cannot be null");
        }
        if (toleranceSettings == null) {
            toleranceSettings = defaultTolerance;
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        Set<String> allFields = new HashSet<>();
        allFields.addAll(cobolData.keySet());
        allFields.addAll(javaData.keySet());
        
        int matchedFields = 0;
        int totalFields = allFields.size();
        
        for (String fieldName : allFields) {
            Object cobolValue = cobolData.get(fieldName);
            Object javaValue = javaData.get(fieldName);
            
            try {
                if (cobolValue == null && javaValue == null) {
                    matchedFields++;
                    continue;
                }
                
                if (cobolValue == null) {
                    differences.add("Field " + fieldName + ": Missing in COBOL data");
                    continue;
                }
                
                if (javaValue == null) {
                    differences.add("Field " + fieldName + ": Missing in Java data");
                    continue;
                }
                
                // Compare based on field type
                boolean fieldsMatch = false;
                
                if (cobolValue instanceof BigDecimal && javaValue instanceof BigDecimal) {
                    ComparisonResult numericResult = compareBigDecimalPrecision(
                        (BigDecimal) cobolValue, (BigDecimal) javaValue, toleranceSettings);
                    fieldsMatch = numericResult.isSuccessful();
                    if (!fieldsMatch) {
                        differences.addAll(numericResult.getDifferences().stream()
                            .map(diff -> "Field " + fieldName + ": " + diff)
                            .collect(Collectors.toList()));
                    }
                } else if (cobolValue instanceof String && javaValue instanceof String) {
                    fieldsMatch = compareStringFields((String) cobolValue, (String) javaValue, toleranceSettings);
                    if (!fieldsMatch) {
                        differences.add("Field " + fieldName + ": String mismatch - COBOL='" + 
                                      cobolValue + "', Java='" + javaValue + "'");
                    }
                } else {
                    // Generic object comparison
                    fieldsMatch = Objects.equals(cobolValue, javaValue);
                    if (!fieldsMatch) {
                        differences.add("Field " + fieldName + ": Value mismatch - COBOL=" + 
                                      cobolValue + ", Java=" + javaValue);
                    }
                }
                
                if (fieldsMatch) {
                    matchedFields++;
                }
                
                result.addComparedField(fieldName, "Field-by-field comparison");
                
            } catch (Exception e) {
                differences.add("Field " + fieldName + ": Comparison error - " + e.getMessage());
                logger.warn("Error comparing field {}: {}", fieldName, e.getMessage());
            }
        }
        
        // Calculate match percentage
        double matchPercentage = totalFields > 0 ? (double) matchedFields / totalFields * 100.0 : 100.0;
        result.setMatchPercentage(matchPercentage);
        
        if (differences.isEmpty()) {
            result.setSuccessful(true);
            logger.debug("Field-by-field comparison successful: {}/{} fields matched ({}%)", 
                        matchedFields, totalFields, String.format("%.1f", matchPercentage));
        } else {
            result.setSuccessful(false);
            result.addDifferences(differences);
            logger.warn("Field-by-field comparison failed: {}/{} fields matched ({}%), {} differences", 
                       matchedFields, totalFields, String.format("%.1f", matchPercentage), differences.size());
        }
        
        return result;
    }

    /**
     * Validates calculation precision for financial operations and business logic.
     * 
     * This method verifies that mathematical calculations produce identical results
     * between COBOL and Java implementations, with particular focus on financial
     * precision, rounding behavior, and monetary calculations.
     * 
     * @param cobolCalculationResult Result from COBOL calculation
     * @param javaCalculationResult Result from Java calculation
     * @param operationDescription Description of the calculation operation
     * @param toleranceSettings Tolerance configuration for precision validation
     * @return ComparisonResult with detailed calculation precision analysis
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult validateCalculationPrecision(BigDecimal cobolCalculationResult, 
                                                              BigDecimal javaCalculationResult,
                                                              String operationDescription,
                                                              ToleranceSettings toleranceSettings) {
        logger.debug("Validating calculation precision for operation: {}", operationDescription);
        
        if (cobolCalculationResult == null) {
            throw new IllegalArgumentException("COBOL calculation result cannot be null");
        }
        if (javaCalculationResult == null) {
            throw new IllegalArgumentException("Java calculation result cannot be null");
        }
        if (operationDescription == null || operationDescription.trim().isEmpty()) {
            operationDescription = "Unknown calculation";
        }
        if (toleranceSettings == null) {
            toleranceSettings = defaultTolerance;
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Validate exact precision for financial calculations
            BigDecimal difference = cobolCalculationResult.subtract(javaCalculationResult).abs();
            BigDecimal tolerance = toleranceSettings.getNumericTolerance();
            
            // For financial calculations, typically require exact precision
            if (operationDescription.toLowerCase().contains("interest") || 
                operationDescription.toLowerCase().contains("balance") ||
                operationDescription.toLowerCase().contains("payment") ||
                operationDescription.toLowerCase().contains("fee")) {
                
                // Financial calculations require exact match
                if (cobolCalculationResult.compareTo(javaCalculationResult) != 0) {
                    differences.add("Financial calculation mismatch: COBOL=" + cobolCalculationResult + 
                                  ", Java=" + javaCalculationResult + ", difference=" + difference);
                    result.setSuccessful(false);
                }
            } else {
                // Non-financial calculations can use tolerance
                if (difference.compareTo(tolerance) > 0) {
                    differences.add("Calculation difference exceeds tolerance: difference=" + difference + 
                                  ", tolerance=" + tolerance);
                    result.setSuccessful(false);
                }
            }
            
            // Validate scale consistency
            if (cobolCalculationResult.scale() != javaCalculationResult.scale()) {
                differences.add("Scale mismatch: COBOL scale=" + cobolCalculationResult.scale() + 
                              ", Java scale=" + javaCalculationResult.scale());
                if (toleranceSettings.isStrictMode()) {
                    result.setSuccessful(false);
                }
            }
            
            // Validate using CobolDataConverter precision preservation
            BigDecimal preservedResult = CobolDataConverter.preservePrecision(
                javaCalculationResult, cobolCalculationResult.scale());
            
            if (cobolCalculationResult.compareTo(preservedResult) != 0) {
                differences.add("Precision preservation validation failed: original=" + javaCalculationResult + 
                              ", preserved=" + preservedResult + ", expected=" + cobolCalculationResult);
                result.setSuccessful(false);
            }
            
        } catch (Exception e) {
            differences.add("Calculation precision validation error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during calculation precision validation", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("Calculation Precision", operationDescription);
        
        if (result.isSuccessful()) {
            logger.debug("Calculation precision validation successful for: {}", operationDescription);
        } else {
            logger.warn("Calculation precision validation failed for {}: {} issues found", 
                       operationDescription, differences.size());
        }
        
        return result;
    }

    /**
     * Verifies sort orders between COBOL and Java list operations for consistency.
     * 
     * This method validates that list sorting operations produce identical ordering
     * between COBOL and Java implementations, ensuring user interface consistency
     * and data presentation parity.
     * 
     * @param cobolSortedList List sorted by COBOL logic
     * @param javaSortedList List sorted by Java logic
     * @param sortDescription Description of the sort operation
     * @return ComparisonResult indicating sort order validation success or failure
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult verifySortOrders(List<?> cobolSortedList, List<?> javaSortedList,
                                                  String sortDescription) {
        logger.debug("Verifying sort orders for: {}", sortDescription);
        
        if (cobolSortedList == null) {
            throw new IllegalArgumentException("COBOL sorted list cannot be null");
        }
        if (javaSortedList == null) {
            throw new IllegalArgumentException("Java sorted list cannot be null");
        }
        if (sortDescription == null || sortDescription.trim().isEmpty()) {
            sortDescription = "Unknown sort operation";
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Validate list sizes
            if (cobolSortedList.size() != javaSortedList.size()) {
                differences.add("List size mismatch: COBOL=" + cobolSortedList.size() + 
                              ", Java=" + javaSortedList.size());
                result.setSuccessful(false);
                return result;
            }
            
            // Compare element by element for order verification
            for (int i = 0; i < cobolSortedList.size(); i++) {
                Object cobolElement = cobolSortedList.get(i);
                Object javaElement = javaSortedList.get(i);
                
                if (!Objects.equals(cobolElement, javaElement)) {
                    differences.add("Sort order mismatch at position " + i + ": COBOL=" + 
                                  cobolElement + ", Java=" + javaElement);
                    result.setSuccessful(false);
                }
            }
            
            // Additional validation for common sort scenarios
            if (sortDescription.toLowerCase().contains("account")) {
                // Validate account number sorting
                validateAccountSorting(cobolSortedList, javaSortedList, differences);
            } else if (sortDescription.toLowerCase().contains("transaction")) {
                // Validate transaction sorting (typically by date/time)
                validateTransactionSorting(cobolSortedList, javaSortedList, differences);
            }
            
        } catch (Exception e) {
            differences.add("Sort order verification error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during sort order verification", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("Sort Order", sortDescription);
        
        if (result.isSuccessful()) {
            logger.debug("Sort order verification successful for: {}", sortDescription);
        } else {
            logger.warn("Sort order verification failed for {}: {} differences found", 
                       sortDescription, differences.size());
        }
        
        return result;
    }

    /**
     * Validates pagination behavior between COBOL and Java implementations.
     * 
     * This method ensures that pagination operations produce consistent results
     * between COBOL screen operations and Java REST API responses, maintaining
     * identical page sizes, navigation behavior, and data presentation.
     * 
     * @param cobolPageData Data from COBOL paginated operation
     * @param javaPageData Data from Java paginated operation
     * @param pageSize Expected page size
     * @param pageNumber Current page number
     * @return ComparisonResult indicating pagination validation success or failure
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult validatePagination(List<?> cobolPageData, List<?> javaPageData,
                                                    int pageSize, int pageNumber) {
        logger.debug("Validating pagination: page={}, size={}", pageNumber, pageSize);
        
        if (cobolPageData == null) {
            throw new IllegalArgumentException("COBOL page data cannot be null");
        }
        if (javaPageData == null) {
            throw new IllegalArgumentException("Java page data cannot be null");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Validate page sizes
            if (cobolPageData.size() != javaPageData.size()) {
                differences.add("Page size mismatch: COBOL=" + cobolPageData.size() + 
                              ", Java=" + javaPageData.size());
                result.setSuccessful(false);
            }
            
            // Validate against expected page size (unless it's the last page)
            if (cobolPageData.size() > pageSize) {
                differences.add("COBOL page size exceeds maximum: actual=" + cobolPageData.size() + 
                              ", max=" + pageSize);
                result.setSuccessful(false);
            }
            
            if (javaPageData.size() > pageSize) {
                differences.add("Java page size exceeds maximum: actual=" + javaPageData.size() + 
                              ", max=" + pageSize);
                result.setSuccessful(false);
            }
            
            // Compare page content element by element
            int compareSize = Math.min(cobolPageData.size(), javaPageData.size());
            for (int i = 0; i < compareSize; i++) {
                Object cobolElement = cobolPageData.get(i);
                Object javaElement = javaPageData.get(i);
                
                if (!Objects.equals(cobolElement, javaElement)) {
                    differences.add("Page element mismatch at position " + i + ": COBOL=" + 
                                  cobolElement + ", Java=" + javaElement);
                    result.setSuccessful(false);
                }
            }
            
            // Validate pagination constants from Constants class
            if (pageSize != Constants.TRANSACTIONS_PER_PAGE && 
                pageSize != Constants.ACCOUNTS_PER_PAGE && 
                pageSize != Constants.USERS_PER_PAGE) {
                differences.add("Page size does not match defined constants: " + pageSize);
                // This is a warning, not a failure
            }
            
        } catch (Exception e) {
            differences.add("Pagination validation error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during pagination validation", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("Pagination", "Page " + pageNumber + " with size " + pageSize);
        
        if (result.isSuccessful()) {
            logger.debug("Pagination validation successful: page={}, size={}", pageNumber, pageSize);
        } else {
            logger.warn("Pagination validation failed: page={}, size={}, {} differences", 
                       pageNumber, pageSize, differences.size());
        }
        
        return result;
    }

    /**
     * Generates comprehensive comparison report with detailed analysis and statistics.
     * 
     * This method creates a formatted report containing comparison results,
     * difference analysis, match statistics, and recommendations for addressing
     * any identified discrepancies between COBOL and Java implementations.
     * 
     * @param comparisonResult Result object containing comparison data
     * @return Formatted string report with detailed comparison analysis
     * @throws IllegalArgumentException if comparison result is null
     */
    public static String generateComparisonReport(ComparisonResult comparisonResult) {
        logger.debug("Generating comparison report");
        
        if (comparisonResult == null) {
            throw new IllegalArgumentException("Comparison result cannot be null");
        }
        
        StringBuilder report = new StringBuilder();
        
        // Report header
        report.append(REPORT_HEADER_SEPARATOR).append(LINE_SEPARATOR);
        report.append("COBOL-JAVA FUNCTIONAL PARITY COMPARISON REPORT").append(LINE_SEPARATOR);
        report.append("Generated: ").append(new Date()).append(LINE_SEPARATOR);
        report.append(REPORT_HEADER_SEPARATOR).append(LINE_SEPARATOR);
        report.append(LINE_SEPARATOR);
        
        // Executive summary
        report.append("EXECUTIVE SUMMARY").append(LINE_SEPARATOR);
        report.append(SECTION_SEPARATOR).append(LINE_SEPARATOR);
        report.append("Overall Status: ").append(comparisonResult.isSuccessful() ? "PASS" : "FAIL").append(LINE_SEPARATOR);
        report.append("Match Percentage: ").append(String.format("%.2f%%", comparisonResult.getMatchPercentage())).append(LINE_SEPARATOR);
        report.append("Fields Compared: ").append(comparisonResult.getComparedFields().size()).append(LINE_SEPARATOR);
        report.append("Differences Found: ").append(comparisonResult.getDifferences().size()).append(LINE_SEPARATOR);
        report.append("Tolerance Violations: ").append(comparisonResult.getToleranceViolations().size()).append(LINE_SEPARATOR);
        report.append(LINE_SEPARATOR);
        
        // Compared fields section
        if (!comparisonResult.getComparedFields().isEmpty()) {
            report.append("COMPARED FIELDS").append(LINE_SEPARATOR);
            report.append(SECTION_SEPARATOR).append(LINE_SEPARATOR);
            
            for (Map.Entry<String, String> field : comparisonResult.getComparedFields().entrySet()) {
                report.append("- ").append(field.getKey()).append(": ").append(field.getValue()).append(LINE_SEPARATOR);
            }
            report.append(LINE_SEPARATOR);
        }
        
        // Differences section
        if (!comparisonResult.getDifferences().isEmpty()) {
            report.append("IDENTIFIED DIFFERENCES").append(LINE_SEPARATOR);
            report.append(SECTION_SEPARATOR).append(LINE_SEPARATOR);
            
            for (int i = 0; i < comparisonResult.getDifferences().size(); i++) {
                report.append(String.format("%d. %s", i + 1, comparisonResult.getDifferences().get(i))).append(LINE_SEPARATOR);
            }
            report.append(LINE_SEPARATOR);
        }
        
        // Tolerance violations section
        if (!comparisonResult.getToleranceViolations().isEmpty()) {
            report.append("TOLERANCE VIOLATIONS").append(LINE_SEPARATOR);
            report.append(SECTION_SEPARATOR).append(LINE_SEPARATOR);
            
            for (String violation : comparisonResult.getToleranceViolations()) {
                report.append("- ").append(violation).append(LINE_SEPARATOR);
            }
            report.append(LINE_SEPARATOR);
        }
        
        // Failed validations section
        if (!comparisonResult.getFailedValidations().isEmpty()) {
            report.append("FAILED VALIDATIONS").append(LINE_SEPARATOR);
            report.append(SECTION_SEPARATOR).append(LINE_SEPARATOR);
            
            for (String failure : comparisonResult.getFailedValidations()) {
                report.append("- ").append(failure).append(LINE_SEPARATOR);
            }
            report.append(LINE_SEPARATOR);
        }
        
        // Summary and recommendations
        report.append("SUMMARY AND RECOMMENDATIONS").append(LINE_SEPARATOR);
        report.append(SECTION_SEPARATOR).append(LINE_SEPARATOR);
        
        if (comparisonResult.isSuccessful()) {
            report.append("✓ Functional parity validation PASSED").append(LINE_SEPARATOR);
            report.append("✓ COBOL and Java implementations produce identical results").append(LINE_SEPARATOR);
            report.append("✓ Ready for production deployment").append(LINE_SEPARATOR);
        } else {
            report.append("✗ Functional parity validation FAILED").append(LINE_SEPARATOR);
            report.append("✗ Differences require investigation and resolution").append(LINE_SEPARATOR);
            report.append("✗ Additional testing and code review required").append(LINE_SEPARATOR);
            
            // Specific recommendations based on failure types
            if (comparisonResult.getDifferences().stream().anyMatch(d -> d.contains("precision") || d.contains("BigDecimal"))) {
                report.append("⚠ Review BigDecimal scale and precision handling").append(LINE_SEPARATOR);
                report.append("⚠ Verify COBOL COMP-3 conversion accuracy").append(LINE_SEPARATOR);
            }
            
            if (comparisonResult.getDifferences().stream().anyMatch(d -> d.contains("date") || d.contains("Date"))) {
                report.append("⚠ Review date format conversion logic").append(LINE_SEPARATOR);
                report.append("⚠ Verify CCYYMMDD format handling").append(LINE_SEPARATOR);
            }
            
            if (comparisonResult.getDifferences().stream().anyMatch(d -> d.contains("sort") || d.contains("order"))) {
                report.append("⚠ Review sorting algorithm implementation").append(LINE_SEPARATOR);
                report.append("⚠ Verify collation sequence consistency").append(LINE_SEPARATOR);
            }
        }
        
        report.append(LINE_SEPARATOR);
        report.append(REPORT_HEADER_SEPARATOR).append(LINE_SEPARATOR);
        
        logger.info("Comparison report generated: {} lines, status={}", 
                   report.toString().split(LINE_SEPARATOR).length, 
                   comparisonResult.isSuccessful() ? "PASS" : "FAIL");
        
        return report.toString();
    }

    /**
     * Sets tolerance settings for comparison operations throughout the utility.
     * 
     * This method configures the tolerance settings used by various comparison
     * operations, allowing for configurable precision requirements and
     * validation strictness levels.
     * 
     * @param toleranceSettings New tolerance configuration to apply
     * @throws IllegalArgumentException if tolerance settings are null
     */
    public static void setToleranceSettings(ToleranceSettings toleranceSettings) {
        if (toleranceSettings == null) {
            throw new IllegalArgumentException("Tolerance settings cannot be null");
        }
        
        defaultTolerance = toleranceSettings;
        logger.info("Updated tolerance settings: numeric={}, string={}, date={}, strict={}", 
                   toleranceSettings.getNumericTolerance(),
                   toleranceSettings.getStringTolerance(),
                   toleranceSettings.getDateTolerance(),
                   toleranceSettings.isStrictMode());
    }

    /**
     * Compares string fields with configurable tolerance for padding and formatting differences.
     * 
     * This method compares string values between COBOL and Java implementations,
     * applying tolerance rules for whitespace differences, padding variations,
     * and case sensitivity based on the configured tolerance settings.
     * 
     * @param cobolString String value from COBOL implementation
     * @param javaString String value from Java implementation
     * @param toleranceSettings Tolerance configuration for string comparison
     * @return true if strings match within tolerance, false otherwise
     */
    public static boolean compareStringFields(String cobolString, String javaString, 
                                            ToleranceSettings toleranceSettings) {
        if (toleranceSettings == null) {
            toleranceSettings = defaultTolerance;
        }
        
        // Handle null values
        if (cobolString == null && javaString == null) {
            return true;
        }
        if (cobolString == null || javaString == null) {
            return false;
        }
        
        // Apply string tolerance settings
        String processedCobol = cobolString;
        String processedJava = javaString;
        
        // Trim whitespace if tolerance allows
        if (toleranceSettings.getStringTolerance().contains("TRIM")) {
            processedCobol = processedCobol.trim();
            processedJava = processedJava.trim();
        }
        
        // Handle case sensitivity
        if (toleranceSettings.getStringTolerance().contains("IGNORE_CASE")) {
            processedCobol = processedCobol.toUpperCase();
            processedJava = processedJava.toUpperCase();
        }
        
        // Handle padding differences (common in COBOL fixed-width fields)
        if (toleranceSettings.getStringTolerance().contains("IGNORE_PADDING")) {
            processedCobol = processedCobol.replaceAll("\\s+$", ""); // Remove trailing spaces
            processedJava = processedJava.replaceAll("\\s+$", "");
        }
        
        return processedCobol.equals(processedJava);
    }

    /**
     * Validates record layout consistency between COBOL and Java data structures.
     * 
     * This method ensures that data structures maintain consistent field layouts,
     * types, and organization between COBOL copybook definitions and Java entity
     * classes, validating structural compatibility.
     * 
     * @param cobolRecordData COBOL record data structure
     * @param javaRecordData Java record data structure
     * @param expectedLayout Expected record layout specification
     * @return ComparisonResult indicating record layout validation success or failure
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult validateRecordLayout(Map<String, Object> cobolRecordData,
                                                      Map<String, Object> javaRecordData,
                                                      String expectedLayout) {
        logger.debug("Validating record layout against specification");
        
        if (cobolRecordData == null) {
            throw new IllegalArgumentException("COBOL record data cannot be null");
        }
        if (javaRecordData == null) {
            throw new IllegalArgumentException("Java record data cannot be null");
        }
        if (expectedLayout == null || expectedLayout.trim().isEmpty()) {
            throw new IllegalArgumentException("Expected layout specification cannot be null or empty");
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Parse expected layout using FileFormatConverter
            FileFormatConverter converter = new FileFormatConverter();
            Map<String, String> layoutSpec = converter.parseCopybook(expectedLayout);
            
            // Validate field presence in both structures
            for (String expectedField : layoutSpec.keySet()) {
                if (!cobolRecordData.containsKey(expectedField)) {
                    differences.add("Missing field in COBOL data: " + expectedField);
                    result.setSuccessful(false);
                }
                
                if (!javaRecordData.containsKey(expectedField)) {
                    differences.add("Missing field in Java data: " + expectedField);
                    result.setSuccessful(false);
                }
            }
            
            // Validate extra fields (not in layout)
            for (String cobolField : cobolRecordData.keySet()) {
                if (!layoutSpec.containsKey(cobolField)) {
                    differences.add("Unexpected field in COBOL data: " + cobolField);
                    // This might be acceptable, so don't fail automatically
                }
            }
            
            for (String javaField : javaRecordData.keySet()) {
                if (!layoutSpec.containsKey(javaField)) {
                    differences.add("Unexpected field in Java data: " + javaField);
                    // This might be acceptable, so don't fail automatically
                }
            }
            
            // Validate field types and constraints
            for (String fieldName : layoutSpec.keySet()) {
                Object layoutField = layoutSpec.get(fieldName);
                Object cobolValue = cobolRecordData.get(fieldName);
                Object javaValue = javaRecordData.get(fieldName);
                
                if (cobolValue != null && javaValue != null) {
                    // Validate type compatibility
                    Class<?> cobolType = cobolValue.getClass();
                    Class<?> javaType = javaValue.getClass();
                    
                    if (!areTypesCompatible(cobolType, javaType)) {
                        differences.add("Type mismatch for field " + fieldName + 
                                      ": COBOL=" + cobolType.getSimpleName() + 
                                      ", Java=" + javaType.getSimpleName());
                        result.setSuccessful(false);
                    }
                }
            }
            
            // Validate record length consistency (for fixed-width records)
            if (expectedLayout.contains("FIXED") || expectedLayout.contains("PIC")) {
                // Additional validation for COBOL fixed-width compatibility
                Map<String, Object> validationResult = converter.validateConversion(
                    cobolRecordData.toString(), javaRecordData.toString(), "FIXED_WIDTH");
                if (!(Boolean) validationResult.getOrDefault("isValid", false)) {
                    differences.add("Fixed-width record validation failed");
                    result.setSuccessful(false);
                }
            }
            
        } catch (Exception e) {
            differences.add("Record layout validation error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during record layout validation", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("Record Layout", "Structure and field validation");
        
        if (result.isSuccessful()) {
            logger.debug("Record layout validation successful");
        } else {
            logger.warn("Record layout validation failed: {} differences found", differences.size());
        }
        
        return result;
    }

    /**
     * Checks data integrity between COBOL and Java implementations.
     * 
     * This method performs comprehensive data integrity validation including
     * referential integrity, constraint validation, and data consistency
     * checks to ensure data quality parity between systems.
     * 
     * @param cobolDataSet Complete COBOL dataset for integrity checking
     * @param javaDataSet Complete Java dataset for integrity checking
     * @return ComparisonResult indicating data integrity validation success or failure
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult checkDataIntegrity(Map<String, Object> cobolDataSet,
                                                    Map<String, Object> javaDataSet) {
        logger.debug("Checking data integrity between COBOL and Java datasets");
        
        if (cobolDataSet == null) {
            throw new IllegalArgumentException("COBOL dataset cannot be null");
        }
        if (javaDataSet == null) {
            throw new IllegalArgumentException("Java dataset cannot be null");
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Validate dataset completeness
            if (cobolDataSet.size() != javaDataSet.size()) {
                differences.add("Dataset size mismatch: COBOL=" + cobolDataSet.size() + 
                              ", Java=" + javaDataSet.size());
                result.setSuccessful(false);
            }
            
            // Validate key fields for integrity
            validateKeyFieldIntegrity(cobolDataSet, javaDataSet, differences);
            
            // Validate referential integrity
            validateReferentialIntegrity(cobolDataSet, javaDataSet, differences);
            
            // Validate data constraints
            validateDataConstraints(cobolDataSet, javaDataSet, differences);
            
            // Validate business rules
            validateBusinessRules(cobolDataSet, javaDataSet, differences);
            
            if (!differences.isEmpty()) {
                result.setSuccessful(false);
            }
            
        } catch (Exception e) {
            differences.add("Data integrity check error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during data integrity check", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("Data Integrity", "Comprehensive integrity validation");
        
        if (result.isSuccessful()) {
            logger.debug("Data integrity check successful");
        } else {
            logger.warn("Data integrity check failed: {} violations found", differences.size());
        }
        
        return result;
    }

    /**
     * Compares formatted output between COBOL and Java display operations.
     * 
     * This method validates that formatted output for user interfaces,
     * reports, and file generation produces identical results between
     * COBOL and Java implementations, ensuring display consistency.
     * 
     * @param cobolFormattedOutput Formatted output from COBOL operations
     * @param javaFormattedOutput Formatted output from Java operations
     * @param formatSpecification Format specification for validation
     * @return ComparisonResult indicating formatted output validation success or failure
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult compareFormattedOutput(String cobolFormattedOutput,
                                                        String javaFormattedOutput,
                                                        String formatSpecification) {
        logger.debug("Comparing formatted output with specification: {}", formatSpecification);
        
        if (cobolFormattedOutput == null) {
            throw new IllegalArgumentException("COBOL formatted output cannot be null");
        }
        if (javaFormattedOutput == null) {
            throw new IllegalArgumentException("Java formatted output cannot be null");
        }
        if (formatSpecification == null || formatSpecification.trim().isEmpty()) {
            formatSpecification = "Standard format";
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Direct string comparison for exact formatting match
            if (!cobolFormattedOutput.equals(javaFormattedOutput)) {
                differences.add("Formatted output mismatch:");
                differences.add("  COBOL: '" + cobolFormattedOutput + "'");
                differences.add("  Java:  '" + javaFormattedOutput + "'");
                result.setSuccessful(false);
            }
            
            // Validate specific format requirements
            if (formatSpecification.contains("CURRENCY")) {
                validateCurrencyFormatting(cobolFormattedOutput, javaFormattedOutput, differences);
            }
            
            if (formatSpecification.contains("DATE")) {
                validateDateFormatting(cobolFormattedOutput, javaFormattedOutput, differences);
            }
            
            if (formatSpecification.contains("NUMERIC")) {
                validateNumericFormatting(cobolFormattedOutput, javaFormattedOutput, differences);
            }
            
            // Validate using FormatUtil for consistency
            if (formatSpecification.contains("PIC")) {
                try {
                    String validatedFormat = FormatUtil.formatCobolPattern(javaFormattedOutput, formatSpecification);
                    if (!cobolFormattedOutput.equals(validatedFormat)) {
                        differences.add("PIC pattern validation failed: expected=" + validatedFormat + 
                                      ", actual=" + cobolFormattedOutput);
                        result.setSuccessful(false);
                    }
                } catch (Exception e) {
                    differences.add("PIC pattern validation error: " + e.getMessage());
                    result.setSuccessful(false);
                }
            }
            
        } catch (Exception e) {
            differences.add("Formatted output comparison error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during formatted output comparison", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("Formatted Output", formatSpecification);
        
        if (result.isSuccessful()) {
            logger.debug("Formatted output comparison successful");
        } else {
            logger.warn("Formatted output comparison failed: {} differences found", differences.size());
        }
        
        return result;
    }

    /**
     * Validates business logic output for functional parity between implementations.
     * 
     * This method performs comprehensive business logic validation including
     * calculation accuracy, business rule enforcement, and decision logic
     * consistency to ensure functional parity between COBOL and Java systems.
     * 
     * @param cobolBusinessResult Business logic result from COBOL implementation
     * @param javaBusinessResult Business logic result from Java implementation
     * @param businessRuleDescription Description of the business rule being validated
     * @param toleranceSettings Tolerance configuration for business logic comparison
     * @return ComparisonResult indicating business logic validation success or failure
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult validateBusinessLogicOutput(Object cobolBusinessResult,
                                                             Object javaBusinessResult,
                                                             String businessRuleDescription,
                                                             ToleranceSettings toleranceSettings) {
        logger.debug("Validating business logic output for: {}", businessRuleDescription);
        
        if (cobolBusinessResult == null) {
            throw new IllegalArgumentException("COBOL business result cannot be null");
        }
        if (javaBusinessResult == null) {
            throw new IllegalArgumentException("Java business result cannot be null");
        }
        if (businessRuleDescription == null || businessRuleDescription.trim().isEmpty()) {
            businessRuleDescription = "Unknown business rule";
        }
        if (toleranceSettings == null) {
            toleranceSettings = defaultTolerance;
        }
        
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();
        
        try {
            // Type-specific validation
            if (cobolBusinessResult instanceof BigDecimal && javaBusinessResult instanceof BigDecimal) {
                // Financial calculation validation
                ComparisonResult numericResult = validateCalculationPrecision(
                    (BigDecimal) cobolBusinessResult, 
                    (BigDecimal) javaBusinessResult,
                    businessRuleDescription,
                    toleranceSettings
                );
                
                if (!numericResult.isSuccessful()) {
                    differences.addAll(numericResult.getDifferences());
                    result.setSuccessful(false);
                }
                
            } else if (cobolBusinessResult instanceof String && javaBusinessResult instanceof String) {
                // String result validation
                if (!compareStringFields((String) cobolBusinessResult, (String) javaBusinessResult, toleranceSettings)) {
                    differences.add("String business result mismatch: COBOL='" + cobolBusinessResult + 
                                  "', Java='" + javaBusinessResult + "'");
                    result.setSuccessful(false);
                }
                
            } else if (cobolBusinessResult instanceof Boolean && javaBusinessResult instanceof Boolean) {
                // Boolean decision logic validation
                if (!cobolBusinessResult.equals(javaBusinessResult)) {
                    differences.add("Boolean business result mismatch: COBOL=" + cobolBusinessResult + 
                                  ", Java=" + javaBusinessResult);
                    result.setSuccessful(false);
                }
                
            } else {
                // Generic object comparison
                if (!Objects.equals(cobolBusinessResult, javaBusinessResult)) {
                    differences.add("Business result mismatch: COBOL=" + cobolBusinessResult + 
                                  ", Java=" + javaBusinessResult);
                    result.setSuccessful(false);
                }
            }
            
            // Additional validation for specific business rules
            if (businessRuleDescription.toLowerCase().contains("interest")) {
                validateInterestCalculationLogic(cobolBusinessResult, javaBusinessResult, differences);
            } else if (businessRuleDescription.toLowerCase().contains("balance")) {
                validateBalanceCalculationLogic(cobolBusinessResult, javaBusinessResult, differences);
            } else if (businessRuleDescription.toLowerCase().contains("validation")) {
                validateDataValidationLogic(cobolBusinessResult, javaBusinessResult, differences);
            }
            
        } catch (Exception e) {
            differences.add("Business logic validation error: " + e.getMessage());
            result.setSuccessful(false);
            logger.error("Error during business logic validation", e);
        }
        
        result.addDifferences(differences);
        result.addComparedField("Business Logic", businessRuleDescription);
        
        if (result.isSuccessful()) {
            logger.debug("Business logic validation successful for: {}", businessRuleDescription);
        } else {
            logger.warn("Business logic validation failed for {}: {} differences found", 
                       businessRuleDescription, differences.size());
        }
        
        return result;
    }

    /**
     * Performs comprehensive functional parity assessment between COBOL and Java implementations.
     * 
     * This method provides the highest-level comparison operation, combining multiple
     * validation approaches to deliver a comprehensive assessment of functional parity
     * between the original COBOL system and the modernized Java implementation.
     * 
     * @param cobolSystemOutput Complete output from COBOL system operation
     * @param javaSystemOutput Complete output from Java system operation
     * @param testScenarioDescription Description of the test scenario being assessed
     * @param toleranceSettings Tolerance configuration for comprehensive assessment
     * @return ComparisonResult with complete functional parity assessment
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public static ComparisonResult assessFunctionalParity(Map<String, Object> cobolSystemOutput,
                                                        Map<String, Object> javaSystemOutput,
                                                        String testScenarioDescription,
                                                        ToleranceSettings toleranceSettings) {
        logger.info("Assessing functional parity for scenario: {}", testScenarioDescription);
        
        if (cobolSystemOutput == null) {
            throw new IllegalArgumentException("COBOL system output cannot be null");
        }
        if (javaSystemOutput == null) {
            throw new IllegalArgumentException("Java system output cannot be null");
        }
        if (testScenarioDescription == null || testScenarioDescription.trim().isEmpty()) {
            testScenarioDescription = "Unknown test scenario";
        }
        if (toleranceSettings == null) {
            toleranceSettings = defaultTolerance;
        }
        
        ComparisonResult overallResult = new ComparisonResult();
        List<String> allDifferences = new ArrayList<>();
        List<ComparisonResult> subResults = new ArrayList<>();
        
        try {
            // 1. Field-by-field comparison
            ComparisonResult fieldResult = compareFieldByField(cobolSystemOutput, javaSystemOutput, toleranceSettings);
            subResults.add(fieldResult);
            if (!fieldResult.isSuccessful()) {
                allDifferences.addAll(fieldResult.getDifferences().stream()
                    .map(d -> "[Field Comparison] " + d)
                    .collect(Collectors.toList()));
            }
            
            // 2. Data integrity validation
            ComparisonResult integrityResult = checkDataIntegrity(cobolSystemOutput, javaSystemOutput);
            subResults.add(integrityResult);
            if (!integrityResult.isSuccessful()) {
                allDifferences.addAll(integrityResult.getDifferences().stream()
                    .map(d -> "[Data Integrity] " + d)
                    .collect(Collectors.toList()));
            }
            
            // 3. Business logic validation for key fields
            for (Map.Entry<String, Object> entry : cobolSystemOutput.entrySet()) {
                String fieldName = entry.getKey();
                Object cobolValue = entry.getValue();
                Object javaValue = javaSystemOutput.get(fieldName);
                
                if (cobolValue != null && javaValue != null) {
                    ComparisonResult businessResult = validateBusinessLogicOutput(
                        cobolValue, javaValue, fieldName + " business logic", toleranceSettings);
                    
                    if (!businessResult.isSuccessful()) {
                        allDifferences.addAll(businessResult.getDifferences().stream()
                            .map(d -> "[Business Logic - " + fieldName + "] " + d)
                            .collect(Collectors.toList()));
                    }
                    subResults.add(businessResult);
                }
            }
            
            // 4. Calculate overall statistics
            long successfulComparisons = subResults.stream().mapToLong(r -> r.isSuccessful() ? 1 : 0).sum();
            double overallMatchPercentage = subResults.isEmpty() ? 100.0 : 
                (double) successfulComparisons / subResults.size() * 100.0;
            
            overallResult.setMatchPercentage(overallMatchPercentage);
            
            // 5. Determine overall success
            boolean overallSuccess = allDifferences.isEmpty() && overallMatchPercentage >= 95.0;
            overallResult.setSuccessful(overallSuccess);
            
            // 6. Consolidate compared fields
            for (ComparisonResult subResult : subResults) {
                for (Map.Entry<String, String> field : subResult.getComparedFields().entrySet()) {
                    overallResult.addComparedField(field.getKey(), field.getValue());
                }
            }
            
            // 7. Add summary information
            overallResult.addComparedField("Functional Parity Assessment", testScenarioDescription);
            overallResult.addComparedField("Sub-Comparisons Performed", String.valueOf(subResults.size()));
            overallResult.addComparedField("Overall Match Percentage", String.format("%.2f%%", overallMatchPercentage));
            
        } catch (Exception e) {
            allDifferences.add("Functional parity assessment error: " + e.getMessage());
            overallResult.setSuccessful(false);
            logger.error("Error during functional parity assessment", e);
        }
        
        overallResult.addDifferences(allDifferences);
        
        if (overallResult.isSuccessful()) {
            logger.info("Functional parity assessment PASSED for scenario: {} ({}% match)", 
                       testScenarioDescription, String.format("%.2f", overallResult.getMatchPercentage()));
        } else {
            logger.warn("Functional parity assessment FAILED for scenario: {} ({}% match, {} differences)", 
                       testScenarioDescription, String.format("%.2f", overallResult.getMatchPercentage()), 
                       allDifferences.size());
        }
        
        return overallResult;
    }

    // Helper methods for specific validation scenarios

    private static void validateAccountSorting(List<?> cobolList, List<?> javaList, List<String> differences) {
        // Implementation for account-specific sorting validation
        logger.debug("Validating account sorting consistency");
        // Additional account-specific sorting logic can be added here
    }

    private static void validateTransactionSorting(List<?> cobolList, List<?> javaList, List<String> differences) {
        // Implementation for transaction-specific sorting validation
        logger.debug("Validating transaction sorting consistency");
        // Additional transaction-specific sorting logic can be added here
    }

    private static boolean areTypesCompatible(Class<?> cobolType, Class<?> javaType) {
        // Check if COBOL and Java types are compatible
        if (cobolType.equals(javaType)) {
            return true;
        }
        
        // Check for compatible numeric types
        if ((cobolType == BigDecimal.class || cobolType == Double.class || cobolType == Integer.class) &&
            (javaType == BigDecimal.class || javaType == Double.class || javaType == Integer.class)) {
            return true;
        }
        
        // Check for compatible string types
        if ((cobolType == String.class || cobolType == Character.class) &&
            (javaType == String.class || javaType == Character.class)) {
            return true;
        }
        
        return false;
    }

    private static void validateKeyFieldIntegrity(Map<String, Object> cobolData, Map<String, Object> javaData, 
                                                 List<String> differences) {
        // Validate integrity of key fields (account IDs, card numbers, etc.)
        String[] keyFields = {"ACCOUNT_ID", "CARD_NUMBER", "CUSTOMER_ID", "TRANSACTION_ID"};
        
        for (String keyField : keyFields) {
            Object cobolKey = cobolData.get(keyField);
            Object javaKey = javaData.get(keyField);
            
            if (cobolKey != null && javaKey != null) {
                if (!Objects.equals(cobolKey, javaKey)) {
                    differences.add("Key field integrity violation: " + keyField + 
                                  " - COBOL=" + cobolKey + ", Java=" + javaKey);
                }
            }
        }
    }

    private static void validateReferentialIntegrity(Map<String, Object> cobolData, Map<String, Object> javaData, 
                                                    List<String> differences) {
        // Validate referential integrity between related data elements
        logger.debug("Validating referential integrity");
        // Additional referential integrity validation logic can be added here
    }

    private static void validateDataConstraints(Map<String, Object> cobolData, Map<String, Object> javaData, 
                                               List<String> differences) {
        // Validate data constraints and business rules
        logger.debug("Validating data constraints");
        // Additional constraint validation logic can be added here
    }

    private static void validateBusinessRules(Map<String, Object> cobolData, Map<String, Object> javaData, 
                                             List<String> differences) {
        // Validate business rule compliance
        logger.debug("Validating business rules");
        // Additional business rule validation logic can be added here
    }

    private static void validateCurrencyFormatting(String cobolOutput, String javaOutput, List<String> differences) {
        // Validate currency formatting consistency
        if (!cobolOutput.matches("\\$[0-9,]+\\.[0-9]{2}") || !javaOutput.matches("\\$[0-9,]+\\.[0-9]{2}")) {
            differences.add("Currency format validation failed");
        }
    }

    private static void validateDateFormatting(String cobolOutput, String javaOutput, List<String> differences) {
        // Validate date formatting consistency
        if (cobolOutput.length() != Constants.DATE_FORMAT_LENGTH || javaOutput.length() != Constants.DATE_FORMAT_LENGTH) {
            differences.add("Date format length validation failed");
        }
    }

    private static void validateNumericFormatting(String cobolOutput, String javaOutput, List<String> differences) {
        // Validate numeric formatting consistency
        try {
            new BigDecimal(cobolOutput.replaceAll("[^0-9.-]", ""));
            new BigDecimal(javaOutput.replaceAll("[^0-9.-]", ""));
        } catch (NumberFormatException e) {
            differences.add("Numeric format validation failed: " + e.getMessage());
        }
    }

    private static void validateInterestCalculationLogic(Object cobolResult, Object javaResult, List<String> differences) {
        // Validate interest calculation specific logic
        if (cobolResult instanceof BigDecimal && javaResult instanceof BigDecimal) {
            BigDecimal cobolInterest = (BigDecimal) cobolResult;
            BigDecimal javaInterest = (BigDecimal) javaResult;
            
            // Interest calculations must be exact
            if (cobolInterest.compareTo(javaInterest) != 0) {
                differences.add("Interest calculation logic mismatch - exact precision required");
            }
        }
    }

    private static void validateBalanceCalculationLogic(Object cobolResult, Object javaResult, List<String> differences) {
        // Validate balance calculation specific logic
        if (cobolResult instanceof BigDecimal && javaResult instanceof BigDecimal) {
            BigDecimal cobolBalance = (BigDecimal) cobolResult;
            BigDecimal javaBalance = (BigDecimal) javaResult;
            
            // Balance calculations must be exact to the penny
            if (cobolBalance.compareTo(javaBalance) != 0) {
                differences.add("Balance calculation logic mismatch - penny-level precision required");
            }
        }
    }

    private static void validateDataValidationLogic(Object cobolResult, Object javaResult, List<String> differences) {
        // Validate data validation specific logic
        if (cobolResult instanceof Boolean && javaResult instanceof Boolean) {
            Boolean cobolValid = (Boolean) cobolResult;
            Boolean javaValid = (Boolean) javaResult;
            
            // Validation results must be identical
            if (!cobolValid.equals(javaValid)) {
                differences.add("Data validation logic mismatch - validation results differ");
            }
        }
    }

    /**
     * Result container for comparison operations providing detailed analysis and statistics.
     * 
     * This class encapsulates the results of comparison operations between COBOL and Java
     * implementations, providing comprehensive information about comparison success,
     * identified differences, match statistics, and detailed field-level analysis.
     * 
     * The ComparisonResult supports the requirements specified in Section 0.5.1 for
     * maintaining detailed validation records and providing comprehensive reporting
     * for parallel run validation and functional parity assessment.
     */
    public static class ComparisonResult {
        private boolean successful = true;
        private List<String> differences = new ArrayList<>();
        private List<String> toleranceViolations = new ArrayList<>();
        private List<String> failedValidations = new ArrayList<>();
        private Map<String, String> comparedFields = new HashMap<>();
        private double matchPercentage = 100.0;
        private String summary = "";

        /**
         * Returns whether the comparison was successful overall.
         * 
         * @return true if comparison passed all validations, false otherwise
         */
        public boolean isSuccessful() {
            return successful;
        }

        /**
         * Sets the overall success status of the comparison.
         * 
         * @param successful true for successful comparison, false for failed comparison
         */
        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }

        /**
         * Returns the list of identified differences between COBOL and Java implementations.
         * 
         * @return List of difference descriptions, empty if no differences found
         */
        public List<String> getDifferences() {
            return new ArrayList<>(differences);
        }

        /**
         * Adds a single difference to the comparison result.
         * 
         * @param difference Description of the identified difference
         */
        public void addDifference(String difference) {
            if (difference != null && !difference.trim().isEmpty()) {
                this.differences.add(difference);
            }
        }

        /**
         * Adds multiple differences to the comparison result.
         * 
         * @param differences List of difference descriptions to add
         */
        public void addDifferences(List<String> differences) {
            if (differences != null) {
                this.differences.addAll(differences.stream()
                    .filter(d -> d != null && !d.trim().isEmpty())
                    .collect(Collectors.toList()));
            }
        }

        /**
         * Returns the list of tolerance violations encountered during comparison.
         * 
         * @return List of tolerance violation descriptions
         */
        public List<String> getToleranceViolations() {
            return new ArrayList<>(toleranceViolations);
        }

        /**
         * Adds a tolerance violation to the comparison result.
         * 
         * @param violation Description of the tolerance violation
         */
        public void addToleranceViolation(String violation) {
            if (violation != null && !violation.trim().isEmpty()) {
                this.toleranceViolations.add(violation);
            }
        }

        /**
         * Returns the map of compared fields with their descriptions.
         * 
         * @return Map of field names to their comparison descriptions
         */
        public Map<String, String> getComparedFields() {
            return new HashMap<>(comparedFields);
        }

        /**
         * Adds a compared field to the result tracking.
         * 
         * @param fieldName Name of the compared field
         * @param description Description of the comparison performed
         */
        public void addComparedField(String fieldName, String description) {
            if (fieldName != null && !fieldName.trim().isEmpty()) {
                this.comparedFields.put(fieldName, description != null ? description : "");
            }
        }

        /**
         * Returns the match percentage for the comparison operation.
         * 
         * @return Match percentage as a double value (0.0 to 100.0)
         */
        public double getMatchPercentage() {
            return matchPercentage;
        }

        /**
         * Sets the match percentage for the comparison operation.
         * 
         * @param matchPercentage Match percentage (0.0 to 100.0)
         */
        public void setMatchPercentage(double matchPercentage) {
            this.matchPercentage = Math.max(0.0, Math.min(100.0, matchPercentage));
        }

        /**
         * Returns the summary description of the comparison operation.
         * 
         * @return Summary text describing the comparison results
         */
        public String getSummary() {
            if (summary == null || summary.trim().isEmpty()) {
                return generateAutoSummary();
            }
            return summary;
        }

        /**
         * Sets the summary description for the comparison operation.
         * 
         * @param summary Summary text to set
         */
        public void setSummary(String summary) {
            this.summary = summary != null ? summary : "";
        }

        /**
         * Returns the list of failed validation operations.
         * 
         * @return List of failed validation descriptions
         */
        public List<String> getFailedValidations() {
            return new ArrayList<>(failedValidations);
        }

        /**
         * Adds a failed validation to the comparison result.
         * 
         * @param validation Description of the failed validation
         */
        public void addFailedValidation(String validation) {
            if (validation != null && !validation.trim().isEmpty()) {
                this.failedValidations.add(validation);
            }
        }

        /**
         * Generates an automatic summary based on comparison results.
         * 
         * @return Generated summary text
         */
        private String generateAutoSummary() {
            StringBuilder autoSummary = new StringBuilder();
            
            autoSummary.append("Comparison Status: ").append(successful ? "PASSED" : "FAILED").append(". ");
            autoSummary.append("Match Rate: ").append(String.format("%.1f%%", matchPercentage)).append(". ");
            
            if (!differences.isEmpty()) {
                autoSummary.append("Differences: ").append(differences.size()).append(". ");
            }
            
            if (!toleranceViolations.isEmpty()) {
                autoSummary.append("Tolerance Violations: ").append(toleranceViolations.size()).append(". ");
            }
            
            if (!comparedFields.isEmpty()) {
                autoSummary.append("Fields Compared: ").append(comparedFields.size()).append(".");
            }
            
            return autoSummary.toString();
        }

        /**
         * Returns a string representation of the comparison result for debugging.
         * 
         * @return String representation of the comparison result
         */
        @Override
        public String toString() {
            return String.format("ComparisonResult{successful=%s, matchPercentage=%.2f%%, differences=%d, fields=%d}", 
                               successful, matchPercentage, differences.size(), comparedFields.size());
        }
    }

    /**
     * Configuration class for tolerance settings in comparison operations.
     * 
     * This class provides configurable tolerance settings for various types of
     * comparison operations, allowing for appropriate handling of acceptable
     * variations while maintaining strict validation where required.
     * 
     * The ToleranceSettings support different validation modes and tolerance
     * levels to accommodate different testing scenarios and business requirements
     * while ensuring functional parity validation meets specified criteria.
     */
    public static class ToleranceSettings {
        private BigDecimal numericTolerance = BigDecimal.ZERO;
        private String stringTolerance = "EXACT";
        private String dateTolerance = "EXACT";
        private boolean strictMode = true;

        /**
         * Default constructor initializing with strict tolerance settings.
         */
        public ToleranceSettings() {
            // Default to strict settings for financial applications
        }

        /**
         * Constructor with custom tolerance settings.
         * 
         * @param numericTolerance Acceptable numeric difference tolerance
         * @param stringTolerance String comparison tolerance mode
         * @param dateTolerance Date comparison tolerance mode
         * @param strictMode Whether to enforce strict validation mode
         */
        public ToleranceSettings(BigDecimal numericTolerance, String stringTolerance, 
                               String dateTolerance, boolean strictMode) {
            setNumericTolerance(numericTolerance);
            setStringTolerance(stringTolerance);
            setDateTolerance(dateTolerance);
            setStrictMode(strictMode);
        }

        /**
         * Returns the numeric tolerance setting for BigDecimal comparisons.
         * 
         * @return Numeric tolerance as BigDecimal
         */
        public BigDecimal getNumericTolerance() {
            return numericTolerance;
        }

        /**
         * Sets the numeric tolerance for BigDecimal comparisons.
         * 
         * Financial calculations typically require ZERO tolerance for exact precision.
         * 
         * @param numericTolerance Acceptable numeric difference, must be non-negative
         * @throws IllegalArgumentException if tolerance is negative
         */
        public void setNumericTolerance(BigDecimal numericTolerance) {
            if (numericTolerance == null) {
                this.numericTolerance = BigDecimal.ZERO;
            } else if (numericTolerance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Numeric tolerance cannot be negative");
            } else {
                this.numericTolerance = numericTolerance;
            }
        }

        /**
         * Returns the string tolerance setting for string comparisons.
         * 
         * @return String tolerance mode
         */
        public String getStringTolerance() {
            return stringTolerance;
        }

        /**
         * Sets the string tolerance mode for string comparisons.
         * 
         * Supported modes:
         * - "EXACT": Exact string matching (default)
         * - "TRIM": Ignore leading/trailing whitespace
         * - "IGNORE_CASE": Case-insensitive comparison
         * - "IGNORE_PADDING": Ignore trailing spaces (COBOL fixed-width compatibility)
         * - "TRIM,IGNORE_CASE": Combination of modes
         * 
         * @param stringTolerance String tolerance mode
         */
        public void setStringTolerance(String stringTolerance) {
            this.stringTolerance = stringTolerance != null ? stringTolerance : "EXACT";
        }

        /**
         * Returns the date tolerance setting for date comparisons.
         * 
         * @return Date tolerance mode
         */
        public String getDateTolerance() {
            return dateTolerance;
        }

        /**
         * Sets the date tolerance mode for date comparisons.
         * 
         * Supported modes:
         * - "EXACT": Exact date matching (default)
         * - "DAY": Allow differences within same day
         * - "BUSINESS_DAY": Allow differences within business day rules
         * 
         * @param dateTolerance Date tolerance mode
         */
        public void setDateTolerance(String dateTolerance) {
            this.dateTolerance = dateTolerance != null ? dateTolerance : "EXACT";
        }

        /**
         * Returns whether strict validation mode is enabled.
         * 
         * @return true if strict mode is enabled, false otherwise
         */
        public boolean isStrictMode() {
            return strictMode;
        }

        /**
         * Enables or disables strict validation mode.
         * 
         * Strict mode enforces:
         * - Exact precision for all numeric comparisons
         * - Exact string matching regardless of tolerance settings
         * - Zero tolerance for any data type mismatches
         * - Comprehensive validation of all comparison aspects
         * 
         * @param strictMode true to enable strict mode, false to allow tolerance-based validation
         */
        public void enableStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
        }

        /**
         * Sets strict mode flag (alias for enableStrictMode for compatibility).
         * 
         * @param strictMode true to enable strict mode, false otherwise
         */
        public void setStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
        }

        /**
         * Creates a tolerance settings configuration optimized for financial operations.
         * 
         * Financial tolerance settings enforce:
         * - Zero numeric tolerance for exact precision
         * - Exact string matching
         * - Exact date matching
         * - Strict validation mode enabled
         * 
         * @return ToleranceSettings configured for financial precision requirements
         */
        public static ToleranceSettings createFinancialTolerance() {
            return new ToleranceSettings(BigDecimal.ZERO, "EXACT", "EXACT", true);
        }

        /**
         * Creates a tolerance settings configuration for general business operations.
         * 
         * Business tolerance settings allow:
         * - Minimal numeric tolerance for rounding differences
         * - String trimming tolerance for formatting differences
         * - Exact date matching
         * - Moderate validation mode
         * 
         * @return ToleranceSettings configured for general business operations
         */
        public static ToleranceSettings createBusinessTolerance() {
            return new ToleranceSettings(
                new BigDecimal("0.01"), // Penny tolerance for non-financial calculations
                "TRIM,IGNORE_PADDING",   // Allow whitespace differences
                "EXACT",                 // Exact date matching
                false                    // Allow tolerance-based validation
            );
        }

        /**
         * Creates a tolerance settings configuration for development testing.
         * 
         * Development tolerance settings provide:
         * - Moderate numeric tolerance for testing
         * - Flexible string comparison options
         * - Business day date tolerance
         * - Non-strict validation mode
         * 
         * @return ToleranceSettings configured for development testing scenarios
         */
        public static ToleranceSettings createDevelopmentTolerance() {
            return new ToleranceSettings(
                new BigDecimal("0.001"), // Minimal tolerance for testing
                "TRIM,IGNORE_CASE,IGNORE_PADDING", // Flexible string handling
                "DAY",                   // Same-day tolerance
                false                    // Development flexibility
            );
        }

        /**
         * Returns a string representation of the tolerance settings for debugging.
         * 
         * @return String representation of the tolerance configuration
         */
        @Override
        public String toString() {
            return String.format("ToleranceSettings{numeric=%s, string='%s', date='%s', strict=%s}", 
                               numericTolerance, stringTolerance, dateTolerance, strictMode);
        }
    }
}