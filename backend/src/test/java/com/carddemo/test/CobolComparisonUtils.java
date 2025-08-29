package com.carddemo.test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

/**
 * Specialized utility for validating functional parity between Java and COBOL implementations.
 * Provides BigDecimal precision validation, numeric comparison with COMP-3 equivalent behavior,
 * and date format verification.
 */
public class CobolComparisonUtils {

    // COBOL COMP-3 decimal scale matching
    public static final int COBOL_CURRENCY_SCALE = 2;
    public static final int COBOL_RATE_SCALE = 4;
    public static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    // COBOL date formats
    private static final DateTimeFormatter COBOL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Precision tolerance for floating point comparisons
    private static final BigDecimal PRECISION_TOLERANCE = new BigDecimal("0.001");

    /**
     * Compares numeric precision between Java BigDecimal and expected COBOL COMP-3 behavior
     */
    public static boolean compareNumericPrecision(BigDecimal javaValue, BigDecimal expectedCobolValue) {
        if (javaValue == null && expectedCobolValue == null) {
            return true;
        }
        if (javaValue == null || expectedCobolValue == null) {
            return false;
        }
        
        // Ensure both values have the same scale for comparison
        BigDecimal normalizedJava = javaValue.setScale(COBOL_CURRENCY_SCALE, COBOL_ROUNDING_MODE);
        BigDecimal normalizedCobol = expectedCobolValue.setScale(COBOL_CURRENCY_SCALE, COBOL_ROUNDING_MODE);
        
        return normalizedJava.compareTo(normalizedCobol) == 0;
    }

    /**
     * Validates that a BigDecimal value matches COMP-3 equivalent behavior
     */
    public static boolean validateComp3Equivalent(BigDecimal value, int expectedScale) {
        if (value == null) {
            return false;
        }
        
        // Check that scale matches COBOL COMP-3 expectations
        return value.scale() == expectedScale;
    }

    /**
     * Compares date formats between Java LocalDateTime and COBOL date patterns
     */
    public static boolean compareDateFormats(LocalDateTime javaDate, String expectedCobolDateString) {
        if (javaDate == null && expectedCobolDateString == null) {
            return true;
        }
        if (javaDate == null || expectedCobolDateString == null) {
            return false;
        }
        
        String javaDateString = javaDate.format(COBOL_DATE_FORMAT);
        return javaDateString.equals(expectedCobolDateString);
    }

    /**
     * Asserts functional parity between Java and COBOL implementations
     */
    public static void assertFunctionalParity(Map<String, Object> javaResults, Map<String, Object> cobolResults) {
        if (javaResults == null || cobolResults == null) {
            throw new AssertionError("Cannot compare null result sets");
        }
        
        // Check that both result sets have the same keys
        if (!javaResults.keySet().equals(cobolResults.keySet())) {
            throw new AssertionError("Java and COBOL results have different field sets");
        }
        
        // Compare each field
        for (String key : javaResults.keySet()) {
            Object javaValue = javaResults.get(key);
            Object cobolValue = cobolResults.get(key);
            
            if (!compareValues(javaValue, cobolValue)) {
                throw new AssertionError(String.format(
                    "Functional parity failed for field '%s': Java=%s, COBOL=%s", 
                    key, javaValue, cobolValue
                ));
            }
        }
    }

    /**
     * Validates transaction equivalence between Java and COBOL processing
     */
    public static boolean validateTransactionEquivalence(
            BigDecimal javaAmount, 
            BigDecimal cobolAmount, 
            BigDecimal javaBalance, 
            BigDecimal cobolBalance) {
        
        boolean amountMatch = compareNumericPrecision(javaAmount, cobolAmount);
        boolean balanceMatch = compareNumericPrecision(javaBalance, cobolBalance);
        
        return amountMatch && balanceMatch;
    }

    /**
     * Compares BigDecimal values with COBOL precision semantics
     */
    public static boolean compareBigDecimals(BigDecimal value1, BigDecimal value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        
        // Use COBOL rounding semantics for comparison
        BigDecimal normalized1 = value1.setScale(COBOL_CURRENCY_SCALE, COBOL_ROUNDING_MODE);
        BigDecimal normalized2 = value2.setScale(COBOL_CURRENCY_SCALE, COBOL_ROUNDING_MODE);
        
        return normalized1.compareTo(normalized2) == 0;
    }

    /**
     * Compares BigDecimal values with COBOL precision semantics and custom message
     */
    public static boolean compareBigDecimals(BigDecimal value1, BigDecimal value2, String message) {
        boolean result = compareBigDecimals(value1, value2);
        if (!result && message != null && !message.trim().isEmpty()) {
            System.out.println("Comparison failed: " + message + " - Expected: " + value2 + ", Actual: " + value1);
        }
        return result;
    }

    /**
     * Compares file outputs for batch processing validation
     */
    public static boolean compareFiles(String javaOutput, String cobolOutput) {
        if (javaOutput == null && cobolOutput == null) {
            return true;
        }
        if (javaOutput == null || cobolOutput == null) {
            return false;
        }
        
        // Normalize line endings and whitespace for comparison
        String normalizedJava = javaOutput.replaceAll("\\r\\n|\\r", "\\n").trim();
        String normalizedCobol = cobolOutput.replaceAll("\\r\\n|\\r", "\\n").trim();
        
        return normalizedJava.equals(normalizedCobol);
    }

    /**
     * Generates comparison report for debugging mismatches
     */
    public static String generateComparisonReport(Object javaResult, Object cobolResult) {
        StringBuilder report = new StringBuilder();
        report.append("COBOL-Java Comparison Report\n");
        report.append("============================\n");
        report.append("Java Result:  ").append(javaResult).append("\n");
        report.append("COBOL Result: ").append(cobolResult).append("\n");
        
        if (javaResult instanceof BigDecimal && cobolResult instanceof BigDecimal) {
            BigDecimal javaBD = (BigDecimal) javaResult;
            BigDecimal cobolBD = (BigDecimal) cobolResult;
            
            report.append("Java Scale:   ").append(javaBD.scale()).append("\n");
            report.append("COBOL Scale:  ").append(cobolBD.scale()).append("\n");
            report.append("Precision Match: ").append(compareNumericPrecision(javaBD, cobolBD)).append("\n");
        }
        
        report.append("Objects Equal: ").append(compareValues(javaResult, cobolResult)).append("\n");
        
        return report.toString();
    }

    /**
     * Validates precision for interest rate calculations
     */
    public static boolean validatePrecision(BigDecimal calculated, BigDecimal expected) {
        if (calculated == null || expected == null) {
            return calculated == expected;
        }
        
        BigDecimal difference = calculated.subtract(expected).abs();
        return difference.compareTo(PRECISION_TOLERANCE) <= 0;
    }

    /**
     * Compares financial calculations with COBOL ROUNDED clause behavior
     */
    public static boolean compareFinancialCalculations(
            BigDecimal javaCalculation, 
            BigDecimal cobolCalculation) {
        
        if (javaCalculation == null && cobolCalculation == null) {
            return true;
        }
        if (javaCalculation == null || cobolCalculation == null) {
            return false;
        }
        
        // Apply COBOL ROUNDED semantics
        BigDecimal javaRounded = javaCalculation.setScale(COBOL_CURRENCY_SCALE, COBOL_ROUNDING_MODE);
        BigDecimal cobolRounded = cobolCalculation.setScale(COBOL_CURRENCY_SCALE, COBOL_ROUNDING_MODE);
        
        return javaRounded.compareTo(cobolRounded) == 0;
    }

    /**
     * Validates data integrity between Java JPA entities and COBOL record layouts
     */
    public static boolean validateDataIntegrity(Map<String, Object> jpaData, Map<String, Object> cobolData) {
        if (jpaData == null || cobolData == null) {
            return false;
        }
        
        // Check all COBOL fields are present in JPA data
        for (String cobolField : cobolData.keySet()) {
            if (!jpaData.containsKey(cobolField)) {
                return false;
            }
            
            Object jpaValue = jpaData.get(cobolField);
            Object cobolValue = cobolData.get(cobolField);
            
            if (!compareValues(jpaValue, cobolValue)) {
                return false;
            }
        }
        
        return true;
    }

    // Private helper methods
    private static boolean compareValues(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        
        // Special handling for BigDecimal comparison
        if (value1 instanceof BigDecimal && value2 instanceof BigDecimal) {
            return compareBigDecimals((BigDecimal) value1, (BigDecimal) value2);
        }
        
        // Special handling for LocalDateTime comparison
        if (value1 instanceof LocalDateTime && value2 instanceof LocalDateTime) {
            return ((LocalDateTime) value1).isEqual((LocalDateTime) value2);
        }
        
        // Default equality check
        return value1.equals(value2);
    }

    /**
     * Creates a test result map for COBOL comparison
     */
    public static Map<String, Object> createCobolTestResult(
            BigDecimal amount, 
            BigDecimal balance, 
            String status, 
            LocalDateTime timestamp) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("amount", amount);
        result.put("balance", balance);
        result.put("status", status);
        result.put("timestamp", timestamp);
        
        return result;
    }

    /**
     * Creates a test result map for Java comparison
     */
    public static Map<String, Object> createJavaTestResult(
            BigDecimal amount, 
            BigDecimal balance, 
            String status, 
            LocalDateTime timestamp) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("amount", amount);
        result.put("balance", balance);
        result.put("status", status);
        result.put("timestamp", timestamp);
        
        return result;
    }

    /**
     * Validates financial precision between Java BigDecimal and expected COBOL values
     */
    public static boolean validateFinancialPrecision(BigDecimal actual, BigDecimal expected, String context) {
        boolean result = compareBigDecimals(actual, expected);
        if (!result && context != null && !context.trim().isEmpty()) {
            System.out.println("Financial precision validation failed in " + context + " - Expected: " + expected + ", Actual: " + actual);
        }
        return result;
    }

    /**
     * Validates currency amount format and precision
     */
    public static boolean validateCurrencyAmount(BigDecimal amount, String context) {
        if (amount == null) {
            System.out.println("Currency validation failed in " + context + " - Amount is null");
            return false;
        }
        
        // Ensure proper currency scale (2 decimal places)
        boolean scaleValid = amount.scale() <= COBOL_CURRENCY_SCALE;
        if (!scaleValid && context != null) {
            System.out.println("Currency scale validation failed in " + context + " - Scale: " + amount.scale() + ", Expected max: " + COBOL_CURRENCY_SCALE);
        }
        
        return scaleValid;
    }

    /**
     * Validates FICO score precision (typically integer value)
     */
    public static boolean validateFicoScorePrecision(BigDecimal actual, BigDecimal expected, String context) {
        if (actual == null || expected == null) {
            boolean result = (actual == expected);
            if (!result && context != null) {
                System.out.println("FICO score validation failed in " + context + " - One value is null");
            }
            return result;
        }
        
        // FICO scores are typically integers, so we use no decimal places
        BigDecimal actualRounded = actual.setScale(0, COBOL_ROUNDING_MODE);
        BigDecimal expectedRounded = expected.setScale(0, COBOL_ROUNDING_MODE);
        
        boolean result = actualRounded.compareTo(expectedRounded) == 0;
        if (!result && context != null) {
            System.out.println("FICO score precision validation failed in " + context + " - Expected: " + expectedRounded + ", Actual: " + actualRounded);
        }
        
        return result;
    }

    /**
     * ComparisonResult class for detailed comparison results
     */
    public static class ComparisonResult {
        private final String field;
        private final Object javaValue;
        private final Object cobolValue;
        private final boolean matches;
        private final String message;

        public ComparisonResult(String field, Object javaValue, Object cobolValue, boolean matches, String message) {
            this.field = field;
            this.javaValue = javaValue;
            this.cobolValue = cobolValue;
            this.matches = matches;
            this.message = message;
        }

        public String getField() { return field; }
        public Object getJavaValue() { return javaValue; }
        public Object getCobolValue() { return cobolValue; }
        public boolean isMatches() { return matches; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return String.format("ComparisonResult{field='%s', matches=%s, message='%s'}", field, matches, message);
        }
    }

    /**
     * Creates a ComparisonResult for detailed comparison tracking
     */
    public static ComparisonResult createComparisonResult(String field, Object javaValue, Object cobolValue, boolean matches, String message) {
        return new ComparisonResult(field, javaValue, cobolValue, matches, message);
    }
    
    /**
     * Validates COBOL parity for a specific paragraph/section
     */
    public static boolean validateCobolParity(Object javaResult, Object cobolResult, String section) {
        if (javaResult == null && cobolResult == null) {
            return true;
        }
        if (javaResult == null || cobolResult == null) {
            return false;
        }
        
        // For boolean results, direct comparison
        if (javaResult instanceof Boolean && cobolResult instanceof Boolean) {
            return javaResult.equals(cobolResult);
        }
        
        // For other types, use string comparison
        return javaResult.toString().equals(cobolResult.toString());
    }
    
    /**
     * Compares balance calculations between COBOL and Java implementations.
     * @param cobolBalance COBOL calculated balance
     * @param javaBalance Java calculated balance
     * @return true if calculations match
     */
    public static boolean compareBalanceCalculations(BigDecimal cobolBalance, BigDecimal javaBalance) {
        return compareBigDecimals(javaBalance, cobolBalance);
    }
    
    /**
     * Compares transaction processing results between COBOL and Java.
     * @param cobolResult COBOL processing result
     * @param javaResult Java processing result
     * @return true if processing matches
     */
    public static boolean compareTransactionProcessing(Object cobolResult, Object javaResult) {
        // Validates that transaction processing produces equivalent results
        if (cobolResult == null && javaResult == null) {
            return true;
        }
        if (cobolResult == null || javaResult == null) {
            return false;
        }
        return cobolResult.toString().equals(javaResult.toString());
    }
    
    /**
     * Generates a comprehensive comparison report.
     * @return Formatted comparison report
     */
    public static String generateComparisonReport() {
        return "COBOL-Java Comparison Report Generated: " + 
               LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}