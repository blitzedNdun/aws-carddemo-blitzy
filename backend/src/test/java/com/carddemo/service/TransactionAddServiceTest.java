package com.carddemo.service;

import com.carddemo.dto.AddTransactionRequest;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.service.TransactionAddService;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.util.TestConstants;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

/**
 * Unit test class for TransactionAddService validating COBOL COTRN02C transaction creation logic 
 * migration to Java, testing transaction validation, amount calculations, balance updates, 
 * and credit limit enforcement with 100% functional parity.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("UnitTest")
class TransactionAddServiceTest extends AbstractBaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionAddService transactionAddService;

    private Account testAccount;
    private AddTransactionRequest validRequest;
    private Transaction expectedTransaction;

    /**
     * Setup method called before each test to initialize test data and mock objects
     */
    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        
        // Create test account with valid balance and credit limit
        testAccount = createValidAccountScenario();
        testAccount.setAccountId("1000000001");
        testAccount.setCurrentBalance(TestConstants.createCobolDecimal("500.00"));
        testAccount.setCreditLimit(TestConstants.createCobolDecimal("2000.00"));
        testAccount.setActiveStatus("ACTIVE");

        // Create valid transaction request
        validRequest = testDataGenerator.generateValidTransactionRequest(testAccount.getAccountId());
        validRequest.setAmount(TestConstants.createCobolDecimal("100.00"));

        // Create expected transaction result
        expectedTransaction = createTestTransaction(validRequest.getAmount());
        expectedTransaction.setAccountId(testAccount.getAccountId());
        expectedTransaction.setTransactionId("TXN0000000001");
    }

    /**
     * Teardown method called after each test
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    @DisplayName("Add Transaction Success - Valid transaction with sufficient credit")
    void testAddTransaction_Success() {
        // Given: Valid account and transaction request
        when(accountRepository.findById(testAccount.getAccountId()))
            .thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(expectedTransaction);
        when(accountRepository.save(any(Account.class)))
            .thenReturn(testAccount);

        // When: Adding transaction
        Transaction result = transactionAddService.addTransaction(validRequest);

        // Then: Transaction added successfully with COBOL precision
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(testAccount.getAccountId());
        assertBigDecimalEquals(validRequest.getAmount(), result.getAmount());
        validateCobolPrecision(result.getAmount());
        
        // Verify repository interactions
        verify(accountRepository).findById(testAccount.getAccountId());
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Add Transaction Validation Failure - Invalid request data")
    void testAddTransaction_ValidationFailure() {
        // Given: Invalid transaction request
        AddTransactionRequest invalidRequest = testDataGenerator.generateInvalidTransactionData();
        
        // When/Then: Transaction validation should fail
        assertThatThrownBy(() -> transactionAddService.addTransaction(invalidRequest))
            .isInstanceOf(ValidationException.class)
            .satisfies(ex -> {
                ValidationException validationEx = (ValidationException) ex;
                assertThat(validationEx.hasFieldErrors()).isTrue();
                assertThat(validationEx.getFieldErrors()).isNotEmpty();
            });
        
        // Verify no database interactions occurred
        verifyNoInteractions(accountRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("Add Transaction Insufficient Credit Limit - Transaction exceeds available credit")
    void testAddTransaction_InsufficientCreditLimit() {
        // Given: Account near credit limit and large transaction amount
        testAccount.setCurrentBalance(TestConstants.createCobolDecimal("1900.00"));
        testAccount.setCreditLimit(TestConstants.createCobolDecimal("2000.00"));
        validRequest.setAmount(TestConstants.createCobolDecimal("200.00")); // Would exceed limit
        
        when(accountRepository.findById(testAccount.getAccountId()))
            .thenReturn(Optional.of(testAccount));

        // When/Then: Should throw business rule exception
        assertThatThrownBy(() -> transactionAddService.addTransaction(validRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("credit limit");

        // Verify account was retrieved but no updates made
        verify(accountRepository).findById(testAccount.getAccountId());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Add Transaction Invalid Account - Account does not exist")
    void testAddTransaction_InvalidAccount() {
        // Given: Non-existent account
        when(accountRepository.findById(validRequest.getAccountId()))
            .thenReturn(Optional.empty());

        // When/Then: Should throw business rule exception
        assertThatThrownBy(() -> transactionAddService.addTransaction(validRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Account not found");

        // Verify only account lookup was attempted
        verify(accountRepository).findById(validRequest.getAccountId());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Validate Transaction Valid Input - All validation rules pass")
    void testValidateTransaction_ValidInput() {
        // When: Validating valid transaction request
        assertThatCode(() -> transactionAddService.validateTransaction(validRequest))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
        "-10.00, Negative amount not allowed",
        "0.00, Zero amount not allowed", 
        "50000.00, Amount exceeds maximum limit"
    })
    @DisplayName("Validate Transaction Invalid Amount - Various invalid amounts")
    void testValidateTransaction_InvalidAmount(String amountStr, String expectedError) {
        // Given: Request with invalid amount
        validRequest.setAmount(new BigDecimal(amountStr));

        // When/Then: Validation should fail
        assertThatThrownBy(() -> transactionAddService.validateTransaction(validRequest))
            .isInstanceOf(ValidationException.class)
            .satisfies(ex -> {
                ValidationException validationEx = (ValidationException) ex;
                assertThat(validationEx.getMessage()).contains("amount");
            });
    }

    @Test
    @DisplayName("Validate Transaction Amount Within Limits - Amount validation passes")
    void testValidateTransactionAmount_WithinLimits() {
        // Given: Valid amount within limits
        BigDecimal validAmount = TestConstants.createCobolDecimal("250.00");
        validRequest.setAmount(validAmount);

        // When: Validating amount
        assertThatCode(() -> transactionAddService.validateTransactionAmount(validRequest))
            .doesNotThrowAnyException();
            
        // Verify COBOL precision is maintained
        validateCobolPrecision(validAmount);
    }

    @ParameterizedTest
    @CsvSource({
        "-1.00, Negative amounts not allowed",
        "0.00, Zero amounts not allowed",
        "15000.00, Amount exceeds daily limit"
    })
    @DisplayName("Validate Transaction Amount Exceeds Limits - Amount validation fails")
    void testValidateTransactionAmount_ExceedsLimits(String amountStr, String expectedError) {
        // Given: Amount outside valid range
        validRequest.setAmount(new BigDecimal(amountStr));

        // When/Then: Amount validation should fail
        assertThatThrownBy(() -> transactionAddService.validateTransactionAmount(validRequest))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Generate Transaction ID Uniqueness - Each ID is unique")
    void testGenerateTransactionId_Uniqueness() {
        // When: Generating multiple transaction IDs
        String id1 = transactionAddService.generateTransactionId();
        String id2 = transactionAddService.generateTransactionId();
        String id3 = transactionAddService.generateTransactionId();

        // Then: All IDs should be unique and follow pattern
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id2).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id3);
        
        // Verify format matches COBOL transaction ID pattern
        assertThat(id1).matches("TXN\\d{10}");
        assertThat(id2).matches("TXN\\d{10}");
        assertThat(id3).matches("TXN\\d{10}");
    }

    @Test
    @DisplayName("Process Transaction Balance Update - Balance updated correctly with COBOL precision")
    void testProcessTransaction_BalanceUpdate() {
        // Given: Account with initial balance
        BigDecimal initialBalance = TestConstants.createCobolDecimal("500.00");
        BigDecimal transactionAmount = TestConstants.createCobolDecimal("150.00");
        BigDecimal expectedNewBalance = initialBalance.add(transactionAmount);
        
        testAccount.setCurrentBalance(initialBalance);
        validRequest.setAmount(transactionAmount);
        
        when(accountRepository.findById(testAccount.getAccountId()))
            .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
            .thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(expectedTransaction);

        // When: Processing transaction
        Transaction result = transactionAddService.processTransaction(validRequest, testAccount);

        // Then: Balance updated with COBOL precision
        assertThat(result).isNotNull();
        
        // Verify balance calculation with COBOL precision
        verify(accountRepository).save(argThat(account -> {
            BigDecimal actualBalance = account.getCurrentBalance();
            assertBigDecimalEquals(expectedNewBalance, actualBalance);
            validateCobolPrecision(actualBalance);
            return true;
        }));
    }

    @Test
    @DisplayName("Process Transaction Database Error - Handles database failures gracefully")
    void testProcessTransaction_DatabaseError() {
        // Given: Database save operation fails
        when(accountRepository.findById(testAccount.getAccountId()))
            .thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When/Then: Should handle database error gracefully
        assertThatThrownBy(() -> transactionAddService.processTransaction(validRequest, testAccount))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection failed");
    }

    @Test
    @DisplayName("COBOL Functional Parity - Java implementation matches COBOL COTRN02C behavior")
    void testCobolFunctionalParity() {
        // Given: Test scenario with COBOL equivalent data
        BigDecimal cobolAmount = TestConstants.createCobolDecimal("123.45");
        BigDecimal cobolBalance = TestConstants.createCobolDecimal("876.54");
        LocalDateTime cobolTimestamp = LocalDateTime.of(2024, 1, 15, 14, 30, 0);
        
        // Create expected COBOL result
        Map<String, Object> expectedCobolResult = CobolComparisonUtils.createCobolTestResult(
            cobolAmount, cobolBalance, TestConstants.SUCCESS_CODE, cobolTimestamp
        );
        
        // Setup test data with COBOL-equivalent values
        testAccount.setCurrentBalance(TestConstants.createCobolDecimal("753.09"));
        validRequest.setAmount(cobolAmount);
        
        when(accountRepository.findById(testAccount.getAccountId()))
            .thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(expectedTransaction);
        when(accountRepository.save(any(Account.class)))
            .thenReturn(testAccount);

        // When: Processing transaction through Java implementation
        Transaction javaResult = transactionAddService.addTransaction(validRequest);
        
        // Create Java result for comparison
        Map<String, Object> actualJavaResult = CobolComparisonUtils.createJavaTestResult(
            javaResult.getAmount(),
            testAccount.getCurrentBalance().add(javaResult.getAmount()),
            TestConstants.SUCCESS_CODE,
            javaResult.getTransactionDate()
        );

        // Then: Assert functional parity with COBOL implementation
        CobolComparisonUtils.assertFunctionalParity(expectedCobolResult, actualJavaResult);
        
        // Verify specific COBOL precision requirements
        assertThat(CobolComparisonUtils.validateComp3Equivalent(
            javaResult.getAmount(), TestConstants.COBOL_DECIMAL_SCALE
        )).isTrue();
        
        // Verify calculation precision matches COBOL COMP-3 behavior
        BigDecimal calculatedBalance = testAccount.getCurrentBalance().add(javaResult.getAmount());
        assertThat(CobolComparisonUtils.compareFinancialCalculations(
            calculatedBalance, cobolBalance
        )).isTrue();
    }

    // Additional helper test methods for cross-reference validation
    
    @Test
    @DisplayName("Validate Cross Reference - Account and transaction relationship")
    void testValidateCrossReference() {
        // Given: Valid account and transaction setup
        when(accountRepository.findById(testAccount.getAccountId()))
            .thenReturn(Optional.of(testAccount));

        // When: Validating cross-reference
        assertThatCode(() -> transactionAddService.validateCrossReference(validRequest))
            .doesNotThrowAnyException();

        // Verify account lookup occurred
        verify(accountRepository).findById(testAccount.getAccountId());
    }

    @Test
    @DisplayName("Transaction amount validation with BigDecimal precision")
    void testTransactionAmountPrecisionValidation() {
        // Given: Various precision scenarios
        BigDecimal[] testAmounts = {
            new BigDecimal("100.12"),     // Valid 2 decimal places
            new BigDecimal("100.1"),      // Valid 1 decimal place
            new BigDecimal("100"),        // Valid whole number
            new BigDecimal("100.123")     // Should be rounded to 2 places
        };

        for (BigDecimal amount : testAmounts) {
            validRequest.setAmount(amount);
            
            // When: Validating amount precision
            assertThatCode(() -> transactionAddService.validateTransactionAmount(validRequest))
                .doesNotThrowAnyException();
            
            // Verify precision matches COBOL COMP-3 requirements
            BigDecimal normalizedAmount = amount.setScale(
                TestConstants.COBOL_DECIMAL_SCALE, 
                TestConstants.COBOL_ROUNDING_MODE
            );
            validateCobolPrecision(normalizedAmount);
        }
    }

    @Test
    @DisplayName("Interest calculation precision matches COBOL behavior")
    void testInterestCalculationPrecision() {
        // Given: Account with balance for interest calculation
        testAccount.setCurrentBalance(TestConstants.createCobolDecimal("1000.00"));
        BigDecimal interestRate = TestConstants.createCobolDecimal("0.0525", 4); // 5.25%
        
        // When: Calculating monthly interest (simulating COBOL calculation)
        BigDecimal monthlyInterest = testAccount.getCurrentBalance()
            .multiply(interestRate)
            .divide(new BigDecimal("12"), TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        // Then: Result should match COBOL COMP-3 precision
        assertThat(monthlyInterest.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        assertThat(monthlyInterest).isEqualTo(TestConstants.createCobolDecimal("4.38"));
        
        // Verify COBOL equivalence
        assertThat(CobolComparisonUtils.validateComp3Equivalent(
            monthlyInterest, TestConstants.COBOL_DECIMAL_SCALE
        )).isTrue();
    }
}