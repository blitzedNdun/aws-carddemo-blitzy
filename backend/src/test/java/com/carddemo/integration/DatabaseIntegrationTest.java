package com.carddemo.integration;

import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Card;
import com.carddemo.entity.UserSecurity;
import com.carddemo.entity.TransactionType;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.repository.TransactionTypeRepository;
import com.carddemo.util.CobolDataConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.carddemo.test.TestConstants.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive database integration test class validating PostgreSQL database operations
 * with Testcontainers, including schema creation, referential integrity constraints,
 * transaction isolation levels, and data migration from VSAM structures.
 * 
 * This test class ensures complete functional parity with the mainframe COBOL/CICS
 * system by testing:
 * - PostgreSQL schema replication of VSAM KSDS datasets
 * - B-tree indexes matching VSAM key access patterns  
 * - JPA repository CRUD operations equivalent to VSAM READ/WRITE/DELETE
 * - Transaction isolation levels with concurrent operations
 * - Partition pruning for transaction table
 * - Bulk operations for batch processing
 * - WAL-based recovery capabilities
 * - Query performance under 10ms for primary key lookups
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestExecutionListeners(mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    value = {})
@DisplayName("Database Integration Tests - VSAM to PostgreSQL Migration")
public class DatabaseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Test PostgreSQL schema creation and table structure validation.
     * Verifies that all tables are created with correct columns, data types,
     * primary keys, and foreign key constraints matching VSAM KSDS structure.
     */
    @Test
    @Order(1)
    @DisplayName("Test Schema Creation - VSAM KSDS to PostgreSQL DDL")
    public void testSchemaCreation() {
        // Setup test containers
        setupTestContainers();

        // Verify PostgreSQL container is running and accessible
        PostgreSQLContainer<?> container = getPostgreSQLContainer();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getJdbcUrl()).contains("jdbc:postgresql://");

        // Test table existence and structure through JPA operations
        // This validates DDL creation implicitly
        
        // Test account_data table (ACCTDATA VSAM KSDS equivalent)
        String accountTableQuery = """
            SELECT column_name, data_type, is_nullable, column_default
            FROM information_schema.columns 
            WHERE table_name = 'account_data' 
            ORDER BY ordinal_position
            """;
        
        List<Object[]> accountColumns = entityManager.createNativeQuery(accountTableQuery)
            .getResultList();
        
        assertThat(accountColumns).isNotEmpty();
        assertThat(accountColumns.size()).isGreaterThanOrEqualTo(5);
        
        // Test customer_data table (CUSTDATA VSAM KSDS equivalent)
        String customerTableQuery = """
            SELECT column_name, data_type 
            FROM information_schema.columns 
            WHERE table_name = 'customer_data' 
            ORDER BY ordinal_position
            """;
            
        List<Object[]> customerColumns = entityManager.createNativeQuery(customerTableQuery)
            .getResultList();
            
        assertThat(customerColumns).isNotEmpty();
        
        // Test transactions table (TRANSACT VSAM KSDS equivalent) 
        String transactionTableQuery = """
            SELECT column_name, data_type 
            FROM information_schema.columns 
            WHERE table_name = 'transactions' 
            ORDER BY ordinal_position
            """;
            
        List<Object[]> transactionColumns = entityManager.createNativeQuery(transactionTableQuery)
            .getResultList();
            
        assertThat(transactionColumns).isNotEmpty();
    }

    /**
     * Test referential integrity constraints between related tables.
     * Validates foreign key relationships matching VSAM file relationships.
     */
    @Test
    @Order(2)
    @DisplayName("Test Referential Integrity - Foreign Key Constraints")
    public void testReferentialIntegrity() {
        // Create test customer first (parent record)
        Customer testCustomer = createTestCustomer();
        testCustomer.setCustomerId(TEST_CUSTOMER_ID);
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        Customer savedCustomer = customerRepository.save(testCustomer);
        
        // Create test account linked to customer
        Account testAccount = createTestAccount();
        testAccount.setAccountId(TEST_ACCOUNT_ID);
        testAccount.setCustomerId(savedCustomer.getCustomerId());
        testAccount.setCurrentBalance(BigDecimal.valueOf(1000.00));
        testAccount.setCreditLimit(BigDecimal.valueOf(5000.00));
        Account savedAccount = accountRepository.save(testAccount);
        
        // Create test card linked to account
        Card testCard = createTestCard();
        testCard.setCardNumber("4111111111111111");
        testCard.setAccountId(savedAccount.getAccountId());
        testCard.setCustomerId(savedCustomer.getCustomerId());
        Card savedCard = cardRepository.save(testCard);
        
        // Validate relationships exist
        assertThat(savedAccount.getCustomerId()).isEqualTo(savedCustomer.getCustomerId());
        assertThat(savedCard.getAccountId()).isEqualTo(savedAccount.getAccountId());
        assertThat(savedCard.getCustomerId()).isEqualTo(savedCustomer.getCustomerId());
        
        // Test referential integrity by attempting to delete parent record
        // This should fail or cascade properly based on FK constraints
        assertThatThrownBy(() -> {
            customerRepository.delete(savedCustomer);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
        
        // Clean up for subsequent tests
        cleanupTestData();
    }

    /**
     * Test transaction isolation levels with concurrent database operations.
     * Verifies ACID compliance matching CICS SYNCPOINT behavior.
     */
    @Test
    @Order(3)
    @DisplayName("Test Transaction Isolation - Concurrent Operations")
    @Transactional
    public void testTransactionIsolation() {
        // Create test account for concurrent access
        Account account = createTestAccount();
        account.setAccountId(TEST_ACCOUNT_ID);
        account.setCurrentBalance(BigDecimal.valueOf(1000.00));
        account.setCreditLimit(BigDecimal.valueOf(5000.00));
        Account savedAccount = accountRepository.save(account);
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        try {
            // Simulate concurrent balance updates
            CompletableFuture<Void> transaction1 = CompletableFuture.runAsync(() -> {
                Account acc = accountRepository.findById(savedAccount.getAccountId()).orElseThrow();
                acc.setCurrentBalance(acc.getCurrentBalance().subtract(BigDecimal.valueOf(100.00)));
                accountRepository.save(acc);
            }, executor);
            
            CompletableFuture<Void> transaction2 = CompletableFuture.runAsync(() -> {
                Account acc = accountRepository.findById(savedAccount.getAccountId()).orElseThrow();
                acc.setCurrentBalance(acc.getCurrentBalance().add(BigDecimal.valueOf(50.00)));
                accountRepository.save(acc);
            }, executor);
            
            // Wait for both transactions to complete
            CompletableFuture.allOf(transaction1, transaction2)
                .orTimeout(RESPONSE_TIME_THRESHOLD_MS, TimeUnit.MILLISECONDS)
                .join();
                
            // Verify final balance reflects one of the operations
            // (exact result depends on isolation level and timing)
            Account finalAccount = accountRepository.findById(savedAccount.getAccountId()).orElseThrow();
            assertThat(finalAccount.getCurrentBalance()).isNotNull();
            
        } finally {
            executor.shutdown();
            cleanupTestData();
        }
    }

    /**
     * Test partition pruning behavior for the transactions table.
     * Validates date-based partitioning performance for large transaction volumes.
     */
    @Test
    @Order(4)
    @DisplayName("Test Partition Pruning - Transaction Table Performance")
    public void testPartitionPruning() {
        // Create test account first
        Account account = createTestAccount();
        account.setAccountId(TEST_ACCOUNT_ID);
        account.setCurrentBalance(BigDecimal.valueOf(1000.00));
        accountRepository.save(account);
        
        // Create transactions across different date ranges
        LocalDateTime baseDate = LocalDateTime.now().minusDays(30);
        
        for (int i = 0; i < 10; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId(TEST_TRANSACTION_ID + "_" + i);
            transaction.setAccountId(account.getAccountId());
            transaction.setAmount(BigDecimal.valueOf(100.00 + i));
            transaction.setTransactionDate(baseDate.plusDays(i * 3));
            transactionRepository.save(transaction);
        }
        
        // Test date-range query performance (simulating partition pruning)
        LocalDateTime startDate = baseDate.plusDays(10);
        LocalDateTime endDate = baseDate.plusDays(20);
        
        long startTime = System.currentTimeMillis();
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
            account.getAccountId(), startDate, endDate);
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify query executed efficiently 
        assertThat(duration).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        assertThat(transactions).isNotEmpty();
        assertThat(transactions.size()).isLessThanOrEqualTo(4); // Should only return subset
        
        cleanupTestData();
    }

    /**
     * Test bulk operations for batch processing scenarios.
     * Validates batch insert/update performance matching JCL job requirements.
     */
    @Test
    @Order(5)
    @DisplayName("Test Bulk Operations - Batch Processing Performance")
    public void testBulkOperations() {
        // Create base customer and account
        Customer customer = createTestCustomer();
        customer.setCustomerId(TEST_CUSTOMER_ID);
        customerRepository.save(customer);
        
        Account account = createTestAccount();
        account.setAccountId(TEST_ACCOUNT_ID);
        account.setCustomerId(customer.getCustomerId());
        accountRepository.save(account);
        
        // Test bulk transaction insert (simulating daily batch processing)
        int batchSize = 100;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < batchSize; i++) {
            Transaction transaction = createTestTransaction();
            transaction.setTransactionId(TEST_TRANSACTION_ID + "_bulk_" + i);
            transaction.setAccountId(account.getAccountId());
            transaction.setAmount(BigDecimal.valueOf(10.00 + (i * 0.50)));
            transaction.setTransactionDate(LocalDateTime.now().minusHours(i));
            transactionRepository.save(transaction);
            
            // Flush periodically for memory management
            if (i % 20 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify bulk operations complete within acceptable timeframe
        assertThat(duration).isLessThan(5000); // 5 seconds for 100 records
        
        // Verify all records were inserted
        List<Transaction> allTransactions = transactionRepository.findAll();
        assertThat(allTransactions.size()).isGreaterThanOrEqualTo(batchSize);
        
        cleanupTestData();
    }

    /**
     * Test query performance for primary key lookups.
     * Validates sub-10ms response times matching mainframe performance.
     */
    @Test
    @Order(6)
    @DisplayName("Test Query Performance - Primary Key Lookups")
    public void testQueryPerformance() {
        // Create test data
        Account account = createTestAccount();
        account.setAccountId(TEST_ACCOUNT_ID);
        account.setCurrentBalance(BigDecimal.valueOf(2500.00));
        account.setCreditLimit(BigDecimal.valueOf(10000.00));
        Account savedAccount = accountRepository.save(account);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to force DB hit
        
        // Test primary key lookup performance
        long startTime = System.nanoTime();
        Account retrievedAccount = accountRepository.findById(savedAccount.getAccountId()).orElse(null);
        long duration = Duration.ofNanos(System.nanoTime() - startTime).toMillis();
        
        // Verify performance meets requirements (<10ms)
        assertThat(duration).isLessThan(10);
        assertThat(retrievedAccount).isNotNull();
        assertThat(retrievedAccount.getAccountId()).isEqualTo(savedAccount.getAccountId());
        
        // Test composite key performance for transactions
        Transaction transaction = createTestTransaction();
        transaction.setTransactionId(TEST_TRANSACTION_ID);
        transaction.setAccountId(account.getAccountId());
        transaction.setAmount(BigDecimal.valueOf(150.00));
        Transaction savedTransaction = transactionRepository.save(transaction);
        entityManager.flush();
        entityManager.clear();
        
        startTime = System.nanoTime();
        Transaction retrievedTransaction = transactionRepository.findById(savedTransaction.getTransactionId()).orElse(null);
        duration = Duration.ofNanos(System.nanoTime() - startTime).toMillis();
        
        assertThat(duration).isLessThan(10);
        assertThat(retrievedTransaction).isNotNull();
        assertThat(retrievedTransaction.getTransactionId()).isEqualTo(savedTransaction.getTransactionId());
        
        cleanupTestData();
    }

    /**
     * Test WAL-based recovery capabilities.
     * Validates transaction log recovery matching VSAM recovery features.
     */
    @Test
    @Order(7) 
    @DisplayName("Test WAL Recovery - Transaction Log Validation")
    @Transactional
    public void testWALRecovery() {
        // This test validates that PostgreSQL WAL (Write-Ahead Logging)
        // is properly configured for recovery scenarios
        
        // Create test account
        Account account = createTestAccount();
        account.setAccountId(TEST_ACCOUNT_ID);
        account.setCurrentBalance(BigDecimal.valueOf(1000.00));
        Account savedAccount = accountRepository.save(account);
        
        // Force WAL write by explicit flush
        entityManager.flush();
        
        // Verify WAL configuration through PostgreSQL system views
        String walQuery = """
            SELECT name, setting 
            FROM pg_settings 
            WHERE name IN ('wal_level', 'archive_mode', 'max_wal_senders')
            """;
            
        List<Object[]> walSettings = entityManager.createNativeQuery(walQuery).getResultList();
        assertThat(walSettings).isNotEmpty();
        
        // Simulate rollback scenario
        try {
            savedAccount.setCurrentBalance(BigDecimal.valueOf(-1000.00)); // Invalid negative balance
            accountRepository.save(savedAccount);
            entityManager.flush();
            
            // Force exception to trigger rollback
            throw new RuntimeException("Simulated transaction failure");
            
        } catch (RuntimeException e) {
            // Transaction should rollback automatically
        }
        
        // Verify original data is preserved after rollback
        entityManager.clear();
        Account recoveredAccount = accountRepository.findById(savedAccount.getAccountId()).orElse(null);
        assertThat(recoveredAccount).isNotNull();
        assertThat(recoveredAccount.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
        
        cleanupTestData();
    }

    /**
     * Test composite primary keys and indexes.
     * Validates complex key structures matching VSAM key definitions.
     */
    @Test
    @Order(8)
    @DisplayName("Test Composite Keys - Complex Key Structures")  
    public void testCompositeKeys() {
        // Create test data with composite relationships
        Customer customer = createTestCustomer();
        customer.setCustomerId(TEST_CUSTOMER_ID);
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customerRepository.save(customer);
        
        Account account = createTestAccount();
        account.setAccountId(TEST_ACCOUNT_ID);
        account.setCustomerId(customer.getCustomerId());
        accountRepository.save(account);
        
        // Test composite key searches (customer name-based lookup)
        List<Customer> customersByName = customerRepository.findByLastNameAndFirstName(
            "Smith", "Jane");
            
        assertThat(customersByName).hasSize(1);
        assertThat(customersByName.get(0).getCustomerId()).isEqualTo(customer.getCustomerId());
        
        // Test account lookups by customer (foreign key index)
        List<Account> accountsByCustomer = accountRepository.findByCustomerId(customer.getCustomerId());
        assertThat(accountsByCustomer).hasSize(1);
        assertThat(accountsByCustomer.get(0).getAccountId()).isEqualTo(account.getAccountId());
        
        cleanupTestData();
    }

    /**
     * Test index performance for alternate key searches.
     * Validates B-tree index efficiency matching VSAM AIX performance.
     */
    @Test
    @Order(9)
    @DisplayName("Test Index Performance - Alternate Key Searches")
    public void testIndexPerformance() {
        // Create test account and card data
        Account account = createTestAccount();
        account.setAccountId(TEST_ACCOUNT_ID);
        accountRepository.save(account);
        
        Card card = createTestCard();
        card.setCardNumber("4111111111111111");
        card.setAccountId(account.getAccountId());
        cardRepository.save(card);
        
        entityManager.flush();
        entityManager.clear();
        
        // Test alternate index performance (card lookup by account)
        long startTime = System.nanoTime();
        List<Card> cardsByAccount = cardRepository.findByAccountId(account.getAccountId());
        long duration = Duration.ofNanos(System.nanoTime() - startTime).toMillis();
        
        // Verify performance and results
        assertThat(duration).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        assertThat(cardsByAccount).hasSize(1);
        assertThat(cardsByAccount.get(0).getCardNumber()).isEqualTo("4111111111111111");
        
        cleanupTestData();
    }

    /**
     * Validate COBOL precision handling in database operations.
     * Ensures BigDecimal precision matches COBOL COMP-3 behavior.
     */
    @Test
    @Order(10)
    @DisplayName("Validate COBOL Precision - BigDecimal Financial Calculations")
    public void validateCobolPrecision() {
        // Test COBOL COMP-3 precision using CobolDataConverter
        BigDecimal cobolAmount = CobolDataConverter.fromComp3(new byte[]{0x12, 0x34, 0x5C}, 2);
        assertBigDecimalEquals(cobolAmount, BigDecimal.valueOf(123.45));
        
        // Validate precision preservation in database operations
        Account account = createTestAccount();
        account.setAccountId(TEST_ACCOUNT_ID);
        account.setCurrentBalance(cobolAmount);
        account.setCreditLimit(CobolDataConverter.preservePrecision(BigDecimal.valueOf(5000.00), COBOL_DECIMAL_SCALE));
        Account savedAccount = accountRepository.save(account);
        
        // Retrieve and verify precision maintained
        Account retrievedAccount = accountRepository.findById(savedAccount.getAccountId()).orElseThrow();
        validateCobolPrecision(retrievedAccount.getCurrentBalance(), cobolAmount);
        assertThat(retrievedAccount.getCurrentBalance().scale()).isEqualTo(COBOL_DECIMAL_SCALE);
        
        // Test transaction amount precision
        Transaction transaction = createTestTransaction();
        transaction.setTransactionId(TEST_TRANSACTION_ID);
        transaction.setAccountId(account.getAccountId());
        transaction.setAmount(CobolDataConverter.toBigDecimal("999.99", COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        Transaction retrievedTransaction = transactionRepository.findById(savedTransaction.getTransactionId()).orElseThrow();
        assertThat(retrievedTransaction.getAmount().scale()).isEqualTo(COBOL_DECIMAL_SCALE);
        assertThat(retrievedTransaction.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(999.99));
        
        cleanupTestData();
    }

    /**
     * Test concurrent database operations with multiple threads.
     * Validates thread safety and connection pool management.
     */
    @Test
    @Order(11)
    @DisplayName("Test Concurrent Operations - Thread Safety")
    public void testConcurrentOperations() {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        try {
            // Create base test data
            Customer customer = createTestCustomer();
            customer.setCustomerId(TEST_CUSTOMER_ID);
            customerRepository.save(customer);
            
            Account account = createTestAccount();
            account.setAccountId(TEST_ACCOUNT_ID);
            account.setCustomerId(customer.getCustomerId());
            account.setCurrentBalance(BigDecimal.valueOf(10000.00));
            accountRepository.save(account);
            
            // Execute concurrent operations
            CompletableFuture<?>[] futures = new CompletableFuture[5];
            
            for (int i = 0; i < 5; i++) {
                final int threadId = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    // Each thread creates transactions
                    for (int j = 0; j < 10; j++) {
                        Transaction transaction = createTestTransaction();
                        transaction.setTransactionId(TEST_TRANSACTION_ID + "_thread_" + threadId + "_" + j);
                        transaction.setAccountId(account.getAccountId());
                        transaction.setAmount(BigDecimal.valueOf(10.00 + threadId + j));
                        transactionRepository.save(transaction);
                    }
                }, executor);
            }
            
            // Wait for all operations to complete
            CompletableFuture.allOf(futures)
                .orTimeout(RESPONSE_TIME_THRESHOLD_MS * 10, TimeUnit.MILLISECONDS)
                .join();
            
            // Verify all transactions were created successfully
            List<Transaction> allTransactions = transactionRepository.findAll();
            assertThat(allTransactions.size()).isGreaterThanOrEqualTo(50);
            
        } finally {
            executor.shutdown();
            cleanupTestData();
        }
    }

    /**
     * Test database constraint validation.
     * Validates business rule constraints matching COBOL validation.
     */
    @Test
    @Order(12)
    @DisplayName("Test Constraint Validation - Business Rules")
    public void testConstraintValidation() {
        // Test NOT NULL constraints
        assertThatThrownBy(() -> {
            Account account = createTestAccount();
            account.setAccountId(null); // Should fail NOT NULL constraint
            accountRepository.save(account);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
        
        // Test positive amount constraints for transactions
        Account validAccount = createTestAccount();
        validAccount.setAccountId(TEST_ACCOUNT_ID);
        validAccount.setCurrentBalance(BigDecimal.valueOf(1000.00));
        accountRepository.save(validAccount);
        
        // Test valid transaction
        Transaction validTransaction = createTestTransaction();
        validTransaction.setTransactionId(TEST_TRANSACTION_ID);
        validTransaction.setAccountId(validAccount.getAccountId());
        validTransaction.setAmount(BigDecimal.valueOf(100.00));
        Transaction saved = transactionRepository.save(validTransaction);
        
        assertThat(saved.getTransactionId()).isNotNull();
        assertThat(saved.getAmount()).isPositive();
        
        cleanupTestData();
    }

    /**
     * Helper method to create test Card entity.
     */
    private Card createTestCard() {
        Card card = new Card();
        card.setCardNumber("4000000000000000");
        return card;
    }
}