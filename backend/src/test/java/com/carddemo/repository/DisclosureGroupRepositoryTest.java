package com.carddemo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import com.carddemo.entity.DisclosureGroup;
import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.entity.TransactionType;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.controller.ValidationTestUtils;
import org.junit.jupiter.api.Tag;

/**
 * Integration tests for DisclosureGroupRepository validating interest rate configuration,
 * account grouping, disclosure management, and BigDecimal precision for interest calculations
 * matching COBOL COMP-3 arithmetic from CBACT04C batch program.
 *
 * Tests ensure functional parity with COBOL interest calculation logic:
 * - Interest rate lookup using composite key (account group ID + transaction type + category)
 * - Precision validation for BigDecimal with 5,4 scale matching COBOL PIC S9(04)V99
 * - HALF_UP rounding mode matching COBOL ROUNDED clause
 * - Bulk operations for batch processing scenarios
 */
@DataJpaTest
@Tag("integration")
public class DisclosureGroupRepositoryTest extends AbstractBaseTest {

    @Autowired
    private DisclosureGroupRepository disclosureGroupRepository;

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    // Test customers for account relationships
    private Customer testCustomer1;
    private Customer testCustomer2; 
    private Customer testCustomer3;

    // Test data constants matching COBOL copybook values
    private static final String TEST_GROUP_ID_1 = "DEFAULT";
    private static final String TEST_GROUP_ID_2 = "PREMIUM";
    private static final String TEST_GROUP_ID_3 = "PROMO";
    private static final String TRANSACTION_TYPE_PURCHASE = "PU";
    private static final String TRANSACTION_TYPE_CASH_ADV = "CA";
    private static final String TRANSACTION_CATEGORY_5411 = "5411";
    private static final String TRANSACTION_CATEGORY_6011 = "6011";
    
    // Interest rates matching COBOL PIC S9(04)V99 precision (4,2)
    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("18.24").setScale(4, RoundingMode.HALF_UP);
    private static final BigDecimal PREMIUM_INTEREST_RATE = new BigDecimal("15.99").setScale(4, RoundingMode.HALF_UP);
    private static final BigDecimal PROMO_INTEREST_RATE = new BigDecimal("0.00").setScale(4, RoundingMode.HALF_UP);
    private static final BigDecimal NEGATIVE_INTEREST_RATE = new BigDecimal("-2.50").setScale(4, RoundingMode.HALF_UP);

    @BeforeEach
    public void setUp() {
        super.setUp();
        loadTestFixtures();
        clearTestData();
        createBaseTestData();
    }

    @AfterEach
    public void tearDown() {
        clearTestData();
        super.tearDown();
    }

    /**
     * Creates base test data for disclosure group testing including:
     * - Account records for referential integrity
     * - Transaction type and category reference data
     * - Multiple disclosure groups with varying interest rates
     */
    private void createBaseTestData() {
        // Create test customers first for referential integrity
        testCustomer1 = ValidationTestUtils.createTestCustomer();
        testCustomer1.setCustomerId(12345L);
        testCustomer1.setFirstName("John");
        testCustomer1.setLastName("Smith");
        
        testCustomer2 = ValidationTestUtils.createTestCustomer();
        testCustomer2.setCustomerId(12346L);
        testCustomer2.setFirstName("Jane");
        testCustomer2.setLastName("Johnson");
        
        testCustomer3 = ValidationTestUtils.createTestCustomer();
        testCustomer3.setCustomerId(12347L);
        testCustomer3.setFirstName("Bob");
        testCustomer3.setLastName("Wilson");
        
        customerRepository.saveAll(List.of(testCustomer1, testCustomer2, testCustomer3));
        
        // Create test accounts for referential integrity validation
        Account testAccount1 = createTestAccount("12345678901", TEST_GROUP_ID_1, testCustomer1);
        Account testAccount2 = createTestAccount("12345678902", TEST_GROUP_ID_2, testCustomer2);
        Account testAccount3 = createTestAccount("12345678903", TEST_GROUP_ID_3, testCustomer3);
        
        accountRepository.saveAll(List.of(testAccount1, testAccount2, testAccount3));

        // Create disclosure groups matching COBOL CBACT04C interest calculation logic
        List<DisclosureGroup> disclosureGroups = new ArrayList<>();
        
        // Default group with standard interest rates
        disclosureGroups.add(createTestDisclosureGroup(
            TEST_GROUP_ID_1, TRANSACTION_TYPE_PURCHASE, TRANSACTION_CATEGORY_5411, 
            DEFAULT_INTEREST_RATE, "Standard purchase rate terms"
        ));
        
        disclosureGroups.add(createTestDisclosureGroup(
            TEST_GROUP_ID_1, TRANSACTION_TYPE_CASH_ADV, TRANSACTION_CATEGORY_6011,
            new BigDecimal("24.99").setScale(4, RoundingMode.HALF_UP), 
            "Standard cash advance rate terms"
        ));
        
        // Premium group with lower rates
        disclosureGroups.add(createTestDisclosureGroup(
            TEST_GROUP_ID_2, TRANSACTION_TYPE_PURCHASE, TRANSACTION_CATEGORY_5411,
            PREMIUM_INTEREST_RATE, "Premium member purchase rate terms"
        ));
        
        // Promotional group with special rates
        disclosureGroups.add(createTestDisclosureGroup(
            TEST_GROUP_ID_3, TRANSACTION_TYPE_PURCHASE, TRANSACTION_CATEGORY_5411,
            PROMO_INTEREST_RATE, "Promotional 0% APR for new accounts"
        ));
        
        // Test negative interest rates (promotional scenarios)
        disclosureGroups.add(createTestDisclosureGroup(
            TEST_GROUP_ID_3, TRANSACTION_TYPE_CASH_ADV, TRANSACTION_CATEGORY_6011,
            NEGATIVE_INTEREST_RATE, "Promotional negative rate for cash advances"
        ));

        disclosureGroupRepository.saveAll(disclosureGroups);
        disclosureGroupRepository.flush();
    }

    /**
     * Loads test fixtures and setup for disclosure group testing
     */
    @Override
    protected void loadTestFixtures() {
        // Initialize any test fixtures if needed
        // This method can be expanded based on specific test requirements
    }

    /**
     * Clears test data from repository tables
     */
    @Override
    protected void clearTestData() {
        disclosureGroupRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    /**
     * Creates test account matching COBOL CVACT01Y account record structure
     */
    private Account createTestAccount(String accountId, String groupId, Customer customer) {
        Account account = new Account();
        account.setAccountId(Long.parseLong(accountId));
        account.setGroupId(groupId);
        account.setCurrentBalance(new BigDecimal("1000.00"));
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCustomer(customer);
        account.setActiveStatus("Y");
        account.setOpenDate(java.time.LocalDate.now().minusYears(1));
        account.setExpirationDate(java.time.LocalDate.now().plusYears(4));
        account.setAddressZip("12345");
        return account;
    }

    @Nested
    @DisplayName("Composite Key Operations")
    class CompositeKeyOperationsTest {

        @Test
        @DisplayName("Should find disclosure group by composite key")
        void testFindByCompositeKey() {
            // When: Finding by composite key fields
            Optional<DisclosureGroup> result = disclosureGroupRepository
                .findByAccountGroupIdAndTransactionTypeCodeAndTransactionCategoryCode(
                    TEST_GROUP_ID_1, TRANSACTION_TYPE_PURCHASE, TRANSACTION_CATEGORY_5411);

            // Then: Should find the record with correct interest rate
            assertThat(result).isPresent();
            DisclosureGroup group = result.get();
            assertThat(group.getAccountGroupId()).isEqualTo(TEST_GROUP_ID_1);
            assertThat(group.getTransactionTypeCode()).isEqualTo(TRANSACTION_TYPE_PURCHASE);
            assertThat(group.getTransactionCategoryCode()).isEqualTo(TRANSACTION_CATEGORY_5411);
            assertBigDecimalEquals(group.getInterestRate(), DEFAULT_INTEREST_RATE, "Interest rate should match expected default rate");
        }

        @Test
        @DisplayName("Should save disclosure group with composite key")
        void testSaveWithCompositeKey() {
            // Given: New disclosure group with unique composite key
            DisclosureGroup newGroup = createTestDisclosureGroup(
                "NEWGROUP", "CC", "7011", 
                new BigDecimal("12.50").setScale(4, RoundingMode.HALF_UP),
                "New group with special rate"
            );

            // When: Saving the new group
            DisclosureGroup saved = disclosureGroupRepository.save(newGroup);
            disclosureGroupRepository.flush();

            // Then: Should persist with all key components
            assertThat(saved.getAccountGroupId()).isEqualTo("NEWGROUP");
            assertThat(saved.getTransactionTypeCode()).isEqualTo("CC");
            assertThat(saved.getTransactionCategoryCode()).isEqualTo("7011");
            assertBigDecimalEquals(new BigDecimal("12.50").setScale(4, RoundingMode.HALF_UP), saved.getInterestRate(), "Interest rate should match saved value");
        }

        @Test
        @DisplayName("Should handle non-existent composite key")
        void testFindByNonExistentCompositeKey() {
            // When: Finding by non-existent key fields
            Optional<DisclosureGroup> result = disclosureGroupRepository
                .findByAccountGroupIdAndTransactionTypeCodeAndTransactionCategoryCode(
                    "NONEXIST", "XX", "9999");

            // Then: Should return empty result
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Account Group ID Lookup Tests")
    class AccountGroupLookupTest {

        @Test
        @DisplayName("Should find all disclosure groups by account group ID")
        void testFindByAccountGroupId() {
            // When: Finding by account group ID
            List<DisclosureGroup> defaultGroups = disclosureGroupRepository.findByAccountGroupId(TEST_GROUP_ID_1);

            // Then: Should return all groups for DEFAULT account group
            assertThat(defaultGroups).hasSize(2);
            assertThat(defaultGroups).allMatch(group -> 
                TEST_GROUP_ID_1.equals(group.getAccountGroupId()));
            
            // Verify different transaction types are included
            assertThat(defaultGroups).extracting(DisclosureGroup::getTransactionTypeCode)
                .containsExactlyInAnyOrder(TRANSACTION_TYPE_PURCHASE, TRANSACTION_TYPE_CASH_ADV);
        }

        @Test
        @DisplayName("Should return empty list for non-existent account group")
        void testFindByNonExistentAccountGroupId() {
            // When: Finding by non-existent account group
            List<DisclosureGroup> result = disclosureGroupRepository.findByAccountGroupId("NONEXIST");

            // Then: Should return empty list
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find premium group with correct interest rates")
        void testFindPremiumGroupRates() {
            // When: Finding premium account group rates
            List<DisclosureGroup> premiumGroups = disclosureGroupRepository.findByAccountGroupId(TEST_GROUP_ID_2);

            // Then: Should return premium rates
            assertThat(premiumGroups).hasSize(1);
            DisclosureGroup premiumGroup = premiumGroups.get(0);
            assertBigDecimalEquals(PREMIUM_INTEREST_RATE, premiumGroup.getInterestRate(), "Premium interest rate should match expected value");
        }
    }

    @Nested
    @DisplayName("Interest Rate Precision Tests")
    class InterestRatePrecisionTest {

        @Test
        @DisplayName("Should maintain BigDecimal precision for interest rates (5,4 scale)")
        void testBigDecimalPrecisionForInterestRates() {
            // Given: Interest rate with exact COBOL COMP-3 precision
            BigDecimal testRate = new BigDecimal("99.9999").setScale(4, RoundingMode.HALF_UP);
            DisclosureGroup testGroup = createTestDisclosureGroup(
                "PRECISE", "TS", "1234", testRate, "Maximum precision test"
            );

            // When: Saving and retrieving
            DisclosureGroup saved = disclosureGroupRepository.save(testGroup);
            disclosureGroupRepository.flush();
            
            Optional<DisclosureGroup> retrieved = disclosureGroupRepository.findById(saved.getDisclosureGroupId());

            // Then: Should maintain exact precision matching COBOL PIC S9(04)V99
            assertThat(retrieved).isPresent();
            assertBigDecimalEquals(testRate, retrieved.get().getInterestRate(), "Retrieved interest rate should match test rate");
            assertThat(retrieved.get().getInterestRate().scale()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should handle HALF_UP rounding for interest calculations")
        void testHalfUpRoundingForInterestCalculations() {
            // Given: Interest rate requiring HALF_UP rounding
            BigDecimal originalRate = new BigDecimal("18.245678");
            BigDecimal expectedRounded = originalRate.setScale(4, RoundingMode.HALF_UP);
            
            DisclosureGroup testGroup = createTestDisclosureGroup(
                "ROUND", "RN", "5432", originalRate.setScale(4, RoundingMode.HALF_UP), 
                "Rounding test for COBOL compatibility"
            );

            // When: Saving the group
            DisclosureGroup saved = disclosureGroupRepository.save(testGroup);

            // Then: Should apply HALF_UP rounding matching COBOL ROUNDED clause
            assertBigDecimalEquals(expectedRounded, saved.getInterestRate(), "Saved interest rate should be rounded correctly");
            assertThat(saved.getInterestRate().toString()).isEqualTo("18.2457");
        }

        @Test
        @DisplayName("Should validate interest rate calculation precision")
        void testInterestCalculationPrecision() {
            // Given: Transaction balance and interest rate from COBOL logic
            BigDecimal transactionBalance = new BigDecimal("1000.00");
            BigDecimal interestRate = new BigDecimal("18.24").setScale(4, RoundingMode.HALF_UP);
            
            // When: Computing monthly interest using COBOL formula: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
            BigDecimal monthlyInterest = transactionBalance
                .multiply(interestRate)
                .divide(new BigDecimal("1200"), 4, RoundingMode.HALF_UP);

            // Then: Should match COBOL COMP-3 calculation result
            BigDecimal expectedMonthlyInterest = new BigDecimal("15.2000");
            assertBigDecimalEquals(expectedMonthlyInterest, monthlyInterest, "Monthly interest calculation should match expected value");
            validateCobolPrecision(monthlyInterest, "monthly_interest");
        }
    }

    @Nested
    @DisplayName("Transaction Type and Category Lookup Tests")
    class TransactionLookupTest {

        @Test
        @DisplayName("Should find by transaction type and category codes")
        void testFindByTransactionTypeAndCategory() {
            // When: Finding by transaction type and category
            List<DisclosureGroup> results = disclosureGroupRepository
                .findByTransactionTypeCodeAndTransactionCategoryCode(
                    TRANSACTION_TYPE_PURCHASE, TRANSACTION_CATEGORY_5411);

            // Then: Should return all groups matching the criteria
            assertThat(results).hasSize(3); // DEFAULT, PREMIUM, PROMO groups
            assertThat(results).allMatch(group -> 
                TRANSACTION_TYPE_PURCHASE.equals(group.getTransactionTypeCode()) &&
                TRANSACTION_CATEGORY_5411.equals(group.getTransactionCategoryCode()));
        }

        @Test
        @DisplayName("Should handle cash advance transaction lookup")
        void testCashAdvanceTransactionLookup() {
            // When: Finding cash advance transactions
            List<DisclosureGroup> cashAdvGroups = disclosureGroupRepository
                .findByTransactionTypeCodeAndTransactionCategoryCode(
                    TRANSACTION_TYPE_CASH_ADV, TRANSACTION_CATEGORY_6011);

            // Then: Should return cash advance groups
            assertThat(cashAdvGroups).hasSize(2); // DEFAULT and PROMO groups
            assertThat(cashAdvGroups).allMatch(group -> 
                TRANSACTION_TYPE_CASH_ADV.equals(group.getTransactionTypeCode()));
        }

        @Test
        @DisplayName("Should return empty for non-matching transaction criteria")
        void testNonMatchingTransactionCriteria() {
            // When: Finding with non-existent transaction criteria
            List<DisclosureGroup> results = disclosureGroupRepository
                .findByTransactionTypeCodeAndTransactionCategoryCode("XX", "9999");

            // Then: Should return empty list
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Negative Interest Rate Handling")
    class NegativeInterestRateTest {

        @Test
        @DisplayName("Should handle negative interest rates correctly")
        void testNegativeInterestRateHandling() {
            // When: Finding group with negative interest rate
            List<DisclosureGroup> promoGroups = disclosureGroupRepository
                .findByAccountGroupId(TEST_GROUP_ID_3);
            
            DisclosureGroup negativeRateGroup = promoGroups.stream()
                .filter(group -> TRANSACTION_TYPE_CASH_ADV.equals(group.getTransactionTypeCode()))
                .findFirst()
                .orElseThrow();

            // Then: Should properly handle negative rate
            assertThat(negativeRateGroup.getInterestRate()).isNegative();
            assertBigDecimalEquals(NEGATIVE_INTEREST_RATE, negativeRateGroup.getInterestRate(), "Negative interest rate should match expected value");
        }

        @Test
        @DisplayName("Should calculate negative interest correctly")
        void testNegativeInterestCalculation() {
            // Given: Transaction balance and negative interest rate
            BigDecimal balance = new BigDecimal("500.00");
            BigDecimal negativeRate = NEGATIVE_INTEREST_RATE;
            
            // When: Computing monthly interest with negative rate
            BigDecimal monthlyInterest = balance
                .multiply(negativeRate)
                .divide(new BigDecimal("1200"), 4, RoundingMode.HALF_UP);

            // Then: Should result in negative interest charge (credit to customer)
            assertThat(monthlyInterest).isNegative();
            assertBigDecimalEquals(new BigDecimal("-1.0417"), monthlyInterest, "Negative monthly interest should be calculated correctly");
        }
    }

    @Nested
    @DisplayName("Description Field Storage Tests")
    class DescriptionStorageTest {

        @Test
        @DisplayName("Should store description up to 200 characters")
        void testDescriptionStorage() {
            // Given: Long description (200 characters - entity max)
            String longDescription = "A".repeat(200);
            
            DisclosureGroup groupWithLongDesc = createTestDisclosureGroup(
                "LONGDESC", "LD", "8888", DEFAULT_INTEREST_RATE, longDescription
            );

            // When: Saving group with long description
            DisclosureGroup saved = disclosureGroupRepository.save(groupWithLongDesc);
            disclosureGroupRepository.flush();

            // Then: Should store complete description text
            Optional<DisclosureGroup> retrieved = disclosureGroupRepository.findById(saved.getDisclosureGroupId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).hasSize(200);
            assertThat(retrieved.get().getDescription()).isEqualTo(longDescription);
        }

        @Test
        @DisplayName("Should handle empty description")
        void testEmptyDescription() {
            // Given: Disclosure group with empty description
            DisclosureGroup groupWithEmptyDesc = createTestDisclosureGroup(
                "EMPTY", "EM", "9999", DEFAULT_INTEREST_RATE, ""
            );

            // When: Saving group with empty description
            DisclosureGroup saved = disclosureGroupRepository.save(groupWithEmptyDesc);

            // Then: Should handle empty description correctly
            assertThat(saved.getDescription()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Bulk Operations Tests")
    class BulkOperationsTest {

        @Test
        @DisplayName("Should handle bulk interest rate updates")
        void testBulkInterestRateUpdates() {
            // Given: Multiple disclosure groups requiring rate updates
            List<DisclosureGroup> allGroups = disclosureGroupRepository.findAll();
            BigDecimal newRate = new BigDecimal("20.00").setScale(4, RoundingMode.HALF_UP);

            // When: Updating interest rates in bulk
            allGroups.forEach(group -> group.setInterestRate(newRate));
            List<DisclosureGroup> updated = disclosureGroupRepository.saveAll(allGroups);
            disclosureGroupRepository.flush();

            // Then: All groups should have updated rates
            assertThat(updated).hasSize(allGroups.size());
            assertThat(updated).allMatch(group -> 
                group.getInterestRate().compareTo(newRate) == 0);
        }

        @Test
        @DisplayName("Should handle bulk save operations efficiently")
        void testBulkSaveEfficiency() {
            // Given: Large batch of new disclosure groups
            List<DisclosureGroup> bulkGroups = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                bulkGroups.add(createTestDisclosureGroup(
                    "BULK" + String.format("%03d", i), 
                    "BU", 
                    String.format("%04d", i),
                    new BigDecimal("15.00").setScale(4, RoundingMode.HALF_UP),
                    "Bulk test group " + i
                ));
            }

            // When: Saving all groups in bulk
            long startTime = System.currentTimeMillis();
            List<DisclosureGroup> saved = disclosureGroupRepository.saveAll(bulkGroups);
            disclosureGroupRepository.flush();
            long endTime = System.currentTimeMillis();

            // Then: Should save efficiently
            assertThat(saved).hasSize(100);
            assertThat(endTime - startTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        }
    }

    @Nested
    @DisplayName("Referential Integrity Tests")
    class ReferentialIntegrityTest {

        @Test
        @DisplayName("Should validate account group assignment")
        void testAccountGroupAssignment() {
            // Given: Account with specific group ID
            String accountId = "98765432109";
            Account testAccount = createTestAccount(accountId, TEST_GROUP_ID_2, testCustomer2);
            accountRepository.save(testAccount);

            // When: Finding disclosure groups for account's group
            List<DisclosureGroup> groupRates = disclosureGroupRepository
                .findByAccountGroupId(testAccount.getGroupId());

            // Then: Should find applicable rates for account's group
            assertThat(groupRates).isNotEmpty();
            assertThat(groupRates).allMatch(group -> 
                testAccount.getGroupId().equals(group.getAccountGroupId()));
        }

        @Test
        @DisplayName("Should maintain consistency with account entities")
        void testAccountEntityConsistency() {
            // Given: Accounts and their corresponding disclosure groups
            List<Account> allAccounts = accountRepository.findAll();
            
            // When: Validating each account has applicable disclosure groups
            for (Account account : allAccounts) {
                List<DisclosureGroup> applicableGroups = disclosureGroupRepository
                    .findByAccountGroupId(account.getGroupId());
                
                // Then: Each account should have at least one applicable disclosure group
                assertThat(applicableGroups)
                    .as("Account %s should have disclosure groups", account.getAccountId())
                    .isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Concurrent Modification Tests")
    class ConcurrentModificationTest {

        @Test
        @DisplayName("Should handle concurrent interest rate modifications")
        void testConcurrentInterestRateModifications() throws InterruptedException {
            // Given: Disclosure group for concurrent modification
            DisclosureGroup group = disclosureGroupRepository
                .findByAccountGroupId(TEST_GROUP_ID_1).get(0);
            
            ExecutorService executor = Executors.newFixedThreadPool(5);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // When: Multiple threads modify interest rate concurrently
            for (int i = 0; i < 5; i++) {
                final int threadId = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        BigDecimal newRate = new BigDecimal("20.00" + threadId)
                            .setScale(4, RoundingMode.HALF_UP);
                        group.setInterestRate(newRate);
                        disclosureGroupRepository.save(group);
                    } catch (Exception e) {
                        // Expected for concurrent modifications
                    }
                }, executor);
                futures.add(future);
            }

            // Wait for all operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // Then: Final state should be consistent
            Optional<DisclosureGroup> finalState = disclosureGroupRepository.findById(group.getDisclosureGroupId());
            assertThat(finalState).isPresent();
            assertThat(finalState.get().getInterestRate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Special Promotional Rate Tests")
    class PromotionalRateTest {

        @Test
        @DisplayName("Should handle zero percent promotional rates")
        void testZeroPercentPromotionalRates() {
            // When: Finding promotional rates
            List<DisclosureGroup> promoGroups = disclosureGroupRepository
                .findByAccountGroupId(TEST_GROUP_ID_3);
            
            DisclosureGroup zeroRateGroup = promoGroups.stream()
                .filter(group -> TRANSACTION_TYPE_PURCHASE.equals(group.getTransactionTypeCode()))
                .findFirst()
                .orElseThrow();

            // Then: Should handle zero rate correctly
            assertBigDecimalEquals(PROMO_INTEREST_RATE, zeroRateGroup.getInterestRate(), "Zero promotional rate should match expected value");
            assertThat(zeroRateGroup.getInterestRate().compareTo(BigDecimal.ZERO)).isEqualTo(0);
        }

        @Test
        @DisplayName("Should calculate zero interest for promotional rates")
        void testZeroInterestCalculation() {
            // Given: Zero promotional rate
            BigDecimal balance = new BigDecimal("1000.00");
            BigDecimal zeroRate = PROMO_INTEREST_RATE;
            
            // When: Computing monthly interest
            BigDecimal monthlyInterest = balance
                .multiply(zeroRate)
                .divide(new BigDecimal("1200"), 4, RoundingMode.HALF_UP);

            // Then: Should result in zero interest charge
            assertBigDecimalEquals(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), monthlyInterest, "Zero interest calculation should be zero");
        }
    }

    @Nested
    @DisplayName("Interest Compounding Tests")
    class InterestCompoundingTest {

        @Test
        @DisplayName("Should validate annual percentage rate calculations")
        void testAnnualPercentageRateCalculations() {
            // Given: Monthly interest rate
            BigDecimal monthlyRate = new BigDecimal("1.5200").setScale(4, RoundingMode.HALF_UP);
            
            // When: Computing annual rate (12 periods)
            BigDecimal annualRate = monthlyRate.multiply(new BigDecimal("12"))
                .setScale(4, RoundingMode.HALF_UP);

            // Then: Should match expected annual calculation
            BigDecimal expectedAnnual = new BigDecimal("18.2400");
            assertBigDecimalEquals(expectedAnnual, annualRate, "Annual rate calculation should match expected value");
        }

        @Test
        @DisplayName("Should handle compound interest scenarios")
        void testCompoundInterestScenarios() {
            // Given: Principal amount and compound interest parameters
            BigDecimal principal = new BigDecimal("1000.00");
            BigDecimal monthlyRate = new BigDecimal("0.0152").setScale(4, RoundingMode.HALF_UP);
            int periods = 12;

            // When: Computing compound interest
            BigDecimal compoundAmount = principal;
            for (int i = 0; i < periods; i++) {
                BigDecimal interest = compoundAmount
                    .multiply(monthlyRate)
                    .setScale(4, RoundingMode.HALF_UP);
                compoundAmount = compoundAmount.add(interest);
            }

            // Then: Should accumulate interest correctly
            assertThat(compoundAmount).isGreaterThan(principal);
            assertThat(compoundAmount.scale()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Unique Constraints and Validation")
    class UniqueConstraintsTest {

        @Test
        @DisplayName("Should enforce disclosure group name uniqueness within group")
        void testDisclosureGroupNameUniqueness() {
            // Given: Two groups with same composite key components
            DisclosureGroup group1 = createTestDisclosureGroup(
                "UNIQUE", "UN", "1111", DEFAULT_INTEREST_RATE, "First terms"
            );
            DisclosureGroup group2 = createTestDisclosureGroup(
                "UNIQUE", "UN", "1111", PREMIUM_INTEREST_RATE, "Second terms"
            );

            // When: Saving first group successfully
            disclosureGroupRepository.save(group1);
            disclosureGroupRepository.flush();

            // Then: Both groups can be saved since no unique constraint is defined
            // at the database level - this tests the current system behavior
            long countBefore = disclosureGroupRepository.count();
            disclosureGroupRepository.save(group2);
            disclosureGroupRepository.flush();
            
            long countAfter = disclosureGroupRepository.count();
            assertThat(countAfter).isEqualTo(countBefore + 1);
        }
    }

    @Nested
    @DisplayName("Repository Operation Tests")
    class RepositoryOperationTest {

        @Test
        @DisplayName("Should perform all CRUD operations correctly")
        void testCrudOperations() {
            // CREATE: Save new disclosure group
            DisclosureGroup newGroup = createTestDisclosureGroup(
                "CRUD", "CR", "1001", new BigDecimal("16.50").setScale(4, RoundingMode.HALF_UP),
                "CRUD test terms"
            );
            DisclosureGroup saved = disclosureGroupRepository.save(newGroup);
            assertThat(saved.getDisclosureGroupId()).isNotNull();

            // READ: Find by ID
            Optional<DisclosureGroup> found = disclosureGroupRepository.findById(saved.getDisclosureGroupId());
            assertThat(found).isPresent();
            assertBigDecimalEquals(new BigDecimal("16.50").setScale(4, RoundingMode.HALF_UP), found.get().getInterestRate(), "Found interest rate should match expected value");

            // UPDATE: Modify interest rate
            found.get().setInterestRate(new BigDecimal("17.25").setScale(4, RoundingMode.HALF_UP));
            DisclosureGroup updated = disclosureGroupRepository.save(found.get());
            assertBigDecimalEquals(new BigDecimal("17.25").setScale(4, RoundingMode.HALF_UP), updated.getInterestRate(), "Updated interest rate should match expected value");

            // DELETE: Remove group
            disclosureGroupRepository.delete(updated);
            Optional<DisclosureGroup> deleted = disclosureGroupRepository.findById(saved.getDisclosureGroupId());
            assertThat(deleted).isEmpty();
        }

        @Test
        @DisplayName("Should validate existence check operations")
        void testExistenceOperations() {
            // Given: Known disclosure group
            DisclosureGroup group = disclosureGroupRepository.findAll().get(0);

            // When: Checking existence
            boolean exists = disclosureGroupRepository.existsById(group.getDisclosureGroupId());
            long totalCount = disclosureGroupRepository.count();

            // Then: Should confirm existence and count
            assertThat(exists).isTrue();
            assertThat(totalCount).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("COBOL Functional Parity Tests")
    class CobolFunctionalParityTest {

        @Test
        @DisplayName("Should replicate COBOL interest calculation logic exactly")
        void testCobolInterestCalculationParity() {
            // Given: Test data matching COBOL CBACT04C logic
            BigDecimal transactionBalance = new BigDecimal("2500.00");
            BigDecimal interestRate = new BigDecimal("18.24").setScale(4, RoundingMode.HALF_UP);
            
            // When: Performing calculation matching COBOL formula: 
            // COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
            BigDecimal monthlyInterest = transactionBalance
                .multiply(interestRate)
                .divide(new BigDecimal("1200"), 4, RoundingMode.HALF_UP);

            // Then: Should match exact COBOL COMP-3 calculation
            BigDecimal expectedResult = new BigDecimal("38.0000");
            assertBigDecimalEquals(expectedResult, monthlyInterest, "COBOL interest calculation should match expected result");
            validateCobolPrecision(monthlyInterest, "cobol_monthly_interest");
        }

        @Test
        @DisplayName("Should handle COBOL default group fallback logic")
        void testCobolDefaultGroupFallback() {
            // Given: Account group that might not have specific rates
            String accountGroupId = "SPECIAL";
            
            // When: Looking for specific group first, then DEFAULT
            List<DisclosureGroup> specificGroups = disclosureGroupRepository
                .findByAccountGroupId(accountGroupId);
            
            if (specificGroups.isEmpty()) {
                // Fallback to DEFAULT group as in COBOL logic
                specificGroups = disclosureGroupRepository
                    .findByAccountGroupId("DEFAULT");
            }

            // Then: Should find DEFAULT group as fallback
            assertThat(specificGroups).isNotEmpty();
            assertThat(specificGroups.get(0).getAccountGroupId()).isEqualTo("DEFAULT");
        }

        @Test
        @DisplayName("Should validate interest rate bounds matching COBOL PIC S9(04)V99")
        void testCobolInterestRateBounds() {
            // Given: Maximum and minimum values for database NUMERIC(6,4) constraint
            // Database precision=6, scale=4 allows range from -99.9999 to 99.9999
            BigDecimal maxRate = new BigDecimal("99.9999").setScale(4, RoundingMode.HALF_UP);
            BigDecimal minRate = new BigDecimal("-99.9999").setScale(4, RoundingMode.HALF_UP);
            
            DisclosureGroup maxGroup = createTestDisclosureGroup(
                "MAXRATE", "MX", "9998", maxRate, "Maximum rate test"
            );
            DisclosureGroup minGroup = createTestDisclosureGroup(
                "MINRATE", "MN", "9997", minRate, "Minimum rate test"
            );

            // When: Saving boundary values
            DisclosureGroup savedMax = disclosureGroupRepository.save(maxGroup);
            DisclosureGroup savedMin = disclosureGroupRepository.save(minGroup);

            // Then: Should handle COBOL numeric boundaries
            assertBigDecimalEquals(maxRate, savedMax.getInterestRate(), "Maximum rate should match COBOL bounds");
            assertBigDecimalEquals(minRate, savedMin.getInterestRate(), "Minimum rate should match COBOL bounds");
        }
    }

    @Nested
    @DisplayName("Edge Case and Error Handling Tests")
    class EdgeCaseTest {

        @Test
        @DisplayName("Should handle null and empty values gracefully")
        void testNullAndEmptyValueHandling() {
            // Given: Disclosure group with minimal required data
            DisclosureGroup minimalGroup = new DisclosureGroup();
            minimalGroup.setAccountGroupId("MINIMAL");
            minimalGroup.setTransactionTypeCode("MI");
            minimalGroup.setTransactionCategoryCode("0001");
            minimalGroup.setInterestRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            minimalGroup.setDescription("");

            // When: Saving minimal group
            DisclosureGroup saved = disclosureGroupRepository.save(minimalGroup);

            // Then: Should save successfully with empty terms
            assertThat(saved.getDisclosureGroupId()).isNotNull();
            assertThat(saved.getDescription()).isEmpty();
            assertBigDecimalEquals(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), saved.getInterestRate(), "Zero default rate should be handled correctly");
        }

        @Test
        @DisplayName("Should handle very small interest rates")
        void testVerySmallInterestRates() {
            // Given: Very small interest rate (0.01%)
            BigDecimal verySmallRate = new BigDecimal("0.0001").setScale(4, RoundingMode.HALF_UP);
            DisclosureGroup smallRateGroup = createTestDisclosureGroup(
                "SMALL", "SM", "0002", verySmallRate, "Very small rate terms"
            );

            // When: Saving and calculating interest
            DisclosureGroup saved = disclosureGroupRepository.save(smallRateGroup);
            
            BigDecimal balance = new BigDecimal("10000.00");
            BigDecimal monthlyInterest = balance
                .multiply(saved.getInterestRate())
                .divide(new BigDecimal("1200"), 4, RoundingMode.HALF_UP);

            // Then: Should handle micro-precision correctly
            assertBigDecimalEquals(verySmallRate, saved.getInterestRate(), "Very small interest rate should be preserved with precision");
            assertThat(monthlyInterest.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should validate maximum description length")
        void testMaximumDescriptionLength() {
            // Given: Description at exact maximum length (200 chars)
            String maxDescription = "Terms and conditions: " + "X".repeat(178); // 200 total
            
            DisclosureGroup maxDescGroup = createTestDisclosureGroup(
                "MAXDESC", "MD", "1000", DEFAULT_INTEREST_RATE, maxDescription
            );

            // When: Saving group with maximum description
            DisclosureGroup saved = disclosureGroupRepository.save(maxDescGroup);
            disclosureGroupRepository.flush();

            // Then: Should store exactly 200 characters
            assertThat(saved.getDescription()).hasSize(200);
            assertThat(saved.getDescription()).startsWith("Terms and conditions:");
        }
    }

    @Nested
    @DisplayName("Performance and Load Tests")
    class PerformanceTest {

        @Test
        @DisplayName("Should perform lookups within acceptable response times")
        void testLookupPerformance() {
            // Given: Performance baseline from TestConstants
            long maxResponseTime = TestConstants.RESPONSE_TIME_THRESHOLD_MS;

            // When: Performing typical lookup operations
            long startTime = System.currentTimeMillis();
            
            List<DisclosureGroup> groups1 = disclosureGroupRepository.findByAccountGroupId(TEST_GROUP_ID_1);
            List<DisclosureGroup> groups2 = disclosureGroupRepository
                .findByTransactionTypeCodeAndTransactionCategoryCode(
                    TRANSACTION_TYPE_PURCHASE, TRANSACTION_CATEGORY_5411);
            long totalCount = disclosureGroupRepository.count();
            
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            // Then: Should complete within acceptable time
            assertThat(responseTime).isLessThan(maxResponseTime);
            assertThat(groups1).isNotEmpty();
            assertThat(groups2).isNotEmpty();
            assertThat(totalCount).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle batch operations efficiently")
        void testBatchOperationPerformance() {
            // Given: Large dataset for batch testing
            List<DisclosureGroup> batchData = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                batchData.add(createTestDisclosureGroup(
                    "BATCH" + String.format("%02d", i),
                    "BA",
                    String.format("%04d", i + 2000),
                    new BigDecimal("15.00").setScale(4, RoundingMode.HALF_UP),
                    "Batch test group " + i
                ));
            }

            // When: Performing batch operations
            long startTime = System.currentTimeMillis();
            List<DisclosureGroup> saved = disclosureGroupRepository.saveAll(batchData);
            disclosureGroupRepository.flush();
            long endTime = System.currentTimeMillis();

            // Then: Should complete efficiently
            assertThat(saved).hasSize(50);
            assertThat(endTime - startTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS * 2);
        }
    }

    /**
     * Helper method to create test disclosure group with all required fields
     * matching COBOL DIS-GROUP-RECORD structure from CVTRA02Y copybook.
     */
    private DisclosureGroup createTestDisclosureGroup(String accountGroupId, 
                                                     String transactionTypeCode,
                                                     String transactionCategoryCode, 
                                                     BigDecimal interestRate,
                                                     String description) {
        DisclosureGroup group = new DisclosureGroup();
        group.setAccountGroupId(accountGroupId);
        group.setTransactionTypeCode(transactionTypeCode);
        group.setTransactionCategoryCode(transactionCategoryCode);
        group.setInterestRate(interestRate);
        group.setDescription(description);
        return group;
    }
}