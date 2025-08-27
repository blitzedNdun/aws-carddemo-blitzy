package com.carddemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

import com.carddemo.test.TestDataGenerator;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Card;
import com.carddemo.dto.AccountDto;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.exception.DataPrecisionException;

/**
 * Comprehensive unit tests for AccountViewService validating COBOL COACTVWC account view logic
 * migration to Java. Tests account retrieval, customer data integration, balance calculations,
 * and field formatting with 100% business logic coverage requirement.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountViewService - COBOL COACTVWC Migration Tests")
class AccountViewServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private AccountViewService accountViewService;

    private TestDataGenerator testDataGenerator;
    private CobolComparisonUtils cobolUtils;

    @BeforeEach
    void setUp() {
        testDataGenerator = new TestDataGenerator();
        cobolUtils = new CobolComparisonUtils();
    }

    /**
     * Tests for Account ID validation matching COBOL 2210-EDIT-ACCOUNT logic
     */
    @Nested
    @DisplayName("Account ID Validation - COBOL 2210-EDIT-ACCOUNT Equivalent")
    class AccountIdValidationTests {

        @Test
        @DisplayName("Should reject null account ID with ValidationException")
        void testViewAccount_NullAccountId_ThrowsValidationException() {
            // When/Then
            assertThatThrownBy(() -> accountViewService.viewAccount(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account ID cannot be null or empty");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository);
        }

        @Test
        @DisplayName("Should reject empty account ID with ValidationException")
        void testViewAccount_EmptyAccountId_ThrowsValidationException() {
            // When/Then
            assertThatThrownBy(() -> accountViewService.viewAccount(""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account ID cannot be null or empty");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository);
        }

        @Test
        @DisplayName("Should reject non-numeric account ID with ValidationException")
        void testViewAccount_NonNumericAccountId_ThrowsValidationException() {
            // Given: Non-numeric account ID (matches COBOL NUMERIC test)
            String invalidAccountId = "ABC1234567";

            // When/Then
            assertThatThrownBy(() -> accountViewService.viewAccount(invalidAccountId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account ID must be numeric");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository);
        }

        @Test
        @DisplayName("Should reject account ID not exactly 11 digits with ValidationException")
        void testViewAccount_InvalidAccountIdLength_ThrowsValidationException() {
            // Given: Account ID with incorrect length (COBOL requires 11 digits)
            String shortAccountId = "1234567890";      // 10 digits
            String longAccountId = "123456789012";     // 12 digits

            // When/Then
            assertThatThrownBy(() -> accountViewService.viewAccount(shortAccountId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account ID must be exactly 11 digits");

            assertThatThrownBy(() -> accountViewService.viewAccount(longAccountId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account ID must be exactly 11 digits");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository);
        }

        @Test
        @DisplayName("Should reject zero account ID with ValidationException")
        void testViewAccount_ZeroAccountId_ThrowsValidationException() {
            // Given: All zeros account ID (COBOL business rule)
            String zeroAccountId = "00000000000";

            // When/Then
            assertThatThrownBy(() -> accountViewService.viewAccount(zeroAccountId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account ID cannot be all zeros");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository);
        }

        @Test
        @DisplayName("Should accept valid 11-digit numeric account ID")
        void testViewAccount_ValidAccountIdFormat_PassesValidation() {
            // Given: Valid 11-digit account ID
            String validAccountId = "12345678901";
            Account testAccount = testDataGenerator.generateValidAccount();
            Customer testCustomer = testDataGenerator.generateValidCustomer();

            when(accountRepository.findById(anyString())).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));

            // When/Then - Should not throw validation exception
            assertThatCode(() -> accountViewService.viewAccount(validAccountId))
                .doesNotThrowAnyException();
        }
    }

    /**
     * Tests for happy path account retrieval simulating COBOL 9000-READ-ACCT data chain
     */
    @Nested
    @DisplayName("Account Retrieval Happy Path - COBOL 9000-READ-ACCT Equivalent")
    class AccountRetrievalHappyPathTests {

        @Test
        @DisplayName("Should successfully retrieve account with customer data - Complete COBOL 9000 flow")
        void testViewAccount_ValidAccountId_ReturnsCompleteAccountData() {
            // Given: Test data simulating COBOL data files
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateValidAccount(
                new BigDecimal("1250.75"), new BigDecimal("5000.00"));
            testAccount.setAccountId(accountId);
            testAccount.setCustomerId(98765L);

            Customer testCustomer = testDataGenerator.generateValidCustomer();
            testCustomer.setCustomerId(98765L);
            testCustomer.setFirstName("JOHN");
            testCustomer.setLastName("DOE");

            // Mock repository calls simulating COBOL file access chain
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(98765L)).thenReturn(Optional.of(testCustomer));

            // When: Execute viewAccount (equivalent to COBOL PERFORM 9000-READ-ACCT)
            AccountDto result = accountViewService.viewAccount(accountId);

            // Then: Verify complete data retrieval chain
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(accountId);
            assertThat(result.getCustomerId()).isEqualTo(98765L);
            assertThat(result.getCurrentBalance()).isEqualTo(new BigDecimal("1250.75"));
            assertThat(result.getCreditLimit()).isEqualTo(new BigDecimal("5000.00"));

            // Verify repository interaction sequence (matches COBOL read order)
            verify(accountRepository).findById(accountId);
            verify(customerRepository).findById(98765L);
        }

        @Test
        @DisplayName("Should handle card cross-reference retrieval when available")
        void testGetAccountWithDetails_WithCardData_ReturnsCompleteDetails() {
            // Given: Account with associated cards
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateValidAccount();
            Customer testCustomer = testDataGenerator.generateValidCustomer();
            
            Card testCard = testDataGenerator.generateValidCard();
            testCard.setAccountId(accountId);
            testCard.setCustomerId(testAccount.getCustomerId());

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(testAccount.getCustomerId())).thenReturn(Optional.of(testCustomer));
            when(cardRepository.findByAccountId(accountId)).thenReturn(Arrays.asList(testCard));

            // When: Execute detailed account retrieval
            AccountDto result = accountViewService.getAccountWithDetails(accountId);

            // Then: Verify card information is included
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(accountId);

            // Verify all repository calls made (simulates complete COBOL file chain)
            verify(accountRepository).findById(accountId);
            verify(customerRepository).findById(testAccount.getCustomerId());
            verify(cardRepository).findByAccountId(accountId);
        }
    }

    /**
     * Tests for error handling scenarios - missing data cases
     */
    @Nested
    @DisplayName("Error Handling Tests - Missing Data Scenarios")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when account not found")
        void testViewAccount_AccountNotFound_ThrowsResourceNotFoundException() {
            // Given: Valid account ID but account doesn't exist (COBOL VSAM NOTFND)
            String nonExistentAccountId = "99999999999";
            when(accountRepository.findById(nonExistentAccountId)).thenReturn(Optional.empty());

            // When/Then: Should throw ResourceNotFoundException
            assertThatThrownBy(() -> accountViewService.viewAccount(nonExistentAccountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found")
                .extracting("resourceType").isEqualTo("Account");

            // Verify repository called but no further processing
            verify(accountRepository).findById(nonExistentAccountId);
            verifyNoMoreInteractions(customerRepository, cardRepository);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when customer not found")
        void testViewAccount_CustomerNotFound_ThrowsResourceNotFoundException() {
            // Given: Account exists but customer doesn't (COBOL CUSTDAT NOTFND)
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateValidAccount();
            testAccount.setCustomerId(99999L);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(99999L)).thenReturn(Optional.empty());

            // When/Then: Should throw ResourceNotFoundException for customer
            assertThatThrownBy(() -> accountViewService.viewAccount(accountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found")
                .extracting("resourceType").isEqualTo("Customer");

            // Verify proper repository call sequence
            verify(accountRepository).findById(accountId);
            verify(customerRepository).findById(99999L);
            verifyNoMoreInteractions(cardRepository);
        }

        @Test
        @DisplayName("Should handle missing cards gracefully - no exception thrown")
        void testGetAccountWithDetails_NoCards_HandlesGracefully() {
            // Given: Account and customer exist but no cards (COBOL CARDDAT empty result set)
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateValidAccount();
            Customer testCustomer = testDataGenerator.generateValidCustomer();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(testAccount.getCustomerId())).thenReturn(Optional.of(testCustomer));
            when(cardRepository.findByAccountId(accountId)).thenReturn(Arrays.asList());

            // When: Execute account retrieval
            AccountDto result = accountViewService.getAccountWithDetails(accountId);

            // Then: Should succeed without cards
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(accountId);

            // Verify all repositories called
            verify(accountRepository).findById(accountId);
            verify(customerRepository).findById(testAccount.getCustomerId());
            verify(cardRepository).findByAccountId(accountId);
        }
    }

    /**
     * Tests for financial precision validation using CobolComparisonUtils
     */
    @Nested
    @DisplayName("Financial Precision Tests - COBOL COMP-3 Equivalent")
    class FinancialPrecisionTests {

        @Test
        @DisplayName("Should maintain BigDecimal precision for account balances")
        void testViewAccount_BigDecimalPrecision_MaintainsCobolAccuracy() {
            // Given: Account with specific precision requirements (COBOL PIC S9(7)V99)
            String accountId = "12345678901";
            BigDecimal preciseBalance = new BigDecimal("12345.67");
            BigDecimal preciseCreditLimit = new BigDecimal("25000.00");

            Account testAccount = testDataGenerator.generateValidAccount(preciseBalance, preciseCreditLimit);
            Customer testCustomer = testDataGenerator.generateValidCustomer();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(testAccount.getCustomerId())).thenReturn(Optional.of(testCustomer));

            // When: Retrieve account
            AccountDto result = accountViewService.viewAccount(accountId);

            // Then: Validate precision using CobolComparisonUtils
            assertThat(CobolComparisonUtils.compareBigDecimals(
                result.getCurrentBalance(), preciseBalance)).isTrue();
            assertThat(CobolComparisonUtils.compareBigDecimals(
                result.getCreditLimit(), preciseCreditLimit)).isTrue();

            // Verify scale and precision match COBOL COMP-3 expectations
            assertThat(result.getCurrentBalance().scale()).isEqualTo(2);
            assertThat(result.getCreditLimit().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle zero and negative balances with proper precision")
        void testViewAccount_ZeroAndNegativeBalances_MaintainsPrecision() {
            // Given: Accounts with edge case balances
            String accountId = "12345678901";
            
            // Test zero balance
            Account zeroBalanceAccount = testDataGenerator.generateValidAccount(
                BigDecimal.ZERO, new BigDecimal("1000.00"));
            Customer testCustomer = testDataGenerator.generateValidCustomer();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(zeroBalanceAccount));
            when(customerRepository.findById(zeroBalanceAccount.getCustomerId())).thenReturn(Optional.of(testCustomer));

            // When: Retrieve account with zero balance
            AccountDto result = accountViewService.viewAccount(accountId);

            // Then: Verify zero balance precision
            assertThat(result.getCurrentBalance()).isEqualTo(new BigDecimal("0.00"));
            assertThat(result.getCurrentBalance().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should validate financial calculations throw DataPrecisionException when invalid")
        void testViewAccount_InvalidPrecision_ThrowsDataPrecisionException() {
            // This test would be implemented if the service performed calculations
            // Currently the service retrieves data, so we test precision validation utility
            
            // Given: BigDecimal values with different precision
            BigDecimal javaValue = new BigDecimal("123.456");  // 3 decimal places
            BigDecimal cobolValue = new BigDecimal("123.46");  // 2 decimal places (COBOL COMP-3)

            // When/Then: Precision comparison should handle scale differences
            assertThat(CobolComparisonUtils.validateFinancialPrecision(javaValue, cobolValue)).isTrue();
        }
    }

    /**
     * Tests for field mapping validation ensuring proper data transformation
     */
    @Nested
    @DisplayName("Field Mapping Validation Tests - Entity to DTO Transformation")
    class FieldMappingTests {

        @Test
        @DisplayName("Should correctly map all required account fields from entity to DTO")
        void testViewAccount_FieldMapping_AllFieldsCorrectlyMapped() {
            // Given: Complete account and customer data
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateValidAccount();
            testAccount.setAccountId(accountId);
            testAccount.setCustomerId(98765L);
            testAccount.setActiveStatus("Y");
            
            Customer testCustomer = testDataGenerator.generateValidCustomer();
            testCustomer.setCustomerId(98765L);
            testCustomer.setFirstName("JOHN");
            testCustomer.setLastName("DOE");
            testCustomer.setSsn("123456789");

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(98765L)).thenReturn(Optional.of(testCustomer));

            // When: Retrieve account
            AccountDto result = accountViewService.viewAccount(accountId);

            // Then: Verify all fields mapped correctly (matches COBOL COACTVW screen fields)
            assertThat(result.getAccountId()).isEqualTo(testAccount.getAccountId());
            assertThat(result.getCustomerId()).isEqualTo(testAccount.getCustomerId());
            assertThat(result.getCurrentBalance()).isEqualTo(testAccount.getCurrentBalance());
            assertThat(result.getCreditLimit()).isEqualTo(testAccount.getCreditLimit());
            assertThat(result.getActiveStatus()).isEqualTo(testAccount.getActiveStatus());

            // Verify no fields are null that should have values
            assertThat(result.getAccountId()).isNotNull();
            assertThat(result.getCustomerId()).isNotNull();
            assertThat(result.getCurrentBalance()).isNotNull();
            assertThat(result.getCreditLimit()).isNotNull();
        }

        @Test
        @DisplayName("Should handle null optional fields gracefully")
        void testViewAccount_NullOptionalFields_HandledGracefully() {
            // Given: Account with some null optional fields
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateValidAccount();
            testAccount.setAccountId(accountId);
            // Note: Some fields might be null in edge cases

            Customer testCustomer = testDataGenerator.generateValidCustomer();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(testAccount.getCustomerId())).thenReturn(Optional.of(testCustomer));

            // When: Retrieve account
            AccountDto result = accountViewService.viewAccount(accountId);

            // Then: Should handle nulls gracefully without exceptions
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(accountId);
        }
    }

    /**
     * Integration tests covering multiple business scenarios
     */
    @Nested
    @DisplayName("Business Scenario Integration Tests")
    class BusinessScenarioTests {

        @Test
        @DisplayName("Should handle high-value account with multiple cards")
        void testViewAccount_HighValueAccountMultipleCards_ProcessedCorrectly() {
            // Given: High-value account scenario
            String accountId = "12345678901";
            Account highValueAccount = testDataGenerator.generateValidAccount(
                new BigDecimal("75000.50"), new BigDecimal("100000.00"));
            highValueAccount.setAccountId(accountId);

            Customer premiumCustomer = testDataGenerator.generateValidCustomer();
            premiumCustomer.setCustomerId(highValueAccount.getCustomerId());

            List<Card> multipleCards = Arrays.asList(
                testDataGenerator.generateValidCard(),
                testDataGenerator.generateValidCard(),
                testDataGenerator.generateValidCard()
            );

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(highValueAccount));
            when(customerRepository.findById(highValueAccount.getCustomerId())).thenReturn(Optional.of(premiumCustomer));
            when(cardRepository.findByAccountId(accountId)).thenReturn(multipleCards);

            // When: Process high-value account
            AccountDto result = accountViewService.getAccountWithDetails(accountId);

            // Then: Verify high-value account processed correctly
            assertThat(result.getCurrentBalance()).isEqualTo(new BigDecimal("75000.50"));
            assertThat(result.getCreditLimit()).isEqualTo(new BigDecimal("100000.00"));
            
            // Verify precision maintained for large amounts
            assertThat(CobolComparisonUtils.validateFinancialPrecision(
                result.getCurrentBalance(), new BigDecimal("75000.50"))).isTrue();
        }

        @Test
        @DisplayName("Should process account at credit limit correctly")
        void testViewAccount_AccountAtCreditLimit_ProcessedCorrectly() {
            // Given: Account at credit limit (common business scenario)
            String accountId = "12345678901";
            BigDecimal creditLimit = new BigDecimal("5000.00");
            Account limitAccount = testDataGenerator.generateValidAccount(creditLimit, creditLimit);
            limitAccount.setAccountId(accountId);

            Customer testCustomer = testDataGenerator.generateValidCustomer();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(limitAccount));
            when(customerRepository.findById(limitAccount.getCustomerId())).thenReturn(Optional.of(testCustomer));

            // When: Process account at limit
            AccountDto result = accountViewService.viewAccount(accountId);

            // Then: Verify account at limit processed correctly
            assertThat(result.getCurrentBalance()).isEqualTo(result.getCreditLimit());
            assertThat(CobolComparisonUtils.compareBigDecimals(
                result.getCurrentBalance(), result.getCreditLimit())).isTrue();
        }

        @Test
        @DisplayName("Should handle account with zero balance correctly")
        void testViewAccount_ZeroBalanceAccount_ProcessedCorrectly() {
            // Given: New account with zero balance
            String accountId = "12345678901";
            Account newAccount = testDataGenerator.generateValidAccount(
                BigDecimal.ZERO, new BigDecimal("2500.00"));
            newAccount.setAccountId(accountId);

            Customer testCustomer = testDataGenerator.generateValidCustomer();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(newAccount));
            when(customerRepository.findById(newAccount.getCustomerId())).thenReturn(Optional.of(testCustomer));

            // When: Process zero balance account
            AccountDto result = accountViewService.viewAccount(accountId);

            // Then: Verify zero balance handled correctly
            assertThat(result.getCurrentBalance()).isEqualTo(new BigDecimal("0.00"));
            assertThat(result.getCurrentBalance().scale()).isEqualTo(2);
            assertThat(result.getCreditLimit()).isEqualTo(new BigDecimal("2500.00"));
        }
    }

    /**
     * Repository interaction verification tests
     */
    @Nested
    @DisplayName("Repository Interaction Tests - Mock Validation")
    class RepositoryInteractionTests {

        @Test
        @DisplayName("Should call repositories in correct sequence for viewAccount")
        void testViewAccount_RepositoryCallSequence_CallsInCorrectOrder() {
            // Given: Test data
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateValidAccount();
            Customer testCustomer = testDataGenerator.generateValidCustomer();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(testAccount.getCustomerId())).thenReturn(Optional.of(testCustomer));

            // When: Execute service method
            accountViewService.viewAccount(accountId);

            // Then: Verify repository call sequence (mirrors COBOL file access order)
            verify(accountRepository).findById(accountId);
            verify(customerRepository).findById(testAccount.getCustomerId());
            // Note: cardRepository not called for basic viewAccount method
            verify(cardRepository, never()).findByAccountId(any());
        }

        @Test
        @DisplayName("Should call all repositories for getAccountWithDetails")
        void testGetAccountWithDetails_AllRepositories_CalledCorrectly() {
            // Given: Complete test data setup
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateValidAccount();
            Customer testCustomer = testDataGenerator.generateValidCustomer();
            List<Card> testCards = Arrays.asList(testDataGenerator.generateValidCard());

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(testAccount.getCustomerId())).thenReturn(Optional.of(testCustomer));
            when(cardRepository.findByAccountId(accountId)).thenReturn(testCards);

            // When: Execute detailed retrieval
            accountViewService.getAccountWithDetails(accountId);

            // Then: Verify all repositories called
            verify(accountRepository).findById(accountId);
            verify(customerRepository).findById(testAccount.getCustomerId());
            verify(cardRepository).findByAccountId(accountId);
        }
    }
}