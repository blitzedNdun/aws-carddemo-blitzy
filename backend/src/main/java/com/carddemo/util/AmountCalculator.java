/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.carddemo.util.CobolDataConverter;

/**
 * Financial calculation utility class implementing COBOL arithmetic operations with exact precision matching.
 * 
 * This class provides comprehensive financial calculation methods that replicate COBOL COMP-3 arithmetic
 * behavior using Java BigDecimal operations. All calculations maintain penny-level accuracy and use
 * HALF_UP rounding mode to match COBOL ROUNDED clause behavior.
 * 
 * Key features:
 * - Interest calculations replicating CBACT04C formula: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 * - Balance updates matching COBIL00C payment processing logic
 * - Payment allocation between principal and interest components
 * - Credit limit validation and fee calculations
 * - Comprehensive error handling for boundary conditions
 * - Input validation ensuring data integrity
 * 
 * All methods use BigDecimal with scale=2 and RoundingMode.HALF_UP to exactly match
 * COBOL COMP-3 packed decimal behavior and maintain financial precision standards.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public final class AmountCalculator {

    /**
     * Scale for all monetary calculations (2 decimal places for cents precision).
     * Matches COBOL V99 decimal specification used in CVACT01Y and CVTRA01Y copybooks.
     */
    private static final int MONETARY_SCALE = CobolDataConverter.MONETARY_SCALE;

    /**
     * Rounding mode matching COBOL ROUNDED clause behavior.
     * Uses HALF_UP to ensure identical rounding results to mainframe calculations.
     */
    private static final RoundingMode COBOL_ROUNDING = RoundingMode.HALF_UP;

    /**
     * Annual interest rate divisor used in COBOL interest calculations.
     * Value 1200 = 12 months * 100 (percentage conversion)
     */
    private static final BigDecimal ANNUAL_RATE_DIVISOR = new BigDecimal("1200");

    /**
     * Days in a year for daily interest calculations.
     * Uses 365 to match standard banking interest calculation practices.
     */
    private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365");

    /**
     * Maximum allowed monetary amount to prevent overflow conditions.
     * Set to 999,999,999.99 matching COBOL S9(10)V99 field limits.
     */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");

    /**
     * Minimum allowed monetary amount to prevent underflow conditions.
     * Set to -999,999,999.99 matching COBOL S9(10)V99 field limits.
     */
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-999999999.99");

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private AmountCalculator() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Calculates monthly interest using the exact CBACT04C formula.
     * 
     * Implements the COBOL formula: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
     * where DIS-INT-RATE is the annual percentage rate and 1200 converts
     * annual rate to monthly decimal rate (12 months * 100 for percentage).
     * 
     * @param balance           transaction category balance (TRAN-CAT-BAL)
     * @param annualInterestRate annual interest rate percentage (DIS-INT-RATE)
     * @return monthly interest amount with exact COBOL precision
     * @throws IllegalArgumentException if parameters are null or rates are negative
     */
    public static BigDecimal calculateMonthlyInterest(BigDecimal balance, BigDecimal annualInterestRate) {
        validateAmount(balance, "Balance");
        validateAmount(annualInterestRate, "Annual interest rate");
        
        if (annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative: " + annualInterestRate);
        }

        // Handle zero balance or zero rate
        if (balance.compareTo(BigDecimal.ZERO) == 0 || annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, COBOL_ROUNDING);
        }

        // Replicate COBOL formula: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
        BigDecimal monthlyInterest = balance
            .multiply(annualInterestRate)
            .divide(ANNUAL_RATE_DIVISOR, MONETARY_SCALE, COBOL_ROUNDING);

        return CobolDataConverter.preservePrecision(monthlyInterest, MONETARY_SCALE);
    }

    /**
     * Calculates account balance after applying transaction amount.
     * 
     * Implements balance update logic from COBIL00C:
     * COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT
     * 
     * @param currentBalance current account balance
     * @param transactionAmount transaction amount (positive for debits, negative for credits)
     * @return updated account balance
     * @throws IllegalArgumentException if parameters are null or result exceeds limits
     */
    public static BigDecimal calculateBalance(BigDecimal currentBalance, BigDecimal transactionAmount) {
        validateAmount(currentBalance, "Current balance");
        validateAmount(transactionAmount, "Transaction amount");

        BigDecimal newBalance = currentBalance.subtract(transactionAmount);
        newBalance = CobolDataConverter.preservePrecision(newBalance, MONETARY_SCALE);

        // Check for overflow/underflow conditions
        handleOverflow(newBalance);
        handleUnderflow(newBalance);

        return newBalance;
    }

    /**
     * Processes payment allocation between principal and interest components.
     * 
     * Allocates payment amount to interest first, then remaining to principal,
     * following standard credit card payment processing rules.
     * 
     * @param paymentAmount total payment amount
     * @param interestBalance current interest balance
     * @param principalBalance current principal balance
     * @return array containing [interest payment, principal payment, remaining balance]
     * @throws IllegalArgumentException if parameters are null or payment is negative
     */
    public static BigDecimal[] processPayment(BigDecimal paymentAmount, BigDecimal interestBalance, 
                                           BigDecimal principalBalance) {
        validateAmount(paymentAmount, "Payment amount");
        validateAmount(interestBalance, "Interest balance");
        validateAmount(principalBalance, "Principal balance");

        if (paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative: " + paymentAmount);
        }

        BigDecimal remainingPayment = paymentAmount;
        BigDecimal interestPayment = BigDecimal.ZERO;
        BigDecimal principalPayment = BigDecimal.ZERO;

        // Allocate to interest first
        if (interestBalance.compareTo(BigDecimal.ZERO) > 0 && remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
            interestPayment = remainingPayment.min(interestBalance);
            remainingPayment = remainingPayment.subtract(interestPayment);
        }

        // Allocate remaining to principal
        if (principalBalance.compareTo(BigDecimal.ZERO) > 0 && remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
            principalPayment = remainingPayment.min(principalBalance);
            remainingPayment = remainingPayment.subtract(principalPayment);
        }

        // Calculate remaining balance after payment
        BigDecimal newInterestBalance = interestBalance.subtract(interestPayment);
        BigDecimal newPrincipalBalance = principalBalance.subtract(principalPayment);
        BigDecimal totalRemainingBalance = newInterestBalance.add(newPrincipalBalance);

        return new BigDecimal[] {
            CobolDataConverter.preservePrecision(interestPayment, MONETARY_SCALE),
            CobolDataConverter.preservePrecision(principalPayment, MONETARY_SCALE),
            CobolDataConverter.preservePrecision(totalRemainingBalance, MONETARY_SCALE)
        };
    }

    /**
     * Validates credit limit availability for new transactions.
     * 
     * Checks if proposed transaction amount would exceed available credit,
     * considering current balance and credit limit.
     * 
     * @param currentBalance current account balance
     * @param creditLimit total credit limit
     * @param transactionAmount proposed transaction amount
     * @return true if transaction is within credit limit, false otherwise
     * @throws IllegalArgumentException if parameters are null or credit limit is negative
     */
    public static boolean validateCreditLimit(BigDecimal currentBalance, BigDecimal creditLimit, 
                                           BigDecimal transactionAmount) {
        validateAmount(currentBalance, "Current balance");
        validateAmount(creditLimit, "Credit limit");
        validateAmount(transactionAmount, "Transaction amount");

        if (creditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Credit limit cannot be negative: " + creditLimit);
        }

        // Calculate available credit (credit limit - current balance)
        BigDecimal availableCredit = creditLimit.subtract(currentBalance);
        
        // Check if transaction amount is within available credit
        return transactionAmount.compareTo(availableCredit) <= 0;
    }

    /**
     * Calculates fees based on transaction amount and fee rate.
     * 
     * Computes fee amount using percentage rate with proper rounding
     * to maintain penny-level accuracy.
     * 
     * @param transactionAmount base amount for fee calculation
     * @param feeRate fee rate as percentage (e.g., 2.5 for 2.5%)
     * @return calculated fee amount
     * @throws IllegalArgumentException if parameters are null or rate is negative
     */
    public static BigDecimal calculateFee(BigDecimal transactionAmount, BigDecimal feeRate) {
        validateAmount(transactionAmount, "Transaction amount");
        validateAmount(feeRate, "Fee rate");

        if (feeRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee rate cannot be negative: " + feeRate);
        }

        // Handle zero amount or zero rate
        if (transactionAmount.compareTo(BigDecimal.ZERO) == 0 || feeRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, COBOL_ROUNDING);
        }

        // Calculate fee: (amount * rate) / 100
        BigDecimal feeAmount = transactionAmount
            .multiply(feeRate)
            .divide(new BigDecimal("100"), MONETARY_SCALE, COBOL_ROUNDING);

        return CobolDataConverter.preservePrecision(feeAmount, MONETARY_SCALE);
    }

    /**
     * Applies COBOL ROUNDED clause rounding using HALF_UP mode.
     * 
     * Ensures all monetary amounts are rounded to exactly 2 decimal places
     * using the same rounding behavior as COBOL ROUNDED clause.
     * 
     * @param amount amount to round
     * @return amount rounded to monetary scale with HALF_UP rounding
     * @throws IllegalArgumentException if amount is null
     */
    public static BigDecimal applyRounding(BigDecimal amount) {
        validateAmount(amount, "Amount");
        return CobolDataConverter.preservePrecision(amount, MONETARY_SCALE);
    }

    /**
     * Handles overflow conditions when amounts exceed maximum limits.
     * 
     * Validates that calculated amounts do not exceed COBOL field limits
     * to prevent data corruption or unexpected behavior.
     * 
     * @param amount amount to check for overflow
     * @throws IllegalArgumentException if amount exceeds maximum limit
     */
    public static void handleOverflow(BigDecimal amount) {
        if (amount != null && amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds maximum limit: " + amount + " > " + MAX_AMOUNT);
        }
    }

    /**
     * Handles underflow conditions when amounts fall below minimum limits.
     * 
     * Validates that calculated amounts do not fall below COBOL field limits
     * to prevent data corruption or unexpected behavior.
     * 
     * @param amount amount to check for underflow
     * @throws IllegalArgumentException if amount falls below minimum limit
     */
    public static void handleUnderflow(BigDecimal amount) {
        if (amount != null && amount.compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amount below minimum limit: " + amount + " < " + MIN_AMOUNT);
        }
    }

    /**
     * Processes negative balance scenarios and overdraft conditions.
     * 
     * Handles cases where account balance becomes negative, applying
     * appropriate business rules and fee calculations.
     * 
     * @param currentBalance current account balance
     * @param overdraftLimit maximum allowed overdraft amount
     * @param overdraftFee fee charged for overdraft
     * @return adjusted balance after applying overdraft rules
     * @throws IllegalArgumentException if parameters are null or overdraft exceeds limit
     */
    public static BigDecimal processNegativeBalance(BigDecimal currentBalance, BigDecimal overdraftLimit, 
                                                  BigDecimal overdraftFee) {
        validateAmount(currentBalance, "Current balance");
        validateAmount(overdraftLimit, "Overdraft limit");
        validateAmount(overdraftFee, "Overdraft fee");

        // If balance is positive, no processing needed
        if (currentBalance.compareTo(BigDecimal.ZERO) >= 0) {
            return currentBalance;
        }

        // Check if negative balance exceeds overdraft limit
        BigDecimal overdraftAmount = currentBalance.abs();
        if (overdraftAmount.compareTo(overdraftLimit) > 0) {
            throw new IllegalArgumentException("Overdraft amount " + overdraftAmount + 
                                             " exceeds limit " + overdraftLimit);
        }

        // Apply overdraft fee
        BigDecimal adjustedBalance = currentBalance.subtract(overdraftFee);
        adjustedBalance = CobolDataConverter.preservePrecision(adjustedBalance, MONETARY_SCALE);

        handleUnderflow(adjustedBalance);
        return adjustedBalance;
    }

    /**
     * Calculates percentage amounts with proper scale handling.
     * 
     * Computes percentage of a base amount using exact decimal arithmetic
     * to maintain precision requirements.
     * 
     * @param baseAmount base amount for percentage calculation
     * @param percentage percentage rate (e.g., 15.5 for 15.5%)
     * @return calculated percentage amount
     * @throws IllegalArgumentException if parameters are null
     */
    public static BigDecimal calculatePercentage(BigDecimal baseAmount, BigDecimal percentage) {
        validateAmount(baseAmount, "Base amount");
        validateAmount(percentage, "Percentage");

        // Handle zero cases
        if (baseAmount.compareTo(BigDecimal.ZERO) == 0 || percentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, COBOL_ROUNDING);
        }

        // Calculate: (baseAmount * percentage) / 100
        BigDecimal result = baseAmount
            .multiply(percentage)
            .divide(new BigDecimal("100"), MONETARY_SCALE, COBOL_ROUNDING);

        return CobolDataConverter.preservePrecision(result, MONETARY_SCALE);
    }

    /**
     * Validates monetary amounts for null values and basic constraints.
     * 
     * Performs comprehensive validation of input amounts to ensure
     * data integrity and prevent calculation errors.
     * 
     * @param amount amount to validate
     * @param fieldName name of field for error messages
     * @throws IllegalArgumentException if amount is null
     */
    public static void validateAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    /**
     * Calculates daily interest amount using annual rate.
     * 
     * Converts annual interest rate to daily rate and applies to balance.
     * Uses 365-day year for calculation consistency.
     * 
     * @param balance account balance
     * @param annualInterestRate annual interest rate percentage
     * @return daily interest amount
     * @throws IllegalArgumentException if parameters are null or rate is negative
     */
    public static BigDecimal calculateDailyInterest(BigDecimal balance, BigDecimal annualInterestRate) {
        validateAmount(balance, "Balance");
        validateAmount(annualInterestRate, "Annual interest rate");

        if (annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative: " + annualInterestRate);
        }

        // Handle zero balance or zero rate
        if (balance.compareTo(BigDecimal.ZERO) == 0 || annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, COBOL_ROUNDING);
        }

        // Calculate daily interest: (balance * rate) / (365 * 100)
        BigDecimal dailyRate = annualInterestRate.divide(DAYS_PER_YEAR.multiply(new BigDecimal("100")), 
                                                        10, COBOL_ROUNDING);
        BigDecimal dailyInterest = balance.multiply(dailyRate);

        return CobolDataConverter.preservePrecision(dailyInterest, MONETARY_SCALE);
    }

    /**
     * Calculates compound interest over specified periods.
     * 
     * Computes compound interest using the formula: P(1 + r/n)^(nt) - P
     * where P = principal, r = annual rate, n = compounds per year, t = time in years.
     * 
     * @param principal initial principal amount
     * @param annualRate annual interest rate as decimal (e.g., 0.05 for 5%)
     * @param compoundingPeriods number of compounding periods per year
     * @param timeInYears time period in years
     * @return compound interest amount earned
     * @throws IllegalArgumentException if parameters are null or invalid
     */
    public static BigDecimal calculateCompoundInterest(BigDecimal principal, BigDecimal annualRate, 
                                                     int compoundingPeriods, BigDecimal timeInYears) {
        validateAmount(principal, "Principal");
        validateAmount(annualRate, "Annual rate");
        validateAmount(timeInYears, "Time in years");

        if (compoundingPeriods <= 0) {
            throw new IllegalArgumentException("Compounding periods must be positive: " + compoundingPeriods);
        }

        if (annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Annual rate cannot be negative: " + annualRate);
        }

        if (timeInYears.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Time cannot be negative: " + timeInYears);
        }

        // Handle zero cases
        if (principal.compareTo(BigDecimal.ZERO) == 0 || annualRate.compareTo(BigDecimal.ZERO) == 0 ||
            timeInYears.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, COBOL_ROUNDING);
        }

        // Calculate: P * (1 + r/n)^(n*t) - P
        BigDecimal periodicRate = annualRate.divide(new BigDecimal(compoundingPeriods), 10, COBOL_ROUNDING);
        BigDecimal onePlusRate = BigDecimal.ONE.add(periodicRate);
        BigDecimal exponent = new BigDecimal(compoundingPeriods).multiply(timeInYears);
        
        // Use BigDecimal power approximation for compound calculation
        BigDecimal compoundFactor = calculatePower(onePlusRate, exponent.intValue());
        BigDecimal finalAmount = principal.multiply(compoundFactor);
        BigDecimal compoundInterest = finalAmount.subtract(principal);

        return CobolDataConverter.preservePrecision(compoundInterest, MONETARY_SCALE);
    }

    /**
     * Allocates payment amount specifically to principal balance.
     * 
     * Determines how much of a payment should be applied to principal
     * after interest obligations are satisfied.
     * 
     * @param paymentAmount total payment amount
     * @param interestDue amount of interest currently due
     * @return amount to be allocated to principal
     * @throws IllegalArgumentException if parameters are null or payment is negative
     */
    public static BigDecimal allocatePaymentToPrincipal(BigDecimal paymentAmount, BigDecimal interestDue) {
        validateAmount(paymentAmount, "Payment amount");
        validateAmount(interestDue, "Interest due");

        if (paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative: " + paymentAmount);
        }

        // If payment is less than or equal to interest due, all goes to interest
        if (paymentAmount.compareTo(interestDue) <= 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, COBOL_ROUNDING);
        }

        // Remaining amount after covering interest goes to principal
        BigDecimal principalAllocation = paymentAmount.subtract(interestDue);
        return CobolDataConverter.preservePrecision(principalAllocation, MONETARY_SCALE);
    }

    /**
     * Allocates payment amount specifically to interest balance.
     * 
     * Determines how much of a payment should be applied to outstanding
     * interest charges, following payment hierarchy rules.
     * 
     * @param paymentAmount total payment amount
     * @param interestDue amount of interest currently due
     * @return amount to be allocated to interest
     * @throws IllegalArgumentException if parameters are null or payment is negative
     */
    public static BigDecimal allocatePaymentToInterest(BigDecimal paymentAmount, BigDecimal interestDue) {
        validateAmount(paymentAmount, "Payment amount");
        validateAmount(interestDue, "Interest due");

        if (paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative: " + paymentAmount);
        }

        // Interest allocation is the minimum of payment amount and interest due
        BigDecimal interestAllocation = paymentAmount.min(interestDue);
        return CobolDataConverter.preservePrecision(interestAllocation, MONETARY_SCALE);
    }

    /**
     * Helper method to calculate power for compound interest calculations.
     * 
     * Implements power calculation using repeated multiplication for
     * integer exponents, maintaining BigDecimal precision.
     * 
     * @param base base value
     * @param exponent integer exponent
     * @return base raised to the power of exponent
     */
    private static BigDecimal calculatePower(BigDecimal base, int exponent) {
        if (exponent == 0) {
            return BigDecimal.ONE;
        }
        
        if (exponent == 1) {
            return base;
        }

        BigDecimal result = BigDecimal.ONE;
        BigDecimal currentBase = base;
        int currentExponent = Math.abs(exponent);

        while (currentExponent > 0) {
            if (currentExponent % 2 == 1) {
                result = result.multiply(currentBase);
            }
            currentBase = currentBase.multiply(currentBase);
            currentExponent /= 2;
        }

        return exponent < 0 ? BigDecimal.ONE.divide(result, 10, COBOL_ROUNDING) : result;
    }
}