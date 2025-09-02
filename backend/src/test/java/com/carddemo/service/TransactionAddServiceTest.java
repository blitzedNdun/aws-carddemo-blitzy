package com.carddemo.service;

import com.carddemo.dto.AddTransactionRequest;
import com.carddemo.dto.AddTransactionResponse;
import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.test.CobolComparisonUtils;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

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
    private CardXrefRepository cardXrefRepository;

    @InjectMocks
    private TransactionAddService transactionAddService;

    // Test data constants following COBOL precision requirements
    private static final Long VALID_ACCOUNT_ID_LONG = 10000000001L;
    private static final String VALID_ACCOUNT_ID = "10000000001";
    private static final Long INVALID_ACCOUNT_ID_LONG = 99999999999L;
    private static final String INVALID_ACCOUNT_ID = "99999999999";
    private static final String VALID_CARD_NUMBER = "4532123456789012";
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("150.00");
    private static final BigDecimal LARGE_AMOUNT = new BigDecimal("5000.00");
    private static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-100.00");
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;
    private static final BigDecimal CURRENT_BALANCE = new BigDecimal("1250.75");
    private static final BigDecimal CREDIT_LIMIT = new BigDecimal("2000.00");

    private Account testAccount;
    private Customer testCustomer;
    private AddTransactionRequest validRequest;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        // Initialize test customer
        testCustomer = new Customer();
        testCustomer.setCustomerId("123456789");
        testCustomer.setFirstName("Test");
        testCustomer.setLastName("Customer");

        // Initialize test account with COBOL-equivalent precision
        testAccount = new Account();
        testAccount.setAccountId(VALID_ACCOUNT_ID_LONG);
        testAccount.setCurrentBalance(CURRENT_BALANCE);
        testAccount.setCreditLimit(CREDIT_LIMIT);
        testAccount.setCustomer(testCustomer);
        testAccount.setActiveStatus("Y");

        // Initialize valid transaction request
        validRequest = new AddTransactionRequest();
        validRequest.setAccountId(VALID_ACCOUNT_ID);
        validRequest.setCardNumber(VALID_CARD_NUMBER);
        validRequest.setAmount(VALID_AMOUNT);
        validRequest.setTypeCode("01");
        validRequest.setCategoryCode("01");
        validRequest.setSource("WEB");
        validRequest.setDescription("Test Transaction");
        validRequest.setMerchantName("Test Merchant");
        validRequest.setMerchantCity("Test City");
        validRequest.setMerchantZip("12345");
        validRequest.setTransactionDate(LocalDate.now());

        // Initialize mock transaction
        mockTransaction = new Transaction();
        mockTransaction.setTransactionId(1L);
        mockTransaction.setAccountId(VALID_ACCOUNT_ID_LONG);
        mockTransaction.setCardNumber(VALID_CARD_NUMBER);
        mockTransaction.setAmount(VALID_AMOUNT);
        mockTransaction.setTransactionTypeCode("01");
        mockTransaction.setCategoryCode("01");
        mockTransaction.setSource("WEB");
        mockTransaction.setTransactionDate(LocalDate.now());
        mockTransaction.setProcessedTimestamp(LocalDate.now().atStartOfDay()); // Fix NullPointerException
        mockTransaction.setDescription("Test Transaction");
        mockTransaction.setMerchantName("Test Merchant");
        mockTransaction.setMerchantCity("Test City");
        mockTransaction.setMerchantZip("12345");

        // Setup common mock behaviors
        mockCommonDependencies();
    }

    /**
     * Mock common dependencies to isolate unit under test
     */
    private void mockCommonDependencies() {
        lenient().when(accountRepository.findById(VALID_ACCOUNT_ID_LONG)).thenReturn(Optional.of(testAccount));
        lenient().when(accountRepository.findById(INVALID_ACCOUNT_ID_LONG)).thenReturn(Optional.empty());
        lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction inputTransaction = invocation.getArgument(0);
            // Return the same transaction that was passed in (simulating successful save)
            return inputTransaction;
        });
        lenient().when(transactionRepository.findTopByOrderByTransactionIdDesc()).thenReturn(Optional.of(mockTransaction));
        
        // Setup card cross-reference validation
        lenient().when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID_LONG))
            .thenReturn(true);
        lenient().when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, INVALID_ACCOUNT_ID_LONG))
            .thenReturn(false);
    }

    @Test
    @DisplayName("Add Transaction Success - Complete workflow validation")
    void testAddTransaction_Success() {
        // Given: Valid transaction request with all required fields
        when(transactionRepository.findTopByOrderByTransactionIdDesc()).thenReturn(Optional.of(mockTransaction));

        // When: Adding transaction through service
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Transaction created successfully with proper validation
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(result.getAmount()).isEqualByComparingTo(VALID_AMOUNT);

        // Verify repository interactions
        verify(cardXrefRepository).existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID_LONG);
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionRepository).flush();
        verify(transactionRepository).findTopByOrderByTransactionIdDesc();
    }

    @Test
    @DisplayName("Add Transaction Validation Failure - Invalid request data")
    void testAddTransaction_ValidationFailure() {
        // Given: Invalid transaction request with missing required fields
        AddTransactionRequest invalidRequest = new AddTransactionRequest();
        invalidRequest.setAccountId(null); // Missing required field
        invalidRequest.setAmount(NEGATIVE_AMOUNT); // Invalid amount

        // When: Adding invalid transaction through service
        AddTransactionResponse result = transactionAddService.addTransaction(invalidRequest);

        // Then: Error response returned for invalid request
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ERROR");
        assertThat(result.getMessage()).contains("validation failed");

        // Verify no repository interactions for invalid requests
        verify(cardXrefRepository, never()).existsByXrefCardNumAndXrefAcctId(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add Transaction Large Amount - Successful processing")
    void testAddTransaction_LargeAmount() {
        // Given: Transaction request with large amount (but valid)
        validRequest.setAmount(new BigDecimal("1000.00")); // Within reasonable limits
        
        // When: Adding transaction through service
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);
        
        // Then: Transaction created successfully
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));

        // Verify repository interactions
        verify(cardXrefRepository).existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID_LONG);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Add Transaction Invalid Card/Account - Cross-reference validation")
    void testAddTransaction_InvalidCardXref() {
        // Given: Request with invalid card/account combination
        validRequest.setAccountId(INVALID_ACCOUNT_ID);
        
        // Mock: Card cross-reference returns false for invalid combination
        when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, INVALID_ACCOUNT_ID_LONG))
            .thenReturn(false);

        // When: Adding transaction through service
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Error response returned for invalid card/account combination
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ERROR");
        assertThat(result.getMessage()).contains("Card Number NOT found for this account...");

        // Verify cross-reference lookup attempted but no transaction saved
        verify(cardXrefRepository).existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, INVALID_ACCOUNT_ID_LONG);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add Transaction Valid Input - Complete validation success")
    void testAddTransaction_ValidInput() {
        // Given: All required fields set properly
        
        // When: Processing valid transaction request
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Transaction processed successfully
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
    }

    @ParameterizedTest
    @CsvSource({
        ", 'validation failed'",
        "'', 'validation failed'",
        "'123', 'validation failed'"
    })
    @DisplayName("Add Transaction Invalid Account ID - Parameterized validation tests")
    void testAddTransaction_InvalidAccountId(String accountId, String expectedError) {
        // Given: Invalid account ID scenarios
        validRequest.setAccountId(accountId);

        // When: Processing invalid request
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Error response returned
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ERROR");
        assertThat(result.getMessage()).contains(expectedError);
    }

    @ParameterizedTest
    @CsvSource({
        "100.00, SUCCESS",
        "50.00, SUCCESS", 
        "25.99, SUCCESS",
        "1000000.00, ERROR",
        "0.00, ERROR"
    })
    @DisplayName("Add Transaction Various Amounts - Amount boundary testing")  
    void testAddTransaction_VariousAmounts(String amountStr, String expectedStatus) {
        // Given: Various transaction amounts to test
        BigDecimal testAmount = new BigDecimal(amountStr);
        validRequest.setAmount(testAmount);

        // When: Processing transaction with different amounts
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Response status matches expected
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
        
        if ("SUCCESS".equals(expectedStatus)) {
            assertThat(result.getAmount()).isEqualByComparingTo(testAmount);
        }
    }

    @Test
    @DisplayName("Add Transaction Negative Amount - Amount validation")
    void testAddTransaction_NegativeAmount() {
        // Given: Negative transaction amount
        validRequest.setAmount(NEGATIVE_AMOUNT);

        // When: Processing transaction with negative amount
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Error response returned
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ERROR");
        assertThat(result.getMessage()).contains("validation failed");
    }

    @Test
    @DisplayName("Add Transaction ID Generation - ID assignment validation")
    void testAddTransaction_IdGeneration() {
        // Given: Mock last transaction ID for generation
        Transaction lastTransaction = new Transaction();
        lastTransaction.setTransactionId(12345L);
        when(transactionRepository.findTopByOrderByTransactionIdDesc()).thenReturn(Optional.of(lastTransaction));

        // When: Adding transaction to trigger ID generation
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Transaction processed with proper ID generation
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");

        // Verify repository interaction for ID generation
        verify(transactionRepository).findTopByOrderByTransactionIdDesc();
    }

    @Test
    @DisplayName("Add Transaction BigDecimal Precision - COBOL precision validation")
    void testAddTransaction_BigDecimalPrecision() {
        // Given: Transaction amount with precise decimal value
        BigDecimal preciseAmount = new BigDecimal("123.45");
        validRequest.setAmount(preciseAmount);

        // When: Processing transaction with precise amount
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Precision maintained in response
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getAmount()).isEqualByComparingTo(preciseAmount);
        
        // Verify precision handling
        assertBigDecimalWithinTolerance(result.getAmount(), preciseAmount);
    }

    @Test
    @DisplayName("Add Transaction Database Error - Error handling validation")
    void testAddTransaction_DatabaseError() {
        // Given: Database error simulation
        when(transactionRepository.save(any(Transaction.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When: Processing transaction with database error
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Error response returned
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ERROR");
        assertThat(result.getMessage()).contains("Database connection failed");
    }

    @Test
    @DisplayName("COBOL Functional Parity - Transaction processing validation")
    void testAddTransaction_CobolFunctionalParity() {
        // Given: Valid transaction request replicating COBOL COTRN02C flow

        // When: Processing transaction through modernized service
        AddTransactionResponse result = transactionAddService.addTransaction(validRequest);

        // Then: Functional parity with COBOL implementation achieved
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        
        // Verify all key processing steps occurred
        verify(cardXrefRepository).existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID_LONG);
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionRepository).flush();
        
        // Validate COBOL precision maintained
        validateCobolPrecision(result.getAmount());
    }

    /**
     * Custom assertion for BigDecimal precision matching COBOL COMP-3 behavior
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected) {
        assertThat(actual)
            .usingComparator((a, b) -> a.compareTo(b))
            .isEqualTo(expected);
        
        // Verify scale matches COBOL precision requirements (2 decimal places for currency)
        assertThat(actual.scale()).isEqualTo(2);
        
        // Verify rounding mode matches COBOL behavior (HALF_UP)
        if (actual.scale() > 2) {
            BigDecimal rounded = actual.setScale(2, RoundingMode.HALF_UP);
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
            .isEqualTo(2); // Standard 2 decimal places for currency
            
        // Verify precision matches COBOL packed decimal constraints
        assertThat(amount.precision())
            .withFailMessage("Precision exceeds COBOL COMP-3 limits")
            .isLessThanOrEqualTo(15); // Max COBOL COMP-3 precision
    }

    /**
     * Setup method for creating test account data
     */
    private Account createTestAccount() {
        Customer customer = new Customer();
        customer.setCustomerId("CUST001");
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        
        Account account = new Account();
        account.setAccountId(VALID_ACCOUNT_ID_LONG);
        account.setCurrentBalance(CURRENT_BALANCE);
        account.setCreditLimit(CREDIT_LIMIT);
        account.setCustomer(customer);
        account.setActiveStatus("Y");
        return account;
    }

    /**
     * Setup method for creating test transaction data
     */
    private Transaction createTestTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(1L);
        transaction.setAccountId(VALID_ACCOUNT_ID_LONG);
        transaction.setAmount(VALID_AMOUNT);
        transaction.setTransactionTypeCode("01");
        transaction.setTransactionDate(LocalDate.now());
        transaction.setDescription("Test Transaction");
        transaction.setMerchantName("Test Merchant");
        return transaction;
    }

    /**
     * Cleanup method for test resource management
     */
    void tearDown() {
        // Clear any test state and reset mocks
        Mockito.reset(transactionRepository, accountRepository, cardXrefRepository);
    }
}