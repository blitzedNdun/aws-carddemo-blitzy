/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.repository.AccountRepository;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.test.TestDataGenerator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for AccountProcessingService validating COBOL CBACT01C 
 * batch account processing logic migration to Java.
 * 
 * This test class ensures 100% functional parity between the original COBOL CBACT01C.cbl
 * batch processing program and the Java AccountProcessingService implementation.
 * 
 * Test Coverage:
 * - Account updates with COBOL-equivalent validation rules
 * - Balance calculations with exact COMP-3 decimal precision
 * - Batch processing patterns with checkpoint/restart capabilities
 * - Fee assessment logic matching COBOL business rules
 * - Credit limit adjustments with business rule compliance
 * - Account data validation using COBOL edit patterns
 * - Performance validation for large datasets within 4-hour processing windows
 * - BigDecimal precision validation for all financial calculations
 * 
 * Key Testing Principles:
 * - All monetary amounts tested with COBOL COMP-3 precision (scale=2, HALF_UP rounding)
 * - Business logic validation matches original COBOL program paragraph structure
 * - Error handling replicates COBOL ABEND routines and validation patterns
 * - Batch processing performance meets or exceeds mainframe baseline requirements
 * - Integration with repository layer validates VSAM-to-JPA data access patterns
 * 
 * COBOL Source Reference: CBACT01C.cbl - Account processing batch program
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AccountProcessingServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountProcessingService accountProcessingService;

    private TestDataGenerator testDataGenerator;
    private CobolComparisonUtils cobolComparisonUtils;

    // Test constants matching COBOL program values
    private static final BigDecimal TEST_BALANCE = new BigDecimal("1500.00");
    private static final BigDecimal TEST_CREDIT_LIMIT = new BigDecimal("5000.00");
    private static final BigDecimal TEST_MONTHLY_FEE = new BigDecimal("25.00");
    private static final BigDecimal TEST_OVERLIMIT_FEE = new BigDecimal("35.00");
    private static final String TEST_ACCOUNT_GROUP = "DEFAULT";
    private static final int TEST_CHUNK_SIZE = 100;
    private static final int LARGE_DATASET_SIZE = 10000;

    /**
     * Test setup method executed before each test to initialize test data and mock objects.
     * Sets up COBOL-compliant test data generation and comparison utilities.
     */
    @BeforeEach
    public void setUp() {
        testDataGenerator = new TestDataGenerator();
        cobolComparisonUtils = new CobolComparisonUtils();
        
        // Reset mock behaviors for clean test state
        Mockito.reset(accountRepository);
    }

    /**
     * Test suite for processAccountUpdates method validating COBOL paragraph 1000-ACCTFILE-GET-NEXT
     * equivalent functionality with comprehensive field updates and business rule validation.
     */
    @Nested
    @DisplayName("Account Updates Processing Tests")
    class AccountUpdatesTests {

        @Test
        @DisplayName("Test successful account updates with valid data")
        public void testProcessAccountUpdates() {
            // Given: Create test account with COBOL-compatible data
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setAccountId(1000001L);
            testAccount.setCurrentBalance(testDataGenerator.generateComp3BigDecimal("1000.00"));
            testAccount.setCreditLimit(testDataGenerator.generateComp3BigDecimal("5000.00"));
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentBalance", new BigDecimal("1250.75"));
            updates.put("activeStatus", "Y");
            updates.put("addressZip", "12345");
            
            // Mock repository behavior
            when(accountRepository.findById(1000001L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // When: Process account updates
            Account result = accountProcessingService.processAccountUpdates(1000001L, updates);

            // Then: Verify updates applied with COBOL precision
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(1000001L);
            assertThat(result.getActiveStatus()).isEqualTo("Y");
            assertThat(result.getAddressZip()).isEqualTo("12345");
            
            // Verify BigDecimal precision matches COBOL COMP-3 behavior
            BigDecimal expectedBalance = new BigDecimal("1250.75");
            boolean precisionMatch = CobolComparisonUtils.compareBigDecimals(
                result.getCurrentBalance(), expectedBalance);
            assertThat(precisionMatch).isTrue();
            
            // Verify repository interactions
            verify(accountRepository).findById(1000001L);
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("Test account updates with invalid account ID")
        public void testProcessAccountUpdatesInvalidId() {
            // Given: Invalid account ID
            Long invalidAccountId = -1L;
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentBalance", new BigDecimal("100.00"));

            // When/Then: Verify exception thrown for invalid account ID
            assertThatThrownBy(() -> accountProcessingService.processAccountUpdates(invalidAccountId, updates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account ID must be positive");

            // Verify no repository calls made
            verify(accountRepository, never()).findById(anyLong());
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("Test account updates with account not found")
        public void testProcessAccountUpdatesNotFound() {
            // Given: Non-existent account ID
            Long accountId = 9999999L;
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentBalance", new BigDecimal("100.00"));
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // When/Then: Verify exception thrown for missing account
            assertThatThrownBy(() -> accountProcessingService.processAccountUpdates(accountId, updates))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found with ID");

            verify(accountRepository).findById(accountId);
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("Test account updates with empty updates map")
        public void testProcessAccountUpdatesEmptyMap() {
            // Given: Empty updates map
            Long accountId = 1000001L;
            Map<String, Object> emptyUpdates = new HashMap<>();

            // When/Then: Verify exception thrown for empty updates
            assertThatThrownBy(() -> accountProcessingService.processAccountUpdates(accountId, emptyUpdates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Updates map cannot be null or empty");

            verify(accountRepository, never()).findById(anyLong());
            verify(accountRepository, never()).save(any(Account.class));
        }
    }

    /**
     * Test suite for calculateAccountBalance method validating COBOL COMP-3 decimal precision
     * and balance calculation logic matching the original CBACT01C paragraph structure.
     */
    @Nested
    @DisplayName("Balance Calculation with COMP-3 Precision Tests")
    class BalanceCalculationTests {

        @Test
        @DisplayName("Test balance calculation with COBOL COMP-3 precision")
        public void testCalculateAccountBalanceWithComp3Precision() {
            // Given: Account with COBOL-precision monetary values
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setCurrentBalance(testDataGenerator.generateComp3BigDecimal("1000.00"));
            testAccount.setCurrentCycleCredit(testDataGenerator.generateComp3BigDecimal("250.50"));
            testAccount.setCurrentCycleDebit(testDataGenerator.generateComp3BigDecimal("175.25"));

            // Expected calculation: 1000.00 + 250.50 - 175.25 = 1075.25
            BigDecimal expectedBalance = testDataGenerator.generateComp3BigDecimal("1075.25");

            // When: Calculate balance using service method
            BigDecimal calculatedBalance = accountProcessingService.calculateAccountBalance(testAccount);

            // Then: Verify precision matches COBOL COMP-3 behavior exactly
            assertThat(calculatedBalance).isNotNull();
            assertThat(calculatedBalance.scale()).isEqualTo(2); // COBOL COMP-3 scale
            
            boolean precisionMatch = CobolComparisonUtils.compareBigDecimals(
                calculatedBalance, expectedBalance);
            assertThat(precisionMatch).isTrue();

            // Verify rounding mode matches COBOL HALF_UP behavior
            assertThat(calculatedBalance.setScale(2, RoundingMode.HALF_UP)).isEqualTo(expectedBalance);
        }

        @Test
        @DisplayName("Test balance calculation with null values")
        public void testCalculateAccountBalanceNullValues() {
            // Given: Account with null monetary values
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setCurrentBalance(null);
            testAccount.setCurrentCycleCredit(null);
            testAccount.setCurrentCycleDebit(null);

            // When: Calculate balance
            BigDecimal calculatedBalance = accountProcessingService.calculateAccountBalance(testAccount);

            // Then: Verify zero balance with proper scale
            assertThat(calculatedBalance).isNotNull();
            assertThat(calculatedBalance.scale()).isEqualTo(2);
            assertThat(calculatedBalance).isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
        }

        @Test
        @DisplayName("Test balance calculation with negative amounts")
        public void testCalculateAccountBalanceNegativeAmounts() {
            // Given: Account with negative balance (debt situation)
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setCurrentBalance(testDataGenerator.generateComp3BigDecimal("-500.00"));
            testAccount.setCurrentCycleCredit(testDataGenerator.generateComp3BigDecimal("100.00"));
            testAccount.setCurrentCycleDebit(testDataGenerator.generateComp3BigDecimal("200.00"));

            // Expected: -500.00 + 100.00 - 200.00 = -600.00
            BigDecimal expectedBalance = testDataGenerator.generateComp3BigDecimal("-600.00");

            // When: Calculate balance
            BigDecimal calculatedBalance = accountProcessingService.calculateAccountBalance(testAccount);

            // Then: Verify negative balance handled correctly
            boolean precisionMatch = CobolComparisonUtils.compareBigDecimals(
                calculatedBalance, expectedBalance);
            assertThat(precisionMatch).isTrue();
            assertThat(calculatedBalance.signum()).isEqualTo(-1); // Negative
        }

        @Test
        @DisplayName("Test balance calculation with null account")
        public void testCalculateAccountBalanceNullAccount() {
            // When/Then: Verify exception thrown for null account
            assertThatThrownBy(() -> accountProcessingService.calculateAccountBalance(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account cannot be null for balance calculation");
        }
    }

    /**
     * Test suite for processAccountBatch method validating batch processing patterns
     * matching COBOL CBACT01C sequential file processing with chunk-based operations.
     */
    @Nested
    @DisplayName("Batch Processing Tests")
    class BatchProcessingTests {

        @Test
        @DisplayName("Test batch processing with default parameters")
        public void testProcessAccountBatch() {
            // Given: List of test accounts for batch processing
            List<Account> testAccounts = generateTestAccountList(5);
            Map<String, Object> batchParams = new HashMap<>();
            batchParams.put("chunkSize", 2);

            when(accountRepository.findAll()).thenReturn(testAccounts);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Process account batch
            Map<String, Object> results = accountProcessingService.processAccountBatch(batchParams);

            // Then: Verify batch processing results
            assertThat(results).isNotNull();
            assertThat(results.get("totalProcessed")).isEqualTo(5);
            assertThat(results.get("totalErrors")).isEqualTo(0);
            assertThat(results.get("status")).isEqualTo("COMPLETED");

            // Verify repository interactions for all accounts
            verify(accountRepository).findAll();
            verify(accountRepository, times(5)).save(any(Account.class));
        }

        @Test
        @DisplayName("Test batch processing with group filter")
        public void testProcessAccountBatchWithGroupFilter() {
            // Given: Accounts with specific group ID
            List<Account> allAccounts = generateTestAccountList(10);
            List<Account> groupAccounts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                allAccounts.get(i).setAccountGroupId(TEST_ACCOUNT_GROUP);
                groupAccounts.add(allAccounts.get(i));
            }

            Map<String, Object> batchParams = new HashMap<>();
            batchParams.put("groupId", TEST_ACCOUNT_GROUP);
            batchParams.put("chunkSize", 5);

            when(accountRepository.findAll()).thenReturn(allAccounts);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Process batch with group filter
            Map<String, Object> results = accountProcessingService.processAccountBatch(batchParams);

            // Then: Verify only group accounts processed
            assertThat(results).isNotNull();
            assertThat(results.get("totalProcessed")).isEqualTo(3);
            assertThat(results.get("status")).isEqualTo("COMPLETED");

            verify(accountRepository).findAll();
            verify(accountRepository, times(3)).save(any(Account.class));
        }

        @Test
        @DisplayName("Test batch processing performance within 4-hour window")
        public void testProcessAccountBatchPerformance() {
            // Given: Batch parameters with performance tracking
            Map<String, Object> batchParams = new HashMap<>();
            batchParams.put("chunkSize", TEST_CHUNK_SIZE);
            
            List<Account> testAccounts = generateTestAccountList(1000); // Smaller set for unit test
            when(accountRepository.findAll()).thenReturn(testAccounts);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Process batch and measure time
            long startTime = System.currentTimeMillis();
            Map<String, Object> results = accountProcessingService.processAccountBatch(batchParams);
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            // Then: Verify processing completes in reasonable time
            assertThat(results.get("totalProcessed")).isEqualTo(1000);
            assertThat(results.get("status")).isEqualTo("COMPLETED");
            
            // Performance assertion: should complete much faster than 4 hours for test data
            // For 1000 records, expect completion in seconds not minutes
            assertThat(processingTime).isLessThan(60000); // Less than 60 seconds

            // Verify throughput metrics available
            Map<String, Object> metrics = accountProcessingService.getProcessingMetrics();
            assertThat(metrics).containsKey("throughputPerSecond");
            assertThat(metrics).containsKey("totalDurationMs");
        }
    }

    /**
     * Test suite for assessAccountFees method validating fee calculation business logic
     * matching COBOL fee assessment rules and monetary precision requirements.
     */
    @Nested
    @DisplayName("Fee Assessment Tests")
    class FeeAssessmentTests {

        @Test
        @DisplayName("Test fee assessment for account below minimum balance")
        public void testAssessAccountFees() {
            // Given: Account below minimum balance threshold
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setAccountId(1000001L);
            testAccount.setActiveStatus("Y");
            testAccount.setCurrentBalance(testDataGenerator.generateComp3BigDecimal("500.00")); // Below $1000 threshold
            testAccount.setCreditLimit(testDataGenerator.generateComp3BigDecimal("2000.00"));

            when(accountRepository.findById(1000001L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Assess fees
            Map<String, BigDecimal> feeResults = accountProcessingService.assessAccountFees(1000001L);

            // Then: Verify maintenance fee assessed
            assertThat(feeResults).isNotNull();
            assertThat(feeResults).containsKey("maintenanceFee");
            assertThat(feeResults).containsKey("totalFees");
            
            BigDecimal maintenanceFee = feeResults.get("maintenanceFee");
            boolean feeMatch = CobolComparisonUtils.compareBigDecimals(maintenanceFee, TEST_MONTHLY_FEE);
            assertThat(feeMatch).isTrue();

            verify(accountRepository).findById(1000001L);
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("Test fee assessment for overlimit account")
        public void testAssessOverlimitFee() {
            // Given: Account over credit limit
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setAccountId(1000002L);
            testAccount.setActiveStatus("Y");
            testAccount.setCurrentBalance(testDataGenerator.generateComp3BigDecimal("5500.00")); // Over $5000 limit
            testAccount.setCreditLimit(testDataGenerator.generateComp3BigDecimal("5000.00"));

            when(accountRepository.findById(1000002L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Assess fees
            Map<String, BigDecimal> feeResults = accountProcessingService.assessAccountFees(1000002L);

            // Then: Verify overlimit fee assessed
            assertThat(feeResults).containsKey("overlimitFee");
            
            BigDecimal overlimitFee = feeResults.get("overlimitFee");
            boolean feeMatch = CobolComparisonUtils.compareBigDecimals(overlimitFee, TEST_OVERLIMIT_FEE);
            assertThat(feeMatch).isTrue();

            verify(accountRepository).findById(1000002L);
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("Test fee assessment with invalid account ID")
        public void testAssessAccountFeesInvalidId() {
            // When/Then: Verify exception thrown for invalid account ID
            assertThatThrownBy(() -> accountProcessingService.assessAccountFees(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account ID must be positive");

            verify(accountRepository, never()).findById(anyLong());
        }
    }

    /**
     * Test suite for adjustCreditLimits method validating credit limit business rules
     * and monetary precision matching COBOL credit management logic.
     */
    @Nested
    @DisplayName("Credit Limit Adjustment Tests")
    class CreditLimitAdjustmentTests {

        @Test
        @DisplayName("Test successful credit limit adjustment")
        public void testAdjustCreditLimits() {
            // Given: Account eligible for credit limit increase
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setAccountId(1000001L);
            testAccount.setActiveStatus("Y");
            testAccount.setCurrentBalance(testDataGenerator.generateComp3BigDecimal("1000.00"));
            testAccount.setCreditLimit(testDataGenerator.generateComp3BigDecimal("5000.00"));
            
            BigDecimal requestedLimit = testDataGenerator.generateComp3BigDecimal("5500.00"); // 10% increase

            when(accountRepository.findById(1000001L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Adjust credit limit
            Map<String, Object> result = accountProcessingService.adjustCreditLimits(1000001L, requestedLimit);

            // Then: Verify adjustment approved
            assertThat(result).isNotNull();
            assertThat(result.get("approved")).isEqualTo(true);
            assertThat(result.get("accountId")).isEqualTo(1000001L);
            assertThat(result.get("newLimit")).isEqualTo(requestedLimit);

            verify(accountRepository).findById(1000001L);
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("Test credit limit adjustment exceeding maximum increase")
        public void testAdjustCreditLimitsExceedsMaximum() {
            // Given: Account with credit limit increase request exceeding 25% maximum
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setAccountId(1000001L);
            testAccount.setActiveStatus("Y");
            testAccount.setCurrentBalance(testDataGenerator.generateComp3BigDecimal("1000.00"));
            testAccount.setCreditLimit(testDataGenerator.generateComp3BigDecimal("5000.00"));
            
            BigDecimal requestedLimit = testDataGenerator.generateComp3BigDecimal("7000.00"); // 40% increase

            when(accountRepository.findById(1000001L)).thenReturn(Optional.of(testAccount));

            // When: Attempt credit limit adjustment
            Map<String, Object> result = accountProcessingService.adjustCreditLimits(1000001L, requestedLimit);

            // Then: Verify adjustment rejected
            assertThat(result.get("approved")).isEqualTo(false);
            assertThat(result.get("reason")).asString().contains("exceeds maximum allowable increase");

            verify(accountRepository).findById(1000001L);
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("Test credit limit adjustment for inactive account")
        public void testAdjustCreditLimitsInactiveAccount() {
            // Given: Inactive account
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setAccountId(1000001L);
            testAccount.setActiveStatus("N"); // Inactive
            testAccount.setCreditLimit(testDataGenerator.generateComp3BigDecimal("5000.00"));
            
            BigDecimal requestedLimit = testDataGenerator.generateComp3BigDecimal("5500.00");

            when(accountRepository.findById(1000001L)).thenReturn(Optional.of(testAccount));

            // When: Attempt credit limit adjustment
            Map<String, Object> result = accountProcessingService.adjustCreditLimits(1000001L, requestedLimit);

            // Then: Verify adjustment rejected for inactive account
            assertThat(result.get("approved")).isEqualTo(false);
            assertThat(result.get("reason")).asString().contains("Account is not active");

            verify(accountRepository).findById(1000001L);
            verify(accountRepository, never()).save(any(Account.class));
        }
    }

    /**
     * Test suite for validateAccountData method validating COBOL-equivalent data validation
     * rules and business logic constraints matching original edit patterns.
     */
    @Nested
    @DisplayName("Account Data Validation Tests")
    class AccountDataValidationTests {

        @Test
        @DisplayName("Test account data validation with valid data")
        public void testValidateAccountData() {
            // Given: Account with valid data
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setAccountId(1000001L);
            testAccount.setActiveStatus("Y");
            testAccount.setCurrentBalance(testDataGenerator.generateComp3BigDecimal("1500.00"));
            testAccount.setCreditLimit(testDataGenerator.generateComp3BigDecimal("5000.00"));
            testAccount.setCashCreditLimit(testDataGenerator.generateComp3BigDecimal("1000.00"));

            // When: Validate account data
            Map<String, Object> validationResult = accountProcessingService.validateAccountData(testAccount);

            // Then: Verify validation passes
            assertThat(validationResult).isNotNull();
            assertThat(validationResult.get("isValid")).isEqualTo(true);
            assertThat(validationResult.get("accountId")).isEqualTo(1000001L);
            
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) validationResult.get("validationErrors");
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Test account data validation with invalid status")
        public void testValidateAccountDataInvalidStatus() {
            // Given: Account with invalid active status
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setActiveStatus("X"); // Invalid status

            // When: Validate account data
            Map<String, Object> validationResult = accountProcessingService.validateAccountData(testAccount);

            // Then: Verify validation fails
            assertThat(validationResult.get("isValid")).isEqualTo(false);
            
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) validationResult.get("validationErrors");
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(error -> error.contains("Active status must be 'Y' or 'N'"));
        }

        @Test
        @DisplayName("Test account data validation with cash limit exceeding credit limit")
        public void testValidateAccountDataCashLimitExceedsCredit() {
            // Given: Account with cash limit exceeding credit limit
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setCreditLimit(testDataGenerator.generateComp3BigDecimal("2000.00"));
            testAccount.setCashCreditLimit(testDataGenerator.generateComp3BigDecimal("3000.00")); // Exceeds credit limit

            // When: Validate account data
            Map<String, Object> validationResult = accountProcessingService.validateAccountData(testAccount);

            // Then: Verify validation fails
            assertThat(validationResult.get("isValid")).isEqualTo(false);
            
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) validationResult.get("validationErrors");
            assertThat(errors).anyMatch(error -> error.contains("Cash credit limit cannot exceed credit limit"));
        }

        @Test
        @DisplayName("Test account data validation with null account")
        public void testValidateAccountDataNullAccount() {
            // When/Then: Verify exception thrown for null account
            assertThatThrownBy(() -> accountProcessingService.validateAccountData(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account cannot be null for validation");
        }
    }

    /**
     * Test suite for batch checkpoint and restart capabilities validating COBOL-equivalent
     * recovery and continuation patterns for interrupted processing.
     */
    @Nested
    @DisplayName("Batch Checkpoint and Restart Tests")
    class CheckpointRestartTests {

        @Test
        @DisplayName("Test batch checkpoint and restart capabilities")
        public void testBatchCheckpointAndRestart() {
            // Given: Initial batch processing setup
            List<Account> testAccounts = generateTestAccountList(50);
            when(accountRepository.findAll()).thenReturn(testAccounts);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When: Start initial batch processing
            Map<String, Object> initialParams = new HashMap<>();
            initialParams.put("chunkSize", 10);
            
            Map<String, Object> initialResults = accountProcessingService.processAccountBatch(initialParams);
            
            // Simulate restart scenario
            Map<String, Object> restartParams = new HashMap<>();
            restartParams.put("restart", true);
            restartParams.put("resumePosition", 25);
            restartParams.put("chunkSize", 10);

            Map<String, Object> restartResults = accountProcessingService.restartAccountProcessing(restartParams);

            // Then: Verify checkpoint and restart functionality
            assertThat(initialResults.get("status")).isEqualTo("COMPLETED");
            assertThat(restartResults.get("restartStatus")).isEqualTo("SUCCESS");
            assertThat(restartResults.get("resumePosition")).isEqualTo(25);
            assertThat(restartResults).containsKey("batchResults");

            // Verify processing metrics include checkpoint information
            Map<String, Object> metrics = accountProcessingService.getProcessingMetrics();
            assertThat(metrics).containsKey("currentStatus");
        }

        @Test
        @DisplayName("Test restart with invalid parameters")
        public void testRestartWithInvalidParameters() {
            // Given: Invalid restart parameters
            Map<String, Object> invalidParams = new HashMap<>();
            invalidParams.put("resumePosition", -1); // Invalid position

            // When/Then: Verify restart handles invalid parameters gracefully
            Map<String, Object> restartResults = accountProcessingService.restartAccountProcessing(invalidParams);
            
            // Should still process with default values
            assertThat(restartResults).containsKey("restartStatus");
            assertThat(restartResults).containsKey("batchResults");
        }
    }

    /**
     * Test suite for performance validation with large datasets ensuring processing
     * completes within the required 4-hour window with proper throughput metrics.
     */
    @Nested
    @DisplayName("Performance and Large Dataset Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Test performance validation with large dataset")
        public void testPerformanceWithLargeDataset() {
            // Given: Large dataset for performance testing
            List<Account> largeDataset = generateTestAccountList(LARGE_DATASET_SIZE);
            when(accountRepository.findAll()).thenReturn(largeDataset);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, Object> batchParams = new HashMap<>();
            batchParams.put("chunkSize", 1000); // Large chunks for performance

            // When: Process large dataset and measure performance
            long startTime = System.currentTimeMillis();
            Map<String, Object> results = accountProcessingService.processAccountBatch(batchParams);
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            // Then: Verify performance meets requirements
            assertThat(results.get("totalProcessed")).isEqualTo(LARGE_DATASET_SIZE);
            assertThat(results.get("status")).isEqualTo("COMPLETED");

            // Performance validation: should process well within 4-hour window
            // For test environment, expect much faster processing
            long fourHoursInMs = 4 * 60 * 60 * 1000; // 4 hours in milliseconds
            assertThat(processingTime).isLessThan(fourHoursInMs);

            // Verify throughput metrics are calculated
            Map<String, Object> metrics = accountProcessingService.getProcessingMetrics();
            assertThat(metrics).containsKey("throughputPerSecond");
            
            Double throughput = (Double) metrics.get("throughputPerSecond");
            assertThat(throughput).isGreaterThan(0.0);

            // Log performance metrics for monitoring
            System.out.printf("Performance Test Results:%n");
            System.out.printf("  Processed: %d accounts%n", LARGE_DATASET_SIZE);
            System.out.printf("  Time: %d ms%n", processingTime);
            System.out.printf("  Throughput: %.2f accounts/second%n", throughput);
        }

        @Test
        @DisplayName("Test memory efficiency with large dataset processing")
        public void testMemoryEfficiencyLargeDataset() {
            // Given: Memory monitoring setup
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();

            List<Account> largeDataset = generateTestAccountList(5000); // Moderate size for unit test
            when(accountRepository.findAll()).thenReturn(largeDataset);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, Object> batchParams = new HashMap<>();
            batchParams.put("chunkSize", 500);

            // When: Process with memory monitoring
            Map<String, Object> results = accountProcessingService.processAccountBatch(batchParams);

            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = finalMemory - initialMemory;

            // Then: Verify processing completed efficiently
            assertThat(results.get("status")).isEqualTo("COMPLETED");
            
            // Memory usage should be reasonable (less than 100MB for test dataset)
            long maxExpectedMemory = 100 * 1024 * 1024; // 100MB
            assertThat(memoryUsed).isLessThan(maxExpectedMemory);
        }
    }

    // Helper methods for test data setup and validation

    /**
     * Generates a list of test accounts with COBOL-compatible data patterns.
     * 
     * @param count number of test accounts to generate
     * @return List of Account entities for testing
     */
    private List<Account> generateTestAccountList(int count) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Account account = testDataGenerator.generateAccount();
            account.setAccountId(1000000L + i);
            account.setCurrentBalance(testDataGenerator.generateBalance());
            account.setCreditLimit(testDataGenerator.generateCreditLimit());
            accounts.add(account);
        }
        return accounts;
    }

    /**
     * Sets up common test fixtures and data for integration with base test functionality.
     * Replicates functionality that would be provided by BaseIntegrationTest.
     */
    private void setupTestData() {
        // Initialize test data generators with consistent seed for reproducible tests
        testDataGenerator.resetRandomSeed();
    }

    /**
     * Cleans up test data after test execution.
     * Replicates functionality that would be provided by BaseIntegrationTest.
     */
    private void cleanupTestData() {
        // Reset mock objects and clear any cached data
        Mockito.reset(accountRepository);
    }

    /**
     * Creates a test account with specified parameters for controlled testing.
     * Replicates functionality that would be provided by BaseIntegrationTest.
     * 
     * @param accountId the account ID to assign
     * @param balance the initial balance
     * @param creditLimit the credit limit
     * @return Account entity configured for testing
     */
    private Account createTestAccount(Long accountId, BigDecimal balance, BigDecimal creditLimit) {
        Account account = testDataGenerator.generateAccount();
        account.setAccountId(accountId);
        account.setCurrentBalance(balance);
        account.setCreditLimit(creditLimit);
        return account;
    }

    /**
     * Asserts that two BigDecimal values are equal using COBOL comparison semantics.
     * Replicates functionality that would be provided by BaseIntegrationTest.
     * 
     * @param actual the actual BigDecimal value
     * @param expected the expected BigDecimal value
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected) {
        boolean match = CobolComparisonUtils.compareBigDecimals(actual, expected);
        assertThat(match)
            .as("BigDecimal comparison failed: actual=%s, expected=%s", actual, expected)
            .isTrue();
    }
}