# Expected Output Files for Spring Batch Job Testing

## Overview

This directory contains expected output files used for validating Spring Batch job execution in the CardDemo application. These files serve as the baseline for byte-for-byte comparison testing to ensure that the modernized Spring Batch jobs produce identical outputs to the original COBOL batch programs.

## Purpose and Validation Methodology

### Critical Requirement: COBOL-to-Java Output Parity

The CardDemo modernization project requires **100% functional parity** between the original COBOL batch programs and the new Spring Batch implementations. This means:

- **Byte-for-byte identical output** for all file-based batch operations
- **Penny-level accuracy** for all financial calculations using BigDecimal precision
- **Exact field positioning** and data formatting matching original fixed-width layouts
- **Identical error handling** and exception scenarios

### Validation Process

1. **Baseline Generation**: Expected output files are generated from validated COBOL program execution using known test datasets
2. **Parallel Execution**: Spring Batch jobs process identical input data
3. **Automated Comparison**: Test framework performs byte-for-byte comparison between expected and actual outputs
4. **Precision Validation**: Special validation for BigDecimal calculations ensuring COBOL COMP-3 equivalence

## Expected Output File Categories

### 1. Daily Processing Job Outputs

Located in: `daily-processing/`

**Transaction Posting Results** (`transaction-posting-YYYYMMDD.txt`)
- **Purpose**: Validates posted transaction processing from DALYTRAN input files
- **Format**: Fixed-width, 189-character records matching CVTRA05Y copybook layout
- **Key Validations**:
  - Transaction ID sequence generation (10-digit numeric)
  - Amount precision (NUMERIC(12,2) matching COBOL COMP-3)
  - Date formatting (CCYYMMDD ISO format)
  - Account balance updates with penny-level accuracy

**Account Balance Updates** (`balance-updates-YYYYMMDD.txt`)
- **Purpose**: Confirms account balance calculations after transaction posting
- **Format**: Fixed-width, 289-character records matching CVACT01Y copybook layout
- **Key Validations**:
  - Current balance precision (NUMERIC(12,2) with exact decimal placement)
  - Credit limit utilization calculations
  - Transaction count aggregations
  - Cross-reference integrity with CARDXREF

**Error Report** (`posting-errors-YYYYMMDD.txt`)
- **Purpose**: Documents rejected transactions and validation failures
- **Format**: Variable-length error records with standardized error codes
- **Key Validations**:
  - Error code consistency with COBOL ABEND handling
  - Field-level validation error messages
  - Referential integrity violation reporting

### 2. Interest Calculation Job Outputs

Located in: `interest-calculation/`

**Interest Calculation Results** (`interest-calc-YYYYMMDD.txt`)
- **Purpose**: Validates monthly interest calculations using disclosure group rates
- **Format**: Fixed-width records with BigDecimal precision fields
- **Key Validations**:
  - Interest rate application accuracy (4 decimal places)
  - Compounding calculation precision matching COBOL arithmetic
  - Account balance adjustments with exact rounding behavior
  - Category balance distribution calculations

**Interest Transaction Generation** (`interest-trans-YYYYMMDD.txt`)
- **Purpose**: Confirms generation of interest posting transactions
- **Format**: Standard transaction record layout (189 characters)
- **Key Validations**:
  - Transaction type code assignment (INTEREST = 'IT')
  - Amount calculations matching manual verification
  - Account ID and card number cross-references
  - Transaction date and time stamp accuracy

### 3. Statement Generation Job Outputs

Located in: `statement-generation/`

**Customer Statements - Plain Text** (`statements-text-YYYYMMDD.txt`)
- **Purpose**: Validates formatted customer statement generation
- **Format**: Multi-line customer statement with fixed positioning
- **Key Validations**:
  - Customer information formatting and positioning
  - Transaction history chronological ordering
  - Balance calculations and running totals
  - Fee and interest charge computations

**Customer Statements - HTML** (`statements-html-YYYYMMDD.html`)
- **Purpose**: Confirms HTML-formatted statement generation
- **Format**: Valid HTML with embedded CSS for customer presentation
- **Key Validations**:
  - Data consistency with plain text version
  - HTML structure and formatting compliance
  - Calculated field accuracy in presentation format

### 4. Report Generation Job Outputs

Located in: `report-generation/`

**Daily Transaction Report** (`daily-report-YYYYMMDD.txt`)
- **Purpose**: Validates daily transaction summary reporting
- **Format**: Formatted report with totals and subtotals
- **Key Validations**:
  - Transaction count aggregations by type
  - Amount totals with currency formatting
  - Date range filtering accuracy
  - Report footer calculations

**Audit Trail Report** (`audit-trail-YYYYMMDD.txt`)
- **Purpose**: Confirms audit trail generation for compliance
- **Format**: Detailed audit log with user actions and timestamps
- **Key Validations**:
  - User identification and action logging
  - Timestamp accuracy and timezone handling
  - Before/after value comparisons
  - Regulatory compliance field requirements

## Data Format Specifications

### Fixed-Width Format Guidelines

All expected output files maintain strict fixed-width formatting to match original COBOL program outputs:

**Character Field Formatting**:
- Left-justified with trailing spaces
- No truncation of data
- Consistent field width across all records

**Numeric Field Formatting**:
- Right-justified with leading zeros or spaces as appropriate
- Decimal points explicitly positioned (not implied)
- Negative amounts with trailing sign or explicit formatting

**Date Field Formatting**:
- CCYYMMDD format for dates (8 characters)
- HHMMSS format for times (6 characters)
- Timestamp fields as CCYYMMDDHHMMSS (14 characters)

### BigDecimal Precision Requirements

**COBOL COMP-3 Equivalence**:
All monetary amounts must maintain exact precision equivalent to COBOL COMP-3 packed decimal fields:

- **Account Balances**: `PIC S9(10)V99 COMP-3` → `NUMERIC(12,2)` with scale=2, precision=12
- **Transaction Amounts**: `PIC S9(7)V99 COMP-3` → `NUMERIC(9,2)` with scale=2, precision=9
- **Interest Rates**: `PIC S9(1)V9999 COMP-3` → `NUMERIC(5,4)` with scale=4, precision=5
- **Credit Limits**: `PIC S9(10)V99 COMP-3` → `NUMERIC(12,2)` with scale=2, precision=12

**Rounding Behavior**:
All calculations must use `RoundingMode.HALF_UP` to match COBOL ROUNDED clause behavior.

## Test Execution Instructions

### Running Batch Job Tests

1. **Environment Setup**:
   ```bash
   # Ensure test database is populated with baseline data
   ./gradlew test -Dspring.profiles.active=batch-test
   ```

2. **Execute Specific Job Tests**:
   ```bash
   # Test daily processing job
   ./gradlew test --tests *DailyProcessingJobTest
   
   # Test interest calculation job
   ./gradlew test --tests *InterestCalculationJobTest
   
   # Test statement generation job
   ./gradlew test --tests *StatementGenerationJobTest
   ```

3. **Batch Test Suite**:
   ```bash
   # Execute all batch job tests
   ./gradlew test --tests *BatchJobTest*
   ```

### Validation Framework

The test framework automatically:
- Loads expected output files from this directory
- Executes Spring Batch jobs with identical input data
- Performs byte-for-byte comparison of actual vs expected outputs
- Reports any discrepancies with detailed diff analysis
- Validates BigDecimal precision for all financial calculations

### Test Data Dependencies

**Required Test Data Files**:
- `src/test/resources/batch/input/test-customer-data.txt`
- `src/test/resources/batch/input/test-account-data.txt`
- `src/test/resources/batch/input/test-transaction-data.txt`
- `src/test/resources/batch/input/test-daily-transactions.txt`

**Database State Requirements**:
- Clean test database with known baseline records
- Reference data tables populated with test configuration
- User security records for audit trail testing
- Disclosure group records for interest rate testing

## Expected Output Validation Examples

### Financial Calculation Precision Validation

**Example: Interest Calculation Verification**
```
Expected Output (COBOL COMP-3):
Account: 1234567890
Principal: 1000.00
Rate: 0.0525 (5.25% annual)
Monthly Interest: 4.38 (calculated with COBOL ROUNDED)

Validation Requirements:
- Java BigDecimal calculation must produce exactly 4.38
- Scale must be 2 decimal places
- Precision must match COBOL packed decimal behavior
- Rounding mode must be HALF_UP
```

**Example: Account Balance Update Verification**
```
Expected Output Format:
Account ID: 1234567890 (10 digits, zero-padded)
Previous Balance: 00000125067 (11 digits, implied 2 decimal places)
Transaction Amount: 00000002500 (11 digits, implied 2 decimal places)
New Balance: 00000127567 (11 digits, implied 2 decimal places)

Validation Requirements:
- Exact field positioning in fixed-width record
- Proper zero-padding for numeric fields
- Correct decimal point implications
- Balance arithmetic accuracy
```

### Error Handling Validation

**Example: Invalid Transaction Rejection**
```
Expected Error Output:
Error Code: TXN001
Error Message: Invalid account number: 9999999999
Transaction ID: 2024010100001
Timestamp: 20240101120000
Field Name: ACCOUNT_ID
Field Value: 9999999999

Validation Requirements:
- Error code consistency with COBOL error handling
- Message text matching original system
- Proper timestamp formatting
- Complete error context information
```

## Maintaining Expected Output Files

### When to Update Expected Outputs

Expected output files should be updated only when:

1. **Legitimate Business Logic Changes**: When authorized business rule modifications require output format changes
2. **Data Format Enhancements**: When approved improvements to output formatting are implemented
3. **Regulatory Requirement Changes**: When compliance requirements mandate output modifications
4. **COBOL Program Updates**: When the reference COBOL programs are officially modified

### Update Process

1. **Validation Required**:
   - Business analyst approval for output changes
   - Technical lead review of format modifications
   - QA verification of updated baseline accuracy

2. **Regeneration Steps**:
   ```bash
   # Regenerate expected outputs from validated COBOL execution
   ./scripts/generate-expected-outputs.sh
   
   # Verify new outputs against business requirements
   ./scripts/validate-output-changes.sh
   
   # Update test baselines
   git add backend/src/test/resources/batch/expected/
   git commit -m "Update expected batch outputs - [TICKET-NUMBER]"
   ```

3. **Documentation Updates**:
   - Update this README with any format changes
   - Document business justification for modifications
   - Update test validation requirements as needed

### Validation Checksums

Each expected output file includes a validation checksum to ensure file integrity:

```
# Example checksum validation
echo "Expected checksum verification..."
sha256sum daily-processing/transaction-posting-20240101.txt
# Expected: a1b2c3d4e5f6... (documented in test metadata)
```

## Troubleshooting Common Issues

### BigDecimal Precision Mismatches

**Symptom**: Test failures related to decimal calculation differences

**Solution**:
1. Verify BigDecimal scale and precision configuration
2. Check RoundingMode matches COBOL ROUNDED behavior
3. Validate COMP-3 to BigDecimal conversion utilities
4. Review arithmetic operation sequence

### File Format Differences

**Symptom**: Byte-for-byte comparison failures in fixed-width output

**Solution**:
1. Verify field positioning and padding
2. Check character encoding (UTF-8 vs EBCDIC considerations)
3. Validate line ending consistency (LF vs CRLF)
4. Review data type formatting rules

### Test Data Inconsistencies

**Symptom**: Unexpected output variations with identical inputs

**Solution**:
1. Verify test database state initialization
2. Check for data dependencies between test executions
3. Validate input file loading procedures
4. Review transaction isolation in test framework

## Performance Expectations

### Batch Job Execution Targets

- **Daily Processing Job**: Complete within 4-hour window (matches mainframe requirement)
- **Interest Calculation Job**: Complete within 2-hour window
- **Statement Generation Job**: Complete within 3-hour window
- **Report Generation Job**: Complete within 1-hour window

### Validation Performance

- **File Comparison**: <1 second per MB of output data
- **BigDecimal Validation**: <10ms per financial calculation
- **Database Verification**: <5 seconds per batch job test
- **Complete Test Suite**: <30 minutes for full batch validation

## Compliance and Audit Requirements

### Regulatory Documentation

All expected output files serve as compliance documentation for:
- **SOX Section 404**: Financial data integrity validation
- **PCI DSS**: Transaction processing accuracy requirements
- **Banking Regulations**: Audit trail and reporting compliance
- **GDPR Article 5**: Data accuracy and consistency requirements

### Audit Trail Preservation

- All expected output modifications are version controlled
- Business approval documentation maintained for output changes
- Test execution logs retained for compliance reporting
- Validation result archives maintained for audit review

---

**Note**: This documentation is maintained as part of the CardDemo application test suite. For questions or updates, contact the development team or refer to the project's technical specification documentation.