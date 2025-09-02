/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import com.carddemo.dto.BillDetailResponse;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.service.BillDetailService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Comprehensive unit test class for BillDetailService validating COBOL bill detail display logic 
 * migration to Java Spring Boot implementation.
 * 
 * This test class ensures 100% functional parity between the original COBOL COBIL00C.cbl program
 * and the modernized Java BillDetailService implementation. All financial calculations must
 * maintain penny-level accuracy using BigDecimal precision matching COBOL COMP-3 data types.
 * 
 * Test Coverage Areas:
 * - Bill itemization display with transaction categorization
 * - Interest charge calculations with COBOL rounding precision
 * - Payment history retrieval and display formatting
 * - Minimum payment calculation using business rules
 * - Due date determination based on billing cycles
 * - Charge breakdown and categorization logic
 * 
 * COBOL Program Mapping:
 * - Original COBOL: app/cbl/COBIL00C.cbl
 * - Java Service: BillDetailService.java
 * - Copybook: CVTRA05Y.cpy (Transaction structure)
 * - Copybook: CVACT01Y.cpy (Account structure)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillDetailService - COBOL COBIL00C.cbl Functional Parity Tests")
class BillDetailServiceTest extends BaseServiceTest {

    @InjectMocks
    private BillDetailService billDetailService;
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    // Test data generators and utilities
    private TestDataGenerator testDataGenerator = new TestDataGenerator();
    private CobolComparisonUtils cobolComparisonUtils = new CobolComparisonUtils();

    // Test constants matching COBOL precision requirements
    private static final BigDecimal MINIMUM_PAYMENT_RATE = BigDecimal.valueOf(0.02); // 2% minimum payment
    private static final BigDecimal INTEREST_RATE_MONTHLY = BigDecimal.valueOf(0.0125); // 1.25% monthly
    private static final int PAYMENT_DUE_DAYS = 25; // Payment due in 25 days from statement date
    
    /**
     * Tests getBillDetail() method functionality matching COBOL COBIL00C.cbl main processing.
     * 
     * COBOL Paragraph Mapping: 0000-MAIN-PROCESSING
     * Validates complete bill detail retrieval with account balance, transaction history,
     * interest charges, and payment information display.
     */
    @Test
    @DisplayName("getBillDetail() - Complete bill detail retrieval with COBOL precision")
    void testGetBillDetail_CompleteRetrievalWithCobolPrecision() {
        // Given: Account with transactions and balance from COBOL test vectors
        Account testAccount = createTestAccount();
        testAccount.setAccountId(1000000001L);
        testAccount.setCurrentBalance(BigDecimal.valueOf(1234.56));
        testAccount.setCreditLimit(BigDecimal.valueOf(5000.00));
        
        List<Transaction> testTransactions = List.of(createTestTransaction());
        List<Transaction> paymentHistory = List.of(createTestTransaction());
        
        // Mock repository calls
        when(accountRepository.findById(testAccount.getAccountId())).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountIdAndTransactionTypeAndDateRange(anyLong(), anyString(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(paymentHistory);
        
        // When: Retrieving bill detail
        BillDetailResponse actualResponse = billDetailService.getBillDetail(testAccount.getAccountId());
        
        // Then: Assert complete COBOL functional parity
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getAccountId()).isEqualTo(String.valueOf(testAccount.getAccountId()));
        
        // Validate BigDecimal precision matches COBOL COMP-3
        assertBigDecimalEquals(actualResponse.getCurrentBalance(), testAccount.getCurrentBalance());
        assertBigDecimalEquals(actualResponse.getMinimumPayment(), calculateExpectedMinimumPayment(testAccount.getCurrentBalance()));
        assertBigDecimalEquals(actualResponse.getInterestCharges(), calculateExpectedInterestCharges(testAccount.getCurrentBalance()));
        
        // Validate date calculations
        LocalDate expectedDueDate = LocalDate.now().plusDays(PAYMENT_DUE_DAYS);
        // Adjust for weekends
        while (expectedDueDate.getDayOfWeek().getValue() > 5) {
            expectedDueDate = expectedDueDate.plusDays(1);
        }
        assertThat(actualResponse.getPaymentDueDate()).isEqualTo(expectedDueDate);
        
        // Validate transaction data
        assertThat(actualResponse.getPaymentHistory()).isNotNull();
        
        // Verify repository interactions
        verify(accountRepository, times(1)).findById(testAccount.getAccountId());
        verify(transactionRepository, times(1)).findByAccountIdAndTransactionTypeAndDateRange(anyLong(), anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * Tests calculateInterestCharges() method with COBOL COMP-3 precision validation.
     * 
     * COBOL Paragraph Mapping: 2000-CALCULATE-INTEREST
     * Validates interest calculation using exact BigDecimal arithmetic matching 
     * COBOL packed decimal calculations.
     */
    @Test
    @DisplayName("calculateInterestCharges() - COBOL COMP-3 precision interest calculation")
    void testCalculateInterestCharges_CobolComp3Precision() {
        // Given: Account balance with COBOL-compatible precision
        BigDecimal accountBalance = createValidAmount("1500.75");
        BigDecimal expectedInterest = accountBalance.multiply(INTEREST_RATE_MONTHLY)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        
        // When: Calculating interest charges
        BigDecimal actualInterest = billDetailService.calculateInterestCharges(accountBalance);
        
        // Then: Validate exact precision match with COBOL calculation
        assertBigDecimalEquals(actualInterest, expectedInterest);
        
        // Validate rounding behavior matches COBOL ROUNDED clause
        assertThat(actualInterest.scale()).isEqualTo(2);
        
        // Validate calculation is correct (1500.75 * 0.0125 = 18.76)
        BigDecimal expectedCalculation = new BigDecimal("18.76");
        assertBigDecimalEquals(actualInterest, expectedCalculation);
    }

    /**
     * Tests calculateInterestCharges() with zero balance edge case.
     * 
     * COBOL Paragraph Mapping: 2000-CALCULATE-INTEREST (zero balance condition)
     * Validates interest calculation returns zero for zero balance accounts.
     */
    @Test
    @DisplayName("calculateInterestCharges() - Zero balance returns zero interest")
    void testCalculateInterestCharges_ZeroBalanceReturnsZeroInterest() {
        // Given: Zero account balance
        BigDecimal zeroBalance = BigDecimal.ZERO.setScale(2);
        BigDecimal expectedZeroInterest = BigDecimal.ZERO.setScale(2);
        
        // When: Calculating interest on zero balance
        BigDecimal actualInterest = billDetailService.calculateInterestCharges(zeroBalance);
        
        // Then: Interest should be zero
        assertBigDecimalEquals(actualInterest, expectedZeroInterest);
        assertThat(actualInterest.compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    /**
     * Tests getPaymentHistory() method for payment record retrieval and display.
     * 
     * COBOL Paragraph Mapping: 1000-GET-PAYMENT-HISTORY
     * Validates payment history retrieval with proper date formatting and amount precision.
     */
    @Test
    @DisplayName("getPaymentHistory() - Payment record retrieval with COBOL formatting")
    void testGetPaymentHistory_PaymentRecordRetrievalWithCobolFormatting() {
        // Given: Account ID and expected payment history
        Long accountId = 1000000001L;
        List<Transaction> expectedPaymentHistory = List.of(createTestTransaction());
        
        // Mock repository call
        when(transactionRepository.findByAccountIdAndTransactionTypeAndDateRange(anyLong(), anyString(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(expectedPaymentHistory);
        
        // When: Retrieving payment history
        List<Transaction> actualPaymentHistory = billDetailService.getPaymentHistory(accountId);
        
        // Then: Validate payment history structure and precision
        assertThat(actualPaymentHistory).isNotNull();
        assertThat(actualPaymentHistory).hasSize(expectedPaymentHistory.size());
        
        // Validate each transaction maintains COBOL precision
        for (int i = 0; i < actualPaymentHistory.size(); i++) {
            Transaction actual = actualPaymentHistory.get(i);
            Transaction expected = expectedPaymentHistory.get(i);
            
            assertThat(actual.getTransactionId()).isEqualTo(expected.getTransactionId());
            assertThat(actual.getAccountId()).isEqualTo(expected.getAccountId());
            assertBigDecimalEquals(actual.getAmount(), expected.getAmount());
            // assertDateEquals(actual.getTransactionDate(), expected.getTransactionDate());
            assertThat(actual.getTransactionType()).isEqualTo(expected.getTransactionType());
        }
        
        // Verify repository interaction
        verify(transactionRepository, times(1)).findByAccountIdAndTransactionTypeAndDateRange(anyLong(), anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * Tests calculateMinimumPayment() method with business rule validation.
     * 
     * COBOL Paragraph Mapping: 3000-CALCULATE-MIN-PAYMENT
     * Validates minimum payment calculation using 2% of current balance rule
     * with COBOL rounding precision.
     */
    @Test
    @DisplayName("calculateMinimumPayment() - Business rule validation with COBOL precision")
    void testCalculateMinimumPayment_BusinessRuleValidationWithCobolPrecision() {
        // Given: Account balance for minimum payment calculation
        BigDecimal currentBalance = BigDecimal.valueOf(1500.75);
        BigDecimal expectedMinimum = currentBalance.multiply(MINIMUM_PAYMENT_RATE)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        
        // When: Calculating minimum payment
        BigDecimal actualMinimum = billDetailService.calculateMinimumPayment(currentBalance);
        
        // Then: Validate minimum payment calculation
        assertBigDecimalEquals(actualMinimum, expectedMinimum);
        
        // Validate business rule: minimum payment = 2% of balance
        BigDecimal calculatedMinimum = currentBalance.multiply(MINIMUM_PAYMENT_RATE)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        assertBigDecimalEquals(actualMinimum, calculatedMinimum);
        
        // Validate exact calculation (1500.75 * 0.02 = 30.02)
        BigDecimal expectedCalculation = new BigDecimal("30.02");
        assertBigDecimalEquals(actualMinimum, expectedCalculation);
        
        // Validate precision scale
        assertThat(actualMinimum.scale()).isEqualTo(2);
    }

    /**
     * Tests calculateMinimumPayment() with minimum floor amount validation.
     * 
     * COBOL Paragraph Mapping: 3000-CALCULATE-MIN-PAYMENT (minimum floor logic)
     * Validates minimum payment has a floor amount (e.g., $10.00 minimum).
     */
    @Test
    @DisplayName("calculateMinimumPayment() - Minimum floor amount validation")
    void testCalculateMinimumPayment_MinimumFloorAmountValidation() {
        // Given: Small balance that would result in minimum payment below floor
        BigDecimal smallBalance = new BigDecimal("25.00");
        BigDecimal minimumFloor = new BigDecimal("10.00");
        
        // Calculate 2% of small balance (would be $0.50)
        BigDecimal calculatedMinimum = smallBalance.multiply(MINIMUM_PAYMENT_RATE)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        
        // Expected minimum should be the floor amount with proper scale
        BigDecimal expectedMinimum = minimumFloor; // $10.00 minimum floor with scale 2
        
        // When: Calculating minimum payment for small balance
        BigDecimal actualMinimum = billDetailService.calculateMinimumPayment(smallBalance);
        
        // Then: Minimum should be floor amount, not calculated percentage
        assertBigDecimalEquals(actualMinimum, expectedMinimum);
        assertThat(actualMinimum.compareTo(calculatedMinimum)).isGreaterThan(0);
        
        // Validate exact amounts: 2% of $25.00 = $0.50, but floor is $10.00
        assertThat(calculatedMinimum).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(actualMinimum).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    /**
     * Tests getDueDate() method for payment due date calculation.
     * 
     * COBOL Paragraph Mapping: 4000-CALCULATE-DUE-DATE
     * Validates payment due date calculation based on statement cycle rules.
     */
    @Test
    @DisplayName("getDueDate() - Payment due date calculation with business rules")
    void testGetDueDate_PaymentDueDateCalculationWithBusinessRules() {
        // Given: Statement date for due date calculation (use a Wednesday to avoid weekend issues)
        LocalDate statementDate = LocalDate.of(2024, 1, 3); // Wednesday
        LocalDate expectedDueDate = statementDate.plusDays(PAYMENT_DUE_DAYS).plusDays(1); // Jan 28 (Sunday) adjusted to Jan 29 (Monday)
        
        // When: Calculating payment due date
        LocalDate actualDueDate = billDetailService.getDueDate(statementDate);
        
        // Then: Validate due date calculation
        assertThat(actualDueDate).isEqualTo(expectedDueDate);
        
        // Validate business rule: due date is 25 days from statement date, adjusted for weekends
        LocalDate rawDueDate = statementDate.plusDays(PAYMENT_DUE_DAYS);
        // If raw due date falls on weekend, it should be adjusted to next business day
        assertThat(rawDueDate.getDayOfWeek().getValue()).isGreaterThan(5); // Confirms Sunday (7)
        
        // Ensure it's a weekday (business day)
        assertThat(actualDueDate.getDayOfWeek().getValue()).isLessThanOrEqualTo(5);
    }

    /**
     * Tests getDueDate() with weekend adjustment validation.
     * 
     * COBOL Paragraph Mapping: 4000-CALCULATE-DUE-DATE (weekend adjustment logic)
     * Validates payment due date adjustment when falling on weekends.
     */
    @Test
    @DisplayName("getDueDate() - Weekend adjustment validation")
    void testGetDueDate_WeekendAdjustmentValidation() {
        // Given: Statement date that results in weekend due date
        // Use date that will result in weekend (Jan 1, 2024 + 25 days = Jan 26, 2024 which is Friday)
        // Let's use a date that will land on Saturday
        LocalDate statementDate = LocalDate.of(2024, 1, 2); // Tuesday, Jan 2, 2024 + 25 = Saturday Jan 27
        
        // When: Calculating due date that falls on weekend
        LocalDate actualDueDate = billDetailService.getDueDate(statementDate);
        
        // Then: Due date should be adjusted to next business day
        assertThat(actualDueDate.getDayOfWeek().getValue()).isLessThanOrEqualTo(5);
        
        // Validate that original due date would have been weekend
        LocalDate originalDueDate = statementDate.plusDays(PAYMENT_DUE_DAYS);
        if (originalDueDate.getDayOfWeek().getValue() > 5) {
            // If original was weekend, actual should be later
            assertThat(actualDueDate).isAfter(originalDueDate);
        } else {
            // If original was weekday, dates should match
            assertThat(actualDueDate).isEqualTo(originalDueDate);
        }
    }

    /**
     * Tests itemizeCharges() method for charge breakdown and categorization.
     * 
     * COBOL Paragraph Mapping: 5000-ITEMIZE-CHARGES
     * Validates transaction categorization and charge itemization display.
     */
    @Test
    @DisplayName("itemizeCharges() - Charge breakdown and categorization")
    void testItemizeCharges_ChargeBreakdownAndCategorization() {
        // Given: List of transactions for itemization
        Long accountId = 1000000001L;
        List<Transaction> testTransactions = List.of(createTestTransaction());
        
        // Mock repository call
        when(transactionRepository.findByAccountIdAndTransactionDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(testTransactions);
        
        // When: Itemizing charges for account
        List<Transaction> actualItemizedCharges = billDetailService.itemizeCharges(accountId);
        
        // Then: Validate charge itemization structure
        assertThat(actualItemizedCharges).isNotNull();
        assertThat(actualItemizedCharges).hasSize(testTransactions.size());
        
        // Validate each itemized charge maintains COBOL precision and structure
        for (int i = 0; i < actualItemizedCharges.size(); i++) {
            Transaction actual = actualItemizedCharges.get(i);
            Transaction expected = testTransactions.get(i);
            
            assertThat(actual.getTransactionId()).isEqualTo(expected.getTransactionId());
            assertThat(actual.getAccountId()).isEqualTo(expected.getAccountId());
            assertBigDecimalEquals(actual.getAmount(), expected.getAmount());
            assertThat(actual.getTransactionType()).isEqualTo(expected.getTransactionType());
            assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
        }
        
        // Verify repository interaction
        verify(transactionRepository, times(1)).findByAccountIdAndTransactionDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * Tests itemizeCharges() with empty transaction list handling.
     * 
     * COBOL Paragraph Mapping: 5000-ITEMIZE-CHARGES (no transactions condition)
     * Validates proper handling when no transactions exist for itemization.
     */
    @Test
    @DisplayName("itemizeCharges() - Empty transaction list handling")
    void testItemizeCharges_EmptyTransactionListHandling() {
        // Given: Account with no transactions
        Long accountId = 1000000001L;
        List<Transaction> emptyTransactionList = List.of();
        
        // Mock repository call to return empty list
        when(transactionRepository.findByAccountIdAndTransactionDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(emptyTransactionList);
        
        // When: Itemizing charges for account with no transactions
        List<Transaction> actualItemizedCharges = billDetailService.itemizeCharges(accountId);
        
        // Then: Should return empty list without error
        assertThat(actualItemizedCharges).isNotNull();
        assertThat(actualItemizedCharges).isEmpty();
        
        // Verify repository interaction
        verify(transactionRepository, times(1)).findByAccountIdAndTransactionDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * Tests error handling for invalid account ID scenarios.
     * 
     * COBOL Paragraph Mapping: 9000-ERROR-HANDLING
     * Validates proper exception handling matching COBOL ABEND routines.
     */
    @Test
    @DisplayName("Error Handling - Invalid account ID scenarios")
    void testErrorHandling_InvalidAccountIdScenarios() {
        // Given: Invalid account ID (negative)
        Long invalidAccountId = -1L;
        
        // When/Then: Should throw appropriate exception
        assertThatThrownBy(() -> billDetailService.getBillDetail(invalidAccountId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid account ID");
    }

    /**
     * Tests error handling for null parameter scenarios.
     * 
     * COBOL Paragraph Mapping: 9000-ERROR-HANDLING (null parameter validation)
     * Validates proper null parameter handling with appropriate error messages.
     */
    @Test
    @DisplayName("Error Handling - Null parameter validation")
    void testErrorHandling_NullParameterValidation() {
        // Given: Null account ID parameter
        Long nullAccountId = null;
        
        // When/Then: Should throw appropriate exception for null parameter
        assertThatThrownBy(() -> billDetailService.getBillDetail(nullAccountId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Account ID cannot be null");
    }

    /**
     * Tests comprehensive integration scenario with multiple service method calls.
     * 
     * COBOL Paragraph Mapping: 0000-MAIN-PROCESSING (full integration flow)
     * Validates end-to-end bill detail processing with all components working together.
     */
    @Test
    @DisplayName("Integration Test - Complete bill detail processing flow")
    void testIntegrationTest_CompleteBillDetailProcessingFlow() {
        // Given: Complete test scenario data
        Account testAccount = createTestAccount();
        testAccount.setAccountId(1000000001L);
        testAccount.setCurrentBalance(BigDecimal.valueOf(1234.56));
        testAccount.setCreditLimit(BigDecimal.valueOf(5000.00));
        
        Long accountId = testAccount.getAccountId();
        
        List<Transaction> paymentHistory = List.of(createTestTransaction());
        List<Transaction> itemizedCharges = List.of(createTestTransaction());
        
        // Mock all repository calls needed for the service integration
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountIdAndTransactionTypeAndDateRange(anyLong(), anyString(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(paymentHistory);
        
        // When: Executing complete bill detail processing flow
        BillDetailResponse billDetail = billDetailService.getBillDetail(accountId);
        BigDecimal interest = billDetailService.calculateInterestCharges(testAccount.getCurrentBalance());
        List<Transaction> payments = billDetailService.getPaymentHistory(accountId);
        BigDecimal minPayment = billDetailService.calculateMinimumPayment(testAccount.getCurrentBalance());
        LocalDate paymentDueDate = billDetailService.getDueDate(LocalDate.now());
        List<Transaction> charges = billDetailService.itemizeCharges(accountId);
        
        // Then: Validate complete integration results
        assertThat(billDetail).isNotNull();
        assertBigDecimalEquals(billDetail.getCurrentBalance(), testAccount.getCurrentBalance());
        assertBigDecimalEquals(billDetail.getInterestCharges(), interest);
        assertThat(billDetail.getPaymentHistory()).isEqualTo(payments);
        assertBigDecimalEquals(billDetail.getMinimumPayment(), minPayment);
        assertThat(billDetail.getPaymentDueDate()).isEqualTo(paymentDueDate);
        // Validate payment history (no itemized charges in DTO, only payment history)
        
        // Verify repository interactions
        verify(accountRepository, times(1)).findById(accountId);
        verify(transactionRepository, times(2)).findByAccountIdAndTransactionTypeAndDateRange(anyLong(), anyString(), any(LocalDate.class), any(LocalDate.class));
        verify(transactionRepository, times(1)).findByAccountIdAndTransactionDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class));
    }

    // Private helper methods for test data creation and calculations



    /**
     * Calculates expected interest charges using COBOL-equivalent logic.
     */
    private BigDecimal calculateExpectedInterestCharges(BigDecimal balance) {
        return balance.multiply(INTEREST_RATE_MONTHLY)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Calculates expected minimum payment using business rules.
     */
    private BigDecimal calculateExpectedMinimumPayment(BigDecimal balance) {
        BigDecimal calculatedMinimum = balance.multiply(MINIMUM_PAYMENT_RATE)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal minimumFloor = BigDecimal.valueOf(10.00);
        
        return calculatedMinimum.compareTo(minimumFloor) > 0 ? calculatedMinimum : minimumFloor;
    }
}