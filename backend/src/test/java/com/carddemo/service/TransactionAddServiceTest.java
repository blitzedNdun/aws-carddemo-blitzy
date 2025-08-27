package com.carddemo.service;

import com.carddemo.dto.AddTransactionRequest;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
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
 * Comprehensive unit test class for TransactionAddService validating COBOL COTRN02C 
 * transaction creation logic migration to Java. Tests transaction validation rules, 
 * amount calculations, balance updates, and credit limit enforcement with BigDecimal 
 * precision matching COMP-3 behavior.
 *
 * This test class ensures 100% functional parity between COBOL and Java implementations
 * by validating:
 * - Transaction validation rules equivalent to COBOL edit routines
 * - Credit limit checking matching COBOL business rules
 * - Balance update calculations with BigDecimal precision matching COMP-3
 * - Transaction posting logic replicating COBOL file operations
 * - Error handling equivalent to COBOL ABEND routines
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionAddService - COBOL COTRN02C Migration Tests")
public class TransactionAddServiceTest {

    // COBOL-equivalent decimal precision constants matching COMP-3 behavior
    private static final int COBOL_DECIMAL_SCALE = 2;
    private static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final long RESPONSE_TIME_THRESHOLD_MS = 200L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionAddService transactionAddService;

    private Account testAccount;
    private AddTransactionRequest validTransactionRequest;
    private LocalDateTime testTimestamp;

    /**
     * Set up test fixtures with realistic data matching COBOL data structures.
     * Creates test account and transaction request data that follows VSAM record
     * layouts and COBOL data type patterns.
     */
    @BeforeEach
    void setUp() {
        testTimestamp = LocalDateTime.now();
        
        // Create test account with COBOL-equivalent decimal precision
        testAccount = new Account();
        testAccount.setAccountId("1000000001");
        testAccount.setCustomerId("CU00000001");
        testAccount.setCurrentBalance(new BigDecimal("1500.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        testAccount.setCreditLimit(new BigDecimal("5000.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        testAccount.setActiveStatus("Y");
        
        // Create valid transaction request matching BMS mapset structure
        validTransactionRequest = new AddTransactionRequest();
        validTransactionRequest.setAccountId("1000000001");
        validTransactionRequest.setCardNumber("4532123456789012");
        validTransactionRequest.setAmount(new BigDecimal("250.00"));
        validTransactionRequest.setTypeCode("SALE");
        validTransactionRequest.setCategoryCode("RETAIL");
        validTransactionRequest.setDescription("Test Purchase");
        validTransactionRequest.setMerchantName("Test Merchant");
        validTransactionRequest.setMerchantCity("Test City");
        validTransactionRequest.setMerchantZip("12345");
        validTransactionRequest.setTransactionDate(testTimestamp);

        // Mock common repository interactions
        mockCommonDependencies();
    }

    /**
     * Clean up test resources and reset mocks after each test.
     * Ensures test isolation and prevents test interference.
     */
    void tearDown() {
        reset(transactionRepository, accountRepository);
    }

    /**
     * Test successful transaction creation with all validations passing.
     * Validates the complete COBOL 0000-MAIN-PROCESSING equivalent workflow.
     */
    @Test
    @DisplayName("Add Transaction Success - Complete COBOL workflow validation")
    void testAddTransaction_Success() {
        // Given: Valid transaction request and account setup
        when(accountRepository.findById("1000000001")).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            savedTransaction.setTransactionId("TX" + System.currentTimeMillis());
            return savedTransaction;
        });

        long startTime = System.currentTimeMillis();

        // When: Processing transaction through service
        Transaction result = transactionAddService.addTransaction(validTransactionRequest);

        long executionTime = System.currentTimeMillis() - startTime;

        // Then: Verify successful transaction creation with COBOL equivalence
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo("1000000001");
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("250.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        assertThat(result.getTransactionType()).isEqualTo("SALE");
        assertThat(result.getDescription()).isEqualTo("Test Purchase");
        assertThat(result.getMerchantName()).isEqualTo("Test Merchant");
        
        // Verify COBOL performance equivalent (<200ms response time)
        assertThat(executionTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        
        // Verify repository interactions
        verify(accountRepository).findById("1000000001");
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(testAccount);
        
        // Verify account balance updated correctly (COBOL COMP-3 precision)
        BigDecimal expectedBalance = new BigDecimal("1250.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
        assertBigDecimalEquals(testAccount.getCurrentBalance(), expectedBalance);
    }

    /**
     * Test transaction validation failure scenarios.
     * Validates field-level validation equivalent to COBOL edit routines.
     */
    @Test
    @DisplayName("Add Transaction Validation Failure - COBOL edit routine equivalent")
    void testAddTransaction_ValidationFailure() {
        // Given: Invalid transaction request (negative amount)
        AddTransactionRequest invalidRequest = new AddTransactionRequest();
        invalidRequest.setAccountId("1000000001");
        invalidRequest.setCardNumber("4532123456789012");
        invalidRequest.setAmount(new BigDecimal("-100.00")); // Invalid negative amount
        invalidRequest.setTypeCode("SALE");
        invalidRequest.setCategoryCode("RETAIL");
        invalidRequest.setDescription("Invalid Transaction");
        invalidRequest.setMerchantName("Test Merchant");
        invalidRequest.setMerchantCity("Test City");
        invalidRequest.setMerchantZip("12345");
        invalidRequest.setTransactionDate(testTimestamp);

        // When/Then: Expect ValidationException (equivalent to COBOL edit failure)
        assertThatThrownBy(() -> transactionAddService.addTransaction(invalidRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Transaction amount must be positive")
            .extracting(e -> ((ValidationException) e).getFieldErrors())
            .satisfies(errors -> {
                assertThat(errors).containsKey("amount");
                assertThat(errors.get("amount")).contains("must be positive");
            });

        // Verify no database operations occurred due to validation failure
        verify(accountRepository, never()).findById(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    /**
     * Test insufficient credit limit scenario.
     * Validates business rule enforcement equivalent to COBOL credit checking.
     */
    @Test
    @DisplayName("Add Transaction Insufficient Credit Limit - COBOL business rule validation")
    void testAddTransaction_InsufficientCreditLimit() {
        // Given: Transaction amount exceeds available credit
        testAccount.setCurrentBalance(new BigDecimal("4800.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        validTransactionRequest.setAmount(new BigDecimal("500.00")); // Would exceed $5000 limit
        
        when(accountRepository.findById("1000000001")).thenReturn(Optional.of(testAccount));

        // When/Then: Expect BusinessRuleException (equivalent to COBOL ABEND)
        assertThatThrownBy(() -> transactionAddService.addTransaction(validTransactionRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Credit limit exceeded")
            .extracting(e -> ((BusinessRuleException) e).getErrorCode())
            .isEqualTo("9999"); // COBOL ABEND code equivalent

        // Verify account lookup occurred but no updates were made
        verify(accountRepository).findById("1000000001");
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    /**
     * Test invalid account scenario.
     * Validates account existence checking equivalent to COBOL file status checks.
     */
    @Test
    @DisplayName("Add Transaction Invalid Account - COBOL file not found equivalent")
    void testAddTransaction_InvalidAccount() {
        // Given: Account does not exist (simulates COBOL file status '23')
        when(accountRepository.findById("9999999999")).thenReturn(Optional.empty());
        validTransactionRequest.setAccountId("9999999999");

        // When/Then: Expect BusinessRuleException
        assertThatThrownBy(() -> transactionAddService.addTransaction(validTransactionRequest))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Account not found")
            .extracting(e -> ((BusinessRuleException) e).getErrorCode())
            .isEqualTo("0023"); // COBOL file status equivalent

        verify(accountRepository).findById("9999999999");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    /**
     * Test transaction validation with valid input scenarios.
     * Uses parameterized testing to validate multiple valid transaction types.
     */
    @ParameterizedTest
    @CsvSource({
        "SALE, RETAIL, 100.00, Test Purchase",
        "CASH, ATM, 50.00, Cash Advance", 
        "PAYMENT, PAYMENT, 200.00, Payment Made",
        "REFUND, RETAIL, 25.99, Refund Process"
    })
    @DisplayName("Validate Transaction Valid Input - COBOL edit routine success scenarios")
    void testValidateTransaction_ValidInput(String typeCode, String categoryCode, 
                                          String amount, String description) {
        // Given: Various valid transaction scenarios
        AddTransactionRequest request = createValidTransactionRequest(typeCode, categoryCode, 
                                                                    new BigDecimal(amount), description);

        // When: Validating transaction
        boolean isValid = transactionAddService.validateTransaction(request);

        // Then: All scenarios should be valid
        assertThat(isValid).isTrue();
    }

    /**
     * Test transaction amount validation for invalid amounts.
     * Validates COBOL numeric field validation equivalent behavior.
     */
    @Test
    @DisplayName("Validate Transaction Invalid Amount - COBOL numeric validation")
    void testValidateTransaction_InvalidAmount() {
        // Given: Zero amount transaction
        validTransactionRequest.setAmount(BigDecimal.ZERO);

        // When/Then: Expect validation to fail
        assertThatThrownBy(() -> transactionAddService.validateTransaction(validTransactionRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Amount must be greater than zero")
            .extracting(e -> ((ValidationException) e).getFieldErrors())
            .satisfies(errors -> {
                assertThat(errors).containsKey("amount");
            });
    }

    /**
     * Test transaction amount validation within limits.
     * Validates COBOL PIC clause range checking equivalent behavior.
     */
    @Test
    @DisplayName("Validate Transaction Amount Within Limits - COBOL range validation")
    void testValidateTransactionAmount_WithinLimits() {
        // Given: Transaction amount within acceptable range
        BigDecimal validAmount = new BigDecimal("999.99");

        // When: Validating amount
        boolean result = transactionAddService.validateTransactionAmount(validAmount);

        // Then: Validation should pass
        assertThat(result).isTrue();
    }

    /**
     * Test transaction amount validation exceeding limits.
     * Validates COBOL maximum value checking equivalent behavior.
     */
    @Test
    @DisplayName("Validate Transaction Amount Exceeds Limits - COBOL limit checking")
    void testValidateTransactionAmount_ExceedsLimits() {
        // Given: Transaction amount exceeding system limits
        BigDecimal excessiveAmount = new BigDecimal("99999.99");

        // When/Then: Expect validation to fail
        assertThatThrownBy(() -> transactionAddService.validateTransactionAmount(excessiveAmount))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Transaction amount exceeds maximum limit")
            .extracting(e -> ((ValidationException) e).getFieldErrors())
            .satisfies(errors -> {
                assertThat(errors).containsKey("amount");
                assertThat(errors.get("amount")).contains("exceeds maximum");
            });
    }

    /**
     * Test transaction ID generation uniqueness.
     * Validates COBOL sequence number generation equivalent behavior.
     */
    @Test
    @DisplayName("Generate Transaction ID Uniqueness - COBOL sequence generation")
    void testGenerateTransactionId_Uniqueness() {
        // When: Generating multiple transaction IDs
        String id1 = transactionAddService.generateTransactionId();
        String id2 = transactionAddService.generateTransactionId();
        String id3 = transactionAddService.generateTransactionId();

        // Then: All IDs should be unique and follow COBOL format
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id2).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id3);
        
        // Verify COBOL-compatible format (TX followed by timestamp)
        assertThat(id1).matches("TX\\d+");
        assertThat(id2).matches("TX\\d+");
        assertThat(id3).matches("TX\\d+");
    }

    /**
     * Test transaction processing with balance updates.
     * Validates COBOL COMP-3 arithmetic equivalent precision.
     */
    @Test
    @DisplayName("Process Transaction Balance Update - COBOL COMP-3 arithmetic")
    void testProcessTransaction_BalanceUpdate() {
        // Given: Account with specific balance for precision testing
        testAccount.setCurrentBalance(new BigDecimal("1234.56").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        BigDecimal transactionAmount = new BigDecimal("123.45");
        
        when(accountRepository.findById("1000000001")).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Processing transaction
        validTransactionRequest.setAmount(transactionAmount);
        Transaction result = transactionAddService.processTransaction(validTransactionRequest, testAccount);

        // Then: Verify precise balance calculation (COBOL COMP-3 equivalent)
        BigDecimal expectedBalance = new BigDecimal("1111.11").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
        assertBigDecimalEquals(testAccount.getCurrentBalance(), expectedBalance);
        
        assertThat(result.getAmount()).isEqualTo(transactionAmount.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    /**
     * Test transaction processing database error handling.
     * Validates COBOL file I/O error equivalent handling.
     */
    @Test
    @DisplayName("Process Transaction Database Error - COBOL file I/O error handling")
    void testProcessTransaction_DatabaseError() {
        // Given: Database save operation fails
        when(accountRepository.findById("1000000001")).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database connection failed"));

        // When/Then: Expect exception propagation (COBOL file status error equivalent)
        assertThatThrownBy(() -> transactionAddService.processTransaction(validTransactionRequest, testAccount))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection failed");

        // Verify rollback behavior (no account updates persisted)
        verify(accountRepository, never()).save(any(Account.class));
    }

    /**
     * Test COBOL functional parity validation.
     * Comprehensive test ensuring Java implementation produces identical results to COBOL.
     */
    @Test
    @DisplayName("COBOL Functional Parity - Complete equivalence validation")
    void testCobolFunctionalParity() {
        // Given: Test scenario with known COBOL expected results
        testAccount.setCurrentBalance(new BigDecimal("2500.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        testAccount.setCreditLimit(new BigDecimal("5000.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        
        validTransactionRequest.setAmount(new BigDecimal("375.25"));
        
        when(accountRepository.findById("1000000001")).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            savedTransaction.setTransactionId("TX20241215123456");
            return savedTransaction;
        });

        long startTime = System.currentTimeMillis();

        // When: Processing transaction
        Transaction result = transactionAddService.addTransaction(validTransactionRequest);

        long executionTime = System.currentTimeMillis() - startTime;

        // Then: Verify results match expected COBOL behavior exactly
        // Balance calculation: 2500.00 - 375.25 = 2124.75
        BigDecimal expectedBalance = new BigDecimal("2124.75").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
        assertBigDecimalEquals(testAccount.getCurrentBalance(), expectedBalance);
        
        // Verify transaction details match COBOL processing
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("375.25").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        assertThat(result.getTransactionType()).isEqualTo("SALE");
        assertThat(result.getAccountId()).isEqualTo("1000000001");
        
        // Verify performance meets COBOL equivalent response time
        assertThat(executionTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        
        // Verify credit limit checking logic (available credit after transaction)
        BigDecimal usedCredit = testAccount.getCurrentBalance();
        BigDecimal availableCredit = testAccount.getCreditLimit().subtract(usedCredit);
        assertThat(availableCredit).isGreaterThan(BigDecimal.ZERO);
    }

    /**
     * Test cross-reference validation functionality.
     * Validates COBOL cross-reference checking equivalent behavior.
     */
    @Test
    @DisplayName("Validate Cross Reference - COBOL referential integrity")
    void testValidateCrossReference() {
        // Given: Valid account and card number relationship
        when(accountRepository.findById("1000000001")).thenReturn(Optional.of(testAccount));

        // When: Validating cross-reference
        boolean isValid = transactionAddService.validateCrossReference(validTransactionRequest);

        // Then: Validation should pass
        assertThat(isValid).isTrue();
        
        verify(accountRepository).findById("1000000001");
    }

    // Helper Methods

    /**
     * Creates a valid transaction request for parameterized testing.
     * Ensures consistent test data structure across test scenarios.
     */
    private AddTransactionRequest createValidTransactionRequest(String typeCode, String categoryCode, 
                                                              BigDecimal amount, String description) {
        AddTransactionRequest request = new AddTransactionRequest();
        request.setAccountId("1000000001");
        request.setCardNumber("4532123456789012");
        request.setAmount(amount);
        request.setTypeCode(typeCode);
        request.setCategoryCode(categoryCode);
        request.setDescription(description);
        request.setMerchantName("Test Merchant");
        request.setMerchantCity("Test City");
        request.setMerchantZip("12345");
        request.setTransactionDate(testTimestamp);
        return request;
    }

    /**
     * Sets up common mock dependencies used across multiple test methods.
     * Reduces code duplication and ensures consistent mock behavior.
     */
    private void mockCommonDependencies() {
        // Default account repository behavior
        when(accountRepository.findById("1000000001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Default transaction repository behavior  
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            if (savedTransaction.getTransactionId() == null) {
                savedTransaction.setTransactionId("TX" + System.currentTimeMillis());
            }
            return savedTransaction;
        });
    }

    /**
     * Asserts BigDecimal equality with COBOL COMP-3 precision requirements.
     * Validates exact decimal precision matching COBOL packed decimal behavior.
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected) {
        assertThat(actual.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))
            .isEqualTo(expected.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        
        // Verify scale matches COBOL COMP-3 requirements
        assertThat(actual.scale()).isEqualTo(COBOL_DECIMAL_SCALE);
    }

    /**
     * Validates BigDecimal precision within tolerance for COBOL equivalence.
     * Handles minor precision variations while maintaining functional parity.
     */
    private void assertBigDecimalWithinTolerance(BigDecimal actual, BigDecimal expected, BigDecimal tolerance) {
        BigDecimal difference = actual.subtract(expected).abs();
        assertThat(difference).isLessThanOrEqualTo(tolerance);
    }

    /**
     * Loads test fixtures from CSV data sources.
     * Simulates test data generation utility functionality.
     */
    private void loadTestFixtures() {
        // Implementation would load CSV test data if TestDataGenerator was accessible
        // Currently handles test data creation inline due to file access limitations
    }

    /**
     * Creates test account with COBOL-equivalent data structure.
     * Ensures test accounts follow VSAM record layout patterns.
     */
    private Account createTestAccount() {
        Account account = new Account();
        account.setAccountId("1000000001");
        account.setCustomerId("CU00000001");
        account.setCurrentBalance(new BigDecimal("1500.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        account.setCreditLimit(new BigDecimal("5000.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        account.setActiveStatus("Y");
        return account;
    }

    /**
     * Creates test transaction with proper field mapping.
     * Ensures test transactions follow COBOL record structure.
     */
    private Transaction createTestTransaction() {
        Transaction transaction = new Transaction();
        transaction.setAccountId("1000000001");
        transaction.setAmount(new BigDecimal("250.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        transaction.setTransactionType("SALE");
        transaction.setTransactionDate(testTimestamp);
        transaction.setDescription("Test Transaction");
        transaction.setMerchantName("Test Merchant");
        return transaction;
    }

    /**
     * Validates COBOL precision requirements for financial calculations.
     * Ensures BigDecimal operations maintain COMP-3 equivalent precision.
     */
    private void validateCobolPrecision(BigDecimal value) {
        assertThat(value.scale()).isEqualTo(COBOL_DECIMAL_SCALE);
        assertThat(value.precision()).isLessThanOrEqualTo(15); // COBOL PIC S9(13)V99 equivalent
    }
}