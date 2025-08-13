# Test Templates Documentation

## Overview

This directory contains comprehensive test templates designed to support the CardDemo system's migration from COBOL/CICS mainframe to Spring Boot/React/PostgreSQL cloud-native architecture. These templates ensure functional parity validation between the original COBOL implementations and the modernized Java components while maintaining enterprise-grade testing standards.

## Table of Contents

1. [Template Categories](#template-categories)
2. [API Request/Response Templates](#api-requestresponse-templates)
3. [Batch Job Configuration Templates](#batch-job-configuration-templates)
4. [Mock Service Response Templates](#mock-service-response-templates)
5. [Validation Rule Templates](#validation-rule-templates)
6. [Comparison Report Templates](#comparison-report-templates)
7. [COBOL Pattern Compliance Guidelines](#cobol-pattern-compliance-guidelines)
8. [Usage Examples](#usage-examples)
9. [Template Versioning](#template-versioning)
10. [Maintenance Guidelines](#maintenance-guidelines)

## Template Categories

The test templates are organized into five primary categories, each serving specific testing requirements in the COBOL-to-Java migration validation process:

### 1. API Request/Response Templates
- **Purpose**: Validate REST endpoint functional parity with CICS transactions
- **Location**: `api/` subdirectory
- **Coverage**: All 24 CICS transaction codes (CC00, CT00, etc.)

### 2. Batch Job Configuration Templates
- **Purpose**: Configure Spring Batch jobs to replicate JCL processing behavior
- **Location**: `batch/` subdirectory
- **Coverage**: All 12 batch processing components

### 3. Mock Service Response Templates
- **Purpose**: Simulate external service interactions for isolated testing
- **Location**: `mocks/` subdirectory
- **Coverage**: Payment networks, core banking, regulatory systems

### 4. Validation Rule Templates
- **Purpose**: Ensure business logic equivalence between COBOL and Java
- **Location**: `validation/` subdirectory
- **Coverage**: Financial calculations, data type conversions, business rules

### 5. Comparison Report Templates
- **Purpose**: Generate detailed comparison reports for COBOL-Java output validation
- **Location**: `reports/` subdirectory
- **Coverage**: Calculation accuracy, transaction processing, batch results

## API Request/Response Templates

### Template Structure

API templates follow a standardized JSON schema that maps directly to BMS screen definitions and CICS transaction patterns:

```json
{
  "transactionCode": "CC00",
  "description": "User Sign-On Transaction",
  "cobolProgram": "COSGN00C.cbl",
  "bmsMap": "COSGN00.bms",
  "request": {
    "template": {
      "userId": "{{USER_ID}}",
      "password": "{{PASSWORD}}",
      "transactionId": "{{TRANSACTION_ID}}"
    },
    "validation": {
      "userId": {
        "type": "string",
        "maxLength": 8,
        "pattern": "^[A-Z0-9]{1,8}$",
        "cobolField": "USERID"
      },
      "password": {
        "type": "string",
        "maxLength": 8,
        "pattern": "^[A-Za-z0-9!@#$%^&*]{1,8}$",
        "cobolField": "PASSWD"
      }
    }
  },
  "response": {
    "success": {
      "returnCode": "00",
      "menuOptions": [
        {"code": "1", "description": "Account Management"},
        {"code": "2", "description": "Transaction History"},
        {"code": "3", "description": "Card Management"}
      ],
      "sessionData": {
        "userId": "{{USER_ID}}",
        "userType": "{{USER_TYPE}}",
        "sessionId": "{{SESSION_ID}}"
      }
    },
    "failure": {
      "returnCode": "01",
      "errorMessage": "Invalid credentials",
      "cobolAbend": "INVALID-SIGNON"
    }
  },
  "performanceCriteria": {
    "responseTime": "< 200ms",
    "throughput": "> 1000 TPS"
  }
}
```

### Available API Templates

#### Authentication Templates
- **`cc00-signon.json`**: User authentication and menu display
- **`cc01-signoff.json`**: User session termination
- **`cc02-menu-navigation.json`**: Main menu navigation

#### Account Management Templates
- **`acct-view.json`**: Account inquiry operations
- **`acct-update.json`**: Account modification operations
- **`acct-create.json`**: New account creation

#### Transaction Processing Templates
- **`trn-list.json`**: Transaction history retrieval
- **`trn-detail.json`**: Individual transaction details
- **`trn-create.json`**: New transaction processing

#### Card Management Templates
- **`card-list.json`**: Credit card listing operations
- **`card-detail.json`**: Card information display
- **`card-update.json`**: Card status modifications

### Usage Example - API Template

```java
@Test
@DisplayName("CC00 Sign-On Transaction - COBOL Equivalent Validation")
public void testSignOnTransaction_CobolEquivalent() throws Exception {
    // Load template
    String template = loadTemplate("api/cc00-signon.json");
    ApiTemplate signOnTemplate = objectMapper.readValue(template, ApiTemplate.class);
    
    // Populate test data
    String requestBody = signOnTemplate.getRequest()
        .replace("{{USER_ID}}", "TESTUSER")
        .replace("{{PASSWORD}}", "password123")
        .replace("{{TRANSACTION_ID}}", generateTransactionId());
    
    // Execute REST call
    mockMvc.perform(post("/api/transactions/CC00")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.returnCode").value("00"))
            .andExpect(jsonPath("$.menuOptions").isArray())
            .andExpect(responseTime(lessThan(200L)));
}
```

## Batch Job Configuration Templates

### Template Structure

Batch templates define Spring Batch job configurations that replicate JCL processing patterns:

```yaml
batchJob:
  jobName: "dailyProcessingJob"
  cobolProgram: "CBTRN01C.cbl"
  jclJob: "DAILYPROC"
  description: "Daily transaction processing equivalent to JCL DAILYPROC"
  
  parameters:
    processDate: "{{PROCESS_DATE}}"
    batchMode: "{{BATCH_MODE}}"
    inputFile: "{{INPUT_FILE_PATH}}"
    outputFile: "{{OUTPUT_FILE_PATH}}"
    
  steps:
    - stepName: "inputValidation"
      type: "validation"
      cobolParagraph: "1000-VALIDATE-INPUT"
      chunkSize: 1000
      
    - stepName: "transactionProcessing"
      type: "processing"
      cobolParagraph: "2000-PROCESS-TRANSACTIONS"
      chunkSize: 100
      
    - stepName: "outputGeneration"
      type: "output"
      cobolParagraph: "3000-GENERATE-OUTPUT"
      chunkSize: 500
      
  errorHandling:
    retryLimit: 3
    skipLimit: 10
    restartable: true
    
  performanceCriteria:
    maxDuration: "4 hours"
    memoryUsage: "< 2GB"
    
  validation:
    outputComparison: true
    cobolReference: "expected-output/CBTRN01C-output.txt"
    toleranceLevel: "zero-difference"
```

### Available Batch Templates

#### Daily Processing Templates
- **`daily-transaction-processing.yml`**: CBTRN01C equivalent
- **`daily-interest-calculation.yml`**: CBACT04C equivalent
- **`daily-statement-generation.yml`**: CBSTM03A equivalent

#### Monthly Processing Templates
- **`monthly-account-processing.yml`**: CBACT01C equivalent
- **`monthly-customer-processing.yml`**: CBCUS01C equivalent

#### Utility Processing Templates
- **`data-migration.yml`**: Data conversion utilities
- **`report-generation.yml`**: Business reporting jobs

### Usage Example - Batch Template

```java
@Test
@DisplayName("Daily Processing Job - JCL DAILYPROC Equivalent")
public void testDailyProcessingJob_JclEquivalent() throws Exception {
    // Load batch template
    BatchTemplate template = loadBatchTemplate("batch/daily-transaction-processing.yml");
    
    // Configure job parameters
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("processDate", "2024-01-15")
        .addString("batchMode", "FULL")
        .addString("inputFile", "test-data/transactions.txt")
        .addString("outputFile", "output/processed-transactions.txt")
        .toJobParameters();
    
    // Execute job
    JobExecution jobExecution = jobLauncher.run(
        template.getSpringBatchJob(), jobParameters);
    
    // Validate results
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime())
        .isLessThan(Duration.ofHours(4).toMillis());
    
    // Compare output with COBOL reference
    validateOutputEquivalence(
        "output/processed-transactions.txt",
        template.getValidation().getCobolReference()
    );
}
```

## Mock Service Response Templates

### Template Structure

Mock templates simulate external service interactions for comprehensive testing isolation:

```json
{
  "serviceName": "PaymentNetworkService",
  "description": "Mock responses for payment network authorization",
  "externalSystem": "Visa/MasterCard Network",
  "
  scenarios": [
    {
      "scenarioName": "successful_authorization",
      "description": "Successful payment authorization",
      "request": {
        "cardNumber": "{{CARD_NUMBER}}",
        "amount": "{{AMOUNT}}",
        "merchantId": "{{MERCHANT_ID}}",
        "transactionType": "AUTH"
      },
      "response": {
        "responseCode": "00",
        "authCode": "{{AUTH_CODE}}",
        "referenceNumber": "{{REF_NUMBER}}",
        "responseMessage": "APPROVED",
        "balanceAfter": "{{BALANCE_AFTER}}"
      },
      "responseTime": "150ms"
    },
    {
      "scenarioName": "insufficient_funds",
      "description": "Authorization declined - insufficient funds",
      "request": {
        "cardNumber": "{{CARD_NUMBER}}",
        "amount": "{{AMOUNT}}",
        "merchantId": "{{MERCHANT_ID}}",
        "transactionType": "AUTH"
      },
      "response": {
        "responseCode": "51",
        "authCode": null,
        "referenceNumber": "{{REF_NUMBER}}",
        "responseMessage": "INSUFFICIENT FUNDS",
        "balanceAfter": "{{CURRENT_BALANCE}}"
      },
      "responseTime": "120ms"
    }
  ],
  "wiremockMapping": {
    "port": 8089,
    "stubPattern": "/payment/authorize",
    "method": "POST"
  }
}
```

### Available Mock Templates

#### Payment Network Mocks
- **`payment-network-auth.json`**: Authorization service responses
- **`payment-network-settlement.json`**: Settlement processing responses
- **`payment-network-reversal.json`**: Transaction reversal responses

#### Core Banking System Mocks
- **`core-banking-account.json`**: Account inquiry responses
- **`core-banking-balance.json`**: Balance verification responses
- **`core-banking-transfer.json`**: Fund transfer responses

#### Regulatory System Mocks
- **`regulatory-reporting.json`**: Compliance reporting responses
- **`fraud-detection.json`**: Fraud scoring responses

### Usage Example - Mock Service Template

```java
@Test
@DisplayName("Payment Authorization - External Service Mock")
public void testPaymentAuthorization_ExternalServiceMock() throws Exception {
    // Load mock template
    MockTemplate mockTemplate = loadMockTemplate("mocks/payment-network-auth.json");
    
    // Configure WireMock with template
    wireMockServer.stubFor(post(urlEqualTo("/payment/authorize"))
        .withRequestBody(matchingJsonPath("$.cardNumber"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(mockTemplate.getSuccessResponse())
            .withFixedDelay(150)));
    
    // Execute service call
    AuthorizationRequest request = new AuthorizationRequest(
        "4532123456789012", new BigDecimal("150.00"), "MERCHANT001", "AUTH");
    
    AuthorizationResponse response = paymentNetworkService.authorize(request);
    
    // Validate response
    assertThat(response.getResponseCode()).isEqualTo("00");
    assertThat(response.getResponseMessage()).isEqualTo("APPROVED");
    assertThat(response.getAuthCode()).isNotNull();
}
```

## Validation Rule Templates

### Template Structure

Validation templates ensure business logic equivalence between COBOL and Java implementations:

```yaml
validationRule:
  ruleName: "interestCalculationValidation"
  description: "Validates interest calculation precision between COBOL COMP-3 and Java BigDecimal"
  cobolProgram: "CBACT04C.cbl"
  cobolParagraph: "2000-CALCULATE-INTEREST"
  javaMethod: "InterestService.calculateMonthlyInterest"
  
  testCases:
    - testCaseId: "INTEREST_001"
      description: "Standard interest calculation"
      input:
        principal: "1000.00"
        rate: "0.0525"
        days: "30"
      expectedOutput:
        cobolComp3: "4.38"
        javaBigDecimal: "4.38"
        scale: 2
        roundingMode: "HALF_UP"
      tolerance: "0.00"
      
    - testCaseId: "INTEREST_002"
      description: "High precision calculation"
      input:
        principal: "15000.75"
        rate: "0.1899"
        days: "31"
      expectedOutput:
        cobolComp3: "244.21"
        javaBigDecimal: "244.21"
        scale: 2
        roundingMode: "HALF_UP"
      tolerance: "0.00"
      
  validation:
    precisionValidation: true
    scaleValidation: true
    roundingValidation: true
    performanceValidation: true
    
  comparisonMethod:
    type: "bigdecimal_exact_match"
    cobolConverter: "CobolDataConverter.fromComp3"
    javaProcessor: "BigDecimalProcessor.calculate"
```

### Available Validation Templates

#### Financial Calculation Validations
- **`interest-calculation.yml`**: Interest calculation precision validation
- **`balance-calculation.yml`**: Account balance computation validation
- **`payment-calculation.yml`**: Payment processing validation

#### Data Type Conversion Validations
- **`cobol-comp3-conversion.yml`**: COMP-3 to BigDecimal validation
- **`date-conversion.yml`**: Date format conversion validation
- **`numeric-field-conversion.yml`**: Numeric field precision validation

#### Business Rule Validations
- **`credit-limit-validation.yml`**: Credit limit business rule validation
- **`transaction-validation.yml`**: Transaction validation rule verification
- **`account-status-validation.yml`**: Account status business rule validation

### Usage Example - Validation Rule Template

```java
@Test
@DisplayName("Interest Calculation Validation - COBOL COMP-3 Equivalence")
public void testInterestCalculation_CobolComp3Equivalence() throws Exception {
    // Load validation template
    ValidationTemplate template = loadValidationTemplate("validation/interest-calculation.yml");
    
    for (TestCase testCase : template.getTestCases()) {
        // Execute COBOL equivalent calculation
        BigDecimal cobolResult = cobolDataConverter.fromComp3(
            testCase.getExpectedOutput().getCobolComp3(),
            testCase.getExpectedOutput().getScale()
        );
        
        // Execute Java calculation
        BigDecimal javaResult = interestService.calculateMonthlyInterest(
            new BigDecimal(testCase.getInput().getPrincipal()),
            new BigDecimal(testCase.getInput().getRate()),
            RoundingMode.valueOf(testCase.getExpectedOutput().getRoundingMode())
        );
        
        // Validate exact equivalence
        assertThat(javaResult)
            .describedAs("Test case: " + testCase.getTestCaseId())
            .isEqualTo(cobolResult);
            
        assertThat(javaResult.scale())
            .isEqualTo(testCase.getExpectedOutput().getScale());
    }
}
```

## Comparison Report Templates

### Template Structure

Report templates generate detailed comparison analyses between COBOL and Java outputs:

```yaml
comparisonReport:
  reportName: "dailyProcessingComparison"
  description: "Daily processing output comparison between COBOL and Java batch jobs"
  cobolJob: "DAILYPROC JCL"
  javaJob: "dailyProcessingJob Spring Batch"
  
  comparisonCategories:
    - category: "fileOutput"
      description: "Compare generated output files"
      comparisonType: "byte_by_byte"
      files:
        cobolOutput: "mainframe/DAILYPROC.OUTPUT"
        javaOutput: "spring-batch/daily-processing-output.txt"
      validationCriteria:
        exactMatch: true
        toleranceLevel: "zero_difference"
        
    - category: "performanceMetrics"
      description: "Compare processing performance"
      comparisonType: "metrics_analysis"
      metrics:
        processingTime:
          cobolBaseline: "45 minutes"
          javaTarget: "< 45 minutes"
        memoryUsage:
          cobolBaseline: "256MB"
          javaTarget: "< 512MB"
        recordsProcessed:
          expectedCount: "{{RECORD_COUNT}}"
          validationRule: "exact_match"
          
    - category: "calculationAccuracy"
      description: "Compare financial calculation results"
      comparisonType: "precision_analysis"
      calculations:
        interestCalculations:
          precision: "penny_accurate"
          roundingMode: "HALF_UP"
        balanceUpdates:
          precision: "exact_match"
          tolerance: "0.00"
          
  reportFormat:
    outputFile: "reports/daily-processing-comparison-{{DATE}}.html"
    includeCharts: true
    includeDetailedDiffs: true
    includeSummaryStats: true
    
  alertingCriteria:
    criticalDiscrepancies: true
    performanceDegradation: true
    calculationErrors: true
```

### Available Report Templates

#### Batch Processing Reports
- **`daily-processing-comparison.yml`**: Daily batch job comparison
- **`interest-calculation-comparison.yml`**: Interest calculation analysis
- **`statement-generation-comparison.yml`**: Statement output comparison

#### Transaction Processing Reports
- **`transaction-processing-comparison.yml`**: Transaction processing analysis
- **`authorization-response-comparison.yml`**: Authorization processing comparison
- **`payment-processing-comparison.yml`**: Payment processing validation

#### Data Migration Reports
- **`data-migration-validation.yml`**: Data migration accuracy report
- **`schema-conversion-validation.yml`**: Database schema conversion report

### Usage Example - Comparison Report Template

```java
@Test
@DisplayName("Daily Processing Comparison Report Generation")
public void testDailyProcessingComparison_ReportGeneration() throws Exception {
    // Load comparison template
    ComparisonTemplate template = loadComparisonTemplate("reports/daily-processing-comparison.yml");
    
    // Execute COBOL reference processing (simulated)
    CobolProcessingResult cobolResult = simulateCobolProcessing(
        template.getCobolJob(), testDataSet);
    
    // Execute Java Spring Batch processing
    JobExecution javaJobExecution = jobLauncher.run(
        dailyProcessingJob, createJobParameters());
    
    // Generate comparison report
    ComparisonReport report = comparisonReportGenerator.generateReport(
        template, cobolResult, javaJobExecution);
    
    // Validate report results
    assertThat(report.getOverallStatus()).isEqualTo(ComparisonStatus.PASSED);
    assertThat(report.getFileOutputComparison().getMatchPercentage()).isEqualTo(100.0);
    assertThat(report.getCalculationAccuracyComparison().getDiscrepancies()).isEmpty();
    
    // Save report
    reportStorage.saveReport(report, template.getReportFormat().getOutputFile());
}
```

## COBOL Pattern Compliance Guidelines

### Core Compliance Principles

1. **Functional Parity**: Every Java implementation must produce identical results to its COBOL counterpart
2. **Data Precision**: Financial calculations must maintain penny-level accuracy using BigDecimal
3. **Error Handling**: Exception handling must replicate COBOL ABEND behavior
4. **Performance**: Response times must meet or exceed original mainframe performance
5. **Session Management**: Spring Session must replicate CICS COMMAREA behavior

### COBOL Data Type Conversion Guidelines

#### COMP-3 (Packed Decimal) Conversion

```java
// COBOL: PIC S9(7)V99 COMP-3
// Java equivalent configuration
@Converter
public class Comp3ToBigDecimalConverter implements AttributeConverter<BigDecimal, byte[]> {
    
    @Override
    public byte[] convertToDatabaseColumn(BigDecimal attribute) {
        return CobolDataConverter.toComp3(attribute, 7, 2);
    }
    
    @Override
    public BigDecimal convertToEntityAttribute(byte[] dbData) {
        return CobolDataConverter.fromComp3(dbData, 7, 2);
    }
}

// Usage in entity classes
@Entity
public class Account {
    @Convert(converter = Comp3ToBigDecimalConverter.class)
    @Column(name = "ACCOUNT_BALANCE")
    private BigDecimal accountBalance; // Maintains COBOL COMP-3 precision
}
```

#### Date Conversion Guidelines

```java
// COBOL: PIC 9(8) (YYYYMMDD format)
// Java equivalent
@Converter
public class CobolDateConverter implements AttributeConverter<LocalDate, String> {
    
    private static final DateTimeFormatter COBOL_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute != null ? attribute.format(COBOL_DATE_FORMAT) : null;
    }
    
    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        return dbData != null ? LocalDate.parse(dbData, COBOL_DATE_FORMAT) : null;
    }
}
```

### Business Logic Translation Guidelines

#### COBOL Paragraph to Java Method Mapping

```java
// COBOL Program: COSGN00C.cbl
// Java Service: SignOnService.java

@Service
@Transactional
public class SignOnService {
    
    // 0000-MAIN-PROCESSING paragraph equivalent
    public SignOnResponse processMainLogic(SignOnRequest request) {
        try {
            // 1000-PROCESS-INPUTS equivalent
            validateInputs(request);
            
            // 2000-PROCESS-BUSINESS-LOGIC equivalent
            UserAuthentication auth = authenticateUser(request);
            
            // 3000-PROCESS-OUTPUTS equivalent
            return formatResponse(auth);
            
        } catch (Exception e) {
            // 9999-ABEND-PROGRAM equivalent
            return handleSystemException(e);
        }
    }
    
    // 1000-PROCESS-INPUTS paragraph
    private void validateInputs(SignOnRequest request) {
        if (StringUtils.isEmpty(request.getUserId()) || 
            request.getUserId().length() > 8) {
            throw new ValidationException("INVALID-USERID");
        }
        // Additional validation matching COBOL logic
    }
}
```

### Testing Pattern Guidelines

#### Unit Test Structure for COBOL Equivalence

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class SignOnServiceTest {
    
    @Autowired
    private SignOnService signOnService;
    
    @Test
    @DisplayName("0000-MAIN-PROCESSING equivalent - valid user authentication")
    void test0000MainProcessing_ValidUserAuthentication() {
        // Given: Test data matching COBOL test vectors
        SignOnRequest request = SignOnRequest.builder()
            .userId("TESTUSER")
            .password("password123")
            .build();
        
        // When: Execute Java method equivalent to COBOL paragraph
        SignOnResponse response = signOnService.processMainLogic(request);
        
        // Then: Validate identical behavior to COBOL COSGN00C.cbl
        assertThat(response.getReturnCode()).isEqualTo("00");
        assertThat(response.getUserId()).isEqualTo("TESTUSER");
        // Additional assertions matching COBOL expected outputs
    }
}
```

### Performance Compliance Guidelines

#### Response Time Validation

```java
@Test
@DisplayName("Performance validation - response time < 200ms")
void testResponseTimeCompliance() {
    long startTime = System.currentTimeMillis();
    
    SignOnResponse response = signOnService.processMainLogic(testRequest);
    
    long endTime = System.currentTimeMillis();
    long responseTime = endTime - startTime;
    
    assertThat(responseTime)
        .describedAs("Response time must be under 200ms for COBOL parity")
        .isLessThan(200L);
}
```

## Usage Examples

### Template Integration in Test Classes

#### Test Base Class with Template Support

```java
@SpringBootTest
@Testcontainers
public abstract class BaseTemplateTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    protected ObjectMapper objectMapper = new ObjectMapper();
    protected TemplateLoader templateLoader = new TemplateLoader();
    
    protected <T> T loadTemplate(String templatePath, Class<T> templateClass) {
        try {
            String templateContent = templateLoader.loadTemplateContent(templatePath);
            return objectMapper.readValue(templateContent, templateClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template: " + templatePath, e);
        }
    }
    
    protected void validateCobolEquivalence(String javaOutput, String cobolReference) {
        ComparisonResult result = outputComparator.compare(javaOutput, cobolReference);
        assertThat(result.isEquivalent())
            .describedAs("Java output must match COBOL reference exactly")
            .isTrue();
    }
}
```

#### Integration Test with Multiple Templates

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionProcessingIntegrationTest extends BaseTemplateTest {
    
    @Test
    @DisplayName("Complete transaction processing workflow with templates")
    void testCompleteTransactionWorkflow_WithTemplates() throws Exception {
        // Load API template for transaction creation
        ApiTemplate createTemplate = loadTemplate(
            "api/trn-create.json", ApiTemplate.class);
        
        // Load validation template for calculation verification
        ValidationTemplate calcTemplate = loadTemplate(
            "validation/payment-calculation.yml", ValidationTemplate.class);
        
        // Load mock template for external service simulation
        MockTemplate paymentMock = loadTemplate(
            "mocks/payment-network-auth.json", MockTemplate.class);
        
        // Configure mocks from template
        configureMockServices(paymentMock);
        
        // Execute transaction creation using API template
        String requestBody = populateTemplate(createTemplate, testTransactionData);
        MvcResult result = mockMvc.perform(post("/api/transactions/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();
        
        // Validate calculation results using validation template
        TransactionResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), TransactionResponse.class);
        
        validateCalculationAccuracy(response, calcTemplate);
        
        // Generate comparison report
        ComparisonTemplate reportTemplate = loadTemplate(
            "reports/transaction-processing-comparison.yml", ComparisonTemplate.class);
        
        generateComparisonReport(response, reportTemplate);
    }
}
```

## Template Versioning

### Version Control Strategy

Templates follow semantic versioning (MAJOR.MINOR.PATCH) aligned with the COBOL source version:

- **MAJOR**: Breaking changes in COBOL business logic
- **MINOR**: Additional test scenarios or enhanced validation
- **PATCH**: Bug fixes or template corrections

### Version Metadata

Each template includes version metadata:

```json
{
  "templateVersion": "1.2.0",
  "cobolSourceVersion": "2024.01.15",
  "lastUpdated": "2024-01-20T10:30:00Z",
  "updatedBy": "migration-team",
  "changeReason": "Enhanced validation for COMP-3 precision",
  "compatibleWith": {
    "springBootVersion": "3.2.x",
    "junitVersion": "5.10.x",
    "testcontainersVersion": "1.19.x"
  }
}
```

### Update Procedures

1. **COBOL Source Changes**: When COBOL programs are modified, corresponding templates must be updated
2. **Test Framework Updates**: Template syntax may require updates for new testing framework versions
3. **Validation Enhancement**: Templates may be enhanced to include additional validation scenarios
4. **Performance Tuning**: Templates may be optimized for better test execution performance

## Maintenance Guidelines

### Template Maintenance Responsibilities

#### Development Team
- Update templates when modifying corresponding Java service classes
- Ensure template compatibility with code changes
- Validate template accuracy through automated tests

#### Test Team
- Review template coverage for new test scenarios
- Validate template effectiveness in catching defects
- Update templates based on production issue analysis

#### Migration Team
- Maintain COBOL-Java equivalence in templates
- Update templates for COBOL source changes
- Ensure templates support migration validation requirements

### Template Quality Standards

#### Accuracy Requirements
- Templates must accurately represent COBOL program behavior
- All data transformations must preserve original business logic
- Error conditions must match original ABEND scenarios

#### Performance Requirements
- Template-based tests must execute within CI/CD time constraints
- Mock services must respond within realistic timeframes
- Validation rules must not introduce significant overhead

#### Maintainability Requirements
- Templates must be self-documenting with clear descriptions
- Template structure must be consistent across categories
- Template dependencies must be clearly documented

### Automated Template Validation

#### Template Schema Validation

```yaml
# template-validation-schema.yml
templateValidation:
  schemaValidation: true
  requiredFields:
    - templateVersion
    - description
    - cobolProgram
  
  performanceValidation:
    maxResponseTime: "5 seconds"
    maxMemoryUsage: "100MB"
  
  contentValidation:
    validateExamples: true
    validateReferences: true
    validateSyntax: true
```

#### Continuous Integration Checks

```java
@Test
@DisplayName("Template validation - all templates must be valid")
void validateAllTemplates() {
    TemplateValidator validator = new TemplateValidator();
    List<String> templatePaths = templateDiscovery.findAllTemplates();
    
    for (String templatePath : templatePaths) {
        ValidationResult result = validator.validateTemplate(templatePath);
        assertThat(result.isValid())
            .describedAs("Template must be valid: " + templatePath)
            .isTrue();
    }
}
```

### Template Usage Analytics

#### Metrics Collection

Templates include usage analytics to track effectiveness:

- **Template Usage Frequency**: Which templates are used most often
- **Test Success Rates**: How often template-based tests pass/fail
- **Performance Impact**: How templates affect test execution time
- **Defect Detection**: How effectively templates catch issues

#### Continuous Improvement

Based on analytics data:

1. **High-Usage Templates**: Optimize for better performance
2. **Low-Usage Templates**: Review necessity and relevance
3. **High-Failure Templates**: Investigate and improve accuracy
4. **Performance Issues**: Optimize template structure and content

---

## Contact and Support

For questions, issues, or contributions related to test templates:

- **Migration Team**: migration-team@company.com
- **Test Architecture**: test-architecture@company.com
- **COBOL SMEs**: cobol-experts@company.com

## Contributing

When contributing new templates or modifications:

1. Follow the established template structure and naming conventions
2. Include comprehensive documentation and examples
3. Validate templates against COBOL source programs
4. Update this README with new template information
5. Submit pull requests with detailed change descriptions

---

*This documentation is maintained as part of the CardDemo COBOL-to-Java migration project. All templates and guidelines are designed to ensure 100% functional parity between original COBOL implementations and modernized Java components.*