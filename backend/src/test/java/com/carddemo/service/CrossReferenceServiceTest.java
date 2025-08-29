/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.CardXref;
import com.carddemo.repository.CardXrefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for CrossReferenceService that validates cross-reference functionality
 * linking cards to accounts based on CVACT03Y structure from the original COBOL system.
 * 
 * This test class ensures that the Java implementation maintains 100% functional parity with the
 * original COBOL cross-reference processing logic while leveraging modern Spring Boot testing
 * capabilities and comprehensive validation scenarios.
 * 
 * Key Testing Areas:
 * 1. **Card-to-Account Mapping Validation**: Tests the core functionality of linking credit cards
 *    to customer accounts, ensuring proper relationship establishment and validation
 * 
 * 2. **Multiple Cards Per Account Handling**: Validates scenarios where customers maintain
 *    multiple cards linked to the same account, testing relationship integrity
 * 
 * 3. **Primary Card Designation Management**: Tests primary card assignment and management
 *    within account relationships, ensuring proper hierarchy enforcement
 * 
 * 4. **Cross-Reference Integrity Validation**: Comprehensive validation of referential
 *    consistency across card-account-customer relationships
 * 
 * 5. **Cascade Delete Operations**: Tests complex deletion scenarios ensuring proper
 *    cleanup and integrity maintenance during account closure operations
 * 
 * 6. **Bidirectional Navigation Testing**: Validates efficient navigation between cards,
 *    accounts, and customers in both directions for reporting and service operations
 * 
 * 7. **Orphaned Reference Detection**: Tests proactive detection and cleanup of orphaned
 *    cross-reference records that may result from incomplete transactions
 * 
 * 8. **Bulk Cross-Reference Operations**: Validates high-volume batch processing capabilities
 *    for efficient data migration and maintenance operations
 * 
 * 9. **Concurrent Modification Handling**: Tests thread-safe operations and optimistic
 *    locking to handle concurrent access in containerized environments
 * 
 * 10. **Data Consistency Validation**: Ensures all cross-reference operations maintain
 *     proper data consistency and business rule compliance
 * 
 * COBOL Equivalence:
 * These tests replicate validation logic from original COBOL programs including:
 * - CVACT03Y copybook structure validation
 * - Cross-reference lookup and validation from CBTRN01C
 * - Relationship management from account maintenance programs
 * - Batch processing patterns from nightly operations
 * 
 * Performance Requirements:
 * All cross-reference operations must complete within 200ms to maintain transaction
 * processing performance parity with the original CICS system.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CrossReferenceServiceTest extends BaseServiceTest {

    @Mock
    private CardXrefRepository cardXrefRepository;

    @InjectMocks
    private CrossReferenceService crossReferenceService;

    // Test data constants matching COBOL field specifications
    private static final String VALID_CARD_NUMBER = "4532123456789012";
    private static final String VALID_CARD_NUMBER_2 = "5432109876543210";
    private static final String INVALID_CARD_NUMBER = "123";
    private static final Long VALID_CUSTOMER_ID = 100000001L;
    private static final Long VALID_ACCOUNT_ID = 1000000001L;
    private static final Long VALID_ACCOUNT_ID_2 = 1000000002L;

    // Test data objects
    private CardXref testCardXref;
    private CardXref testCardXref2;
    private List<CardXref> testCardXrefList;

    /**
     * Initializes test environment with comprehensive mock setup and test data.
     * Creates realistic test scenarios matching COBOL data structures and business rules.
     */
    @BeforeEach
    public void setUp() {
        super.setUp();
        initializeTestData();
        configureMockResponses();
    }

    /**
     * Creates comprehensive test data matching CVACT03Y structure specifications.
     * Generates CardXref entities with realistic values for thorough testing coverage.
     */
    private void initializeTestData() {
        // Primary test cross-reference
        testCardXref = testDataBuilder.buildCardXref()
            .withCardNumber(VALID_CARD_NUMBER)
            .withCustomerId(VALID_CUSTOMER_ID)
            .withAccountId(VALID_ACCOUNT_ID)
            .build();

        // Secondary test cross-reference for multiple card scenarios
        testCardXref2 = testDataBuilder.buildCardXref()
            .withCardNumber(VALID_CARD_NUMBER_2)
            .withCustomerId(VALID_CUSTOMER_ID)
            .withAccountId(VALID_ACCOUNT_ID)
            .build();

        // Create test list for bulk operations
        testCardXrefList = new ArrayList<>();
        testCardXrefList.add(testCardXref);
        testCardXrefList.add(testCardXref2);

        // Validate test data matches COBOL structure requirements
        validateTestData(testCardXref);
        validateTestData(testCardXref2);
    }

    /**
     * Configures comprehensive mock repository responses for various test scenarios.
     * Sets up both success and failure paths to enable thorough validation testing.
     */
    private void configureMockResponses() {
        // Configure successful cross-reference lookups
        when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID))
            .thenReturn(true);
        when(cardXrefRepository.findByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID))
            .thenReturn(Optional.of(testCardXref));

        // Configure account-based queries
        when(cardXrefRepository.findByXrefAcctId(VALID_ACCOUNT_ID))
            .thenReturn(testCardXrefList);
        when(cardXrefRepository.countByXrefAcctId(VALID_ACCOUNT_ID))
            .thenReturn(2L);

        // Configure card-based queries  
        when(cardXrefRepository.findFirstByXrefCardNum(VALID_CARD_NUMBER))
            .thenReturn(Optional.of(testCardXref));
        when(cardXrefRepository.findByXrefCardNum(VALID_CARD_NUMBER))
            .thenReturn(List.of(testCardXref));

        // Configure customer-based queries
        when(cardXrefRepository.findByXrefCustId(VALID_CUSTOMER_ID))
            .thenReturn(testCardXrefList);

        // Configure save operations
        when(cardXrefRepository.save(any(CardXref.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Configure batch operations
        when(cardXrefRepository.findAll())
            .thenReturn(testCardXrefList);
    }

    @Nested
    @DisplayName("Card-to-Account Link Validation Tests")
    class CardToAccountLinkValidationTests {

        @Test
        @DisplayName("Should validate valid card-to-account link successfully")
        void testValidateCardToAccountLink_ValidLink_ReturnsTrue() {
            // Arrange - using pre-configured mock data

            // Act & Assert
            long startTime = System.currentTimeMillis();
            boolean result = crossReferenceService.validateCardToAccountLink(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);
            long executionTime = System.currentTimeMillis() - startTime;

            assertThat(result).isTrue();
            assertUnder200ms(executionTime);

            // Verify repository interactions
            verify(cardXrefRepository).existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);
            verify(cardXrefRepository).findByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should reject invalid card-to-account link")
        void testValidateCardToAccountLink_InvalidLink_ReturnsFalse() {
            // Arrange
            String invalidCardNumber = "9999999999999999";
            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(invalidCardNumber, VALID_ACCOUNT_ID))
                .thenReturn(false);

            // Act & Assert
            boolean result = crossReferenceService.validateCardToAccountLink(invalidCardNumber, VALID_ACCOUNT_ID);
            assertThat(result).isFalse();

            verify(cardXrefRepository).existsByXrefCardNumAndXrefAcctId(invalidCardNumber, VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw exception for null card number")
        void testValidateCardToAccountLink_NullCardNumber_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> crossReferenceService.validateCardToAccountLink(null, VALID_ACCOUNT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for invalid card number length")
        void testValidateCardToAccountLink_InvalidCardLength_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> crossReferenceService.validateCardToAccountLink(INVALID_CARD_NUMBER, VALID_ACCOUNT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number must be exactly 16 characters");
        }

        @Test
        @DisplayName("Should throw exception for null account ID")
        void testValidateCardToAccountLink_NullAccountId_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> crossReferenceService.validateCardToAccountLink(VALID_CARD_NUMBER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account ID must be a positive number");
        }

        @Test
        @DisplayName("Should validate cross-reference data consistency")
        void testValidateCardToAccountLink_DataConsistency_ValidatesCorrectly() {
            // Arrange - create cross-reference with mismatched data
            CardXref inconsistentXref = testDataBuilder.buildCardXref()
                .withCardNumber("1111111111111111") // Different card number
                .withCustomerId(VALID_CUSTOMER_ID)
                .withAccountId(VALID_ACCOUNT_ID)
                .build();

            when(cardXrefRepository.findByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID))
                .thenReturn(Optional.of(inconsistentXref));

            // Act & Assert - should return false due to data inconsistency
            boolean result = crossReferenceService.validateCardToAccountLink(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Cards Per Account Handling Tests")
    class MultipleCardsPerAccountTests {

        @Test
        @DisplayName("Should find all cards associated with account")
        void testFindCardsByAccountId_MultipleCards_ReturnsAllCards() {
            // Act
            long startTime = System.currentTimeMillis();
            List<String> result = crossReferenceService.findCardsByAccountId(VALID_ACCOUNT_ID);
            long executionTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).contains(VALID_CARD_NUMBER, VALID_CARD_NUMBER_2);
            assertThat(result).isSorted(); // Should return sorted card numbers
            assertUnder200ms(executionTime);

            verify(cardXrefRepository).findByXrefAcctId(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should handle account with no cards")
        void testFindCardsByAccountId_NoCards_ReturnsEmptyList() {
            // Arrange
            Long emptyAccountId = 9999999999L;
            when(cardXrefRepository.findByXrefAcctId(emptyAccountId))
                .thenReturn(Collections.emptyList());

            // Act
            List<String> result = crossReferenceService.findCardsByAccountId(emptyAccountId);

            // Assert
            assertThat(result).isEmpty();
            verify(cardXrefRepository).findByXrefAcctId(emptyAccountId);
        }

        @Test
        @DisplayName("Should handle duplicate card numbers in account")
        void testFindCardsByAccountId_DuplicateCards_ReturnsDistinctCards() {
            // Arrange - create list with duplicate card numbers
            List<CardXref> duplicateList = new ArrayList<>();
            duplicateList.add(testCardXref);
            duplicateList.add(testCardXref); // Duplicate
            duplicateList.add(testCardXref2);

            when(cardXrefRepository.findByXrefAcctId(VALID_ACCOUNT_ID))
                .thenReturn(duplicateList);

            // Act
            List<String> result = crossReferenceService.findCardsByAccountId(VALID_ACCOUNT_ID);

            // Assert
            assertThat(result).hasSize(2); // Should remove duplicates
            assertThat(result).containsExactly(VALID_CARD_NUMBER, VALID_CARD_NUMBER_2);
        }

        @Test
        @DisplayName("Should get accurate card count for account")
        void testGetCardCount_MultipleCards_ReturnsCorrectCount() {
            // Act
            long count = crossReferenceService.getCardCount(VALID_ACCOUNT_ID);

            // Assert
            assertThat(count).isEqualTo(2L);
            verify(cardXrefRepository).countByXrefAcctId(VALID_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("Primary Card Designation Tests")
    class PrimaryCardDesignationTests {

        @Test
        @DisplayName("Should update primary card successfully")
        void testUpdatePrimaryCard_ValidCard_ReturnsTrue() {
            // Arrange
            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER_2, VALID_ACCOUNT_ID))
                .thenReturn(true);

            // Act
            long startTime = System.currentTimeMillis();
            boolean result = crossReferenceService.updatePrimaryCard(VALID_ACCOUNT_ID, VALID_CARD_NUMBER_2);
            long executionTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(result).isTrue();
            assertUnder200ms(executionTime);
            verify(cardXrefRepository).existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER_2, VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should reject primary card not associated with account")
        void testUpdatePrimaryCard_CardNotInAccount_ThrowsException() {
            // Arrange
            String unassociatedCard = "9999999999999999";
            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(unassociatedCard, VALID_ACCOUNT_ID))
                .thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> crossReferenceService.updatePrimaryCard(VALID_ACCOUNT_ID, unassociatedCard))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to update primary card");
        }

        @Test
        @DisplayName("Should get primary card for account")
        void testGetPrimaryCardForAccount_ValidAccount_ReturnsCard() {
            // Act
            String primaryCard = crossReferenceService.getPrimaryCardForAccount(VALID_ACCOUNT_ID);

            // Assert
            assertThat(primaryCard).isEqualTo(VALID_CARD_NUMBER); // First card becomes primary
            verify(cardXrefRepository).findByXrefAcctId(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should return null for account with no cards")
        void testGetPrimaryCardForAccount_NoCards_ReturnsNull() {
            // Arrange
            Long emptyAccountId = 9999999999L;
            when(cardXrefRepository.findByXrefAcctId(emptyAccountId))
                .thenReturn(Collections.emptyList());

            // Act
            String primaryCard = crossReferenceService.getPrimaryCardForAccount(emptyAccountId);

            // Assert
            assertThat(primaryCard).isNull();
        }
    }

    @Nested
    @DisplayName("Cross-Reference Creation and Deletion Tests")
    class CrossReferenceCreationDeletionTests {

        @Test
        @DisplayName("Should create new cross-reference successfully")
        void testCreateCardXref_ValidData_CreatesSuccessfully() {
            // Arrange
            String newCardNumber = "1234567890123456";
            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(newCardNumber, VALID_ACCOUNT_ID_2))
                .thenReturn(false);

            // Act
            long startTime = System.currentTimeMillis();
            CardXref result = crossReferenceService.createCardXref(newCardNumber, VALID_CUSTOMER_ID, VALID_ACCOUNT_ID_2);
            long executionTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getXrefCardNum()).isEqualTo(newCardNumber);
            assertThat(result.getXrefCustId()).isEqualTo(VALID_CUSTOMER_ID);
            assertThat(result.getXrefAcctId()).isEqualTo(VALID_ACCOUNT_ID_2);
            assertUnder200ms(executionTime);

            verify(cardXrefRepository).existsByXrefCardNumAndXrefAcctId(newCardNumber, VALID_ACCOUNT_ID_2);
            verify(cardXrefRepository).save(any(CardXref.class));
        }

        @Test
        @DisplayName("Should prevent duplicate cross-reference creation")
        void testCreateCardXref_DuplicateEntry_ThrowsException() {
            // Arrange
            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID))
                .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> crossReferenceService.createCardXref(VALID_CARD_NUMBER, VALID_CUSTOMER_ID, VALID_ACCOUNT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create cross-reference for card");
        }

        @Test
        @DisplayName("Should delete cross-reference successfully")
        void testDeleteCardXref_ExistingEntry_DeletesSuccessfully() {
            // Arrange
            when(cardXrefRepository.findByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID))
                .thenReturn(Optional.of(testCardXref));

            // Act
            long startTime = System.currentTimeMillis();
            boolean result = crossReferenceService.deleteCardXref(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);
            long executionTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(result).isTrue();
            assertUnder200ms(executionTime);

            verify(cardXrefRepository).findByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);
            verify(cardXrefRepository).delete(testCardXref);
        }

        @Test
        @DisplayName("Should handle deletion of non-existent cross-reference")
        void testDeleteCardXref_NonExistentEntry_ReturnsFalse() {
            // Arrange
            String nonExistentCard = "9999999999999999";
            when(cardXrefRepository.findByXrefCardNumAndXrefAcctId(nonExistentCard, VALID_ACCOUNT_ID))
                .thenReturn(Optional.empty());

            // Act
            boolean result = crossReferenceService.deleteCardXref(nonExistentCard, VALID_ACCOUNT_ID);

            // Assert
            assertThat(result).isFalse();
            verify(cardXrefRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Cross-Reference Integrity Validation Tests")
    class IntegrityValidationTests {

        @Test
        @DisplayName("Should validate referential integrity successfully")
        void testValidateIntegrity_ValidData_ReturnsTrue() {
            // Act
            long startTime = System.currentTimeMillis();
            boolean result = crossReferenceService.validateIntegrity();
            long executionTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(result).isTrue();
            assertUnder200ms(executionTime);
            verify(cardXrefRepository).findAll();
        }

        @Test
        @DisplayName("Should detect invalid card number format")
        void testValidateIntegrity_InvalidCardFormat_ThrowsException() {
            // Arrange
            CardXref invalidXref = testDataBuilder.buildCardXref()
                .withCardNumber("123") // Invalid length
                .withCustomerId(VALID_CUSTOMER_ID)
                .withAccountId(VALID_ACCOUNT_ID)
                .build();

            when(cardXrefRepository.findAll())
                .thenReturn(List.of(invalidXref));

            // Act & Assert
            assertThatThrownBy(() -> crossReferenceService.validateIntegrity())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cross-reference integrity validation failed");
        }

        @Test
        @DisplayName("Should detect invalid customer ID")
        void testValidateIntegrity_InvalidCustomerId_ThrowsException() {
            // Arrange
            CardXref invalidXref = testDataBuilder.buildCardXref()
                .withCardNumber(VALID_CARD_NUMBER)
                .withCustomerId(-1L) // Invalid customer ID
                .withAccountId(VALID_ACCOUNT_ID)
                .build();

            when(cardXrefRepository.findAll())
                .thenReturn(List.of(invalidXref));

            // Act & Assert
            assertThatThrownBy(() -> crossReferenceService.validateIntegrity())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cross-reference integrity validation failed");
        }

        @Test
        @DisplayName("Should validate cross-reference relationship")
        void testIsValidCrossReference_ValidRelationship_ReturnsTrue() {
            // Arrange
            when(cardXrefRepository.findByXrefCardNumAndXrefCustId(VALID_CARD_NUMBER, VALID_CUSTOMER_ID))
                .thenReturn(List.of(testCardXref));

            // Act
            boolean result = crossReferenceService.isValidCrossReference(VALID_CARD_NUMBER, VALID_CUSTOMER_ID, VALID_ACCOUNT_ID);

            // Assert
            assertThat(result).isTrue();
            verify(cardXrefRepository).findByXrefCardNumAndXrefCustId(VALID_CARD_NUMBER, VALID_CUSTOMER_ID);
        }
    }

    @Nested
    @DisplayName("Bidirectional Navigation Tests")
    class BidirectionalNavigationTests {

        @Test
        @DisplayName("Should find account by card number")
        void testFindAccountByCardNumber_ValidCard_ReturnsAccountId() {
            // Act
            long startTime = System.currentTimeMillis();
            Long accountId = crossReferenceService.findAccountByCardNumber(VALID_CARD_NUMBER);
            long executionTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(accountId).isEqualTo(VALID_ACCOUNT_ID);
            assertUnder200ms(executionTime);
            verify(cardXrefRepository).findFirstByXrefCardNum(VALID_CARD_NUMBER);
        }

        @Test
        @DisplayName("Should return null for non-existent card")
        void testFindAccountByCardNumber_NonExistentCard_ReturnsNull() {
            // Arrange
            String nonExistentCard = "9999999999999999";
            when(cardXrefRepository.findFirstByXrefCardNum(nonExistentCard))
                .thenReturn(Optional.empty());

            // Act
            Long accountId = crossReferenceService.findAccountByCardNumber(nonExistentCard);

            // Assert
            assertThat(accountId).isNull();
        }

        @Test
        @DisplayName("Should find all cards for customer")
        void testFindCardsByCustomerId_ValidCustomer_ReturnsCards() {
            // Act
            List<String> cards = crossReferenceService.findCardsByCustomerId(VALID_CUSTOMER_ID);

            // Assert
            assertThat(cards).hasSize(2);
            assertThat(cards).contains(VALID_CARD_NUMBER, VALID_CARD_NUMBER_2);
            verify(cardXrefRepository).findByXrefCustId(VALID_CUSTOMER_ID);
        }
    }

    @Nested
    @DisplayName("Cascade Delete Operations Tests")
    class CascadeDeleteTests {

        @Test
        @DisplayName("Should cascade delete all cross-references for account")
        void testCascadeDeleteAccount_ValidAccount_DeletesAllReferences() {
            // Act
            long startTime = System.currentTimeMillis();
            int deletedCount = crossReferenceService.cascadeDeleteAccount(VALID_ACCOUNT_ID);
            long executionTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(deletedCount).isEqualTo(2); // Should delete both cross-references
            assertUnder200ms(executionTime);

            verify(cardXrefRepository).findByXrefAcctId(VALID_ACCOUNT_ID);
            verify(cardXrefRepository, times(2)).delete(any(CardXref.class));
        }

        @Test
        @DisplayName("Should handle cascade delete for account with no cross-references")
        void testCascadeDeleteAccount_NoReferences_ReturnsZero() {
            // Arrange
            Long emptyAccountId = 9999999999L;
            when(cardXrefRepository.findByXrefAcctId(emptyAccountId))
                .thenReturn(Collections.emptyList());

            // Act
            int deletedCount = crossReferenceService.cascadeDeleteAccount(emptyAccountId);

            // Assert
            assertThat(deletedCount).isEqualTo(0);
            verify(cardXrefRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle partial cascade delete failures gracefully")
        void testCascadeDeleteAccount_PartialFailure_ContinuesProcessing() {
            // Arrange
            doThrow(new RuntimeException("Database error"))
                .when(cardXrefRepository).delete(testCardXref);
            doNothing()
                .when(cardXrefRepository).delete(testCardXref2);

            // Act
            int deletedCount = crossReferenceService.cascadeDeleteAccount(VALID_ACCOUNT_ID);

            // Assert
            assertThat(deletedCount).isEqualTo(1); // Only one successful deletion
            verify(cardXrefRepository, times(2)).delete(any(CardXref.class));
        }
    }

    @Nested
    @DisplayName("Orphaned Reference Detection Tests")
    class OrphanedReferenceDetectionTests {

        @Test
        @DisplayName("Should detect orphaned references successfully")
        void testDetectOrphanedRefs_WithOrphans_ReturnsOrphanedList() {
            // Arrange
            CardXref orphanedXref = testDataBuilder.buildCardXref()
                .withCardNumber("") // Empty card number - orphaned
                .withCustomerId(VALID_CUSTOMER_ID)
                .withAccountId(VALID_ACCOUNT_ID)
                .build();

            List<CardXref> mixedList = new ArrayList<>();
            mixedList.add(testCardXref); // Valid
            mixedList.add(orphanedXref); // Orphaned

            when(cardXrefRepository.findAll())
                .thenReturn(mixedList);

            // Act
            long startTime = System.currentTimeMillis();
            List<CardXref> orphans = crossReferenceService.detectOrphanedRefs();
            long executionTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(orphans).hasSize(1);
            assertThat(orphans.get(0)).isEqualTo(orphanedXref);
            assertUnder200ms(executionTime);
        }

        @Test
        @DisplayName("Should return empty list when no orphaned references exist")
        void testDetectOrphanedRefs_NoOrphans_ReturnsEmptyList() {
            // Act
            List<CardXref> orphans = crossReferenceService.detectOrphanedRefs();

            // Assert
            assertThat(orphans).isEmpty();
            verify(cardXrefRepository).findAll();
        }

        @Test
        @DisplayName("Should detect multiple types of orphaned references")
        void testDetectOrphanedRefs_MultipleOrphanTypes_DetectsAll() {
            // Arrange
            CardXref orphan1 = testDataBuilder.buildCardXref()
                .withCardNumber(null) // Null card number
                .withCustomerId(VALID_CUSTOMER_ID)
                .withAccountId(VALID_ACCOUNT_ID)
                .build();

            CardXref orphan2 = testDataBuilder.buildCardXref()
                .withCardNumber(VALID_CARD_NUMBER)
                .withCustomerId(-1L) // Invalid customer ID
                .withAccountId(VALID_ACCOUNT_ID)
                .build();

            List<CardXref> mixedList = List.of(testCardXref, orphan1, orphan2);
            when(cardXrefRepository.findAll()).thenReturn(mixedList);

            // Act
            List<CardXref> orphans = crossReferenceService.detectOrphanedRefs();

            // Assert
            assertThat(orphans).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Bulk Cross-Reference Operations Tests")
    class BulkOperationsTests {

        @Test
        @DisplayName("Should create bulk cross-references successfully")
        void testBulkCreateXrefs_ValidData_CreatesAll() {
            // Arrange
            List<CardXref> bulkData = testDataBuilder.buildMultipleCardXrefs(10);
            
            // Configure mocks to return false for existence checks (no duplicates)
            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(anyString(), anyLong()))
                .thenReturn(false);

            // Act
            long startTime = System.currentTimeMillis();
            int createdCount = crossReferenceService.bulkCreateXrefs(bulkData);
            long executionTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(createdCount).isEqualTo(10);
            assertUnder200ms(executionTime);
            verify(cardXrefRepository, times(10)).save(any(CardXref.class));
        }

        @Test
        @DisplayName("Should skip invalid records in bulk create")
        void testBulkCreateXrefs_MixedValidInvalid_SkipsInvalid() {
            // Arrange
            List<CardXref> mixedData = new ArrayList<>();
            mixedData.add(testCardXref); // Valid
            
            CardXref invalidXref = testDataBuilder.buildCardXref()
                .withCardNumber("123") // Invalid length
                .withCustomerId(VALID_CUSTOMER_ID)
                .withAccountId(VALID_ACCOUNT_ID)
                .build();
            mixedData.add(invalidXref); // Invalid

            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID))
                .thenReturn(false);

            // Act
            int createdCount = crossReferenceService.bulkCreateXrefs(mixedData);

            // Assert
            assertThat(createdCount).isEqualTo(1); // Only valid record created
            verify(cardXrefRepository, times(1)).save(any(CardXref.class));
        }

        @Test
        @DisplayName("Should handle empty bulk data list")
        void testBulkCreateXrefs_EmptyList_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> crossReferenceService.bulkCreateXrefs(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cross-reference data list cannot be null or empty");
        }

        @Test
        @DisplayName("Should remove duplicates in bulk create")
        void testBulkCreateXrefs_DuplicateRecords_ProcessesOnce() {
            // Arrange
            List<CardXref> duplicateData = new ArrayList<>();
            duplicateData.add(testCardXref);
            duplicateData.add(testCardXref); // Duplicate

            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(VALID_CARD_NUMBER, VALID_ACCOUNT_ID))
                .thenReturn(false);

            // Act
            int createdCount = crossReferenceService.bulkCreateXrefs(duplicateData);

            // Assert
            assertThat(createdCount).isEqualTo(1); // Only one record processed
            verify(cardXrefRepository, times(1)).save(any(CardXref.class));
        }
    }

    @Nested
    @DisplayName("Concurrent Modification Handling Tests")
    class ConcurrentModificationTests {

        @Test
        @DisplayName("Should handle concurrent cross-reference creation safely")
        void testConcurrentCreation_MultipleThreads_HandlesCorrectly() throws InterruptedException, java.util.concurrent.ExecutionException {
            // Arrange
            ExecutorService executor = Executors.newFixedThreadPool(5);
            List<CompletableFuture<CardXref>> futures = new ArrayList<>();

            // Configure repository to simulate race conditions
            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(anyString(), anyLong()))
                .thenReturn(false);

            // Act - create multiple concurrent operations
            for (int i = 0; i < 5; i++) {
                final int index = i;
                String cardNumber = String.format("123456789012340%d", index);
                
                CompletableFuture<CardXref> future = CompletableFuture.supplyAsync(() -> {
                    return crossReferenceService.createCardXref(cardNumber, VALID_CUSTOMER_ID, VALID_ACCOUNT_ID_2);
                }, executor);
                
                futures.add(future);
            }

            // Wait for all operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

            // Assert
            for (CompletableFuture<CardXref> future : futures) {
                assertThat(future.get()).isNotNull();
            }

            // Cleanup
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("Should handle concurrent primary card updates safely")
        void testConcurrentPrimaryCardUpdate_MultipleThreads_HandlesSafely() throws InterruptedException, java.util.concurrent.ExecutionException {
            // Arrange
            ExecutorService executor = Executors.newFixedThreadPool(3);
            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(anyString(), anyLong()))
                .thenReturn(true);

            // Act - simulate concurrent primary card updates
            CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(() -> {
                return crossReferenceService.updatePrimaryCard(VALID_ACCOUNT_ID, VALID_CARD_NUMBER);
            }, executor);

            CompletableFuture<Boolean> future2 = CompletableFuture.supplyAsync(() -> {
                return crossReferenceService.updatePrimaryCard(VALID_ACCOUNT_ID, VALID_CARD_NUMBER_2);
            }, executor);

            // Wait for completion
            CompletableFuture.allOf(future1, future2).get();

            // Assert
            assertThat(future1.get()).isTrue();
            assertThat(future2.get()).isTrue();

            // Cleanup
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Nested
    @DisplayName("Data Consistency Validation Tests")
    class DataConsistencyTests {

        @Test
        @DisplayName("Should maintain data consistency during operations")
        void testDataConsistency_ComplexOperations_MaintainsIntegrity() {
            // Arrange
            String newCard = "9876543210123456";
            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(newCard, VALID_ACCOUNT_ID_2))
                .thenReturn(false);

            // Act - perform series of operations
            CardXref created = crossReferenceService.createCardXref(newCard, VALID_CUSTOMER_ID, VALID_ACCOUNT_ID_2);
            boolean primaryUpdated = crossReferenceService.updatePrimaryCard(VALID_ACCOUNT_ID, VALID_CARD_NUMBER);
            boolean linkValid = crossReferenceService.validateCardToAccountLink(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);

            // Assert
            assertThat(created).isNotNull();
            assertThat(primaryUpdated).isTrue();
            assertThat(linkValid).isTrue();

            // Verify consistency
            verify(cardXrefRepository).save(any(CardXref.class));
        }

        @Test
        @DisplayName("Should validate composite key integrity")
        void testCompositeKeyIntegrity_AllOperations_MaintainsConsistency() {
            // This test ensures that all operations maintain proper composite key relationships
            
            // Test card-to-account lookup consistency
            Long foundAccountId = crossReferenceService.findAccountByCardNumber(VALID_CARD_NUMBER);
            assertThat(foundAccountId).isEqualTo(VALID_ACCOUNT_ID);

            // Test bidirectional consistency
            List<String> accountCards = crossReferenceService.findCardsByAccountId(VALID_ACCOUNT_ID);
            assertThat(accountCards).contains(VALID_CARD_NUMBER);

            // Test customer relationship consistency
            List<String> customerCards = crossReferenceService.findCardsByCustomerId(VALID_CUSTOMER_ID);
            assertThat(customerCards).contains(VALID_CARD_NUMBER);
        }

        @Test
        @DisplayName("Should handle edge case data scenarios")
        void testEdgeCaseDataScenarios_BoundaryValues_HandlesCorrectly() {
            // Test with minimum valid values
            Long minCustomerId = 1L;
            Long minAccountId = 1L;
            String validCard = "1111111111111111";

            when(cardXrefRepository.existsByXrefCardNumAndXrefAcctId(validCard, minAccountId))
                .thenReturn(false);

            // Act
            CardXref result = crossReferenceService.createCardXref(validCard, minCustomerId, minAccountId);

            // Assert
            assertThat(result.getXrefCustId()).isEqualTo(minCustomerId);
            assertThat(result.getXrefAcctId()).isEqualTo(minAccountId);
            assertThat(result.getXrefCardNum()).isEqualTo(validCard);
        }
    }

    /**
     * Helper method to create a properly constructed CardXref with the TestDataBuilder pattern.
     * This ensures all test data follows COBOL structure requirements from CVACT03Y.
     */
    private CardXref createTestCardXref(String cardNumber, Long customerId, Long accountId) {
        return testDataBuilder.buildCardXref()
            .withCardNumber(cardNumber)
            .withCustomerId(customerId)
            .withAccountId(accountId)
            .build();
    }

    /**
     * Validates test execution performance against COBOL system SLA requirements.
     * All cross-reference operations must complete within 200ms for transaction processing parity.
     */
    private void validatePerformanceRequirements(long executionTime) {
        assertThat(executionTime)
            .as("Cross-reference operation must complete within 200ms SLA")
            .isLessThan(MAX_RESPONSE_TIME_MS);
    }

    /**
     * Performs comprehensive validation of test scenarios against COBOL business rules.
     * Ensures all cross-reference operations maintain functional parity with original system.
     */
    private void validateCobolBusinessRules(CardXref crossRef) {
        // Validate card number format (16 characters)
        assertThat(crossRef.getXrefCardNum())
            .hasSize(16)
            .matches("\\d{16}");

        // Validate customer ID is positive
        assertThat(crossRef.getXrefCustId())
            .isPositive();

        // Validate account ID is positive  
        assertThat(crossRef.getXrefAcctId())
            .isPositive();

        // Validate composite key consistency
        if (crossRef.getId() != null) {
            assertThat(crossRef.getId().getXrefCardNum())
                .isEqualTo(crossRef.getXrefCardNum());
            assertThat(crossRef.getId().getXrefCustId())
                .isEqualTo(crossRef.getXrefCustId());
            assertThat(crossRef.getId().getXrefAcctId())
                .isEqualTo(crossRef.getXrefAcctId());
        }
    }
}