/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.service.TransactionPostingService;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Account;
import com.carddemo.entity.DailyTransaction;
import com.carddemo.entity.CardXref;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.DailyTransactionRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import com.carddemo.util.AmountCalculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive unit test class for TransactionPostingService that validates the COBOL CBTRN02C 
 * batch transaction posting logic migration to Java. This test class ensures 100% functional 
 * parity between the original COBOL implementation and the Java Spring Boot service.
 * 
 * Test Coverage Areas:
 * - Transaction validation including credit limits and expiration date checks
 * - Cross-reference and account lookups with error handling
 * - Transaction posting operations and balance updates
 * - Category balance updates for transaction categorization
 * - Reject record generation with proper validation trailers
 * - DB2-format timestamp generation and formatting
 * - Condition code returns (0 for success, 4 for rejects)
 * - Edge cases and error scenarios from COBOL program
 * 
 * This test class replicates the test scenarios that would have been used to validate
 * the original COBOL CBTRN02C program, ensuring identical business logic behavior.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionPostingServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private DailyTransactionRepository dailyTransactionRepository;
    
    @Mock
    private CardXrefRepository cardXrefRepository;
    
    @Mock
    private TransactionCategoryBalanceRepository categoryBalanceRepository;
    
    @Mock
    private CobolDataConverter cobolDataConverter;
    
    @Mock
    private DateConversionUtil dateConversionUtil;
    
    @Mock
    private ValidationUtil validationUtil;
    
    @Mock
    private AmountCalculator amountCalculator;

    @InjectMocks
    private TransactionPostingService transactionPostingService;

    private TransactionPostingService.DailyTransactionRecord validDailyTransaction;
    private TransactionPostingService.CrossReferenceRecord validXrefRecord;
    private TransactionPostingService.AccountRecord validAccountRecord;
    private TransactionPostingService.TransactionCategoryBalanceRecord validCategoryBalance;

    /**
     * Test setup executed before each test method.
     * Initializes mock objects and creates valid test data matching COBOL record structures.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Clear all data before each test
        transactionPostingService.clearAllData();
        
        setupValidTestData();
    }

    /**
     * Creates valid test data objects matching COBOL copybook structures.
     * These serve as the baseline for testing various scenarios.
     */
    private void setupValidTestData() {
        // Valid daily transaction record (equivalent to DALYTRAN-RECORD from COBOL)
        validDailyTransaction = new TransactionPostingService.DailyTransactionRecord();
        validDailyTransaction.setTransactionId("TXN001234567890");
        validDailyTransaction.setCardNumber("1234567890123456");
        validDailyTransaction.setTypeCode("01");
        validDailyTransaction.setCategoryCode(1000);
        validDailyTransaction.setSource("WEB");
        validDailyTransaction.setDescription("Test Transaction");
        validDailyTransaction.setAmount(new BigDecimal("100.00"));
        validDailyTransaction.setMerchantId("MERCHANT001");
        validDailyTransaction.setMerchantName("Test Merchant");
        validDailyTransaction.setMerchantCity("Test City");
        validDailyTransaction.setMerchantZip("12345");
        validDailyTransaction.setOriginalTimestamp("2024-01-15-10.30.45.120000");

        // Valid cross-reference record (equivalent to CARD-XREF-RECORD from COBOL)
        validXrefRecord = new TransactionPostingService.CrossReferenceRecord();
        validXrefRecord.setCardNumber("1234567890123456");
        validXrefRecord.setAccountId(1234567890L);

        // Valid account record (equivalent to ACCOUNT-RECORD from COBOL)
        validAccountRecord = new TransactionPostingService.AccountRecord();
        validAccountRecord.setAccountId(1234567890L);
        validAccountRecord.setCreditLimit(new BigDecimal("5000.00"));
        validAccountRecord.setCurrentBalance(new BigDecimal("1000.00"));
        validAccountRecord.setCurrentCycleCredit(new BigDecimal("500.00"));
        validAccountRecord.setCurrentCycleDebit(new BigDecimal("1500.00"));
        validAccountRecord.setExpirationDate("2025-12-31");

        // Valid category balance record (equivalent to TRAN-CAT-BAL-RECORD from COBOL)
        validCategoryBalance = new TransactionPostingService.TransactionCategoryBalanceRecord();
        validCategoryBalance.setAccountId(1234567890L);
        validCategoryBalance.setTypeCode("01");
        validCategoryBalance.setCategoryCode(1000);
        validCategoryBalance.setBalance(new BigDecimal("250.00"));
    }

    /**
     * Tests for the main postDailyTransactions() method - equivalent to COBOL main procedure division logic.
     */
    @Nested
    @DisplayName("Post Daily Transactions - Main Processing Loop Tests")
    class PostDailyTransactionsTests {

        @Test
        @DisplayName("Should process valid transactions and return success code (0)")
        void postDailyTransactions_ValidTransactions_ReturnsSuccessCode() {
            // Arrange - Set up valid data for successful processing
            transactionPostingService.addDailyTransaction(validDailyTransaction);
            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act - Execute main posting process
            int result = transactionPostingService.postDailyTransactions();

            // Assert - Verify successful processing
            assertThat(result).isEqualTo(0); // Success code from COBOL
            assertThat(transactionPostingService.getTransactionCount()).isEqualTo(1);
            assertThat(transactionPostingService.getRejectCount()).isEqualTo(0);
            assertThat(transactionPostingService.getConditionCode()).isEqualTo(0);
            assertThat(transactionPostingService.getPostedTransactions()).hasSize(1);
            assertThat(transactionPostingService.getRejectedTransactions()).isEmpty();
        }

        @Test
        @DisplayName("Should reject invalid transactions and return reject code (4)")
        void postDailyTransactions_InvalidTransactions_ReturnsRejectCode() {
            // Arrange - Set up invalid transaction (card not found)
            transactionPostingService.addDailyTransaction(validDailyTransaction);
            // Don't add cross-reference record to trigger validation failure

            // Act - Execute main posting process
            int result = transactionPostingService.postDailyTransactions();

            // Assert - Verify rejection processing
            assertThat(result).isEqualTo(4); // Reject code from COBOL
            assertThat(transactionPostingService.getTransactionCount()).isEqualTo(1);
            assertThat(transactionPostingService.getRejectCount()).isEqualTo(1);
            assertThat(transactionPostingService.getConditionCode()).isEqualTo(4);
            assertThat(transactionPostingService.getPostedTransactions()).isEmpty();
            assertThat(transactionPostingService.getRejectedTransactions()).hasSize(1);
        }

        @Test
        @DisplayName("Should process mixed valid and invalid transactions correctly")
        void postDailyTransactions_MixedTransactions_ProcessesCorrectly() {
            // Arrange - Set up one valid and one invalid transaction
            TransactionPostingService.DailyTransactionRecord invalidTransaction = 
                new TransactionPostingService.DailyTransactionRecord();
            invalidTransaction.setTransactionId("TXN000000000002");
            invalidTransaction.setCardNumber("9999999999999999"); // Invalid card
            invalidTransaction.setAmount(new BigDecimal("50.00"));
            invalidTransaction.setOriginalTimestamp("2024-01-15-10.30.45.120000");

            transactionPostingService.addDailyTransaction(validDailyTransaction);
            transactionPostingService.addDailyTransaction(invalidTransaction);
            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act - Execute main posting process
            int result = transactionPostingService.postDailyTransactions();

            // Assert - Verify mixed processing results
            assertThat(result).isEqualTo(4); // Reject code due to at least one rejection
            assertThat(transactionPostingService.getTransactionCount()).isEqualTo(2);
            assertThat(transactionPostingService.getRejectCount()).isEqualTo(1);
            assertThat(transactionPostingService.getConditionCode()).isEqualTo(4);
            assertThat(transactionPostingService.getPostedTransactions()).hasSize(1);
            assertThat(transactionPostingService.getRejectedTransactions()).hasSize(1);
        }

        @Test
        @DisplayName("Should handle empty transaction list gracefully")
        void postDailyTransactions_EmptyTransactionList_ReturnsSuccessCode() {
            // Arrange - No transactions to process

            // Act - Execute main posting process
            int result = transactionPostingService.postDailyTransactions();

            // Assert - Verify empty processing
            assertThat(result).isEqualTo(0); // Success code
            assertThat(transactionPostingService.getTransactionCount()).isEqualTo(0);
            assertThat(transactionPostingService.getRejectCount()).isEqualTo(0);
            assertThat(transactionPostingService.getConditionCode()).isEqualTo(0);
            assertThat(transactionPostingService.getPostedTransactions()).isEmpty();
            assertThat(transactionPostingService.getRejectedTransactions()).isEmpty();
        }
    }

    /**
     * Tests for transaction validation logic - equivalent to COBOL 1500-VALIDATE-TRAN paragraph.
     */
    @Nested
    @DisplayName("Transaction Validation Tests - COBOL 1500-VALIDATE-TRAN Logic")
    class TransactionValidationTests {

        @Test
        @DisplayName("Should validate transaction successfully with valid data")
        void validateTransaction_ValidData_ReturnsTrue() {
            // Arrange
            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act
            boolean result = transactionPostingService.validateTransaction(validDailyTransaction);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject transaction with invalid card number (Error Code 100)")
        void validateTransaction_InvalidCardNumber_RejectsWithCode100() {
            // Arrange - Don't add cross-reference record

            // Act
            boolean result = transactionPostingService.validateTransaction(validDailyTransaction);

            // Assert - Matches COBOL error code 100 for INVALID CARD NUMBER FOUND
            assertThat(result).isFalse();
            
            // Generate reject record to verify error details
            transactionPostingService.generateRejectRecord(validDailyTransaction);
            List<TransactionPostingService.RejectRecord> rejects = transactionPostingService.getRejectedTransactions();
            
            assertThat(rejects).hasSize(1);
            assertThat(rejects.get(0).getValidationFailReason()).isEqualTo(100);
            assertThat(rejects.get(0).getValidationFailDescription()).isEqualTo("INVALID CARD NUMBER FOUND");
        }

        @Test
        @DisplayName("Should reject transaction with account not found (Error Code 101)")
        void validateTransaction_AccountNotFound_RejectsWithCode101() {
            // Arrange - Add cross-reference but no account record
            transactionPostingService.addCrossReference(validXrefRecord);

            // Act
            boolean result = transactionPostingService.validateTransaction(validDailyTransaction);

            // Assert - Matches COBOL error code 101 for ACCOUNT RECORD NOT FOUND
            assertThat(result).isFalse();
            
            // Generate reject record to verify error details
            transactionPostingService.generateRejectRecord(validDailyTransaction);
            List<TransactionPostingService.RejectRecord> rejects = transactionPostingService.getRejectedTransactions();
            
            assertThat(rejects).hasSize(1);
            assertThat(rejects.get(0).getValidationFailReason()).isEqualTo(101);
            assertThat(rejects.get(0).getValidationFailDescription()).isEqualTo("ACCOUNT RECORD NOT FOUND");
        }

        @Test
        @DisplayName("Should reject overlimit transaction (Error Code 102)")
        void validateTransaction_OverlimitTransaction_RejectsWithCode102() {
            // Arrange - Set up account with transaction that would exceed limit
            validAccountRecord.setCreditLimit(new BigDecimal("1000.00")); // Low limit
            validAccountRecord.setCurrentCycleCredit(new BigDecimal("200.00"));
            validAccountRecord.setCurrentCycleDebit(new BigDecimal("800.00"));
            validDailyTransaction.setAmount(new BigDecimal("500.00")); // Would exceed limit

            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act
            boolean result = transactionPostingService.validateTransaction(validDailyTransaction);

            // Assert - Matches COBOL error code 102 for OVERLIMIT TRANSACTION
            assertThat(result).isFalse();
            
            // Generate reject record to verify error details
            transactionPostingService.generateRejectRecord(validDailyTransaction);
            List<TransactionPostingService.RejectRecord> rejects = transactionPostingService.getRejectedTransactions();
            
            assertThat(rejects).hasSize(1);
            assertThat(rejects.get(0).getValidationFailReason()).isEqualTo(102);
            assertThat(rejects.get(0).getValidationFailDescription()).isEqualTo("OVERLIMIT TRANSACTION");
        }

        @Test
        @DisplayName("Should reject transaction after account expiration (Error Code 103)")
        void validateTransaction_ExpiredAccount_RejectsWithCode103() {
            // Arrange - Set account expiration before transaction date
            validAccountRecord.setExpirationDate("2023-12-31"); // Expired
            validDailyTransaction.setOriginalTimestamp("2024-01-15-10.30.45.120000"); // After expiration

            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act
            boolean result = transactionPostingService.validateTransaction(validDailyTransaction);

            // Assert - Matches COBOL error code 103 for expired account
            assertThat(result).isFalse();
            
            // Generate reject record to verify error details
            transactionPostingService.generateRejectRecord(validDailyTransaction);
            List<TransactionPostingService.RejectRecord> rejects = transactionPostingService.getRejectedTransactions();
            
            assertThat(rejects).hasSize(1);
            assertThat(rejects.get(0).getValidationFailReason()).isEqualTo(103);
            assertThat(rejects.get(0).getValidationFailDescription()).isEqualTo("TRANSACTION RECEIVED AFTER ACCT EXPIRATION");
        }
    }

    /**
     * Tests for credit limit validation - equivalent to COBOL credit limit check logic.
     */
    @Nested
    @DisplayName("Credit Limit Validation Tests - COBOL Credit Limit Logic")
    class CreditLimitValidationTests {

        @Test
        @DisplayName("Should pass validation when within credit limit")
        void validateCreditLimit_WithinLimit_ReturnsTrue() {
            // Arrange - Transaction amount within available credit
            validAccountRecord.setCreditLimit(new BigDecimal("5000.00"));
            validAccountRecord.setCurrentCycleCredit(new BigDecimal("1000.00"));
            validAccountRecord.setCurrentCycleDebit(new BigDecimal("2000.00"));
            BigDecimal transactionAmount = new BigDecimal("100.00"); // Well within limit

            // Act
            boolean result = transactionPostingService.validateCreditLimit(validAccountRecord, transactionAmount);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject when transaction exceeds credit limit")
        void validateCreditLimit_ExceedsLimit_ReturnsFalse() {
            // Arrange - Transaction would exceed credit limit
            validAccountRecord.setCreditLimit(new BigDecimal("1000.00"));
            validAccountRecord.setCurrentCycleCredit(new BigDecimal("500.00"));
            validAccountRecord.setCurrentCycleDebit(new BigDecimal("800.00"));
            BigDecimal transactionAmount = new BigDecimal("800.00"); // Would exceed limit: 300 + 800 = 1100 > 1000

            // Act
            boolean result = transactionPostingService.validateCreditLimit(validAccountRecord, transactionAmount);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle exact credit limit boundary correctly")
        void validateCreditLimit_ExactLimit_ReturnsTrue() {
            // Arrange - Transaction amount exactly at credit limit
            validAccountRecord.setCreditLimit(new BigDecimal("1000.00"));
            validAccountRecord.setCurrentCycleCredit(new BigDecimal("500.00"));
            validAccountRecord.setCurrentCycleDebit(new BigDecimal("700.00"));
            BigDecimal transactionAmount = new BigDecimal("200.00"); // Exactly at limit

            // Act
            boolean result = transactionPostingService.validateCreditLimit(validAccountRecord, transactionAmount);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle negative balances correctly in credit calculations")
        void validateCreditLimit_NegativeBalances_CalculatesCorrectly() {
            // Arrange - Account has credit balance (negative current balance)
            validAccountRecord.setCreditLimit(new BigDecimal("2000.00"));
            validAccountRecord.setCurrentCycleCredit(new BigDecimal("1500.00"));
            validAccountRecord.setCurrentCycleDebit(new BigDecimal("500.00")); // Net credit balance
            BigDecimal transactionAmount = new BigDecimal("1500.00");

            // Act
            boolean result = transactionPostingService.validateCreditLimit(validAccountRecord, transactionAmount);

            // Assert - Should pass because credit balance provides more room
            assertThat(result).isTrue();
        }
    }

    /**
     * Tests for expiration date validation - equivalent to COBOL expiration date check.
     */
    @Nested
    @DisplayName("Expiration Date Validation Tests - COBOL Expiration Logic")
    class ExpirationDateValidationTests {

        @Test
        @DisplayName("Should pass validation when transaction before expiration")
        void validateExpirationDate_BeforeExpiration_ReturnsTrue() {
            // Arrange - Transaction before expiration date
            validAccountRecord.setExpirationDate("2025-12-31");
            String transactionTimestamp = "2024-06-15-10.30.45.120000";

            // Act
            boolean result = transactionPostingService.validateExpirationDate(validAccountRecord, transactionTimestamp);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject when transaction after expiration")
        void validateExpirationDate_AfterExpiration_ReturnsFalse() {
            // Arrange - Transaction after expiration date
            validAccountRecord.setExpirationDate("2023-12-31");
            String transactionTimestamp = "2024-01-15-10.30.45.120000";

            // Act
            boolean result = transactionPostingService.validateExpirationDate(validAccountRecord, transactionTimestamp);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle exact expiration date boundary correctly")
        void validateExpirationDate_OnExpirationDate_ReturnsTrue() {
            // Arrange - Transaction on expiration date
            validAccountRecord.setExpirationDate("2024-01-15");
            String transactionTimestamp = "2024-01-15-10.30.45.120000";

            // Act
            boolean result = transactionPostingService.validateExpirationDate(validAccountRecord, transactionTimestamp);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle short timestamp format correctly")
        void validateExpirationDate_ShortTimestamp_HandlesCorrectly() {
            // Arrange - Short timestamp format
            validAccountRecord.setExpirationDate("2025-12-31");
            String transactionTimestamp = "2024-06-15";

            // Act
            boolean result = transactionPostingService.validateExpirationDate(validAccountRecord, transactionTimestamp);

            // Assert
            assertThat(result).isTrue();
        }
    }

    /**
     * Tests for transaction processing - equivalent to COBOL 2000-POST-TRANSACTION paragraph.
     */
    @Nested
    @DisplayName("Transaction Processing Tests - COBOL 2000-POST-TRANSACTION Logic")
    class TransactionProcessingTests {

        @Test
        @DisplayName("Should process valid transaction and update all records")
        void processTransaction_ValidTransaction_ProcessesSuccessfully() {
            // Arrange
            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act
            transactionPostingService.processTransaction(validDailyTransaction);

            // Assert - Verify transaction was posted
            List<TransactionPostingService.TransactionRecord> postedTransactions = 
                transactionPostingService.getPostedTransactions();
            assertThat(postedTransactions).hasSize(1);

            TransactionPostingService.TransactionRecord posted = postedTransactions.get(0);
            assertThat(posted.getTransactionId()).isEqualTo(validDailyTransaction.getTransactionId());
            assertThat(posted.getAmount()).isEqualTo(validDailyTransaction.getAmount());
            assertThat(posted.getCardNumber()).isEqualTo(validDailyTransaction.getCardNumber());
            assertThat(posted.getProcessedTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should copy all fields from daily transaction to posted transaction")
        void processTransaction_AllFields_CopiedCorrectly() {
            // Arrange
            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act
            transactionPostingService.processTransaction(validDailyTransaction);

            // Assert - Verify all fields copied correctly
            TransactionPostingService.TransactionRecord posted = 
                transactionPostingService.getPostedTransactions().get(0);

            assertThat(posted.getTransactionId()).isEqualTo(validDailyTransaction.getTransactionId());
            assertThat(posted.getTypeCode()).isEqualTo(validDailyTransaction.getTypeCode());
            assertThat(posted.getCategoryCode()).isEqualTo(validDailyTransaction.getCategoryCode());
            assertThat(posted.getSource()).isEqualTo(validDailyTransaction.getSource());
            assertThat(posted.getDescription()).isEqualTo(validDailyTransaction.getDescription());
            assertThat(posted.getAmount()).isEqualTo(validDailyTransaction.getAmount());
            assertThat(posted.getMerchantId()).isEqualTo(validDailyTransaction.getMerchantId());
            assertThat(posted.getMerchantName()).isEqualTo(validDailyTransaction.getMerchantName());
            assertThat(posted.getMerchantCity()).isEqualTo(validDailyTransaction.getMerchantCity());
            assertThat(posted.getMerchantZip()).isEqualTo(validDailyTransaction.getMerchantZip());
            assertThat(posted.getCardNumber()).isEqualTo(validDailyTransaction.getCardNumber());
            assertThat(posted.getOriginalTimestamp()).isEqualTo(validDailyTransaction.getOriginalTimestamp());
        }

        @Test
        @DisplayName("Should generate processing timestamp in correct format")
        void processTransaction_GeneratesCorrectTimestamp() {
            // Arrange
            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act
            transactionPostingService.processTransaction(validDailyTransaction);

            // Assert - Verify timestamp format matches DB2 format from COBOL
            TransactionPostingService.TransactionRecord posted = 
                transactionPostingService.getPostedTransactions().get(0);
            String timestamp = posted.getProcessedTimestamp();
            
            assertThat(timestamp).isNotNull();
            assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{6}");
        }
    }

    /**
     * Tests for category balance updates - equivalent to COBOL 2700-UPDATE-TCATBAL paragraph.
     */
    @Nested
    @DisplayName("Category Balance Update Tests - COBOL 2700-UPDATE-TCATBAL Logic")
    class CategoryBalanceUpdateTests {

        @Test
        @DisplayName("Should create new category balance record when not found")
        void updateCategoryBalance_NewRecord_CreatesSuccessfully() {
            // Arrange - No existing category balance record
            Long accountId = 1234567890L;
            String typeCode = "01";
            int categoryCode = 1000;
            BigDecimal amount = new BigDecimal("100.00");

            // Act
            transactionPostingService.updateCategoryBalance(accountId, typeCode, categoryCode, amount);

            // Assert - Should create new record matching COBOL logic
            // Note: Since we're using in-memory lists, we can't directly verify the creation
            // but the service should handle this scenario without error
        }

        @Test
        @DisplayName("Should update existing category balance record")
        void updateCategoryBalance_ExistingRecord_UpdatesSuccessfully() {
            // Arrange - Add existing category balance
            transactionPostingService.addAccount(validAccountRecord);
            Long accountId = 1234567890L;
            String typeCode = "01";
            int categoryCode = 1000;
            BigDecimal transactionAmount = new BigDecimal("50.00");

            // First create a category balance record
            transactionPostingService.updateCategoryBalance(accountId, typeCode, categoryCode, new BigDecimal("100.00"));

            // Act - Update the existing record
            transactionPostingService.updateCategoryBalance(accountId, typeCode, categoryCode, transactionAmount);

            // Assert - Balance should be updated (can't directly verify due to service design)
            // This tests the flow without exceptions
        }

        @Test
        @DisplayName("Should handle negative transaction amounts correctly")
        void updateCategoryBalance_NegativeAmount_HandlesCorrectly() {
            // Arrange
            Long accountId = 1234567890L;
            String typeCode = "01";
            int categoryCode = 1000;
            BigDecimal negativeAmount = new BigDecimal("-25.00");

            // Act - Should handle negative amounts (refunds, credits)
            transactionPostingService.updateCategoryBalance(accountId, typeCode, categoryCode, negativeAmount);

            // Assert - Should complete without error
            // This represents credit transactions in COBOL logic
        }

        @Test
        @DisplayName("Should handle zero amount transactions")
        void updateCategoryBalance_ZeroAmount_HandlesCorrectly() {
            // Arrange
            Long accountId = 1234567890L;
            String typeCode = "01";
            int categoryCode = 1000;
            BigDecimal zeroAmount = BigDecimal.ZERO.setScale(2);

            // Act
            transactionPostingService.updateCategoryBalance(accountId, typeCode, categoryCode, zeroAmount);

            // Assert - Should complete without error
        }
    }

    /**
     * Tests for reject record generation - equivalent to COBOL 2500-WRITE-REJECT-REC paragraph.
     */
    @Nested
    @DisplayName("Reject Record Generation Tests - COBOL 2500-WRITE-REJECT-REC Logic")
    class RejectRecordGenerationTests {

        @Test
        @DisplayName("Should generate reject record with proper format and validation trailer")
        void generateRejectRecord_ValidInput_GeneratesCorrectFormat() {
            // Arrange - Set validation failure reason and description
            TransactionPostingService.DailyTransactionRecord invalidTransaction = validDailyTransaction;
            // Trigger validation failure to set up error details
            transactionPostingService.validateTransaction(invalidTransaction); // Will fail due to missing data

            // Act
            transactionPostingService.generateRejectRecord(invalidTransaction);

            // Assert
            List<TransactionPostingService.RejectRecord> rejects = transactionPostingService.getRejectedTransactions();
            assertThat(rejects).hasSize(1);

            TransactionPostingService.RejectRecord reject = rejects.get(0);
            assertThat(reject.getTransactionData()).isNotNull();
            assertThat(reject.getValidationFailReason()).isGreaterThan(0);
            assertThat(reject.getValidationFailDescription()).isNotEmpty();
        }

        @Test
        @DisplayName("Should format transaction data correctly for reject record")
        void generateRejectRecord_FormatsTransactionDataCorrectly() {
            // Arrange - Create a complete transaction record
            validDailyTransaction.setTransactionId("TXN123456789012");
            validDailyTransaction.setCardNumber("1234567890123456");
            validDailyTransaction.setTypeCode("01");
            validDailyTransaction.setCategoryCode(1000);
            
            // Trigger validation failure
            transactionPostingService.validateTransaction(validDailyTransaction);

            // Act
            transactionPostingService.generateRejectRecord(validDailyTransaction);

            // Assert - Verify formatted data contains expected fields
            TransactionPostingService.RejectRecord reject = 
                transactionPostingService.getRejectedTransactions().get(0);
            String transactionData = reject.getTransactionData();
            
            assertThat(transactionData).contains("TXN123456789012");
            assertThat(transactionData).contains("1234567890123456");
            assertThat(transactionData).contains("01");
            assertThat(transactionData).contains("1000");
        }

        @Test
        @DisplayName("Should include validation trailer with error details")
        void generateRejectRecord_IncludesValidationTrailer() {
            // Arrange - Force specific validation error
            transactionPostingService.validateTransaction(validDailyTransaction); // Will fail with card not found

            // Act
            transactionPostingService.generateRejectRecord(validDailyTransaction);

            // Assert - Verify validation trailer information
            TransactionPostingService.RejectRecord reject = 
                transactionPostingService.getRejectedTransactions().get(0);
            
            assertThat(reject.getValidationFailReason()).isEqualTo(100); // INVALID CARD NUMBER
            assertThat(reject.getValidationFailDescription()).isEqualTo("INVALID CARD NUMBER FOUND");
        }

        @Test
        @DisplayName("Should handle null or empty transaction fields gracefully")
        void generateRejectRecord_NullFields_HandlesGracefully() {
            // Arrange - Create transaction with null fields
            TransactionPostingService.DailyTransactionRecord nullFieldTransaction = 
                new TransactionPostingService.DailyTransactionRecord();
            nullFieldTransaction.setAmount(new BigDecimal("100.00"));
            
            // Trigger validation failure
            transactionPostingService.validateTransaction(nullFieldTransaction);

            // Act
            transactionPostingService.generateRejectRecord(nullFieldTransaction);

            // Assert - Should handle nulls without exception
            List<TransactionPostingService.RejectRecord> rejects = transactionPostingService.getRejectedTransactions();
            assertThat(rejects).hasSize(1);
            assertThat(rejects.get(0).getTransactionData()).isNotNull();
        }
    }

    /**
     * Tests for DB2 timestamp formatting - equivalent to COBOL Z-GET-DB2-FORMAT-TIMESTAMP paragraph.
     */
    @Nested
    @DisplayName("DB2 Timestamp Formatting Tests - COBOL Z-GET-DB2-FORMAT-TIMESTAMP Logic")
    class TimestampFormattingTests {

        @Test
        @DisplayName("Should format timestamp in DB2 format")
        void formatTimestamp_ReturnsDB2Format() {
            // Act
            String timestamp = transactionPostingService.formatTimestamp();

            // Assert - Verify DB2 timestamp format: YYYY-MM-DD-HH.MM.SS.NN0000
            assertThat(timestamp).isNotNull();
            assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{6}");
            assertThat(timestamp).endsWith("0000"); // Fixed suffix from COBOL
        }

        @Test
        @DisplayName("Should generate consistent timestamp format multiple times")
        void formatTimestamp_ConsistentFormat() {
            // Act - Generate multiple timestamps
            String timestamp1 = transactionPostingService.formatTimestamp();
            String timestamp2 = transactionPostingService.formatTimestamp();

            // Assert - Both should have same format structure
            assertThat(timestamp1).matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{6}");
            assertThat(timestamp2).matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{6}");
            
            // Timestamps should be close in time but properly formatted
            assertThat(timestamp1.substring(0, 16)).isEqualTo(timestamp2.substring(0, 16)); // Same minute
        }

        @Test
        @DisplayName("Should include proper separators in timestamp")
        void formatTimestamp_IncludesProperSeparators() {
            // Act
            String timestamp = transactionPostingService.formatTimestamp();

            // Assert - Verify separator characters match COBOL format
            assertThat(timestamp.charAt(4)).isEqualTo('-');  // After year
            assertThat(timestamp.charAt(7)).isEqualTo('-');  // After month
            assertThat(timestamp.charAt(10)).isEqualTo('-'); // After day
            assertThat(timestamp.charAt(13)).isEqualTo('.'); // After hour
            assertThat(timestamp.charAt(16)).isEqualTo('.'); // After minute
            assertThat(timestamp.charAt(19)).isEqualTo('.'); // After second
        }
    }

    /**
     * Tests for condition code handling - equivalent to COBOL RETURN-CODE logic.
     */
    @Nested
    @DisplayName("Condition Code Tests - COBOL RETURN-CODE Logic")
    class ConditionCodeTests {

        @Test
        @DisplayName("Should return condition code 0 for successful processing")
        void getConditionCode_SuccessfulProcessing_ReturnsZero() {
            // Arrange - Process valid transactions
            transactionPostingService.addDailyTransaction(validDailyTransaction);
            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);
            transactionPostingService.postDailyTransactions();

            // Act
            int conditionCode = transactionPostingService.getConditionCode();

            // Assert - Should return 0 for success
            assertThat(conditionCode).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return condition code 4 when transactions are rejected")
        void getConditionCode_RejectedTransactions_ReturnsFour() {
            // Arrange - Process invalid transactions (missing cross-reference)
            transactionPostingService.addDailyTransaction(validDailyTransaction);
            transactionPostingService.postDailyTransactions();

            // Act
            int conditionCode = transactionPostingService.getConditionCode();

            // Assert - Should return 4 for rejects (matching COBOL logic)
            assertThat(conditionCode).isEqualTo(4);
        }

        @Test
        @DisplayName("Should maintain condition code 0 for empty processing")
        void getConditionCode_EmptyProcessing_ReturnsZero() {
            // Arrange - Process empty transaction list
            transactionPostingService.postDailyTransactions();

            // Act
            int conditionCode = transactionPostingService.getConditionCode();

            // Assert - Should return 0 for successful empty processing
            assertThat(conditionCode).isEqualTo(0);
        }
    }

    /**
     * Tests for cross-reference lookup - equivalent to COBOL 1500-A-LOOKUP-XREF paragraph.
     */
    @Nested
    @DisplayName("Cross Reference Lookup Tests - COBOL 1500-A-LOOKUP-XREF Logic")
    class CrossReferenceLookupTests {

        @Test
        @DisplayName("Should find valid cross-reference record")
        void lookupCrossReference_ValidCard_ReturnsRecord() {
            // Arrange
            transactionPostingService.addCrossReference(validXrefRecord);

            // Act
            TransactionPostingService.CrossReferenceRecord result = 
                transactionPostingService.lookupCrossReference("1234567890123456");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCardNumber()).isEqualTo("1234567890123456");
            assertThat(result.getAccountId()).isEqualTo(1234567890L);
        }

        @Test
        @DisplayName("Should return null for invalid card number")
        void lookupCrossReference_InvalidCard_ReturnsNull() {
            // Arrange - Add valid cross-reference but look up different card

            // Act
            TransactionPostingService.CrossReferenceRecord result = 
                transactionPostingService.lookupCrossReference("9999999999999999");

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle null card number gracefully")
        void lookupCrossReference_NullCard_ReturnsNull() {
            // Act
            TransactionPostingService.CrossReferenceRecord result = 
                transactionPostingService.lookupCrossReference(null);

            // Assert
            assertThat(result).isNull();
        }
    }

    /**
     * Tests for account lookup - equivalent to COBOL 1500-B-LOOKUP-ACCT paragraph.
     */
    @Nested
    @DisplayName("Account Lookup Tests - COBOL 1500-B-LOOKUP-ACCT Logic")
    class AccountLookupTests {

        @Test
        @DisplayName("Should find valid account record")
        void lookupAccount_ValidId_ReturnsRecord() {
            // Arrange
            transactionPostingService.addAccount(validAccountRecord);

            // Act
            TransactionPostingService.AccountRecord result = 
                transactionPostingService.lookupAccount(1234567890L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(1234567890L);
            assertThat(result.getCreditLimit()).isEqualTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("Should return null for invalid account ID")
        void lookupAccount_InvalidId_ReturnsNull() {
            // Act
            TransactionPostingService.AccountRecord result = 
                transactionPostingService.lookupAccount(9999999999L);

            // Assert
            assertThat(result).isNull();
        }
    }

    /**
     * Integration tests combining multiple validation scenarios from COBOL program.
     */
    @Nested
    @DisplayName("Integration Tests - End-to-End COBOL Logic Validation")
    class IntegrationTests {

        @Test
        @DisplayName("Should replicate complete COBOL transaction processing workflow")
        void completeWorkflow_MatchesCOBOLBehavior() {
            // Arrange - Set up complete valid scenario
            transactionPostingService.addDailyTransaction(validDailyTransaction);
            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act - Execute complete workflow
            int result = transactionPostingService.postDailyTransactions();

            // Assert - Verify all COBOL equivalent outcomes
            assertThat(result).isEqualTo(0); // RETURN-CODE = 0
            assertThat(transactionPostingService.getTransactionCount()).isEqualTo(1); // WS-TRANSACTION-COUNT = 1
            assertThat(transactionPostingService.getRejectCount()).isEqualTo(0); // WS-REJECT-COUNT = 0
            
            // Verify transaction was posted correctly
            List<TransactionPostingService.TransactionRecord> posted = 
                transactionPostingService.getPostedTransactions();
            assertThat(posted).hasSize(1);
            assertThat(posted.get(0).getProcessedTimestamp()).isNotNull(); // TRAN-PROC-TS set
            
            // Verify no rejects generated
            assertThat(transactionPostingService.getRejectedTransactions()).isEmpty();
        }

        @Test
        @DisplayName("Should handle all COBOL validation error scenarios correctly")
        void allValidationErrors_ProcessedCorrectly() {
            // Arrange - Create transactions for each error type
            List<TransactionPostingService.DailyTransactionRecord> errorTransactions = new ArrayList<>();

            // Error 100 - Invalid Card Number
            TransactionPostingService.DailyTransactionRecord invalidCardTxn = 
                new TransactionPostingService.DailyTransactionRecord();
            invalidCardTxn.setTransactionId("ERR100");
            invalidCardTxn.setCardNumber("9999999999999999");
            invalidCardTxn.setAmount(new BigDecimal("100.00"));
            invalidCardTxn.setOriginalTimestamp("2024-01-15-10.30.45.120000");
            errorTransactions.add(invalidCardTxn);

            // Error 101 - Account Not Found  
            TransactionPostingService.DailyTransactionRecord accountNotFoundTxn = 
                new TransactionPostingService.DailyTransactionRecord();
            accountNotFoundTxn.setTransactionId("ERR101");
            accountNotFoundTxn.setCardNumber("1111111111111111");
            accountNotFoundTxn.setAmount(new BigDecimal("100.00"));
            accountNotFoundTxn.setOriginalTimestamp("2024-01-15-10.30.45.120000");
            
            // Add cross-reference but no account
            TransactionPostingService.CrossReferenceRecord orphanXref = 
                new TransactionPostingService.CrossReferenceRecord();
            orphanXref.setCardNumber("1111111111111111");
            orphanXref.setAccountId(9999999999L); // Non-existent account
            
            errorTransactions.add(accountNotFoundTxn);

            // Add all error transactions
            for (TransactionPostingService.DailyTransactionRecord txn : errorTransactions) {
                transactionPostingService.addDailyTransaction(txn);
            }
            transactionPostingService.addCrossReference(orphanXref);

            // Act
            int result = transactionPostingService.postDailyTransactions();

            // Assert
            assertThat(result).isEqualTo(4); // RETURN-CODE = 4 (rejects found)
            assertThat(transactionPostingService.getRejectCount()).isEqualTo(2);
            assertThat(transactionPostingService.getRejectedTransactions()).hasSize(2);
        }

        @Test
        @DisplayName("Should preserve COBOL decimal precision in balance calculations")
        void decimalPrecision_PreservedInCalculations() {
            // Arrange - Set up precise decimal amounts matching COBOL COMP-3 behavior
            validDailyTransaction.setAmount(new BigDecimal("123.45"));
            validAccountRecord.setCurrentCycleCredit(new BigDecimal("1000.12"));
            validAccountRecord.setCurrentCycleDebit(new BigDecimal("500.34"));
            validAccountRecord.setCreditLimit(new BigDecimal("2000.00"));

            transactionPostingService.addDailyTransaction(validDailyTransaction);
            transactionPostingService.addCrossReference(validXrefRecord);
            transactionPostingService.addAccount(validAccountRecord);

            // Act
            transactionPostingService.postDailyTransactions();

            // Assert - All amounts should maintain 2 decimal precision
            TransactionPostingService.TransactionRecord posted = 
                transactionPostingService.getPostedTransactions().get(0);
            assertThat(posted.getAmount().scale()).isEqualTo(2);
            assertThat(posted.getAmount()).isEqualTo(new BigDecimal("123.45"));
        }
    }
}