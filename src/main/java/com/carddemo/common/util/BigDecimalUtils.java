package com.carddemo.common.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * BigDecimalUtils - Utility class providing exact financial calculations using BigDecimal 
 * with MathContext.DECIMAL128 to replicate COBOL COMP-3 precision.
 * 
 * This class ensures all monetary operations maintain identical decimal precision as the 
 * original COBOL implementation, preventing floating-point errors in critical financial 
 * calculations. All arithmetic operations use HALF_EVEN rounding to replicate COBOL 
 * arithmetic behavior exactly.
 * 
 * Key Features:
 * - Exact COBOL COMP-3 decimal precision using BigDecimal with MathContext.DECIMAL128
 * - Financial calculation utilities maintaining identical results with exact decimal precision
 * - Rounding mode HALF_EVEN to replicate COBOL arithmetic behavior
 * - Utility methods for monetary operations with precision preservation
 * 
 * Technical Compliance:
 * - Implements Section 0.1.2 requirement for exact decimal precision using BigDecimal 
 *   with MathContext.DECIMAL128 for financial calculations
 * - All COBOL COMP-3 packed decimal arithmetic produces identical results using Java BigDecimal
 * - Financial calculations preserve exact decimal precision without floating-point errors
 * - BigDecimal operations use DECIMAL128 context matching COBOL precision per technical specification
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public class BigDecimalUtils {
    
    /**
     * MathContext with DECIMAL128 precision for exact COBOL COMP-3 equivalency.
     * This provides 34 decimal digits of precision with HALF_EVEN rounding mode,
     * ensuring financial calculations match COBOL packed decimal arithmetic exactly.
     */
    public static final MathContext DECIMAL128_CONTEXT = new MathContext(34, RoundingMode.HALF_EVEN);
    
    /**
     * Standard monetary scale for financial calculations.
     * This matches PostgreSQL DECIMAL(12,2) precision and COBOL COMP-3 scale
     * for all monetary fields in the CardDemo system.
     */
    public static final int MONETARY_SCALE = 2;
    
    /**
     * Zero constant with proper monetary scale for financial calculations.
     * Pre-computed for performance in comparison operations.
     */
    private static final BigDecimal ZERO_MONETARY = BigDecimal.ZERO.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    
    /**
     * Currency formatter for US dollar display with proper monetary formatting.
     * Configured for consistent display of financial amounts across the system.
     */
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00");
    
    /**
     * Decimal formatter for numeric display without currency symbols.
     * Used for numeric display of financial amounts in calculation contexts.
     */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    
    /**
     * Private constructor to prevent instantiation.
     * This utility class contains only static methods and should not be instantiated.
     */
    private BigDecimalUtils() {
        throw new UnsupportedOperationException("BigDecimalUtils is a utility class and cannot be instantiated");
    }
    
    /**
     * Creates a BigDecimal with exact COBOL COMP-3 precision from a double value.
     * 
     * This method converts a double value to BigDecimal using DECIMAL128 context
     * and sets the scale to MONETARY_SCALE with HALF_EVEN rounding, ensuring
     * exact precision equivalent to COBOL packed decimal fields.
     * 
     * @param value the double value to convert to BigDecimal
     * @return BigDecimal with exact COBOL COMP-3 precision and monetary scale
     * @throws NumberFormatException if the value cannot be converted to BigDecimal
     */
    public static BigDecimal createDecimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new NumberFormatException("Cannot create BigDecimal from NaN or Infinite value: " + value);
        }
        return BigDecimal.valueOf(value).setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Creates a BigDecimal with exact COBOL COMP-3 precision from a long value.
     * 
     * This method converts a long value to BigDecimal using DECIMAL128 context
     * and sets the scale to MONETARY_SCALE, ensuring exact precision for
     * whole number monetary values.
     * 
     * @param value the long value to convert to BigDecimal
     * @return BigDecimal with exact COBOL COMP-3 precision and monetary scale
     */
    public static BigDecimal createDecimal(long value) {
        return BigDecimal.valueOf(value).setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Creates a BigDecimal with exact COBOL COMP-3 precision from a string value.
     * 
     * This method parses a string value to BigDecimal using DECIMAL128 context
     * and sets the scale to MONETARY_SCALE with HALF_EVEN rounding, ensuring
     * exact precision for string-based monetary input.
     * 
     * @param value the string value to convert to BigDecimal
     * @return BigDecimal with exact COBOL COMP-3 precision and monetary scale
     * @throws NumberFormatException if the string cannot be parsed as a valid decimal number
     */
    public static BigDecimal createDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new NumberFormatException("Cannot create BigDecimal from null or empty string");
        }
        
        // Remove currency symbols and whitespace for parsing
        String cleanValue = value.trim().replaceAll("[$,\\s]", "");
        
        try {
            BigDecimal decimal = new BigDecimal(cleanValue, DECIMAL128_CONTEXT);
            return decimal.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot parse string as BigDecimal: " + value);
        }
    }
    
    /**
     * Adds two BigDecimal values with exact COBOL COMP-3 precision.
     * 
     * This method performs addition using DECIMAL128 context and maintains
     * monetary scale with HALF_EVEN rounding, ensuring results match COBOL
     * packed decimal arithmetic exactly.
     * 
     * @param augend the first BigDecimal value (augend)
     * @param addend the second BigDecimal value (addend)
     * @return BigDecimal result of addition with exact COBOL COMP-3 precision
     * @throws IllegalArgumentException if either parameter is null
     */
    public static BigDecimal add(BigDecimal augend, BigDecimal addend) {
        if (augend == null || addend == null) {
            throw new IllegalArgumentException("BigDecimal parameters cannot be null for addition");
        }
        
        BigDecimal result = augend.add(addend, DECIMAL128_CONTEXT);
        return result.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Subtracts two BigDecimal values with exact COBOL COMP-3 precision.
     * 
     * This method performs subtraction using DECIMAL128 context and maintains
     * monetary scale with HALF_EVEN rounding, ensuring results match COBOL
     * packed decimal arithmetic exactly.
     * 
     * @param minuend the BigDecimal value to subtract from (minuend)
     * @param subtrahend the BigDecimal value to subtract (subtrahend)
     * @return BigDecimal result of subtraction with exact COBOL COMP-3 precision
     * @throws IllegalArgumentException if either parameter is null
     */
    public static BigDecimal subtract(BigDecimal minuend, BigDecimal subtrahend) {
        if (minuend == null || subtrahend == null) {
            throw new IllegalArgumentException("BigDecimal parameters cannot be null for subtraction");
        }
        
        BigDecimal result = minuend.subtract(subtrahend, DECIMAL128_CONTEXT);
        return result.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Multiplies two BigDecimal values with exact COBOL COMP-3 precision.
     * 
     * This method performs multiplication using DECIMAL128 context and maintains
     * monetary scale with HALF_EVEN rounding, ensuring results match COBOL
     * packed decimal arithmetic exactly. Commonly used for interest calculations
     * and fee computations.
     * 
     * @param multiplicand the first BigDecimal value (multiplicand)
     * @param multiplier the second BigDecimal value (multiplier)
     * @return BigDecimal result of multiplication with exact COBOL COMP-3 precision
     * @throws IllegalArgumentException if either parameter is null
     */
    public static BigDecimal multiply(BigDecimal multiplicand, BigDecimal multiplier) {
        if (multiplicand == null || multiplier == null) {
            throw new IllegalArgumentException("BigDecimal parameters cannot be null for multiplication");
        }
        
        BigDecimal result = multiplicand.multiply(multiplier, DECIMAL128_CONTEXT);
        return result.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Divides two BigDecimal values with exact COBOL COMP-3 precision.
     * 
     * This method performs division using DECIMAL128 context and maintains
     * monetary scale with HALF_EVEN rounding, ensuring results match COBOL
     * packed decimal arithmetic exactly. Includes protection against division
     * by zero with appropriate error handling.
     * 
     * @param dividend the BigDecimal value to divide (dividend)
     * @param divisor the BigDecimal value to divide by (divisor)
     * @return BigDecimal result of division with exact COBOL COMP-3 precision
     * @throws IllegalArgumentException if either parameter is null
     * @throws ArithmeticException if divisor is zero
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null) {
            throw new IllegalArgumentException("BigDecimal parameters cannot be null for division");
        }
        
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        
        BigDecimal result = dividend.divide(divisor, DECIMAL128_CONTEXT);
        return result.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Compares two BigDecimal values with exact COBOL COMP-3 precision.
     * 
     * This method performs comparison after ensuring both values have the same
     * scale, providing consistent comparison behavior equivalent to COBOL
     * packed decimal comparison operations.
     * 
     * @param value1 the first BigDecimal value to compare
     * @param value2 the second BigDecimal value to compare
     * @return -1 if value1 < value2, 0 if value1 == value2, 1 if value1 > value2
     * @throws IllegalArgumentException if either parameter is null
     */
    public static int compare(BigDecimal value1, BigDecimal value2) {
        if (value1 == null || value2 == null) {
            throw new IllegalArgumentException("BigDecimal parameters cannot be null for comparison");
        }
        
        // Ensure both values have the same scale for accurate comparison
        BigDecimal scaledValue1 = value1.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal scaledValue2 = value2.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        
        return scaledValue1.compareTo(scaledValue2);
    }
    
    /**
     * Formats a BigDecimal value as currency with proper monetary display.
     * 
     * This method formats a BigDecimal value using US currency formatting
     * with dollar sign, thousands separators, and two decimal places,
     * providing consistent monetary display across the system.
     * 
     * @param value the BigDecimal value to format as currency
     * @return formatted currency string (e.g., "$1,234.56")
     * @throws IllegalArgumentException if value is null
     */
    public static String formatCurrency(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("BigDecimal value cannot be null for currency formatting");
        }
        
        // Ensure proper scale before formatting
        BigDecimal scaledValue = value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        
        synchronized (CURRENCY_FORMAT) {
            return CURRENCY_FORMAT.format(scaledValue);
        }
    }
    
    /**
     * Parses a decimal string with exact COBOL COMP-3 precision.
     * 
     * This method parses a string representation of a decimal number,
     * handling various input formats including currency symbols and
     * thousands separators, and returns a BigDecimal with exact
     * COBOL COMP-3 precision.
     * 
     * @param value the string value to parse as decimal
     * @return BigDecimal with exact COBOL COMP-3 precision and monetary scale
     * @throws IllegalArgumentException if value is null or empty
     * @throws NumberFormatException if the string cannot be parsed as a valid decimal number
     */
    public static BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("String value cannot be null or empty for decimal parsing");
        }
        
        // Remove currency symbols, whitespace, and thousands separators for parsing
        String cleanValue = value.trim()
                .replaceAll("[$]", "")
                .replaceAll("[,\\s]", "");
        
        try {
            BigDecimal decimal = new BigDecimal(cleanValue, DECIMAL128_CONTEXT);
            return decimal.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot parse string as decimal: " + value + " (cleaned: " + cleanValue + ")");
        }
    }
    
    /**
     * Validates if a BigDecimal value is within acceptable monetary range.
     * 
     * This method checks if a BigDecimal value is within the acceptable range
     * for monetary values in the CardDemo system, based on PostgreSQL 
     * DECIMAL(12,2) constraints and business requirements.
     * 
     * @param value the BigDecimal value to validate
     * @return true if the value is within acceptable monetary range, false otherwise
     */
    public static boolean isValidMonetaryAmount(BigDecimal value) {
        if (value == null) {
            return false;
        }
        
        // Check against PostgreSQL DECIMAL(12,2) limits: -9999999999.99 to 9999999999.99
        BigDecimal maxValue = new BigDecimal("9999999999.99");
        BigDecimal minValue = new BigDecimal("-9999999999.99");
        
        return value.compareTo(minValue) >= 0 && value.compareTo(maxValue) <= 0;
    }
    
    /**
     * Checks if a BigDecimal value is zero with proper scale consideration.
     * 
     * This method performs zero comparison after ensuring proper monetary scale,
     * providing consistent zero checking behavior for financial calculations.
     * 
     * @param value the BigDecimal value to check for zero
     * @return true if the value is zero, false otherwise
     */
    public static boolean isZero(BigDecimal value) {
        if (value == null) {
            return false;
        }
        
        BigDecimal scaledValue = value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        return scaledValue.compareTo(ZERO_MONETARY) == 0;
    }
    
    /**
     * Checks if a BigDecimal value is positive with proper scale consideration.
     * 
     * This method performs positive comparison after ensuring proper monetary scale,
     * providing consistent positive checking behavior for financial calculations.
     * 
     * @param value the BigDecimal value to check for positive
     * @return true if the value is positive, false otherwise
     */
    public static boolean isPositive(BigDecimal value) {
        if (value == null) {
            return false;
        }
        
        BigDecimal scaledValue = value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        return scaledValue.compareTo(ZERO_MONETARY) > 0;
    }
    
    /**
     * Checks if a BigDecimal value is negative with proper scale consideration.
     * 
     * This method performs negative comparison after ensuring proper monetary scale,
     * providing consistent negative checking behavior for financial calculations.
     * 
     * @param value the BigDecimal value to check for negative
     * @return true if the value is negative, false otherwise
     */
    public static boolean isNegative(BigDecimal value) {
        if (value == null) {
            return false;
        }
        
        BigDecimal scaledValue = value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        return scaledValue.compareTo(ZERO_MONETARY) < 0;
    }
    
    /**
     * Returns the absolute value of a BigDecimal with exact COBOL COMP-3 precision.
     * 
     * This method computes the absolute value using DECIMAL128 context and maintains
     * monetary scale with HALF_EVEN rounding, ensuring results match COBOL
     * packed decimal arithmetic exactly.
     * 
     * @param value the BigDecimal value to get absolute value of
     * @return BigDecimal absolute value with exact COBOL COMP-3 precision
     * @throws IllegalArgumentException if value is null
     */
    public static BigDecimal abs(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("BigDecimal value cannot be null for absolute value calculation");
        }
        
        BigDecimal result = value.abs(DECIMAL128_CONTEXT);
        return result.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Returns the maximum of two BigDecimal values with exact COBOL COMP-3 precision.
     * 
     * This method compares two BigDecimal values and returns the maximum value
     * with proper monetary scale, providing consistent maximum selection behavior
     * for financial calculations.
     * 
     * @param value1 the first BigDecimal value
     * @param value2 the second BigDecimal value
     * @return BigDecimal maximum value with exact COBOL COMP-3 precision
     * @throws IllegalArgumentException if either parameter is null
     */
    public static BigDecimal max(BigDecimal value1, BigDecimal value2) {
        if (value1 == null || value2 == null) {
            throw new IllegalArgumentException("BigDecimal parameters cannot be null for maximum calculation");
        }
        
        BigDecimal scaledValue1 = value1.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal scaledValue2 = value2.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        
        return scaledValue1.compareTo(scaledValue2) >= 0 ? scaledValue1 : scaledValue2;
    }
    
    /**
     * Returns the minimum of two BigDecimal values with exact COBOL COMP-3 precision.
     * 
     * This method compares two BigDecimal values and returns the minimum value
     * with proper monetary scale, providing consistent minimum selection behavior
     * for financial calculations.
     * 
     * @param value1 the first BigDecimal value
     * @param value2 the second BigDecimal value
     * @return BigDecimal minimum value with exact COBOL COMP-3 precision
     * @throws IllegalArgumentException if either parameter is null
     */
    public static BigDecimal min(BigDecimal value1, BigDecimal value2) {
        if (value1 == null || value2 == null) {
            throw new IllegalArgumentException("BigDecimal parameters cannot be null for minimum calculation");
        }
        
        BigDecimal scaledValue1 = value1.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal scaledValue2 = value2.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        
        return scaledValue1.compareTo(scaledValue2) <= 0 ? scaledValue1 : scaledValue2;
    }
}