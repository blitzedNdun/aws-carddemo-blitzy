/*
 * BmsMessageConverter - Spring HTTP Message Converter for BMS Map Transformations
 * 
 * This converter handles the transformation of BMS (Basic Mapping Support) mapset
 * data structures to JSON format and vice versa, enabling seamless integration
 * between legacy CICS BMS screen handling and modern REST API operations.
 * 
 * Key Features:
 * - Converts BMS field structures to JSON objects for REST API responses
 * - Transforms JSON request payloads back to BMS-compatible field structures
 * - Preserves COBOL field lengths, attributes, and formatting requirements
 * - Maintains compatibility with legacy 3270 screen handling patterns
 * - Supports precise numeric conversions matching COBOL COMP-3 behavior
 * 
 * Copyright (C) 2024 CardDemo Application
 */

package com.carddemo.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Custom Spring HTTP message converter for transforming BMS mapset data structures 
 * to JSON format and vice versa. This converter bridges the gap between legacy 
 * CICS BMS screen handling and modern REST API communication patterns.
 * 
 * The converter maintains strict compatibility with COBOL data type formats,
 * field lengths, and numeric precision requirements while providing seamless
 * JSON serialization for web-based frontend consumption.
 */
@Component
public class BmsMessageConverter implements HttpMessageConverter<Object> {

    private static final MediaType APPLICATION_JSON = MediaType.APPLICATION_JSON;
    private static final List<MediaType> SUPPORTED_MEDIA_TYPES = Arrays.asList(APPLICATION_JSON);
    
    private final ObjectMapper objectMapper;
    
    // BMS field attribute constants for conversion processing
    private static final String ATTR_ASKIP = "ASKIP";
    private static final String ATTR_UNPROT = "UNPROT";
    private static final String ATTR_PROT = "PROT";
    private static final String ATTR_NUM = "NUM";
    private static final String ATTR_IC = "IC";
    private static final String ATTR_BRT = "BRT";
    private static final String ATTR_NORM = "NORM";
    private static final String ATTR_FSET = "FSET";
    
    // Color coding constants for BMS compatibility
    private static final String COLOR_GREEN = "GREEN";
    private static final String COLOR_RED = "RED";
    private static final String COLOR_YELLOW = "YELLOW";
    private static final String COLOR_BLUE = "BLUE";
    private static final String COLOR_NEUTRAL = "NEUTRAL";
    private static final String COLOR_TURQUOISE = "TURQUOISE";
    
    // Numeric field validation patterns
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    
    public BmsMessageConverter() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Determines if this converter can read the specified class type.
     * Returns true for Object types and Map types that represent BMS field structures.
     * 
     * @param clazz the class to check for read compatibility
     * @param mediaType the media type for the read operation
     * @return true if the converter can read this type, false otherwise
     */
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return (Object.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)) 
               && APPLICATION_JSON.includes(mediaType);
    }

    /**
     * Determines if this converter can write the specified class type.
     * Returns true for Object types and Map types that contain BMS field data.
     * 
     * @param clazz the class to check for write compatibility  
     * @param mediaType the media type for the write operation
     * @return true if the converter can write this type, false otherwise
     */
    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return (Object.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz))
               && APPLICATION_JSON.includes(mediaType);
    }

    /**
     * Returns the list of media types supported by this converter.
     * Currently supports application/json for BMS-to-JSON transformations.
     * 
     * @return list containing the supported MediaType objects
     */
    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    /**
     * Reads JSON input and converts it to a BMS-compatible object structure.
     * This method handles the transformation from JSON request payloads to
     * BMS field structures that maintain COBOL data type compatibility.
     * 
     * @param clazz the target class for the conversion
     * @param inputMessage the HTTP input message containing JSON data
     * @return Object containing BMS-compatible field structure
     * @throws IOException if reading the input stream fails
     * @throws HttpMessageNotReadableException if JSON parsing fails
     */
    @Override
    public Object read(Class<? extends Object> clazz, HttpInputMessage inputMessage) 
            throws IOException, HttpMessageNotReadableException {
        
        try (InputStream inputStream = inputMessage.getBody()) {
            JsonNode jsonNode = objectMapper.readTree(inputStream);
            return convertJsonToBmsStructure(jsonNode);
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors as-is for better error handling in tests
            throw e;
        } catch (Exception e) {
            throw new HttpMessageNotReadableException(
                "Failed to convert JSON to BMS structure: " + e.getMessage(), 
                inputMessage
            );
        }
    }

    /**
     * Writes a BMS object structure to JSON output format.
     * This method transforms BMS field structures to JSON responses suitable
     * for consumption by React frontend components.
     * 
     * @param object the BMS object structure to convert
     * @param contentType the content type for the response
     * @param outputMessage the HTTP output message for writing JSON
     * @throws IOException if writing to the output stream fails
     * @throws HttpMessageNotWritableException if JSON serialization fails
     */
    @Override
    public void write(Object object, MediaType contentType, HttpOutputMessage outputMessage) 
            throws IOException, HttpMessageNotWritableException {
        
        try (OutputStream outputStream = outputMessage.getBody()) {
            ObjectNode jsonOutput = convertBmsStructureToJson(object);
            
            // Set appropriate content type header
            outputMessage.getHeaders().setContentType(APPLICATION_JSON);
            
            // Write JSON to output stream
            objectMapper.writeValue(outputStream, jsonOutput);
            outputStream.flush();
            
        } catch (Exception e) {
            throw new HttpMessageNotWritableException(
                "Failed to convert BMS structure to JSON: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Converts a JSON node structure to BMS-compatible field organization.
     * This method processes JSON input and creates field structures that maintain
     * COBOL data type precision and formatting requirements.
     * 
     * @param jsonNode the input JSON node to convert
     * @return Map containing BMS field structure with proper data types
     */
    private Map<String, Object> convertJsonToBmsStructure(JsonNode jsonNode) {
        Map<String, Object> bmsStructure = new HashMap<>();
        
        if (jsonNode.isObject()) {
            jsonNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();
                
                // Convert field value based on expected BMS data type
                Object convertedValue = convertJsonValueToBmsFormat(fieldName, fieldValue);
                bmsStructure.put(fieldName, convertedValue);
            });
        }
        
        return bmsStructure;
    }

    /**
     * Converts individual JSON field values to BMS-compatible formats.
     * This method handles the precise conversion of JSON data types to maintain
     * compatibility with COBOL field definitions and COMP-3 packed decimal formats.
     * 
     * @param fieldName the name of the field being converted
     * @param fieldValue the JSON value to convert
     * @return Object with BMS-compatible formatting and precision
     */
    private Object convertJsonValueToBmsFormat(String fieldName, JsonNode fieldValue) {
        if (fieldValue.isNull()) {
            return null;
        }
        
        // Handle numeric fields with COBOL precision requirements
        if (isNumericField(fieldName)) {
            return convertToCobolNumeric(fieldValue);
        }
        
        // Handle date fields with COBOL date format requirements  
        if (isDateField(fieldName)) {
            return convertToCobolDate(fieldValue.asText());
        }
        
        // Handle character fields with proper padding and length validation
        if (fieldValue.isTextual()) {
            return convertToCobolCharacter(fieldName, fieldValue.asText());
        }
        
        // Default conversion for complex objects
        if (fieldValue.isObject() || fieldValue.isArray()) {
            return objectMapper.convertValue(fieldValue, Object.class);
        }
        
        return fieldValue.asText();
    }

    /**
     * Converts a BMS object structure to JSON format suitable for REST API responses.
     * This method transforms internal BMS field representations to JSON objects
     * that React frontend components can consume effectively.
     * 
     * @param bmsObject the BMS object structure to convert
     * @return ObjectNode containing JSON representation with proper formatting
     */
    private ObjectNode convertBmsStructureToJson(Object bmsObject) {
        ObjectNode jsonOutput = objectMapper.createObjectNode();
        
        if (bmsObject instanceof Map) {
            Map<?, ?> bmsMap = (Map<?, ?>) bmsObject;
            
            bmsMap.forEach((key, value) -> {
                String fieldName = String.valueOf(key);
                
                if (value != null) {
                    // Convert BMS field value to appropriate JSON representation
                    Object jsonValue = convertBmsValueToJsonFormat(fieldName, value);
                    addValueToJsonNode(jsonOutput, fieldName, jsonValue);
                }
            });
        } else {
            // Handle direct object conversion using Jackson's default mapping
            JsonNode objectNode = objectMapper.valueToTree(bmsObject);
            if (objectNode.isObject()) {
                objectNode.fields().forEachRemaining(entry -> {
                    jsonOutput.set(entry.getKey(), entry.getValue());
                });
            }
        }
        
        return jsonOutput;
    }

    /**
     * Converts BMS field values to JSON-compatible formats with proper type handling.
     * This method ensures that COBOL data types are properly represented in JSON
     * while maintaining precision and formatting requirements.
     * 
     * @param fieldName the name of the field being converted
     * @param bmsValue the BMS field value to convert
     * @return Object containing JSON-compatible representation
     */
    private Object convertBmsValueToJsonFormat(String fieldName, Object bmsValue) {
        if (bmsValue == null) {
            return null;
        }
        
        // Handle BigDecimal numeric values with proper scale preservation
        if (bmsValue instanceof BigDecimal) {
            return formatNumericForJson((BigDecimal) bmsValue);
        }
        
        // Handle numeric string values that represent COBOL packed decimals
        if (bmsValue instanceof String && isNumericField(fieldName)) {
            try {
                BigDecimal numericValue = new BigDecimal((String) bmsValue);
                return formatNumericForJson(numericValue);
            } catch (NumberFormatException e) {
                // Return original string if not a valid number
                return bmsValue;
            }
        }
        
        // Handle date field formatting for JSON consumption
        if (isDateField(fieldName) && bmsValue instanceof String) {
            return formatDateForJson((String) bmsValue);
        }
        
        // Handle character fields with proper trimming and formatting
        if (bmsValue instanceof String) {
            return formatCharacterForJson((String) bmsValue);
        }
        
        return bmsValue;
    }

    /**
     * Adds a value to the JSON object node with appropriate type conversion.
     * This method handles the proper insertion of converted values into the
     * JSON output structure while maintaining type safety.
     * 
     * @param jsonNode the target JSON object node
     * @param fieldName the name of the field to add
     * @param value the value to add to the JSON structure
     */
    private void addValueToJsonNode(ObjectNode jsonNode, String fieldName, Object value) {
        if (value == null) {
            jsonNode.putNull(fieldName);
        } else if (value instanceof String) {
            jsonNode.put(fieldName, (String) value);
        } else if (value instanceof Integer) {
            jsonNode.put(fieldName, (Integer) value);
        } else if (value instanceof Long) {
            jsonNode.put(fieldName, (Long) value);
        } else if (value instanceof BigDecimal) {
            jsonNode.put(fieldName, (BigDecimal) value);
        } else if (value instanceof Boolean) {
            jsonNode.put(fieldName, (Boolean) value);
        } else {
            // Handle complex objects by converting to JSON node
            JsonNode valueNode = objectMapper.valueToTree(value);
            jsonNode.set(fieldName, valueNode);
        }
    }

    /**
     * Determines if a field name represents a numeric field based on BMS conventions.
     * This method identifies fields that require numeric validation and precision handling.
     * 
     * @param fieldName the name of the field to check
     * @return true if the field should be treated as numeric, false otherwise
     */
    private boolean isNumericField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String upperFieldName = fieldName.toUpperCase();
        
        // Check for common numeric field patterns from BMS maps with more precise matching
        return upperFieldName.contains("AMT") ||       // Amount fields
               upperFieldName.contains("BAL") ||       // Balance fields  
               upperFieldName.endsWith("NUM") ||       // Number fields (ending with NUM)
               upperFieldName.endsWith("ID") ||        // ID fields (ending with ID like accountId, transactionId)
               upperFieldName.contains("LIMIT") ||     // Credit limit fields
               upperFieldName.contains("SCORE") ||     // FICO score fields
               upperFieldName.contains("RATE") ||      // Interest rate fields
               upperFieldName.equals("OPTION") ||      // Menu option field
               upperFieldName.matches(".*\\d+.*") ||   // Fields containing digits
               (upperFieldName.startsWith("ACCOUNT") && upperFieldName.endsWith("ID")) ||
               (upperFieldName.startsWith("TRANSACTION") && upperFieldName.endsWith("ID")) ||
               (upperFieldName.startsWith("CARD") && upperFieldName.endsWith("ID")) ||
               upperFieldName.equals("ACCOUNTID") ||
               upperFieldName.equals("TRANSACTIONID") ||
               upperFieldName.equals("CARDID");
    }

    /**
     * Determines if a field name represents a date field based on BMS conventions.
     * This method identifies fields that require date formatting and validation.
     * 
     * @param fieldName the name of the field to check
     * @return true if the field should be treated as a date, false otherwise
     */
    private boolean isDateField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String upperFieldName = fieldName.toUpperCase();
        
        // Check for common date field patterns from BMS maps
        return upperFieldName.contains("DATE") ||    // Date fields
               upperFieldName.contains("TIME") ||    // Time fields
               upperFieldName.equals("CURDATE") ||   // Current date
               upperFieldName.equals("CURTIME");     // Current time
    }

    /**
     * Converts JSON numeric values to COBOL-compatible BigDecimal format.
     * This method ensures that numeric precision matches COBOL COMP-3 packed decimal behavior.
     * 
     * @param jsonValue the JSON numeric value to convert
     * @return BigDecimal with appropriate scale and rounding mode
     */
    private BigDecimal convertToCobolNumeric(JsonNode jsonValue) {
        if (jsonValue.isNumber()) {
            BigDecimal value = jsonValue.decimalValue();
            
            // Apply COBOL rounding behavior (HALF_UP) and appropriate scale
            return value.setScale(2, RoundingMode.HALF_UP);
        } else if (jsonValue.isTextual() && NUMERIC_PATTERN.matcher(jsonValue.asText()).matches()) {
            try {
                BigDecimal value = new BigDecimal(jsonValue.asText());
                return value.setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid numeric value: " + jsonValue.asText());
            }
        }
        
        throw new IllegalArgumentException("Field value is not numeric: " + jsonValue);
    }

    /**
     * Converts date string values to COBOL-compatible date format.
     * This method handles date conversion while maintaining COBOL date format requirements.
     * 
     * @param dateValue the date string to convert
     * @return String containing COBOL-formatted date
     */
    private String convertToCobolDate(String dateValue) {
        if (dateValue == null || dateValue.trim().isEmpty()) {
            return "";
        }
        
        // Handle various input date formats and convert to COBOL format
        String cleanDate = dateValue.trim();
        
        // If already in COBOL format (CCYYMMDD or MM/DD/YY), return as-is
        if (cleanDate.matches("\\d{8}") || cleanDate.matches("\\d{2}/\\d{2}/\\d{2}")) {
            return cleanDate;
        }
        
        // Additional date format conversion logic could be added here
        // For now, return the input value if it doesn't match expected patterns
        return cleanDate;
    }

    /**
     * Converts character field values to COBOL-compatible format with proper padding.
     * This method ensures character fields meet COBOL length and format requirements.
     * 
     * @param fieldName the name of the character field
     * @param characterValue the character value to convert
     * @return String with proper COBOL formatting and padding
     */
    private String convertToCobolCharacter(String fieldName, String characterValue) {
        if (characterValue == null) {
            return "";
        }
        
        String processedValue = characterValue.trim();
        
        // Apply field-specific formatting based on field name
        if (fieldName != null) {
            String upperFieldName = fieldName.toUpperCase();
            
            // Handle specific character field formatting requirements
            if (upperFieldName.equals("USERID") || upperFieldName.equals("PASSWD")) {
                // User ID and password fields - uppercase and limited to 8 characters
                processedValue = processedValue.toUpperCase();
                if (processedValue.length() > 8) {
                    processedValue = processedValue.substring(0, 8);
                }
            } else if (upperFieldName.contains("NAME")) {
                // Name fields - proper case formatting
                processedValue = properCase(processedValue);
            }
        }
        
        return processedValue;
    }

    /**
     * Formats numeric values for JSON output with appropriate precision.
     * This method ensures that numeric values maintain proper decimal places for frontend display.
     * 
     * @param numericValue the BigDecimal value to format
     * @return Object containing properly formatted numeric value for JSON
     */
    private Object formatNumericForJson(BigDecimal numericValue) {
        if (numericValue == null) {
            return null;
        }
        
        // For integer values, return as integer type
        if (numericValue.scale() <= 0 || numericValue.remainder(BigDecimal.ONE).equals(BigDecimal.ZERO)) {
            try {
                return numericValue.intValueExact();
            } catch (ArithmeticException e) {
                // Value too large for int, return as long
                return numericValue.longValue();
            }
        }
        
        // For decimal values, return with appropriate precision
        return numericValue.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Formats date values for JSON output with consistent date representation.
     * This method converts COBOL date formats to JSON-friendly date strings.
     * 
     * @param dateValue the date string to format
     * @return String containing JSON-compatible date format
     */
    private String formatDateForJson(String dateValue) {
        if (dateValue == null || dateValue.trim().isEmpty()) {
            return "";
        }
        
        String cleanDate = dateValue.trim();
        
        // Convert COBOL date formats to more readable JSON format
        if (cleanDate.matches("\\d{8}")) {
            // Convert CCYYMMDD to MM/DD/YYYY format
            String year = cleanDate.substring(0, 4);
            String month = cleanDate.substring(4, 6);
            String day = cleanDate.substring(6, 8);
            return month + "/" + day + "/" + year;
        }
        
        // Return original format if no conversion needed
        return cleanDate;
    }

    /**
     * Formats character values for JSON output with proper trimming and casing.
     * This method ensures character fields are properly formatted for frontend display.
     * 
     * @param characterValue the character string to format
     * @return String containing properly formatted character value
     */
    private String formatCharacterForJson(String characterValue) {
        if (characterValue == null) {
            return "";
        }
        
        // Trim whitespace and handle empty values
        String trimmed = characterValue.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        
        return trimmed;
    }

    /**
     * Converts a string to proper case (first letter of each word capitalized).
     * This utility method provides consistent formatting for name fields.
     * 
     * @param input the input string to convert
     * @return String with proper case formatting
     */
    private String properCase(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        
        return result.toString();
    }
}