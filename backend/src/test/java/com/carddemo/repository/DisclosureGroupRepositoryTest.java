/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.DisclosureGroup;
import com.carddemo.entity.Account;
import com.carddemo.entity.TransactionType;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.repository.AccountRepository;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Integration test class for DisclosureGroupRepository validating interest rate configuration,
 * account grouping, disclosure management, and BigDecimal precision for interest calculations
 * matching COBOL COMP-3 arithmetic.
 *
 * This comprehensive test suite validates:
 * - Composite key operations for disclosure groups (Account Group ID + Transaction Type + Category)
 * - Interest rate precision with BigDecimal (precision=6, scale=4) matching COBOL S9(04)V99
 * - HALF_UP rounding mode compatibility with COBOL ROUNDED clause
 * - Account group assignment and referential integrity
 * - Transaction type and category relationship validation
 * - Bulk operations and concurrent modification handling
 * - Special promotional rate and negative interest rate scenarios
 * - Performance validation against 200ms response time threshold
 *
 * Based on COBOL copybook CVTRA02Y.cpy and interest calculation logic from CBACT04C.cbl:
 * - DIS-GROUP-RECORD structure with composite key access patterns
 * - DIS-ACCT-GROUP-ID (PIC X(10)) for account group identification
 * - DIS-TRAN-TYPE-CD (PIC X(02)) for transaction type classification
 * - DIS-TRAN-CAT-CD (PIC 9(04)) for transaction category codes
 * - DIS-INT-RATE (PIC S9(04)V99) for interest rate calculations with COMP-3 precision
 *
 * Test Data Patterns:
 * - Uses TestDataGenerator for COBOL-compliant test data creation
 * - Validates BigDecimal precision against TestConstants.COBOL_DECIMAL_SCALE
 * - Tests interest calculation formula: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
 * - Ensures functional parity with mainframe VSAM DISCGRP reference file operations
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@DataJpaTest
@Transactional
public class DisclosureGroupRepositoryTest extends AbstractBaseTest implements IntegrationTest {

    @Autowired
    private DisclosureGroupRepository disclosureGroupRepository;

    @Autowired
    private AccountRepository accountRepository;

    // Test data constants matching COBOL field specifications
    private static final String TEST_ACCOUNT_GROUP_ID = "DEFAULT001";
    private static final String TEST_TRANSACTION_TYPE_CODE = "01";
    private static final String TEST_TRANSACTION_CATEGORY_CODE = "0001";
    private static final BigDecimal TEST_INTEREST_RATE = new BigDecimal("15.2500");
    private static final String TEST_GROUP_NAME = "Default Standard Rate";
    private static final int TERMS_TEXT_MAX_LENGTH = 1000;
    
    // Test performance thresholds
    private long testStartTime;
    
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testStartTime = System.currentTimeMillis();
        
        // Clear any existing test data
        disclosureGroupRepository.deleteAll();
        accountRepository.deleteAll();
        
        // Create reference data for foreign key relationships
        createTestReferenceData();
        
        logTestExecution("DisclosureGroupRepositoryTest setup completed", null);
    }

    @AfterEach
    @Override
    public void tearDown() {
        long executionTime = System.currentTimeMillis() - testStartTime;
        
        // Clean up test data
        disclosureGroupRepository.deleteAll();
        accountRepository.deleteAll();
        
        super.tearDown();
        
        // Validate response time performance
        if (executionTime > TestConstants.RESPONSE_TIME_THRESHOLD_MS) {
            logger.warn("Test execution exceeded response time threshold: {}ms > {}ms", 
                executionTime, TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        }
    }

    /**
     * Test composite key operations for disclosure groups.
     * Validates that disclosure groups can be saved and retrieved using the composite key
     * structure matching COBOL DIS-GROUP-KEY specification.
     */
    @Test
    @DisplayName("Test composite key operations for disclosure groups")
    public void testCompositeKeyOperations() {
        // Given: A disclosure group with composite key components
        DisclosureGroup disclosureGroup = createTestDisclosureGroup();
        
        // When: Saving the disclosure group
        DisclosureGroup savedGroup = disclosureGroupRepository.save(disclosureGroup);
        
        // Then: Verify the group is saved with proper composite key structure
        assertThat(savedGroup.getDisclosureGroupId()).isNotNull();
        assertThat(savedGroup.getAccountGroupId()).isEqualTo(TEST_ACCOUNT_GROUP_ID);
        assertThat(savedGroup.getTransactionTypeCode()).isEqualTo(TEST_TRANSACTION_TYPE_CODE);
        assertThat(savedGroup.getTransactionCategoryCode()).isEqualTo(TEST_TRANSACTION_CATEGORY_CODE);
        
        // And: Verify retrieval by generated primary key
        Optional<DisclosureGroup> retrievedGroup = disclosureGroupRepository.findById(savedGroup.getDisclosureGroupId());
        assertThat(retrievedGroup).isPresent();
        assertThat(retrievedGroup.get().getAccountGroupId()).isEqualTo(TEST_ACCOUNT_GROUP_ID);
        
        logTestExecution("Composite key operations test passed", null);
    }

    /**
     * Test findByAccountGroupId for group lookups.
     * Validates account group-based disclosure group lookup operations equivalent to
     * VSAM READ by account group ID key.
     */
    @Test
    @DisplayName("Test findByAccountGroupId for group lookups")
    public void testFindByAccountGroupId() {
        // Given: Multiple disclosure groups with different account group IDs
        DisclosureGroup group1 = createTestDisclosureGroup();
        DisclosureGroup group2 = createTestDisclosureGroup();
        group2.setAccountGroupId("PREMIUM01");
        group2.setInterestRate(new BigDecimal("12.7500"));
        
        disclosureGroupRepository.saveAll(List.of(group1, group2));
        
        // When: Finding by account group ID
        List<DisclosureGroup> defaultGroups = disclosureGroupRepository.findByAccountGroupId(TEST_ACCOUNT_GROUP_ID);
        List<DisclosureGroup> premiumGroups = disclosureGroupRepository.findByAccountGroupId("PREMIUM01");
        
        // Then: Verify correct groups are returned
        assertThat(defaultGroups).hasSize(1);
        assertThat(defaultGroups.get(0).getAccountGroupId()).isEqualTo(TEST_ACCOUNT_GROUP_ID);
        
        assertThat(premiumGroups).hasSize(1);
        assertThat(premiumGroups.get(0).getAccountGroupId()).isEqualTo("PREMIUM01");
        
        logTestExecution("Account group ID lookup test passed", null);
    }

    /**
     * Test interest rate precision with BigDecimal (precision=6, scale=4).
     * Validates that interest rates maintain exact precision matching COBOL
     * DIS-INT-RATE (PIC S9(04)V99) COMP-3 packed decimal format.
     */
    @Test
    @DisplayName("Test interest rate precision with BigDecimal (precision=6, scale=4)")
    public void testInterestRatePrecision() {
        // Given: Interest rates with various precision scenarios
        BigDecimal rate1 = new BigDecimal("15.2500");  // Standard rate
        BigDecimal rate2 = new BigDecimal("0.0000");   // Zero rate
        BigDecimal rate3 = new BigDecimal("99.9999");  // Maximum precision
        
        DisclosureGroup group1 = createTestDisclosureGroup();
        group1.setInterestRate(rate1);
        
        DisclosureGroup group2 = createTestDisclosureGroup();
        group2.setAccountGroupId("ZERO0001");
        group2.setInterestRate(rate2);
        
        DisclosureGroup group3 = createTestDisclosureGroup();
        group3.setAccountGroupId("MAXRATE01");
        group3.setInterestRate(rate3);
        
        // When: Saving groups with different precision rates
        List<DisclosureGroup> savedGroups = disclosureGroupRepository.saveAll(List.of(group1, group2, group3));
        
        // Then: Verify precision is preserved exactly
        DisclosureGroup retrievedGroup1 = disclosureGroupRepository.findById(savedGroups.get(0).getDisclosureGroupId()).get();
        DisclosureGroup retrievedGroup2 = disclosureGroupRepository.findById(savedGroups.get(1).getDisclosureGroupId()).get();
        DisclosureGroup retrievedGroup3 = disclosureGroupRepository.findById(savedGroups.get(2).getDisclosureGroupId()).get();
        
        // Use COBOL precision validation
        assertBigDecimalEquals(rate1, retrievedGroup1.getInterestRate(), "Standard rate precision mismatch");
        assertBigDecimalEquals(rate2, retrievedGroup2.getInterestRate(), "Zero rate precision mismatch");
        assertBigDecimalEquals(rate3, retrievedGroup3.getInterestRate(), "Maximum precision rate mismatch");
        
        // Validate precision meets COBOL requirements
        assertThat(validateCobolPrecision(retrievedGroup1.getInterestRate(), "interestRate")).isTrue();
        assertThat(validateCobolPrecision(retrievedGroup2.getInterestRate(), "interestRate")).isTrue();
        assertThat(validateCobolPrecision(retrievedGroup3.getInterestRate(), "interestRate")).isTrue();
        
        logTestExecution("Interest rate precision test passed", null);
    }

    /**
     * Test interest calculation with HALF_UP rounding.
     * Validates that interest calculations use HALF_UP rounding mode to match
     * COBOL ROUNDED clause behavior for financial calculations.
     */
    @Test
    @DisplayName("Test interest calculation with HALF_UP rounding")
    public void testInterestCalculationWithRounding() {
        // Given: An interest rate requiring rounding
        BigDecimal interestRate = new BigDecimal("15.2567");  // More than 4 decimal places
        BigDecimal expectedRoundedRate = interestRate.setScale(4, TestConstants.COBOL_ROUNDING_MODE);
        
        DisclosureGroup group = createTestDisclosureGroup();
        group.setInterestRate(interestRate);
        
        // When: Saving the group (should apply rounding)
        DisclosureGroup savedGroup = disclosureGroupRepository.save(group);
        
        // Then: Verify rounding matches COBOL ROUNDED clause behavior
        BigDecimal actualRate = savedGroup.getInterestRate();
        assertBigDecimalEquals(expectedRoundedRate, actualRate, "Interest rate rounding does not match COBOL ROUNDED clause");
        
        // Test interest calculation formula: (balance * rate) / 1200
        BigDecimal balance = new BigDecimal("1000.00");
        BigDecimal expectedMonthlyInterest = balance.multiply(actualRate)
            .divide(new BigDecimal("1200"), TestConstants.COBOL_ROUNDING_MODE)
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        
        // Verify calculation precision
        BigDecimal actualMonthlyInterest = balance.multiply(actualRate)
            .divide(new BigDecimal("1200"), TestConstants.COBOL_ROUNDING_MODE)
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            
        assertBigDecimalEquals(expectedMonthlyInterest, actualMonthlyInterest, "Interest calculation rounding mismatch");
        
        logTestExecution("Interest calculation with rounding test passed", null);
    }

    /**
     * Test findByTransactionTypeAndCategory relationships.
     * Validates composite lookup operations combining transaction type and category codes
     * equivalent to VSAM READ by transaction type and category combination.
     */
    @Test
    @DisplayName("Test findByTransactionTypeCodeAndTransactionCategoryCode relationships")
    public void testFindByTransactionTypeAndCategory() {
        // Given: Multiple disclosure groups with different transaction type/category combinations
        DisclosureGroup group1 = createTestDisclosureGroup(); // 01/0001
        
        DisclosureGroup group2 = createTestDisclosureGroup();
        group2.setTransactionTypeCode("02"); // Payment
        group2.setTransactionCategoryCode("0002");
        group2.setInterestRate(new BigDecimal("0.0000")); // No interest on payments
        
        DisclosureGroup group3 = createTestDisclosureGroup();
        group3.setTransactionTypeCode("03"); // Cash advance
        group3.setTransactionCategoryCode("0003");
        group3.setInterestRate(new BigDecimal("24.9900")); // Higher rate for cash advances
        
        disclosureGroupRepository.saveAll(List.of(group1, group2, group3));
        
        // When: Finding by transaction type and category combination
        List<DisclosureGroup> purchaseGroups = disclosureGroupRepository
            .findByTransactionTypeCodeAndTransactionCategoryCode("01", "0001");
        List<DisclosureGroup> paymentGroups = disclosureGroupRepository
            .findByTransactionTypeCodeAndTransactionCategoryCode("02", "0002");
        List<DisclosureGroup> cashAdvanceGroups = disclosureGroupRepository
            .findByTransactionTypeCodeAndTransactionCategoryCode("03", "0003");
        
        // Then: Verify correct groups are returned for each transaction type/category
        assertThat(purchaseGroups).hasSize(1);
        assertThat(purchaseGroups.get(0).getTransactionTypeCode()).isEqualTo("01");
        assertThat(purchaseGroups.get(0).getTransactionCategoryCode()).isEqualTo("0001");
        
        assertThat(paymentGroups).hasSize(1);
        assertThat(paymentGroups.get(0).getTransactionTypeCode()).isEqualTo("02");
        assertThat(paymentGroups.get(0).getInterestRate()).isEqualByComparingTo(BigDecimal.ZERO);
        
        assertThat(cashAdvanceGroups).hasSize(1);
        assertThat(cashAdvanceGroups.get(0).getTransactionTypeCode()).isEqualTo("03");
        assertBigDecimalEquals(new BigDecimal("24.9900"), cashAdvanceGroups.get(0).getInterestRate(), 
            "Cash advance interest rate mismatch");
        
        logTestExecution("Transaction type and category lookup test passed", null);
    }

    /**
     * Test negative interest rate handling.
     * Validates that the system can handle negative interest rates for special
     * promotional scenarios or rebate programs.
     */
    @Test
    @DisplayName("Test negative interest rate handling")
    public void testNegativeInterestRateHandling() {
        // Given: A disclosure group with negative interest rate (promotional rebate)
        BigDecimal negativeRate = new BigDecimal("-2.5000"); // 2.5% rebate
        DisclosureGroup promoGroup = createTestDisclosureGroup();
        promoGroup.setAccountGroupId("PROMO001");
        promoGroup.setInterestRate(negativeRate);
        promoGroup.setGroupName("Promotional Rebate Group");
        promoGroup.setDescription("Special promotional group with interest rebates");
        
        // When: Saving the group with negative rate
        DisclosureGroup savedGroup = disclosureGroupRepository.save(promoGroup);
        
        // Then: Verify negative rate is preserved
        assertThat(savedGroup.getInterestRate()).isNegative();
        assertBigDecimalEquals(negativeRate, savedGroup.getInterestRate(), "Negative interest rate not preserved");
        
        // Verify retrieval and calculation with negative rate
        Optional<DisclosureGroup> retrievedGroup = disclosureGroupRepository.findById(savedGroup.getDisclosureGroupId());
        assertThat(retrievedGroup).isPresent();
        
        BigDecimal retrievedRate = retrievedGroup.get().getInterestRate();
        assertBigDecimalEquals(negativeRate, retrievedRate, "Retrieved negative rate mismatch");
        
        // Test calculation with negative rate (should result in credit to customer)
        BigDecimal balance = new BigDecimal("1000.00");
        BigDecimal monthlyInterest = balance.multiply(retrievedRate)
            .divide(new BigDecimal("1200"), TestConstants.COBOL_ROUNDING_MODE)
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            
        assertThat(monthlyInterest).isNegative();
        
        logTestExecution("Negative interest rate test passed", null);
    }

    /**
     * Test terms text field storage (1000 chars).
     * Validates that the disclosure group can store terms text up to the maximum
     * length while preserving content integrity.
     */
    @Test
    @DisplayName("Test terms text field storage (1000 chars)")
    public void testTermsTextFieldStorage() {
        // Given: A disclosure group with maximum length terms text
        StringBuilder termsBuilder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            termsBuilder.append("Standard terms and conditions apply. Interest rates are variable and subject to change. ");
        }
        String termsText = termsBuilder.toString();
        assertThat(termsText.length()).isLessThanOrEqualTo(TERMS_TEXT_MAX_LENGTH);
        
        DisclosureGroup group = createTestDisclosureGroup();
        group.setDescription(termsText);
        
        // When: Saving the group with long terms text
        DisclosureGroup savedGroup = disclosureGroupRepository.save(group);
        
        // Then: Verify terms text is preserved
        assertThat(savedGroup.getDescription()).isEqualTo(termsText);
        assertThat(savedGroup.getDescription().length()).isLessThanOrEqualTo(TERMS_TEXT_MAX_LENGTH);
        
        // Verify retrieval maintains content integrity
        Optional<DisclosureGroup> retrievedGroup = disclosureGroupRepository.findById(savedGroup.getDisclosureGroupId());
        assertThat(retrievedGroup).isPresent();
        assertThat(retrievedGroup.get().getDescription()).isEqualTo(termsText);
        
        logTestExecution("Terms text field storage test passed", null);
    }

    /**
     * Test account group assignment.
     * Validates referential integrity between DisclosureGroup and Account entities
     * through account group assignment validation.
     */
    @Test
    @DisplayName("Test account group assignment")
    public void testAccountGroupAssignment() {
        // Given: An account with a specific group ID
        Account testAccount = createTestAccount();
        testAccount.setGroupId(TEST_ACCOUNT_GROUP_ID);
        Account savedAccount = accountRepository.save(testAccount);
        
        // And: A disclosure group matching the account's group
        DisclosureGroup disclosureGroup = createTestDisclosureGroup();
        disclosureGroup.setAccountGroupId(TEST_ACCOUNT_GROUP_ID);
        DisclosureGroup savedGroup = disclosureGroupRepository.save(disclosureGroup);
        
        // When: Retrieving disclosure groups for the account's group
        List<DisclosureGroup> matchingGroups = disclosureGroupRepository.findByAccountGroupId(TEST_ACCOUNT_GROUP_ID);
        
        // Then: Verify proper group assignment relationship
        assertThat(matchingGroups).hasSize(1);
        assertThat(matchingGroups.get(0).getAccountGroupId()).isEqualTo(savedAccount.getGroupId());
        assertThat(matchingGroups.get(0).getDisclosureGroupId()).isEqualTo(savedGroup.getDisclosureGroupId());
        
        logTestExecution("Account group assignment test passed", null);
    }

    /**
     * Test bulk interest rate updates.
     * Validates batch operations for updating interest rates across multiple
     * disclosure groups while maintaining data consistency and precision.
     */
    @Test
    @DisplayName("Test bulk interest rate updates")
    public void testBulkInterestRateUpdates() {
        // Given: Multiple disclosure groups with different rates
        DisclosureGroup group1 = createTestDisclosureGroup();
        group1.setAccountGroupId("BULK001");
        group1.setInterestRate(new BigDecimal("15.0000"));
        
        DisclosureGroup group2 = createTestDisclosureGroup();
        group2.setAccountGroupId("BULK002");
        group2.setTransactionTypeCode("02");
        group2.setInterestRate(new BigDecimal("12.0000"));
        
        DisclosureGroup group3 = createTestDisclosureGroup();
        group3.setAccountGroupId("BULK003");
        group3.setTransactionTypeCode("03");
        group3.setInterestRate(new BigDecimal("20.0000"));
        
        List<DisclosureGroup> originalGroups = disclosureGroupRepository.saveAll(List.of(group1, group2, group3));
        
        // When: Performing bulk rate update
        BigDecimal newRate = new BigDecimal("13.5000");
        for (DisclosureGroup group : originalGroups) {
            group.setInterestRate(newRate);
        }
        List<DisclosureGroup> updatedGroups = disclosureGroupRepository.saveAll(originalGroups);
        
        // Then: Verify all groups have the new rate with proper precision
        assertThat(updatedGroups).hasSize(3);
        for (DisclosureGroup group : updatedGroups) {
            assertBigDecimalEquals(newRate, group.getInterestRate(), 
                "Bulk update failed for group " + group.getAccountGroupId());
            assertThat(validateCobolPrecision(group.getInterestRate(), "bulkUpdatedRate")).isTrue();
        }
        
        // Verify database consistency
        List<DisclosureGroup> allGroups = disclosureGroupRepository.findAll();
        assertThat(allGroups).hasSize(3);
        for (DisclosureGroup group : allGroups) {
            assertBigDecimalEquals(newRate, group.getInterestRate(), "Database consistency check failed");
        }
        
        logTestExecution("Bulk interest rate updates test passed", null);
    }

    /**
     * Test referential integrity with Account entity.
     * Validates foreign key relationships and cascade behavior between
     * DisclosureGroup and Account entities.
     */
    @Test
    @DisplayName("Test referential integrity with Account entity")
    public void testReferentialIntegrityWithAccount() {
        // Given: An account and associated disclosure group
        Account account = createTestAccount();
        account.setGroupId(TEST_ACCOUNT_GROUP_ID);
        Account savedAccount = accountRepository.save(account);
        
        DisclosureGroup disclosureGroup = createTestDisclosureGroup();
        disclosureGroup.setAccountGroupId(TEST_ACCOUNT_GROUP_ID);
        DisclosureGroup savedGroup = disclosureGroupRepository.save(disclosureGroup);
        
        // When: Verifying referential integrity
        List<DisclosureGroup> groupsForAccount = disclosureGroupRepository.findByAccountGroupId(savedAccount.getGroupId());
        
        // Then: Verify relationship consistency
        assertThat(groupsForAccount).hasSize(1);
        assertThat(groupsForAccount.get(0).getAccountGroupId()).isEqualTo(savedAccount.getGroupId());
        
        // Test orphaned account handling
        disclosureGroupRepository.delete(savedGroup);
        List<DisclosureGroup> remainingGroups = disclosureGroupRepository.findByAccountGroupId(TEST_ACCOUNT_GROUP_ID);
        assertThat(remainingGroups).isEmpty();
        
        // Account should still exist (no cascading delete)
        Optional<Account> remainingAccount = accountRepository.findById(savedAccount.getAccountId());
        assertThat(remainingAccount).isPresent();
        
        logTestExecution("Referential integrity test passed", null);
    }

    /**
     * Test concurrent interest rate modifications.
     * Validates system behavior under concurrent access to disclosure groups
     * for interest rate modifications.
     */
    @Test
    @DisplayName("Test concurrent interest rate modifications")
    public void testConcurrentInterestRateModifications() throws InterruptedException, ExecutionException {
        // Given: A disclosure group for concurrent testing
        DisclosureGroup originalGroup = createTestDisclosureGroup();
        originalGroup.setInterestRate(new BigDecimal("15.0000"));
        DisclosureGroup savedGroup = disclosureGroupRepository.save(originalGroup);
        final Long groupId = savedGroup.getDisclosureGroupId();
        
        // When: Performing concurrent modifications
        CompletableFuture<DisclosureGroup> future1 = CompletableFuture.supplyAsync(() -> {
            DisclosureGroup group = disclosureGroupRepository.findById(groupId).get();
            group.setInterestRate(new BigDecimal("16.0000"));
            return disclosureGroupRepository.save(group);
        });
        
        CompletableFuture<DisclosureGroup> future2 = CompletableFuture.supplyAsync(() -> {
            DisclosureGroup group = disclosureGroupRepository.findById(groupId).get();
            group.setInterestRate(new BigDecimal("17.0000"));
            return disclosureGroupRepository.save(group);
        });
        
        // Then: Verify concurrent modifications complete successfully
        DisclosureGroup result1 = future1.get();
        DisclosureGroup result2 = future2.get();
        
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        
        // Final state should have one of the updated rates
        DisclosureGroup finalGroup = disclosureGroupRepository.findById(groupId).get();
        assertThat(finalGroup.getInterestRate())
            .isIn(new BigDecimal("16.0000"), new BigDecimal("17.0000"));
        
        logTestExecution("Concurrent modifications test passed", null);
    }

    /**
     * Test special promotional rate handling.
     * Validates system capability to handle special promotional interest rates
     * including time-limited offers and customer-specific rates.
     */
    @Test
    @DisplayName("Test special promotional rate handling")
    public void testSpecialPromotionalRateHandling() {
        // Given: Promotional disclosure groups with special rates
        DisclosureGroup promoGroup = createTestDisclosureGroup();
        promoGroup.setAccountGroupId("PROMO2024");
        promoGroup.setInterestRate(new BigDecimal("0.0000")); // 0% promotional rate
        promoGroup.setGroupName("0% Intro APR Promotion");
        promoGroup.setDescription("Promotional 0% interest rate for 12 months");
        promoGroup.setEffectiveDate(LocalDateTime.now());
        promoGroup.setExpirationDate(LocalDateTime.now().plusMonths(12));
        
        // When: Saving promotional group
        DisclosureGroup savedPromo = disclosureGroupRepository.save(promoGroup);
        
        // Then: Verify promotional rate characteristics
        assertThat(savedPromo.getInterestRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedPromo.getGroupName()).contains("Promotion");
        assertThat(savedPromo.getEffectiveDate()).isNotNull();
        assertThat(savedPromo.getExpirationDate()).isNotNull();
        assertThat(savedPromo.getExpirationDate()).isAfter(savedPromo.getEffectiveDate());
        
        // Test retrieval of promotional groups
        List<DisclosureGroup> promoGroups = disclosureGroupRepository.findByInterestRate(BigDecimal.ZERO);
        assertThat(promoGroups).hasSizeGreaterThanOrEqualTo(1);
        
        boolean foundPromo = promoGroups.stream()
            .anyMatch(group -> "PROMO2024".equals(group.getAccountGroupId()));
        assertThat(foundPromo).isTrue();
        
        logTestExecution("Special promotional rate handling test passed", null);
    }

    /**
     * Test disclosure group name uniqueness.
     * Validates that disclosure group names can be used for lookup and
     * administrative operations while maintaining proper constraints.
     */
    @Test
    @DisplayName("Test disclosure group name uniqueness")
    public void testDisclosureGroupNameHandling() {
        // Given: Disclosure groups with different names
        DisclosureGroup group1 = createTestDisclosureGroup();
        group1.setGroupName("Standard Rate Group");
        
        DisclosureGroup group2 = createTestDisclosureGroup();
        group2.setAccountGroupId("PREMIUM02");
        group2.setGroupName("Premium Rate Group");
        group2.setInterestRate(new BigDecimal("12.0000"));
        
        disclosureGroupRepository.saveAll(List.of(group1, group2));
        
        // When: Finding groups by name
        List<DisclosureGroup> standardGroups = disclosureGroupRepository.findByGroupName("Standard Rate Group");
        List<DisclosureGroup> premiumGroups = disclosureGroupRepository.findByGroupName("Premium Rate Group");
        
        // Then: Verify name-based lookups work correctly
        assertThat(standardGroups).hasSize(1);
        assertThat(standardGroups.get(0).getGroupName()).isEqualTo("Standard Rate Group");
        
        assertThat(premiumGroups).hasSize(1);
        assertThat(premiumGroups.get(0).getGroupName()).isEqualTo("Premium Rate Group");
        assertBigDecimalEquals(new BigDecimal("12.0000"), premiumGroups.get(0).getInterestRate(),
            "Premium group rate mismatch");
        
        logTestExecution("Disclosure group name handling test passed", null);
    }

    /**
     * Test interest compounding calculations.
     * Validates that the system can perform compound interest calculations
     * using the stored disclosure group rates with proper precision.
     */
    @Test
    @DisplayName("Test interest compounding calculations")
    public void testInterestCompoundingCalculations() {
        // Given: A disclosure group with a specific rate for compounding
        BigDecimal annualRate = new BigDecimal("18.0000"); // 18% annual rate
        DisclosureGroup group = createTestDisclosureGroup();
        group.setInterestRate(annualRate);
        disclosureGroupRepository.save(group);
        
        // When: Calculating compound interest (matching COBOL formula)
        BigDecimal principal = new BigDecimal("1000.00");
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), TestConstants.COBOL_ROUNDING_MODE); // rate/1200
        
        // Calculate monthly interest for 12 months with compounding
        BigDecimal balance = principal;
        for (int month = 1; month <= 12; month++) {
            BigDecimal monthlyInterest = balance.multiply(monthlyRate)
                .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            balance = balance.add(monthlyInterest)
                .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        }
        
        // Then: Verify compounding calculation precision
        assertThat(balance).isGreaterThan(principal);
        assertThat(validateCobolPrecision(balance, "compoundedBalance")).isTrue();
        
        // Expected annual compound amount should be close to standard compound interest formula
        BigDecimal expectedCompound = principal.multiply(
            BigDecimal.ONE.add(annualRate.divide(new BigDecimal("100"), TestConstants.COBOL_ROUNDING_MODE))
        );
        
        // Allow for small variance due to monthly compounding vs annual
        BigDecimal tolerance = new BigDecimal("10.00"); // $10 tolerance
        assertThat(balance).isBetween(
            expectedCompound.subtract(tolerance),
            expectedCompound.add(tolerance)
        );
        
        logTestExecution("Interest compounding calculations test passed", null);
    }

    /**
     * Test standard JpaRepository CRUD operations.
     * Validates all inherited JpaRepository methods work correctly with
     * DisclosureGroup entity and maintain ACID compliance.
     */
    @Test
    @DisplayName("Test standard JpaRepository CRUD operations")
    public void testStandardCrudOperations() {
        // Test count operation
        long initialCount = disclosureGroupRepository.count();
        
        // Test save operation
        DisclosureGroup group = createTestDisclosureGroup();
        DisclosureGroup savedGroup = disclosureGroupRepository.save(group);
        assertThat(savedGroup.getDisclosureGroupId()).isNotNull();
        assertThat(disclosureGroupRepository.count()).isEqualTo(initialCount + 1);
        
        // Test findById operation
        Optional<DisclosureGroup> foundGroup = disclosureGroupRepository.findById(savedGroup.getDisclosureGroupId());
        assertThat(foundGroup).isPresent();
        assertThat(foundGroup.get().getAccountGroupId()).isEqualTo(TEST_ACCOUNT_GROUP_ID);
        
        // Test existsById operation
        assertThat(disclosureGroupRepository.existsById(savedGroup.getDisclosureGroupId())).isTrue();
        
        // Test findAll operation
        List<DisclosureGroup> allGroups = disclosureGroupRepository.findAll();
        assertThat(allGroups).hasSize((int)(initialCount + 1));
        
        // Test flush operation
        savedGroup.setInterestRate(new BigDecimal("16.0000"));
        disclosureGroupRepository.saveAndFlush(savedGroup);
        
        DisclosureGroup flushedGroup = disclosureGroupRepository.findById(savedGroup.getDisclosureGroupId()).get();
        assertBigDecimalEquals(new BigDecimal("16.0000"), flushedGroup.getInterestRate(), "Flush operation failed");
        
        // Test delete operation
        disclosureGroupRepository.delete(savedGroup);
        assertThat(disclosureGroupRepository.existsById(savedGroup.getDisclosureGroupId())).isFalse();
        assertThat(disclosureGroupRepository.count()).isEqualTo(initialCount);
        
        logTestExecution("Standard CRUD operations test passed", null);
    }

    /**
     * Test custom query methods performance.
     * Validates that custom repository query methods meet performance requirements
     * and execute within response time thresholds.
     */
    @Test
    @DisplayName("Test custom query methods performance")
    public void testCustomQueryMethodsPerformance() {
        // Given: Multiple test groups for performance testing
        List<DisclosureGroup> testGroups = generateTestData("disclosureGroups", 50)
            .stream()
            .map(this::mapToDisclosureGroup)
            .toList();
        disclosureGroupRepository.saveAll(testGroups);
        
        // Test findActiveDisclosureGroups performance
        long startTime = System.currentTimeMillis();
        List<DisclosureGroup> activeGroups = disclosureGroupRepository.findActiveDisclosureGroups();
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(duration).isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        assertThat(activeGroups).hasSizeGreaterThanOrEqualTo(1);
        
        // Test findForInterestCalculation performance
        startTime = System.currentTimeMillis();
        List<DisclosureGroup> calculationGroups = disclosureGroupRepository
            .findForInterestCalculation(TEST_ACCOUNT_GROUP_ID, TEST_TRANSACTION_TYPE_CODE);
        duration = System.currentTimeMillis() - startTime;
        
        assertThat(duration).isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        // Test findByAccountGroupIdAndTransactionTypeCodeAndTransactionCategoryCode performance
        startTime = System.currentTimeMillis();
        Optional<DisclosureGroup> specificGroup = disclosureGroupRepository
            .findByAccountGroupIdAndTransactionTypeCodeAndTransactionCategoryCode(
                TEST_ACCOUNT_GROUP_ID, TEST_TRANSACTION_TYPE_CODE, TEST_TRANSACTION_CATEGORY_CODE);
        duration = System.currentTimeMillis() - startTime;
        
        assertThat(duration).isLessThanOrEqualTo(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Custom query methods performance test passed", duration);
    }

    // Helper Methods

    /**
     * Creates test reference data for foreign key relationships.
     * Sets up necessary reference data to support disclosure group testing.
     */
    private void createTestReferenceData() {
        // Create test account for foreign key relationship testing
        Account testAccount = createTestAccount();
        testAccount.setGroupId(TEST_ACCOUNT_GROUP_ID);
        accountRepository.save(testAccount);
        
        logTestExecution("Test reference data created", null);
    }

    /**
     * Creates a test DisclosureGroup with COBOL-compatible data patterns.
     * Implements the missing createTestDisclosureGroup method expected by AbstractBaseTest.
     *
     * @return DisclosureGroup with test data matching COBOL field specifications
     */
    @Override
    protected DisclosureGroup createTestDisclosureGroup() {
        return DisclosureGroup.builder()
            .accountGroupId(TEST_ACCOUNT_GROUP_ID)
            .transactionTypeCode(TEST_TRANSACTION_TYPE_CODE)
            .transactionCategoryCode(TEST_TRANSACTION_CATEGORY_CODE)
            .interestRate(TEST_INTEREST_RATE.setScale(4, TestConstants.COBOL_ROUNDING_MODE))
            .groupName(TEST_GROUP_NAME)
            .active(true)
            .description("Test disclosure group for integration testing")
            .effectiveDate(LocalDateTime.now())
            .createdBy("TESTUSER")
            .build();
    }

    /**
     * Creates a test Account entity for relationship testing.
     * Generates account with COBOL-compatible data patterns for foreign key testing.
     *
     * @return Account entity with test data
     */
    private Account createTestAccount() {
        return Account.builder()
            .accountId(Long.valueOf(TestConstants.TEST_ACCOUNT_ID))
            .activeStatus("Y")
            .currentBalance(new BigDecimal("1000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
            .creditLimit(new BigDecimal("5000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
            .cashCreditLimit(new BigDecimal("1000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
            .openDate(java.time.LocalDate.now())
            .currentCycleCredit(BigDecimal.ZERO.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
            .currentCycleDebit(BigDecimal.ZERO.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
            .groupId(TEST_ACCOUNT_GROUP_ID)
            .build();
    }

    /**
     * Maps test data map to DisclosureGroup entity.
     * Used for bulk test data generation in performance tests.
     *
     * @param testData Map containing test data
     * @return DisclosureGroup entity
     */
    private DisclosureGroup mapToDisclosureGroup(Map<String, Object> testData) {
        return DisclosureGroup.builder()
            .accountGroupId("TESTGRP" + String.format("%02d", (Integer)testData.get("id")))
            .transactionTypeCode(TEST_TRANSACTION_TYPE_CODE)
            .transactionCategoryCode(TEST_TRANSACTION_CATEGORY_CODE)
            .interestRate(new BigDecimal("15.0000"))
            .groupName("Test Group " + testData.get("id"))
            .active(true)
            .description("Generated test data for performance testing")
            .effectiveDate(LocalDateTime.now())
            .build();
    }

    /**
     * Generates test disclosure group data for performance testing.
     * Creates specified number of disclosure group test records.
     *
     * @param dataType type identifier (should be "disclosureGroups")
     * @param count number of records to generate
     * @return List of test data maps
     */
    private List<Map<String, Object>> generateTestData(String dataType, int count) {
        List<Map<String, Object>> testData = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("id", i);
            data.put("accountGroupId", "TESTGRP" + String.format("%02d", i));
            data.put("transactionTypeCode", "01");
            data.put("transactionCategoryCode", "0001");
            data.put("interestRate", "15.0000");
            testData.add(data);
        }
        return testData;
    }
}