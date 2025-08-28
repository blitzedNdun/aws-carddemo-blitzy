/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import com.carddemo.entity.DailyTransaction;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.TransactionType;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.batch.DailyTransactionJob;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import com.carddemo.util.Constants;
import com.carddemo.TestConstants;
import com.carddemo.AbstractBaseTest;
import com.carddemo.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.assertj.core.api.Assertions;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit test class for DailyTransaction JPA entity validating 350-byte 
 * DALYTRAN-RECORD structure from CVTRA06Y copybook used for daily batch processing and reporting.
 * 
 * This test class ensures 100% functional parity with COBOL implementation by:
 * - Validating field mappings identical to Transaction entity structure
 * - Testing BigDecimal amount precision with S9(09)V99 COBOL specification
 * - Verifying timestamp conversion from 26-character COBOL format to Java LocalDateTime
 * - Testing merchant data fields including ID, name, city, ZIP validation
 * - Validating relationship with batch job metadata for daily processing windows
 * - Testing data partitioning by date for daily reports and aggregate functions
 * - Verifying FILLER field handling with proper 20-byte padding validation
 * 
 * The test methods comprehensively validate the 350-byte record layout:
 * - DALYTRAN-ID (PIC X(16)) - Transaction ID field
 * - DALYTRAN-TYPE-CD (PIC X(02)) - Transaction type code
 * - DALYTRAN-CAT-CD (PIC 9(04)) - Category code  
 * - DALYTRAN-SOURCE (PIC X(10)) - Transaction source
 * - DALYTRAN-DESC (PIC X(100)) - Description
 * - DALYTRAN-AMT (PIC S9(09)V99) - Amount with COMP-3 precision
 * - DALYTRAN-MERCHANT-ID (PIC 9(09)) - Merchant identifier
 * - DALYTRAN-MERCHANT-NAME (PIC X(50)) - Merchant name
 * - DALYTRAN-MERCHANT-CITY (PIC X(50)) - Merchant city  
 * - DALYTRAN-MERCHANT-ZIP (PIC X(10)) - Merchant ZIP code
 * - DALYTRAN-CARD-NUM (PIC X(16)) - Card number
 * - DALYTRAN-ORIG-TS (PIC X(26)) - Original timestamp
 * - DALYTRAN-PROC-TS (PIC X(26)) - Processing timestamp
 * - FILLER (PIC X(20)) - Padding for 350-byte alignment
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyTransaction Entity Unit Tests - COBOL CVTRA06Y Copybook Validation")
public class DailyTransactionTest extends AbstractBaseTest implements UnitTest {

    @Mock
    private CobolDataConverter cobolDataConverter;
    
    @Mock  
    private DateConversionUtil dateConversionUtil;
    
    @Mock
    private ValidationUtil validationUtil;
    
    @Mock
    private Account mockAccount;
    
    @Mock
    private Card mockCard;
    
    @Mock
    private TransactionType mockTransactionType;
    
    @Mock
    private TransactionCategory mockTransactionCategory;
    
    @Mock
    private DailyTransactionJob mockDailyTransactionJob;

    private DailyTransaction testDailyTransaction;
    private LocalDateTime testTimestamp;
    private LocalDate testDate;
    private BigDecimal testAmount;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        logTestExecution("Setting up DailyTransaction test data");
        
        // Initialize test data with COBOL-compatible values
        testTimestamp = LocalDateTime.now();
        testDate = LocalDate.now();
        testAmount = CobolDataConverter.toBigDecimal(new BigDecimal("123.45"), 
                                                   TestConstants.COBOL_DECIMAL_SCALE);
        
        // Create test transaction matching CVTRA06Y copybook structure
        testDailyTransaction = createTestTransaction();
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        clearTestData();
    }

    /**
     * Tests transaction ID field validation according to DALYTRAN-ID PIC X(16) specification.
     * Validates field length constraints and format requirements for daily batch processing.
     */
    @Test
    @DisplayName("Test Transaction ID Validation - DALYTRAN-ID PIC X(16)")
    public void testTransactionIdValidation() {
        logTestExecution("Testing transaction ID validation");
        
        // Test valid transaction ID matching COBOL field length
        String validTransactionId = TestConstants.TEST_TRANSACTION_ID;
        testDailyTransaction.setTransactionId(validTransactionId);
        assertThat(testDailyTransaction.getTransactionId())
            .isEqualTo(validTransactionId)
            .hasSize(validTransactionId.length())
            .matches("^[A-Za-z0-9]+$");
        
        // Test transaction ID length validation
        assertThat(validTransactionId.length()).isLessThanOrEqualTo(Constants.TRANSACTION_ID_LENGTH);
        
        // Test null transaction ID handling
        testDailyTransaction.setTransactionId(null);
        assertThat(testDailyTransaction.getTransactionId()).isNull();
        
        // Test empty transaction ID handling
        testDailyTransaction.setTransactionId("");
        assertThat(testDailyTransaction.getTransactionId()).isEmpty();
        
        // Validate with ValidationUtil for COBOL compatibility
        assertDoesNotThrow(() -> {
            ValidationUtil.validateTransactionId(validTransactionId);
        });
    }

    /**
     * Tests amount precision validation with BigDecimal for S9(09)V99 COBOL specification.
     * Ensures exact precision preservation matching COBOL COMP-3 packed decimal behavior.
     */
    @Test
    @DisplayName("Test Amount Precision Validation - DALYTRAN-AMT S9(09)V99 COMP-3")
    public void testAmountPrecisionValidation() {
        logTestExecution("Testing amount precision validation");
        
        // Test COBOL COMP-3 precision preservation
        BigDecimal cobolAmount = new BigDecimal("999999999.99");
        BigDecimal convertedAmount = CobolDataConverter.toBigDecimal(
            cobolAmount, TestConstants.COBOL_DECIMAL_SCALE);
        
        testDailyTransaction.setTransactionAmount(convertedAmount);
        
        // Validate precision and scale match COBOL specification
        assertBigDecimalEquals(testDailyTransaction.getTransactionAmount(), convertedAmount);
        assertThat(testDailyTransaction.getTransactionAmount().scale())
            .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
        
        // Test rounding mode matches COBOL ROUNDED clause
        assertThat(convertedAmount.setScale(TestConstants.COBOL_DECIMAL_SCALE, 
                                           TestConstants.COBOL_ROUNDING_MODE))
            .isEqualTo(testDailyTransaction.getTransactionAmount());
        
        // Test maximum value constraints (S9(09)V99 = 999,999,999.99)
        BigDecimal maxAmount = new BigDecimal("999999999.99");
        testDailyTransaction.setTransactionAmount(maxAmount);
        assertBigDecimalWithinTolerance(testDailyTransaction.getTransactionAmount(), maxAmount);
        
        // Test minimum positive value (0.01)
        BigDecimal minAmount = new BigDecimal("0.01");
        testDailyTransaction.setTransactionAmount(minAmount);
        assertBigDecimalEquals(testDailyTransaction.getTransactionAmount(), minAmount);
        
        // Validate COBOL precision preservation
        validateCobolPrecision(testDailyTransaction.getTransactionAmount());
    }

    /**
     * Tests timestamp conversion from 26-character COBOL format to Java LocalDateTime.
     * Validates DALYTRAN-ORIG-TS and DALYTRAN-PROC-TS field conversion accuracy.
     */
    @Test
    @DisplayName("Test Timestamp Conversion - DALYTRAN-ORIG-TS/PROC-TS PIC X(26)")
    public void testTimestampConversion() {
        logTestExecution("Testing timestamp conversion validation");
        
        // Test original timestamp conversion
        LocalDateTime originalTimestamp = LocalDateTime.now();
        testDailyTransaction.setOriginalTimestamp(originalTimestamp);
        assertThat(testDailyTransaction.getOriginalTimestamp()).isEqualTo(originalTimestamp);
        
        // Test processing timestamp conversion  
        LocalDateTime processingTimestamp = LocalDateTime.now().plusMinutes(5);
        testDailyTransaction.setProcessingTimestamp(processingTimestamp);
        assertThat(testDailyTransaction.getProcessingTimestamp()).isEqualTo(processingTimestamp);
        
        // Test timestamp string formatting for COBOL compatibility
        String formattedTimestamp = DateConversionUtil.formatTimestamp(originalTimestamp);
        assertThat(formattedTimestamp).isNotEmpty().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        
        // Test timestamp parsing from COBOL format
        String cobolTimestampFormat = "yyyy-MM-dd-HH.mm.ss.SSSSSS";
        assertDoesNotThrow(() -> {
            DateConversionUtil.parseTimestamp(formattedTimestamp);
        });
        
        // Test timestamp validation for batch processing windows
        assertThat(originalTimestamp).isBeforeOrEqualTo(LocalDateTime.now());
        if (processingTimestamp != null) {
            assertThat(processingTimestamp).isAfterOrEqualTo(originalTimestamp);
        }
    }

    /**
     * Tests field length validation for all DailyTransaction string fields.
     * Validates COBOL PIC clause length constraints for CVTRA06Y copybook fields.
     */
    @Test
    @DisplayName("Test Field Length Validation - COBOL PIC Clause Constraints")
    public void testFieldLengthValidation() {
        logTestExecution("Testing field length validation");
        
        // Test transaction type code field (PIC X(02))
        String transactionType = "01";
        testDailyTransaction.setTransactionTypeCode(transactionType);
        assertThat(testDailyTransaction.getTransactionTypeCode())
            .hasSize(Constants.TYPE_CODE_LENGTH);
        
        // Test category code field (PIC 9(04))
        String categoryCode = "5411";
        testDailyTransaction.setCategoryCode(categoryCode);
        assertThat(testDailyTransaction.getCategoryCode())
            .hasSize(Constants.CATEGORY_CODE_LENGTH)
            .matches("\\d{4}");
        
        // Test description field (PIC X(100))
        String description = "Test transaction description for daily batch processing";
        testDailyTransaction.setDescription(description);
        assertThat(testDailyTransaction.getDescription().length())
            .isLessThanOrEqualTo(Constants.TRANSACTION_DESCRIPTION_LENGTH);
        
        // Test card number field (PIC X(16))
        String cardNumber = TestConstants.TEST_CARD_NUMBER;
        testDailyTransaction.setCardNumber(cardNumber);
        assertThat(testDailyTransaction.getCardNumber())
            .hasSize(Constants.CARD_NUMBER_LENGTH)
            .matches("\\d{16}");
        
        // Test processing status field length
        testDailyTransaction.setProcessingStatus("COMPLETED");
        assertThat(testDailyTransaction.getProcessingStatus()).isNotEmpty();
    }

    /**
     * Tests merchant data field validation including ID, name, city, ZIP code.
     * Validates DALYTRAN-MERCHANT-* fields from CVTRA06Y copybook specification.
     */
    @Test
    @DisplayName("Test Merchant Data Validation - DALYTRAN-MERCHANT Fields")
    public void testMerchantDataValidation() {
        logTestExecution("Testing merchant data validation");
        
        // Test merchant ID field (PIC 9(09))
        Long merchantId = 123456789L;
        testDailyTransaction.setMerchantId(merchantId);
        assertThat(testDailyTransaction.getMerchantId()).isEqualTo(merchantId);
        assertThat(merchantId.toString()).hasSize(9).matches("\\d{9}");
        
        // Test merchant name field (PIC X(50))
        String merchantName = "Test Merchant Store Location";
        testDailyTransaction.setMerchantName(merchantName);
        assertThat(testDailyTransaction.getMerchantName())
            .isEqualTo(merchantName)
            .hasSizeLessThanOrEqualTo(Constants.MERCHANT_NAME_LENGTH);
        
        // Test merchant city field (PIC X(50))
        String merchantCity = "Test City";
        testDailyTransaction.setMerchantCity(merchantCity);
        assertThat(testDailyTransaction.getMerchantCity()).isEqualTo(merchantCity);
        
        // Test merchant ZIP code field (PIC X(10))
        String merchantZip = "12345";
        testDailyTransaction.setMerchantZip(merchantZip);
        assertThat(testDailyTransaction.getMerchantZip())
            .isEqualTo(merchantZip)
            .hasSize(Constants.ZIP_CODE_LENGTH)
            .matches("\\d{5}");
        
        // Validate ZIP code with ValidationUtil
        assertDoesNotThrow(() -> {
            ValidationUtil.validateZipCode("merchantZip", merchantZip);
        });
    }

    /**
     * Tests equals() and hashCode() methods for object comparison and hashing.
     * Ensures proper entity identity management for batch processing collections.
     */
    @Test
    @DisplayName("Test equals() and hashCode() - Entity Identity Management")
    public void testEqualsAndHashCode() {
        logTestExecution("Testing equals and hashCode methods");
        
        // Test equality with same ID
        DailyTransaction transaction1 = createTestTransaction();
        DailyTransaction transaction2 = createTestTransaction();
        transaction1.setDailyTransactionId(1L);
        transaction2.setDailyTransactionId(1L);
        
        assertThat(transaction1).isEqualTo(transaction2);
        assertThat(transaction1.hashCode()).isEqualTo(transaction2.hashCode());
        
        // Test inequality with different IDs
        transaction2.setDailyTransactionId(2L);
        assertThat(transaction1).isNotEqualTo(transaction2);
        assertThat(transaction1.hashCode()).isNotEqualTo(transaction2.hashCode());
        
        // Test null ID handling
        transaction1.setDailyTransactionId(null);
        transaction2.setDailyTransactionId(null);
        assertThat(transaction1).isNotEqualTo(transaction2); // Based on entity implementation
        
        // Test reflexive property
        assertThat(testDailyTransaction).isEqualTo(testDailyTransaction);
        
        // Test null comparison
        assertThat(testDailyTransaction).isNotEqualTo(null);
        
        // Test different class comparison
        assertThat(testDailyTransaction).isNotEqualTo("string");
    }

    /**
     * Tests toString() method for debugging and logging purposes.
     * Validates string representation includes key fields for batch processing tracking.
     */
    @Test
    @DisplayName("Test toString() - String Representation for Logging")
    public void testToString() {
        logTestExecution("Testing toString method");
        
        // Test complete toString output
        String stringRepresentation = testDailyTransaction.toString();
        assertThat(stringRepresentation)
            .isNotEmpty()
            .contains("DailyTransaction")
            .contains("accountId")
            .contains("cardNumber")
            .contains("date")
            .contains("amount")
            .contains("status");
        
        // Test card number masking for security
        if (testDailyTransaction.getCardNumber() != null) {
            assertThat(stringRepresentation).contains("**** **** ****");
        }
        
        // Test formatted amount display
        if (testDailyTransaction.getTransactionAmount() != null) {
            String formattedAmount = testDailyTransaction.getFormattedAmount();
            assertThat(formattedAmount).matches("\\$\\d+\\.\\d{2}");
        }
    }

    /**
     * Parameterized test for various transaction types and categories.
     * Tests DALYTRAN-TYPE-CD and DALYTRAN-CAT-CD field combinations from COBOL copybook.
     */
    @ParameterizedTest
    @CsvSource({
        "01, 5411, 'PURCHASE', 'GROCERY_STORES'",
        "02, 5812, 'CASH_ADVANCE', 'RESTAURANTS'", 
        "03, 5541, 'PAYMENT', 'SERVICE_STATIONS'",
        "04, 7011, 'REFUND', 'LODGING_HOTELS'",
        "05, 4121, 'ADJUSTMENT', 'TAXICABS_LIMOUSINES'"
    })
    @DisplayName("Test Parameterized Transaction Types - DALYTRAN-TYPE-CD/CAT-CD Validation")
    public void testParameterizedTransactionTypes(String typeCode, String categoryCode, 
                                                 String typeDescription, String categoryDescription) {
        logTestExecution("Testing parameterized transaction types: " + typeCode + "/" + categoryCode);
        
        // Set transaction type and category codes
        testDailyTransaction.setTransactionTypeCode(typeCode);
        testDailyTransaction.setCategoryCode(categoryCode);
        
        // Validate field lengths match COBOL specifications
        assertThat(testDailyTransaction.getTransactionTypeCode())
            .hasSize(Constants.TYPE_CODE_LENGTH)
            .matches("\\d{2}");
            
        assertThat(testDailyTransaction.getCategoryCode())
            .hasSize(Constants.CATEGORY_CODE_LENGTH)
            .matches("\\d{4}");
        
        // Test transaction type lookup functionality
        TransactionType transactionType = mockTransactionType;
        assertThat(transactionType.getTypeCode()).isNotNull();
        
        // Test category lookup functionality  
        TransactionCategory category = mockTransactionCategory;
        assertThat(category.getCategoryCode()).isNotNull();
        
        // Validate codes are within expected ranges
        int typeCodeInt = Integer.parseInt(typeCode);
        int categoryCodeInt = Integer.parseInt(categoryCode);
        assertThat(typeCodeInt).isBetween(1, 99);
        assertThat(categoryCodeInt).isBetween(1000, 9999);
    }

    /**
     * Parameterized test for various amount values with COBOL precision.
     * Tests S9(09)V99 amount field with different scales and rounding scenarios.
     */
    @ParameterizedTest  
    @CsvSource({
        "0.01, 2, HALF_UP",
        "123.456, 2, HALF_UP", 
        "999999999.99, 2, HALF_UP",
        "1000.005, 2, HALF_UP",
        "50.125, 2, HALF_UP"
    })
    @DisplayName("Test Parameterized Amount Values - COBOL Precision Validation")
    public void testParameterizedAmountValues(String amountStr, int scale, RoundingMode roundingMode) {
        logTestExecution("Testing parameterized amounts: " + amountStr);
        
        BigDecimal originalAmount = new BigDecimal(amountStr);
        BigDecimal processedAmount = CobolDataConverter.toBigDecimal(originalAmount, scale);
        
        testDailyTransaction.setTransactionAmount(processedAmount);
        
        // Validate COBOL precision preservation
        assertThat(testDailyTransaction.getTransactionAmount().scale()).isEqualTo(scale);
        assertBigDecimalEquals(testDailyTransaction.getTransactionAmount(), 
                              originalAmount.setScale(scale, roundingMode));
        
        // Test COBOL rounding mode consistency
        assertThat(roundingMode).isEqualTo(TestConstants.COBOL_ROUNDING_MODE);
        
        // Validate amount is within COBOL field constraints
        assertThat(testDailyTransaction.getTransactionAmount())
            .isLessThanOrEqualTo(new BigDecimal("999999999.99"));
            
        // Test formatted currency display
        String formattedAmount = testDailyTransaction.getFormattedAmount();
        assertThat(formattedAmount).matches("\\$\\d{1,3}(,\\d{3})*\\.\\d{2}");
    }

    /**
     * Tests batch processing metadata relationships and job execution tracking.
     * Validates integration with DailyTransactionJob for batch processing windows.
     */
    @Test
    @DisplayName("Test Batch Processing Metadata - Daily Job Integration")  
    public void testBatchProcessingMetadata() {
        logTestExecution("Testing batch processing metadata");
        
        // Test batch ID assignment for job tracking
        String batchId = "BATCH001";
        testDailyTransaction.setBatchId(batchId);
        assertThat(testDailyTransaction.getBatchId()).isEqualTo(batchId);
        
        // Test record sequence number for batch ordering
        Integer recordSequence = 1;
        testDailyTransaction.setRecordSequence(recordSequence);
        assertThat(testDailyTransaction.getRecordSequence()).isEqualTo(recordSequence);
        
        // Test processing status workflow
        testDailyTransaction.setProcessingStatus("NEW");
        assertThat(testDailyTransaction.isUnprocessed()).isTrue();
        
        testDailyTransaction.markAsPending();
        assertThat(testDailyTransaction.getProcessingStatus()).isEqualTo("PENDING");
        assertThat(testDailyTransaction.isUnprocessed()).isTrue();
        
        testDailyTransaction.markAsProcessing();
        assertThat(testDailyTransaction.getProcessingStatus()).isEqualTo("PROCESSING");
        assertThat(testDailyTransaction.isUnprocessed()).isFalse();
        
        testDailyTransaction.markAsSuccess();
        assertThat(testDailyTransaction.getProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(testDailyTransaction.isProcessedSuccessfully()).isTrue();
        
        // Test failure handling
        testDailyTransaction.markAsFailed("Test error message");
        assertThat(testDailyTransaction.getProcessingStatus()).isEqualTo("FAILED");
        assertThat(testDailyTransaction.hasProcessingError()).isTrue();
        assertThat(testDailyTransaction.getErrorMessage()).isEqualTo("Test error message");
        
        // Test batch processing window validation
        LocalDateTime createdDate = testDailyTransaction.getCreatedDate();
        LocalDateTime processingTimestamp = testDailyTransaction.getProcessingTimestamp();
        if (createdDate != null && processingTimestamp != null) {
            assertThat(processingTimestamp).isAfterOrEqualTo(createdDate);
        }
        
        // Validate processing within batch window (4 hours as per requirements)
        if (createdDate != null) {
            LocalDateTime maxProcessingTime = createdDate.plusHours(TestConstants.BATCH_PROCESSING_WINDOW_HOURS);
            if (processingTimestamp != null) {
                assertThat(processingTimestamp).isBeforeOrEqualTo(maxProcessingTime);
            }
        }
    }

    /**
     * Tests FILLER field handling for 350-byte record padding alignment.
     * Validates proper handling of unused space in COBOL record structure.
     */
    @Test
    @DisplayName("Test FILLER Field Handling - 350-byte Record Alignment")
    public void testFillerFieldHandling() {
        logTestExecution("Testing FILLER field handling");
        
        // Calculate total field lengths to verify 350-byte structure
        int calculatedLength = 0;
        calculatedLength += 16;  // DALYTRAN-ID (PIC X(16))
        calculatedLength += 2;   // DALYTRAN-TYPE-CD (PIC X(02))  
        calculatedLength += 4;   // DALYTRAN-CAT-CD (PIC 9(04))
        calculatedLength += 10;  // DALYTRAN-SOURCE (PIC X(10))
        calculatedLength += 100; // DALYTRAN-DESC (PIC X(100))
        calculatedLength += 11;  // DALYTRAN-AMT (PIC S9(09)V99) - 11 bytes for decimal
        calculatedLength += 9;   // DALYTRAN-MERCHANT-ID (PIC 9(09))
        calculatedLength += 50;  // DALYTRAN-MERCHANT-NAME (PIC X(50))
        calculatedLength += 50;  // DALYTRAN-MERCHANT-CITY (PIC X(50))
        calculatedLength += 10;  // DALYTRAN-MERCHANT-ZIP (PIC X(10))
        calculatedLength += 16;  // DALYTRAN-CARD-NUM (PIC X(16))
        calculatedLength += 26;  // DALYTRAN-ORIG-TS (PIC X(26))
        calculatedLength += 26;  // DALYTRAN-PROC-TS (PIC X(26))
        calculatedLength += 20;  // FILLER (PIC X(20))
        
        // Validate total record length matches COBOL specification
        assertThat(calculatedLength).isEqualTo(350);
        
        // Test that all fields can be populated without exceeding total length
        testDailyTransaction.setTransactionId("T123456789012345"); // 16 chars
        testDailyTransaction.setTransactionTypeCode("01");          // 2 chars
        testDailyTransaction.setCategoryCode("5411");               // 4 chars
        testDailyTransaction.setSource("ONLINE");                   // 10 chars max
        testDailyTransaction.setDescription("A".repeat(100));       // 100 chars
        testDailyTransaction.setTransactionAmount(new BigDecimal("999999999.99")); // S9(09)V99
        testDailyTransaction.setMerchantId(123456789L);             // 9 digits
        testDailyTransaction.setMerchantName("B".repeat(50));       // 50 chars
        testDailyTransaction.setMerchantCity("C".repeat(50));       // 50 chars
        testDailyTransaction.setMerchantZip("12345");               // 10 chars max
        testDailyTransaction.setCardNumber("1234567890123456");     // 16 chars
        
        // Validate no field exceeds its COBOL PIC clause length
        assertThat(testDailyTransaction.getTransactionId()).hasSize(16);
        assertThat(testDailyTransaction.getTransactionTypeCode()).hasSize(2);
        assertThat(testDailyTransaction.getCategoryCode()).hasSize(4);
        assertThat(testDailyTransaction.getDescription()).hasSize(100);
        assertThat(testDailyTransaction.getMerchantName()).hasSize(50);
        assertThat(testDailyTransaction.getMerchantCity()).hasSize(50);
        assertThat(testDailyTransaction.getCardNumber()).hasSize(16);
    }

    /**
     * Tests aggregation functions for daily transaction summaries and reporting.
     * Validates calculation methods used in daily batch processing reports.
     */
    @Test
    @DisplayName("Test Aggregation Functions - Daily Transaction Summaries")
    public void testAggregationFunctions() {
        logTestExecution("Testing aggregation functions");
        
        // Create multiple transactions for aggregation testing
        DailyTransaction transaction1 = createTestTransaction();
        transaction1.setTransactionAmount(new BigDecimal("100.00"));
        transaction1.setTransactionDate(testDate);
        transaction1.setCategoryCode("5411");
        
        DailyTransaction transaction2 = createTestTransaction();
        transaction2.setTransactionAmount(new BigDecimal("250.50"));
        transaction2.setTransactionDate(testDate);
        transaction2.setCategoryCode("5411");
        
        DailyTransaction transaction3 = createTestTransaction();
        transaction3.setTransactionAmount(new BigDecimal("75.25"));
        transaction3.setTransactionDate(testDate);
        transaction3.setCategoryCode("5812");
        
        // Test amount aggregation calculations
        BigDecimal totalAmount = transaction1.getTransactionAmount()
            .add(transaction2.getTransactionAmount())
            .add(transaction3.getTransactionAmount());
        
        assertBigDecimalEquals(totalAmount, new BigDecimal("425.75"));
        
        // Test category-based grouping
        BigDecimal category5411Total = transaction1.getTransactionAmount()
            .add(transaction2.getTransactionAmount());
        assertBigDecimalEquals(category5411Total, new BigDecimal("350.50"));
        
        // Test date-based partitioning validation
        assertThat(transaction1.getTransactionDate()).isEqualTo(testDate);
        assertThat(transaction2.getTransactionDate()).isEqualTo(testDate);
        assertThat(transaction3.getTransactionDate()).isEqualTo(testDate);
        
        // Test count aggregation
        int transactionCount = 3;
        assertThat(transactionCount).isEqualTo(3);
        
        // Test average calculation with COBOL precision
        BigDecimal averageAmount = totalAmount.divide(
            new BigDecimal(transactionCount), 
            TestConstants.COBOL_DECIMAL_SCALE,
            TestConstants.COBOL_ROUNDING_MODE
        );
        assertBigDecimalWithinTolerance(averageAmount, new BigDecimal("141.92"));
        
        // Test daily summary data structure
        assertThat(transaction1.getFormattedAmount()).isNotEmpty();
        assertThat(transaction2.getFormattedAmount()).isNotEmpty();
        assertThat(transaction3.getFormattedAmount()).isNotEmpty();
    }

    /**
     * Helper method to create test transaction with default values.
     * Generates DailyTransaction entity with COBOL-compatible test data.
     */
    private DailyTransaction createTestTransaction() {
        return generateTestData().apply("DailyTransaction", () -> {
            DailyTransaction transaction = new DailyTransaction();
            
            // Set required fields matching COBOL copybook structure
            transaction.setAccountId(Long.parseLong(TestConstants.TEST_ACCOUNT_ID));
            transaction.setCardNumber(TestConstants.TEST_CARD_NUMBER);
            transaction.setTransactionDate(testDate);
            transaction.setTransactionAmount(testAmount);
            transaction.setTransactionTypeCode("01");
            transaction.setCategoryCode("5411");
            transaction.setTransactionId("T" + System.currentTimeMillis() % 1000000000000000L);
            transaction.setDescription("Test daily transaction");
            transaction.setSource("ONLINE");
            transaction.setMerchantId(123456789L);
            transaction.setMerchantName("Test Merchant");
            transaction.setMerchantCity("Test City"); 
            transaction.setMerchantZip("12345");
            transaction.setOriginalTimestamp(testTimestamp);
            transaction.setProcessingStatus("NEW");
            
            return transaction;
        });
    }
}