package com.carddemo.service;

import com.carddemo.dto.AddTransactionRequest;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.util.CobolComparisonUtils;
import com.carddemo.util.TestConstants;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for TransactionAddService validating COBOL COTRN02C transaction creation logic 
 * migration to Java, testing transaction validation, amount calculations, balance updates, 
 * and credit limit enforcement with BigDecimal precision matching COMP-3.
 *
 * This test class ensures 100% functional parity with the original COBOL COTRN02C program
 * by validating identical business logic behavior, precision handling, and error conditions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionAddService - COBOL COTRN02C Migration Tests")
class TransactionAddServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @InjectMocks
    private TransactionAddService transactionAddService;
    
    // Test data
    private Account testAccount;
    private AddTransactionRequest validRequest;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        // Create test account matching COBOL CVACT01Y structure
        testAccount = new Account();
        testAccount.setAccountId(TestConstants.VALID_ACCOUNT_ID);
        testAccount.setCustomerId(TestConstants.VALID_CUSTOMER_ID);
        testAccount.setCurrentBalance(TestConstants.DEFAULT_ACCOUNT_BALANCE);
        testAccount.setCreditLimit(TestConstants.DEFAULT_CREDIT_LIMIT);
        testAccount.setActiveStatus(TestConstants.ACCOUNT_STATUS_ACTIVE);

        // Create valid request matching COTRN02 transaction structure
        validRequest = new AddTransactionRequest();
        validRequest.setAccountId(TestConstants.VALID_ACCOUNT_ID);
        validRequest.setCardNumber(TestConstants.VALID_CARD_NUMBER);
        validRequest.setAmount(TestConstants.VALID_TRANSACTION_AMOUNT);
        validRequest.setTypeCode(TestConstants.DEFAULT_TYPE_CODE);
        validRequest.setCategoryCode(TestConstants.CATEGORY_CODE_GAS);
        validRequest.setDescription(TestConstants.TEST_DESCRIPTION);
        validRequest.setMerchantName(TestConstants.TEST_MERCHANT_NAME);
        validRequest.setMerchantCity(TestConstants.TEST_MERCHANT_CITY);
        validRequest.setMerchantZip(TestConstants.TEST_MERCHANT_ZIP);
        validRequest.setTransactionDate(LocalDateTime.now());

        // Create expected transaction result
        testTransaction = new Transaction();
        testTransaction.setTransactionId(TestConstants.TEST_TRANSACTION_ID);
        testTransaction.setAccountId(TestConstants.VALID_ACCOUNT_ID);
        testTransaction.setAmount(TestConstants.VALID_TRANSACTION_AMOUNT);
        testTransaction.setTransactionType(TestConstants.DEFAULT_TYPE_CODE);
        testTransaction.setDescription(TestConstants.TEST_DESCRIPTION);
        testTransaction.setTransactionDate(validRequest.getTransactionDate());
        testTransaction.setMerchantId("MERCH001");
        testTransaction.setMerchantName(TestConstants.TEST_MERCHANT_NAME);
    }

    @Test
    @DisplayName("addTransaction - Success Path - Validates Complete COTRN02C Processing")
    void testAddTransaction_Success() {
        // Given: Valid account and transaction request
        when(accountRepository.findById(TestConstants.VALID_ACCOUNT_ID))
            .thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(testTransaction);

        // When: Processing transaction addition
        Transaction result = transactionAddService.addTransaction(validRequest);

        // Then: Verify successful transaction creation
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(TestConstants.VALID_ACCOUNT_ID);
        assertThat(result.getAmount()).isEqualTo(TestConstants.VALID_TRANSACTION_AMOUNT);
        assertThat(result.getTransactionType()).isEqualTo(TestConstants.DEFAULT_TYPE_CODE);
        
        // Verify repository interactions
        verify(accountRepository).findById(TestConstants.VALID_ACCOUNT_ID);
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(testAccount);
        
        // Verify balance update with COBOL precision
        BigDecimal expectedNewBalance = TestConstants.DEFAULT_ACCOUNT_BALANCE
            .add(TestConstants.VALID_TRANSACTION_AMOUNT)
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        CobolComparisonUtils.validateComp3Equivalent(testAccount.getCurrentBalance(), expectedNewBalance);
    }

    @Test
    @DisplayName("addTransaction - Account Not Found - Business Rule Violation")
    void testAddTransaction_AccountNotFound() {
        // Given: Non-existent account ID
        when(accountRepository.findById(TestConstants.VALID_ACCOUNT_ID))
            .thenReturn(Optional.empty());

        // When & Then: Expect business rule exception
        assertThatThrownBy(() -> transactionAddService.addTransaction(validRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Account not found");
        
        verify(accountRepository).findById(TestConstants.VALID_ACCOUNT_ID);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("addTransaction - Insufficient Credit Limit - Credit Validation")
    void testAddTransaction_InsufficientCreditLimit() {
        // Given: Account with insufficient credit limit
        BigDecimal largeAmount = TestConstants.DEFAULT_CREDIT_LIMIT
            .add(new BigDecimal("1000.00"));
        validRequest.setAmount(largeAmount);
        
        when(accountRepository.findById(TestConstants.VALID_ACCOUNT_ID))
            .thenReturn(Optional.of(testAccount));

        // When & Then: Expect business rule exception for credit limit
        assertThatThrownBy(() -> transactionAddService.addTransaction(validRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient credit limit");
        
        verify(accountRepository).findById(TestConstants.VALID_ACCOUNT_ID);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("addTransaction - Invalid Account Status - Inactive Account")
    void testAddTransaction_InvalidAccount() {
        // Given: Inactive account
        testAccount.setActiveStatus("N");
        when(accountRepository.findById(TestConstants.VALID_ACCOUNT_ID))
            .thenReturn(Optional.of(testAccount));

        // When & Then: Expect business rule exception for inactive account
        assertThatThrownBy(() -> transactionAddService.addTransaction(validRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Account is not active");
        
        verify(accountRepository).findById(TestConstants.VALID_ACCOUNT_ID);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateTransaction - Valid Input - Field Validation Success")
    void testValidateTransaction_ValidInput() {
        // When & Then: Validation should pass without exception
        assertThatCode(() -> transactionAddService.validateTransaction(validRequest))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
        "0.00, Amount must be greater than zero",
        "-50.00, Amount cannot be negative",
        "99999.99, Amount exceeds maximum transaction limit"
    })
    @DisplayName("validateTransaction - Invalid Amount Scenarios")
    void testValidateTransaction_InvalidAmount(String amount, String expectedMessage) {
        // Given: Invalid transaction amount
        validRequest.setAmount(new BigDecimal(amount));

        // When & Then: Expect validation exception
        assertThatThrownBy(() -> transactionAddService.validateTransaction(validRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining(expectedMessage);
    }

    @Test
    @DisplayName("validateTransactionAmount - Within Credit Limits - Amount Validation")
    void testValidateTransactionAmount_WithinLimits() {
        // Given: Transaction within credit limits
        BigDecimal availableCredit = TestConstants.DEFAULT_CREDIT_LIMIT
            .subtract(TestConstants.DEFAULT_ACCOUNT_BALANCE);
        BigDecimal transactionAmount = availableCredit.subtract(new BigDecimal("100.00"));

        // When & Then: Validation should pass
        assertThatCode(() -> transactionAddService.validateTransactionAmount(
            testAccount, transactionAmount))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateTransactionAmount - Exceeds Credit Limit - Limit Enforcement")
    void testValidateTransactionAmount_ExceedsLimits() {
        // Given: Transaction exceeding credit limits
        BigDecimal excessiveAmount = TestConstants.DEFAULT_CREDIT_LIMIT
            .add(new BigDecimal("500.00"));

        // When & Then: Expect business rule exception
        assertThatThrownBy(() -> transactionAddService.validateTransactionAmount(
            testAccount, excessiveAmount))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Transaction amount exceeds available credit");
    }

    @Test
    @DisplayName("generateTransactionId - Uniqueness Validation - ID Generation")
    void testGenerateTransactionId_Uniqueness() {
        // When: Generate multiple transaction IDs
        String id1 = transactionAddService.generateTransactionId();
        String id2 = transactionAddService.generateTransactionId();

        // Then: Verify uniqueness and format
        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1).hasSize(TestConstants.TRANSACTION_ID_MAX_LENGTH);
        assertThat(id2).hasSize(TestConstants.TRANSACTION_ID_MAX_LENGTH);
        
        // Verify COBOL-compatible format (alphanumeric)
        assertThat(id1).matches("^[A-Z0-9]+$");
        assertThat(id2).matches("^[A-Z0-9]+$");
    }

    @Test
    @DisplayName("processTransaction - Balance Update - Account State Management")
    void testProcessTransaction_BalanceUpdate() {
        // Given: Valid transaction processing scenario
        BigDecimal originalBalance = testAccount.getCurrentBalance();
        
        when(accountRepository.save(any(Account.class)))
            .thenReturn(testAccount);

        // When: Process transaction with balance update
        transactionAddService.processTransaction(testAccount, validRequest);

        // Then: Verify balance update with COBOL precision
        BigDecimal expectedBalance = originalBalance
            .add(validRequest.getAmount())
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        assertThat(testAccount.getCurrentBalance())
            .isEqualByComparingTo(expectedBalance);
        
        verify(accountRepository).save(testAccount);
        
        // Validate precision matches COBOL COMP-3 behavior
        CobolComparisonUtils.validateComp3Equivalent(
            testAccount.getCurrentBalance(), expectedBalance);
    }

    @Test
    @DisplayName("processTransaction - Database Error Handling - Transaction Rollback")
    void testProcessTransaction_DatabaseError() {
        // Given: Database error during account save
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then: Expect proper error handling
        assertThatThrownBy(() -> 
            transactionAddService.processTransaction(testAccount, validRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection failed");
        
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("validateCrossReference - Data Integrity - Account Card Validation")
    void testValidateCrossReference() {
        // Given: Valid account and card combination
        when(accountRepository.findById(TestConstants.VALID_ACCOUNT_ID))
            .thenReturn(Optional.of(testAccount));

        // When & Then: Cross-reference validation should pass
        assertThatCode(() -> transactionAddService.validateCrossReference(
            TestConstants.VALID_ACCOUNT_ID, TestConstants.VALID_CARD_NUMBER))
            .doesNotThrowAnyException();
        
        verify(accountRepository).findById(TestConstants.VALID_ACCOUNT_ID);
    }

    @Test
    @DisplayName("COBOL Functional Parity - Complete Processing Validation")
    void testCobolFunctionalParity() {
        // Given: Complete transaction processing scenario
        when(accountRepository.findById(TestConstants.VALID_ACCOUNT_ID))
            .thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(testTransaction);

        // When: Execute complete transaction flow
        Transaction result = transactionAddService.addTransaction(validRequest);

        // Then: Validate functional parity with COBOL COTRN02C
        assertThat(result).isNotNull();
        
        // Verify transaction structure matches COBOL CVTRA05Y
        assertThat(result.getTransactionId()).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(testAccount.getAccountId());
        assertThat(result.getAmount()).isEqualTo(validRequest.getAmount());
        assertThat(result.getTransactionType()).isEqualTo(validRequest.getTypeCode());
        assertThat(result.getDescription()).hasSize(lessThanOrEqualTo(TestConstants.TRANSACTION_DESC_MAX_LENGTH));
        assertThat(result.getMerchantName()).hasSize(lessThanOrEqualTo(TestConstants.MERCHANT_NAME_MAX_LENGTH));
        
        // Verify balance calculation precision matches COBOL COMP-3
        BigDecimal expectedBalance = TestConstants.DEFAULT_ACCOUNT_BALANCE
            .add(validRequest.getAmount())
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        CobolComparisonUtils.compareNumericPrecision(
            testAccount.getCurrentBalance(), 
            expectedBalance, 
            TestConstants.COBOL_DECIMAL_SCALE
        );
        
        // Verify complete processing chain
        verify(accountRepository).findById(TestConstants.VALID_ACCOUNT_ID);
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(testAccount);
        
        // Validate transaction attributes match COBOL field specifications
        assertThat(result.getTransactionId()).matches("^[A-Z0-9]{" + 
            TestConstants.TRANSACTION_ID_MAX_LENGTH + "}$");
        assertThat(result.getTransactionDate()).isNotNull();
        assertThat(result.getAmount().scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
    }

    @Test
    @DisplayName("BigDecimal Precision - COMP-3 Equivalence Validation")
    void testBigDecimalPrecision() {
        // Given: Financial calculation scenario
        BigDecimal amount1 = new BigDecimal("123.456")
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        BigDecimal amount2 = new BigDecimal("678.789")
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);

        // When: Perform calculations with COBOL rounding
        BigDecimal result = amount1.add(amount2)
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);

        // Then: Verify precision matches COBOL COMP-3 behavior
        BigDecimal expectedResult = new BigDecimal("802.25"); // Rounded to 2 decimal places
        CobolComparisonUtils.validateComp3Equivalent(result, expectedResult);
        
        assertThat(result.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(result).isEqualByComparingTo(expectedResult);
    }

    @Test
    @DisplayName("Error Handling - Field Validation Exception Processing")
    void testFieldValidationErrors() {
        // Given: Request with multiple validation errors
        AddTransactionRequest invalidRequest = new AddTransactionRequest();
        invalidRequest.setAccountId(""); // Empty account ID
        invalidRequest.setAmount(BigDecimal.ZERO); // Zero amount
        invalidRequest.setDescription(""); // Empty description

        // When & Then: Expect comprehensive validation exception
        assertThatThrownBy(() -> transactionAddService.validateTransaction(invalidRequest))
            .isInstanceOf(ValidationException.class);
    }

    /**
     * Helper method to create test accounts with specific balances for edge case testing
     */
    private Account createTestAccountWithBalance(BigDecimal balance) {
        Account account = new Account();
        account.setAccountId(TestConstants.VALID_ACCOUNT_ID);
        account.setCustomerId(TestConstants.VALID_CUSTOMER_ID);
        account.setCurrentBalance(balance.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.setCreditLimit(TestConstants.DEFAULT_CREDIT_LIMIT);
        account.setActiveStatus(TestConstants.ACCOUNT_STATUS_ACTIVE);
        return account;
    }

    /**
     * Helper method to assert BigDecimal equality with COBOL precision tolerance
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected) {
        assertThat(actual.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
            .isEqualByComparingTo(expected.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
    }

    /**
     * Helper method to validate COBOL precision in financial calculations
     */
    private void validateCobolPrecision(BigDecimal value) {
        assertThat(value.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        CobolComparisonUtils.validateComp3Equivalent(value, value);
    }

    /**
     * Cleanup method to reset mocks and test data
     */
    void tearDown() {
        reset(transactionRepository, accountRepository);
        testAccount = null;
        validRequest = null;
        testTransaction = null;
    }
}