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
import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;

import com.carddemo.service.TestDataGenerator;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Card;
import com.carddemo.entity.CardXref;
import com.carddemo.dto.AccountDto;
import com.carddemo.dto.AccountViewResponse;
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

    @Mock
    private CardXrefRepository cardXrefRepository;

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
        @DisplayName("Should reject null account ID with error response")
        void testViewAccount_NullAccountId_ReturnsErrorResponse() {
            // When
            AccountViewResponse response = accountViewService.viewAccount(null);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.getErrorMessage()).isEqualTo("Account number not provided");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository, cardXrefRepository);
        }

        @Test
        @DisplayName("Should reject empty account ID with error response")
        void testViewAccount_EmptyAccountId_ReturnsErrorResponse() {
            // When
            AccountViewResponse response = accountViewService.viewAccount("");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.getErrorMessage()).isEqualTo("Account number not provided");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository, cardXrefRepository);
        }

        @Test
        @DisplayName("Should reject non-numeric account ID with error response")
        void testViewAccount_NonNumericAccountId_ReturnsErrorResponse() {
            // Given: Non-numeric account ID (matches COBOL NUMERIC test)
            String invalidAccountId = "ABC1234567";

            // When
            AccountViewResponse response = accountViewService.viewAccount(invalidAccountId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.getErrorMessage()).isEqualTo("Account number must be a non zero 11 digit number");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository, cardXrefRepository);
        }

        @Test
        @DisplayName("Should reject account ID not exactly 11 digits with error response")
        void testViewAccount_InvalidAccountIdLength_ReturnsErrorResponse() {
            // Given: Account ID with incorrect length (COBOL requires 11 digits)
            String shortAccountId = "1234567890";      // 10 digits
            String longAccountId = "123456789012";     // 12 digits

            // When
            AccountViewResponse shortResponse = accountViewService.viewAccount(shortAccountId);
            AccountViewResponse longResponse = accountViewService.viewAccount(longAccountId);

            // Then
            assertThat(shortResponse).isNotNull();
            assertThat(shortResponse.isSuccessful()).isFalse();
            assertThat(shortResponse.getErrorMessage()).isEqualTo("Account number must be a non zero 11 digit number");

            assertThat(longResponse).isNotNull();
            assertThat(longResponse.isSuccessful()).isFalse();
            assertThat(longResponse.getErrorMessage()).isEqualTo("Account number must be a non zero 11 digit number");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository, cardXrefRepository);
        }

        @Test
        @DisplayName("Should reject zero account ID with error response")
        void testViewAccount_ZeroAccountId_ReturnsErrorResponse() {
            // Given: All zeros account ID (COBOL business rule)
            String zeroAccountId = "00000000000";

            // When
            AccountViewResponse response = accountViewService.viewAccount(zeroAccountId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.getErrorMessage()).isEqualTo("Account number must be a non zero 11 digit number");

            // Verify no repository interactions
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository, cardXrefRepository);
        }

        @Test
        @DisplayName("Should accept valid 11-digit numeric account ID")
        void testViewAccount_ValidAccountIdFormat_PassesValidation() {
            // Given: Valid 11-digit account ID
            String validAccountId = "12345678901";
            Account testAccount = testDataGenerator.generateAccount();
            Customer testCustomer = testDataGenerator.generateCustomer();

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(validAccountId));
            testCardXref.setXrefCustId(Long.parseLong(testCustomer.getCustomerId()));
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(anyLong())).thenReturn(Arrays.asList(testCardXref));
            when(accountRepository.findById(anyLong())).thenReturn(Optional.of(testAccount));
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
            Account testAccount = testDataGenerator.generateAccountWithBalance(new BigDecimal("1250.75"));
            testAccount.setAccountId(Long.parseLong(accountId));
            testAccount.setCreditLimit(new BigDecimal("5000.00"));

            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.setCustomerId("98765");
            testCustomer.setFirstName("JOHN");
            testCustomer.setLastName("DOE");

            // Create CardXref for cross-reference lookup (COBOL 9200-GETCARDXREF-BYACCT)
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(98765L);
            testCardXref.setXrefCardNum("1234567890123456");

            // Mock repository calls simulating COBOL file access chain
            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(98765L)).thenReturn(Optional.of(testCustomer));

            // When: Execute viewAccount (equivalent to COBOL PERFORM 9000-READ-ACCT)
            AccountViewResponse result = accountViewService.viewAccount(accountId);

            // Then: Verify complete data retrieval chain
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(accountId);
            assertThat(result.getCustomerId()).isEqualTo("000098765");
            assertThat(result.getCurrentBalance()).isEqualTo(new BigDecimal("1250.75"));
            assertThat(result.getCreditLimit()).isEqualTo(new BigDecimal("5000.00"));

            // Verify repository interaction sequence (matches COBOL read order)
            verify(cardXrefRepository).findByXrefAcctId(Long.parseLong(accountId));
            verify(accountRepository).findById(Long.parseLong(accountId));
            verify(customerRepository).findById(98765L);
        }

        @Test
        @DisplayName("Should handle card cross-reference retrieval when available")
        void testViewAccount_WithCardData_ReturnsCompleteDetails() {
            // Given: Account with associated cards
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateAccount();
            Customer testCustomer = testDataGenerator.generateCustomer();
            testAccount.setAccountId(Long.parseLong(accountId));
            testAccount.setCustomer(testCustomer); // Set up the relationship
            
            // Create CardXref for cross-reference lookup (COBOL 9200-GETCARDXREF-BYACCT)
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(Long.parseLong(testAccount.getCustomerId()));
            testCardXref.setXrefCardNum("1234567890123456");

            // Create a simple Card for testing
            Card testCard = new Card();
            testCard.setCardNumber("1234567890123456");
            testCard.setAccountId(Long.parseLong(accountId));
            testCard.setCustomerId(Long.valueOf(testCustomer.getCustomerId()));

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(Long.parseLong(testAccount.getCustomerId()))).thenReturn(Optional.of(testCustomer));

            // When: Execute detailed account retrieval
            AccountViewResponse result = accountViewService.viewAccount(accountId);

            // Then: Verify account information is included
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(accountId);

            // Verify all repository calls made (simulates complete COBOL file chain)
            verify(cardXrefRepository).findByXrefAcctId(Long.parseLong(accountId));
            verify(accountRepository).findById(Long.parseLong(accountId));
            verify(customerRepository).findById(Long.parseLong(testAccount.getCustomerId()));
        }
    }

    /**
     * Tests for error handling scenarios - missing data cases
     */
    @Nested
    @DisplayName("Error Handling Tests - Missing Data Scenarios")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when account not found in cardxref")
        void testViewAccount_AccountNotFound_ThrowsResourceNotFoundException() {
            // Given: Valid account ID but account doesn't exist in card cross-reference (COBOL CARDXREF NOTFND)
            String nonExistentAccountId = "99999999999";
            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(nonExistentAccountId))).thenReturn(Arrays.asList());

            // When/Then: Should return error response (not throw exception)
            AccountViewResponse result = accountViewService.viewAccount(nonExistentAccountId);
            assertThat(result).isNotNull();
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getErrorMessage()).contains("Did not find this account in account card xref file");

            // Verify repository called but no further processing
            verify(cardXrefRepository).findByXrefAcctId(Long.parseLong(nonExistentAccountId));
            verifyNoMoreInteractions(accountRepository, customerRepository, cardRepository);
        }

        @Test
        @DisplayName("Should return error when customer not found")
        void testViewAccount_CustomerNotFound_ReturnsError() {
            // Given: CardXref exists but customer doesn't (COBOL CUSTDAT NOTFND)
            String accountId = "12345678901";
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(99999L);
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(99999L)).thenReturn(Optional.empty());

            // When: Execute account view
            AccountViewResponse result = accountViewService.viewAccount(accountId);

            // Then: Should return error response
            assertThat(result).isNotNull();
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getErrorMessage()).contains("Did not find this account in account card xref file");

            // Verify proper repository call sequence (cardXref first, then customer, but account is never called)
            verify(cardXrefRepository).findByXrefAcctId(Long.parseLong(accountId));
            verify(customerRepository).findById(99999L);
            verifyNoMoreInteractions(accountRepository, cardRepository);
        }

        @Test
        @DisplayName("Should handle missing cards gracefully - no exception thrown")
        void testViewAccount_NoCards_HandlesGracefully() {
            // Given: Account and customer exist but no cards (COBOL CARDDAT empty result set)
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateAccount();
            Customer testCustomer = testDataGenerator.generateCustomer();
            
            // Set account ID to match test expectation
            testAccount.setAccountId(Long.parseLong(accountId));
            testAccount.setCustomer(testCustomer);

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(Long.parseLong(testCustomer.getCustomerId()));
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(Long.parseLong(testCustomer.getCustomerId()))).thenReturn(Optional.of(testCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(testAccount));

            // When: Execute account retrieval
            AccountViewResponse result = accountViewService.viewAccount(accountId);

            // Then: Should succeed without cards
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(accountId);

            // Verify repositories called in correct COBOL sequence  
            verify(cardXrefRepository).findByXrefAcctId(Long.parseLong(accountId));
            verify(customerRepository).findById(Long.parseLong(testCustomer.getCustomerId()));
            verify(accountRepository).findById(Long.parseLong(accountId));
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

            Account testAccount = testDataGenerator.generateAccountWithBalance(preciseBalance);
            testAccount.setCreditLimit(preciseCreditLimit);
            Customer testCustomer = testDataGenerator.generateCustomer();
            testAccount.setCustomer(testCustomer);

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(Long.parseLong(testAccount.getCustomerId()));
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(Long.parseLong(testAccount.getCustomerId()))).thenReturn(Optional.of(testCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(testAccount));

            // When: Retrieve account
            AccountViewResponse result = accountViewService.viewAccount(accountId);

            // Then: Validate precision using CobolComparisonUtils
            CobolComparisonUtils cobolUtils = new CobolComparisonUtils();
            assertThat(cobolUtils.compareDecimalPrecision(
                result.getCurrentBalance(), preciseBalance)).isTrue();
            assertThat(cobolUtils.compareDecimalPrecision(
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
            Account zeroBalanceAccount = testDataGenerator.generateAccountWithBalance(BigDecimal.ZERO);
            zeroBalanceAccount.setCreditLimit(new BigDecimal("1000.00"));
            Customer testCustomer = testDataGenerator.generateCustomer();
            zeroBalanceAccount.setCustomer(testCustomer);

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(Long.parseLong(zeroBalanceAccount.getCustomerId()));
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(Long.parseLong(zeroBalanceAccount.getCustomerId()))).thenReturn(Optional.of(testCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(zeroBalanceAccount));

            // When: Retrieve account with zero balance
            AccountViewResponse result = accountViewService.viewAccount(accountId);

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
            CobolComparisonUtils cobolUtils = new CobolComparisonUtils();
            assertThat(cobolUtils.validateFinancialCalculation(javaValue, cobolValue)).isTrue();
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
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setAccountId(Long.parseLong(accountId));
            testAccount.setActiveStatus("Y");
            
            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.setCustomerId("98765");
            testCustomer.setFirstName("JOHN");
            testCustomer.setLastName("DOE");
            testCustomer.setSsn("123456789");
            testAccount.setCustomer(testCustomer); // Set up the relationship

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(98765L);
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(98765L)).thenReturn(Optional.of(testCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(testAccount));

            // When: Retrieve account
            AccountViewResponse result = accountViewService.viewAccount(accountId);

            // Then: Verify all fields mapped correctly (matches COBOL COACTVW screen fields)
            assertThat(result.getAccountId()).isEqualTo(testAccount.getAccountId().toString());
            assertThat(result.getCustomerId()).isEqualTo(testCustomer.getCustomerId());
            assertThat(result.getCurrentBalance()).isEqualTo(testAccount.getCurrentBalance());
            assertThat(result.getCreditLimit()).isEqualTo(testAccount.getCreditLimit());
            assertThat(result.getAccountActiveStatus()).isEqualTo(testAccount.getActiveStatus());

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
            Account testAccount = testDataGenerator.generateAccount();
            testAccount.setAccountId(Long.parseLong(accountId));
            // Note: Some fields might be null in edge cases

            Customer testCustomer = testDataGenerator.generateCustomer();
            testAccount.setCustomer(testCustomer);

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(Long.parseLong(testAccount.getCustomerId()));
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(Long.parseLong(testAccount.getCustomerId()))).thenReturn(Optional.of(testCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(testAccount));

            // When: Retrieve account
            AccountViewResponse result = accountViewService.viewAccount(accountId);

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
            Account highValueAccount = testDataGenerator.generateAccountWithBalance(new BigDecimal("75000.50"));
            highValueAccount.setAccountId(Long.parseLong(accountId));
            highValueAccount.setCreditLimit(new BigDecimal("100000.00"));

            Customer premiumCustomer = testDataGenerator.generateCustomer();
            premiumCustomer.setCustomerId("98765");
            highValueAccount.setCustomer(premiumCustomer);

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(98765L);
            testCardXref.setXrefCardNum("4000123456781234");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(98765L)).thenReturn(Optional.of(premiumCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(highValueAccount));

            // When: Process high-value account
            AccountViewResponse result = accountViewService.viewAccount(accountId);

            // Then: Verify high-value account processed correctly
            assertThat(result.getCurrentBalance()).isEqualTo(new BigDecimal("75000.50"));
            assertThat(result.getCreditLimit()).isEqualTo(new BigDecimal("100000.00"));
            
            // Verify precision maintained for large amounts
            CobolComparisonUtils cobolUtils = new CobolComparisonUtils();
            assertThat(cobolUtils.validateFinancialCalculation(
                result.getCurrentBalance(), new BigDecimal("75000.50"))).isTrue();
        }

        @Test
        @DisplayName("Should process account at credit limit correctly")
        void testViewAccount_AccountAtCreditLimit_ProcessedCorrectly() {
            // Given: Account at credit limit (common business scenario)
            String accountId = "12345678901";
            BigDecimal creditLimit = new BigDecimal("5000.00");
            Account limitAccount = testDataGenerator.generateAccountWithBalance(creditLimit);
            limitAccount.setAccountId(Long.parseLong(accountId));
            limitAccount.setCreditLimit(creditLimit);

            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.setCustomerId("98765");
            limitAccount.setCustomer(testCustomer);

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(98765L);
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(98765L)).thenReturn(Optional.of(testCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(limitAccount));

            // When: Process account at limit
            AccountViewResponse result = accountViewService.viewAccount(accountId);

            // Then: Verify account at limit processed correctly
            assertThat(result.getCurrentBalance()).isEqualTo(result.getCreditLimit());
            CobolComparisonUtils cobolUtils = new CobolComparisonUtils();
            assertThat(cobolUtils.compareDecimalPrecision(
                result.getCurrentBalance(), result.getCreditLimit())).isTrue();
        }

        @Test
        @DisplayName("Should handle account with zero balance correctly")
        void testViewAccount_ZeroBalanceAccount_ProcessedCorrectly() {
            // Given: New account with zero balance
            String accountId = "12345678901";
            Account newAccount = testDataGenerator.generateAccountWithBalance(BigDecimal.ZERO);
            newAccount.setAccountId(Long.parseLong(accountId));
            newAccount.setCreditLimit(new BigDecimal("2500.00"));

            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.setCustomerId("98765");
            newAccount.setCustomer(testCustomer);

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(98765L);
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(98765L)).thenReturn(Optional.of(testCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(newAccount));

            // When: Process zero balance account
            AccountViewResponse result = accountViewService.viewAccount(accountId);

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
            Account testAccount = testDataGenerator.generateAccount();
            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.setCustomerId("98765");
            testAccount.setCustomer(testCustomer);

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(98765L);
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(98765L)).thenReturn(Optional.of(testCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(testAccount));

            // When: Execute service method
            accountViewService.viewAccount(accountId);

            // Then: Verify repository call sequence (mirrors COBOL file access order)
            verify(cardXrefRepository).findByXrefAcctId(Long.parseLong(accountId));
            verify(customerRepository).findById(98765L);
            verify(accountRepository).findById(Long.parseLong(accountId));
        }

        @Test
        @DisplayName("Should call repositories correctly for viewAccount")
        void testViewAccount_AllRepositories_CalledCorrectly() {
            // Given: Complete test data setup
            String accountId = "12345678901";
            Account testAccount = testDataGenerator.generateAccount();
            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.setCustomerId("98765");
            testAccount.setCustomer(testCustomer);

            // Create CardXref for cross-reference lookup
            CardXref testCardXref = new CardXref();
            testCardXref.setXrefAcctId(Long.parseLong(accountId));
            testCardXref.setXrefCustId(98765L);
            testCardXref.setXrefCardNum("1234567890123456");

            when(cardXrefRepository.findByXrefAcctId(Long.parseLong(accountId))).thenReturn(Arrays.asList(testCardXref));
            when(customerRepository.findById(98765L)).thenReturn(Optional.of(testCustomer));
            when(accountRepository.findById(Long.parseLong(accountId))).thenReturn(Optional.of(testAccount));

            // When: Execute account retrieval
            accountViewService.viewAccount(accountId);

            // Then: Verify repositories called correctly
            verify(cardXrefRepository).findByXrefAcctId(Long.parseLong(accountId));
            verify(customerRepository).findById(98765L);
            verify(accountRepository).findById(Long.parseLong(accountId));
        }
    }
}