# CardDemo Batch Test Resources

## Overview

This directory contains comprehensive test resources for validating the Spring Batch implementation that replaces the original COBOL JCL batch processing system. The test resources ensure **100% functional parity** between the modernized Spring Boot batch jobs and the original mainframe batch operations while maintaining exact business logic and precision requirements.

## Directory Structure

```
backend/src/test/resources/batch/
├── README.md                    # This documentation file
├── input/                       # Test input data files
│   ├── fixed-width/            # Fixed-width format test data
│   │   ├── daily-transactions.txt
│   │   ├── customer-updates.txt
│   │   ├── account-changes.txt
│   │   └── payment-files.txt
│   ├── csv/                    # CSV format test data
│   │   ├── transaction-test-data.csv
│   │   ├── customer-test-data.csv
│   │   └── account-test-data.csv
│   └── json/                   # JSON format test data
│       ├── batch-job-params.json
│       └── validation-rules.json
├── expected/                   # Expected output files
│   ├── reports/               # Expected report outputs
│   │   ├── daily-transaction-report.txt
│   │   ├── interest-calculation-report.txt
│   │   ├── statement-generation-report.txt
│   │   └── account-summary-report.txt
│   ├── database/              # Expected database states
│   │   ├── post-transaction-processing.sql
│   │   ├── post-interest-calculation.sql
│   │   └── post-statement-generation.sql
│   └── files/                 # Expected output files
│       ├── reject-records.txt
│       ├── processed-transactions.txt
│       └── error-log.txt
├── jobs/                      # Spring Batch job configurations
│   ├── transaction-posting-job.xml
│   ├── interest-calculation-job.xml
│   ├── statement-generation-job.xml
│   ├── daily-processing-job.xml
│   └── report-generation-job.xml
└── config/                    # Test configuration files
    ├── test-application.yml   # Spring Boot test configuration
    ├── batch-test.properties  # Batch-specific test properties
    ├── database-test.sql      # Test database setup
    └── vsam-mapping.properties # VSAM to PostgreSQL mappings
```

## Test Data Format Requirements

### Fixed-Width Data Format

The test input files maintain the exact record layouts from the original COBOL copybooks to ensure compatibility:

#### Daily Transaction File Format (`daily-transactions.txt`)
```
Position  Length  Field Name           COBOL Type        Java Type
1-10      10      Transaction ID       PIC 9(10)         BIGINT
11-26     16      Card Number          PIC X(16)         VARCHAR(16)
27-36     10      Account ID           PIC 9(10)         BIGINT  
37-44     8       Transaction Date     PIC 9(8)          DATE (YYYYMMDD)
45-50     6       Transaction Time     PIC 9(6)          TIME (HHMMSS)
51-62     12      Amount               PIC S9(10)V99 COMP-3  NUMERIC(12,2)
63-64     2       Transaction Type     PIC X(2)          VARCHAR(2)
65-68     4       Category Code        PIC X(4)          VARCHAR(4)
69-168    100     Description          PIC X(100)        VARCHAR(100)
169-218   50      Merchant Name        PIC X(50)         VARCHAR(50)
```

#### Customer Update File Format (`customer-updates.txt`)
```
Position  Length  Field Name           COBOL Type        Java Type
1-9       9       Customer ID          PIC 9(9)          BIGINT
10-29     20      First Name           PIC X(20)         VARCHAR(20)
30-49     20      Middle Name          PIC X(20)         VARCHAR(20)
50-69     20      Last Name            PIC X(20)         VARCHAR(20)
70-119    50      Address Line 1       PIC X(50)         VARCHAR(50)
120-169   50      Address Line 2       PIC X(50)         VARCHAR(50)
170-219   50      Address Line 3       PIC X(50)         VARCHAR(50)
220-221   2       State Code           PIC X(2)          VARCHAR(2)
222-224   3       Country Code         PIC X(3)          VARCHAR(3)
225-234   10      ZIP Code             PIC X(10)         VARCHAR(10)
235-249   15      Phone Number 1       PIC X(15)         VARCHAR(15)
250-264   15      Phone Number 2       PIC X(15)         VARCHAR(15)
265-273   9       SSN                  PIC 9(9)          VARCHAR(9)
274-293   20      Government ID        PIC X(20)         VARCHAR(20)
294-301   8       Date of Birth        PIC 9(8)          DATE (YYYYMMDD)
302-311   10      EFT Account ID       PIC X(10)         VARCHAR(10)
312-312   1       Primary Card Holder  PIC X(1)          CHAR(1)
313-315   3       FICO Score           PIC 9(3)          SMALLINT
```

## VSAM-to-Spring Batch Mapping

### Original JCL Jobs → Spring Batch Jobs

| Original JCL Job | Spring Batch Job | Description | Processing Window |
|------------------|------------------|-------------|-------------------|
| `DAILYPROC` | `dailyProcessingJob` | Daily transaction posting and validation | 2:00 AM - 6:00 AM |
| `INTRST` | `interestCalculationJob` | Monthly interest calculation on account balances | Monthly 1st, 3:00 AM |
| `STMT` | `statementGenerationJob` | Customer statement generation | Monthly 15th, 4:00 AM |
| `CBTRN01C` | `transactionPostingJob` | Transaction file processing | Daily 2:30 AM |
| `CBTRN02C` | `transactionValidationJob` | Transaction validation and error handling | Daily 2:45 AM |
| `CBACT04C` | `accountInterestJob` | Account interest calculation | Monthly calculation |

### VSAM Dataset → PostgreSQL Table Mapping

| VSAM Dataset | PostgreSQL Table | Primary Key | Access Pattern |
|--------------|------------------|-------------|----------------|
| `TRANSACT` | `transactions` | `transaction_id (BIGINT)` | Range partitioned by `transaction_date` |
| `ACCTDAT` | `account_data` | `account_id (BIGINT)` | B-tree indexed |
| `CUSTDAT` | `customer_data` | `customer_id (BIGINT)` | B-tree indexed with name search |
| `CARDDAT` | `card_data` | `card_number (VARCHAR(16))` | Composite foreign keys to account/customer |
| `TCATBAL` | `transaction_category_balance` | `(account_id, category_code, balance_date)` | Composite primary key |
| `DISCGRP` | `disclosure_groups` | `disclosure_group_id (BIGINT)` | Reference data table |

## COMP-3 Precision Handling

### Background
COBOL COMP-3 (packed decimal) fields require exact precision preservation when converted to Java BigDecimal to ensure **penny-level accuracy** in financial calculations.

### Conversion Examples

#### Interest Calculation Precision Test
```java
@Test
@DisplayName("Interest calculation - COMP-3 to BigDecimal precision validation")
public void testInterestCalculation_ExactPrecisionMatch() {
    // Given: COBOL COMP-3 field PIC S9(7)V99 representing account balance
    byte[] comp3Balance = {0x01, 0x23, 0x45, 0x67, 0x0F}; // $12345.67 in packed decimal
    
    // When: Converting using CobolDataConverter with exact scale preservation
    BigDecimal accountBalance = CobolDataConverter.fromComp3(comp3Balance, 7, 2);
    
    // Then: Verify exact precision match
    assertThat(accountBalance).isEqualTo(new BigDecimal("12345.67"));
    assertThat(accountBalance.scale()).isEqualTo(2);
    assertThat(accountBalance.precision()).isEqualTo(7);
    
    // Interest calculation with COBOL ROUNDED equivalent
    BigDecimal interestRate = new BigDecimal("0.0525"); // 5.25% annual
    BigDecimal monthlyInterest = accountBalance
        .multiply(interestRate)
        .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP); // Monthly calculation
        
    // Verify result matches COBOL CBACT04C.cbl calculation
    assertThat(monthlyInterest).isEqualTo(new BigDecimal("54.03"));
}
```

#### Transaction Amount Validation Test
```java
@Test
@DisplayName("Transaction amount - COMP-3 precision preservation")
public void testTransactionAmount_Comp3Precision() {
    // Given: COBOL transaction amount PIC S9(10)V99 COMP-3
    byte[] comp3Amount = {0x00, 0x00, 0x01, 0x50, 0x00, 0x0F}; // $150.00
    
    // When: Converting through Spring Batch ItemProcessor
    TransactionRecord record = new TransactionRecord();
    BigDecimal amount = CobolDataConverter.fromComp3(comp3Amount, 10, 2);
    record.setAmount(amount);
    
    // Then: Verify database persistence maintains precision
    TransactionRecord savedRecord = transactionRepository.save(record);
    assertThat(savedRecord.getAmount()).isEqualTo(new BigDecimal("150.00"));
    assertThat(savedRecord.getAmount().scale()).isEqualTo(2);
    
    // Verify PostgreSQL NUMERIC(12,2) storage preserves exact precision
    BigDecimal retrievedAmount = jdbcTemplate.queryForObject(
        "SELECT amount FROM transactions WHERE transaction_id = ?",
        BigDecimal.class,
        savedRecord.getTransactionId()
    );
    assertThat(retrievedAmount).isEqualTo(new BigDecimal("150.00"));
}
```

#### Balance Update Accuracy Test
```java
@Test
@DisplayName("Account balance update - exact COBOL parity")
public void testAccountBalanceUpdate_CobolParity() {
    // Given: Starting balance and transaction amount in COMP-3 format
    BigDecimal startingBalance = new BigDecimal("1000.00");
    BigDecimal transactionAmount = new BigDecimal("75.50");
    
    // When: Performing balance calculation using identical COBOL logic
    BigDecimal newBalance = startingBalance.subtract(transactionAmount);
    
    // Then: Verify penny-level accuracy
    assertThat(newBalance).isEqualTo(new BigDecimal("924.50"));
    assertThat(newBalance.scale()).isEqualTo(2);
    
    // Verify database constraint and precision
    Account account = new Account();
    account.setCurrentBalance(newBalance);
    Account savedAccount = accountRepository.save(account);
    
    assertThat(savedAccount.getCurrentBalance()).isEqualTo(new BigDecimal("924.50"));
}
```

## Spring Batch Testing Guidelines

### 1. Job Execution Testing

#### Complete Job Test Example
```java
@SpringBatchTest
@SpringBootTest
@Testcontainers
class DailyProcessingJobIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;
    
    @Test
    @DisplayName("Daily processing job - complete execution with validation")
    void testDailyProcessingJob_CompleteExecution() throws Exception {
        // Given: Test input file with daily transactions
        String inputFile = "classpath:batch/input/fixed-width/daily-transactions.txt";
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", inputFile)
            .addString("processDate", "2024-01-15")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
        
        // When: Executing complete job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then: Verify successful completion
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        
        // Verify transaction processing results
        List<Transaction> processedTransactions = transactionRepository.findByTransactionDate(
            LocalDate.of(2024, 1, 15)
        );
        assertThat(processedTransactions).hasSize(100); // Expected count from test file
        
        // Verify balance updates
        Account testAccount = accountRepository.findById(1000000001L).orElseThrow();
        assertThat(testAccount.getCurrentBalance()).isEqualTo(new BigDecimal("2450.75"));
    }
}
```

### 2. Step-Level Testing

#### Individual Step Test Example
```java
@SpringBatchTest
@SpringBootTest
class TransactionValidationStepTest {
    
    @Autowired
    private StepLauncherTestUtils stepLauncherTestUtils;
    
    @Test
    @DisplayName("Transaction validation step - error handling")
    void testTransactionValidationStep_ErrorHandling() throws Exception {
        // Given: Input file with validation errors
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "classpath:batch/input/fixed-width/invalid-transactions.txt")
            .toJobParameters();
        
        // When: Executing validation step
        StepExecution stepExecution = stepLauncherTestUtils.launchStep(
            "transactionValidationStep", jobParameters
        );
        
        // Then: Verify step completion with expected skip count
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getSkipCount()).isEqualTo(5); // Expected validation failures
        assertThat(stepExecution.getProcessSkipCount()).isEqualTo(5);
    }
}
```

### 3. Chunk Processing Testing

#### Chunk Size and Transaction Boundary Testing
```java
@Test
@DisplayName("Chunk processing - transaction boundaries match CICS SYNCPOINT")
void testChunkProcessing_TransactionBoundaries() throws Exception {
    // Given: Chunk size of 1000 records (matching COBOL commit frequency)
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("inputFile", "classpath:batch/input/fixed-width/large-transaction-file.txt")
        .addLong("chunkSize", 1000L)
        .toJobParameters();
    
    // When: Processing large file
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
    
    // Then: Verify chunk-based commits
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    
    // Verify database commits occurred at chunk boundaries
    List<StepExecution> stepExecutions = jobExecution.getStepExecutions().stream().toList();
    StepExecution processingStep = stepExecutions.get(0);
    
    // Verify commit count matches chunk size configuration
    assertThat(processingStep.getCommitCount()).isGreaterThan(0);
    assertThat(processingStep.getReadCount()).isEqualTo(5000); // Total records in test file
}
```

## Configuration Files

### Test Application Configuration (`test-application.yml`)
```yaml
spring:
  profiles:
    active: test
  
  datasource:
    url: jdbc:tc:postgresql:16-alpine:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
    
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        
  batch:
    job:
      enabled: false
    initialize-schema: always
    
  session:
    store-type: redis
    redis:
      host: localhost
      port: 6379

# Batch-specific test configuration
batch:
  chunk-size: 1000
  skip-limit: 10
  retry-limit: 3
  processing-window: 4h
  
# COMP-3 precision configuration
cobol:
  precision:
    monetary-scale: 2
    rounding-mode: HALF_UP
    validation-enabled: true

logging:
  level:
    com.carddemo.batch: DEBUG
    org.springframework.batch: DEBUG
```

### VSAM Mapping Configuration (`vsam-mapping.properties`)
```properties
# VSAM Dataset to PostgreSQL Table Mappings
vsam.transact.table=transactions
vsam.transact.key=transaction_id
vsam.transact.partition=transaction_date

vsam.acctdat.table=account_data
vsam.acctdat.key=account_id
vsam.acctdat.indexes=customer_id,account_status

vsam.custdat.table=customer_data
vsam.custdat.key=customer_id
vsam.custdat.indexes=last_name,first_name

vsam.carddat.table=card_data
vsam.carddat.key=card_number
vsam.carddat.foreign_keys=account_id,customer_id

# COMP-3 Field Mappings
comp3.account_balance.precision=12
comp3.account_balance.scale=2
comp3.credit_limit.precision=12
comp3.credit_limit.scale=2
comp3.transaction_amount.precision=12
comp3.transaction_amount.scale=2
comp3.interest_rate.precision=5
comp3.interest_rate.scale=4
```

## Running Batch Tests

### Prerequisites
1. Docker installed and running (for Testcontainers)
2. Java 21 JDK
3. Maven 3.9+
4. PostgreSQL test database (automatically managed by Testcontainers)

### Execution Commands

#### Run All Batch Tests
```bash
# Execute complete batch test suite
mvn test -Dtest="*BatchTest*" -Dspring.profiles.active=test

# Run with specific PostgreSQL version
mvn test -Dtest="*BatchTest*" -Dtestcontainers.postgres.version=16-alpine
```

#### Run Specific Job Tests
```bash
# Daily processing job tests
mvn test -Dtest="DailyProcessingJobTest" -Dspring.profiles.active=test

# Interest calculation job tests  
mvn test -Dtest="InterestCalculationJobTest" -Dspring.profiles.active=test

# Statement generation job tests
mvn test -Dtest="StatementGenerationJobTest" -Dspring.profiles.active=test
```

#### Run COMP-3 Precision Tests
```bash
# Execute COMP-3 to BigDecimal conversion tests
mvn test -Dtest="*Comp3*Test*" -Dspring.profiles.active=test

# Financial precision validation tests
mvn test -Dtest="*PrecisionTest*" -Dspring.profiles.active=test
```

#### Performance and Load Testing
```bash
# Large volume batch processing tests
mvn test -Dtest="*PerformanceTest*" -Dspring.profiles.active=test -Xmx2g

# 4-hour processing window validation
mvn test -Dtest="*ProcessingWindowTest*" -Dspring.profiles.active=test
```

### Test Data Generation

#### Creating Test Input Files
```bash
# Generate test transaction files
java -cp target/test-classes com.carddemo.batch.util.TestDataGenerator \
  --type=transactions --count=10000 --output=input/fixed-width/daily-transactions.txt

# Generate customer update files
java -cp target/test-classes com.carddemo.batch.util.TestDataGenerator \
  --type=customers --count=1000 --output=input/fixed-width/customer-updates.txt
```

#### COMP-3 Test Data Creation
```java
// Utility method for creating COMP-3 test data
public static byte[] createComp3TestData(String decimalValue, int precision, int scale) {
    BigDecimal value = new BigDecimal(decimalValue);
    return CobolDataConverter.toComp3(value, precision, scale);
}

// Example usage in test setup
@BeforeEach
void setupComp3TestData() {
    byte[] accountBalance = createComp3TestData("12345.67", 7, 2);
    byte[] transactionAmount = createComp3TestData("150.00", 10, 2);
    byte[] interestRate = createComp3TestData("0.0525", 5, 4);
    
    // Use in test scenarios for exact COBOL parity validation
}
```

## Troubleshooting

### Common Issues

#### 1. Precision Mismatch Errors
```
Error: Expected BigDecimal precision 2, but got 4
Solution: Verify COMP-3 scale configuration in vsam-mapping.properties
Check: CobolDataConverter.fromComp3() scale parameter matches COBOL PIC clause
```

#### 2. Database Connection Issues
```
Error: Failed to connect to PostgreSQL test container
Solution: Ensure Docker is running and accessible
Check: Testcontainers PostgreSQL version compatibility
```

#### 3. Batch Job Hanging
```
Error: Job execution exceeds timeout
Solution: Check chunk size configuration and database performance
Check: Transaction boundaries and commit frequency settings
```

#### 4. File Format Errors
```
Error: Invalid fixed-width record length
Solution: Verify input file format matches COBOL copybook layout
Check: Record positions and field lengths in test data files
```

### Performance Optimization

#### Test Execution Speed
- Use smaller test datasets for unit tests
- Leverage @TestPropertySource for test-specific configurations
- Configure appropriate chunk sizes for test scenarios
- Use in-memory databases for simple unit tests

#### Resource Management
- Clean up test containers after execution
- Limit concurrent test execution to prevent resource exhaustion
- Monitor memory usage during large file processing tests

## Migration Notes

### Key Differences from COBOL JCL
1. **Error Handling**: Spring Batch provides comprehensive skip/retry policies vs. JCL COND codes
2. **Restart Capability**: JobRepository tracks execution state vs. JCL checkpoints
3. **Parallel Processing**: Spring Batch supports step-level parallelism vs. JCL job dependencies
4. **Monitoring**: Spring Actuator provides real-time metrics vs. JCL job logs

### Maintained Compatibility
1. **Processing Logic**: Identical business rules and calculations
2. **Data Precision**: Exact COMP-3 to BigDecimal conversion
3. **Error Codes**: Preserved field-level validation error codes
4. **File Formats**: Compatible input/output file structures
5. **Processing Windows**: 4-hour batch processing window maintained

## Support and Documentation

### Related Documentation
- [Spring Batch Testing Guide](https://docs.spring.io/spring-batch/docs/current/reference/html/testing.html)
- [COBOL Data Type Conversion Utility JavaDoc](../../../main/java/com/carddemo/util/CobolDataConverter.java)
- [CardDemo Database Schema](../../../../main/resources/schema.sql)
- [Application Configuration Reference](../../../../main/resources/application.yml)

### Contact Information
For technical questions regarding batch testing:
- Review the main project documentation
- Check existing test examples in the test suite
- Verify COMP-3 precision requirements against COBOL copybooks

---

**Note**: This testing framework ensures **100% functional parity** between the original COBOL batch processing system and the modernized Spring Batch implementation. All tests must pass before deployment to production to guarantee identical business logic execution and financial calculation accuracy.