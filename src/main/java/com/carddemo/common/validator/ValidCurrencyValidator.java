package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Validator implementation for {@link ValidCurrency} annotation.
 * 
 * <p>This validator ensures that BigDecimal values maintain exact financial precision
 * equivalent to COBOL COMP-3 calculations while supporting configurable range validation.
 * It validates monetary fields according to the CardDemo application's database schema
 * requirements and financial calculation standards.</p>
 * 
 * <p>The validator implements comprehensive validation logic including:</p>
 * <ul>
 *   <li>BigDecimal precision and scale validation</li>
 *   <li>Range validation with configurable minimum and maximum values</li>
 *   <li>COBOL COMP-3 compatibility for legacy financial calculations</li>
 *   <li>Context-aware validation for different currency types</li>
 *   <li>Strict precision enforcement when required</li>
 *   <li>Proper rounding mode handling</li>
 * </ul>
 * 
 * <p>This validator is designed to maintain compatibility with the legacy COBOL COMP-3
 * packed decimal format while providing modern Java BigDecimal validation capabilities.</p>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 * @see ValidCurrency
 * @see java.math.BigDecimal
 * @see jakarta.validation.ConstraintValidator
 */
public class ValidCurrencyValidator implements ConstraintValidator<ValidCurrency, BigDecimal> {

    /**
     * MathContext for COBOL COMP-3 equivalent calculations.
     * Uses DECIMAL128 precision with HALF_EVEN rounding to maintain exact financial precision.
     */
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_EVEN);

    /**
     * Pattern for validating decimal string format.
     * Matches optional sign, digits, optional decimal point, and optional fractional digits.
     */
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    /**
     * Maximum safe precision for BigDecimal operations to prevent performance issues.
     */
    private static final int MAX_SAFE_PRECISION = 50;

    /**
     * Maximum safe scale for BigDecimal operations to prevent performance issues.
     */
    private static final int MAX_SAFE_SCALE = 10;

    private BigDecimal minValue;
    private BigDecimal maxValue;
    private int precision;
    private int scale;
    private boolean allowNull;
    private boolean strictPrecision;
    private RoundingMode roundingMode;
    private String context;
    private boolean cobolCompatible;

    /**
     * Initializes the validator with the annotation parameters.
     * 
     * @param constraintAnnotation the ValidCurrency annotation instance
     * @throws IllegalArgumentException if annotation parameters are invalid
     */
    @Override
    public void initialize(ValidCurrency constraintAnnotation) {
        try {
            // Parse and validate min/max values
            this.minValue = parseDecimalValue(constraintAnnotation.min(), "min");
            this.maxValue = parseDecimalValue(constraintAnnotation.max(), "max");
            
            // Validate min <= max
            if (minValue.compareTo(maxValue) > 0) {
                throw new IllegalArgumentException("Minimum value (" + minValue + ") cannot be greater than maximum value (" + maxValue + ")");
            }
            
            // Validate precision and scale
            this.precision = constraintAnnotation.precision();
            this.scale = constraintAnnotation.scale();
            
            if (precision <= 0 || precision > MAX_SAFE_PRECISION) {
                throw new IllegalArgumentException("Precision must be between 1 and " + MAX_SAFE_PRECISION + ", got: " + precision);
            }
            
            if (scale < 0 || scale > MAX_SAFE_SCALE) {
                throw new IllegalArgumentException("Scale must be between 0 and " + MAX_SAFE_SCALE + ", got: " + scale);
            }
            
            if (scale > precision) {
                throw new IllegalArgumentException("Scale (" + scale + ") cannot be greater than precision (" + precision + ")");
            }
            
            // Parse rounding mode
            this.roundingMode = parseRoundingMode(constraintAnnotation.roundingMode());
            
            // Set other parameters
            this.allowNull = constraintAnnotation.allowNull();
            this.strictPrecision = constraintAnnotation.strictPrecision();
            this.context = constraintAnnotation.context();
            this.cobolCompatible = constraintAnnotation.cobolCompatible();
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ValidCurrency annotation parameters: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the BigDecimal value against the configured constraints.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Handle null values
        if (value == null) {
            return allowNull;
        }
        
        try {
            // Apply COBOL compatibility if required
            if (cobolCompatible) {
                value = normalizeForCobolCompatibility(value);
            }
            
            // Validate precision and scale
            if (!validatePrecisionAndScale(value, context)) {
                return false;
            }
            
            // Validate range
            if (!validateRange(value, context)) {
                return false;
            }
            
            // Context-specific validation
            if (!validateContext(value, context)) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            addConstraintViolation(context, "Invalid BigDecimal value: " + e.getMessage());
            return false;
        }
    }

    /**
     * Normalizes BigDecimal value for COBOL COMP-3 compatibility.
     * 
     * @param value the BigDecimal value to normalize
     * @return the normalized BigDecimal value
     */
    private BigDecimal normalizeForCobolCompatibility(BigDecimal value) {
        // Use COBOL-compatible math context for all operations
        MathContext cobolContext = new MathContext(precision, roundingMode);
        
        // Round to the specified scale using COBOL-compatible rounding
        return value.setScale(scale, roundingMode).round(cobolContext);
    }

    /**
     * Validates the precision and scale of the BigDecimal value.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context
     * @return true if precision and scale are valid, false otherwise
     */
    private boolean validatePrecisionAndScale(BigDecimal value, ConstraintValidatorContext context) {
        // Check scale
        int actualScale = value.scale();
        if (strictPrecision && actualScale != scale) {
            addConstraintViolation(context, "Scale must be exactly " + scale + " but was " + actualScale);
            return false;
        }
        
        if (actualScale > scale) {
            addConstraintViolation(context, "Scale cannot exceed " + scale + " but was " + actualScale);
            return false;
        }
        
        // Check precision (total number of digits)
        int actualPrecision = value.precision();
        if (actualPrecision > precision) {
            addConstraintViolation(context, "Precision cannot exceed " + precision + " but was " + actualPrecision);
            return false;
        }
        
        return true;
    }

    /**
     * Validates that the BigDecimal value is within the specified range.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context
     * @return true if the value is within range, false otherwise
     */
    private boolean validateRange(BigDecimal value, ConstraintValidatorContext context) {
        if (value.compareTo(minValue) < 0) {
            addConstraintViolation(context, "Value must be greater than or equal to " + minValue + " but was " + value);
            return false;
        }
        
        if (value.compareTo(maxValue) > 0) {
            addConstraintViolation(context, "Value must be less than or equal to " + maxValue + " but was " + value);
            return false;
        }
        
        return true;
    }

    /**
     * Performs context-specific validation based on the currency type.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context
     * @return true if context validation passes, false otherwise
     */
    private boolean validateContext(BigDecimal value, ConstraintValidatorContext context) {
        switch (this.context.toUpperCase()) {
            case "ACCOUNT_BALANCE":
                return validateAccountBalance(value, context);
            case "TRANSACTION_AMOUNT":
                return validateTransactionAmount(value, context);
            case "CREDIT_LIMIT":
                return validateCreditLimit(value, context);
            case "INTEREST_RATE":
                return validateInterestRate(value, context);
            case "GENERAL":
            default:
                return true; // No additional validation for general context
        }
    }

    /**
     * Validates account balance specific rules.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context
     * @return true if account balance validation passes, false otherwise
     */
    private boolean validateAccountBalance(BigDecimal value, ConstraintValidatorContext context) {
        // Account balances can be negative (overdraft) but should have reasonable limits
        BigDecimal maxNegative = new BigDecimal("-999999999.99");
        BigDecimal maxPositive = new BigDecimal("999999999.99");
        
        if (value.compareTo(maxNegative) < 0) {
            addConstraintViolation(context, "Account balance cannot be less than " + maxNegative);
            return false;
        }
        
        if (value.compareTo(maxPositive) > 0) {
            addConstraintViolation(context, "Account balance cannot exceed " + maxPositive);
            return false;
        }
        
        return true;
    }

    /**
     * Validates transaction amount specific rules.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context
     * @return true if transaction amount validation passes, false otherwise
     */
    private boolean validateTransactionAmount(BigDecimal value, ConstraintValidatorContext context) {
        // Transaction amounts should not be zero and should have reasonable limits
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            addConstraintViolation(context, "Transaction amount cannot be zero");
            return false;
        }
        
        BigDecimal maxAmount = new BigDecimal("99999.99");
        BigDecimal minAmount = new BigDecimal("-99999.99");
        
        if (value.compareTo(minAmount) < 0 || value.compareTo(maxAmount) > 0) {
            addConstraintViolation(context, "Transaction amount must be between " + minAmount + " and " + maxAmount);
            return false;
        }
        
        return true;
    }

    /**
     * Validates credit limit specific rules.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context
     * @return true if credit limit validation passes, false otherwise
     */
    private boolean validateCreditLimit(BigDecimal value, ConstraintValidatorContext context) {
        // Credit limits must be positive and within reasonable bounds
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            addConstraintViolation(context, "Credit limit must be positive");
            return false;
        }
        
        BigDecimal maxCreditLimit = new BigDecimal("999999.99");
        if (value.compareTo(maxCreditLimit) > 0) {
            addConstraintViolation(context, "Credit limit cannot exceed " + maxCreditLimit);
            return false;
        }
        
        return true;
    }

    /**
     * Validates interest rate specific rules.
     * 
     * @param value the BigDecimal value to validate
     * @param context the validation context
     * @return true if interest rate validation passes, false otherwise
     */
    private boolean validateInterestRate(BigDecimal value, ConstraintValidatorContext context) {
        // Interest rates must be positive and within reasonable bounds (0.01% to 999.99%)
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            addConstraintViolation(context, "Interest rate must be positive");
            return false;
        }
        
        BigDecimal maxRate = new BigDecimal("9.9999"); // 999.99% as decimal
        BigDecimal minRate = new BigDecimal("0.0001"); // 0.01% as decimal
        
        if (value.compareTo(minRate) < 0) {
            addConstraintViolation(context, "Interest rate must be at least " + minRate);
            return false;
        }
        
        if (value.compareTo(maxRate) > 0) {
            addConstraintViolation(context, "Interest rate cannot exceed " + maxRate);
            return false;
        }
        
        return true;
    }

    /**
     * Parses a decimal value from string representation.
     * 
     * @param value the string representation of the decimal value
     * @param fieldName the name of the field (for error messages)
     * @return the parsed BigDecimal value
     * @throws IllegalArgumentException if the string cannot be parsed as a valid decimal
     */
    private BigDecimal parseDecimalValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " value cannot be null or empty");
        }
        
        String trimmedValue = value.trim();
        
        // Validate format
        if (!DECIMAL_PATTERN.matcher(trimmedValue).matches()) {
            throw new IllegalArgumentException(fieldName + " value '" + value + "' is not a valid decimal format");
        }
        
        try {
            BigDecimal result = new BigDecimal(trimmedValue);
            
            // Validate reasonable bounds to prevent performance issues
            if (result.precision() > MAX_SAFE_PRECISION) {
                throw new IllegalArgumentException(fieldName + " value precision exceeds maximum safe limit of " + MAX_SAFE_PRECISION);
            }
            
            return result;
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " value '" + value + "' cannot be parsed as BigDecimal", e);
        }
    }

    /**
     * Parses the rounding mode from string representation.
     * 
     * @param roundingModeString the string representation of the rounding mode
     * @return the parsed RoundingMode
     * @throws IllegalArgumentException if the string is not a valid rounding mode
     */
    private RoundingMode parseRoundingMode(String roundingModeString) {
        if (roundingModeString == null || roundingModeString.trim().isEmpty()) {
            return RoundingMode.HALF_EVEN; // Default for financial calculations
        }
        
        try {
            return RoundingMode.valueOf(roundingModeString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid rounding mode: " + roundingModeString + 
                ". Valid values are: " + java.util.Arrays.toString(RoundingMode.values()), e);
        }
    }

    /**
     * Adds a constraint violation with a custom message.
     * 
     * @param context the validation context
     * @param message the violation message
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}