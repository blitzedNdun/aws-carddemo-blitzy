package com.carddemo.service;

import com.carddemo.dto.TransactionDetailDto;
import com.carddemo.entity.Transaction;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit test class for TransactionDetailService validating COBOL COTRN01C transaction detail logic migration to Java.
 * Tests transaction retrieval, field formatting, and data presentation logic with 100% functional parity
 * with the original COBOL implementation.
 * 
 * This test class ensures that all Spring Boot service methods produce identical outputs to their
 * COBOL paragraph equivalents, maintaining exact precision and business logic behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionDetailService - COBOL COTRN01C Functional Parity Tests")
class TransactionDetailServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionDetailService transactionDetailService;

    private Transaction testTransaction;
    private TransactionDetailDto expectedDto;

    @BeforeEach
    void setUp() {
        // Initialize test data that matches COBOL CVTRA05Y copybook structure
        testTransaction = createTestTransaction();
        expectedDto = createExpectedDto();
    }

    @Test
    @DisplayName("getTransactionDetail - Valid transaction ID returns complete transaction details")
    void getTransactionDetail_ValidTransactionId_ReturnsTransactionDetails() {
        // Given: Valid transaction ID exists in repository
        Long transactionId = 1000000001L;
        when(transactionRepository.findById(transactionId))
            .thenReturn(Optional.of(testTransaction));

        // When: Retrieving transaction details
        TransactionDetailDto result = transactionDetailService.getTransactionDetail(transactionId);

        // Then: Returns properly formatted transaction details matching COBOL output
        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo(testTransaction.getTransactionId());
        assertThat(result.getAmount()).isEqualByComparingTo(testTransaction.getAmount());
        assertThat(result.getDescription()).isEqualTo(testTransaction.getDescription());
        assertThat(result.getMerchantName()).isEqualTo(testTransaction.getMerchantName());
        assertThat(result.getOriginalTimestamp()).isEqualTo(testTransaction.getOriginalTimestamp());
        assertThat(result.getProcessedTimestamp()).isEqualTo(testTransaction.getProcessedTimestamp());
        
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    @DisplayName("getTransactionDetail - Invalid transaction ID throws ResourceNotFoundException")
    void getTransactionDetail_InvalidTransactionId_ThrowsResourceNotFoundException() {
        // Given: Transaction ID does not exist in repository
        Long nonExistentId = 9999999999L;
        when(transactionRepository.findById(nonExistentId))
            .thenReturn(Optional.empty());

        // When/Then: Accessing non-existent transaction throws proper exception
        assertThatThrownBy(() -> transactionDetailService.getTransactionDetail(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Transaction not found with ID: " + nonExistentId)
            .satisfies(exception -> {
                ResourceNotFoundException ex = (ResourceNotFoundException) exception;
                assertThat(ex.getResourceType()).isEqualTo("Transaction");
                assertThat(ex.getResourceId()).isEqualTo(nonExistentId.toString());
            });

        verify(transactionRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("getTransactionDetail - Null transaction ID throws IllegalArgumentException")
    void getTransactionDetail_NullTransactionId_ThrowsIllegalArgumentException() {
        // When/Then: Null transaction ID throws validation exception
        assertThatThrownBy(() -> transactionDetailService.getTransactionDetail(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transaction ID cannot be null");

        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("validateTransactionId - Valid positive ID passes validation")
    void validateTransactionId_ValidPositiveId_PassesValidation() {
        // Given: Valid positive transaction ID
        Long validId = 1000000001L;

        // When/Then: Validation passes without exception
        assertThatCode(() -> transactionDetailService.validateTransactionId(validId))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateTransactionId - Zero ID throws IllegalArgumentException")
    void validateTransactionId_ZeroId_ThrowsIllegalArgumentException() {
        // Given: Zero transaction ID
        Long zeroId = 0L;

        // When/Then: Zero ID throws validation exception
        assertThatThrownBy(() -> transactionDetailService.validateTransactionId(zeroId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transaction ID must be positive");
    }

    @Test
    @DisplayName("validateTransactionId - Negative ID throws IllegalArgumentException")
    void validateTransactionId_NegativeId_ThrowsIllegalArgumentException() {
        // Given: Negative transaction ID
        Long negativeId = -1L;

        // When/Then: Negative ID throws validation exception
        assertThatThrownBy(() -> transactionDetailService.validateTransactionId(negativeId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transaction ID must be positive");
    }

    @Test
    @DisplayName("formatTransactionAmount - Standard amount formatting maintains COBOL COMP-3 precision")
    void formatTransactionAmount_StandardAmount_MaintainsCobolPrecision() {
        // Given: Transaction amount with COMP-3 equivalent precision
        BigDecimal amount = new BigDecimal("123.45").setScale(2, RoundingMode.HALF_UP);

        // When: Formatting transaction amount
        String formattedAmount = transactionDetailService.formatTransactionAmount(amount);

        // Then: Amount formatted to match COBOL PIC S9(7)V99 COMP-3 output
        assertThat(formattedAmount).isEqualTo("$123.45");
    }

    @Test
    @DisplayName("formatTransactionAmount - Large amount formatting handles maximum COBOL values")
    void formatTransactionAmount_LargeAmount_HandlesMaximumCobolValues() {
        // Given: Large amount near COBOL COMP-3 maximum
        BigDecimal largeAmount = new BigDecimal("99999.99").setScale(2, RoundingMode.HALF_UP);

        // When: Formatting large transaction amount
        String formattedAmount = transactionDetailService.formatTransactionAmount(largeAmount);

        // Then: Large amount properly formatted with currency symbol
        assertThat(formattedAmount).isEqualTo("$99,999.99");
    }

    @Test
    @DisplayName("formatTransactionAmount - Zero amount formatting")
    void formatTransactionAmount_ZeroAmount_FormatsCorrectly() {
        // Given: Zero transaction amount
        BigDecimal zeroAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // When: Formatting zero amount
        String formattedAmount = transactionDetailService.formatTransactionAmount(zeroAmount);

        // Then: Zero amount formatted correctly
        assertThat(formattedAmount).isEqualTo("$0.00");
    }

    @Test
    @DisplayName("formatTransactionAmount - Null amount throws IllegalArgumentException")
    void formatTransactionAmount_NullAmount_ThrowsIllegalArgumentException() {
        // When/Then: Null amount throws validation exception
        assertThatThrownBy(() -> transactionDetailService.formatTransactionAmount(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transaction amount cannot be null");
    }

    @Test
    @DisplayName("formatTransactionDate - Standard date formatting matches COBOL timestamp format")
    void formatTransactionDate_StandardDate_MatchesCobolTimestampFormat() {
        // Given: Standard transaction date
        LocalDateTime testDate = LocalDateTime.of(2024, 1, 15, 14, 30, 45);

        // When: Formatting transaction date
        String formattedDate = transactionDetailService.formatTransactionDate(testDate);

        // Then: Date formatted to match COBOL timestamp display format
        assertThat(formattedDate).isEqualTo("01/15/2024 14:30:45");
    }

    @Test
    @DisplayName("formatTransactionDate - Current date formatting")
    void formatTransactionDate_CurrentDate_FormatsCorrectly() {
        // Given: Current date and time
        LocalDateTime currentDate = LocalDateTime.now();

        // When: Formatting current date
        String formattedDate = transactionDetailService.formatTransactionDate(currentDate);

        // Then: Current date formatted correctly
        DateTimeFormatter expectedFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
        String expectedFormatted = currentDate.format(expectedFormat);
        assertThat(formattedDate).isEqualTo(expectedFormatted);
    }

    @Test
    @DisplayName("formatTransactionDate - Null date throws IllegalArgumentException")
    void formatTransactionDate_NullDate_ThrowsIllegalArgumentException() {
        // When/Then: Null date throws validation exception
        assertThatThrownBy(() -> transactionDetailService.formatTransactionDate(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transaction date cannot be null");
    }

    @Test
    @DisplayName("mapTransactionToDto - Complete transaction mapping preserves all COBOL fields")
    void mapTransactionToDto_CompleteTransaction_PreservesAllCobolFields() {
        // Given: Complete transaction with all CVTRA05Y copybook fields populated
        Transaction completeTransaction = createCompleteTestTransaction();

        // When: Mapping transaction to DTO
        TransactionDetailDto dto = transactionDetailService.mapTransactionToDto(completeTransaction);

        // Then: All fields mapped correctly preserving COBOL data structure
        assertThat(dto.getTransactionId()).isEqualTo(completeTransaction.getTransactionId());
        assertThat(dto.getAmount()).isEqualByComparingTo(completeTransaction.getAmount());
        assertThat(dto.getDescription()).isEqualTo(completeTransaction.getDescription());
        assertThat(dto.getMerchantName()).isEqualTo(completeTransaction.getMerchantName());
        assertThat(dto.getMerchantCity()).isEqualTo(completeTransaction.getMerchantCity());
        assertThat(dto.getMerchantZip()).isEqualTo(completeTransaction.getMerchantZip());
        assertThat(dto.getCardNumber()).isEqualTo(completeTransaction.getCardNumber());
        assertThat(dto.getOriginalTimestamp()).isEqualTo(completeTransaction.getOriginalTimestamp());
        assertThat(dto.getProcessedTimestamp()).isEqualTo(completeTransaction.getProcessedTimestamp());
    }

    @Test
    @DisplayName("mapTransactionToDto - Transaction with minimal fields")
    void mapTransactionToDto_MinimalTransaction_HandlesOptionalFields() {
        // Given: Transaction with only required fields (simulating sparse COBOL record)
        Transaction minimalTransaction = createMinimalTestTransaction();

        // When: Mapping minimal transaction to DTO
        TransactionDetailDto dto = transactionDetailService.mapTransactionToDto(minimalTransaction);

        // Then: Required fields mapped, optional fields handled gracefully
        assertThat(dto.getTransactionId()).isEqualTo(minimalTransaction.getTransactionId());
        assertThat(dto.getAmount()).isEqualByComparingTo(minimalTransaction.getAmount());
        assertThat(dto.getDescription()).isEqualTo(minimalTransaction.getDescription());
        // Optional fields should be null or empty as per COBOL logic
        assertThat(dto.getMerchantCity()).isNull();
        assertThat(dto.getMerchantZip()).isNull();
    }

    @Test
    @DisplayName("Repository integration - findById method usage validation")
    void repositoryIntegration_FindById_UsesCorrectRepositoryMethod() {
        // Given: Transaction ID for repository lookup
        Long transactionId = 1000000001L;
        when(transactionRepository.findById(transactionId))
            .thenReturn(Optional.of(testTransaction));

        // When: Service retrieves transaction
        transactionDetailService.getTransactionDetail(transactionId);

        // Then: Correct repository method called with proper parameters
        verify(transactionRepository).findById(transactionId);
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    @DisplayName("Business logic validation - Amount precision preservation")
    void businessLogicValidation_AmountPrecision_PreservesCobolComp3Precision() {
        // Given: Transaction with precise COMP-3 equivalent amount
        BigDecimal preciseAmount = new BigDecimal("1234.56").setScale(2, RoundingMode.HALF_UP);
        testTransaction.setAmount(preciseAmount);
        when(transactionRepository.findById(anyLong()))
            .thenReturn(Optional.of(testTransaction));

        // When: Retrieving transaction details
        TransactionDetailDto result = transactionDetailService.getTransactionDetail(1L);

        // Then: Amount precision preserved exactly as COBOL COMP-3 field
        assertThat(result.getAmount().scale()).isEqualTo(2);
        assertThat(result.getAmount().precision()).isEqualTo(6);
        assertThat(result.getAmount()).isEqualByComparingTo(preciseAmount);
    }

    @Test
    @DisplayName("Error handling validation - Repository exception propagation")
    void errorHandlingValidation_RepositoryException_PropagatesCorrectly() {
        // Given: Repository throws runtime exception
        Long transactionId = 1000000001L;
        RuntimeException repositoryException = new RuntimeException("Database connection failed");
        when(transactionRepository.findById(transactionId))
            .thenThrow(repositoryException);

        // When/Then: Service propagates repository exception
        assertThatThrownBy(() -> transactionDetailService.getTransactionDetail(transactionId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database connection failed");

        verify(transactionRepository).findById(transactionId);
    }

    // Test Data Generation Methods (simulating TestDataGenerator functionality)

    /**
     * Creates a test transaction matching COBOL CVTRA05Y copybook structure
     */
    private Transaction createTestTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(1000000001L);
        transaction.setAmount(generateValidTransactionAmount());
        transaction.setTransactionType("PURCHASE");
        transaction.setDescription("TEST MERCHANT PURCHASE");
        transaction.setMerchantId(generateMerchantId());
        transaction.setMerchantName("TEST MERCHANT");
        transaction.setMerchantCity("NEW YORK");
        transaction.setMerchantZip("10001");
        transaction.setCardNumber("4532123456789012");
        transaction.setOriginalTimestamp(generateRandomTransactionDate());
        transaction.setProcessedTimestamp(generateRandomTransactionDate().plusMinutes(5));
        return transaction;
    }

    /**
     * Creates expected DTO for comparison in tests
     */
    private TransactionDetailDto createExpectedDto() {
        TransactionDetailDto dto = new TransactionDetailDto();
        dto.setTransactionId(1000000001L);
        dto.setAmount(generateValidTransactionAmount());
        dto.setDescription("TEST MERCHANT PURCHASE");
        dto.setMerchantName("TEST MERCHANT");
        dto.setMerchantCity("NEW YORK");
        dto.setMerchantZip("10001");
        dto.setCardNumber("4532123456789012");
        dto.setOriginalTimestamp(generateRandomTransactionDate());
        dto.setProcessedTimestamp(generateRandomTransactionDate().plusMinutes(5));
        return dto;
    }

    /**
     * Creates a complete test transaction with all fields populated
     */
    private Transaction createCompleteTestTransaction() {
        Transaction transaction = createTestTransaction();
        transaction.setMerchantCity("CHICAGO");
        transaction.setMerchantZip("60601");
        return transaction;
    }

    /**
     * Creates a minimal test transaction with only required fields
     */
    private Transaction createMinimalTestTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(2000000001L);
        transaction.setAmount(new BigDecimal("50.00").setScale(2, RoundingMode.HALF_UP));
        transaction.setDescription("MINIMAL TRANSACTION");
        transaction.setMerchantName("MINIMAL MERCHANT");
        return transaction;
    }

    /**
     * Generates a list of test transactions for batch testing
     */
    private List<Transaction> generateTransactionList() {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Transaction transaction = new Transaction();
            transaction.setTransactionId(1000000000L + i);
            transaction.setAmount(generateValidTransactionAmount());
            transaction.setDescription("TEST TRANSACTION " + i);
            transaction.setMerchantName("TEST MERCHANT " + i);
            transactions.add(transaction);
        }
        return transactions;
    }

    /**
     * Generates valid transaction amount matching COBOL COMP-3 precision
     */
    private BigDecimal generateValidTransactionAmount() {
        return new BigDecimal("125.75").setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generates random transaction date for testing
     */
    private LocalDateTime generateRandomTransactionDate() {
        return LocalDateTime.of(2024, 1, 15, 10, 30, 0);
    }

    /**
     * Generates merchant ID for testing
     */
    private String generateMerchantId() {
        return "MERCH001";
    }
}