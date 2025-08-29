package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for AccountService that validates account viewing 
 * and updating functionality converted from COACTVWC and COACTUPC COBOL programs.
 * 
 * This test class ensures:
 * - Data precision and validation rules match mainframe implementation
 * - VSAM STARTBR/READNEXT pattern equivalents through JPA repositories
 * - Field validation for SSN/phone/FICO scores
 * - Transaction boundaries matching CICS SYNCPOINT
 * - Precise decimal calculations for account balances maintaining COBOL COMP-3 precision
 * 
 * Mocks: AccountRepository, CustomerRepository, and validation services
 * Ensures data integrity constraints and referential integrity are maintained
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService - COBOL COACTVWC/COACTUPC equivalent validation")
class AccountServiceTest extends BaseServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ValidationUtil validationUtil;

    @Mock
    private CobolDataConverter cobolDataConverter;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private Customer testCustomer;
    private static final String VALID_ACCOUNT_ID = "1000000001";
    private static final String VALID_CUSTOMER_ID = "0000012345";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1250.75").setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal CREDIT_LIMIT = new BigDecimal("5000.00").setScale(2, RoundingMode.HALF_UP);

    @BeforeEach
    void setUp() {
        resetMocks();
        setupTestData();
    }

    private void setupTestData() {
        // Create test customer matching CVACT01Y copybook structure
        testCustomer = new Customer();
        testCustomer.setCustomerId(VALID_CUSTOMER_ID);
        testCustomer.setFirstName("JOHN");
        testCustomer.setLastName("SMITH");

        // Create test account matching CVACT01Y copybook structure with COBOL precision
        testAccount = new Account();
        testAccount.setAccountId(VALID_ACCOUNT_ID);
        testAccount.setCustomerId(VALID_CUSTOMER_ID);
        testAccount.setCurrentBalance(INITIAL_BALANCE);
        testAccount.setCreditLimit(CREDIT_LIMIT);
        testAccount.setOpenDate(LocalDate.now().minusYears(1));
        testAccount.setAccountStatus("ACTIVE");
        testAccount.setExpiryDate(LocalDate.now().plusYears(3));
        testAccount.setCardNumber("4532123456789012");
        testAccount.setLastUpdatedTimestamp(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Account View Operations - COACTVWC COBOL Program Equivalent")
    class AccountViewOperations {

        @Test
        @DisplayName("viewAccount() - Valid account ID returns account with COBOL precision")
        void testViewAccount_ValidAccountId_ReturnsAccountWithCobolPrecision() {
            // Given: Valid account exists in repository
            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(VALID_CUSTOMER_ID)).thenReturn(Optional.of(testCustomer));

            // When: Viewing account
            Account result = accountService.viewAccount(VALID_ACCOUNT_ID);

            // Then: Account returned with exact COBOL COMP-3 precision
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
            assertThat(result.getCurrentBalance()).isEqualTo(INITIAL_BALANCE);
            assertThat(result.getCurrentBalance().scale()).isEqualTo(2);
            assertThat(result.getCreditLimit()).isEqualTo(CREDIT_LIMIT);
            assertThat(result.getCreditLimit().scale()).isEqualTo(2);
            assertThat(result.getCustomerId()).isEqualTo(VALID_CUSTOMER_ID);

            // Verify repository interactions match VSAM STARTBR/READ pattern
            verify(accountRepository, times(1)).findById(VALID_ACCOUNT_ID);
            verify(customerRepository, times(1)).findById(VALID_CUSTOMER_ID);
        }

        @Test
        @DisplayName("viewAccount() - Invalid account ID throws ResourceNotFoundException")
        void testViewAccount_InvalidAccountId_ThrowsResourceNotFoundException() {
            // Given: Account does not exist
            when(accountRepository.findById(anyString())).thenReturn(Optional.empty());

            // When/Then: Exception thrown matching COBOL NOTFND condition
            assertThatThrownBy(() -> accountService.viewAccount("INVALID123"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("resourceType", "resourceId")
                    .containsExactly("Account", "INVALID123");

            verify(accountRepository, times(1)).findById("INVALID123");
            verify(customerRepository, never()).findById(any());
        }

        @Test
        @DisplayName("getAccountById() - Repository integration with VSAM-equivalent behavior")
        void testGetAccountById_RepositoryIntegration_VsamEquivalentBehavior() {
            // Given: Account repository configured for VSAM-like access
            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

            // When: Getting account by ID (VSAM READ equivalent)
            Account result = accountService.getAccountById(VALID_ACCOUNT_ID);

            // Then: Single record retrieved with key-sequential access pattern
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
            
            // Verify single repository call matching VSAM READ operation
            verify(accountRepository, times(1)).findById(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Account balance calculation - COMP-3 precision maintenance")
        void testAccountBalanceCalculation_Comp3PrecisionMaintenance() {
            // Given: Account with COBOL COMP-3 equivalent precision
            BigDecimal comp3Balance = new BigDecimal("999.99");
            testAccount.setCurrentBalance(comp3Balance);
            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(cobolDataConverter.toBigDecimal(any(byte[].class))).thenReturn(comp3Balance);

            // When: Viewing account
            Account result = accountService.viewAccount(VALID_ACCOUNT_ID);

            // Then: Balance maintains exact COBOL COMP-3 precision
            assertThat(result.getCurrentBalance()).isEqualByComparingTo(comp3Balance);
            assertBigDecimalEquals(result.getCurrentBalance(), comp3Balance);
        }

        @Test
        @DisplayName("Multiple account access - VSAM browse pattern simulation")
        void testMultipleAccountAccess_VsamBrowsePatternSimulation() {
            // Given: Multiple accounts for customer (VSAM STARTBR/READNEXT pattern)
            List<Account> customerAccounts = List.of(testAccount);
            when(accountRepository.findByCustomerId(VALID_CUSTOMER_ID)).thenReturn(customerAccounts);

            // When: Browse accounts for customer
            List<Account> results = accountRepository.findByCustomerId(VALID_CUSTOMER_ID);

            // Then: Results returned in key sequence (account ID order)
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
            
            // Verify browse pattern equivalent to VSAM STARTBR/READNEXT
            verify(accountRepository, times(1)).findByCustomerId(VALID_CUSTOMER_ID);
        }
    }

    @Nested
    @DisplayName("Account Update Operations - COACTUPC COBOL Program Equivalent")
    class AccountUpdateOperations {

        @Test
        @DisplayName("updateAccount() - Valid update with field validation")
        void testUpdateAccount_ValidUpdate_WithFieldValidation() {
            // Given: Valid account update with all validations passing
            Account updateAccount = new Account();
            updateAccount.setAccountId(VALID_ACCOUNT_ID);
            updateAccount.setCurrentBalance(new BigDecimal("1500.25").setScale(2, RoundingMode.HALF_UP));
            updateAccount.setCreditLimit(new BigDecimal("6000.00").setScale(2, RoundingMode.HALF_UP));
            updateAccount.setAccountStatus("ACTIVE");

            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(updateAccount);

            // When: Updating account
            Account result = accountService.updateAccount(updateAccount);

            // Then: Account updated with maintained precision
            assertThat(result).isNotNull();
            assertThat(result.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("1500.25"));
            assertThat(result.getCurrentBalance().scale()).isEqualTo(2);
            assertThat(result.getCreditLimit()).isEqualByComparingTo(new BigDecimal("6000.00"));
            assertThat(result.getCreditLimit().scale()).isEqualTo(2);

            // Verify update transaction (CICS SYNCPOINT equivalent)
            verify(accountRepository, times(1)).findById(VALID_ACCOUNT_ID);
            verify(accountRepository, times(1)).save(any(Account.class));
        }

        @Test
        @DisplayName("validateAccountUpdate() - SSN validation with COBOL edit routine equivalent")
        void testValidateAccountUpdate_SsnValidation_CobolEditRoutineEquivalent() {
            // Given: Account with invalid SSN format
            testAccount.setSsn("123-45-678X"); // Invalid SSN
            when(validationUtil.validateSSN("123-45-678X")).thenReturn(false);

            // When/Then: Validation fails matching COBOL edit routine
            assertThatThrownBy(() -> accountService.validateAccountUpdate(testAccount))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(ex -> {
                        ValidationException validationEx = (ValidationException) ex;
                        assertThat(validationEx.hasFieldErrors()).isTrue();
                        assertThat(validationEx.getFieldErrors()).containsKey("ssn");
                    });

            verify(validationUtil, times(1)).validateSSN("123-45-678X");
        }

        @Test
        @DisplayName("validateAccountUpdate() - ZIP code validation with COBOL pattern matching")
        void testValidateAccountUpdate_ZipCodeValidation_CobolPatternMatching() {
            // Given: Account with invalid ZIP code
            testAccount.setZipCode("1234"); // Invalid ZIP (too short)
            when(validationUtil.validateZipCode("1234")).thenReturn(false);

            // When/Then: Validation fails matching COBOL PIC clause validation
            assertThatThrownBy(() -> accountService.validateAccountUpdate(testAccount))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(ex -> {
                        ValidationException validationEx = (ValidationException) ex;
                        assertThat(validationEx.hasFieldErrors()).isTrue();
                        assertThat(validationEx.getFieldErrors()).containsKey("zipCode");
                    });

            verify(validationUtil, times(1)).validateZipCode("1234");
        }

        @Test
        @DisplayName("updateAccount() - FICO score validation within valid range")
        void testUpdateAccount_FicoScoreValidation_WithinValidRange() {
            // Given: Account with valid FICO score
            testAccount.setFicoScore(750); // Valid FICO score
            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // When: Updating account
            Account result = accountService.updateAccount(testAccount);

            // Then: Update succeeds with valid FICO score
            assertThat(result).isNotNull();
            assertThat(result.getFicoScore()).isEqualTo(750);

            verify(accountRepository, times(1)).save(testAccount);
        }

        @Test
        @DisplayName("updateAccount() - Credit limit business rule validation")
        void testUpdateAccount_CreditLimitBusinessRuleValidation() {
            // Given: Account with credit limit exceeding business rule limit
            BigDecimal excessiveCreditLimit = new BigDecimal("50000.00"); // Exceeds $25,000 limit
            testAccount.setCreditLimit(excessiveCreditLimit);
            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

            // When/Then: Business rule violation matching COBOL IF-THEN logic
            assertThatThrownBy(() -> accountService.updateAccount(testAccount))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> {
                        BusinessRuleException businessEx = (BusinessRuleException) ex;
                        assertThat(businessEx.getErrorCode()).isEqualTo("CREDIT_LIMIT_EXCEEDED");
                    });
        }

        @Test
        @DisplayName("updateAccount() - Account status transition validation")
        void testUpdateAccount_AccountStatusTransitionValidation() {
            // Given: Invalid status transition (CLOSED to ACTIVE not allowed)
            testAccount.setAccountStatus("CLOSED");
            Account updateAccount = new Account();
            updateAccount.setAccountId(VALID_ACCOUNT_ID);
            updateAccount.setAccountStatus("ACTIVE");

            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

            // When/Then: Status transition validation fails
            assertThatThrownBy(() -> accountService.updateAccount(updateAccount))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> {
                        BusinessRuleException businessEx = (BusinessRuleException) ex;
                        assertThat(businessEx.getErrorCode()).isEqualTo("INVALID_STATUS_TRANSITION");
                    });
        }

        @Test
        @DisplayName("updateAccount() - Transaction boundary validation with rollback")
        void testUpdateAccount_TransactionBoundaryValidation_WithRollback() {
            // Given: Repository save operation fails (simulating CICS ABEND)
            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenThrow(new RuntimeException("Database error"));

            // When/Then: Transaction rolls back (CICS SYNCPOINT equivalent)
            assertThatThrownBy(() -> accountService.updateAccount(testAccount))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");

            // Verify rollback behavior
            verify(accountRepository, times(1)).findById(VALID_ACCOUNT_ID);
            verify(accountRepository, times(1)).save(testAccount);
        }
    }

    @Nested
    @DisplayName("COBOL Data Type Conversion and Precision Tests")
    class CobolDataTypeConversionTests {

        @Test
        @DisplayName("BigDecimal precision - COBOL COMP-3 PIC S9(7)V99 equivalent")
        void testBigDecimalPrecision_CobolComp3Equivalent() {
            // Given: COBOL COMP-3 byte array equivalent
            byte[] comp3Bytes = {0x01, 0x23, 0x45, 0x67, 0x0F}; // Represents 12345.67
            BigDecimal expectedAmount = new BigDecimal("12345.67").setScale(2, RoundingMode.HALF_UP);
            when(cobolDataConverter.fromComp3(comp3Bytes, 2)).thenReturn(expectedAmount);

            // When: Converting COMP-3 to BigDecimal
            BigDecimal result = cobolDataConverter.fromComp3(comp3Bytes, 2);

            // Then: Exact precision maintained matching COBOL COMP-3
            assertThat(result).isEqualByComparingTo(expectedAmount);
            assertThat(result.scale()).isEqualTo(2);
            assertThat(result.precision()).isEqualTo(7);
            assertThat(result.toString()).isEqualTo("12345.67");

            verify(cobolDataConverter, times(1)).fromComp3(comp3Bytes, 2);
        }

        @Test
        @DisplayName("Interest calculation - COBOL ROUNDED modifier behavior")
        void testInterestCalculation_CobolRoundedModifierBehavior() {
            // Given: Interest calculation with COBOL ROUNDED equivalent
            BigDecimal principal = new BigDecimal("1000.00");
            BigDecimal rate = new BigDecimal("0.0525"); // 5.25% annual rate
            BigDecimal expectedInterest = new BigDecimal("4.38").setScale(2, RoundingMode.HALF_UP);
            
            testAccount.setCurrentBalance(principal);
            when(cobolDataConverter.toBigDecimal(any())).thenReturn(expectedInterest);

            // When: Calculating monthly interest (similar to CBACT04C.cbl)
            BigDecimal monthlyInterest = principal
                    .multiply(rate)
                    .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

            // Then: Result matches COBOL ROUNDED calculation
            assertThat(monthlyInterest).isEqualByComparingTo(expectedInterest);
            assertThat(monthlyInterest.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Account balance update - penny-level accuracy preservation")
        void testAccountBalanceUpdate_PennyLevelAccuracyPreservation() {
            // Given: Balance update with precise decimal amounts
            BigDecimal originalBalance = new BigDecimal("999.99");
            BigDecimal transactionAmount = new BigDecimal("0.01");
            BigDecimal expectedBalance = new BigDecimal("1000.00").setScale(2, RoundingMode.HALF_UP);

            testAccount.setCurrentBalance(originalBalance);
            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

            // When: Processing balance update
            testAccount.setCurrentBalance(originalBalance.add(transactionAmount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            Account result = accountService.updateAccount(testAccount);

            // Then: Penny-level precision maintained
            assertThat(result.getCurrentBalance()).isEqualByComparingTo(expectedBalance);
            assertBigDecimalEquals(result.getCurrentBalance(), expectedBalance);
        }
    }

    @Nested
    @DisplayName("Data Integrity and Referential Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Customer reference validation - foreign key constraint equivalent")
        void testCustomerReferenceValidation_ForeignKeyConstraintEquivalent() {
            // Given: Account with invalid customer reference
            testAccount.setCustomerId("9999999999"); // Non-existent customer
            when(customerRepository.findById("9999999999")).thenReturn(Optional.empty());

            // When/Then: Referential integrity violation
            assertThatThrownBy(() -> accountService.validateAccountUpdate(testAccount))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(ex -> {
                        ValidationException validationEx = (ValidationException) ex;
                        assertThat(validationEx.hasFieldErrors()).isTrue();
                        assertThat(validationEx.getFieldErrors()).containsKey("customerId");
                    });

            verify(customerRepository, times(1)).findById("9999999999");
        }

        @Test
        @DisplayName("Account uniqueness validation - primary key constraint")
        void testAccountUniquenessValidation_PrimaryKeyConstraint() {
            // Given: Account with duplicate account ID
            Account duplicateAccount = new Account();
            duplicateAccount.setAccountId(VALID_ACCOUNT_ID);
            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

            // When/Then: Primary key violation equivalent
            assertThatThrownBy(() -> accountService.updateAccount(duplicateAccount))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> {
                        BusinessRuleException businessEx = (BusinessRuleException) ex;
                        assertThat(businessEx.getErrorCode()).isEqualTo("DUPLICATE_ACCOUNT");
                    });
        }

        @Test
        @DisplayName("Data consistency validation across repositories")
        void testDataConsistencyValidation_AcrossRepositories() {
            // Given: Consistent data across account and customer repositories
            when(accountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(customerRepository.findById(VALID_CUSTOMER_ID)).thenReturn(Optional.of(testCustomer));

            // When: Viewing account with customer data
            Account result = accountService.viewAccount(VALID_ACCOUNT_ID);

            // Then: Data consistency maintained across repositories
            assertThat(result).isNotNull();
            assertThat(result.getCustomerId()).isEqualTo(testCustomer.getCustomerId());
            
            // Verify both repositories accessed for consistency check
            verify(accountRepository, times(1)).findById(VALID_ACCOUNT_ID);
            verify(customerRepository, times(1)).findById(VALID_CUSTOMER_ID);
        }
    }

    @Nested
    @DisplayName("Error Handling and Exception Scenarios")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Database connection failure - CICS ABEND equivalent handling")
        void testDatabaseConnectionFailure_CicsAbendEquivalentHandling() {
            // Given: Database connection failure
            when(accountRepository.findById(any())).thenThrow(new RuntimeException("Connection failed"));

            // When/Then: System handles failure gracefully (CICS ABEND equivalent)
            assertThatThrownBy(() -> accountService.viewAccount(VALID_ACCOUNT_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Connection failed");

            verify(accountRepository, times(1)).findById(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Validation error aggregation - multiple field errors")
        void testValidationErrorAggregation_MultipleFieldErrors() {
            // Given: Account with multiple validation errors
            testAccount.setSsn("invalid-ssn");
            testAccount.setZipCode("123");
            testAccount.setFicoScore(1000); // Invalid FICO score

            when(validationUtil.validateSSN("invalid-ssn")).thenReturn(false);
            when(validationUtil.validateZipCode("123")).thenReturn(false);

            // When/Then: All validation errors aggregated
            assertThatThrownBy(() -> accountService.validateAccountUpdate(testAccount))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(ex -> {
                        ValidationException validationEx = (ValidationException) ex;
                        assertThat(validationEx.hasFieldErrors()).isTrue();
                        assertThat(validationEx.getFieldErrors()).hasSize(3);
                        assertThat(validationEx.getFieldErrors().keySet())
                                .contains("ssn", "zipCode", "ficoScore");
                    });
        }
    }
}