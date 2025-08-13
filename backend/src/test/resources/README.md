# Test Resources Directory

This directory contains all test configuration files, test data fixtures, mock responses, and related resources for the CardDemo Spring Boot application testing framework. The testing infrastructure supports comprehensive validation of the COBOL-to-Java modernization, ensuring functional parity between the original mainframe system and the cloud-native implementation.

## Overview

The test resources are organized to support multiple testing layers as part of our cloud-native testing strategy:

- **Unit Testing**: JUnit 5 tests with business logic validation
- **Integration Testing**: Spring Boot Test with Testcontainers 
- **End-to-End Testing**: Complete workflow validation
- **Performance Testing**: Response time and throughput validation
- **Security Testing**: Authentication and authorization validation

All test configurations maintain compatibility with the modernized technology stack including Java 21, Spring Boot 3.2.x, PostgreSQL 17.x, and Redis 7.4.x.

## Directory Structure

```
backend/src/test/resources/
├── README.md                          # This documentation file
├── application-test.yml               # Spring Boot test configuration
├── application-integration.yml        # Integration test specific config
├── application-performance.yml        # Performance test configuration
├── data/                              # Test data fixtures
│   ├── csv/                          # CSV format test data
│   │   ├── account-test-data.csv     # Account entity test fixtures
│   │   ├── customer-test-data.csv    # Customer entity test fixtures
│   │   ├── transaction-test-data.csv # Transaction entity test fixtures
│   │   └── card-test-data.csv        # Card entity test fixtures
│   ├── json/                         # JSON format test data
│   │   ├── rest-api-requests/        # REST API request payloads
│   │   │   ├── sign-on-requests.json # CC00 transaction test data
│   │   │   ├── account-requests.json # Account management requests
│   │   │   └── transaction-requests.json # Transaction processing requests
│   │   └── rest-api-responses/       # Expected REST API responses
│   │       ├── sign-on-responses.json
│   │       ├── account-responses.json
│   │       └── transaction-responses.json
│   └── sql/                          # SQL test scripts
│       ├── schema/                   # Test database schema
│       │   ├── test-schema.sql       # Base test schema creation
│       │   └── test-constraints.sql  # Test-specific constraints
│       └── data/                     # Test data insertion scripts
│           ├── insert-test-accounts.sql
│           ├── insert-test-customers.sql
│           └── insert-test-transactions.sql
├── mock/                             # Mock service responses
│   ├── external-apis/               # External service mocks
│   │   ├── payment-network-mocks.json
│   │   └── core-banking-mocks.json
│   └── internal-services/           # Internal service mocks
│       ├── batch-job-responses.json
│       └── audit-service-responses.json
├── config/                          # Test-specific configurations
│   ├── testcontainers/             # Container test configurations
│   │   ├── postgresql-test.conf    # PostgreSQL test configuration
│   │   └── redis-test.conf         # Redis test configuration
│   ├── security/                   # Security test configurations
│   │   ├── test-users.json         # Test user accounts and roles
│   │   └── security-test.yml       # Spring Security test settings
│   └── batch/                      # Spring Batch test configurations
│       ├── test-job-config.xml     # Batch job test definitions
│       └── batch-test.properties   # Batch processing test properties
└── templates/                      # Test data templates
    ├── cobol-conversion/           # COBOL-to-Java conversion templates
    │   ├── comp3-test-values.json  # COMP-3 to BigDecimal test cases
    │   └── packed-decimal-tests.json # Packed decimal precision tests
    └── performance/                # Performance test templates
        ├── load-test-scenarios.json
        └── stress-test-data.json
```

## Test Configuration Files

### Core Configuration Files

#### `application-test.yml`
Main Spring Boot test configuration supporting unit and integration tests.

**Key Features:**
- In-memory H2 database for unit tests
- Testcontainers PostgreSQL for integration tests
- Mock external service endpoints
- Test-specific logging configuration
- Spring Security test configuration

**Usage:**
```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@ActiveProfiles("test")
class ServiceTest {
    // Test implementation
}
```

#### `application-integration.yml` 
Integration test configuration with external service integration.

**Key Features:**
- Testcontainers PostgreSQL with full schema
- Redis session storage testing
- External API contract testing
- Performance monitoring during tests
- Cross-service communication testing

**Usage:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-integration.yml")
@Testcontainers
class IntegrationTest {
    // Integration test implementation
}
```

#### `application-performance.yml`
Performance test configuration with monitoring and metrics.

**Key Features:**
- Production-like database configuration
- Micrometer metrics collection
- JVM performance monitoring
- Response time threshold configuration
- Throughput measurement settings

**Usage:**
```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-performance.yml")
@EnabledIfEnvironmentVariable(named = "PERFORMANCE_TESTS", matches = "true")
class PerformanceTest {
    // Performance test implementation
}
```

## Test Data Organization

### CSV Test Data Format

The CSV test data files maintain consistency with the original VSAM record layouts while supporting modern JPA entity mapping.

#### Account Test Data (`data/csv/account-test-data.csv`)
```csv
account_id,customer_id,account_type,balance,interest_rate,status,created_date
1000000001,1000000001,CHECKING,1500.75,0.0125,ACTIVE,2024-01-01
1000000002,1000000001,SAVINGS,5000.00,0.0350,ACTIVE,2024-01-01
1000000003,1000000002,CHECKING,750.25,0.0125,ACTIVE,2024-01-15
```

**Usage:**
```java
@Sql(scripts = "/data/sql/data/insert-test-accounts.sql")
@Test
void testAccountRepository() {
    // Test with pre-loaded account data
}
```

### JSON Test Data Format

JSON test data supports REST API testing and maintains compatibility with the React frontend.

#### REST API Request Examples (`data/json/rest-api-requests/sign-on-requests.json`)
```json
{
  "validSignOnRequest": {
    "userId": "TESTUSER",
    "password": "testpass123",
    "transactionCode": "CC00"
  },
  "invalidCredentialsRequest": {
    "userId": "INVALIDUSER",
    "password": "wrongpass",
    "transactionCode": "CC00"
  }
}
```

**Usage:**
```java
@Test
void testSignOnEndpoint() {
    String requestJson = testDataLoader.loadJson("sign-on-requests.json", "validSignOnRequest");
    // Use in MockMvc test
}
```

## Test Profiles and Activation

### Available Test Profiles

#### `test` Profile (Default)
- **Purpose**: Standard unit and integration testing
- **Database**: H2 in-memory for unit tests, Testcontainers PostgreSQL for integration
- **External Services**: Mocked
- **Performance Monitoring**: Disabled
- **Activation**: `@ActiveProfiles("test")`

#### `integration` Profile
- **Purpose**: Cross-service integration testing
- **Database**: Testcontainers PostgreSQL with full schema
- **External Services**: WireMock contract testing
- **Session Management**: Redis integration testing
- **Activation**: `@ActiveProfiles("integration")`

#### `performance` Profile
- **Purpose**: Performance and load testing
- **Database**: Production-like PostgreSQL configuration
- **Monitoring**: Full Micrometer metrics collection
- **Thresholds**: Response time <200ms validation
- **Activation**: `@ActiveProfiles("performance")`

#### `security` Profile
- **Purpose**: Security-focused testing
- **Authentication**: Full Spring Security testing
- **Authorization**: Role-based access control validation
- **Audit**: Security event logging and validation
- **Activation**: `@ActiveProfiles("security")`

### Profile Activation Examples

```java
// Unit test with default test profile
@SpringBootTest
@ActiveProfiles("test")
class UnitTest { }

// Integration test with database and external services
@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
class IntegrationTest { }

// Performance test with metrics collection
@SpringBootTest
@ActiveProfiles("performance")
@EnabledIfEnvironmentVariable(named = "PERFORMANCE_TESTS", matches = "true")
class PerformanceTest { }

// Security test with authentication and authorization
@SpringBootTest
@ActiveProfiles("security")
@WithMockUser(roles = {"USER", "ADMIN"})
class SecurityTest { }
```

## Testcontainers Configuration

### PostgreSQL Test Container

Configuration for PostgreSQL integration testing with Docker containers.

**Container Configuration** (`config/testcontainers/postgresql-test.conf`):
```properties
# PostgreSQL test container settings
max_connections = 20
shared_buffers = 128MB
effective_cache_size = 256MB
maintenance_work_mem = 32MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
```

**Usage in Tests:**
```java
@Testcontainers
class DatabaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5")
            .withDatabaseName("carddemo_test")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("config/testcontainers/postgresql-test.conf"),
                "/etc/postgresql/postgresql.conf"
            );
    
    @Test
    void testDatabaseOperations() {
        // Database integration tests
    }
}
```

### Redis Test Container

Configuration for Redis session testing.

**Usage:**
```java
@Testcontainers
class SessionIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379)
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("config/testcontainers/redis-test.conf"),
                "/usr/local/etc/redis/redis.conf"
            );
    
    @Test
    void testSessionManagement() {
        // Redis session tests
    }
}
```

## Mock Service Configuration

### External API Mocks

Mock configurations for external service dependencies using WireMock.

#### Payment Network Mocks (`mock/external-apis/payment-network-mocks.json`)
```json
{
  "authorizationSuccess": {
    "request": {
      "method": "POST",
      "url": "/api/authorize",
      "headers": {
        "Content-Type": "application/json"
      },
      "bodyPattern": {
        "matchesJsonPath": "$.cardNumber"
      }
    },
    "response": {
      "status": 200,
      "headers": {
        "Content-Type": "application/json"
      },
      "body": {
        "responseCode": "00",
        "authCode": "ABC123",
        "transactionId": "TXN001"
      }
    }
  }
}
```

**Usage:**
```java
@Test
void testPaymentNetworkIntegration() {
    // WireMock server setup using mock configuration
    wireMockServer.stubFor(post(urlEqualTo("/api/authorize"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(mockData.getPaymentNetworkSuccess())));
}
```

## COBOL Conversion Testing

### Packed Decimal Precision Testing

Test cases ensuring BigDecimal precision matches COBOL COMP-3 behavior.

#### COMP-3 Test Values (`templates/cobol-conversion/comp3-test-values.json`)
```json
{
  "precisionTests": [
    {
      "cobolField": "PIC S9(7)V99 COMP-3",
      "cobolValue": "0001234567F",
      "expectedBigDecimal": "12345.67",
      "scale": 2,
      "precision": 7,
      "roundingMode": "HALF_UP"
    },
    {
      "cobolField": "PIC S9(9)V9999 COMP-3", 
      "cobolValue": "000052500000F",
      "expectedBigDecimal": "52500.0000",
      "scale": 4,
      "precision": 9,
      "roundingMode": "HALF_UP"
    }
  ]
}
```

**Usage:**
```java
@Test
void testComp3Conversion() {
    String testData = testDataLoader.loadJson("comp3-test-values.json");
    Comp3TestCase testCase = objectMapper.readValue(testData, Comp3TestCase.class);
    
    BigDecimal result = comp3Converter.convert(testCase.getCobolValue());
    assertThat(result).isEqualTo(new BigDecimal(testCase.getExpectedBigDecimal()));
    assertThat(result.scale()).isEqualTo(testCase.getScale());
}
```

## Performance Test Data

### Load Test Scenarios

Performance test templates for validating response time requirements.

#### Load Test Configuration (`templates/performance/load-test-scenarios.json`)
```json
{
  "signOnLoad": {
    "endpoint": "/api/transactions/CC00",
    "method": "POST",
    "concurrentUsers": 100,
    "duration": "30s",
    "responseTimeThreshold": "200ms",
    "successRate": "99%"
  },
  "accountInquiry": {
    "endpoint": "/api/transactions/CC01",
    "method": "GET", 
    "concurrentUsers": 200,
    "duration": "60s",
    "responseTimeThreshold": "200ms",
    "successRate": "99.5%"
  }
}
```

## Security Test Configuration

### Test User Accounts

Security test user definitions with various roles and permissions.

#### Test Users (`config/security/test-users.json`)
```json
{
  "users": [
    {
      "username": "testuser",
      "password": "testpass123",
      "roles": ["USER"],
      "permissions": ["READ_ACCOUNT", "READ_TRANSACTION"]
    },
    {
      "username": "testadmin",
      "password": "adminpass123", 
      "roles": ["ADMIN"],
      "permissions": ["READ_ACCOUNT", "WRITE_ACCOUNT", "READ_TRANSACTION", "WRITE_TRANSACTION", "ADMIN_FUNCTIONS"]
    }
  ]
}
```

**Usage:**
```java
@Test
@WithMockUser(username = "testuser", roles = {"USER"})
void testUserAccess() {
    // Test with USER role permissions
}

@Test
@WithMockUser(username = "testadmin", roles = {"ADMIN"}) 
void testAdminAccess() {
    // Test with ADMIN role permissions
}
```

## Adding New Test Data

### Guidelines for New Test Data

When adding new test data, follow these consistency guidelines:

1. **File Naming**: Use kebab-case naming (e.g., `new-feature-test-data.json`)
2. **Data Anonymization**: Ensure no real customer data in test fixtures
3. **Schema Consistency**: Maintain field names matching JPA entity definitions
4. **Version Control**: Include all test data files in version control
5. **Documentation**: Update this README when adding new data types

### Example: Adding New Entity Test Data

1. **Create CSV file**: `data/csv/new-entity-test-data.csv`
2. **Create JSON fixtures**: `data/json/new-entity-requests.json`
3. **Create SQL scripts**: `data/sql/data/insert-test-new-entity.sql`
4. **Update configuration**: Add to appropriate `application-*.yml`
5. **Document usage**: Add examples to this README

### COBOL Pattern Consistency

Maintain consistency with original COBOL patterns when creating test data:

1. **Field Lengths**: Respect original COBOL PIC clause field lengths
2. **Data Types**: Use appropriate Java types matching COBOL equivalents
3. **Validation Rules**: Implement same validation logic as COBOL programs
4. **Error Codes**: Use identical error codes and messages from COBOL
5. **Business Logic**: Ensure test scenarios cover all COBOL program paths

## Best Practices

### Test Data Management

1. **Isolation**: Each test method should use isolated test data
2. **Cleanup**: Use `@DirtiesContext` for tests that modify shared state
3. **Performance**: Use `@Sql` with `TRUNCATE` for fast data cleanup
4. **Consistency**: Maintain referential integrity in test data relationships
5. **Anonymization**: Never use production data in test environments

### Configuration Management

1. **Profiles**: Use specific profiles for different test types
2. **Environment Variables**: Override configuration via environment variables
3. **Secrets**: Use test-specific credentials and API keys
4. **Documentation**: Document all configuration options and their purposes
5. **Validation**: Validate configuration values in test setup methods

### Container Testing

1. **Resource Limits**: Set appropriate memory and CPU limits for test containers
2. **Network Isolation**: Use dedicated networks for container tests
3. **Cleanup**: Ensure containers are properly cleaned up after tests
4. **Performance**: Share containers across test classes when possible
5. **Debugging**: Provide container logs access for test debugging

This test resources directory provides comprehensive support for validating the complete COBOL-to-Java modernization while maintaining functional parity and ensuring enterprise-grade quality standards.