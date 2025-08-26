/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for validating functional parity between COBOL and Java implementations.
 * This class provides comprehensive comparison methods to ensure that the modernized Java
 * implementation produces identical results to the original COBOL system, particularly
 * for financial calculations and payment processing operations.
 * 
 * <p>The CobolComparisonUtils focuses on validating the critical requirement that all
 * financial calculations maintain exact precision equivalence between COBOL COMP-3
 * packed decimal operations and Java BigDecimal implementations.</p>
 * 
 * <p>Key Features:
 * <ul>
 * <li>BigDecimal precision validation matching COBOL COMP-3 behavior</li>
 * <li>Financial calculation accuracy verification</li>
 * <li>Comprehensive comparison reporting for audit trails</li>
 * <li>Rounding mode validation ensuring COBOL equivalence</li>
 * <li>Edge case and boundary condition validation</li>
 * </ul>
 * 
 * <p>Usage in Testing:
 * <ul>
 * <li>Unit test validation of bill payment calculation accuracy</li>
 * <li>Integration test verification of account balance updates</li>
 * <li>Regression testing for COBOL-Java functional parity</li>
 * <li>Performance test validation of calculation consistency</li>
 * <li>Audit trail generation for compliance verification</li>
 * </ul>
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Component
@Slf4j
public class CobolComparisonUtils {

    // COBOL COMP-3 precision constants for validation
    private static final int COBOL_CURRENCY_SCALE = 2;
    private static final int COBOL_INTEREST_SCALE = 4;
    private static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    // Tolerance for floating point comparisons (should be zero for exact match)
    private static final BigDecimal ZERO_TOLERANCE = BigDecimal.ZERO;
    
    /**
     * Comparison result class for detailed validation reporting.
     */
    public static class ComparisonResult {
        private final boolean isEqual;
        private final String description;
        private final BigDecimal expectedValue;
        private final BigDecimal actualValue;
        private final BigDecimal difference;
        private final LocalDateTime comparisonTimestamp;
        private final List<String> validationNotes;

        public ComparisonResult(boolean isEqual, String description, BigDecimal expectedValue, 
                              BigDecimal actualValue) {
            this.isEqual = isEqual;
            this.description = description;
            this.expectedValue = expectedValue;
            this.actualValue = actualValue;
            this.difference = expectedValue != null && actualValue != null 
                ? expectedValue.subtract(actualValue) 
                : null;
            this.comparisonTimestamp = LocalDateTime.now();
            this.validationNotes = new ArrayList<>();
        }

        // Getters
        public boolean isEqual() { return isEqual; }
        public String getDescription() { return description; }
        public BigDecimal getExpectedValue() { return expectedValue; }
        public BigDecimal getActualValue() { return actualValue; }
        public BigDecimal getDifference() { return difference; }
        public LocalDateTime getComparisonTimestamp() { return comparisonTimestamp; }
        public List<String> getValidationNotes() { return validationNotes; }
        
        public void addValidationNote(String note) {
            this.validationNotes.add(note);
        }
    }

    /**
     * Compares two BigDecimal values for exact equality with COBOL precision validation.
     * This method ensures that Java BigDecimal calculations produce identical results
     * to COBOL COMP-3 packed decimal operations.
     * 
     * @param expected the expected value from COBOL calculation or reference
     * @param actual the actual value from Java implementation
     * @param description descriptive text for the comparison context
     * @return ComparisonResult containing detailed comparison information
     */
    public static ComparisonResult compareBigDecimals(BigDecimal expected, BigDecimal actual, String description) {
        log.debug("Comparing BigDecimal values - Expected: {}, Actual: {}, Context: {}", 
                  expected, actual, description);
        
        // Handle null cases
        if (expected == null && actual == null) {
            ComparisonResult result = new ComparisonResult(true, description, null, null);
            result.addValidationNote("Both values are null - comparison passes");
            return result;
        }
        
        if (expected == null || actual == null) {
            ComparisonResult result = new ComparisonResult(false, description, expected, actual);
            result.addValidationNote("One value is null while the other is not - comparison fails");
            return result;
        }
        
        // Normalize scales for comparison (COBOL COMP-3 behavior)
        BigDecimal normalizedExpected = normalizeForCobolComparison(expected);
        BigDecimal normalizedActual = normalizeForCobolComparison(actual);
        
        boolean isEqual = normalizedExpected.compareTo(normalizedActual) == 0;
        ComparisonResult result = new ComparisonResult(isEqual, description, expected, actual);
        
        // Add detailed validation notes
        result.addValidationNote(String.format("Original Expected: %s", expected));
        result.addValidationNote(String.format("Original Actual: %s", actual));
        result.addValidationNote(String.format("Normalized Expected: %s", normalizedExpected));
        result.addValidationNote(String.format("Normalized Actual: %s", normalizedActual));
        
        if (isEqual) {
            result.addValidationNote("Values match with COBOL COMP-3 precision requirements");
        } else {
            BigDecimal difference = normalizedExpected.subtract(normalizedActual);
            result.addValidationNote(String.format("Values differ by: %s", difference));
            result.addValidationNote("CRITICAL: Financial calculation precision mismatch detected");
        }
        
        return result;
    }

    /**
     * Validates financial precision for currency amounts, ensuring penny-level accuracy.
     * This method specifically validates that bill payment amounts and account balances
     * maintain exact precision between COBOL and Java implementations.
     * 
     * @param cobolValue the value as calculated by COBOL system
     * @param javaValue the value as calculated by Java system
     * @param operationType the type of financial operation being validated
     * @return ComparisonResult with financial precision validation details
     */
    public static ComparisonResult validateFinancialPrecision(BigDecimal cobolValue, BigDecimal javaValue, 
                                                      String operationType) {
        log.info("Validating financial precision for operation: {}", operationType);
        
        String description = String.format("Financial Precision Validation - %s", operationType);
        
        // Ensure both values have exactly 2 decimal places for currency
        BigDecimal normalizedCobol = cobolValue != null 
            ? cobolValue.setScale(COBOL_CURRENCY_SCALE, COBOL_ROUNDING_MODE)
            : null;
        BigDecimal normalizedJava = javaValue != null 
            ? javaValue.setScale(COBOL_CURRENCY_SCALE, COBOL_ROUNDING_MODE)
            : null;
        
        ComparisonResult result = compareBigDecimals(normalizedCobol, normalizedJava, description);
        
        // Add financial precision specific validations
        result.addValidationNote(String.format("Operation Type: %s", operationType));
        result.addValidationNote("Financial precision requirements: 2 decimal places, HALF_UP rounding");
        
        if (cobolValue != null) {
            result.addValidationNote(String.format("COBOL value scale: %d", cobolValue.scale()));
        }
        if (javaValue != null) {
            result.addValidationNote(String.format("Java value scale: %d", javaValue.scale()));
        }
        
        // Validate scale requirements
        if (normalizedCobol != null && normalizedCobol.scale() != COBOL_CURRENCY_SCALE) {
            result.addValidationNote("WARNING: COBOL value does not match expected currency scale");
        }
        if (normalizedJava != null && normalizedJava.scale() != COBOL_CURRENCY_SCALE) {
            result.addValidationNote("WARNING: Java value does not match expected currency scale");
        }
        
        // Critical validation for financial operations
        if (!result.isEqual()) {
            result.addValidationNote("CRITICAL FAILURE: Financial precision mismatch - System halt required");
            log.error("Financial precision validation failed for {}: COBOL={}, Java={}", 
                     operationType, normalizedCobol, normalizedJava);
        } else {
            result.addValidationNote("SUCCESS: Financial precision validation passed");
            log.info("Financial precision validation successful for {}", operationType);
        }
        
        return result;
    }

    /**
     * Generates a comprehensive comparison report for multiple validation results.
     * This method creates detailed audit documentation for COBOL-Java equivalence validation.
     * 
     * @param results list of ComparisonResult objects to include in the report
     * @param testContext descriptive context for the test scenario
     * @return formatted comparison report as String
     */
    public static String generateComparisonReport(List<ComparisonResult> results, String testContext) {
        log.info("Generating comparison report for test context: {}", testContext);
        
        StringBuilder report = new StringBuilder();
        report.append("COBOL-JAVA FUNCTIONAL PARITY VALIDATION REPORT\n");
        report.append("=".repeat(60)).append("\n");
        report.append(String.format("Test Context: %s\n", testContext));
        report.append(String.format("Report Generated: %s\n", LocalDateTime.now()));
        report.append(String.format("Total Comparisons: %d\n", results.size()));
        
        // Summary statistics
        long passedCount = results.stream().mapToLong(r -> r.isEqual() ? 1 : 0).sum();
        long failedCount = results.size() - passedCount;
        
        report.append(String.format("Passed: %d\n", passedCount));
        report.append(String.format("Failed: %d\n", failedCount));
        report.append(String.format("Success Rate: %.2f%%\n", 
                                   results.isEmpty() ? 0.0 : (double) passedCount / results.size() * 100));
        report.append("\n");
        
        // Overall status
        if (failedCount == 0) {
            report.append("OVERALL STATUS: ✓ ALL VALIDATIONS PASSED\n");
            report.append("Functional parity confirmed - System ready for operation\n");
        } else {
            report.append("OVERALL STATUS: ✗ VALIDATION FAILURES DETECTED\n");
            report.append("CRITICAL: Functional parity violations found - Investigation required\n");
        }
        report.append("\n");
        
        // Detailed results
        report.append("DETAILED COMPARISON RESULTS\n");
        report.append("-".repeat(40)).append("\n");
        
        for (int i = 0; i < results.size(); i++) {
            ComparisonResult result = results.get(i);
            report.append(String.format("\n%d. %s\n", i + 1, result.getDescription()));
            report.append(String.format("   Status: %s\n", result.isEqual() ? "PASS" : "FAIL"));
            report.append(String.format("   Expected: %s\n", result.getExpectedValue()));
            report.append(String.format("   Actual: %s\n", result.getActualValue()));
            
            if (result.getDifference() != null) {
                report.append(String.format("   Difference: %s\n", result.getDifference()));
            }
            
            if (!result.getValidationNotes().isEmpty()) {
                report.append("   Validation Notes:\n");
                for (String note : result.getValidationNotes()) {
                    report.append(String.format("     • %s\n", note));
                }
            }
        }
        
        // Critical failures section
        List<ComparisonResult> failures = results.stream()
            .filter(r -> !r.isEqual())
            .toList();
        
        if (!failures.isEmpty()) {
            report.append("\n").append("CRITICAL FAILURES REQUIRING IMMEDIATE ATTENTION\n");
            report.append("=".repeat(50)).append("\n");
            
            for (ComparisonResult failure : failures) {
                report.append(String.format("• %s\n", failure.getDescription()));
                report.append(String.format("  Expected: %s, Actual: %s\n", 
                                           failure.getExpectedValue(), failure.getActualValue()));
            }
        }
        
        report.append("\n").append("END OF REPORT\n");
        
        String reportString = report.toString();
        log.info("Comparison report generated: {} comparisons, {} failures", results.size(), failedCount);
        
        return reportString;
    }

    /**
     * Validates interest rate calculations with COBOL precision requirements.
     * 
     * @param cobolRate COBOL calculated interest rate
     * @param javaRate Java calculated interest rate  
     * @return ComparisonResult for interest rate validation
     */
    public ComparisonResult validateInterestCalculation(BigDecimal cobolRate, BigDecimal javaRate) {
        // Interest rates typically have 4 decimal places in COBOL systems
        BigDecimal normalizedCobol = cobolRate != null 
            ? cobolRate.setScale(COBOL_INTEREST_SCALE, COBOL_ROUNDING_MODE) 
            : null;
        BigDecimal normalizedJava = javaRate != null 
            ? javaRate.setScale(COBOL_INTEREST_SCALE, COBOL_ROUNDING_MODE) 
            : null;
        
        ComparisonResult result = compareBigDecimals(normalizedCobol, normalizedJava, 
                                                    "Interest Rate Calculation");
        result.addValidationNote("Interest rate precision: 4 decimal places required");
        
        return result;
    }

    /**
     * Validates account balance calculations ensuring COBOL equivalence.
     * 
     * @param cobolBalance COBOL calculated balance
     * @param javaBalance Java calculated balance
     * @return ComparisonResult for balance validation
     */
    public ComparisonResult validateBalanceCalculation(BigDecimal cobolBalance, BigDecimal javaBalance) {
        return validateFinancialPrecision(cobolBalance, javaBalance, "Account Balance Calculation");
    }

    /**
     * Validates payment amount processing with exact precision requirements.
     * 
     * @param cobolAmount COBOL processed payment amount
     * @param javaAmount Java processed payment amount
     * @return ComparisonResult for payment amount validation
     */
    public ComparisonResult validatePaymentAmount(BigDecimal cobolAmount, BigDecimal javaAmount) {
        return validateFinancialPrecision(cobolAmount, javaAmount, "Payment Amount Processing");
    }

    // Private utility methods
    
    /**
     * Normalizes BigDecimal values for COBOL comparison by applying appropriate
     * scale and rounding mode matching COBOL COMP-3 behavior.
     */
    private static BigDecimal normalizeForCobolComparison(BigDecimal value) {
        if (value == null) {
            return null;
        }
        
        // Determine appropriate scale based on value magnitude
        int targetScale = COBOL_CURRENCY_SCALE;
        if (value.abs().compareTo(new BigDecimal("1")) < 0) {
            // For values less than 1, might be interest rates or percentages
            targetScale = COBOL_INTEREST_SCALE;
        }
        
        return value.setScale(targetScale, COBOL_ROUNDING_MODE);
    }

    /**
     * Creates a summary comparison result for a collection of individual comparisons.
     * 
     * @param results individual comparison results
     * @param summaryDescription description for the summary
     * @return aggregate ComparisonResult
     */
    public ComparisonResult createSummaryResult(List<ComparisonResult> results, String summaryDescription) {
        boolean allPassed = results.stream().allMatch(ComparisonResult::isEqual);
        long totalComparisons = results.size();
        long passedComparisons = results.stream().mapToLong(r -> r.isEqual() ? 1 : 0).sum();
        
        ComparisonResult summary = new ComparisonResult(allPassed, summaryDescription, null, null);
        summary.addValidationNote(String.format("Total Comparisons: %d", totalComparisons));
        summary.addValidationNote(String.format("Passed: %d", passedComparisons));
        summary.addValidationNote(String.format("Failed: %d", totalComparisons - passedComparisons));
        summary.addValidationNote(String.format("Success Rate: %.2f%%", 
                                               totalComparisons > 0 ? (double) passedComparisons / totalComparisons * 100 : 0.0));
        
        if (allPassed) {
            summary.addValidationNote("All individual validations passed");
        } else {
            summary.addValidationNote("Some validations failed - detailed investigation required");
        }
        
        return summary;
    }

    /**
     * Validates FICO score precision equivalence between COBOL and Java implementations.
     * Ensures that FICO score values maintain identical precision and scale as COBOL COMP-3 values.
     *
     * @param actualFicoScore The Java Integer FICO score value
     * @param expectedFicoScore The expected COBOL equivalent FICO score value
     * @param customerId Customer identifier for error reporting
     * @throws AssertionError if precision does not match COBOL equivalent
     */
    public static void validateFicoScorePrecision(Integer actualFicoScore, Integer expectedFicoScore, String customerId) {
        log.info("Validating FICO score precision for customer: {}", customerId);
        
        if (actualFicoScore == null && expectedFicoScore == null) {
            log.debug("Both FICO scores are null for customer: {} - validation passed", customerId);
            return; // Both null is valid
        }
        
        if (actualFicoScore == null || expectedFicoScore == null) {
            String errorMsg = String.format(
                "FICO score precision validation failed for customer %s: one value is null - actual: %s, expected: %s",
                customerId, actualFicoScore, expectedFicoScore
            );
            log.error(errorMsg);
            throw new AssertionError(errorMsg);
        }
        
        // FICO scores are integer values in COBOL, typically PIC 9(3) format (range 300-850)
        if (!actualFicoScore.equals(expectedFicoScore)) {
            String errorMsg = String.format(
                "FICO score precision validation failed for customer %s: actual value %d does not match expected value %d",
                customerId, actualFicoScore, expectedFicoScore
            );
            log.error(errorMsg);
            throw new AssertionError(errorMsg);
        }
        
        // Validate FICO score range (300-850 is standard range)
        if (actualFicoScore < 300 || actualFicoScore > 850) {
            String errorMsg = String.format(
                "FICO score validation failed for customer %s: value %d is outside valid range (300-850)",
                customerId, actualFicoScore
            );
            log.error(errorMsg);
            throw new AssertionError(errorMsg);
        }
        
        log.info("FICO score precision validation passed for customer: {} with score: {}", customerId, actualFicoScore);
    }
}