/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for AmountCalculator financial calculation utilities.
 * 
 * This test class validates that all financial calculations maintain exact precision 
 * matching COBOL COMP-3 arithmetic behavior. Tests cover interest calculations, 
 * balance updates, payment processing, and all edge cases to ensure 100% functional 
 * parity with the original CBACT04C and COBIL00C COBOL programs.
 * 
 * Key validation areas:
 * - Interest calculation precision matching CBACT04C formula: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 * - Balance calculations maintaining penny-level accuracy from COBIL00C
 * - Payment allocation between principal and interest components
 * - Credit limit validation and fee calculations
 * - COBOL ROUNDED clause behavior using HALF_UP rounding mode
 * - Overflow/underflow handling for COBOL S9(10)V99 field limits
 * - Negative balance scenarios and overdraft processing
 * - Percentage calculations with proper scale preservation
 * 
 * All test data and expected results derived from known COBOL calculation scenarios
 * to ensure byte-for-byte identical output between Java and COBOL implementations.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@DisplayName("AmountCalculator Financial Calculations Test Suite")
public class AmountCalculatorTest {

    // Test data constants matching COBOL copybook field specifications
    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode COBOL_ROUNDING = RoundingMode.HALF_UP;
    
    // Test amounts matching COBOL S9(10)V99 field limits
    private BigDecimal testBalance;
    private BigDecimal testInterestRate;
    private BigDecimal testTransactionAmount;
    private BigDecimal testCreditLimit;
    private BigDecimal testPaymentAmount;
    private BigDecimal testFeeRate;
    
    // COMP-3 test data for precision validation
    private byte[] comp3Balance;
    private byte[] comp3InterestRate;

    /**
     * Sets up test data before each test execution.
     * Initializes AmountCalculator instance and common test data values
     * representing typical financial scenarios from COBOL programs.
     */
    @BeforeEach
    void setUp() {
        // Initialize standard test amounts
        testBalance = new BigDecimal("1250.75");
        testInterestRate = new BigDecimal("18.25"); // 18.25% annual rate
        testTransactionAmount = new BigDecimal("150.00");
        testCreditLimit = new BigDecimal("5000.00");
        testPaymentAmount = new BigDecimal("200.00");
        testFeeRate = new BigDecimal("2.50"); // 2.5% fee rate
        
        // Create COMP-3 test data for precision validation
        // 1250.75 in COMP-3 format: 0x01, 0x25, 0x07, 0x5C (positive)
        comp3Balance = new byte[]{(byte) 0x01, (byte) 0x25, (byte) 0x07, (byte) 0x5C};
        
        // 18.25 in COMP-3 format with 4 decimal places: 0x01, 0x82, 0x50, 0x0C
        comp3InterestRate = new byte[]{(byte) 0x01, (byte) 0x82, (byte) 0x50, (byte) 0x0C};
    }

    /**
     * Tests for monthly interest calculation using CBACT04C formula.
     * Validates the core interest calculation: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
     */
    @Nested
    @DisplayName("Monthly Interest Calculation Tests")
    class MonthlyInterestCalculationTests {

        @Test
        @DisplayName("Calculate monthly interest with standard values")
        void testCalculateMonthlyInterest() {
            // Test the exact CBACT04C formula
            BigDecimal result = AmountCalculator.calculateMonthlyInterest(testBalance, testInterestRate);
            
            // Expected: (1250.75 * 18.25) / 1200 = 19.02239... = 19.02 (with HALF_UP rounding)
            BigDecimal expected = new BigDecimal("19.02");
            
            assertEquals(expected, result, "Monthly interest calculation should match CBACT04C formula");
            assertEquals(MONETARY_SCALE, result.scale(), "Result should have monetary scale");
        }

        @Test
        @DisplayName("Calculate monthly interest with zero balance")
        void testCalculateMonthlyInterestZeroBalance() {
            BigDecimal zeroBalance = BigDecimal.ZERO;
            BigDecimal result = AmountCalculator.calculateMonthlyInterest(zeroBalance, testInterestRate);
            
            assertEquals(BigDecimal.ZERO.setScale(MONETARY_SCALE), result, 
                "Zero balance should result in zero interest");
        }

        @Test
        @DisplayName("Calculate monthly interest with zero rate")
        void testCalculateMonthlyInterestZeroRate() {
            BigDecimal zeroRate = BigDecimal.ZERO;
            BigDecimal result = AmountCalculator.calculateMonthlyInterest(testBalance, zeroRate);
            
            assertEquals(BigDecimal.ZERO.setScale(MONETARY_SCALE), result, 
                "Zero interest rate should result in zero interest");
        }

        @Test
        @DisplayName("Calculate monthly interest throws exception for null balance")
        void testCalculateMonthlyInterestNullBalance() {
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.calculateMonthlyInterest(null, testInterestRate),
                "Null balance should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Calculate monthly interest throws exception for null rate")
        void testCalculateMonthlyInterestNullRate() {
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.calculateMonthlyInterest(testBalance, null),
                "Null interest rate should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Calculate monthly interest throws exception for negative rate")
        void testCalculateMonthlyInterestNegativeRate() {
            BigDecimal negativeRate = new BigDecimal("-5.00");
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.calculateMonthlyInterest(testBalance, negativeRate),
                "Negative interest rate should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Calculate monthly interest from COMP-3 data")
        void testCalculateMonthlyInterestFromComp3() {
            BigDecimal result = AmountCalculator.calculateMonthlyInterestFromComp3(comp3Balance, comp3InterestRate);
            
            // Expected: (1250.75 * 18.25) / 1200 = 19.02239... = 19.02 (with HALF_UP rounding)
            BigDecimal expected = new BigDecimal("19.02");
            
            assertEquals(expected, result, "COMP-3 interest calculation should match BigDecimal calculation");
        }

        @Test
        @DisplayName("Calculate monthly interest from COMP-3 throws exception for null data")
        void testCalculateMonthlyInterestFromComp3NullData() {
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.calculateMonthlyInterestFromComp3(null, comp3InterestRate),
                "Null COMP-3 balance data should throw IllegalArgumentException");
        }

        @ParameterizedTest
        @CsvSource({
            "1000.00, 12.00, 10.00",     // Standard case
            "2500.50, 15.75, 32.82",    // Higher amounts
            "500.25, 24.99, 10.42",     // High interest rate
            "0.01, 36.00, 0.00",        // Minimal balance
            "999999.99, 0.01, 8.33"     // Maximum balance, minimal rate
        })
        @DisplayName("Calculate monthly interest with various parameter combinations")
        void testCalculateMonthlyInterestParameterized(String balanceStr, String rateStr, String expectedStr) {
            BigDecimal balance = new BigDecimal(balanceStr);
            BigDecimal rate = new BigDecimal(rateStr);
            BigDecimal expected = new BigDecimal(expectedStr);
            
            BigDecimal result = AmountCalculator.calculateMonthlyInterest(balance, rate);
            
            assertEquals(expected, result, 
                String.format("Interest calculation for balance=%s, rate=%s should equal %s", 
                    balanceStr, rateStr, expectedStr));
        }
    }

    /**
     * Tests for balance calculation matching COBIL00C payment processing logic.
     * Validates: COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT
     */
    @Nested
    @DisplayName("Balance Calculation Tests")
    class BalanceCalculationTests {

        @Test
        @DisplayName("Calculate balance with debit transaction")
        void testCalculateBalance() {
            BigDecimal result = AmountCalculator.calculateBalance(testBalance, testTransactionAmount);
            
            // Expected: 1250.75 - 150.00 = 1100.75
            BigDecimal expected = new BigDecimal("1100.75");
            
            assertEquals(expected, result, "Balance calculation should subtract transaction amount");
            assertEquals(MONETARY_SCALE, result.scale(), "Result should have monetary scale");
        }

        @Test
        @DisplayName("Calculate balance with credit transaction")
        void testCalculateBalanceCredit() {
            BigDecimal creditAmount = new BigDecimal("-100.00"); // Negative for credit
            BigDecimal result = AmountCalculator.calculateBalance(testBalance, creditAmount);
            
            // Expected: 1250.75 - (-100.00) = 1350.75
            BigDecimal expected = new BigDecimal("1350.75");
            
            assertEquals(expected, result, "Credit transaction should increase balance");
        }

        @Test
        @DisplayName("Calculate balance throws exception for null current balance")
        void testCalculateBalanceNullCurrentBalance() {
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.calculateBalance(null, testTransactionAmount),
                "Null current balance should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Calculate balance throws exception for null transaction amount")
        void testCalculateBalanceNullTransactionAmount() {
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.calculateBalance(testBalance, null),
                "Null transaction amount should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Calculate balance with zero transaction")
        void testCalculateBalanceZeroTransaction() {
            BigDecimal zeroTransaction = BigDecimal.ZERO;
            BigDecimal result = AmountCalculator.calculateBalance(testBalance, zeroTransaction);
            
            assertEquals(testBalance.setScale(MONETARY_SCALE), result, 
                "Zero transaction should not change balance");
        }
    }

    /**
     * Tests for payment processing allocation between principal and interest.
     * Validates payment hierarchy: interest first, then principal.
     */
    @Nested
    @DisplayName("Payment Processing Tests")
    class PaymentProcessingTests {

        @Test
        @DisplayName("Process payment with interest and principal")
        void testProcessPayment() {
            BigDecimal interestBalance = new BigDecimal("50.00");
            BigDecimal principalBalance = new BigDecimal("1000.00");
            
            BigDecimal[] result = AmountCalculator.processPayment(testPaymentAmount, interestBalance, principalBalance);
            
            // Expected: $50 to interest, $150 to principal, $850 remaining balance
            assertEquals(new BigDecimal("50.00"), result[0], "Interest payment allocation");
            assertEquals(new BigDecimal("150.00"), result[1], "Principal payment allocation");
            assertEquals(new BigDecimal("850.00"), result[2], "Remaining balance after payment");
        }

        @Test
        @DisplayName("Process payment when payment covers only interest")
        void testProcessPaymentInterestOnly() {
            BigDecimal paymentAmount = new BigDecimal("30.00");
            BigDecimal interestBalance = new BigDecimal("50.00");
            BigDecimal principalBalance = new BigDecimal("1000.00");
            
            BigDecimal[] result = AmountCalculator.processPayment(paymentAmount, interestBalance, principalBalance);
            
            // Expected: $30 to interest, $0 to principal, $1020 remaining balance
            assertEquals(new BigDecimal("30.00"), result[0], "Partial interest payment");
            assertEquals(new BigDecimal("0.00"), result[1], "No principal payment");
            assertEquals(new BigDecimal("1020.00"), result[2], "Full remaining balance");
        }

        @Test
        @DisplayName("Process payment when payment exceeds total balance")
        void testProcessPaymentExceedsBalance() {
            BigDecimal largePayment = new BigDecimal("2000.00");
            BigDecimal interestBalance = new BigDecimal("50.00");
            BigDecimal principalBalance = new BigDecimal("1000.00");
            
            BigDecimal[] result = AmountCalculator.processPayment(largePayment, interestBalance, principalBalance);
            
            // Expected: $50 to interest, $1000 to principal, $0 remaining balance
            assertEquals(new BigDecimal("50.00"), result[0], "Full interest payment");
            assertEquals(new BigDecimal("1000.00"), result[1], "Full principal payment");
            assertEquals(new BigDecimal("0.00"), result[2], "Zero remaining balance");
        }

        @Test
        @DisplayName("Process payment throws exception for negative payment")
        void testProcessPaymentNegativeAmount() {
            BigDecimal negativePayment = new BigDecimal("-100.00");
            BigDecimal interestBalance = new BigDecimal("50.00");
            BigDecimal principalBalance = new BigDecimal("1000.00");
            
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.processPayment(negativePayment, interestBalance, principalBalance),
                "Negative payment amount should throw IllegalArgumentException");
        }

        @ParameterizedTest
        @CsvSource({
            "100.00, 25.00, 500.00, 25.00, 75.00, 425.00",    // Partial payment
            "600.00, 50.00, 500.00, 50.00, 500.00, 0.00",     // Payment covers all
            "40.00, 75.00, 1000.00, 40.00, 0.00, 1035.00",    // Payment less than interest
            "0.00, 100.00, 500.00, 0.00, 0.00, 600.00"        // Zero payment
        })
        @DisplayName("Process payment with various scenarios")
        void testPaymentAllocationParameterized(String paymentStr, String interestStr, String principalStr,
                                               String expectedInterestStr, String expectedPrincipalStr, String expectedRemainingStr) {
            BigDecimal payment = new BigDecimal(paymentStr);
            BigDecimal interest = new BigDecimal(interestStr);
            BigDecimal principal = new BigDecimal(principalStr);
            BigDecimal expectedInterest = new BigDecimal(expectedInterestStr);
            BigDecimal expectedPrincipal = new BigDecimal(expectedPrincipalStr);
            BigDecimal expectedRemaining = new BigDecimal(expectedRemainingStr);
            
            BigDecimal[] result = AmountCalculator.processPayment(payment, interest, principal);
            
            assertEquals(expectedInterest, result[0], "Interest allocation mismatch");
            assertEquals(expectedPrincipal, result[1], "Principal allocation mismatch");
            assertEquals(expectedRemaining, result[2], "Remaining balance mismatch");
        }
    }

    /**
     * Tests for credit limit validation functionality.
     * Validates transaction approval based on available credit.
     */
    @Nested
    @DisplayName("Credit Limit Validation Tests")
    class CreditLimitValidationTests {

        @Test
        @DisplayName("Validate credit limit with approved transaction")
        void testValidateCreditLimit() {
            boolean result = AmountCalculator.validateCreditLimit(testBalance, testCreditLimit, testTransactionAmount);
            
            assertTrue(result, "Transaction within credit limit should be approved");
        }

        @Test
        @DisplayName("Validate credit limit with declined transaction")
        void testValidateCreditLimitDeclined() {
            BigDecimal largeTransaction = new BigDecimal("4000.00"); // Exceeds available credit
            boolean result = AmountCalculator.validateCreditLimit(testBalance, testCreditLimit, largeTransaction);
            
            assertFalse(result, "Transaction exceeding credit limit should be declined");
        }

        @Test
        @DisplayName("Validate credit limit at exact limit")
        void testValidateCreditLimitExactLimit() {
            BigDecimal exactLimitTransaction = new BigDecimal("3749.25"); // 5000 - 1250.75
            boolean result = AmountCalculator.validateCreditLimit(testBalance, testCreditLimit, exactLimitTransaction);
            
            assertTrue(result, "Transaction at exact credit limit should be approved");
        }

        @Test
        @DisplayName("Validate credit limit throws exception for negative limit")
        void testValidateCreditLimitNegativeLimit() {
            BigDecimal negativeLimit = new BigDecimal("-1000.00");
            
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.validateCreditLimit(testBalance, negativeLimit, testTransactionAmount),
                "Negative credit limit should throw IllegalArgumentException");
        }
    }

    /**
     * Tests for fee calculation functionality.
     * Validates percentage-based fee calculations with proper rounding.
     */
    @Nested
    @DisplayName("Fee Calculation Tests")
    class FeeCalculationTests {

        @Test
        @DisplayName("Calculate fee with standard values")
        void testCalculateFee() {
            BigDecimal result = AmountCalculator.calculateFee(testTransactionAmount, testFeeRate);
            
            // Expected: (150.00 * 2.50) / 100 = 3.75
            BigDecimal expected = new BigDecimal("3.75");
            
            assertEquals(expected, result, "Fee calculation should apply percentage correctly");
            assertEquals(MONETARY_SCALE, result.scale(), "Fee should have monetary scale");
        }

        @Test
        @DisplayName("Calculate fee with zero amount")
        void testCalculateFeeZeroAmount() {
            BigDecimal zeroAmount = BigDecimal.ZERO;
            BigDecimal result = AmountCalculator.calculateFee(zeroAmount, testFeeRate);
            
            assertEquals(BigDecimal.ZERO.setScale(MONETARY_SCALE), result, 
                "Zero amount should result in zero fee");
        }

        @Test
        @DisplayName("Calculate fee with zero rate")
        void testCalculateFeeZeroRate() {
            BigDecimal zeroRate = BigDecimal.ZERO;
            BigDecimal result = AmountCalculator.calculateFee(testTransactionAmount, zeroRate);
            
            assertEquals(BigDecimal.ZERO.setScale(MONETARY_SCALE), result, 
                "Zero fee rate should result in zero fee");
        }

        @Test
        @DisplayName("Calculate fee throws exception for negative rate")
        void testCalculateFeeNegativeRate() {
            BigDecimal negativeRate = new BigDecimal("-1.50");
            
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.calculateFee(testTransactionAmount, negativeRate),
                "Negative fee rate should throw IllegalArgumentException");
        }
    }

    /**
     * Tests for COBOL ROUNDED clause rounding behavior.
     * Validates HALF_UP rounding mode matches COBOL behavior.
     */
    @Nested
    @DisplayName("COBOL Rounding Tests")
    class CobolRoundingTests {

        @Test
        @DisplayName("Apply COBOL rounding with HALF_UP behavior")
        void testApplyRounding() {
            BigDecimal amountToRound = new BigDecimal("123.456");
            BigDecimal result = AmountCalculator.applyRounding(amountToRound);
            
            BigDecimal expected = new BigDecimal("123.46"); // HALF_UP rounding
            
            assertEquals(expected, result, "Rounding should use HALF_UP mode");
            assertEquals(MONETARY_SCALE, result.scale(), "Rounded amount should have monetary scale");
        }

        @Test
        @DisplayName("Apply rounding with amount already at correct scale")
        void testApplyRoundingCorrectScale() {
            BigDecimal correctScaleAmount = new BigDecimal("123.45");
            BigDecimal result = AmountCalculator.applyRounding(correctScaleAmount);
            
            assertEquals(correctScaleAmount, result, "Amount with correct scale should remain unchanged");
        }

        @Test
        @DisplayName("Apply rounding throws exception for null amount")
        void testApplyRoundingNullAmount() {
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.applyRounding(null),
                "Null amount should throw IllegalArgumentException");
        }
    }

    /**
     * Tests for overflow and underflow handling.
     * Validates COBOL S9(10)V99 field limit enforcement.
     */
    @Nested
    @DisplayName("Overflow and Underflow Handling Tests")
    class OverflowUnderflowTests {

        @Test
        @DisplayName("Handle overflow detection")
        void testHandleOverflow() {
            BigDecimal maxAmount = new BigDecimal("999999999.99");
            
            assertDoesNotThrow(() -> AmountCalculator.handleOverflow(maxAmount),
                "Maximum valid amount should not trigger overflow");
            
            BigDecimal overflowAmount = new BigDecimal("1000000000.00");
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.handleOverflow(overflowAmount),
                "Amount exceeding maximum should throw overflow exception");
        }

        @Test
        @DisplayName("Handle underflow detection")
        void testHandleUnderflow() {
            BigDecimal minAmount = new BigDecimal("-999999999.99");
            
            assertDoesNotThrow(() -> AmountCalculator.handleUnderflow(minAmount),
                "Minimum valid amount should not trigger underflow");
            
            BigDecimal underflowAmount = new BigDecimal("-1000000000.00");
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.handleUnderflow(underflowAmount),
                "Amount below minimum should throw underflow exception");
        }

        @Test
        @DisplayName("Handle overflow with null amount")
        void testHandleOverflowNull() {
            assertDoesNotThrow(() -> AmountCalculator.handleOverflow(null),
                "Null amount should not trigger overflow exception");
        }

        @Test
        @DisplayName("Handle underflow with null amount")
        void testHandleUnderflowNull() {
            assertDoesNotThrow(() -> AmountCalculator.handleUnderflow(null),
                "Null amount should not trigger underflow exception");
        }
    }

    /**
     * Tests for negative balance and overdraft processing.
     * Validates overdraft fee application and limit enforcement.
     */
    @Nested
    @DisplayName("Negative Balance Processing Tests")
    class NegativeBalanceProcessingTests {

        @Test
        @DisplayName("Process negative balance within overdraft limit")
        void testProcessNegativeBalance() {
            BigDecimal negativeBalance = new BigDecimal("-100.00");
            BigDecimal overdraftLimit = new BigDecimal("500.00");
            BigDecimal overdraftFee = new BigDecimal("35.00");
            
            BigDecimal result = AmountCalculator.processNegativeBalance(negativeBalance, overdraftLimit, overdraftFee);
            
            BigDecimal expected = new BigDecimal("-135.00"); // -100.00 - 35.00 fee
            
            assertEquals(expected, result, "Negative balance should have overdraft fee applied");
        }

        @Test
        @DisplayName("Process positive balance")
        void testProcessPositiveBalance() {
            BigDecimal positiveBalance = new BigDecimal("100.00");
            BigDecimal overdraftLimit = new BigDecimal("500.00");
            BigDecimal overdraftFee = new BigDecimal("35.00");
            
            BigDecimal result = AmountCalculator.processNegativeBalance(positiveBalance, overdraftLimit, overdraftFee);
            
            assertEquals(positiveBalance, result, "Positive balance should remain unchanged");
        }

        @Test
        @DisplayName("Process negative balance exceeding overdraft limit")
        void testProcessNegativeBalanceExceedsLimit() {
            BigDecimal negativeBalance = new BigDecimal("-600.00");
            BigDecimal overdraftLimit = new BigDecimal("500.00");
            BigDecimal overdraftFee = new BigDecimal("35.00");
            
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.processNegativeBalance(negativeBalance, overdraftLimit, overdraftFee),
                "Overdraft exceeding limit should throw IllegalArgumentException");
        }
    }

    /**
     * Tests for percentage calculations with proper scale handling.
     * Validates percentage computations maintain precision.
     */
    @Nested
    @DisplayName("Percentage Calculation Tests")
    class PercentageCalculationTests {

        @Test
        @DisplayName("Calculate percentage with standard values")
        void testCalculatePercentage() {
            BigDecimal baseAmount = new BigDecimal("1000.00");
            BigDecimal percentage = new BigDecimal("15.5");
            
            BigDecimal result = AmountCalculator.calculatePercentage(baseAmount, percentage);
            
            BigDecimal expected = new BigDecimal("155.00"); // (1000.00 * 15.5) / 100
            
            assertEquals(expected, result, "Percentage calculation should be accurate");
            assertEquals(MONETARY_SCALE, result.scale(), "Result should have monetary scale");
        }

        @Test
        @DisplayName("Calculate percentage with zero base amount")
        void testCalculatePercentageZeroBase() {
            BigDecimal zeroBase = BigDecimal.ZERO;
            BigDecimal percentage = new BigDecimal("10.0");
            
            BigDecimal result = AmountCalculator.calculatePercentage(zeroBase, percentage);
            
            assertEquals(BigDecimal.ZERO.setScale(MONETARY_SCALE), result, 
                "Zero base amount should result in zero");
        }

        @Test
        @DisplayName("Calculate percentage with zero percentage")
        void testCalculatePercentageZeroPercent() {
            BigDecimal baseAmount = new BigDecimal("500.00");
            BigDecimal zeroPercentage = BigDecimal.ZERO;
            
            BigDecimal result = AmountCalculator.calculatePercentage(baseAmount, zeroPercentage);
            
            assertEquals(BigDecimal.ZERO.setScale(MONETARY_SCALE), result, 
                "Zero percentage should result in zero");
        }
    }

    /**
     * Tests for amount validation functionality.
     * Validates input validation and error handling.
     */
    @Nested
    @DisplayName("Amount Validation Tests")
    class AmountValidationTests {

        @Test
        @DisplayName("Validate amount with valid BigDecimal")
        void testValidateAmount() {
            assertDoesNotThrow(() -> AmountCalculator.validateAmount(testBalance, "Test Balance"),
                "Valid BigDecimal should not throw exception");
        }

        @Test
        @DisplayName("Validate amount throws exception for null value")
        void testValidateAmountNull() {
            assertThrows(IllegalArgumentException.class, 
                () -> AmountCalculator.validateAmount(null, "Test Field"),
                "Null amount should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Validate amount with zero value")
        void testValidateAmountZero() {
            assertDoesNotThrow(() -> AmountCalculator.validateAmount(BigDecimal.ZERO, "Zero Amount"),
                "Zero amount should be valid");
        }

        @Test
        @DisplayName("Validate amount with negative value")
        void testValidateAmountNegative() {
            BigDecimal negativeAmount = new BigDecimal("-500.00");
            assertDoesNotThrow(() -> AmountCalculator.validateAmount(negativeAmount, "Negative Amount"),
                "Negative amount should be valid for validation");
        }
    }

    /**
     * Tests for COBOL precision matching validation.
     * Validates calculations match exactly with COBOL COMP-3 behavior.
     */
    @Nested
    @DisplayName("COBOL Precision Matching Tests")
    class CobolPrecisionMatchingTests {

        @Test
        @DisplayName("Test COBOL precision matching for known calculation scenarios")
        void testCobolPrecisionMatching() {
            // Test scenario from CBACT04C interest calculation
            BigDecimal balance = CobolDataConverter.fromComp3(comp3Balance, MONETARY_SCALE);
            BigDecimal rate = CobolDataConverter.fromComp3(comp3InterestRate, 4);
            
            BigDecimal javaResult = AmountCalculator.calculateMonthlyInterest(balance, rate);
            
            // Expected result calculated using COBOL COMP-3 arithmetic
            BigDecimal expectedCobolResult = new BigDecimal("19.02");
            
            assertEquals(expectedCobolResult, javaResult, 
                "Java calculation should match COBOL COMP-3 result exactly");
        }

        @Test
        @DisplayName("Validate BigDecimal precision preservation")
        void testBigDecimalPrecisionPreservation() {
            BigDecimal testAmount = new BigDecimal("123.456789");
            BigDecimal preservedAmount = CobolDataConverter.preservePrecision(testAmount, MONETARY_SCALE);
            
            assertEquals(MONETARY_SCALE, preservedAmount.scale(), 
                "Preserved amount should have correct scale");
            assertEquals(new BigDecimal("123.46"), preservedAmount, 
                "Precision should be preserved with COBOL rounding");
        }

        @Test
        @DisplayName("Validate COMP-3 to BigDecimal conversion accuracy")
        void testComp3ConversionAccuracy() {
            // Test multiple COMP-3 scenarios
            byte[] comp3Zero = new byte[]{(byte) 0x0C};
            byte[] comp3Negative = new byte[]{(byte) 0x12, (byte) 0x34, (byte) 0x5D};
            
            BigDecimal zeroResult = CobolDataConverter.fromComp3(comp3Zero, MONETARY_SCALE);
            BigDecimal negativeResult = CobolDataConverter.fromComp3(comp3Negative, MONETARY_SCALE);
            
            assertEquals(BigDecimal.ZERO.setScale(MONETARY_SCALE), zeroResult, 
                "COMP-3 zero should convert correctly");
            assertEquals(new BigDecimal("-123.45"), negativeResult, 
                "COMP-3 negative should convert correctly");
        }

        @ParameterizedTest
        @CsvSource({
            "1000.00, 12.00, 10.00",
            "2500.00, 18.75, 39.06", 
            "750.50, 21.50, 13.45",
            "999999.99, 0.01, 8.33"
        })
        @DisplayName("Validate interest calculation precision across multiple scenarios")
        void testInterestCalculationParameterized(String balanceStr, String rateStr, String expectedStr) {
            BigDecimal balance = new BigDecimal(balanceStr);
            BigDecimal rate = new BigDecimal(rateStr);
            BigDecimal expected = new BigDecimal(expectedStr);
            
            BigDecimal result = AmountCalculator.calculateMonthlyInterest(balance, rate);
            
            assertEquals(expected, result, 
                String.format("Interest calculation should be precise for balance=%s, rate=%s", 
                    balanceStr, rateStr));
            assertEquals(MONETARY_SCALE, result.scale(), 
                "All results should maintain monetary scale");
        }
    }

    /**
     * Integration tests for comprehensive financial calculation workflows.
     * Tests complete financial scenarios combining multiple operations.
     */
    @Nested
    @DisplayName("Integration Workflow Tests")
    class IntegrationWorkflowTests {

        @Test
        @DisplayName("Complete financial calculation workflow")
        void testCompleteFinancialWorkflow() {
            // Simulate complete account processing scenario
            BigDecimal initialBalance = new BigDecimal("1500.00");
            BigDecimal creditLimit = new BigDecimal("5000.00");
            BigDecimal transactionAmount = new BigDecimal("250.00");
            BigDecimal interestRate = new BigDecimal("19.99");
            BigDecimal paymentAmount = new BigDecimal("100.00");
            
            // Step 1: Validate credit limit
            boolean creditApproved = AmountCalculator.validateCreditLimit(
                initialBalance, creditLimit, transactionAmount);
            assertTrue(creditApproved, "Transaction should be approved");
            
            // Step 2: Update balance with transaction
            BigDecimal newBalance = AmountCalculator.calculateBalance(
                initialBalance, transactionAmount);
            assertEquals(new BigDecimal("1250.00"), newBalance, "Balance should be updated");
            
            // Step 3: Calculate monthly interest
            BigDecimal monthlyInterest = AmountCalculator.calculateMonthlyInterest(
                newBalance, interestRate);
            assertEquals(new BigDecimal("20.82"), monthlyInterest, "Monthly interest should be calculated");
            
            // Step 4: Add interest to balance
            BigDecimal balanceWithInterest = newBalance.add(monthlyInterest);
            assertEquals(new BigDecimal("1270.82"), balanceWithInterest, "Interest should be added");
            
            // Step 5: Process payment
            BigDecimal[] paymentResult = AmountCalculator.processPayment(
                paymentAmount, monthlyInterest, newBalance);
            assertEquals(new BigDecimal("20.82"), paymentResult[0], "Interest payment");
            assertEquals(new BigDecimal("79.18"), paymentResult[1], "Principal payment");
            assertEquals(new BigDecimal("1170.82"), paymentResult[2], "Remaining balance");
        }

        @Test
        @DisplayName("Overdraft scenario workflow")
        void testOverdraftScenarioWorkflow() {
            BigDecimal lowBalance = new BigDecimal("50.00");
            BigDecimal largeTransaction = new BigDecimal("200.00");
            BigDecimal overdraftLimit = new BigDecimal("300.00");
            BigDecimal overdraftFee = new BigDecimal("35.00");
            
            // Process transaction that causes overdraft
            BigDecimal newBalance = AmountCalculator.calculateBalance(lowBalance, largeTransaction);
            assertEquals(new BigDecimal("-150.00"), newBalance, "Balance should be negative");
            
            // Apply overdraft processing
            BigDecimal processedBalance = AmountCalculator.processNegativeBalance(
                newBalance, overdraftLimit, overdraftFee);
            assertEquals(new BigDecimal("-185.00"), processedBalance, "Overdraft fee should be applied");
        }
    }
}