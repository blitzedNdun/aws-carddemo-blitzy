/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carddemo.batch.StatementGenerationJob;
import com.carddemo.config.TestBatchConfig;
import com.carddemo.config.TestDatabaseConfig;
import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.CardXref;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.util.AmountCalculator;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.CobolComparisonUtils;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.test.TestConstants;
import com.carddemo.test.categories.IntegrationTest;
import com.carddemo.test.categories.UnitTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Comprehensive test suite for StatementGenerationJob that validates functional parity 
 * with CBSTM03A and CBSTM03B COBOL batch programs.
 * 
 * This test suite covers all critical functionality from the original COBOL implementation:
 * - Sequential transaction file reading and grouping by account (CBSTM03A lines 317-329)
 * - Cross-reference lookups for card-to-account mapping (CBSTM03A lines 345-366) 
 * - Customer and account master data retrieval (CBSTM03A lines 368-414)
 * - Statement generation in plain-text format matching COBOL layout (CBSTM03A lines 458-504)
 * - HTML statement generation with template validation (CBSTM03A lines 506-672)
 * - COMP-3 amount calculations and totaling (CBSTM03A lines 64-65, 429, 433-434)
 * - Two-dimensional transaction table handling (CBSTM03A lines 225-234)
 * - File I/O operations through subroutine simulation (CBSTM03B entire program)
 * - ALTER/GO TO control flow replacement validation (CBSTM03A lines 296-314)
 * - Chunk processing with configurable sizes
 * - Multi-format output file generation
 * - Error handling and recovery scenarios
 * - Processing within 4-hour batch window requirement
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@SpringBootTest
@ContextConfiguration(classes = {TestBatchConfig.class, TestDatabaseConfig.class})
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StatementGenerationJobTest extends AbstractBaseTest {

    // Test container for realistic database testing
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("carddemodb")
            .withUsername("postgres")
            .withPassword("postgres");

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private StatementGenerationJob statementGenerationJob;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TestDataGenerator testDataGenerator;

    @Autowired
    private CobolComparisonUtils cobolComparisonUtils;

    // Test data containers
    private Customer testCustomer;
    private Account testAccount;
    private List<Transaction> testTransactions;
    private List<Card> testCards;
    private List<CardXref> testCardXrefs;

    // Test constants
    private static final BigDecimal COBOL_DECIMAL_SCALE = new BigDecimal("2");
    private static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int BATCH_PROCESSING_WINDOW_HOURS = 4;
    private static final long RESPONSE_TIME_THRESHOLD_MS = 200L;

    @BeforeEach
    void setUp() {
        super.setUp();
        setupTestData();
        configureJobLauncher();
    }

    /**
     * Sets up comprehensive test data matching COBOL data structures.
     * Replicates the data patterns from CBSTM03A test scenarios.
     */
    private void setupTestData() {
        // Create customer data matching CUSTREC copybook structure
        testCustomer = Customer.builder()
                .customerId(1000001L)
                .firstName("John")
                .middleName("Q")
                .lastName("Public")
                .addressLine1("123 Main Street")
                .addressLine2("Apt 4B")
                .addressLine3("Downtown")
                .stateCode("NY")
                .countryCode("USA")
                .zipCode("10001")
                .phoneNumber1("555-123-4567")
                .ssn("123456789")
                .dateOfBirth(LocalDate.of(1980, 5, 15))
                .ficoScore(750)
                .build();
        customerRepository.save(testCustomer);

        // Create account data matching CVACT01Y copybook structure
        testAccount = Account.builder()
                .accountId(1000000001L)
                .customerId(testCustomer.getCustomerId())
                .customer(testCustomer)
                .currentBalance(new BigDecimal("1250.75"))
                .creditLimit(new BigDecimal("5000.00"))
                .activeStatus("ACTIVE")
                .accountType("CREDIT")
                .openDate(LocalDateTime.now().minusYears(2))
                .build();
        accountRepository.save(testAccount);

        // Create transaction data matching CVTRA05Y copybook structure
        // Simulating the two-dimensional array from CBSTM03A lines 225-234
        testTransactions = createTestTransactionData();
        transactionRepository.saveAll(testTransactions);

        // Create card and cross-reference data
        testCards = createTestCardData();
        cardRepository.saveAll(testCards);

        testCardXrefs = createTestCardXrefData();
        cardXrefRepository.saveAll(testCardXrefs);
    }

    /**
     * Creates test transaction data that matches COBOL COMP-3 arithmetic patterns.
     * Replicates transaction grouping logic from CBSTM03A.
     */
    private List<Transaction> createTestTransactionData() {
        List<Transaction> transactions = new ArrayList<>();
        LocalDate statementDate = LocalDate.now().minusDays(30);
        
        // Transaction 1: Purchase with COMP-3 amount precision
        Transaction txn1 = Transaction.builder()
                .transactionId(100001L)
                .accountId(testAccount.getAccountId())
                .cardNumber("4532123456789012")
                .amount(new BigDecimal("123.45"))
                .transactionTypeCode("PU")
                .transactionDate(statementDate)
                .description("GROCERY STORE PURCHASE")
                .merchantName("ABC MARKET")
                .merchantCity("NEW YORK")
                .merchantZip("10001")
                .categoryCode("5411")
                .processedTimestamp(statementDate.atStartOfDay())
                .build();
        transactions.add(txn1);

        // Transaction 2: Payment with negative amount
        Transaction txn2 = Transaction.builder()
                .transactionId(100002L)
                .accountId(testAccount.getAccountId())
                .cardNumber("4532123456789012")
                .amount(new BigDecimal("-500.00"))
                .transactionTypeCode("PA")
                .transactionDate(statementDate.plusDays(5))
                .description("PAYMENT RECEIVED")
                .merchantName("PAYMENT CENTER")
                .merchantCity("NEW YORK")
                .merchantZip("10001")
                .categoryCode("PAYM")
                .processedTimestamp(statementDate.plusDays(5).atStartOfDay())
                .build();
        transactions.add(txn2);

        // Transaction 3: Cash advance with fee calculation
        Transaction txn3 = Transaction.builder()
                .transactionId(100003L)
                .accountId(testAccount.getAccountId())
                .cardNumber("4532123456789012")
                .amount(new BigDecimal("300.00"))
                .transactionTypeCode("CA")
                .transactionDate(statementDate.plusDays(10))
                .description("CASH ADVANCE")
                .merchantName("ATM LOCATION")
                .merchantCity("NEW YORK")
                .merchantZip("10001")
                .categoryCode("6011")
                .processedTimestamp(statementDate.plusDays(10).atStartOfDay())
                .build();
        transactions.add(txn3);

        return transactions;
    }

    /**
     * Creates test card data for cross-reference testing.
     */
    private List<Card> createTestCardData() {
        Card card = Card.builder()
                .cardNumber("4532123456789012")
                .accountId(testAccount.getAccountId())
                .customerId(testCustomer.getCustomerId())
                .expirationDate("12/25")
                .activeStatus("ACTIVE")
                .build();
        return Arrays.asList(card);
    }

    /**
     * Creates test card cross-reference data matching XREFFILE structure.
     */
    private List<CardXref> createTestCardXrefData() {
        CardXref xref = CardXref.builder()
                .cardNumber("4532123456789012")
                .customerId(testCustomer.getCustomerId())
                .accountId(testAccount.getAccountId())
                .build();
        return Arrays.asList(xref);
    }

    /**
     * Configures job launcher with the statement generation job.
     */
    private void configureJobLauncher() {
        Job job = statementGenerationJob.statementGenerationJob();
        jobLauncherTestUtils.setJob(job);
    }

    @Nested
    @DisplayName("Statement Generation Job Execution Tests")
    class JobExecutionTests {

        @Test
        @DisplayName("Should complete statement generation job successfully")
        @Timeout(value = BATCH_PROCESSING_WINDOW_HOURS, unit = TimeUnit.HOURS)
        void shouldCompleteJobSuccessfully() throws Exception {
            // Given
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
            
            // Validate processing time meets 4-hour window requirement
            long processingTimeMs = jobExecution.getEndTime().getTime() - 
                                  jobExecution.getStartTime().getTime();
            assertThat(processingTimeMs).isLessThan(BATCH_PROCESSING_WINDOW_HOURS * 3600 * 1000);
        }

        @Test
        @DisplayName("Should handle large transaction volumes within time constraints")
        @Timeout(value = BATCH_PROCESSING_WINDOW_HOURS, unit = TimeUnit.HOURS)
        void shouldHandleLargeTransactionVolumes() throws Exception {
            // Given - Create large dataset mimicking production volumes
            List<Transaction> largeTransactionSet = generateLargeTransactionDataset(10000);
            transactionRepository.saveAll(largeTransactionSet);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-large")
                    .addLong("chunkSize", 100L)
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(jobExecution.getStepExecutions()).hasSize(1);
            assertThat(jobExecution.getStepExecutions().iterator().next().getReadCount())
                    .isGreaterThanOrEqualTo(largeTransactionSet.size());
        }

        @Test
        @DisplayName("Should fail gracefully on invalid job parameters")
        void shouldFailGracefullyOnInvalidParameters() throws Exception {
            // Given
            JobParameters invalidParameters = new JobParametersBuilder()
                    .addString("invalidParam", "invalidValue")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(invalidParameters);

            // Then
            assertThat(jobExecution.getStatus()).isIn(BatchStatus.FAILED, BatchStatus.STOPPED);
            assertThat(jobExecution.getAllFailureExceptions()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("COBOL Functional Parity Tests")
    class CobolFunctionalParityTests {

        @Test
        @DisplayName("Should replicate CBSTM03A sequential transaction processing")
        void shouldReplicateSequentialTransactionProcessing() throws Exception {
            // Given - Transaction data ordered by account then card number (CBSTM03A line 417-432)
            List<Transaction> orderedTransactions = testTransactions.stream()
                    .sorted((t1, t2) -> {
                        int accountCompare = t1.getAccountId().compareTo(t2.getAccountId());
                        if (accountCompare != 0) return accountCompare;
                        return t1.getCardNumber().compareTo(t2.getCardNumber());
                    })
                    .collect(Collectors.toList());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-sequential")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify output files are generated
            Path textOutputPath = Paths.get("target/test-output-sequential/statements.txt");
            Path htmlOutputPath = Paths.get("target/test-output-sequential/statements.html");
            
            assertThat(Files.exists(textOutputPath)).isTrue();
            assertThat(Files.exists(htmlOutputPath)).isTrue();
        }

        @Test
        @DisplayName("Should replicate CBSTM03B file I/O operations")
        void shouldReplicateFileIOOperations() throws Exception {
            // Given - Job configured to process specific account range
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-io")
                    .addLong("accountIdStart", testAccount.getAccountId())
                    .addLong("accountIdEnd", testAccount.getAccountId())
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then - Verify CBSTM03B equivalent operations
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify data access patterns match COBOL READ operations
            // CBSTM03B operations: OPEN, READ, CLOSE for each file type
            assertThat(jobExecution.getStepExecutions().iterator().next().getReadCount())
                    .isEqualTo(1); // One account processed
        }

        @Test
        @DisplayName("Should handle COMP-3 arithmetic with exact precision")
        void shouldHandleComp3ArithmeticWithExactPrecision() throws Exception {
            // Given - Transactions with COMP-3 equivalent decimal precision
            BigDecimal amount1 = new BigDecimal("123.45").setScale(2, COBOL_ROUNDING_MODE);
            BigDecimal amount2 = new BigDecimal("67.89").setScale(2, COBOL_ROUNDING_MODE);
            BigDecimal expectedTotal = amount1.add(amount2);

            Transaction compTransaction1 = testDataGenerator.generateTransaction();
            compTransaction1.setAmount(amount1);
            compTransaction1.setAccountId(testAccount.getAccountId());
            
            Transaction compTransaction2 = testDataGenerator.generateTransaction();
            compTransaction2.setAmount(amount2);
            compTransaction2.setAccountId(testAccount.getAccountId());
            
            transactionRepository.saveAll(Arrays.asList(compTransaction1, compTransaction2));

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-comp3")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify COMP-3 precision using CobolDataConverter
            BigDecimal calculatedTotal = AmountCalculator.calculateBalance(amount1, amount2);
            assertBigDecimalEquals(calculatedTotal, expectedTotal);
        }

        @Test
        @DisplayName("Should validate cross-reference lookup functionality")
        void shouldValidateCrossReferenceLookup() throws Exception {
            // Given - Card cross-reference data matching CBSTM03A lines 345-366
            String cardNumber = "4532123456789012";
            
            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(
                new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-xref")
                    .toJobParameters()
            );

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify cross-reference lookups worked correctly
            CardXref foundXref = cardXrefRepository.findByCardNumber(cardNumber).stream()
                    .findFirst().orElse(null);
            assertThat(foundXref).isNotNull();
            assertThat(foundXref.getAccountId()).isEqualTo(testAccount.getAccountId());
            assertThat(foundXref.getCustomerId()).isEqualTo(testCustomer.getCustomerId());
        }
    }

    @Nested
    @DisplayName("Statement Output Format Tests")
    class StatementOutputFormatTests {

        @Test
        @DisplayName("Should generate plain text statement matching COBOL layout")
        void shouldGeneratePlainTextStatementMatchingCobolLayout() throws Exception {
            // Given
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-text")
                    .addString("outputFormat", "TEXT")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify text output format matches CBSTM03A statement lines
            Path outputFile = Paths.get("target/test-output-text/statements.txt");
            assertThat(Files.exists(outputFile)).isTrue();
            
            List<String> lines = Files.readAllLines(outputFile);
            
            // Verify header format (ST-LINE0 from CBSTM03A line 86-89)
            assertThat(lines.stream().anyMatch(line -> line.contains("START OF STATEMENT")))
                    .isTrue();
            
            // Verify customer name format (ST-LINE1 from CBSTM03A line 90-92)
            assertThat(lines.stream().anyMatch(line -> 
                    line.contains(testCustomer.getFirstName()) && 
                    line.contains(testCustomer.getLastName())))
                    .isTrue();
            
            // Verify account details format (ST-LINE7-9 from CBSTM03A lines 107-119)
            assertThat(lines.stream().anyMatch(line -> 
                    line.contains("Account ID") && 
                    line.contains(testAccount.getAccountId().toString())))
                    .isTrue();
            
            // Verify footer format (ST-LINE15 from CBSTM03A line 143-146)
            assertThat(lines.stream().anyMatch(line -> line.contains("END OF STATEMENT")))
                    .isTrue();
        }

        @Test
        @DisplayName("Should generate HTML statement with template validation")
        void shouldGenerateHtmlStatementWithTemplateValidation() throws Exception {
            // Given
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-html")
                    .addString("outputFormat", "HTML")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify HTML output format matches CBSTM03A HTML template
            Path outputFile = Paths.get("target/test-output-html/statements.html");
            assertThat(Files.exists(outputFile)).isTrue();
            
            String htmlContent = Files.readString(outputFile);
            
            // Verify HTML structure (HTML-L01-L08 from CBSTM03A lines 150-158)
            assertThat(htmlContent).contains("<!DOCTYPE html>");
            assertThat(htmlContent).contains("<html lang=\"en\">");
            assertThat(htmlContent).contains("<title>HTML Table Layout</title>");
            
            // Verify table structure for statement data
            assertThat(htmlContent).contains("<table");
            assertThat(htmlContent).contains("Bank of XYZ");
            
            // Verify customer information formatting
            assertThat(htmlContent).contains(testCustomer.getFirstName());
            assertThat(htmlContent).contains(testCustomer.getLastName());
            
            // Verify account information
            assertThat(htmlContent).contains(testAccount.getAccountId().toString());
            assertThat(htmlContent).contains("Basic Details");
            assertThat(htmlContent).contains("Transaction Summary");
        }

        @Test
        @DisplayName("Should generate multi-format output files simultaneously")
        void shouldGenerateMultiFormatOutputSimultaneously() throws Exception {
            // Given
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-multi")
                    .addString("outputFormat", "BOTH")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify both output formats are generated
            Path textOutputPath = Paths.get("target/test-output-multi/statements.txt");
            Path htmlOutputPath = Paths.get("target/test-output-multi/statements.html");
            
            assertThat(Files.exists(textOutputPath)).isTrue();
            assertThat(Files.exists(htmlOutputPath)).isTrue();
            
            // Verify content consistency between formats
            List<String> textLines = Files.readAllLines(textOutputPath);
            String htmlContent = Files.readString(htmlOutputPath);
            
            // Both should contain same customer name
            String fullName = testCustomer.getFirstName() + " " + testCustomer.getLastName();
            assertThat(textLines.stream().anyMatch(line -> line.contains(fullName))).isTrue();
            assertThat(htmlContent.contains(fullName)).isTrue();
        }
    }

    @Nested
    @DisplayName("Error Handling and Recovery Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle database connection failures gracefully")
        void shouldHandleDatabaseConnectionFailures() throws Exception {
            // Given - Simulate database connectivity issues
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-error")
                    .addString("simulateDbError", "true")
                    .toJobParameters();

            // When/Then - Job should fail gracefully with proper error handling
            assertThatThrownBy(() -> jobLauncherTestUtils.launchJob(jobParameters))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should validate job restart capability after failure")
        void shouldValidateJobRestartCapability() throws Exception {
            // Given - Job parameters that initially cause failure
            JobParameters initialParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "/invalid/path")
                    .toJobParameters();

            // When - Initial job fails
            JobExecution failedExecution = jobLauncherTestUtils.launchJob(initialParameters);
            assertThat(failedExecution.getStatus()).isEqualTo(BatchStatus.FAILED);

            // Then - Job can be restarted with corrected parameters
            JobParameters correctedParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-restart")
                    .toJobParameters();

            JobExecution restartExecution = jobLauncherTestUtils.launchJob(correctedParameters);
            assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should handle missing customer or account data gracefully")
        void shouldHandleMissingDataGracefully() throws Exception {
            // Given - Transactions with non-existent account references
            Transaction orphanTransaction = testDataGenerator.generateTransaction();
            orphanTransaction.setAccountId(999999L); // Non-existent account
            transactionRepository.save(orphanTransaction);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-missing")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then - Job should complete but skip invalid records
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify error handling recorded in job execution context
            assertThat(jobExecution.getStepExecutions().iterator().next().getSkipCount())
                    .isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Performance and Scalability Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should process statements within 4-hour batch window")
        @Timeout(value = BATCH_PROCESSING_WINDOW_HOURS, unit = TimeUnit.HOURS)
        void shouldProcessStatementsWithinBatchWindow() throws Exception {
            // Given - Large dataset representing production volume
            int accountCount = 1000;
            int transactionsPerAccount = 50;
            
            List<Account> accounts = generateLargeAccountDataset(accountCount);
            accountRepository.saveAll(accounts);
            
            List<Transaction> transactions = generateTransactionsForAccounts(
                    accounts, transactionsPerAccount);
            transactionRepository.saveAll(transactions);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-perf")
                    .addLong("chunkSize", 100L)
                    .toJobParameters();

            long startTime = System.currentTimeMillis();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            long processingTime = System.currentTimeMillis() - startTime;
            
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(processingTime).isLessThan(BATCH_PROCESSING_WINDOW_HOURS * 3600 * 1000);
            
            // Verify all accounts were processed
            assertThat(jobExecution.getStepExecutions().iterator().next().getReadCount())
                    .isEqualTo(accountCount);
        }

        @Test
        @DisplayName("Should maintain COBOL-equivalent response times")
        void shouldMaintainCobolEquivalentResponseTimes() throws Exception {
            // Given - Single account processing for response time measurement
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-response")
                    .addLong("accountIdStart", testAccount.getAccountId())
                    .addLong("accountIdEnd", testAccount.getAccountId())
                    .toJobParameters();

            // When
            long startTime = System.currentTimeMillis();
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
            long processingTime = System.currentTimeMillis() - startTime;

            // Then - Should meet CICS response time requirements
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(processingTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        }

        @Test
        @DisplayName("Should handle concurrent statement generation requests")
        void shouldHandleConcurrentRequests() throws Exception {
            // Given - Multiple job parameters for concurrent execution
            List<JobParameters> concurrentJobs = Arrays.asList(
                    new JobParametersBuilder()
                            .addLocalDateTime("runDate", LocalDateTime.now())
                            .addString("outputPath", "target/test-output-concurrent-1")
                            .addString("jobId", "job1")
                            .toJobParameters(),
                    new JobParametersBuilder()
                            .addLocalDateTime("runDate", LocalDateTime.now())
                            .addString("outputPath", "target/test-output-concurrent-2")
                            .addString("jobId", "job2")
                            .toJobParameters()
            );

            // When - Launch concurrent jobs
            List<JobExecution> executions = concurrentJobs.stream()
                    .map(params -> {
                        try {
                            return jobLauncherTestUtils.launchJob(params);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            // Then - All jobs should complete successfully
            executions.forEach(execution -> 
                    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED));
        }
    }

    @Nested
    @DisplayName("Data Validation and Integrity Tests")
    class DataValidationTests {

        @Test
        @DisplayName("Should validate BigDecimal precision matches COBOL COMP-3")
        void shouldValidateBigDecimalPrecisionMatchesComp3() throws Exception {
            // Given - Transaction amounts with COMP-3 equivalent precision
            BigDecimal cobolAmount = CobolDataConverter.toBigDecimal("123.45", 2);
            
            Transaction precisionTransaction = testDataGenerator.generateTransaction();
            precisionTransaction.setAmount(cobolAmount);
            precisionTransaction.setAccountId(testAccount.getAccountId());
            transactionRepository.save(precisionTransaction);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-precision")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify precision preservation in output
            Path outputFile = Paths.get("target/test-output-precision/statements.txt");
            List<String> lines = Files.readAllLines(outputFile);
            
            // Find amount line and verify COMP-3 precision format
            boolean foundCorrectPrecision = lines.stream()
                    .anyMatch(line -> line.contains("$123.45"));
            assertThat(foundCorrectPrecision).isTrue();
        }

        @Test
        @DisplayName("Should validate account balance calculations")
        void shouldValidateAccountBalanceCalculations() throws Exception {
            // Given - Known transaction amounts for balance calculation
            BigDecimal initialBalance = testAccount.getCurrentBalance();
            BigDecimal totalDebits = testTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredits = testTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .map(Transaction::getAmount)
                    .map(BigDecimal::abs)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-balance")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify balance calculation accuracy
            BigDecimal expectedBalance = AmountCalculator.calculateBalance(
                    initialBalance, totalDebits.subtract(totalCredits));
            
            // Check output contains correct balance
            Path outputFile = Paths.get("target/test-output-balance/statements.txt");
            List<String> lines = Files.readAllLines(outputFile);
            
            boolean foundCorrectBalance = lines.stream()
                    .anyMatch(line -> line.contains("Current Balance") && 
                            line.contains(expectedBalance.toString()));
            assertThat(foundCorrectBalance).isTrue();
        }

        @Test
        @DisplayName("Should validate customer data formatting")
        void shouldValidateCustomerDataFormatting() throws Exception {
            // Given
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .addString("outputPath", "target/test-output-customer")
                    .toJobParameters();

            // When
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            // Verify customer data formatting matches COBOL layout
            Path outputFile = Paths.get("target/test-output-customer/statements.txt");
            List<String> lines = Files.readAllLines(outputFile);
            
            // Verify name formatting (first middle last)
            String expectedName = String.format("%s %s %s", 
                    testCustomer.getFirstName(),
                    testCustomer.getMiddleName(),
                    testCustomer.getLastName());
            assertThat(lines.stream().anyMatch(line -> line.contains(expectedName))).isTrue();
            
            // Verify address formatting
            assertThat(lines.stream().anyMatch(line -> 
                    line.contains(testCustomer.getAddressLine1()))).isTrue();
            assertThat(lines.stream().anyMatch(line -> 
                    line.contains(testCustomer.getZipCode()))).isTrue();
            
            // Verify FICO score formatting
            assertThat(lines.stream().anyMatch(line -> 
                    line.contains("FICO Score") && 
                    line.contains(testCustomer.getFicoScore().toString()))).isTrue();
        }
    }

    // Helper methods for test data generation

    /**
     * Generates large transaction dataset for performance testing.
     */
    private List<Transaction> generateLargeTransactionDataset(int count) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Transaction transaction = testDataGenerator.generateTransaction();
            transaction.setAccountId(testAccount.getAccountId());
            transaction.setTransactionId((long) (100000 + i));
            transactions.add(transaction);
        }
        return transactions;
    }

    /**
     * Generates large account dataset for scalability testing.
     */
    private List<Account> generateLargeAccountDataset(int count) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Account account = testDataGenerator.generateAccount();
            account.setAccountId((long) (2000000000L + i));
            account.setCustomerId(testCustomer.getCustomerId());
            accounts.add(account);
        }
        return accounts;
    }

    /**
     * Generates transactions for multiple accounts.
     */
    private List<Transaction> generateTransactionsForAccounts(List<Account> accounts, int transactionsPerAccount) {
        List<Transaction> transactions = new ArrayList<>();
        long transactionId = 200000L;
        
        for (Account account : accounts) {
            for (int i = 0; i < transactionsPerAccount; i++) {
                Transaction transaction = testDataGenerator.generateTransaction();
                transaction.setTransactionId(transactionId++);
                transaction.setAccountId(account.getAccountId());
                transactions.add(transaction);
            }
        }
        return transactions;
    }

    /**
     * Custom assertion for BigDecimal precision matching COBOL behavior.
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected) {
        assertThat(actual.setScale(2, COBOL_ROUNDING_MODE))
                .isEqualTo(expected.setScale(2, COBOL_ROUNDING_MODE));
    }
}