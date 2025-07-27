package com.carddemo.common.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * BigDecimalUtils - Utility class providing exact financial calculations using BigDecimal 
 * with MathContext.DECIMAL128 to replicate COBOL COMP-3 precision.
 * 
 * This utility ensures all monetary operations maintain identical decimal precision as the 
 * original COBOL implementation, preventing floating-point errors in critical financial 
 * calculations as mandated by Section 0.1.2 of the technical specification.
 * 
 * Key Features:
 * - Exact COBOL COMP-3 decimal precision using BigDecimal with MathContext.DECIMAL128
 * - HALF_EVEN rounding mode to replicate COBOL arithmetic behavior
 * - Financial calculation utilities maintaining identical results with exact decimal precision
 * - Currency formatting and parsing with precision preservation
 * 
 * All financial calculations MUST use these utilities to ensure regulatory compliance
 * and maintain exact equivalence with legacy mainframe arithmetic operations.
 * 
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public final class BigDecimalUtils {

    /**
     * DECIMAL128 MathContext for exact financial precision matching COBOL COMP-3 arithmetic.
     * Uses 34 decimal digit precision with HALF_EVEN rounding mode to replicate
     * mainframe financial calculation behavior exactly.
     */
    public static final MathContext DECIMAL128_CONTEXT = MathContext.DECIMAL128;
    
    /**
     * Alternative precision contexts for specialized calculations when needed.
     * DECIMAL64 and DECIMAL32 provided for compatibility with specific COBOL field types.
     */
    public static final MathContext DECIMAL64_CONTEXT = MathContext.DECIMAL64;
    public static final MathContext DECIMAL32_CONTEXT = MathContext.DECIMAL32;
    public static final MathContext UNLIMITED_CONTEXT = MathContext.UNLIMITED;
    
    /**
     * Standard monetary scale for currency amounts - 2 decimal places.
     * Matches COBOL PIC S9(10)V99 COMP-3 field definitions for financial amounts.
     */
    public static final int MONETARY_SCALE = 2;
    
    /**
     * Scale for interest rate calculations - 4 decimal places.
     * Matches COBOL PIC S9(1)V9999 COMP-3 field definitions for interest rates.
     */
    public static final int INTEREST_RATE_SCALE = 4;
    
    /**
     * Maximum precision for account balances and credit limits.
     * Supports up to 9,999,999,999.99 matching COBOL S9(10)V99 COMP-3 fields.
     */
    public static final int BALANCE_PRECISION = 12;
    
    /**
     * Zero constant with proper monetary scale for performance optimization.
     */
    public static final BigDecimal ZERO_MONETARY = BigDecimal.ZERO.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    
    /**
     * One constant with proper monetary scale for calculations.
     */
    public static final BigDecimal ONE_MONETARY = BigDecimal.ONE.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    
    /**
     * Currency formatter for US Dollar amounts with proper precision.
     */
    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("$#,##0.00");
    
    /**
     * Number formatter for parsing monetary values without currency symbols.
     */
    private static final NumberFormat MONETARY_PARSER = NumberFormat.getNumberInstance(Locale.US);
    
    static {
        // Configure currency formatter for exact precision
        CURRENCY_FORMATTER.setMinimumFractionDigits(MONETARY_SCALE);
        CURRENCY_FORMATTER.setMaximumFractionDigits(MONETARY_SCALE);
        CURRENCY_FORMATTER.setRoundingMode(RoundingMode.HALF_EVEN);
        
        // Configure monetary parser for exact precision
        MONETARY_PARSER.setMinimumFractionDigits(0);
        MONETARY_PARSER.setMaximumFractionDigits(MONETARY_SCALE);
        MONETARY_PARSER.setRoundingMode(RoundingMode.HALF_EVEN);
    }
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private BigDecimalUtils() {
        throw new UnsupportedOperationException("BigDecimalUtils is a utility class and cannot be instantiated");
    }
    
    /**
     * Creates a BigDecimal with exact monetary precision from a double value.
     * Uses DECIMAL128 context and HALF_EVEN rounding to match COBOL behavior.
     * 
     * WARNING: This method should be used sparingly as double values can introduce
     * precision issues. Prefer createDecimal(String) or createDecimal(long, int) methods.
     * 
     * @param value The double value to convert
     * @return BigDecimal with exact monetary scale and precision
     * @throws IllegalArgumentException if value is infinite or NaN
     */
    public static BigDecimal createDecimal(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new IllegalArgumentException("Cannot create BigDecimal from infinite or NaN double value: " + value);
        }
        return new BigDecimal(value, DECIMAL128_CONTEXT).setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Creates a BigDecimal with exact monetary precision from a string value.
     * This is the preferred method for creating monetary BigDecimal values
     * as it avoids any floating-point precision issues.
     * 
     * @param value The string representation of the decimal value
     * @return BigDecimal with exact monetary scale and precision
     * @throws NumberFormatException if the string is not a valid decimal representation
     * @throws IllegalArgumentException if value is null
     */
    public static BigDecimal createDecimal(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create BigDecimal from null string value");
        }
        return new BigDecimal(value.trim(), DECIMAL128_CONTEXT).setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Creates a BigDecimal with exact precision from long value and specified scale.
     * Useful for creating BigDecimal from cents or other scaled integer representations.
     * 
     * @param unscaledValue The unscaled long value
     * @param scale The scale to apply
     * @return BigDecimal with exact precision using DECIMAL128 context
     * @throws IllegalArgumentException if scale is negative
     */
    public static BigDecimal createDecimal(long unscaledValue, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("Scale cannot be negative: " + scale);
        }
        return BigDecimal.valueOf(unscaledValue, scale).round(DECIMAL128_CONTEXT);
    }
    
    /**
     * Creates a BigDecimal with monetary precision from a long value representing cents.
     * Convenience method for converting cent values to monetary BigDecimal.
     * 
     * @param cents The value in cents (e.g., 1599 for $15.99)
     * @return BigDecimal representing the monetary amount
     */
    public static BigDecimal createFromCents(long cents) {
        return createDecimal(cents, MONETARY_SCALE);
    }
    
    /**
     * Performs exact addition of two BigDecimal values using DECIMAL128 precision.
     * Maintains exact decimal precision equivalent to COBOL COMP-3 arithmetic.
     * 
     * @param augend The first operand (value being added to)
     * @param addend The second operand (value being added)
     * @return The sum with exact precision using DECIMAL128 context
     * @throws IllegalArgumentException if either operand is null
     */
    public static BigDecimal add(BigDecimal augend, BigDecimal addend) {
        if (augend == null || addend == null) {
            throw new IllegalArgumentException("Cannot perform addition with null BigDecimal operands");
        }
        return augend.add(addend, DECIMAL128_CONTEXT);
    }
    
    /**
     * Performs exact subtraction of two BigDecimal values using DECIMAL128 precision.
     * Maintains exact decimal precision equivalent to COBOL COMP-3 arithmetic.
     * 
     * @param minuend The first operand (value being subtracted from)
     * @param subtrahend The second operand (value being subtracted)
     * @return The difference with exact precision using DECIMAL128 context
     * @throws IllegalArgumentException if either operand is null
     */
    public static BigDecimal subtract(BigDecimal minuend, BigDecimal subtrahend) {
        if (minuend == null || subtrahend == null) {
            throw new IllegalArgumentException("Cannot perform subtraction with null BigDecimal operands");
        }
        return minuend.subtract(subtrahend, DECIMAL128_CONTEXT);
    }
    
    /**
     * Performs exact multiplication of two BigDecimal values using DECIMAL128 precision.
     * Maintains exact decimal precision equivalent to COBOL COMP-3 arithmetic.
     * 
     * @param multiplicand The first operand (value being multiplied)
     * @param multiplier The second operand (multiplier)
     * @return The product with exact precision using DECIMAL128 context
     * @throws IllegalArgumentException if either operand is null
     */
    public static BigDecimal multiply(BigDecimal multiplicand, BigDecimal multiplier) {
        if (multiplicand == null || multiplier == null) {
            throw new IllegalArgumentException("Cannot perform multiplication with null BigDecimal operands");
        }
        return multiplicand.multiply(multiplier, DECIMAL128_CONTEXT);
    }
    
    /**
     * Performs exact division of two BigDecimal values using DECIMAL128 precision.
     * Maintains exact decimal precision equivalent to COBOL COMP-3 arithmetic.
     * Uses HALF_EVEN rounding mode to match COBOL behavior.
     * 
     * @param dividend The first operand (value being divided)
     * @param divisor The second operand (divisor)
     * @return The quotient with exact precision using DECIMAL128 context
     * @throws IllegalArgumentException if either operand is null
     * @throws ArithmeticException if divisor is zero
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null) {
            throw new IllegalArgumentException("Cannot perform division with null BigDecimal operands");
        }
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return dividend.divide(divisor, DECIMAL128_CONTEXT);
    }
    
    /**
     * Performs exact division with explicit scale and rounding mode.
     * Useful for financial calculations requiring specific precision control.
     * 
     * @param dividend The first operand (value being divided)
     * @param divisor The second operand (divisor)
     * @param scale The scale of the result
     * @param roundingMode The rounding mode to apply
     * @return The quotient with specified scale and rounding
     * @throws IllegalArgumentException if either operand is null or scale is negative
     * @throws ArithmeticException if divisor is zero
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int scale, RoundingMode roundingMode) {
        if (dividend == null || divisor == null) {
            throw new IllegalArgumentException("Cannot perform division with null BigDecimal operands");
        }
        if (scale < 0) {
            throw new IllegalArgumentException("Scale cannot be negative: " + scale);
        }
        if (roundingMode == null) {
            throw new IllegalArgumentException("RoundingMode cannot be null");
        }
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return dividend.divide(divisor, scale, roundingMode);
    }
    
    /**
     * Compares two BigDecimal values numerically.
     * Provides null-safe comparison with consistent behavior.
     * 
     * @param left The first BigDecimal value
     * @param right The second BigDecimal value
     * @return -1, 0, or 1 as left is numerically less than, equal to, or greater than right
     * @throws IllegalArgumentException if either operand is null
     */
    public static int compare(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Cannot compare null BigDecimal values");
        }
        return left.compareTo(right);
    }
    
    /**
     * Checks if two BigDecimal values are equal numerically.
     * Uses compareTo() method for proper numerical equality comparison.
     * 
     * @param left The first BigDecimal value
     * @param right The second BigDecimal value
     * @return true if the values are numerically equal, false otherwise
     */
    public static boolean equals(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }
    
    /**
     * Checks if the first BigDecimal is greater than the second.
     * 
     * @param left The first BigDecimal value
     * @param right The second BigDecimal value
     * @return true if left > right, false otherwise
     * @throws IllegalArgumentException if either operand is null
     */
    public static boolean isGreaterThan(BigDecimal left, BigDecimal right) {
        return compare(left, right) > 0;
    }
    
    /**
     * Checks if the first BigDecimal is less than the second.
     * 
     * @param left The first BigDecimal value
     * @param right The second BigDecimal value
     * @return true if left < right, false otherwise
     * @throws IllegalArgumentException if either operand is null
     */
    public static boolean isLessThan(BigDecimal left, BigDecimal right) {
        return compare(left, right) < 0;
    }
    
    /**
     * Checks if the first BigDecimal is greater than or equal to the second.
     * 
     * @param left The first BigDecimal value
     * @param right The second BigDecimal value
     * @return true if left >= right, false otherwise
     * @throws IllegalArgumentException if either operand is null
     */
    public static boolean isGreaterThanOrEqual(BigDecimal left, BigDecimal right) {
        return compare(left, right) >= 0;
    }
    
    /**
     * Checks if the first BigDecimal is less than or equal to the second.
     * 
     * @param left The first BigDecimal value
     * @param right The second BigDecimal value
     * @return true if left <= right, false otherwise
     * @throws IllegalArgumentException if either operand is null
     */
    public static boolean isLessThanOrEqual(BigDecimal left, BigDecimal right) {
        return compare(left, right) <= 0;
    }
    
    /**
     * Formats a BigDecimal value as a currency string with proper precision.
     * Uses US Dollar formatting with exactly 2 decimal places.
     * 
     * @param value The BigDecimal value to format
     * @return Formatted currency string (e.g., "$1,234.56")
     * @throws IllegalArgumentException if value is null
     */
    public static String formatCurrency(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot format null BigDecimal value as currency");
        }
        // Ensure proper scale before formatting
        BigDecimal scaledValue = value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        return CURRENCY_FORMATTER.format(scaledValue);
    }
    
    /**
     * Formats a BigDecimal value as a plain decimal string with proper precision.
     * Uses specified scale with HALF_EVEN rounding.
     * 
     * @param value The BigDecimal value to format
     * @param scale The number of decimal places
     * @return Formatted decimal string (e.g., "1234.56")
     * @throws IllegalArgumentException if value is null or scale is negative
     */
    public static String formatDecimal(BigDecimal value, int scale) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot format null BigDecimal value");
        }
        if (scale < 0) {
            throw new IllegalArgumentException("Scale cannot be negative: " + scale);
        }
        BigDecimal scaledValue = value.setScale(scale, RoundingMode.HALF_EVEN);
        return scaledValue.toPlainString();
    }
    
    /**
     * Parses a string value into a BigDecimal with monetary precision.
     * Handles various input formats including currency symbols and thousands separators.
     * 
     * @param value The string value to parse
     * @return BigDecimal with monetary precision
     * @throws IllegalArgumentException if value is null or empty
     * @throws NumberFormatException if the string cannot be parsed as a number
     */
    public static BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string as BigDecimal");
        }
        
        String cleanValue = value.trim();
        
        // Handle currency symbols and separators
        if (cleanValue.startsWith("$")) {
            cleanValue = cleanValue.substring(1);
        }
        
        // Remove thousands separators but preserve decimal point
        cleanValue = cleanValue.replace(",", "");
        
        try {
            return createDecimal(cleanValue);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid number format: " + value);
        }
    }
    
    /**
     * Parses a monetary string using NumberFormat for complex formatting.
     * Useful for parsing user input with various formatting conventions.
     * 
     * @param value The string value to parse
     * @return BigDecimal with monetary precision
     * @throws IllegalArgumentException if value is null or empty
     * @throws ParseException if the string cannot be parsed
     */
    public static BigDecimal parseMonetary(String value) throws ParseException {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string as monetary value");
        }
        
        Number parsedNumber = MONETARY_PARSER.parse(value.trim());
        BigDecimal result = new BigDecimal(parsedNumber.toString(), DECIMAL128_CONTEXT);
        return result.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Rounds a BigDecimal value to monetary precision using HALF_EVEN rounding.
     * Ensures consistent rounding behavior matching COBOL arithmetic.
     * 
     * @param value The BigDecimal value to round
     * @return BigDecimal rounded to monetary precision
     * @throws IllegalArgumentException if value is null
     */
    public static BigDecimal roundToMonetary(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot round null BigDecimal value");
        }
        return value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Converts a BigDecimal to cents as a long value.
     * Useful for storing monetary values as integers in certain contexts.
     * 
     * @param value The BigDecimal monetary value
     * @return The value in cents as a long
     * @throws IllegalArgumentException if value is null
     * @throws ArithmeticException if the value cannot be represented as a long
     */
    public static long toCents(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot convert null BigDecimal to cents");
        }
        BigDecimal scaledValue = value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal centsValue = scaledValue.multiply(BigDecimal.valueOf(100));
        try {
            return centsValue.longValueExact();
        } catch (ArithmeticException e) {
            throw new ArithmeticException("BigDecimal value too large to convert to cents as long: " + value);
        }
    }
    
    /**
     * Calculates percentage of a base amount with exact precision.
     * Useful for interest calculations and fee computations.
     * 
     * @param baseAmount The base amount
     * @param percentage The percentage (e.g., 5.25 for 5.25%)
     * @return The calculated percentage amount
     * @throws IllegalArgumentException if either operand is null
     */
    public static BigDecimal calculatePercentage(BigDecimal baseAmount, BigDecimal percentage) {
        if (baseAmount == null || percentage == null) {
            throw new IllegalArgumentException("Cannot calculate percentage with null operands");
        }
        BigDecimal percentageDecimal = percentage.divide(BigDecimal.valueOf(100), DECIMAL128_CONTEXT);
        return multiply(baseAmount, percentageDecimal);
    }
    
    /**
     * Validates that a BigDecimal value is within the specified range (inclusive).
     * Useful for validating financial amounts against business rules.
     * 
     * @param value The value to validate
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @return true if value is within range, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean isInRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null || min == null || max == null) {
            throw new IllegalArgumentException("Cannot validate range with null values");
        }
        return compare(value, min) >= 0 && compare(value, max) <= 0;
    }
    
    /**
     * Returns the absolute value of a BigDecimal with preserved precision.
     * 
     * @param value The BigDecimal value
     * @return The absolute value with DECIMAL128 precision
     * @throws IllegalArgumentException if value is null
     */
    public static BigDecimal abs(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot get absolute value of null BigDecimal");
        }
        return value.abs(DECIMAL128_CONTEXT);
    }
    
    /**
     * Returns the maximum of two BigDecimal values.
     * 
     * @param left The first BigDecimal value
     * @param right The second BigDecimal value
     * @return The maximum value
     * @throws IllegalArgumentException if either operand is null
     */
    public static BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Cannot find maximum of null BigDecimal values");
        }
        return left.max(right);
    }
    
    /**
     * Returns the minimum of two BigDecimal values.
     * 
     * @param left The first BigDecimal value
     * @param right The second BigDecimal value
     * @return The minimum value
     * @throws IllegalArgumentException if either operand is null
     */
    public static BigDecimal min(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Cannot find minimum of null BigDecimal values");
        }
        return left.min(right);
    }
}