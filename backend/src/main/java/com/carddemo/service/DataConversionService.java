/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carddemo.util.CobolDataConverter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Service class that provides high-level data conversion operations for COBOL to Java migration.
 * 
 * This service orchestrates the CobolDataConverter utilities and adds comprehensive business logic
 * validation to ensure data integrity during the CardDemo system migration from COBOL/CICS to
 * Java/Spring Boot. It implements conversion tracking, error handling, and validation rules
 * derived from the original COBOL copybooks and procedures.
 * 
 * Key Features:
 * - COMP-3 packed decimal conversions with scale preservation
 * - EBCDIC to ASCII character translations
 * - Date validation based on CSUTLDPY.cpy logic (year/month/day/leap year validation)
 * - Numeric precision handling for financial calculations
 * - Bulk data transformation operations
 * - Comprehensive error handling for invalid data formats
 * - Detailed logging for conversion tracking and auditing
 * 
 * This implementation directly supports the requirements specified in Section 0.1.2 of the
 * technical specification for maintaining "exact decimal precision matching in financial calculations"
 * and preserving COBOL data conversion behavior.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
public class DataConversionService {

    private static final Logger logger = LoggerFactory.getLogger(DataConversionService.class);

    /**
     * COBOL date format pattern for CCYYMMDD (Century, Year, Month, Day).
     * Used for parsing and validating dates from COBOL systems.
     */
    private static final String COBOL_DATE_FORMAT = "yyyyMMdd";
    
    /**
     * Date formatter for COBOL CCYYMMDD format.
     */
    private static final DateTimeFormatter COBOL_DATE_FORMATTER = DateTimeFormatter.ofPattern(COBOL_DATE_FORMAT);

    /**
     * Pattern for validating COBOL CCYYMMDD date strings.
     * Ensures exactly 8 digits representing century, year, month, and day.
     */
    private static final Pattern COBOL_DATE_PATTERN = Pattern.compile("^\\d{8}$");

    /**
     * Pattern for validating numeric fields - digits only with optional sign.
     */
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[+-]?\\d*\\.?\\d*$");

    /**
     * EBCDIC to ASCII character mapping for common COBOL characters.
     * Based on standard EBCDIC code page 037 used in mainframe systems.
     */
    private static final Map<Character, Character> EBCDIC_TO_ASCII_MAP = createEbcdicToAsciiMap();

    /**
     * Converts COBOL COMP-3 packed decimal data to Java BigDecimal with exact precision preservation.
     * 
     * This method provides high-level orchestration of the CobolDataConverter.fromComp3() utility,
     * adding business logic validation, error handling, and conversion tracking. It ensures that
     * financial calculations maintain identical precision to the COBOL implementation.
     * 
     * @param packedData byte array containing COMP-3 packed decimal data from COBOL
     * @param scale      number of decimal places (typically 2 for monetary amounts)
     * @param fieldName  descriptive name of the field being converted (for logging)
     * @return BigDecimal with exact precision and scale matching COBOL behavior
     * @throws IllegalArgumentException if conversion fails or data is invalid
     */
    public BigDecimal convertComp3ToBigDecimal(byte[] packedData, int scale, String fieldName) {
        logger.debug("Converting COMP-3 field '{}' with scale {} and {} bytes", 
                    fieldName != null ? fieldName : "unknown", scale, 
                    packedData != null ? packedData.length : 0);

        try {
            if (packedData == null || packedData.length == 0) {
                logger.warn("Empty or null COMP-3 data for field '{}', returning zero", fieldName);
                return BigDecimal.ZERO.setScale(scale, CobolDataConverter.COBOL_ROUNDING_MODE);
            }

            // Use CobolDataConverter utility for the actual conversion
            BigDecimal result = CobolDataConverter.fromComp3(packedData, scale);
            
            logger.debug("Successfully converted COMP-3 field '{}' to BigDecimal: {}", fieldName, result);
            return result;

        } catch (Exception e) {
            logger.error("Failed to convert COMP-3 field '{}': {}", fieldName, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("COMP-3 conversion failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Converts EBCDIC character data to ASCII encoding for Java string processing.
     * 
     * This method handles character set conversion from EBCDIC (Extended Binary Coded Decimal
     * Interchange Code) used in mainframe systems to ASCII/UTF-8 used in Java applications.
     * It processes character-by-character mapping for accurate conversion of special characters
     * and symbols commonly found in COBOL data.
     * 
     * @param ebcdicData byte array containing EBCDIC encoded character data
     * @param fieldName  descriptive name of the field being converted (for logging)
     * @return ASCII/UTF-8 encoded string
     * @throws IllegalArgumentException if conversion fails
     */
    public String convertEbcdicToAscii(byte[] ebcdicData, String fieldName) {
        logger.debug("Converting EBCDIC field '{}' with {} bytes to ASCII", 
                    fieldName != null ? fieldName : "unknown", 
                    ebcdicData != null ? ebcdicData.length : 0);

        try {
            if (ebcdicData == null || ebcdicData.length == 0) {
                logger.debug("Empty EBCDIC data for field '{}', returning empty string", fieldName);
                return "";
            }

            // Convert EBCDIC bytes to string using character mapping
            StringBuilder result = new StringBuilder();
            for (byte b : ebcdicData) {
                char ebcdicChar = (char) (b & 0xFF);
                char asciiChar = EBCDIC_TO_ASCII_MAP.getOrDefault(ebcdicChar, ebcdicChar);
                result.append(asciiChar);
            }

            String converted = result.toString().trim(); // Remove trailing spaces common in COBOL
            logger.debug("Successfully converted EBCDIC field '{}' to ASCII: '{}'", fieldName, converted);
            return converted;

        } catch (Exception e) {
            logger.error("Failed to convert EBCDIC field '{}': {}", fieldName, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("EBCDIC to ASCII conversion failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Validates and converts COBOL CCYYMMDD date strings using CSUTLDPY.cpy validation logic.
     * 
     * This method implements the comprehensive date validation procedures from CSUTLDPY.cpy,
     * including century validation (19xx or 20xx), month range checking (01-12), day range
     * validation (01-31), leap year calculations for February, and month-specific day limits.
     * It replicates the exact validation logic used in the original COBOL programs.
     * 
     * @param dateString COBOL CCYYMMDD format date string (8 digits)
     * @param fieldName  descriptive name of the date field (for error messages)
     * @return LocalDate object representing the validated date
     * @throws IllegalArgumentException if date is invalid or malformed
     */
    public LocalDate validateAndConvertDate(String dateString, String fieldName) {
        logger.debug("Validating COBOL date field '{}' with value: '{}'", 
                    fieldName != null ? fieldName : "unknown", dateString);

        try {
            // Validate input format - must be exactly 8 digits
            if (dateString == null || dateString.trim().isEmpty()) {
                throw new IllegalArgumentException("Date field cannot be null or empty");
            }

            String trimmedDate = dateString.trim();
            
            if (!COBOL_DATE_PATTERN.matcher(trimmedDate).matches()) {
                throw new IllegalArgumentException("Date must be 8 digits in CCYYMMDD format");
            }

            // Extract components following CSUTLDPY.cpy structure
            int century = Integer.parseInt(trimmedDate.substring(0, 2));
            int year = Integer.parseInt(trimmedDate.substring(2, 4));
            int month = Integer.parseInt(trimmedDate.substring(4, 6));
            int day = Integer.parseInt(trimmedDate.substring(6, 8));
            
            int fullYear = century * 100 + year;

            // Century validation - only 19 and 20 are valid (CSUTLDPY.cpy lines 70-84)
            if (century != 19 && century != 20) {
                throw new IllegalArgumentException("Century is not valid - only 19 and 20 are acceptable");
            }

            // Month validation (CSUTLDPY.cpy lines 91-143)
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Month must be a number between 1 and 12");
            }

            // Day validation (CSUTLDPY.cpy lines 150-201)
            if (day < 1 || day > 31) {
                throw new IllegalArgumentException("Day must be a number between 1 and 31");
            }

            // Month-specific day validation (CSUTLDPY.cpy lines 213-272)
            if (!isValidDayForMonth(day, month, fullYear)) {
                throw new IllegalArgumentException(
                    String.format("Day %d is not valid for month %d in year %d", day, month, fullYear));
            }

            // Convert to LocalDate for final validation
            LocalDate result = LocalDate.of(fullYear, month, day);
            
            logger.debug("Successfully validated and converted date field '{}' to: {}", fieldName, result);
            return result;

        } catch (DateTimeParseException e) {
            logger.error("Date parsing failed for field '{}' with value '{}': {}", fieldName, dateString, e.getMessage());
            throw new IllegalArgumentException(
                String.format("Invalid date format for field '%s': %s", fieldName, e.getMessage()), e);
        } catch (Exception e) {
            logger.error("Date validation failed for field '{}' with value '{}': {}", fieldName, dateString, e.getMessage());
            throw new IllegalArgumentException(
                String.format("Date validation failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Converts COBOL numeric fields to appropriate Java types with precision handling.
     * 
     * This method orchestrates the conversion of various COBOL numeric field types including
     * PIC 9, PIC S9, and COMP-3 fields to Java BigDecimal with proper scale and precision
     * preservation. It uses the CobolDataConverter utilities while adding business validation.
     * 
     * @param value     raw numeric value from COBOL field
     * @param picClause COBOL PIC clause specification (e.g., "PIC S9(10)V99")
     * @param fieldName descriptive name of the field (for logging)
     * @return BigDecimal with appropriate scale and precision
     * @throws IllegalArgumentException if conversion fails or value is invalid
     */
    public BigDecimal convertNumericField(Object value, String picClause, String fieldName) {
        logger.debug("Converting numeric field '{}' with PIC clause '{}' and value: {}", 
                    fieldName != null ? fieldName : "unknown", picClause, value);

        try {
            if (value == null) {
                logger.debug("Null value for numeric field '{}', returning zero", fieldName);
                return BigDecimal.ZERO.setScale(CobolDataConverter.DEFAULT_SCALE, CobolDataConverter.COBOL_ROUNDING_MODE);
            }

            // Use CobolDataConverter to handle the conversion based on PIC clause
            Object converted = CobolDataConverter.convertToJavaType(value, picClause);
            
            // Ensure result is BigDecimal with proper precision
            BigDecimal result;
            if (converted instanceof BigDecimal) {
                result = (BigDecimal) converted;
            } else {
                result = CobolDataConverter.toBigDecimal(converted, CobolDataConverter.DEFAULT_SCALE);
            }

            // Apply precision preservation
            result = CobolDataConverter.preservePrecision(result, result.scale());
            
            logger.debug("Successfully converted numeric field '{}' to BigDecimal: {}", fieldName, result);
            return result;

        } catch (Exception e) {
            logger.error("Failed to convert numeric field '{}' with PIC '{}': {}", fieldName, picClause, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("Numeric conversion failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Performs bulk data conversion operations for processing multiple COBOL fields efficiently.
     * 
     * This method orchestrates the conversion of multiple data fields in a single operation,
     * providing transaction-level consistency and comprehensive error tracking. It's designed
     * for processing entire COBOL records or copybook structures during data migration.
     * 
     * @param dataMap    Map of field names to raw COBOL data values
     * @param schemaMap  Map of field names to PIC clause specifications
     * @param recordName descriptive name of the record being processed (for logging)
     * @return Map of field names to converted Java objects
     * @throws IllegalArgumentException if any conversion fails
     */
    public Map<String, Object> convertBulkData(Map<String, Object> dataMap, 
                                               Map<String, String> schemaMap, 
                                               String recordName) {
        logger.info("Starting bulk conversion for record '{}' with {} fields", 
                   recordName != null ? recordName : "unknown", 
                   dataMap != null ? dataMap.size() : 0);

        Map<String, Object> results = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            if (dataMap == null || dataMap.isEmpty()) {
                logger.warn("Empty data map for bulk conversion of record '{}'", recordName);
                return results;
            }

            if (schemaMap == null || schemaMap.isEmpty()) {
                logger.warn("Empty schema map for bulk conversion of record '{}'", recordName);
                return results;
            }

            int successCount = 0;
            int errorCount = 0;

            // Process each field in the data map
            for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();
                String picClause = schemaMap.get(fieldName);

                try {
                    if (picClause == null) {
                        logger.warn("No PIC clause found for field '{}' in record '{}'", fieldName, recordName);
                        results.put(fieldName, fieldValue); // Pass through unchanged
                        continue;
                    }

                    // Determine conversion type based on PIC clause
                    Object converted;
                    if (picClause.toUpperCase().contains("COMP-3")) {
                        // Handle COMP-3 packed decimal
                        if (fieldValue instanceof byte[]) {
                            int scale = extractScaleFromPic(picClause);
                            converted = convertComp3ToBigDecimal((byte[]) fieldValue, scale, fieldName);
                        } else {
                            throw new IllegalArgumentException("COMP-3 field must be byte array");
                        }
                    } else if (picClause.toUpperCase().contains("PIC 9") || picClause.toUpperCase().contains("PIC S9")) {
                        // Handle numeric fields
                        converted = convertNumericField(fieldValue, picClause, fieldName);
                    } else if (picClause.toUpperCase().contains("PIC X")) {
                        // Handle character fields
                        converted = convertCharacterField(fieldValue, picClause, fieldName);
                    } else {
                        // Use general conversion
                        converted = CobolDataConverter.convertToJavaType(fieldValue, picClause);
                    }

                    results.put(fieldName, converted);
                    successCount++;
                    
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = String.format("Field '%s': %s", fieldName, e.getMessage());
                    errors.add(errorMsg);
                    logger.error("Conversion failed for field '{}' in record '{}': {}", 
                               fieldName, recordName, e.getMessage());
                }
            }

            logger.info("Bulk conversion completed for record '{}': {} successful, {} errors", 
                       recordName, successCount, errorCount);

            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Bulk conversion failed for record '%s' with %d errors: %s", 
                                 recordName, errors.size(), String.join("; ", errors)));
            }

            return results;

        } catch (Exception e) {
            logger.error("Bulk conversion failed for record '{}': {}", recordName, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("Bulk data conversion failed for record '%s': %s", recordName, e.getMessage()), e);
        }
    }

    /**
     * Validates numeric precision for BigDecimal values to ensure COBOL compatibility.
     * 
     * This method verifies that BigDecimal values maintain the exact precision and scale
     * requirements specified in COBOL PIC clauses. It uses CobolDataConverter.preservePrecision()
     * to ensure consistency with COBOL COMP-3 and numeric field behavior.
     * 
     * @param value         BigDecimal value to validate
     * @param requiredScale required number of decimal places
     * @param maxPrecision  maximum total number of digits allowed
     * @param fieldName     descriptive name of the field (for logging)
     * @return validated BigDecimal with corrected precision and scale
     * @throws IllegalArgumentException if precision requirements cannot be met
     */
    public BigDecimal validateNumericPrecision(BigDecimal value, int requiredScale, int maxPrecision, String fieldName) {
        logger.debug("Validating numeric precision for field '{}': value={}, scale={}, maxPrecision={}", 
                    fieldName != null ? fieldName : "unknown", value, requiredScale, maxPrecision);

        try {
            if (value == null) {
                logger.debug("Null value for field '{}', returning zero with required scale", fieldName);
                return BigDecimal.ZERO.setScale(requiredScale, CobolDataConverter.COBOL_ROUNDING_MODE);
            }

            // Check total precision (number of digits)
            int actualPrecision = value.precision();
            if (actualPrecision > maxPrecision) {
                throw new IllegalArgumentException(
                    String.format("Value precision %d exceeds maximum allowed %d for field '%s'", 
                                 actualPrecision, maxPrecision, fieldName));
            }

            // Use CobolDataConverter to preserve precision with required scale
            BigDecimal result = CobolDataConverter.preservePrecision(value, requiredScale);
            
            logger.debug("Successfully validated precision for field '{}': {}", fieldName, result);
            return result;

        } catch (Exception e) {
            logger.error("Precision validation failed for field '{}': {}", fieldName, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("Numeric precision validation failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Converts COBOL string fields using CobolDataConverter with business validation.
     * 
     * This method orchestrates the conversion of COBOL PIC X fields to Java strings,
     * applying proper trimming, length validation, and character encoding handling.
     * It uses CobolDataConverter.convertPicString() for the core conversion logic.
     * 
     * @param value     raw string value from COBOL field
     * @param picClause COBOL PIC X clause specification (e.g., "PIC X(25)")
     * @param fieldName descriptive name of the field (for logging)
     * @return processed and validated string
     * @throws IllegalArgumentException if conversion fails or value is invalid
     */
    public String convertCobolString(Object value, String picClause, String fieldName) {
        logger.debug("Converting COBOL string field '{}' with PIC clause '{}' and value: '{}'", 
                    fieldName != null ? fieldName : "unknown", picClause, value);

        try {
            if (value == null) {
                logger.debug("Null value for string field '{}', returning empty string", fieldName);
                return "";
            }

            String stringValue = value.toString();

            // Extract maximum length from PIC clause
            int maxLength = extractLengthFromPicX(picClause);
            
            // Use CobolDataConverter for the actual string conversion
            String result = CobolDataConverter.convertPicString(stringValue, maxLength);
            
            logger.debug("Successfully converted COBOL string field '{}': '{}'", fieldName, result);
            return result;

        } catch (Exception e) {
            logger.error("Failed to convert COBOL string field '{}': {}", fieldName, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("COBOL string conversion failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Handles signed numeric conversions with proper sign bit processing.
     * 
     * This method processes COBOL signed numeric fields (PIC S9) including those with
     * decimal positions, maintaining exact sign information and precision for financial
     * calculations. It handles both positive and negative values with COBOL-compatible
     * sign representation.
     * 
     * @param value     raw signed numeric value
     * @param picClause COBOL PIC S9 clause specification
     * @param fieldName descriptive name of the field (for logging)
     * @return BigDecimal with proper sign and precision
     * @throws IllegalArgumentException if conversion fails or value is invalid
     */
    public BigDecimal handleSignedNumeric(Object value, String picClause, String fieldName) {
        logger.debug("Handling signed numeric field '{}' with PIC clause '{}' and value: {}", 
                    fieldName != null ? fieldName : "unknown", picClause, value);

        try {
            if (value == null) {
                logger.debug("Null value for signed numeric field '{}', returning zero", fieldName);
                return BigDecimal.ZERO.setScale(CobolDataConverter.DEFAULT_SCALE, CobolDataConverter.COBOL_ROUNDING_MODE);
            }

            // Validate that this is a signed numeric field
            if (picClause == null || !picClause.toUpperCase().contains("PIC S9")) {
                throw new IllegalArgumentException("PIC clause must specify signed numeric (PIC S9) format");
            }

            // Use CobolDataConverter for signed numeric conversion
            Object converted = CobolDataConverter.convertToJavaType(value, picClause);
            
            BigDecimal result;
            if (converted instanceof BigDecimal) {
                result = (BigDecimal) converted;
            } else {
                result = CobolDataConverter.toBigDecimal(converted, CobolDataConverter.DEFAULT_SCALE);
            }

            logger.debug("Successfully handled signed numeric field '{}': {}", fieldName, result);
            return result;

        } catch (Exception e) {
            logger.error("Failed to handle signed numeric field '{}': {}", fieldName, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("Signed numeric conversion failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Converts COMP-3 packed decimal with alternate interface for different use cases.
     * 
     * This method provides an alternative interface to convertComp3ToBigDecimal() with
     * additional validation and error handling specific to packed decimal processing.
     * It includes enhanced logging and supports different scale determination strategies.
     * 
     * @param packedData   byte array containing COMP-3 packed decimal data
     * @param totalDigits  total number of digits in the packed field
     * @param decimalPlaces number of decimal places
     * @param fieldName    descriptive name of the field (for logging)
     * @return BigDecimal with exact precision matching COBOL COMP-3 behavior
     * @throws IllegalArgumentException if conversion fails
     */
    public BigDecimal convertPackedDecimal(byte[] packedData, int totalDigits, int decimalPlaces, String fieldName) {
        logger.debug("Converting packed decimal field '{}' with {} total digits, {} decimal places, {} bytes", 
                    fieldName != null ? fieldName : "unknown", totalDigits, decimalPlaces,
                    packedData != null ? packedData.length : 0);

        try {
            if (packedData == null || packedData.length == 0) {
                logger.warn("Empty packed decimal data for field '{}', returning zero", fieldName);
                return BigDecimal.ZERO.setScale(decimalPlaces, CobolDataConverter.COBOL_ROUNDING_MODE);
            }

            // Validate packed decimal structure
            int expectedBytes = (totalDigits + 1) / 2;
            if (packedData.length != expectedBytes) {
                logger.warn("Packed decimal length mismatch for field '{}': expected {} bytes, got {}", 
                           fieldName, expectedBytes, packedData.length);
            }

            // Use CobolDataConverter for the conversion
            BigDecimal result = CobolDataConverter.fromComp3(packedData, decimalPlaces);

            // Validate total digits don't exceed specification
            if (result.precision() > totalDigits) {
                logger.warn("Packed decimal result precision {} exceeds specified total digits {} for field '{}'", 
                           result.precision(), totalDigits, fieldName);
            }

            logger.debug("Successfully converted packed decimal field '{}': {}", fieldName, result);
            return result;

        } catch (Exception e) {
            logger.error("Failed to convert packed decimal field '{}': {}", fieldName, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("Packed decimal conversion failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Validates date of birth fields ensuring no future dates (CSUTLDPY.cpy logic).
     * 
     * This method implements the date of birth validation logic from CSUTLDPY.cpy
     * (lines 341-372) which ensures that birth dates cannot be in the future.
     * It performs comprehensive date validation and business rule checking.
     * 
     * @param dateString COBOL CCYYMMDD format date string
     * @param fieldName  descriptive name of the date field (for logging)
     * @return LocalDate object representing the validated birth date
     * @throws IllegalArgumentException if date is invalid or in the future
     */
    public LocalDate validateDateOfBirth(String dateString, String fieldName) {
        logger.debug("Validating date of birth field '{}' with value: '{}'", 
                    fieldName != null ? fieldName : "unknown", dateString);

        try {
            // First perform standard date validation
            LocalDate birthDate = validateAndConvertDate(dateString, fieldName);
            
            // Get current date for comparison (CSUTLDPY.cpy line 343)
            LocalDate currentDate = LocalDate.now();
            
            // Birth date cannot be in the future (CSUTLDPY.cpy lines 350-368)
            if (birthDate.isAfter(currentDate)) {
                throw new IllegalArgumentException(
                    String.format("Date of birth cannot be in the future: %s", birthDate));
            }

            // Additional business validation - reasonable birth date range
            LocalDate minimumBirthDate = currentDate.minusYears(150); // Maximum reasonable age
            if (birthDate.isBefore(minimumBirthDate)) {
                logger.warn("Date of birth '{}' for field '{}' is more than 150 years ago", birthDate, fieldName);
            }

            logger.debug("Successfully validated date of birth field '{}': {}", fieldName, birthDate);
            return birthDate;

        } catch (Exception e) {
            logger.error("Date of birth validation failed for field '{}' with value '{}': {}", 
                        fieldName, dateString, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("Date of birth validation failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Converts COBOL character fields with encoding and validation.
     * 
     * This method handles the conversion of various COBOL character field types including
     * PIC X (alphanumeric), applying proper character encoding, trimming, and length
     * validation. It provides a unified interface for character field processing.
     * 
     * @param value     raw character field value
     * @param picClause COBOL PIC clause specification
     * @param fieldName descriptive name of the field (for logging)
     * @return processed and validated string
     * @throws IllegalArgumentException if conversion fails
     */
    public String convertCharacterField(Object value, String picClause, String fieldName) {
        logger.debug("Converting character field '{}' with PIC clause '{}' and value: '{}'", 
                    fieldName != null ? fieldName : "unknown", picClause, value);

        try {
            if (value == null) {
                logger.debug("Null value for character field '{}', returning empty string", fieldName);
                return "";
            }

            // Handle byte array input (potential EBCDIC data)
            if (value instanceof byte[]) {
                return convertEbcdicToAscii((byte[]) value, fieldName);
            }

            // Handle string input with PIC clause validation
            String stringValue = value.toString();
            
            if (picClause != null && picClause.toUpperCase().contains("PIC X")) {
                return convertCobolString(stringValue, picClause, fieldName);
            } else {
                // General character field conversion
                String result = stringValue.trim();
                logger.debug("Successfully converted character field '{}': '{}'", fieldName, result);
                return result;
            }

        } catch (Exception e) {
            logger.error("Failed to convert character field '{}': {}", fieldName, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("Character field conversion failed for field '%s': %s", fieldName, e.getMessage()), e);
        }
    }

    /**
     * Main orchestration method for comprehensive data conversion operations.
     * 
     * This method serves as the primary entry point for complex data conversion workflows,
     * coordinating multiple conversion operations and providing transaction-level consistency.
     * It's designed for processing complete COBOL records during system migration.
     * 
     * @param inputData     Map containing raw COBOL data fields
     * @param conversionSpec Map containing conversion specifications and PIC clauses
     * @param contextInfo   Additional context information for logging and validation
     * @return Map containing converted Java objects
     * @throws IllegalArgumentException if any conversion fails
     */
    public Map<String, Object> processDataConversion(Map<String, Object> inputData, 
                                                    Map<String, Map<String, Object>> conversionSpec,
                                                    Map<String, Object> contextInfo) {
        String recordType = contextInfo != null ? (String) contextInfo.get("recordType") : "unknown";
        String transactionId = contextInfo != null ? (String) contextInfo.get("transactionId") : "unknown";
        
        logger.info("Starting comprehensive data conversion for record type '{}', transaction '{}'", 
                   recordType, transactionId);

        Map<String, Object> results = new HashMap<>();
        
        try {
            if (inputData == null || inputData.isEmpty()) {
                logger.warn("No input data provided for conversion");
                return results;
            }

            if (conversionSpec == null || conversionSpec.isEmpty()) {
                logger.warn("No conversion specification provided");
                return results;
            }

            int totalFields = inputData.size();
            int processedFields = 0;
            int successfulConversions = 0;
            List<String> conversionErrors = new ArrayList<>();

            // Process each field according to its conversion specification
            for (Map.Entry<String, Object> dataEntry : inputData.entrySet()) {
                String fieldName = dataEntry.getKey();
                Object fieldValue = dataEntry.getValue();
                
                try {
                    Map<String, Object> fieldSpec = conversionSpec.get(fieldName);
                    if (fieldSpec == null) {
                        logger.debug("No conversion spec for field '{}', passing through unchanged", fieldName);
                        results.put(fieldName, fieldValue);
                        processedFields++;
                        continue;
                    }

                    String conversionType = (String) fieldSpec.get("type");
                    String picClause = (String) fieldSpec.get("picClause");
                    Integer scale = (Integer) fieldSpec.get("scale");
                    Integer maxLength = (Integer) fieldSpec.get("maxLength");

                    Object convertedValue = performFieldConversion(
                        fieldValue, conversionType, picClause, scale, maxLength, fieldName);
                    
                    results.put(fieldName, convertedValue);
                    successfulConversions++;
                    
                } catch (Exception e) {
                    String errorMsg = String.format("Field '%s': %s", fieldName, e.getMessage());
                    conversionErrors.add(errorMsg);
                    logger.error("Conversion failed for field '{}' in transaction '{}': {}", 
                               fieldName, transactionId, e.getMessage());
                }
                
                processedFields++;
            }

            logger.info("Data conversion completed for transaction '{}': {}/{} fields successful", 
                       transactionId, successfulConversions, totalFields);

            if (!conversionErrors.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Data conversion failed for transaction '%s' with %d errors: %s", 
                                 transactionId, conversionErrors.size(), String.join("; ", conversionErrors)));
            }

            return results;

        } catch (Exception e) {
            logger.error("Comprehensive data conversion failed for transaction '{}': {}", transactionId, e.getMessage(), e);
            throw new IllegalArgumentException(
                String.format("Data conversion process failed for transaction '%s': %s", transactionId, e.getMessage()), e);
        }
    }

    // Private helper methods

    /**
     * Validates day/month/year combinations including leap year calculations.
     * Implements the logic from CSUTLDPY.cpy lines 213-272.
     * 
     * @param day      day of month (1-31)
     * @param month    month of year (1-12)
     * @param fullYear full year (e.g., 2023)
     * @return true if the day is valid for the specified month and year
     */
    private boolean isValidDayForMonth(int day, int month, int fullYear) {
        // Days in each month (non-leap year)
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        
        // February leap year handling (CSUTLDPY.cpy lines 243-272)
        if (month == 2 && isLeapYear(fullYear)) {
            daysInMonth[1] = 29;
        }
        
        // Check if day exceeds maximum for the month (CSUTLDPY.cpy lines 213-226)
        if (day > daysInMonth[month - 1]) {
            return false;
        }
        
        return true;
    }

    /**
     * Determines if a year is a leap year using COBOL leap year calculation logic.
     * Implements the logic from CSUTLDPY.cpy lines 245-271.
     * 
     * @param year full year to check
     * @return true if the year is a leap year
     */
    private boolean isLeapYear(int year) {
        // COBOL leap year logic: divisible by 4, except century years must be divisible by 400
        if (year % 400 == 0) {
            return true; // Divisible by 400 (e.g., 2000)
        }
        if (year % 100 == 0) {
            return false; // Divisible by 100 but not 400 (e.g., 1900)
        }
        return year % 4 == 0; // Divisible by 4 (e.g., 2024)
    }

    /**
     * Extracts the scale (decimal places) from a COBOL PIC clause.
     * 
     * @param picClause COBOL PIC clause (e.g., "PIC S9(10)V99" has scale 2)
     * @return number of decimal places, or 0 if none specified
     */
    private int extractScaleFromPic(String picClause) {
        if (picClause == null) {
            return 0;
        }
        
        String upperPic = picClause.toUpperCase();
        if (upperPic.contains("V99")) {
            return 2; // Two decimal places
        } else if (upperPic.contains("V9")) {
            return 1; // One decimal place
        }
        return 0; // No decimal places
    }

    /**
     * Extracts the maximum length from a COBOL PIC X clause.
     * 
     * @param picClause COBOL PIC X clause (e.g., "PIC X(25)" has length 25)
     * @return maximum character length, or 1 if not specified
     */
    private int extractLengthFromPicX(String picClause) {
        if (picClause == null) {
            return 1;
        }
        
        // Look for pattern like "PIC X(25)" or "PIC X"
        Pattern lengthPattern = Pattern.compile("PIC\\s+X(?:\\((\\d+)\\))?");
        var matcher = lengthPattern.matcher(picClause.toUpperCase());
        
        if (matcher.find()) {
            String lengthGroup = matcher.group(1);
            return lengthGroup != null ? Integer.parseInt(lengthGroup) : 1;
        }
        
        return 1; // Default length
    }

    /**
     * Performs individual field conversion based on specification.
     * 
     * @param value          field value to convert
     * @param conversionType type of conversion to perform
     * @param picClause      COBOL PIC clause
     * @param scale          decimal scale (if applicable)
     * @param maxLength      maximum length (if applicable)
     * @param fieldName      field name for logging
     * @return converted value
     */
    private Object performFieldConversion(Object value, String conversionType, String picClause, 
                                        Integer scale, Integer maxLength, String fieldName) {
        if (conversionType == null) {
            return value; // Pass through unchanged
        }

        switch (conversionType.toLowerCase()) {
            case "comp3":
            case "packed-decimal":
                if (value instanceof byte[]) {
                    int actualScale = scale != null ? scale : extractScaleFromPic(picClause);
                    return convertComp3ToBigDecimal((byte[]) value, actualScale, fieldName);
                }
                throw new IllegalArgumentException("COMP-3 conversion requires byte array input");

            case "numeric":
            case "pic9":
            case "pics9":
                return convertNumericField(value, picClause, fieldName);

            case "signed-numeric":
                return handleSignedNumeric(value, picClause, fieldName);

            case "character":
            case "picx":
                return convertCharacterField(value, picClause, fieldName);

            case "string":
                return convertCobolString(value, picClause, fieldName);

            case "date":
                if (value instanceof String) {
                    return validateAndConvertDate((String) value, fieldName);
                }
                throw new IllegalArgumentException("Date conversion requires string input");

            case "date-of-birth":
                if (value instanceof String) {
                    return validateDateOfBirth((String) value, fieldName);
                }
                throw new IllegalArgumentException("Date of birth conversion requires string input");

            case "ebcdic":
                if (value instanceof byte[]) {
                    return convertEbcdicToAscii((byte[]) value, fieldName);
                }
                throw new IllegalArgumentException("EBCDIC conversion requires byte array input");

            default:
                logger.warn("Unknown conversion type '{}' for field '{}', using general conversion", 
                           conversionType, fieldName);
                return CobolDataConverter.convertToJavaType(value, picClause);
        }
    }

    /**
     * Creates the EBCDIC to ASCII character mapping based on standard EBCDIC code page 037.
     * This mapping handles the most common characters used in COBOL mainframe systems.
     * 
     * @return Map containing EBCDIC to ASCII character conversions
     */
    private static Map<Character, Character> createEbcdicToAsciiMap() {
        Map<Character, Character> map = new HashMap<>();
        
        // Basic ASCII characters (0x20-0x7F) - most are direct mappings
        // Space (0x40 in EBCDIC -> 0x20 in ASCII)
        map.put((char) 0x40, ' ');
        
        // Numbers 0-9 (0xF0-0xF9 in EBCDIC -> 0x30-0x39 in ASCII)
        for (int i = 0; i <= 9; i++) {
            map.put((char) (0xF0 + i), (char) ('0' + i));
        }
        
        // Uppercase letters A-Z (scattered in EBCDIC -> 0x41-0x5A in ASCII)
        char[] ebcdicUppercase = {
            0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7, 0xC8, 0xC9, // A-I
            0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7, 0xD8, 0xD9, // J-R
            0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xE8, 0xE9        // S-Z
        };
        for (int i = 0; i < ebcdicUppercase.length; i++) {
            map.put((char) ebcdicUppercase[i], (char) ('A' + i));
        }
        
        // Lowercase letters a-z (scattered in EBCDIC -> 0x61-0x7A in ASCII)
        char[] ebcdicLowercase = {
            0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, // a-i
            0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, // j-r
            0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8, 0xA9        // s-z
        };
        for (int i = 0; i < ebcdicLowercase.length; i++) {
            map.put((char) ebcdicLowercase[i], (char) ('a' + i));
        }
        
        // Common special characters
        map.put((char) 0x4B, '.'); // Period
        map.put((char) 0x4C, '<'); // Less than
        map.put((char) 0x4D, '('); // Left parenthesis
        map.put((char) 0x4E, '+'); // Plus sign
        map.put((char) 0x4F, '|'); // Vertical bar
        map.put((char) 0x50, '&'); // Ampersand
        map.put((char) 0x5A, '!'); // Exclamation point
        map.put((char) 0x5B, '$'); // Dollar sign
        map.put((char) 0x5C, '*'); // Asterisk
        map.put((char) 0x5D, ')'); // Right parenthesis
        map.put((char) 0x5E, ';'); // Semicolon
        map.put((char) 0x60, '-'); // Minus/hyphen
        map.put((char) 0x61, '/'); // Forward slash
        map.put((char) 0x6B, ','); // Comma
        map.put((char) 0x6C, '%'); // Percent
        map.put((char) 0x6D, '_'); // Underscore
        map.put((char) 0x6E, '>'); // Greater than
        map.put((char) 0x6F, '?'); // Question mark
        map.put((char) 0x7A, ':'); // Colon
        map.put((char) 0x7B, '#'); // Number sign
        map.put((char) 0x7C, '@'); // At sign
        map.put((char) 0x7D, '\''); // Single quote
        map.put((char) 0x7E, '='); // Equal sign
        map.put((char) 0x7F, '"'); // Double quote
        
        // Control characters and special cases
        map.put((char) 0x00, (char) 0x00); // Null
        map.put((char) 0x05, '\t'); // Tab
        map.put((char) 0x15, '\n'); // Line feed
        map.put((char) 0x0D, '\r'); // Carriage return
        
        return map;
    }
}
