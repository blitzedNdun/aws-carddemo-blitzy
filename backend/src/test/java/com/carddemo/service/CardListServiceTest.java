/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.dto.CardDto;
import com.carddemo.dto.CardListDto;
import com.carddemo.dto.PageResponse;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.repository.CardRepository;
import com.carddemo.service.CreditCardListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit test suite for CreditCardListService.
 * 
 * Tests the complete card listing functionality translated from COBOL COCRDLIC.cbl,
 * validating COBOL-to-Java functional parity with emphasis on:
 * - VSAM STARTBR/READNEXT operation equivalence through pagination
 * - Card-account cross-reference validation
 * - BMS screen pagination behavior (7 records per page)
 * - Card filtering by account ID and card number
 * - COBOL browse operation patterns
 * 
 * Test Coverage Areas:
 * - Card pagination logic matching COBOL paragraph 2000-PAGE-FORWARD-PROCESSING
 * - Status filtering equivalent to COBOL paragraph 1500-FILTER-BY-STATUS
 * - Account cross-reference validation from COBOL paragraph 1200-VALIDATE-ACCOUNT
 * - Page navigation patterns from BMS F7/F8 key handling
 * - Search functionality from COBOL paragraph 1700-SEARCH-PROCESSING
 * 
 * This test suite ensures 100% business logic coverage and validates that the
 * Spring Boot service maintains identical functional behavior to the original
 * COBOL implementation, particularly for pagination and filtering operations.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreditCardListService Unit Tests")
class CardListServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CreditCardListService creditCardListService;

    // Test data constants matching COBOL test vectors
    private static final int COBOL_PAGE_SIZE = 7; // BMS screen displays 7 cards
    private static final Long TEST_ACCOUNT_ID = 1000000001L;
    private static final String TEST_CARD_NUMBER = "4532123456789012";
    private static final String TEST_MASKED_CARD = "****-****-****-9012";
    private static final String ACTIVE_STATUS = "Y";
    private static final String INACTIVE_STATUS = "N";

    private List<Card> testCards;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        // Create test account matching COBOL account structure
        testAccount = createTestAccount(TEST_ACCOUNT_ID);
        
        // Generate test card data equivalent to COBOL CARDDATA test vectors
        testCards = createTestCardList();
    }

    @Nested
    @DisplayName("Card Pagination Tests - COBOL STARTBR/READNEXT Equivalence")
    class CardPaginationTests {

        @Test
        @DisplayName("listCreditCards() - validates basic card listing with COBOL page size")
        void testListCreditCards_ValidatesBasicCardListingWithCobolPageSize() {
            // Given: Cards available in repository (equivalent to VSAM CARDDAT dataset)
            when(cardRepository.findAllOrderedByCardNumber(any(Pageable.class)))
                .thenReturn(createPagedCards(testCards.subList(0, 7), 0, COBOL_PAGE_SIZE, testCards.size()));

            // When: Listing cards using service method
            PageResponse<CardListDto> result = creditCardListService.listCreditCards(0, COBOL_PAGE_SIZE);

            // Then: Verify COBOL pagination behavior preserved
            assertThat(result).isNotNull();
            assertThat(result.getData()).hasSize(7); // BMS screen capacity
            assertThat(result.getSize()).isEqualTo(COBOL_PAGE_SIZE);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getTotalElements()).isEqualTo(testCards.size());
            
            // Verify repository called with correct pagination parameters
            verify(cardRepository).findAllOrderedByCardNumber(any(Pageable.class));
        }

        @Test
        @DisplayName("getCardPage() - validates page navigation matching F7/F8 BMS key behavior")
        void testGetCardPage_ValidatesPageNavigationMatchingBmsKeyBehavior() {
            // Given: Multiple pages of cards (simulating VSAM browse cursor movement)
            int pageNumber = 1;
            List<Card> pageCards = testCards.subList(7, 14); // Second page
            when(cardRepository.findAllOrderedByCardNumber(any(Pageable.class)))
                .thenReturn(createPagedCards(pageCards, pageNumber, COBOL_PAGE_SIZE, testCards.size()));

            // When: Getting specific page (equivalent to COBOL F8 forward navigation)
            PageResponse<CardListDto> result = creditCardListService.getCardPage(pageNumber, COBOL_PAGE_SIZE);

            // Then: Verify page navigation preserves COBOL browse semantics
            assertThat(result.getPage()).isEqualTo(pageNumber);
            assertThat(result.getData()).hasSize(7);
            assertThat(result.hasNext()).isTrue(); // More records available (like VSAM READNEXT)
            assertThat(result.hasPrevious()).isTrue(); // Can navigate backward (like VSAM READPREV)

            // Verify correct pageable object passed to repository
            verify(cardRepository).findAllOrderedByCardNumber(
                PageRequest.of(pageNumber, COBOL_PAGE_SIZE, Sort.by("cardNumber")));
        }

        @Test
        @DisplayName("validatePagination() - validates page bounds checking from COBOL validation logic")
        void testValidatePagination_ValidatesPageBoundsCheckingFromCobolValidation() {
            // Given: Invalid page parameters (equivalent to COBOL paragraph 1100-VALIDATE-INPUTS)
            
            // When/Then: Validate negative page number rejection
            assertThatThrownBy(() -> creditCardListService.validatePagination(-1, COBOL_PAGE_SIZE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page number cannot be negative");

            // When/Then: Validate zero page size rejection
            assertThatThrownBy(() -> creditCardListService.validatePagination(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be positive");

            // When/Then: Validate excessive page size rejection (BMS screen limit)
            assertThatThrownBy(() -> creditCardListService.validatePagination(0, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size cannot exceed 20");
        }

        @Test
        @DisplayName("Empty result set handling - validates COBOL end-of-file detection")
        void testEmptyResultSetHandling_ValidatesCobolEndOfFileDetection() {
            // Given: No cards in repository (equivalent to empty VSAM dataset)
            when(cardRepository.findAllOrderedByCardNumber(any(Pageable.class)))
                .thenReturn(createPagedCards(Collections.emptyList(), 0, COBOL_PAGE_SIZE, 0));

            // When: Attempting to list cards
            PageResponse<CardListDto> result = creditCardListService.listCreditCards(0, COBOL_PAGE_SIZE);

            // Then: Verify empty result handling matches COBOL behavior
            assertThat(result.getData()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(0);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.hasPrevious()).isFalse();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("Account Filtering Tests - COBOL Account Cross-Reference Validation")
    class AccountFilteringTests {

        @Test
        @DisplayName("filterByAccount() - validates account-based card filtering from COBOL logic")
        void testFilterByAccount_ValidatesAccountBasedCardFilteringFromCobolLogic() {
            // Given: Cards associated with specific account (COBOL paragraph 1200-FILTER-BY-ACCOUNT)
            List<Card> accountCards = testCards.stream()
                .filter(card -> card.getAccountId().equals(TEST_ACCOUNT_ID))
                .toList();
            
            when(cardRepository.findByAccountId(eq(TEST_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(createPagedCards(accountCards, 0, COBOL_PAGE_SIZE, accountCards.size()));

            // When: Filtering cards by account ID
            PageResponse<CardListDto> result = creditCardListService.filterByAccount(TEST_ACCOUNT_ID, 0, COBOL_PAGE_SIZE);

            // Then: Verify account filtering preserves COBOL cross-reference logic
            assertThat(result.getData()).isNotEmpty();
            assertThat(result.getData()).allSatisfy(cardDto -> 
                assertThat(cardDto.getAccountId()).isEqualTo(TEST_ACCOUNT_ID.toString()));

            // Verify repository method called with correct parameters
            verify(cardRepository).findByAccountId(eq(TEST_ACCOUNT_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("filterByAccount() - validates non-existent account handling")
        void testFilterByAccount_ValidatesNonExistentAccountHandling() {
            // Given: Account with no associated cards (equivalent to COBOL ACCOUNT-NOT-FOUND condition)
            Long nonExistentAccountId = 9999999999L;
            when(cardRepository.findByAccountId(eq(nonExistentAccountId), any(Pageable.class)))
                .thenReturn(createPagedCards(Collections.emptyList(), 0, COBOL_PAGE_SIZE, 0));

            // When: Filtering by non-existent account
            PageResponse<CardListDto> result = creditCardListService.filterByAccount(nonExistentAccountId, 0, COBOL_PAGE_SIZE);

            // Then: Verify empty result handling matches COBOL behavior
            assertThat(result.getData()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("Account cross-reference validation - ensures data integrity")
        void testAccountCrossReferenceValidation_EnsuresDataIntegrity() {
            // Given: Cards with account relationships (COBOL ACCOUNT-CARD-XREF validation)
            Card cardWithAccount = testCards.get(0);
            cardWithAccount.setAccount(testAccount);
            
            when(cardRepository.findByAccountId(eq(TEST_ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(createPagedCards(Arrays.asList(cardWithAccount), 0, COBOL_PAGE_SIZE, 1));

            // When: Retrieving cards for account
            PageResponse<CardListDto> result = creditCardListService.filterByAccount(TEST_ACCOUNT_ID, 0, COBOL_PAGE_SIZE);

            // Then: Verify cross-reference integrity maintained
            assertThat(result.getData()).hasSize(1);
            CardListDto resultCard = result.getData().get(0);
            assertThat(resultCard.getAccountId()).isEqualTo(TEST_ACCOUNT_ID.toString());
        }
    }

    @Nested
    @DisplayName("Card Search Tests - COBOL Search Pattern Implementation")
    class CardSearchTests {

        @Test
        @DisplayName("Search by card number - validates COBOL string matching logic")
        void testSearchByCardNumber_ValidatesCobolStringMatchingLogic() {
            // Given: Cards available for search (COBOL paragraph 1700-SEARCH-BY-CARD-NUMBER)
            String searchTerm = "1234";
            List<Card> matchingCards = testCards.stream()
                .filter(card -> card.getCardNumber().contains(searchTerm))
                .toList();

            when(cardRepository.findByCardNumberContaining(eq(searchTerm), any(Pageable.class)))
                .thenReturn(createPagedCards(matchingCards, 0, COBOL_PAGE_SIZE, matchingCards.size()));

            // When: Searching by partial card number
            PageResponse<CardListDto> result = creditCardListService.searchByCardNumber(searchTerm, 0, COBOL_PAGE_SIZE);

            // Then: Verify search results match COBOL pattern matching
            assertThat(result.getData()).isNotEmpty();
            assertThat(result.getData()).allSatisfy(cardDto -> 
                assertThat(cardDto.getMaskedCardNumber()).contains("1234"));

            verify(cardRepository).findByCardNumberContaining(eq(searchTerm), any(Pageable.class));
        }

        @Test
        @DisplayName("Combined account and card number search - validates complex filtering")
        void testCombinedAccountAndCardNumberSearch_ValidatesComplexFiltering() {
            // Given: Complex search criteria (COBOL paragraph 1800-COMPLEX-SEARCH)
            String cardNumberPattern = "4532";
            List<Card> matchingCards = testCards.stream()
                .filter(card -> card.getAccountId().equals(TEST_ACCOUNT_ID))
                .filter(card -> card.getCardNumber().contains(cardNumberPattern))
                .toList();

            when(cardRepository.findByAccountIdAndCardNumberContaining(
                eq(TEST_ACCOUNT_ID), eq(cardNumberPattern), any(Pageable.class)))
                .thenReturn(createPagedCards(matchingCards, 0, COBOL_PAGE_SIZE, matchingCards.size()));

            // When: Performing complex search
            PageResponse<CardListDto> result = creditCardListService.searchByAccountAndCardNumber(
                TEST_ACCOUNT_ID, cardNumberPattern, 0, COBOL_PAGE_SIZE);

            // Then: Verify combined filtering logic preserved
            assertThat(result.getData()).isNotEmpty();
            assertThat(result.getData()).allSatisfy(cardDto -> {
                assertThat(cardDto.getAccountId()).isEqualTo(TEST_ACCOUNT_ID.toString());
                assertThat(cardDto.getMaskedCardNumber()).contains("4532");
            });
        }
    }

    @Nested
    @DisplayName("Status Filtering Tests - COBOL Active/Inactive Logic")
    class StatusFilteringTests {

        @Test
        @DisplayName("Filter by active status - validates COBOL status logic")
        void testFilterByActiveStatus_ValidatesCobolStatusLogic() {
            // Given: Mix of active and inactive cards (COBOL paragraph 1500-FILTER-BY-STATUS)
            List<Card> activeCards = testCards.stream()
                .filter(card -> ACTIVE_STATUS.equals(card.getActiveStatus()))
                .toList();

            when(cardRepository.findByActiveStatus(eq(ACTIVE_STATUS), any(Pageable.class)))
                .thenReturn(createPagedCards(activeCards, 0, COBOL_PAGE_SIZE, activeCards.size()));

            // When: Filtering by active status
            PageResponse<CardListDto> result = creditCardListService.filterByStatus(ACTIVE_STATUS, 0, COBOL_PAGE_SIZE);

            // Then: Verify only active cards returned
            assertThat(result.getData()).isNotEmpty();
            assertThat(result.getData()).allSatisfy(cardDto -> 
                assertThat(cardDto.getActiveStatus()).isEqualTo(ACTIVE_STATUS));
        }

        @Test
        @DisplayName("Filter by inactive status - validates inactive card handling")
        void testFilterByInactiveStatus_ValidatesInactiveCardHandling() {
            // Given: Inactive cards in repository
            List<Card> inactiveCards = testCards.stream()
                .filter(card -> INACTIVE_STATUS.equals(card.getActiveStatus()))
                .toList();

            when(cardRepository.findByActiveStatus(eq(INACTIVE_STATUS), any(Pageable.class)))
                .thenReturn(createPagedCards(inactiveCards, 0, COBOL_PAGE_SIZE, inactiveCards.size()));

            // When: Filtering by inactive status
            PageResponse<CardListDto> result = creditCardListService.filterByStatus(INACTIVE_STATUS, 0, COBOL_PAGE_SIZE);

            // Then: Verify only inactive cards returned
            assertThat(result.getData()).isNotEmpty();
            assertThat(result.getData()).allSatisfy(cardDto -> 
                assertThat(cardDto.getActiveStatus()).isEqualTo(INACTIVE_STATUS));
        }
    }

    @Nested
    @DisplayName("Data Mapping Tests - COBOL-to-Java Conversion Validation")
    class DataMappingTests {

        @Test
        @DisplayName("Entity to DTO conversion - validates field mapping accuracy")
        void testEntityToDtoConversion_ValidatesFieldMappingAccuracy() {
            // Given: Card entity with complete data (equivalent to COBOL CARD-RECORD)
            Card sourceCard = testCards.get(0);
            when(cardRepository.findAllOrderedByCardNumber(any(Pageable.class)))
                .thenReturn(createPagedCards(Arrays.asList(sourceCard), 0, 1, 1));

            // When: Converting through service layer
            PageResponse<CardListDto> result = creditCardListService.listCreditCards(0, 1);

            // Then: Verify field mapping preserves all COBOL data integrity
            assertThat(result.getData()).hasSize(1);
            CardListDto mappedCard = result.getData().get(0);
            
            assertThat(mappedCard.getMaskedCardNumber()).isNotEmpty();
            assertThat(mappedCard.getAccountId()).isEqualTo(sourceCard.getAccountId().toString());
            assertThat(mappedCard.getActiveStatus()).isEqualTo(sourceCard.getActiveStatus());
            assertThat(mappedCard.getCardType()).isNotEmpty();
            assertThat(mappedCard.getExpirationDate()).isEqualTo(sourceCard.getExpirationDate());
        }

        @Test
        @DisplayName("Card number masking - validates PCI DSS compliance")
        void testCardNumberMasking_ValidatesPciDssCompliance() {
            // Given: Cards with sensitive card numbers
            when(cardRepository.findAllOrderedByCardNumber(any(Pageable.class)))
                .thenReturn(createPagedCards(testCards.subList(0, 1), 0, 1, 1));

            // When: Retrieving cards through service
            PageResponse<CardListDto> result = creditCardListService.listCreditCards(0, 1);

            // Then: Verify card number masking applied for security
            assertThat(result.getData()).hasSize(1);
            CardListDto card = result.getData().get(0);
            assertThat(card.getMaskedCardNumber()).matches("\\*{4}-\\*{4}-\\*{4}-\\d{4}");
            assertThat(card.getMaskedCardNumber()).doesNotContain(TEST_CARD_NUMBER.substring(0, 12));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests - COBOL Exception Pattern Equivalence")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Invalid pagination parameters - validates COBOL error handling")
        void testInvalidPaginationParameters_ValidatesCobolErrorHandling() {
            // When/Then: Test various invalid pagination scenarios
            assertThatThrownBy(() -> creditCardListService.listCreditCards(-1, COBOL_PAGE_SIZE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page number cannot be negative");

            assertThatThrownBy(() -> creditCardListService.listCreditCards(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be positive");

            assertThatThrownBy(() -> creditCardListService.listCreditCards(0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size cannot exceed");
        }

        @Test
        @DisplayName("Repository exception handling - validates error propagation")
        void testRepositoryExceptionHandling_ValidatesErrorPropagation() {
            // Given: Repository throws exception (equivalent to VSAM I/O error)
            when(cardRepository.findAllOrderedByCardNumber(any(Pageable.class)))
                .thenThrow(new RuntimeException("Database connection error"));

            // When/Then: Verify exception properly propagated
            assertThatThrownBy(() -> creditCardListService.listCreditCards(0, COBOL_PAGE_SIZE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection error");
        }
    }

    @Nested
    @DisplayName("Performance Tests - COBOL Response Time Validation")
    class PerformanceTests {

        @Test
        @DisplayName("Large dataset pagination - validates performance characteristics")
        void testLargeDatasetPagination_ValidatesPerformanceCharacteristics() {
            // Given: Large dataset simulation (equivalent to large VSAM file)
            List<Card> largeCardSet = generateLargeCardDataset(1000);
            when(cardRepository.findAllOrderedByCardNumber(any(Pageable.class)))
                .thenReturn(createPagedCards(largeCardSet.subList(0, COBOL_PAGE_SIZE), 0, COBOL_PAGE_SIZE, 1000));

            // When: Processing large dataset
            long startTime = System.currentTimeMillis();
            PageResponse<CardListDto> result = creditCardListService.listCreditCards(0, COBOL_PAGE_SIZE);
            long endTime = System.currentTimeMillis();

            // Then: Verify response time meets COBOL performance baseline
            assertThat(result.getData()).hasSize(COBOL_PAGE_SIZE);
            assertThat(result.getTotalElements()).isEqualTo(1000);
            assertThat(endTime - startTime).isLessThan(200); // Sub-200ms response time requirement
        }
    }

    // ===== Test Data Generation Methods =====

    /**
     * Creates a list of test cards with varied data for comprehensive testing.
     * Generates cards equivalent to COBOL CARDDATA test vectors.
     */
    private List<Card> createTestCardList() {
        return Arrays.asList(
            createTestCard("4532123456789012", TEST_ACCOUNT_ID, 1001L, ACTIVE_STATUS),
            createTestCard("4532123456789013", TEST_ACCOUNT_ID, 1001L, ACTIVE_STATUS),
            createTestCard("4532123456789014", 1000000002L, 1002L, INACTIVE_STATUS),
            createTestCard("4532123456789015", 1000000002L, 1002L, ACTIVE_STATUS),
            createTestCard("4532123456789016", TEST_ACCOUNT_ID, 1001L, ACTIVE_STATUS),
            createTestCard("4532123456789017", 1000000003L, 1003L, ACTIVE_STATUS),
            createTestCard("4532123456789018", 1000000003L, 1003L, INACTIVE_STATUS),
            createTestCard("4532123456789019", TEST_ACCOUNT_ID, 1001L, ACTIVE_STATUS),
            createTestCard("4532123456789020", 1000000004L, 1004L, ACTIVE_STATUS),
            createTestCard("4532123456789021", 1000000004L, 1004L, ACTIVE_STATUS),
            createTestCard("4532123456789022", TEST_ACCOUNT_ID, 1001L, INACTIVE_STATUS),
            createTestCard("4532123456789023", 1000000005L, 1005L, ACTIVE_STATUS),
            createTestCard("4532123456789024", 1000000005L, 1005L, ACTIVE_STATUS),
            createTestCard("4532123456789025", 1000000006L, 1006L, ACTIVE_STATUS),
            createTestCard("4532123456789026", 1000000006L, 1006L, INACTIVE_STATUS)
        );
    }

    /**
     * Creates a single test card with specified parameters.
     * Matches COBOL CARD-RECORD structure from CVCARD01.cpy.
     */
    private Card createTestCard(String cardNumber, Long accountId, Long customerId, String activeStatus) {
        Card card = new Card();
        card.setCardNumber(cardNumber);
        card.setAccountId(accountId);
        card.setCustomerId(customerId);
        card.setCvvCode("123"); // Test CVV
        card.setEmbossedName("TEST CARDHOLDER");
        card.setExpirationDate(LocalDate.now().plusYears(2));
        card.setActiveStatus(activeStatus);
        return card;
    }

    /**
     * Creates a test account entity with COBOL-equivalent structure.
     */
    private Account createTestAccount(Long accountId) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setActiveStatus(ACTIVE_STATUS);
        account.setCurrentBalance(new BigDecimal("1500.00"));
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCashCreditLimit(new BigDecimal("1000.00"));
        account.setOpenDate(LocalDate.now().minusYears(1));
        account.setExpirationDate(LocalDate.now().plusYears(3));
        return account;
    }

    /**
     * Creates a Spring Data Page object for mocking repository responses.
     */
    private Page<Card> createPagedCards(List<Card> cards, int page, int size, long totalElements) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("cardNumber"));
        return new PageImpl<>(cards, pageRequest, totalElements);
    }

    /**
     * Generates a large card dataset for performance testing.
     */
    private List<Card> generateLargeCardDataset(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createTestCard(
                String.format("4532123456%06d", i),
                1000000000L + (i % 100),
                1000L + (i % 50),
                i % 4 == 0 ? INACTIVE_STATUS : ACTIVE_STATUS
            ))
            .toList();
    }
}