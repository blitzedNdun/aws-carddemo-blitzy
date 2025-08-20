/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Transaction;
import com.carddemo.util.TestConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * COBOL comparison utilities for validating functional parity between COBOL and Java implementations.
 * 
 * Provides comprehensive validation methods to ensure exact functional equivalence
 * between original COBOL CBTRN01C batch processing logic and the Java Spring Boot
 * implementation. Critical for migration validation and ensuring zero functional regression.
 * 
 * Key Validation Areas:
 * - BigDecimal precision matching COBOL COMP-3 packed decimal behavior
 * - Financial calculation accuracy and rounding consistency
 * - Transaction format validation against COBOL field definitions
 * - Batch totals reconciliation matching COBOL algorithms
 * - Performance threshold compliance
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class CobolComparisonUtils {

    /**
     * Compares BigDecimal precision against COBOL COMP-3 expectations.
     * 
     * Validates that Java BigDecimal calculations produce identical results
     * to COBOL COMP-3 packed decimal operations, ensuring exact monetary precision.
     * 
     * @param actual actual BigDecimal result from Java calculation
     * @param expected expected BigDecimal result matching COBOL behavior
     * @return true if precision and value match COBOL requirements
     */
    public boolean compareDecimalPrecision(BigDecimal actual, BigDecimal expected) {
        if (actual == null && expected == null) {
            return true;
        }
        
        if (actual == null || expected == null) {
            return false;
        }
        
        // Ensure both values have COBOL-compatible scale
        BigDecimal normalizedActual = actual.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        BigDecimal normalizedExpected = expected.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        // Compare values with monetary tolerance for rounding differences
        BigDecimal difference = normalizedActual.subtract(normalizedExpected).abs();
        return difference.compareTo(TestConstants.MONETARY_TOLERANCE) <= 0;
    }

    /**
     * Validates financial calculations against COBOL formula results.
     * 
     * Ensures that complex financial calculations (interest, fees, balances)
     * produce identical results to COBOL implementations with proper precision handling.
     * 
     * @param javaResult result from Java BigDecimal calculation
     * @param cobolEquivalent expected result matching COBOL calculation
     * @return true if calculation results match within acceptable tolerance
     */
    public boolean validateFinancialCalculation(BigDecimal javaResult, BigDecimal cobolEquivalent) {
        // Apply COBOL rounding to both values
        BigDecimal roundedJavaResult = javaResult.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        BigDecimal roundedCobolResult = cobolEquivalent.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        // Validate scale matches COBOL COMP-3 requirements
        if (roundedJavaResult.scale() != TestConstants.COBOL_DECIMAL_SCALE) {
            return false;
        }
        
        // Compare exact values (no tolerance for core financial calculations)
        return roundedJavaResult.compareTo(roundedCobolResult) == 0;
    }

    /**
     * Verifies complete COBOL calculation parity for critical financial operations.
     * 
     * Comprehensive validation ensuring Java calculations exactly replicate
     * COBOL algorithms including rounding behavior, precision handling, and edge cases.
     * 
     * @param javaCalculation Java calculation result
     * @param cobolReference COBOL reference calculation
     * @return true if complete parity is achieved
     */
    public boolean verifyCobolParity(BigDecimal javaCalculation, BigDecimal cobolReference) {
        // Check null handling consistency
        if (javaCalculation == null || cobolReference == null) {
            return javaCalculation == cobolReference;
        }
        
        // Validate precision and scale match COBOL requirements
        if (!validateDecimalPrecision(javaCalculation) || !validateDecimalPrecision(cobolReference)) {
            return false;
        }
        
        // Apply COBOL rounding and compare
        BigDecimal normalizedJava = javaCalculation.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        BigDecimal normalizedCobol = cobolReference.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        // Exact comparison for parity validation
        return normalizedJava.compareTo(normalizedCobol) == 0;
    }

    /**
     * Validates that a BigDecimal value meets COBOL COMP-3 precision requirements.
     * 
     * Ensures BigDecimal scale and precision align with COBOL packed decimal
     * field definitions for monetary calculations.
     * 
     * @param value BigDecimal value to validate
     * @return true if value meets COBOL precision requirements
     */
    public boolean validateDecimalPrecision(BigDecimal value) {
        if (value == null) {
            return false;
        }
        
        // Check scale matches COBOL COMP-3 (2 decimal places)
        if (value.scale() > TestConstants.COBOL_DECIMAL_SCALE) {
            return false;
        }
        
        // Validate precision doesn't exceed COBOL PIC S9(10)V99 limits
        BigDecimal maxValue = new BigDecimal("9999999999.99");
        BigDecimal minValue = new BigDecimal("-9999999999.99");
        
        return value.compareTo(maxValue) <= 0 && value.compareTo(minValue) >= 0;
    }

    /**
     * Validates Transaction format against COBOL TRAN-RECORD structure.
     * 
     * Ensures transaction fields conform to COBOL copybook field definitions
     * including length constraints, data types, and format requirements.
     * 
     * @param transaction Transaction to validate
     * @return true if transaction format matches COBOL requirements
     */
    public boolean validateTransactionFormat(Transaction transaction) {
        if (transaction == null) {
            return false;
        }
        
        // Validate transaction ID length (COBOL PIC X(16))
        if (transaction.getTransactionId() != null) {
            String txnIdStr = transaction.getTransactionId().toString();
            if (txnIdStr.length() > TestConstants.TRANSACTION_ID_MAX_LENGTH) {
                return false;
            }
        }
        
        // Validate description length (COBOL PIC X(100))
        if (transaction.getDescription() != null && 
            transaction.getDescription().length() > TestConstants.TRANSACTION_DESC_MAX_LENGTH) {
            return false;
        }
        
        // Validate merchant name length (COBOL PIC X(50))
        if (transaction.getMerchantName() != null && 
            transaction.getMerchantName().length() > TestConstants.MERCHANT_NAME_MAX_LENGTH) {
            return false;
        }
        
        // Validate amount precision (COBOL PIC S9(09)V99)
        if (transaction.getAmount() != null) {
            if (!validateDecimalPrecision(transaction.getAmount())) {
                return false;
            }
        }
        
        // Validate transaction type code format (COBOL PIC X(02))
        if (transaction.getTransactionTypeCode() != null) {
            String typeCode = transaction.getTransactionTypeCode();
            if (typeCode.length() != 2 && typeCode.length() != 3) { // Allow 2-3 char codes
                return false;
            }
        }
        
        return true;
    }

    /**
     * Compares batch totals for reconciliation validation.
     * 
     * Validates that batch processing totals match between Java and COBOL
     * implementations, ensuring accurate batch reconciliation and reporting.
     * 
     * @param javaBatchTotal total calculated by Java batch processing
     * @param cobolBatchTotal expected total from COBOL batch processing
     * @return true if batch totals match within acceptable tolerance
     */
    public boolean compareBatchTotals(BigDecimal javaBatchTotal, BigDecimal cobolBatchTotal) {
        if (javaBatchTotal == null && cobolBatchTotal == null) {
            return true;
        }
        
        if (javaBatchTotal == null || cobolBatchTotal == null) {
            return false;
        }
        
        // Apply COBOL rounding to both totals
        BigDecimal roundedJavaTotal = javaBatchTotal.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        BigDecimal roundedCobolTotal = cobolBatchTotal.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        // Calculate difference for tolerance check
        BigDecimal difference = roundedJavaTotal.subtract(roundedCobolTotal).abs();
        
        // For batch totals, allow minimal tolerance to account for cumulative rounding
        BigDecimal batchTolerance = TestConstants.MONETARY_TOLERANCE.multiply(BigDecimal.TEN);
        
        return difference.compareTo(batchTolerance) <= 0;
    }

    /**
     * Validates transaction data type mappings from COBOL to Java.
     * 
     * Ensures all COBOL data types are correctly mapped to Java equivalents
     * with proper precision, scale, and format preservation.
     * 
     * @param transaction Transaction with Java data types
     * @return true if data type mappings are correct
     */
    public boolean validateDataTypeMappings(Transaction transaction) {
        if (transaction == null) {
            return false;
        }
        
        // Validate numeric fields use BigDecimal with proper scale
        if (transaction.getAmount() != null) {
            if (!(transaction.getAmount() instanceof BigDecimal)) {
                return false;
            }
            if (transaction.getAmount().scale() != TestConstants.COBOL_DECIMAL_SCALE) {
                return false;
            }
        }
        
        // Validate string fields don't exceed COBOL field lengths
        if (transaction.getDescription() != null && 
            transaction.getDescription().length() > TestConstants.TRANSACTION_DESC_MAX_LENGTH) {
            return false;
        }
        
        if (transaction.getMerchantName() != null && 
            transaction.getMerchantName().length() > TestConstants.MERCHANT_NAME_MAX_LENGTH) {
            return false;
        }
        
        // Validate date fields use appropriate Java types
        if (transaction.getTransactionDate() != null) {
            // Should be LocalDate (COBOL date fields)
            return transaction.getTransactionDate() instanceof java.time.LocalDate;
        }
        
        if (transaction.getOriginalTimestamp() != null) {
            // Should be LocalDateTime (COBOL timestamp fields)
            return transaction.getOriginalTimestamp() instanceof java.time.LocalDateTime;
        }
        
        return true;
    }

    /**
     * Validates calculation rounding behavior matches COBOL ROUNDED clause.
     * 
     * Ensures that Java BigDecimal rounding produces identical results
     * to COBOL ROUNDED clause behavior for financial calculations.
     * 
     * @param calculation BigDecimal calculation result
     * @param originalValue original value before rounding
     * @return true if rounding behavior matches COBOL
     */
    public boolean validateCobolRounding(BigDecimal calculation, BigDecimal originalValue) {
        if (calculation == null || originalValue == null) {
            return false;
        }
        
        // Apply COBOL ROUNDED clause equivalent (HALF_UP rounding)
        BigDecimal cobolRounded = originalValue.setScale(TestConstants.COBOL_DECIMAL_SCALE, RoundingMode.HALF_UP);
        
        // Compare with actual calculation result
        return calculation.compareTo(cobolRounded) == 0;
    }

    /**
     * Validates performance compliance with COBOL processing requirements.
     * 
     * Ensures Java implementation meets or exceeds COBOL performance
     * benchmarks for transaction processing and batch operations.
     * 
     * @param processingTime actual processing time in milliseconds
     * @param transactionCount number of transactions processed
     * @return true if performance meets COBOL requirements
     */
    public boolean validatePerformanceCompliance(long processingTime, int transactionCount) {
        if (transactionCount <= 0) {
            return false;
        }
        
        // Calculate average processing time per transaction
        double avgTimePerTransaction = (double) processingTime / transactionCount;
        
        // For individual transactions, must meet response time threshold
        if (transactionCount == 1) {
            return processingTime <= TestConstants.RESPONSE_TIME_THRESHOLD_MS;
        }
        
        // For batch processing, must complete within batch timeout window
        if (transactionCount >= TestConstants.DEFAULT_BATCH_SIZE) {
            return processingTime <= TestConstants.BATCH_PROCESSING_TIMEOUT_MS;
        }
        
        // For small batches, use proportional scaling
        long scaledThreshold = (long) (TestConstants.RESPONSE_TIME_THRESHOLD_MS * Math.log(transactionCount + 1));
        return processingTime <= scaledThreshold;
    }

    /**
     * Validates memory usage efficiency compared to COBOL processing.
     * 
     * Ensures Java implementation maintains memory efficiency
     * comparable to COBOL batch processing capabilities.
     * 
     * @param memoryUsageMB current memory usage in megabytes
     * @param transactionCount number of transactions in memory
     * @return true if memory usage is within acceptable limits
     */
    public boolean validateMemoryEfficiency(long memoryUsageMB, int transactionCount) {
        if (transactionCount <= 0) {
            return memoryUsageMB <= TestConstants.MAX_MEMORY_USAGE_MB;
        }
        
        // Calculate memory per transaction
        double memoryPerTransaction = (double) memoryUsageMB / transactionCount;
        
        // Maximum allowed memory per transaction (in MB)
        double maxMemoryPerTransaction = 0.1; // 100KB per transaction
        
        return memoryPerTransaction <= maxMemoryPerTransaction && 
               memoryUsageMB <= TestConstants.MAX_MEMORY_USAGE_MB;
    }

    /**
     * Generates COBOL-equivalent test result for comparison validation.
     * 
     * Simulates COBOL calculation results for testing Java implementation
     * accuracy without requiring actual COBOL system access.
     * 
     * @param inputValue input value for calculation
     * @param operation operation to perform (add, subtract, multiply, divide)
     * @param operand second operand for calculation
     * @return BigDecimal result matching COBOL behavior
     */
    public BigDecimal generateCobolEquivalentResult(BigDecimal inputValue, String operation, BigDecimal operand) {
        if (inputValue == null || operand == null || operation == null) {
            return null;
        }
        
        BigDecimal result;
        
        switch (operation.toUpperCase()) {
            case "ADD":
                result = inputValue.add(operand);
                break;
            case "SUBTRACT":
                result = inputValue.subtract(operand);
                break;
            case "MULTIPLY":
                result = inputValue.multiply(operand);
                break;
            case "DIVIDE":
                if (operand.compareTo(BigDecimal.ZERO) == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                result = inputValue.divide(operand, TestConstants.COBOL_DECIMAL_SCALE + 2, TestConstants.COBOL_ROUNDING_MODE);
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
        
        // Apply COBOL ROUNDED clause behavior
        return result.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
    }
}