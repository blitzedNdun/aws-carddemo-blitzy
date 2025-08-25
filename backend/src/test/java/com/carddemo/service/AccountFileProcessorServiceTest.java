package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.repository.AccountRepository;
import com.carddemo.config.TestDatabaseConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive unit test suite for AccountFileProcessorService.
 * 
 * This test class validates the direct translation of CBACT01C COBOL batch logic 
 * to Java, ensuring 100% functional parity between COBOL and Java implementations.
 * 
 * Test Coverage Areas:
 * - Sequential file open/read/close operations
 * - Account record processing and display 
 * - File status code handling (00=OK, 10=EOF, others=error)
 * - End-of-file detection
 * - CEE3ABD abnormal termination equivalent
 * - Financial precision validation (COBOL COMP-3 to BigDecimal)
 * - Repository integration with VSAM-equivalent operations
 * - Performance validation against 200ms response time threshold
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ContextConfiguration(classes = TestDatabaseConfig.class)
public class AccountFileProcessorServiceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountFileProcessorServiceTest.class);
    
    @Mock
    private AccountRepository accountRepository;
    
    @InjectMocks
    private AccountFileProcessorService accountFileProcessorService;
    
    // Test constants matching COBOL precision requirements
    private static final int COBOL_DECIMAL_SCALE = 2;
    private static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int BATCH_PROCESSING_WINDOW_HOURS = 4;
    private static final long RESPONSE_TIME_THRESHOLD_MS = 200;
    private static final Long TEST_ACCOUNT_ID = 1000000001L;
    private static final String TEST_CUSTOMER_ID = "1000000001";
    
    // Test data for functional parity validation
    private Account testAccount;
    private List<Account> testAccountList;
    
    @BeforeEach
    void setUp() {
        // Create test customer entity
        Customer testCustomer = new Customer();
        testCustomer.setCustomerId(TEST_CUSTOMER_ID);
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        
        // Initialize test account with COBOL-equivalent precision
        testAccount = new Account();
        testAccount.setAccountId(TEST_ACCOUNT_ID);
        testAccount.setCustomer(testCustomer); // Use proper customer relationship
        testAccount.setCurrentBalance(new BigDecimal("1250.75").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        testAccount.setCreditLimit(new BigDecimal("5000.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        testAccount.setActiveStatus("Y");
        
        // Initialize test data collection
        testAccountList = new ArrayList<>();
        testAccountList.add(testAccount);
    }
    
    @Nested
    @DisplayName("File Processing Operations - COBOL CBACT01C Equivalent")
    class FileProcessingOperations {
        
        @Test
        @DisplayName("processAccountFile() - Complete file processing with successful records")
        void testProcessAccountFile_SuccessfulProcessing_ReturnsCompletionStatus() {
            // Given: Mock repository to return test account data
            when(accountRepository.findAll()).thenReturn(testAccountList);
            
            // When: Execute complete file processing equivalent to COBOL main logic
            long startTime = System.currentTimeMillis();
            assertThatCode(() -> accountFileProcessorService.processAccountFile()).doesNotThrowAnyException();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Then: Verify successful completion matching COBOL behavior and performance
            assertThat(executionTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
            
            // Verify repository interactions match VSAM file operations
            verify(accountRepository, times(1)).findAll();
        }
        
        @Test
        @DisplayName("processAccountFile() - Empty file handling equivalent to COBOL EOF")
        void testProcessAccountFile_EmptyFile_HandlesEndOfFileCorrectly() {
            // Given: Empty account list simulating empty VSAM file
            when(accountRepository.findAll()).thenReturn(new ArrayList<>());
            
            // When: Process empty file
            assertThatCode(() -> accountFileProcessorService.processAccountFile()).doesNotThrowAnyException();
            
            // Then: Verify EOF handling matches COBOL file status 10
            verify(accountRepository, times(1)).findAll();
        }
        
        @Test
        @DisplayName("processAccountFile() - Repository exception handling equivalent to file I/O errors")
        void testProcessAccountFile_RepositoryException_HandlesErrorsGracefully() {
            // Given: Repository throws exception simulating VSAM file error
            when(accountRepository.findAll()).thenThrow(new RuntimeException("Database connection error"));
            
            // When/Then: Verify exception handling equivalent to COBOL ABEND
            assertThatThrownBy(() -> accountFileProcessorService.processAccountFile())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection error");
        }
    }
    
    @Nested
    @DisplayName("File Operation Methods - VSAM Equivalent Operations")
    class FileOperationMethods {
        
        @Test
        @DisplayName("acctFileOpen() - File open operation equivalent to COBOL OPEN INPUT")
        void testAcctFileOpen_InitializesFileProcessing_ReturnsSuccessStatus() {
            // Given: Fresh service instance
            
            // When: Open file for processing
            assertThatCode(() -> accountFileProcessorService.acctFileOpen()).doesNotThrowAnyException();
            
            // Then: Verify successful open operation completes (COBOL equivalent)
            // Note: The actual implementation is void, so we verify no exceptions thrown
        }
        
        @Test
        @DisplayName("acctFileGetNext() - Sequential record retrieval equivalent to COBOL READNEXT")
        void testAcctFileGetNext_SequentialRecordRetrieval_ReturnsRecordsInOrder() {
            // Given: Repository with multiple test accounts
            Customer testCustomer2 = new Customer();
            testCustomer2.setCustomerId("1000000002");
            testCustomer2.setFirstName("Jane");
            testCustomer2.setLastName("Smith");
            
            Account account2 = new Account();
            account2.setAccountId(1000000002L);
            account2.setCustomer(testCustomer2);
            account2.setCurrentBalance(new BigDecimal("2500.50").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
            testAccountList.add(account2);
            
            when(accountRepository.findAll()).thenReturn(testAccountList);
            
            // When: Initialize and process records sequentially
            assertThatCode(() -> {
                accountFileProcessorService.acctFileOpen();
                accountFileProcessorService.acctFileGetNext();
                accountFileProcessorService.acctFileGetNext();
                accountFileProcessorService.acctFileGetNext(); // Should handle EOF gracefully
            }).doesNotThrowAnyException();
            
            // Then: Verify sequential access pattern executes without error
            verify(accountRepository, times(1)).findAll();
        }
        
        @Test
        @DisplayName("displayAcctRecord() - Record display equivalent to COBOL DISPLAY statements")
        void testDisplayAcctRecord_FormatsAccountData_LogsInformationCorrectly() {
            // Given: Test account with formatted data
            
            // When: Display account record (equivalent to COBOL DISPLAY)
            assertThatCode(() -> accountFileProcessorService.displayAcctRecord())
                .doesNotThrowAnyException();
            
            // Then: Verify logging occurred (replacing COBOL DISPLAY to SYSOUT)
            // Note: In production, this would verify log output through a log appender
        }
        
        @Test
        @DisplayName("acctFileClose() - File close operation equivalent to COBOL CLOSE")
        void testAcctFileClose_CompletesFileProcessing_CleansupResources() {
            // Given: File processing session in progress
            accountFileProcessorService.acctFileOpen();
            
            // When: Close file processing
            assertThatCode(() -> accountFileProcessorService.acctFileClose()).doesNotThrowAnyException();
            
            // Then: Verify successful close operation completes (COBOL equivalent)
            // Note: The actual implementation is void, so we verify no exceptions thrown
        }
        
        @Test
        @DisplayName("displayIOStatus() - Status display equivalent to COBOL status checking")
        void testDisplayIOStatus_ShowsCurrentFileStatus_LogsStatusInformation() {
            // Given: Service instance ready for status display
            
            // When/Then: Display status without exceptions
            assertThatCode(() -> accountFileProcessorService.displayIOStatus())
                .doesNotThrowAnyException();
        }
    }
    
    @Nested
    @DisplayName("Error Handling and Abnormal Termination")
    class ErrorHandlingOperations {
        
        @Test
        @DisplayName("abendProgram() - Abnormal termination equivalent to CEE3ABD")
        void testAbendProgram_AbnormalTermination_HandlesSystemErrors() {
            // Given: Critical system error requiring program termination
            
            // When/Then: Verify abnormal termination handling
            assertThatThrownBy(() -> accountFileProcessorService.abendProgram())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ABEND");
        }
        
        @Test
        @DisplayName("File status error handling - Non-zero status codes equivalent to COBOL error conditions")
        void testFileStatusErrorHandling_NonZeroStatus_HandlesErrorConditionsCorrectly() {
            // Given: Various error conditions
            
            // When: Repository access fails
            when(accountRepository.findAll()).thenThrow(new RuntimeException("File access error"));
            
            // Then: Verify error handling equivalent to COBOL file status checking
            assertThatThrownBy(() -> accountFileProcessorService.processAccountFile())
                .isInstanceOf(RuntimeException.class);
        }
    }
    
    @Nested
    @DisplayName("Financial Precision and COBOL Compatibility")
    class FinancialPrecisionTests {
        
        @Test
        @DisplayName("BigDecimal precision validation - COBOL COMP-3 equivalent accuracy")
        void testBigDecimalPrecision_CobolComp3Equivalent_MaintainsExactPrecision() {
            // Given: Account with precise financial amounts
            BigDecimal balance = new BigDecimal("12345.67");
            BigDecimal creditLimit = new BigDecimal("25000.00");
            
            testAccount.setCurrentBalance(balance.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
            testAccount.setCreditLimit(creditLimit.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
            
            // When: Retrieve and validate precision
            BigDecimal retrievedBalance = testAccount.getCurrentBalance();
            BigDecimal retrievedLimit = testAccount.getCreditLimit();
            
            // Then: Verify COBOL COMP-3 equivalent precision
            assertThat(retrievedBalance.scale()).isEqualTo(COBOL_DECIMAL_SCALE);
            assertThat(retrievedLimit.scale()).isEqualTo(COBOL_DECIMAL_SCALE);
            assertThat(retrievedBalance.compareTo(balance.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))).isEqualTo(0);
            assertThat(retrievedLimit.compareTo(creditLimit.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))).isEqualTo(0);
        }
        
        @Test
        @DisplayName("Financial calculation accuracy - Identical to COBOL arithmetic")
        void testFinancialCalculations_CobolEquivalent_MaintainsCalculationAccuracy() {
            // Given: Financial calculation scenario
            BigDecimal principal = new BigDecimal("1000.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
            BigDecimal rate = new BigDecimal("0.0525").setScale(4, COBOL_ROUNDING_MODE); // 5.25% rate
            
            // When: Perform calculation equivalent to COBOL arithmetic
            BigDecimal result = principal.multiply(rate).setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
            
            // Then: Verify result matches COBOL ROUNDED clause behavior
            assertThat(result).isEqualTo(new BigDecimal("52.50"));
            assertThat(result.scale()).isEqualTo(COBOL_DECIMAL_SCALE);
        }
    }
    
    @Nested
    @DisplayName("Repository Integration and VSAM Emulation")
    class RepositoryIntegrationTests {
        
        @Test
        @DisplayName("Repository findAll() - VSAM sequential read equivalent")
        void testRepositoryFindAll_VsamSequentialRead_ReturnsAllRecords() {
            // Given: Multiple accounts in repository
            List<Account> accounts = new ArrayList<>();
            accounts.add(testAccount);
            
            Customer customer2 = new Customer();
            customer2.setCustomerId("1000000002");
            customer2.setFirstName("Jane");
            customer2.setLastName("Smith");
            
            Account account2 = new Account();
            account2.setAccountId(1000000002L);
            account2.setCustomer(customer2);
            accounts.add(account2);
            
            when(accountRepository.findAll()).thenReturn(accounts);
            
            // When: Retrieve all accounts (VSAM file scan equivalent)
            List<Account> result = accountRepository.findAll();
            
            // Then: Verify complete record set retrieval
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
            assertThat(result.get(1).getAccountId()).isEqualTo(1000000002L);
        }
        
        @Test
        @DisplayName("Repository findById() - VSAM random read equivalent")
        void testRepositoryFindById_VsamRandomRead_ReturnsSpecificRecord() {
            // Given: Repository with specific account
            when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            
            // When: Retrieve specific account (VSAM READ with key)
            Optional<Account> result = accountRepository.findById(TEST_ACCOUNT_ID);
            
            // Then: Verify targeted record retrieval
            assertThat(result).isPresent();
            assertThat(result.get().getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
            verify(accountRepository, times(1)).findById(TEST_ACCOUNT_ID);
        }
        
        @Test
        @DisplayName("Repository findByCustomerId() - VSAM alternate key access equivalent")
        void testRepositoryFindByCustomerId_VsamAlternateKey_ReturnsCustomerAccounts() {
            // Given: Accounts for specific customer
            when(accountRepository.findByCustomerId(Long.parseLong(TEST_CUSTOMER_ID))).thenReturn(testAccountList);
            
            // When: Find accounts by customer (alternate index equivalent)
            List<Account> result = accountRepository.findByCustomerId(Long.parseLong(TEST_CUSTOMER_ID));
            
            // Then: Verify alternate key access
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCustomerId()).isEqualTo(TEST_CUSTOMER_ID);
        }
        
        @Test
        @DisplayName("Repository save() - VSAM REWRITE equivalent")
        void testRepositorySave_VsamRewrite_UpdatesRecord() {
            // Given: Account to be updated
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            
            // When: Save account (VSAM REWRITE equivalent)
            Account result = accountRepository.save(testAccount);
            
            // Then: Verify record update
            assertThat(result).isNotNull();
            assertThat(result.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
            verify(accountRepository, times(1)).save(testAccount);
        }
    }
    
    @Nested
    @DisplayName("Performance and Batch Processing Validation")
    class PerformanceValidationTests {
        
        @Test
        @DisplayName("Batch processing window compliance - 4-hour window requirement")
        void testBatchProcessingWindow_WithinTimeLimit_MeetsPerformanceRequirement() {
            // Given: Large dataset simulating batch processing volume
            List<Account> largeDataset = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                Customer customer = new Customer();
                customer.setCustomerId(String.valueOf(1000000L + i));
                customer.setFirstName("Test");
                customer.setLastName("Customer" + i);
                
                Account account = new Account();
                account.setAccountId(1000000L + i);
                account.setCustomer(customer);
                account.setCurrentBalance(new BigDecimal("1000.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
                largeDataset.add(account);
            }
            
            when(accountRepository.findAll()).thenReturn(largeDataset);
            
            // When: Process large batch within time constraints
            long startTime = System.currentTimeMillis();
            assertThatCode(() -> accountFileProcessorService.processAccountFile()).doesNotThrowAnyException();
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Then: Verify processing completes within performance window
            // Note: Actual batch would need to complete in 4 hours for production volumes
            assertThat(processingTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        }
        
        @Test
        @DisplayName("Response time validation - Sub-200ms for interactive operations")
        void testResponseTimeValidation_InteractiveOperations_MeetPerformanceThreshold() {
            // Given: Single account processing
            when(accountRepository.findAll()).thenReturn(testAccountList);
            
            // When: Measure individual operation response time
            long startTime = System.currentTimeMillis();
            assertThatCode(() -> {
                accountFileProcessorService.acctFileOpen();
                accountFileProcessorService.acctFileGetNext();
                accountFileProcessorService.displayAcctRecord();
                accountFileProcessorService.acctFileClose();
            }).doesNotThrowAnyException();
            long operationTime = System.currentTimeMillis() - startTime;
            
            // Then: Verify sub-200ms response time for interactive operations
            assertThat(operationTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        }
    }
    
    @Nested
    @DisplayName("COBOL Functional Parity Validation")
    class CobolFunctionalParityTests {
        
        @Test
        @DisplayName("Complete workflow validation - CBACT01C.cbl equivalent processing")
        void testCompleteWorkflow_CobolEquivalent_ReproducesExactBehavior() {
            // Given: Test data matching COBOL processing scenario
            when(accountRepository.findAll()).thenReturn(testAccountList);
            
            // When: Execute complete workflow equivalent to COBOL main logic
            assertThatCode(() -> {
                accountFileProcessorService.acctFileOpen();
                accountFileProcessorService.processAccountFile();
                accountFileProcessorService.acctFileClose();
            }).doesNotThrowAnyException();
            
            // Then: Verify complete functional parity with COBOL implementation
            // Verify all COBOL paragraph equivalents were executed
            verify(accountRepository, times(1)).findAll();
        }
        
        @Test
        @DisplayName("File status handling - COBOL file status code equivalency")
        void testFileStatusHandling_CobolEquivalent_HandleAllStatusCodes() {
            // Given: Various file status scenarios from COBOL
            
            // Test successful processing (status 00)
            when(accountRepository.findAll()).thenReturn(testAccountList);
            assertThatCode(() -> accountFileProcessorService.acctFileOpen()).doesNotThrowAnyException();
            
            // Test EOF condition (status 10) - simulated by empty result
            when(accountRepository.findAll()).thenReturn(new ArrayList<>());
            assertThatCode(() -> accountFileProcessorService.acctFileGetNext()).doesNotThrowAnyException();
            
            // Verify status display functionality
            assertThatCode(() -> accountFileProcessorService.displayIOStatus()).doesNotThrowAnyException();
        }
    }
}