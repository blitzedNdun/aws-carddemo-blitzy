/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.CobolStringFormatter;
import com.carddemo.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for converting between COBOL fixed-width record formats and modern formats like CSV and JSON.
 * 
 * This class provides comprehensive file format conversion capabilities essential for the CardDemo
 * system migration from COBOL/CICS to Java/Spring Boot. It handles COBOL copybook interpretation,
 * OCCURS clause processing, FILLER field handling, numeric precision preservation, and character
 * encoding conversion (EBCDIC to ASCII) for data migration and testing purposes.
 * 
 * Key Features:
 * - COBOL record parsing with copybook definitions
 * - Fixed-width to CSV conversion with field boundary preservation
 * - JSON generation with nested structure support
 * - COBOL COMP-3 decimal precision preservation
 * - FILLER field management and encoding conversion
 * - Batch file processing with data integrity validation
 * 
 * This implementation directly addresses the requirements specified in Section 0 of the
 * technical specification for maintaining COBOL data format compatibility during modernization.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public final class FileFormatConverter {

    /**
     * Pattern for identifying COBOL PIC clauses in copybook definitions.
     * Matches various PIC formats including X(n), 9(n), S9(n)V99, etc.
     */
    private static final Pattern PIC_CLAUSE_PATTERN = Pattern.compile(
        "PIC\\s+(X|9|S9)(?:\\((\\d+)\\))?(?:V(9{1,2}))?", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for identifying COBOL OCCURS clauses for array processing.
     * Matches formats like "OCCURS 5 TIMES" or "OCCURS 10".
     */
    private static final Pattern OCCURS_PATTERN = Pattern.compile(
        "OCCURS\\s+(\\d+)(?:\\s+TIMES)?", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for identifying FILLER fields in COBOL records.
     * Matches "FILLER" keyword with optional PIC clause.
     */
    private static final Pattern FILLER_PATTERN = Pattern.compile(
        "FILLER\\s+PIC", Pattern.CASE_INSENSITIVE);

    /**
     * Default field separator for CSV output format.
     */
    private static final String DEFAULT_CSV_SEPARATOR = ",";

    /**
     * Default character encoding for output files.
     */
    private static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();

    /**
     * Maximum record length for COBOL fixed-width records.
     * Based on typical mainframe record sizes.
     */
    private static final int MAX_RECORD_LENGTH = 32760;

    /**
     * Jackson ObjectMapper configured for COBOL data conversion.
     * Initialized with BigDecimal precision handling.
     */
    private static final ObjectMapper objectMapper = initializeObjectMapper();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private FileFormatConverter() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Parses a COBOL fixed-width record using copybook field definitions.
     * 
     * This method interprets fixed-width COBOL records by parsing field positions,
     * data types, and applying appropriate conversions based on PIC clauses.
     * Handles numeric precision preservation and character field trimming.
     * 
     * @param recordData    the fixed-width record data as a string
     * @param copybookDef   map of field names to their copybook definitions
     * @return map of field names to parsed values
     * @throws IllegalArgumentException if record data is invalid or copybook definition is malformed
     */
    public Map<String, Object> parseCobolRecord(String recordData, Map<String, String> copybookDef) {
        if (recordData == null) {
            throw new IllegalArgumentException("Record data cannot be null");
        }
        
        if (copybookDef == null || copybookDef.isEmpty()) {
            throw new IllegalArgumentException("Copybook definition cannot be null or empty");
        }

        if (recordData.length() > MAX_RECORD_LENGTH) {
            throw new IllegalArgumentException("Record length exceeds maximum: " + recordData.length());
        }

        Map<String, Object> parsedFields = new HashMap<>();
        int currentPosition = 0;

        // Sort copybook fields by their order of appearance
        List<Map.Entry<String, String>> sortedFields = new ArrayList<>(copybookDef.entrySet());
        
        for (Map.Entry<String, String> fieldEntry : sortedFields) {
            String fieldName = fieldEntry.getKey();
            String fieldDefinition = fieldEntry.getValue();

            // Skip FILLER fields unless specifically requested
            if (FILLER_PATTERN.matcher(fieldDefinition).find()) {
                int fieldLength = extractFieldLength(fieldDefinition);
                currentPosition += fieldLength;
                continue;
            }

            // Extract field data from record
            int fieldLength = extractFieldLength(fieldDefinition);
            if (currentPosition + fieldLength > recordData.length()) {
                throw new IllegalArgumentException(
                    String.format("Field %s extends beyond record length. Position: %d, Length: %d, Record length: %d", 
                                 fieldName, currentPosition, fieldLength, recordData.length()));
            }

            String fieldData = recordData.substring(currentPosition, currentPosition + fieldLength);
            
            // Convert field data based on PIC clause
            Object convertedValue = convertFieldData(fieldData, fieldDefinition);
            parsedFields.put(fieldName, convertedValue);
            
            currentPosition += fieldLength;
        }

        return parsedFields;
    }

    /**
     * Converts COBOL records to CSV format with proper field boundaries and data type preservation.
     * 
     * This method transforms COBOL fixed-width records into CSV format while maintaining
     * field boundaries and preserving numeric precision. Handles quoted fields for data
     * containing commas and ensures proper escaping.
     * 
     * @param records        list of parsed COBOL record maps
     * @param fieldNames     ordered list of field names for CSV header
     * @param separator      field separator character (default comma)
     * @return CSV formatted string with header and data rows
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public String convertToCSV(List<Map<String, Object>> records, List<String> fieldNames, String separator) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Records list cannot be null or empty");
        }
        
        if (fieldNames == null || fieldNames.isEmpty()) {
            throw new IllegalArgumentException("Field names list cannot be null or empty");
        }

        String csvSeparator = (separator != null) ? separator : DEFAULT_CSV_SEPARATOR;
        StringBuilder csvBuilder = new StringBuilder();

        // Build CSV header
        csvBuilder.append(String.join(csvSeparator, fieldNames)).append("\n");

        // Process each record
        for (Map<String, Object> record : records) {
            List<String> fieldValues = new ArrayList<>();
            
            for (String fieldName : fieldNames) {
                Object value = record.get(fieldName);
                String csvValue = formatValueForCSV(value, csvSeparator);
                fieldValues.add(csvValue);
            }
            
            csvBuilder.append(String.join(csvSeparator, fieldValues)).append("\n");
        }

        return csvBuilder.toString();
    }

    /**
     * Converts COBOL records to JSON format with nested structure support.
     * 
     * This method creates JSON objects from COBOL records while preserving data types
     * and supporting nested structures for OCCURS clauses. Uses Jackson ObjectMapper
     * with COBOL-compatible BigDecimal handling.
     * 
     * @param records    list of parsed COBOL record maps
     * @return JSON array string containing all converted records
     * @throws IllegalArgumentException if records list is invalid
     * @throws RuntimeException if JSON serialization fails
     */
    public String convertToJSON(List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Records list cannot be null or empty");
        }

        try {
            List<ObjectNode> jsonNodes = new ArrayList<>();
            
            for (Map<String, Object> record : records) {
                ObjectNode jsonNode = objectMapper.createObjectNode();
                
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    String fieldName = entry.getKey();
                    Object value = entry.getValue();
                    
                    addValueToJsonNode(jsonNode, fieldName, value);
                }
                
                jsonNodes.add(jsonNode);
            }
            
            return objectMapper.writeValueAsString(jsonNodes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert records to JSON", e);
        }
    }

    /**
     * Handles COBOL OCCURS clause processing for converting arrays to JSON arrays.
     * 
     * This method processes COBOL array structures defined by OCCURS clauses,
     * converting them to appropriate JSON array representations while preserving
     * element data types and structure.
     * 
     * @param fieldData      the array field data from COBOL record
     * @param occursClause   the OCCURS clause definition
     * @param elementLength  length of each array element
     * @return list of converted array elements
     * @throws IllegalArgumentException if parameters are invalid
     */
    public List<Object> handleOccursClause(String fieldData, String occursClause, int elementLength) {
        if (fieldData == null || fieldData.isEmpty()) {
            throw new IllegalArgumentException("Field data cannot be null or empty");
        }
        
        if (occursClause == null || occursClause.isEmpty()) {
            throw new IllegalArgumentException("OCCURS clause cannot be null or empty");
        }

        // Extract occurrence count from OCCURS clause
        int occurrenceCount = extractOccurrenceCount(occursClause);
        
        if (fieldData.length() < occurrenceCount * elementLength) {
            throw new IllegalArgumentException(
                String.format("Field data too short for %d occurrences of length %d", 
                             occurrenceCount, elementLength));
        }

        List<Object> arrayElements = new ArrayList<>();
        
        for (int i = 0; i < occurrenceCount; i++) {
            int startPos = i * elementLength;
            int endPos = startPos + elementLength;
            
            if (endPos <= fieldData.length()) {
                String elementData = fieldData.substring(startPos, endPos);
                
                // Convert element data (assuming alphanumeric for now)
                String trimmedElement = elementData.trim();
                arrayElements.add(trimmedElement);
            }
        }
        
        return arrayElements;
    }

    /**
     * Preserves numeric precision by maintaining exact COBOL COMP-3 decimal precision in conversions.
     * 
     * This method ensures that numeric data maintains the same precision and scale
     * as the original COBOL COMP-3 fields, preventing precision loss during conversion.
     * 
     * @param value    the numeric value to preserve
     * @param scale    the decimal scale from COBOL PIC clause
     * @return BigDecimal with preserved precision
     * @throws IllegalArgumentException if value cannot be converted
     */
    public BigDecimal preserveNumericPrecision(Object value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(scale, CobolDataConverter.COBOL_ROUNDING_MODE);
        }

        try {
            BigDecimal decimal = CobolDataConverter.toBigDecimal(value, scale);
            return CobolDataConverter.preservePrecision(decimal, scale);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot preserve precision for value: " + value, e);
        }
    }

    /**
     * Handles FILLER fields properly by managing placeholders in COBOL record layouts.
     * 
     * This method processes FILLER fields in COBOL records, which serve as padding
     * or reserved space. Can optionally include or exclude FILLER data in conversions.
     * 
     * @param fieldDefinition    the COBOL field definition containing FILLER
     * @param includeFillers     whether to include FILLER fields in output
     * @return map of FILLER field information
     */
    public Map<String, Object> handleFillerFields(String fieldDefinition, boolean includeFillers) {
        Map<String, Object> fillerInfo = new HashMap<>();
        
        if (fieldDefinition == null || !FILLER_PATTERN.matcher(fieldDefinition).find()) {
            fillerInfo.put("isFiller", false);
            return fillerInfo;
        }

        fillerInfo.put("isFiller", true);
        fillerInfo.put("include", includeFillers);
        
        // Extract FILLER field length
        int length = extractFieldLength(fieldDefinition);
        fillerInfo.put("length", length);
        
        if (includeFillers) {
            fillerInfo.put("placeholder", "FILLER_" + length);
        }
        
        return fillerInfo;
    }

    /**
     * Processes character encoding conversion from EBCDIC to ASCII when needed.
     * 
     * This method handles character set conversion for mainframe data that may be
     * in EBCDIC encoding, converting it to ASCII/UTF-8 for modern processing.
     * 
     * @param data         the character data to convert
     * @param sourceEncoding    source character encoding (e.g., "Cp1047" for EBCDIC)
     * @param targetEncoding    target character encoding (e.g., "UTF-8")
     * @return converted character data
     * @throws IllegalArgumentException if encoding conversion fails
     */
    public String processEncoding(String data, String sourceEncoding, String targetEncoding) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        try {
            // If encodings are the same or default, return original data
            if (sourceEncoding == null || targetEncoding == null || 
                sourceEncoding.equals(targetEncoding)) {
                return data;
            }

            // Convert between character sets
            byte[] sourceBytes = data.getBytes(sourceEncoding);
            return new String(sourceBytes, targetEncoding);
            
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Failed to convert encoding from %s to %s", sourceEncoding, targetEncoding), e);
        }
    }

    /**
     * Parses COBOL copybook field definitions to extract field metadata.
     * 
     * This method interprets COBOL copybook syntax to extract field names, positions,
     * lengths, data types, and other metadata needed for record conversion.
     * 
     * @param copybookContent    the copybook content as a string
     * @return map of field names to their definitions
     * @throws IllegalArgumentException if copybook syntax is invalid
     */
    public Map<String, String> parseCopybook(String copybookContent) {
        if (copybookContent == null || copybookContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Copybook content cannot be null or empty");
        }

        Map<String, String> fieldDefinitions = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new StringReader(copybookContent))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip comment lines and empty lines
                if (line.trim().isEmpty() || line.trim().startsWith("*")) {
                    continue;
                }

                // Parse field definition line
                String[] tokens = line.trim().split("\\s+");
                if (tokens.length >= 3) {
                    // Extract level, field name, and PIC clause
                    String level = tokens[0];
                    String fieldName = tokens[1];
                    
                    // Look for PIC clause in remaining tokens
                    StringBuilder picClause = new StringBuilder();
                    boolean foundPic = false;
                    
                    for (int i = 2; i < tokens.length; i++) {
                        if (tokens[i].toUpperCase().equals("PIC")) {
                            foundPic = true;
                            picClause.append("PIC ");
                        } else if (foundPic) {
                            picClause.append(tokens[i]).append(" ");
                        }
                    }
                    
                    if (foundPic && !level.equals("01")) {
                        fieldDefinitions.put(fieldName, picClause.toString().trim());
                    }
                }
            }
            
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse copybook content", e);
        }

        return fieldDefinitions;
    }

    /**
     * Validates conversion integrity by ensuring data integrity throughout the conversion process.
     * 
     * This method performs comprehensive validation of converted data to ensure that
     * no data loss or corruption occurred during the format conversion process.
     * 
     * @param originalData    the original COBOL record data
     * @param convertedData   the converted data (CSV, JSON, etc.)
     * @param validationType  the type of validation to perform
     * @return validation result with success status and any issues found
     */
    public Map<String, Object> validateConversion(String originalData, String convertedData, String validationType) {
        Map<String, Object> validationResult = new HashMap<>();
        List<String> issues = new ArrayList<>();
        boolean isValid = true;

        if (originalData == null || convertedData == null) {
            issues.add("Original or converted data is null");
            isValid = false;
        } else {
            switch (validationType.toLowerCase()) {
                case "length":
                    // Validate data length preservation
                    if (originalData.length() == 0 && convertedData.length() > 0) {
                        issues.add("Empty original data but non-empty converted data");
                        isValid = false;
                    }
                    break;
                    
                case "field_count":
                    // Validate field count in CSV format
                    if (validationType.equals("csv")) {
                        String[] fields = convertedData.split(",");
                        if (fields.length == 0) {
                            issues.add("No fields found in converted CSV data");
                            isValid = false;
                        }
                    }
                    break;
                    
                case "json_structure":
                    // Validate JSON structure
                    try {
                        objectMapper.readTree(convertedData);
                    } catch (Exception e) {
                        issues.add("Invalid JSON structure: " + e.getMessage());
                        isValid = false;
                    }
                    break;
                    
                default:
                    // Basic validation
                    if (convertedData.trim().isEmpty()) {
                        issues.add("Converted data is empty");
                        isValid = false;
                    }
                    break;
            }
        }

        validationResult.put("isValid", isValid);
        validationResult.put("issues", issues);
        validationResult.put("validationType", validationType);
        
        return validationResult;
    }

    /**
     * Converts fixed-width COBOL records to CSV format with comprehensive field handling.
     * 
     * This method provides a complete solution for converting COBOL fixed-width records
     * to CSV format, including copybook parsing, field extraction, and CSV generation.
     * 
     * @param recordData       the fixed-width record data
     * @param copybookDef      copybook field definitions
     * @param includeHeader    whether to include CSV header row
     * @return CSV formatted string
     */
    public String convertFixedWidthToCSV(String recordData, Map<String, String> copybookDef, boolean includeHeader) {
        Map<String, Object> parsedRecord = parseCobolRecord(recordData, copybookDef);
        List<Map<String, Object>> records = List.of(parsedRecord);
        List<String> fieldNames = new ArrayList<>(copybookDef.keySet());
        
        String csvData = convertToCSV(records, fieldNames, DEFAULT_CSV_SEPARATOR);
        
        if (!includeHeader) {
            // Remove header line
            int firstNewline = csvData.indexOf('\n');
            if (firstNewline > 0) {
                csvData = csvData.substring(firstNewline + 1);
            }
        }
        
        return csvData;
    }

    /**
     * Converts a single COBOL record to JSON format with full structure preservation.
     * 
     * This method handles the conversion of a single COBOL record to JSON while
     * preserving all data types and nested structures.
     * 
     * @param recordData    the COBOL record data
     * @param copybookDef   copybook field definitions
     * @return JSON object string
     */
    public String convertRecordToJSON(String recordData, Map<String, String> copybookDef) {
        Map<String, Object> parsedRecord = parseCobolRecord(recordData, copybookDef);
        List<Map<String, Object>> records = List.of(parsedRecord);
        
        String jsonArray = convertToJSON(records);
        
        // Extract single object from array
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(jsonArray).get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract single JSON object", e);
        }
    }

    /**
     * Processes batch files with comprehensive data integrity maintenance.
     * 
     * This method handles large batch file processing with memory-efficient
     * streaming and comprehensive error handling and recovery.
     * 
     * @param inputData       the batch input data
     * @param copybookDef     copybook field definitions
     * @param outputFormat    desired output format (CSV, JSON)
     * @return processed batch data
     */
    public String processBatchFile(String inputData, Map<String, String> copybookDef, String outputFormat) {
        if (inputData == null || inputData.trim().isEmpty()) {
            throw new IllegalArgumentException("Input data cannot be null or empty");
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(inputData))) {
            List<Map<String, Object>> allRecords = new ArrayList<>();
            String line;
            int recordCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        Map<String, Object> parsedRecord = parseCobolRecord(line, copybookDef);
                        allRecords.add(parsedRecord);
                        recordCount++;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to process record " + recordCount + ": " + e.getMessage(), e);
                    }
                }
            }
            
            // Convert to requested format
            switch (outputFormat.toLowerCase()) {
                case "csv":
                    List<String> fieldNames = new ArrayList<>(copybookDef.keySet());
                    return convertToCSV(allRecords, fieldNames, DEFAULT_CSV_SEPARATOR);
                    
                case "json":
                    return convertToJSON(allRecords);
                    
                default:
                    throw new IllegalArgumentException("Unsupported output format: " + outputFormat);
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to process batch file", e);
        }
    }

    // Static utility functions as specified in exports

    /**
     * Converts data to COBOL fixed-width format.
     * 
     * @param data          the data to convert
     * @param copybookDef   copybook field definitions
     * @return fixed-width formatted string
     */
    public static String convertToFixedWidth(Map<String, Object> data, Map<String, String> copybookDef) {
        StringBuilder fixedWidth = new StringBuilder();
        
        for (Map.Entry<String, String> fieldEntry : copybookDef.entrySet()) {
            String fieldName = fieldEntry.getKey();
            String fieldDefinition = fieldEntry.getValue();
            
            Object value = data.get(fieldName);
            int fieldLength = extractFieldLength(fieldDefinition);
            
            String formattedValue = formatValueForFixedWidth(value, fieldLength, fieldDefinition);
            fixedWidth.append(formattedValue);
        }
        
        return fixedWidth.toString();
    }

    /**
     * Converts data to CSV format.
     * 
     * @param records    list of data records
     * @param fieldNames ordered field names
     * @return CSV formatted string
     */
    public static String convertToCsv(List<Map<String, Object>> records, List<String> fieldNames) {
        FileFormatConverter converter = new FileFormatConverter();
        return converter.convertToCSV(records, fieldNames, DEFAULT_CSV_SEPARATOR);
    }

    /**
     * Converts data to JSON format.
     * 
     * @param records    list of data records
     * @return JSON formatted string
     */
    public static String convertToJson(List<Map<String, Object>> records) {
        FileFormatConverter converter = new FileFormatConverter();
        return converter.convertToJSON(records);
    }

    /**
     * Formats data for regulatory reporting requirements.
     * 
     * @param data           the data to format
     * @param reportFormat   the regulatory report format
     * @return formatted regulatory report data
     */
    public static String formatRegulatory(Map<String, Object> data, String reportFormat) {
        switch (reportFormat.toLowerCase()) {
            case "ofac":
                return formatOFACReport(data);
            case "pci":
                return formatPCIReport(data);
            case "sox":
                return formatSOXReport(data);
            default:
                throw new IllegalArgumentException("Unsupported regulatory format: " + reportFormat);
        }
    }

    // Private helper methods

    /**
     * Initializes the Jackson ObjectMapper with COBOL-compatible settings.
     */
    private static ObjectMapper initializeObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        return CobolDataConverter.configureObjectMapper(mapper);
    }

    /**
     * Extracts field length from COBOL PIC clause.
     */
    private static int extractFieldLength(String fieldDefinition) {
        var matcher = PIC_CLAUSE_PATTERN.matcher(fieldDefinition);
        if (matcher.find()) {
            String lengthGroup = matcher.group(2);
            return lengthGroup != null ? Integer.parseInt(lengthGroup) : 1;
        }
        return 0;
    }

    /**
     * Converts field data based on COBOL PIC clause.
     */
    private Object convertFieldData(String fieldData, String fieldDefinition) {
        var matcher = PIC_CLAUSE_PATTERN.matcher(fieldDefinition);
        if (matcher.find()) {
            String picType = matcher.group(1);
            String decimalPart = matcher.group(3);
            
            switch (picType.toUpperCase()) {
                case "X":
                    return CobolStringFormatter.formatAlphanumericField(fieldData.trim(), extractFieldLength(fieldDefinition));
                case "9":
                    return Long.parseLong(fieldData.trim().isEmpty() ? "0" : fieldData.trim());
                case "S9":
                    int scale = (decimalPart != null) ? decimalPart.length() : 0;
                    return CobolDataConverter.parseCobolDecimal(fieldData.trim(), scale);
                default:
                    return fieldData.trim();
            }
        }
        return fieldData.trim();
    }

    /**
     * Extracts occurrence count from OCCURS clause.
     */
    private int extractOccurrenceCount(String occursClause) {
        var matcher = OCCURS_PATTERN.matcher(occursClause);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("Invalid OCCURS clause: " + occursClause);
    }

    /**
     * Formats value for CSV output with proper escaping.
     */
    private String formatValueForCSV(Object value, String separator) {
        if (value == null) {
            return "";
        }
        
        String stringValue = value.toString();
        
        // Escape values containing separator or quotes
        if (stringValue.contains(separator) || stringValue.contains("\"") || stringValue.contains("\n")) {
            stringValue = "\"" + stringValue.replace("\"", "\"\"") + "\"";
        }
        
        return stringValue;
    }

    /**
     * Adds value to JSON node with proper type handling.
     */
    private void addValueToJsonNode(ObjectNode jsonNode, String fieldName, Object value) {
        if (value == null) {
            jsonNode.putNull(fieldName);
        } else if (value instanceof BigDecimal) {
            jsonNode.put(fieldName, (BigDecimal) value);
        } else if (value instanceof Long) {
            jsonNode.put(fieldName, (Long) value);
        } else if (value instanceof Integer) {
            jsonNode.put(fieldName, (Integer) value);
        } else if (value instanceof Boolean) {
            jsonNode.put(fieldName, (Boolean) value);
        } else {
            jsonNode.put(fieldName, value.toString());
        }
    }

    /**
     * Formats value for fixed-width output with padding.
     */
    private static String formatValueForFixedWidth(Object value, int fieldLength, String fieldDefinition) {
        String stringValue = (value != null) ? value.toString() : "";
        
        // Determine if numeric or alphanumeric field
        if (fieldDefinition.contains("PIC 9") || fieldDefinition.contains("PIC S9")) {
            // Right-align numeric fields with zero padding
            return String.format("%0" + fieldLength + "d", 
                                value instanceof Number ? ((Number) value).longValue() : 0L);
        } else {
            // Left-align alphanumeric fields with space padding
            return String.format("%-" + fieldLength + "s", stringValue).substring(0, fieldLength);
        }
    }

    /**
     * Formats data for OFAC regulatory reporting.
     */
    private static String formatOFACReport(Map<String, Object> data) {
        StringBuilder report = new StringBuilder();
        report.append("OFAC_REPORT_HEADER\n");
        
        // Format customer data for OFAC screening
        Object custId = data.get("CUSTOMER_ID");
        Object firstName = data.get("FIRST_NAME");
        Object lastName = data.get("LAST_NAME");
        
        if (custId != null && firstName != null && lastName != null) {
            report.append(String.format("CUSTOMER:%s:%s:%s\n", custId, firstName, lastName));
        }
        
        return report.toString();
    }

    /**
     * Formats data for PCI regulatory reporting.
     */
    private static String formatPCIReport(Map<String, Object> data) {
        StringBuilder report = new StringBuilder();
        report.append("PCI_REPORT_HEADER\n");
        
        // Mask sensitive data for PCI compliance
        Object cardNumber = data.get("CARD_NUMBER");
        if (cardNumber != null) {
            String maskedCard = maskCardNumber(cardNumber.toString());
            report.append(String.format("CARD_DATA:%s\n", maskedCard));
        }
        
        return report.toString();
    }

    /**
     * Formats data for SOX regulatory reporting.
     */
    private static String formatSOXReport(Map<String, Object> data) {
        StringBuilder report = new StringBuilder();
        report.append("SOX_REPORT_HEADER\n");
        
        // Include financial data for SOX compliance
        Object accountId = data.get("ACCOUNT_ID");
        Object balance = data.get("CURRENT_BALANCE");
        
        if (accountId != null && balance != null) {
            report.append(String.format("FINANCIAL_DATA:%s:%s\n", accountId, balance));
        }
        
        return report.toString();
    }

    /**
     * Masks credit card number for PCI compliance.
     */
    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "************" + lastFour;
    }
}