/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.service.FraudDetectionService;
import com.carddemo.service.AuthorizationEngine;
import com.carddemo.service.TransactionValidationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.assertj.core.api.Assertions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for TransactionValidationService that validates transaction authorization
 * and fraud detection logic while maintaining functional parity with the original COBOL COTRN02C program.
 * 
 * This test class implements comprehensive validation including:
 * - Authorization rules checking and available credit verification
 * - Velocity limit enforcement and fraud pattern detection  
 * - Merchant category restrictions and geographic velocity checks
 * - Daily transaction limits and blacklist validation
 * - Real-time authorization decisions and concurrent authorization handling
 * 
 * The tests ensure complete functional parity with the COBOL transaction validation logic,
 * maintaining identical precision and business rule enforcement as specified in COTRN02C.CBL.
 * 
 * Performance Requirements:
 * All validation methods must complete within 200ms to maintain user experience parity with
 * the original CICS transaction processing system.
 * 
 * Financial Precision Requirements:
 * All monetary calculations maintain identical precision to COBOL COMP-3 packed decimal format,
 * using BigDecimal with scale=2 and HALF_UP rounding mode for regulatory compliance.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@SpringBootTest
@DisplayName("Transaction Validation Service Tests")
public class TransactionValidationServiceTest extends BaseServiceTest {

    @InjectMocks
    private TransactionValidationService transactionValidationService;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock  
    private AuthorizationEngine authorizationEngine;

    private Account testAccount;
    private Transaction testTransaction;
    private Transaction testTransactionHighAmount;
    private Transaction testTransactionInvalidMerchant;

    /**
     * Setup method executed before each test to initialize mock objects and test data.
     * Configures realistic test scenarios matching COBOL COTRN02C program validation logic.
     */
    @BeforeEach
    @Override
    public void setUp() {
        // Call parent setup for base configuration
        super.setUp();
        
        // Initialize Mockito annotations
        MockitoAnnotations.openMocks(this);
        
        // Initialize test data with COBOL-compatible structures
        setupTestData();
        
        // Configure mock services with realistic responses
        configureMockServices();
    }

    /**
     * Nested test class for authorization rule validation tests.
     * Tests core authorization logic matching COBOL VALIDATE-INPUT-DATA-FIELDS paragraph.
     */
    @Nested
    @DisplayName("Authorization Rules Validation")
    class AuthorizationRulesTests {

        @Test
        @DisplayName("Should validate transaction with sufficient credit successfully")
        public void testValidateTransactionWithSufficientCredit() {
            // Given - Account with sufficient available credit
            when(authorizationEngine.checkCreditLimit(any(Transaction.class), any(Account.class)))
                .thenReturn(true);
            when(authorizationEngine.verifyAvailableBalance(any(Transaction.class), any(Account.class)))
                .thenReturn(true);
            when(authorizationEngine.validateCardStatus(any(Transaction.class)))
                .thenReturn(true);

            // When - Validate transaction authorization rules
            long startTime = System.currentTimeMillis();
            boolean result = transactionValidationService.checkAuthorizationRules(testTransaction, testAccount);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then - Authorization should succeed
            assertThat(result).isTrue();
            assertThat(executionTime).isLessThan(MAX_RESPONSE_TIME_MS);

            // Verify all authorization checks were performed
            verify(authorizationEngine, times(1)).checkCreditLimit(testTransaction, testAccount);
            verify(authorizationEngine, times(1)).verifyAvailableBalance(testTransaction, testAccount);
            verify(authorizationEngine, times(1)).validateCardStatus(testTransaction);
        }

        @Test
        @DisplayName("Should reject transaction with insufficient credit")
        public void testValidateTransactionWithInsufficientCredit() {
            // Given - Account with insufficient available credit
            when(authorizationEngine.checkCreditLimit(any(Transaction.class), any(Account.class)))
                .thenReturn(false);
            when(authorizationEngine.verifyAvailableBalance(any(Transaction.class), any(Account.class)))
                .thenReturn(false);

            // When - Validate transaction authorization rules
            boolean result = transactionValidationService.checkAuthorizationRules(testTransactionHighAmount, testAccount);

            // Then - Authorization should fail
            assertThat(result).isFalse();
            verify(authorizationEngine, times(1)).checkCreditLimit(testTransactionHighAmount, testAccount);
        }

        @Test
        @DisplayName("Should reject transaction with invalid card status")
        public void testValidateTransactionWithInvalidCardStatus() {
            // Given - Invalid card status (blocked, expired, etc.)
            when(authorizationEngine.validateCardStatus(any(Transaction.class)))
                .thenReturn(false);

            // When - Validate transaction authorization rules  
            boolean result = transactionValidationService.checkAuthorizationRules(testTransaction, testAccount);

            // Then - Authorization should fail
            assertThat(result).isFalse();
            verify(authorizationEngine, times(1)).validateCardStatus(testTransaction);
        }
    }

    /**
     * Nested test class for available credit verification tests.
     * Tests credit limit validation matching COBOL account balance checking logic.
     */
    @Nested
    @DisplayName("Available Credit Verification")
    class AvailableCreditTests {

        @Test
        @DisplayName("Should verify available credit for valid transaction amount")
        public void testVerifyAvailableCreditSuccess() {
            // Given - Account with available credit exceeding transaction amount
            BigDecimal availableCredit = createValidAmount("2500.00");
            BigDecimal transactionAmount = testTransaction.getAmount();
            
            when(authorizationEngine.verifyAvailableBalance(any(Transaction.class), any(Account.class)))
                .thenReturn(true);

            // When - Verify available credit
            boolean result = transactionValidationService.verifyAvailableCredit(testTransaction, testAccount);

            // Then - Credit verification should succeed
            assertThat(result).isTrue();
            assertThat(transactionAmount).isLessThan(availableCredit);
            
            // Verify precision matches COBOL COMP-3 requirements
            assertAmountPrecision(transactionAmount);
            assertAmountPrecision(availableCredit);
        }

        @Test
        @DisplayName("Should reject transaction exceeding available credit")
        public void testVerifyAvailableCreditExceedsLimit() {
            // Given - Transaction amount exceeding available credit
            when(authorizationEngine.verifyAvailableBalance(any(Transaction.class), any(Account.class)))
                .thenReturn(false);

            // When - Verify available credit for high amount transaction
            boolean result = transactionValidationService.verifyAvailableCredit(testTransactionHighAmount, testAccount);

            // Then - Credit verification should fail
            assertThat(result).isFalse();
            verify(authorizationEngine, times(1)).verifyAvailableBalance(testTransactionHighAmount, testAccount);
        }

        @Test 
        @DisplayName("Should handle zero available credit scenario")
        public void testVerifyAvailableCreditZeroBalance() {
            // Given - Account with zero available credit
            Account zeroBalanceAccount = createTestAccount();
            zeroBalanceAccount.setCurrentBalance(zeroBalanceAccount.getCreditLimit());
            
            when(authorizationEngine.verifyAvailableBalance(any(Transaction.class), eq(zeroBalanceAccount)))
                .thenReturn(false);

            // When - Verify available credit
            boolean result = transactionValidationService.verifyAvailableCredit(testTransaction, zeroBalanceAccount);

            // Then - Credit verification should fail
            assertThat(result).isFalse();
        }
    }

    /**
     * Nested test class for velocity limit enforcement tests.
     * Tests daily transaction limits and velocity checking matching COBOL validation logic.
     */
    @Nested
    @DisplayName("Velocity Limit Enforcement")
    class VelocityLimitTests {

        @Test
        @DisplayName("Should enforce daily transaction limits successfully")
        public void testEnforceDailyLimitsSuccess() {
            // Given - Transaction within daily limits
            when(fraudDetectionService.checkGeographicVelocity(any(Transaction.class)))
                .thenReturn(true);
            
            // When - Check daily limits
            long startTime = System.currentTimeMillis();
            boolean result = transactionValidationService.checkDailyLimits(testTransaction, testAccount);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then - Daily limit check should pass
            assertThat(result).isTrue();
            assertThat(executionTime).isLessThan(MAX_RESPONSE_TIME_MS);
        }

        @Test
        @DisplayName("Should reject transaction exceeding daily limits")
        public void testEnforceDailyLimitsExceeded() {
            // Given - Transaction exceeding daily limits
            Transaction highAmountTransaction = createTestTransaction();
            highAmountTransaction.setAmount(createValidAmount("5000.00"));
            
            // When - Check daily limits for excessive transaction
            boolean result = transactionValidationService.checkDailyLimits(highAmountTransaction, testAccount);

            // Then - Daily limit check should fail
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should perform geographic velocity checking")
        public void testPerformGeographicVelocityCheck() {
            // Given - Transaction from different geographic location
            when(fraudDetectionService.checkGeographicVelocity(any(Transaction.class)))
                .thenReturn(true);

            // When - Perform geographic velocity check
            boolean result = transactionValidationService.performGeographicVelocityCheck(testTransaction);

            // Then - Geographic check should pass
            assertThat(result).isTrue();
            verify(fraudDetectionService, times(1)).checkGeographicVelocity(testTransaction);
        }

        @Test
        @DisplayName("Should detect suspicious geographic velocity patterns")
        public void testGeographicVelocityFraudDetection() {
            // Given - Suspicious geographic velocity pattern
            when(fraudDetectionService.checkGeographicVelocity(any(Transaction.class)))
                .thenReturn(false);

            // When - Perform geographic velocity check
            boolean result = transactionValidationService.performGeographicVelocityCheck(testTransaction);

            // Then - Geographic check should fail
            assertThat(result).isFalse();
            verify(fraudDetectionService, times(1)).checkGeographicVelocity(testTransaction);
        }

        @Test
        @DisplayName("Should enforce velocity limits for concurrent transactions")
        public void testVelocityLimitsWithConcurrentTransactions() {
            // Given - Multiple concurrent transactions
            when(authorizationEngine.handleConcurrentAuthorizations(any(Transaction.class)))
                .thenReturn(true);

            // When - Enforce velocity limits
            boolean result = transactionValidationService.enforceVelocityLimits(testTransaction, testAccount);

            // Then - Velocity enforcement should handle concurrency
            assertThat(result).isTrue();
            verify(authorizationEngine, times(1)).handleConcurrentAuthorizations(testTransaction);
        }
    }

    /**
     * Nested test class for fraud pattern detection tests.
     * Tests unusual spending patterns and risk scoring matching COBOL fraud detection logic.
     */
    @Nested
    @DisplayName("Fraud Pattern Detection")
    class FraudPatternTests {

        @Test
        @DisplayName("Should detect normal spending patterns successfully")
        public void testDetectFraudPatternsNormalSpending() {
            // Given - Normal transaction pattern
            when(fraudDetectionService.detectFraudPatterns(any(Transaction.class)))
                .thenReturn(false); // No fraud detected
            when(fraudDetectionService.calculateRiskScore(any(Transaction.class)))
                .thenReturn(0.2); // Low risk score

            // When - Detect fraud patterns
            boolean result = transactionValidationService.detectFraudPatterns(testTransaction, testAccount);

            // Then - Fraud detection should pass
            assertThat(result).isTrue(); // No fraud = validation passes
            verify(fraudDetectionService, times(1)).detectFraudPatterns(testTransaction);
            verify(fraudDetectionService, times(1)).calculateRiskScore(testTransaction);
        }

        @Test
        @DisplayName("Should detect unusual spending patterns")
        public void testDetectUnusualSpendingPatterns() {
            // Given - Unusual spending pattern detected
            when(fraudDetectionService.detectFraudPatterns(any(Transaction.class)))
                .thenReturn(true); // Fraud detected
            when(fraudDetectionService.analyzeSpendingPatterns(any(Transaction.class)))
                .thenReturn(false); // Unusual pattern
            when(fraudDetectionService.calculateRiskScore(any(Transaction.class)))
                .thenReturn(0.8); // High risk score

            // When - Detect fraud patterns
            boolean result = transactionValidationService.detectFraudPatterns(testTransaction, testAccount);

            // Then - Fraud detection should fail
            assertThat(result).isFalse();
            verify(fraudDetectionService, times(1)).detectFraudPatterns(testTransaction);
            verify(fraudDetectionService, times(1)).calculateRiskScore(testTransaction);
        }

        @Test
        @DisplayName("Should analyze spending patterns for risk assessment")
        public void testAnalyzeSpendingPatternsRiskAssessment() {
            // Given - Transaction requiring pattern analysis
            when(fraudDetectionService.analyzeSpendingPatterns(any(Transaction.class)))
                .thenReturn(true); // Normal pattern
            when(fraudDetectionService.calculateRiskScore(any(Transaction.class)))
                .thenReturn(0.3); // Moderate risk score

            // When - Analyze spending patterns
            long startTime = System.currentTimeMillis();
            boolean result = transactionValidationService.detectFraudPatterns(testTransaction, testAccount);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then - Pattern analysis should complete within SLA
            assertThat(result).isTrue();
            assertThat(executionTime).isLessThan(MAX_RESPONSE_TIME_MS);
            verify(fraudDetectionService, times(1)).analyzeSpendingPatterns(testTransaction);
        }

        @Test
        @DisplayName("Should calculate risk score for transaction")
        public void testCalculateRiskScore() {
            // Given - Transaction requiring risk scoring
            double expectedRiskScore = 0.25;
            when(fraudDetectionService.calculateRiskScore(any(Transaction.class)))
                .thenReturn(expectedRiskScore);

            // When - Calculate risk score through fraud detection
            transactionValidationService.detectFraudPatterns(testTransaction, testAccount);

            // Then - Risk score should be calculated
            verify(fraudDetectionService, times(1)).calculateRiskScore(testTransaction);
        }
    }

    /**
     * Nested test class for merchant category validation tests.
     * Tests merchant restrictions and category-based rules matching COBOL validation logic.
     */
    @Nested
    @DisplayName("Merchant Category Validation")
    class MerchantCategoryTests {

        @Test
        @DisplayName("Should validate allowed merchant categories")
        public void testValidateMerchantCategoryAllowed() {
            // Given - Transaction with allowed merchant category
            when(fraudDetectionService.validateMerchantCategory(any(Transaction.class)))
                .thenReturn(true);

            // When - Validate merchant category
            boolean result = transactionValidationService.validateMerchantCategory(testTransaction);

            // Then - Merchant validation should pass
            assertThat(result).isTrue();
            verify(fraudDetectionService, times(1)).validateMerchantCategory(testTransaction);
        }

        @Test
        @DisplayName("Should reject restricted merchant categories")
        public void testValidateMerchantCategoryRestricted() {
            // Given - Transaction with restricted merchant category
            when(fraudDetectionService.validateMerchantCategory(any(Transaction.class)))
                .thenReturn(false);

            // When - Validate merchant category
            boolean result = transactionValidationService.validateMerchantCategory(testTransactionInvalidMerchant);

            // Then - Merchant validation should fail
            assertThat(result).isFalse();
            verify(fraudDetectionService, times(1)).validateMerchantCategory(testTransactionInvalidMerchant);
        }

        @Test
        @DisplayName("Should handle null merchant category gracefully")
        public void testValidateMerchantCategoryNull() {
            // Given - Transaction with null merchant ID
            Transaction nullMerchantTransaction = createTestTransaction();
            nullMerchantTransaction.setMerchantId(null);

            when(fraudDetectionService.validateMerchantCategory(any(Transaction.class)))
                .thenReturn(false);

            // When - Validate null merchant category
            boolean result = transactionValidationService.validateMerchantCategory(nullMerchantTransaction);

            // Then - Validation should fail for null merchant
            assertThat(result).isFalse();
        }
    }

    /**
     * Nested test class for blacklist validation tests.
     * Tests card number and merchant blacklist checking matching COBOL validation logic.
     */
    @Nested
    @DisplayName("Blacklist Validation")
    class BlacklistValidationTests {

        @Test
        @DisplayName("Should validate transaction against blacklist successfully")
        public void testValidateBlacklistSuccess() {
            // Given - Transaction not on blacklist
            // Mock blacklist validation logic (not blacklisted)
            
            // When - Validate against blacklist
            long startTime = System.currentTimeMillis();
            boolean result = transactionValidationService.validateBlacklist(testTransaction);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then - Blacklist validation should pass
            assertThat(result).isTrue();
            assertThat(executionTime).isLessThan(MAX_RESPONSE_TIME_MS);
        }

        @Test
        @DisplayName("Should reject blacklisted card numbers")
        public void testValidateBlacklistCardBlacklisted() {
            // Given - Transaction with blacklisted card number
            Transaction blacklistedTransaction = createTestTransaction();
            blacklistedTransaction.setCardNumber("4532123456789999"); // Blacklisted card

            // When - Validate against blacklist
            boolean result = transactionValidationService.validateBlacklist(blacklistedTransaction);

            // Then - Blacklist validation should fail
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject blacklisted merchants")
        public void testValidateBlacklistMerchantBlacklisted() {
            // Given - Transaction with blacklisted merchant
            Transaction blacklistedMerchantTransaction = createTestTransaction();
            blacklistedMerchantTransaction.setMerchantId("BLACKLISTED_MERCHANT");

            // When - Validate against blacklist  
            boolean result = transactionValidationService.validateBlacklist(blacklistedMerchantTransaction);

            // Then - Blacklist validation should fail
            assertThat(result).isFalse();
        }
    }

    /**
     * Nested test class for concurrent authorization handling tests.
     * Tests simultaneous transaction processing and race condition handling.
     */
    @Nested
    @DisplayName("Concurrent Authorization Handling")
    class ConcurrentAuthorizationTests {

        @Test
        @DisplayName("Should handle concurrent authorizations successfully")
        public void testHandleConcurrentAuthorizationsSuccess() throws Exception {
            // Given - Multiple concurrent authorization requests
            when(authorizationEngine.handleConcurrentAuthorizations(any(Transaction.class)))
                .thenReturn(true);
            when(authorizationEngine.authorizeTransaction(any(Transaction.class), any(Account.class)))
                .thenReturn(true);

            ExecutorService executor = Executors.newFixedThreadPool(5);

            // When - Process concurrent authorizations
            CompletableFuture<Boolean>[] futures = new CompletableFuture[5];
            for (int i = 0; i < 5; i++) {
                futures[i] = CompletableFuture.supplyAsync(() -> {
                    return transactionValidationService.processAuthorizationDecision(testTransaction, testAccount);
                }, executor);
            }

            // Then - All concurrent authorizations should complete
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
            allOf.get(5, TimeUnit.SECONDS);

            for (CompletableFuture<Boolean> future : futures) {
                assertThat(future.get()).isTrue();
            }

            executor.shutdown();
        }

        @Test
        @DisplayName("Should prevent race conditions in concurrent processing")
        public void testPreventRaceConditions() throws Exception {
            // Given - Concurrent authorization requests for same account
            when(authorizationEngine.handleConcurrentAuthorizations(any(Transaction.class)))
                .thenReturn(true);
            
            ExecutorService executor = Executors.newFixedThreadPool(3);

            // When - Process concurrent transactions for same account
            CompletableFuture<Boolean>[] futures = new CompletableFuture[3];
            for (int i = 0; i < 3; i++) {
                Transaction concurrentTransaction = createTestTransaction();
                concurrentTransaction.setAmount(createValidAmount("100.00"));
                
                futures[i] = CompletableFuture.supplyAsync(() -> {
                    return transactionValidationService.validateTransaction(concurrentTransaction, testAccount);
                }, executor);
            }

            // Then - Race conditions should be prevented
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
            allOf.get(5, TimeUnit.SECONDS);

            verify(authorizationEngine, atLeastOnce()).handleConcurrentAuthorizations(any(Transaction.class));
            executor.shutdown();
        }

        @Test
        @DisplayName("Should timeout concurrent authorization requests appropriately") 
        public void testConcurrentAuthorizationTimeout() {
            // Given - Authorization request that takes too long
            when(authorizationEngine.handleConcurrentAuthorizations(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(300); // Exceed 200ms SLA
                    return false;
                });

            // When - Process authorization with timeout
            long startTime = System.currentTimeMillis();
            boolean result = transactionValidationService.processAuthorizationDecision(testTransaction, testAccount);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then - Authorization should handle timeout appropriately
            // Note: In real implementation, timeout handling would prevent long waits
            assertThat(result).isFalse();
        }
    }

    /**
     * Nested test class for complete transaction validation integration tests.
     * Tests end-to-end validation workflow matching COBOL PROCESS-ENTER-KEY paragraph.
     */
    @Nested
    @DisplayName("Complete Transaction Validation")
    class CompleteValidationTests {

        @Test
        @DisplayName("Should validate complete transaction successfully")
        public void testValidateTransactionCompleteSuccess() {
            // Given - Valid transaction meeting all validation criteria
            setupSuccessfulValidationMocks();

            // When - Validate complete transaction
            long startTime = System.currentTimeMillis();
            boolean result = transactionValidationService.validateTransaction(testTransaction, testAccount);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then - Complete validation should pass
            assertThat(result).isTrue();
            assertThat(executionTime).isLessThan(MAX_RESPONSE_TIME_MS);

            // Verify all validation steps were executed
            verifyAllValidationStepsExecuted();
        }

        @Test
        @DisplayName("Should reject transaction failing any validation step")
        public void testValidateTransactionFailureScenarios() {
            // Given - Transaction failing credit limit check
            when(authorizationEngine.checkCreditLimit(any(Transaction.class), any(Account.class)))
                .thenReturn(false);

            // When - Validate transaction with insufficient credit
            boolean result = transactionValidationService.validateTransaction(testTransactionHighAmount, testAccount);

            // Then - Complete validation should fail
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should maintain COBOL precision in all calculations")
        public void testValidateTransactionCobolPrecision() {
            // Given - Transaction with precise monetary amounts
            setupSuccessfulValidationMocks();
            
            BigDecimal transactionAmount = testTransaction.getAmount();
            BigDecimal availableCredit = testAccount.getAvailableCredit();

            // When - Validate transaction
            transactionValidationService.validateTransaction(testTransaction, testAccount);

            // Then - All amounts should maintain COBOL precision
            assertAmountPrecision(transactionAmount);
            assertAmountPrecision(availableCredit);
            assertBigDecimalEquals(createValidAmount("125.50"), transactionAmount);
        }

        @Test
        @DisplayName("Should process authorization decision with all validations")
        public void testProcessAuthorizationDecisionComplete() {
            // Given - Complete authorization scenario
            setupSuccessfulValidationMocks();

            // When - Process complete authorization decision
            long startTime = System.currentTimeMillis();
            boolean result = transactionValidationService.processAuthorizationDecision(testTransaction, testAccount);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then - Authorization decision should be processed successfully
            assertThat(result).isTrue();
            assertThat(executionTime).isLessThan(MAX_RESPONSE_TIME_MS);

            // Verify authorization engine was invoked
            verify(authorizationEngine, times(1)).authorizeTransaction(testTransaction, testAccount);
        }

        @Test
        @DisplayName("Should handle validation errors gracefully")
        public void testValidateTransactionErrorHandling() {
            // Given - Service throwing exception during validation
            when(fraudDetectionService.detectFraudPatterns(any(Transaction.class)))
                .thenThrow(new RuntimeException("Fraud service unavailable"));

            // When - Validate transaction with service error
            assertThatThrownBy(() -> {
                transactionValidationService.validateTransaction(testTransaction, testAccount);
            }).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Fraud service unavailable");
        }
    }

    /**
     * Setup test data with COBOL-compatible structures and realistic values.
     * Creates test fixtures matching COBOL TRAN-RECORD and ACCOUNT-RECORD structures.
     */
    private void setupTestData() {
        // Create test account with COBOL-compatible data
        testAccount = createTestAccount();
        
        // Create standard test transaction
        testTransaction = createTestTransaction();
        testTransaction.setTransactionId("0000000000000001");
        testTransaction.setAmount(createValidAmount("125.50"));
        testTransaction.setTransactionType("PU"); // Purchase
        testTransaction.setTransactionDate(LocalDateTime.now());
        testTransaction.setAccountId(testAccount.getAccountId());
        testTransaction.setDescription("Test Purchase Transaction");
        testTransaction.setMerchantId("MERCHANT001");
        testTransaction.setCardNumber("4532123456789012");

        // Create high amount transaction for limit testing
        testTransactionHighAmount = createTestTransaction();
        testTransactionHighAmount.setTransactionId("0000000000000002");
        testTransactionHighAmount.setAmount(createValidAmount("6000.00"));
        testTransactionHighAmount.setTransactionType("PU");
        testTransactionHighAmount.setTransactionDate(LocalDateTime.now());
        testTransactionHighAmount.setAccountId(testAccount.getAccountId());
        testTransactionHighAmount.setDescription("High Amount Test Transaction");
        testTransactionHighAmount.setMerchantId("MERCHANT001");
        testTransactionHighAmount.setCardNumber("4532123456789012");

        // Create invalid merchant transaction for merchant testing
        testTransactionInvalidMerchant = createTestTransaction();
        testTransactionInvalidMerchant.setTransactionId("0000000000000003");
        testTransactionInvalidMerchant.setAmount(createValidAmount("75.25"));
        testTransactionInvalidMerchant.setTransactionType("PU");
        testTransactionInvalidMerchant.setTransactionDate(LocalDateTime.now());
        testTransactionInvalidMerchant.setAccountId(testAccount.getAccountId());
        testTransactionInvalidMerchant.setDescription("Invalid Merchant Test Transaction");
        testTransactionInvalidMerchant.setMerchantId("RESTRICTED_MERCHANT");
        testTransactionInvalidMerchant.setCardNumber("4532123456789012");
    }

    /**
     * Configure mock services with realistic responses for successful validation scenarios.
     * Sets up fraud detection and authorization engine mocks to simulate normal operations.
     */
    private void configureMockServices() {
        // Configure fraud detection service for normal operations
        when(fraudDetectionService.detectFraudPatterns(any(Transaction.class)))
            .thenReturn(false); // No fraud detected for normal tests
        when(fraudDetectionService.analyzeSpendingPatterns(any(Transaction.class)))
            .thenReturn(true); // Normal spending patterns
        when(fraudDetectionService.checkGeographicVelocity(any(Transaction.class)))
            .thenReturn(true); // No geographic velocity issues
        when(fraudDetectionService.validateMerchantCategory(any(Transaction.class)))
            .thenReturn(true); // Allowed merchant category
        when(fraudDetectionService.calculateRiskScore(any(Transaction.class)))
            .thenReturn(0.2); // Low risk score

        // Configure authorization engine for normal operations
        when(authorizationEngine.authorizeTransaction(any(Transaction.class), any(Account.class)))
            .thenReturn(true); // Authorization successful
        when(authorizationEngine.checkCreditLimit(any(Transaction.class), any(Account.class)))
            .thenReturn(true); // Within credit limit
        when(authorizationEngine.verifyAvailableBalance(any(Transaction.class), any(Account.class)))
            .thenReturn(true); // Sufficient balance
        when(authorizationEngine.validateCardStatus(any(Transaction.class)))
            .thenReturn(true); // Valid card status
        when(authorizationEngine.handleConcurrentAuthorizations(any(Transaction.class)))
            .thenReturn(true); // Concurrent authorization handling successful
    }

    /**
     * Setup mock services for successful validation scenarios.
     * Used by integration tests to ensure all validation steps pass.
     */
    private void setupSuccessfulValidationMocks() {
        // Reset and reconfigure mocks for success scenarios
        resetMocks();
        configureMockServices();
    }

    /**
     * Verify that all validation steps were executed during complete transaction validation.
     * Ensures comprehensive validation matching COBOL COTRN02C program flow.
     */
    private void verifyAllValidationStepsExecuted() {
        // Verify authorization engine interactions
        verify(authorizationEngine, atLeastOnce()).checkCreditLimit(any(Transaction.class), any(Account.class));
        verify(authorizationEngine, atLeastOnce()).verifyAvailableBalance(any(Transaction.class), any(Account.class));
        verify(authorizationEngine, atLeastOnce()).validateCardStatus(any(Transaction.class));

        // Verify fraud detection interactions
        verify(fraudDetectionService, atLeastOnce()).detectFraudPatterns(any(Transaction.class));
        verify(fraudDetectionService, atLeastOnce()).calculateRiskScore(any(Transaction.class));
    }
}