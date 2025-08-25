/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.util.AmountCalculator;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import com.carddemo.batch.BatchProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for AccountMaintenanceBatchService validating the COBOL CBACT01C
 * account maintenance logic migration, testing account processing, dormancy checks,
 * fee assessments, and balance reconciliation functionality.
 * 
 * This test validates:
 * - Account dormancy determination based on transaction history  
 * - Fee assessment for dormant and active accounts with COBOL precision
 * - Balance reconciliation with transaction activity
 * - Negative balance handling with overdraft protection
 * - Error handling and validation
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountMaintenanceBatchService Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class AccountMaintenanceBatchServiceTest {

    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private BatchProperties batchProperties;

    @InjectMocks
    private AccountMaintenanceBatchService accountMaintenanceBatchService;

    private Account testAccount;
    private Transaction testTransaction;
    
    private static final Long TEST_ACCOUNT_ID = 4000000000000001L;
    private static final String ACTIVE_STATUS = "A";
    private static final String DORMANT_STATUS = "D";
    private static final String SUSPENDED_STATUS = "S";
    private static final BigDecimal TEST_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal TEST_CREDIT_LIMIT = new BigDecimal("5000.00");
    private static final BigDecimal MONTHLY_MAINTENANCE_FEE = new BigDecimal("5.00");
    private static final BigDecimal DORMANCY_FEE = new BigDecimal("25.00");

    @BeforeEach
    void setUp() {
        // Create test account with typical credit card data
        testAccount = new Account();
        testAccount.setAccountId(TEST_ACCOUNT_ID);
        testAccount.setActiveStatus(ACTIVE_STATUS);
        testAccount.setCurrentBalance(TEST_BALANCE);
        testAccount.setCreditLimit(TEST_CREDIT_LIMIT);
        testAccount.setCashCreditLimit(new BigDecimal("1000.00"));
        testAccount.setOpenDate(LocalDate.now().minusYears(2));
        testAccount.setExpirationDate(LocalDate.now().plusYears(2));
        testAccount.setReissueDate(LocalDate.now().minusMonths(6));
        testAccount.setCurrentCycleCredit(BigDecimal.ZERO);
        testAccount.setCurrentCycleDebit(new BigDecimal("250.00"));
        testAccount.setGroupId("DEFAULT");

        // Create test transaction with recent activity
        testTransaction = new Transaction();
        testTransaction.setAccountId(TEST_ACCOUNT_ID);
        testTransaction.setTransactionDate(LocalDate.now().minusDays(30));
        testTransaction.setAmount(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Should successfully process account maintenance for active account")
    void testProcessAccountMaintenance_ActiveAccount() {
        // Given: Active account with recent transactions
        when(transactionRepository.findTopByAccountIdOrderByTransactionDateDesc(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.findByAccountIdAndTransactionDateBetween(
            eq(TEST_ACCOUNT_ID), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(testTransaction));

        try (MockedStatic<ValidationUtil> validationUtilMock = mockStatic(ValidationUtil.class);
             MockedStatic<CobolDataConverter> cobolConverterMock = mockStatic(CobolDataConverter.class);
             MockedStatic<AmountCalculator> amountCalculatorMock = mockStatic(AmountCalculator.class)) {
            
            // Mock utility methods
            validationUtilMock.when(() -> ValidationUtil.validateRequiredField(anyString(), anyString()))
                .then(invocation -> null);
            validationUtilMock.when(() -> ValidationUtil.validateNumericField(anyString(), anyString()))
                .then(invocation -> null);
            
            cobolConverterMock.when(() -> CobolDataConverter.preservePrecision(any(BigDecimal.class), anyInt()))
                .thenReturn(MONTHLY_MAINTENANCE_FEE);
                
            amountCalculatorMock.when(() -> AmountCalculator.validateAmount(any(BigDecimal.class), anyString()))
                .thenAnswer(invocation -> null); // void method
            amountCalculatorMock.when(() -> AmountCalculator.calculateBalance(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("995.00"));
            amountCalculatorMock.when(() -> AmountCalculator.applyRounding(any(BigDecimal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When: Processing account maintenance
            Account result = accountMaintenanceBatchService.processAccountMaintenance(testAccount);

            // Then: Account should be processed successfully
            assertNotNull(result);
            assertEquals(TEST_ACCOUNT_ID, result.getAccountId());
            assertEquals(ACTIVE_STATUS, result.getActiveStatus()); // Should remain active
            
            // Verify utility calls
            validationUtilMock.verify(() -> ValidationUtil.validateRequiredField("accountId", TEST_ACCOUNT_ID.toString()));
            validationUtilMock.verify(() -> ValidationUtil.validateNumericField("accountId", TEST_ACCOUNT_ID.toString()));
        }
    }

    @Test
    @DisplayName("Should mark account as dormant when no recent transactions")
    void testPerformDormancyCheck_NoRecentTransactions() {
        // Given: Account with old transactions (over 365 days)
        Transaction oldTransaction = new Transaction();
        oldTransaction.setAccountId(TEST_ACCOUNT_ID);
        oldTransaction.setTransactionDate(LocalDate.now().minusDays(400));
        oldTransaction.setAmount(new BigDecimal("50.00"));
        
        when(transactionRepository.findTopByAccountIdOrderByTransactionDateDesc(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(oldTransaction));

        try (MockedStatic<DateConversionUtil> dateUtilMock = mockStatic(DateConversionUtil.class)) {
            dateUtilMock.when(() -> DateConversionUtil.addDays(any(LocalDate.class), eq(-365L)))
                .thenReturn(LocalDate.now().minusDays(365));

            // When: Performing dormancy check
            accountMaintenanceBatchService.performDormancyCheck(testAccount);

            // Then: Account should be marked as dormant
            assertEquals(DORMANT_STATUS, testAccount.getActiveStatus());
        }
    }

    @Test
    @DisplayName("Should keep account active when recent transactions exist")
    void testPerformDormancyCheck_RecentTransactions() {
        // Given: Account with recent transactions (within 365 days)
        testTransaction.setTransactionDate(LocalDate.now().minusDays(30));
        when(transactionRepository.findTopByAccountIdOrderByTransactionDateDesc(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(testTransaction));

        try (MockedStatic<DateConversionUtil> dateUtilMock = mockStatic(DateConversionUtil.class)) {
            dateUtilMock.when(() -> DateConversionUtil.addDays(any(LocalDate.class), eq(-365L)))
                .thenReturn(LocalDate.now().minusDays(365));

            // When: Performing dormancy check
            accountMaintenanceBatchService.performDormancyCheck(testAccount);

            // Then: Account should remain active
            assertEquals(ACTIVE_STATUS, testAccount.getActiveStatus());
        }
    }

    @Test
    @DisplayName("Should assess monthly maintenance fee for active account")
    void testAssessFees_ActiveAccount() {
        // Given: Active account
        testAccount.setActiveStatus(ACTIVE_STATUS);

        try (MockedStatic<CobolDataConverter> cobolConverterMock = mockStatic(CobolDataConverter.class);
             MockedStatic<AmountCalculator> amountCalculatorMock = mockStatic(AmountCalculator.class)) {
            
            cobolConverterMock.when(() -> CobolDataConverter.preservePrecision(any(BigDecimal.class), anyInt()))
                .thenReturn(MONTHLY_MAINTENANCE_FEE);
                
            amountCalculatorMock.when(() -> AmountCalculator.validateAmount(any(BigDecimal.class), anyString()))
                .thenAnswer(invocation -> null); // void method
            amountCalculatorMock.when(() -> AmountCalculator.calculateBalance(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("995.00"));
            amountCalculatorMock.when(() -> AmountCalculator.applyRounding(any(BigDecimal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When: Assessing fees
            accountMaintenanceBatchService.assessFees(testAccount);

            // Then: Monthly maintenance fee should be applied
            assertEquals(new BigDecimal("995.00"), testAccount.getCurrentBalance());
            
            // Verify COBOL precision preservation
            cobolConverterMock.verify(() -> CobolDataConverter.preservePrecision(MONTHLY_MAINTENANCE_FEE, 2));
            amountCalculatorMock.verify(() -> AmountCalculator.validateAmount(MONTHLY_MAINTENANCE_FEE, "Monthly Maintenance Fee"));
        }
    }

    @Test
    @DisplayName("Should assess dormancy fee for dormant account")
    void testAssessFees_DormantAccount() {
        // Given: Dormant account
        testAccount.setActiveStatus(DORMANT_STATUS);

        try (MockedStatic<CobolDataConverter> cobolConverterMock = mockStatic(CobolDataConverter.class);
             MockedStatic<AmountCalculator> amountCalculatorMock = mockStatic(AmountCalculator.class)) {
            
            cobolConverterMock.when(() -> CobolDataConverter.preservePrecision(any(BigDecimal.class), anyInt()))
                .thenReturn(DORMANCY_FEE);
                
            amountCalculatorMock.when(() -> AmountCalculator.validateAmount(any(BigDecimal.class), anyString()))
                .thenAnswer(invocation -> null); // void method
            amountCalculatorMock.when(() -> AmountCalculator.calculateBalance(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("975.00"));
            amountCalculatorMock.when(() -> AmountCalculator.applyRounding(any(BigDecimal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When: Assessing fees
            accountMaintenanceBatchService.assessFees(testAccount);

            // Then: Dormancy fee should be applied
            assertEquals(new BigDecimal("975.00"), testAccount.getCurrentBalance());
            
            // Verify COBOL precision preservation
            cobolConverterMock.verify(() -> CobolDataConverter.preservePrecision(DORMANCY_FEE, 2));
            amountCalculatorMock.verify(() -> AmountCalculator.validateAmount(DORMANCY_FEE, "Dormancy Fee"));
        }
    }

    @Test
    @DisplayName("Should handle negative balance with credit limit")
    void testAssessFees_NegativeBalanceWithCreditLimit() {
        // Given: Account with low balance that will go negative after fee and exceed credit limit
        testAccount.setCurrentBalance(new BigDecimal("3.00"));
        testAccount.setActiveStatus(ACTIVE_STATUS);
        // Set smaller credit limit so negative balance will exceed it
        testAccount.setCreditLimit(new BigDecimal("1.00"));

        try (MockedStatic<CobolDataConverter> cobolConverterMock = mockStatic(CobolDataConverter.class);
             MockedStatic<AmountCalculator> amountCalculatorMock = mockStatic(AmountCalculator.class)) {
            
            cobolConverterMock.when(() -> CobolDataConverter.preservePrecision(any(BigDecimal.class), anyInt()))
                .thenReturn(MONTHLY_MAINTENANCE_FEE);
                
            amountCalculatorMock.when(() -> AmountCalculator.validateAmount(any(BigDecimal.class), anyString()))
                .thenAnswer(invocation -> null); // void method
            amountCalculatorMock.when(() -> AmountCalculator.calculateBalance(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("-2.00")); // -2.00 exceeds credit limit of 1.00
            amountCalculatorMock.when(() -> AmountCalculator.applyRounding(any(BigDecimal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            amountCalculatorMock.when(() -> AmountCalculator.processNegativeBalance(
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("-37.00"));

            // When: Assessing fees
            accountMaintenanceBatchService.assessFees(testAccount);

            // Then: Overdraft fee should be processed
            amountCalculatorMock.verify(() -> AmountCalculator.processNegativeBalance(
                any(BigDecimal.class), eq(new BigDecimal("1.00")), any(BigDecimal.class)));
        }
    }

    @Test
    @DisplayName("Should suspend account when negative balance exceeds credit limit")
    void testAssessFees_NegativeBalanceNoCredit() {
        // Given: Account with no credit limit
        testAccount.setCurrentBalance(new BigDecimal("3.00"));
        testAccount.setCreditLimit(null);
        testAccount.setActiveStatus(ACTIVE_STATUS);

        try (MockedStatic<CobolDataConverter> cobolConverterMock = mockStatic(CobolDataConverter.class);
             MockedStatic<AmountCalculator> amountCalculatorMock = mockStatic(AmountCalculator.class)) {
            
            cobolConverterMock.when(() -> CobolDataConverter.preservePrecision(any(BigDecimal.class), anyInt()))
                .thenReturn(MONTHLY_MAINTENANCE_FEE);
                
            amountCalculatorMock.when(() -> AmountCalculator.validateAmount(any(BigDecimal.class), anyString()))
                .thenAnswer(invocation -> null); // void method
            amountCalculatorMock.when(() -> AmountCalculator.calculateBalance(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("-2.00"));
            amountCalculatorMock.when(() -> AmountCalculator.applyRounding(any(BigDecimal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When: Assessing fees
            accountMaintenanceBatchService.assessFees(testAccount);

            // Then: Account should be suspended
            assertEquals(SUSPENDED_STATUS, testAccount.getActiveStatus());
        }
    }

    @Test
    @DisplayName("Should reconcile balance with recent transactions")
    void testReconcileBalance_WithTransactions() {
        // Given: Account with recent transactions
        when(transactionRepository.findByAccountIdAndTransactionDateBetween(
            eq(TEST_ACCOUNT_ID), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(testTransaction));

        try (MockedStatic<DateConversionUtil> dateUtilMock = mockStatic(DateConversionUtil.class);
             MockedStatic<CobolDataConverter> cobolConverterMock = mockStatic(CobolDataConverter.class);
             MockedStatic<AmountCalculator> amountCalculatorMock = mockStatic(AmountCalculator.class)) {
            
            dateUtilMock.when(() -> DateConversionUtil.addDays(any(LocalDate.class), eq(-30L)))
                .thenReturn(LocalDate.now().minusDays(30));
                
            cobolConverterMock.when(() -> CobolDataConverter.preservePrecision(any(BigDecimal.class), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));
                
            amountCalculatorMock.when(() -> AmountCalculator.applyRounding(any(BigDecimal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            amountCalculatorMock.when(() -> AmountCalculator.validateAmount(any(BigDecimal.class), anyString()))
                .thenAnswer(invocation -> null); // void method

            // When: Reconciling balance
            accountMaintenanceBatchService.reconcileBalance(testAccount);

            // Then: Balance should be validated
            amountCalculatorMock.verify(() -> AmountCalculator.validateAmount(any(BigDecimal.class), eq("Reconciled Balance")));
            
            // Verify transaction sum calculation
            cobolConverterMock.verify(() -> CobolDataConverter.preservePrecision(testTransaction.getAmount(), 2));
        }
    }

    @Test
    @DisplayName("Should handle reconciliation with no transactions")
    void testReconcileBalance_NoTransactions() {
        // Given: Account with no recent transactions
        when(transactionRepository.findByAccountIdAndTransactionDateBetween(
            eq(TEST_ACCOUNT_ID), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        try (MockedStatic<DateConversionUtil> dateUtilMock = mockStatic(DateConversionUtil.class)) {
            dateUtilMock.when(() -> DateConversionUtil.addDays(any(LocalDate.class), eq(-30L)))
                .thenReturn(LocalDate.now().minusDays(30));

            // When: Reconciling balance
            accountMaintenanceBatchService.reconcileBalance(testAccount);

            // Then: No exception should be thrown and method completes successfully
            verify(transactionRepository).findByAccountIdAndTransactionDateBetween(
                eq(TEST_ACCOUNT_ID), any(LocalDate.class), any(LocalDate.class));
        }
    }

    @Test
    @DisplayName("Should handle validation errors gracefully")
    void testProcessAccountMaintenance_ValidationError() {
        // Given: Invalid account data that fails validation
        testAccount.setAccountId(null);

        try (MockedStatic<ValidationUtil> validationUtilMock = mockStatic(ValidationUtil.class)) {
            // String.valueOf(null) returns "null", so we need to match that
            validationUtilMock.when(() -> ValidationUtil.validateRequiredField(eq("accountId"), eq("null")))
                .thenThrow(new IllegalArgumentException("Account ID is required"));

            // When/Then: Processing should fail with validation error
            assertThrows(IllegalArgumentException.class, () -> {
                accountMaintenanceBatchService.processAccountMaintenance(testAccount);
            });
        }
    }

    @Test
    @DisplayName("Should reactivate dormant account with recent activity")
    void testPerformDormancyCheck_ReactivateDormantAccount() {
        // Given: Dormant account with recent transactions
        testAccount.setActiveStatus(DORMANT_STATUS);
        Transaction recentTransaction = new Transaction();
        recentTransaction.setAccountId(TEST_ACCOUNT_ID);
        recentTransaction.setTransactionDate(LocalDate.now().minusDays(30));
        recentTransaction.setAmount(new BigDecimal("50.00"));
        
        when(transactionRepository.findTopByAccountIdOrderByTransactionDateDesc(TEST_ACCOUNT_ID))
            .thenReturn(Optional.of(recentTransaction));

        try (MockedStatic<DateConversionUtil> dateUtilMock = mockStatic(DateConversionUtil.class)) {
            dateUtilMock.when(() -> DateConversionUtil.addDays(any(LocalDate.class), eq(-365L)))
                .thenReturn(LocalDate.now().minusDays(365));

            // When: Performing dormancy check
            accountMaintenanceBatchService.performDormancyCheck(testAccount);

            // Then: Account should be reactivated
            assertEquals(ACTIVE_STATUS, testAccount.getActiveStatus());
        }
    }
}