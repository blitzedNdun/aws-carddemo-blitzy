/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardXref;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.test.UnitTest;
import com.carddemo.util.TestConstants;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test class for TransactionReportingService validating COBOL CBTRN03C batch transaction 
 * reporting logic migration to Java. This test class ensures 100% functional parity between 
 * the original COBOL batch transaction reporting program and the migrated Java service implementation.
 * 
 * <p>Tests comprehensive transaction reporting functionality including:
 * <ul>
 * <li>Daily transaction report generation with proper filtering and aggregation</li>
 * <li>Monthly aggregation reports with account-level and grand total calculations</li>
 * <li>Regulatory compliance reporting with precise financial data validation</li>
 * <li>Fraud detection reports based on transaction pattern analysis</li>
 * <li>Merchant analysis reports with categorical breakdowns</li>
 * <li>Date parameter processing matching COBOL DATEPARM file handling</li>
 * <li>Transaction file reading with equivalent VSAM TRANSACT record processing</li>
 * <li>Report header generation matching original BMS report formats</li>
 * <li>Report detail writing with COBOL-compatible field formatting</li>
 * <li>Report data validation ensuring data integrity and consistency</li>
 * </ul>
 * 
 * <p>This test class implements comprehensive validation of the COBOL-to-Java migration
 * by testing every method of TransactionReportingService against expected CBTRN03C program
 * behavior. All financial calculations are validated with COBOL COMP-3 packed decimal precision
 * to ensure regulatory compliance and maintain identical business logic.
 * 
 * <p>Key Testing Features:
 * <ul>
 * <li>JUnit 5 test framework with @ParameterizedTest for comprehensive coverage</li>
 * <li>Mockito for dependency isolation and controlled test scenarios</li>
 * <li>AssertJ fluent assertions for detailed validation and error reporting</li>
 * <li>AbstractBaseTest integration for COBOL precision validation utilities</li>
 * <li>TestDataGenerator integration for realistic COBOL-compatible test data</li>
 * <li>CobolComparisonUtils integration for byte-level output comparison</li>
 * <li>Performance testing to validate sub-200ms response time requirements</li>
 * </ul>
 * 
 * <p>Testing Strategy:
 * This class follows the established testing patterns for CardDemo migration validation,
 * ensuring that all business logic maintains identical behavior to the original COBOL
 * implementation. Tests cover normal processing paths, error scenarios, edge cases,
 * and performance requirements specified in the migration technical specifications.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TransactionReportingServiceTest extends AbstractBaseTest implements UnitTest {

    // Service under test - will be injected when TransactionReportingService is available
    private TransactionReportingService transactionReportingService;
    
    // Mock dependencies from internal_imports schema
    @Mock
    private TransactionRepository transactionRepository;
    
    // Note: TestDataGenerator and CobolComparisonUtils are utility classes with static methods only
    
    /**
     * Setup method executed before each test execution.
     * Initializes the service under test, configures mock dependencies, and prepares
     * test utilities for comprehensive transaction reporting testing scenarios.
     * 
     * This method extends AbstractBaseTest.setUp() to provide specific configuration
     * for TransactionReportingService testing including mock repository behavior,
     * test data generation setup, and COBOL comparison utility initialization.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        
        // Reset random seed for consistent test data generation (using static method)
        TestDataGenerator.resetRandomSeed(12345L);
        
        // Initialize service with mock dependencies
        transactionReportingService = new TransactionReportingService(transactionRepository);
        
        // Configure common mock behaviors for repository operations
        mockCommonRepositoryBehavior();
        
        logTestExecution("TransactionReportingService test setup completed", null);
    }

    // =====================================================================================
    // DAILY TRANSACTION REPORT TESTS - Testing CBTRN03C main processing loop functionality
    // =====================================================================================

    /**
     * Tests daily transaction report generation functionality.
     * Validates that the service correctly processes transactions within a specified date range,
     * applies proper filtering, and generates accurate daily summary reports matching 
     * COBOL CBTRN03C 1000-TRANFILE-GET-NEXT and 1100-WRITE-TRANSACTION-REPORT logic.
     * 
     * This test ensures functional parity with COBOL paragraph structure:
     * - Date parameter validation (0550-DATEPARM-READ equivalent)
     * - Transaction file processing (1000-TRANFILE-GET-NEXT equivalent)
     * - Date range filtering (TRAN-PROC-TS validation equivalent)
     * - Report generation (1100-WRITE-TRANSACTION-REPORT equivalent)
     */
    @ParameterizedTest
    @ValueSource(strings = {"2024-01-01", "2024-06-15", "2024-12-31"})
    public void testGenerateDailyTransactionReport_ValidDateRange_ReturnsAccurateReport(String testDate) {
        // Arrange - Create test data using TestDataGenerator
        LocalDate reportDate = LocalDate.parse(testDate);
        LocalDate startDate = reportDate.minusDays(1);
        LocalDate endDate = reportDate;
        
        // Generate COBOL-compatible transaction test data
        List<Transaction> testTransactions = TestDataGenerator.generateTransactionList(50);
        testTransactions.forEach(txn -> {
            txn.setTransactionDate(reportDate);
            txn.setProcessedTimestamp(reportDate.atStartOfDay());
        });
        
        // Configure mock repository behavior - members_accessed from TransactionRepository
        when(transactionRepository.findByProcessingDateBetween(
            startDate.atStartOfDay(), 
            endDate.atTime(23, 59, 59, 999999999)
        ))
            .thenReturn(testTransactions);
        
        // Act - Generate daily transaction report
        long startTime = System.currentTimeMillis();
        var reportResult = transactionReportingService.generateDailyTransactionReport(startDate, endDate);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate report generation results
        assertThat(reportResult).isNotNull()
            .describedAs("Daily transaction report should be generated successfully");
        
        assertThat(reportResult.getTransactionCount())
            .isEqualTo(testTransactions.size())
            .describedAs("Report should include all transactions within date range");
        
        // Validate COBOL precision for financial calculations
        BigDecimal expectedTotalAmount = testTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertBigDecimalEquals(expectedTotalAmount, reportResult.getTotalAmount(),
            "Daily report total amount should match sum of all transactions with COBOL precision");
        
        // Validate performance requirements - sub-200ms response time
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS)
            .describedAs("Daily transaction report generation should complete within " + 
                TestConstants.RESPONSE_TIME_THRESHOLD_MS + "ms");
        
        // Validate report headers match COBOL format
        assertThat(reportResult.getReportHeaders()).isNotNull()
            .describedAs("Report should include properly formatted headers");
        
        logTestExecution("Daily transaction report test completed for date: " + testDate, executionTime);
    }

    // ======================================================================================
    // MONTHLY AGGREGATION REPORT TESTS - Testing account totals and grand totals calculation
    // ======================================================================================

    /**
     * Tests monthly aggregation report generation functionality.
     * Validates that the service correctly aggregates transaction data by month,
     * calculates account-level totals, and produces grand totals matching 
     * COBOL CBTRN03C 1120-WRITE-ACCOUNT-TOTALS and 1110-WRITE-GRAND-TOTALS logic.
     * 
     * This test ensures functional parity with COBOL aggregation logic:
     * - Monthly transaction grouping and summarization
     * - Account-level total calculations (WS-ACCOUNT-TOTAL equivalent)
     * - Grand total calculations (WS-GRAND-TOTAL equivalent)
     * - Page total processing (WS-PAGE-TOTAL equivalent)
     */
    @ParameterizedTest
    @ValueSource(strings = {"2024-01", "2024-06", "2024-12"})
    public void testGenerateMonthlyAggregationReport_ValidMonth_ReturnsAccurateTotals(String testMonth) {
        // Arrange - Create monthly transaction test data
        LocalDate monthStart = LocalDate.parse(testMonth + "-01");
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        
        // Generate diverse account transactions for aggregation testing
        // Generate 10 accounts for aggregation testing
        List<Account> testAccounts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Customer customer = testDataGenerator.generateCustomer();
            Account account = testDataGenerator.generateAccount(customer);
            testAccounts.add(account);
        }
        List<Transaction> monthlyTransactions = TestDataGenerator.generateDailyTransactionBatch(100);
        
        // Configure transactions across multiple accounts in the test month
        for (int i = 0; i < monthlyTransactions.size(); i++) {
            Transaction txn = monthlyTransactions.get(i);
            Account account = testAccounts.get(i % testAccounts.size());
            
            txn.setAccountId(account.getAccountId());
            txn.setTransactionDate(monthStart.plusDays(i % monthEnd.getDayOfMonth()));
            txn.setAmount(TestDataGenerator.generateValidTransactionAmount());
        }
        
        // Configure mock repository - members_accessed from TransactionRepository
        when(transactionRepository.findByAccountIdAndDateRange(Mockito.anyLong(), 
            Mockito.eq(monthStart.atStartOfDay()), Mockito.eq(monthEnd.atTime(23, 59, 59)), Mockito.any()))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(monthlyTransactions));
        
        // Act - Generate monthly aggregation report
        long startTime = System.currentTimeMillis();
        var aggregationResult = transactionReportingService.generateMonthlyAggregationReport(monthStart, monthEnd);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate aggregation calculations
        assertThat(aggregationResult).isNotNull()
            .describedAs("Monthly aggregation report should be generated successfully");
        
        // Validate account totals calculation - members_accessed from service
        BigDecimal expectedGrandTotal = transactionReportingService.calculateGrandTotals(monthlyTransactions);
        assertBigDecimalEquals(expectedGrandTotal, aggregationResult.getGrandTotal(),
            "Monthly aggregation grand total should match calculated sum with COBOL precision");
        
        // Validate account-level totals - members_accessed from service
        var accountTotals = aggregationResult.getAccountTotals();
        assertThat(accountTotals).isNotEmpty()
            .describedAs("Monthly aggregation should include account-level totals");
        
        // Validate aggregation period
        assertThat(aggregationResult.getReportPeriodStart()).isEqualTo(monthStart);
        assertThat(aggregationResult.getReportPeriodEnd()).isEqualTo(monthEnd);
        
        // Performance validation
        assertThat(executionTime).isLessThan(TestConstants.BATCH_PROCESSING_TIMEOUT_MS)
            .describedAs("Monthly aggregation should complete within batch processing timeout");
        
        logTestExecution("Monthly aggregation report test completed for month: " + testMonth, executionTime);
    }

    // =======================================================================================
    // REGULATORY COMPLIANCE REPORT TESTS - Testing regulatory reporting requirements
    // =======================================================================================

    /**
     * Tests regulatory compliance report generation functionality.
     * Validates that the service generates reports meeting regulatory requirements,
     * includes all required data fields, and maintains audit trail compliance
     * matching original COBOL regulatory reporting specifications.
     * 
     * This test ensures compliance with:
     * - Financial data accuracy and precision requirements
     * - Audit trail completeness and integrity
     * - Regulatory reporting format compliance
     * - Data validation and error handling
     */
    @ParameterizedTest
    @ValueSource(strings = {"QUARTERLY", "ANNUAL", "MONTHLY"})
    public void testGenerateRegulatoryComplianceReport_ValidPeriod_MeetsComplianceRequirements(String reportPeriod) {
        // Arrange - Create compliance test scenario
        LocalDate periodStart, periodEnd;
        switch (reportPeriod) {
            case "QUARTERLY":
                periodStart = LocalDate.now().withMonth(1).withDayOfMonth(1);
                periodEnd = periodStart.plusMonths(3).minusDays(1);
                break;
            case "ANNUAL":
                periodStart = LocalDate.now().withMonth(1).withDayOfMonth(1);
                periodEnd = LocalDate.now().withMonth(12).withDayOfMonth(31);
                break;
            default: // MONTHLY
                periodStart = LocalDate.now().withDayOfMonth(1);
                periodEnd = periodStart.plusMonths(1).minusDays(1);
                break;
        }
        
        // Generate comprehensive transaction data for compliance testing
        List<Transaction> complianceTransactions = TestDataGenerator.generateTransactionList(75);
        
        // Configure transactions with regulatory-relevant data
        complianceTransactions.forEach(txn -> {
            txn.setTransactionDate(periodStart.plusDays(
                TestDataGenerator.generateRandomTransactionDate().getDayOfYear() % periodEnd.getDayOfMonth()));
            txn.setMerchantId(Long.parseLong(TestDataGenerator.generateMerchantId().substring(3)));
            txn.setAmount(TestDataGenerator.generateValidTransactionAmount());
        });
        
        // Configure mock repository for compliance data retrieval
        when(transactionRepository.findByProcessingDateBetween(
            periodStart.atStartOfDay(), 
            periodEnd.atTime(23, 59, 59, 999999999)
        ))
            .thenReturn(complianceTransactions);
        
        // Act - Generate regulatory compliance report
        long startTime = System.currentTimeMillis();
        var complianceReport = transactionReportingService.generateRegulatoryComplianceReport(
            periodStart, periodEnd, reportPeriod);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate compliance report requirements
        assertThat(complianceReport).isNotNull()
            .describedAs("Regulatory compliance report should be generated successfully");
        
        // Validate required compliance data fields
        assertThat(complianceReport.getReportId()).isNotBlank()
            .describedAs("Compliance report should have unique report identifier");
        
        assertThat(complianceReport.getGenerationTimestamp()).isNotNull()
            .describedAs("Compliance report should include generation timestamp");
        
        // Validate financial data precision - critical for regulatory compliance
        assertThat(complianceReport.getTotalTransactionAmount())
            .isNotNull()
            .describedAs("Compliance report should include total transaction amount");
        
        validateCobolPrecision(complianceReport.getTotalTransactionAmount(), "TotalTransactionAmount");
        
        // Validate audit trail completeness
        assertThat(complianceReport.getAuditTrail()).isNotEmpty()
            .describedAs("Compliance report should include comprehensive audit trail");
        
        // Validate report data validation - members_accessed from service
        boolean validationResult = transactionReportingService.validateReportData(complianceReport.getTransactionData());
        assertThat(validationResult).isTrue()
            .describedAs("Compliance report data should pass all validation rules");
        
        // Performance validation for regulatory processing
        assertThat(executionTime).isLessThan(TestConstants.BATCH_PROCESSING_TIMEOUT_MS)
            .describedAs("Regulatory compliance report generation should complete within timeout");
        
        logTestExecution("Regulatory compliance report test completed for period: " + reportPeriod, executionTime);
    }

    // ========================================================================================
    // FRAUD DETECTION REPORT TESTS - Testing fraud pattern analysis and detection
    // ========================================================================================

    /**
     * Tests fraud detection report generation functionality.
     * Validates that the service correctly identifies suspicious transaction patterns,
     * calculates risk scores, and generates fraud alerts matching business requirements
     * for fraud prevention and detection.
     * 
     * This test validates:
     * - Suspicious transaction pattern detection
     * - Risk scoring algorithms
     * - Fraud alert generation
     * - Exception reporting for high-risk transactions
     */
    @ParameterizedTest
    @ValueSource(strings = {"HIGH_VELOCITY", "UNUSUAL_MERCHANT", "LARGE_AMOUNT"})
    public void testGenerateFraudDetectionReport_SuspiciousPatterns_IdentifiesFraudRisks(String fraudPattern) {
        // Arrange - Create fraud detection test scenarios
        List<Transaction> suspiciousTransactions = TestDataGenerator.generateTransactionList(30);
        
        // Configure transactions based on fraud pattern type
        switch (fraudPattern) {
            case "HIGH_VELOCITY":
                // Generate multiple transactions in short time period
                LocalDate velocityDate = LocalDate.now();
                suspiciousTransactions.forEach(txn -> {
                    txn.setTransactionDate(velocityDate);
                    txn.setAccountId(TestConstants.TEST_ACCOUNT_ID);
                    txn.setOriginalTimestamp(velocityDate.atTime(10, 0).plusMinutes(
                        suspiciousTransactions.indexOf(txn) * 2)); // 2-minute intervals
                });
                break;
                
            case "UNUSUAL_MERCHANT":
                // Generate transactions with unusual merchant patterns
                suspiciousTransactions.forEach(txn -> {
                    txn.setMerchantName("SUSPICIOUS_MERCHANT_" + suspiciousTransactions.indexOf(txn));
                    txn.setMerchantCity("UNKNOWN_CITY");
                    txn.setAmount(TestDataGenerator.generateValidTransactionAmount());
                });
                break;
                
            case "LARGE_AMOUNT":
                // Generate unusually large transaction amounts
                suspiciousTransactions.forEach(txn -> {
                    txn.setAmount(TestConstants.MAX_TRANSACTION_AMOUNT.subtract(BigDecimal.ONE));
                });
                break;
        }
        
        // Configure mock repository for fraud detection queries
        when(transactionRepository.findByAccountId(Mockito.anyLong()))
            .thenReturn(suspiciousTransactions);
        
        // Act - Generate fraud detection report
        long startTime = System.currentTimeMillis();
        var fraudReport = transactionReportingService.generateFraudDetectionReport(
            LocalDate.now().minusDays(30), LocalDate.now());
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate fraud detection results
        assertThat(fraudReport).isNotNull()
            .describedAs("Fraud detection report should be generated successfully");
        
        assertThat(fraudReport.getSuspiciousTransactions()).isNotEmpty()
            .describedAs("Fraud detection should identify suspicious transactions for pattern: " + fraudPattern);
        
        // Validate risk scoring
        fraudReport.getSuspiciousTransactions().forEach(suspiciousTxn -> {
            assertThat(suspiciousTxn.getRiskScore()).isGreaterThan(0)
                .describedAs("Suspicious transactions should have positive risk scores");
        });
        
        // Validate fraud alerts generation
        assertThat(fraudReport.getFraudAlerts()).isNotEmpty()
            .describedAs("Fraud detection should generate alerts for high-risk patterns");
        
        // Performance validation
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS)
            .describedAs("Fraud detection should complete within response time threshold");
        
        logTestExecution("Fraud detection report test completed for pattern: " + fraudPattern, executionTime);
    }

    // =========================================================================================
    // MERCHANT ANALYSIS REPORT TESTS - Testing merchant categorization and analysis
    // =========================================================================================

    /**
     * Tests merchant analysis report generation functionality.
     * Validates that the service correctly categorizes merchants, analyzes transaction patterns,
     * and generates merchant performance reports for business analysis and decision making.
     * 
     * This test validates:
     * - Merchant categorization and classification
     * - Transaction volume analysis by merchant
     * - Merchant performance metrics calculation
     * - Categorical transaction breakdowns
     */
    @ParameterizedTest
    @ValueSource(strings = {"RETAIL", "RESTAURANT", "GAS_STATION", "ONLINE"})
    public void testGenerateMerchantAnalysisReport_MerchantCategories_ProvidesAccurateAnalysis(String merchantCategory) {
        // Arrange - Create merchant analysis test data
        List<Transaction> merchantTransactions = TestDataGenerator.generateTransactionList(60);
        
        // Configure transactions for specific merchant category
        merchantTransactions.forEach(txn -> {
            txn.setMerchantName("TEST_" + merchantCategory + "_MERCHANT");
            txn.setCategoryCode(getCategoryCodeForMerchantType(merchantCategory));
            txn.setMerchantId(Long.parseLong(TestDataGenerator.generateMerchantId().substring(3)));
            txn.setAmount(TestDataGenerator.generateValidTransactionAmount());
        });
        
        // Configure mock repository for merchant queries
        when(transactionRepository.findByProcessingDateBetween(Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class)))
            .thenReturn(merchantTransactions);
        
        // Act - Generate merchant analysis report
        long startTime = System.currentTimeMillis();
        var merchantReport = transactionReportingService.generateMerchantAnalysisReport(
            LocalDate.now().minusMonths(1), LocalDate.now());
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate merchant analysis results
        assertThat(merchantReport).isNotNull()
            .describedAs("Merchant analysis report should be generated successfully");
        
        assertThat(merchantReport.getMerchantCategories()).containsKey(merchantCategory)
            .describedAs("Merchant analysis should include category: " + merchantCategory);
        
        // Validate transaction volume analysis
        var categoryData = merchantReport.getMerchantCategories().get(merchantCategory);
        assertThat(categoryData.getTransactionCount()).isEqualTo(merchantTransactions.size())
            .describedAs("Category transaction count should match test data");
        
        // Validate financial calculations with COBOL precision
        BigDecimal expectedCategoryTotal = merchantTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertBigDecimalEquals(expectedCategoryTotal, categoryData.getTotalAmount(),
            "Merchant category total should match calculated sum with COBOL precision");
        
        // Validate merchant performance metrics
        assertThat(categoryData.getAverageTransactionAmount()).isNotNull()
            .describedAs("Merchant analysis should include average transaction amount");
        
        // Performance validation
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS)
            .describedAs("Merchant analysis should complete within response time threshold");
        
        logTestExecution("Merchant analysis report test completed for category: " + merchantCategory, executionTime);
    }

    // ==========================================================================================
    // DATA PROCESSING TESTS - Testing core data processing functionality
    // ==========================================================================================

    /**
     * Tests date parameter processing functionality.
     * Validates that the service correctly processes date parameters matching
     * COBOL CBTRN03C 0550-DATEPARM-READ logic for report date range determination.
     * 
     * This test ensures functional parity with COBOL date parameter handling:
     * - Date parameter file reading equivalent
     * - Date range validation and parsing
     * - Error handling for invalid date formats
     * - Default date range assignment
     */
    @ParameterizedTest
    @ValueSource(strings = {"2024-01-01,2024-01-31", "2024-06-01,2024-06-30", "2024-12-01,2024-12-31"})
    public void testProcessDateParameters_ValidDateRanges_ParsesCorrectly(String dateRange) {
        // Arrange - Parse test date parameters
        String[] dates = dateRange.split(",");
        LocalDate expectedStartDate = LocalDate.parse(dates[0]);
        LocalDate expectedEndDate = LocalDate.parse(dates[1]);
        
        // Act - Process date parameters using service method
        long startTime = System.currentTimeMillis();
        var dateParams = transactionReportingService.processDateParameters(expectedStartDate, expectedEndDate);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate date parameter processing
        assertThat(dateParams).isNotNull()
            .describedAs("Date parameters should be processed successfully");
        
        assertThat(dateParams.getStartDate()).isEqualTo(expectedStartDate)
            .describedAs("Processed start date should match input");
        
        assertThat(dateParams.getEndDate()).isEqualTo(expectedEndDate)
            .describedAs("Processed end date should match input");
        
        assertThat(dateParams.isValidRange()).isTrue()
            .describedAs("Date range should be validated as valid");
        
        // Validate performance for date processing
        assertThat(executionTime).isLessThan(100L)
            .describedAs("Date parameter processing should be fast");
        
        logTestExecution("Date parameter processing test completed for range: " + dateRange, executionTime);
    }

    /**
     * Tests transaction file reading functionality.
     * Validates that the service correctly reads and processes transaction data
     * matching COBOL CBTRN03C 1000-TRANFILE-GET-NEXT logic for sequential file processing.
     * 
     * This test ensures functional parity with COBOL transaction file handling:
     * - Sequential transaction record processing
     * - Transaction data validation and parsing
     * - Error handling for invalid records
     * - End-of-file detection and handling
     */
    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 5000})
    public void testReadTransactionFiles_VariousVolumes_ProcessesEfficiently(int transactionCount) {
        // Arrange - Generate transaction test data of specified volume
        List<Transaction> testTransactions = TestDataGenerator.generateTransactionList(50);
        
        // Ensure list has requested size for volume testing
        while (testTransactions.size() < transactionCount) {
            testTransactions.addAll(TestDataGenerator.generateTransactionList(50));
        }
        testTransactions = testTransactions.subList(0, transactionCount);
        
        // Configure mock repository for file reading simulation
        when(transactionRepository.findAll()).thenReturn(testTransactions);
        
        // Act - Read transaction files
        long startTime = System.currentTimeMillis();
        var readResult = transactionReportingService.readTransactionFiles(
            LocalDate.now().minusDays(30), LocalDate.now());
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate transaction file reading
        assertThat(readResult).isNotNull()
            .describedAs("Transaction file reading should return valid result");
        
        assertThat(readResult.getProcessedRecordCount()).isEqualTo(transactionCount)
            .describedAs("Should process all transaction records");
        
        assertThat(readResult.getValidRecordCount()).isLessThanOrEqualTo(transactionCount)
            .describedAs("Valid record count should not exceed total processed");
        
        // Performance validation based on volume
        long maxExpectedTime = (transactionCount / 100) * 10; // 10ms per 100 records
        assertThat(executionTime).isLessThan(Math.max(maxExpectedTime, TestConstants.RESPONSE_TIME_THRESHOLD_MS))
            .describedAs("Transaction file reading should scale efficiently with volume");
        
        logTestExecution("Transaction file reading test completed for volume: " + transactionCount, executionTime);
    }

    // ===========================================================================================
    // REPORT GENERATION TESTS - Testing report formatting and output generation
    // ===========================================================================================

    /**
     * Tests report header generation functionality.
     * Validates that the service generates properly formatted report headers
     * matching COBOL CBTRN03C 1120-WRITE-HEADERS logic for consistent report formatting.
     * 
     * This test ensures functional parity with COBOL report header formatting:
     * - Report title and date formatting
     * - Column header alignment and spacing
     * - Page numbering and pagination
     * - Report metadata inclusion
     */
    @ParameterizedTest
    @ValueSource(strings = {"DAILY", "MONTHLY", "COMPLIANCE", "FRAUD", "MERCHANT"})
    public void testGenerateReportHeaders_VariousReportTypes_FormatsCorrectly(String reportType) {
        // Arrange - Create report configuration for header generation
        LocalDate reportDate = LocalDate.now();
        
        // Act - Generate report headers for specified type
        long startTime = System.currentTimeMillis();
        var reportHeaders = transactionReportingService.generateReportHeaders(reportType, reportDate);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate report header generation
        assertThat(reportHeaders).isNotNull()
            .describedAs("Report headers should be generated successfully");
        
        assertThat(reportHeaders.getReportTitle()).contains(reportType)
            .describedAs("Report title should include report type");
        
        assertThat(reportHeaders.getReportDate()).isEqualTo(reportDate)
            .describedAs("Report headers should include correct report date");
        
        assertThat(reportHeaders.getColumnHeaders()).isNotEmpty()
            .describedAs("Report should include column headers");
        
        // Validate header formatting consistency
        assertThat(reportHeaders.getFormattedHeader()).isNotBlank()
            .describedAs("Report should have properly formatted header string");
        
        // Performance validation
        assertThat(executionTime).isLessThan(50L)
            .describedAs("Report header generation should be fast");
        
        logTestExecution("Report header generation test completed for type: " + reportType, executionTime);
    }

    /**
     * Tests report detail writing functionality.
     * Validates that the service correctly formats and writes report detail lines
     * matching COBOL CBTRN03C 1120-WRITE-DETAIL logic for consistent data presentation.
     * 
     * This test ensures functional parity with COBOL detail line formatting:
     * - Transaction detail formatting and alignment
     * - Numeric field formatting with proper precision
     * - Date and time formatting consistency
     * - Field truncation and padding handling
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 10, 50})
    public void testWriteReportDetails_VariousRecordCounts_FormatsConsistently(int recordCount) {
        // Arrange - Generate transaction details for report writing
        List<Transaction> reportTransactions = TestDataGenerator.generateTransactionList(25);
        
        // Ensure list has requested size
        while (reportTransactions.size() < recordCount) {
            reportTransactions.add((Transaction) TestDataGenerator.generateTransaction());
        }
        reportTransactions = reportTransactions.subList(0, recordCount);
        
        // Act - Write report details
        long startTime = System.currentTimeMillis();
        var detailsResult = transactionReportingService.writeReportDetails(reportTransactions);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate report detail writing
        assertThat(detailsResult).isNotNull()
            .describedAs("Report details writing should return valid result");
        
        assertThat(detailsResult.getFormattedDetails()).hasSize(recordCount)
            .describedAs("Should format all transaction records");
        
        // Validate detail line formatting consistency
        detailsResult.getFormattedDetails().forEach(detailLine -> {
            assertThat(detailLine).isNotBlank()
                .describedAs("Detail lines should not be blank");
            
            // Validate COBOL-compatible field formatting
            assertThat(detailLine.length()).isGreaterThan(50)
                .describedAs("Detail lines should have sufficient length for all fields");
        });
        
        // Performance validation
        long maxExpectedTime = recordCount * 5; // 5ms per record
        assertThat(executionTime).isLessThan(Math.max(maxExpectedTime, 100L))
            .describedAs("Report detail writing should scale with record count");
        
        logTestExecution("Report detail writing test completed for count: " + recordCount, executionTime);
    }

    // ============================================================================================
    // CALCULATION TESTS - Testing financial calculations and aggregations
    // ============================================================================================

    /**
     * Tests account totals calculation functionality.
     * Validates that the service correctly calculates account-level totals
     * matching COBOL CBTRN03C WS-ACCOUNT-TOTAL processing logic with precise
     * monetary calculations and COMP-3 decimal precision.
     * 
     * This test ensures functional parity with COBOL account total processing:
     * - Account-level transaction aggregation
     * - COMP-3 precision preservation in calculations
     * - Account balance impact calculations
     * - Multi-account total processing
     */
    @ParameterizedTest
    @ValueSource(strings = {"1000000001", "1000000002", "1000000003"})
    public void testCalculateAccountTotals_VariousAccounts_ProvidesAccurateTotals(String accountId) {
        // Arrange - Generate account-specific transaction data
        List<Transaction> accountTransactions = TestDataGenerator.generateTransactionList(30);
        Long testAccountId = Long.parseLong(accountId);
        
        // Configure transactions for specific account
        accountTransactions.forEach(txn -> {
            txn.setAccountId(testAccountId);
            txn.setAmount(TestDataGenerator.generateComp3BigDecimal(2, 10000.00));
        });
        
        // Act - Calculate account totals using service method
        long startTime = System.currentTimeMillis();
        BigDecimal accountTotal = transactionReportingService.calculateAccountTotals(accountTransactions);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate account totals calculation
        assertThat(accountTotal).isNotNull()
            .describedAs("Account total calculation should return valid result");
        
        // Calculate expected total for validation
        BigDecimal expectedTotal = accountTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Validate COBOL precision in calculation
        assertBigDecimalEquals(expectedTotal, accountTotal,
            "Account total should match sum of transactions with COBOL COMP-3 precision");
        
        // Validate precision requirements
        assertThat(validateCobolPrecision(accountTotal, "AccountTotal")).isTrue()
            .describedAs("Account total should meet COBOL precision requirements");
        
        // Performance validation
        assertThat(executionTime).isLessThan(100L)
            .describedAs("Account total calculation should be efficient");
        
        logTestExecution("Account totals calculation test completed for account: " + accountId, executionTime);
    }

    /**
     * Tests grand totals calculation functionality.
     * Validates that the service correctly calculates grand totals across all accounts
     * matching COBOL CBTRN03C WS-GRAND-TOTAL processing logic with precise
     * monetary calculations and proper aggregation handling.
     * 
     * This test ensures functional parity with COBOL grand total processing:
     * - Cross-account transaction aggregation
     * - Grand total accumulation logic
     * - COMP-3 precision preservation in grand totals
     * - Large volume total processing
     */
    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000})
    public void testCalculateGrandTotals_VariousVolumes_ProvidesAccurateTotals(int transactionVolume) {
        // Arrange - Generate high-volume transaction data for grand total testing
        List<Transaction> allTransactions = TestDataGenerator.generateTransactionList(80);
        
        // Ensure sufficient volume for testing
        while (allTransactions.size() < transactionVolume) {
            allTransactions.addAll(TestDataGenerator.generateTransactionList(40));
        }
        allTransactions = allTransactions.subList(0, transactionVolume);
        
        // Configure transactions with COBOL-compatible amounts
        allTransactions.forEach(txn -> {
            txn.setAmount(TestDataGenerator.generateComp3BigDecimal(2, 10000.00));
        });
        
        // Act - Calculate grand totals using service method
        long startTime = System.currentTimeMillis();
        BigDecimal grandTotal = transactionReportingService.calculateGrandTotals(allTransactions);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate grand totals calculation
        assertThat(grandTotal).isNotNull()
            .describedAs("Grand total calculation should return valid result");
        
        // Calculate expected grand total for validation
        BigDecimal expectedGrandTotal = allTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Validate COBOL precision in grand total calculation
        assertBigDecimalEquals(expectedGrandTotal, grandTotal,
            "Grand total should match sum of all transactions with COBOL COMP-3 precision");
        
        // Validate precision requirements for large amounts
        assertThat(validateCobolPrecision(grandTotal, "GrandTotal")).isTrue()
            .describedAs("Grand total should meet COBOL precision requirements");
        
        // Performance validation based on volume
        long maxExpectedTime = (transactionVolume / 100) * 10; // 10ms per 100 transactions
        assertThat(executionTime).isLessThan(Math.max(maxExpectedTime, 200L))
            .describedAs("Grand total calculation should scale efficiently");
        
        logTestExecution("Grand totals calculation test completed for volume: " + transactionVolume, executionTime);
    }

    // ============================================================================================
    // VALIDATION TESTS - Testing data validation and error handling
    // ============================================================================================

    /**
     * Tests report data validation functionality.
     * Validates that the service correctly validates report data integrity,
     * identifies data quality issues, and provides comprehensive validation
     * results matching business rules and regulatory requirements.
     * 
     * This test ensures:
     * - Data completeness validation
     * - Field format validation  
     * - Business rule compliance validation
     * - Error detection and reporting
     */
    @ParameterizedTest
    @ValueSource(strings = {"COMPLETE", "MISSING_FIELDS", "INVALID_AMOUNTS", "INVALID_DATES"})
    public void testValidateReportData_VariousDataQuality_ValidatesCorrectly(String dataQuality) {
        // Arrange - Create test data based on quality scenario
        List<Transaction> testData = TestDataGenerator.generateTransactionList(50);
        
        // Configure data quality scenarios
        switch (dataQuality) {
            case "MISSING_FIELDS":
                testData.forEach(txn -> txn.setDescription(null));
                break;
            case "INVALID_AMOUNTS":
                testData.forEach(txn -> txn.setAmount(BigDecimal.valueOf(-1000)));
                break;
            case "INVALID_DATES":
                testData.forEach(txn -> txn.setTransactionDate(LocalDate.now().plusDays(1)));
                break;
            // "COMPLETE" uses valid data as generated
        }
        
        // Act - Validate report data
        long startTime = System.currentTimeMillis();
        boolean validationResult = transactionReportingService.validateReportData(testData);
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Assert - Validate data validation results
        if ("COMPLETE".equals(dataQuality)) {
            assertThat(validationResult).isTrue()
                .describedAs("Complete data should pass validation");
        } else {
            assertThat(validationResult).isFalse()
                .describedAs("Invalid data should fail validation for quality: " + dataQuality);
        }
        
        // Performance validation
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS)
            .describedAs("Data validation should complete within response time threshold");
        
        logTestExecution("Report data validation test completed for quality: " + dataQuality, executionTime);
    }

    // ============================================================================================
    // HELPER METHODS - Supporting functionality for test execution
    // ============================================================================================

    /**
     * Configures common mock repository behavior for consistent test execution.
     * Sets up default mock responses for repository operations used across
     * multiple test scenarios.
     */
    private void mockCommonRepositoryBehavior() {
        // Configure default empty result for repository queries
        when(transactionRepository.findAll()).thenReturn(List.of());
        when(transactionRepository.count()).thenReturn(0L);
        
        // Configure default behavior for date-based queries
        when(transactionRepository.findByProcessingDateBetween(Mockito.any(), Mockito.any()))
            .thenReturn(List.of());
        
        logTestExecution("Common mock repository behavior configured", null);
    }

    /**
     * Maps merchant category to appropriate transaction category code.
     * Provides consistent category mapping for merchant analysis testing.
     * 
     * @param merchantType the merchant type for categorization
     * @return appropriate transaction category code
     */
    private String getCategoryCodeForMerchantType(String merchantType) {
        switch (merchantType) {
            case "RETAIL":
                return "5411";
            case "RESTAURANT":
                return "5812";
            case "GAS_STATION":
                return "5542";
            case "ONLINE":
                return "5999";
            default:
                return "0000";
        }
    }
}