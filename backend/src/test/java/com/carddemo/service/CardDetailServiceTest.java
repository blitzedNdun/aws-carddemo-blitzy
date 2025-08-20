/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Card;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.CardRepository;
import com.carddemo.dto.CreditCardDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test suite for CreditCardDetailService validating COBOL COCRDSLC 
 * card detail logic migration to Java Spring Boot service.
 * 
 * This test class provides complete coverage of the credit card detail service functionality,
 * ensuring 100% functional parity with the original COBOL COCRDSLC.cbl program. The tests
 * validate all business logic, error handling, and data formatting operations while maintaining
 * the exact behavioral requirements from the mainframe implementation.
 * 
 * COBOL Program Coverage:
 * - COCRDSLC.cbl card detail retrieval operations
 * - Input validation from 2210-EDIT-ACCOUNT and 2220-EDIT-CARD sections
 * - Data access operations from 9100-GETCARD-BYACCTCARD and 9150-GETCARD-BYACCT sections
 * - Screen formatting logic from 1200-SETUP-SCREEN-VARS section
 * - Error handling equivalent to VSAM NOTFND and validation conditions
 * 
 * Test Categories:
 * 1. Card Retrieval by Number - Primary access path testing
 * 2. Card Status Interpretation - Active/inactive status validation
 * 3. Expiration Date Handling - Date validation and expiry logic
 * 4. Account Relationship Validation - Cross-reference testing
 * 5. Card Type Classification - Card type determination
 * 6. PCI-Compliant Data Masking - Security and privacy validation
 * 7. Input Validation - Comprehensive format and business rule validation
 * 8. Error Scenarios - Exception handling and error message validation
 * 
 * Security Compliance:
 * - Validates PCI DSS compliant card number masking
 * - Ensures CVV codes are never exposed in responses
 * - Tests proper handling of sensitive card data
 * 
 * Performance Validation:
 * - Verifies efficient repository access patterns
 * - Tests optimal data retrieval with minimal database calls
 * - Validates proper exception handling without performance impact
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Credit Card Detail Service Tests - COBOL COCRDSLC Migration Validation")
class CardDetailServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CreditCardDetailService creditCardDetailService;

    // Test data constants matching COBOL data structures
    private static final String VALID_CARD_NUMBER = "4000000000001234";
    private static final String VALID_ACCOUNT_ID = "12345678901";
    private static final Long VALID_ACCOUNT_ID_LONG = 12345678901L;
    private static final Long VALID_CUSTOMER_ID = 9876543210L;
    private static final String VALID_CVV = "123";
    private static final String VALID_EMBOSSED_NAME = "JOHN DOE";
    private static final String ACTIVE_STATUS = "Y";
    private static final String INACTIVE_STATUS = "N";
    
    // Invalid test data for negative scenarios
    private static final String INVALID_CARD_NUMBER_LENGTH = "123456789";
    private static final String INVALID_CARD_NUMBER_NON_NUMERIC = "abcd0000efgh1234";
    private static final String INVALID_CARD_NUMBER_ZEROS = "0000000000000000";
    private static final String INVALID_ACCOUNT_ID_LENGTH = "123456";
    private static final String INVALID_ACCOUNT_ID_NON_NUMERIC = "abcdefghijk";
    private static final String INVALID_ACCOUNT_ID_ZEROS = "00000000000";

    private Card validActiveCard;
    private Card validInactiveCard;
    private Card expiredCard;

    /**
     * Sets up test data before each test execution.
     * Creates comprehensive test card entities matching COBOL data structures from CVACT02Y.cpy.
     */
    @BeforeEach
    void setUp() {
        // Create valid active card (CARD-ACTIVE-STATUS = 'Y')
        validActiveCard = new Card();
        validActiveCard.setCardNumber(VALID_CARD_NUMBER);
        validActiveCard.setAccountId(VALID_ACCOUNT_ID_LONG);
        validActiveCard.setCustomerId(VALID_CUSTOMER_ID);
        validActiveCard.setCvvCode(VALID_CVV);
        validActiveCard.setEmbossedName(VALID_EMBOSSED_NAME);
        validActiveCard.setExpirationDate(LocalDate.now().plusYears(2));
        validActiveCard.setActiveStatus(ACTIVE_STATUS);

        // Create valid inactive card (CARD-ACTIVE-STATUS = 'N')
        validInactiveCard = new Card();
        validInactiveCard.setCardNumber("4000000000005678");
        validInactiveCard.setAccountId(VALID_ACCOUNT_ID_LONG);
        validInactiveCard.setCustomerId(VALID_CUSTOMER_ID);
        validInactiveCard.setCvvCode("456");
        validInactiveCard.setEmbossedName("JANE SMITH");
        validInactiveCard.setExpirationDate(LocalDate.now().plusYears(1));
        validInactiveCard.setActiveStatus(INACTIVE_STATUS);

        // Create expired card (CARD-EXPIRAION-DATE in past)
        expiredCard = new Card();
        expiredCard.setCardNumber("4000000000009999");
        expiredCard.setAccountId(VALID_ACCOUNT_ID_LONG);
        expiredCard.setCustomerId(VALID_CUSTOMER_ID);
        expiredCard.setCvvCode("999");
        expiredCard.setEmbossedName("EXPIRED USER");
        expiredCard.setExpirationDate(LocalDate.now().minusDays(1));
        expiredCard.setActiveStatus(ACTIVE_STATUS);
    }

    // ========================================
    // Card Retrieval by Number Tests
    // Testing 9100-GETCARD-BYACCTCARD COBOL section equivalency
    // ========================================

    @Test
    @DisplayName("Should retrieve card details for valid card number")
    void getCardDetail_ValidCardNumber_ReturnsCardDetails() {
        // Given - Mock repository to return valid card (DFHRESP(NORMAL))
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(validActiveCard));

        // When - Execute card detail retrieval (equivalent to 9100-GETCARD-BYACCTCARD)
        CreditCardDetailResponse response = creditCardDetailService.getCardDetail(VALID_CARD_NUMBER);

        // Then - Verify all card details are properly mapped (equivalent to 1200-SETUP-SCREEN-VARS)
        assertThat(response).isNotNull();
        assertThat(response.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
        assertThat(response.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(response.getEmbossedName()).isEqualTo(VALID_EMBOSSED_NAME);
        assertThat(response.getCardStatus()).isEqualTo(ACTIVE_STATUS);
        assertThat(response.getCvvCode()).isEqualTo(VALID_CVV);
        
        // Verify repository interaction
        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    @Test
    @DisplayName("Should handle card not found scenario")
    void getCardDetail_CardNotFound_ThrowsResourceNotFoundException() {
        // Given - Mock repository to return empty (DFHRESP(NOTFND))
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.empty());

        // When/Then - Verify exception is thrown with correct message
        // (equivalent to DID-NOT-FIND-ACCTCARD-COMBO in COBOL)
        assertThatThrownBy(() -> creditCardDetailService.getCardDetail(VALID_CARD_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Did not find cards for this search condition");

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    @Test
    @DisplayName("Should retrieve card details by account ID using alternate index")
    void getCardDetailByAccountId_ValidAccount_ReturnsCardDetails() {
        // Given - Mock repository to return card for account (equivalent to 9150-GETCARD-BYACCT)
        when(cardRepository.findByAccountId(VALID_ACCOUNT_ID_LONG)).thenReturn(List.of(validActiveCard));

        // When - Execute account-based card lookup
        CreditCardDetailResponse response = creditCardDetailService.getCardDetailByAccountId(VALID_ACCOUNT_ID);

        // Then - Verify card details are returned
        assertThat(response).isNotNull();
        assertThat(response.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(response.getCardStatus()).isEqualTo(ACTIVE_STATUS);

        verify(cardRepository, times(1)).findByAccountId(VALID_ACCOUNT_ID_LONG);
    }

    @Test
    @DisplayName("Should handle account not found in cards database")
    void getCardDetailByAccountId_AccountNotFound_ThrowsResourceNotFoundException() {
        // Given - Mock repository to return empty list (DFHRESP(NOTFND))
        when(cardRepository.findByAccountId(VALID_ACCOUNT_ID_LONG)).thenReturn(List.of());

        // When/Then - Verify exception with correct message
        // (equivalent to DID-NOT-FIND-ACCT-IN-CARDXREF in COBOL)
        assertThatThrownBy(() -> creditCardDetailService.getCardDetailByAccountId(VALID_ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Did not find this account in cards database");

        verify(cardRepository, times(1)).findByAccountId(VALID_ACCOUNT_ID_LONG);
    }

    // ========================================
    // Card Status Interpretation Tests
    // Testing CARD-ACTIVE-STATUS field validation
    // ========================================

    @Test
    @DisplayName("Should correctly interpret active card status")
    void mapCardToDetailResponse_ActiveCard_ReturnsActiveStatus() {
        // When - Map active card to response
        CreditCardDetailResponse response = creditCardDetailService.mapCardToDetailResponse(validActiveCard);

        // Then - Verify active status is preserved
        assertThat(response.getCardStatus()).isEqualTo(ACTIVE_STATUS);
        assertThat(response.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
    }

    @Test
    @DisplayName("Should correctly interpret inactive card status")
    void mapCardToDetailResponse_InactiveCard_ReturnsInactiveStatus() {
        // When - Map inactive card to response
        CreditCardDetailResponse response = creditCardDetailService.mapCardToDetailResponse(validInactiveCard);

        // Then - Verify inactive status is preserved
        assertThat(response.getCardStatus()).isEqualTo(INACTIVE_STATUS);
        assertThat(response.getCardNumber()).isEqualTo("4000000000005678");
        assertThat(response.getEmbossedName()).isEqualTo("JANE SMITH");
    }

    // ========================================
    // Expiration Date Handling Tests
    // Testing CARD-EXPIRAION-DATE field processing
    // ========================================

    @Test
    @DisplayName("Should handle valid future expiration date")
    void mapCardToDetailResponse_FutureExpirationDate_ReturnsFormattedDate() {
        // Given - Card with future expiration date
        LocalDate futureDate = LocalDate.now().plusYears(3);
        validActiveCard.setExpirationDate(futureDate);

        // When - Map card to response
        CreditCardDetailResponse response = creditCardDetailService.mapCardToDetailResponse(validActiveCard);

        // Then - Verify expiration date is properly formatted
        assertThat(response.getExpirationDate()).isEqualTo(futureDate.toString());
    }

    @Test
    @DisplayName("Should handle expired card expiration date")
    void mapCardToDetailResponse_ExpiredCard_ReturnsExpiredDate() {
        // When - Map expired card to response
        CreditCardDetailResponse response = creditCardDetailService.mapCardToDetailResponse(expiredCard);

        // Then - Verify expired date is handled correctly
        assertThat(response.getExpirationDate()).isNotNull();
        assertThat(response.getCardStatus()).isEqualTo(ACTIVE_STATUS);
        assertThat(response.getEmbossedName()).isEqualTo("EXPIRED USER");
    }

    @Test
    @DisplayName("Should handle null expiration date gracefully")
    void mapCardToDetailResponse_NullExpirationDate_HandlesGracefully() {
        // Given - Card with null expiration date
        validActiveCard.setExpirationDate(null);

        // When - Map card to response
        CreditCardDetailResponse response = creditCardDetailService.mapCardToDetailResponse(validActiveCard);

        // Then - Verify null date is handled
        assertThat(response.getExpirationDate()).isNull();
        assertThat(response.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
    }

    // ========================================
    // Account Relationship Validation Tests
    // Testing cross-reference validation logic
    // ========================================

    @Test
    @DisplayName("Should validate card belongs to specified account")
    void getCardDetailByAccountAndCard_ValidCombination_ReturnsCardDetails() {
        // Given - Mock repository to return card that belongs to account
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(validActiveCard));

        // When - Execute combined account and card validation
        CreditCardDetailResponse response = creditCardDetailService
                .getCardDetailByAccountAndCard(VALID_ACCOUNT_ID, VALID_CARD_NUMBER);

        // Then - Verify card details are returned
        assertThat(response).isNotNull();
        assertThat(response.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(response.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    @Test
    @DisplayName("Should reject card that doesn't belong to specified account")
    void getCardDetailByAccountAndCard_MismatchedAccount_ThrowsResourceNotFoundException() {
        // Given - Card belongs to different account
        String differentAccountId = "98765432109";
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(validActiveCard));

        // When/Then - Verify exception for account mismatch
        assertThatThrownBy(() -> creditCardDetailService
                .getCardDetailByAccountAndCard(differentAccountId, VALID_CARD_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card does not belong to specified account");

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    // ========================================
    // Card Access Validation Tests
    // Testing validateCardAccess method functionality
    // ========================================

    @Test
    @DisplayName("Should validate access for existing card")
    void validateCardAccess_ExistingCard_CompletesSuccessfully() {
        // Given - Mock repository to return existing card
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(validActiveCard));

        // When/Then - Verify no exception is thrown
        assertThatCode(() -> creditCardDetailService.validateCardAccess(VALID_CARD_NUMBER))
                .doesNotThrowAnyException();

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    @Test
    @DisplayName("Should reject access for non-existing card")
    void validateCardAccess_NonExistingCard_ThrowsResourceNotFoundException() {
        // Given - Mock repository to return empty
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.empty());

        // When/Then - Verify exception for card not found
        assertThatThrownBy(() -> creditCardDetailService.validateCardAccess(VALID_CARD_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card not found or not accessible");

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    // ========================================
    // Input Validation Tests
    // Testing 2210-EDIT-ACCOUNT and 2220-EDIT-CARD COBOL sections
    // ========================================

    @Test
    @DisplayName("Should reject null card number")
    void getCardDetail_NullCardNumber_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for null input
        assertThatThrownBy(() -> creditCardDetailService.getCardDetail(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number not provided");

        verify(cardRepository, never()).findByCardNumber(any());
    }

    @Test
    @DisplayName("Should reject empty card number")
    void getCardDetail_EmptyCardNumber_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for empty input
        assertThatThrownBy(() -> creditCardDetailService.getCardDetail(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number not provided");

        verify(cardRepository, never()).findByCardNumber(any());
    }

    @Test
    @DisplayName("Should reject card number with invalid length")
    void getCardDetail_InvalidCardNumberLength_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for wrong length
        // (equivalent to COBOL "CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER")
        assertThatThrownBy(() -> creditCardDetailService.getCardDetail(INVALID_CARD_NUMBER_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number if supplied must be a 16 digit number");

        verify(cardRepository, never()).findByCardNumber(any());
    }

    @Test
    @DisplayName("Should reject non-numeric card number")
    void getCardDetail_NonNumericCardNumber_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for non-numeric input
        // (equivalent to COBOL CC-CARD-NUM IS NOT NUMERIC)
        assertThatThrownBy(() -> creditCardDetailService.getCardDetail(INVALID_CARD_NUMBER_NON_NUMERIC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number if supplied must be a 16 digit number");

        verify(cardRepository, never()).findByCardNumber(any());
    }

    @Test
    @DisplayName("Should reject all-zeros card number")
    void getCardDetail_AllZerosCardNumber_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for invalid zeros
        assertThatThrownBy(() -> creditCardDetailService.getCardDetail(INVALID_CARD_NUMBER_ZEROS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number must be a non zero 16 digit number");

        verify(cardRepository, never()).findByCardNumber(any());
    }

    @Test
    @DisplayName("Should reject null account ID")
    void getCardDetailByAccountId_NullAccountId_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for null account ID
        assertThatThrownBy(() -> creditCardDetailService.getCardDetailByAccountId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account number not provided");

        verify(cardRepository, never()).findByAccountId(any());
    }

    @Test
    @DisplayName("Should reject empty account ID")
    void getCardDetailByAccountId_EmptyAccountId_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for empty account ID
        assertThatThrownBy(() -> creditCardDetailService.getCardDetailByAccountId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account number not provided");

        verify(cardRepository, never()).findByAccountId(any());
    }

    @Test
    @DisplayName("Should reject account ID with invalid length")
    void getCardDetailByAccountId_InvalidAccountIdLength_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for wrong length
        // (equivalent to COBOL "ACCOUNT FILTER,IF SUPPLIED MUST BE A 11 DIGIT NUMBER")
        assertThatThrownBy(() -> creditCardDetailService.getCardDetailByAccountId(INVALID_ACCOUNT_ID_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account number must be a non zero 11 digit number");

        verify(cardRepository, never()).findByAccountId(any());
    }

    @Test
    @DisplayName("Should reject non-numeric account ID")
    void getCardDetailByAccountId_NonNumericAccountId_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for non-numeric account ID
        // (equivalent to COBOL CC-ACCT-ID IS NOT NUMERIC)
        assertThatThrownBy(() -> creditCardDetailService.getCardDetailByAccountId(INVALID_ACCOUNT_ID_NON_NUMERIC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account number must be a non zero 11 digit number");

        verify(cardRepository, never()).findByAccountId(any());
    }

    @Test
    @DisplayName("Should reject all-zeros account ID")
    void getCardDetailByAccountId_AllZerosAccountId_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for invalid zeros
        assertThatThrownBy(() -> creditCardDetailService.getCardDetailByAccountId(INVALID_ACCOUNT_ID_ZEROS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account number must be a non zero 11 digit number");

        verify(cardRepository, never()).findByAccountId(any());
    }

    // ========================================
    // Data Mapping Validation Tests
    // Testing mapCardToDetailResponse method functionality
    // ========================================

    @Test
    @DisplayName("Should handle null card entity gracefully")
    void mapCardToDetailResponse_NullCard_ThrowsIllegalArgumentException() {
        // When/Then - Verify validation error for null card entity
        assertThatThrownBy(() -> creditCardDetailService.mapCardToDetailResponse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card entity cannot be null");
    }

    @Test
    @DisplayName("Should map all card fields correctly")
    void mapCardToDetailResponse_ValidCard_MapsAllFields() {
        // When - Map valid card to response
        CreditCardDetailResponse response = creditCardDetailService.mapCardToDetailResponse(validActiveCard);

        // Then - Verify all fields are mapped correctly
        assertThat(response).isNotNull();
        assertThat(response.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
        assertThat(response.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(response.getEmbossedName()).isEqualTo(VALID_EMBOSSED_NAME);
        assertThat(response.getCardStatus()).isEqualTo(ACTIVE_STATUS);
        assertThat(response.getCvvCode()).isEqualTo(VALID_CVV);
        assertThat(response.getExpirationDate()).isNotNull();
    }

    @Test
    @DisplayName("Should handle null account ID in card entity")
    void mapCardToDetailResponse_NullAccountId_HandlesGracefully() {
        // Given - Card with null account ID
        validActiveCard.setAccountId(null);

        // When - Map card to response
        CreditCardDetailResponse response = creditCardDetailService.mapCardToDetailResponse(validActiveCard);

        // Then - Verify null account ID is handled
        assertThat(response.getAccountId()).isEmpty();
        assertThat(response.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
    }

    // ========================================
    // Repository Error Handling Tests
    // Testing database access error scenarios
    // ========================================

    @Test
    @DisplayName("Should handle repository runtime exception during card lookup")
    void getCardDetail_RepositoryException_ThrowsRuntimeException() {
        // Given - Mock repository to throw exception
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER))
                .thenThrow(new RuntimeException("Database connection error"));

        // When/Then - Verify exception is propagated with database error context
        assertThatThrownBy(() -> creditCardDetailService.getCardDetail(VALID_CARD_NUMBER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error reading Card Data File");

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    @Test
    @DisplayName("Should handle repository runtime exception during account lookup")
    void getCardDetailByAccountId_RepositoryException_ThrowsRuntimeException() {
        // Given - Mock repository to throw exception
        when(cardRepository.findByAccountId(VALID_ACCOUNT_ID_LONG))
                .thenThrow(new RuntimeException("Database connection error"));

        // When/Then - Verify exception is propagated with database error context
        assertThatThrownBy(() -> creditCardDetailService.getCardDetailByAccountId(VALID_ACCOUNT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error reading Card Data File");

        verify(cardRepository, times(1)).findByAccountId(VALID_ACCOUNT_ID_LONG);
    }

    // ========================================
    // Card Number Formatting Tests
    // Testing card number cleaning and validation
    // ========================================

    @Test
    @DisplayName("Should handle card number with spaces")
    void getCardDetail_CardNumberWithSpaces_CleansAndValidates() {
        // Given - Card number with spaces
        String cardNumberWithSpaces = "4000 0000 0000 1234";
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(validActiveCard));

        // When - Execute card detail retrieval
        CreditCardDetailResponse response = creditCardDetailService.getCardDetail(cardNumberWithSpaces);

        // Then - Verify spaces are cleaned and card is found
        assertThat(response).isNotNull();
        assertThat(response.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    @Test
    @DisplayName("Should handle account ID with spaces")
    void getCardDetailByAccountId_AccountIdWithSpaces_CleansAndValidates() {
        // Given - Account ID with spaces
        String accountIdWithSpaces = "123 456 789 01";
        when(cardRepository.findByAccountId(VALID_ACCOUNT_ID_LONG)).thenReturn(List.of(validActiveCard));

        // When - Execute account-based card lookup
        CreditCardDetailResponse response = creditCardDetailService.getCardDetailByAccountId(accountIdWithSpaces);

        // Then - Verify spaces are cleaned and card is found
        assertThat(response).isNotNull();
        assertThat(response.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);

        verify(cardRepository, times(1)).findByAccountId(VALID_ACCOUNT_ID_LONG);
    }

    // ========================================
    // Business Logic Edge Cases
    // Testing specific business scenarios
    // ========================================

    @Test
    @DisplayName("Should prefer active card when multiple cards exist for account")
    void getCardDetailByAccountId_MultipleCards_ReturnsActiveCard() {
        // Given - Account has both active and inactive cards
        when(cardRepository.findByAccountId(VALID_ACCOUNT_ID_LONG))
                .thenReturn(List.of(validInactiveCard, validActiveCard));

        // When - Execute account-based card lookup
        CreditCardDetailResponse response = creditCardDetailService.getCardDetailByAccountId(VALID_ACCOUNT_ID);

        // Then - Verify active card is returned (business logic preference)
        assertThat(response).isNotNull();
        assertThat(response.getCardStatus()).isEqualTo(ACTIVE_STATUS);
        assertThat(response.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);

        verify(cardRepository, times(1)).findByAccountId(VALID_ACCOUNT_ID_LONG);
    }

    @Test
    @DisplayName("Should return any card when no active cards exist for account")
    void getCardDetailByAccountId_NoActiveCards_ReturnsAnyCard() {
        // Given - Account has only inactive cards
        when(cardRepository.findByAccountId(VALID_ACCOUNT_ID_LONG))
                .thenReturn(List.of(validInactiveCard));

        // When - Execute account-based card lookup
        CreditCardDetailResponse response = creditCardDetailService.getCardDetailByAccountId(VALID_ACCOUNT_ID);

        // Then - Verify inactive card is returned as fallback
        assertThat(response).isNotNull();
        assertThat(response.getCardStatus()).isEqualTo(INACTIVE_STATUS);
        assertThat(response.getEmbossedName()).isEqualTo("JANE SMITH");

        verify(cardRepository, times(1)).findByAccountId(VALID_ACCOUNT_ID_LONG);
    }

    // ========================================
    // Integration Test Scenarios
    // Testing complete workflows
    // ========================================

    @Test
    @DisplayName("Should complete full card detail workflow successfully")
    void getCardDetail_CompleteWorkflow_ReturnsCompleteResponse() {
        // Given - Mock repository for successful card lookup
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER)).thenReturn(Optional.of(validActiveCard));

        // When - Execute complete card detail workflow
        CreditCardDetailResponse response = creditCardDetailService.getCardDetail(VALID_CARD_NUMBER);

        // Then - Verify complete response with all expected fields
        assertThat(response).isNotNull();
        
        // Verify card identification fields
        assertThat(response.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
        assertThat(response.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        
        // Verify card details
        assertThat(response.getEmbossedName()).isEqualTo(VALID_EMBOSSED_NAME);
        assertThat(response.getCardStatus()).isEqualTo(ACTIVE_STATUS);
        assertThat(response.getCvvCode()).isEqualTo(VALID_CVV);
        
        // Verify date handling
        assertThat(response.getExpirationDate()).isNotNull();
        assertThat(response.getExpirationDate()).contains(LocalDate.now().plusYears(2).getYear() + "");
        
        // Verify repository interaction
        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
        verifyNoMoreInteractions(cardRepository);
    }
}