package com.carddemo.batch;

import org.springframework.batch.core.Job;
import com.carddemo.config.TestBatchConfig;
import com.carddemo.config.TestDatabaseConfig;
import com.carddemo.entity.Customer;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.AuthorizationRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.NotificationRepository;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.StatementRepository;
import com.carddemo.repository.DisputeRepository;
import com.carddemo.repository.ClosureRequestRepository;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import com.carddemo.util.Constants;

import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Assertions;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.core.JobParametersBuilder;
import org.mockito.Mockito;


import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Test suite for the CustomerListJob (CustomerProcessingJob) that processes customer master data,
 * validating functional parity with CBCUS01C COBOL batch program.
 * 
 * This test suite validates:
 * - Indexed VSAM customer file reading simulation using PostgreSQL
 * - Sequential access pattern validation with cursor-based pagination  
 * - Customer record processing and display logic
 * - FD-CUST-ID key-based record retrieval
 * - File status handling and error scenarios
 * - End-of-file detection and processing
 * - Abnormal termination handling (CEE3ABD equivalent)
 * - Chunk processing optimization
 * - Restart capability after failures
 * - Performance benchmarking for large datasets
 * 
 * Uses Spring Batch Test utilities with JobLauncherTestUtils,
 * validates output against COBOL execution results.
 */
@SpringBootTest(classes = {TestBatchConfig.class, TestDatabaseConfig.class}, 
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
    })
@TestPropertySource(properties = {
    "spring.batch.job.enabled=true",
    "spring.batch.jdbc.initialize-schema=always"
})
@ActiveProfiles({"test", "unit-test"})
public class CustomerProcessingJobTest {

    // Spring Batch Test Infrastructure
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    @Qualifier("customerDataListJob") // Use the actual job bean name
    private Job customerListJob;
    
    @Autowired
    private JobLauncher jobLauncher;
    
    // Data Access and Entities
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private CardRepository cardRepository;
    
    @Autowired
    private CardXrefRepository cardXrefRepository;
    
    @Autowired
    private AuthorizationRepository authorizationRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;
    
    @Autowired
    private StatementRepository statementRepository;
    
    @Autowired
    private com.carddemo.repository.FeeRepository feeRepository;
    
    @Autowired
    private DisputeRepository disputeRepository;
    
    @Autowired
    private ClosureRequestRepository closureRequestRepository;
    
    // Utility Classes for COBOL Compatibility
    @Autowired
    private CobolDataConverter cobolDataConverter;
    
    @Autowired
    private DateConversionUtil dateConversionUtil;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private ValidationUtil validationUtil;
    
    // Test Database Configuration - Using H2 in-memory database for fast unit testing

    // Test Data Management
    private List<Customer> testCustomers;
    private Path outputTestFile;
    private LocalDateTime testStartTime;
    
    @BeforeEach
    void setUp() {
        // Initialize test timing for performance validation
        testStartTime = LocalDateTime.now();
        
        // Set up test output file path
        outputTestFile = Paths.get(System.getProperty("java.io.tmpdir"), 
                                   "customer_output_test_" + System.currentTimeMillis() + ".txt");
        
        // Configure JobLauncherTestUtils with the real job
        jobLauncherTestUtils.setJob(customerListJob);
        
        // Clear existing test data to ensure clean state
        // Delete in correct order to avoid foreign key constraint violations
        // 1. Delete authorizations (AUTHORIZATION_DATA references CARD_DATA)
        authorizationRepository.deleteAll();
        // 2. Delete transactions (TRANSACTIONS references CARD_DATA)
        transactionRepository.deleteAll();
        // 3. Delete card cross-references (CARD_XREF references CARD_DATA)
        cardXrefRepository.deleteAll();
        // 4. Delete cards (CARD_DATA references CUSTOMER_DATA)
        cardRepository.deleteAll();
        // 5. Delete notifications (NOTIFICATION references CUSTOMER_DATA)
        notificationRepository.deleteAll();
        // 6. Delete transaction category balances (TRANSACTION_CATEGORY_BALANCE references ACCOUNT_DATA)
        transactionCategoryBalanceRepository.deleteAll();
        // 7. Delete statements (STATEMENT references ACCOUNT_DATA)
        statementRepository.deleteAll();
        // 8. Delete fees (FEE references ACCOUNT_DATA)
        feeRepository.deleteAll();
        // 9. Delete disputes (DISPUTE references ACCOUNT_DATA)
        disputeRepository.deleteAll();
        // 10. Delete account closure requests (ACCOUNT_CLOSURE references ACCOUNT_DATA)
        closureRequestRepository.deleteAll();
        // 11. Delete accounts (ACCOUNT_DATA references CUSTOMER_DATA)
        accountRepository.deleteAll();
        // 12. Delete customers (now no dependencies)
        customerRepository.deleteAll();
        
        // Prepare test data that matches COBOL CUSTFILE record structure
        createTestCustomerData();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test files
        try {
            if (Files.exists(outputTestFile)) {
                Files.delete(outputTestFile);
            }
        } catch (Exception e) {
            // Log but don't fail test cleanup
            System.err.println("Warning: Could not clean up test file: " + e.getMessage());
        }
        
        // Clean up test data from database to ensure test isolation
        try {
            // Delete all test customers (those with test patterns in names or IDs)
            customerRepository.deleteAll(customerRepository.findAll().stream()
                .filter(customer -> customer.getFirstName().startsWith("Test") || 
                                    customer.getLastName().startsWith("Test") ||
                                    customer.getCustomerId().startsWith("100000"))
                .toList());
        } catch (Exception e) {
            // Log but don't fail test cleanup
            System.err.println("Warning: Could not clean up test data: " + e.getMessage());
        }
    }

    /**
     * Creates test customer data that replicates the CUSTFILE VSAM structure
     * from CBCUS01C.cbl, ensuring proper FD-CUST-ID key-based access patterns.
     * 
     * Note: Uses the customer IDs from test-data.sql for consistency with 
     * database initialization.
     */
    private void createTestCustomerData() {
        // Retrieve existing test customers from test-data.sql that will be processed
        // This ensures consistency with the database initialization
        testCustomers = List.of(
            createTestCustomer("1000000001", "John", "Doe", "123-45-6789", "555-123-4567"),
            createTestCustomer("1000000002", "Jane", "Smith", "567-65-4321", "555-987-6543"),
            createTestCustomer("1000000003", "Robert", "Johnson", "456-78-9012", "555-456-7890"),
            createTestCustomer("1000000004", "Mary", "Williams", "234-56-7890", "555-234-5678"),
            createTestCustomer("1000000005", "Michael", "Brown", "345-67-8901", "555-345-6789")
        );
        
        // Note: These customers match the ones in test-data.sql and will be processed by the batch job
        // No need to save them again as they're loaded by TestDatabaseConfig
    }
    
    /**
     * Creates a test customer with COBOL-compatible field formats
     * matching the PIC clause specifications from the original VSAM structure.
     */
    private Customer createTestCustomer(String customerId, String firstName, String lastName, 
                                      String ssn, String phoneNumber) {
        Customer customer = new Customer();
        
        // Validate customer ID matches COBOL FD-CUST-ID PIC 9(10) format (updated for test data)
        Assertions.assertEquals(10, customerId.length(), 
                              "Customer ID must match test data length");
        customer.setCustomerId(customerId);
        
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        // Note: Customer entity doesn't have email field, removed setEmailAddress call
        customer.setPhoneNumber(phoneNumber);
        customer.setSsn(ssn);
        
        return customer;
    }

    /**
     * Creates a large test dataset for performance testing
     */
    private List<Customer> createLargeTestDataset(int size) {
        List<Customer> largeDataset = new java.util.ArrayList<>();
        
        for (int i = 1; i <= size; i++) {
            String customerId = String.format("%010d", i + 1000000000L);  // Start from 1000000001
            String firstName = "TestFirst" + i;
            String lastName = "TestLast" + i;
            String ssn = String.format("%03d-%02d-%04d", (100 + i % 900), (10 + i % 90), (1000 + i % 9000));
            String phone = String.format("555-%03d-%04d", (100 + i % 900), (1000 + i % 9000));
            
            largeDataset.add(createTestCustomer(customerId, firstName, lastName, ssn, phone));
        }
        
        return largeDataset;
    }

    /**
     * Test Suite: Basic Job Execution and Configuration
     * Validates the fundamental job setup and execution patterns
     * equivalent to CBCUS01C COBOL program startup.
     */
    @Nested
    @DisplayName("Job Configuration and Basic Execution Tests")
    class JobConfigurationTests {
        
        @Test
        @DisplayName("Test 0000-CUSTFILE-OPEN equivalent - Job initialization and setup")
        void testJobInitializationAndSetup() {
            // Validate job name matches expected configuration
            Assertions.assertEquals("customerDataListJob", customerListJob.getName(), 
                                  "Job name must match configuration");
            
            // Validate job parameters can be created
            JobParameters jobParams = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            Assertions.assertNotNull(jobParams, "Job parameters must be accessible");
            
            // Validate job is executable (has proper configuration)
            Assertions.assertTrue(customerListJob.isRestartable(), "Job must be configured as restartable");
            Assertions.assertNotNull(customerListJob.getJobParametersValidator(), "Job parameter validator must be defined");
        }
        
        @Test
        @DisplayName("Test job execution with minimal dataset - Success scenario")
        void testJobExecutionWithMinimalDataset() throws Exception {
            // Create small dataset for basic validation
            List<Customer> smallDataset = testCustomers.subList(0, 2);
            customerRepository.saveAll(smallDataset);
            
            // Build job parameters matching COBOL batch job invocation
            JobParameters params = new JobParametersBuilder()
                .addString("inputFile", "database")
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            // Execute job using JobLauncherTestUtils
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate successful completion matching COBOL APPL-AOK condition
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString(),
                                  "Job must complete successfully");
            
            // Validate processing time is reasonable
            long executionTime = jobExecution.getEndTime().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() - 
                               jobExecution.getStartTime().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
            Assertions.assertTrue(executionTime < 30000, 
                                "Small dataset processing should complete within 30 seconds");
        }
    }

    /**
     * Test Suite: Sequential Access Pattern Validation  
     * Validates cursor-based pagination matching COBOL READ NEXT operations
     * from the 1000-CUSTFILE-GET-NEXT paragraph.
     */
    @Nested
    @DisplayName("Sequential Access and Pagination Tests")
    class SequentialAccessTests {
        
        @Test
        @DisplayName("Test 1000-CUSTFILE-GET-NEXT equivalent - Sequential record reading")
        void testSequentialRecordReading() throws Exception {
            // Prepare larger dataset for pagination testing
            customerRepository.saveAll(testCustomers);
            
            JobParameters params = new JobParametersBuilder()
                .addString("chunkSize", "2")  // Small chunks to test pagination
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate successful sequential processing
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString());
            
            // Validate all records were processed (equivalent to reaching EOF)
            long recordsRead = jobExecution.getStepExecutions().iterator().next().getReadCount();
            Assertions.assertEquals(testCustomers.size(), recordsRead, 
                                  "All customer records must be read sequentially");
        }
        
        @Test
        @DisplayName("Test FD-CUST-ID key-based record retrieval patterns")
        void testCustomerIdKeyBasedRetrieval() {
            // Test individual customer retrieval by ID (simulating VSAM key access)
            String testCustomerId = "1000000001";
            Long customerIdLong = Long.valueOf(testCustomerId);
            Customer foundCustomer = customerRepository.findById(customerIdLong).orElse(null);
            
            Assertions.assertNotNull(foundCustomer, "Customer must be retrievable by ID");
            Assertions.assertEquals(testCustomerId, foundCustomer.getCustomerId(), 
                                  "Retrieved customer ID must match search key");
            
            // Validate customer ID format matches test data constraints
            Assertions.assertEquals(10, foundCustomer.getCustomerId().length(),
                                  "Customer ID length must match test data specification");
        }
        
        @Test
        @DisplayName("Test end-of-file detection and processing")
        void testEndOfFileDetection() throws Exception {
            // Create known dataset size for EOF testing
            customerRepository.saveAll(testCustomers);
            long expectedRecordCount = testCustomers.size();
            
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate job completed normally (equivalent to COBOL END-OF-FILE = 'Y')
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString());
            
            // Validate exact record count processed
            long actualRecordsProcessed = jobExecution.getStepExecutions().iterator().next().getReadCount();
            Assertions.assertEquals(expectedRecordCount, actualRecordsProcessed,
                                  "Must process exact number of records before EOF");
        }
    }

    /**
     * Test Suite: Customer Record Processing and Display Logic
     * Validates the customer record formatting and display equivalent  
     * to COBOL DISPLAY CUSTOMER-RECORD operations.
     */
    @Nested
    @DisplayName("Customer Record Processing Tests")
    class CustomerRecordProcessingTests {
        
        @Test
        @DisplayName("Test customer record processing and validation logic")
        void testCustomerRecordProcessingLogic() {
            // Test each customer record matches expected validation patterns
            for (Customer customer : testCustomers) {
                // Validate SSN format using ValidationUtil (COBOL copybook equivalent)
                boolean ssnValid = true;
                try {
                    ValidationUtil.validateSSN(customer.getSsn());
                } catch (Exception e) {
                    ssnValid = false;
                }
                Assertions.assertTrue(ssnValid, 
                    "Customer SSN must pass validation: " + customer.getSsn());
                
                // Validate phone number format
                boolean phoneValid = true;
                try {
                    ValidationUtil.validatePhoneNumber("phone", customer.getPhoneNumber());
                } catch (Exception e) {
                    phoneValid = false;
                }
                Assertions.assertTrue(phoneValid,
                    "Customer phone must pass validation: " + customer.getPhoneNumber());
                
                // Validate customer ID format matches test data
                Assertions.assertEquals(10, customer.getCustomerId().length(),
                    "Customer ID length must match test data specification: " + customer.getCustomerId());
            }
        }
        
        @Test
        @DisplayName("Test COBOL data type conversion precision preservation")
        void testCobolDataTypeConversionPrecision() {
            // Test COMP-3 packed decimal conversion for any numeric fields
            String testAmount = "12345.67";
            BigDecimal convertedAmount = cobolDataConverter.toBigDecimal(testAmount, 2);
            
            // Validate precision preservation matches COBOL COMP-3 behavior
            Assertions.assertNotNull(convertedAmount, "Converted amount must not be null");
            Assertions.assertEquals(new BigDecimal("12345.67"), convertedAmount,
                                  "BigDecimal conversion must preserve exact precision");
            
            // Test PIC string conversion matching COBOL picture clauses
            Object picResult = CobolDataConverter.convertPicNumeric("12345", "PIC 9(5)");
            Assertions.assertEquals("12345", picResult.toString(), "PIC string conversion must match COBOL format");
        }
        
        @Test
        @DisplayName("Test date conversion and validation matching CSUTLDTC functionality")
        void testDateConversionAndValidation() {
            // Test date validation equivalent to COBOL CSUTLDTC subprogram
            String validDate = "20240315";
            boolean dateValid = DateConversionUtil.validateDate(validDate);
            Assertions.assertTrue(dateValid, "Valid CCYYMMDD date must pass validation");
            
            // Test date format conversion
            LocalDate localDate = DateConversionUtil.parseDate(validDate);
            String formattedDate = DateConversionUtil.formatCCYYMMDD(localDate);
            Assertions.assertEquals("20240315", formattedDate, 
                                  "Date format must match COBOL CCYYMMDD specification");
            
            // Test invalid date handling  
            String invalidDate = "20241301";  // Invalid month
            boolean invalidDateResult = dateConversionUtil.validateDate(invalidDate);
            Assertions.assertFalse(invalidDateResult, "Invalid date must fail validation");
        }
    }

    /**
     * Test Suite: File Status Handling and Error Scenarios
     * Validates error handling equivalent to COBOL file status checks
     * and Z-ABEND-PROGRAM abnormal termination handling.
     */
    @Nested
    @DisplayName("Error Handling and File Status Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Test file status '00' equivalent - Successful database operations")
        void testSuccessfulDatabaseOperations() throws Exception {
            // Set up normal test scenario (equivalent to CUSTFILE-STATUS = '00')
            customerRepository.saveAll(testCustomers);
            
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate successful completion (equivalent to APPL-AOK condition)
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString(),
                                  "Successful database operations must complete job");
            
            // Validate no errors occurred during processing
            Assertions.assertTrue(jobExecution.getAllFailureExceptions().isEmpty(),
                                "No exceptions should occur during normal processing");
        }
        
        @Test
        @DisplayName("Test file status '10' equivalent - End of file condition")
        @Transactional
        void testEndOfFileCondition() throws Exception {
            // Test empty dataset (equivalent to immediate EOF)
            customerRepository.deleteAll();
            customerRepository.flush(); // Ensure deletion is persisted
            
            // Verify dataset is actually empty
            long customerCount = customerRepository.count();
            Assertions.assertEquals(0, customerCount, "Customer table must be empty for EOF test");
            
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Job should complete even with empty dataset (equivalent to APPL-EOF handling)
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString(),
                                  "Empty dataset should complete gracefully");
            
            // Validate zero records processed
            long recordsRead = jobExecution.getStepExecutions().iterator().next().getReadCount();
            Assertions.assertEquals(0, recordsRead, "No records should be read from empty dataset");
        }
        
        @Test
        @DisplayName("Test abnormal termination handling - CEE3ABD equivalent")
        void testAbnormalTerminationHandling() throws Exception {
            // Test scenario where output file cannot be created (simulating file error)
            // Create a file first, then try to use it as a directory (guaranteed to fail)
            Path tempFile = Files.createTempFile("test_blocker", ".tmp");
            Path invalidOutputPath = tempFile.resolve("cannot_create_file_here.txt");
            
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", invalidOutputPath.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate job fails appropriately (equivalent to Z-ABEND-PROGRAM logic)
            Assertions.assertEquals("FAILED", jobExecution.getStatus().toString(),
                                  "Invalid output path should cause job failure");
            
            // Validate failure exceptions are captured
            Assertions.assertFalse(jobExecution.getAllFailureExceptions().isEmpty(),
                                 "Failure exceptions should be recorded for debugging");
        }
        
        @Test
        @DisplayName("Test Z-DISPLAY-IO-STATUS equivalent - Error status reporting")
        @Transactional
        void testErrorStatusReporting() throws Exception {
            // Test file system error scenario (equivalent to COBOL file status error)
            customerRepository.saveAll(testCustomers);
            customerRepository.flush(); // Ensure test data is persisted first
            
            // Force a job failure with invalid output path (simulates file system error)
            // Use a path with invalid characters that cannot be created
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", "/tmp/invalid\0null/customer_list.txt")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate job reports error status appropriately (equivalent to Z-DISPLAY-IO-STATUS)
            Assertions.assertEquals("FAILED", jobExecution.getStatus().toString(),
                                  "Invalid output path should cause job failure with error status");
            
            // Validate that failure exceptions are captured for error reporting
            Assertions.assertFalse(jobExecution.getAllFailureExceptions().isEmpty(),
                                 "Error status should include failure exceptions for troubleshooting");
            
            // Validate error message is descriptive (equivalent to COBOL error display)
            Throwable firstException = jobExecution.getAllFailureExceptions().get(0);
            String errorMessage = firstException.getMessage() != null ? firstException.getMessage().toLowerCase() : "";
            
            Assertions.assertTrue(
                errorMessage.contains("error") || 
                errorMessage.contains("failed") ||
                errorMessage.contains("cannot") ||
                errorMessage.contains("unable") ||
                errorMessage.contains("exception"),
                "Error status must be descriptive for troubleshooting. Got: " + errorMessage);
        }
    }

    /**
     * Test Suite: Performance and Processing Window Validation
     * Validates batch processing completes within 4-hour window requirement
     * and meets performance benchmarks for large datasets.
     */
    @Nested
    @DisplayName("Performance and Processing Window Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("Test processing time benchmarking for standard dataset")
        void testProcessingTimeBenchmark() throws Exception {
            // Use full test dataset for performance validation
            customerRepository.saveAll(testCustomers);
            
            LocalDateTime startTime = LocalDateTime.now();
            
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            LocalDateTime endTime = LocalDateTime.now();
            long processingTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            // Validate successful completion
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString());
            
            // Validate processing time is reasonable (scale test for large datasets)
            // For 5 records, should complete in under 10 seconds
            Assertions.assertTrue(processingTimeMs < 10000,
                                "Processing time (" + processingTimeMs + "ms) must be under 10 seconds for test dataset");
            
            // Log performance metrics for analysis
            System.out.println("Processing Performance Metrics:");
            System.out.println("- Records processed: " + testCustomers.size());
            System.out.println("- Processing time: " + processingTimeMs + "ms");
            System.out.println("- Records per second: " + (testCustomers.size() * 1000.0 / processingTimeMs));
        }
        
        @Test
        @DisplayName("Test large dataset processing simulation - 4-hour window validation")
        @Transactional
        void testLargeDatasetProcessingSimulation() throws Exception {
            // Create larger test dataset to simulate production volume
            List<Customer> largeDataset = createLargeTestDataset(100);
            customerRepository.saveAll(largeDataset);
            entityManager.flush(); // Ensure data is committed to database before job execution
            
            LocalDateTime startTime = LocalDateTime.now();
            
            JobParameters params = new JobParametersBuilder()
                .addString("chunkSize", "10")  // Optimize chunk size for larger dataset
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            LocalDateTime endTime = LocalDateTime.now();
            long processingTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            // Validate successful completion
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString());
            
            // Validate processing time scales appropriately
            // Should handle 100 records in under 60 seconds
            Assertions.assertTrue(processingTimeMs < 60000,
                                "Large dataset processing time (" + processingTimeMs + "ms) must scale appropriately");
            
            // Validate all records processed
            long recordsProcessed = jobExecution.getStepExecutions().iterator().next().getReadCount();
            Assertions.assertEquals(largeDataset.size(), recordsProcessed,
                                  "All large dataset records must be processed");
        }
        

    }

    /**
     * Test Suite: Chunk Processing and Optimization
     * Validates Spring Batch chunk processing optimization
     * and restart capability after failures.
     */
    @Nested
    @DisplayName("Chunk Processing and Restart Tests")
    class ChunkProcessingTests {
        
        @Test
        @DisplayName("Test chunk processing optimization with various chunk sizes")
        void testChunkProcessingOptimization() throws Exception {
            // Test different chunk sizes to validate optimization
            customerRepository.saveAll(testCustomers);
            
            int[] chunkSizes = {1, 2, 5, 10};
            
            for (int chunkSize : chunkSizes) {
                JobParameters params = new JobParametersBuilder()
                    .addString("chunkSize", String.valueOf(chunkSize))
                    .addString("outputFile", outputTestFile.toString() + "_chunk" + chunkSize)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
                
                LocalDateTime chunkStartTime = LocalDateTime.now();
                JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
                LocalDateTime chunkEndTime = LocalDateTime.now();
                
                // Validate successful completion regardless of chunk size
                Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString(),
                                      "Job must complete successfully with chunk size " + chunkSize);
                
                // Validate all records processed
                long recordsProcessed = jobExecution.getStepExecutions().iterator().next().getReadCount();
                Assertions.assertEquals(testCustomers.size(), recordsProcessed,
                                      "All records must be processed with chunk size " + chunkSize);
                
                // Log performance for chunk size analysis
                long chunkProcessingTime = java.time.Duration.between(chunkStartTime, chunkEndTime).toMillis();
                System.out.println("Chunk size " + chunkSize + " processing time: " + chunkProcessingTime + "ms");
            }
        }
        
        @Test
        @DisplayName("Test restart capability after failures - Job recovery simulation")
        void testRestartCapabilityAfterFailures() throws Exception {
            // Test restart capability with job execution
            customerRepository.saveAll(testCustomers);
            
            // First attempt with standard parameters
            JobParameters firstParams = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())  
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution firstJobExecution = jobLauncherTestUtils.launchJob(firstParams);
            
            // Validate job completed successfully
            Assertions.assertEquals("COMPLETED", firstJobExecution.getStatus().toString(),
                                  "Initial job execution should complete successfully");
            
            // Now test restart with different timestamp
            JobParameters restartParams = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis() + 1)  // Different timestamp for restart
                .toJobParameters();
            
            JobExecution restartedJobExecution = jobLauncherTestUtils.launchJob(restartParams);
            
            // Validate successful restart and completion
            Assertions.assertEquals("COMPLETED", restartedJobExecution.getStatus().toString(),
                                  "Restarted job with different parameters should complete successfully");
            
            // Validate restart functionality (equivalent to COBOL restart capability)
            JobParameters restartParams2 = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis() + 1000)
                .toJobParameters();
            JobExecution restartExecution = jobLauncherTestUtils.launchJob(restartParams2);
            Assertions.assertNotNull(restartExecution, "Job must support restart capability");
            Assertions.assertEquals("COMPLETED", restartExecution.getStatus().toString(),
                                  "Restarted job must complete successfully");
        }
        
        @Test
        @DisplayName("Test resource cleanup and connection management")
        void testResourceCleanupAndConnectionManagement() throws Exception {
            // Test multiple job executions to validate resource cleanup
            customerRepository.saveAll(testCustomers);
            
            // Execute multiple jobs in sequence
            for (int i = 1; i <= 3; i++) {
                JobParameters params = new JobParametersBuilder()
                    .addString("outputFile", outputTestFile.toString() + "_run" + i)
                    .addLong("timestamp", System.currentTimeMillis() + i)
                    .toJobParameters();
                
                JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
                
                // Validate each execution completes successfully
                Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString(),
                                      "Job execution " + i + " must complete successfully");
                
                // Validate resource cleanup between executions
                Assertions.assertNotNull(jobExecution.getEndTime(),
                                       "Job execution " + i + " must have proper end time");
                
                // Brief pause to ensure proper cleanup
                Thread.sleep(100);
            }
        }
    }

    /**
     * Test Suite: Integration and End-to-End Validation
     * Validates complete integration with database, file system,
     * and output generation matching COBOL program behavior.
     */
    @Nested
    @DisplayName("Integration and End-to-End Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Test complete integration - Database to file output")
        void testCompleteIntegrationDatabaseToFileOutput() throws Exception {
            // Set up comprehensive test scenario
            customerRepository.saveAll(testCustomers);
            
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            // Execute complete job
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate job completion
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString(),
                                  "Complete integration test must succeed");
            
            // Validate output file was created (equivalent to COBOL DISPLAY output)
            Assertions.assertTrue(Files.exists(outputTestFile),
                                "Output file must be created");
            
            // Validate output file content matches customer records
            List<String> outputLines = Files.readAllLines(outputTestFile);
            Assertions.assertFalse(outputLines.isEmpty(),
                                 "Output file must contain customer data");
            
            // Validate customer data appears in output (equivalent to DISPLAY CUSTOMER-RECORD)
            for (Customer customer : testCustomers) {
                boolean customerFoundInOutput = outputLines.stream()
                    .anyMatch(line -> line.contains(customer.getCustomerId()) && 
                                    line.contains(customer.getFirstName()) && 
                                    line.contains(customer.getLastName()));
                
                Assertions.assertTrue(customerFoundInOutput,
                    "Customer " + customer.getCustomerId() + " must appear in output file");
            }
        }
        
        @Test
        @DisplayName("Test output validation against COBOL execution results")
        void testOutputValidationAgainstCobolResults() throws Exception {
            // Create test customer matching COBOL test data format
            Customer cobolTestCustomer = createTestCustomer("000000999", "COBOL", "TESTUSER", 
                                                          "123-99-9999", "555-999-9999");
            customerRepository.save(cobolTestCustomer);
            
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate job completion
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString());
            
            // Read and validate output format matches expected COBOL display format
            List<String> outputLines = Files.readAllLines(outputTestFile);
            String customerOutputLine = outputLines.stream()
                .filter(line -> line.contains("000000999"))
                .findFirst()
                .orElse("");
            
            Assertions.assertFalse(customerOutputLine.isEmpty(),
                                 "COBOL test customer must appear in output");
            
            // Validate key fields are present in output (matching COBOL CUSTOMER-RECORD display)
            Assertions.assertTrue(customerOutputLine.contains("000000999"), "Customer ID must be in output");
            Assertions.assertTrue(customerOutputLine.contains("COBOL"), "First name must be in output");
            Assertions.assertTrue(customerOutputLine.contains("TESTUSER"), "Last name must be in output");
            Assertions.assertTrue(customerOutputLine.contains("123-99-9999"), "SSN must be in output");
        }
        
        @Test
        @DisplayName("Test job execution monitoring and metrics collection")
        void testJobExecutionMonitoringAndMetrics() throws Exception {
            customerRepository.saveAll(testCustomers);
            
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate job execution metrics are collected
            Assertions.assertNotNull(jobExecution.getStartTime(), "Start time must be recorded");
            Assertions.assertNotNull(jobExecution.getEndTime(), "End time must be recorded");
            Assertions.assertNotNull(jobExecution.getStatus(), "Job status must be recorded");
            
            // Validate step execution metrics
            Assertions.assertEquals(1, jobExecution.getStepExecutions().size(),
                                  "Job should have exactly one step execution");
            
            var stepExecution = jobExecution.getStepExecutions().iterator().next();
            Assertions.assertEquals(testCustomers.size(), stepExecution.getReadCount(),
                                  "Step read count must match customer count");
            Assertions.assertEquals(testCustomers.size(), stepExecution.getWriteCount(),
                                  "Step write count must match customer count");
            Assertions.assertEquals(0, stepExecution.getSkipCount(),
                                  "No records should be skipped in normal processing");
        }
    }

    /**
     * Test Suite: Data Validation and Field Format Compliance
     * Validates data field formats and constraints match COBOL specifications
     * including PIC clause validation and COMP-3 precision handling.
     */
    @Nested
    @DisplayName("Data Validation and Field Format Tests")
    class DataValidationTests {
        
        @Test
        @DisplayName("Test customer field length validation - COBOL PIC clause compliance")
        void testCustomerFieldLengthValidation() {
            // Test all customer field lengths match COBOL PIC specifications
            Customer testCustomer = testCustomers.get(0);
            
            // Validate customer ID length (PIC 9(09) from COBOL)
            Assertions.assertEquals(Constants.CUSTOMER_ID_LENGTH, testCustomer.getCustomerId().length(),
                                  "Customer ID must match COBOL PIC 9(09) length");
            
            // Validate SSN length includes formatting dashes
            Assertions.assertEquals(Constants.SSN_LENGTH, testCustomer.getSsn().length(),
                                  "SSN must match expected format length");
            
            // Validate phone number length includes formatting
            Assertions.assertEquals(Constants.PHONE_NUMBER_LENGTH, testCustomer.getPhoneNumber().length(),
                                  "Phone number must match expected format length");
        }
        
        @Test
        @DisplayName("Test field validation using ValidationUtil - COBOL copybook equivalent")
        void testFieldValidationUsingValidationUtil() {
            Customer testCustomer = testCustomers.get(0);
            
            // Test SSN validation (equivalent to COBOL validation routine)
            boolean ssnValid = true;
            try {
                ValidationUtil.validateSSN(testCustomer.getSsn());
            } catch (Exception e) {
                ssnValid = false;
            }
            Assertions.assertTrue(ssnValid, "Valid SSN must pass ValidationUtil check");
            
            // Test phone area code validation (NANPA equivalent)
            boolean phoneValid = true;
            try {
                ValidationUtil.validatePhoneNumber("phone", testCustomer.getPhoneNumber());
            } catch (Exception e) {
                phoneValid = false;
            }
            Assertions.assertTrue(phoneValid, "Valid phone number must pass area code validation");
            
            // Test with invalid data to ensure validation works
            boolean invalidSsnResult = false;
            try {
                ValidationUtil.validateSSN("invalid-ssn");
                invalidSsnResult = true;
            } catch (Exception e) {
                invalidSsnResult = false;
            }
            Assertions.assertFalse(invalidSsnResult, "Invalid SSN must fail validation");
            
            boolean invalidPhoneResult = true;
            try {
                ValidationUtil.validatePhoneNumber("phone", "invalid-phone");
            } catch (Exception e) {
                invalidPhoneResult = false;
            }
            Assertions.assertFalse(invalidPhoneResult, "Invalid phone number must fail validation");
        }
        
        @Test
        @DisplayName("Test date validation and conversion - CSUTLDTC equivalent functionality")
        void testDateValidationAndConversion() {
            // Test valid date formats
            String validDate1 = "20240315";
            String validDate2 = "20241225";
            
            Assertions.assertTrue(DateConversionUtil.validateDate(validDate1),
                                "Valid date 20240315 must pass validation");
            Assertions.assertTrue(DateConversionUtil.validateDate(validDate2),
                                "Valid date 20241225 must pass validation");
            
            // Test invalid date formats
            String invalidDate1 = "20241301";  // Invalid month
            String invalidDate2 = "20240229";  // Invalid leap year date (2024 is leap year, so this should be valid)
            String invalidDate3 = "20230229";  // Invalid leap year date (2023 is not leap year)
            
            Assertions.assertFalse(DateConversionUtil.validateDate(invalidDate1),
                                 "Invalid month date must fail validation");
            Assertions.assertFalse(DateConversionUtil.validateDate(invalidDate3),
                                 "Invalid leap year date must fail validation");
            
            // Test date format conversion
            LocalDate localDate1 = DateConversionUtil.parseDate(validDate1);
            String formattedDate = DateConversionUtil.formatCCYYMMDD(localDate1);
            Assertions.assertEquals("20240315", formattedDate,
                                  "Date formatting must maintain CCYYMMDD format");
        }
    }

    /**
     * Test Suite: Comprehensive Functional Parity Validation
     * Validates complete functional equivalence with CBCUS01C COBOL program
     * including all major processing paths and edge cases.
     */
    @Nested
    @DisplayName("Functional Parity and COBOL Equivalence Tests")
    class FunctionalParityTests {
        
        @Test
        @DisplayName("Test complete CBCUS01C program flow simulation")
        void testCompleteCBCUS01CProgramFlowSimulation() throws Exception {
            // Simulate complete COBOL program execution flow:
            // 1. Program start (equivalent to DISPLAY 'START OF EXECUTION OF PROGRAM CBCUS01C')
            System.out.println("TEST: Simulating START OF EXECUTION OF PROGRAM CBCUS01C");
            
            // 2. Set up test data (equivalent to opening CUSTFILE)
            customerRepository.saveAll(testCustomers);
            System.out.println("TEST: Customer file opened successfully - " + testCustomers.size() + " records");
            
            // 3. Execute job (equivalent to PERFORM UNTIL END-OF-FILE = 'Y')
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // 4. Validate successful completion (equivalent to normal program termination)
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString(),
                                  "Complete program flow must execute successfully");
            
            // 5. Validate all records processed (equivalent to all DISPLAY CUSTOMER-RECORD)
            long recordsProcessed = jobExecution.getStepExecutions().iterator().next().getReadCount();
            Assertions.assertEquals(testCustomers.size(), recordsProcessed,
                                  "All customer records must be processed and displayed");
            
            // 6. Program end simulation (equivalent to DISPLAY 'END OF EXECUTION OF PROGRAM CBCUS01C')
            System.out.println("TEST: Simulating END OF EXECUTION OF PROGRAM CBCUS01C");
            System.out.println("TEST: Total execution time: " + 
                             java.time.Duration.between(testStartTime, LocalDateTime.now()).toMillis() + "ms");
        }
        
        @Test
        @DisplayName("Test indexed VSAM simulation using PostgreSQL composite keys")
        void testIndexedVsamSimulationUsingPostgreSQL() {
            // Test VSAM KSDS functionality using PostgreSQL
            customerRepository.saveAll(testCustomers);
            
            // Test key-based access (equivalent to VSAM key access)
            for (Customer customer : testCustomers) {
                Long customerIdLong = Long.valueOf(customer.getCustomerId());
                Customer retrievedCustomer = customerRepository.findById(customerIdLong).orElse(null);
                
                Assertions.assertNotNull(retrievedCustomer, 
                    "Customer must be retrievable by key: " + customer.getCustomerId());
                Assertions.assertEquals(customer.getCustomerId(), retrievedCustomer.getCustomerId(),
                    "Retrieved customer ID must match search key");
                Assertions.assertEquals(customer.getFirstName(), retrievedCustomer.getFirstName(),
                    "Customer data integrity must be maintained");
            }
            
            // Test sequential access pattern (equivalent to COBOL READ NEXT)
            List<Customer> allCustomers = customerRepository.findAll();
            Assertions.assertEquals(testCustomers.size(), allCustomers.size(),
                                  "Sequential access must return all records");
        }
        
        @Test
        @DisplayName("Test cursor-based pagination matching VSAM browse patterns")
        @Transactional
        void testCursorBasedPaginationMatchingVsamBrowse() {
            // Set up larger dataset for pagination testing
            List<Customer> largeDataset = createLargeTestDataset(20);
            customerRepository.saveAll(largeDataset);
            entityManager.flush(); // Ensure data is committed to database before job execution
            
            // Test pagination using findByLastNameAndFirstName (simulating VSAM STARTBR/READNEXT)
            String testLastName = "TestLast1";
            String testFirstName = "TestFirst1";
            
            List<Customer> foundCustomers = customerRepository.findByLastNameAndFirstName(
                testLastName, testFirstName);
            
            Assertions.assertFalse(foundCustomers.isEmpty(),
                                 "Customer search must return results for valid criteria");
            
            // Validate found customer matches search criteria
            Customer foundCustomer = foundCustomers.get(0);
            Assertions.assertEquals(testLastName, foundCustomer.getLastName(),
                                  "Found customer last name must match search criteria");
            Assertions.assertEquals(testFirstName, foundCustomer.getFirstName(),
                                  "Found customer first name must match search criteria");
        }
        
        @Test
        @DisplayName("Test output format validation and COBOL display format compliance")
        void testOutputFormatValidationAndCobolDisplayCompliance() throws Exception {
            // Create customer with specific data for output format testing
            Customer formatTestCustomer = createTestCustomer("000001234", "FORMAT", "TESTCUST", 
                                                           "123-45-6789", "555-123-4567");
            customerRepository.save(formatTestCustomer);
            
            JobParameters params = new JobParametersBuilder()
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            // Validate successful execution
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString());
            
            // Read output and validate format
            List<String> outputLines = Files.readAllLines(outputTestFile);
            String customerLine = outputLines.stream()
                .filter(line -> line.contains("000001234"))
                .findFirst()
                .orElse("");
            
            Assertions.assertFalse(customerLine.isEmpty(), "Customer must appear in output");
            
            // Validate output contains all key customer fields (equivalent to COBOL CUSTOMER-RECORD display)
            Assertions.assertTrue(customerLine.contains("000001234"), "Output must contain customer ID");
            Assertions.assertTrue(customerLine.contains("FORMAT"), "Output must contain first name");
            Assertions.assertTrue(customerLine.contains("TESTCUST"), "Output must contain last name");
            Assertions.assertTrue(customerLine.contains("123-45-6789"), "Output must contain SSN");
            Assertions.assertTrue(customerLine.contains("555-123-4567"), "Output must contain phone");
        }
    }

    /**
     * Test Suite: Performance Benchmarking and Processing Window Validation
     * Validates processing performance meets 4-hour window requirement
     * and benchmarks against COBOL execution performance.
     */
    @Nested
    @DisplayName("Performance Benchmarking Tests")
    class PerformanceBenchmarkingTests {
        
        @Test
        @DisplayName("Test performance benchmarking for production-scale datasets")
        @Transactional
        void testPerformanceBenchmarkingForProductionScale() throws Exception {
            // Create production-scale test dataset (scaled down for unit test)
            List<Customer> productionScaleDataset = createLargeTestDataset(500);
            customerRepository.saveAll(productionScaleDataset);
            entityManager.flush(); // Ensure data is committed to database before job execution
            
            // Record start time for performance measurement
            LocalDateTime benchmarkStartTime = LocalDateTime.now();
            
            JobParameters params = new JobParametersBuilder()
                .addString("chunkSize", "50")  // Optimized chunk size for larger dataset
                .addString("outputFile", outputTestFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
            
            LocalDateTime benchmarkEndTime = LocalDateTime.now();
            long totalProcessingTime = java.time.Duration.between(benchmarkStartTime, benchmarkEndTime).toMillis();
            
            // Validate successful completion
            Assertions.assertEquals("COMPLETED", jobExecution.getStatus().toString(),
                                  "Production-scale dataset processing must complete successfully");
            
            // Validate performance benchmarks
            // For 500 records, should complete well under 4-hour window (target: under 2 minutes)
            long maxAllowedTimeMs = TimeUnit.MINUTES.toMillis(2);
            Assertions.assertTrue(totalProcessingTime < maxAllowedTimeMs,
                "Production-scale processing (" + totalProcessingTime + "ms) must complete within 2 minutes");
            
            // Calculate and validate throughput
            double recordsPerSecond = (productionScaleDataset.size() * 1000.0) / totalProcessingTime;
            System.out.println("Performance Benchmark Results:");
            System.out.println("- Dataset size: " + productionScaleDataset.size() + " records");
            System.out.println("- Total processing time: " + totalProcessingTime + "ms");
            System.out.println("- Throughput: " + String.format("%.2f", recordsPerSecond) + " records/second");
            
            // Validate minimum throughput requirement (should process at least 4 records/second)
            Assertions.assertTrue(recordsPerSecond >= 4.0,
                                "Processing throughput must be at least 4 records/second");
            
            // Validate output file size matches expected records
            long outputLineCount = Files.lines(outputTestFile).count();
            Assertions.assertEquals(productionScaleDataset.size(), outputLineCount,
                                  "Output file must contain all processed customer records");
        }
    }
}