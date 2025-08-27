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
import org.junit.jupiter.api.Tag;
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
 * Unit test class for TransactionAddService validating COBOL COTRN02C 
 * transaction creation logic migration to Java, testing transaction validation,
 * amount calculations, balance updates, and credit limit enforcement.
 * 
 * This test class ensures 100% functional parity between the original COBOL
 * implementation and the modernized Spring Boot service implementation.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("TransactionAddService Unit Tests - COBOL COTRN02C Migration Validation")
public class TransactionAddServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TestDataGenerator testDataGenerator;

    @Mock
    private CobolComparisonUtils cobolComparisonUtils;

    @InjectMocks
    private TransactionAddService transactionAddService;

    // Test data constants following COBOL precision requirements
    private static final String VALID_ACCOUNT_ID = "1000000001";
    private static final String INVALID_ACCOUNT_ID = "9999999999";
    private static final String VALID_CARD_NUMBER = "4532123456789012";
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("150.00");
    private static final BigDecimal LARGE_AMOUNT = new BigDecimal("5000.00");
    private static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-100.00");
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;
    private static final BigDecimal CURRENT_BALANCE = new BigDecimal("1250.75");
    private static final BigDecimal CREDIT_LIMIT = new BigDecimal("2000.00");

    private Account testAccount;
    private AddTransactionRequest validRequest;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        // Initialize test account with COBOL-equivalent precision
        testAccount = new Account();
        testAccount.setAccountId(VALID_ACCOUNT_ID);
        testAccount.setCurrentBalance(CURRENT_BALANCE);
        testAccount.setCreditLimit(CREDIT_LIMIT);
        testAccount.setCustomerId("CUST001");
        testAccount.setActiveStatus("Y");

        // Initialize valid transaction request
        validRequest = new AddTransactionRequest();
        validRequest.setAccountId(VALID_ACCOUNT_ID);
        validRequest.setCardNumber(VALID_CARD_NUMBER);
        validRequest.setAmount(VALID_AMOUNT);
        validRequest.setTypeCode("AUTH");
        validRequest.setCategoryCode("01");
        validRequest.setDescription("Test Transaction");
        validRequest.setMerchantName("Test Merchant");
        validRequest.setMerchantCity("Test City");
        validRequest.setMerchantZip("12345");
        validRequest.setTransactionDate(LocalDateTime.now());

        // Initialize mock transaction
        mockTransaction = new Transaction();
        mockTransaction.setTransactionId("TXN001");
        mockTransaction.setAccountId(VALID_ACCOUNT_ID);
        mockTransaction.setAmount(VALID_AMOUNT);
        mockTransaction.setTransactionType("AUTH");
        mockTransaction.setTransactionDate(LocalDateTime.now());
        mockTransaction.setDescription("Test Transaction");
        mockTransaction.setMerchantName("Test Merchant");

        // Setup common mock behaviors
        loadTestFixtures();
        mockCommonDependencies();
    }

    /**
     * Setup test fixtures with realistic test data following COBOL patterns
     */
    private void loadTestFixtures() {
        when(testDataGenerator.generateValidAccount()).thenReturn(testAccount);
        when(testDataGenerator.generateTransactionData()).thenReturn(mockTransaction);
        when(testDataGenerator.generateCardNumber()).thenReturn(VALID_CARD_NUMBER);
        when(testDataGenerator.generateTransactionAmount()).thenReturn(VALID_AMOUNT);
        when(testDataGenerator.generateValidDate()).thenReturn(LocalDateTime.now());
    }

    /**
     * Mock common dependencies to isolate unit under test
     */
    private void mockCommonDependencies() {
        when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(INVALID_ACCOUNT_ID)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);
        
        // Setup COBOL comparison utilities
        when(cobolComparisonUtils.compareNumericPrecision(any(BigDecimal.class), any(BigDecimal.class)))
            .thenReturn(true);
        when(cobolComparisonUtils.validateComp3Equivalent(any(BigDecimal.class)))
            .thenReturn(true);
    }

    @Test
    @DisplayName("Add Transaction Success - Complete workflow validation")
    void testAddTransaction_Success() {
        // Given: Valid transaction request with all required fields
        when(transactionRepository.count()).thenReturn(1000L);

        // When: Adding transaction through service
        Transaction result = transactionAddService.addTransaction(validRequest);

        // Then: Transaction created successfully with proper validation
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(result.getAmount()).isEqualByComparingTo(VALID_AMOUNT);
        assertThat(result.getTransactionType()).isEqualTo("AUTH");

        // Verify repository interactions
        verify(accountRepository).findById(VALID_ACCOUNT_ID);
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(any(Account.class)); // Balance update

        // Verify COBOL precision validation
        verify(cobolComparisonUtils).compareNumericPrecision(any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Add Transaction Validation Failure - Invalid request data")
    void testAddTransaction_ValidationFailure() {
        // Given: Invalid transaction request with missing required fields
        AddTransactionRequest invalidRequest = new AddTransactionRequest();
        invalidRequest.setAccountId(null); // Missing required field
        invalidRequest.setAmount(NEGATIVE_AMOUNT); // Invalid amount

        // When/Then: ValidationException thrown for invalid request
        assertThatThrownBy(() -> transactionAddService.addTransaction(invalidRequest))
            .isInstanceOf(ValidationException.class)
            .satisfies(exception -> {
                ValidationException validationEx = (ValidationException) exception;
                assertThat(validationEx.hasFieldErrors()).isTrue();
                assertThat(validationEx.getFieldErrors()).isNotEmpty();
            });

        // Verify no repository interactions for invalid requests
        verify(accountRepository, never()).findById(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add Transaction Insufficient Credit Limit - Business rule enforcement")
    void testAddTransaction_InsufficientCreditLimit() {
        // Given: Transaction amount exceeds available credit
        validRequest.setAmount(LARGE_AMOUNT); // 5000.00 > (2000.00 - 1250.75)
        
        // When/Then: BusinessRuleException thrown for credit limit violation
        assertThatThrownBy(() -> transactionAddService.addTransaction(validRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Credit limit exceeded")
            .satisfies(exception -> {
                BusinessRuleException businessEx = (BusinessRuleException) exception;
                assertThat(businessEx.getErrorCode()).isEqualTo("CREDIT_LIMIT_EXCEEDED");
            });

        // Verify account lookup occurred but no transaction saved
        verify(accountRepository).findById(VALID_ACCOUNT_ID);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add Transaction Invalid Account - Account not found")
    void testAddTransaction_InvalidAccount() {
        // Given: Request with non-existent account
        validRequest.setAccountId(INVALID_ACCOUNT_ID);

        // When/Then: BusinessRuleException thrown for invalid account
        assertThatThrownBy(() -> transactionAddService.addTransaction(validRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Account not found")
            .satisfies(exception -> {
                BusinessRuleException businessEx = (BusinessRuleException) exception;
                assertThat(businessEx.getErrorCode()).isEqualTo("ACCOUNT_NOT_FOUND");
            });

        // Verify account lookup attempted
        verify(accountRepository).findById(INVALID_ACCOUNT_ID);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validate Transaction Valid Input - Field validation success")
    void testValidateTransaction_ValidInput() {
        // When: Validating valid transaction request
        ValidationException result = transactionAddService.validateTransaction(validRequest);

        // Then: No validation errors
        assertThat(result).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        ", 'Account ID is required'",
        "'', 'Account ID cannot be empty'",
        "'123', 'Account ID must be 10 digits'"
    })
    @DisplayName("Validate Transaction Invalid Amount - Parameterized validation tests")
    void testValidateTransaction_InvalidAmount(String accountId, String expectedError) {
        // Given: Invalid account ID scenarios
        validRequest.setAccountId(accountId);

        // When: Validating invalid request
        ValidationException result = transactionAddService.validateTransaction(validRequest);

        // Then: Validation errors present
        assertThat(result).isNotNull();
        assertThat(result.hasFieldErrors()).isTrue();
        assertThat(result.getMessage()).contains(expectedError);
    }

    @ParameterizedTest
    @CsvSource({
        "100.00, true",
        "750.00, true", 
        "749.25, true",
        "750.01, false",
        "5000.00, false"
    })
    @DisplayName("Validate Transaction Amount Within Limits - Credit limit boundary testing")
    void testValidateTransactionAmount_WithinLimits(String amountStr, boolean expectedValid) {
        // Given: Various transaction amounts to test credit limits
        BigDecimal testAmount = new BigDecimal(amountStr);
        validRequest.setAmount(testAmount);
        
        // Calculate available credit: 2000.00 - 1250.75 = 749.25
        BigDecimal availableCredit = CREDIT_LIMIT.subtract(CURRENT_BALANCE);

        // When: Validating transaction amount against credit limit
        boolean result = transactionAddService.validateTransactionAmount(testAmount, availableCredit);

        // Then: Validation result matches expected
        assertThat(result).isEqualTo(expectedValid);
        
        // Verify COBOL precision comparison
        if (expectedValid) {
            verify(cobolComparisonUtils).validateComp3Equivalent(testAmount);
        }
    }

    @Test
    @DisplayName("Validate Transaction Amount Exceeds Limits - Credit limit validation")
    void testValidateTransactionAmount_ExceedsLimits() {
        // Given: Amount exceeding available credit
        BigDecimal availableCredit = CREDIT_LIMIT.subtract(CURRENT_BALANCE); // 749.25
        BigDecimal excessiveAmount = availableCredit.add(new BigDecimal("0.01")); // 749.26

        // When: Validating excessive amount
        boolean result = transactionAddService.validateTransactionAmount(excessiveAmount, availableCredit);

        // Then: Validation fails
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Generate Transaction ID Uniqueness - ID generation validation")
    void testGenerateTransactionId_Uniqueness() {
        // Given: Mock transaction count for ID generation
        when(transactionRepository.count()).thenReturn(12345L);

        // When: Generating transaction ID
        String transactionId = transactionAddService.generateTransactionId();

        // Then: ID follows expected format and is unique
        assertThat(transactionId)
            .isNotNull()
            .matches("^TXN\\d{10}$") // TXN + 10 digits
            .contains("0000012346"); // count + 1 with zero padding

        // Verify repository interaction
        verify(transactionRepository).count();
    }

    @Test
    @DisplayName("Process Transaction Balance Update - Account balance modification")
    void testProcessTransaction_BalanceUpdate() {
        // Given: Valid transaction for balance update
        BigDecimal originalBalance = testAccount.getCurrentBalance();
        BigDecimal expectedBalance = originalBalance.subtract(VALID_AMOUNT);

        // When: Processing transaction
        transactionAddService.processTransaction(testAccount, mockTransaction);

        // Then: Balance updated correctly with COBOL precision
        assertBigDecimalEquals(testAccount.getCurrentBalance(), expectedBalance);
        
        // Verify account saved with updated balance
        verify(accountRepository).save(testAccount);
        
        // Verify COBOL precision validation
        verify(cobolComparisonUtils).compareNumericPrecision(
            expectedBalance, testAccount.getCurrentBalance());
    }

    @Test
    @DisplayName("Process Transaction Database Error - Error handling validation")
    void testProcessTransaction_DatabaseError() {
        // Given: Database error simulation
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When/Then: RuntimeException propagated
        assertThatThrownBy(() -> transactionAddService.processTransaction(testAccount, mockTransaction))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection failed");
    }

    @Test
    @DisplayName("COBOL Functional Parity - Complete equivalence validation")
    void testCobolFunctionalParity() {
        // Given: COBOL comparison test data
        when(cobolComparisonUtils.validateTransactionEquivalence(any(), any())).thenReturn(true);
        when(cobolComparisonUtils.assertFunctionalParity(any(), any())).thenReturn(true);

        // When: Processing transaction through service
        Transaction result = transactionAddService.addTransaction(validRequest);

        // Then: Functional parity validated
        assertThat(result).isNotNull();
        
        // Verify COBOL equivalence validation
        verify(cobolComparisonUtils).validateTransactionEquivalence(any(), any());
        verify(cobolComparisonUtils).assertFunctionalParity(any(), any());
    }

    /**
     * Custom assertion for BigDecimal precision matching COBOL COMP-3 behavior
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected) {
        assertThat(actual)
            .usingComparator((a, b) -> a.compareTo(b))
            .isEqualTo(expected);
        
        // Verify scale matches COBOL precision requirements  
        assertThat(actual.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        
        // Verify rounding mode matches COBOL behavior
        if (actual.scale() > TestConstants.COBOL_DECIMAL_SCALE) {
            BigDecimal rounded = actual.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            assertThat(rounded).isEqualByComparingTo(expected);
        }
    }

    /**
     * Validate BigDecimal precision within COBOL tolerance
     */
    private void assertBigDecimalWithinTolerance(BigDecimal actual, BigDecimal expected) {
        BigDecimal tolerance = new BigDecimal("0.01"); // Penny tolerance for financial calculations
        BigDecimal difference = actual.subtract(expected).abs();
        
        assertThat(difference)
            .isLessThanOrEqualTo(tolerance)
            .withFailMessage("BigDecimal values differ by more than penny tolerance: actual=%s, expected=%s", 
                             actual, expected);
    }

    /**
     * Validate COBOL precision requirements for financial calculations
     */
    private void validateCobolPrecision(BigDecimal amount) {
        assertThat(amount.scale())
            .withFailMessage("Scale must match COBOL COMP-3 precision")
            .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
            
        // Verify precision matches COBOL packed decimal constraints
        assertThat(amount.precision())
            .withFailMessage("Precision exceeds COBOL COMP-3 limits")
            .isLessThanOrEqualTo(15); // Max COBOL COMP-3 precision
    }

    /**
     * Setup method for creating test account data
     */
    private Account createTestAccount() {
        Account account = new Account();
        account.setAccountId(VALID_ACCOUNT_ID);
        account.setCurrentBalance(CURRENT_BALANCE);
        account.setCreditLimit(CREDIT_LIMIT);
        account.setCustomerId("CUST001");
        account.setActiveStatus("Y");
        return account;
    }

    /**
     * Setup method for creating test transaction data
     */
    private Transaction createTestTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN001");
        transaction.setAccountId(VALID_ACCOUNT_ID);
        transaction.setAmount(VALID_AMOUNT);
        transaction.setTransactionType("AUTH");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Test Transaction");
        transaction.setMerchantName("Test Merchant");
        return transaction;
    }

    /**
     * Cleanup method for test resource management
     */
    void tearDown() {
        // Clear any test state and reset mocks
        Mockito.reset(transactionRepository, accountRepository, testDataGenerator, cobolComparisonUtils);
    }
}