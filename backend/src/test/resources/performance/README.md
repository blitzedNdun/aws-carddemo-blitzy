# Performance Testing Resources Guide

## Table of Contents

1. [Overview](#overview)
2. [Test Resource Structure](#test-resource-structure)
3. [Performance Testing Tools](#performance-testing-tools)
4. [SLA Requirements and Validation](#sla-requirements-and-validation)
5. [Mainframe Baseline Comparison](#mainframe-baseline-comparison)
6. [Test Execution Procedures](#test-execution-procedures)
7. [Metrics Collection and Analysis](#metrics-collection-and-analysis)
8. [Test Configurations](#test-configurations)
9. [Adding New Test Data](#adding-new-test-data)
10. [Troubleshooting Guide](#troubleshooting-guide)
11. [Continuous Integration Integration](#continuous-integration-integration)
12. [References](#references)

## Overview

This directory contains comprehensive performance testing resources for the CardDemo COBOL-to-Java migration project. The performance testing framework validates that the modernized Spring Boot application meets or exceeds the performance characteristics of the original mainframe COBOL/CICS system while maintaining 100% functional parity.

### Project Context

**Migration Scope**: Complete modernization from IBM mainframe (z/OS, COBOL/CICS, VSAM) to cloud-native architecture (Java 21, Spring Boot 3.2.x, PostgreSQL 16.x, React 18.x) with containerized deployment on Kubernetes.

**Critical Performance Requirements**:
- REST API transactions: **< 200ms response time (95th percentile)**
- Batch processing window: **< 4 hours daily processing**
- Transaction throughput: **10,000+ TPS capacity**
- Financial calculation precision: **100% accuracy matching COBOL COMP-3**

### Testing Philosophy

Performance testing follows a **minimal-change validation approach** ensuring the modernized system provides identical performance characteristics to the legacy mainframe implementation without introducing performance regressions during the technology migration.

## Test Resource Structure

The performance testing resources are organized in a hierarchical structure supporting different test types, environments, and execution scenarios:

```
backend/src/test/resources/performance/
├── README.md                          # This documentation file
├── k6/                                 # K6 load testing scripts
│   ├── scripts/
│   │   ├── api-load-test.js           # REST API load testing
│   │   ├── auth-performance.js        # Authentication flow testing
│   │   ├── transaction-stress.js      # Transaction processing stress test
│   │   └── batch-simulation.js        # Batch processing simulation
│   ├── configs/
│   │   ├── development.json           # Development environment config
│   │   ├── staging.json               # Staging environment config
│   │   └── production-like.json       # Production-like environment config
│   └── data/
│       ├── test-users.csv             # Test user credentials
│       ├── test-accounts.json         # Account test data
│       └── test-transactions.json     # Transaction test data
├── jmeter/                            # Apache JMeter test plans
│   ├── test-plans/
│   │   ├── api-performance.jmx        # REST API performance testing
│   │   ├── batch-load.jmx             # Batch processing load testing
│   │   └── stress-test.jmx            # System stress testing
│   ├── configs/
│   │   ├── environment.properties     # Environment-specific properties
│   │   └── test-data.properties       # Test data configuration
│   └── results/
│       └── .gitkeep                   # Results directory placeholder
├── gatling/                           # Gatling load testing scenarios
│   ├── simulations/
│   │   ├── ApiLoadSimulation.scala    # API load simulation
│   │   ├── UserJourneySimulation.scala # End-to-end user journey
│   │   └── SpikeTestSimulation.scala  # Spike testing scenarios
│   ├── data/
│   │   ├── feeders/                   # Test data feeders
│   │   └── templates/                 # Request templates
│   └── conf/
│       ├── gatling.conf               # Gatling configuration
│       └── logback.xml                # Logging configuration
├── monitoring/                        # Performance monitoring configurations
│   ├── prometheus/
│   │   ├── rules.yml                  # Alert rules for performance metrics
│   │   └── targets.json               # Monitoring targets configuration
│   ├── grafana/
│   │   ├── dashboards/
│   │   │   ├── performance-overview.json
│   │   │   ├── api-response-times.json
│   │   │   └── batch-processing.json
│   │   └── datasources/
│   │       └── prometheus.yml
│   └── spring-actuator/
│       ├── micrometer-config.yml      # Custom metrics configuration
│       └── actuator-endpoints.yml     # Endpoint exposure configuration
├── test-data/                         # Performance test datasets
│   ├── baseline/
│   │   ├── cobol-performance.csv      # Original mainframe benchmarks
│   │   └── expected-metrics.json      # Expected performance targets
│   ├── synthetic/
│   │   ├── large-dataset.sql          # Large volume test data
│   │   ├── stress-data.json           # Stress testing datasets
│   │   └── edge-cases.csv             # Edge case scenarios
│   └── anonymized/
│       ├── prod-like-accounts.csv     # Production-like account data
│       └── transaction-patterns.json  # Real transaction patterns
├── environments/                      # Environment-specific configurations
│   ├── docker-compose/
│   │   ├── performance-test.yml       # Docker Compose for testing
│   │   └── monitoring-stack.yml       # Monitoring infrastructure
│   ├── kubernetes/
│   │   ├── performance-namespace.yaml # K8s performance testing namespace
│   │   ├── resource-quotas.yaml       # Resource allocation limits
│   │   └── network-policies.yaml      # Network isolation policies
│   └── configs/
│       ├── application-perf.yml       # Spring Boot performance profile
│       ├── postgresql-tuning.conf     # Database performance tuning
│       └── redis-performance.conf     # Redis performance configuration
├── scripts/                           # Automation and utility scripts
│   ├── setup/
│   │   ├── prepare-environment.sh     # Environment preparation
│   │   ├── load-test-data.sh          # Test data loading
│   │   └── configure-monitoring.sh    # Monitoring stack setup
│   ├── execution/
│   │   ├── run-all-tests.sh           # Execute complete test suite
│   │   ├── run-baseline-comparison.sh # Mainframe baseline comparison
│   │   └── generate-reports.sh        # Performance report generation
│   └── analysis/
│       ├── analyze-results.py         # Performance data analysis
│       ├── compare-baselines.py       # Baseline comparison analysis
│       └── generate-sla-report.py     # SLA compliance reporting
└── reports/                           # Performance test reports and analysis
    ├── templates/
    │   ├── performance-report.html     # HTML report template
    │   └── sla-compliance.md           # SLA compliance template
    └── generated/
        └── .gitkeep                   # Generated reports directory
```

### Directory Descriptions

- **`k6/`**: Lightweight JavaScript-based load testing scripts optimized for REST API testing
- **`jmeter/`**: GUI-based performance testing plans for complex scenarios and batch processing
- **`gatling/`**: Scala-based high-performance testing for advanced load simulation
- **`monitoring/`**: Prometheus, Grafana, and Spring Actuator configurations for real-time metrics
- **`test-data/`**: Performance testing datasets including mainframe baselines and synthetic data
- **`environments/`**: Infrastructure configurations for different testing environments
- **`scripts/`**: Automation scripts for test execution, environment setup, and analysis
- **`reports/`**: Performance testing reports, analysis, and SLA compliance documentation

## Performance Testing Tools

### K6 Load Testing

**Purpose**: Lightweight, developer-friendly load testing focused on REST API performance validation.

**Key Features**:
- JavaScript-based test scripts with modular configuration
- Built-in support for HTTP/2, WebSockets, and gRPC protocols
- Real-time metrics streaming to Prometheus and Grafana
- Threshold-based pass/fail criteria for automated CI/CD integration

**Usage Scenarios**:
- REST API response time validation (<200ms SLA)
- Authentication flow performance testing
- Transaction processing stress testing
- API endpoint capacity planning

**Sample K6 Test Execution**:
```bash
# Install K6 (if not already available)
curl https://github.com/grafana/k6/releases/download/v0.47.0/k6-v0.47.0-linux-amd64.tar.gz -L | tar xvz --strip-components 1

# Execute API load test with staging configuration
k6 run --config k6/configs/staging.json k6/scripts/api-load-test.js

# Run stress test with custom thresholds
k6 run --vus 500 --duration 10m --threshold http_req_duration=p(95)<200 k6/scripts/transaction-stress.js

# Execute performance test with Prometheus output
k6 run --out prometheus-remote-write=http://prometheus:9090/api/v1/write k6/scripts/api-load-test.js
```

### Apache JMeter

**Purpose**: Comprehensive performance testing platform with GUI support for complex test plan creation and batch processing validation.

**Key Features**:
- Visual test plan designer with extensive protocol support
- Built-in reporting and result analysis capabilities
- Distributed testing support for high-load scenarios
- Integration with CI/CD pipelines through command-line execution

**Usage Scenarios**:
- Batch processing performance validation (4-hour window compliance)
- Complex user journey testing with multiple protocol interactions
- Database performance testing with JDBC samplers
- System stress testing with gradual load ramping

**Sample JMeter Test Execution**:
```bash
# Execute API performance test plan in non-GUI mode
jmeter -n -t jmeter/test-plans/api-performance.jmx -l results/api-performance-results.jtl -e -o results/html-report/

# Run batch load test with custom properties
jmeter -n -t jmeter/test-plans/batch-load.jmx -Jusers=1000 -Jramp-up=300 -l results/batch-load-results.jtl

# Execute stress test with distributed testing
jmeter -n -t jmeter/test-plans/stress-test.jmx -R server1,server2,server3 -l results/distributed-results.jtl

# Generate performance report from existing results
jmeter -g results/api-performance-results.jtl -o reports/generated/jmeter-report/
```

### Gatling Load Testing

**Purpose**: High-performance, Scala-based testing platform optimized for maximum throughput and detailed performance analysis.

**Key Features**:
- Asynchronous, non-blocking architecture for efficient resource utilization
- Advanced simulation scenarios with realistic user behavior modeling
- Comprehensive real-time and post-test reporting with detailed metrics
- Integration with DevOps toolchains and continuous performance testing

**Usage Scenarios**:
- High-throughput capacity testing (10,000+ TPS validation)
- Advanced user journey simulation with realistic think times
- Performance regression testing in continuous deployment pipelines
- Detailed response time distribution analysis

**Sample Gatling Test Execution**:
```bash
# Execute API load simulation
gatling.sh -sf gatling/simulations -s ApiLoadSimulation -rf gatling/results

# Run user journey simulation with custom parameters
gatling.sh -sf gatling/simulations -s UserJourneySimulation -Dusers=2000 -Dduration=15m

# Execute spike test simulation
gatling.sh -sf gatling/simulations -s SpikeTestSimulation -rf gatling/results

# Generate custom report from simulation results
gatling.sh -ro results/user-journey-20240115-143022 -rf reports/generated/gatling/
```

### Spring Boot Actuator & Micrometer Integration

**Purpose**: Production-ready monitoring and metrics collection integrated directly into the Spring Boot application.

**Key Features**:
- Custom business metrics for financial transaction monitoring
- JVM performance metrics (memory, GC, threads)
- HTTP request metrics with response time tracking
- Database connection pool and query performance metrics

**Configuration Example**:
```yaml
# backend/src/main/resources/application-perf.yml
management:
  endpoints:
    web:
      exposure:
        include: "health,metrics,prometheus,info"
  metrics:
    export:
      prometheus:
        enabled: true
        descriptions: true
    distribution:
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      percentiles-histogram:
        http.server.requests: true
  endpoint:
    metrics:
      enabled: true
    health:
      show-details: always

# Custom metrics configuration
spring:
  application:
    name: carddemo-performance
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      leak-detection-threshold: 60000
```

## SLA Requirements and Validation

### Performance Service Level Agreements

| Performance Metric | Target Value | Measurement Method | Validation Frequency |
|-------------------|--------------|-------------------|---------------------|
| **REST API Response Time** | 95th percentile < 200ms | Micrometer Timer metrics | Continuous monitoring |
| **Authentication Flow** | < 150ms average | K6 load testing + Prometheus | Per deployment |
| **Transaction Processing** | < 100ms median | Spring Actuator custom metrics | Real-time |
| **Batch Processing Window** | < 4 hours daily completion | Spring Batch JobRepository | Daily validation |
| **Database Query Performance** | < 50ms average execution | PostgreSQL pg_stat_statements | Continuous monitoring |
| **System Availability** | 99.9% uptime during business hours | Kubernetes health probes | 24/7 monitoring |
| **Memory Utilization** | < 80% heap usage sustained | JVM metrics via Micrometer | Continuous monitoring |
| **CPU Utilization** | < 70% average load | Container metrics | Continuous monitoring |

### SLA Validation Procedures

#### REST API Response Time Validation

**Objective**: Validate that 95% of REST API requests complete within 200ms under normal load conditions.

**Validation Steps**:
1. **Load Test Execution**:
   ```bash
   # Execute K6 load test with SLA thresholds
   k6 run --threshold http_req_duration=p(95)<200 \
          --threshold http_req_failed=rate<0.01 \
          k6/scripts/api-load-test.js
   ```

2. **Prometheus Query Validation**:
   ```promql
   # 95th percentile response time over 5-minute window
   histogram_quantile(0.95, 
     rate(http_server_requests_seconds_bucket{uri=~"/api/.*"}[5m])
   ) < 0.2
   ```

3. **Spring Actuator Metrics Check**:
   ```bash
   # Retrieve response time metrics
   curl http://localhost:8080/actuator/metrics/http.server.requests \
        -H "Accept: application/json" | jq '.measurements'
   ```

#### Batch Processing Window Validation

**Objective**: Ensure all Spring Batch jobs complete within the 4-hour nightly processing window.

**Validation Steps**:
1. **Job Execution Monitoring**:
   ```bash
   # Monitor batch job execution times
   kubectl logs -f job/daily-processing-job -n carddemo-production
   ```

2. **Database Query for Job Duration**:
   ```sql
   -- Query Spring Batch job execution times
   SELECT job_instance_id, job_name, start_time, end_time,
          EXTRACT(EPOCH FROM (end_time - start_time))/3600 as duration_hours
   FROM batch_job_execution 
   WHERE job_name IN ('dailyProcessingJob', 'interestCalculationJob', 'statementGenerationJob')
     AND start_time >= CURRENT_DATE - INTERVAL '7 days'
   ORDER BY start_time DESC;
   ```

3. **Prometheus Alert Rule**:
   ```yaml
   groups:
     - name: batch_processing_sla
       rules:
         - alert: BatchProcessingWindowExceeded
           expr: spring_batch_job_seconds{status="COMPLETED"} > 14400
           labels:
             severity: critical
           annotations:
             summary: "Batch processing exceeded 4-hour window"
             description: "Job {{ $labels.job_name }} took {{ $value }}s to complete"
   ```

### Performance Baseline Establishment

**Baseline Metrics Collection**:
```bash
# Establish performance baseline with clean environment
scripts/execution/run-baseline-comparison.sh --environment=staging --duration=30m

# Generate baseline performance report
scripts/analysis/generate-sla-report.py --baseline=true --output=reports/generated/baseline-$(date +%Y%m%d).html
```

**Baseline Validation Criteria**:
- All API endpoints must meet response time SLAs under normal load
- Batch processing must complete within time windows
- Memory and CPU utilization must remain within acceptable thresholds
- No performance regressions compared to previous baseline measurements

## Mainframe Baseline Comparison

### COBOL System Performance Characteristics

The performance testing framework includes comprehensive comparison with the original mainframe COBOL/CICS system to ensure the modernized Java application meets or exceeds legacy performance benchmarks.

#### Original Mainframe Performance Baselines

| COBOL/CICS Component | Legacy Performance | Modern Equivalent | Target Performance |
|---------------------|-------------------|-------------------|-------------------|
| **CICS Transaction Response** | 150-250ms average | Spring Boot REST API | < 200ms (95th percentile) |
| **VSAM KSDS Read Operations** | 5-15ms per record | PostgreSQL B-tree lookup | < 10ms per query |
| **Batch Job Processing** | 3.5-4 hours nightly | Spring Batch jobs | < 4 hours daily window |
| **COBOL COMP-3 Calculations** | Deterministic precision | BigDecimal operations | 100% precision match |
| **COMMAREA State Management** | In-memory session state | Redis session store | Equivalent latency |
| **CICS Transaction Throughput** | 8,000-12,000 TPS peak | Spring Boot concurrent processing | 10,000+ TPS capacity |

### Baseline Comparison Methodology

#### 1. Functional Equivalence Validation

**COBOL Program Logic Verification**:
```bash
# Execute parallel comparison testing
scripts/execution/run-baseline-comparison.sh \
  --cobol-reference=test-data/baseline/cobol-performance.csv \
  --java-target=http://localhost:8080 \
  --test-scenarios=all \
  --output=reports/generated/equivalence-comparison.json
```

**Sample Comparison Test Script**:
```javascript
// k6/scripts/baseline-comparison.js
import { check } from 'k6';
import { cobolBaseline } from '../data/cobol-baseline.js';

export default function() {
    // Test identical transaction with COBOL baseline data
    const response = http.post('/api/transactions/CC01', {
        userId: cobolBaseline.userId,
        accountId: cobolBaseline.accountId,
        amount: cobolBaseline.transactionAmount
    });
    
    // Validate response time meets or exceeds COBOL performance
    check(response, {
        'response_time_better_than_cobol': (r) => 
            r.timings.duration <= cobolBaseline.maxResponseTime,
        'calculation_matches_cobol': (r) => 
            JSON.parse(r.body).balance === cobolBaseline.expectedBalance,
        'business_logic_identical': (r) => 
            JSON.parse(r.body).interestAmount === cobolBaseline.interestAmount
    });
}
```

#### 2. Performance Regression Detection

**Automated Baseline Comparison**:
```python
# scripts/analysis/compare-baselines.py
import pandas as pd
import numpy as np
from datetime import datetime

def compare_performance_baselines(cobol_baseline_file, java_results_file):
    """
    Compare Java Spring Boot performance against COBOL baseline metrics
    """
    cobol_df = pd.read_csv(cobol_baseline_file)
    java_df = pd.read_csv(java_results_file)
    
    comparison_results = {
        'response_time_improvement': calculate_improvement_percentage(
            cobol_df['response_time_ms'].mean(),
            java_df['response_time_ms'].mean()
        ),
        'throughput_comparison': compare_throughput_metrics(cobol_df, java_df),
        'precision_validation': validate_calculation_precision(cobol_df, java_df),
        'regression_analysis': detect_performance_regressions(cobol_df, java_df)
    }
    
    return comparison_results

def generate_baseline_report(comparison_results, output_file):
    """
    Generate comprehensive baseline comparison report
    """
    report_template = """
    # Mainframe Baseline Comparison Report
    
    ## Executive Summary
    - Response Time: {response_time_improvement}% improvement over COBOL
    - Throughput: {throughput_comparison}
    - Calculation Precision: {precision_validation}
    
    ## Detailed Analysis
    {regression_analysis}
    
    Generated: {timestamp}
    """
    
    with open(output_file, 'w') as f:
        f.write(report_template.format(
            **comparison_results,
            timestamp=datetime.now().isoformat()
        ))
```

#### 3. Business Logic Precision Validation

**Financial Calculation Accuracy Testing**:
```bash
# Validate BigDecimal precision matches COBOL COMP-3
scripts/analysis/validate-precision.sh \
  --cobol-calculations=test-data/baseline/comp3-calculations.csv \
  --java-endpoint=http://localhost:8080/api/interest/calculate \
  --tolerance=0.00 \
  --output=reports/generated/precision-validation.json
```

**Sample Precision Validation**:
```sql
-- PostgreSQL precision validation query
WITH cobol_baseline AS (
    SELECT account_id, expected_interest, expected_balance 
    FROM cobol_calculation_baseline
),
java_results AS (
    SELECT account_id, calculated_interest, current_balance
    FROM daily_interest_calculation
    WHERE calculation_date = CURRENT_DATE
)
SELECT 
    cb.account_id,
    cb.expected_interest,
    jr.calculated_interest,
    CASE 
        WHEN cb.expected_interest = jr.calculated_interest THEN 'MATCH'
        ELSE 'MISMATCH'
    END as precision_validation,
    ABS(cb.expected_interest - jr.calculated_interest) as variance
FROM cobol_baseline cb
JOIN java_results jr ON cb.account_id = jr.account_id
ORDER BY variance DESC;
```

### Continuous Baseline Monitoring

**Automated Performance Regression Detection**:
```yaml
# monitoring/prometheus/rules.yml
groups:
  - name: baseline_performance_monitoring
    rules:
      - alert: PerformanceRegressionDetected
        expr: |
          histogram_quantile(0.95, 
            rate(http_server_requests_seconds_bucket[5m])
          ) > 0.25  # 25% worse than COBOL baseline
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Performance regression detected vs COBOL baseline"
          description: "API response time {{ $value }}s exceeds baseline threshold"
          
      - alert: ThroughputBelowBaseline
        expr: |
          rate(http_server_requests_total[5m]) < 8000  # Below CICS baseline TPS
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "Throughput below COBOL baseline"
          description: "Current TPS {{ $value }} below mainframe baseline"
```

## Test Execution Procedures

### Environment Preparation

#### 1. Local Development Environment Setup

**Prerequisites**:
- Docker Desktop 4.x+ with 8GB+ memory allocation
- Java 21 LTS with Maven 3.9+
- Node.js 20.x LTS for frontend build
- kubectl CLI for Kubernetes interaction

**Environment Initialization**:
```bash
# Clone repository and navigate to performance testing directory
cd backend/src/test/resources/performance

# Execute environment preparation script
chmod +x scripts/setup/prepare-environment.sh
./scripts/setup/prepare-environment.sh --environment=local

# Start Docker Compose stack for performance testing
docker-compose -f environments/docker-compose/performance-test.yml up -d

# Verify services are healthy
docker-compose -f environments/docker-compose/performance-test.yml ps
```

**Service Health Verification**:
```bash
# Check Spring Boot application health
curl http://localhost:8080/actuator/health

# Verify PostgreSQL connectivity
docker exec carddemo-postgres pg_isready -U carddemo

# Confirm Redis session store availability
docker exec carddemo-redis redis-cli ping

# Test Prometheus metrics endpoint
curl http://localhost:8080/actuator/prometheus | head -20
```

#### 2. Kubernetes Performance Testing Environment

**Namespace and Resource Allocation**:
```bash
# Create dedicated performance testing namespace
kubectl apply -f environments/kubernetes/performance-namespace.yaml

# Configure resource quotas for testing isolation
kubectl apply -f environments/kubernetes/resource-quotas.yaml

# Deploy monitoring stack (Prometheus, Grafana)
kubectl apply -f environments/kubernetes/monitoring-stack.yaml

# Verify namespace resource allocation
kubectl describe quota performance-testing-quota -n carddemo-performance
```

**Application Deployment for Testing**:
```bash
# Deploy application with performance-optimized configuration
helm install carddemo-perf ./helm/carddemo \
  --namespace carddemo-performance \
  --values environments/configs/performance-values.yaml \
  --wait --timeout=300s

# Verify pod readiness and resource allocation
kubectl get pods -n carddemo-performance -o wide

# Check horizontal pod autoscaler configuration
kubectl describe hpa carddemo-perf-hpa -n carddemo-performance
```

### Test Data Preparation

#### 1. Synthetic Test Data Generation

**Large Volume Dataset Creation**:
```bash
# Generate synthetic test data for performance testing
scripts/setup/load-test-data.sh \
  --size=large \
  --accounts=100000 \
  --transactions=1000000 \
  --customers=50000 \
  --output=test-data/synthetic/

# Load test data into PostgreSQL
docker exec carddemo-postgres psql -U carddemo -d carddemo \
  -f /test-data/synthetic/large-dataset.sql

# Verify data loading completion and statistics
docker exec carddemo-postgres psql -U carddemo -d carddemo \
  -c "SELECT table_name, n_tup_ins, n_tup_upd FROM pg_stat_user_tables;"
```

#### 2. Anonymized Production-like Data

**Data Masking and Preparation**:
```bash
# Process anonymized production data (if available)
scripts/setup/prepare-anonymized-data.sh \
  --source=test-data/anonymized/ \
  --target=postgresql://carddemo:password@localhost:5432/carddemo \
  --mask-pii=true

# Validate data integrity after masking
scripts/analysis/validate-test-data.py \
  --database-url=postgresql://carddemo:password@localhost:5432/carddemo \
  --output=reports/generated/data-validation.json
```

### Test Execution Commands

#### 1. Comprehensive Performance Test Suite

**Full Test Suite Execution**:
```bash
# Execute complete performance testing suite
scripts/execution/run-all-tests.sh \
  --environment=staging \
  --duration=30m \
  --concurrent-users=1000 \
  --ramp-up=5m \
  --tools=k6,jmeter,gatling \
  --output-dir=reports/generated/$(date +%Y%m%d-%H%M%S)

# Monitor test execution progress
tail -f logs/performance-test-execution.log
```

**Individual Tool Execution**:

**K6 Load Testing**:
```bash
# API response time validation
k6 run --vus 500 --duration 15m \
  --threshold http_req_duration=p(95)<200 \
  --threshold http_req_failed=rate<0.01 \
  --out prometheus-remote-write=http://localhost:9090/api/v1/write \
  k6/scripts/api-load-test.js

# Authentication flow performance testing
k6 run --vus 200 --duration 10m \
  --env USERS_CSV=k6/data/test-users.csv \
  k6/scripts/auth-performance.js

# Transaction processing stress test
k6 run --stages='[{"duration":"5m","target":100},{"duration":"10m","target":500},{"duration":"5m","target":0}]' \
  k6/scripts/transaction-stress.js
```

**JMeter Execution**:
```bash
# Execute API performance test plan
jmeter -n -t jmeter/test-plans/api-performance.jmx \
  -Jusers=1000 -Jramp-up=300 -Jduration=1800 \
  -l results/api-performance-$(date +%Y%m%d-%H%M%S).jtl \
  -e -o reports/generated/jmeter/api-performance/

# Batch processing load testing
jmeter -n -t jmeter/test-plans/batch-load.jmx \
  -Jbatch_size=10000 -Jprocessing_threads=4 \
  -l results/batch-load-$(date +%Y%m%d-%H%M%S).jtl

# System stress testing with distributed execution
jmeter -n -t jmeter/test-plans/stress-test.jmx \
  -R load-generator-1:1099,load-generator-2:1099 \
  -l results/distributed-stress-$(date +%Y%m%d-%H%M%S).jtl
```

**Gatling Simulation Execution**:
```bash
# API load simulation with detailed analysis
gatling.sh -sf gatling/simulations \
  -s ApiLoadSimulation \
  -Dusers=2000 -Dduration=20m \
  -rf reports/generated/gatling/api-load-$(date +%Y%m%d-%H%M%S)

# User journey simulation for end-to-end testing
gatling.sh -sf gatling/simulations \
  -s UserJourneySimulation \
  -Dconcurrent_users=500 -Jthink_time=2s \
  -rf reports/generated/gatling/user-journey/

# Spike testing simulation
gatling.sh -sf gatling/simulations \
  -s SpikeTestSimulation \
  -Dspike_users=5000 -Dspike_duration=2m \
  -rf reports/generated/gatling/spike-test/
```

#### 2. Automated Test Execution Pipeline

**CI/CD Pipeline Integration**:
```bash
# Execute performance testing in CI/CD pipeline
scripts/execution/ci-performance-test.sh \
  --environment=staging \
  --test-type=regression \
  --baseline-comparison=true \
  --sla-validation=true \
  --output-format=junit \
  --results-dir=reports/ci/

# Generate performance gate decision
scripts/analysis/performance-gate.py \
  --results=reports/ci/performance-results.json \
  --baseline=test-data/baseline/performance-baseline.json \
  --sla-config=configs/sla-thresholds.yaml \
  --gate-decision=reports/ci/gate-decision.json
```

### Test Environment Management

#### 1. Environment Lifecycle Management

**Environment Provisioning**:
```bash
# Provision dedicated performance testing environment
scripts/setup/provision-perf-environment.sh \
  --provider=kubernetes \
  --cluster=performance-testing \
  --monitoring=true \
  --persistent-storage=true

# Configure auto-scaling for load testing
kubectl apply -f environments/kubernetes/autoscaling-config.yaml
```

**Environment Cleanup**:
```bash
# Clean up test environment after execution
scripts/setup/cleanup-environment.sh \
  --environment=performance-testing \
  --preserve-reports=true \
  --remove-test-data=true

# Archive test results for historical analysis
scripts/execution/archive-results.sh \
  --source=reports/generated/ \
  --archive-location=s3://carddemo-performance-archives/ \
  --retention-days=90
```

## Metrics Collection and Analysis

### Real-time Monitoring During Testing

#### 1. Prometheus Metrics Collection

**Key Metrics Categories**:

**Application Performance Metrics**:
```promql
# API response time 95th percentile
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Transaction throughput (requests per second)
rate(http_server_requests_total[5m])

# Error rate percentage
rate(http_server_requests_total{status=~"5.."}[5m]) / rate(http_server_requests_total[5m]) * 100

# JVM heap memory utilization
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Database connection pool active connections
hikaricp_connections_active{pool="carddemo"}

# Custom business metrics - transaction processing time
custom_transaction_processing_seconds_bucket
```

**Infrastructure Metrics**:
```promql
# CPU utilization percentage
rate(container_cpu_usage_seconds_total[5m]) * 100

# Memory utilization percentage
container_memory_working_set_bytes / container_spec_memory_limit_bytes * 100

# Network I/O rates
rate(container_network_receive_bytes_total[5m])
rate(container_network_transmit_bytes_total[5m])

# Disk I/O operations per second
rate(container_fs_reads_total[5m])
rate(container_fs_writes_total[5m])
```

#### 2. Spring Boot Actuator Integration

**Custom Metrics Configuration**:
```java
// Custom metrics for financial transaction monitoring
@Component
public class PerformanceMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer transactionProcessingTimer;
    private final Counter financialCalculationCounter;
    
    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.transactionProcessingTimer = Timer.builder("custom.transaction.processing.time")
            .description("Time spent processing financial transactions")
            .tags("component", "transaction-service")
            .register(meterRegistry);
        this.financialCalculationCounter = Counter.builder("custom.financial.calculations.total")
            .description("Total number of financial calculations performed")
            .tags("calculation", "interest")
            .register(meterRegistry);
    }
    
    public void recordTransactionProcessingTime(Duration duration) {
        transactionProcessingTimer.record(duration);
    }
    
    public void incrementFinancialCalculation() {
        financialCalculationCounter.increment();
    }
}
```

**Actuator Endpoint Monitoring**:
```bash
# Retrieve application metrics during testing
curl http://localhost:8080/actuator/metrics/http.server.requests \
  -H "Accept: application/json" | jq '.measurements[]'

# Monitor JVM memory metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used \
  -H "Accept: application/json" | jq '.measurements[]'

# Check database connection pool status
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active \
  -H "Accept: application/json" | jq '.measurements[]'

# Retrieve custom business metrics
curl http://localhost:8080/actuator/metrics/custom.transaction.processing.time \
  -H "Accept: application/json" | jq '.measurements[]'
```

### Post-Test Analysis Procedures

#### 1. Performance Data Analysis

**Automated Results Processing**:
```python
# scripts/analysis/analyze-results.py
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from datetime import datetime, timedelta

class PerformanceAnalyzer:
    def __init__(self, results_directory):
        self.results_dir = results_directory
        self.k6_results = self.load_k6_results()
        self.jmeter_results = self.load_jmeter_results()
        self.prometheus_metrics = self.load_prometheus_metrics()
    
    def analyze_response_times(self):
        """Analyze API response time distribution and SLA compliance"""
        response_times = self.k6_results['http_req_duration']
        
        analysis = {
            'mean': np.mean(response_times),
            'median': np.median(response_times),
            'p95': np.percentile(response_times, 95),
            'p99': np.percentile(response_times, 99),
            'sla_compliance': np.percentile(response_times, 95) < 200,  # 200ms SLA
            'outliers': self.identify_outliers(response_times)
        }
        
        return analysis
    
    def generate_performance_report(self, output_file):
        """Generate comprehensive performance analysis report"""
        response_analysis = self.analyze_response_times()
        throughput_analysis = self.analyze_throughput()
        error_analysis = self.analyze_error_rates()
        resource_analysis = self.analyze_resource_utilization()
        
        report = self.create_html_report({
            'response_times': response_analysis,
            'throughput': throughput_analysis,
            'errors': error_analysis,
            'resources': resource_analysis,
            'timestamp': datetime.now().isoformat()
        })
        
        with open(output_file, 'w') as f:
            f.write(report)
        
        return output_file
```

**Execution Example**:
```bash
# Analyze performance test results
python scripts/analysis/analyze-results.py \
  --results-dir=reports/generated/20240115-143022/ \
  --output=reports/generated/analysis-report.html \
  --format=html \
  --include-graphs=true

# Generate SLA compliance report
python scripts/analysis/generate-sla-report.py \
  --metrics-source=prometheus \
  --prometheus-url=http://localhost:9090 \
  --time-range=1h \
  --output=reports/generated/sla-compliance.json
```

#### 2. Trend Analysis and Historical Comparison

**Performance Trend Monitoring**:
```python
# scripts/analysis/trend-analysis.py
def analyze_performance_trends(historical_data_path, current_results):
    """
    Analyze performance trends over time to detect gradual degradation
    """
    historical_df = pd.read_csv(historical_data_path)
    current_df = pd.DataFrame(current_results)
    
    trends = {
        'response_time_trend': calculate_trend(historical_df['avg_response_time']),
        'throughput_trend': calculate_trend(historical_df['requests_per_second']),
        'error_rate_trend': calculate_trend(historical_df['error_percentage']),
        'memory_usage_trend': calculate_trend(historical_df['memory_utilization'])
    }
    
    return trends

def detect_performance_anomalies(metrics_data, threshold_config):
    """
    Detect performance anomalies using statistical analysis
    """
    anomalies = []
    
    for metric, values in metrics_data.items():
        z_scores = np.abs(stats.zscore(values))
        threshold = threshold_config.get(metric, 3.0)  # Default 3 sigma
        
        anomaly_indices = np.where(z_scores > threshold)[0]
        if len(anomaly_indices) > 0:
            anomalies.append({
                'metric': metric,
                'anomaly_count': len(anomaly_indices),
                'severity': classify_anomaly_severity(z_scores[anomaly_indices])
            })
    
    return anomalies
```

#### 3. Comparative Analysis with Baselines

**Baseline Comparison Execution**:
```bash
# Compare current performance with established baseline
python scripts/analysis/compare-baselines.py \
  --baseline=test-data/baseline/performance-baseline.json \
  --current=reports/generated/current-performance.json \
  --output=reports/generated/baseline-comparison.html \
  --threshold=0.05  # 5% acceptable variance

# Generate regression analysis
python scripts/analysis/regression-analysis.py \
  --historical-data=data/performance-history.csv \
  --current-results=reports/generated/current-performance.json \
  --output=reports/generated/regression-analysis.pdf
```

## Test Configurations

### Environment-Specific Configurations

#### 1. Development Environment Configuration

**Purpose**: Lightweight testing for development workflow validation and basic performance sanity checks.

**Configuration File**: `k6/configs/development.json`
```json
{
  "scenarios": {
    "api_load_test": {
      "executor": "ramping-vus",
      "stages": [
        {"duration": "2m", "target": 10},
        {"duration": "5m", "target": 50},
        {"duration": "2m", "target": 0}
      ]
    }
  },
  "thresholds": {
    "http_req_duration": ["p(95)<500"],
    "http_req_failed": ["rate<0.05"]
  },
  "options": {
    "hosts": {
      "api.carddemo.local": "127.0.0.1:8080"
    }
  }
}
```

**JMeter Development Configuration**: `jmeter/configs/development.properties`
```properties
# Development environment settings
test.duration=300
test.users=50
test.ramp_up=60
test.host=localhost
test.port=8080
test.protocol=http

# Database connection settings for development
db.host=localhost
db.port=5432
db.name=carddemo_dev
db.user=carddemo
db.password=password
```

#### 2. Staging Environment Configuration

**Purpose**: Production-like performance validation with realistic data volumes and load patterns.

**Configuration File**: `k6/configs/staging.json`
```json
{
  "scenarios": {
    "api_performance_test": {
      "executor": "ramping-vus",
      "stages": [
        {"duration": "5m", "target": 100},
        {"duration": "15m", "target": 500},
        {"duration": "10m", "target": 1000},
        {"duration": "5m", "target": 0}
      ]
    },
    "spike_test": {
      "executor": "ramping-vus",
      "startTime": "35m",
      "stages": [
        {"duration": "30s", "target": 2000},
        {"duration": "1m", "target": 2000},
        {"duration": "30s", "target": 100}
      ]
    }
  },
  "thresholds": {
    "http_req_duration": ["p(95)<200", "p(99)<500"],
    "http_req_failed": ["rate<0.01"],
    "checks": ["rate>0.99"]
  },
  "options": {
    "hosts": {
      "api.carddemo.staging": "carddemo-staging.internal"
    },
    "insecureSkipTLSVerify": false,
    "tlsAuth": [
      {
        "domains": ["*.carddemo.staging"],
        "cert": "../certs/staging-client.crt",
        "key": "../certs/staging-client.key"
      }
    ]
  }
}
```

**Gatling Staging Configuration**: `gatling/conf/staging.conf`
```hocon
gatling {
  core {
    simulationClass = "ApiLoadSimulation"
    outputDirectoryBaseName = "staging-performance"
  }
  http {
    baseUrl = "https://api.carddemo.staging"
    connectionTimeout = 10000
    requestTimeout = 60000
    pooledConnectionIdleTimeout = 60000
    maxConnectionsPerHost = 10
  }
  data {
    writers = [console, file, graphite]
    console {
      light = false
      writePeriod = 5
    }
    file {
      bufferSize = 8192
    }
    graphite {
      host = "graphite.monitoring.internal"
      port = 2003
      protocol = "tcp"
      rootPathPrefix = "carddemo.staging.performance"
      bufferSize = 8192
      writePeriod = 1
    }
  }
}
```

#### 3. Production-like Environment Configuration

**Purpose**: Final validation before production deployment with full-scale load testing and comprehensive monitoring.

**Configuration File**: `k6/configs/production-like.json`
```json
{
  "scenarios": {
    "sustained_load": {
      "executor": "constant-vus",
      "vus": 1000,
      "duration": "30m"
    },
    "peak_load": {
      "executor": "ramping-vus",
      "startTime": "30m",
      "stages": [
        {"duration": "10m", "target": 2000},
        {"duration": "20m", "target": 3000},
        {"duration": "10m", "target": 1000}
      ]
    },
    "stress_test": {
      "executor": "ramping-vus",
      "startTime": "70m",
      "stages": [
        {"duration": "5m", "target": 5000},
        {"duration": "10m", "target": 5000},
        {"duration": "5m", "target": 0}
      ]
    }
  },
  "thresholds": {
    "http_req_duration": [
      "p(95)<200",
      "p(99)<400",
      "p(99.9)<1000"
    ],
    "http_req_failed": ["rate<0.001"],
    "checks": ["rate>0.999"],
    "custom_transaction_processing_time": ["p(95)<100"]
  },
  "ext": {
    "loadimpact": {
      "distribution": {
        "us-east-1": {"loadZone": "amazon:us:ashburn", "percent": 50},
        "us-west-1": {"loadZone": "amazon:us:palo alto", "percent": 30},
        "eu-west-1": {"loadZone": "amazon:ie:dublin", "percent": 20}
      }
    }
  }
}
```

### Test Scenario Configurations

#### 1. API Load Testing Scenarios

**REST API Endpoint Testing**: `k6/scripts/api-load-test.js`
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics for detailed analysis
const errorRate = new Rate('errors');
const transactionTime = new Trend('transaction_processing_time');

export let options = {
  scenarios: {
    authentication_flow: {
      executor: 'constant-vus',
      vus: 100,
      duration: '10m',
      tags: { test_type: 'authentication' }
    },
    transaction_processing: {
      executor: 'ramping-vus',
      startTime: '10m',
      stages: [
        { duration: '5m', target: 200 },
        { duration: '15m', target: 500 },
        { duration: '5m', target: 0 }
      ],
      tags: { test_type: 'transactions' }
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<200'],
    errors: ['rate<0.01'],
    transaction_processing_time: ['p(95)<100']
  }
};

export default function() {
  // Authentication flow
  const authResponse = http.post('/api/auth/login', {
    username: 'testuser',
    password: 'testpass'
  });
  
  check(authResponse, {
    'authentication successful': (r) => r.status === 200,
    'auth response time OK': (r) => r.timings.duration < 150
  });
  
  if (authResponse.status === 200) {
    const token = authResponse.json('token');
    
    // Transaction processing
    const transactionStart = Date.now();
    const transactionResponse = http.post('/api/transactions', {
      accountId: '1000000001',
      amount: 125.50,
      type: 'PURCHASE'
    }, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    
    const transactionDuration = Date.now() - transactionStart;
    transactionTime.add(transactionDuration);
    
    check(transactionResponse, {
      'transaction processed': (r) => r.status === 200,
      'transaction response time OK': (r) => r.timings.duration < 100,
      'balance updated correctly': (r) => r.json('newBalance') !== undefined
    });
    
    errorRate.add(transactionResponse.status !== 200);
  }
  
  sleep(1); // Think time between requests
}

export function handleSummary(data) {
  return {
    'results/api-load-test-summary.json': JSON.stringify(data),
    'results/api-load-test-summary.html': htmlReport(data)
  };
}
```

#### 2. Batch Processing Simulation

**Spring Batch Performance Testing**: `k6/scripts/batch-simulation.js`
```javascript
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const batchProcessingTime = new Trend('batch_processing_duration');

export let options = {
  scenarios: {
    daily_batch_simulation: {
      executor: 'shared-iterations',
      iterations: 1,
      vus: 1,
      maxDuration: '4h'  // SLA: 4-hour processing window
    },
    concurrent_batch_jobs: {
      executor: 'constant-vus',
      vus: 4,
      duration: '2h',
      startTime: '10m'
    }
  },
  thresholds: {
    batch_processing_duration: ['p(95)<14400']  // 4 hours in seconds
  }
};

export default function() {
  // Trigger batch job via REST API
  const batchStart = Date.now();
  
  const jobLaunchResponse = http.post('/actuator/batch/jobs', {
    jobName: 'dailyProcessingJob',
    jobParameters: {
      processDate: new Date().toISOString().split('T')[0],
      batchSize: '10000'
    }
  });
  
  check(jobLaunchResponse, {
    'batch job launched successfully': (r) => r.status === 202,
    'job ID returned': (r) => r.json('jobExecutionId') !== undefined
  });
  
  if (jobLaunchResponse.status === 202) {
    const jobId = jobLaunchResponse.json('jobExecutionId');
    
    // Monitor job progress
    let jobCompleted = false;
    while (!jobCompleted) {
      sleep(30); // Check every 30 seconds
      
      const statusResponse = http.get(`/actuator/batch/jobs/executions/${jobId}`);
      const jobStatus = statusResponse.json('status');
      
      if (jobStatus === 'COMPLETED' || jobStatus === 'FAILED') {
        jobCompleted = true;
        const batchDuration = (Date.now() - batchStart) / 1000;
        batchProcessingTime.add(batchDuration);
        
        check(statusResponse, {
          'batch job completed successfully': (r) => r.json('status') === 'COMPLETED',
          'processing within SLA': () => batchDuration < 14400  // 4 hours
        });
      }
    }
  }
}
```

### Database Performance Configuration

#### PostgreSQL Performance Tuning for Testing

**Configuration File**: `environments/configs/postgresql-tuning.conf`
```conf
# PostgreSQL performance tuning for load testing

# Memory configuration
shared_buffers = 4GB                    # 25% of total RAM
effective_cache_size = 12GB             # 75% of total RAM
work_mem = 32MB                         # Per-operation memory
maintenance_work_mem = 512MB            # Maintenance operations

# Connection settings
max_connections = 200                   # Support concurrent load testing
superuser_reserved_connections = 3

# Write-ahead logging
wal_buffers = 64MB                      # WAL buffer size
checkpoint_completion_target = 0.9      # Checkpoint spreading
checkpoint_timeout = 15min              # Checkpoint frequency
max_wal_size = 8GB                      # Maximum WAL size

# Query planner
random_page_cost = 1.1                  # SSD storage optimization
effective_io_concurrency = 200          # Concurrent I/O operations
max_worker_processes = 8                # Parallel processing workers
max_parallel_workers_per_gather = 4     # Parallel query workers

# Monitoring and logging
log_min_duration_statement = 1000       # Log slow queries (1 second)
log_checkpoints = on                    # Log checkpoint activity
log_connections = on                    # Log connection activity
log_disconnections = on                 # Log disconnection activity
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '

# Performance monitoring
track_activities = on                   # Track query activity
track_counts = on                       # Track table statistics
track_io_timing = on                    # Track I/O timing
track_functions = all                   # Track function calls

# Auto-vacuum tuning
autovacuum_vacuum_scale_factor = 0.1    # Vacuum when 10% of table changes
autovacuum_analyze_scale_factor = 0.05  # Analyze when 5% of table changes
autovacuum_vacuum_cost_delay = 10ms     # Vacuum cost delay
autovacuum_vacuum_cost_limit = 1000     # Vacuum cost limit
```

#### Spring Boot Database Configuration

**Configuration File**: `environments/configs/application-perf.yml`
```yaml
spring:
  datasource:
    hikari:
      # Connection pool optimization for performance testing
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      
      # Connection validation
      connection-test-query: "SELECT 1"
      validation-timeout: 5000
      
      # Performance tuning
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
        elideSetAutoCommits: true
        maintainTimeStats: false
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        # Query optimization
        generate_statistics: true
        use_query_cache: true
        use_second_level_cache: true
        cache.use_structured_entries: true
        
        # Batch processing optimization
        jdbc.batch_size: 1000
        jdbc.fetch_size: 1000
        order_inserts: true
        order_updates: true
        
        # Performance monitoring
        show_sql: false
        format_sql: false
        use_sql_comments: false
        
        # Connection handling
        connection.provider_disables_autocommit: true
        connection.autocommit: false

# Actuator configuration for performance monitoring
management:
  endpoints:
    web:
      exposure:
        include: "health,metrics,prometheus,info,env,beans"
  metrics:
    export:
      prometheus:
        enabled: true
        descriptions: true
        step: 10s
    distribution:
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
        custom.transaction.processing.time: 0.5, 0.95, 0.99
      percentiles-histogram:
        http.server.requests: true
        custom.transaction.processing.time: true
    tags:
      application: carddemo-performance
      environment: ${ENVIRONMENT:local}

# Logging configuration for performance analysis
logging:
  level:
    com.carddemo: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
    org.hibernate.type.descriptor.sql.BasicBinder: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/carddemo-performance.log
    max-size: 100MB
    max-history: 10
```

## Adding New Test Data

### Test Data Generation Guidelines

#### 1. Synthetic Data Generation

**Purpose**: Create large-volume, realistic test datasets for performance validation without using production data.

**Data Generation Script**: `scripts/setup/generate-test-data.sh`
```bash
#!/bin/bash

# Test data generation script for performance testing
# Usage: ./generate-test-data.sh --size=<small|medium|large|xlarge> --output=<directory>

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SIZE="medium"
OUTPUT_DIR="test-data/synthetic"
ANONYMIZE="true"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --size=*)
      SIZE="${1#*=}"
      shift
      ;;
    --output=*)
      OUTPUT_DIR="${1#*=}"
      shift
      ;;
    --anonymize=*)
      ANONYMIZE="${1#*=}"
      shift
      ;;
    *)
      echo "Unknown option $1"
      exit 1
      ;;
  esac
done

# Define data volumes based on size parameter
case $SIZE in
  small)
    CUSTOMERS=1000
    ACCOUNTS=2000
    CARDS=3000
    TRANSACTIONS=10000
    ;;
  medium)
    CUSTOMERS=10000
    ACCOUNTS=20000
    CARDS=30000
    TRANSACTIONS=100000
    ;;
  large)
    CUSTOMERS=50000
    ACCOUNTS=100000
    CARDS=150000
    TRANSACTIONS=1000000
    ;;
  xlarge)
    CUSTOMERS=100000
    ACCOUNTS=250000
    CARDS=400000
    TRANSACTIONS=5000000
    ;;
  *)
    echo "Invalid size: $SIZE. Use small, medium, large, or xlarge."
    exit 1
    ;;
esac

echo "Generating $SIZE test dataset:"
echo "  Customers: $CUSTOMERS"
echo "  Accounts: $ACCOUNTS" 
echo "  Cards: $CARDS"
echo "  Transactions: $TRANSACTIONS"
echo "  Output directory: $OUTPUT_DIR"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Generate synthetic customer data
python3 "$SCRIPT_DIR/generators/generate-customers.py" \
  --count=$CUSTOMERS \
  --anonymize=$ANONYMIZE \
  --output="$OUTPUT_DIR/synthetic-customers.csv"

# Generate synthetic account data
python3 "$SCRIPT_DIR/generators/generate-accounts.py" \
  --count=$ACCOUNTS \
  --customers-file="$OUTPUT_DIR/synthetic-customers.csv" \
  --output="$OUTPUT_DIR/synthetic-accounts.csv"

# Generate synthetic card data
python3 "$SCRIPT_DIR/generators/generate-cards.py" \
  --count=$CARDS \
  --accounts-file="$OUTPUT_DIR/synthetic-accounts.csv" \
  --output="$OUTPUT_DIR/synthetic-cards.csv"

# Generate synthetic transaction data
python3 "$SCRIPT_DIR/generators/generate-transactions.py" \
  --count=$TRANSACTIONS \
  --accounts-file="$OUTPUT_DIR/synthetic-accounts.csv" \
  --cards-file="$OUTPUT_DIR/synthetic-cards.csv" \
  --output="$OUTPUT_DIR/synthetic-transactions.csv"

# Generate SQL loading scripts
python3 "$SCRIPT_DIR/generators/generate-sql-loader.py" \
  --data-directory="$OUTPUT_DIR" \
  --output="$OUTPUT_DIR/load-synthetic-data.sql"

echo "Synthetic test data generation completed successfully!"
echo "Files generated in: $OUTPUT_DIR"
echo "To load data: psql -U carddemo -d carddemo -f $OUTPUT_DIR/load-synthetic-data.sql"
```

**Customer Data Generator**: `scripts/setup/generators/generate-customers.py`
```python
#!/usr/bin/env python3

import csv
import random
import argparse
from faker import Faker
from datetime import datetime, timedelta

def generate_synthetic_customers(count, anonymize=True, output_file='customers.csv'):
    """
    Generate synthetic customer data for performance testing
    """
    fake = Faker()
    
    with open(output_file, 'w', newline='') as csvfile:
        fieldnames = [
            'customer_id', 'first_name', 'middle_name', 'last_name',
            'address_line_1', 'address_line_2', 'address_line_3',
            'state_code', 'country_code', 'zip_code',
            'phone_number_1', 'phone_number_2', 'ssn', 'government_id',
            'date_of_birth', 'eft_account_id', 'primary_card_holder', 'fico_score'
        ]
        
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        
        for i in range(1, count + 1):
            # Generate realistic but synthetic data
            birth_date = fake.date_of_birth(minimum_age=18, maximum_age=80)
            
            customer_data = {
                'customer_id': 1000000000 + i,
                'first_name': fake.first_name(),
                'middle_name': fake.first_name() if random.random() > 0.7 else '',
                'last_name': fake.last_name(),
                'address_line_1': fake.street_address(),
                'address_line_2': fake.secondary_address() if random.random() > 0.8 else '',
                'address_line_3': '',
                'state_code': fake.state_abbr(),
                'country_code': 'USA',
                'zip_code': fake.zipcode(),
                'phone_number_1': fake.phone_number(),
                'phone_number_2': fake.phone_number() if random.random() > 0.6 else '',
                'ssn': generate_synthetic_ssn() if anonymize else fake.ssn(),
                'government_id': f"ID{random.randint(100000000, 999999999)}",
                'date_of_birth': birth_date.strftime('%Y-%m-%d'),
                'eft_account_id': f"EFT{random.randint(1000000, 9999999)}" if random.random() > 0.3 else '',
                'primary_card_holder': 'Y',
                'fico_score': random.randint(300, 850)
            }
            
            writer.writerow(customer_data)
    
    print(f"Generated {count} synthetic customer records in {output_file}")

def generate_synthetic_ssn():
    """Generate synthetic SSN that follows format but is not real"""
    # Use 9XX pattern for clearly synthetic SSNs
    area = random.randint(900, 999)
    group = random.randint(10, 99)
    serial = random.randint(1000, 9999)
    return f"{area}-{group:02d}-{serial:04d}"

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Generate synthetic customer data')
    parser.add_argument('--count', type=int, default=1000, help='Number of customers to generate')
    parser.add_argument('--anonymize', type=bool, default=True, help='Use synthetic data patterns')
    parser.add_argument('--output', type=str, default='customers.csv', help='Output CSV file')
    
    args = parser.parse_args()
    generate_synthetic_customers(args.count, args.anonymize, args.output)
```

#### 2. Test Data Validation

**Data Quality Validation Script**: `scripts/analysis/validate-test-data.py`
```python
#!/usr/bin/env python3

import pandas as pd
import numpy as np
import argparse
import json
from datetime import datetime
import psycopg2
from psycopg2.extras import RealDictCursor

class TestDataValidator:
    def __init__(self, database_url):
        self.db_url = database_url
        self.validation_results = {}
    
    def validate_data_integrity(self):
        """Validate referential integrity and data consistency"""
        with psycopg2.connect(self.db_url) as conn:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                
                # Validate customer-account relationships
                cur.execute("""
                    SELECT 
                        COUNT(*) as total_accounts,
                        COUNT(DISTINCT customer_id) as unique_customers,
                        COUNT(*) - COUNT(customer_id) as orphaned_accounts
                    FROM account_data
                """)
                account_validation = cur.fetchone()
                
                # Validate card-account relationships
                cur.execute("""
                    SELECT 
                        COUNT(*) as total_cards,
                        COUNT(DISTINCT account_id) as unique_accounts,
                        COUNT(*) - COUNT(account_id) as orphaned_cards
                    FROM card_data
                """)
                card_validation = cur.fetchone()
                
                # Validate transaction data consistency
                cur.execute("""
                    SELECT 
                        COUNT(*) as total_transactions,
                        COUNT(DISTINCT account_id) as unique_accounts,
                        MIN(transaction_date) as earliest_transaction,
                        MAX(transaction_date) as latest_transaction,
                        AVG(amount) as average_amount
                    FROM transactions
                """)
                transaction_validation = cur.fetchone()
                
                self.validation_results = {
                    'accounts': dict(account_validation),
                    'cards': dict(card_validation),
                    'transactions': dict(transaction_validation),
                    'validation_timestamp': datetime.now().isoformat()
                }
    
    def validate_performance_characteristics(self):
        """Validate data characteristics for performance testing"""
        with psycopg2.connect(self.db_url) as conn:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                
                # Check data distribution for realistic load testing
                cur.execute("""
                    SELECT 
                        account_id,
                        COUNT(*) as transaction_count,
                        AVG(amount) as avg_transaction_amount,
                        MAX(amount) as max_transaction_amount
                    FROM transactions 
                    GROUP BY account_id 
                    ORDER BY transaction_count DESC 
                    LIMIT 10
                """)
                high_volume_accounts = cur.fetchall()
                
                # Validate date distribution for time-based testing
                cur.execute("""
                    SELECT 
                        DATE_TRUNC('month', transaction_date) as month,
                        COUNT(*) as transaction_count
                    FROM transactions 
                    GROUP BY DATE_TRUNC('month', transaction_date)
                    ORDER BY month
                """)
                monthly_distribution = cur.fetchall()
                
                self.validation_results['performance_characteristics'] = {
                    'high_volume_accounts': [dict(row) for row in high_volume_accounts],
                    'monthly_distribution': [dict(row) for row in monthly_distribution]
                }
    
    def generate_validation_report(self, output_file):
        """Generate comprehensive validation report"""
        with open(output_file, 'w') as f:
            json.dump(self.validation_results, f, indent=2, default=str)
        
        print(f"Validation report generated: {output_file}")
        return self.validation_results

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Validate test data quality')
    parser.add_argument('--database-url', required=True, help='PostgreSQL database URL')
    parser.add_argument('--output', default='validation-report.json', help='Output report file')
    
    args = parser.parse_args()
    
    validator = TestDataValidator(args.database_url)
    validator.validate_data_integrity()
    validator.validate_performance_characteristics()
    validator.generate_validation_report(args.output)
```

#### 3. Test Data Maintenance

**Data Refresh and Cleanup Procedures**:

**Automated Data Refresh**: `scripts/setup/refresh-test-data.sh`
```bash
#!/bin/bash

# Automated test data refresh for performance testing environments
# Maintains consistent test data across test executions

set -e

ENVIRONMENT="development"
BACKUP_EXISTING="true"
LOAD_BASELINE="true"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --environment=*)
      ENVIRONMENT="${1#*=}"
      shift
      ;;
    --backup-existing=*)
      BACKUP_EXISTING="${1#*=}"
      shift
      ;;
    --load-baseline=*)
      LOAD_BASELINE="${1#*=}"
      shift
      ;;
    *)
      echo "Unknown option $1"
      exit 1
      ;;
  esac
done

echo "Refreshing test data for environment: $ENVIRONMENT"

# Backup existing data if requested
if [[ "$BACKUP_EXISTING" == "true" ]]; then
  echo "Creating backup of existing test data..."
  timestamp=$(date +%Y%m%d_%H%M%S)
  pg_dump -U carddemo -h localhost carddemo > "backups/carddemo_backup_${timestamp}.sql"
  echo "Backup created: backups/carddemo_backup_${timestamp}.sql"
fi

# Clean existing test data
echo "Cleaning existing test data..."
psql -U carddemo -h localhost carddemo << EOF
TRUNCATE TABLE transactions CASCADE;
TRUNCATE TABLE card_data CASCADE;
TRUNCATE TABLE account_data CASCADE;
TRUNCATE TABLE customer_data CASCADE;
TRUNCATE TABLE user_security CASCADE;
RESET SEQUENCE customer_data_customer_id_seq RESTART WITH 1000000001;
RESET SEQUENCE account_data_account_id_seq RESTART WITH 1000000001;
RESET SEQUENCE transactions_transaction_id_seq RESTART WITH 1;
EOF

# Load baseline test data if requested
if [[ "$LOAD_BASELINE" == "true" ]]; then
  echo "Loading baseline test data..."
  
  case $ENVIRONMENT in
    development)
      ./generate-test-data.sh --size=small --output=test-data/development/
      psql -U carddemo -h localhost carddemo -f test-data/development/load-synthetic-data.sql
      ;;
    staging)
      ./generate-test-data.sh --size=large --output=test-data/staging/
      psql -U carddemo -h localhost carddemo -f test-data/staging/load-synthetic-data.sql
      ;;
    performance)
      ./generate-test-data.sh --size=xlarge --output=test-data/performance/
      psql -U carddemo -h localhost carddemo -f test-data/performance/load-synthetic-data.sql
      ;;
    *)
      echo "Unknown environment: $ENVIRONMENT"
      exit 1
      ;;
  esac
fi

# Update database statistics for optimal query performance
echo "Updating database statistics..."
psql -U carddemo -h localhost carddemo << EOF
ANALYZE customer_data;
ANALYZE account_data;
ANALYZE card_data;
ANALYZE transactions;
ANALYZE user_security;
EOF

# Validate data loading
echo "Validating test data..."
python3 scripts/analysis/validate-test-data.py \
  --database-url="postgresql://carddemo:password@localhost:5432/carddemo" \
  --output="reports/generated/test-data-validation-$(date +%Y%m%d_%H%M%S).json"

echo "Test data refresh completed successfully!"
```

### Custom Test Scenario Creation

#### Adding New Performance Test Scenarios

**Template for New K6 Test Script**: `k6/scripts/_template.js`
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Define custom metrics for your specific test scenario
const customErrorRate = new Rate('custom_errors');
const customResponseTime = new Trend('custom_response_time');
const customOperationCounter = new Counter('custom_operations_total');

// Test configuration - modify for your scenario
export let options = {
  scenarios: {
    your_test_scenario: {
      executor: 'ramping-vus',
      stages: [
        { duration: '2m', target: 10 },
        { duration: '5m', target: 50 },
        { duration: '2m', target: 0 }
      ],
      tags: { test_type: 'custom_scenario' }
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<200'],  // Adjust based on your SLA
    custom_errors: ['rate<0.01'],
    custom_response_time: ['p(95)<150']
  }
};

// Setup function - executed once per VU before the test starts
export function setup() {
  // Initialize test data, authenticate, etc.
  const authResponse = http.post('/api/auth/login', {
    username: 'test_user',
    password: 'test_password'
  });
  
  if (authResponse.status === 200) {
    return { token: authResponse.json('token') };
  }
  throw new Error('Setup failed: unable to authenticate');
}

// Main test function - executed repeatedly during the test
export default function(data) {
  // Your custom test logic here
  const startTime = Date.now();
  
  // Example API call - replace with your specific endpoint
  const response = http.get('/api/your-endpoint', {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json'
    }
  });
  
  const endTime = Date.now();
  const responseTime = endTime - startTime;
  
  // Record custom metrics
  customResponseTime.add(responseTime);
  customOperationCounter.add(1);
  
  // Validate response
  const isSuccess = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time OK': (r) => r.timings.duration < 200,
    'response has expected data': (r) => r.json('data') !== undefined
  });
  
  customErrorRate.add(!isSuccess);
  
  // Think time between requests
  sleep(Math.random() * 2 + 1); // 1-3 seconds
}

// Teardown function - executed once after the test completes
export function teardown(data) {
  // Cleanup operations if needed
  if (data.token) {
    http.post('/api/auth/logout', {}, {
      headers: { 'Authorization': `Bearer ${data.token}` }
    });
  }
}

// Custom summary handler for test results
export function handleSummary(data) {
  return {
    'results/custom-test-summary.json': JSON.stringify(data, null, 2),
    'results/custom-test-summary.html': generateHtmlReport(data)
  };
}

function generateHtmlReport(data) {
  // Generate custom HTML report based on your requirements
  return `
    <!DOCTYPE html>
    <html>
    <head>
        <title>Custom Performance Test Report</title>
        <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            .metric { margin: 10px 0; padding: 10px; background: #f5f5f5; }
            .pass { color: green; }
            .fail { color: red; }
        </style>
    </head>
    <body>
        <h1>Custom Performance Test Results</h1>
        <div class="metric">
            <h3>Response Time (95th percentile)</h3>
            <p>${data.metrics.http_req_duration.values.p95.toFixed(2)} ms</p>
        </div>
        <div class="metric">
            <h3>Error Rate</h3>
            <p>${(data.metrics.custom_errors.values.rate * 100).toFixed(2)}%</p>
        </div>
        <div class="metric">
            <h3>Total Operations</h3>
            <p>${data.metrics.custom_operations_total.values.count}</p>
        </div>
    </body>
    </html>
  `;
}
```

## Troubleshooting Guide

### Common Performance Testing Issues

#### 1. Test Environment Issues

**Problem**: Performance tests fail to start due to environment connectivity issues.

**Symptoms**:
```
Connection refused to localhost:8080
Failed to connect to PostgreSQL database
Redis connection timeout
```

**Troubleshooting Steps**:
```bash
# Check application health
curl -f http://localhost:8080/actuator/health || echo "Application not responding"

# Verify database connectivity
docker exec carddemo-postgres pg_isready -U carddemo || echo "PostgreSQL not ready"

# Test Redis connectivity
docker exec carddemo-redis redis-cli ping || echo "Redis not available"

# Check Docker container status
docker-compose -f environments/docker-compose/performance-test.yml ps

# Review container logs for errors
docker-compose -f environments/docker-compose/performance-test.yml logs carddemo-app
docker-compose -f environments/docker-compose/performance-test.yml logs carddemo-postgres
docker-compose -f environments/docker-compose/performance-test.yml logs carddemo-redis
```

**Resolution**:
```bash
# Restart the entire stack
docker-compose -f environments/docker-compose/performance-test.yml down
docker-compose -f environments/docker-compose/performance-test.yml up -d

# Wait for services to become healthy
scripts/setup/wait-for-services.sh --timeout=300

# Verify service readiness
scripts/setup/verify-environment.sh --environment=local
```

#### 2. Load Testing Tool Issues

**Problem**: K6, JMeter, or Gatling tests fail during execution.

**K6 Common Issues**:
```bash
# Issue: K6 script syntax errors
k6 run --verbose k6/scripts/api-load-test.js

# Issue: Threshold failures
# Check thresholds in test configuration
grep -n "thresholds" k6/configs/staging.json

# Issue: Memory exhaustion during high load
# Reduce VU count or add --max-vus parameter
k6 run --max-vus 1000 k6/scripts/api-load-test.js
```

**JMeter Common Issues**:
```bash
# Issue: OutOfMemoryError during test execution
# Increase JMeter heap size
export JVM_ARGS="-Xms1g -Xmx4g"
jmeter -n -t jmeter/test-plans/api-performance.jmx

# Issue: Connection timeouts
# Check JMeter HTTP Request Defaults
grep -A 5 "HTTPSampler.connect_timeout" jmeter/test-plans/api-performance.jmx

# Issue: Result file corruption
# Verify disk space and permissions
df -h .
ls -la results/
```

**Gatling Common Issues**:
```bash
# Issue: Simulation compilation errors
# Check Scala syntax in simulation files
gatling.sh -sf gatling/simulations --run-mode=test-compile

# Issue: High GC pressure during simulation
# Increase Gatling JVM memory
export JAVA_OPTS="-Xms2g -Xmx8g -XX:+UseG1GC"
gatling.sh -sf gatling/simulations -s ApiLoadSimulation

# Issue: Report generation failures
# Check results directory permissions and disk space
ls -la gatling/results/
df -h gatling/results/
```

#### 3. Database Performance Issues

**Problem**: Database becomes bottleneck during load testing.

**Symptoms**:
```
High database connection wait times
Query timeouts during load testing
PostgreSQL connection pool exhaustion
Slow batch processing performance
```

**Diagnostic Queries**:
```sql
-- Check active connections and queries
SELECT 
    pid, 
    usename, 
    application_name, 
    client_addr, 
    state, 
    query_start, 
    query 
FROM pg_stat_activity 
WHERE state = 'active' 
ORDER BY query_start;

-- Identify slow queries during testing
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    stddev_time,
    rows
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 20;

-- Check connection pool status
SELECT 
    pool_name,
    pool_size,
    pool_available,
    pool_used
FROM pg_pool_status;

-- Monitor table and index usage
SELECT 
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch
FROM pg_stat_user_tables 
ORDER BY seq_scan DESC;
```

**Resolution Steps**:
```bash
# Optimize PostgreSQL configuration
cp environments/configs/postgresql-tuning.conf /var/lib/postgresql/data/postgresql.conf
docker restart carddemo-postgres

# Increase connection pool size in application
# Edit application-perf.yml
sed -i 's/maximum-pool-size: 20/maximum-pool-size: 50/g' \
  environments/configs/application-perf.yml

# Restart application with optimized configuration
docker-compose -f environments/docker-compose/performance-test.yml restart carddemo-app

# Monitor connection pool metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

#### 4. Memory and Resource Issues

**Problem**: Application runs out of memory or CPU during load testing.

**Symptoms**:
```
OutOfMemoryError in application logs
High GC pressure and long GC pauses
CPU utilization consistently above 90%
Kubernetes pod restarts due to resource limits
```

**Monitoring and Diagnosis**:
```bash
# Monitor JVM memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq '.'

# Check GC metrics
curl http://localhost:8080/actuator/metrics/jvm.gc.pause | jq '.'

# Monitor container resource usage
docker stats carddemo-app

# Kubernetes resource monitoring
kubectl top pods -n carddemo-performance
kubectl describe pod carddemo-app-xxx -n carddemo-performance
```

**Resolution Strategies**:
```bash
# Increase JVM heap size
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Update Docker Compose memory limits
# Edit docker-compose.yml
cat >> environments/docker-compose/performance-test.yml << EOF
    deploy:
      resources:
        limits:
          memory: 6G
          cpus: '4'
        reservations:
          memory: 4G
          cpus: '2'
EOF

# For Kubernetes deployment, update resource requests/limits
kubectl patch deployment carddemo-app -n carddemo-performance -p '
{
  "spec": {
    "template": {
      "spec": {
        "containers": [
          {
            "name": "carddemo-app",
            "resources": {
              "requests": {
                "memory": "4Gi",
                "cpu": "2"
              },
              "limits": {
                "memory": "6Gi", 
                "cpu": "4"
              }
            }
          }
        ]
      }
    }
  }
}'
```

#### 5. Network and Connectivity Issues

**Problem**: Network latency or connectivity issues affect test results.

**Network Diagnostic Commands**:
```bash
# Test network latency to application
ping -c 10 localhost

# Check port connectivity
telnet localhost 8080

# Monitor network connections during testing
netstat -an | grep :8080

# Check for network packet loss
ping -c 100 localhost | grep "packet loss"

# Monitor network interface statistics
cat /proc/net/dev

# Test DNS resolution (if using hostnames)
nslookup carddemo-app.local
```

**Network Optimization**:
```bash
# Increase OS network buffers
sudo sysctl -w net.core.rmem_max=16777216
sudo sysctl -w net.core.wmem_max=16777216
sudo sysctl -w net.ipv4.tcp_rmem="4096 65536 16777216"
sudo sysctl -w net.ipv4.tcp_wmem="4096 65536 16777216"

# Increase file descriptor limits
ulimit -n 65536

# Configure load balancer connection settings (if applicable)
# Edit nginx.conf or load balancer configuration
cat >> /etc/nginx/nginx.conf << EOF
upstream carddemo_backend {
    server carddemo-app:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

server {
    location / {
        proxy_pass http://carddemo_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
}
EOF
```

### Performance Analysis Troubleshooting

#### 1. Unexpected Response Time Variations

**Problem**: Inconsistent response times or performance spikes during testing.

**Analysis Steps**:
```bash
# Check for periodic batch jobs or background processes
ps aux | grep java
crontab -l

# Monitor system resource usage over time
sar -u 1 60  # CPU utilization
sar -r 1 60  # Memory usage
sar -d 1 60  # Disk I/O

# Analyze application logs for performance patterns
grep -E "(slow|timeout|delay)" logs/carddemo-performance.log

# Check for database lock contention
SELECT * FROM pg_locks WHERE NOT granted;
```

**Solutions**:
```bash
# Schedule background maintenance during low-traffic periods
# Disable unnecessary cron jobs during testing
sudo systemctl stop cron

# Implement connection pooling optimization
# Update application configuration
kubectl create configmap carddemo-perf-config \
  --from-file=environments/configs/application-perf.yml \
  -n carddemo-performance

# Monitor and tune JVM GC settings
export JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
```

#### 2. Test Results Interpretation Issues

**Problem**: Difficulty interpreting performance test results or comparing baselines.

**Analysis Automation**:
```python
# scripts/analysis/interpret-results.py
def interpret_performance_results(results_file):
    """
    Automated performance results interpretation with recommendations
    """
    with open(results_file, 'r') as f:
        results = json.load(f)
    
    interpretation = {
        'sla_compliance': analyze_sla_compliance(results),
        'performance_trends': identify_performance_trends(results),
        'bottleneck_analysis': identify_bottlenecks(results),
        'recommendations': generate_recommendations(results)
    }
    
    return interpretation

def generate_recommendations(results):
    """Generate actionable recommendations based on results"""
    recommendations = []
    
    # Response time analysis
    p95_response_time = results.get('http_req_duration', {}).get('p95', 0)
    if p95_response_time > 200:
        recommendations.append({
            'category': 'Response Time',
            'severity': 'High',
            'issue': f'95th percentile response time ({p95_response_time}ms) exceeds 200ms SLA',
            'recommendations': [
                'Investigate database query performance',
                'Check application memory usage and GC patterns',
                'Verify network latency between components',
                'Consider implementing caching for frequently accessed data'
            ]
        })
    
    # Error rate analysis
    error_rate = results.get('http_req_failed', {}).get('rate', 0)
    if error_rate > 0.01:
        recommendations.append({
            'category': 'Error Rate',
            'severity': 'Critical',
            'issue': f'Error rate ({error_rate*100:.2f}%) exceeds 1% threshold',
            'recommendations': [
                'Review application logs for error patterns',
                'Check database connection pool configuration',
                'Verify resource limits and scaling policies',
                'Implement circuit breaker patterns for external dependencies'
            ]
        })
    
    return recommendations
```

### Monitoring and Alerting Troubleshooting

#### 1. Prometheus Metrics Collection Issues

**Problem**: Missing or incomplete metrics during performance testing.

**Diagnostic Steps**:
```bash
# Check Prometheus configuration
curl http://localhost:9090/api/v1/config

# Verify target discovery
curl http://localhost:9090/api/v1/targets

# Check metric availability
curl http://localhost:9090/api/v1/label/__name__/values | jq '.data[]' | grep carddemo

# Test metrics endpoint directly
curl http://localhost:8080/actuator/prometheus | grep http_server_requests
```

**Resolution**:
```bash
# Restart Prometheus with updated configuration
docker restart prometheus

# Verify Spring Boot Actuator configuration
curl http://localhost:8080/actuator/info
curl http://localhost:8080/actuator/health

# Check network connectivity between Prometheus and application
docker network ls
docker network inspect performance-test_default
```

#### 2. Grafana Dashboard Issues

**Problem**: Grafana dashboards not displaying performance data correctly.

**Troubleshooting**:
```bash
# Check Grafana datasource configuration
curl -u admin:admin http://localhost:3000/api/datasources

# Test datasource connectivity
curl -u admin:admin http://localhost:3000/api/datasources/1/health

# Verify dashboard configuration
curl -u admin:admin http://localhost:3000/api/dashboards/uid/performance-overview

# Check Grafana logs for errors
docker logs grafana
```

**Resolution**:
```bash
# Update datasource configuration
curl -X PUT -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{"name":"Prometheus","type":"prometheus","url":"http://prometheus:9090","access":"proxy"}' \
  http://localhost:3000/api/datasources/1

# Import updated dashboard
curl -X POST -u admin:admin \
  -H "Content-Type: application/json" \
  -d @monitoring/grafana/dashboards/performance-overview.json \
  http://localhost:3000/api/dashboards/db
```

## Continuous Integration Integration

### CI/CD Pipeline Integration

#### 1. GitHub Actions Performance Testing

**Performance Testing Workflow**: `.github/workflows/performance-testing.yml`
```yaml
name: Performance Testing

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  performance-test:
    runs-on: ubuntu-latest
    timeout-minutes: 120
    
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: carddemo
          POSTGRES_USER: carddemo
          POSTGRES_PASSWORD: password
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      
      - name: Build application
        run: |
          # Build backend
          cd backend
          ./mvnw clean package -DskipTests
          
          # Build frontend
          cd ../frontend
          npm ci
          npm run build
      
      - name: Start application
        run: |
          cd backend
          java -jar target/carddemo-*.jar \
            --spring.profiles.active=test \
            --spring.datasource.url=jdbc:postgresql://localhost:5432/carddemo \
            --spring.redis.host=localhost &
          
          # Wait for application to start
          timeout 60 bash -c 'until curl -f http://localhost:8080/actuator/health; do sleep 2; done'
      
      - name: Setup test data
        run: |
          cd backend/src/test/resources/performance
          chmod +x scripts/setup/prepare-environment.sh
          ./scripts/setup/prepare-environment.sh --environment=ci
          
          # Load test data
          ./scripts/setup/load-test-data.sh --size=medium
      
      - name: Install K6
        run: |
          curl https://github.com/grafana/k6/releases/download/v0.47.0/k6-v0.47.0-linux-amd64.tar.gz -L | tar xvz --strip-components 1
          sudo mv k6 /usr/local/bin/
      
      - name: Run K6 performance tests
        run: |
          cd backend/src/test/resources/performance
          k6 run --out json=results/k6-results.json \
            --config k6/configs/ci.json \
            k6/scripts/api-load-test.js
      
      - name: Install JMeter
        run: |
          wget -q https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.2.tgz
          tar -xzf apache-jmeter-5.6.2.tgz
          sudo mv apache-jmeter-5.6.2 /opt/jmeter
          echo "PATH=$PATH:/opt/jmeter/bin" >> $GITHUB_ENV
      
      - name: Run JMeter performance tests
        run: |
          cd backend/src/test/resources/performance
          jmeter -n -t jmeter/test-plans/api-performance.jmx \
            -l results/jmeter-results.jtl \
            -e -o results/jmeter-report/
      
      - name: Analyze performance results
        run: |
          cd backend/src/test/resources/performance
          python3 scripts/analysis/analyze-results.py \
            --k6-results=results/k6-results.json \
            --jmeter-results=results/jmeter-results.jtl \
            --output=results/performance-analysis.json
      
      - name: Performance gate validation
        run: |
          cd backend/src/test/resources/performance
          python3 scripts/analysis/performance-gate.py \
            --results=results/performance-analysis.json \
            --sla-config=configs/sla-thresholds.yaml \
            --fail-on-sla-violation=true
      
      - name: Upload performance results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: performance-test-results
          path: |
            backend/src/test/resources/performance/results/
            backend/src/test/resources/performance/reports/generated/
          retention-days: 30
      
      - name: Comment PR with results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const analysisFile = 'backend/src/test/resources/performance/results/performance-analysis.json';
            
            if (fs.existsSync(analysisFile)) {
              const analysis = JSON.parse(fs.readFileSync(analysisFile, 'utf8'));
              
              const comment = `
              ## Performance Test Results
              
              | Metric | Value | SLA | Status |
              |--------|-------|-----|---------|
              | 95th Percentile Response Time | ${analysis.response_time.p95}ms | <200ms | ${analysis.response_time.p95 < 200 ? '✅' : '❌'} |
              | Error Rate | ${(analysis.error_rate * 100).toFixed(2)}% | <1% | ${analysis.error_rate < 0.01 ? '✅' : '❌'} |
              | Throughput | ${analysis.throughput.rps} RPS | >1000 RPS | ${analysis.throughput.rps > 1000 ? '✅' : '❌'} |
              
              [View detailed report](https://github.com/${{ github.repository }}/actions/runs/${{ github.run_id }})
              `;
              
              github.rest.issues.createComment({
                issue_number: context.issue.number,
                owner: context.repo.owner,
                repo: context.repo.repo,
                body: comment
              });
            }
```

#### 2. Performance Regression Detection

**Automated Baseline Comparison in CI**: `scripts/analysis/ci-performance-gate.py`
```python
#!/usr/bin/env python3

import json
import argparse
import sys
from datetime import datetime, timedelta

class PerformanceGate:
    def __init__(self, sla_config_file):
        with open(sla_config_file, 'r') as f:
            self.sla_config = json.load(f)
        self.violations = []
    
    def validate_performance_results(self, results_file):
        """Validate performance results against SLA thresholds"""
        with open(results_file, 'r') as f:
            results = json.load(f)
        
        # Response time validation
        self._validate_response_time(results)
        
        # Error rate validation
        self._validate_error_rate(results)
        
        # Throughput validation
        self._validate_throughput(results)
        
        # Resource utilization validation
        self._validate_resource_usage(results)
        
        return len(self.violations) == 0
    
    def _validate_response_time(self, results):
        """Validate API response time SLAs"""
        response_time_config = self.sla_config.get('response_time', {})
        
        if 'http_req_duration' in results:
            p95_time = results['http_req_duration'].get('p95', 0)
            threshold = response_time_config.get('p95_threshold_ms', 200)
            
            if p95_time > threshold:
                self.violations.append({
                    'type': 'response_time',
                    'metric': '95th percentile response time',
                    'value': f"{p95_time}ms",
                    'threshold': f"{threshold}ms",
                    'severity': 'high'
                })
    
    def _validate_error_rate(self, results):
        """Validate error rate SLAs"""
        error_config = self.sla_config.get('error_rate', {})
        
        if 'http_req_failed' in results:
            error_rate = results['http_req_failed'].get('rate', 0)
            threshold = error_config.get('max_error_rate', 0.01)
            
            if error_rate > threshold:
                self.violations.append({
                    'type': 'error_rate',
                    'metric': 'HTTP request error rate',
                    'value': f"{error_rate * 100:.2f}%",
                    'threshold': f"{threshold * 100:.2f}%",
                    'severity': 'critical'
                })
    
    def _validate_throughput(self, results):
        """Validate throughput SLAs"""
        throughput_config = self.sla_config.get('throughput', {})
        
        if 'http_reqs' in results:
            rps = results['http_reqs'].get('rate', 0)
            min_threshold = throughput_config.get('min_rps', 1000)
            
            if rps < min_threshold:
                self.violations.append({
                    'type': 'throughput',
                    'metric': 'Requests per second',
                    'value': f"{rps:.2f} RPS",
                    'threshold': f"{min_threshold} RPS minimum",
                    'severity': 'medium'
                })
    
    def generate_gate_report(self, output_file):
        """Generate performance gate decision report"""
        gate_passed = len(self.violations) == 0
        
        report = {
            'timestamp': datetime.now().isoformat(),
            'gate_decision': 'PASS' if gate_passed else 'FAIL',
            'violations': self.violations,
            'total_violations': len(self.violations),
            'summary': self._generate_summary()
        }
        
        with open(output_file, 'w') as f:
            json.dump(report, f, indent=2)
        
        return gate_passed
    
    def _generate_summary(self):
        """Generate executive summary of gate results"""
        if not self.violations:
            return "All performance SLAs met. Pipeline can proceed to deployment."
        
        critical_count = len([v for v in self.violations if v['severity'] == 'critical'])
        high_count = len([v for v in self.violations if v['severity'] == 'high'])
        
        summary = f"Performance gate failed with {len(self.violations)} violations. "
        summary += f"Critical: {critical_count}, High: {high_count}. "
        summary += "Review performance optimizations before deployment."
        
        return summary

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Performance gate validation')
    parser.add_argument('--results', required=True, help='Performance results file')
    parser.add_argument('--sla-config', required=True, help='SLA configuration file')
    parser.add_argument('--output', default='gate-decision.json', help='Output report file')
    parser.add_argument('--fail-on-sla-violation', action='store_true', help='Exit with error on SLA violations')
    
    args = parser.parse_args()
    
    gate = PerformanceGate(args.sla_config)
    gate_passed = gate.validate_performance_results(args.results)
    gate.generate_gate_report(args.output)
    
    print(f"Performance gate: {'PASSED' if gate_passed else 'FAILED'}")
    
    if not gate_passed:
        print("\nSLA Violations:")
        for violation in gate.violations:
            print(f"  - {violation['metric']}: {violation['value']} (threshold: {violation['threshold']})")
    
    if args.fail_on_sla_violation and not gate_passed:
        sys.exit(1)
```

#### 3. Performance Trending and Historical Analysis

**Historical Performance Tracking**: `scripts/analysis/track-performance-trends.py`
```python
#!/usr/bin/env python3

import json
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime, timedelta
import boto3  # For S3 storage of historical data

class PerformanceTrendTracker:
    def __init__(self, s3_bucket=None):
        self.s3_bucket = s3_bucket
        self.s3_client = boto3.client('s3') if s3_bucket else None
    
    def store_performance_results(self, results_file, build_info):
        """Store performance results for historical tracking"""
        with open(results_file, 'r') as f:
            results = json.load(f)
        
        # Enhance results with build metadata
        enhanced_results = {
            'timestamp': datetime.now().isoformat(),
            'build_number': build_info.get('build_number'),
            'commit_sha': build_info.get('commit_sha'),
            'branch': build_info.get('branch'),
            'performance_metrics': results
        }
        
        # Store locally
        local_file = f"historical-data/performance-{datetime.now().strftime('%Y%m%d-%H%M%S')}.json"
        with open(local_file, 'w') as f:
            json.dump(enhanced_results, f, indent=2)
        
        # Store in S3 if configured
        if self.s3_client and self.s3_bucket:
            s3_key = f"performance-data/{datetime.now().strftime('%Y/%m/%d')}/performance-{build_info.get('build_number')}.json"
            self.s3_client.put_object(
                Bucket=self.s3_bucket,
                Key=s3_key,
                Body=json.dumps(enhanced_results, indent=2)
            )
        
        return local_file
    
    def analyze_performance_trends(self, days_back=30):
        """Analyze performance trends over specified period"""
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days_back)
        
        historical_data = self._load_historical_data(start_date, end_date)
        
        if not historical_data:
            print("No historical data available for trend analysis")
            return None
        
        df = pd.DataFrame(historical_data)
        trends = {
            'response_time_trend': self._calculate_trend(df, 'response_time_p95'),
            'error_rate_trend': self._calculate_trend(df, 'error_rate'),
            'throughput_trend': self._calculate_trend(df, 'throughput_rps')
        }
        
        return trends
    
    def detect_performance_regressions(self, current_results, historical_baseline):
        """Detect performance regressions compared to baseline"""
        regressions = []
        
        # Response time regression
        current_p95 = current_results.get('http_req_duration', {}).get('p95', 0)
        baseline_p95 = historical_baseline.get('response_time_p95_avg', 0)
        
        if baseline_p95 > 0 and current_p95 > baseline_p95 * 1.2:  # 20% regression threshold
            regressions.append({
                'metric': 'Response Time P95',
                'current': f"{current_p95}ms",
                'baseline': f"{baseline_p95}ms",
                'regression_percent': ((current_p95 - baseline_p95) / baseline_p95) * 100
            })
        
        # Error rate regression
        current_error_rate = current_results.get('http_req_failed', {}).get('rate', 0)
        baseline_error_rate = historical_baseline.get('error_rate_avg', 0)
        
        if current_error_rate > baseline_error_rate * 2:  # 100% increase threshold
            regressions.append({
                'metric': 'Error Rate',
                'current': f"{current_error_rate * 100:.2f}%",
                'baseline': f"{baseline_error_rate * 100:.2f}%",
                'regression_percent': ((current_error_rate - baseline_error_rate) / baseline_error_rate) * 100
            })
        
        return regressions
    
    def _load_historical_data(self, start_date, end_date):
        """Load historical performance data from storage"""
        # Implementation depends on storage backend (S3, database, etc.)
        # This is a simplified version
        historical_files = []
        
        if self.s3_client and self.s3_bucket:
            # Load from S3
            response = self.s3_client.list_objects_v2(
                Bucket=self.s3_bucket,
                Prefix="performance-data/"
            )
            
            for obj in response.get('Contents', []):
                # Filter by date range and load data
                # Implementation details...
                pass
        
        return historical_files
    
    def generate_trend_report(self, output_file):
        """Generate comprehensive performance trend report"""
        trends = self.analyze_performance_trends()
        
        if not trends:
            return None
        
        report = {
            'generated_at': datetime.now().isoformat(),
            'analysis_period': '30 days',
            'trends': trends,
            'recommendations': self._generate_trend_recommendations(trends)
        }
        
        with open(output_file, 'w') as f:
            json.dump(report, f, indent=2)
        
        return output_file
```

## References

### Documentation and Standards

- **COBOL to Java Migration Guide**: Technical specification sections 0.1-0.6 covering migration strategy and validation requirements
- **Spring Boot Performance Tuning**: Official Spring Boot documentation for production-ready applications
- **PostgreSQL Performance Optimization**: PostgreSQL official documentation for enterprise database tuning
- **K6 Load Testing Guide**: Official K6 documentation for JavaScript-based load testing
- **Apache JMeter User Manual**: Comprehensive guide for GUI and command-line performance testing
- **Gatling Documentation**: High-performance load testing framework documentation
- **Prometheus Monitoring Guide**: Official Prometheus documentation for metrics collection and alerting
- **Grafana Visualization Guide**: Dashboard creation and data visualization best practices

### Technical Specification References

- **Section 0.5 VALIDATION CHECKLIST**: Implementation verification points and performance benchmarks
- **Section 6.6 TESTING STRATEGY**: Comprehensive testing approach including performance testing framework
- **Section 6.2 DATABASE DESIGN**: PostgreSQL schema design and optimization strategies
- **Section 3.2 FRAMEWORKS & LIBRARIES**: Spring Boot and monitoring technology stack details
- **Section 8.3 CONTAINERIZATION**: Docker and Kubernetes deployment configurations for testing

### Performance Testing Best Practices

- **Financial Services Performance Testing**: Industry-specific guidelines for banking application testing
- **Microservices Performance Testing**: Best practices for distributed system performance validation
- **Cloud-Native Application Testing**: Guidelines for containerized application performance testing
- **Continuous Performance Testing**: Integration of performance testing in CI/CD pipelines
- **Database Performance Testing**: Specialized testing for high-transaction database systems

### Tool-Specific Resources

- **K6 Examples Repository**: Community-contributed examples and best practices
- **JMeter Performance Testing Patterns**: Common test plan patterns and configurations
- **Gatling Advanced Scenarios**: Complex simulation examples and optimization techniques
- **Spring Boot Actuator Metrics**: Custom metrics implementation and monitoring integration
- **Prometheus Query Examples**: PromQL queries for performance monitoring and alerting

### Sample Implementations

- **CardDemo Spring Boot Repository**: Reference implementation demonstrating performance testing integration
- **Performance Testing Framework**: Template repository for enterprise performance testing setup
- **Monitoring Stack Configuration**: Complete monitoring setup with Prometheus, Grafana, and alerting
- **CI/CD Pipeline Examples**: GitHub Actions and GitLab CI configurations for automated performance testing

---

This comprehensive performance testing guide provides the foundation for validating the COBOL-to-Java migration project's performance characteristics. The documented procedures, tools, and configurations ensure that the modernized Spring Boot application meets or exceeds the original mainframe system's performance while maintaining 100% functional parity.

For questions or support with performance testing implementation, consult the technical specification sections referenced above or engage with the development team through established communication channels.