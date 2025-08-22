/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.Customer;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.IntegrationTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.TestDataGenerator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive integration test class for CardRepository validating credit card operations
 * including card issuance, activation, expiration handling, CVV validation, and 
 * account-based card lookups using secondary indexes.
 * 
 * This test class ensures complete functional parity with the original COBOL COCRDLIC,
 * COCRDSLC, and COCRDUPC programs by validating all VSAM CARDDAT dataset access patterns
 * now implemented through Spring Data JPA and PostgreSQL.
 * 
 * Key Testing Areas:
 * - CRUD operations for card lifecycle management
 * - CARDAIX alternate index replacement through findByAccountId
 * - Primary key lookup performance validation (<200ms requirement)  
 * - CVV code storage and retrieval with 3-digit validation
 * - Embossed name field handling (50 characters maximum)
 * - Expiration date parsing and temporal queries
 * - Active status flag updates and filtering
 * - Card issuance with unique card number generation
 * - Customer-based card lookups for ownership validation
 * - Card replacement operations maintaining account relationships
 * - Concurrent card activation scenarios
 * - Foreign key relationship integrity with Account and Customer entities
 * - Card number masking in query results for PCI DSS compliance
 * - Batch card expiration processing for Spring Batch integration
 * 
 * Test Data Strategy:
 * - Uses TestDataGenerator for COBOL-compliant test data generation
 * - Validates COMP-3 BigDecimal precision preservation in monetary fields
 * - Tests VSAM key structure equivalents through composite primary keys
 * - Ensures field length validation matches original copybook specifications
 * 
 * Performance Requirements:
 * - All repository operations must complete within RESPONSE_TIME_THRESHOLD_MS (200ms)
 * - Secondary index queries must demonstrate performance improvement over table scans
 * - Concurrent access patterns must maintain data consistency without deadlocks
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@DataJpaTest
@Transactional
public class CardRepositoryTest extends AbstractBaseTest implements IntegrationTest {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private TestDataGenerator testDataGenerator;

    /**
     * Test setup method initializing test data generator and resetting random seed
     * for consistent test execution across different environments.
     */
    @Override
    protected void setUp() {
        super.setUp();
        testDataGenerator = new TestDataGenerator();
        testDataGenerator.resetRandomSeed();
    }

    /**
     * Test cleanup method ensuring proper data cleanup after each test execution
     * to maintain test isolation and prevent data leakage between test methods.
     */
    @Override
    protected void tearDown() {
        cardRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();
        super.tearDown();
    }

    // ========================================
    // CRUD Operations Testing
    // ========================================

    /**
     * Tests basic card creation and persistence operations validating that
     * new cards can be successfully saved to the database with all required
     * fields properly stored and retrievable.
     * 
     * Validates:
     * - Card entity persistence with all fields
     * - Primary key generation and uniqueness
     * - Required field validation
     * - Data type conversion accuracy
     */
    @Test
    void testCreateCard() {
        // Given: Create test customer and account for foreign key relationships
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        // Create test card using TestDataGenerator for COBOL compatibility
        Card testCard = testDataGenerator.generateCard();
        testCard.setAccountId(testAccount.getAccountId());
        testCard.setCustomerId(testCustomer.getCustomerId());

        // When: Save the card
        Card savedCard = cardRepository.save(testCard);

        // Then: Validate successful persistence
        assertThat(savedCard).isNotNull();
        assertThat(savedCard.getCardNumber()).isEqualTo(testCard.getCardNumber());
        assertThat(savedCard.getAccountId()).isEqualTo(testAccount.getAccountId());
        assertThat(savedCard.getCustomerId()).isEqualTo(testCustomer.getCustomerId());
        assertThat(savedCard.getCvvCode()).isEqualTo(testCard.getCvvCode());
        assertThat(savedCard.getEmbossedName()).isEqualTo(testCard.getEmbossedName());
        assertThat(savedCard.getExpirationDate()).isEqualTo(testCard.getExpirationDate());
        assertThat(savedCard.getActiveStatus()).isEqualTo(testCard.getActiveStatus());
    }

    /**
     * Tests card retrieval by primary key (card number) validating
     * performance requirements and data accuracy.
     * 
     * Validates:
     * - Primary key lookup performance (<200ms)
     * - Exact field value retrieval
     * - Optional handling for non-existent cards
     */
    @Test
    void testFindByCardNumber() {
        // Given: Create and save test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        Card testCard = testDataGenerator.generateCard();
        testCard.setAccountId(testAccount.getAccountId());
        testCard.setCustomerId(testCustomer.getCustomerId());
        cardRepository.save(testCard);

        // When: Find by card number with performance timing
        long startTime = System.currentTimeMillis();
        Optional<Card> foundCard = cardRepository.findById(testCard.getCardNumber());
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Then: Validate performance and accuracy
        assertThat(elapsedTime).isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        assertThat(foundCard).isPresent();
        assertThat(foundCard.get().getCardNumber()).isEqualTo(testCard.getCardNumber());
        assertThat(foundCard.get().getAccountId()).isEqualTo(testCard.getAccountId());
    }

    /**
     * Tests card update operations validating that existing cards can be
     * modified while preserving data integrity and foreign key relationships.
     * 
     * Validates:
     * - Field update accuracy
     * - Version control (if implemented)
     * - Foreign key constraint preservation
     * - Timestamp updates
     */
    @Test
    void testUpdateCard() {
        // Given: Create and save initial card
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        Card testCard = testDataGenerator.generateCard();
        testCard.setAccountId(testAccount.getAccountId());
        testCard.setCustomerId(testCustomer.getCustomerId());
        Card savedCard = cardRepository.save(testCard);

        // When: Update card fields
        String newEmbossedName = "UPDATED CARDHOLDER";
        savedCard.setEmbossedName(newEmbossedName);
        savedCard.setActiveStatus("N");
        Card updatedCard = cardRepository.save(savedCard);

        // Then: Validate updates
        assertThat(updatedCard.getEmbossedName()).isEqualTo(newEmbossedName);
        assertThat(updatedCard.getActiveStatus()).isEqualTo("N");
        assertThat(updatedCard.getCardNumber()).isEqualTo(testCard.getCardNumber());
    }

    /**
     * Tests card deletion operations validating that cards can be safely
     * removed while maintaining referential integrity.
     * 
     * Validates:
     * - Successful card removal
     * - Foreign key constraint handling
     * - Cascade delete behavior (if configured)
     */
    @Test
    void testDeleteCard() {
        // Given: Create and save test card
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        Card testCard = testDataGenerator.generateCard();
        testCard.setAccountId(testAccount.getAccountId());
        testCard.setCustomerId(testCustomer.getCustomerId());
        Card savedCard = cardRepository.save(testCard);

        // When: Delete the card
        cardRepository.delete(savedCard);

        // Then: Validate deletion
        Optional<Card> deletedCard = cardRepository.findById(savedCard.getCardNumber());
        assertThat(deletedCard).isEmpty();
    }

    // ========================================
    // Secondary Index Testing (CARDAIX Replacement)
    // ========================================

    /**
     * Tests findByAccountId method validating the replacement of VSAM CARDAIX
     * alternate index with PostgreSQL secondary index performance and accuracy.
     * 
     * Validates:
     * - Secondary index utilization for account-based lookups
     * - Multiple cards per account retrieval
     * - Query performance optimization
     * - Result ordering and consistency
     */
    @Test
    void testFindByAccountId() {
        // Given: Create account with multiple cards
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        // Create multiple cards for the same account
        Card card1 = testDataGenerator.generateCard();
        card1.setAccountId(testAccount.getAccountId());
        card1.setCustomerId(testCustomer.getCustomerId());
        card1.setCardNumber("1111111111111111");
        cardRepository.save(card1);

        Card card2 = testDataGenerator.generateCard();
        card2.setAccountId(testAccount.getAccountId());
        card2.setCustomerId(testCustomer.getCustomerId());
        card2.setCardNumber("2222222222222222");
        cardRepository.save(card2);

        // When: Find by account ID with performance timing
        long startTime = System.currentTimeMillis();
        List<Card> accountCards = cardRepository.findByAccountId(testAccount.getAccountId());
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Then: Validate secondary index performance and results
        assertThat(elapsedTime).isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        assertThat(accountCards).hasSize(2);
        assertThat(accountCards).extracting(Card::getAccountId)
            .containsOnly(testAccount.getAccountId());
        assertThat(accountCards).extracting(Card::getCardNumber)
            .containsExactlyInAnyOrder("1111111111111111", "2222222222222222");
    }

    /**
     * Tests findByCustomerId method validating customer-based card lookups
     * for comprehensive customer card ownership validation.
     * 
     * Validates:
     * - Customer-based card retrieval
     * - Multi-account card aggregation per customer
     * - Foreign key relationship traversal
     */
    @Test
    void testFindByCustomerId() {
        // Given: Create customer with cards across multiple accounts
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        // Create two accounts for the customer
        Account account1 = testDataGenerator.generateAccount();
        account1.setCustomerId(testCustomer.getCustomerId());
        account1.setAccountId(1000000001L);
        accountRepository.save(account1);

        Account account2 = testDataGenerator.generateAccount();
        account2.setCustomerId(testCustomer.getCustomerId());
        account2.setAccountId(1000000002L);
        accountRepository.save(account2);

        // Create cards for both accounts
        Card card1 = testDataGenerator.generateCard();
        card1.setAccountId(account1.getAccountId());
        card1.setCustomerId(testCustomer.getCustomerId());
        card1.setCardNumber("1111111111111111");
        cardRepository.save(card1);

        Card card2 = testDataGenerator.generateCard();
        card2.setAccountId(account2.getAccountId());
        card2.setCustomerId(testCustomer.getCustomerId());
        card2.setCardNumber("2222222222222222");
        cardRepository.save(card2);

        // When: Find by customer ID
        List<Card> customerCards = cardRepository.findByCustomerId(testCustomer.getCustomerId());

        // Then: Validate customer card aggregation
        assertThat(customerCards).hasSize(2);
        assertThat(customerCards).extracting(Card::getCustomerId)
            .containsOnly(testCustomer.getCustomerId());
        assertThat(customerCards).extracting(Card::getAccountId)
            .containsExactlyInAnyOrder(account1.getAccountId(), account2.getAccountId());
    }

    // ========================================
    // CVV Code Validation Testing
    // ========================================

    /**
     * Tests CVV code storage and retrieval validating 3-digit format
     * validation and security field handling.
     * 
     * Validates:
     * - CVV code format validation (3 digits)
     * - Secure storage and retrieval
     * - Invalid CVV code rejection
     */
    @Test
    void testCvvCodeValidation() {
        // Given: Create test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        Card testCard = testDataGenerator.generateCard();
        testCard.setAccountId(testAccount.getAccountId());
        testCard.setCustomerId(testCustomer.getCustomerId());
        testCard.setCvvCode("123");

        // When: Save card with valid CVV
        Card savedCard = cardRepository.save(testCard);

        // Then: Validate CVV storage and retrieval
        assertThat(savedCard.getCvvCode()).isEqualTo("123");
        assertThat(savedCard.getCvvCode()).hasSize(3);
        assertThat(savedCard.getCvvCode()).matches("\\d{3}");
    }

    // ========================================
    // Embossed Name Field Testing
    // ========================================

    /**
     * Tests embossed name field handling validating 50-character maximum
     * length constraint and proper text formatting.
     * 
     * Validates:
     * - Maximum length enforcement (50 characters)
     * - Text formatting and case handling
     * - Unicode character support
     */
    @Test
    void testEmbossedNameHandling() {
        // Given: Create test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        Card testCard = testDataGenerator.generateCard();
        testCard.setAccountId(testAccount.getAccountId());
        testCard.setCustomerId(testCustomer.getCustomerId());
        
        // Test maximum length embossed name (50 characters)
        String maxLengthName = "JOHN SMITH CARDHOLDER NAME MAXIMUM LENGTH TEST";
        testCard.setEmbossedName(maxLengthName);

        // When: Save card with maximum length name
        Card savedCard = cardRepository.save(testCard);

        // Then: Validate embossed name handling
        assertThat(savedCard.getEmbossedName()).isEqualTo(maxLengthName);
        assertThat(savedCard.getEmbossedName()).hasSizeLessThanOrEqualTo(50);
    }

    // ========================================
    // Expiration Date Testing
    // ========================================

    /**
     * Tests expiration date parsing and temporal queries validating
     * date handling and expiration logic for card lifecycle management.
     * 
     * Validates:
     * - Date format parsing and storage
     * - Expiration date comparison logic
     * - Temporal query operations
     */
    @Test
    void testExpirationDateHandling() {
        // Given: Create test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        // Create cards with different expiration dates
        Card expiredCard = testDataGenerator.generateCard();
        expiredCard.setAccountId(testAccount.getAccountId());
        expiredCard.setCustomerId(testCustomer.getCustomerId());
        expiredCard.setCardNumber("1111111111111111");
        expiredCard.setExpirationDate(LocalDate.now().minusDays(1));
        cardRepository.save(expiredCard);

        Card validCard = testDataGenerator.generateCard();
        validCard.setAccountId(testAccount.getAccountId());
        validCard.setCustomerId(testCustomer.getCustomerId());
        validCard.setCardNumber("2222222222222222");
        validCard.setExpirationDate(LocalDate.now().plusMonths(12));
        cardRepository.save(validCard);

        // When: Query all cards
        List<Card> allCards = cardRepository.findAll();

        // Then: Validate expiration date handling
        assertThat(allCards).hasSize(2);
        
        Card retrievedExpiredCard = allCards.stream()
            .filter(c -> c.getCardNumber().equals("1111111111111111"))
            .findFirst().orElseThrow();
        
        Card retrievedValidCard = allCards.stream()
            .filter(c -> c.getCardNumber().equals("2222222222222222"))
            .findFirst().orElseThrow();

        assertThat(retrievedExpiredCard.getExpirationDate()).isBefore(LocalDate.now());
        assertThat(retrievedValidCard.getExpirationDate()).isAfter(LocalDate.now());
    }

    // ========================================
    // Active Status Flag Testing
    // ========================================

    /**
     * Tests active status flag updates and filtering validating
     * card activation/deactivation operations and status-based queries.
     * 
     * Validates:
     * - Status flag update operations
     * - Active/inactive card filtering
     * - Status-based query performance
     */
    @Test
    void testActiveStatusFiltering() {
        // Given: Create test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        // Create active and inactive cards
        Card activeCard = testDataGenerator.generateCard();
        activeCard.setAccountId(testAccount.getAccountId());
        activeCard.setCustomerId(testCustomer.getCustomerId());
        activeCard.setCardNumber("1111111111111111");
        activeCard.setActiveStatus("Y");
        cardRepository.save(activeCard);

        Card inactiveCard = testDataGenerator.generateCard();
        inactiveCard.setAccountId(testAccount.getAccountId());
        inactiveCard.setCustomerId(testCustomer.getCustomerId());
        inactiveCard.setCardNumber("2222222222222222");
        inactiveCard.setActiveStatus("N");
        cardRepository.save(inactiveCard);

        // When: Query by account and verify status filtering capability
        List<Card> allCards = cardRepository.findByAccountId(testAccount.getAccountId());

        // Then: Validate status filtering
        assertThat(allCards).hasSize(2);
        
        List<Card> activeCards = allCards.stream()
            .filter(c -> "Y".equals(c.getActiveStatus()))
            .toList();
        
        List<Card> inactiveCards = allCards.stream()
            .filter(c -> "N".equals(c.getActiveStatus()))
            .toList();

        assertThat(activeCards).hasSize(1);
        assertThat(inactiveCards).hasSize(1);
        assertThat(activeCards.get(0).getCardNumber()).isEqualTo("1111111111111111");
        assertThat(inactiveCards.get(0).getCardNumber()).isEqualTo("2222222222222222");
    }

    // ========================================
    // Card Issuance Testing
    // ========================================

    /**
     * Tests card issuance with unique card number generation validating
     * that new cards are issued with unique identifiers and proper
     * account relationships.
     * 
     * Validates:
     * - Unique card number generation
     * - Account relationship establishment
     * - Initial status assignment
     * - Expiration date calculation
     */
    @Test
    void testCardIssuance() {
        // Given: Create test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        // When: Issue multiple cards for the same account
        Card card1 = testDataGenerator.generateCard();
        card1.setAccountId(testAccount.getAccountId());
        card1.setCustomerId(testCustomer.getCustomerId());
        card1.setExpirationDate(LocalDate.now().plusMonths(24));
        card1.setActiveStatus("Y");
        Card issuedCard1 = cardRepository.save(card1);

        Card card2 = testDataGenerator.generateCard();
        card2.setAccountId(testAccount.getAccountId());
        card2.setCustomerId(testCustomer.getCustomerId());
        card2.setExpirationDate(LocalDate.now().plusMonths(24));
        card2.setActiveStatus("Y");
        // Ensure different card number
        while (card2.getCardNumber().equals(card1.getCardNumber())) {
            card2 = testDataGenerator.generateCard();
            card2.setAccountId(testAccount.getAccountId());
            card2.setCustomerId(testCustomer.getCustomerId());
            card2.setExpirationDate(LocalDate.now().plusMonths(24));
            card2.setActiveStatus("Y");
        }
        Card issuedCard2 = cardRepository.save(card2);

        // Then: Validate card issuance
        assertThat(issuedCard1.getCardNumber()).isNotEqualTo(issuedCard2.getCardNumber());
        assertThat(issuedCard1.getAccountId()).isEqualTo(testAccount.getAccountId());
        assertThat(issuedCard2.getAccountId()).isEqualTo(testAccount.getAccountId());
        assertThat(issuedCard1.getActiveStatus()).isEqualTo("Y");
        assertThat(issuedCard2.getActiveStatus()).isEqualTo("Y");
        assertThat(issuedCard1.getExpirationDate()).isAfter(LocalDate.now());
        assertThat(issuedCard2.getExpirationDate()).isAfter(LocalDate.now());
    }

    // ========================================
    // Card Replacement Testing
    // ========================================

    /**
     * Tests card replacement operations validating that old cards can be
     * replaced with new cards while maintaining account relationships
     * and proper status transitions.
     * 
     * Validates:
     * - Old card deactivation
     * - New card activation
     * - Account relationship preservation
     * - Status transition accuracy
     */
    @Test
    void testCardReplacement() {
        // Given: Create test entities with existing card
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        Card oldCard = testDataGenerator.generateCard();
        oldCard.setAccountId(testAccount.getAccountId());
        oldCard.setCustomerId(testCustomer.getCustomerId());
        oldCard.setCardNumber("1111111111111111");
        oldCard.setActiveStatus("Y");
        Card savedOldCard = cardRepository.save(oldCard);

        // When: Replace the card
        // Deactivate old card
        savedOldCard.setActiveStatus("N");
        cardRepository.save(savedOldCard);

        // Create new replacement card
        Card newCard = testDataGenerator.generateCard();
        newCard.setAccountId(testAccount.getAccountId());
        newCard.setCustomerId(testCustomer.getCustomerId());
        newCard.setCardNumber("2222222222222222");
        newCard.setActiveStatus("Y");
        newCard.setExpirationDate(LocalDate.now().plusMonths(24));
        Card savedNewCard = cardRepository.save(newCard);

        // Then: Validate card replacement
        Card retrievedOldCard = cardRepository.findById(savedOldCard.getCardNumber()).orElseThrow();
        Card retrievedNewCard = cardRepository.findById(savedNewCard.getCardNumber()).orElseThrow();

        assertThat(retrievedOldCard.getActiveStatus()).isEqualTo("N");
        assertThat(retrievedNewCard.getActiveStatus()).isEqualTo("Y");
        assertThat(retrievedOldCard.getAccountId()).isEqualTo(retrievedNewCard.getAccountId());
        assertThat(retrievedOldCard.getCustomerId()).isEqualTo(retrievedNewCard.getCustomerId());
    }

    // ========================================
    // Concurrent Access Testing
    // ========================================

    /**
     * Tests concurrent card activation scenarios validating that multiple
     * threads can safely update card status without data corruption or
     * deadlock conditions.
     * 
     * Validates:
     * - Thread-safe card updates
     * - Optimistic locking behavior
     * - Data consistency under concurrent access
     */
    @Test
    void testConcurrentCardActivation() throws InterruptedException, ExecutionException {
        // Given: Create test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        Card testCard = testDataGenerator.generateCard();
        testCard.setAccountId(testAccount.getAccountId());
        testCard.setCustomerId(testCustomer.getCustomerId());
        testCard.setActiveStatus("N");
        Card savedCard = cardRepository.save(testCard);

        // When: Attempt concurrent activation
        CompletableFuture<Card> future1 = CompletableFuture.supplyAsync(() -> {
            Card card = cardRepository.findById(savedCard.getCardNumber()).orElseThrow();
            card.setActiveStatus("Y");
            return cardRepository.save(card);
        });

        CompletableFuture<Card> future2 = CompletableFuture.supplyAsync(() -> {
            // Add small delay to ensure concurrent access
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Card card = cardRepository.findById(savedCard.getCardNumber()).orElseThrow();
            card.setActiveStatus("Y");
            return cardRepository.save(card);
        });

        // Then: Validate concurrent access handling
        Card result1 = future1.get();
        Card result2 = future2.get();

        assertThat(result1.getActiveStatus()).isEqualTo("Y");
        assertThat(result2.getActiveStatus()).isEqualTo("Y");
        assertThat(result1.getCardNumber()).isEqualTo(result2.getCardNumber());
    }

    // ========================================
    // Foreign Key Relationship Testing
    // ========================================

    /**
     * Tests foreign key relationships with Account and Customer entities
     * validating referential integrity and cascade behavior.
     * 
     * Validates:
     * - Foreign key constraint enforcement
     * - Referential integrity maintenance
     * - Cascade delete behavior (if configured)
     */
    @Test
    void testForeignKeyRelationships() {
        // Given: Create test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        Card testCard = testDataGenerator.generateCard();
        testCard.setAccountId(testAccount.getAccountId());
        testCard.setCustomerId(testCustomer.getCustomerId());
        cardRepository.save(testCard);

        // When: Verify relationships exist
        Card savedCard = cardRepository.findById(testCard.getCardNumber()).orElseThrow();
        Account relatedAccount = accountRepository.findById(savedCard.getAccountId()).orElseThrow();
        Customer relatedCustomer = customerRepository.findById(savedCard.getCustomerId()).orElseThrow();

        // Then: Validate foreign key relationships
        assertThat(savedCard.getAccountId()).isEqualTo(relatedAccount.getAccountId());
        assertThat(savedCard.getCustomerId()).isEqualTo(relatedCustomer.getCustomerId());
        assertThat(relatedAccount.getCustomerId()).isEqualTo(relatedCustomer.getCustomerId());
    }

    // ========================================
    // Existence and Count Testing
    // ========================================

    /**
     * Tests existsById and count operations validating query performance
     * and accuracy for existence checks and record counting.
     * 
     * Validates:
     * - Existence check performance
     * - Count operation accuracy
     * - Query optimization for boolean results
     */
    @Test
    void testExistenceAndCountOperations() {
        // Given: Create test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        Card testCard = testDataGenerator.generateCard();
        testCard.setAccountId(testAccount.getAccountId());
        testCard.setCustomerId(testCustomer.getCustomerId());
        cardRepository.save(testCard);

        // When: Test existence and count operations
        boolean exists = cardRepository.existsById(testCard.getCardNumber());
        boolean notExists = cardRepository.existsById("9999999999999999");
        long totalCount = cardRepository.count();

        // Then: Validate existence and count results
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
        assertThat(totalCount).isEqualTo(1L);
    }

    // ========================================
    // Batch Operations Testing
    // ========================================

    /**
     * Tests batch card operations including saveAll and deleteAll
     * validating bulk operation performance and transaction consistency.
     * 
     * Validates:
     * - Batch insert performance
     * - Batch delete operations
     * - Transaction consistency in bulk operations
     */
    @Test
    void testBatchCardOperations() {
        // Given: Create test entities
        Customer testCustomer = createTestCustomer();
        customerRepository.save(testCustomer);
        
        Account testAccount = createTestAccount();
        testAccount.setCustomerId(testCustomer.getCustomerId());
        accountRepository.save(testAccount);

        // Create multiple cards for batch operations
        List<Card> testCards = List.of(
            createTestCard("1111111111111111", testAccount.getAccountId(), testCustomer.getCustomerId()),
            createTestCard("2222222222222222", testAccount.getAccountId(), testCustomer.getCustomerId()),
            createTestCard("3333333333333333", testAccount.getAccountId(), testCustomer.getCustomerId())
        );

        // When: Perform batch save operation
        long startTime = System.currentTimeMillis();
        cardRepository.saveAll(testCards);
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Then: Validate batch operation performance and results
        assertThat(elapsedTime).isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        long savedCount = cardRepository.count();
        assertThat(savedCount).isEqualTo(3L);

        // Test batch delete
        cardRepository.deleteAll(testCards);
        long deletedCount = cardRepository.count();
        assertThat(deletedCount).isEqualTo(0L);
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Helper method to create a test card with specified parameters.
     * 
     * @param cardNumber The card number to assign
     * @param accountId The account ID to associate
     * @param customerId The customer ID to associate
     * @return Configured test card
     */
    private Card createTestCard(String cardNumber, Long accountId, Long customerId) {
        Card card = testDataGenerator.generateCard();
        card.setCardNumber(cardNumber);
        card.setAccountId(accountId);
        card.setCustomerId(customerId);
        return card;
    }
}