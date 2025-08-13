# Batch Input Test Data Files Documentation

## Overview

This directory contains test input files for Spring Batch processing operations within the CardDemo application. These files replicate production data formats converted from mainframe VSAM datasets to support comprehensive testing of the COBOL-to-Java migration while maintaining exact data precision and field layout compatibility.

The test files follow fixed-width ASCII format patterns derived from original COBOL copybook structures, ensuring that Spring Batch FlatFileItemReader components process test data using identical field mapping and validation logic as production systems.

## File Types and Purposes

### Core Entity Test Files

| File Name | Purpose | COBOL Copybook Source | Record Count | Spring Batch Job |
|-----------|---------|----------------------|--------------|------------------|
| `acctdata.txt` | Account data testing | `CVACT01Y.cpy` | 50 records | DailyTransactionJob |
| `custdata.txt` | Customer profile testing | `CUSTREC.cpy` | 100 records | CustomerProcessingJob |
| `carddata.txt` | Credit card testing | `CVACT02Y.cpy` | 75 records | CardProcessingJob |
| `dailytran.txt` | Daily transaction testing | `CVTRA06Y.cpy` | 200 records | TransactionPostingJob |
| `cardxref.txt` | Cross-reference testing | `CVACT03Y.cpy` | 75 records | ValidationJob |

### Reference Data Test Files

| File Name | Purpose | COBOL Copybook Source | Record Count | Usage |
|-----------|---------|----------------------|--------------|-------|
| `trancatg.txt` | Transaction categories | `CVTRA04Y.cpy` | 25 records | Category validation |
| `trantype.txt` | Transaction types | `CVTRA03Y.cpy` | 15 records | Type validation |
| `discgrp.txt` | Disclosure groups | `CVTRA02Y.cpy` | 10 records | Interest calculation |
| `usrsec.txt` | User security data | `CSUSR01Y.cpy` | 20 records | Authentication testing |

## Fixed-Width Format Specifications

### Account Data Format (acctdata.txt)

Based on COBOL copybook `CVACT01Y.cpy` with PostgreSQL column mappings:

```
Position  Length  COBOL Picture     Java Type        PostgreSQL Type   Description
1-10      10      PIC 9(10)        Long             BIGINT           Account ID
11-20     10      PIC 9(10)        Long             BIGINT           Customer ID  
21-21     1       PIC X(1)         String           CHAR(1)          Active Status
22-33     12      PIC S9(10)V99    BigDecimal       NUMERIC(12,2)    Current Balance
                  COMP-3           
34-45     12      PIC S9(10)V99    BigDecimal       NUMERIC(12,2)    Credit Limit
                  COMP-3
46-57     12      PIC S9(10)V99    BigDecimal       NUMERIC(12,2)    Cash Credit Limit
                  COMP-3
58-65     8       PIC 9(8)         LocalDate        DATE             Open Date (YYYYMMDD)
66-73     8       PIC 9(8)         LocalDate        DATE             Expiration Date
74-81     8       PIC 9(8)         LocalDate        DATE             Reissue Date
82-93     12      PIC S9(10)V99    BigDecimal       NUMERIC(12,2)    Current Cycle Credit
                  COMP-3
94-105    12      PIC S9(10)V99    BigDecimal       NUMERIC(12,2)    Current Cycle Debit
                  COMP-3
106-115   10      PIC X(10)        String           VARCHAR(10)      ZIP Code
116-125   10      PIC X(10)        String           VARCHAR(10)      Group ID
126-135   10      PIC 9(10)        Long             BIGINT           Disclosure Group ID
```

### Customer Data Format (custdata.txt)

Based on COBOL copybook `CUSTREC.cpy`:

```
Position  Length  COBOL Picture     Java Type        PostgreSQL Type   Description
1-10      10      PIC 9(10)        Long             BIGINT           Customer ID
11-30     20      PIC X(20)        String           VARCHAR(20)      First Name
31-50     20      PIC X(20)        String           VARCHAR(20)      Middle Name
51-70     20      PIC X(20)        String           VARCHAR(20)      Last Name
71-120    50      PIC X(50)        String           VARCHAR(50)      Address Line 1
121-170   50      PIC X(50)        String           VARCHAR(50)      Address Line 2
171-220   50      PIC X(50)        String           VARCHAR(50)      Address Line 3
221-222   2       PIC X(2)         String           VARCHAR(2)       State Code
223-225   3       PIC X(3)         String           VARCHAR(3)       Country Code
226-235   10      PIC X(10)        String           VARCHAR(10)      ZIP Code
236-250   15      PIC X(15)        String           VARCHAR(15)      Phone Number 1
251-265   15      PIC X(15)        String           VARCHAR(15)      Phone Number 2
266-274   9       PIC X(9)         String           VARCHAR(9)       SSN (Encrypted)
275-294   20      PIC X(20)        String           VARCHAR(20)      Government ID
295-302   8       PIC 9(8)         LocalDate        DATE             Date of Birth
303-312   10      PIC X(10)        String           VARCHAR(10)      EFT Account ID
313-313   1       PIC X(1)         String           CHAR(1)          Primary Card Holder
314-318   5       PIC 9(5)         Integer          SMALLINT         FICO Score
```

### Daily Transaction Format (dailytran.txt)

Based on COBOL copybook `CVTRA06Y.cpy`:

```
Position  Length  COBOL Picture     Java Type        PostgreSQL Type   Description
1-10      10      PIC 9(10)        Long             BIGINT           Transaction ID
11-20     10      PIC 9(10)        Long             BIGINT           Account ID
21-36     16      PIC X(16)        String           VARCHAR(16)      Card Number
37-44     8       PIC 9(8)         LocalDate        DATE             Transaction Date
45-50     6       PIC 9(6)         LocalTime        TIME             Transaction Time
51-62     12      PIC S9(10)V99    BigDecimal       NUMERIC(12,2)    Amount
                  COMP-3
63-64     2       PIC X(2)         String           VARCHAR(2)       Transaction Type Code
65-68     4       PIC X(4)         String           VARCHAR(4)       Category Code
69-168    100     PIC X(100)       String           VARCHAR(100)     Description
169-218   50      PIC X(50)        String           VARCHAR(50)      Merchant Name
```

## COBOL to Java Data Type Mappings

### Numeric Field Conversions

#### COMP-3 Packed Decimal to BigDecimal

COBOL COMP-3 fields require precise decimal conversion to maintain financial accuracy:

```java
// CobolDataConverter.java utility class
public class CobolDataConverter {
    
    /**
     * Convert COBOL COMP-3 packed decimal to Java BigDecimal
     * Maintains exact precision from original COBOL picture clause
     */
    public static BigDecimal fromComp3(String value, int scale) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Remove spaces and validate numeric content
        String cleanValue = value.trim().replace("+", "").replace(" ", "0");
        
        try {
            BigDecimal decimal = new BigDecimal(cleanValue);
            return decimal.movePointLeft(scale)
                          .setScale(scale, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new DataConversionException(
                "Invalid COMP-3 value: " + value, e);
        }
    }
    
    /**
     * Convert fixed-width monetary field to BigDecimal
     * Example: "000001234567" with scale 2 becomes 12345.67
     */
    public static BigDecimal parseMonetaryField(String field, int scale) {
        return fromComp3(field, scale);
    }
}
```

#### Date Field Conversions

COBOL date fields in YYYYMMDD format:

```java
public static LocalDate parseCobolDate(String dateField) {
    if (dateField == null || dateField.trim().isEmpty() || 
        dateField.equals("00000000")) {
        return null;
    }
    
    String cleanDate = dateField.trim();
    if (cleanDate.length() != 8) {
        throw new DataConversionException("Invalid date format: " + dateField);
    }
    
    try {
        int year = Integer.parseInt(cleanDate.substring(0, 4));
        int month = Integer.parseInt(cleanDate.substring(4, 6));
        int day = Integer.parseInt(cleanDate.substring(6, 8));
        
        return LocalDate.of(year, month, day);
    } catch (Exception e) {
        throw new DataConversionException("Cannot parse date: " + dateField, e);
    }
}
```

## Spring Batch FlatFileItemReader Configuration

### Basic File Reader Configuration

```java
@Bean
public FlatFileItemReader<AccountData> accountDataReader() {
    return new FlatFileItemReaderBuilder<AccountData>()
        .name("accountDataReader")
        .resource(new ClassPathResource("batch/input/acctdata.txt"))
        .fixedLength()
        .columns(new Range(1, 10),    // Account ID
                new Range(11, 20),    // Customer ID  
                new Range(21, 21),    // Active Status
                new Range(22, 33),    // Current Balance
                new Range(34, 45),    // Credit Limit
                new Range(46, 57),    // Cash Credit Limit
                new Range(58, 65),    // Open Date
                new Range(66, 73),    // Expiration Date
                new Range(74, 81),    // Reissue Date
                new Range(82, 93),    // Current Cycle Credit
                new Range(94, 105),   // Current Cycle Debit
                new Range(106, 115),  // ZIP Code
                new Range(116, 125),  // Group ID
                new Range(126, 135))  // Disclosure Group ID
        .names("accountId", "customerId", "activeStatus", 
               "currentBalance", "creditLimit", "cashCreditLimit",
               "openDate", "expirationDate", "reissueDate",
               "currentCycleCredit", "currentCycleDebit", 
               "zipCode", "groupId", "disclosureGroupId")
        .fieldSetMapper(new AccountDataFieldSetMapper())
        .build();
}
```

### Custom FieldSetMapper Implementation

```java
public class AccountDataFieldSetMapper implements FieldSetMapper<AccountData> {
    
    @Override
    public AccountData mapFieldSet(FieldSet fieldSet) throws BindException {
        AccountData account = new AccountData();
        
        account.setAccountId(fieldSet.readLong("accountId"));
        account.setCustomerId(fieldSet.readLong("customerId"));
        account.setActiveStatus(fieldSet.readString("activeStatus"));
        
        // Use CobolDataConverter for monetary fields
        account.setCurrentBalance(
            CobolDataConverter.parseMonetaryField(
                fieldSet.readString("currentBalance"), 2));
        account.setCreditLimit(
            CobolDataConverter.parseMonetaryField(
                fieldSet.readString("creditLimit"), 2));
        account.setCashCreditLimit(
            CobolDataConverter.parseMonetaryField(
                fieldSet.readString("cashCreditLimit"), 2));
        
        // Date field conversions
        account.setOpenDate(
            CobolDataConverter.parseCobolDate(
                fieldSet.readString("openDate")));
        account.setExpirationDate(
            CobolDataConverter.parseCobolDate(
                fieldSet.readString("expirationDate")));
        
        account.setZipCode(fieldSet.readString("zipCode"));
        account.setGroupId(fieldSet.readString("groupId"));
        account.setDisclosureGroupId(fieldSet.readLong("disclosureGroupId"));
        
        return account;
    }
}
```

### Transaction File Reader with Validation

```java
@Bean
public FlatFileItemReader<DailyTransaction> dailyTransactionReader() {
    return new FlatFileItemReaderBuilder<DailyTransaction>()
        .name("dailyTransactionReader")
        .resource(new ClassPathResource("batch/input/dailytran.txt"))
        .fixedLength()
        .columns(new Range(1, 10),    // Transaction ID
                new Range(11, 20),    // Account ID
                new Range(21, 36),    // Card Number
                new Range(37, 44),    // Transaction Date
                new Range(45, 50),    // Transaction Time
                new Range(51, 62),    // Amount
                new Range(63, 64),    // Transaction Type Code
                new Range(65, 68),    // Category Code
                new Range(69, 168),   // Description
                new Range(169, 218))  // Merchant Name
        .names("transactionId", "accountId", "cardNumber",
               "transactionDate", "transactionTime", "amount",
               "transactionTypeCode", "categoryCode", 
               "description", "merchantName")
        .fieldSetMapper(new DailyTransactionFieldSetMapper())
        .lineTokenizer(new FixedWidthTokenizer())
        .build();
}
```

## Test Data Generation Guidelines

### Creating New Test Records

When adding new test data records, follow these guidelines:

#### 1. Maintain Data Relationships

Ensure referential integrity across test files:

```
Account ID 4000000001 in acctdata.txt
  └── Must have Customer ID 1000000001 in custdata.txt
  └── May have Card Number 4000000000000001 in carddata.txt
      └── Must have matching entry in cardxref.txt
```

#### 2. Use Consistent ID Patterns

Follow these ID generation patterns for test data:

- **Customer IDs**: 1000000001-1000000100 (sequential)
- **Account IDs**: 4000000001-4000000050 (sequential)  
- **Card Numbers**: 4000000000000001-4000000000000075 (MOD-10 valid)
- **Transaction IDs**: 9000000001-9000000200 (sequential)

#### 3. Monetary Field Formatting

All monetary fields must be right-aligned with leading zeros:

```
Correct:   "000001234567" (represents $12,345.67)
Incorrect: "12345.67    " 
Incorrect: "   12345.67 "
```

#### 4. Date Field Formatting

Use YYYYMMDD format with no separators:

```
Correct:   "20240115" (January 15, 2024)
Incorrect: "2024-01-15"
Incorrect: "01/15/2024"
```

### Sample Record Templates

#### Account Data Template
```
4000000999100000999Y000012345670000500000000001000020240115202412152024121500000000000000000012345590210     GROUP001  0000000001
```

#### Customer Data Template  
```
1000000999John                Middle              Smith               123 Main Street                          Apt 4B                                    City Name                                 CA USA9999912345 555-1234       555-5678       123456789Government123456789920240101  1234567890Y  750
```

#### Transaction Data Template
```
9000000999400000999940000000000000012024011512345600012345671601PURCHASE                                                                            MERCHANT NAME                            
```

## Data Validation Rules

### Business Rule Validation

Test data must comply with business validation rules:

#### Account Data Rules
- Active Status: Must be 'Y' or 'N'
- Balance fields: Cannot exceed credit limits
- Dates: Open date must be <= current date
- ZIP codes: Must be valid 5 or 10 digit format
- FICO scores: Must be between 300-850

#### Transaction Data Rules
- Amount: Must be positive for debits, negative for credits
- Transaction codes: Must exist in trantype.txt
- Category codes: Must exist in trancatg.txt
- Card/Account: Must match entries in cardxref.txt

### Cross-Reference Validation

Implement validation across multiple files:

```java
@Component
public class TestDataValidator {
    
    public void validateAccountCustomerRelationship(
            List<AccountData> accounts, 
            List<CustomerData> customers) {
        
        Set<Long> customerIds = customers.stream()
            .map(CustomerData::getCustomerId)
            .collect(Collectors.toSet());
            
        for (AccountData account : accounts) {
            if (!customerIds.contains(account.getCustomerId())) {
                throw new ValidationException(
                    "Account " + account.getAccountId() + 
                    " references non-existent customer " + 
                    account.getCustomerId());
            }
        }
    }
}
```

## Batch Job Testing Strategies

### Unit Testing Approach

Test individual batch components:

```java
@SpringBatchTest
@SpringBootTest
class AccountDataReaderTest {
    
    @Autowired
    private TestExecutionContext executionContext;
    
    @Test
    void testAccountDataReader() throws Exception {
        FlatFileItemReader<AccountData> reader = accountDataReader();
        reader.open(executionContext);
        
        AccountData account = reader.read();
        assertThat(account).isNotNull();
        assertThat(account.getAccountId()).isEqualTo(4000000001L);
        assertThat(account.getCurrentBalance())
            .isEqualByComparingTo(new BigDecimal("12345.67"));
    }
}
```

### Integration Testing

Test complete batch job flows:

```java
@SpringBatchTest  
@SpringBootTest
class DailyTransactionJobTest {
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Test
    void testDailyTransactionProcessing() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "batch/input/dailytran.txt")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
            
        JobExecution jobExecution = jobLauncherTestUtils
            .launchJob(jobParameters);
            
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions()).hasSize(3);
    }
}
```

## Maintenance Procedures

### Updating Test Data

When production data structures change:

1. **Update Copybook Mappings**: Modify field position mappings in README
2. **Regenerate Test Files**: Use data generation scripts to create new test files
3. **Update FieldSetMappers**: Adjust Spring Batch mapper classes
4. **Validate Data Relationships**: Ensure cross-reference integrity maintained
5. **Run Regression Tests**: Execute full test suite to verify compatibility

### Monitoring Test Data Quality

Implement data quality checks:

```java
@Component  
public class TestDataQualityChecker {
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateTestDataQuality() {
        // Check file existence
        validateFileExists("batch/input/acctdata.txt");
        validateFileExists("batch/input/custdata.txt");
        
        // Validate record counts
        validateRecordCount("batch/input/acctdata.txt", 50);
        validateRecordCount("batch/input/custdata.txt", 100);
        
        // Check data relationships
        validateCrossReferences();
    }
}
```

### Performance Considerations

Optimize test data for performance testing:

- **Large Volume Testing**: Generate files with 10,000+ records for stress testing
- **Concurrent Processing**: Test multiple batch jobs running simultaneously  
- **Memory Usage**: Monitor memory consumption with large file processing
- **Database Load**: Test PostgreSQL performance with bulk inserts

## File Management Best Practices

### Version Control

- Keep test files in version control with the application code
- Use meaningful commit messages when updating test data
- Tag test data versions that correspond to application releases

### File Organization

```
backend/src/test/resources/batch/input/
├── README.md                 # This documentation
├── core/                     # Core entity files
│   ├── acctdata.txt         # Account test data
│   ├── custdata.txt         # Customer test data  
│   ├── carddata.txt         # Card test data
│   └── cardxref.txt         # Cross-reference data
├── transactions/            # Transaction files
│   ├── dailytran.txt        # Daily transactions
│   └── monthlytran.txt      # Monthly aggregates
├── reference/               # Reference data
│   ├── trancatg.txt        # Transaction categories
│   ├── trantype.txt        # Transaction types
│   ├── discgrp.txt         # Disclosure groups
│   └── usrsec.txt          # User security
└── samples/                 # Sample records for reference
    ├── account_sample.txt   # Sample account record
    ├── customer_sample.txt  # Sample customer record
    └── transaction_sample.txt # Sample transaction record
```

This comprehensive documentation ensures that test data files maintain consistency with COBOL data patterns while supporting thorough testing of the Spring Batch migration from mainframe VSAM datasets to PostgreSQL database operations.