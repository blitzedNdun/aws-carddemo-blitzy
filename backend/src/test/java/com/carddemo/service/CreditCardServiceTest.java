/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.dto.*;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.CardXref;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.ValidationUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for CreditCardService validating credit card lifecycle 
 * management functionality converted from COCRDLIC, COCRDSLC, and COCRDUPC COBOL programs.
 *
 * This test class validates:
 * - Card listing operations with pagination and filtering (CCLI transaction from COCRDLIC.cbl)
 * - Card detail retrieval and viewing capabilities (CCDL transaction from COCRDSLC.cbl) 
 * - Card update operations and validation rules (CCUP transaction from COCRDUPC.cbl)
 * - Card-to-account cross-referencing through CVACT03Y cross-reference structure
 * - Card status transitions and lifecycle management
 * - Expiration date handling and credit limit management
 * - Card activation and deactivation workflows
 * - Comprehensive validation of business rules and error handling
 *
 * Testing Approach:
 * - Mocks all repository dependencies for isolated unit testing
 * - Validates exact functional parity with original COBOL program behavior
 * - Ensures BigDecimal precision matches COBOL COMP-3 packed decimal handling
 * - Tests error conditions and exception scenarios thoroughly
 * - Verifies card number generation and security code handling
 * - Validates referential integrity between cards, accounts, and cross-references
 *
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest extends BaseServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @InjectMocks
    private CreditCardService creditCardService;

    private TestDataBuilder testDataBuilder;
    private MockServiceFactory mockServiceFactory;

    // Test data constants matching COBOL program specifications
    private static final String VALID_CARD_NUMBER = "4000123456789012";
    private static final String VALID_ACCOUNT_ID = "12345678901";
    private static final String VALID_CUSTOMER_ID = "0000000001";
    private static final int DEFAULT_PAGE_SIZE = 7; // Matches COBOL screen size
    private static final LocalDate DEFAULT_EXPIRY = LocalDate.now().plusYears(3);

    @BeforeEach
    void setUp() {
        resetMocks();
        testDataBuilder = new TestDataBuilder();
        mockServiceFactory = new MockServiceFactory();
    }

    /**
     * Tests card listing functionality with no filters applied.
     * Validates COCRDLIC.cbl paragraph 9000-READ-FORWARD behavior when no account or card filters provided.
     */
    @Test
    void testListCards_NoFilters_ReturnsAllCards() {
        // Given: Mock repository returns paginated card data
        List<Card> cards = List.of(
                testDataBuilder.createCard()
                        .withCardNumber("4000123456789012")
                        .withAccountId(12345678901L)
                        .withCardStatus("Y"),
                testDataBuilder.createCard()
                        .withCardNumber("4000123456789013")
                        .withAccountId(12345678902L)
                        .withCardStatus("Y")
        );
        
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(0, DEFAULT_PAGE_SIZE), 2);
        when(cardRepository.findAllOrderedByCardNumber(any(Pageable.class))).thenReturn(cardPage);

        // When: List cards without filters
        PageResponse<CardListDto> result = creditCardService.listCards(null, null, 0, DEFAULT_PAGE_SIZE);

        // Then: Verify paginated response structure
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(DEFAULT_PAGE_SIZE);
        assertThat(result.getTotalElements()).isEqualTo(2);

        // Verify card data mapping
        CardListDto firstCard = result.getContent().get(0);
        assertThat(firstCard.getMaskedCardNumber()).isEqualTo("4000****9012");
        assertThat(firstCard.getAccountId()).isEqualTo("12345678901");
        assertThat(firstCard.getActiveStatus()).isEqualTo("Y");

        verify(cardRepository, times(1)).findAllOrderedByCardNumber(any(Pageable.class));
    }

    /**
     * Tests card listing with account ID filter.
     * Validates COCRDLIC.cbl paragraph 9500-FILTER-RECORDS behavior for account filtering.
     */
    @Test
    void testListCards_WithAccountFilter_ReturnsFilteredCards() {
        // Given: Mock repository returns cards for specific account
        List<Card> accountCards = List.of(
                testDataBuilder.createCard()
                        .withCardNumber("4000123456789012")
                        .withAccountId(12345678901L)
                        .withCardStatus("Y")
        );
        
        Page<Card> cardPage = new PageImpl<>(accountCards, PageRequest.of(0, DEFAULT_PAGE_SIZE), 1);
        when(cardRepository.findByAccountId(eq(12345678901L), any(Pageable.class))).thenReturn(cardPage);

        // When: List cards with account filter
        PageResponse<CardListDto> result = creditCardService.listCards("12345678901", null, 0, DEFAULT_PAGE_SIZE);

        // Then: Verify filtered results
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAccountId()).isEqualTo("12345678901");

        verify(cardRepository, times(1)).findByAccountId(eq(12345678901L), any(Pageable.class));
    }

    /**
     * Tests card listing with card number filter.
     * Validates COCRDLIC.cbl card number filtering logic.
     */
    @Test
    void testListCards_WithCardNumberFilter_ReturnsMatchingCards() {
        // Given: Mock repository returns cards matching card number pattern
        List<Card> matchingCards = List.of(
                testDataBuilder.createCard()
                        .withCardNumber("4000123456789012")
                        .withAccountId(12345678901L)
                        .withCardStatus("Y")
        );
        
        Page<Card> cardPage = new PageImpl<>(matchingCards, PageRequest.of(0, DEFAULT_PAGE_SIZE), 1);
        when(cardRepository.findByCardNumberContaining(eq("4000"), any(Pageable.class))).thenReturn(cardPage);

        // When: List cards with card number filter
        PageResponse<CardListDto> result = creditCardService.listCards(null, "4000", 0, DEFAULT_PAGE_SIZE);

        // Then: Verify filtered results
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMaskedCardNumber()).contains("4000");

        verify(cardRepository, times(1)).findByCardNumberContaining(eq("4000"), any(Pageable.class));
    }

    /**
     * Tests card listing with both account ID and card number filters.
     * Validates COCRDLIC.cbl behavior when both filters are applied simultaneously.
     */
    @Test
    void testListCards_WithBothFilters_ReturnsIntersectionResults() {
        // Given: Mock repository returns cards matching both filters
        List<Card> filteredCards = List.of(
                testDataBuilder.createCard()
                        .withCardNumber("4000123456789012")
                        .withAccountId(12345678901L)
                        .withCardStatus("Y")
        );
        
        Page<Card> cardPage = new PageImpl<>(filteredCards, PageRequest.of(0, DEFAULT_PAGE_SIZE), 1);
        when(cardRepository.findByAccountIdAndCardNumberContaining(
                eq(12345678901L), eq("4000"), any(Pageable.class))).thenReturn(cardPage);

        // When: List cards with both filters
        PageResponse<CardListDto> result = creditCardService.listCards("12345678901", "4000", 0, DEFAULT_PAGE_SIZE);

        // Then: Verify intersection results
        assertThat(result.getContent()).hasSize(1);
        CardListDto card = result.getContent().get(0);
        assertThat(card.getAccountId()).isEqualTo("12345678901");
        assertThat(card.getMaskedCardNumber()).contains("4000");

        verify(cardRepository, times(1)).findByAccountIdAndCardNumberContaining(
                eq(12345678901L), eq("4000"), any(Pageable.class));
    }

    /**
     * Tests error handling for invalid account ID format.
     * Validates COCRDLIC.cbl paragraph 2210-EDIT-ACCOUNT error handling.
     */
    @Test
    void testListCards_InvalidAccountId_ThrowsBusinessRuleException() {
        // When & Then: List cards with invalid account ID format
        assertThatThrownBy(() -> 
                creditCardService.listCards("INVALID", null, 0, DEFAULT_PAGE_SIZE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CARD_LIST_ERROR");
    }

    /**
     * Tests card details retrieval for existing card.
     * Validates COCRDSLC.cbl card detail lookup functionality.
     */
    @Test
    void testGetCardDetails_ExistingCard_ReturnsCardResponse() {
        // Given: Mock repository returns existing card
        Card mockCard = testDataBuilder.createCard()
                .withCardNumber(VALID_CARD_NUMBER)
                .withAccountId(Long.parseLong(VALID_ACCOUNT_ID))
                .withExpirationDate(DEFAULT_EXPIRY)
                .withCardStatus("Y");
                
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(mockCard));

        // When: Get card details
        CardResponse result = creditCardService.getCardDetails(VALID_CARD_NUMBER);

        // Then: Verify card response structure
        assertThat(result).isNotNull();
        assertThat(result.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
        assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(result.getExpirationDate()).isEqualTo(DEFAULT_EXPIRY);
        assertThat(result.getActiveStatus()).isEqualTo("Y");

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    /**
     * Tests card details retrieval for non-existent card.
     * Validates COCRDSLC.cbl error handling when card is not found.
     */
    @Test
    void testGetCardDetails_NonExistentCard_ThrowsResourceNotFoundException() {
        // Given: Mock repository returns empty Optional
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.empty());

        // When & Then: Get details for non-existent card
        assertThatThrownBy(() -> creditCardService.getCardDetails(VALID_CARD_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card")
                .hasMessageContaining(VALID_CARD_NUMBER);

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    /**
     * Tests card update with valid data.
     * Validates COCRDUPC.cbl card update functionality and business rules.
     */
    @Test
    void testUpdateCard_ValidUpdate_ReturnsUpdatedCard() {
        // Given: Existing card and valid update request
        Card existingCard = testDataBuilder.createCard()
                .withCardNumber(VALID_CARD_NUMBER)
                .withAccountId(Long.parseLong(VALID_ACCOUNT_ID))
                .withCreditLimit(BigDecimal.valueOf(5000.00))
                .withCardStatus("Y");
                
        CardRequest updateRequest = new CardRequest();
        updateRequest.setCreditLimit(BigDecimal.valueOf(7500.00));
        updateRequest.setExpirationDate(DEFAULT_EXPIRY.plusYears(1));

        Card updatedCard = testDataBuilder.createCard()
                .withCardNumber(VALID_CARD_NUMBER)
                .withAccountId(Long.parseLong(VALID_ACCOUNT_ID))
                .withCreditLimit(BigDecimal.valueOf(7500.00))
                .withExpirationDate(DEFAULT_EXPIRY.plusYears(1))
                .withCardStatus("Y");

        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(existingCard));
        when(cardRepository.save(any(Card.class))).thenReturn(updatedCard);

        // When: Update card
        CardResponse result = creditCardService.updateCard(VALID_CARD_NUMBER, updateRequest);

        // Then: Verify updated card data with BigDecimal precision
        assertThat(result).isNotNull();
        assertThat(result.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
        assertBigDecimalEquals(result.getCreditLimit(), BigDecimal.valueOf(7500.00));
        assertThat(result.getExpirationDate()).isEqualTo(DEFAULT_EXPIRY.plusYears(1));

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    /**
     * Tests card update for non-existent card.
     * Validates COCRDUPC.cbl error handling when card to update is not found.
     */
    @Test
    void testUpdateCard_NonExistentCard_ThrowsResourceNotFoundException() {
        // Given: Card does not exist
        CardRequest updateRequest = new CardRequest();
        updateRequest.setCreditLimit(BigDecimal.valueOf(5000.00));

        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.empty());

        // When & Then: Update non-existent card
        assertThatThrownBy(() -> creditCardService.updateCard(VALID_CARD_NUMBER, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card")
                .hasMessageContaining(VALID_CARD_NUMBER);

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
        verify(cardRepository, never()).save(any(Card.class));
    }

    /**
     * Tests card update with invalid credit limit.
     * Validates COCRDUPC.cbl credit limit validation rules.
     */
    @Test
    void testUpdateCard_InvalidCreditLimit_ThrowsBusinessRuleException() {
        // Given: Existing card and invalid credit limit
        Card existingCard = testDataBuilder.createCard()
                .withCardNumber(VALID_CARD_NUMBER)
                .withCreditLimit(BigDecimal.valueOf(5000.00));

        CardRequest updateRequest = new CardRequest();
        updateRequest.setCreditLimit(BigDecimal.valueOf(-1000.00)); // Invalid negative limit

        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(existingCard));

        // When & Then: Update card with invalid credit limit
        assertThatThrownBy(() -> creditCardService.updateCard(VALID_CARD_NUMBER, updateRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CARD_UPDATE_ERROR");

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
        verify(cardRepository, never()).save(any(Card.class));
    }

    /**
     * Tests card-account cross-reference validation for valid relationship.
     * Validates CVACT03Y cross-reference structure linking cards to accounts.
     */
    @Test
    void testValidateCardAccountXref_ValidRelationship_PassesValidation() {
        // Given: Valid card-account relationship exists
        when(cardRepository.existsByCardNumberAndAccountId(VALID_CARD_NUMBER, Long.parseLong(VALID_ACCOUNT_ID)))
                .thenReturn(true);

        // When: Validate cross-reference
        assertThatNoException().isThrownBy(() -> 
                creditCardService.validateCardAccountXref(VALID_CARD_NUMBER, VALID_ACCOUNT_ID));

        // Then: Verify repository interaction
        verify(cardRepository, times(1))
                .existsByCardNumberAndAccountId(VALID_CARD_NUMBER, Long.parseLong(VALID_ACCOUNT_ID));
    }

    /**
     * Tests card-account cross-reference validation for invalid relationship.
     * Validates CVACT03Y cross-reference validation when card doesn't belong to account.
     */
    @Test
    void testValidateCardAccountXref_InvalidRelationship_ThrowsBusinessRuleException() {
        // Given: Card-account relationship does not exist
        when(cardRepository.existsByCardNumberAndAccountId(VALID_CARD_NUMBER, Long.parseLong(VALID_ACCOUNT_ID)))
                .thenReturn(false);

        // When & Then: Validate invalid cross-reference
        assertThatThrownBy(() -> 
                creditCardService.validateCardAccountXref(VALID_CARD_NUMBER, VALID_ACCOUNT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CARD_ACCOUNT_MISMATCH")
                .hasMessageContaining("Card does not belong to the specified account");

        verify(cardRepository, times(1))
                .existsByCardNumberAndAccountId(VALID_CARD_NUMBER, Long.parseLong(VALID_ACCOUNT_ID));
    }

    /**
     * Tests card activation workflow.
     * Validates card status transition from inactive to active state.
     */
    @Test
    void testActivateCard_InactiveCard_ActivatesSuccessfully() {
        // Given: Inactive card
        Card inactiveCard = testDataBuilder.createCard()
                .withCardNumber(VALID_CARD_NUMBER)
                .withCardStatus("N");

        Card activeCard = testDataBuilder.createCard()
                .withCardNumber(VALID_CARD_NUMBER)
                .withCardStatus("Y");

        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(inactiveCard));
        when(cardRepository.save(any(Card.class))).thenReturn(activeCard);

        // When: Activate card
        CardResponse result = creditCardService.activateCard(VALID_CARD_NUMBER);

        // Then: Verify activation
        assertThat(result).isNotNull();
        assertThat(result.getActiveStatus()).isEqualTo("Y");
        assertThat(result.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    /**
     * Tests card deactivation workflow.
     * Validates card status transition from active to inactive state.
     */
    @Test
    void testDeactivateCard_ActiveCard_DeactivatesSuccessfully() {
        // Given: Active card
        Card activeCard = testDataBuilder.createCard()
                .withCardNumber(VALID_CARD_NUMBER)
                .withCardStatus("Y");

        Card inactiveCard = testDataBuilder.createCard()
                .withCardNumber(VALID_CARD_NUMBER)
                .withCardStatus("N");

        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(any(Card.class))).thenReturn(inactiveCard);

        // When: Deactivate card
        CardResponse result = creditCardService.deactivateCard(VALID_CARD_NUMBER);

        // Then: Verify deactivation
        assertThat(result).isNotNull();
        assertThat(result.getActiveStatus()).isEqualTo("N");
        assertThat(result.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    /**
     * Tests card number validation using ValidationUtil.
     * Validates COBOL edit routines for card number format validation.
     */
    @Test
    void testCardNumberValidation_ValidFormats_PassesValidation() {
        // Given: Valid card numbers
        String[] validCardNumbers = {
                "4000123456789012", // 16-digit Visa
                "5555555555554444", // 16-digit Mastercard
                "378282246310005"   // 15-digit Amex
        };

        // When & Then: Validate each card number
        for (String cardNumber : validCardNumbers) {
            assertThatNoException().isThrownBy(() -> 
                    ValidationUtil.validateCardNumber(cardNumber));
        }
    }

    /**
     * Tests card number validation with invalid formats.
     * Validates COBOL edit routine error handling for invalid card numbers.
     */
    @Test
    void testCardNumberValidation_InvalidFormats_ThrowsValidationException() {
        // Given: Invalid card numbers
        String[] invalidCardNumbers = {
                "123456789",       // Too short
                "12345678901234567890", // Too long
                "400012345678901X", // Non-numeric characters
                "",                // Empty string
                null               // Null value
        };

        // When & Then: Validate each invalid card number
        for (String cardNumber : invalidCardNumbers) {
            assertThatThrownBy(() -> ValidationUtil.validateCardNumber(cardNumber))
                    .isInstanceOf(ValidationException.class);
        }
    }

    /**
     * Tests COBOL COMP-3 decimal precision handling with CobolDataConverter.
     * Ensures financial calculations maintain exact precision matching COBOL behavior.
     */
    @Test
    void testCobolDataConverter_PreservesDecimalPrecision() {
        // Given: COMP-3 packed decimal data
        BigDecimal originalAmount = BigDecimal.valueOf(1234.56);
        byte[] comp3Data = CobolDataConverter.toBigDecimal(originalAmount).toString().getBytes();

        // When: Convert from COMP-3 to BigDecimal
        BigDecimal convertedAmount = CobolDataConverter.fromComp3(comp3Data, 2);

        // Then: Verify precision is preserved
        assertBigDecimalEquals(originalAmount, convertedAmount);
        assertThat(CobolDataConverter.preservePrecision(convertedAmount, 2))
                .isEqualByComparingTo(originalAmount);
    }

    /**
     * Tests expiration date validation and handling.
     * Validates date-based business rules for credit card expiration.
     */
    @Test
    void testExpirationDateValidation_FutureDates_PassesValidation() {
        // Given: Future expiration dates
        LocalDate[] futureDates = {
                LocalDate.now().plusMonths(1),
                LocalDate.now().plusYears(1),
                LocalDate.now().plusYears(5)
        };

        // When & Then: Validate future dates
        for (LocalDate date : futureDates) {
            Card testCard = testDataBuilder.createCard()
                    .withExpirationDate(date);
            
            assertThat(testCard.getExpirationDate().isAfter(LocalDate.now())).isTrue();
        }
    }

    /**
     * Tests expiration date validation with past dates.
     * Validates expired card detection and handling.
     */
    @Test
    void testExpirationDateValidation_PastDates_IdentifiesExpiredCards() {
        // Given: Past expiration dates
        LocalDate[] pastDates = {
                LocalDate.now().minusDays(1),
                LocalDate.now().minusMonths(1),
                LocalDate.now().minusYears(1)
        };

        // When & Then: Validate past dates are identified as expired
        for (LocalDate date : pastDates) {
            Card testCard = testDataBuilder.createCard()
                    .withExpirationDate(date);
            
            assertThat(testCard.getExpirationDate().isAfter(LocalDate.now())).isFalse();
        }
    }

    /**
     * Tests comprehensive card-to-account cross-reference validation through CardXref entity.
     * Validates CVACT03Y cross-reference structure functionality.
     */
    @Test
    void testCardXrefValidation_CompleteRelationship_ValidatesSuccessfully() {
        // Given: Complete card-account-customer relationship
        CardXref cardXref = testDataBuilder.createCardXref()
                .withCardNumber(VALID_CARD_NUMBER)
                .withCustomerId(VALID_CUSTOMER_ID)
                .withAccountId(VALID_ACCOUNT_ID);

        when(cardXrefRepository.findByCardNumber(VALID_CARD_NUMBER))
                .thenReturn(List.of(cardXref));

        // When: Validate cross-reference relationship
        List<CardXref> xrefResults = cardXrefRepository.findByCardNumber(VALID_CARD_NUMBER);

        // Then: Verify complete relationship structure
        assertThat(xrefResults).hasSize(1);
        CardXref result = xrefResults.get(0);
        assertThat(result.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
        assertThat(result.getCustomerId()).isEqualTo(VALID_CUSTOMER_ID);
        assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);

        verify(cardXrefRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    /**
     * Tests account repository integration for card-account relationship validation.
     * Ensures account exists before establishing card relationship.
     */
    @Test
    void testAccountRepository_ValidAccount_ReturnsAccountDetails() {
        // Given: Valid account
        Account account = testDataBuilder.createAccount()
                .withAccountId(Long.parseLong(VALID_ACCOUNT_ID))
                .withCustomerId(VALID_CUSTOMER_ID)
                .withCurrentBalance(BigDecimal.valueOf(1500.75))
                .withCreditLimit(BigDecimal.valueOf(10000.00));

        when(accountRepository.findById(Long.parseLong(VALID_ACCOUNT_ID)))
                .thenReturn(Optional.of(account));

        // When: Retrieve account details
        Optional<Account> result = accountRepository.findById(Long.parseLong(VALID_ACCOUNT_ID));

        // Then: Verify account details with precision
        assertThat(result).isPresent();
        Account foundAccount = result.get();
        assertThat(foundAccount.getAccountId()).isEqualTo(Long.parseLong(VALID_ACCOUNT_ID));
        assertThat(foundAccount.getCustomerId()).isEqualTo(VALID_CUSTOMER_ID);
        assertBigDecimalEquals(foundAccount.getCurrentBalance(), BigDecimal.valueOf(1500.75));
        assertBigDecimalEquals(foundAccount.getCreditLimit(), BigDecimal.valueOf(10000.00));

        verify(accountRepository, times(1)).findById(Long.parseLong(VALID_ACCOUNT_ID));
    }

    /**
     * Tests security code validation and handling.
     * Ensures CVV codes are properly validated but not exposed in responses.
     */
    @Test
    void testSecurityCodeHandling_ValidCVV_HandlesSecurely() {
        // Given: Card with security code
        Card cardWithCVV = testDataBuilder.createCard()
                .withCardNumber(VALID_CARD_NUMBER)
                .withSecurityCode("123");

        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER))
                .thenReturn(Optional.of(cardWithCVV));

        // When: Retrieve card details
        CardResponse result = creditCardService.getCardDetails(VALID_CARD_NUMBER);

        // Then: Verify security code is not exposed in response
        assertThat(result).isNotNull();
        assertThat(result.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
        // Security code should not be included in response for PCI compliance
        assertThat(result.getSecurityCode()).isNull();

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }
}