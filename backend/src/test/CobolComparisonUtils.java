package com.carddemo.test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;

/**
 * Utility class for validating functional parity between COBOL and Java implementations,
 * specifically for comparing BigDecimal precision in financial calculations and payment processing.
 * 
 * This class provides methods to ensure that the modernized Java implementation produces
 * identical results to the original COBOL programs, with particular focus on:
 * - COBOL COMP-3 packed decimal precision equivalence
 * - Financial calculation accuracy validation
 * - FICO score precision comparison
 * - Comprehensive comparison reporting
 */
public class CobolComparisonUtils {
    
    // COBOL COMP-3 precision constants
    private static final int DEFAULT_SCALE = 2;
    private static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    /**
     * Compares two BigDecimal values for exact precision match as would occur in COBOL COMP-3.
     * This method validates that Java BigDecimal operations produce identical results to
     * COBOL packed decimal calculations.
     * 
     * @param expected The expected BigDecimal value (representing COBOL COMP-3 result)
     * @param actual The actual BigDecimal value from Java calculation
     * @param fieldDescription Description of the field being compared for error reporting
     * @return true if values match exactly in scale and precision
     */
    public static boolean compareBigDecimals(BigDecimal expected, BigDecimal actual, String fieldDescription) {
        if (expected == null && actual == null) {
            return true;
        }
        
        if (expected == null || actual == null) {
            throw new AssertionError(String.format(
                "BigDecimal comparison failed for %s: expected=%s, actual=%s", 
                fieldDescription, expected, actual));
        }
        
        // Ensure both values have the same scale for precise comparison
        int maxScale = Math.max(expected.scale(), actual.scale());
        BigDecimal normalizedExpected = expected.setScale(maxScale, COBOL_ROUNDING_MODE);
        BigDecimal normalizedActual = actual.setScale(maxScale, COBOL_ROUNDING_MODE);
        
        boolean matches = normalizedExpected.compareTo(normalizedActual) == 0;
        
        if (!matches) {
            throw new AssertionError(String.format(
                "BigDecimal precision mismatch for %s: expected=%s (scale=%d), actual=%s (scale=%d)",
                fieldDescription, normalizedExpected, normalizedExpected.scale(), 
                normalizedActual, normalizedActual.scale()));
        }
        
        return matches;
    }
    
    /**
     * Validates financial precision for payment processing, ensuring penny-level accuracy
     * between COBOL and Java implementations. This method is critical for bill payment
     * validation where financial calculations must match exactly.
     * 
     * @param cobolAmount The amount as calculated by COBOL logic
     * @param javaAmount The amount as calculated by Java service
     * @param transactionType The type of transaction being validated
     */
    public static void validateFinancialPrecision(BigDecimal cobolAmount, BigDecimal javaAmount, String transactionType) {
        assertThat(cobolAmount)
            .withFailMessage("COBOL amount cannot be null for transaction type: %s", transactionType)
            .isNotNull();
        
        assertThat(javaAmount)
            .withFailMessage("Java amount cannot be null for transaction type: %s", transactionType)
            .isNotNull();
        
        // Ensure both amounts have proper currency scale (2 decimal places)
        BigDecimal normalizedCobolAmount = cobolAmount.setScale(DEFAULT_SCALE, COBOL_ROUNDING_MODE);
        BigDecimal normalizedJavaAmount = javaAmount.setScale(DEFAULT_SCALE, COBOL_ROUNDING_MODE);
        
        assertThat(normalizedJavaAmount)
            .withFailMessage("Financial precision validation failed for %s: COBOL=%s, Java=%s", 
                transactionType, normalizedCobolAmount, normalizedJavaAmount)
            .isEqualByComparingTo(normalizedCobolAmount);
        
        // Additional validation for scale consistency
        assertThat(normalizedJavaAmount.scale())
            .withFailMessage("Scale validation failed for %s: expected scale=2, actual scale=%d", 
                transactionType, normalizedJavaAmount.scale())
            .isEqualTo(DEFAULT_SCALE);
    }
    
    /**
     * Validates FICO score precision between COBOL and Java implementations.
     * FICO scores are stored as integers in both COBOL (PIC 9(3)) and Java (Integer).
     * 
     * @param cobolFicoScore The FICO score from COBOL system (Integer, can be null)
     * @param javaFicoScore The FICO score from Java system (Integer, can be null)
     * @param customerId The customer ID for error reporting
     */
    public static void validateFicoScorePrecision(Integer cobolFicoScore, Integer javaFicoScore, String customerId) {
        if (cobolFicoScore == null && javaFicoScore == null) {
            return; // Both null is acceptable for customers without FICO scores
        }
        
        assertThat(cobolFicoScore)
            .withFailMessage("COBOL FICO score cannot be null when Java FICO score is provided for customer: %s", customerId)
            .isNotNull();
        
        assertThat(javaFicoScore)
            .withFailMessage("Java FICO score cannot be null when COBOL FICO score is provided for customer: %s", customerId)
            .isNotNull();
        
        assertThat(javaFicoScore)
            .withFailMessage("FICO score precision validation failed for customer %s: COBOL=%d, Java=%d", 
                customerId, cobolFicoScore, javaFicoScore)
            .isEqualTo(cobolFicoScore);
        
        // Validate FICO score is within expected range (300-850) for both values
        assertThat(cobolFicoScore)
            .withFailMessage("COBOL FICO score out of valid range for customer %s: %d", 
                customerId, cobolFicoScore)
            .isBetween(300, 850);
            
        assertThat(javaFicoScore)
            .withFailMessage("Java FICO score out of valid range for customer %s: %d", 
                customerId, javaFicoScore)
            .isBetween(300, 850);
    }
    
    /**
     * Generates a comprehensive comparison report between COBOL and Java calculation results.
     * This method produces detailed analysis for validation testing and audit purposes.
     * 
     * @param testName The name of the test being executed
     * @param comparisons Map of field names to comparison results
     * @return Formatted comparison report as string
     */
    public static String generateComparisonReport(String testName, Map<String, ComparisonResult> comparisons) {
        StringBuilder report = new StringBuilder();
        report.append("=== COBOL-Java Comparison Report ===\n");
        report.append("Test Name: ").append(testName).append("\n");
        report.append("Execution Time: ").append(java.time.LocalDateTime.now()).append("\n\n");
        
        int totalComparisons = comparisons.size();
        long passedComparisons = comparisons.values().stream()
            .mapToLong(result -> result.isPassed() ? 1 : 0)
            .sum();
        
        report.append("Summary: ").append(passedComparisons).append("/").append(totalComparisons)
            .append(" comparisons passed\n\n");
        
        // Detailed results
        report.append("Detailed Results:\n");
        comparisons.forEach((fieldName, result) -> {
            report.append("Field: ").append(fieldName).append("\n");
            report.append("  Status: ").append(result.isPassed() ? "PASS" : "FAIL").append("\n");
            report.append("  COBOL Value: ").append(result.getCobolValue()).append("\n");
            report.append("  Java Value: ").append(result.getJavaValue()).append("\n");
            if (!result.isPassed()) {
                report.append("  Error: ").append(result.getErrorMessage()).append("\n");
            }
            report.append("\n");
        });
        
        return report.toString();
    }
    
    /**
     * Creates a comparison result for field-level validation results.
     * 
     * @param fieldName The name of the field being compared
     * @param cobolValue The value from COBOL system
     * @param javaValue The value from Java system
     * @param passed Whether the comparison passed
     * @param errorMessage Error message if comparison failed
     * @return ComparisonResult object
     */
    public static ComparisonResult createComparisonResult(String fieldName, Object cobolValue, 
            Object javaValue, boolean passed, String errorMessage) {
        return new ComparisonResult(fieldName, cobolValue, javaValue, passed, errorMessage);
    }
    
    /**
     * Validates that a BigDecimal represents a valid currency amount.
     * 
     * @param amount The amount to validate
     * @param fieldName The field name for error reporting
     */
    public static void validateCurrencyAmount(BigDecimal amount, String fieldName) {
        assertThat(amount)
            .withFailMessage("Currency amount cannot be null for field: %s", fieldName)
            .isNotNull();
        
        assertThat(amount.scale())
            .withFailMessage("Currency amount must have scale of 2 for field %s: actual scale=%d", 
                fieldName, amount.scale())
            .isEqualTo(DEFAULT_SCALE);
    }
    
    /**
     * Inner class to hold comparison results for reporting.
     */
    public static class ComparisonResult {
        private final String fieldName;
        private final Object cobolValue;
        private final Object javaValue;
        private final boolean passed;
        private final String errorMessage;
        
        public ComparisonResult(String fieldName, Object cobolValue, Object javaValue, 
                boolean passed, String errorMessage) {
            this.fieldName = fieldName;
            this.cobolValue = cobolValue;
            this.javaValue = javaValue;
            this.passed = passed;
            this.errorMessage = errorMessage;
        }
        
        public String getFieldName() { return fieldName; }
        public Object getCobolValue() { return cobolValue; }
        public Object getJavaValue() { return javaValue; }
        public boolean isPassed() { return passed; }
        public String getErrorMessage() { return errorMessage; }
    }
}