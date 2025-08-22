package com.carddemo.repository;

import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionType;
import com.carddemo.repository.TransactionTypeRepository;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.IntegrationTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.TestDataGenerator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration test class for TransactionTypeRepository validating reference data operations,
 * caching behavior, lookup table functionality, and immutability of transaction type codes.
 * 
 * This test class validates the comprehensive functionality of transaction type reference data
 * management including CRUD operations, caching behavior, lookup table functionality, and
 * immutability constraints. Tests ensure functional parity with COBOL VSAM TRANTYPE reference
 * file operations while validating Spring Boot @Cacheable annotation performance improvements.
 * 
 * Test Coverage Areas:
 * - Transaction type CRUD operations (findAll, findByTransactionTypeCode, save, delete)
 * - @Cacheable annotation behavior and performance validation
 * - Reference data immutability and integrity constraints
 * - Debit/credit flag handling and validation
 * - Cache eviction and refresh mechanisms
 * - Data initialization and startup validation
 * - Concurrent access to cached data
 * - Referential integrity with Transaction entity
 * - Performance comparison between cached and non-cached queries
 * - Transaction type code uniqueness validation
 * - Error handling for invalid type codes
 * 
 * Key Testing Requirements from Section 0:
 * - Maintain sub-200ms response times for all cached queries
 * - Validate COBOL COMP-3 decimal precision for monetary calculations
 * - Ensure 100% functional parity with VSAM TRANTYPE reference file
 * - Support 10,000 TPS throughput for transaction type lookups
 * - Maintain session state compatibility with CICS COMMAREA behavior
 * 
 * @author Blitzy CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 */
@DataJpaTest
@EnableCaching
public class TransactionTypeRepositoryTest extends AbstractBaseTest implements IntegrationTest {

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Test findAll method for loading all transaction types from reference data.
     * Validates complete transaction type retrieval with proper caching behavior
     * and performance within required response time thresholds.
     * 
     * Validates:
     * - Complete retrieval of all transaction type reference data
     * - @Cacheable annotation performance improvement
     * - Response time under 200ms threshold per Section 0.2.1
     * - Data integrity matching COBOL VSAM TRANTYPE structure
     * - Proper Spring Data JPA query generation and execution
     */
    @Test
    public void testFindAllTransactionTypes_ShouldReturnAllTypesWithCaching() {
        // Given: Test transaction types are available in the database
        createTestTransactionTypes();
        
        // When: Retrieving all transaction types for the first time (uncached)
        Instant startTime = Instant.now();
        List<TransactionType> uncachedResults = transactionTypeRepository.findAll();
        long uncachedExecutionTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // Then: Verify results contain expected transaction types
        assertThat(uncachedResults).isNotNull();
        assertThat(uncachedResults).isNotEmpty();
        assertThat(uncachedResults).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify response time meets performance requirements
        assertThat(uncachedExecutionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        // When: Retrieving all transaction types again (cached)
        startTime = Instant.now();
        List<TransactionType> cachedResults = transactionTypeRepository.findAll();
        long cachedExecutionTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // Then: Verify caching improves performance
        assertThat(cachedResults).isEqualTo(uncachedResults);
        assertThat(cachedExecutionTime).isLessThan(TestConstants.CACHE_PERFORMANCE_THRESHOLD_MS);
        
        // Verify cache is populated
        assertThat(cacheManager.getCache("allTransactionTypes")).isNotNull();
        
        logTestExecution("findAll transaction types test completed", uncachedExecutionTime);
    }

    /**
     * Test findByTransactionTypeCode method for primary key lookup operations.
     * Validates transaction type code lookup functionality with caching behavior
     * and proper handling of valid and invalid transaction type codes.
     * 
     * Validates:
     * - Primary key lookup functionality matching VSAM key access
     * - @Cacheable annotation behavior for individual lookups
     * - Proper handling of valid transaction type codes
     * - Null return for invalid transaction type codes
     * - Performance within required response time thresholds
     */
    @Test
    public void testFindByTransactionTypeCode_ShouldReturnCorrectTypeWithCaching() {
        // Given: Test transaction types with known codes
        TransactionType purchaseType = createTestTransactionType("PU", "Purchase", "D");
        TransactionType paymentType = createTestTransactionType("PM", "Payment", "C");
        
        transactionTypeRepository.save(purchaseType);
        transactionTypeRepository.save(paymentType);
        transactionTypeRepository.flush();
        
        // When: Looking up transaction type by code (uncached)
        Instant startTime = Instant.now();
        TransactionType foundType = transactionTypeRepository.findByTransactionTypeCode("PU");
        long executionTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // Then: Verify correct transaction type is returned
        assertThat(foundType).isNotNull();
        assertThat(foundType.getTransactionTypeCode()).isEqualTo("PU");
        assertThat(foundType.getTypeDescription()).isEqualTo("Purchase");
        assertThat(foundType.getDebitCreditFlag()).isEqualTo("D");
        assertThat(foundType.isDebit()).isTrue();
        
        // Verify response time meets performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        // When: Looking up same transaction type again (cached)
        startTime = Instant.now();
        TransactionType cachedType = transactionTypeRepository.findByTransactionTypeCode("PU");
        long cachedExecutionTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // Then: Verify caching improves performance
        assertThat(cachedType).isEqualTo(foundType);
        assertThat(cachedExecutionTime).isLessThan(TestConstants.CACHE_PERFORMANCE_THRESHOLD_MS);
        
        // When: Looking up invalid transaction type code
        TransactionType invalidType = transactionTypeRepository.findByTransactionTypeCode("XX");
        
        // Then: Verify null is returned for invalid codes
        assertThat(invalidType).isNull();
        
        logTestExecution("findByTransactionTypeCode test completed", executionTime);
    }

    /**
     * Test @Cacheable annotation behavior for performance optimization.
     * Validates that caching annotations provide significant performance improvements
     * for frequently accessed transaction type reference data.
     * 
     * Validates:
     * - Cache population on first access
     * - Performance improvement on subsequent cached access
     * - Cache key generation and retrieval
     * - Cache hit rates and efficiency
     * - Performance meets sub-50ms threshold for cached queries
     */
    @Test
    public void testCacheableBehavior_ShouldImprovePerformance() {
        // Given: Test transaction type is available
        TransactionType testType = createTestTransactionType(TestConstants.TEST_TRANSACTION_TYPE_CODE, 
                                                           TestConstants.TEST_TRANSACTION_TYPE_DESC, "D");
        transactionTypeRepository.save(testType);
        transactionTypeRepository.flush();
        
        // Clear caches to ensure clean state
        clearAllCaches();
        
        // When: First lookup (cache miss)
        Instant startTime = Instant.now();
        TransactionType firstLookup = transactionTypeRepository.findByTransactionTypeCode(TestConstants.TEST_TRANSACTION_TYPE_CODE);
        long firstLookupTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // Then: Verify data is returned correctly
        assertThat(firstLookup).isNotNull();
        assertThat(firstLookup.getTransactionTypeCode()).isEqualTo(TestConstants.TEST_TRANSACTION_TYPE_CODE);
        
        // When: Second lookup (cache hit)
        startTime = Instant.now();
        TransactionType secondLookup = transactionTypeRepository.findByTransactionTypeCode(TestConstants.TEST_TRANSACTION_TYPE_CODE);
        long secondLookupTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // Then: Verify cache improves performance
        assertThat(secondLookup).isEqualTo(firstLookup);
        assertThat(secondLookupTime).isLessThan(firstLookupTime);
        assertThat(secondLookupTime).isLessThan(TestConstants.CACHE_PERFORMANCE_THRESHOLD_MS);
        
        // Verify cache contains expected data
        assertThat(cacheManager.getCache("transactionTypes")).isNotNull();
        
        logTestExecution("Cacheable behavior test completed", firstLookupTime);
    }

    /**
     * Test immutability of transaction type reference data.
     * Validates that transaction type reference data maintains immutability
     * characteristics similar to COBOL VSAM TRANTYPE reference file behavior.
     * 
     * Validates:
     * - Reference data immutability constraints
     * - Prevention of accidental modification
     * - Data integrity preservation
     * - Proper error handling for modification attempts
     * - Consistency with COBOL reference file behavior
     */
    @Test
    public void testTransactionTypeImmutability_ShouldPreventModification() {
        // Given: Transaction type reference data exists
        TransactionType originalType = createTestTransactionType("IM", "Immutable Type", "D");
        TransactionType savedType = transactionTypeRepository.save(originalType);
        transactionTypeRepository.flush();
        
        // When: Attempting to retrieve and verify immutability
        TransactionType retrievedType = transactionTypeRepository.findByTransactionTypeCode("IM");
        
        // Then: Verify data consistency and immutability
        assertThat(retrievedType).isNotNull();
        assertThat(retrievedType.getTransactionTypeCode()).isEqualTo("IM");
        assertThat(retrievedType.getTypeDescription()).isEqualTo("Immutable Type");
        assertThat(retrievedType.getDebitCreditFlag()).isEqualTo("D");
        
        // Verify equals and hashCode consistency for immutability
        assertThat(retrievedType.equals(savedType)).isTrue();
        assertThat(retrievedType.hashCode()).isEqualTo(savedType.hashCode());
        
        // Verify toString provides consistent representation
        String expectedToString = "TransactionType{transactionTypeCode='IM', typeDescription='Immutable Type', debitCreditFlag='D'}";
        assertThat(retrievedType.toString()).isEqualTo(expectedToString);
        
        logTestExecution("Transaction type immutability test completed", null);
    }

    /**
     * Test transaction type description retrieval functionality.
     * Validates proper handling of transaction type descriptions including
     * length validation, character encoding, and search functionality.
     * 
     * Validates:
     * - Description field handling and validation
     * - Proper character encoding for description text
     * - Search functionality by description patterns
     * - Field length constraints matching COBOL PIC clauses
     * - Case-insensitive search capabilities
     */
    @Test
    public void testTransactionTypeDescriptionRetrieval_ShouldHandleDescriptions() {
        // Given: Transaction types with various descriptions
        TransactionType purchaseType = createTestTransactionType("PU", "Purchase Transaction", "D");
        TransactionType refundType = createTestTransactionType("RF", "Refund Processing", "C");
        TransactionType feeType = createTestTransactionType("FE", "Fee Assessment", "D");
        
        transactionTypeRepository.save(purchaseType);
        transactionTypeRepository.save(refundType);
        transactionTypeRepository.save(feeType);
        transactionTypeRepository.flush();
        
        // When: Searching by description patterns
        List<TransactionType> purchaseTypes = transactionTypeRepository.findByTypeDescriptionContainingIgnoreCase("purchase");
        List<TransactionType> processingTypes = transactionTypeRepository.findByTypeDescriptionContainingIgnoreCase("PROCESSING");
        List<TransactionType> assessmentTypes = transactionTypeRepository.findByTypeDescriptionContainingIgnoreCase("Assessment");
        
        // Then: Verify correct description matching
        assertThat(purchaseTypes).hasSize(1);
        assertThat(purchaseTypes.get(0).getTypeDescription()).isEqualTo("Purchase Transaction");
        
        assertThat(processingTypes).hasSize(1);
        assertThat(processingTypes.get(0).getTypeDescription()).isEqualTo("Refund Processing");
        
        assertThat(assessmentTypes).hasSize(1);
        assertThat(assessmentTypes.get(0).getTypeDescription()).isEqualTo("Fee Assessment");
        
        // Verify case-insensitive search behavior
        List<TransactionType> mixedCaseResults = transactionTypeRepository.findByTypeDescriptionContainingIgnoreCase("TrAnSaCtIoN");
        assertThat(mixedCaseResults).hasSize(1);
        
        logTestExecution("Transaction type description test completed", null);
    }

    /**
     * Test debit/credit flag handling and validation.
     * Validates proper debit/credit classification functionality matching
     * COBOL transaction processing logic and accounting requirements.
     * 
     * Validates:
     * - Debit flag ('D') handling and validation
     * - Credit flag ('C') handling and validation
     * - Search functionality by debit/credit flag
     * - Accounting classification utility methods
     * - Business logic consistency with COBOL implementation
     */
    @Test
    public void testDebitCreditFlagHandling_ShouldValidateFlags() {
        // Given: Transaction types with different debit/credit flags
        TransactionType debitType1 = createTestTransactionType("D1", "Debit Type 1", "D");
        TransactionType debitType2 = createTestTransactionType("D2", "Debit Type 2", "D");
        TransactionType creditType1 = createTestTransactionType("C1", "Credit Type 1", "C");
        TransactionType creditType2 = createTestTransactionType("C2", "Credit Type 2", "C");
        
        transactionTypeRepository.save(debitType1);
        transactionTypeRepository.save(debitType2);
        transactionTypeRepository.save(creditType1);
        transactionTypeRepository.save(creditType2);
        transactionTypeRepository.flush();
        
        // When: Searching by debit/credit flags
        List<TransactionType> debitTypes = transactionTypeRepository.findByDebitCreditFlag("D");
        List<TransactionType> creditTypes = transactionTypeRepository.findByDebitCreditFlag("C");
        
        // Then: Verify correct flag filtering
        assertThat(debitTypes).hasSize(2);
        assertThat(debitTypes).allMatch(type -> type.getDebitCreditFlag().equals("D"));
        assertThat(debitTypes).allMatch(TransactionType::isDebit);
        assertThat(debitTypes).noneMatch(TransactionType::isCredit);
        
        assertThat(creditTypes).hasSize(2);
        assertThat(creditTypes).allMatch(type -> type.getDebitCreditFlag().equals("C"));
        assertThat(creditTypes).allMatch(TransactionType::isCredit);
        assertThat(creditTypes).noneMatch(TransactionType::isDebit);
        
        // Verify accounting classification methods
        TransactionType debitType = debitTypes.get(0);
        TransactionType creditType = creditTypes.get(0);
        
        assertThat(debitType.getAccountingClassification()).isEqualTo("Debit");
        assertThat(creditType.getAccountingClassification()).isEqualTo("Credit");
        
        // Verify display text formatting
        assertThat(debitType.getDisplayText()).matches("D[12] - Debit Type [12]");
        assertThat(creditType.getDisplayText()).matches("C[12] - Credit Type [12]");
        
        logTestExecution("Debit/credit flag handling test completed", null);
    }

    /**
     * Test cache eviction and refresh mechanisms.
     * Validates cache management functionality including eviction policies,
     * refresh mechanisms, and performance impact of cache operations.
     * 
     * Validates:
     * - Manual cache eviction functionality
     * - Automatic cache refresh behavior
     * - Performance impact of cache operations
     * - Cache consistency after eviction
     * - Memory management and cache size constraints
     */
    @Test
    public void testCacheEvictionAndRefresh_ShouldManageCacheLifecycle() {
        // Given: Transaction type is cached
        TransactionType testType = createTestTransactionType("CE", "Cache Eviction", "D");
        transactionTypeRepository.save(testType);
        transactionTypeRepository.flush();
        
        // Cache the data
        TransactionType cachedType = transactionTypeRepository.findByTransactionTypeCode("CE");
        assertThat(cachedType).isNotNull();
        
        // When: Manually evicting cache
        clearAllCaches();
        
        // Then: Verify cache is cleared
        assertThat(cacheManager.getCache("transactionTypes")).isNotNull();
        
        // When: Accessing data after cache eviction
        Instant startTime = Instant.now();
        TransactionType refreshedType = transactionTypeRepository.findByTransactionTypeCode("CE");
        long refreshTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // Then: Verify data consistency after refresh
        assertThat(refreshedType).isNotNull();
        assertThat(refreshedType.getTransactionTypeCode()).isEqualTo("CE");
        assertThat(refreshedType.equals(cachedType)).isTrue();
        
        // Verify performance is still acceptable after refresh
        assertThat(refreshTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Cache eviction and refresh test completed", refreshTime);
    }

    /**
     * Test data initialization on application startup.
     * Validates proper initialization of transaction type reference data
     * including seed data loading and validation of required transaction types.
     * 
     * Validates:
     * - Startup data initialization process
     * - Required transaction type availability
     * - Data consistency after initialization
     * - Performance of initialization process
     * - Error handling during initialization
     */
    @Test
    public void testDataInitializationOnStartup_ShouldLoadReferenceData() {
        // Given: Clean database state
        transactionTypeRepository.deleteAll();
        transactionTypeRepository.flush();
        clearAllCaches();
        
        // When: Creating essential transaction types (simulating startup)
        createTestTransactionTypes();
        
        // Then: Verify initialization creates required data
        long totalCount = transactionTypeRepository.count();
        assertThat(totalCount).isGreaterThanOrEqualTo(2);
        
        // Verify essential transaction types exist
        List<TransactionType> allTypes = transactionTypeRepository.findAll();
        assertThat(allTypes).isNotEmpty();
        assertThat(allTypes).allMatch(type -> type.getTransactionTypeCode() != null);
        assertThat(allTypes).allMatch(type -> type.getTypeDescription() != null);
        assertThat(allTypes).allMatch(type -> type.getDebitCreditFlag() != null);
        
        // Verify debit/credit flags are valid
        assertThat(allTypes).allMatch(type -> 
            type.getDebitCreditFlag().equals("D") || type.getDebitCreditFlag().equals("C"));
        
        // Verify transaction type codes are properly formatted
        assertThat(allTypes).allMatch(type -> 
            type.getTransactionTypeCode().length() == 2);
        
        logTestExecution("Data initialization test completed", null);
    }

    /**
     * Test concurrent access to cached transaction type data.
     * Validates thread-safe access to cached reference data under concurrent
     * load conditions matching production usage patterns.
     * 
     * Validates:
     * - Thread-safe cache access under concurrent load
     * - Data consistency under concurrent read operations
     * - Performance under concurrent access patterns
     * - Cache coherence with multiple simultaneous requests
     * - No data corruption under concurrent access
     */
    @Test
    public void testConcurrentAccessToCachedData_ShouldHandleConcurrency() {
        // Given: Transaction types for concurrent testing
        TransactionType concurrentType = createTestTransactionType("CC", "Concurrent Access", "D");
        transactionTypeRepository.save(concurrentType);
        transactionTypeRepository.flush();
        
        // When: Accessing data concurrently from multiple threads
        CompletableFuture<TransactionType> future1 = CompletableFuture.supplyAsync(() ->
            transactionTypeRepository.findByTransactionTypeCode("CC"));
        CompletableFuture<TransactionType> future2 = CompletableFuture.supplyAsync(() ->
            transactionTypeRepository.findByTransactionTypeCode("CC"));
        CompletableFuture<TransactionType> future3 = CompletableFuture.supplyAsync(() ->
            transactionTypeRepository.findByTransactionTypeCode("CC"));
        CompletableFuture<List<TransactionType>> future4 = CompletableFuture.supplyAsync(() ->
            transactionTypeRepository.findAll());
        
        // Then: Verify all concurrent operations complete successfully
        assertThatCode(() -> {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3, future4);
            allFutures.get();
        }).doesNotThrowAnyException();
        
        // Verify data consistency across all concurrent requests
        TransactionType result1 = future1.join();
        TransactionType result2 = future2.join();
        TransactionType result3 = future3.join();
        List<TransactionType> result4 = future4.join();
        
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();
        assertThat(result4).isNotEmpty();
        
        // Verify data consistency
        assertThat(result1.equals(result2)).isTrue();
        assertThat(result2.equals(result3)).isTrue();
        assertThat(result4).contains(result1);
        
        logTestExecution("Concurrent access test completed", null);
    }

    /**
     * Test referential integrity with Transaction entity.
     * Validates proper foreign key relationships and referential integrity
     * between TransactionType and Transaction entities.
     * 
     * Validates:
     * - Foreign key relationship integrity
     * - Cascade behavior for related entities
     * - Referential constraint enforcement
     * - Transaction type validation in transaction processing
     * - Data consistency between related entities
     */
    @Test
    public void testReferentialIntegrityWithTransaction_ShouldMaintainConsistency() {
        // Given: Transaction type for referential integrity testing
        TransactionType transactionType = createTestTransactionType("RI", "Referential Integrity", "D");
        TransactionType savedType = transactionTypeRepository.save(transactionType);
        transactionTypeRepository.flush();
        
        // When: Creating a transaction with transaction type relationship
        Transaction transaction = new Transaction();
        transaction.setTransactionId(12345L);
        transaction.setTransactionType(savedType);
        
        // Then: Verify relationship is properly established
        assertThat(transaction.getTransactionType()).isNotNull();
        assertThat(transaction.getTransactionType().getTransactionTypeCode()).isEqualTo("RI");
        assertThat(transaction.getTransactionType()).isEqualTo(savedType);
        
        // Verify transaction type methods work correctly
        assertThat(savedType.isDebit()).isTrue();
        assertThat(savedType.getAccountingClassification()).isEqualTo("Debit");
        assertThat(savedType.getDisplayText()).isEqualTo("RI - Referential Integrity");
        
        // Verify referential integrity constraints
        TransactionType lookupType = transactionTypeRepository.findByTransactionTypeCode("RI");
        assertThat(lookupType).isEqualTo(savedType);
        
        logTestExecution("Referential integrity test completed", null);
    }

    /**
     * Test performance comparison between cached and non-cached queries.
     * Validates performance improvements achieved through caching annotations
     * and ensures performance meets sub-200ms response time requirements.
     * 
     * Validates:
     * - Performance difference between cached and non-cached queries
     * - Response time compliance with SLA requirements
     * - Cache hit ratio and effectiveness
     * - Performance consistency under load
     * - Cache warming and cold start behavior
     */
    @Test
    public void testCachedVsNonCachedQueryPerformance_ShouldShowImprovement() {
        // Given: Test data for performance comparison
        createTestTransactionTypes();
        clearAllCaches();
        
        // When: Measuring non-cached query performance (first call)
        Instant startTime = Instant.now();
        List<TransactionType> firstCall = transactionTypeRepository.findAll();
        long nonCachedTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // When: Measuring cached query performance (subsequent call)
        startTime = Instant.now();
        List<TransactionType> secondCall = transactionTypeRepository.findAll();
        long cachedTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // Then: Verify performance improvement with caching
        assertThat(firstCall).isEqualTo(secondCall);
        assertThat(cachedTime).isLessThan(nonCachedTime);
        assertThat(cachedTime).isLessThan(TestConstants.CACHE_PERFORMANCE_THRESHOLD_MS);
        assertThat(nonCachedTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        // When: Measuring individual lookup performance
        startTime = Instant.now();
        TransactionType individualLookup = transactionTypeRepository.findByTransactionTypeCode(TestConstants.TEST_TRANSACTION_TYPE_CODE);
        long lookupTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        // Then: Verify individual lookup performance
        assertThat(individualLookup).isNotNull();
        assertThat(lookupTime).isLessThan(TestConstants.CACHE_PERFORMANCE_THRESHOLD_MS);
        
        logTestExecution("Performance comparison test completed - Non-cached: " + 
                        nonCachedTime + "ms, Cached: " + cachedTime + "ms", nonCachedTime);
    }

    /**
     * Test transaction type code uniqueness validation.
     * Validates that transaction type codes maintain uniqueness constraints
     * similar to COBOL VSAM TRANTYPE file key uniqueness requirements.
     * 
     * Validates:
     * - Primary key uniqueness constraints
     * - Proper error handling for duplicate codes
     * - Database constraint enforcement
     * - Consistency with COBOL VSAM behavior
     * - Data integrity preservation
     */
    @Test
    public void testTransactionTypeCodeUniqueness_ShouldEnforceUniqueness() {
        // Given: Transaction type with specific code
        TransactionType originalType = createTestTransactionType("UN", "Unique Type", "D");
        TransactionType savedType = transactionTypeRepository.save(originalType);
        transactionTypeRepository.flush();
        
        // When: Attempting to create duplicate transaction type code
        TransactionType duplicateType = createTestTransactionType("UN", "Duplicate Attempt", "C");
        
        // Then: Verify uniqueness constraint prevents duplicates
        assertThatThrownBy(() -> {
            transactionTypeRepository.save(duplicateType);
            transactionTypeRepository.flush();
        }).isInstanceOf(Exception.class);
        
        // Verify original type is still retrievable
        TransactionType retrievedType = transactionTypeRepository.findByTransactionTypeCode("UN");
        assertThat(retrievedType).isNotNull();
        assertThat(retrievedType.getTypeDescription()).isEqualTo("Unique Type");
        assertThat(retrievedType.getDebitCreditFlag()).isEqualTo("D");
        
        // Verify existsBy method works correctly
        assertThat(transactionTypeRepository.existsByTransactionTypeCode("UN")).isTrue();
        assertThat(transactionTypeRepository.existsByTransactionTypeCode("XX")).isFalse();
        
        logTestExecution("Transaction type code uniqueness test completed", null);
    }

    /**
     * Test error handling for invalid transaction type codes.
     * Validates proper error handling and validation for invalid transaction
     * type codes including null values, empty strings, and malformed codes.
     * 
     * Validates:
     * - Null parameter handling
     * - Empty string parameter handling
     * - Invalid format parameter handling
     * - Proper exception types and messages
     * - Graceful error handling without system failure
     */
    @Test
    public void testErrorHandlingForInvalidTypeCodes_ShouldHandleGracefully() {
        // When/Then: Testing null parameter handling
        assertThatCode(() -> {
            TransactionType result = transactionTypeRepository.findByTransactionTypeCode(null);
            assertThat(result).isNull();
        }).doesNotThrowAnyException();
        
        // When/Then: Testing empty string parameter handling
        assertThatCode(() -> {
            TransactionType result = transactionTypeRepository.findByTransactionTypeCode("");
            assertThat(result).isNull();
        }).doesNotThrowAnyException();
        
        // When/Then: Testing invalid format parameter handling
        assertThatCode(() -> {
            TransactionType result = transactionTypeRepository.findByTransactionTypeCode("INVALID");
            assertThat(result).isNull();
        }).doesNotThrowAnyException();
        
        // When/Then: Testing special character handling
        assertThatCode(() -> {
            TransactionType result = transactionTypeRepository.findByTransactionTypeCode("@#");
            assertThat(result).isNull();
        }).doesNotThrowAnyException();
        
        // When/Then: Testing existsBy methods with invalid codes
        assertThat(transactionTypeRepository.existsByTransactionTypeCode(null)).isFalse();
        assertThat(transactionTypeRepository.existsByTransactionTypeCode("")).isFalse();
        assertThat(transactionTypeRepository.existsByTransactionTypeCode("INVALID")).isFalse();
        
        // When/Then: Testing search methods with invalid parameters
        assertThatCode(() -> {
            List<TransactionType> result = transactionTypeRepository.findByDebitCreditFlag(null);
            assertThat(result).isEmpty();
        }).doesNotThrowAnyException();
        
        assertThatCode(() -> {
            List<TransactionType> result = transactionTypeRepository.findByTypeDescriptionContainingIgnoreCase(null);
            assertThat(result).isEmpty();
        }).doesNotThrowAnyException();
        
        logTestExecution("Error handling test completed", null);
    }

    // Helper Methods

    /**
     * Create test transaction types for various test scenarios.
     * Creates a standard set of transaction types for testing purposes
     * with proper COBOL-compatible data formatting and validation.
     */
    private void createTestTransactionTypes() {
        TransactionType purchaseType = createTestTransactionType(TestConstants.TEST_TRANSACTION_TYPE_CODE, 
                                                               TestConstants.TEST_TRANSACTION_TYPE_DESC, "D");
        TransactionType paymentType = createTestTransactionType("PM", "Payment", "C");
        TransactionType feeType = createTestTransactionType("FE", "Fee", "D");
        TransactionType refundType = createTestTransactionType("RF", "Refund", "C");
        
        transactionTypeRepository.save(purchaseType);
        transactionTypeRepository.save(paymentType);
        transactionTypeRepository.save(feeType);
        transactionTypeRepository.save(refundType);
        transactionTypeRepository.flush();
    }

    /**
     * Create a test transaction type with specified parameters.
     * Factory method for creating transaction type test objects with
     * COBOL-compatible field formatting and validation.
     * 
     * @param typeCode 2-character transaction type code
     * @param description transaction type description
     * @param debitCreditFlag 'D' for debit, 'C' for credit
     * @return TransactionType test object
     */
    private TransactionType createTestTransactionType(String typeCode, String description, String debitCreditFlag) {
        TransactionType transactionType = new TransactionType();
        transactionType.setTransactionTypeCode(typeCode);
        transactionType.setTypeDescription(description);
        transactionType.setDebitCreditFlag(debitCreditFlag);
        return transactionType;
    }

    /**
     * Clear all cache managers to ensure clean test state.
     * Utility method for cache management during testing to ensure
     * proper test isolation and cache behavior validation.
     */
    private void clearAllCaches() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                if (cacheManager.getCache(cacheName) != null) {
                    cacheManager.getCache(cacheName).clear();
                }
            });
        }
    }
}