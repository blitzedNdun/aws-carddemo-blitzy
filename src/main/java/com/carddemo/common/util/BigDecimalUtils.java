package com.carddemo.common.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * BigDecimalUtils - Comprehensive utility class for exact financial calculations
 * 
 * This utility class provides exact financial calculations using BigDecimal with 
 * MathContext.DECIMAL128 to replicate COBOL COMP-3 precision. Ensures all monetary 
 * operations maintain identical decimal precision as the original COBOL implementation,
 * preventing floating-point errors in critical financial calculations.
 * 
 * Key Features:
 * - Exact COBOL COMP-3 decimal precision using BigDecimal with MathContext.DECIMAL128
 * - Financial calculation utilities maintaining identical results with exact decimal precision
 * - Rounding mode HALF_EVEN to replicate COBOL arithmetic behavior
 * - Utility methods for monetary operations with precision preservation
 * - Support for PostgreSQL NUMERIC(12,2) and NUMERIC(5,4) data types
 * - Range validation for financial amounts (-9999999999.99 to 9999999999.99)
 * - Interest rate validation (0.0001 to 9.9999)
 * 
 * Performance Requirements:
 * - Supports < 200ms response time for card authorization at 95th percentile
 * - Optimized for 10,000+ TPS transaction processing
 * - Memory efficient for batch processing within 4-hour window
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public final class BigDecimalUtils {
    
    /**
     * DECIMAL128 Mathematical Context for exact financial calculations
     * 
     * This context provides 34 decimal digits of precision with HALF_EVEN rounding mode,
     * exactly matching COBOL COMP-3 arithmetic behavior and ensuring identical results
     * for all financial operations during the COBOL-to-Java migration.
     */
    public static final MathContext DECIMAL128_CONTEXT = MathContext.DECIMAL128;
    
    /**
     * Standard monetary scale for financial amounts
     * 
     * Represents 2 decimal places for currency values, matching PostgreSQL NUMERIC(12,2)
     * column definition and COBOL COMP-3 monetary field precision requirements.
     */
    public static final int MONETARY_SCALE = 2;
    
    /**
     * Standard interest rate scale for financial calculations
     * 
     * Represents 4 decimal places for interest rates, matching PostgreSQL NUMERIC(5,4)
     * column definition and supporting percentage calculations with 0.01% precision.
     */
    public static final int INTEREST_RATE_SCALE = 4;
    
    /**
     * Maximum financial amount value supporting business requirements
     * 
     * Upper bound for financial amounts matching PostgreSQL NUMERIC(12,2) constraints
     * and COBOL COMP-3 field capacity for account balances and transaction amounts.
     */
    public static final BigDecimal MAX_FINANCIAL_AMOUNT = new BigDecimal("9999999999.99");
    
    /**
     * Minimum financial amount value supporting business requirements
     * 
     * Lower bound for financial amounts matching PostgreSQL NUMERIC(12,2) constraints
     * and supporting negative balances and charge-back transactions.
     */
    public static final BigDecimal MIN_FINANCIAL_AMOUNT = new BigDecimal("-9999999999.99");
    
    /**
     * Maximum interest rate value for validation
     * 
     * Upper bound for interest rates matching PostgreSQL NUMERIC(5,4) constraints
     * and supporting up to 999.99% annual percentage rates.
     */
    public static final BigDecimal MAX_INTEREST_RATE = new BigDecimal("9.9999");
    
    /**
     * Minimum interest rate value for validation
     * 
     * Lower bound for interest rates supporting minimum 0.01% annual percentage rates
     * and preventing negative interest rate scenarios.
     */
    public static final BigDecimal MIN_INTEREST_RATE = new BigDecimal("0.0001");
    
    /**
     * Zero constant with monetary precision
     * 
     * Represents zero value with 2 decimal places for financial calculations,
     * ensuring consistent scale in monetary operations.
     */
    public static final BigDecimal ZERO_MONETARY = BigDecimal.ZERO.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    
    /**
     * One constant with monetary precision
     * 
     * Represents one dollar with 2 decimal places for financial calculations,
     * useful for unit conversions and scaling operations.
     */
    public static final BigDecimal ONE_MONETARY = BigDecimal.ONE.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    
    /**
     * Currency formatter for US dollar display
     * 
     * ThreadLocal formatter ensuring thread-safe currency formatting operations
     * across multiple concurrent microservice requests.
     */
    private static final ThreadLocal<DecimalFormat> CURRENCY_FORMATTER = ThreadLocal.withInitial(() -> {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        formatter.setRoundingMode(RoundingMode.HALF_EVEN);
        return formatter;
    });
    
    /**
     * Number formatter for decimal display
     * 
     * ThreadLocal formatter ensuring thread-safe number formatting operations
     * with consistent decimal precision across financial calculations.
     */
    private static final ThreadLocal<DecimalFormat> NUMBER_FORMATTER = ThreadLocal.withInitial(() -> {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        formatter.setRoundingMode(RoundingMode.HALF_EVEN);
        return formatter;
    });
    
    /**
     * Private constructor preventing instantiation
     * 
     * This utility class contains only static methods and should not be instantiated.
     * All methods are designed to be called statically for optimal performance.
     */
    private BigDecimalUtils() {
        throw new UnsupportedOperationException("BigDecimalUtils is a utility class and cannot be instantiated");
    }
    
    /**
     * Creates a BigDecimal with exact decimal precision for financial calculations
     * 
     * This method ensures all financial values are created with DECIMAL128 precision
     * and proper scale settings, maintaining exact COBOL COMP-3 equivalency.
     * 
     * @param value The string representation of the decimal value
     * @return BigDecimal with DECIMAL128 precision and appropriate scale
     * @throws NumberFormatException if the value cannot be parsed as a decimal
     * @throws IllegalArgumentException if the value is null or exceeds financial limits
     */
    public static BigDecimal createDecimal(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        BigDecimal decimal = new BigDecimal(value, DECIMAL128_CONTEXT);
        
        // Set appropriate scale based on the value characteristics
        if (decimal.scale() <= MONETARY_SCALE) {
            decimal = decimal.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        }
        
        // Validate against financial limits
        validateFinancialAmount(decimal);
        
        return decimal;
    }
    
    /**
     * Creates a BigDecimal from a double value with exact precision
     * 
     * This method converts double values to BigDecimal using string conversion
     * to avoid floating-point precision errors, ensuring exact decimal representation.
     * 
     * @param value The double value to convert
     * @return BigDecimal with exact precision and appropriate scale
     * @throws IllegalArgumentException if the value is infinite or NaN
     */
    public static BigDecimal createDecimal(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new IllegalArgumentException("Value must be a finite number");
        }
        
        // Convert to string to avoid floating-point precision issues
        return createDecimal(String.valueOf(value));
    }
    
    /**
     * Creates a BigDecimal from a long value with monetary precision
     * 
     * This method converts long values to BigDecimal with monetary scale,
     * useful for converting cent amounts to dollar amounts.
     * 
     * @param value The long value to convert
     * @return BigDecimal with monetary precision
     */
    public static BigDecimal createDecimal(long value) {
        return new BigDecimal(value, DECIMAL128_CONTEXT).setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Performs exact addition of two BigDecimal values
     * 
     * This method ensures addition operations maintain DECIMAL128 precision
     * and proper scale settings, replicating COBOL COMP-3 arithmetic behavior.
     * 
     * @param left The left operand for addition
     * @param right The right operand for addition
     * @return The sum with exact precision and appropriate scale
     * @throws IllegalArgumentException if either operand is null
     */
    public static BigDecimal add(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Operands cannot be null");
        }
        
        BigDecimal result = left.add(right, DECIMAL128_CONTEXT);
        
        // Maintain consistent scale for financial calculations
        int targetScale = Math.max(left.scale(), right.scale());
        targetScale = Math.max(targetScale, MONETARY_SCALE);
        
        result = result.setScale(targetScale, RoundingMode.HALF_EVEN);
        
        // Validate result against financial limits
        validateFinancialAmount(result);
        
        return result;
    }
    
    /**
     * Performs exact subtraction of two BigDecimal values
     * 
     * This method ensures subtraction operations maintain DECIMAL128 precision
     * and proper scale settings, supporting negative results for charge-backs.
     * 
     * @param left The left operand (minuend)
     * @param right The right operand (subtrahend)
     * @return The difference with exact precision and appropriate scale
     * @throws IllegalArgumentException if either operand is null
     */
    public static BigDecimal subtract(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Operands cannot be null");
        }
        
        BigDecimal result = left.subtract(right, DECIMAL128_CONTEXT);
        
        // Maintain consistent scale for financial calculations
        int targetScale = Math.max(left.scale(), right.scale());
        targetScale = Math.max(targetScale, MONETARY_SCALE);
        
        result = result.setScale(targetScale, RoundingMode.HALF_EVEN);
        
        // Validate result against financial limits
        validateFinancialAmount(result);
        
        return result;
    }
    
    /**
     * Performs exact multiplication of two BigDecimal values
     * 
     * This method ensures multiplication operations maintain DECIMAL128 precision
     * and proper scale settings, essential for interest calculations and fees.
     * 
     * @param left The left operand (multiplicand)
     * @param right The right operand (multiplier)
     * @return The product with exact precision and appropriate scale
     * @throws IllegalArgumentException if either operand is null
     */
    public static BigDecimal multiply(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Operands cannot be null");
        }
        
        BigDecimal result = left.multiply(right, DECIMAL128_CONTEXT);
        
        // For financial calculations, maintain monetary scale
        result = result.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        
        // Validate result against financial limits
        validateFinancialAmount(result);
        
        return result;
    }
    
    /**
     * Performs exact division of two BigDecimal values
     * 
     * This method ensures division operations maintain DECIMAL128 precision
     * and proper scale settings, with protection against division by zero.
     * 
     * @param dividend The dividend (numerator)
     * @param divisor The divisor (denominator)
     * @return The quotient with exact precision and appropriate scale
     * @throws IllegalArgumentException if either operand is null
     * @throws ArithmeticException if divisor is zero
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null) {
            throw new IllegalArgumentException("Operands cannot be null");
        }
        
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        
        BigDecimal result = dividend.divide(divisor, DECIMAL128_CONTEXT);
        
        // For financial calculations, maintain monetary scale
        result = result.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        
        // Validate result against financial limits
        validateFinancialAmount(result);
        
        return result;
    }
    
    /**
     * Performs exact division with specified scale
     * 
     * This method provides division operations with custom scale settings,
     * useful for interest rate calculations and percentage computations.
     * 
     * @param dividend The dividend (numerator)
     * @param divisor The divisor (denominator)
     * @param scale The desired scale for the result
     * @return The quotient with exact precision and specified scale
     * @throws IllegalArgumentException if either operand is null or scale is negative
     * @throws ArithmeticException if divisor is zero
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int scale) {
        if (dividend == null || divisor == null) {
            throw new IllegalArgumentException("Operands cannot be null");
        }
        
        if (scale < 0) {
            throw new IllegalArgumentException("Scale must be non-negative");
        }
        
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        
        return dividend.divide(divisor, scale, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Compares two BigDecimal values with null-safe handling
     * 
     * This method provides consistent comparison operations for financial values,
     * handling null values appropriately and maintaining COBOL comparison semantics.
     * 
     * @param left The left operand for comparison
     * @param right The right operand for comparison
     * @return Negative integer if left < right, zero if equal, positive if left > right
     */
    public static int compare(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        
        return left.compareTo(right);
    }
    
    /**
     * Formats a BigDecimal value as currency string
     * 
     * This method provides consistent currency formatting for financial amounts,
     * using US dollar format with proper decimal precision and rounding.
     * 
     * @param value The BigDecimal value to format
     * @return Formatted currency string (e.g., "$1,234.56")
     * @throws IllegalArgumentException if value is null
     */
    public static String formatCurrency(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        // Ensure consistent scale for formatting
        BigDecimal scaledValue = value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        
        return CURRENCY_FORMATTER.get().format(scaledValue);
    }
    
    /**
     * Formats a BigDecimal value as a number string
     * 
     * This method provides consistent number formatting for financial amounts,
     * using comma separators and proper decimal precision.
     * 
     * @param value The BigDecimal value to format
     * @return Formatted number string (e.g., "1,234.56")
     * @throws IllegalArgumentException if value is null
     */
    public static String formatNumber(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        // Ensure consistent scale for formatting
        BigDecimal scaledValue = value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        
        return NUMBER_FORMATTER.get().format(scaledValue);
    }
    
    /**
     * Parses a string to BigDecimal with exact precision
     * 
     * This method provides robust parsing of financial values from string inputs,
     * handling currency symbols, commas, and various decimal formats.
     * 
     * @param value The string value to parse
     * @return BigDecimal with exact precision and appropriate scale
     * @throws IllegalArgumentException if value is null or cannot be parsed
     */
    public static BigDecimal parseDecimal(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        // Clean the input string
        String cleanValue = value.trim()
            .replace("$", "")
            .replace(",", "")
            .replace("(", "-")
            .replace(")", "");
        
        try {
            return createDecimal(cleanValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid decimal format: " + value, e);
        }
    }
    
    /**
     * Parses a currency string to BigDecimal
     * 
     * This method provides specialized parsing for currency-formatted strings,
     * handling various currency symbols and formatting conventions.
     * 
     * @param currencyValue The currency string to parse
     * @return BigDecimal with monetary precision
     * @throws IllegalArgumentException if value is null or cannot be parsed
     */
    public static BigDecimal parseCurrency(String currencyValue) {
        if (currencyValue == null) {
            throw new IllegalArgumentException("Currency value cannot be null");
        }
        
        try {
            // Use currency formatter to parse the value
            Number number = CURRENCY_FORMATTER.get().parse(currencyValue);
            return createDecimal(number.toString());
        } catch (ParseException e) {
            // Fall back to general decimal parsing
            return parseDecimal(currencyValue);
        }
    }
    
    /**
     * Validates a financial amount against business rules
     * 
     * This method ensures financial amounts are within acceptable ranges
     * for PostgreSQL NUMERIC(12,2) constraints and business logic requirements.
     * 
     * @param amount The amount to validate
     * @throws IllegalArgumentException if amount is null or outside valid range
     */
    public static void validateFinancialAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Financial amount cannot be null");
        }
        
        if (amount.compareTo(MIN_FINANCIAL_AMOUNT) < 0) {
            throw new IllegalArgumentException("Financial amount cannot be less than " + MIN_FINANCIAL_AMOUNT);
        }
        
        if (amount.compareTo(MAX_FINANCIAL_AMOUNT) > 0) {
            throw new IllegalArgumentException("Financial amount cannot be greater than " + MAX_FINANCIAL_AMOUNT);
        }
    }
    
    /**
     * Validates an interest rate against business rules
     * 
     * This method ensures interest rates are within acceptable ranges
     * for PostgreSQL NUMERIC(5,4) constraints and business logic requirements.
     * 
     * @param rate The interest rate to validate
     * @throws IllegalArgumentException if rate is null or outside valid range
     */
    public static void validateInterestRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Interest rate cannot be null");
        }
        
        if (rate.compareTo(MIN_INTEREST_RATE) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be less than " + MIN_INTEREST_RATE);
        }
        
        if (rate.compareTo(MAX_INTEREST_RATE) > 0) {
            throw new IllegalArgumentException("Interest rate cannot be greater than " + MAX_INTEREST_RATE);
        }
    }
    
    /**
     * Calculates percentage of an amount with exact precision
     * 
     * This method provides accurate percentage calculations for financial operations,
     * ensuring proper scale and rounding for interest calculations and fees.
     * 
     * @param amount The base amount
     * @param percentage The percentage rate (e.g., 0.05 for 5%)
     * @return The calculated percentage amount
     * @throws IllegalArgumentException if either parameter is null
     */
    public static BigDecimal calculatePercentage(BigDecimal amount, BigDecimal percentage) {
        if (amount == null || percentage == null) {
            throw new IllegalArgumentException("Amount and percentage cannot be null");
        }
        
        validateFinancialAmount(amount);
        validateInterestRate(percentage);
        
        return multiply(amount, percentage);
    }
    
    /**
     * Rounds a BigDecimal to monetary precision
     * 
     * This method ensures all financial calculations are rounded to standard
     * monetary precision using HALF_EVEN rounding mode for COBOL compatibility.
     * 
     * @param value The value to round
     * @return The rounded value with monetary precision
     * @throws IllegalArgumentException if value is null
     */
    public static BigDecimal roundToMonetaryPrecision(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        return value.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Rounds a BigDecimal to interest rate precision
     * 
     * This method ensures all interest rate calculations are rounded to standard
     * interest rate precision using HALF_EVEN rounding mode for COBOL compatibility.
     * 
     * @param value The value to round
     * @return The rounded value with interest rate precision
     * @throws IllegalArgumentException if value is null
     */
    public static BigDecimal roundToInterestRatePrecision(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        return value.setScale(INTEREST_RATE_SCALE, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Checks if a BigDecimal value is zero
     * 
     * This method provides null-safe zero checking for financial calculations,
     * considering values within monetary precision as effectively zero.
     * 
     * @param value The value to check
     * @return true if value is null or zero, false otherwise
     */
    public static boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Checks if a BigDecimal value is positive
     * 
     * This method provides null-safe positive checking for financial calculations,
     * returning false for null values to maintain defensive programming practices.
     * 
     * @param value The value to check
     * @return true if value is positive, false if null, zero, or negative
     */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Checks if a BigDecimal value is negative
     * 
     * This method provides null-safe negative checking for financial calculations,
     * useful for identifying charge-backs and negative balances.
     * 
     * @param value The value to check
     * @return true if value is negative, false if null, zero, or positive
     */
    public static boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Gets the absolute value of a BigDecimal
     * 
     * This method provides null-safe absolute value calculation for financial amounts,
     * maintaining exact precision and appropriate scale.
     * 
     * @param value The value to get absolute value for
     * @return The absolute value with exact precision
     * @throws IllegalArgumentException if value is null
     */
    public static BigDecimal abs(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        return value.abs(DECIMAL128_CONTEXT);
    }
    
    /**
     * Gets the maximum of two BigDecimal values
     * 
     * This method provides null-safe maximum value calculation for financial amounts,
     * handling null values by treating them as negative infinity.
     * 
     * @param left The left operand
     * @param right The right operand
     * @return The maximum value, or null if both are null
     */
    public static BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return null;
        }
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        
        return left.max(right);
    }
    
    /**
     * Gets the minimum of two BigDecimal values
     * 
     * This method provides null-safe minimum value calculation for financial amounts,
     * handling null values by treating them as positive infinity.
     * 
     * @param left The left operand
     * @param right The right operand
     * @return The minimum value, or null if both are null
     */
    public static BigDecimal min(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return null;
        }
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        
        return left.min(right);
    }
}