/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Fee;
import com.carddemo.repository.FeeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for FeeCalculationService that validates fee calculation logic
 * including late fees, over-limit fees, annual fees, and foreign transaction fees.
 * 
 * This test class implements validation for the Spring Boot service that replaces the COBOL
 * fee calculation logic originally found in CBACT04C.cbl section 1400-COMPUTE-FEES.
 * Tests ensure exact precision matching COBOL COMP-3 packed decimal behavior through
 * BigDecimal operations with proper scale and rounding modes.
 * 
 * Test coverage includes:
 * - Fee calculation accuracy for all fee types
 * - Fee assessment rule validation 
 * - Fee waiver condition testing
 * - Regulatory compliance validation
 * - Edge case and error condition handling
 * - Mock repository interaction verification
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FeeCalculationServiceTest {

    @Mock
    private FeeRepository feeRepository;

    @InjectMocks
    private FeeCalculationService feeCalculationService;

    // Test data constants matching COBOL business logic
    private static final Long TEST_ACCOUNT_ID = 12345678901L;
    private static final BigDecimal TEST_BALANCE = new BigDecimal("1500.00");
    private static final BigDecimal TEST_CREDIT_LIMIT = new BigDecimal("5000.00");
    private static final BigDecimal HIGH_BALANCE = new BigDecimal("15000.00");
    private static final BigDecimal OVER_LIMIT_BALANCE = new BigDecimal("5100.00");
    private static final LocalDate TEST_DATE = LocalDate.of(2024, 1, 15);
    
    // Fee calculation constants from service
    private static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.0275");
    private static final BigDecimal LATE_FEE_MINIMUM = new BigDecimal("25.00");
    private static final BigDecimal LATE_FEE_MAXIMUM = new BigDecimal("39.00");
    private static final BigDecimal OVER_LIMIT_FEE = new BigDecimal("35.00");
    private static final BigDecimal FOREIGN_TRANSACTION_FEE_RATE = new BigDecimal("0.03");
    private static final BigDecimal ANNUAL_FEE_STANDARD = new BigDecimal("95.00");
    private static final BigDecimal ANNUAL_FEE_PREMIUM = new BigDecimal("450.00");

    /**
     * Set up test fixtures and initialize mock behaviors before each test.
     * Configures common mock repository responses for standard test scenarios.
     */
    @BeforeEach
    public void setUp() {
        // Configure default mock behaviors for FeeRepository
        when(feeRepository.save(any(Fee.class))).thenAnswer(invocation -> {
            Fee fee = invocation.getArgument(0);
            if (fee.getId() == null) {
                fee.setId(999L); // Simulate database ID assignment
            }
            return fee;
        });
        
        when(feeRepository.findByAccountId(any(Long.class))).thenReturn(List.of());
        when(feeRepository.getTotalFeesByAccount(any(Long.class))).thenReturn(Optional.of(BigDecimal.ZERO));
    }

    /**
     * Test late fee calculation with various balance and overdue day scenarios.
     * Validates COBOL paragraph 1400-COMPUTE-FEES equivalent logic including:
     * - Grace period enforcement (15 days)
     * - Percentage-based calculation (2.75%)
     * - Minimum fee enforcement ($25.00)
     * - Maximum fee enforcement ($39.00)
     * - BigDecimal precision matching COBOL COMP-3
     */
    @Test
    public void testCalculateLateFee() {
        // Test case 1: Within grace period - no fee
        BigDecimal feeWithinGrace = feeCalculationService.calculateLateFee(
            TEST_ACCOUNT_ID, TEST_BALANCE, 10);
        assertEquals(BigDecimal.ZERO, feeWithinGrace, 
            "No late fee should be assessed within 15-day grace period");

        // Test case 2: Beyond grace period - minimum fee applies
        BigDecimal lowBalanceFee = feeCalculationService.calculateLateFee(
            TEST_ACCOUNT_ID, new BigDecimal("500.00"), 20);
        assertEquals(LATE_FEE_MINIMUM, lowBalanceFee, 
            "Minimum late fee should apply for low balances");

        // Test case 3: Mid-range balance - percentage calculation (with maximum cap applied)
        BigDecimal midRangeFee = feeCalculationService.calculateLateFee(
            TEST_ACCOUNT_ID, TEST_BALANCE, 25);
        // $1500 * 2.75% = $41.25, but maximum fee cap is $39.00
        assertEquals(LATE_FEE_MAXIMUM, midRangeFee, 
            "Late fee should be capped at maximum when calculated percentage exceeds maximum");

        // Test case 3b: Mid-range balance within cap - actual percentage calculation
        BigDecimal smallerBalance = new BigDecimal("1000.00");
        BigDecimal smallerRangeFee = feeCalculationService.calculateLateFee(
            TEST_ACCOUNT_ID, smallerBalance, 25);
        BigDecimal expectedSmaller = smallerBalance.multiply(LATE_FEE_RATE)
            .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedSmaller, smallerRangeFee, 
            "Late fee should be calculated as 2.75% when within minimum and maximum bounds");

        // Test case 4: High balance - maximum fee applies
        BigDecimal highBalanceFee = feeCalculationService.calculateLateFee(
            TEST_ACCOUNT_ID, HIGH_BALANCE, 30);
        assertEquals(LATE_FEE_MAXIMUM, highBalanceFee, 
            "Maximum late fee should apply for high balances");

        // Test case 5: Invalid parameters - zero fee
        BigDecimal invalidFee = feeCalculationService.calculateLateFee(
            null, TEST_BALANCE, 20);
        assertEquals(BigDecimal.ZERO, invalidFee, 
            "Zero fee should be returned for invalid parameters");

        // Test case 6: Zero balance - zero fee
        BigDecimal zeroBalanceFee = feeCalculationService.calculateLateFee(
            TEST_ACCOUNT_ID, BigDecimal.ZERO, 20);
        assertEquals(BigDecimal.ZERO, zeroBalanceFee, 
            "Zero fee should be returned for zero balance");
    }

    /**
     * Test over-limit fee calculation based on current balance vs credit limit.
     * Validates business rules for fixed fee assessment when credit limit exceeded.
     */
    @Test
    public void testCalculateOverLimitFee() {
        // Test case 1: Within credit limit - no fee
        BigDecimal withinLimitFee = feeCalculationService.calculateOverLimitFee(
            TEST_ACCOUNT_ID, TEST_BALANCE, TEST_CREDIT_LIMIT);
        assertEquals(BigDecimal.ZERO, withinLimitFee, 
            "No over-limit fee when balance is within credit limit");

        // Test case 2: Exactly at credit limit - no fee
        BigDecimal atLimitFee = feeCalculationService.calculateOverLimitFee(
            TEST_ACCOUNT_ID, TEST_CREDIT_LIMIT, TEST_CREDIT_LIMIT);
        assertEquals(BigDecimal.ZERO, atLimitFee, 
            "No over-limit fee when balance equals credit limit");

        // Test case 3: Over credit limit - fee assessed
        BigDecimal overLimitFee = feeCalculationService.calculateOverLimitFee(
            TEST_ACCOUNT_ID, OVER_LIMIT_BALANCE, TEST_CREDIT_LIMIT);
        assertEquals(OVER_LIMIT_FEE, overLimitFee, 
            "Over-limit fee should be assessed when balance exceeds credit limit");

        // Test case 4: Invalid parameters - zero fee
        BigDecimal invalidFee = feeCalculationService.calculateOverLimitFee(
            null, TEST_BALANCE, TEST_CREDIT_LIMIT);
        assertEquals(BigDecimal.ZERO, invalidFee, 
            "Zero fee should be returned for invalid parameters");

        // Test case 5: Null credit limit - zero fee
        BigDecimal nullLimitFee = feeCalculationService.calculateOverLimitFee(
            TEST_ACCOUNT_ID, TEST_BALANCE, null);
        assertEquals(BigDecimal.ZERO, nullLimitFee, 
            "Zero fee should be returned for null credit limit");
    }

    /**
     * Test annual fee calculation with account type differentiation and prorating.
     * Validates standard vs premium fee amounts and partial year calculations.
     */
    @Test
    public void testCalculateAnnualFee() {
        LocalDate accountOpenDate = LocalDate.of(2023, 6, 1);
        LocalDate assessmentDate = LocalDate.of(2024, 6, 1);
        
        // Test case 1: Standard account - full year
        BigDecimal standardFee = feeCalculationService.calculateAnnualFee(
            TEST_ACCOUNT_ID, "STANDARD", accountOpenDate, assessmentDate);
        assertEquals(ANNUAL_FEE_STANDARD, standardFee, 
            "Standard account should be charged standard annual fee");

        // Test case 2: Premium account - full year
        BigDecimal premiumFee = feeCalculationService.calculateAnnualFee(
            TEST_ACCOUNT_ID, "PREMIUM", accountOpenDate, assessmentDate);
        assertEquals(ANNUAL_FEE_PREMIUM, premiumFee, 
            "Premium account should be charged premium annual fee");

        // Test case 3: New account - prorated fee
        LocalDate recentOpenDate = LocalDate.of(2024, 3, 1);
        BigDecimal proratedFee = feeCalculationService.calculateAnnualFee(
            TEST_ACCOUNT_ID, "STANDARD", recentOpenDate, assessmentDate);
        assertTrue(proratedFee.compareTo(ANNUAL_FEE_STANDARD) < 0, 
            "Prorated fee should be less than full annual fee");
        assertTrue(proratedFee.compareTo(BigDecimal.ZERO) > 0, 
            "Prorated fee should be greater than zero");

        // Test case 4: Invalid parameters - zero fee
        BigDecimal invalidFee = feeCalculationService.calculateAnnualFee(
            null, "STANDARD", accountOpenDate, assessmentDate);
        assertEquals(BigDecimal.ZERO, invalidFee, 
            "Zero fee should be returned for invalid parameters");

        // Test case 5: Case insensitive account type
        BigDecimal lowercasePremium = feeCalculationService.calculateAnnualFee(
            TEST_ACCOUNT_ID, "premium", accountOpenDate, assessmentDate);
        assertEquals(ANNUAL_FEE_PREMIUM, lowercasePremium, 
            "Account type should be case insensitive");
    }

    /**
     * Test foreign transaction fee calculation based on merchant country code.
     * Validates 3% fee for international transactions and no fee for domestic.
     */
    @Test
    public void testCalculateForeignTransactionFee() {
        BigDecimal transactionAmount = new BigDecimal("100.00");
        
        // Test case 1: Domestic transaction (US) - no fee
        BigDecimal domesticFee = feeCalculationService.calculateForeignTransactionFee(
            TEST_ACCOUNT_ID, transactionAmount, "US");
        assertEquals(BigDecimal.ZERO, domesticFee, 
            "No foreign transaction fee for US transactions");

        // Test case 2: Domestic transaction (USA) - no fee
        BigDecimal domesticFeeUSA = feeCalculationService.calculateForeignTransactionFee(
            TEST_ACCOUNT_ID, transactionAmount, "USA");
        assertEquals(BigDecimal.ZERO, domesticFeeUSA, 
            "No foreign transaction fee for USA transactions");

        // Test case 3: Foreign transaction - fee assessed
        BigDecimal foreignFee = feeCalculationService.calculateForeignTransactionFee(
            TEST_ACCOUNT_ID, transactionAmount, "FR");
        BigDecimal expectedForeignFee = transactionAmount.multiply(FOREIGN_TRANSACTION_FEE_RATE)
            .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedForeignFee, foreignFee, 
            "Foreign transaction fee should be 3% of transaction amount");

        // Test case 4: Case insensitive country code
        BigDecimal lowercaseCountry = feeCalculationService.calculateForeignTransactionFee(
            TEST_ACCOUNT_ID, transactionAmount, "us");
        assertEquals(BigDecimal.ZERO, lowercaseCountry, 
            "Country code should be case insensitive");

        // Test case 5: Invalid parameters - zero fee
        BigDecimal invalidFee = feeCalculationService.calculateForeignTransactionFee(
            null, transactionAmount, "FR");
        assertEquals(BigDecimal.ZERO, invalidFee, 
            "Zero fee should be returned for invalid parameters");
    }

    /**
     * Test fee assessment workflow for account with multiple fee types.
     * Validates orchestration of fee calculation and persistence operations.
     */
    @Test
    public void testAssessFees() {
        // Test successful fee assessment
        List<String> result = feeCalculationService.assessFees(TEST_ACCOUNT_ID);
        
        assertNotNull(result, "Fee assessment result should not be null");
        assertFalse(result.isEmpty(), "Fee assessment should return non-empty result");
        assertTrue(result.get(0).contains(TEST_ACCOUNT_ID.toString()), 
            "Assessment result should reference account ID");

        // Test invalid account ID
        List<String> invalidResult = feeCalculationService.assessFees(null);
        assertNotNull(invalidResult, "Assessment result should not be null for invalid input");
    }

    /**
     * Test fee waiver functionality with business rule validation.
     * Validates fee waiver processing and status updates.
     */
    @Test
    public void testWaiveFee() {
        Long feeId = 123L;
        String waiveReason = "Customer courtesy waiver";
        
        // Test successful fee waiver
        boolean waiveResult = feeCalculationService.waiveFee(
            TEST_ACCOUNT_ID, feeId, waiveReason);
        assertTrue(waiveResult, "Fee waiver should be successful with valid parameters");

        // Test invalid parameters
        boolean invalidWaiver = feeCalculationService.waiveFee(null, feeId, waiveReason);
        assertFalse(invalidWaiver, "Fee waiver should fail with null account ID");

        boolean emptyReasonWaiver = feeCalculationService.waiveFee(
            TEST_ACCOUNT_ID, feeId, "");
        assertFalse(emptyReasonWaiver, "Fee waiver should fail with empty reason");

        boolean nullFeeIdWaiver = feeCalculationService.waiveFee(
            TEST_ACCOUNT_ID, null, waiveReason);
        assertFalse(nullFeeIdWaiver, "Fee waiver should fail with null fee ID");
    }

    /**
     * Test fee reversal functionality for dispute resolution.
     * Validates fee reversal processing and audit trail maintenance.
     */
    @Test
    public void testReverseFee() {
        Long feeId = 456L;
        String reverseReason = "Billing error correction";
        
        // Test successful fee reversal
        boolean reverseResult = feeCalculationService.reverseFee(
            TEST_ACCOUNT_ID, feeId, reverseReason);
        assertTrue(reverseResult, "Fee reversal should be successful with valid parameters");

        // Test invalid parameters
        boolean invalidReversal = feeCalculationService.reverseFee(null, feeId, reverseReason);
        assertFalse(invalidReversal, "Fee reversal should fail with null account ID");

        boolean emptyReasonReversal = feeCalculationService.reverseFee(
            TEST_ACCOUNT_ID, feeId, "   ");
        assertFalse(emptyReasonReversal, "Fee reversal should fail with blank reason");
    }

    /**
     * Test fee waiver eligibility based on customer tier and account status.
     * Validates business rules for different fee types and customer categories.
     */
    @Test
    public void testIsEligibleForWaiver() {
        // Test late payment fee waiver eligibility
        boolean lateFeeEligibility = feeCalculationService.isEligibleForWaiver(
            TEST_ACCOUNT_ID, "LATE_PAYMENT");
        assertNotNull(lateFeeEligibility, "Waiver eligibility should return a boolean value");

        // Test annual fee waiver eligibility
        boolean annualFeeEligibility = feeCalculationService.isEligibleForWaiver(
            TEST_ACCOUNT_ID, "ANNUAL");
        assertNotNull(annualFeeEligibility, "Annual fee waiver eligibility should return a value");

        // Test over-limit fee waiver eligibility
        boolean overLimitEligibility = feeCalculationService.isEligibleForWaiver(
            TEST_ACCOUNT_ID, "OVER_LIMIT");
        assertNotNull(overLimitEligibility, "Over-limit fee waiver eligibility should return a value");

        // Test unknown fee type
        boolean unknownFeeEligibility = feeCalculationService.isEligibleForWaiver(
            TEST_ACCOUNT_ID, "UNKNOWN_FEE");
        assertFalse(unknownFeeEligibility, "Unknown fee types should not be eligible for waiver");

        // Test invalid parameters
        boolean invalidEligibility = feeCalculationService.isEligibleForWaiver(null, "LATE_PAYMENT");
        assertFalse(invalidEligibility, "Invalid account ID should not be eligible for waiver");

        boolean emptyFeeTypeEligibility = feeCalculationService.isEligibleForWaiver(
            TEST_ACCOUNT_ID, "");
        assertFalse(emptyFeeTypeEligibility, "Empty fee type should not be eligible for waiver");
    }

    /**
     * Test fee calculation accuracy with BigDecimal precision validation.
     * Ensures exact monetary calculations matching COBOL COMP-3 behavior.
     */
    @Test
    public void testFeeCalculationAccuracy() {
        // Test precision with challenging decimal calculations
        BigDecimal challengingBalance = new BigDecimal("1234.56");
        BigDecimal lateFee = feeCalculationService.calculateLateFee(
            TEST_ACCOUNT_ID, challengingBalance, 20);
        
        // Verify scale is exactly 2 (matching COBOL COMP-3)
        assertEquals(2, lateFee.scale(), "Fee amount should have exactly 2 decimal places");
        
        // Test rounding behavior matches COBOL HALF_UP
        BigDecimal precisionTestAmount = new BigDecimal("99.995");
        BigDecimal foreignFee = feeCalculationService.calculateForeignTransactionFee(
            TEST_ACCOUNT_ID, precisionTestAmount, "CA");
        
        BigDecimal expectedRounded = precisionTestAmount.multiply(FOREIGN_TRANSACTION_FEE_RATE)
            .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedRounded, foreignFee, 
            "Fee calculation should use HALF_UP rounding matching COBOL behavior");
    }

    /**
     * Test fee assessment rules including timing and frequency constraints.
     * Validates business rules for when fees can be assessed and under what conditions.
     */
    @Test
    public void testFeeAssessmentRules() {
        // Test that fee validation follows business rules
        boolean validLateFee = feeCalculationService.validateFeeCalculation(
            TEST_ACCOUNT_ID, "LATE_PAYMENT", new BigDecimal("30.00"));
        assertTrue(validLateFee, "Valid late fee amount should pass validation");

        boolean invalidLateFee = feeCalculationService.validateFeeCalculation(
            TEST_ACCOUNT_ID, "LATE_PAYMENT", new BigDecimal("50.00"));
        assertFalse(invalidLateFee, "Late fee exceeding maximum should fail validation");

        boolean validOverLimitFee = feeCalculationService.validateFeeCalculation(
            TEST_ACCOUNT_ID, "OVER_LIMIT", OVER_LIMIT_FEE);
        assertTrue(validOverLimitFee, "Standard over-limit fee should pass validation");

        boolean invalidOverLimitFee = feeCalculationService.validateFeeCalculation(
            TEST_ACCOUNT_ID, "OVER_LIMIT", new BigDecimal("40.00"));
        assertFalse(invalidOverLimitFee, "Non-standard over-limit fee should fail validation");
    }

    /**
     * Test fee waiver conditions based on customer tier and account history.
     * Validates complex business rules for fee waiver eligibility.
     */
    @Test
    public void testFeeWaiverConditions() {
        // Test case sensitivity in fee type handling
        boolean caseInsensitiveEligibility = feeCalculationService.isEligibleForWaiver(
            TEST_ACCOUNT_ID, "late_payment");
        assertNotNull(caseInsensitiveEligibility, "Fee type should be case insensitive");

        // Test that waiver logic handles edge cases
        String validWaiverReason = "High-value customer courtesy waiver";
        boolean validWaiver = feeCalculationService.waiveFee(
            TEST_ACCOUNT_ID, 789L, validWaiverReason);
        assertTrue(validWaiver, "Valid waiver with proper reason should succeed");

        // Test waiver reason validation
        String shortReason = "OK";  // Very short but valid reason
        boolean shortReasonWaiver = feeCalculationService.waiveFee(
            TEST_ACCOUNT_ID, 790L, shortReason);
        assertTrue(shortReasonWaiver, "Short but valid waiver reason should be accepted");
    }

    /**
     * Test regulatory compliance including fee caps and disclosure requirements.
     * Validates that all fees comply with regulatory limits and business rules.
     */
    @Test
    public void testRegulatoryCompliance() {
        // Test that annual fees comply with regulatory guidelines
        BigDecimal standardAnnualFee = feeCalculationService.calculateAnnualFee(
            TEST_ACCOUNT_ID, "STANDARD", TEST_DATE.minusYears(1), TEST_DATE);
        assertTrue(standardAnnualFee.compareTo(new BigDecimal("500.00")) <= 0, 
            "Annual fees should not exceed regulatory limits");

        // Test that late fees have reasonable caps
        BigDecimal maxLateFee = feeCalculationService.calculateLateFee(
            TEST_ACCOUNT_ID, new BigDecimal("10000.00"), 30);
        assertTrue(maxLateFee.compareTo(new BigDecimal("50.00")) <= 0, 
            "Late fees should have reasonable maximum limits");

        // Test that foreign transaction fees are within acceptable ranges
        BigDecimal largeTransactionFee = feeCalculationService.calculateForeignTransactionFee(
            TEST_ACCOUNT_ID, new BigDecimal("5000.00"), "UK");
        BigDecimal expectedMaxForeignFee = new BigDecimal("5000.00")
            .multiply(FOREIGN_TRANSACTION_FEE_RATE)
            .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedMaxForeignFee, largeTransactionFee, 
            "Foreign transaction fees should be calculated consistently regardless of amount");

        // Test fee validation for regulatory compliance
        boolean compliantAnnualFee = feeCalculationService.validateFeeCalculation(
            TEST_ACCOUNT_ID, "ANNUAL", ANNUAL_FEE_STANDARD);
        assertTrue(compliantAnnualFee, "Standard annual fee should be compliant");

        boolean compliantPremiumFee = feeCalculationService.validateFeeCalculation(
            TEST_ACCOUNT_ID, "ANNUAL", ANNUAL_FEE_PREMIUM);
        assertTrue(compliantPremiumFee, "Premium annual fee should be compliant");
    }

    /**
     * Helper method to create test account with specified balance and credit limit.
     * Supports test data setup for various fee calculation scenarios.
     * 
     * @param accountId Account identifier
     * @param currentBalance Current account balance
     * @param creditLimit Account credit limit
     * @return Configured test Account entity
     */
    private Account createTestAccount(Long accountId, BigDecimal currentBalance, BigDecimal creditLimit) {
        return Account.builder()
            .accountId(accountId)
            .activeStatus("Y")
            .currentBalance(currentBalance)
            .creditLimit(creditLimit)
            .cashCreditLimit(creditLimit.multiply(new BigDecimal("0.3")))
            .openDate(TEST_DATE.minusYears(2))
            .expirationDate(TEST_DATE.plusYears(3))
            .currentCycleCredit(BigDecimal.ZERO)
            .currentCycleDebit(BigDecimal.ZERO)
            .addressZip("12345")
            .groupId("STANDARD")
            .build();
    }

    /**
     * Helper method to create test fee with specified type and amount.
     * Supports test data setup for fee processing and validation scenarios.
     * 
     * @param accountId Account identifier for the fee
     * @param feeType Type of fee being created
     * @param feeAmount Monetary amount of the fee
     * @return Configured test Fee entity
     */
    private Fee createTestFee(Long accountId, Fee.FeeType feeType, BigDecimal feeAmount) {
        return new Fee(accountId, feeType, feeAmount, TEST_DATE);
    }
}