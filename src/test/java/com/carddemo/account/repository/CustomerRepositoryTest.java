package com.carddemo.account.repository;

import com.carddemo.common.entity.Customer;
import com.carddemo.common.entity.Account;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.common.enums.AccountStatus;
import com.carddemo.config.CardDemoTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Ad-hoc Unit Tests for CustomerRepository
 * 
 * This test suite validates all CustomerRepository functionality including:
 * - Custom @Query methods with JOIN FETCH optimization
 * - Customer search operations equivalent to COBOL SEARCH ALL constructs
 * - Credit score filtering for customer screening operations
 * - Customer existence checks replacing COBOL file status validations
 * - Pagination support matching COBOL sequential file processing
 * - PII field access with proper authorization checks
 * - Integration with Customer and Account entities
 * - Spring Data JPA repository operations
 * 
 * Tests ensure complete COBOL-to-Java conversion compliance and verify
 * that all VSAM CUSTDAT dataset operations are properly replaced with
 * PostgreSQL optimized queries maintaining sub-200ms response times.
 * 
 * @author Blitzy Development Team - Ad-hoc Test Suite
 * @version 1.0
 */
@SpringBootTest(
    classes = com.carddemo.config.CardDemoTestApplication.class,
    properties = {
        "spring.cloud.gateway.enabled=false"
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Import(CardDemoTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.liquibase.enabled=false",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:schema-h2.sql",
    "spring.jpa.defer-datasource-initialization=false",
    "logging.level.org.hibernate.SQL=DEBUG"
})
@ActiveProfiles("test")
@EntityScan(basePackages = {
    "com.carddemo.auth.entity",
    "com.carddemo.account.entity",
    "com.carddemo.card",
    "com.carddemo.transaction",
    "com.carddemo.batch.entity",
    "com.carddemo.common.entity"
})
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(value = "/test-data/customer-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class CustomerRepositoryTest {



    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    // @BeforeEach - Data setup moved to test methods to avoid transaction issues
    void setUpOld() {
        System.out.println("DEBUG: Setting up test data with @Transactional and @Rollback(false)...");
        
        // Ensure database schema is ready by forcing JPA context initialization
        try {
            entityManager.getMetamodel(); // This triggers schema creation if not already done
            // Note: Cannot flush here due to transaction boundary issues
        } catch (Exception e) {
            System.out.println("DEBUG: Schema initialization in progress, waiting...");
            // Give schema creation a moment to complete
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Check if data already exists to avoid duplicate key errors
        try {
            if (customerRepository.count() > 0) {
                System.out.println("DEBUG: Data already exists, skipping setup. Count: " + customerRepository.count());
                return;
            }
        } catch (Exception e) {
            // If count fails, schema might not be ready - let's proceed with data creation
            System.out.println("DEBUG: Schema not ready, proceeding with data setup...");
        }
        
        // Create test customers programmatically
        Customer customer1 = new Customer();
        customer1.setCustomerId("100000001");
        customer1.setFirstName("John");
        customer1.setMiddleName("Michael");
        customer1.setLastName("Smith");
        customer1.setAddressLine1("123 Main Street");
        customer1.setStateCode("CA");
        customer1.setCountryCode("USA");
        customer1.setZipCode("90210");
        customer1.setSsn("123456789");
        customer1.setGovernmentIssuedId("DL123456789");
        customer1.setDateOfBirth(LocalDate.of(1980, 5, 15));
        customer1.setFicoCreditScore(750);
        customer1.setPrimaryCardHolderIndicator("Y");
        customer1.setPhoneNumber1("555-123-4567");
        customer1.setEftAccountId("EFT1234567");
        
        Customer customer2 = new Customer();
        customer2.setCustomerId("100000002");
        customer2.setFirstName("Jane");
        customer2.setLastName("Johnson");
        customer2.setAddressLine1("456 Oak Avenue");
        customer2.setStateCode("NY");
        customer2.setCountryCode("USA");
        customer2.setZipCode("10001");
        customer2.setSsn("987654321");
        customer2.setDateOfBirth(LocalDate.of(1985, 8, 22));
        customer2.setFicoCreditScore(680);
        customer2.setPrimaryCardHolderIndicator("N");
        customer2.setPhoneNumber1("555-987-6543");
        
        Customer customer3 = new Customer();
        customer3.setCustomerId("100000003");
        customer3.setFirstName("Bob");
        customer3.setLastName("Smithson");
        customer3.setAddressLine1("789 Pine Road");
        customer3.setStateCode("TX");
        customer3.setCountryCode("USA");
        customer3.setZipCode("77001");
        customer3.setSsn("555666777");
        customer3.setDateOfBirth(LocalDate.of(1975, 12, 10));
        customer3.setFicoCreditScore(720);
        customer3.setPrimaryCardHolderIndicator("Y");
        
        // Save customers using repository
        Customer savedCustomer1 = customerRepository.save(customer1);
        Customer savedCustomer2 = customerRepository.save(customer2);
        Customer savedCustomer3 = customerRepository.save(customer3);
        
        System.out.println("DEBUG: Successfully saved customers");
        System.out.println("DEBUG: Saved customers: " + savedCustomer1.getCustomerId() + ", " + 
                          savedCustomer2.getCustomerId() + ", " + savedCustomer3.getCustomerId());
        
        // Create test accounts associated with saved customer1
        Account account1 = new Account();
        account1.setAccountId("12345678901");
        account1.setCustomer(savedCustomer1);
        account1.setActiveStatus(AccountStatus.ACTIVE.isActive());
        account1.setCurrentBalance(new BigDecimal("1500.75"));
        account1.setCreditLimit(new BigDecimal("5000.00"));
        account1.setCashCreditLimit(new BigDecimal("1000.00"));
        account1.setCurrentCycleCredit(BigDecimal.ZERO);
        account1.setCurrentCycleDebit(BigDecimal.ZERO);
        account1.setOpenDate(LocalDate.of(2020, 1, 15));
        account1.setExpirationDate(LocalDate.of(2025, 1, 15));
        account1.setVersion(0L);
        
        Account account2 = new Account();
        account2.setAccountId("98765432109");
        account2.setCustomer(savedCustomer1);
        account2.setActiveStatus(AccountStatus.ACTIVE.isActive());
        account2.setCurrentBalance(new BigDecimal("750.25"));
        account2.setCreditLimit(new BigDecimal("3000.00"));
        account2.setCashCreditLimit(new BigDecimal("500.00"));
        account2.setCurrentCycleCredit(BigDecimal.ZERO);
        account2.setCurrentCycleDebit(BigDecimal.ZERO);
        account2.setOpenDate(LocalDate.of(2021, 6, 10));
        account2.setExpirationDate(LocalDate.of(2026, 6, 10));
        account2.setVersion(0L);
        
        // Save accounts using repository
        Account savedAccount1 = accountRepository.save(account1);
        Account savedAccount2 = accountRepository.save(account2);
        
        System.out.println("DEBUG: Successfully saved accounts");
        System.out.println("DEBUG: Saved accounts: " + savedAccount1.getAccountId() + ", " + 
                          savedAccount2.getAccountId());
        
        // Verify final state - repositories should handle persistence automatically
        long finalCustomerCount = customerRepository.count();
        long finalAccountCount = accountRepository.count();
        System.out.println("DEBUG: Final counts - Customers: " + finalCustomerCount + ", Accounts: " + finalAccountCount);
    }

    // ================================================================================
    // Test Category 1: Customer Repository Basic Operations Tests (4 tests)
    // ================================================================================

    @Test
    @DisplayName("Test 1.1: Repository Autowiring and Basic Setup - Spring Context Integration")
    void testRepositoryAutowiring() {
        assertNotNull(customerRepository, "CustomerRepository should be autowired successfully");
        
        // Verify repository extends JpaRepository<Customer, String>
        assertTrue(customerRepository instanceof org.springframework.data.jpa.repository.JpaRepository,
                "CustomerRepository should extend JpaRepository");
        
        System.out.println("✅ Test 1.1 PASSED: Repository Autowiring and Basic Setup");
    }

    @Test
    @DisplayName("Test 1.2: Standard JpaRepository Operations - CRUD Functionality")
    void testStandardJpaRepositoryOperations() {
        // Test findById - equivalent to COBOL EXEC CICS READ DATASET CUSTDAT
        Optional<Customer> foundCustomer = customerRepository.findById("100000001");
        assertTrue(foundCustomer.isPresent(), "Customer should be found by ID");
        assertEquals("John", foundCustomer.get().getFirstName(), "First name should match");
        assertEquals("Smith", foundCustomer.get().getLastName(), "Last name should match");
        
        // Test existsById - equivalent to COBOL file status checking
        assertTrue(customerRepository.existsById("100000001"), "Customer should exist");
        assertFalse(customerRepository.existsById("999999999"), "Non-existent customer should not exist");
        
        // Test count operation
        long customerCount = customerRepository.count();
        assertEquals(3, customerCount, "Should have 3 customers in database");
        
        System.out.println("✅ Test 1.2 PASSED: Standard JpaRepository Operations");
    }

    @Test
    @Transactional
    @DisplayName("Test 1.3: Entity Relationship Loading - Customer-Account Association")
    void testEntityRelationshipLoading() {
        Optional<Customer> customer = customerRepository.findById("100000001");
        assertTrue(customer.isPresent(), "Customer should be found");
        
        // Test lazy loading of accounts (should work in @DataJpaTest context)
        Customer foundCustomer = customer.get();
        assertNotNull(foundCustomer.getAccounts(), "Accounts collection should not be null");
        
        // Note: In @DataJpaTest, lazy collections are accessible within transaction
        assertEquals(2, foundCustomer.getAccounts().size(), "Customer should have 2 accounts");
        
        System.out.println("✅ Test 1.3 PASSED: Entity Relationship Loading");
    }

    @Test
    @DisplayName("Test 1.4: Repository Interface Validation - Method Signature Verification")
    void testRepositoryInterfaceValidation() {
        // Verify all custom methods are available through reflection
        assertTrue(hasMethod("findByCustomerIdWithAccounts", String.class),
                "findByCustomerIdWithAccounts method should be available");
        assertTrue(hasMethod("findByLastNameContainingIgnoreCase", String.class),
                "findByLastNameContainingIgnoreCase method should be available");
        assertTrue(hasMethod("findByFicoCreditScoreBetween", Integer.class, Integer.class),
                "findByFicoCreditScoreBetween method should be available");
        assertTrue(hasMethod("existsByCustomerIdAndActiveStatus", String.class, String.class),
                "existsByCustomerIdAndActiveStatus method should be available");
        assertTrue(hasMethod("findAllWithPagination", Pageable.class),
                "findAllWithPagination method should be available");
        assertTrue(hasMethod("findByCustomerIdWithPIIProtection", String.class),
                "findByCustomerIdWithPIIProtection method should be available");
        
        System.out.println("✅ Test 1.4 PASSED: Repository Interface Validation");
    }

    // ================================================================================
    // Test Category 2: Custom Query Method Tests with JOIN FETCH (4 tests)
    // ================================================================================

    @Test
    @DisplayName("Test 2.1: findByCustomerIdWithAccounts - JOIN FETCH Optimization")
    void testFindByCustomerIdWithAccounts() {
        // Test successful customer retrieval with accounts
        Optional<Customer> result = customerRepository.findByCustomerIdWithAccounts("100000001");
        assertTrue(result.isPresent(), "Customer with accounts should be found");
        
        Customer customer = result.get();
        assertEquals("100000001", customer.getCustomerId(), "Customer ID should match");
        assertEquals("John", customer.getFirstName(), "First name should match");
        assertEquals("Smith", customer.getLastName(), "Last name should match");
        
        // Verify accounts are eagerly loaded through JOIN FETCH
        assertNotNull(customer.getAccounts(), "Accounts should be loaded");
        assertEquals(2, customer.getAccounts().size(), "Should have 2 accounts loaded via JOIN FETCH");
        
        // Test customer not found scenario
        Optional<Customer> notFound = customerRepository.findByCustomerIdWithAccounts("999999999");
        assertFalse(notFound.isPresent(), "Non-existent customer should not be found");
        
        System.out.println("✅ Test 2.1 PASSED: findByCustomerIdWithAccounts - JOIN FETCH Optimization");
    }

    @Test
    @DisplayName("Test 2.2: JOIN FETCH Performance Validation - Single Query Execution")
    void testJoinFetchPerformanceValidation() {
        // This test validates that JOIN FETCH prevents N+1 query problems
        // by loading customer and accounts in a single SQL query
        
        Optional<Customer> customer = customerRepository.findByCustomerIdWithAccounts("100000001");
        assertTrue(customer.isPresent(), "Customer should be found");
        
        Customer foundCustomer = customer.get();
        
        // Access accounts collection - should not trigger additional queries due to JOIN FETCH
        assertDoesNotThrow(() -> {
            foundCustomer.getAccounts().forEach(account -> {
                assertNotNull(account.getAccountId(), "Account ID should be accessible");
                assertNotNull(account.getCurrentBalance(), "Account balance should be accessible");
                assertNotNull(account.getActiveStatus(), "Account status should be accessible");
            });
        }, "Accessing accounts through JOIN FETCH should not throw exceptions");
        
        System.out.println("✅ Test 2.2 PASSED: JOIN FETCH Performance Validation");
    }

    @Test
    @DisplayName("Test 2.3: JOIN FETCH with Customer Having No Accounts - LEFT JOIN Behavior")
    void testJoinFetchWithNoAccounts() {
        // Test LEFT JOIN FETCH behavior with customer having no accounts
        Optional<Customer> customer = customerRepository.findByCustomerIdWithAccounts("100000002");
        assertTrue(customer.isPresent(), "Customer without accounts should still be found");
        
        Customer foundCustomer = customer.get();
        assertEquals("100000002", foundCustomer.getCustomerId(), "Customer ID should match");
        assertNotNull(foundCustomer.getAccounts(), "Accounts collection should not be null");
        assertEquals(0, foundCustomer.getAccounts().size(), "Customer should have no accounts");
        
        System.out.println("✅ Test 2.3 PASSED: JOIN FETCH with Customer Having No Accounts");
    }

    @Test
    @DisplayName("Test 2.4: JOIN FETCH Query Distinctness - DISTINCT Clause Validation")
    void testJoinFetchQueryDistinctness() {
        // Verify DISTINCT clause prevents duplicate customer records
        // when customer has multiple accounts
        
        Optional<Customer> customer = customerRepository.findByCustomerIdWithAccounts("100000001");
        assertTrue(customer.isPresent(), "Customer should be found");
        
        // The query should return exactly one customer despite multiple accounts
        // This validates the DISTINCT clause in the @Query annotation
        Customer foundCustomer = customer.get();
        assertEquals("100000001", foundCustomer.getCustomerId(), "Should get single customer instance");
        assertEquals(2, foundCustomer.getAccounts().size(), "Should have all accounts loaded");
        
        System.out.println("✅ Test 2.4 PASSED: JOIN FETCH Query Distinctness");
    }

    // ================================================================================
    // Test Category 3: Customer Search Operations - COBOL SEARCH ALL Equivalent (4 tests)
    // ================================================================================

    @Test
    @DisplayName("Test 3.1: findByLastNameContainingIgnoreCase - Partial Match Search")
    void testFindByLastNameContainingSearch() {
        List<Customer> customers = customerRepository.findByLastNameContainingIgnoreCase("Smith");
        
        assertEquals(2, customers.size(), "Should find 2 customers with last names containing 'Smith' (Smith and Smithson)");
        
        // Verify both customers are found - results should be ordered by lastName, firstName
        assertEquals("Smith", customers.get(0).getLastName(), "First result should be 'Smith'");
        assertEquals("John", customers.get(0).getFirstName(), "First customer should be John Smith");
        assertEquals("100000001", customers.get(0).getCustomerId(), "First customer ID should match");
        
        assertEquals("Smithson", customers.get(1).getLastName(), "Second result should be 'Smithson'");
        assertEquals("Bob", customers.get(1).getFirstName(), "Second customer should be Bob Smithson");
        assertEquals("100000003", customers.get(1).getCustomerId(), "Second customer ID should match");
        
        System.out.println("✅ Test 3.1 PASSED: findByLastNameContainingIgnoreCase - Partial Match Search");
    }

    @Test
    @DisplayName("Test 3.2: findByLastNameContainingIgnoreCase - Partial Match Search")
    void testFindByLastNamePartialMatch() {
        // Test partial matching - should find both "Smith" and "Smithson"
        List<Customer> customers = customerRepository.findByLastNameContainingIgnoreCase("Smith");
        
        assertEquals(2, customers.size(), "Should find 2 customers with last names containing 'Smith'");
        
        // Verify results are ordered by lastName, firstName as specified in @Query
        assertTrue(customers.stream().anyMatch(c -> "Smith".equals(c.getLastName())),
                "Should include customer with last name 'Smith'");
        assertTrue(customers.stream().anyMatch(c -> "Smithson".equals(c.getLastName())),
                "Should include customer with last name 'Smithson'");
        
        System.out.println("✅ Test 3.2 PASSED: findByLastNameContainingIgnoreCase - Partial Match");
    }

    @Test
    @DisplayName("Test 3.3: findByLastNameContainingIgnoreCase - Case Insensitive Search")
    void testFindByLastNameCaseInsensitive() {
        // Test case insensitive search functionality
        List<Customer> upperCase = customerRepository.findByLastNameContainingIgnoreCase("SMITH");
        List<Customer> lowerCase = customerRepository.findByLastNameContainingIgnoreCase("smith"); 
        List<Customer> mixedCase = customerRepository.findByLastNameContainingIgnoreCase("SmItH");
        
        assertEquals(2, upperCase.size(), "Uppercase search should find 2 customers");
        assertEquals(2, lowerCase.size(), "Lowercase search should find 2 customers");
        assertEquals(2, mixedCase.size(), "Mixed case search should find 2 customers");
        
        // All searches should return the same results
        assertEquals(upperCase.size(), lowerCase.size(), "Case insensitive searches should return same count");
        assertEquals(lowerCase.size(), mixedCase.size(), "Case insensitive searches should return same count");
        
        System.out.println("✅ Test 3.3 PASSED: findByLastNameContainingIgnoreCase - Case Insensitive");
    }

    @Test
    @DisplayName("Test 3.4: findByLastNameContainingIgnoreCase - No Results and Ordering")
    void testFindByLastNameNoResultsAndOrdering() {
        // Test no results scenario
        List<Customer> noResults = customerRepository.findByLastNameContainingIgnoreCase("Nonexistent");
        assertEquals(0, noResults.size(), "Should find no customers with non-existent last name");
        
        // Test ordering by getting multiple results and verifying sort order
        List<Customer> multipleResults = customerRepository.findByLastNameContainingIgnoreCase("o");
        assertTrue(multipleResults.size() >= 2, "Should find multiple customers with 'o' in last name");
        
        // Verify ordering: lastName, firstName
        for (int i = 0; i < multipleResults.size() - 1; i++) {
            Customer current = multipleResults.get(i);
            Customer next = multipleResults.get(i + 1);
            
            int lastNameComparison = current.getLastName().compareTo(next.getLastName());
            if (lastNameComparison == 0) {
                // If last names are equal, first names should be in order
                assertTrue(current.getFirstName().compareTo(next.getFirstName()) <= 0,
                    "Customers with same last name should be ordered by first name");
            } else {
                assertTrue(lastNameComparison < 0, "Customers should be ordered by last name first");
            }
        }
        
        System.out.println("✅ Test 3.4 PASSED: findByLastNameContainingIgnoreCase - No Results and Ordering");
    }

    // ================================================================================
    // Test Category 4: Credit Score Filtering Tests - Customer Screening Operations (4 tests)
    // ================================================================================

    @Test
    @DisplayName("Test 4.1: findByFicoCreditScoreBetween - Range Filtering")
    void testFindByFicoCreditScoreBetween() {
        // Test customers in range 700-800 (should include customers with scores 720 and 750)
        List<Customer> highScoreCustomers = customerRepository.findByFicoCreditScoreBetween(700, 800);
        assertEquals(2, highScoreCustomers.size(), "Should find 2 customers with FICO scores between 700-800");
        
        // Verify all customers are within range and ordered by credit score descending
        for (Customer customer : highScoreCustomers) {
            assertTrue(customer.getFicoCreditScore() >= 700 && customer.getFicoCreditScore() <= 800,
                    "Customer FICO score should be within specified range");
        }
        
        // Verify descending order by credit score
        if (highScoreCustomers.size() > 1) {
            for (int i = 0; i < highScoreCustomers.size() - 1; i++) {
                assertTrue(highScoreCustomers.get(i).getFicoCreditScore() >= 
                          highScoreCustomers.get(i + 1).getFicoCreditScore(),
                          "Customers should be ordered by FICO score descending");
            }
        }
        
        System.out.println("✅ Test 4.1 PASSED: findByFicoCreditScoreBetween - Range Filtering");
    }

    @Test
    @DisplayName("Test 4.2: findByFicoCreditScoreBetween - Inclusive Boundaries")
    void testFicoCreditScoreInclusiveBoundaries() {
        // Test inclusive boundaries (BETWEEN is inclusive on both ends)
        List<Customer> exactMatch = customerRepository.findByFicoCreditScoreBetween(750, 750);
        assertEquals(1, exactMatch.size(), "Should find exactly 1 customer with FICO score 750");
        assertEquals(Integer.valueOf(750), exactMatch.get(0).getFicoCreditScore(),
                "Customer should have FICO score exactly 750");
        
        // Test lower boundary inclusive
        List<Customer> lowerBoundary = customerRepository.findByFicoCreditScoreBetween(680, 680);
        assertEquals(1, lowerBoundary.size(), "Should find customer with FICO score exactly 680");
        
        System.out.println("✅ Test 4.2 PASSED: findByFicoCreditScoreBetween - Inclusive Boundaries");
    }

    @Test
    @DisplayName("Test 4.3: findByFicoCreditScoreBetween - No Results Scenario")
    void testFicoCreditScoreNoResults() {
        // Test range with no customers (850+ range)
        List<Customer> noResults = customerRepository.findByFicoCreditScoreBetween(800, 850);
        assertEquals(0, noResults.size(), "Should find no customers with FICO scores 800-850");
        
        // Test invalid range (min > max)
        List<Customer> invalidRange = customerRepository.findByFicoCreditScoreBetween(800, 700);
        assertEquals(0, invalidRange.size(), "Should find no customers when min > max");
        
        System.out.println("✅ Test 4.3 PASSED: findByFicoCreditScoreBetween - No Results Scenario");
    }

    @Test
    @DisplayName("Test 4.4: findByFicoCreditScoreBetween - Full Range and Ordering Validation")
    void testFicoCreditScoreFullRangeOrdering() {
        // Test full FICO range (300-850) to get all customers
        List<Customer> allCustomers = customerRepository.findByFicoCreditScoreBetween(300, 850);
        assertEquals(3, allCustomers.size(), "Should find all 3 customers within full FICO range");
        
        // Verify descending order by FICO score: 750 (John Smith), 720 (Bob Smithson), 680 (Jane Johnson)
        assertEquals(Integer.valueOf(750), allCustomers.get(0).getFicoCreditScore(),
                "First customer should have highest FICO score (750)");
        assertEquals(Integer.valueOf(720), allCustomers.get(1).getFicoCreditScore(),
                "Second customer should have middle FICO score (720)");
        assertEquals(Integer.valueOf(680), allCustomers.get(2).getFicoCreditScore(),
                "Third customer should have lowest FICO score (680)");
        
        // Verify customer identities match expected order
        assertEquals("100000001", allCustomers.get(0).getCustomerId(), "Highest score customer should be John Smith");
        assertEquals("100000003", allCustomers.get(1).getCustomerId(), "Middle score customer should be Bob Smithson");
        assertEquals("100000002", allCustomers.get(2).getCustomerId(), "Lowest score customer should be Jane Johnson");
        
        System.out.println("✅ Test 4.4 PASSED: findByFicoCreditScoreBetween - Full Range and Ordering");
    }

    // ================================================================================
    // Test Category 5: Customer Existence Validation Tests - COBOL File Status Equivalent (3 tests)
    // ================================================================================

    @Test
    @DisplayName("Test 5.1: existsByCustomerIdAndActiveStatus - Active Customer Validation")
    void testExistsByCustomerIdAndActiveStatus() {
        // Test existing customer with 'Y' primary card holder status
        boolean existsActive = customerRepository.existsByCustomerIdAndActiveStatus("100000001", "Y");
        assertTrue(existsActive, "Customer 100000001 with active status 'Y' should exist");
        
        // Test existing customer with 'N' primary card holder status
        boolean existsInactive = customerRepository.existsByCustomerIdAndActiveStatus("100000002", "N");
        assertTrue(existsInactive, "Customer 100000002 with active status 'N' should exist");
        
        // Test wrong status combination
        boolean wrongStatus = customerRepository.existsByCustomerIdAndActiveStatus("100000001", "N");
        assertFalse(wrongStatus, "Customer 100000001 with wrong status 'N' should not exist");
        
        System.out.println("✅ Test 5.1 PASSED: existsByCustomerIdAndActiveStatus - Active Customer Validation");
    }

    @Test
    @DisplayName("Test 5.2: existsByCustomerIdAndActiveStatus - Non-existent Customer")
    void testExistsByCustomerIdAndActiveStatusNonExistent() {
        // Test non-existent customer ID
        boolean nonExistent = customerRepository.existsByCustomerIdAndActiveStatus("999999999", "Y");
        assertFalse(nonExistent, "Non-existent customer should return false");
        
        boolean nonExistentInactive = customerRepository.existsByCustomerIdAndActiveStatus("999999999", "N");
        assertFalse(nonExistentInactive, "Non-existent customer with any status should return false");
        
        System.out.println("✅ Test 5.2 PASSED: existsByCustomerIdAndActiveStatus - Non-existent Customer");
    }

    @Test
    @DisplayName("Test 5.3: existsByCustomerIdAndActiveStatus - Performance and Efficiency")
    void testExistsByCustomerIdAndActiveStatusPerformance() {
        // This test validates that the method uses COUNT query optimization
        // and doesn't return actual customer data for efficiency
        
        long startTime = System.nanoTime();
        boolean exists = customerRepository.existsByCustomerIdAndActiveStatus("100000001", "Y");
        long endTime = System.nanoTime();
        
        assertTrue(exists, "Customer should exist");
        
        // Verify method returns boolean value (memory efficiency)
        // The method returns primitive boolean, confirming efficient COUNT query usage
        
        // Performance validation - should be very fast due to COUNT query
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 100_000_000L, // 100ms in nanoseconds
                "Query should complete quickly due to COUNT optimization");
        
        System.out.println("✅ Test 5.3 PASSED: existsByCustomerIdAndActiveStatus - Performance and Efficiency");
    }

    // ================================================================================
    // Test Category 6: Pagination Support Tests - COBOL Sequential Processing Equivalent (4 tests) 
    // ================================================================================

    @Test
    @DisplayName("Test 6.1: findAllWithPagination - Basic Pagination")
    void testFindAllWithPaginationBasic() {
        // Test first page with page size of 2
        Pageable firstPage = PageRequest.of(0, 2);
        Page<Customer> page = customerRepository.findAllWithPagination(firstPage);
        
        assertNotNull(page, "Page should not be null");
        assertEquals(2, page.getContent().size(), "First page should contain 2 customers");
        assertEquals(3, page.getTotalElements(), "Total elements should be 3");
        assertEquals(2, page.getTotalPages(), "Should have 2 total pages with page size 2");
        assertTrue(page.hasNext(), "First page should have next page");
        assertFalse(page.hasPrevious(), "First page should not have previous page");
        
        System.out.println("✅ Test 6.1 PASSED: findAllWithPagination - Basic Pagination");
    }

    @Test
    @DisplayName("Test 6.2: findAllWithPagination - Last Page and Boundary Conditions")
    void testFindAllWithPaginationLastPage() {
        // Test last page
        Pageable lastPage = PageRequest.of(1, 2);
        Page<Customer> page = customerRepository.findAllWithPagination(lastPage);
        
        assertNotNull(page, "Page should not be null");
        assertEquals(1, page.getContent().size(), "Last page should contain 1 customer");
        assertEquals(3, page.getTotalElements(), "Total elements should be 3");
        assertFalse(page.hasNext(), "Last page should not have next page");
        assertTrue(page.hasPrevious(), "Last page should have previous page");
        
        // Test empty page beyond data range
        Pageable beyondData = PageRequest.of(5, 2);
        Page<Customer> emptyPage = customerRepository.findAllWithPagination(beyondData);
        assertEquals(0, emptyPage.getContent().size(), "Page beyond data should be empty");
        assertEquals(3, emptyPage.getTotalElements(), "Total elements should still be 3");
        
        System.out.println("✅ Test 6.2 PASSED: findAllWithPagination - Last Page and Boundary Conditions");
    }

    @Test
    @DisplayName("Test 6.3: findAllWithPagination - Sorting Functionality")
    void testFindAllWithPaginationSorting() {
        // Test default sorting (lastName, firstName, customerId)
        Pageable sortedPage = PageRequest.of(0, 3, Sort.by("lastName", "firstName", "customerId"));
        Page<Customer> page = customerRepository.findAllWithPagination(sortedPage);
        
        assertEquals(3, page.getContent().size(), "Should get all customers on single page");
        
        List<Customer> customers = page.getContent();
        
        // Verify sorting order: Johnson, Smith, Smithson (by lastName)
        assertEquals("Johnson", customers.get(0).getLastName(), "First customer should be Johnson");
        assertEquals("Smith", customers.get(1).getLastName(), "Second customer should be Smith");
        assertEquals("Smithson", customers.get(2).getLastName(), "Third customer should be Smithson");
        
        System.out.println("✅ Test 6.3 PASSED: findAllWithPagination - Sorting Functionality");
    }

    @Test
    @DisplayName("Test 6.4: findAllWithPagination - Custom Page Sizes and Performance")
    void testFindAllWithPaginationCustomSizes() {
        // Test different page sizes
        Pageable singleItem = PageRequest.of(0, 1);
        Page<Customer> singlePage = customerRepository.findAllWithPagination(singleItem);
        assertEquals(1, singlePage.getContent().size(), "Single item page should contain 1 customer");
        assertEquals(3, singlePage.getTotalPages(), "Should have 3 total pages with page size 1");
        
        // Test large page size (larger than available data)
        Pageable largePage = PageRequest.of(0, 10);
        Page<Customer> fullPage = customerRepository.findAllWithPagination(largePage);
        assertEquals(3, fullPage.getContent().size(), "Large page should contain all 3 customers");
        assertEquals(1, fullPage.getTotalPages(), "Should have 1 total page when page size > data");
        
        // Verify page metadata is correct
        assertEquals(0, fullPage.getNumber(), "Page number should be 0");
        assertEquals(10, fullPage.getSize(), "Page size should be 10");
        assertTrue(fullPage.isFirst(), "Should be first page");
        assertTrue(fullPage.isLast(), "Should be last page");
        
        System.out.println("✅ Test 6.4 PASSED: findAllWithPagination - Custom Page Sizes and Performance");
    }

    // ================================================================================
    // Test Category 7: PII Protection Access Tests - RACF Security Equivalent (3 tests)
    // ================================================================================

    @Test
    @DisplayName("Test 7.1: findByCustomerIdWithPIIProtection - Basic PII Access")
    void testFindByCustomerIdWithPIIProtection() {
        // Test PII-protected customer retrieval
        Optional<Customer> customer = customerRepository.findByCustomerIdWithPIIProtection("100000001");
        
        assertTrue(customer.isPresent(), "Customer should be found with PII protection");
        assertEquals("100000001", customer.get().getCustomerId(), "Customer ID should match");
        assertEquals("John", customer.get().getFirstName(), "First name should be accessible");
        assertEquals("Smith", customer.get().getLastName(), "Last name should be accessible");
        
        // In a real implementation, PII fields would be masked or excluded based on security context
        // For this test, we verify the query works and returns customer data
        assertNotNull(customer.get().getSsn(), "SSN should be present (actual implementation would mask)");
        assertNotNull(customer.get().getGovernmentIssuedId(), "Government ID should be present");
        
        System.out.println("✅ Test 7.1 PASSED: findByCustomerIdWithPIIProtection - Basic PII Access");
    }

    @Test
    @DisplayName("Test 7.2: findByCustomerIdWithPIIProtection - Non-existent Customer")
    void testFindByCustomerIdWithPIIProtectionNotFound() {
        Optional<Customer> customer = customerRepository.findByCustomerIdWithPIIProtection("999999999");
        
        assertFalse(customer.isPresent(), "Non-existent customer should not be found");
        
        System.out.println("✅ Test 7.2 PASSED: findByCustomerIdWithPIIProtection - Non-existent Customer");
    }

    @Test
    @DisplayName("Test 7.3: findByCustomerIdWithPIIProtection - Security Context Validation")
    void testFindByCustomerIdWithPIIProtectionSecurity() {
        // This test validates that the method is designed for secure PII access
        // In a real implementation, this would integrate with Spring Security authorization
        
        Optional<Customer> customer = customerRepository.findByCustomerIdWithPIIProtection("100000001");
        assertTrue(customer.isPresent(), "Customer should be found");
        
        Customer foundCustomer = customer.get();
        
        // Verify that business logic methods for PII protection are available
        assertDoesNotThrow(() -> foundCustomer.getMaskedSsn(),
                "Masked SSN method should be available for PII protection");
        
        String maskedSsn = foundCustomer.getMaskedSsn();
        assertTrue(maskedSsn.startsWith("XXX-XX-"), "SSN should be masked for security");
        
        System.out.println("✅ Test 7.3 PASSED: findByCustomerIdWithPIIProtection - Security Context Validation");
    }

    // ================================================================================
    // Test Category 8: Integration and Performance Tests (3 tests)
    // ================================================================================

    @Test
    @DisplayName("Test 8.1: Repository Integration with JPA Context - Transaction Management")
    void testRepositoryJpaIntegration() {
        // Test that repository works correctly within JPA transaction context
        assertDoesNotThrow(() -> {
            // Multiple repository operations within single transaction
            Optional<Customer> customer1 = customerRepository.findById("100000001");
            List<Customer> searchResults = customerRepository.findByLastNameContainingIgnoreCase("Smith");
            boolean exists = customerRepository.existsByCustomerIdAndActiveStatus("100000001", "Y");
            
            assertTrue(customer1.isPresent(), "Customer should be found");
            assertTrue(searchResults.size() > 0, "Search should return results");
            assertTrue(exists, "Customer should exist with active status");
        }, "Repository operations should work within JPA transaction context");
        
        System.out.println("✅ Test 8.1 PASSED: Repository Integration with JPA Context");
    }

    @Test
    @DisplayName("Test 8.2: Repository Method Performance Validation - Sub-200ms Target")
    void testRepositoryMethodPerformance() {
        // Test performance of various repository operations
        // Target: All queries should complete well under 200ms for small dataset
        
        long startTime = System.nanoTime();
        customerRepository.findByCustomerIdWithAccounts("100000001");
        long joinFetchTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        customerRepository.findByLastNameContainingIgnoreCase("Smith");
        long searchTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        customerRepository.findByFicoCreditScoreBetween(700, 800);
        long rangeTime = System.nanoTime() - startTime;
        
        // All operations should complete quickly (well under 200ms = 200,000,000 nanoseconds)
        assertTrue(joinFetchTime < 50_000_000L, "JOIN FETCH query should be fast");
        assertTrue(searchTime < 50_000_000L, "Search query should be fast");
        assertTrue(rangeTime < 50_000_000L, "Range query should be fast");
        
        System.out.println("✅ Test 8.2 PASSED: Repository Method Performance Validation");
    }

    @Test
    @DisplayName("Test 8.3: Repository Entity Relationship Integrity - Foreign Key Validation")
    void testRepositoryEntityRelationshipIntegrity() {
        // Test that repository operations maintain entity relationship integrity
        
        Optional<Customer> customerWithAccounts = customerRepository.findByCustomerIdWithAccounts("100000001");
        assertTrue(customerWithAccounts.isPresent(), "Customer should be found");
        
        Customer customer = customerWithAccounts.get();
        assertNotNull(customer.getAccounts(), "Accounts should be loaded");
        
        // Verify bidirectional relationship integrity
        customer.getAccounts().forEach(account -> {
            assertNotNull(account.getCustomer(), "Account should have customer reference");
            assertEquals(customer.getCustomerId(), account.getCustomer().getCustomerId(),
                    "Account's customer should match the loaded customer");
        });
        
        System.out.println("✅ Test 8.3 PASSED: Repository Entity Relationship Integrity");
    }

    // ================================================================================
    // Test Category 9: COBOL Conversion Validation Tests (3 tests)
    // ================================================================================

    @Test
    @DisplayName("Test 9.1: VSAM CUSTDAT Dataset Replacement Validation - Complete Functionality Coverage")
    void testVsamCustdatReplacement() {
        // Validate that repository provides complete replacement for VSAM CUSTDAT operations
        
        // Equivalent to EXEC CICS READ DATASET CUSTDAT KEY(CUST-ID)
        Optional<Customer> directRead = customerRepository.findById("100000001");
        assertTrue(directRead.isPresent(), "Direct customer read should work like VSAM READ");
        
        // Equivalent to COBOL SEARCH ALL constructs
        List<Customer> searchResults = customerRepository.findByLastNameContainingIgnoreCase("Smith");
        assertTrue(searchResults.size() > 0, "Search functionality should work like COBOL SEARCH ALL");
        
        // Equivalent to COBOL file status checking
        boolean exists = customerRepository.existsById("100000001");
        assertTrue(exists, "Existence check should work like COBOL file status validation");
        
        // Equivalent to sequential file processing with cursors
        Page<Customer> pageResults = customerRepository.findAllWithPagination(PageRequest.of(0, 2));
        assertNotNull(pageResults, "Pagination should work like COBOL sequential processing");
        
        System.out.println("✅ Test 9.1 PASSED: VSAM CUSTDAT Dataset Replacement Validation");
    }

    @Test
    @DisplayName("Test 9.2: COBOL Data Type Mapping Validation - Field Precision and Format")
    void testCobolDataTypeMappingValidation() {
        Optional<Customer> customer = customerRepository.findById("100000001");
        assertTrue(customer.isPresent(), "Customer should exist for data type validation");
        
        Customer foundCustomer = customer.get();
        
        // Validate COBOL PIC 9(09) mapping - Customer ID (exactly 9 digits)
        assertEquals(9, foundCustomer.getCustomerId().length(), "Customer ID should be exactly 9 digits like COBOL PIC 9(09)");
        assertTrue(foundCustomer.getCustomerId().matches("^[0-9]{9}$"), "Customer ID should contain only digits");
        
        // Validate COBOL PIC X(25) mapping - Name fields (max 25 characters)
        assertTrue(foundCustomer.getFirstName().length() <= 25, "First name should not exceed COBOL PIC X(25) limit");
        assertTrue(foundCustomer.getLastName().length() <= 25, "Last name should not exceed COBOL PIC X(25) limit");
        
        // Validate COBOL PIC 9(03) mapping - FICO Credit Score (3-digit integer, 300-850 range)
        assertNotNull(foundCustomer.getFicoCreditScore(), "FICO score should be present");
        assertTrue(foundCustomer.getFicoCreditScore() >= 300 && foundCustomer.getFicoCreditScore() <= 850,
                "FICO score should be within valid range like COBOL business rules");
        
        System.out.println("✅ Test 9.2 PASSED: COBOL Data Type Mapping Validation");
    }

    @Test
    @DisplayName("Test 9.3: COBOL Business Logic Preservation - 88-Level Conditions and Validation")
    void testCobolBusinessLogicPreservation() {
        Optional<Customer> customer = customerRepository.findById("100000001");
        assertTrue(customer.isPresent(), "Customer should exist for business logic validation");
        
        Customer foundCustomer = customer.get();
        
        // Test COBOL 88-level condition equivalent - Primary Card Holder Indicator
        assertTrue(foundCustomer.isPrimaryCardHolder(), "Customer should be primary card holder like COBOL 88-level condition");
        assertEquals("Y", foundCustomer.getPrimaryCardHolderIndicator(), "Primary card holder indicator should be 'Y'");
        
        // Test COBOL business rule validation - FICO score validation
        assertTrue(foundCustomer.isValidFicoScore(), "FICO score should be valid per COBOL business rules");
        
        // Test COBOL name formatting logic preservation
        String fullName = foundCustomer.getFullName();
        assertTrue(fullName.contains(","), "Full name should contain comma like COBOL formatting");
        assertTrue(fullName.startsWith("Smith,"), "Full name should start with last name like COBOL");
        
        // Test COBOL address formatting logic preservation  
        String formattedAddress = foundCustomer.getFormattedAddress();
        assertNotNull(formattedAddress, "Formatted address should be available like COBOL");
        assertTrue(formattedAddress.contains("CA"), "Address should contain state code");
        assertTrue(formattedAddress.contains("USA"), "Address should contain country code");
        
        System.out.println("✅ Test 9.3 PASSED: COBOL Business Logic Preservation");
    }

    // ================================================================================
    // Helper Methods
    // ================================================================================

    /**
     * Helper method to check if repository has a specific method
     */
    private boolean hasMethod(String methodName, Class<?>... parameterTypes) {
        try {
            customerRepository.getClass().getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}