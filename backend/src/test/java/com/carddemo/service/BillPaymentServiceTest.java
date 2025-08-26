package com.carddemo.service;

import com.carddemo.dto.AccountDto;
import com.carddemo.dto.BillDto;
import com.carddemo.dto.TransactionDto;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.test.TestDataGenerator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for BillPaymentService validating COBOL COBIL00C 
 * bill payment logic migration to Java, testing payment processing, balance updates, 
 * payment validation, and transaction recording with 100% functional parity validation.
 * 
 * Tests focus on:
 * - Payment amount validation with COBOL precision equivalence
 * - Balance update calculations with COMP-3 BigDecimal precision matching
 * - Payment transaction creation and recording
 * - Business rule enforcement and error handling
 * - Insufficient funds detection and handling
 * - COBOL-to-Java functional parity validation
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillPaymentService - COBIL00C Migration Tests")
class BillPaymentServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TestDataGenerator testDataGenerator;

    @Mock
    private CobolComparisonUtils cobolComparisonUtils;

    @InjectMocks
    private BillPaymentService billPaymentService;

    private Account testAccount;
    private BillDto testBillDto;
    private TransactionDto testTransactionDto;
    private Transaction testTransaction;

    /**
     * Setup test data before each test execution using COBOL-compliant test patterns.
     * Creates standardized test entities with COMP-3 precision BigDecimal values.
     */
    @BeforeEach
    void setUp() {
        // Create test account with COBOL COMP-3 equivalent precision
        testAccount = new Account();
        testAccount.setAccountId("1000000001");
        testAccount.setCurrentBalance(new BigDecimal("1250.75").setScale(2, RoundingMode.HALF_UP));
        testAccount.setCreditLimit(new BigDecimal("5000.00").setScale(2, RoundingMode.HALF_UP));
        testAccount.setActiveStatus("Y");

        // Create test bill DTO matching BMS COBIL00 screen structure
        testBillDto = new BillDto();
        testBillDto.setAccountId("1000000001");
        testBillDto.setStatementBalance(new BigDecimal("875.50").setScale(2, RoundingMode.HALF_UP));
        testBillDto.setMinimumPayment(new BigDecimal("35.00").setScale(2, RoundingMode.HALF_UP));
        testBillDto.setDueDate(LocalDateTime.now().plusDays(30));

        // Create test transaction DTO for payment processing
        testTransactionDto = new TransactionDto();
        testTransactionDto.setAccountId("1000000001");
        testTransactionDto.setAmount(new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP));
        testTransactionDto.setTypeCode("PAY");
        testTransactionDto.setCategoryCode("BILLPAY");

        // Create test transaction entity for persistence
        testTransaction = new Transaction();
        testTransaction.setAccountId("1000000001");
        testTransaction.setAmount(new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP));
        testTransaction.setDescription("Bill Payment");
        testTransaction.setTransactionType("PAY");
    }

    /**
     * Test successful bill payment processing with valid account and payment amount.
     * Validates complete payment workflow including balance updates and transaction creation.
     */
    @Test
    @DisplayName("processBillPayment - successful payment processing with valid inputs")
    void testProcessBillPayment_SuccessfulPayment_ValidInputs() {
        // Given: Valid account exists with sufficient available credit
        when(accountRepository.findById(anyString())).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        BigDecimal paymentAmount = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP);

        // When: Processing bill payment
        TransactionDto result = billPaymentService.processBillPayment(
            testAccount.getAccountId(), paymentAmount);

        // Then: Payment processed successfully
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(testAccount.getAccountId());
        assertThat(result.getAmount()).isEqualTo(paymentAmount);
        assertThat(result.getTypeCode()).isEqualTo("PAY");

        // Verify repository interactions
        verify(accountRepository).findById(testAccount.getAccountId());
        verify(accountRepository).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    /**
     * Test payment processing with insufficient funds scenario.
     * Validates proper exception handling and error reporting for insufficient available credit.
     */
    @Test
    @DisplayName("processBillPayment - insufficient funds exception handling")
    void testProcessBillPayment_InsufficientFunds_ThrowsBusinessRuleException() {
        // Given: Account with insufficient available credit
        testAccount.setCurrentBalance(new BigDecimal("4950.00").setScale(2, RoundingMode.HALF_UP));
        when(accountRepository.findById(anyString())).thenReturn(Optional.of(testAccount));

        BigDecimal paymentAmount = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP);

        // When/Then: Payment processing throws BusinessRuleException for insufficient funds
        assertThatThrownBy(() -> billPaymentService.processBillPayment(
            testAccount.getAccountId(), paymentAmount))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient available credit for payment");

        // Verify no database updates occurred
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    /**
     * Test payment validation with invalid payment amount scenarios.
     * Validates proper input validation and error handling for negative and zero amounts.
     */
    @Test
    @DisplayName("validatePaymentAmount - invalid payment amounts throw validation exceptions")
    void testValidatePaymentAmount_InvalidAmounts_ThrowsValidationException() {
        // Test negative payment amount
        BigDecimal negativeAmount = new BigDecimal("-50.00");
        assertThatThrownBy(() -> billPaymentService.validatePaymentAmount(negativeAmount))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Payment amount must be positive");

        // Test zero payment amount
        BigDecimal zeroAmount = BigDecimal.ZERO;
        assertThatThrownBy(() -> billPaymentService.validatePaymentAmount(zeroAmount))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Payment amount must be positive");

        // Test null payment amount
        assertThatThrownBy(() -> billPaymentService.validatePaymentAmount(null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Payment amount is required");
    }

    /**
     * Test payment validation with valid payment amounts.
     * Validates proper acceptance of valid payment amounts with correct precision.
     */
    @Test
    @DisplayName("validatePaymentAmount - valid payment amounts pass validation")
    void testValidatePaymentAmount_ValidAmounts_PassValidation() {
        // Given: Valid payment amounts with proper COMP-3 precision
        BigDecimal validAmount1 = new BigDecimal("25.50").setScale(2, RoundingMode.HALF_UP);
        BigDecimal validAmount2 = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP);
        BigDecimal validAmount3 = new BigDecimal("0.01").setScale(2, RoundingMode.HALF_UP);

        // When/Then: Valid amounts should not throw exceptions
        assertThatCode(() -> billPaymentService.validatePaymentAmount(validAmount1))
            .doesNotThrowAnyException();
        assertThatCode(() -> billPaymentService.validatePaymentAmount(validAmount2))
            .doesNotThrowAnyException();
        assertThatCode(() -> billPaymentService.validatePaymentAmount(validAmount3))
            .doesNotThrowAnyException();
    }

    /**
     * Test new balance calculation with COBOL COMP-3 precision equivalence.
     * Validates BigDecimal calculations match COBOL packed decimal behavior.
     */
    @Test
    @DisplayName("calculateNewBalance - COMP-3 precision equivalence validation")
    void testCalculateNewBalance_Comp3PrecisionEquivalence_ValidCalculation() {
        // Given: Current balance and payment amount with COBOL COMP-3 precision
        BigDecimal currentBalance = new BigDecimal("1250.75").setScale(2, RoundingMode.HALF_UP);
        BigDecimal paymentAmount = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedNewBalance = new BigDecimal("1350.75").setScale(2, RoundingMode.HALF_UP);

        when(cobolComparisonUtils.compareBigDecimals(any(BigDecimal.class), any(BigDecimal.class)))
            .thenReturn(true);

        // When: Calculating new balance
        BigDecimal newBalance = billPaymentService.calculateNewBalance(currentBalance, paymentAmount);

        // Then: New balance calculated with COBOL precision
        assertThat(newBalance).isEqualTo(expectedNewBalance);
        assertThat(newBalance.scale()).isEqualTo(2);
        assertThat(newBalance.precision()).isLessThanOrEqualTo(15); // COBOL PIC S9(13)V99 equivalent

        // Verify COBOL comparison validation
        verify(cobolComparisonUtils).compareBigDecimals(newBalance, expectedNewBalance);
    }

    /**
     * Test balance calculation with edge cases and precision scenarios.
     * Validates proper handling of decimal precision in financial calculations.
     */
    @Test
    @DisplayName("calculateNewBalance - edge cases and precision scenarios")
    void testCalculateNewBalance_EdgeCasesAndPrecision_ValidCalculations() {
        // Test fractional penny amounts
        BigDecimal balance1 = new BigDecimal("999.99").setScale(2, RoundingMode.HALF_UP);
        BigDecimal payment1 = new BigDecimal("0.01").setScale(2, RoundingMode.HALF_UP);
        BigDecimal result1 = billPaymentService.calculateNewBalance(balance1, payment1);
        assertThat(result1).isEqualTo(new BigDecimal("1000.00").setScale(2, RoundingMode.HALF_UP));

        // Test large payment amounts
        BigDecimal balance2 = new BigDecimal("500.00").setScale(2, RoundingMode.HALF_UP);
        BigDecimal payment2 = new BigDecimal("4500.00").setScale(2, RoundingMode.HALF_UP);
        BigDecimal result2 = billPaymentService.calculateNewBalance(balance2, payment2);
        assertThat(result2).isEqualTo(new BigDecimal("5000.00").setScale(2, RoundingMode.HALF_UP));

        // Test precision preservation
        BigDecimal balance3 = new BigDecimal("1234.56").setScale(2, RoundingMode.HALF_UP);
        BigDecimal payment3 = new BigDecimal("876.44").setScale(2, RoundingMode.HALF_UP);
        BigDecimal result3 = billPaymentService.calculateNewBalance(balance3, payment3);
        assertThat(result3).isEqualTo(new BigDecimal("2111.00").setScale(2, RoundingMode.HALF_UP));
        assertThat(result3.scale()).isEqualTo(2);
    }

    /**
     * Test transaction ID generation with COBOL-compatible patterns.
     * Validates unique transaction identifier creation with proper format.
     */
    @Test
    @DisplayName("generateTransactionId - COBOL-compatible transaction ID generation")
    void testGenerateTransactionId_CobolCompatibleFormat_ValidGeneration() {
        // When: Generating transaction IDs
        String transactionId1 = billPaymentService.generateTransactionId();
        String transactionId2 = billPaymentService.generateTransactionId();

        // Then: Transaction IDs are unique and properly formatted
        assertThat(transactionId1).isNotNull();
        assertThat(transactionId2).isNotNull();
        assertThat(transactionId1).isNotEqualTo(transactionId2);
        
        // Validate COBOL-compatible format (15 digits max for PIC S9(15) COMP-3)
        assertThat(transactionId1).matches("\\d{10,15}");
        assertThat(transactionId2).matches("\\d{10,15}");
        
        // Validate length constraints
        assertThat(transactionId1.length()).isBetween(10, 15);
        assertThat(transactionId2.length()).isBetween(10, 15);
    }

    /**
     * Test account balance update with proper persistence and validation.
     * Validates balance updates are saved correctly with proper precision.
     */
    @Test
    @DisplayName("updateAccountBalance - successful balance update with persistence")
    void testUpdateAccountBalance_SuccessfulUpdate_BalancePersisted() {
        // Given: Account with current balance and payment amount
        BigDecimal originalBalance = testAccount.getCurrentBalance();
        BigDecimal paymentAmount = new BigDecimal("150.00").setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedNewBalance = originalBalance.add(paymentAmount);

        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When: Updating account balance
        Account updatedAccount = billPaymentService.updateAccountBalance(testAccount, paymentAmount);

        // Then: Balance updated correctly
        assertThat(updatedAccount.getCurrentBalance()).isEqualTo(expectedNewBalance);
        assertThat(updatedAccount.getCurrentBalance().scale()).isEqualTo(2);

        // Verify repository save was called
        verify(accountRepository).save(testAccount);
    }

    /**
     * Test payment transaction creation with proper field mapping.
     * Validates transaction entity creation with all required fields populated.
     */
    @Test
    @DisplayName("createPaymentTransaction - complete transaction entity creation")
    void testCreatePaymentTransaction_CompleteEntityCreation_AllFieldsPopulated() {
        // Given: Account and payment details
        BigDecimal paymentAmount = new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP);
        String generatedTransactionId = "1234567890123";

        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When: Creating payment transaction
        Transaction createdTransaction = billPaymentService.createPaymentTransaction(
            testAccount, paymentAmount, generatedTransactionId);

        // Then: Transaction created with all required fields
        assertThat(createdTransaction).isNotNull();
        assertThat(createdTransaction.getAccountId()).isEqualTo(testAccount.getAccountId());
        assertThat(createdTransaction.getAmount()).isEqualTo(paymentAmount);
        assertThat(createdTransaction.getTransactionId()).isEqualTo(generatedTransactionId);
        assertThat(createdTransaction.getTransactionType()).isEqualTo("PAY");
        assertThat(createdTransaction.getDescription()).isEqualTo("Bill Payment");

        // Verify repository save was called
        verify(transactionRepository).save(any(Transaction.class));
    }

    /**
     * Test account access validation with various account statuses.
     * Validates proper authorization checking and error handling.
     */
    @Test
    @DisplayName("validateAccountAccess - account status validation scenarios")
    void testValidateAccountAccess_AccountStatusValidation_ProperErrorHandling() {
        // Test active account - should pass
        testAccount.setActiveStatus("Y");
        assertThatCode(() -> billPaymentService.validateAccountAccess(testAccount))
            .doesNotThrowAnyException();

        // Test inactive account - should throw exception
        testAccount.setActiveStatus("N");
        assertThatThrownBy(() -> billPaymentService.validateAccountAccess(testAccount))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Account is not active");

        // Test suspended account - should throw exception
        testAccount.setActiveStatus("S");
        assertThatThrownBy(() -> billPaymentService.validateAccountAccess(testAccount))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Account is suspended");

        // Test null account - should throw exception
        assertThatThrownBy(() -> billPaymentService.validateAccountAccess(null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Account is required");
    }

    /**
     * Test insufficient funds checking with various balance scenarios.
     * Validates proper available credit calculation and limit enforcement.
     */
    @Test
    @DisplayName("checkInsufficientFunds - available credit validation scenarios")
    void testCheckInsufficientFunds_AvailableCreditValidation_ProperLimitEnforcement() {
        // Test sufficient available credit - should pass
        testAccount.setCurrentBalance(new BigDecimal("1000.00"));
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        BigDecimal payment1 = new BigDecimal("500.00");
        
        assertThatCode(() -> billPaymentService.checkInsufficientFunds(testAccount, payment1))
            .doesNotThrowAnyException();

        // Test insufficient available credit - should throw exception
        testAccount.setCurrentBalance(new BigDecimal("4900.00"));
        BigDecimal payment2 = new BigDecimal("200.00");
        
        assertThatThrownBy(() -> billPaymentService.checkInsufficientFunds(testAccount, payment2))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient available credit");

        // Test payment exactly at limit - should throw exception
        testAccount.setCurrentBalance(new BigDecimal("4950.00"));
        BigDecimal payment3 = new BigDecimal("50.00");
        
        assertThatThrownBy(() -> billPaymentService.checkInsufficientFunds(testAccount, payment3))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient available credit");
    }

    /**
     * Test account not found scenario handling.
     * Validates proper exception handling when account doesn't exist.
     */
    @Test
    @DisplayName("processBillPayment - account not found exception handling")
    void testProcessBillPayment_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given: Account does not exist in repository
        String nonExistentAccountId = "9999999999";
        when(accountRepository.findById(nonExistentAccountId)).thenReturn(Optional.empty());

        BigDecimal paymentAmount = new BigDecimal("100.00");

        // When/Then: Processing payment for non-existent account throws exception
        assertThatThrownBy(() -> billPaymentService.processBillPayment(
            nonExistentAccountId, paymentAmount))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Account not found")
            .hasMessageContaining(nonExistentAccountId);

        // Verify repository was called but no updates occurred
        verify(accountRepository).findById(nonExistentAccountId);
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    /**
     * Test financial precision validation with COBOL comparison utilities.
     * Validates BigDecimal operations maintain COBOL COMP-3 equivalence.
     */
    @Test
    @DisplayName("Financial precision validation - COBOL COMP-3 equivalence testing")
    void testFinancialPrecisionValidation_CobolComp3Equivalence_ValidPrecision() {
        // Given: Financial amounts requiring precise calculation
        BigDecimal principal = new BigDecimal("1000.00").setScale(2, RoundingMode.HALF_UP);
        BigDecimal payment = new BigDecimal("123.45").setScale(2, RoundingMode.HALF_UP);

        when(cobolComparisonUtils.validateFinancialPrecision(any(BigDecimal.class)))
            .thenReturn(true);
        when(cobolComparisonUtils.generateComparisonReport(any(BigDecimal.class), any(BigDecimal.class)))
            .thenReturn("Precision validation passed");

        // When: Performing financial calculation
        BigDecimal result = billPaymentService.calculateNewBalance(principal, payment);

        // Then: Result maintains COBOL precision standards
        assertThat(result.scale()).isEqualTo(2);
        assertThat(result.precision()).isLessThanOrEqualTo(15);

        // Verify COBOL comparison utilities were used
        verify(cobolComparisonUtils).validateFinancialPrecision(result);
        verify(cobolComparisonUtils).generateComparisonReport(any(BigDecimal.class), any(BigDecimal.class));
    }

    /**
     * Test complete bill payment workflow with all validation steps.
     * Integration-style unit test validating the complete payment process.
     */
    @Test
    @DisplayName("Complete bill payment workflow - end-to-end payment processing")
    void testCompleteBillPaymentWorkflow_EndToEndProcessing_ValidWorkflow() {
        // Given: Valid account and payment scenario
        BigDecimal paymentAmount = new BigDecimal("75.25").setScale(2, RoundingMode.HALF_UP);
        String generatedTransactionId = "1234567890123";
        
        when(accountRepository.findById(testAccount.getAccountId()))
            .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
            .thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(testTransaction);

        // When: Processing complete bill payment
        TransactionDto result = billPaymentService.processBillPayment(
            testAccount.getAccountId(), paymentAmount);

        // Then: Complete workflow executed successfully
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(testAccount.getAccountId());
        assertThat(result.getAmount()).isEqualTo(paymentAmount);

        // Verify all repository operations occurred in correct sequence
        verify(accountRepository).findById(testAccount.getAccountId());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    /**
     * Test error handling with validation exception field errors.
     * Validates proper field-level error handling and reporting.
     */
    @Test
    @DisplayName("Error handling - validation exception with field errors")
    void testErrorHandling_ValidationExceptionWithFieldErrors_ProperErrorReporting() {
        // Given: Payment amount with too many decimal places
        BigDecimal invalidPrecisionAmount = new BigDecimal("100.123");

        // When/Then: Validation should detect precision error
        assertThatThrownBy(() -> billPaymentService.validatePaymentAmount(invalidPrecisionAmount))
            .isInstanceOf(ValidationException.class)
            .satisfies(ex -> {
                ValidationException valEx = (ValidationException) ex;
                assertThat(valEx.hasFieldErrors()).isTrue();
                assertThat(valEx.getFieldErrors()).containsKey("paymentAmount");
                assertThat(valEx.getFieldErrors().get("paymentAmount"))
                    .contains("must have exactly 2 decimal places");
            });
    }
}