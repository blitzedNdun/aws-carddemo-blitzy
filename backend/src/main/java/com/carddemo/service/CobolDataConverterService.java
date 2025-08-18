/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carddemo.util.CobolDataConverter;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Spring Boot service wrapper for CobolDataConverter utility that provides dependency injection 
 * and transaction management for COBOL data type conversion operations.
 * 
 * This service implements the service layer pattern by delegating to the CobolDataConverter 
 * utility class while providing Spring framework integration including:
 * - Dependency injection through @Service annotation
 * - Transaction management for data conversion operations
 * - Enhanced error handling and logging
 * - Integration with Spring application context
 * 
 * The service maintains the same precision and conversion logic as the underlying utility
 * while adding enterprise-grade features like transaction boundaries, logging, and proper
 * exception handling suitable for production deployment.
 * 
 * This implementation directly supports the CardDemo system migration from COBOL/CICS to 
 * Java/Spring Boot by providing Spring-managed access to COBOL data conversion capabilities.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
@Transactional
public class CobolDataConverterService {

    private static final Logger logger = LoggerFactory.getLogger(CobolDataConverterService.class);

    /**
     * Converts COBOL COMP-3 packed decimal data to Java BigDecimal with exact precision preservation.
     * 
     * This method provides a Spring-managed wrapper around the core COMP-3 conversion algorithm,
     * adding transaction support and enhanced error handling for enterprise deployment.
     * 
     * Key Features:
     * - Maintains identical precision to COBOL COMP-3 behavior
     * - Proper sign bit handling (positive, negative, unsigned)
     * - Comprehensive validation and error reporting
     * - Transaction-aware for data consistency
     * 
     * @param packedData byte array containing COMP-3 packed decimal data
     * @param scale      number of decimal places (typically 2 for monetary amounts)
     * @return BigDecimal with exact precision and scale matching COBOL behavior
     * @throws IllegalArgumentException if packedData is null, empty, or invalid format
     */
    @Transactional(readOnly = true)
    public BigDecimal fromComp3ToBigDecimal(byte[] packedData, int scale) {
        logger.debug("Converting COMP-3 packed data to BigDecimal with scale: {}", scale);
        
        try {
            BigDecimal result = CobolDataConverter.fromComp3(packedData, scale);
            logger.debug("Successfully converted COMP-3 data to BigDecimal: {}", result);
            return result;
        } catch (IllegalArgumentException e) {
            logger.error("Failed to convert COMP-3 packed data: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during COMP-3 conversion", e);
            throw new RuntimeException("COMP-3 conversion failed", e);
        }
    }

    /**
     * Converts COBOL PIC X (alphanumeric) string with proper character encoding and length validation.
     * 
     * This method provides Spring-managed access to COBOL string conversion with transaction
     * support and enhanced validation capabilities.
     * 
     * Features:
     * - Character set conversion from potential EBCDIC encoding
     * - Proper trimming of COBOL fixed-length fields
     * - Length validation against PIC X(n) specifications
     * - Transaction-aware processing
     * 
     * @param value      raw string value from COBOL field
     * @param maxLength  maximum length from PIC X(n) specification
     * @return trimmed and validated string
     * @throws IllegalArgumentException if value exceeds maximum length
     */
    @Transactional(readOnly = true)
    public String convertPicString(String value, int maxLength) {
        logger.debug("Converting PIC X string with max length: {}", maxLength);
        
        try {
            String result = CobolDataConverter.convertPicString(value, maxLength);
            logger.debug("Successfully converted PIC X string: '{}'", result);
            return result;
        } catch (IllegalArgumentException e) {
            logger.error("Failed to convert PIC X string: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during PIC X conversion", e);
            throw new RuntimeException("PIC X string conversion failed", e);
        }
    }

    /**
     * Converts data to appropriate Java type based on COBOL PIC clause specification.
     * 
     * This method serves as the main Spring-managed entry point for COBOL data type conversion,
     * providing transaction support and comprehensive error handling for production use.
     * 
     * Supported PIC clause formats:
     * - PIC X(n) - alphanumeric fields converted to String
     * - PIC 9(n) - unsigned numeric fields converted to Long or BigDecimal
     * - PIC S9(n)V99 - signed numeric with decimal places converted to BigDecimal
     * 
     * @param value     raw data value to convert
     * @param picClause COBOL PIC clause specification (e.g., "PIC X(10)", "PIC S9(5)V99")
     * @return converted Java object (String for PIC X, BigDecimal/Long for numeric types)
     * @throws IllegalArgumentException if PIC clause is invalid or conversion fails
     */
    @Transactional(readOnly = true)
    public Object convertToJavaType(Object value, String picClause) {
        logger.debug("Converting to Java type using PIC clause: {}", picClause);
        
        try {
            Object result = CobolDataConverter.convertToJavaType(value, picClause);
            logger.debug("Successfully converted to Java type: {} -> {}", 
                        value != null ? value.getClass().getSimpleName() : "null",
                        result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (IllegalArgumentException e) {
            logger.error("Failed to convert to Java type with PIC clause '{}': {}", picClause, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during Java type conversion", e);
            throw new RuntimeException("Java type conversion failed", e);
        }
    }

    /**
     * Preserves decimal precision by ensuring consistent scale and rounding across calculations.
     * 
     * This method provides Spring-managed precision standardization to match COBOL computational
     * behavior, particularly critical for financial calculations requiring exact decimal accuracy.
     * 
     * Features:
     * - COBOL-compatible rounding mode (HALF_UP)
     * - Consistent scale enforcement
     * - Transaction-aware processing
     * - Null safety with default zero handling
     * 
     * @param decimal input BigDecimal value to standardize
     * @param scale   required number of decimal places
     * @return BigDecimal with standardized precision and COBOL-compatible rounding
     */
    @Transactional(readOnly = true)
    public BigDecimal preservePrecision(BigDecimal decimal, int scale) {
        logger.debug("Preserving precision for BigDecimal with scale: {}", scale);
        
        try {
            BigDecimal result = CobolDataConverter.preservePrecision(decimal, scale);
            logger.debug("Successfully preserved precision: {} -> {}", decimal, result);
            return result;
        } catch (Exception e) {
            logger.error("Unexpected error during precision preservation", e);
            throw new RuntimeException("Precision preservation failed", e);
        }
    }

    /**
     * Formats BigDecimal values for display purposes with proper locale and currency formatting.
     * 
     * This method provides comprehensive display formatting capabilities that combine multiple
     * utility functions to create user-friendly representations of financial data while
     * maintaining underlying precision.
     * 
     * Features:
     * - Currency formatting with locale support
     * - Automatic scale adjustment for monetary amounts
     * - Proper COBOL rounding mode application
     * - Transaction-aware processing
     * - Support for both currency and plain decimal formatting
     * 
     * @param value  BigDecimal value to format
     * @param locale desired locale for formatting (null defaults to US)
     * @param isCurrency true to format as currency, false for plain decimal
     * @return formatted string representation suitable for display
     */
    @Transactional(readOnly = true)
    public String formatToDisplay(BigDecimal value, Locale locale, boolean isCurrency) {
        logger.debug("Formatting BigDecimal for display: currency={}, locale={}", isCurrency, locale);
        
        try {
            String result;
            if (isCurrency) {
                // Use CobolDataConverter's currency formatting capabilities
                result = CobolDataConverter.formatCurrency(value, locale);
            } else {
                // Format as plain decimal with preserved precision
                if (value == null) {
                    value = BigDecimal.ZERO.setScale(CobolDataConverter.MONETARY_SCALE, 
                                                   CobolDataConverter.COBOL_ROUNDING_MODE);
                }
                BigDecimal standardized = CobolDataConverter.preservePrecision(value, value.scale());
                result = standardized.toPlainString();
            }
            
            logger.debug("Successfully formatted BigDecimal for display: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Unexpected error during display formatting", e);
            throw new RuntimeException("Display formatting failed", e);
        }
    }

    /**
     * Convenience method for formatting BigDecimal as currency with US locale.
     * 
     * @param value BigDecimal value to format as US currency
     * @return formatted currency string in US format
     */
    @Transactional(readOnly = true)
    public String formatToDisplay(BigDecimal value) {
        return formatToDisplay(value, Locale.US, true);
    }

    /**
     * Convenience method for formatting BigDecimal as plain decimal.
     * 
     * @param value BigDecimal value to format as plain decimal
     * @return formatted decimal string
     */
    @Transactional(readOnly = true)
    public String formatToDisplayPlain(BigDecimal value) {
        return formatToDisplay(value, null, false);
    }

    /**
     * Validates COBOL field specifications and provides detailed error reporting.
     * 
     * This method extends the utility validation capabilities with Spring-managed
     * transaction support and enhanced error reporting suitable for enterprise applications.
     * 
     * @param value     field value to validate
     * @param picClause COBOL PIC clause specification
     * @return true if field is valid according to COBOL rules
     * @throws IllegalArgumentException if validation rules are violated
     */
    @Transactional(readOnly = true)
    public boolean validateCobolField(Object value, String picClause) {
        logger.debug("Validating COBOL field with PIC clause: {}", picClause);
        
        try {
            boolean isValid = CobolDataConverter.validateCobolField(value, picClause);
            logger.debug("COBOL field validation result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Error during COBOL field validation", e);
            throw new RuntimeException("COBOL field validation failed", e);
        }
    }

    /**
     * Performs batch conversion of multiple COBOL fields with transaction management.
     * 
     * This method provides efficient batch processing of multiple COBOL data conversions
     * within a single transaction boundary, optimizing performance for bulk operations.
     * 
     * @param values array of values to convert
     * @param picClauses corresponding array of PIC clause specifications
     * @return array of converted Java objects
     * @throws IllegalArgumentException if arrays have different lengths or contain invalid data
     */
    @Transactional(readOnly = true)
    public Object[] convertMultipleFields(Object[] values, String[] picClauses) {
        logger.debug("Performing batch conversion of {} COBOL fields", values != null ? values.length : 0);
        
        if (values == null || picClauses == null) {
            throw new IllegalArgumentException("Values and PIC clauses arrays cannot be null");
        }
        
        if (values.length != picClauses.length) {
            throw new IllegalArgumentException("Values and PIC clauses arrays must have same length");
        }
        
        try {
            Object[] results = new Object[values.length];
            for (int i = 0; i < values.length; i++) {
                results[i] = convertToJavaType(values[i], picClauses[i]);
            }
            
            logger.debug("Successfully completed batch conversion of {} fields", results.length);
            return results;
        } catch (Exception e) {
            logger.error("Error during batch COBOL field conversion", e);
            throw new RuntimeException("Batch conversion failed", e);
        }
    }
}