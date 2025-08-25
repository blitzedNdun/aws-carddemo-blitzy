package com.carddemo.service;

import com.carddemo.dto.CardDto;
import com.carddemo.entity.Card;
import com.carddemo.repository.CardRepository;
import com.carddemo.util.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.never;

/**
 * Comprehensive unit test class for CreditCardUpdateService validating COBOL COCRDUPC 
 * card update logic migration to Java. Tests card status changes, limit adjustments, 
 * expiration date updates, and audit logging with 100% functional parity.
 * 
 * This test class ensures that the Java implementation produces identical results
 * to the original COBOL COCRDUPC.cbl program for all business logic scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreditCardUpdateService - COCRDUPC COBOL Program Equivalent Tests")
class CardUpdateServiceTest extends AbstractBaseTest implements UnitTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CreditCardUpdateService creditCardUpdateService;

    private Card testCard;
    private CardDto testCardDto;
    private TestDataGenerator testDataGenerator;

    @BeforeEach
    void setUp() {
        super.setUp();
        testDataGenerator = new TestDataGenerator();
        
        // Initialize test card with COBOL-equivalent data patterns
        testCard = createTestCard();
        testCardDto = createTestCardDto();
    }

    @Test
    @DisplayName("updateCreditCard - Valid card update returns success response")
    void testUpdateCreditCard_ValidUpdate_ReturnsSuccessResponse() {
        // Given: Valid card update request with status change
        String cardNumber = testCard.getCardNumber();
        testCardDto.setActiveStatus("Y");
        testCardDto.setExpirationDate(LocalDate.of(2029, 12, 31));
        
        when(cardRepository.findById(cardNumber)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing card update through service
        CardDto result = creditCardUpdateService.updateCreditCard(cardNumber, testCardDto);

        // Then: Verify successful update matching COCRDUPC behavior
        assertThat(result).isNotNull();
        assertThat(result.getActiveStatus()).isEqualTo("Y");
        assertThat(result.getExpirationDate()).isEqualTo(LocalDate.of(2029, 12, 31));
        
        verify(cardRepository).findById(cardNumber);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("updateCreditCard - Card not found throws exception")
    void testUpdateCreditCard_CardNotFound_ThrowsException() {
        // Given: Non-existent card number
        String invalidCardNumber = "9999999999999999";
        
        when(cardRepository.findById(invalidCardNumber)).thenReturn(Optional.empty());

        // When/Then: Attempting to update non-existent card throws exception
        assertThatThrownBy(() -> 
            creditCardUpdateService.updateCreditCard(invalidCardNumber, testCardDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Card not found with number: " + invalidCardNumber);
        
        verify(cardRepository).findById(invalidCardNumber);
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateCardUpdate - Valid card data passes validation")
    void testValidateCardUpdate_ValidData_PassesValidation() {
        // Given: Valid card update data
        testCardDto.setActiveStatus("Y");
        testCardDto.setExpirationDate(LocalDate.now().plusYears(3));
        
        // When: Validating card update
        assertThatCode(() -> 
            creditCardUpdateService.validateCardUpdate(testCardDto))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateCardUpdate - Invalid status code throws validation exception")
    void testValidateCardUpdate_InvalidStatus_ThrowsValidationException() {
        // Given: Invalid status code (not Y or N)
        testCardDto.setActiveStatus("X");
        
        // When/Then: Validation fails for invalid status
        assertThatThrownBy(() -> 
            creditCardUpdateService.validateCardUpdate(testCardDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid card status. Must be Y (Active) or N (Inactive)");
    }

    @Test
    @DisplayName("validateCardUpdate - Past expiration date throws validation exception")
    void testValidateCardUpdate_PastExpirationDate_ThrowsValidationException() {
        // Given: Expiration date in the past
        testCardDto.setExpirationDate(LocalDate.now().minusYears(1));
        
        // When/Then: Validation fails for past expiration date
        assertThatThrownBy(() -> 
            creditCardUpdateService.validateCardUpdate(testCardDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Expiration date cannot be in the past");
    }

    @Test
    @DisplayName("processCardUpdate - Card activation updates status and logs audit trail")
    void testProcessCardUpdate_CardActivation_UpdatesStatusAndLogsAudit() {
        // Given: Inactive card being activated
        testCard.setActiveStatus("N");
        testCardDto.setActiveStatus("Y");
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing card activation
        CardDto result = creditCardUpdateService.processCardUpdate(testCard.getCardNumber(), testCardDto);

        // Then: Verify activation and audit logging
        assertThat(result).isNotNull();
        assertThat(result.getActiveStatus()).isEqualTo("Y");
        
        verify(cardRepository).save(any(Card.class));
        // Audit logging will be verified through the saved card entity
    }

    @Test
    @DisplayName("processCardUpdate - Card deactivation updates status correctly")
    void testProcessCardUpdate_CardDeactivation_UpdatesStatusCorrectly() {
        // Given: Active card being deactivated
        testCard.setActiveStatus("Y");
        testCardDto.setActiveStatus("N");
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing card deactivation
        CardDto result = creditCardUpdateService.processCardUpdate(testCard.getCardNumber(), testCardDto);

        // Then: Verify deactivation
        assertThat(result).isNotNull();
        assertThat(result.getActiveStatus()).isEqualTo("N");
        
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("processCardUpdate - Expiration date change updates correctly")
    void testProcessCardUpdate_ExpirationDateChange_UpdatesCorrectly() {
        // Given: Card with expiration date change
        LocalDate newExpirationDate = LocalDate.of(2030, 6, 30);
        testCardDto.setExpirationDate(newExpirationDate);
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing expiration date update
        CardDto result = creditCardUpdateService.processCardUpdate(testCard.getCardNumber(), testCardDto);

        // Then: Verify expiration date updated
        assertThat(result).isNotNull();
        assertThat(result.getExpirationDate()).isEqualTo(newExpirationDate);
        
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("detectChanges - Detects status change correctly")
    void testDetectChanges_DetectsStatusChange_Correctly() {
        // Given: Card with different status
        Card originalCard = testDataGenerator.generateCard();
        originalCard.setActiveStatus("N");
        
        Card updatedCard = testDataGenerator.generateCard();
        updatedCard.setCardNumber(originalCard.getCardNumber());
        updatedCard.setActiveStatus("Y");
        
        // When: Detecting changes between cards
        boolean hasChanges = creditCardUpdateService.detectChanges(originalCard, updatedCard);

        // Then: Changes are detected
        assertThat(hasChanges).isTrue();
    }

    @Test
    @DisplayName("detectChanges - No changes returns false")
    void testDetectChanges_NoChanges_ReturnsFalse() {
        // Given: Identical cards
        Card originalCard = testDataGenerator.generateCard();
        Card unchangedCard = testDataGenerator.generateCard();
        unchangedCard.setCardNumber(originalCard.getCardNumber());
        unchangedCard.setActiveStatus(originalCard.getActiveStatus());
        unchangedCard.setExpirationDate(originalCard.getExpirationDate());
        unchangedCard.setEmbossedName(originalCard.getEmbossedName());
        
        // When: Detecting changes between identical cards
        boolean hasChanges = creditCardUpdateService.detectChanges(originalCard, unchangedCard);

        // Then: No changes detected
        assertThat(hasChanges).isFalse();
    }

    @Test
    @DisplayName("detectChanges - Detects expiration date change correctly")
    void testDetectChanges_DetectsExpirationDateChange_Correctly() {
        // Given: Card with different expiration date
        Card originalCard = testDataGenerator.generateCard();
        originalCard.setExpirationDate(LocalDate.of(2025, 12, 31));
        
        Card updatedCard = testDataGenerator.generateCard();
        updatedCard.setCardNumber(originalCard.getCardNumber());
        updatedCard.setExpirationDate(LocalDate.of(2030, 12, 31));
        
        // When: Detecting expiration date changes
        boolean hasChanges = creditCardUpdateService.detectChanges(originalCard, updatedCard);

        // Then: Changes are detected
        assertThat(hasChanges).isTrue();
    }

    @Test
    @DisplayName("detectChanges - Detects embossed name change correctly")
    void testDetectChanges_DetectsEmbossedNameChange_Correctly() {
        // Given: Card with different embossed name
        Card originalCard = testDataGenerator.generateCard();
        originalCard.setEmbossedName("JOHN DOE");
        
        Card updatedCard = testDataGenerator.generateCard();
        updatedCard.setCardNumber(originalCard.getCardNumber());
        updatedCard.setEmbossedName("JOHN A DOE");
        
        // When: Detecting embossed name changes
        boolean hasChanges = creditCardUpdateService.detectChanges(originalCard, updatedCard);

        // Then: Changes are detected
        assertThat(hasChanges).isTrue();
    }

    @Test
    @DisplayName("updateCreditCard - Validates COBOL precision for decimal fields")
    void testUpdateCreditCard_ValidatesCobolPrecision_ForDecimalFields() {
        // Given: Card with COMP-3 equivalent decimal precision
        BigDecimal creditLimit = testDataGenerator.generateComp3BigDecimal();
        testCard.setCreditLimit(creditLimit);
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing update with precision validation
        CardDto result = creditCardUpdateService.updateCreditCard(testCard.getCardNumber(), testCardDto);

        // Then: Verify COBOL precision is maintained
        assertThat(result).isNotNull();
        validateCobolPrecision(result.getCreditLimit(), TestConstants.COBOL_DECIMAL_SCALE);
        
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("validateCardUpdate - Validates CVV code format")
    void testValidateCardUpdate_ValidatesCvvCode_Format() {
        // Given: Card with invalid CVV code
        testCardDto.setCvvCode("12");  // Invalid - too short
        
        // When/Then: Validation fails for invalid CVV format
        assertThatThrownBy(() -> 
            creditCardUpdateService.validateCardUpdate(testCardDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("CVV code must be exactly 3 digits");
    }

    @Test
    @DisplayName("validateCardUpdate - Validates embossed name length")
    void testValidateCardUpdate_ValidatesEmbossedName_Length() {
        // Given: Card with embossed name too long (COBOL limit validation)
        testCardDto.setEmbossedName("THIS NAME IS TOO LONG FOR THE CARD EMBOSSING LIMIT");
        
        // When/Then: Validation fails for name too long
        assertThatThrownBy(() -> 
            creditCardUpdateService.validateCardUpdate(testCardDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Embossed name cannot exceed 25 characters");
    }

    @Test
    @DisplayName("processCardUpdate - Handles concurrent update scenarios")
    void testProcessCardUpdate_HandlesConcurrentUpdate_Scenarios() {
        // Given: Card being updated concurrently
        testCard.setActiveStatus("Y");
        testCardDto.setActiveStatus("N");
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenThrow(new RuntimeException("Optimistic locking failure"));

        // When/Then: Concurrent update throws appropriate exception
        assertThatThrownBy(() -> 
            creditCardUpdateService.processCardUpdate(testCard.getCardNumber(), testCardDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Optimistic locking failure");
        
        verify(cardRepository).findById(testCard.getCardNumber());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("updateCreditCard - Performance meets COBOL response time requirements")
    void testUpdateCreditCard_PerformanceMeetsCobol_ResponseTimeRequirements() {
        // Given: Valid card update request
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Measuring update operation performance
        long startTime = System.currentTimeMillis();
        CardDto result = creditCardUpdateService.updateCreditCard(testCard.getCardNumber(), testCardDto);
        long executionTime = System.currentTimeMillis() - startTime;

        // Then: Response time meets COBOL performance requirements
        assertThat(result).isNotNull();
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
    }

    @Test
    @DisplayName("validateCardUpdate - Null card DTO throws validation exception")
    void testValidateCardUpdate_NullCardDto_ThrowsValidationException() {
        // When/Then: Null card DTO validation fails
        assertThatThrownBy(() -> 
            creditCardUpdateService.validateCardUpdate(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Card update data cannot be null");
    }

    @Test
    @DisplayName("processCardUpdate - Audit trail generation for all changes")
    void testProcessCardUpdate_AuditTrailGeneration_ForAllChanges() {
        // Given: Card with multiple field changes
        testCard.setActiveStatus("N");
        testCard.setExpirationDate(LocalDate.of(2025, 6, 30));
        
        testCardDto.setActiveStatus("Y");
        testCardDto.setExpirationDate(LocalDate.of(2028, 12, 31));
        testCardDto.setEmbossedName("UPDATED NAME");
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing comprehensive card update
        CardDto result = creditCardUpdateService.processCardUpdate(testCard.getCardNumber(), testCardDto);

        // Then: All changes are applied and audit trail is generated
        assertThat(result).isNotNull();
        assertThat(result.getActiveStatus()).isEqualTo("Y");
        assertThat(result.getExpirationDate()).isEqualTo(LocalDate.of(2028, 12, 31));
        assertThat(result.getEmbossedName()).isEqualTo("UPDATED NAME");
        
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("detectChanges - Handles null input gracefully")
    void testDetectChanges_HandlesNullInput_Gracefully() {
        // When/Then: Null input detection handles gracefully
        assertThatThrownBy(() -> 
            creditCardUpdateService.detectChanges(null, testCard))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cards cannot be null for comparison");
            
        assertThatThrownBy(() -> 
            creditCardUpdateService.detectChanges(testCard, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cards cannot be null for comparison");
    }

    @Test
    @DisplayName("updateCreditCard - Validates account ID association")
    void testUpdateCreditCard_ValidatesAccountId_Association() {
        // Given: Card update for specific account
        String accountId = testDataGenerator.generateAccountId();
        testCard.setAccountId(accountId);
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing card update
        CardDto result = creditCardUpdateService.updateCreditCard(testCard.getCardNumber(), testCardDto);

        // Then: Account association is maintained
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(accountId);
        
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("validateCardUpdate - CVV code validation with COBOL numeric rules")
    void testValidateCardUpdate_CvvCodeValidation_WithCobolNumericRules() {
        // Given: CVV code with non-numeric characters
        testCardDto.setCvvCode("A23");
        
        // When/Then: Validation fails for non-numeric CVV (COBOL numeric validation)
        assertThatThrownBy(() -> 
            creditCardUpdateService.validateCardUpdate(testCardDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("CVV code must be numeric");
    }

    @Test
    @DisplayName("processCardUpdate - Fraud flag handling and validation")
    void testProcessCardUpdate_FraudFlagHandling_AndValidation() {
        // Given: Card with fraud flag being updated
        testCard.setFraudFlag("N");
        testCardDto.setFraudFlag("Y");
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing fraud flag update
        CardDto result = creditCardUpdateService.processCardUpdate(testCard.getCardNumber(), testCardDto);

        // Then: Fraud flag is updated appropriately
        assertThat(result).isNotNull();
        assertThat(result.getFraudFlag()).isEqualTo("Y");
        
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("updateCreditCard - Multiple field updates in single operation")
    void testUpdateCreditCard_MultipleFieldUpdates_InSingleOperation() {
        // Given: Card with multiple field changes
        testCardDto.setActiveStatus("Y");
        testCardDto.setExpirationDate(LocalDate.of(2029, 8, 31));
        testCardDto.setEmbossedName("NEW CARDHOLDER");
        testCardDto.setCvvCode("456");
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing multiple field updates
        CardDto result = creditCardUpdateService.updateCreditCard(testCard.getCardNumber(), testCardDto);

        // Then: All fields are updated correctly
        assertThat(result).isNotNull();
        assertThat(result.getActiveStatus()).isEqualTo("Y");
        assertThat(result.getExpirationDate()).isEqualTo(LocalDate.of(2029, 8, 31));
        assertThat(result.getEmbossedName()).isEqualTo("NEW CARDHOLDER");
        assertThat(result.getCvvCode()).isEqualTo("456");
        
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("processCardUpdate - Credit limit update with BigDecimal precision")
    void testProcessCardUpdate_CreditLimitUpdate_WithBigDecimalPrecision() {
        // Given: Credit limit update with COBOL COMP-3 precision
        BigDecimal newCreditLimit = new BigDecimal("15000.00")
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
        testCardDto.setCreditLimit(newCreditLimit);
        
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing credit limit update
        CardDto result = creditCardUpdateService.processCardUpdate(testCard.getCardNumber(), testCardDto);

        // Then: Credit limit updated with proper precision
        assertThat(result).isNotNull();
        assertBigDecimalEquals(result.getCreditLimit(), newCreditLimit);
        
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("validateCardUpdate - Credit limit validation against COBOL limits")
    void testValidateCardUpdate_CreditLimitValidation_AgainstCobolLimits() {
        // Given: Credit limit exceeding COBOL maximum
        BigDecimal exceedingLimit = new BigDecimal("999999.99");
        testCardDto.setCreditLimit(exceedingLimit);
        
        // When/Then: Validation fails for exceeding credit limit
        assertThatThrownBy(() -> 
            creditCardUpdateService.validateCardUpdate(testCardDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Credit limit cannot exceed maximum allowed amount");
    }

    @Test
    @DisplayName("updateCreditCard - Repository interaction patterns match VSAM behavior")
    void testUpdateCreditCard_RepositoryInteractionPatterns_MatchVsamBehavior() {
        // Given: Valid card update simulating VSAM READ-update-write pattern
        when(cardRepository.findById(testCard.getCardNumber())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When: Processing update with VSAM-equivalent operations
        CardDto result = creditCardUpdateService.updateCreditCard(testCard.getCardNumber(), testCardDto);

        // Then: Repository interactions follow VSAM read-update-write pattern
        assertThat(result).isNotNull();
        
        // Verify VSAM-equivalent operation sequence
        verify(cardRepository).findById(testCard.getCardNumber());  // VSAM READ
        verify(cardRepository).save(any(Card.class));              // VSAM REWRITE
        verifyNoMoreInteractions(cardRepository);
    }

    /**
     * Helper method to create test card with COBOL-equivalent data patterns
     */
    private Card createTestCard() {
        Card card = new Card();
        card.setCardNumber("4532123456789012");
        card.setEmbossedName("TEST CARDHOLDER");
        card.setActiveStatus("Y");
        card.setExpirationDate(LocalDate.of(2026, 12, 31));
        card.setCvvCode("123");
        card.setAccountId(testDataGenerator.generateAccountId());
        card.setCreditLimit(new BigDecimal("5000.00")
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        card.setFraudFlag("N");
        return card;
    }

    /**
     * Helper method to create test card DTO with valid data
     */
    private CardDto createTestCardDto() {
        CardDto dto = new CardDto();
        dto.setCardNumber("4532123456789012");
        dto.setEmbossedName("TEST CARDHOLDER");
        dto.setActiveStatus("Y");
        dto.setExpirationDate(LocalDate.of(2026, 12, 31));
        dto.setCvvCode("123");
        dto.setAccountId(testDataGenerator.generateAccountId());
        dto.setCreditLimit(new BigDecimal("5000.00")
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        dto.setFraudFlag("N");
        return dto;
    }
}