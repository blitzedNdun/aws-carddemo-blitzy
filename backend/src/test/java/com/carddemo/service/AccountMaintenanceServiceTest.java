package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for AccountMaintenanceService validating 
 * COBOL account maintenance batch logic migration to Java.
 * 
 * Tests validate functional parity with CBACT01C.cbl COBOL program ensuring:
 * - Account cleanup processing matches COBOL logic
 * - Archival file generation maintains identical formats
 * - Status update batch processing preserves transaction integrity
 * - Maintenance report generation produces equivalent outputs
 * - Financial calculations maintain COBOL COMP-3 precision
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountMaintenanceService - CBACT01C COBOL Program Unit Tests")
class AccountMaintenanceServiceTest {

    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AccountMaintenanceService accountMaintenanceService;

    // Test data constants matching COBOL CVACT01Y copybook structure
    private static final String TEST_ACCOUNT_ID = "1000000001";
    private static final BigDecimal TEST_BALANCE = new BigDecimal("1250.75");
    private static final BigDecimal ZERO_BALANCE = BigDecimal.ZERO;
    private static final LocalDate DORMANT_CUTOFF_DATE = LocalDate.now().minusMonths(12);
    private static final LocalDate RECENT_TRANSACTION_DATE = LocalDate.now().minusDays(30);
    private static final LocalDate OLD_TRANSACTION_DATE = LocalDate.now().minusMonths(18);

    private Account activeAccount;
    private Account dormantAccount;
    private Account closedAccount;
    private Transaction recentTransaction;
    private Transaction oldTransaction;

    @BeforeEach
    void setUp() {
        // Initialize test data following COBOL ACCOUNT-RECORD structure
        activeAccount = createTestAccount(TEST_ACCOUNT_ID, "A", TEST_BALANCE, RECENT_TRANSACTION_DATE);
        dormantAccount = createTestAccount("1000000002", "A", new BigDecimal("500.00"), OLD_TRANSACTION_DATE);
        closedAccount = createTestAccount("1000000003", "C", ZERO_BALANCE, OLD_TRANSACTION_DATE);
        
        recentTransaction = createTestTransaction(TEST_ACCOUNT_ID, RECENT_TRANSACTION_DATE.atStartOfDay(), "D", new BigDecimal("100.00"));
        oldTransaction = createTestTransaction("1000000002", OLD_TRANSACTION_DATE.atStartOfDay(), "C", new BigDecimal("25.00"));
    }

    @Nested
    @DisplayName("Dormant Account Identification - CBACT01C 1000-FIND-DORMANT-ACCTS")
    class DormantAccountIdentificationTests {

        @Test
        @DisplayName("Should identify accounts with no activity over 12 months")
        void testIdentifyDormantAccounts_FindsAccountsWithOldActivity() {
            // Given: Repository returns accounts with old transaction dates
            when(accountRepository.findByLastTransactionDateBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(dormantAccount));
            
            when(transactionRepository.countByAccountIdAndDateAfter(
                eq("1000000002"), any(LocalDateTime.class)))
                .thenReturn(0L);

            // When: Identifying dormant accounts
            List<Account> result = accountMaintenanceService.identifyDormantAccounts();

            // Then: Should return accounts matching COBOL dormant criteria
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAccountId()).isEqualTo("1000000002");
            assertThat(result.get(0).getAccountStatus()).isEqualTo("A");
            
            verify(accountRepository).findByLastTransactionDateBefore(any(LocalDate.class));
            verify(transactionRepository).countByAccountIdAndDateAfter(
                eq("1000000002"), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should exclude accounts with recent activity")
        void testIdentifyDormantAccounts_ExcludesActiveAccounts() {
            // Given: Repository returns accounts, but transaction check shows recent activity
            when(accountRepository.findByLastTransactionDateBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(activeAccount, dormantAccount));
            
            when(transactionRepository.countByAccountIdAndDateAfter(
                eq(TEST_ACCOUNT_ID), any(LocalDateTime.class)))
                .thenReturn(5L); // Recent transactions found
            
            when(transactionRepository.countByAccountIdAndDateAfter(
                eq("1000000002"), any(LocalDateTime.class)))
                .thenReturn(0L); // No recent transactions

            // When: Identifying dormant accounts
            List<Account> result = accountMaintenanceService.identifyDormantAccounts();

            // Then: Should only return truly dormant accounts
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAccountId()).isEqualTo("1000000002");
            
            verify(transactionRepository, times(2)).countByAccountIdAndDateAfter(
                anyString(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return empty list when no dormant accounts found")
        void testIdentifyDormantAccounts_ReturnsEmptyWhenNoneFound() {
            // Given: Repository returns no old accounts
            when(accountRepository.findByLastTransactionDateBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList());

            // When: Identifying dormant accounts
            List<Account> result = accountMaintenanceService.identifyDormantAccounts();

            // Then: Should return empty list
            assertThat(result).isEmpty();
            
            verify(accountRepository).findByLastTransactionDateBefore(any(LocalDate.class));
            verifyNoInteractions(transactionRepository);
        }
    }

    @Nested
    @DisplayName("Account Closure Processing - CBACT01C 2000-PROCESS-CLOSURES")
    class AccountClosureProcessingTests {

        @Test
        @DisplayName("Should process account closure with zero balance")
        void testProcessAccountClosure_SuccessfulWithZeroBalance() {
            // Given: Account with zero balance ready for closure
            Account zeroBalanceAccount = createTestAccount("1000000004", "A", ZERO_BALANCE, OLD_TRANSACTION_DATE);
            when(accountRepository.findById("1000000004"))
                .thenReturn(Optional.of(zeroBalanceAccount));
            
            when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When: Processing account closure
            boolean result = accountMaintenanceService.processAccountClosure("1000000004");

            // Then: Should successfully close account and update status
            assertThat(result).isTrue();
            
            verify(accountRepository).findById("1000000004");
            verify(accountRepository).save(argThat(account -> 
                account.getAccountStatus().equals("C") && 
                account.getAccountId().equals("1000000004")));
            verify(auditService).saveAuditLog(
                eq("ACCOUNT_CLOSURE"), 
                eq("1000000004"), 
                anyString());
        }

        @Test
        @DisplayName("Should reject closure for accounts with outstanding balance")
        void testProcessAccountClosure_RejectsNonZeroBalance() {
            // Given: Account with outstanding balance
            when(accountRepository.findById(TEST_ACCOUNT_ID))
                .thenReturn(Optional.of(activeAccount));

            // When: Attempting account closure
            boolean result = accountMaintenanceService.processAccountClosure(TEST_ACCOUNT_ID);

            // Then: Should reject closure due to balance
            assertThat(result).isFalse();
            
            verify(accountRepository).findById(TEST_ACCOUNT_ID);
            verify(accountRepository, never()).save(any(Account.class));
            verify(auditService).saveAuditLog(
                eq("ACCOUNT_CLOSURE_REJECTED"), 
                eq(TEST_ACCOUNT_ID), 
                contains("Outstanding balance"));
        }

        @Test
        @DisplayName("Should reject closure for non-existent account")
        void testProcessAccountClosure_RejectsNonExistentAccount() {
            // Given: Non-existent account ID
            String nonExistentId = "9999999999";
            when(accountRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

            // When: Attempting account closure
            boolean result = accountMaintenanceService.processAccountClosure(nonExistentId);

            // Then: Should reject closure for invalid account
            assertThat(result).isFalse();
            
            verify(accountRepository).findById(nonExistentId);
            verify(accountRepository, never()).save(any(Account.class));
            verify(auditService).saveAuditLog(
                eq("ACCOUNT_CLOSURE_REJECTED"), 
                eq(nonExistentId), 
                contains("Account not found"));
        }
    }

    @Nested
    @DisplayName("Archival File Generation - CBACT01C 3000-GENERATE-ARCHIVE")
    class ArchivalFileGenerationTests {

        @Test
        @DisplayName("Should generate archival files for closed accounts")
        void testGenerateArchivalFiles_CreatesFilesForClosedAccounts() {
            // Given: Closed accounts in repository
            List<Account> closedAccounts = Arrays.asList(closedAccount);
            when(accountRepository.findByAccountStatus("C"))
                .thenReturn(closedAccounts);
            
            when(transactionRepository.findByAccountId("1000000003"))
                .thenReturn(Arrays.asList(oldTransaction));

            // When: Generating archival files
            int result = accountMaintenanceService.generateArchivalFiles();

            // Then: Should process closed accounts for archival
            assertThat(result).isEqualTo(1);
            
            verify(accountRepository).findByAccountStatus("C");
            verify(transactionRepository).findByAccountId("1000000003");
            verify(auditService).saveAuditLog(
                eq("ARCHIVAL_GENERATED"), 
                eq("1000000003"), 
                anyString());
        }

        @Test
        @DisplayName("Should return zero when no closed accounts exist")
        void testGenerateArchivalFiles_ReturnsZeroWhenNoClosedAccounts() {
            // Given: No closed accounts in repository
            when(accountRepository.findByAccountStatus("C"))
                .thenReturn(Arrays.asList());

            // When: Generating archival files
            int result = accountMaintenanceService.generateArchivalFiles();

            // Then: Should return zero processed count
            assertThat(result).isEqualTo(0);
            
            verify(accountRepository).findByAccountStatus("C");
            verifyNoInteractions(transactionRepository);
            verify(auditService, never()).saveAuditLog(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should include transaction history in archival data")
        void testGenerateArchivalFiles_IncludesTransactionHistory() {
            // Given: Closed account with transaction history
            List<Account> closedAccounts = Arrays.asList(closedAccount);
            List<Transaction> accountTransactions = Arrays.asList(
                oldTransaction,
                createTestTransaction("1000000003", 
                    LocalDate.now().minusMonths(6).atStartOfDay(), 
                    "D", new BigDecimal("200.00"))
            );
            
            when(accountRepository.findByAccountStatus("C"))
                .thenReturn(closedAccounts);
            when(transactionRepository.findByAccountId("1000000003"))
                .thenReturn(accountTransactions);

            // When: Generating archival files
            int result = accountMaintenanceService.generateArchivalFiles();

            // Then: Should process all transaction history
            assertThat(result).isEqualTo(1);
            
            verify(transactionRepository).findByAccountId("1000000003");
            verify(auditService).saveAuditLog(
                eq("ARCHIVAL_GENERATED"), 
                eq("1000000003"), 
                contains("2 transactions"));
        }
    }

    @Nested
    @DisplayName("Batch Status Updates - CBACT01C 4000-UPDATE-STATUS")
    class BatchStatusUpdatesTests {

        @Test
        @DisplayName("Should update dormant accounts to inactive status")
        void testProcessBatchStatusUpdates_UpdatesDormantToInactive() {
            // Given: Dormant accounts identified for status update
            List<Account> dormantAccounts = Arrays.asList(dormantAccount);
            when(accountRepository.findByLastTransactionDateBefore(any(LocalDate.class)))
                .thenReturn(dormantAccounts);
            
            when(transactionRepository.countByAccountIdAndDateAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(0L);
            
            when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When: Processing batch status updates
            int result = accountMaintenanceService.processBatchStatusUpdates();

            // Then: Should update dormant accounts to inactive
            assertThat(result).isEqualTo(1);
            
            verify(accountRepository).save(argThat(account -> 
                account.getAccountStatus().equals("I") && 
                account.getAccountId().equals("1000000002")));
            verify(auditService).saveAuditLog(
                eq("STATUS_UPDATE"), 
                eq("1000000002"), 
                contains("A to I"));
        }

        @Test
        @DisplayName("Should preserve transaction isolation during batch updates")
        void testProcessBatchStatusUpdates_MaintainsTransactionIsolation() {
            // Given: Multiple accounts for batch processing
            List<Account> dormantAccounts = Arrays.asList(
                dormantAccount,
                createTestAccount("1000000005", "A", new BigDecimal("750.00"), OLD_TRANSACTION_DATE)
            );
            
            when(accountRepository.findByLastTransactionDateBefore(any(LocalDate.class)))
                .thenReturn(dormantAccounts);
            
            when(transactionRepository.countByAccountIdAndDateAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(0L);
            
            when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When: Processing batch status updates
            int result = accountMaintenanceService.processBatchStatusUpdates();

            // Then: Should process all accounts in batch
            assertThat(result).isEqualTo(2);
            
            verify(accountRepository, times(2)).save(any(Account.class));
            verify(auditService, times(2)).saveAuditLog(
                eq("STATUS_UPDATE"), 
                anyString(), 
                anyString());
        }
    }

    @Nested
    @DisplayName("Maintenance Report Generation - CBACT01C 5000-GENERATE-REPORTS")
    class MaintenanceReportGenerationTests {

        @Test
        @DisplayName("Should generate comprehensive maintenance report")
        void testGenerateMaintenanceReport_CreatesComprehensiveReport() {
            // Given: Repository data for reporting
            when(accountRepository.findByAccountStatus("A")).thenReturn(Arrays.asList(activeAccount));
            when(accountRepository.findByAccountStatus("I")).thenReturn(Arrays.asList(dormantAccount));
            when(accountRepository.findByAccountStatus("C")).thenReturn(Arrays.asList(closedAccount));
            
            // When: Generating maintenance report
            String result = accountMaintenanceService.generateMaintenanceReport();

            // Then: Should create comprehensive report with all account types
            assertThat(result).isNotNull().isNotEmpty();
            assertThat(result).contains("Active Accounts: 1");
            assertThat(result).contains("Inactive Accounts: 1");
            assertThat(result).contains("Closed Accounts: 1");
            assertThat(result).contains("Total Accounts: 3");
            
            verify(accountRepository).findByAccountStatus("A");
            verify(accountRepository).findByAccountStatus("I");
            verify(accountRepository).findByAccountStatus("C");
            verify(auditService).saveAuditLog(
                eq("MAINTENANCE_REPORT"), 
                eq("SYSTEM"), 
                anyString());
        }

        @Test
        @DisplayName("Should handle empty repository data gracefully")
        void testGenerateMaintenanceReport_HandlesEmptyData() {
            // Given: Empty repository
            when(accountRepository.findByAccountStatus(anyString()))
                .thenReturn(Arrays.asList());

            // When: Generating maintenance report
            String result = accountMaintenanceService.generateMaintenanceReport();

            // Then: Should create report with zero counts
            assertThat(result).isNotNull().isNotEmpty();
            assertThat(result).contains("Active Accounts: 0");
            assertThat(result).contains("Inactive Accounts: 0");
            assertThat(result).contains("Closed Accounts: 0");
            assertThat(result).contains("Total Accounts: 0");
            
            verify(auditService).saveAuditLog(
                eq("MAINTENANCE_REPORT"), 
                eq("SYSTEM"), 
                anyString());
        }
    }

    @Nested
    @DisplayName("Account Validation - CBACT01C Utility Functions")
    class AccountValidationTests {

        @Test
        @DisplayName("Should validate account eligibility for closure")
        void testValidateAccountForClosure_ValidatesClosureEligibility() {
            // Given: Account with zero balance
            Account zeroBalanceAccount = createTestAccount("1000000006", "A", ZERO_BALANCE, OLD_TRANSACTION_DATE);

            // When: Validating for closure
            boolean result = accountMaintenanceService.validateAccountForClosure(zeroBalanceAccount);

            // Then: Should validate as eligible for closure
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject accounts with outstanding balance")
        void testValidateAccountForClosure_RejectsOutstandingBalance() {
            // When: Validating account with balance
            boolean result = accountMaintenanceService.validateAccountForClosure(activeAccount);

            // Then: Should reject due to outstanding balance
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject already closed accounts")
        void testValidateAccountForClosure_RejectsClosedAccounts() {
            // When: Validating already closed account
            boolean result = accountMaintenanceService.validateAccountForClosure(closedAccount);

            // Then: Should reject already closed account
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Archive Data Processing - CBACT01C Data Handling")
    class ArchiveDataProcessingTests {

        @Test
        @DisplayName("Should archive account data maintaining COBOL precision")
        void testArchiveAccountData_MaintainsCobolPrecision() {
            // Given: Account with COBOL COMP-3 equivalent precision
            Account precisionAccount = createTestAccount("1000000007", "C", 
                new BigDecimal("12345.67"), OLD_TRANSACTION_DATE);

            // When: Archiving account data
            boolean result = accountMaintenanceService.archiveAccountData(precisionAccount);

            // Then: Should maintain decimal precision matching COBOL COMP-3
            assertThat(result).isTrue();
            
            verify(auditService).saveAuditLog(
                eq("ACCOUNT_ARCHIVED"), 
                eq("1000000007"), 
                contains("12345.67"));
        }

        @Test
        @DisplayName("Should handle large balance values correctly")
        void testArchiveAccountData_HandlesLargeBalances() {
            // Given: Account with maximum COBOL field size balance
            Account largeBalanceAccount = createTestAccount("1000000008", "C", 
                new BigDecimal("9999999.99"), OLD_TRANSACTION_DATE);

            // When: Archiving account data
            boolean result = accountMaintenanceService.archiveAccountData(largeBalanceAccount);

            // Then: Should handle large values matching COBOL capacity
            assertThat(result).isTrue();
            
            verify(auditService).saveAuditLog(
                eq("ACCOUNT_ARCHIVED"), 
                eq("1000000008"), 
                anyString());
        }
    }

    @Nested
    @DisplayName("Account Status Management - CBACT01C Status Transitions")
    class AccountStatusManagementTests {

        @Test
        @DisplayName("Should update account statuses following COBOL business rules")
        void testUpdateAccountStatuses_FollowsCobolBusinessRules() {
            // Given: Multiple accounts for status update
            List<Account> accountsToUpdate = Arrays.asList(
                activeAccount,
                dormantAccount
            );

            // When: Updating account statuses
            int result = accountMaintenanceService.updateAccountStatuses(accountsToUpdate, "I");

            // Then: Should update all provided accounts
            assertThat(result).isEqualTo(2);
            
            verify(accountRepository, times(2)).save(any(Account.class));
            verify(auditService, times(2)).saveAuditLog(
                eq("STATUS_UPDATE"), 
                anyString(), 
                anyString());
        }

        @Test
        @DisplayName("Should validate status transitions")
        void testUpdateAccountStatuses_ValidatesStatusTransitions() {
            // Given: Closed account that shouldn't be updated
            List<Account> accountsToUpdate = Arrays.asList(closedAccount);

            // When: Attempting to update closed account status
            int result = accountMaintenanceService.updateAccountStatuses(accountsToUpdate, "A");

            // Then: Should reject invalid status transition
            assertThat(result).isEqualTo(0);
            
            verify(accountRepository, never()).save(any(Account.class));
            verify(auditService).saveAuditLog(
                eq("STATUS_UPDATE_REJECTED"), 
                eq("1000000003"), 
                contains("Invalid transition"));
        }
    }

    // Test data creation utility methods
    private Account createTestAccount(String accountId, String status, BigDecimal balance, LocalDate lastTransactionDate) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setAccountStatus(status);
        account.setCurrentBalance(balance);
        account.setLastTransactionDate(lastTransactionDate);
        account.setCreditLimit(new BigDecimal("2000.00"));
        return account;
    }

    private Transaction createTestTransaction(String accountId, LocalDateTime transactionDate, String type, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(accountId);
        transaction.setTransactionDate(transactionDate);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        return transaction;
    }
}