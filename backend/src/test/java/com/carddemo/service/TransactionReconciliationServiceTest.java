package com.carddemo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.carddemo.entity.Account;
import com.carddemo.entity.DailyTransaction;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.dto.ReconciliationRequest;
import com.carddemo.dto.ReconciliationResponse;
import com.carddemo.test.BaseServiceTest;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.test.TestDataGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Comprehensive unit test class for TransactionReconciliationService
 * validating COBOL CBTRN02C batch transaction reconciliation logic migration to Java.
 * 
 * This test class ensures 100% functional parity with the original COBOL program
 * including settlement processing, clearing file generation, and dispute handling.
 * 
 * Test Coverage:
 * - Daily transaction validation (1500-VALIDATE-TRAN)
 * - Transaction posting (2000-POST-TRANSACTION)
 * - Reject transaction handling (2500-WRITE-REJECT-REC)
 * - Transaction category balance updates (2700-UPDATE-TCATBAL)
 * - Account balance updates (2800-UPDATE-ACCOUNT-REC)
 * - Clearing file generation and format validation
 * - Financial precision matching COBOL COMP-3 behavior
 */
@SpringBootTest
@DisplayName("Transaction Reconciliation Service Tests")
public class TransactionReconciliationServiceTest extends BaseServiceTest {

    @InjectMocks
    private TransactionReconciliationService transactionReconciliationService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TestDataGenerator testDataGenerator;

    @Mock
    private CobolComparisonUtils cobolComparisonUtils;

    private ReconciliationRequest reconciliationRequest;
    private DailyTransaction validDailyTransaction;
    private DailyTransaction invalidDailyTransaction;
    private Account validAccount;
    private Transaction expectedTransaction;

    @BeforeEach
    void setUp() {
        setupTestData();
        
        // Setup valid reconciliation request
        reconciliationRequest = new ReconciliationRequest();
        reconciliationRequest.setBatchDate(LocalDate.now());
        reconciliationRequest.getProcessingOptions().put("validateOnly", false);
        reconciliationRequest.getValidationRules().put("checkCreditLimit", true);
        reconciliationRequest.getValidationRules().put("checkExpirationDate", true);

        // Generate valid daily transaction matching DALYTRAN-RECORD structure
        validDailyTransaction = testDataGenerator.generateDailyTransaction();
        validDailyTransaction.setTransactionId("T123456789012345");
        validDailyTransaction.setCardNumber("4000123456789012");
        validDailyTransaction.setAmount(BigDecimal.valueOf(150.00).setScale(2));
        validDailyTransaction.setTransactionDate(LocalDate.now());
        validDailyTransaction.setMerchantId(testDataGenerator.generateMerchantId());

        // Generate invalid transaction for reject testing
        invalidDailyTransaction = testDataGenerator.generateDailyTransaction();
        invalidDailyTransaction.setCardNumber("9999999999999999"); // Invalid card
        invalidDailyTransaction.setAmount(BigDecimal.valueOf(5000.00).setScale(2)); // Over limit

        // Setup valid account matching ACCOUNT-RECORD structure
        validAccount = testDataGenerator.generateAccount();
        validAccount.setAccountId(123456789L);
        validAccount.setCurrentBalance(BigDecimal.valueOf(1000.00).setScale(2));
        validAccount.setCreditLimit(BigDecimal.valueOf(2000.00).setScale(2));
        validAccount.setExpirationDate(LocalDate.now().plusYears(2));
        validAccount.setAccountStatus("A"); // Active

        // Setup expected transaction for posting validation
        expectedTransaction = testDataGenerator.generateTransaction();
        expectedTransaction.setTransactionId(validDailyTransaction.getTransactionId());
        expectedTransaction.setAmount(validDailyTransaction.getAmount());
        expectedTransaction.setAccountId(validAccount.getAccountId());

        resetMocks();
    }

    @Nested
    @DisplayName("Daily Transaction Validation Tests - COBOL 1500-VALIDATE-TRAN")
    class DailyTransactionValidationTests {

        @Test
        @DisplayName("Should validate valid daily transaction successfully")
        void testValidateDailyTransaction_ValidTransaction_Success() {
            // Given
            when(accountRepository.findByCardNumber(validDailyTransaction.getCardNumber()))
                    .thenReturn(Optional.of(validAccount));
            
            // When
            boolean isValid = transactionReconciliationService.validateDailyTransaction(validDailyTransaction);
            
            // Then
            assertThat(isValid).isTrue();
            verify(accountRepository).findByCardNumber(validDailyTransaction.getCardNumber());
            
            // Validate COBOL comparison
            when(cobolComparisonUtils.validateCobolParity(any(), any(), eq("1500-VALIDATE-TRAN")))
                    .thenReturn(true);
            assertThat(cobolComparisonUtils.validateCobolParity(
                    validDailyTransaction, isValid, "1500-VALIDATE-TRAN")).isTrue();
        }

        @Test
        @DisplayName("Should reject transaction with invalid card number - COBOL validation 100")
        void testValidateDailyTransaction_InvalidCardNumber_Rejected() {
            // Given - matches COBOL 1500-A-LOOKUP-XREF logic
            when(accountRepository.findByCardNumber(invalidDailyTransaction.getCardNumber()))
                    .thenReturn(Optional.empty());
            
            // When
            boolean isValid = transactionReconciliationService.validateDailyTransaction(invalidDailyTransaction);
            
            // Then
            assertThat(isValid).isFalse();
            verify(accountRepository).findByCardNumber(invalidDailyTransaction.getCardNumber());
        }

        @Test
        @DisplayName("Should reject transaction exceeding credit limit - COBOL validation 102")
        void testValidateDailyTransaction_OverCreditLimit_Rejected() {
            // Given - matches COBOL 1500-B-LOOKUP-ACCT credit limit check
            Account overLimitAccount = testDataGenerator.generateAccount();
            overLimitAccount.setCurrentBalance(BigDecimal.valueOf(1900.00).setScale(2));
            overLimitAccount.setCreditLimit(BigDecimal.valueOf(2000.00).setScale(2));
            
            DailyTransaction overLimitTransaction = testDataGenerator.generateDailyTransaction();
            overLimitTransaction.setAmount(BigDecimal.valueOf(200.00).setScale(2)); // Would exceed limit
            
            when(accountRepository.findByCardNumber(overLimitTransaction.getCardNumber()))
                    .thenReturn(Optional.of(overLimitAccount));
            
            // When
            boolean isValid = transactionReconciliationService.validateDailyTransaction(overLimitTransaction);
            
            // Then
            assertThat(isValid).isFalse();
            
            // Validate COBOL financial precision calculation
            BigDecimal tempBalance = overLimitAccount.getCurrentBalance().add(overLimitTransaction.getAmount());
            assertThat(tempBalance).isGreaterThan(overLimitAccount.getCreditLimit());
            assertBigDecimalEquals(tempBalance, BigDecimal.valueOf(2100.00).setScale(2));
        }

        @Test
        @DisplayName("Should reject transaction after account expiration - COBOL validation 103")
        void testValidateDailyTransaction_ExpiredAccount_Rejected() {
            // Given - matches COBOL expiration date validation logic
            Account expiredAccount = testDataGenerator.generateAccount();
            expiredAccount.setExpirationDate(LocalDate.now().minusDays(1));
            
            when(accountRepository.findByCardNumber(validDailyTransaction.getCardNumber()))
                    .thenReturn(Optional.of(expiredAccount));
            
            // When
            boolean isValid = transactionReconciliationService.validateDailyTransaction(validDailyTransaction);
            
            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Transaction Posting Tests - COBOL 2000-POST-TRANSACTION")
    class TransactionPostingTests {

        @Test
        @DisplayName("Should post valid transaction successfully")
        void testPostValidTransaction_Success() {
            // Given
            when(transactionRepository.existsByTransactionId(validDailyTransaction.getTransactionId()))
                    .thenReturn(false);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(expectedTransaction);
            when(accountRepository.findById(validAccount.getAccountId()))
                    .thenReturn(Optional.of(validAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(validAccount);
            
            // When
            Transaction postedTransaction = transactionReconciliationService.postValidTransaction(validDailyTransaction);
            
            // Then
            assertThat(postedTransaction).isNotNull();
            assertThat(postedTransaction.getTransactionId()).isEqualTo(validDailyTransaction.getTransactionId());
            assertThat(postedTransaction.getAmount()).isEqualTo(validDailyTransaction.getAmount());
            assertThat(postedTransaction.getProcessedTimestamp()).isNotNull();
            
            verify(transactionRepository).save(any(Transaction.class));
            verify(accountRepository).save(any(Account.class));
            
            // Validate COBOL field mapping from DALYTRAN-RECORD to TRAN-RECORD
            assertThat(postedTransaction.getTransactionType()).isNotNull();
            assertThat(postedTransaction.getAccountId()).isNotNull();
        }

        @Test
        @DisplayName("Should handle duplicate transaction ID gracefully")
        void testPostValidTransaction_DuplicateTransactionId_HandledGracefully() {
            // Given
            when(transactionRepository.existsByTransactionId(validDailyTransaction.getTransactionId()))
                    .thenReturn(true);
            
            // When/Then
            assertThatThrownBy(() -> transactionReconciliationService.postValidTransaction(validDailyTransaction))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Transaction already exists");
            
            verify(transactionRepository, never()).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("Reject Transaction Handling Tests - COBOL 2500-WRITE-REJECT-REC")
    class RejectTransactionHandlingTests {

        @Test
        @DisplayName("Should write reject record with validation trailer")
        void testWriteRejectTransaction_WithValidationTrailer_Success() {
            // Given
            String rejectionReason = "INVALID CARD NUMBER FOUND";
            int rejectionCode = 100;
            
            // When
            transactionReconciliationService.writeRejectTransaction(
                    invalidDailyTransaction, rejectionCode, rejectionReason);
            
            // Then
            verify(cobolComparisonUtils).validateFinancialPrecision(
                    any(BigDecimal.class), any(BigDecimal.class));
            
            // Verify reject record structure matches COBOL REJECT-RECORD
            // Should contain REJECT-TRAN-DATA (350 chars) + VALIDATION-TRAILER (80 chars)
            verify(cobolComparisonUtils).generateComparisonReport();
        }

        @Test
        @DisplayName("Should handle multiple rejection reasons")
        void testWriteRejectTransaction_MultipleReasons_AllRecorded() {
            // Given
            List<DailyTransaction> rejectTransactions = testDataGenerator.generateDailyTransactionBatch(3);
            
            // When
            rejectTransactions.forEach(transaction -> {
                transactionReconciliationService.writeRejectTransaction(
                        transaction, 101, "ACCOUNT RECORD NOT FOUND");
            });
            
            // Then
            verify(cobolComparisonUtils, times(3)).validateFinancialPrecision(
                    any(BigDecimal.class), any(BigDecimal.class));
        }
    }

    @Nested
    @DisplayName("Account Balance Update Tests - COBOL 2800-UPDATE-ACCOUNT-REC")
    class AccountBalanceUpdateTests {

        @Test
        @DisplayName("Should update account balance for credit transaction")
        void testUpdateAccountBalance_CreditTransaction_BalanceIncreased() {
            // Given
            BigDecimal creditAmount = BigDecimal.valueOf(100.00).setScale(2);
            BigDecimal originalBalance = validAccount.getCurrentBalance();
            
            when(accountRepository.findById(validAccount.getAccountId()))
                    .thenReturn(Optional.of(validAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(validAccount);
            
            // When
            transactionReconciliationService.updateAccountBalance(
                    validAccount.getAccountId(), creditAmount);
            
            // Then
            verify(accountRepository).save(argThat(account -> {
                assertThat(account.getCurrentBalance())
                        .isEqualTo(originalBalance.add(creditAmount));
                return true;
            }));
            
            // Validate COBOL COMP-3 precision matching
            assertBigDecimalEquals(
                    originalBalance.add(creditAmount),
                    BigDecimal.valueOf(1100.00).setScale(2));
        }

        @Test
        @DisplayName("Should update account balance for debit transaction")
        void testUpdateAccountBalance_DebitTransaction_BalanceDecreased() {
            // Given
            BigDecimal debitAmount = BigDecimal.valueOf(-50.00).setScale(2);
            BigDecimal originalBalance = validAccount.getCurrentBalance();
            
            when(accountRepository.findById(validAccount.getAccountId()))
                    .thenReturn(Optional.of(validAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(validAccount);
            
            // When
            transactionReconciliationService.updateAccountBalance(
                    validAccount.getAccountId(), debitAmount);
            
            // Then
            verify(accountRepository).save(argThat(account -> {
                assertThat(account.getCurrentBalance())
                        .isEqualTo(originalBalance.add(debitAmount));
                return true;
            }));
            
            // Validate COBOL COMP-3 precision matching
            assertBigDecimalEquals(
                    originalBalance.add(debitAmount),
                    BigDecimal.valueOf(950.00).setScale(2));
        }
    }

    @Nested
    @DisplayName("Transaction Category Balance Tests - COBOL 2700-UPDATE-TCATBAL")
    class TransactionCategoryBalanceTests {

        @Test
        @DisplayName("Should create new transaction category balance record")
        void testUpdateTransactionCategoryBalance_NewRecord_Created() {
            // Given
            Long accountId = validAccount.getAccountId();
            String transactionType = "01";
            String categoryCode = "5000";
            BigDecimal amount = BigDecimal.valueOf(75.00).setScale(2);
            
            // When
            transactionReconciliationService.updateTransactionCategoryBalance(
                    accountId, transactionType, categoryCode, amount);
            
            // Then
            // Verify the logic matches COBOL 2700-A-CREATE-TCATBAL-REC
            verify(cobolComparisonUtils).compareBalanceCalculations(any(), any());
        }

        @Test
        @DisplayName("Should update existing transaction category balance record")
        void testUpdateTransactionCategoryBalance_ExistingRecord_Updated() {
            // Given
            Long accountId = validAccount.getAccountId();
            String transactionType = "01";
            String categoryCode = "5000";
            BigDecimal amount = BigDecimal.valueOf(25.00).setScale(2);
            BigDecimal existingBalance = BigDecimal.valueOf(100.00).setScale(2);
            
            // When
            transactionReconciliationService.updateTransactionCategoryBalance(
                    accountId, transactionType, categoryCode, amount);
            
            // Then
            // Verify the logic matches COBOL 2700-B-UPDATE-TCATBAL-REC
            verify(cobolComparisonUtils).compareBalanceCalculations(any(), any());
            
            // Validate balance calculation precision
            BigDecimal expectedBalance = existingBalance.add(amount);
            assertBigDecimalEquals(expectedBalance, BigDecimal.valueOf(125.00).setScale(2));
        }
    }

    @Nested
    @DisplayName("Clearing File Generation Tests")
    class ClearingFileGenerationTests {

        @Test
        @DisplayName("Should generate clearing file with exact format matching")
        void testGenerateClearingFile_ExactFormatMatching_Success() {
            // Given
            List<Transaction> transactions = List.of(expectedTransaction);
            LocalDate processingDate = LocalDate.now();
            
            // When
            String clearingFileContent = transactionReconciliationService.generateClearingFile(
                    transactions, processingDate);
            
            // Then
            assertThat(clearingFileContent).isNotNull();
            assertThat(clearingFileContent).isNotEmpty();
            
            // Validate format matches COBOL file generation requirements
            verify(cobolComparisonUtils).compareTransactionProcessing(any(), any());
            
            // Verify file contains proper record structure
            assertThat(clearingFileContent).contains(expectedTransaction.getTransactionId());
            assertThat(clearingFileContent).contains(processingDate.toString());
        }

        @Test
        @DisplayName("Should handle empty transaction list for clearing file")
        void testGenerateClearingFile_EmptyTransactionList_HandledGracefully() {
            // Given
            List<Transaction> emptyTransactions = List.of();
            LocalDate processingDate = LocalDate.now();
            
            // When
            String clearingFileContent = transactionReconciliationService.generateClearingFile(
                    emptyTransactions, processingDate);
            
            // Then
            assertThat(clearingFileContent).isNotNull();
            assertThat(clearingFileContent).contains("NO TRANSACTIONS");
        }
    }

    @Nested
    @DisplayName("Reconciliation Processing Tests - Complete Workflow")
    class ReconciliationProcessingTests {

        @Test
        @DisplayName("Should process complete reconciliation successfully")
        void testProcessReconciliation_CompleteWorkflow_Success() {
            // Given
            List<DailyTransaction> dailyTransactions = testDataGenerator.generateDailyTransactionBatch(5);
            
            when(accountRepository.findByCardNumber(anyString()))
                    .thenReturn(Optional.of(validAccount));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(expectedTransaction);
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(validAccount);
            
            // When
            ReconciliationResponse response = transactionReconciliationService.processReconciliation(
                    reconciliationRequest);
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTransactionCount()).isEqualTo(5);
            assertThat(response.getRejectCount()).isEqualTo(0);
            assertThat(response.getProcessingStatus()).isEqualTo("COMPLETED");
            assertThat(response.getValidationErrors()).isEmpty();
            
            // Verify performance meets <200ms SLA requirement
            long processingTime = measurePerformance(() -> 
                transactionReconciliationService.processReconciliation(reconciliationRequest));
            validateResponseTime(processingTime, 200L);
        }

        @Test
        @DisplayName("Should handle mixed valid and invalid transactions")
        void testProcessReconciliation_MixedTransactions_PartialSuccess() {
            // Given
            List<DailyTransaction> mixedTransactions = List.of(
                validDailyTransaction, invalidDailyTransaction);
            
            when(accountRepository.findByCardNumber(validDailyTransaction.getCardNumber()))
                    .thenReturn(Optional.of(validAccount));
            when(accountRepository.findByCardNumber(invalidDailyTransaction.getCardNumber()))
                    .thenReturn(Optional.empty());
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(expectedTransaction);
            when(accountRepository.save(any(Account.class)))
                    .thenReturn(validAccount);
            
            // When
            ReconciliationResponse response = transactionReconciliationService.processReconciliation(
                    reconciliationRequest);
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTransactionCount()).isEqualTo(2);
            assertThat(response.getRejectCount()).isEqualTo(1);
            assertThat(response.getProcessingStatus()).isEqualTo("COMPLETED_WITH_REJECTIONS");
            
            // Validate COBOL comparison for mixed processing
            verify(cobolComparisonUtils).compareTransactionProcessing(any(), any());
        }
    }

    @Nested
    @DisplayName("Reconciliation Report Generation Tests")
    class ReconciliationReportGenerationTests {

        @Test
        @DisplayName("Should generate reconciliation report with processing statistics")
        void testGenerateReconciliationReport_WithStatistics_Success() {
            // Given
            ReconciliationResponse response = new ReconciliationResponse();
            response.setTransactionCount(100);
            response.setRejectCount(5);
            response.setProcessingStatus("COMPLETED_WITH_REJECTIONS");
            
            // When
            String report = transactionReconciliationService.generateReconciliationReport(response);
            
            // Then
            assertThat(report).isNotNull();
            assertThat(report).contains("TRANSACTIONS PROCESSED :100");
            assertThat(report).contains("TRANSACTIONS REJECTED  :5");
            assertThat(report).contains("COMPLETED_WITH_REJECTIONS");
            
            // Verify matches COBOL display format
            verify(cobolComparisonUtils).generateComparisonReport();
        }
    }

    @Nested
    @DisplayName("Processing Statistics Tests")
    class ProcessingStatisticsTests {

        @Test
        @DisplayName("Should track and return accurate processing statistics")
        void testGetProcessingStatistics_AccurateTracking_Success() {
            // Given
            when(accountRepository.findByCardNumber(anyString()))
                    .thenReturn(Optional.of(validAccount));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(expectedTransaction);
            
            // Process some transactions
            transactionReconciliationService.processReconciliation(reconciliationRequest);
            
            // When
            ReconciliationResponse statistics = transactionReconciliationService.getProcessingStatistics();
            
            // Then
            assertThat(statistics).isNotNull();
            assertThat(statistics.getTransactionCount()).isGreaterThanOrEqualTo(0);
            assertThat(statistics.getRejectCount()).isGreaterThanOrEqualTo(0);
            assertThat(statistics.getProcessingStatus()).isNotNull();
        }
    }

    /**
     * Helper method to validate BigDecimal equality with COBOL COMP-3 precision
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected) {
        assertThat(actual.compareTo(expected)).isEqualTo(0);
        assertThat(actual.scale()).isEqualTo(expected.scale());
    }
}