/*
 * CardDemo Application
 * 
 * TransactionCategoryRepository Integration Test
 * 
 * Comprehensive Spring Data JPA repository test validating transaction category
 * management operations including composite key handling, hierarchical relationships,
 * reference data caching, and database migration from VSAM to PostgreSQL.
 * 
 * Tests COBOL-to-Java functional parity for transaction categorization operations
 * originally handled by VSAM TRANCATG reference file with CVTRA04Y.cpy structure.
 * 
 * Key Test Areas:
 * - Composite primary key operations using @EmbeddedId
 * - Category lookup methods and performance
 * - Cache behavior validation for reference data
 * - Hierarchical category relationships
 * - Transaction type integration
 * - CRUD operations and data integrity
 * - Concurrent access patterns
 * - Invalid data handling and error cases
 * - Performance validation meeting COBOL equivalent response times
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */

package com.carddemo.repository;

import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionCategory.TransactionCategoryId;
import com.carddemo.entity.TransactionType;
import com.carddemo.test.IntegrationTest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Integration test class for TransactionCategoryRepository validating:
 * - Category management with composite keys
 * - Hierarchical category relationships  
 * - Reference data caching for transaction categorization
 * - Database migration from VSAM to PostgreSQL
 * - COBOL functional parity validation
 * - Performance requirements validation
 * 
 * Uses Testcontainers for PostgreSQL database isolation and Spring Boot
 * test slice configuration for JPA repository testing.
 */
@DataJpaTest
@Transactional
@EnableCaching
class TransactionCategoryRepositoryTest implements IntegrationTest {

    // Test infrastructure constants
    private static final long RESPONSE_TIME_THRESHOLD_MS = 200L;
    private static final int COBOL_DECIMAL_SCALE = 2;
    private static final String TEST_TRANSACTION_CATEGORY_CODE = "FOOD";
    private static final String TEST_TRANSACTION_TYPE_CODE = "01"; 
    private static final long CACHE_PERFORMANCE_THRESHOLD_MS = 50L;

    @Autowired
    private TransactionCategoryRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Test composite key queries using @EmbeddedId for category lookups.
     * Validates proper handling of TransactionCategoryId composite primary key
     * matching COBOL CVTRA04Y.cpy key structure with category and subcategory codes.
     */
    @Test
    void testCompositeKeyQueries() {
        // Create test data with composite keys
        TransactionCategory category1 = createTestTransactionCategory("FOOD", "01", "01", 
            "Restaurant Purchases", "Restaurant");
        TransactionCategory category2 = createTestTransactionCategory("FOOD", "02", "01", 
            "Grocery Store Purchases", "Grocery");
        TransactionCategory category3 = createTestTransactionCategory("GAS", "01", "02", 
            "Fuel Station Purchases", "Fuel");

        // Save test entities
        repository.save(category1);
        repository.save(category2);  
        repository.save(category3);
        repository.flush();

        // Test findById with composite key
        TransactionCategoryId compositeId = new TransactionCategoryId("FOOD", "01");
        Optional<TransactionCategory> found = repository.findById(compositeId);
        
        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().getCategoryCode()).isEqualTo("FOOD");
        Assertions.assertThat(found.get().getSubcategoryCode()).isEqualTo("01");
        Assertions.assertThat(found.get().getCategoryDescription()).isEqualTo("Restaurant Purchases");

        // Test findByIdCategoryCode for hierarchical queries
        List<TransactionCategory> foodCategories = repository.findByIdCategoryCode("FOOD");
        Assertions.assertThat(foodCategories).hasSize(2);
        Assertions.assertThat(foodCategories).extracting(TransactionCategory::getSubcategoryCode)
            .containsExactlyInAnyOrder("01", "02");

        // Test findByIdCategoryCodeAndIdSubcategoryCode for exact match
        Optional<TransactionCategory> exactMatch = repository.findByIdCategoryCodeAndIdSubcategoryCode("GAS", "01");
        Assertions.assertThat(exactMatch).isPresent();
        Assertions.assertThat(exactMatch.get().getCategoryDescription()).isEqualTo("Fuel Station Purchases");
    }

    /**
     * Validate findByCategoryCode for category lookups and hierarchical relationships.
     * Tests category hierarchy navigation supporting parent-child category structures
     * with proper subcategory code handling.
     */
    @Test
    void testCategoryLookupAndHierarchy() {
        // Create hierarchical category structure
        TransactionCategory parentCategory = createTestTransactionCategory("SHOP", "00", "01", 
            "Shopping - General", "Shopping");
        TransactionCategory childCategory1 = createTestTransactionCategory("SHOP", "01", "01", 
            "Shopping - Clothing", "Clothing");
        TransactionCategory childCategory2 = createTestTransactionCategory("SHOP", "02", "01", 
            "Shopping - Electronics", "Electronics");

        repository.save(parentCategory);
        repository.save(childCategory1);
        repository.save(childCategory2);
        repository.flush();

        // Test hierarchical category retrieval
        List<TransactionCategory> shopCategories = repository.findByIdCategoryCode("SHOP");
        Assertions.assertThat(shopCategories).hasSize(3);

        // Test primary category identification (subcategory "00")
        Optional<TransactionCategory> primaryCategory = shopCategories.stream()
            .filter(cat -> "00".equals(cat.getSubcategoryCode()))
            .findFirst();
        Assertions.assertThat(primaryCategory).isPresent();
        Assertions.assertThat(primaryCategory.get().isPrimaryCategory()).isTrue();

        // Test subcategory relationships
        List<TransactionCategory> subcategories = shopCategories.stream()
            .filter(cat -> !"00".equals(cat.getSubcategoryCode()))
            .toList();
        Assertions.assertThat(subcategories).hasSize(2);
        Assertions.assertThat(subcategories).extracting(TransactionCategory::getCategoryName)
            .containsExactlyInAnyOrder("Clothing", "Electronics");
    }

    /**
     * Test findByTransactionTypeCode for transaction type filtering.
     * Validates relationship between TransactionCategory and TransactionType entities
     * ensuring proper referential integrity and type-based categorization.
     */
    @Test
    void testTransactionTypeFiltering() {
        // Create categories with different transaction types
        TransactionCategory purchaseCategory = createTestTransactionCategory("PURCH", "01", "01", 
            "Purchase Transactions", "Purchase");
        TransactionCategory paymentCategory = createTestTransactionCategory("PYMNT", "01", "02", 
            "Payment Transactions", "Payment");
        TransactionCategory feeCategory = createTestTransactionCategory("FEES", "01", "03", 
            "Fee Transactions", "Fees");

        repository.save(purchaseCategory);
        repository.save(paymentCategory);
        repository.save(feeCategory);
        repository.flush();

        // Test filtering by transaction type code
        List<TransactionCategory> purchaseCategories = repository.findByTransactionTypeCode("01");
        Assertions.assertThat(purchaseCategories).hasSize(1);
        Assertions.assertThat(purchaseCategories.get(0).getCategoryCode()).isEqualTo("PURCH");
        Assertions.assertThat(purchaseCategories.get(0).appliesToTransactionType("01")).isTrue();

        List<TransactionCategory> paymentCategories = repository.findByTransactionTypeCode("02");
        Assertions.assertThat(paymentCategories).hasSize(1);
        Assertions.assertThat(paymentCategories.get(0).getCategoryCode()).isEqualTo("PYMNT");

        // Test ordered retrieval by transaction type
        List<TransactionCategory> orderedCategories = repository.findByTransactionTypeCodeOrderByIdCategoryCodeAsc("01");
        Assertions.assertThat(orderedCategories).hasSize(1);
        Assertions.assertThat(orderedCategories.get(0).getCategoryCode()).isEqualTo("PURCH");
    }

    /**
     * Validate category description and name retrieval with search functionality.
     * Tests flexible category lookup supporting case-insensitive searches
     * for user interface category selection operations.
     */
    @Test
    void testCategoryDescriptionAndNameRetrieval() {
        // Create categories with varied descriptions and names
        TransactionCategory category1 = createTestTransactionCategory("REST", "01", "01", 
            "Restaurant and Dining Expenses", "Restaurants");
        TransactionCategory category2 = createTestTransactionCategory("TRVL", "01", "01", 
            "Travel and Transportation", "Travel");
        TransactionCategory category3 = createTestTransactionCategory("ENTM", "01", "01", 
            "Entertainment and Recreation", "Entertainment");

        repository.save(category1);
        repository.save(category2);
        repository.save(category3);
        repository.flush();

        // Test description-based search (case-insensitive)
        List<TransactionCategory> restaurantCategories = repository.findByCategoryDescriptionContainingIgnoreCase("restaurant");
        Assertions.assertThat(restaurantCategories).hasSize(1);
        Assertions.assertThat(restaurantCategories.get(0).getCategoryDescription()).contains("Restaurant");

        List<TransactionCategory> travelCategories = repository.findByCategoryDescriptionContainingIgnoreCase("TRAVEL");
        Assertions.assertThat(travelCategories).hasSize(1);
        Assertions.assertThat(travelCategories.get(0).getCategoryCode()).isEqualTo("TRVL");

        // Test name-based search (case-insensitive)
        List<TransactionCategory> entertainmentCategories = repository.findByCategoryNameContainingIgnoreCase("entertain");
        Assertions.assertThat(entertainmentCategories).hasSize(1);
        Assertions.assertThat(entertainmentCategories.get(0).getCategoryName()).isEqualTo("Entertainment");

        // Test display text functionality
        String displayText = category1.getDisplayText();
        Assertions.assertThat(displayText).isEqualTo("Restaurants - Restaurant and Dining Expenses");
    }

    /**
     * Test @Cacheable behavior for reference data optimization.
     * Validates caching performance matching COBOL equivalent response times
     * for frequently accessed transaction category lookups.
     */
    @Test
    void testCacheableBehavior() {
        // Create test category
        TransactionCategory category = createTestTransactionCategory("CACHE", "01", "01", 
            "Cache Test Category", "Cache Test");
        repository.save(category);
        repository.flush();

        // First call - should hit database and cache result
        Instant start1 = Instant.now();
        List<TransactionCategory> result1 = repository.findByIdCategoryCode("CACHE");
        long duration1 = Instant.now().toEpochMilli() - start1.toEpochMilli();

        Assertions.assertThat(result1).hasSize(1);
        Assertions.assertThat(duration1).isLessThan(RESPONSE_TIME_THRESHOLD_MS);

        // Second call - should use cached result (faster)
        Instant start2 = Instant.now();
        List<TransactionCategory> result2 = repository.findByIdCategoryCode("CACHE");
        long duration2 = Instant.now().toEpochMilli() - start2.toEpochMilli();

        Assertions.assertThat(result2).hasSize(1);
        Assertions.assertThat(duration2).isLessThan(CACHE_PERFORMANCE_THRESHOLD_MS);
        Assertions.assertThat(duration2).isLessThan(duration1);

        // Verify cache manager has cached results
        Assertions.assertThat(cacheManager.getCacheNames()).contains("transactionCategoriesByCode");
        Assertions.assertThat(cacheManager.getCache("transactionCategoriesByCode")).isNotNull();
    }

    /**
     * Verify composite key uniqueness constraints and data integrity.
     * Tests constraint violations and proper error handling for duplicate
     * composite keys maintaining VSAM file organization equivalence.
     */
    @Test
    void testCompositeKeyUniquenessConstraints() {
        // Create initial category
        TransactionCategory original = createTestTransactionCategory("UNIQ", "01", "01", 
            "Unique Test Category", "Unique");
        repository.save(original);
        repository.flush();

        // Attempt to create duplicate with same composite key
        TransactionCategory duplicate = createTestTransactionCategory("UNIQ", "01", "01", 
            "Duplicate Test Category", "Duplicate");

        // Should violate unique constraint
        Assertions.assertThatCode(() -> {
            repository.save(duplicate);
            repository.flush();
        }).isInstanceOf(Exception.class);

        // Verify only original exists
        long count = repository.countByIdCategoryCode("UNIQ");
        Assertions.assertThat(count).isEqualTo(1L);

        boolean exists = repository.existsByIdCategoryCodeAndIdSubcategoryCode("UNIQ", "01");
        Assertions.assertThat(exists).isTrue();
    }

    /**
     * Test bulk category loading and startup behavior.
     * Validates efficient bulk operations for reference data initialization
     * matching VSAM file loading patterns during system startup.
     */
    @Test
    void testBulkCategoryLoadingAndStartup() {
        // Create bulk test data
        List<TransactionCategory> bulkCategories = generateTransactionCategoryList();
        
        // Measure bulk save performance
        Instant bulkStart = Instant.now();
        repository.saveAll(bulkCategories);
        repository.flush();
        long bulkDuration = Instant.now().toEpochMilli() - bulkStart.toEpochMilli();

        // Verify bulk save performance meets requirements
        Assertions.assertThat(bulkDuration).isLessThan(RESPONSE_TIME_THRESHOLD_MS * 5);

        // Validate all categories were saved
        long totalCount = repository.count();
        Assertions.assertThat(totalCount).isEqualTo(bulkCategories.size());

        // Test bulk retrieval performance
        Instant retrievalStart = Instant.now();
        List<TransactionCategory> allCategories = repository.findAll();
        long retrievalDuration = Instant.now().toEpochMilli() - retrievalStart.toEpochMilli();

        Assertions.assertThat(retrievalDuration).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        Assertions.assertThat(allCategories).hasSize(bulkCategories.size());

        // Verify category code distribution
        long uniqueCategoryCodes = allCategories.stream()
            .map(TransactionCategory::getCategoryCode)
            .distinct()
            .count();
        Assertions.assertThat(uniqueCategoryCodes).isGreaterThan(1);
    }

    /**
     * Validate relationship with TransactionType entity integration.
     * Tests referential integrity between category and type entities
     * ensuring proper transaction classification workflows.
     */
    @Test
    void testTransactionTypeEntityRelationship() {
        // Note: TransactionType entity validation - testing relationship logic
        TransactionCategory category = createTestTransactionCategory("TYPE", "01", "01", 
            "Type Relationship Test", "TypeTest");
        repository.save(category);
        repository.flush();

        // Test transaction type code validation
        String typeCode = category.getTransactionTypeCode();
        Assertions.assertThat(typeCode).isEqualTo("01");
        Assertions.assertThat(typeCode).hasSize(2);
        Assertions.assertThat(typeCode).matches("^[A-Z0-9]{2}$");

        // Test relationship method
        boolean appliesToType01 = category.appliesToTransactionType("01");
        boolean appliesToType02 = category.appliesToTransactionType("02");
        
        Assertions.assertThat(appliesToType01).isTrue();
        Assertions.assertThat(appliesToType02).isFalse();

        // Test filtering by transaction type
        List<TransactionCategory> typeCategories = repository.findByTransactionTypeCode("01");
        Assertions.assertThat(typeCategories).contains(category);
    }

    /**
     * Verify concurrent access to cached categories for thread safety.
     * Tests concurrent read operations on cached reference data ensuring
     * thread-safe access patterns for high-frequency category lookups.
     */
    @Test
    void testConcurrentAccessToCachedCategories() throws InterruptedException {
        // Create test categories
        TransactionCategory category1 = createTestTransactionCategory("CONC", "01", "01", 
            "Concurrent Test 1", "Concurrent1");
        TransactionCategory category2 = createTestTransactionCategory("CONC", "02", "01", 
            "Concurrent Test 2", "Concurrent2");
        
        repository.save(category1);
        repository.save(category2);
        repository.flush();

        // Create thread pool for concurrent access testing
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<List<TransactionCategory>>> futures = new java.util.ArrayList<>();

        // Submit concurrent read operations
        for (int i = 0; i < 20; i++) {
            CompletableFuture<List<TransactionCategory>> future = CompletableFuture.supplyAsync(() -> 
                repository.findByIdCategoryCode("CONC"), executor);
            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        allFutures.get(5, TimeUnit.SECONDS);

        // Verify all concurrent operations succeeded
        for (CompletableFuture<List<TransactionCategory>> future : futures) {
            List<TransactionCategory> result = future.get();
            Assertions.assertThat(result).hasSize(2);
            Assertions.assertThat(result).extracting(TransactionCategory::getCategoryCode)
                .containsOnly("CONC");
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Test invalid category code error handling and validation.
     * Validates proper error responses for invalid composite keys, 
     * missing references, and constraint violations.
     */
    @Test
    void testInvalidCategoryCodeErrorHandling() {
        // Test invalid category code patterns
        Assertions.assertThatThrownBy(() -> {
            TransactionCategory invalidCategory = new TransactionCategory();
            invalidCategory.setCategoryCode("TOOLONG"); // Should be 4 chars max
            invalidCategory.setSubcategoryCode("01");
            invalidCategory.setTransactionTypeCode("01");
            invalidCategory.setCategoryDescription("Invalid Category");
            invalidCategory.setCategoryName("Invalid");
            repository.save(invalidCategory);
            repository.flush();
        }).isInstanceOf(Exception.class);

        // Test invalid subcategory code
        Assertions.assertThatThrownBy(() -> {
            TransactionCategory invalidSubcat = new TransactionCategory();
            invalidSubcat.setCategoryCode("TEST");
            invalidSubcat.setSubcategoryCode("ABC"); // Should be 2 chars max
            invalidSubcat.setTransactionTypeCode("01");
            invalidSubcat.setCategoryDescription("Invalid Subcategory");
            invalidSubcat.setCategoryName("Invalid");
            repository.save(invalidSubcat);
            repository.flush();
        }).isInstanceOf(Exception.class);

        // Test missing required fields
        Assertions.assertThatThrownBy(() -> {
            TransactionCategory incomplete = new TransactionCategory();
            incomplete.setCategoryCode("TEST");
            incomplete.setSubcategoryCode("01");
            // Missing required fields: transactionTypeCode, description, name
            repository.save(incomplete);
            repository.flush();
        }).isInstanceOf(Exception.class);

        // Test non-existent category retrieval
        Optional<TransactionCategory> notFound = repository.findByIdCategoryCodeAndIdSubcategoryCode("XXXX", "99");
        Assertions.assertThat(notFound).isEmpty();

        boolean exists = repository.existsByIdCategoryCodeAndIdSubcategoryCode("XXXX", "99");
        Assertions.assertThat(exists).isFalse();

        long count = repository.countByIdCategoryCode("XXXX");
        Assertions.assertThat(count).isEqualTo(0L);
    }

    /**
     * Test standard CRUD operations inherited from JpaRepository.
     * Validates basic repository functionality including save, delete,
     * and existence checking operations.
     */
    @Test
    void testStandardCrudOperations() {
        // Test save operation
        TransactionCategory category = createTestTransactionCategory("CRUD", "01", "01", 
            "CRUD Test Category", "CRUD");
        TransactionCategory saved = repository.save(category);
        
        Assertions.assertThat(saved).isNotNull();
        Assertions.assertThat(saved.getCategoryCode()).isEqualTo("CRUD");
        Assertions.assertThat(saved.getSubcategoryCode()).isEqualTo("01");

        // Test exists check
        boolean exists = repository.existsById(saved.getId());
        Assertions.assertThat(exists).isTrue();

        // Test findById
        Optional<TransactionCategory> found = repository.findById(saved.getId());
        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().equals(saved)).isTrue();
        Assertions.assertThat(found.get().hashCode()).isEqualTo(saved.hashCode());

        // Test toString method
        String stringRepresentation = saved.toString();
        Assertions.assertThat(stringRepresentation).contains("CRUD");
        Assertions.assertThat(stringRepresentation).contains("01");

        // Test count operation
        long initialCount = repository.count();
        Assertions.assertThat(initialCount).isGreaterThan(0L);

        // Test delete operation
        repository.deleteById(saved.getId());
        repository.flush();

        boolean existsAfterDelete = repository.existsById(saved.getId());
        Assertions.assertThat(existsAfterDelete).isFalse();

        long finalCount = repository.count();
        Assertions.assertThat(finalCount).isEqualTo(initialCount - 1);

        // Test deleteAll operation
        repository.deleteAll();
        repository.flush();
        
        long countAfterDeleteAll = repository.count();
        Assertions.assertThat(countAfterDeleteAll).isEqualTo(0L);
    }

    // Helper Methods

    /**
     * Create test TransactionCategory with specified parameters.
     * Utility method for generating consistent test data matching COBOL structure.
     */
    private TransactionCategory createTestTransactionCategory(String categoryCode, String subcategoryCode, 
                                                            String transactionTypeCode, String description, String name) {
        return new TransactionCategory(categoryCode, subcategoryCode, transactionTypeCode, description, name);
    }

    /**
     * Generate list of TransactionCategory entities for bulk testing.
     * Creates realistic test data with proper PIC clause formatting
     * and COBOL-compatible data structures.
     */
    private List<TransactionCategory> generateTransactionCategoryList() {
        return List.of(
            createTestTransactionCategory("FOOD", "01", "01", "Restaurant Purchases", "Restaurant"),
            createTestTransactionCategory("FOOD", "02", "01", "Grocery Store Purchases", "Grocery"),
            createTestTransactionCategory("FUEL", "01", "01", "Gas Station Purchases", "Fuel"),
            createTestTransactionCategory("FUEL", "02", "01", "Electric Vehicle Charging", "EV Charging"),
            createTestTransactionCategory("SHOP", "01", "01", "Retail Store Purchases", "Retail"),
            createTestTransactionCategory("SHOP", "02", "01", "Online Store Purchases", "Online"),
            createTestTransactionCategory("TRVL", "01", "01", "Transportation Expenses", "Transport"),
            createTestTransactionCategory("TRVL", "02", "01", "Hotel and Lodging", "Lodging"),
            createTestTransactionCategory("ENTM", "01", "01", "Entertainment Expenses", "Entertainment"),
            createTestTransactionCategory("ENTM", "02", "01", "Sports and Recreation", "Recreation"),
            createTestTransactionCategory("HLTH", "01", "02", "Medical Expenses", "Medical"),
            createTestTransactionCategory("HLTH", "02", "02", "Pharmacy Purchases", "Pharmacy"),
            createTestTransactionCategory("UTIL", "01", "03", "Utility Bill Payments", "Utilities"),
            createTestTransactionCategory("UTIL", "02", "03", "Phone Bill Payments", "Phone"),
            createTestTransactionCategory("CASH", "01", "04", "ATM Cash Withdrawals", "ATM"),
            createTestTransactionCategory("CASH", "02", "04", "Bank Cash Advances", "Cash Advance")
        );
    }
}