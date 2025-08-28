/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.integration;

import com.carddemo.service.InterestCalculationBatchService;
import com.carddemo.service.AccountUpdateService;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration test class specifically validating financial calculation parity between COBOL COMP-3 
 * and Java BigDecimal implementations. Tests interest calculations, balance updates, and monetary 
 * precision to ensure penny-level accuracy matching COBOL implementations.
 * 
 * This comprehensive test suite validates the core requirement of maintaining 100% functional 
 * parity with the original COBOL system, particularly focusing on the critical financial 
 * calculations performed by CBACT04C.cbl (Interest Calculation Batch) and related programs.
 * 
 * Test Coverage:
 * - Interest calculation algorithm validation against COBOL reference data
 * - BigDecimal scale and RoundingMode.HALF_UP configuration testing  
 * - Balance update calculations with transaction posting precision
 * - Credit limit and available credit calculations
 * - Currency conversion and rounding scenarios
 * - Zero deviation requirement validation for financial calculations
 * 
 * COBOL Program References:
 * - CBACT04C.cbl: Interest calculation batch processing logic
 * - CVACT01Y.cpy: Account record structure with COMP-3 monetary fields
 * - CVTRA02Y.cpy: Interest rate structure and disclosure group data
 * - CVTRA05Y.cpy: Transaction record structure for posting operations
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FinancialCalculationParityTest extends BaseIntegrationTest {

    @Autowired
    private InterestCalculationBatchService interestCalculationBatchService;

    @Autowired
    private AccountUpdateService accountUpdateService;

    // Test constants for COBOL precision validation
    private static final int COBOL_MONETARY_SCALE = 2;
    private static final int COBOL_INTEREST_RATE_SCALE = 4;  
    private static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal INTEREST_RATE_DIVISOR = new BigDecimal("1200");
    
    // Test data for financial calculation validation
    private Account testAccount;
    private Transaction testTransaction;
    private List<BigDecimal> cobolReferenceResults;
    private Map<String, BigDecimal> testInterestRates;

    /**
     * Sets up test environment and initializes test data before each test execution.
     * Creates test containers, configures test accounts with COBOL-compatible precision,
     * and loads reference calculation results from COBOL implementation.
     */
    @BeforeEach
    public void setupTestData() {
        // Call parent setup for container initialization
        setupTestContainers();
        
        // Create test account with COBOL-compatible monetary precision
        testAccount = createTestAccount();
        
        // Create test transaction for balance update testing
        testTransaction = createTestTransaction();
        
        // Initialize COBOL reference calculation results for validation
        initializeCobolReferenceResults();
        
        // Setup test interest rates matching disclosure group data
        initializeTestInterestRates();
        
        logger.info("Financial calculation parity test setup completed");
    }

    /**
     * Tests interest calculation parity between Java and COBOL implementations.
     * Validates that the Java BigDecimal implementation produces identical results
     * to the original COBOL COMP-3 calculations from CBACT04C.cbl.
     */
    @Test
    @Order(1)
    @DisplayName("Test Interest Calculation Parity with COBOL Reference Implementation")
    public void testInterestCalculationParity() {
        logger.info("Starting interest calculation parity test");
        
        // Test data from COBOL reference calculations
        BigDecimal testBalance = new BigDecimal("1500.00").setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
        BigDecimal testInterestRate = new BigDecimal("18.50").setScale(COBOL_INTEREST_RATE_SCALE, COBOL_ROUNDING_MODE);
        
        // Expected result from COBOL calculation: (1500.00 * 18.50) / 1200 = 23.125 -> 23.13 (HALF_UP)
        BigDecimal expectedCobolResult = new BigDecimal("23.13").setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
        
        // Execute Java calculation using InterestCalculationBatchService
        BigDecimal actualJavaResult = interestCalculationBatchService.calculateMonthlyInterest(testBalance, testInterestRate);
        
        // Validate exact precision match
        assertBigDecimalEquals(expectedCobolResult, actualJavaResult);
        validateCobolPrecision(actualJavaResult);
        
        // Additional precision validation
        assertThat(actualJavaResult.scale()).isEqualTo(COBOL_MONETARY_SCALE);
        assertThat(actualJavaResult.stripTrailingZeros().scale()).isLessThanOrEqualTo(COBOL_MONETARY_SCALE);
        
        logger.info("Interest calculation parity test passed: {} == {}", expectedCobolResult, actualJavaResult);
    }

    /**
     * Tests BigDecimal scale configuration to ensure all monetary calculations
     * maintain exactly 2 decimal places matching COBOL COMP-3 requirements.
     */
    @Test
    @Order(2) 
    @DisplayName("Test BigDecimal Scale Configuration for COBOL COMP-3 Compliance")
    public void testBigDecimalScaleConfiguration() {
        logger.info("Starting BigDecimal scale configuration test");
        
        // Test various monetary values for consistent scale behavior
        List<BigDecimal> testValues = List.of(
            new BigDecimal("100"),
            new BigDecimal("100.5"),  
            new BigDecimal("100.50"),
            new BigDecimal("100.555"), // Should round to 100.56 with HALF_UP
            new BigDecimal("100.554")  // Should round to 100.55 with HALF_UP
        );
        
        for (BigDecimal testValue : testValues) {
            BigDecimal normalized = CobolDataConverter.createBigDecimalWithCobolPrecision(testValue);
            
            // Validate scale is exactly 2 (COBOL COMP-3 monetary standard)
            assertThat(normalized.scale()).isEqualTo(COBOL_MONETARY_SCALE);
            
            // Validate value maintains proper precision
            validateCobolPrecision(normalized);
            
            // Validate rounding mode matches COBOL HALF_UP behavior
            if (testValue.equals(new BigDecimal("100.555"))) {
                assertThat(normalized).isEqualTo(new BigDecimal("100.56"));
            }
            if (testValue.equals(new BigDecimal("100.554"))) {
                assertThat(normalized).isEqualTo(new BigDecimal("100.55"));
            }
        }
        
        logger.info("BigDecimal scale configuration test passed");
    }

    /**
     * Tests RoundingMode.HALF_UP configuration to ensure Java calculations
     * exactly match COBOL ROUNDED clause behavior for financial precision.
     */
    @Test
    @Order(3)
    @DisplayName("Test RoundingMode.HALF_UP Configuration Matching COBOL ROUNDED Behavior")
    public void testRoundingModeHalfUp() {
        logger.info("Starting RoundingMode.HALF_UP configuration test");
        
        // Test cases that specifically validate HALF_UP rounding behavior
        Map<BigDecimal, BigDecimal> roundingTestCases = Map.of(
            new BigDecimal("10.125"), new BigDecimal("10.13"),  // .125 rounds UP to .13
            new BigDecimal("10.124"), new BigDecimal("10.12"),  // .124 rounds DOWN to .12  
            new BigDecimal("10.115"), new BigDecimal("10.12"),  // .115 rounds UP to .12
            new BigDecimal("10.114"), new BigDecimal("10.11"),  // .114 rounds DOWN to .11
            new BigDecimal("10.005"), new BigDecimal("10.01")   // .005 rounds UP to .01
        );
        
        for (Map.Entry<BigDecimal, BigDecimal> testCase : roundingTestCases.entrySet()) {
            BigDecimal input = testCase.getKey();
            BigDecimal expectedOutput = testCase.getValue();
            
            // Use CobolDataConverter to apply COBOL rounding behavior
            BigDecimal actualOutput = input.setScale(COBOL_MONETARY_SCALE, CobolDataConverter.getRoundingModeForCobol());
            
            assertBigDecimalEquals(expectedOutput, actualOutput);
            validateCobolPrecision(actualOutput);
        }
        
        logger.info("RoundingMode.HALF_UP configuration test passed");
    }

    /**
     * Tests balance update calculations with transaction posting to ensure
     * account balance modifications maintain exact COBOL precision.
     */
    @Test
    @Order(4)
    @DisplayName("Test Balance Update Calculations with Transaction Posting Precision")
    public void testBalanceUpdateCalculations() {
        logger.info("Starting balance update calculations test");
        
        // Initial account balance with COBOL precision
        BigDecimal initialBalance = new BigDecimal("1500.00").setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
        testAccount.setCurrentBalance(initialBalance);
        
        // Transaction amounts to post (mix of credits and debits)
        List<BigDecimal> transactionAmounts = List.of(
            new BigDecimal("150.25"),   // Credit transaction
            new BigDecimal("-75.50"),   // Debit transaction  
            new BigDecimal("0.01"),     // Minimum precision test
            new BigDecimal("-299.99")   // Large debit transaction
        );
        
        BigDecimal runningBalance = initialBalance;
        
        for (BigDecimal transactionAmount : transactionAmounts) {
            // Calculate expected balance using COBOL precision
            BigDecimal expectedBalance = runningBalance.add(transactionAmount)
                    .setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
            
            // Update balance using CobolDataConverter precision handling
            runningBalance = CobolDataConverter.createBigDecimalWithCobolPrecision(
                    runningBalance.add(transactionAmount));
            
            // Validate precision and accuracy
            assertBigDecimalEquals(expectedBalance, runningBalance);
            validateCobolPrecision(runningBalance);
            
            // Ensure no precision loss during calculation
            assertThat(runningBalance.scale()).isEqualTo(COBOL_MONETARY_SCALE);
        }
        
        // Final balance should match cumulative calculation
        BigDecimal finalExpected = new BigDecimal("1274.77").setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
        assertBigDecimalEquals(finalExpected, runningBalance);
        
        logger.info("Balance update calculations test passed: final balance = {}", runningBalance);
    }

    /**
     * Tests transaction posting precision to ensure individual transaction
     * amounts maintain exact monetary precision throughout processing.
     */
    @Test
    @Order(5)
    @DisplayName("Test Transaction Posting Precision for Monetary Accuracy")
    public void testTransactionPostingPrecision() {
        logger.info("Starting transaction posting precision test");
        
        // Test transaction amounts with various precision challenges
        List<BigDecimal> testAmounts = List.of(
            new BigDecimal("100.555"),  // Rounding required
            new BigDecimal("50.125"),   // HALF_UP rounding test
            new BigDecimal("25.004"),   // Near-zero precision
            new BigDecimal("0.99"),     // Less than $1 transaction
            new BigDecimal("1000000.01") // Large amount with precision
        );
        
        for (BigDecimal originalAmount : testAmounts) {
            // Process transaction amount through COBOL precision converter
            BigDecimal processedAmount = CobolDataConverter.createBigDecimalWithCobolPrecision(originalAmount);
            
            // Validate precision maintenance
            validateCobolPrecision(processedAmount);
            
            // Create transaction with processed amount
            Transaction precisionTransaction = createTestTransaction();
            precisionTransaction.setAmount(processedAmount);
            
            // Validate transaction amount precision
            assertThat(precisionTransaction.getAmount().scale()).isEqualTo(COBOL_MONETARY_SCALE);
            validateCobolPrecision(precisionTransaction.getAmount());
            
            // Validate rounding behavior matches COBOL expectations
            if (originalAmount.equals(new BigDecimal("100.555"))) {
                assertThat(processedAmount).isEqualTo(new BigDecimal("100.56"));
            }
            if (originalAmount.equals(new BigDecimal("50.125"))) {
                assertThat(processedAmount).isEqualTo(new BigDecimal("50.13"));
            }
        }
        
        logger.info("Transaction posting precision test passed");
    }

    /**
     * Tests credit limit and available credit calculations to ensure
     * credit management logic maintains COBOL precision standards.
     */
    @Test
    @Order(6)
    @DisplayName("Test Credit Limit and Available Credit Calculations")
    public void testCreditLimitCalculations() {
        logger.info("Starting credit limit calculations test");
        
        // Setup test account with credit information
        BigDecimal creditLimit = new BigDecimal("5000.00").setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
        BigDecimal currentBalance = new BigDecimal("1500.75").setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
        BigDecimal pendingCharges = new BigDecimal("250.25").setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
        
        // Calculate available credit using COBOL precision
        BigDecimal availableCredit = creditLimit.subtract(currentBalance).subtract(pendingCharges);
        availableCredit = CobolDataConverter.createBigDecimalWithCobolPrecision(availableCredit);
        
        // Expected available credit: 5000.00 - 1500.75 - 250.25 = 3249.00
        BigDecimal expectedAvailableCredit = new BigDecimal("3249.00").setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
        
        assertBigDecimalEquals(expectedAvailableCredit, availableCredit);
        validateCobolPrecision(availableCredit);
        
        // Test credit utilization percentage with precision
        BigDecimal utilizationRate = currentBalance.divide(creditLimit, 4, COBOL_ROUNDING_MODE);
        BigDecimal expectedUtilization = new BigDecimal("0.3002").setScale(4, COBOL_ROUNDING_MODE);
        
        assertThat(utilizationRate).isEqualTo(expectedUtilization);
        
        logger.info("Credit limit calculations test passed: available credit = {}", availableCredit);
    }

    /**
     * Tests currency conversion and rounding scenarios to ensure
     * international transaction processing maintains precision.
     */
    @Test
    @Order(7)
    @DisplayName("Test Currency Conversion and Rounding Scenarios")
    public void testCurrencyConversionRounding() {
        logger.info("Starting currency conversion rounding test");
        
        // Test currency conversion rates and amounts
        Map<BigDecimal, BigDecimal> conversionTests = Map.of(
            new BigDecimal("100.00"), new BigDecimal("1.2345"),  // USD to EUR
            new BigDecimal("250.75"), new BigDecimal("0.8567"),  // EUR to USD
            new BigDecimal("50.33"), new BigDecimal("1.4532")    // USD to GBP
        );
        
        for (Map.Entry<BigDecimal, BigDecimal> conversion : conversionTests.entrySet()) {
            BigDecimal originalAmount = conversion.getKey();
            BigDecimal exchangeRate = conversion.getValue();
            
            // Perform currency conversion with COBOL precision
            BigDecimal convertedAmount = originalAmount.multiply(exchangeRate);
            convertedAmount = CobolDataConverter.createBigDecimalWithCobolPrecision(convertedAmount);
            
            // Validate precision maintenance after conversion
            validateCobolPrecision(convertedAmount);
            assertThat(convertedAmount.scale()).isEqualTo(COBOL_MONETARY_SCALE);
            
            // Validate rounding behavior
            if (originalAmount.equals(new BigDecimal("100.00")) && 
                exchangeRate.equals(new BigDecimal("1.2345"))) {
                // 100.00 * 1.2345 = 123.45 (no rounding needed)
                assertThat(convertedAmount).isEqualTo(new BigDecimal("123.45"));
            }
        }
        
        logger.info("Currency conversion rounding test passed");
    }

    /**
     * Tests comparison of Java calculation outputs with COBOL reference implementations
     * to ensure zero deviation in financial calculations.
     */
    @Test
    @Order(8)
    @DisplayName("Test Cobol-Java Output Comparison for Zero Deviation")
    public void testCobolJavaOutputComparison() {
        logger.info("Starting COBOL-Java output comparison test");
        
        // Execute parallel calculations using both reference data and Java implementation
        for (int i = 0; i < cobolReferenceResults.size(); i++) {
            BigDecimal cobolResult = cobolReferenceResults.get(i);
            
            // Calculate corresponding result using Java implementation
            BigDecimal testBalance = new BigDecimal((i + 1) * 1000 + ".00").setScale(COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
            BigDecimal testRate = testInterestRates.get("DEFAULT");
            
            BigDecimal javaResult = interestCalculationBatchService.calculateMonthlyInterest(testBalance, testRate);
            
            // Validate exact match with zero deviation
            assertBigDecimalEquals(cobolResult, javaResult);
            
            // Additional precision validation
            validateCobolPrecision(javaResult);
            assertThat(javaResult.subtract(cobolResult)).isEqualTo(BigDecimal.ZERO.setScale(COBOL_MONETARY_SCALE));
        }
        
        logger.info("COBOL-Java output comparison test passed with zero deviation");
    }

    /**
     * Final validation test ensuring zero deviation requirement is met
     * across all financial calculation scenarios.
     */
    @Test
    @Order(9)
    @DisplayName("Validate Zero Deviation Requirement for All Financial Calculations")
    public void validateZeroDeviationRequirement() {
        logger.info("Starting zero deviation requirement validation");
        
        // Comprehensive test of all financial calculation paths
        List<BigDecimal> testBalances = List.of(
            new BigDecimal("100.00"),
            new BigDecimal("1000.50"), 
            new BigDecimal("5000.75"),
            new BigDecimal("10000.25")
        );
        
        List<BigDecimal> testRates = List.of(
            new BigDecimal("12.50"),
            new BigDecimal("18.25"),
            new BigDecimal("24.75")
        );
        
        for (BigDecimal balance : testBalances) {
            for (BigDecimal rate : testRates) {
                // Manual calculation using exact COBOL formula
                BigDecimal expectedResult = balance.multiply(rate)
                        .divide(INTEREST_RATE_DIVISOR, COBOL_MONETARY_SCALE, COBOL_ROUNDING_MODE);
                
                // Service calculation
                BigDecimal actualResult = interestCalculationBatchService.calculateMonthlyInterest(balance, rate);
                
                // Validate zero deviation
                BigDecimal deviation = actualResult.subtract(expectedResult).abs();
                assertThat(deviation).isEqualTo(BigDecimal.ZERO.setScale(COBOL_MONETARY_SCALE));
                
                // Validate precision
                validateCobolPrecision(actualResult);
                assertBigDecimalEquals(expectedResult, actualResult);
            }
        }
        
        logger.info("Zero deviation requirement validation passed");
    }

    /**
     * Cleanup method to remove test data and reset test environment
     * after each test execution to ensure test isolation.
     */
    public void cleanupTestData() {
        // Clear test data
        testAccount = null;
        testTransaction = null;
        cobolReferenceResults = null;
        testInterestRates = null;
        
        // Call parent cleanup
        super.cleanupTestData();
        
        logger.info("Financial calculation parity test cleanup completed");
    }

    // Private helper methods

    /**
     * Initializes COBOL reference calculation results for validation testing.
     * These results represent the expected outputs from the original CBACT04C.cbl
     * interest calculation program for various test scenarios.
     */
    private void initializeCobolReferenceResults() {
        cobolReferenceResults = new ArrayList<>();
        
        // Reference results from COBOL calculations (balance * rate / 1200)
        // Using default test rate of 18.50% APR
        cobolReferenceResults.add(new BigDecimal("15.42"));  // 1000.00 balance
        cobolReferenceResults.add(new BigDecimal("30.83"));  // 2000.00 balance  
        cobolReferenceResults.add(new BigDecimal("46.25"));  // 3000.00 balance
        cobolReferenceResults.add(new BigDecimal("61.67"));  // 4000.00 balance
        cobolReferenceResults.add(new BigDecimal("77.08"));  // 5000.00 balance
        
        logger.debug("Initialized {} COBOL reference results", cobolReferenceResults.size());
    }

    /**
     * Initializes test interest rates matching disclosure group configuration.
     * Rates are configured with proper COBOL precision for testing scenarios.
     */
    private void initializeTestInterestRates() {
        testInterestRates = new HashMap<>();
        
        // Interest rates with COBOL precision (4 decimal places for percentage)
        testInterestRates.put("DEFAULT", new BigDecimal("18.5000").setScale(COBOL_INTEREST_RATE_SCALE, COBOL_ROUNDING_MODE));
        testInterestRates.put("PREMIUM", new BigDecimal("14.2500").setScale(COBOL_INTEREST_RATE_SCALE, COBOL_ROUNDING_MODE));
        testInterestRates.put("STANDARD", new BigDecimal("21.7500").setScale(COBOL_INTEREST_RATE_SCALE, COBOL_ROUNDING_MODE));
        
        logger.debug("Initialized {} test interest rates", testInterestRates.size());
    }
}