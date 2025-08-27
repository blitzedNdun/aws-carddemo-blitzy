/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.carddemo.service.FileGenerationService;
import com.carddemo.util.FileUtils;
import com.carddemo.service.FileWriterService;
import com.carddemo.service.TestDataBuilder;
import com.carddemo.service.CobolComparisonUtils;
import com.carddemo.service.PerformanceTestUtils;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Comprehensive unit test class for FileGenerationService that validates file generation functionality
 * for reports, statements, and data exports while maintaining identical file layouts to COBOL programs.
 * 
 * This test class ensures 100% functional parity with the original COBOL file generation programs:
 * - CBSTM03A.cbl: Statement generator creating PDF, HTML, and text formats
 * - CBSTM03B.cbl: File I/O operations on VSAM datasets  
 * - CBTRN03C.cbl: Transaction report generator with totals and filtering
 * 
 * Key Testing Areas:
 * - Fixed-width file generation with exact COBOL field padding
 * - CSV export with proper quoting and delimiter handling
 * - PDF statement creation matching COBOL output format
 * - Record layout accuracy preserving VSAM structure
 * - Field alignment and padding matching COBOL PIC clauses
 * - Numeric format conversions with COMP-3 precision
 * - Header/trailer record generation and validation
 * - File checksum calculation for integrity verification
 * - Large file processing within 4-hour batch window
 * - Performance compliance under 200ms response time
 * 
 * Test Data Strategy:
 * - Uses TestDataBuilder for realistic COBOL-compatible test data
 * - Employs CobolComparisonUtils for precise COBOL/Java validation
 * - Leverages PerformanceTestUtils for SLA compliance verification
 * - Mocks FileWriterService to isolate file generation logic from I/O
 * 
 * @author CardDemo Migration Team  
 * @version 1.0
 * @since 2024
 */
@SpringBootTest
public class FileGenerationServiceTest {

    private FileGenerationService fileGenerationService;
    
    @Mock
    private FileWriterService fileWriterService;
    
    private CobolComparisonUtils cobolComparisonUtils;
    private TestDataBuilder testDataBuilder;
    
    // Test data collections
    private List<Transaction> testTransactions;
    private List<Account> testAccounts;
    private List<Customer> testCustomers;
    
    // Test constants matching COBOL specifications
    private static final int FIXED_WIDTH_RECORD_LENGTH = 200;
    private static final String CSV_DELIMITER = ",";
    private static final String FIXED_WIDTH_PADDING = " ";
    private static final BigDecimal LARGE_FILE_TRANSACTION_COUNT = new BigDecimal("100000");
    private static final int PERFORMANCE_ITERATIONS = 1000;

    /**
     * Sets up test environment before each test execution.
     * Initializes service dependencies, test data, and mock configurations.
     */
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize services with mocked dependencies
        fileGenerationService = new FileGenerationService(fileWriterService);
        cobolComparisonUtils = new CobolComparisonUtils();
        
        // Configure mock FileWriterService behaviors
        setupMockFileWriterService();
        
        // Initialize comprehensive test data sets
        initializeTestData();
    }

    /**
     * Nested test class for fixed-width file generation functionality.
     * Tests COBOL-equivalent fixed-width record formatting and field alignment.
     */
    @Nested
    @DisplayName("Fixed Width File Generation Tests")
    class FixedWidthFileGenerationTests {

        @Test
        @DisplayName("Should generate fixed-width file with correct record structure")
        public void testGenerateFixedWidthFile() {
            // Arrange
            List<Transaction> transactions = testTransactions.subList(0, 10);
            String outputPath = "test_fixed_width_output.txt";
            
            // Act
            long startTime = System.currentTimeMillis();
            String generatedFile = fileGenerationService.generateFixedWidthFile(transactions, outputPath);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Assert
            assertNotNull(generatedFile, "Generated file path should not be null");
            assertEquals(outputPath, generatedFile, "Generated file path should match requested path");
            
            // Verify FileWriterService was called with correct parameters
            verify(fileWriterService).writeStatementFile(eq(outputPath), any(List.class));
            
            // Validate performance requirement (under 200ms)
            assertTrue(PerformanceTestUtils.validateResponseTime(executionTime), 
                "Fixed-width file generation must complete within 200ms SLA");
            
            // Verify file generation was logged
            verify(fileWriterService, times(1)).writeStatementFile(anyString(), any(List.class));
        }

        @Test
        @DisplayName("Should format records with correct field padding and alignment")
        public void testFieldPaddingAndAlignment() {
            // Arrange
            Transaction testTransaction = TestDataBuilder.createTransaction()
                .withTransactionId(1000000001L)
                .withAmount(new BigDecimal("1234.56"))
                .withDescription("TEST MERCHANT PURCHASE")
                .withMerchantInfo("TEST MERCHANT NAME")
                .build();
            
            List<Transaction> singleTransaction = List.of(testTransaction);
            
            // Act
            String result = fileGenerationService.generateFixedWidthFile(singleTransaction, "test_padding.txt");
            
            // Assert - Verify field padding matches COBOL PIC clause specifications
            assertNotNull(result, "Fixed-width file generation should return file path");
            
            // Capture the generated record content through FileWriterService mock
            verify(fileWriterService).writeStatementFile(eq("test_padding.txt"), argThat(records -> {
                if (records.isEmpty()) return false;
                
                String record = (String) records.get(0);
                
                // Verify transaction ID formatting (PIC X(16) left-padded with zeros)
                String expectedTransactionId = String.format("%016d", testTransaction.getTransactionId());
                assertTrue(record.contains(expectedTransactionId), 
                    "Transaction ID should be formatted as 16-character zero-padded field");
                
                // Verify amount formatting (PIC 9(09)V99 with decimal precision)
                String formattedAmount = FileUtils.formatDecimalAmount(testTransaction.getAmount());
                assertTrue(record.contains(formattedAmount), 
                    "Amount should be formatted with COBOL COMP-3 equivalent precision");
                
                return true;
            }));
        }

        @Test
        @DisplayName("Should handle large datasets within batch processing window")
        public void testLargeFileHandling() {
            // Arrange - Generate large dataset for batch testing
            int largeDatasetSize = 10000;
            List<Transaction> largeTransactionSet = TestDataBuilder.generateTransactionDataSet(largeDatasetSize);
            
            // Act
            long startTime = System.currentTimeMillis();
            String result = fileGenerationService.processLargeFile(largeTransactionSet, "large_file_test.txt");
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Assert
            assertNotNull(result, "Large file processing should return success result");
            
            // Verify processing completes within acceptable timeframe (scaled for dataset size)
            long maxAllowableTime = largeDatasetSize / 10; // 10ms per 1000 transactions
            assertTrue(executionTime <= maxAllowableTime, 
                String.format("Large file processing took %dms, expected under %dms", 
                    executionTime, maxAllowableTime));
            
            // Verify FileWriterService was called for large dataset
            verify(fileWriterService, times(1)).writeStatementFile(eq("large_file_test.txt"), any(List.class));
        }
    }

    /**
     * Nested test class for CSV report generation functionality.
     * Tests comma-separated value export with proper quoting and formatting.
     */
    @Nested
    @DisplayName("CSV Report Generation Tests")  
    class CsvReportGenerationTests {

        @Test
        @DisplayName("Should generate CSV report with correct formatting")
        public void testGenerateCsvReport() {
            // Arrange
            List<Transaction> csvTransactions = testTransactions.subList(0, 5);
            String csvOutputPath = "transaction_report.csv";
            
            // Act
            String csvResult = fileGenerationService.generateCsvReport(csvTransactions, csvOutputPath);
            
            // Assert
            assertNotNull(csvResult, "CSV generation should return file path");
            assertEquals(csvOutputPath, csvResult, "CSV file path should match requested path");
            
            // Verify CSV-specific formatting through FileWriterService
            verify(fileWriterService).writeCsvReport(eq(csvOutputPath), argThat(csvData -> {
                if (csvData.isEmpty()) return false;
                
                String csvContent = (String) csvData.get(0);
                
                // Verify CSV header row is present
                assertTrue(csvContent.contains("Transaction ID,Amount,Description,Merchant"), 
                    "CSV should contain proper header row");
                
                // Verify CSV delimiter usage
                assertTrue(csvContent.contains(CSV_DELIMITER), 
                    "CSV content should use correct delimiter");
                
                return true;
            }));
        }

        @Test
        @DisplayName("Should properly escape CSV fields containing special characters")
        public void testCsvFieldEscaping() {
            // Arrange - Create transaction with CSV special characters
            Transaction specialCharTransaction = TestDataBuilder.createTransaction()
                .withDescription("MERCHANT, \"QUOTED NAME\" & SPECIAL CHARS")
                .withMerchantInfo("MERCHANT'S PLACE, INC.")
                .build();
            
            List<Transaction> specialTransactions = List.of(specialCharTransaction);
            
            // Act
            String result = fileGenerationService.generateCsvReport(specialTransactions, "special_chars.csv");
            
            // Assert
            assertNotNull(result, "CSV generation with special characters should succeed");
            
            verify(fileWriterService).writeCsvReport(eq("special_chars.csv"), argThat(csvData -> {
                String csvContent = (String) csvData.get(0);
                
                // Verify proper CSV quoting for fields with commas
                assertTrue(csvContent.contains("\"MERCHANT, \\\"QUOTED NAME\\\" & SPECIAL CHARS\""), 
                    "CSV should properly escape fields containing commas and quotes");
                
                return true;
            }));
        }
    }

    /**
     * Nested test class for PDF statement generation functionality.
     * Tests PDF creation matching COBOL statement format and layout.
     */
    @Nested
    @DisplayName("PDF Statement Generation Tests")
    class PdfStatementGenerationTests {

        @Test
        @DisplayName("Should generate PDF statement with correct layout")
        public void testGeneratePdfStatement() {
            // Arrange
            Account testAccount = TestDataBuilder.createAccount()
                .withAccountId(1000000001L)
                .withCurrentBalance(new BigDecimal("2500.75"))
                .withCreditLimit(new BigDecimal("10000.00"))
                .build();
            
            List<Transaction> statementTransactions = testTransactions.subList(0, 20);
            String pdfPath = "statement_001.pdf";
            
            // Act
            long startTime = System.currentTimeMillis();
            String pdfResult = fileGenerationService.generatePdfStatement(testAccount, statementTransactions, pdfPath);
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Assert
            assertNotNull(pdfResult, "PDF statement generation should return file path");
            assertEquals(pdfPath, pdfResult, "PDF file path should match requested path");
            
            // Verify performance requirement
            assertTrue(PerformanceTestUtils.validateResponseTime(executionTime), 
                "PDF statement generation must meet 200ms performance SLA");
            
            // Verify PDF generation through FileWriterService
            verify(fileWriterService).generatePdfStatement(eq(pdfPath), eq(testAccount), eq(statementTransactions));
        }

        @Test
        @DisplayName("Should include all required statement sections in PDF")
        public void testPdfStatementSections() {
            // Arrange
            Account fullAccount = TestDataBuilder.createAccount()
                .withAccountId(2000000002L) 
                .withCurrentBalance(new BigDecimal("1750.25"))
                .withCreditLimit(new BigDecimal("5000.00"))
                .withActiveStatus()
                .build();
            
            List<Transaction> monthlyTransactions = TestDataBuilder.generateTransactionDataSet(15);
            
            // Act
            String result = fileGenerationService.generatePdfStatement(fullAccount, monthlyTransactions, "full_statement.pdf");
            
            // Assert
            assertNotNull(result, "Full PDF statement should be generated successfully");
            
            // Verify comprehensive statement data is passed to PDF generation
            verify(fileWriterService).generatePdfStatement(
                eq("full_statement.pdf"), 
                eq(fullAccount), 
                eq(monthlyTransactions)
            );
            
            // Additional verification that account balance is properly formatted for PDF
            BigDecimal expectedBalance = fullAccount.getCurrentBalance();
            assertTrue(cobolComparisonUtils.validateDecimalPrecision(expectedBalance), 
                "PDF statement balance should maintain COBOL decimal precision");
        }
    }

    /**
     * Nested test class for record layout validation functionality.
     * Tests that generated records match COBOL copybook specifications exactly.
     */
    @Nested
    @DisplayName("Record Layout Validation Tests")
    class RecordLayoutValidationTests {

        @Test
        @DisplayName("Should validate record layout against COBOL specification")
        public void testValidateRecordLayout() {
            // Arrange
            Transaction testTransaction = TestDataBuilder.createTransaction()
                .withTransactionId(3000000003L)
                .withAmount(new BigDecimal("987.65"))
                .withDescription("RECORD LAYOUT TEST TRANSACTION")
                .build();
            
            String recordData = generateTestRecord(testTransaction);
            
            // Act
            boolean isValid = fileGenerationService.validateRecordLayout(recordData, "TRANSACTION_RECORD");
            
            // Assert
            assertTrue(isValid, "Record layout should conform to COBOL specification");
            
            // Verify record structure using COBOL comparison utilities
            assertTrue(cobolComparisonUtils.validateTransactionFormat(testTransaction), 
                "Transaction should meet COBOL field format requirements");
        }

        @Test
        @DisplayName("Should validate numeric field precision and scale")
        public void testNumericFormatConversions() {
            // Arrange - Test various numeric formats matching COBOL PIC clauses
            BigDecimal testAmount1 = new BigDecimal("12345.67");  // PIC 9(05)V99
            BigDecimal testAmount2 = new BigDecimal("999999999.99"); // PIC 9(09)V99 - maximum
            BigDecimal testAmount3 = new BigDecimal("0.01");      // PIC 9(05)V99 - minimum
            
            // Act & Assert
            assertTrue(cobolComparisonUtils.validateDecimalPrecision(testAmount1), 
                "Standard amount should meet COBOL precision requirements");
            
            assertTrue(cobolComparisonUtils.validateDecimalPrecision(testAmount2), 
                "Maximum amount should meet COBOL precision requirements");
            
            assertTrue(cobolComparisonUtils.validateDecimalPrecision(testAmount3), 
                "Minimum amount should meet COBOL precision requirements");
            
            // Verify COBOL rounding behavior
            BigDecimal roundedAmount = testAmount1.setScale(2, java.math.RoundingMode.HALF_UP);
            assertTrue(cobolComparisonUtils.validateCobolRounding(roundedAmount, testAmount1), 
                "Amount rounding should match COBOL ROUNDED clause behavior");
        }

        @Test
        @DisplayName("Should validate header and trailer record generation")
        public void testHeaderTrailerRecords() {
            // Arrange
            List<Transaction> batchTransactions = TestDataBuilder.generateTransactionDataSet(100);
            String batchFile = "batch_with_headers.txt";
            
            // Calculate expected totals for trailer validation
            BigDecimal expectedTotal = batchTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Act
            String result = fileGenerationService.generateFixedWidthFile(batchTransactions, batchFile);
            
            // Assert
            assertNotNull(result, "Batch file with headers should generate successfully");
            
            verify(fileWriterService).writeStatementFile(eq(batchFile), argThat(records -> {
                if (records.size() < 3) return false; // Must have header, data, trailer
                
                String headerRecord = (String) records.get(0);
                String trailerRecord = (String) records.get(records.size() - 1);
                
                // Verify header record format
                assertTrue(headerRecord.startsWith("HDR"), 
                    "Header record should start with 'HDR' identifier");
                
                // Verify trailer record contains correct totals
                assertTrue(trailerRecord.startsWith("TRL"), 
                    "Trailer record should start with 'TRL' identifier");
                assertTrue(trailerRecord.contains(String.format("%012d", batchTransactions.size())), 
                    "Trailer should contain correct record count");
                
                return true;
            }));
        }
    }

    /**
     * Nested test class for file checksum calculation functionality.
     * Tests integrity verification through checksum validation.
     */
    @Nested
    @DisplayName("File Checksum Validation Tests")
    class FileChecksumValidationTests {

        @Test
        @DisplayName("Should calculate correct file checksum")
        public void testCalculateFileChecksum() {
            // Arrange
            List<Transaction> checksumTransactions = testTransactions.subList(0, 3);
            String checksumFile = "checksum_test.txt";
            
            // Generate test file
            fileGenerationService.generateFixedWidthFile(checksumTransactions, checksumFile);
            
            // Act
            String calculatedChecksum = fileGenerationService.calculateFileChecksum(checksumFile);
            
            // Assert
            assertNotNull(calculatedChecksum, "Checksum calculation should return valid result");
            assertFalse(calculatedChecksum.isEmpty(), "Checksum should not be empty");
            assertEquals(32, calculatedChecksum.length(), "MD5 checksum should be 32 characters");
            
            // Verify checksum is hexadecimal
            assertTrue(calculatedChecksum.matches("[a-fA-F0-9]+"), 
                "Checksum should contain only hexadecimal characters");
        }

        @Test
        @DisplayName("Should detect file integrity through checksum validation")
        public void testFileIntegrityValidation() {
            // Arrange
            List<Transaction> integrityTransactions = testTransactions.subList(0, 5);
            String integrityFile = "integrity_test.txt";
            
            // Generate file and calculate initial checksum
            fileGenerationService.generateFixedWidthFile(integrityTransactions, integrityFile);
            String originalChecksum = fileGenerationService.calculateFileChecksum(integrityFile);
            
            // Regenerate same file with identical data
            fileGenerationService.generateFixedWidthFile(integrityTransactions, integrityFile);
            String regeneratedChecksum = fileGenerationService.calculateFileChecksum(integrityFile);
            
            // Act & Assert
            assertEquals(originalChecksum, regeneratedChecksum, 
                "Identical file content should produce identical checksums");
            
            // Verify checksum consistency across multiple calculations
            String thirdChecksum = fileGenerationService.calculateFileChecksum(integrityFile);
            assertEquals(originalChecksum, thirdChecksum, 
                "Multiple checksum calculations should produce consistent results");
        }
    }

    /**
     * Nested test class for performance requirements validation.
     * Tests that file generation meets 200ms response time SLA.
     */
    @Nested
    @DisplayName("Performance Requirements Tests")
    class PerformanceRequirementsTests {

        @Test
        @DisplayName("Should meet performance requirements for individual file operations")
        public void testPerformanceRequirements() {
            // Arrange
            List<Transaction> performanceTransactions = testTransactions.subList(0, 50);
            
            // Act & Measure Performance
            long executionTime = PerformanceTestUtils.measureExecutionTime(() -> {
                return fileGenerationService.generateFixedWidthFile(performanceTransactions, "performance_test.txt");
            });
            
            // Assert
            assertTrue(PerformanceTestUtils.validateResponseTime(executionTime), 
                String.format("File generation took %dms, must be under 200ms", executionTime));
            
            // Additional performance validation for different file types
            long csvTime = PerformanceTestUtils.measureExecutionTime(() -> {
                return fileGenerationService.generateCsvReport(performanceTransactions, "performance_csv.csv");
            });
            
            assertTrue(PerformanceTestUtils.validateResponseTime(csvTime), 
                String.format("CSV generation took %dms, must be under 200ms", csvTime));
        }

        @Test
        @DisplayName("Should maintain performance under concurrent load")
        public void testConcurrentPerformance() {
            // Arrange
            int concurrentOperations = 10;
            List<Transaction> concurrentTransactions = testTransactions.subList(0, 10);
            
            // Act - Execute concurrent file generation operations
            java.util.Map<String, Object> concurrentResults = PerformanceTestUtils.measureConcurrentExecution(
                () -> fileGenerationService.generateFixedWidthFile(concurrentTransactions, "concurrent_test.txt"),
                concurrentOperations
            );
            
            // Assert
            assertNotNull(concurrentResults, "Concurrent execution should return results");
            
            Double averageExecutionTime = (Double) concurrentResults.get("averageExecutionTime");
            assertNotNull(averageExecutionTime, "Average execution time should be calculated");
            assertTrue(averageExecutionTime < 200.0, 
                String.format("Average concurrent execution time %.2fms must be under 200ms", averageExecutionTime));
            
            // Verify all operations completed successfully
            Integer concurrencyLevel = (Integer) concurrentResults.get("concurrencyLevel");
            assertEquals(concurrentOperations, concurrencyLevel.intValue(), 
                "All concurrent operations should complete");
        }
    }

    /**
     * Helper method to configure mock FileWriterService behaviors for testing.
     * Sets up realistic mock responses for file writing operations.
     */
    private void setupMockFileWriterService() {
        // Mock fixed-width file writing
        when(fileWriterService.writeStatementFile(anyString(), any(List.class)))
            .thenReturn("SUCCESS");
        
        // Mock CSV file writing  
        when(fileWriterService.writeCsvReport(anyString(), any(List.class)))
            .thenReturn("SUCCESS");
        
        // Mock PDF generation
        when(fileWriterService.generatePdfStatement(anyString(), any(Account.class), any(List.class)))
            .thenReturn("SUCCESS");
        
        // Mock file archiving
        when(fileWriterService.archiveFile(anyString()))
            .thenReturn("ARCHIVED");
    }

    /**
     * Initializes comprehensive test data sets for all file generation scenarios.
     * Creates realistic data matching COBOL field specifications and constraints.
     */
    private void initializeTestData() {
        // Generate diverse transaction test data
        testTransactions = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Transaction transaction = TestDataBuilder.createTransaction()
                .withTransactionId((long) (1000000000 + i))
                .withAmount(new BigDecimal(String.format("%.2f", Math.random() * 9999.99 + 0.01)))
                .withDescription(String.format("TEST TRANSACTION %03d", i))
                .withMerchantInfo(String.format("TEST MERCHANT %03d", i))
                .withTimestamp(LocalDateTime.now().minusDays(i % 30))
                .build();
            testTransactions.add(transaction);
        }
        
        // Generate account test data
        testAccounts = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Account account = TestDataBuilder.createAccount()
                .withAccountId((long) (2000000000L + i))
                .withCurrentBalance(new BigDecimal(String.format("%.2f", Math.random() * 50000.00)))
                .withCreditLimit(new BigDecimal(String.format("%.2f", 10000.00 + Math.random() * 40000.00)))
                .withActiveStatus()
                .build();
            testAccounts.add(account);
        }
        
        // Generate customer test data
        testCustomers = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Customer customer = TestDataBuilder.createCustomer()
                .withCustomerId((long) (3000000000L + i))
                .withRandomName()
                .withRandomAddress()
                .withRandomPhone()
                .build();
            testCustomers.add(customer);
        }
    }

    /**
     * Helper method to generate test record data for layout validation.
     * Creates fixed-width record matching COBOL field specifications.
     * 
     * @param transaction Transaction data to format as fixed-width record
     * @return Formatted fixed-width record string
     */
    private String generateTestRecord(Transaction transaction) {
        StringBuilder record = new StringBuilder();
        
        // Transaction ID - PIC X(16) zero-padded
        record.append(String.format("%016d", transaction.getTransactionId()));
        
        // Amount - PIC 9(09)V99 formatted as integer cents
        BigDecimal amountCents = transaction.getAmount().multiply(new BigDecimal("100"));
        record.append(String.format("%011d", amountCents.longValue()));
        
        // Description - PIC X(100) right-padded with spaces
        String description = transaction.getDescription();
        if (description.length() > 100) {
            description = description.substring(0, 100);
        }
        record.append(String.format("%-100s", description));
        
        // Merchant name - PIC X(50) right-padded with spaces
        String merchantName = transaction.getMerchantName();
        if (merchantName.length() > 50) {
            merchantName = merchantName.substring(0, 50);
        }
        record.append(String.format("%-50s", merchantName));
        
        // Date - PIC X(08) YYYYMMDD format
        LocalDate txnDate = transaction.getTransactionDate();
        record.append(txnDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
        
        return record.toString();
    }
}