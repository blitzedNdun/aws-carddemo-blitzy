# CardDemo Testing Strategy

## Overview

This document outlines the comprehensive testing approach for the CardDemo application's migration from mainframe COBOL/CICS to modern Java 21/Spring Boot architecture. The testing strategy ensures 100% functional parity with the original COBOL implementation while leveraging cloud-native testing frameworks and practices.

## Four-Layer Testing Strategy

The CardDemo testing architecture implements a modern cloud-native testing strategy that validates minimal-change functional parity between the original COBOL implementation and the modernized Spring Boot/React/PostgreSQL architecture.

### Layer 1: Unit Testing
- **Framework**: JUnit 5 with Mockito and AssertJ
- **Coverage Target**: 90% overall code coverage, 100% business logic coverage
- **Purpose**: Validates Java service class behavior equivalent to COBOL paragraphs
- **Key Feature**: COBOL-Java output comparison for identical functional results

### Layer 2: Integration Testing
- **Framework**: Spring Boot Test with Testcontainers
- **Coverage Target**: 100% API endpoint coverage
- **Purpose**: Validates data access parity and component integration
- **Key Feature**: PostgreSQL container-based testing with VSAM behavior emulation

### Layer 3: End-to-End Testing
- **Framework**: Cypress with React Testing Library
- **Coverage Target**: 100% user journey validation
- **Purpose**: Complete user workflow validation across frontend and backend
- **Key Feature**: Cross-browser testing and PF-key functionality simulation

### Layer 4: Performance Testing
- **Framework**: Spring Actuator with Micrometer, K6 load testing
- **Coverage Target**: 95% response time SLA compliance (<200ms)
- **Purpose**: Response time and throughput validation
- **Key Feature**: Real-time performance metrics and automated alerting

## Folder Structure

```
backend/src/test/
├── java/com/carddemo/
│   ├── service/                          # Unit tests for business logic
│   │   ├── SignOnServiceTest.java        # COSGN00C.cbl equivalent tests
│   │   ├── TransactionServiceTest.java   # COTRN00C.cbl equivalent tests
│   │   ├── AccountServiceTest.java       # COACTVWC.cbl equivalent tests
│   │   └── CardServiceTest.java          # COCRDVWC.cbl equivalent tests
│   ├── repository/                       # Data access layer tests
│   │   ├── AccountRepositoryTest.java    # VSAM ACCTDAT emulation tests
│   │   ├── TransactionRepositoryTest.java # VSAM TRANSACT emulation tests
│   │   └── CustomerRepositoryTest.java   # VSAM CUSTDAT emulation tests
│   ├── controller/                       # REST API integration tests
│   │   ├── TransactionControllerTest.java # REST endpoint validation
│   │   └── AccountControllerTest.java    # Controller layer testing
│   ├── integration/                      # End-to-end integration tests
│   │   ├── SecurityIntegrationTest.java  # Spring Security validation
│   │   ├── BatchProcessingTest.java      # Spring Batch job testing
│   │   └── SessionManagementTest.java    # Redis session testing
│   ├── util/                            # Utility and converter tests
│   │   ├── BigDecimalConverterTest.java  # COMP-3 precision tests
│   │   └── TestDataGeneratorTest.java    # Test data generation
│   └── performance/                      # Performance and load tests
│       ├── LoadTestConfiguration.java    # K6 load test setup
│       └── PerformanceMetricsTest.java   # Response time validation
├── resources/
│   ├── application-test.yml              # Test environment configuration
│   ├── testcontainers.properties         # Container configuration
│   ├── test-data/                       # Test datasets
│   │   ├── customer-test.csv            # Customer test data
│   │   ├── account-test.json            # Account test data
│   │   └── transaction-test.json        # Transaction test data
│   └── db/
│       └── test-schema.sql              # Test database schema
└── docker/
    └── docker-compose-test.yml          # Test environment containers
```

## Test Execution Commands

### Running Unit Tests

```bash
# Run all unit tests with coverage report
mvn test -Dtest="*Test" -Djacoco.skip=false

# Run specific service tests
mvn test -Dtest="com.carddemo.service.*Test"

# Run unit tests with parallel execution
mvn test -DforkCount=4 -DreuseForks=true

# Generate coverage report
mvn jacoco:report
```

### Running Integration Tests

```bash
# Run all integration tests (requires Docker)
mvn verify -Dtest="*IntegrationTest"

# Run Spring Boot integration tests with Testcontainers
mvn test -Dspring.profiles.active=test -Dtestcontainers.enabled=true

# Run specific integration test categories
mvn test -Dtest="*RepositoryTest" -DargLine="-Dtestcontainers.reuse.enable=true"

# Run security integration tests
mvn test -Dtest="SecurityIntegrationTest"
```

### Running End-to-End Tests

```bash
# Run Cypress end-to-end tests (requires frontend running)
cd frontend && npm run cypress:run

# Run specific E2E test suites
cd frontend && npm run cypress:run --spec "cypress/e2e/authentication.cy.js"

# Run E2E tests in headless mode
cd frontend && npm run cypress:run --headless

# Run cross-browser E2E tests
cd frontend && npm run cypress:run --browser chrome,firefox,edge
```

### Running Performance Tests

```bash
# Run K6 load tests
k6 run src/test/performance/load-test.js

# Run performance tests with metrics collection
mvn test -Dtest="PerformanceMetricsTest" -Dspring.profiles.active=performance

# Run batch processing performance tests
mvn test -Dtest="BatchProcessingTest" -Dbatch.performance.enabled=true

# Generate performance reports
mvn spring-boot:run -Dspring-boot.run.arguments="--performance.test.enabled=true"
```

### Running All Tests

```bash
# Full test suite execution
mvn clean verify -Dspring.profiles.active=test

# CI/CD pipeline test execution
mvn clean verify -Dtest.parallel=true -DforkCount=4 -Dtestcontainers.reuse.enable=true

# Test execution with quality gates
mvn clean verify sonar:sonar -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

## Coverage Requirements and Quality Gates

### Coverage Thresholds

| Test Category | Minimum Coverage | Quality Gate | Enforcement Level |
|---------------|------------------|--------------|-------------------|
| **Business Logic Methods** | 100% | Pipeline Blocking | Critical |
| **Overall Code Coverage** | 90% | Pipeline Blocking | High |
| **Integration Test Coverage** | 95% | Warning | Medium |
| **API Endpoint Coverage** | 100% | Pipeline Blocking | Critical |

### SonarQube Integration

```xml
<!-- Maven configuration for SonarQube integration -->
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>

<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.90</minimum>
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.85</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### Quality Gate Configuration

```bash
# Run tests with quality gate validation
mvn clean verify sonar:sonar \
  -Dsonar.coverage.exclusions="**/config/**,**/dto/**" \
  -Dsonar.cpd.exclusions="**/entity/**" \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

# Quality gate metrics
mvn sonar:sonar \
  -Dsonar.qualitygate.wait=true \
  -Dsonar.qualitygate.timeout=300
```

## Testcontainers Setup

### Prerequisites

```bash
# Ensure Docker is running
docker --version
docker-compose --version

# Verify Docker daemon accessibility
docker ps
```

### Container Configuration

#### PostgreSQL Test Container

```java
@SpringBootTest
@Testcontainers
class DatabaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("carddemo_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("db/test-schema.sql");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

#### Redis Test Container

```java
@SpringBootTest
@Testcontainers
class SessionManagementTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--appendonly", "yes");
    
    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
}
```

#### Docker Compose Test Environment

```yaml
# docker-compose-test.yml
version: '3.8'
services:
  postgres-test:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: carddemo_test
      POSTGRES_USER: test_user
      POSTGRES_PASSWORD: test_password
    ports:
      - "5432:5432"
    volumes:
      - ./src/test/resources/db/test-schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
      - ./src/test/resources/test-data:/docker-entrypoint-initdb.d/data

  redis-test:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes

  carddemo-app-test:
    build: .
    depends_on:
      - postgres-test
      - redis-test
    environment:
      SPRING_PROFILES_ACTIVE: test
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-test:5432/carddemo_test
      SPRING_REDIS_HOST: redis-test
    ports:
      - "8080:8080"
```

### Testcontainers Best Practices

```properties
# testcontainers.properties
testcontainers.reuse.enable=true
testcontainers.image.substitutor=org.testcontainers.utility.NoOpImageSubstitutor
```

## Performance Testing Guidelines

### Performance Test Categories

#### Response Time Validation

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PerformanceMetricsTest {
    
    @Test
    @DisplayName("REST API response time validation - <200ms SLA")
    void validateApiResponseTime() {
        StopWatch stopWatch = new StopWatch();
        
        stopWatch.start();
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/accounts/1000000001", String.class);
        stopWatch.stop();
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(200);
    }
}
```

#### Load Testing with K6

```javascript
// load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 }, // Ramp up
    { duration: '5m', target: 100 }, // Stay at 100 users
    { duration: '2m', target: 200 }, // Ramp up to 200 users
    { duration: '5m', target: 200 }, // Stay at 200 users
    { duration: '2m', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests under 200ms
    http_req_failed: ['rate<0.1'],    // Error rate under 10%
  },
};

export default function () {
  let response = http.get('http://localhost:8080/api/accounts/1000000001');
  
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });
  
  sleep(1);
}
```

#### Batch Processing Performance

```java
@SpringBootTest
class BatchProcessingPerformanceTest {
    
    @Test
    @DisplayName("Daily batch processing completes within 4-hour window")
    void validateBatchProcessingWindow() {
        LocalDateTime startTime = LocalDateTime.now();
        
        JobExecution jobExecution = jobLauncher.run(
            dailyProcessingJob, 
            new JobParametersBuilder()
                .addString("date", startTime.format(DateTimeFormatter.ISO_DATE))
                .toJobParameters()
        );
        
        LocalDateTime endTime = LocalDateTime.now();
        Duration processingTime = Duration.between(startTime, endTime);
        
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(processingTime.toHours()).isLessThan(4);
    }
}
```

### Performance Monitoring Setup

```yaml
# application-performance.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: carddemo
  endpoint:
    metrics:
      enabled: true

logging:
  level:
    com.carddemo: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
```

## Parallel Test Execution Configuration

### Maven Parallel Execution

```xml
<!-- Maven Surefire Plugin Configuration -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.1.2</version>
    <configuration>
        <parallel>all</parallel>
        <threadCount>4</threadCount>
        <forkCount>4</forkCount>
        <reuseForks>true</reuseForks>
        <useUnlimitedThreads>false</useUnlimitedThreads>
        <perCoreThreadCount>true</perCoreThreadCount>
        <includes>
            <include>**/*Test.java</include>
        </includes>
        <systemPropertyVariables>
            <testcontainers.reuse.enable>true</testcontainers.reuse.enable>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

### JUnit 5 Parallel Execution

```properties
# junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
junit.jupiter.execution.parallel.config.strategy=dynamic
junit.jupiter.execution.parallel.config.dynamic.factor=2
```

### Test Class Parallel Configuration

```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
@Execution(ExecutionMode.CONCURRENT)
class ParallelExecutionTest {
    
    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testConcurrentExecution1() {
        // Test implementation
    }
    
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void testSequentialExecution() {
        // Test requiring sequential execution
    }
}
```

### Container Resource Management

```java
@Testcontainers
class OptimizedContainerTest {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("carddemo_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // Enable container reuse across test runs
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);
}
```

## CI/CD Integration

### GitHub Actions Integration

```yaml
# .github/workflows/test.yml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: carddemo_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        
    - name: Run tests with coverage
      run: mvn clean verify -Dspring.profiles.active=test
      
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: target/surefire-reports/*.xml
        reporter: java-junit
        
    - name: SonarQube analysis
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
      run: mvn sonar:sonar
```

### Quality Gate Validation

```bash
#!/bin/bash
# quality-gate.sh
echo "Running quality gate validation..."

# Execute full test suite
mvn clean verify -Dtest.parallel=true

# Check coverage thresholds
mvn jacoco:check

# Run SonarQube analysis
mvn sonar:sonar -Dsonar.qualitygate.wait=true

# Validate performance benchmarks
mvn test -Dtest="PerformanceMetricsTest" -Dperformance.validation=true

echo "Quality gate validation completed successfully"
```

## Troubleshooting

### Common Test Issues

#### Testcontainers Issues

```bash
# Check Docker daemon status
docker system info

# Clean up stopped containers
docker container prune -f

# Reset Testcontainers state
rm -rf ~/.testcontainers/
```

#### Database Connection Issues

```bash
# Verify PostgreSQL connectivity
pg_isready -h localhost -p 5432 -U test_user

# Check database logs
docker logs <postgres_container_id>

# Reset test database
psql -h localhost -U test_user -d carddemo_test -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

#### Memory Issues

```bash
# Increase Maven memory allocation
export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=256m"

# Monitor JVM memory usage
jstat -gc <java_process_id> 5s
```

### Test Data Management

#### Test Data Reset

```sql
-- reset-test-data.sql
TRUNCATE TABLE transactions, transaction_category_balance, card_data, account_data, customer_data RESTART IDENTITY CASCADE;

-- Load fresh test data
\copy customer_data FROM 'src/test/resources/test-data/customer-test.csv' WITH CSV HEADER;
\copy account_data FROM 'src/test/resources/test-data/account-test.csv' WITH CSV HEADER;
```

#### Test Environment Cleanup

```bash
#!/bin/bash
# cleanup-test-env.sh
echo "Cleaning up test environment..."

# Stop test containers
docker-compose -f docker-compose-test.yml down -v

# Remove test data volumes
docker volume prune -f

# Clean Maven test cache
mvn clean

echo "Test environment cleanup completed"
```

## Additional Resources

- [Spring Boot Testing Documentation](https://spring.io/guides/gs/testing-web/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Cypress Testing Framework](https://docs.cypress.io/)
- [K6 Performance Testing](https://k6.io/docs/)
- [SonarQube Quality Gates](https://docs.sonarqube.org/latest/user-guide/quality-gates/)

---

**Note**: This testing strategy ensures 100% functional parity with the original COBOL implementation while leveraging modern cloud-native testing practices. All tests validate identical behavior between the legacy mainframe system and the modernized Spring Boot application.