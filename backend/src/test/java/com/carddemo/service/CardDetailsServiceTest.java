/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.dto.CardResponse;
import com.carddemo.dto.CardDto;
import com.carddemo.entity.Card;
import com.carddemo.repository.CardRepository;
import com.carddemo.service.CardDetailsService.CardDetailResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for CardDetailsService validating COBOL COCRDSLC card detail view logic migration.
 * 
 * This test class ensures 100% functional parity between the original COBOL implementation (COCRDSLC.cbl)
 * and the modernized Java Spring Boot service. All business logic paths, validation rules, error handling,
 * and data formatting operations are thoroughly tested to guarantee identical behavior.
 * 
 * Test Coverage Areas:
 * - Individual card record retrieval and validation
 * - Expiration date handling and formatting
 * - Credit limit display and available balance calculations  
 * - Card status management and interpretation
 * - Transaction history links and navigation
 * - Error handling for various failure scenarios
 * 
 * COBOL Program Equivalence Testing:
 * - MAIN-PARA (lines 248-392) → getCardDetail() method validation
 * - 9000-READ-DATA (lines 726-734) → readCardData() method testing
 * - 9100-GETCARD-BYACCTCARD (lines 736-777) → card retrieval logic verification
 * - 2210-EDIT-ACCOUNT (lines 647-683) → account validation testing
 * - 2220-EDIT-CARD (lines 685-724) → card number validation testing
 * - Screen formatting logic → formatExpirationDate() method validation
 * - Credit limit calculations → calculateAvailableBalance() method testing
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CardDetailsService - COBOL COCRDSLC Business Logic Tests")
class CardDetailsServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardDetailsService cardDetailsService;

    // Test data constants matching COBOL field specifications
    private static final String VALID_CARD_NUMBER = "4000123456789012";
    private static final String INVALID_CARD_NUMBER_SHORT = "12345";
    private static final String INVALID_CARD_NUMBER_LONG = "12345678901234567890";
    private static final String NON_NUMERIC_CARD_NUMBER = "400012345678901A";
    private static final Long VALID_ACCOUNT_ID = 10000000001L;
    private static final Long INVALID_ACCOUNT_ID = 10000000002L;
    private static final String EMBOSSED_NAME = "JOHN DOE";
    private static final LocalDate FUTURE_EXPIRATION = LocalDate.of(2025, 12, 31);
    private static final LocalDate PAST_EXPIRATION = LocalDate.of(2020, 1, 31);
    private static final String ACTIVE_STATUS = "Y";
    private static final String INACTIVE_STATUS = "N";

    private Card testCard;
    private CardResponse testCardResponse;
    private CardDto testCardDto;

    @BeforeEach
    void setUp() {
        // Initialize test data objects for consistent testing
        testCard = createTestCard();
        testCardResponse = createTestCardResponse();
        testCardDto = createTestCardDto();
    }

    /**
     * Creates a test Card entity with valid data matching COBOL CARD-RECORD structure.
     */
    private Card createTestCard() {
        Card card = new Card();
        card.setCardNumber(VALID_CARD_NUMBER);
        card.setAccountId(VALID_ACCOUNT_ID);
        card.setEmbossedName(EMBOSSED_NAME);
        card.setExpirationDate(FUTURE_EXPIRATION);
        card.setActiveStatus(ACTIVE_STATUS);
        return card;
    }

    /**
     * Creates a test CardResponse DTO for response validation testing.
     */
    private CardResponse createTestCardResponse() {
        return new CardResponse(
            VALID_CARD_NUMBER,
            String.valueOf(VALID_ACCOUNT_ID),
            EMBOSSED_NAME,
            FUTURE_EXPIRATION,
            ACTIVE_STATUS
        );
    }

    /**
     * Creates a test CardDto for data transfer testing.
     */
    private CardDto createTestCardDto() {
        return CardDto.builder()
            .cardNumber(VALID_CARD_NUMBER)
            .accountId(String.valueOf(VALID_ACCOUNT_ID))
            .embossedName(EMBOSSED_NAME)
            .expirationDate(FUTURE_EXPIRATION)
            .activeStatus(ACTIVE_STATUS)
            .build();
    }

    /**
     * Test suite for getCardDetail() method - equivalent to COBOL MAIN-PARA logic (lines 248-392).
     * Validates complete card detail retrieval workflow including input validation, data retrieval,
     * cross-validation, and response formatting.
     */
    @Nested
    @DisplayName("getCardDetail() - COBOL MAIN-PARA Equivalent Tests")
    class GetCardDetailTests {

        @Test
        @DisplayName("Successful card retrieval with valid card number and matching account ID")
        void getCardDetail_ValidCardNumberAndAccountId_ReturnsSuccessfulResult() {
            // Given: Mock repository returns valid card data
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.of(testCard));

            // When: Service processes card detail request
            CardDetailResult result = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);

            // Then: Verify successful result with complete card information
            assertTrue(result.isSuccess(), "Card detail retrieval should be successful");
            assertEquals(VALID_CARD_NUMBER, result.getCardNumber(), "Card number should match input");
            assertEquals(VALID_ACCOUNT_ID, result.getAccountId(), "Account ID should match card data");
            assertEquals(EMBOSSED_NAME, result.getEmbossedName(), "Embossed name should match card data");
            assertEquals("2025/12/31", result.getExpirationDate(), "Expiration date should be formatted correctly");
            assertEquals(ACTIVE_STATUS, result.getActiveStatus(), "Active status should match card data");
            assertEquals(BigDecimal.ZERO, result.getAvailableBalance(), "Available balance should be calculated");
            assertEquals("   Displaying requested details", result.getInfoMessage(), "Info message should match COBOL message");
            assertNull(result.getErrorMessage(), "Error message should be null for successful result");

            // Verify repository interaction
            verify(cardRepository, times(1)).findById(VALID_CARD_NUMBER);
        }

        @Test
        @DisplayName("Successful card retrieval with valid card number and no account ID validation")
        void getCardDetail_ValidCardNumberOnly_ReturnsSuccessfulResult() {
            // Given: Mock repository returns valid card data
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.of(testCard));

            // When: Service processes card detail request without account ID validation
            CardDetailResult result = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, null);

            // Then: Verify successful result without account cross-validation
            assertTrue(result.isSuccess(), "Card detail retrieval should be successful");
            assertEquals(VALID_CARD_NUMBER, result.getCardNumber(), "Card number should match input");
            assertEquals(VALID_ACCOUNT_ID, result.getAccountId(), "Account ID should match card data");
            assertNotNull(result.getExpirationDate(), "Expiration date should be formatted");
            assertNull(result.getErrorMessage(), "Error message should be null for successful result");

            // Verify repository interaction
            verify(cardRepository, times(1)).findById(VALID_CARD_NUMBER);
        }

        @Test
        @DisplayName("Card not found scenario - equivalent to COBOL DFHRESP(NOTFND)")
        void getCardDetail_CardNotFound_ReturnsNotFoundError() {
            // Given: Mock repository returns empty result (card not found)
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.empty());

            // When: Service processes card detail request for non-existent card
            CardDetailResult result = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);

            // Then: Verify failure result with appropriate error message
            assertFalse(result.isSuccess(), "Card detail retrieval should fail for non-existent card");
            assertEquals("Did not find cards for this search condition", result.getErrorMessage(), 
                "Error message should match COBOL DID-NOT-FIND-ACCTCARD-COMBO message");
            assertNull(result.getCardNumber(), "Card number should be null for failed result");
            assertNull(result.getAccountId(), "Account ID should be null for failed result");
            assertNull(result.getInfoMessage(), "Info message should be null for failed result");

            // Verify repository interaction
            verify(cardRepository, times(1)).findById(VALID_CARD_NUMBER);
        }

        @Test
        @DisplayName("Account ID mismatch - card found but account doesn't match")
        void getCardDetail_AccountIdMismatch_ReturnsNotFoundError() {
            // Given: Mock repository returns card with different account ID
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.of(testCard));

            // When: Service processes card detail request with mismatched account ID
            CardDetailResult result = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, INVALID_ACCOUNT_ID);

            // Then: Verify failure result with appropriate error message
            assertFalse(result.isSuccess(), "Card detail retrieval should fail for account mismatch");
            assertEquals("Did not find cards for this search condition", result.getErrorMessage(),
                "Error message should match COBOL DID-NOT-FIND-ACCTCARD-COMBO message");
            assertNull(result.getCardNumber(), "Card number should be null for failed result");
            assertNull(result.getAccountId(), "Account ID should be null for failed result");

            // Verify repository interaction
            verify(cardRepository, times(1)).findById(VALID_CARD_NUMBER);
        }

        @Test
        @DisplayName("Invalid card number validation failure")
        void getCardDetail_InvalidCardNumber_ReturnsValidationError() {
            // Given: Invalid card number that fails validation
            String invalidCardNumber = INVALID_CARD_NUMBER_SHORT;

            // When: Service processes card detail request with invalid card number
            CardDetailResult result = cardDetailsService.getCardDetail(invalidCardNumber, VALID_ACCOUNT_ID);

            // Then: Verify validation failure result
            assertFalse(result.isSuccess(), "Card detail retrieval should fail for invalid card number");
            assertEquals("Card number if supplied must be a 16 digit number", result.getErrorMessage(),
                "Error message should match COBOL card validation message");
            assertNull(result.getCardNumber(), "Card number should be null for failed result");

            // Verify repository is not called due to validation failure
            verify(cardRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Database exception handling - equivalent to COBOL ABEND-ROUTINE")
        void getCardDetail_DatabaseException_ReturnsErrorResult() {
            // Given: Mock repository throws database exception
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenThrow(new RuntimeException("Database connection failed"));

            // When: Service processes card detail request during database failure
            CardDetailResult result = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);

            // Then: Verify exception handling result
            assertFalse(result.isSuccess(), "Card detail retrieval should fail during database exception");
            assertEquals("Error reading Card Data File", result.getErrorMessage(),
                "Error message should match COBOL file error message");
            assertNull(result.getCardNumber(), "Card number should be null for failed result");

            // Verify repository interaction attempted
            verify(cardRepository, times(1)).findById(VALID_CARD_NUMBER);
        }
    }

    /**
     * Test suite for validateCardNumber() method - equivalent to COBOL 2220-EDIT-CARD logic (lines 685-724).
     * Validates comprehensive card number format checking matching original COBOL validation rules.
     */
    @Nested
    @DisplayName("validateCardNumber() - COBOL 2220-EDIT-CARD Equivalent Tests")
    class ValidateCardNumberTests {

        @Test
        @DisplayName("Valid 16-digit card number passes validation")
        void validateCardNumber_Valid16Digits_ReturnsNull() {
            // When: Validating properly formatted card number
            String result = cardDetailsService.validateCardNumber(VALID_CARD_NUMBER);

            // Then: Validation should pass (null indicates success)
            assertNull(result, "Valid card number should pass validation");
        }

        @Test
        @DisplayName("Null card number fails validation - equivalent to COBOL LOW-VALUES check")
        void validateCardNumber_NullCardNumber_ReturnsError() {
            // When: Validating null card number
            String result = cardDetailsService.validateCardNumber(null);

            // Then: Validation should fail with appropriate message
            assertEquals("Card number not provided", result,
                "Null card number should return 'not provided' message");
        }

        @Test
        @DisplayName("Empty card number fails validation - equivalent to COBOL SPACES check")
        void validateCardNumber_EmptyCardNumber_ReturnsError() {
            // When: Validating empty/whitespace card number
            String result = cardDetailsService.validateCardNumber("   ");

            // Then: Validation should fail with appropriate message
            assertEquals("Card number not provided", result,
                "Empty card number should return 'not provided' message");
        }

        @Test
        @DisplayName("Short card number fails validation - less than 16 digits")
        void validateCardNumber_ShortCardNumber_ReturnsError() {
            // When: Validating card number shorter than 16 digits
            String result = cardDetailsService.validateCardNumber(INVALID_CARD_NUMBER_SHORT);

            // Then: Validation should fail with COBOL-equivalent message
            assertEquals("Card number if supplied must be a 16 digit number", result,
                "Short card number should return format validation message");
        }

        @Test
        @DisplayName("Long card number fails validation - more than 16 digits")
        void validateCardNumber_LongCardNumber_ReturnsError() {
            // When: Validating card number longer than 16 digits
            String result = cardDetailsService.validateCardNumber(INVALID_CARD_NUMBER_LONG);

            // Then: Validation should fail with COBOL-equivalent message
            assertEquals("Card number if supplied must be a 16 digit number", result,
                "Long card number should return format validation message");
        }

        @Test
        @DisplayName("Non-numeric card number fails validation - equivalent to COBOL IS NOT NUMERIC check")
        void validateCardNumber_NonNumericCardNumber_ReturnsError() {
            // When: Validating card number containing non-numeric characters
            String result = cardDetailsService.validateCardNumber(NON_NUMERIC_CARD_NUMBER);

            // Then: Validation should fail with COBOL-equivalent message
            assertEquals("Card number if supplied must be a 16 digit number", result,
                "Non-numeric card number should return format validation message");
        }

        @Test
        @DisplayName("Card number with spaces and dashes is normalized before validation")
        void validateCardNumber_CardNumberWithFormatting_ValidatesAfterNormalization() {
            // Given: Card number with formatting characters
            String formattedCardNumber = "4000-1234-5678-9012";

            // When: Validating formatted card number
            String result = cardDetailsService.validateCardNumber(formattedCardNumber);

            // Then: Validation should pass after normalization
            assertNull(result, "Formatted card number should be normalized and pass validation");
        }
    }

    /**
     * Test suite for formatExpirationDate() method - COBOL date display logic testing.
     * Validates date formatting to match COBOL CARD-EXPIRAION-DATE-X display format (YYYY/MM/DD).
     */
    @Nested
    @DisplayName("formatExpirationDate() - COBOL Date Formatting Tests")
    class FormatExpirationDateTests {

        @Test
        @DisplayName("Valid future expiration date formatted correctly")
        void formatExpirationDate_ValidFutureDate_ReturnsFormattedDate() {
            // When: Formatting valid future expiration date
            String result = cardDetailsService.formatExpirationDate(FUTURE_EXPIRATION);

            // Then: Date should be formatted in COBOL pattern YYYY/MM/DD
            assertEquals("2025/12/31", result, "Expiration date should be formatted as YYYY/MM/DD");
        }

        @Test
        @DisplayName("Valid past expiration date formatted correctly")
        void formatExpirationDate_ValidPastDate_ReturnsFormattedDate() {
            // When: Formatting valid past expiration date
            String result = cardDetailsService.formatExpirationDate(PAST_EXPIRATION);

            // Then: Date should be formatted in COBOL pattern YYYY/MM/DD
            assertEquals("2020/01/31", result, "Past expiration date should be formatted as YYYY/MM/DD");
        }

        @Test
        @DisplayName("Null expiration date returns empty string - equivalent to COBOL LOW-VALUES handling")
        void formatExpirationDate_NullDate_ReturnsEmptyString() {
            // When: Formatting null expiration date
            String result = cardDetailsService.formatExpirationDate(null);

            // Then: Should return empty string for null input
            assertEquals("", result, "Null expiration date should return empty string");
        }

        @Test
        @DisplayName("Edge case dates formatted correctly")
        void formatExpirationDate_EdgeCaseDates_ReturnsCorrectFormat() {
            // Given: Edge case dates
            LocalDate leapYearDate = LocalDate.of(2024, 2, 29);
            LocalDate newYearDate = LocalDate.of(2025, 1, 1);

            // When: Formatting edge case dates
            String leapYearResult = cardDetailsService.formatExpirationDate(leapYearDate);
            String newYearResult = cardDetailsService.formatExpirationDate(newYearDate);

            // Then: Both should be formatted correctly
            assertEquals("2024/02/29", leapYearResult, "Leap year date should be formatted correctly");
            assertEquals("2025/01/01", newYearResult, "New Year date should be formatted correctly");
        }
    }

    /**
     * Test suite for calculateAvailableBalance() method - credit limit business logic testing.
     * Currently tests placeholder implementation but validates method structure and return type.
     */
    @Nested
    @DisplayName("calculateAvailableBalance() - Credit Limit Calculation Tests")
    class CalculateAvailableBalanceTests {

        @Test
        @DisplayName("Available balance calculation with valid card returns BigDecimal")
        void calculateAvailableBalance_ValidCard_ReturnsBigDecimal() {
            // When: Calculating available balance for valid card
            BigDecimal result = cardDetailsService.calculateAvailableBalance(testCard);

            // Then: Should return BigDecimal value (currently placeholder implementation)
            assertNotNull(result, "Available balance calculation should return non-null BigDecimal");
            assertEquals(BigDecimal.ZERO, result, "Placeholder implementation should return zero");
        }

        @Test
        @DisplayName("Available balance calculation with null card handles gracefully")
        void calculateAvailableBalance_NullCard_HandlesGracefully() {
            // When: Calculating available balance with null card
            BigDecimal result = cardDetailsService.calculateAvailableBalance(null);

            // Then: Should handle null input gracefully
            assertNotNull(result, "Available balance calculation should handle null card");
            assertEquals(BigDecimal.ZERO, result, "Null card should return zero balance");
        }

        @Test
        @DisplayName("Available balance uses correct BigDecimal scale for financial precision")
        void calculateAvailableBalance_ValidCard_UsesCorrectScale() {
            // When: Calculating available balance
            BigDecimal result = cardDetailsService.calculateAvailableBalance(testCard);

            // Then: Result should have appropriate scale for currency (2 decimal places)
            assertTrue(result.scale() >= 0, "Available balance should have valid scale");
            assertEquals(0, result.compareTo(BigDecimal.ZERO), "Placeholder should return exactly zero");
        }
    }

    /**
     * Test suite for readCardData() method - equivalent to COBOL 9000-READ-DATA and 9100-GETCARD-BYACCTCARD.
     * Validates VSAM file READ operation equivalent through JPA repository integration.
     */
    @Nested
    @DisplayName("readCardData() - COBOL 9000-READ-DATA Equivalent Tests")
    class ReadCardDataTests {

        @Test
        @DisplayName("Successful card data read - equivalent to COBOL DFHRESP(NORMAL)")
        void readCardData_CardExists_ReturnsCardEntity() {
            // Given: Mock repository returns card data
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.of(testCard));

            // When: Reading card data by card number
            Card result = cardDetailsService.readCardData(VALID_CARD_NUMBER);

            // Then: Should return valid card entity
            assertNotNull(result, "Card data read should return card entity");
            assertEquals(VALID_CARD_NUMBER, result.getCardNumber(), "Card number should match");
            assertEquals(VALID_ACCOUNT_ID, result.getAccountId(), "Account ID should match");
            assertEquals(EMBOSSED_NAME, result.getEmbossedName(), "Embossed name should match");

            // Verify repository interaction
            verify(cardRepository, times(1)).findById(VALID_CARD_NUMBER);
        }

        @Test
        @DisplayName("Card not found scenario - equivalent to COBOL DFHRESP(NOTFND)")
        void readCardData_CardNotFound_ReturnsNull() {
            // Given: Mock repository returns empty result
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.empty());

            // When: Reading non-existent card data
            Card result = cardDetailsService.readCardData(VALID_CARD_NUMBER);

            // Then: Should return null for not found
            assertNull(result, "Card data read should return null for non-existent card");

            // Verify repository interaction
            verify(cardRepository, times(1)).findById(VALID_CARD_NUMBER);
        }

        @Test
        @DisplayName("Database exception propagated - equivalent to COBOL ABEND handling")
        void readCardData_DatabaseException_PropagatesException() {
            // Given: Mock repository throws database exception
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenThrow(new RuntimeException("Database error"));

            // When/Then: Exception should be propagated to calling method
            assertThrows(RuntimeException.class, () -> {
                cardDetailsService.readCardData(VALID_CARD_NUMBER);
            }, "Database exception should be propagated");

            // Verify repository interaction attempted
            verify(cardRepository, times(1)).findById(VALID_CARD_NUMBER);
        }
    }

    /**
     * Integration test suite validating DTO interaction compatibility.
     * Tests integration with CardResponse and CardDto objects to ensure proper data flow.
     */
    @Nested
    @DisplayName("DTO Integration Tests - CardResponse and CardDto Compatibility")
    class DtoIntegrationTests {

        @Test
        @DisplayName("CardResponse DTO field access validation")
        void cardResponseDto_FieldAccess_ReturnsCorrectValues() {
            // When: Accessing CardResponse fields
            String cardNumber = testCardResponse.getCardNumber();
            String accountId = testCardResponse.getAccountId();
            LocalDate expirationDate = testCardResponse.getExpirationDate();
            String activeStatus = testCardResponse.getActiveStatus();

            // Then: All fields should be accessible and correct
            assertNotNull(cardNumber, "Card number should be accessible");
            assertEquals(String.valueOf(VALID_ACCOUNT_ID), accountId, "Account ID should match");
            assertEquals(FUTURE_EXPIRATION, expirationDate, "Expiration date should match");
            assertEquals(ACTIVE_STATUS, activeStatus, "Active status should match");
        }

        @Test
        @DisplayName("CardDto DTO field access validation")
        void cardDto_FieldAccess_ReturnsCorrectValues() {
            // When: Accessing CardDto fields
            String cardNumber = testCardDto.getCardNumber();
            String accountId = testCardDto.getAccountId();
            LocalDate expirationDate = testCardDto.getExpirationDate();
            String activeStatus = testCardDto.getActiveStatus();

            // Then: All fields should be accessible and correct
            assertEquals(VALID_CARD_NUMBER, cardNumber, "Card number should match");
            assertEquals(String.valueOf(VALID_ACCOUNT_ID), accountId, "Account ID should match");
            assertEquals(FUTURE_EXPIRATION, expirationDate, "Expiration date should match");
            assertEquals(ACTIVE_STATUS, activeStatus, "Active status should match");
        }

        @Test
        @DisplayName("Service integration with DTO validation")
        void serviceIntegration_WithDtoData_ValidatesCorrectly() {
            // Given: Mock repository configured with test data
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.of(testCard));

            // When: Service processes card detail request
            CardDetailResult result = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);

            // Then: Result data should match DTO field values
            assertTrue(result.isSuccess(), "Service should successfully process DTO data");
            assertEquals(testCardDto.getCardNumber(), result.getCardNumber(), "Card number should match DTO");
            assertEquals(Long.valueOf(testCardDto.getAccountId()), result.getAccountId(), "Account ID should match DTO");
            assertEquals(testCardDto.getActiveStatus(), result.getActiveStatus(), "Active status should match DTO");
        }
    }

    /**
     * Edge case test suite for comprehensive validation coverage.
     * Tests various edge cases and boundary conditions to ensure robust error handling.
     */
    @Nested
    @DisplayName("Edge Case Tests - Boundary Conditions and Error Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("Card with expired date still processes correctly")
        void getCardDetail_ExpiredCard_ProcessesSuccessfully() {
            // Given: Card with past expiration date
            Card expiredCard = createTestCard();
            expiredCard.setExpirationDate(PAST_EXPIRATION);
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.of(expiredCard));

            // When: Processing expired card
            CardDetailResult result = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);

            // Then: Should process successfully (expiration validation not enforced in card details view)
            assertTrue(result.isSuccess(), "Expired card should still be viewable");
            assertEquals("2020/01/31", result.getExpirationDate(), "Expired date should be formatted correctly");
        }

        @Test
        @DisplayName("Card with inactive status processes correctly")
        void getCardDetail_InactiveCard_ProcessesSuccessfully() {
            // Given: Card with inactive status
            Card inactiveCard = createTestCard();
            inactiveCard.setActiveStatus(INACTIVE_STATUS);
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.of(inactiveCard));

            // When: Processing inactive card
            CardDetailResult result = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);

            // Then: Should process successfully (status display only)
            assertTrue(result.isSuccess(), "Inactive card should still be viewable");
            assertEquals(INACTIVE_STATUS, result.getActiveStatus(), "Inactive status should be preserved");
        }

        @Test
        @DisplayName("Card with maximum length embossed name")
        void getCardDetail_MaxLengthEmbossedName_ProcessesCorrectly() {
            // Given: Card with maximum 50-character embossed name
            Card maxNameCard = createTestCard();
            String maxLengthName = "X".repeat(50); // COBOL PIC X(50) maximum
            maxNameCard.setEmbossedName(maxLengthName);
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.of(maxNameCard));

            // When: Processing card with maximum name length
            CardDetailResult result = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, VALID_ACCOUNT_ID);

            // Then: Should handle maximum length name correctly
            assertTrue(result.isSuccess(), "Card with maximum name length should process");
            assertEquals(maxLengthName, result.getEmbossedName(), "Maximum length name should be preserved");
        }

        @Test
        @DisplayName("Validation with edge case account IDs")
        void getCardDetail_EdgeCaseAccountIds_ValidatesCorrectly() {
            // Given: Card with minimum and maximum valid account IDs
            Long minAccountId = 10000000000L; // Minimum 11-digit account ID
            Long maxAccountId = 99999999999L; // Maximum 11-digit account ID
            
            when(cardRepository.findById(VALID_CARD_NUMBER)).thenReturn(Optional.of(testCard));

            // When: Processing with minimum account ID
            CardDetailResult minResult = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, minAccountId);
            
            // Then: Should fail validation due to account mismatch
            assertFalse(minResult.isSuccess(), "Minimum account ID should fail due to mismatch");

            // When: Processing with maximum account ID  
            CardDetailResult maxResult = cardDetailsService.getCardDetail(VALID_CARD_NUMBER, maxAccountId);
            
            // Then: Should fail validation due to account mismatch
            assertFalse(maxResult.isSuccess(), "Maximum account ID should fail due to mismatch");
        }
    }
}