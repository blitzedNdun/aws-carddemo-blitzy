# VSAM-to-PostgreSQL Migration Testing Framework

## Overview

This document provides comprehensive guidance for validating the migration from IBM mainframe VSAM KSDS datasets to PostgreSQL relational database tables. The migration testing framework ensures 100% functional parity between the original COBOL/CICS/VSAM implementation and the modernized Java 21/Spring Boot/PostgreSQL architecture.

## Critical Migration Goals

### Primary Objective
**Minimal-Change Functional Parity**: The migration must preserve identical business logic, data precision, and user experience without introducing new functionality or optimizations. Every COBOL program paragraph must have an equivalent Java method with identical output.

### Key Success Metrics
- **Response Time Parity**: REST API endpoints must meet <200ms (95th percentile) response time requirement
- **Financial Calculation Accuracy**: 100% precision match for all monetary calculations between COBOL COMP-3 and Java BigDecimal
- **Data Integrity**: Byte-for-byte identical results for all transaction processing scenarios
- **Batch Processing Window**: Complete daily batch processing within 4-hour window constraint

## VSAM-to-PostgreSQL Conversion Process

### 1. Data Model Transformation

The migration replaces VSAM KSDS (Key Sequenced Data Sets) with PostgreSQL relational tables while preserving identical access patterns and data relationships:

| VSAM Dataset | PostgreSQL Table | Primary Key Structure | Access Pattern |
|--------------|------------------|----------------------|----------------|
| USRSEC | user_security | user_id (BIGINT) | Authentication lookups |
| ACCTDAT | account_data | account_id (BIGINT) | Account operations |
| CUSTDAT | customer_data | customer_id (BIGINT) | Customer management |
| CARDDAT | card_data | card_number (VARCHAR(16)) | Credit card operations |
| TRANSACT | transactions | transaction_id (BIGINT) | Transaction processing |
| TCATBAL | transaction_category_balance | Composite: (account_id, category_code, balance_date) | Balance management |
| DISCGRP | disclosure_groups | disclosure_group_id (BIGINT) | Interest rate configuration |
| TRANTYPE | transaction_types | transaction_type_code (VARCHAR(2)) | Transaction classification |
| TRANCATG | transaction_categories | Composite: (category_code, subcategory_code) | Category management |

### 2. Index Strategy Migration

VSAM alternate indexes are replaced with PostgreSQL B-tree indexes:

- **CARDAIX → card_account_idx**: Account-based card lookup operations
- **CXACAIX → customer_account_idx**: Customer-account relationship queries
- **Date Range Access → transaction_date_idx**: Date-range queries with partition pruning
- **Name Search → customer_name_idx**: Customer search operations

### 3. Data Type Conversion Rules

#### COBOL to PostgreSQL Type Mappings

| COBOL Data Type | PostgreSQL Equivalent | Java Type | Conversion Notes |
|-----------------|----------------------|-----------|------------------|
| PIC 9(9) (Customer ID) | BIGINT | Long | Direct numeric conversion |
| PIC X(n) (Character fields) | VARCHAR(n) | String | UTF-8 encoding from EBCDIC |
| PIC S9(10)V99 COMP-3 (Monetary) | NUMERIC(12,2) | BigDecimal | **CRITICAL**: Exact precision preservation |
| PIC X(1) (Status flags) | CHAR(1) | Character | Direct character mapping |
| PIC 9(8) (YYYYMMDD dates) | DATE | LocalDate | ISO 8601 format conversion |
| PIC 9(6) (HHMMSS times) | TIME | LocalTime | Time component extraction |

#### COMP-3 to BigDecimal Precision Handling

**Critical Implementation**: The `CobolDataConverter` utility class ensures exact monetary precision:

```java
// Example validation pattern for COMP-3 conversion
BigDecimal cobolAmount = CobolDataConverter.fromComp3(packedBytes, 2);
BigDecimal javaAmount = new BigDecimal("123.45").setScale(2, RoundingMode.HALF_UP);
assertEquals("COMP-3 conversion must match exactly", cobolAmount, javaAmount);
```

**Rounding Mode Configuration**: All BigDecimal operations use `RoundingMode.HALF_UP` to match COBOL ROUNDED clause behavior.

## Migration Test Resources Structure

### Test Data Files (app/data/ASCII/)

These fixed-width ASCII files serve as the source data for migration validation:

1. **acctdata.txt**: Account master data with balance fields
2. **custdata.txt**: Customer profile information 
3. **carddata.txt**: Credit card records with security fields
4. **dailytran.txt**: Daily transaction records for batch processing
5. **discgrp.txt**: Interest rate and disclosure group definitions
6. **tcatbal.txt**: Transaction category balance records
7. **transact.txt**: Historical transaction data
8. **trantype.txt**: Transaction type classification
9. **trancatg.txt**: Transaction category definitions

### Spring Batch ETL Configuration

The migration uses Spring Batch 5.0.x with chunk-based processing:

```yaml
# Example Spring Batch configuration
spring:
  batch:
    job:
      enabled: true
    initialize-schema: always
    
migration:
  chunk-size: 1000
  commit-interval: 5000
  parallel-threads: 4
  error-threshold: 10
```

### JPA Entity Validation

Each PostgreSQL table is mapped to JPA entities with composite key support:

```java
// Example composite key validation
@Entity
@Table(name = "transaction_category_balance")
public class TransactionCategoryBalance {
    @EmbeddedId
    private TransactionCategoryBalanceKey id;
    
    @Column(name = "balance", precision = 12, scale = 2)
    private BigDecimal balance;
}
```

## Running Migration Validation Tests

### Prerequisites

1. **Environment Setup**:
   - Java 21 LTS runtime
   - PostgreSQL 15.x cluster
   - Redis 7.x for session management
   - Docker/Kubernetes for containerization

2. **Test Dependencies**:
   - JUnit 5 for unit testing framework
   - Testcontainers for PostgreSQL integration testing
   - AssertJ for fluent assertion syntax
   - Mockito for service layer mocking

### Execution Workflow

#### 1. Unit Testing Phase

**Target**: Validate individual COBOL-to-Java method conversions

```bash
# Run all unit tests for service layer components
./mvnw test -Dtest=**/*ServiceTest

# Run specific migration validation tests
./mvnw test -Dtest=MigrationValidationTest
```

**Coverage Requirements**:
- ≥80% code coverage on all service classes
- 100% coverage on financial calculation methods
- Complete validation of all COBOL paragraph equivalents

#### 2. Integration Testing Phase

**Target**: Validate database operations and REST endpoints

```bash
# Run integration tests with Testcontainers PostgreSQL
./mvnw test -Dtest=**/*IntegrationTest -Dspring.profiles.active=test

# Validate REST API endpoints against CICS transaction equivalents
./mvnw test -Dtest=**/*ControllerTest
```

**Validation Points**:
- REST endpoint response times <200ms
- Session state management via Redis
- Transaction boundary preservation
- Cross-reference validation accuracy

#### 3. End-to-End Testing Phase

**Target**: Complete workflow validation through React UI

```bash
# Run Cypress E2E tests
npm run test:e2e

# Validate specific user workflows
npm run cypress:run --spec="cypress/integration/account-management.spec.js"
```

**Critical Scenarios**:
- Sign-on authentication flow
- Account management operations
- Credit card lifecycle management
- Transaction processing workflows
- Batch processing validation

#### 4. Performance Testing Phase

**Target**: Validate response time and throughput requirements

```bash
# JMeter load testing
jmeter -n -t performance-tests/load-test.jmx -l results/load-test-results.jtl

# K6 stress testing for REST endpoints
k6 run performance-tests/api-stress-test.js
```

**Performance Metrics**:
- REST API response time <200ms (95th percentile)
- Concurrent user support (1000+ users)
- Batch processing completion within 4-hour window
- Database query optimization validation

## Parallel Run Comparison Methodology

### Automated Comparison Framework

The migration includes a comprehensive parallel run capability to validate identical behavior between COBOL and Java implementations:

#### 1. Transaction-Level Comparison

```java
@Test
public void validateTransactionProcessingParity() {
    // Execute COBOL transaction (baseline)
    CobolTransactionResult cobolResult = cobolService.processTransaction(inputData);
    
    // Execute Java transaction (modernized)
    JavaTransactionResult javaResult = javaService.processTransaction(inputData);
    
    // Validate identical results
    assertThat(javaResult.getAmount())
        .isEqualByComparingTo(cobolResult.getAmount());
    assertThat(javaResult.getBalance())
        .isEqualByComparingTo(cobolResult.getBalance());
    assertThat(javaResult.getResponseTime())
        .isLessThan(Duration.ofMillis(200));
}
```

#### 2. Batch Processing Validation

**Daily Comparison Job**: Automated Spring Batch job compares processing results:

```yaml
# Nightly comparison configuration
comparison:
  schedule: "0 2 * * *"  # 2 AM daily
  tolerance:
    monetary: 0.00        # Zero tolerance for financial calculations
    timing: 10%           # 10% variance allowed for processing time
  alerts:
    critical-threshold: 1 # Any discrepancy triggers critical alert
```

#### 3. Data Integrity Validation

**Checksum Verification**: Record-level integrity checks ensure data preservation:

```sql
-- Example checksum validation query
SELECT 
    table_name,
    COUNT(*) as record_count,
    MD5(STRING_AGG(CONCAT(primary_key, '|', data_checksum), '')) as table_checksum
FROM migration_validation 
GROUP BY table_name;
```

## Expected Accuracy Metrics and Tolerance Levels

### Financial Calculation Precision

**Zero Tolerance Policy**: All monetary calculations must match exactly between COBOL and Java implementations:

- **Interest Calculations**: Penny-perfect accuracy required
- **Balance Updates**: No variance permitted in account balances
- **Transaction Amounts**: Exact BigDecimal precision matching
- **Fee Calculations**: Identical results for all fee computations

### Performance Tolerance Levels

| Metric | COBOL Baseline | Java Target | Tolerance |
|--------|----------------|-------------|-----------|
| Transaction Response Time | <100ms | <200ms | +100ms acceptable |
| Batch Processing Window | 3.5 hours | 4 hours | +30 minutes maximum |
| Concurrent Users | 500 | 1000+ | 2x improvement expected |
| Database Query Performance | N/A | <10ms | New capability |

### Data Consistency Validation

**Referential Integrity**: All foreign key relationships must be preserved during migration:

```sql
-- Validation query for referential integrity
SELECT 
    'customer_data to account_data' as relationship,
    COUNT(a.account_id) as orphaned_records
FROM account_data a 
LEFT JOIN customer_data c ON a.customer_id = c.customer_id 
WHERE c.customer_id IS NULL;
```

## Troubleshooting Guide for Common Migration Issues

### 1. COMP-3 Precision Mismatches

**Symptom**: Financial calculations produce slightly different results

**Root Cause**: BigDecimal scale/precision configuration mismatch

**Solution**:
```java
// Correct BigDecimal configuration for COBOL COMP-3 compatibility
BigDecimal amount = new BigDecimal("123.45")
    .setScale(2, RoundingMode.HALF_UP);

// Validate precision matches COBOL
assertEquals(amount.scale(), 2);
assertEquals(amount.precision(), 5);
```

**Validation Test**:
```java
@Test
public void validateComp3Conversion() {
    byte[] cobolComp3 = {0x12, 0x34, 0x5F}; // COBOL packed decimal
    BigDecimal javaDecimal = CobolDataConverter.fromComp3(cobolComp3, 2);
    assertEquals(new BigDecimal("123.45"), javaDecimal);
}
```

### 2. Session State Management Issues

**Symptom**: User session data not preserved between REST calls

**Root Cause**: Redis session configuration or serialization problems

**Solution**:
```yaml
# Correct Spring Session Redis configuration
spring:
  session:
    store-type: redis
    redis:
      namespace: carddemo
      timeout: 1800s
    serialization-mode: json
```

**Validation Test**:
```java
@Test
public void validateSessionPersistence() {
    // Store session data
    session.setAttribute("userContext", userData);
    
    // Simulate new request
    MockHttpSession newSession = restoreSessionFromRedis(sessionId);
    
    // Validate data preservation
    assertThat(newSession.getAttribute("userContext"))
        .isEqualTo(userData);
}
```

### 3. Transaction Boundary Misalignment

**Symptom**: Database transactions don't match CICS SYNCPOINT behavior

**Root Cause**: Spring @Transactional configuration issues

**Solution**:
```java
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED,
    rollbackFor = {Exception.class}
)
public void processTransaction(TransactionRequest request) {
    // Business logic implementation
    // Automatic commit at method completion matches CICS SYNCPOINT
}
```

### 4. Performance Degradation

**Symptom**: Response times exceed 200ms requirement

**Root Cause**: Database query optimization or connection pooling issues

**Solution**:
```yaml
# Optimized HikariCP configuration
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

**Index Optimization**:
```sql
-- Example index for performance improvement
CREATE INDEX CONCURRENTLY idx_transactions_account_date 
ON transactions (account_id, transaction_date) 
WHERE active_status = 'Y';
```

### 5. Character Encoding Issues

**Symptom**: Text data corruption during EBCDIC to UTF-8 conversion

**Root Cause**: Character set mapping problems

**Solution**:
```java
// Correct character encoding conversion
String utf8Text = new String(ebcdicBytes, "IBM037");
byte[] utf8Bytes = utf8Text.getBytes(StandardCharsets.UTF_8);
```

## Integration with CI/CD Pipeline

### GitHub Actions Workflow

The migration testing framework integrates with the CI/CD pipeline for automated validation:

```yaml
# .github/workflows/migration-tests.yml
name: Migration Validation Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  migration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15.4
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      redis:
        image: redis:7.0.5
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Java 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'adopt'
    
    - name: Run Migration Unit Tests
      run: ./mvnw test -Dtest=**/*MigrationTest
    
    - name: Run Integration Tests
      run: ./mvnw test -Dtest=**/*IntegrationTest
    
    - name: Run Performance Tests
      run: ./mvnw test -Dtest=**/*PerformanceTest
    
    - name: Generate Migration Report
      run: |
        ./mvnw jacoco:report
        ./mvnw sonar:sonar -Dsonar.token=${{ secrets.SONAR_TOKEN }}
```

### Quality Gates

**SonarQube Integration**: Automated quality validation with strict thresholds:

- **Code Coverage**: ≥80% for all service classes
- **Duplicated Code**: <3% duplication allowed
- **Security Vulnerabilities**: 0 High/Critical vulnerabilities
- **Maintainability Rating**: A-grade required

**Performance Gates**: Automated performance validation:

```bash
# K6 performance gate example
k6 run --threshold http_req_duration:avg<200 migration-performance-test.js
```

### Deployment Validation

**Kubernetes Readiness Checks**: Ensure migration validation before production deployment:

```yaml
# deployment.yaml readiness probe
readinessProbe:
  httpGet:
    path: /actuator/health/migration
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3
```

## Migration Validation Checklist

### Pre-Migration Validation

- [ ] All VSAM datasets exported to ASCII format
- [ ] PostgreSQL schema created with identical table structures  
- [ ] Spring Batch ETL jobs configured and tested
- [ ] JPA entities validated against COBOL copybooks
- [ ] Test data sets prepared for parallel comparison

### During Migration Validation

- [ ] Unit tests pass with 100% coverage on financial calculations
- [ ] Integration tests validate REST endpoint functionality
- [ ] Performance tests confirm <200ms response time requirement
- [ ] E2E tests validate complete user workflows
- [ ] Parallel run comparison shows 0% variance in financial results

### Post-Migration Validation

- [ ] All data integrity checks pass
- [ ] Performance metrics meet or exceed baseline requirements
- [ ] Security validation confirms RACF-equivalent access control
- [ ] Batch processing completes within 4-hour window
- [ ] Production readiness confirmed through staging environment validation

## References and Additional Resources

### Technical Documentation
- **Section 3.3**: DATABASES & STORAGE - PostgreSQL implementation details
- **Section 6.2**: DATABASE DESIGN - Schema architecture and optimization
- **Section 6.6**: TESTING STRATEGY - Comprehensive testing methodology
- **Section 4.1**: SYSTEM WORKFLOWS - Business process validation

### Migration Utilities
- `backend/src/main/java/com/carddemo/util/CobolDataConverter.java` - COMP-3 conversion utility
- `backend/src/main/java/com/carddemo/batch/migration/` - Spring Batch migration jobs
- `backend/src/test/resources/migration/` - Migration test resources and data
- `backend/src/main/resources/schema.sql` - PostgreSQL schema definition

### Validation Scripts
- `scripts/migration-validation.sh` - Complete migration validation workflow
- `scripts/performance-validation.sh` - Performance testing automation
- `scripts/data-integrity-check.sql` - Database integrity validation queries

For additional support and troubleshooting, contact the migration team or refer to the comprehensive technical specification documentation.