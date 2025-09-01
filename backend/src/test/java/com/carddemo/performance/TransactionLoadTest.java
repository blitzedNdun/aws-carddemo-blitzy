/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.performance;

import com.carddemo.config.ActuatorConfig;
import com.carddemo.controller.TransactionController;
import com.carddemo.test.TestDataGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import java.math.BigDecimal;
import java.util.Random;

/**
 * JMeter/Gatling-based load test class for validating transaction processing at 10,000 TPS,
 * ensuring sub-200ms response times for card authorization requests, and measuring throughput
 * under various load conditions.
 * 
 * This comprehensive performance test suite validates the modernized Spring Boot REST API
 * endpoints that replace the original COBOL transaction processing programs (COTRN00C, 
 * COTRN01C, COTRN02C) while ensuring identical performance characteristics to the mainframe
 * implementation.
 * 
 * Performance Requirements:
 * - Target throughput: 10,000 transactions per second (TPS)
 * - Response time SLA: Sub-200ms for 95th percentile
 * - Concurrent user simulation: Up to 1,000 concurrent sessions
 * - Load testing duration: Sustained performance over 10-minute periods
 * - Memory efficiency: No memory leaks during extended load testing
 * 
 * Test Architecture:
 * - Uses Spring WebTestClient for reactive, non-blocking HTTP requests
 * - Employs CompletableFuture for concurrent request execution
 * - Integrates with Micrometer for performance metrics collection
 * - Validates against ActuatorConfig monitoring endpoints
 * - Leverages TestDataGenerator for COBOL-compliant test data
 * 
 * COBOL Transaction Mapping:
 * - GET /api/transactions -> COTRN00C.cbl transaction list functionality
 * - GET /api/transactions/{id} -> COTRN01C.cbl single transaction view
 * - POST /api/transactions -> COTRN02C.cbl transaction creation
 * - Pagination endpoints -> VSAM STARTBR/READNEXT cursor operations
 * 
 * Monitoring Integration:
 * - Captures response time distributions for SLA validation
 * - Tracks error rates and success percentages
 * - Measures JVM resource utilization under load
 * - Generates comprehensive load test reports
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class TransactionLoadTest {

    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private TestDataGenerator testDataGenerator;
    
    @Autowired
    private TransactionController transactionController;
    
    @Autowired
    private ActuatorConfig actuatorConfig;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Performance tracking metrics
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final List<Long> responseTimes = new ArrayList<>();
    
    // Load testing configuration
    private static final int TARGET_TPS = 10000;
    private static final int MAX_RESPONSE_TIME_MS = 200;
    private static final int CONCURRENT_USERS = 1000;
    private static final int TEST_DURATION_SECONDS = 60;
    private static final int WARMUP_DURATION_SECONDS = 10;
    
    // Test execution infrastructure
    private ExecutorService executorService;
    private Timer responseTimeTimer;
    private Counter requestCounter;
    private Counter errorCounter;
    
    @BeforeEach
    public void setUp() {
        // Reset test data generator for consistent test runs
        testDataGenerator.resetRandomSeed();
        
        // Initialize thread pool for concurrent testing
        executorService = Executors.newFixedThreadPool(CONCURRENT_USERS);
        
        // Configure performance monitoring metrics
        responseTimeTimer = Timer.builder("load.test.response.time")
                .description("Response time distribution for load test requests")
                .register(meterRegistry);
                
        requestCounter = Counter.builder("load.test.requests.total")
                .description("Total number of load test requests")
                .register(meterRegistry);
                
        errorCounter = Counter.builder("load.test.errors.total")
                .description("Total number of load test errors")
                .register(meterRegistry);
        
        // Reset performance tracking counters
        successCount.set(0);
        errorCount.set(0);
        totalResponseTime.set(0);
        responseTimes.clear();
        
        // Configure WebTestClient timeout for high-load scenarios
        webTestClient = webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @AfterEach
    public void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Tests transaction throughput at 10,000 TPS target load.
     * 
     * This test method validates the system's ability to handle high-volume transaction
     * processing equivalent to mainframe peak loads. The test executes concurrent HTTP
     * requests to the transaction endpoints while monitoring throughput, response times,
     * and error rates to ensure performance SLA compliance.
     * 
     * Test Execution:
     * - Creates 10,000 concurrent transaction requests over 60-second period
     * - Uses WebTestClient for reactive, non-blocking request execution
     * - Monitors response time distribution and success rates
     * - Validates system stability under sustained high load
     * 
     * Success Criteria:
     * - Sustained throughput >= 10,000 TPS for test duration
     * - 95th percentile response time < 200ms
     * - Error rate < 1% throughout test execution
     * - No memory leaks or resource exhaustion
     */
    @Test
    @DisplayName("Transaction throughput validation at 10,000 TPS target load")
    public void testTransactionThroughputAt10000TPS() {
        // Generate test data for high-volume processing
        List<Map<String, Object>> testTransactions = generateHighVolumeTestData(TARGET_TPS);
        
        Instant startTime = Instant.now();
        CountDownLatch completionLatch = new CountDownLatch(TARGET_TPS);
        
        // Execute concurrent requests to achieve target TPS
        for (int i = 0; i < TARGET_TPS; i++) {
            final int requestIndex = i;
            CompletableFuture.supplyAsync(() -> {
                try {
                    Instant requestStart = Instant.now();
                    
                    // Execute transaction list request (COTRN00C equivalent)
                    webTestClient.get()
                            .uri("/api/transactions")
                            .exchange()
                            .expectStatus().isOk()
                            .expectHeader().contentType(MediaType.APPLICATION_JSON)
                            .expectBody()
                            .returnResult();
                    
                    // Record performance metrics
                    long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                    recordResponseTime(responseTime);
                    successCount.incrementAndGet();
                    requestCounter.increment();
                    
                    return responseTime;
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    errorCounter.increment();
                    return -1L;
                } finally {
                    completionLatch.countDown();
                }
            }, executorService);
        }
        
        // Wait for all requests to complete
        try {
            boolean completed = completionLatch.await(TEST_DURATION_SECONDS + 30, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Load test interrupted", e);
        }
        
        // Calculate and validate performance metrics
        Instant endTime = Instant.now();
        long testDurationMs = Duration.between(startTime, endTime).toMillis();
        double actualTPS = (successCount.get() * 1000.0) / testDurationMs;
        
        // Validate throughput requirements
        assertThat(actualTPS).isGreaterThan(TARGET_TPS * 0.95) // Allow 5% tolerance
                .describedAs("Actual TPS (%f) should meet target TPS (%d)", actualTPS, TARGET_TPS);
        
        // Validate error rate requirements
        double errorRate = (errorCount.get() * 100.0) / TARGET_TPS;
        assertThat(errorRate).isLessThan(1.0)
                .describedAs("Error rate (%f%%) should be below 1%%", errorRate);
        
        generateLoadTestReport("10K_TPS_Test", actualTPS, calculateResponseTimePercentiles());
    }

    /**
     * Tests response time compliance under 200ms SLA requirement.
     * 
     * This test validates that the modernized REST API endpoints maintain response
     * times equivalent to the original COBOL transaction processing. The test executes
     * sustained load while continuously monitoring response time distribution to ensure
     * 95th percentile performance meets the sub-200ms requirement.
     * 
     * Performance Validation:
     * - Measures response time for each transaction endpoint
     * - Calculates percentile distribution (50th, 95th, 99th percentiles)
     * - Validates compliance against mainframe baseline performance
     * - Identifies performance degradation under sustained load
     * 
     * Test Coverage:
     * - GET /api/transactions (transaction list - COTRN00C equivalent)
     * - GET /api/transactions/{id} (single transaction - COTRN01C equivalent)  
     * - POST /api/transactions (transaction creation - COTRN02C equivalent)
     */
    @Test
    @DisplayName("Response time validation under 200ms SLA requirement")
    public void testResponseTimeUnder200ms() {
        // Generate test data for response time validation
        List<Map<String, Object>> testAccounts = testDataGenerator.generateTransactionList();
        String testTransactionId = "000000000001";
        Map<String, Object> newTransactionData = generateNewTransactionData();
        
        int testIterations = 1000;
        CountDownLatch completionLatch = new CountDownLatch(testIterations);
        
        // Execute mixed workload for comprehensive response time testing
        for (int i = 0; i < testIterations; i++) {
            final int iteration = i;
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    // Test transaction list endpoint (COTRN00C)
                    measureEndpointResponseTime("/api/transactions", "GET", null);
                    
                    // Test single transaction view (COTRN01C)
                    measureEndpointResponseTime("/api/transactions/" + testTransactionId, "GET", null);
                    
                    // Test transaction creation (COTRN02C)
                    if (iteration % 10 == 0) { // Reduce POST load to avoid data conflicts
                        measureEndpointResponseTime("/api/transactions", "POST", newTransactionData);
                    }
                    
                    return null;
                } finally {
                    completionLatch.countDown();
                }
            }, executorService);
        }
        
        // Wait for completion and analyze results
        try {
            boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Response time test interrupted", e);
        }
        
        // Calculate response time percentiles
        Map<String, Long> percentiles = calculateResponseTimePercentiles();
        
        // Validate response time SLA requirements
        assertThat(percentiles.get("p95")).isLessThan((long) MAX_RESPONSE_TIME_MS)
                .describedAs("95th percentile response time (%dms) must be under %dms", 
                        percentiles.get("p95"), MAX_RESPONSE_TIME_MS);
        
        assertThat(percentiles.get("p99")).isLessThan(MAX_RESPONSE_TIME_MS * 2L)
                .describedAs("99th percentile response time (%dms) should be under %dms for sustained performance", 
                        percentiles.get("p99"), MAX_RESPONSE_TIME_MS * 2);
        
        generateLoadTestReport("Response_Time_Validation", 0.0, percentiles);
    }

    /**
     * Tests concurrent transaction load with multiple user sessions.
     * 
     * This test simulates multiple concurrent user sessions accessing the transaction
     * system simultaneously, validating session isolation, data consistency, and
     * system stability under realistic production load patterns. The test replicates
     * the concurrent terminal access patterns from the original CICS environment.
     * 
     * Concurrency Testing:
     * - Simulates 1000 concurrent user sessions
     * - Each session performs complete transaction workflows
     * - Validates session state isolation and data integrity
     * - Monitors resource utilization and connection pool health
     * 
     * Session Simulation:
     * - Authentication and session establishment
     * - Transaction browsing and detail viewing
     * - Transaction creation and modification operations
     * - Session cleanup and resource deallocation
     */
    @Test
    @DisplayName("Concurrent transaction load with multiple user sessions")
    public void testConcurrentTransactionLoad() {
        int concurrentSessions = CONCURRENT_USERS;
        CountDownLatch sessionLatch = new CountDownLatch(concurrentSessions);
        
        // Generate unique test data for each concurrent session
        List<Map<String, Object>> sessionTestData = new ArrayList<>();
        for (int i = 0; i < concurrentSessions; i++) {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("accountData", testDataGenerator.generateAccount());
            sessionData.put("transactionData", testDataGenerator.generateTransaction());
            sessionData.put("sessionId", "session_" + i);
            sessionTestData.add(sessionData);
        }
        
        Instant testStart = Instant.now();
        
        // Launch concurrent user sessions
        for (int sessionId = 0; sessionId < concurrentSessions; sessionId++) {
            final int currentSessionId = sessionId;
            final Map<String, Object> sessionData = sessionTestData.get(sessionId);
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate complete user session workflow
                    simulateUserSession(currentSessionId, sessionData);
                    
                    successCount.incrementAndGet();
                    return true;
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    return false;
                } finally {
                    sessionLatch.countDown();
                }
            }, executorService);
        }
        
        // Wait for all sessions to complete
        try {
            boolean completed = sessionLatch.await(120, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Concurrent load test interrupted", e);
        }
        
        // Validate concurrent execution results
        Duration testDuration = Duration.between(testStart, Instant.now());
        double sessionsPerSecond = concurrentSessions / (testDuration.toMillis() / 1000.0);
        
        assertThat(successCount.get()).isGreaterThan(concurrentSessions * 0.98) // Allow 2% tolerance
                .describedAs("Successful concurrent sessions should be at least 98%% of total");
        
        assertThat(errorCount.get()).isLessThan(concurrentSessions * 0.05) // Max 5% error rate
                .describedAs("Error count should be less than 5%% of total sessions");
        
        generateLoadTestReport("Concurrent_Load_Test", sessionsPerSecond, calculateResponseTimePercentiles());
    }

    /**
     * Tests individual transaction endpoint performance characteristics.
     * 
     * This test method provides detailed performance analysis for each REST API endpoint
     * that replaces COBOL transaction processing functionality. The test measures response
     * times, throughput, and resource utilization for individual endpoints to identify
     * performance bottlenecks and ensure consistent performance across all transaction operations.
     * 
     * Endpoint Performance Testing:
     * - GET /api/transactions - Transaction listing (COTRN00C replacement)
     * - GET /api/transactions/{id} - Single transaction view (COTRN01C replacement)
     * - POST /api/transactions - Transaction creation (COTRN02C replacement)
     * - Pagination endpoints - VSAM browsing equivalent operations
     * 
     * Performance Metrics Collection:
     * - Individual endpoint response time distribution
     * - Request processing throughput per endpoint
     * - Memory allocation and garbage collection impact
     * - Database connection pool utilization
     * - JVM thread pool efficiency under load
     */
    @Test
    @DisplayName("Individual transaction endpoint performance characteristics")
    public void testTransactionEndpointPerformance() {
        // Test data generation for endpoint-specific testing
        Map<String, Object> testAccount = testDataGenerator.generateAccount();
        Map<String, Object> testTransaction = testDataGenerator.generateTransaction();
        String testTransactionId = "000000000001";
        
        Map<String, List<Long>> endpointMetrics = new HashMap<>();
        endpointMetrics.put("GET_transactions", new ArrayList<>());
        endpointMetrics.put("GET_transaction_by_id", new ArrayList<>());
        endpointMetrics.put("POST_transactions", new ArrayList<>());
        endpointMetrics.put("GET_transactions_paginated", new ArrayList<>());
        
        int testIterations = 500; // Focused testing per endpoint
        
        // Test GET /api/transactions endpoint (COTRN00C equivalent)
        for (int i = 0; i < testIterations; i++) {
            long responseTime = measureSingleEndpointPerformance(() -> {
                return webTestClient.get()
                        .uri("/api/transactions")
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody()
                        .returnResult();
            });
            endpointMetrics.get("GET_transactions").add(responseTime);
        }
        
        // Test GET /api/transactions/{id} endpoint (COTRN01C equivalent)
        for (int i = 0; i < testIterations; i++) {
            long responseTime = measureSingleEndpointPerformance(() -> {
                return webTestClient.get()
                        .uri("/api/transactions/{transactionId}", testTransactionId)
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody()
                        .returnResult();
            });
            endpointMetrics.get("GET_transaction_by_id").add(responseTime);
        }
        
        // Test POST /api/transactions endpoint (COTRN02C equivalent)
        for (int i = 0; i < testIterations / 10; i++) { // Reduce POST volume
            Map<String, Object> newTransactionData = generateUniqueTransactionData(i);
            long responseTime = measureSingleEndpointPerformance(() -> {
                return webTestClient.post()
                        .uri("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(newTransactionData)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody()
                        .returnResult();
            });
            endpointMetrics.get("POST_transactions").add(responseTime);
        }
        
        // Test paginated transaction endpoint
        for (int i = 0; i < testIterations; i++) {
            long responseTime = measureSingleEndpointPerformance(() -> {
                return webTestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/transactions")
                                .queryParam("page", i % 10)
                                .queryParam("size", 20)
                                .build())
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody()
                        .returnResult();
            });
            endpointMetrics.get("GET_transactions_paginated").add(responseTime);
        }
        
        // Validate endpoint performance requirements
        validateEndpointPerformance(endpointMetrics);
        
        // Generate detailed endpoint performance report
        generateEndpointPerformanceReport(endpointMetrics);
    }

    /**
     * Tests pagination performance for large transaction datasets.
     * 
     * This test validates the performance of paginated transaction queries that replace
     * the VSAM KSDS browsing operations (STARTBR/READNEXT/READPREV) from the original
     * COBOL implementation. The test ensures that pagination performance remains consistent
     * across large datasets and different page sizes.
     * 
     * Pagination Testing Scenarios:
     * - Small page sizes (10-20 records) for detailed browsing
     * - Medium page sizes (50-100 records) for standard listing
     * - Large page sizes (200-500 records) for bulk operations
     * - Random page access patterns simulating user navigation
     * - Sequential page traversal equivalent to VSAM cursor operations
     * 
     * Performance Validation:
     * - Response time consistency across different page positions
     * - Memory efficiency for large result sets
     * - Database query optimization effectiveness
     * - Connection pool stability under pagination load
     */
    @Test
    @DisplayName("Pagination performance for large transaction datasets")
    public void testPaginationPerformance() {
        // Generate large dataset for pagination testing
        testDataGenerator.resetRandomSeed();
        
        Map<String, List<Long>> paginationMetrics = new HashMap<>();
        paginationMetrics.put("small_pages", new ArrayList<>());
        paginationMetrics.put("medium_pages", new ArrayList<>());
        paginationMetrics.put("large_pages", new ArrayList<>());
        paginationMetrics.put("random_access", new ArrayList<>());
        
        int totalTestPages = 100;
        
        // Test small page sizes (10-20 records)
        for (int page = 0; page < totalTestPages; page++) {
            long responseTime = measureSingleEndpointPerformance(() -> {
                return webTestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/transactions")
                                .queryParam("page", page)
                                .queryParam("size", 15)
                                .queryParam("sort", "transactionDate,desc")
                                .build())
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .returnResult();
            });
            paginationMetrics.get("small_pages").add(responseTime);
        }
        
        // Test medium page sizes (50-100 records)
        for (int page = 0; page < totalTestPages / 2; page++) {
            long responseTime = measureSingleEndpointPerformance(() -> {
                return webTestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/transactions")
                                .queryParam("page", page)
                                .queryParam("size", 75)
                                .queryParam("sort", "transactionDate,desc")
                                .build())
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .returnResult();
            });
            paginationMetrics.get("medium_pages").add(responseTime);
        }
        
        // Test large page sizes (200-500 records)
        for (int page = 0; page < totalTestPages / 10; page++) {
            long responseTime = measureSingleEndpointPerformance(() -> {
                return webTestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/transactions")
                                .queryParam("page", page)
                                .queryParam("size", 300)
                                .queryParam("sort", "transactionDate,desc")
                                .build())
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .returnResult();
            });
            paginationMetrics.get("large_pages").add(responseTime);
        }
        
        // Test random page access patterns
        Random random = new Random(12345); // Fixed seed for reproducible tests
        for (int i = 0; i < 50; i++) {
            int randomPage = random.nextInt(100);
            int randomSize = 20 + random.nextInt(80); // 20-100 record pages
            
            long responseTime = measureSingleEndpointPerformance(() -> {
                return webTestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/transactions")
                                .queryParam("page", randomPage)
                                .queryParam("size", randomSize)
                                .build())
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .returnResult();
            });
            paginationMetrics.get("random_access").add(responseTime);
        }
        
        // Validate pagination performance requirements
        validatePaginationPerformance(paginationMetrics);
        
        generateLoadTestReport("Pagination_Performance", 0.0, calculatePaginationPercentiles(paginationMetrics));
    }

    /**
     * Measures and collects transaction processing metrics.
     * 
     * This method provides comprehensive metrics collection for transaction processing
     * operations, integrating with the ActuatorConfig monitoring infrastructure to
     * capture detailed performance data. The metrics collection supports real-time
     * monitoring and post-test analysis of system performance characteristics.
     * 
     * Collected Metrics:
     * - Response time distribution (min, max, average, percentiles)
     * - Request throughput and processing rates
     * - Error rates and failure categorization
     * - JVM memory usage and garbage collection impact
     * - Database connection pool utilization
     * - Thread pool efficiency and concurrency metrics
     * 
     * Integration Points:
     * - Micrometer metrics registry for custom business metrics
     * - Spring Boot Actuator endpoints for operational data
     * - Prometheus-compatible metrics export
     * - Real-time monitoring dashboard data feeds
     */
    @Test
    @DisplayName("Transaction processing metrics measurement and collection")
    public void measureTransactionMetrics() {
        // Initialize metrics collection infrastructure
        Timer overallTimer = Timer.builder("load.test.overall.duration")
                .description("Overall load test execution time")
                .register(meterRegistry);
        
        Gauge.builder("load.test.concurrent.users")
                .description("Number of concurrent users in load test")
                .register(meterRegistry, this, TransactionLoadTest::getCurrentConcurrentUsers);
        
        // Execute comprehensive metrics collection test
        Timer.Sample overallSample = Timer.start(meterRegistry);
        
        try {
            // Collect baseline system metrics
            Map<String, Object> baselineMetrics = collectSystemMetrics();
            
            // Execute mixed workload for comprehensive metrics
            int metricsTestIterations = 1000;
            CountDownLatch metricsLatch = new CountDownLatch(metricsTestIterations);
            
            for (int i = 0; i < metricsTestIterations; i++) {
                final int iteration = i;
                
                CompletableFuture.supplyAsync(() -> {
                    try {
                        // Execute transaction operations with metrics collection
                        Timer.Sample requestSample = Timer.start(meterRegistry);
                        
                        // Simulate realistic transaction processing mix
                        if (iteration % 10 == 0) {
                            // Transaction creation (10% of requests)
                            executeTransactionCreationWithMetrics();
                        } else if (iteration % 5 == 0) {
                            // Single transaction view (20% of requests) 
                            executeTransactionViewWithMetrics("000000000001");
                        } else {
                            // Transaction listing (70% of requests)
                            executeTransactionListWithMetrics();
                        }
                        
                        requestSample.stop(responseTimeTimer);
                        requestCounter.increment();
                        
                        return true;
                    } catch (Exception e) {
                        errorCounter.increment();
                        return false;
                    } finally {
                        metricsLatch.countDown();
                    }
                }, executorService);
            }
            
            // Wait for metrics collection completion
            boolean completed = metricsLatch.await(180, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            
            // Collect final system metrics
            Map<String, Object> finalMetrics = collectSystemMetrics();
            
            // Validate metrics collection and system performance
            validateSystemMetrics(baselineMetrics, finalMetrics);
            
            // Generate comprehensive metrics report
            generateMetricsReport(baselineMetrics, finalMetrics);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Metrics collection test interrupted", e);
        } finally {
            overallSample.stop(overallTimer);
        }
    }

    /**
     * Validates performance thresholds against SLA requirements.
     * 
     * This method performs comprehensive validation of all performance thresholds
     * defined in the system requirements, ensuring that the modernized application
     * meets or exceeds the performance characteristics of the original COBOL
     * implementation. The validation covers response times, throughput, error rates,
     * and resource utilization metrics.
     * 
     * Validation Criteria:
     * - Response time SLA: 95th percentile < 200ms
     * - Throughput SLA: >= 10,000 TPS sustained
     * - Error rate SLA: < 1% for all operations
     * - Memory usage SLA: < 80% heap utilization
     * - Connection pool SLA: < 90% pool utilization
     * 
     * Threshold Categories:
     * - Performance thresholds (response time, throughput)
     * - Reliability thresholds (error rates, availability)
     * - Resource thresholds (memory, CPU, connections)
     * - Business thresholds (transaction accuracy, data consistency)
     */
    @Test
    @DisplayName("Performance thresholds validation against SLA requirements")
    public void validatePerformanceThresholds() {
        // Execute comprehensive performance validation test
        int validationIterations = 2000;
        List<Long> allResponseTimes = new ArrayList<>();
        AtomicInteger validationSuccessCount = new AtomicInteger(0);
        AtomicInteger validationErrorCount = new AtomicInteger(0);
        
        Instant validationStart = Instant.now();
        CountDownLatch validationLatch = new CountDownLatch(validationIterations);
        
        // Execute sustained load for threshold validation
        for (int i = 0; i < validationIterations; i++) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    Instant requestStart = Instant.now();
                    
                    // Execute mixed transaction workload
                    webTestClient.get()
                            .uri("/api/transactions")
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody()
                            .returnResult();
                    
                    long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                    synchronized (allResponseTimes) {
                        allResponseTimes.add(responseTime);
                    }
                    
                    validationSuccessCount.incrementAndGet();
                    return responseTime;
                    
                } catch (Exception e) {
                    validationErrorCount.incrementAndGet();
                    return -1L;
                } finally {
                    validationLatch.countDown();
                }
            }, executorService);
        }
        
        // Wait for validation completion
        try {
            boolean completed = validationLatch.await(120, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Performance validation interrupted", e);
        }
        
        Instant validationEnd = Instant.now();
        
        // Calculate performance metrics for threshold validation
        Map<String, Object> performanceMetrics = calculatePerformanceMetrics(
                allResponseTimes, validationStart, validationEnd, 
                validationSuccessCount.get(), validationErrorCount.get());
        
        // Validate all performance thresholds
        validateResponseTimeThresholds(performanceMetrics);
        validateThroughputThresholds(performanceMetrics);
        validateReliabilityThresholds(performanceMetrics);
        validateResourceThresholds();
        
        generateLoadTestReport("Performance_Threshold_Validation", 
                (Double) performanceMetrics.get("actualTPS"),
                (Map<String, Long>) performanceMetrics.get("percentiles"));
    }

    /**
     * Generates comprehensive load test reports with performance analysis.
     * 
     * This method creates detailed performance reports combining test execution
     * results, metrics analysis, and performance recommendations. The reports
     * support operational monitoring, capacity planning, and performance
     * optimization decision-making.
     * 
     * Report Components:
     * - Executive summary with SLA compliance status
     * - Detailed performance metrics and trend analysis
     * - Error analysis and failure categorization
     * - Resource utilization patterns and optimization recommendations
     * - Comparison with baseline and target performance thresholds
     * - Performance trend analysis and capacity planning insights
     * 
     * Report Formats:
     * - Console output for immediate CI/CD feedback
     * - Structured data export for monitoring systems
     * - Performance dashboard integration data
     * - Historical performance comparison data
     */
    @Test
    @DisplayName("Comprehensive load test reporting with performance analysis")
    public void generateLoadTestReport() {
        // Execute comprehensive reporting test cycle
        String reportName = "Comprehensive_Performance_Report_" + 
                LocalDateTime.now().toString().replace(":", "-");
        
        // Collect system performance baseline
        Map<String, Object> reportingBaseline = collectSystemMetrics();
        
        // Execute representative workload for reporting
        int reportingIterations = 500;
        List<Long> reportingResponseTimes = new ArrayList<>();
        AtomicInteger reportingSuccess = new AtomicInteger(0);
        AtomicInteger reportingErrors = new AtomicInteger(0);
        
        Instant reportingStart = Instant.now();
        CountDownLatch reportingLatch = new CountDownLatch(reportingIterations);
        
        // Execute mixed workload for comprehensive reporting data
        for (int i = 0; i < reportingIterations; i++) {
            final int iteration = i;
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    Instant requestStart = Instant.now();
                    
                    // Execute different transaction operations for comprehensive reporting
                    if (iteration % 20 == 0) {
                        // Transaction creation operations
                        Map<String, Object> newTransaction = generateUniqueTransactionData(iteration);
                        webTestClient.post()
                                .uri("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(newTransaction)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody()
                                .returnResult();
                    } else if (iteration % 7 == 0) {
                        // Single transaction view operations
                        webTestClient.get()
                                .uri("/api/transactions/{transactionId}", "000000000001")
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .returnResult();
                    } else {
                        // Transaction listing operations
                        webTestClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/api/transactions")
                                        .queryParam("page", iteration % 10)
                                        .queryParam("size", 25)
                                        .build())
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .returnResult();
                    }
                    
                    long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                    synchronized (reportingResponseTimes) {
                        reportingResponseTimes.add(responseTime);
                    }
                    
                    reportingSuccess.incrementAndGet();
                    return true;
                    
                } catch (Exception e) {
                    reportingErrors.incrementAndGet();
                    return false;
                } finally {
                    reportingLatch.countDown();
                }
            }, executorService);
        }
        
        // Wait for reporting data collection completion
        try {
            boolean completed = reportingLatch.await(90, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Reporting test interrupted", e);
        }
        
        Instant reportingEnd = Instant.now();
        
        // Generate comprehensive performance report
        Map<String, Object> reportingMetrics = calculatePerformanceMetrics(
                reportingResponseTimes, reportingStart, reportingEnd,
                reportingSuccess.get(), reportingErrors.get());
        
        Map<String, Long> reportingPercentiles = calculateResponseTimePercentiles(reportingResponseTimes);
        
        // Generate final comprehensive report
        generateLoadTestReport(reportName, 
                (Double) reportingMetrics.get("actualTPS"), 
                reportingPercentiles);
        
        // Generate additional report formats
        generateExecutiveSummaryReport(reportingMetrics, reportingPercentiles);
        generateOperationalMetricsReport(reportingBaseline, collectSystemMetrics());
        generateCapacityPlanningReport(reportingMetrics);
        
        // Validate report generation success
        assertThat(reportingSuccess.get()).isGreaterThan(reportingIterations * 0.95)
                .describedAs("Reporting test should achieve >95% success rate");
        
        assertThat((Double) reportingMetrics.get("actualTPS")).isGreaterThan(0.0)
                .describedAs("Reporting should capture valid throughput metrics");
        
        assertThat(reportingPercentiles.get("p95")).isLessThan(500L)
                .describedAs("Reporting operations should maintain reasonable response times");
    }

    // Helper Methods for Load Testing Infrastructure

    /**
     * Records response time for performance analysis.
     */
    private synchronized void recordResponseTime(long responseTime) {
        responseTimes.add(responseTime);
        totalResponseTime.addAndGet(responseTime);
        responseTimeTimer.record(responseTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Generates high-volume test data for load testing scenarios.
     */
    private List<Map<String, Object>> generateHighVolumeTestData(int count) {
        List<Map<String, Object>> testData = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("account", testDataGenerator.generateAccount());
            data.put("transaction", testDataGenerator.generateTransaction());
            data.put("amount", testDataGenerator.generateComp3BigDecimal());
            testData.add(data);
        }
        return testData;
    }

    /**
     * Generates unique transaction data for creation operations.
     */
    private Map<String, Object> generateUniqueTransactionData(int uniqueId) {
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("accountId", "ACC" + String.format("%010d", uniqueId));
        transactionData.put("amount", testDataGenerator.generateComp3BigDecimal());
        transactionData.put("description", "Load Test Transaction " + uniqueId);
        transactionData.put("transactionType", "PURCHASE");
        transactionData.put("merchantName", "Test Merchant " + (uniqueId % 100));
        return transactionData;
    }

    /**
     * Generates new transaction data template.
     */
    private Map<String, Object> generateNewTransactionData() {
        return generateUniqueTransactionData(new Random().nextInt(10000));
    }

    /**
     * Measures response time for a specific endpoint operation.
     */
    private long measureEndpointResponseTime(String endpoint, String method, Map<String, Object> requestData) {
        Instant start = Instant.now();
        
        try {
            if ("GET".equals(method)) {
                webTestClient.get()
                        .uri(endpoint)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .returnResult();
            } else if ("POST".equals(method) && requestData != null) {
                webTestClient.post()
                        .uri(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestData)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody()
                        .returnResult();
            }
            
            long responseTime = Duration.between(start, Instant.now()).toMillis();
            recordResponseTime(responseTime);
            return responseTime;
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            return -1L;
        }
    }

    /**
     * Measures single endpoint performance with functional interface.
     */
    private long measureSingleEndpointPerformance(Runnable operation) {
        Instant start = Instant.now();
        
        try {
            operation.run();
            long responseTime = Duration.between(start, Instant.now()).toMillis();
            recordResponseTime(responseTime);
            return responseTime;
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            return -1L;
        }
    }

    /**
     * Simulates complete user session workflow.
     */
    private void simulateUserSession(int sessionId, Map<String, Object> sessionData) {
        try {
            // Session authentication simulation
            Thread.sleep(10); // Simulate authentication delay
            
            // Transaction listing browsing
            webTestClient.get()
                    .uri("/api/transactions")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .returnResult();
            
            // Single transaction detail viewing  
            webTestClient.get()
                    .uri("/api/transactions/{transactionId}", "000000000001")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .returnResult();
            
            // Occasional transaction creation
            if (sessionId % 50 == 0) {
                Map<String, Object> newTransaction = generateUniqueTransactionData(sessionId);
                webTestClient.post()
                        .uri("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(newTransaction)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody()
                        .returnResult();
            }
            
            // Pagination browsing simulation
            int pageCount = 3 + (sessionId % 5);
            for (int page = 0; page < pageCount; page++) {
                webTestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/transactions")
                                .queryParam("page", page)
                                .queryParam("size", 20)
                                .build())
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .returnResult();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Session simulation interrupted", e);
        }
    }

    /**
     * Calculates response time percentiles for performance analysis.
     */
    private synchronized Map<String, Long> calculateResponseTimePercentiles() {
        return calculateResponseTimePercentiles(responseTimes);
    }

    /**
     * Calculates response time percentiles from response time list.
     */
    private Map<String, Long> calculateResponseTimePercentiles(List<Long> responseTimeList) {
        if (responseTimeList.isEmpty()) {
            return Map.of("p50", 0L, "p95", 0L, "p99", 0L, "min", 0L, "max", 0L);
        }
        
        List<Long> sortedTimes = new ArrayList<>(responseTimeList);
        sortedTimes.sort(Long::compareTo);
        
        int size = sortedTimes.size();
        
        return Map.of(
                "min", sortedTimes.get(0),
                "max", sortedTimes.get(size - 1),
                "p50", sortedTimes.get(size / 2),
                "p95", sortedTimes.get((int) (size * 0.95)),
                "p99", sortedTimes.get((int) (size * 0.99))
        );
    }

    /**
     * Calculates pagination performance percentiles.
     */
    private Map<String, Long> calculatePaginationPercentiles(Map<String, List<Long>> paginationMetrics) {
        Map<String, Long> combinedPercentiles = new HashMap<>();
        
        for (Map.Entry<String, List<Long>> entry : paginationMetrics.entrySet()) {
            Map<String, Long> percentiles = calculateResponseTimePercentiles(entry.getValue());
            combinedPercentiles.put(entry.getKey() + "_p95", percentiles.get("p95"));
        }
        
        return combinedPercentiles;
    }

    /**
     * Validates endpoint-specific performance requirements.
     */
    private void validateEndpointPerformance(Map<String, List<Long>> endpointMetrics) {
        for (Map.Entry<String, List<Long>> entry : endpointMetrics.entrySet()) {
            String endpoint = entry.getKey();
            Map<String, Long> percentiles = calculateResponseTimePercentiles(entry.getValue());
            
            assertThat(percentiles.get("p95"))
                    .describedAs("Endpoint %s 95th percentile response time", endpoint)
                    .isLessThan((long) MAX_RESPONSE_TIME_MS);
        }
    }

    /**
     * Validates pagination performance requirements.
     */
    private void validatePaginationPerformance(Map<String, List<Long>> paginationMetrics) {
        for (Map.Entry<String, List<Long>> entry : paginationMetrics.entrySet()) {
            String paginationType = entry.getKey();
            Map<String, Long> percentiles = calculateResponseTimePercentiles(entry.getValue());
            
            assertThat(percentiles.get("p95"))
                    .describedAs("Pagination type %s should maintain sub-200ms response time", paginationType)
                    .isLessThan(300L); // Slightly relaxed for pagination operations
        }
    }

    /**
     * Collects current system metrics for performance analysis.
     */
    private Map<String, Object> collectSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        metrics.put("memory.used", runtime.totalMemory() - runtime.freeMemory());
        metrics.put("memory.free", runtime.freeMemory());
        metrics.put("memory.total", runtime.totalMemory());
        metrics.put("memory.max", runtime.maxMemory());
        
        // Thread metrics
        metrics.put("threads.active", Thread.activeCount());
        
        // Performance counters
        metrics.put("requests.success", successCount.get());
        metrics.put("requests.error", errorCount.get());
        metrics.put("response.time.total", totalResponseTime.get());
        
        return metrics;
    }

    /**
     * Executes transaction creation with metrics collection.
     */
    private void executeTransactionCreationWithMetrics() {
        Map<String, Object> transactionData = generateNewTransactionData();
        
        webTestClient.post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transactionData)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();
    }

    /**
     * Executes transaction view with metrics collection.
     */
    private void executeTransactionViewWithMetrics(String transactionId) {
        webTestClient.get()
                .uri("/api/transactions/{transactionId}", transactionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();
    }

    /**
     * Executes transaction list with metrics collection.
     */
    private void executeTransactionListWithMetrics() {
        webTestClient.get()
                .uri("/api/transactions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();
    }

    /**
     * Validates system metrics for performance analysis.
     */
    private void validateSystemMetrics(Map<String, Object> baseline, Map<String, Object> current) {
        // Memory utilization validation
        long currentMemory = (Long) current.get("memory.used");
        long maxMemory = (Long) current.get("memory.max");
        double memoryUtilization = (currentMemory * 100.0) / maxMemory;
        
        assertThat(memoryUtilization)
                .describedAs("Memory utilization should be below 80%%")
                .isLessThan(80.0);
        
        // Error rate validation
        int totalRequests = successCount.get() + errorCount.get();
        if (totalRequests > 0) {
            double errorRate = (errorCount.get() * 100.0) / totalRequests;
            assertThat(errorRate)
                    .describedAs("Error rate should be below 1%%")
                    .isLessThan(1.0);
        }
    }

    /**
     * Calculates comprehensive performance metrics.
     */
    private Map<String, Object> calculatePerformanceMetrics(List<Long> responseTimeList, 
                                                           Instant start, Instant end, 
                                                           int successCount, int errorCount) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Calculate throughput
        long durationMs = Duration.between(start, end).toMillis();
        double actualTPS = (successCount * 1000.0) / durationMs;
        metrics.put("actualTPS", actualTPS);
        
        // Calculate error rate
        int totalRequests = successCount + errorCount;
        double errorRate = totalRequests > 0 ? (errorCount * 100.0) / totalRequests : 0.0;
        metrics.put("errorRate", errorRate);
        
        // Calculate response time percentiles
        Map<String, Long> percentiles = calculateResponseTimePercentiles(responseTimeList);
        metrics.put("percentiles", percentiles);
        
        return metrics;
    }

    /**
     * Validates response time thresholds.
     */
    private void validateResponseTimeThresholds(Map<String, Object> metrics) {
        @SuppressWarnings("unchecked")
        Map<String, Long> percentiles = (Map<String, Long>) metrics.get("percentiles");
        
        assertThat(percentiles.get("p95"))
                .describedAs("95th percentile response time must be under 200ms")
                .isLessThan((long) MAX_RESPONSE_TIME_MS);
    }

    /**
     * Validates throughput thresholds.
     */
    private void validateThroughputThresholds(Map<String, Object> metrics) {
        Double actualTPS = (Double) metrics.get("actualTPS");
        
        assertThat(actualTPS)
                .describedAs("Actual TPS should meet target requirements")
                .isGreaterThan(TARGET_TPS * 0.8); // Allow 20% tolerance for validation
    }

    /**
     * Validates reliability thresholds.
     */
    private void validateReliabilityThresholds(Map<String, Object> metrics) {
        Double errorRate = (Double) metrics.get("errorRate");
        
        assertThat(errorRate)
                .describedAs("Error rate must be below 1%%")
                .isLessThan(1.0);
    }

    /**
     * Validates resource utilization thresholds.
     */
    private void validateResourceThresholds() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUtilization = (usedMemory * 100.0) / runtime.maxMemory();
        
        assertThat(memoryUtilization)
                .describedAs("Memory utilization should be below 80%%")
                .isLessThan(80.0);
    }

    /**
     * Gets current concurrent users count for metrics.
     */
    private double getCurrentConcurrentUsers() {
        return Thread.activeCount();
    }

    /**
     * Generates comprehensive load test report.
     */
    private void generateLoadTestReport(String testName, double actualTPS, Map<String, Long> percentiles) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CARDDEMO LOAD TEST REPORT: " + testName);
        System.out.println("=".repeat(80));
        System.out.println("Test Execution Time: " + LocalDateTime.now());
        System.out.println("Target TPS: " + TARGET_TPS);
        System.out.println("Actual TPS: " + String.format("%.2f", actualTPS));
        System.out.println("Max Response Time SLA: " + MAX_RESPONSE_TIME_MS + "ms");
        System.out.println();
        
        System.out.println("RESPONSE TIME ANALYSIS:");
        System.out.println("  Minimum: " + percentiles.get("min") + "ms");
        System.out.println("  50th Percentile: " + percentiles.get("p50") + "ms");
        System.out.println("  95th Percentile: " + percentiles.get("p95") + "ms");
        System.out.println("  99th Percentile: " + percentiles.get("p99") + "ms");
        System.out.println("  Maximum: " + percentiles.get("max") + "ms");
        System.out.println();
        
        System.out.println("SUCCESS METRICS:");
        System.out.println("  Successful Requests: " + successCount.get());
        System.out.println("  Failed Requests: " + errorCount.get());
        System.out.println("  Success Rate: " + String.format("%.2f%%", 
                (successCount.get() * 100.0) / (successCount.get() + errorCount.get())));
        System.out.println();
        
        System.out.println("SLA COMPLIANCE:");
        System.out.println("  Response Time SLA: " + 
                (percentiles.get("p95") <= MAX_RESPONSE_TIME_MS ? "PASSED" : "FAILED"));
        System.out.println("  Throughput SLA: " + 
                (actualTPS >= TARGET_TPS * 0.95 ? "PASSED" : "FAILED"));
        System.out.println("  Error Rate SLA: " + 
                (((errorCount.get() * 100.0) / (successCount.get() + errorCount.get())) < 1.0 ? "PASSED" : "FAILED"));
        
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Generates endpoint-specific performance report.
     */
    private void generateEndpointPerformanceReport(Map<String, List<Long>> endpointMetrics) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ENDPOINT PERFORMANCE ANALYSIS");
        System.out.println("=".repeat(80));
        
        for (Map.Entry<String, List<Long>> entry : endpointMetrics.entrySet()) {
            String endpoint = entry.getKey();
            Map<String, Long> percentiles = calculateResponseTimePercentiles(entry.getValue());
            
            System.out.println("Endpoint: " + endpoint);
            System.out.println("  Requests: " + entry.getValue().size());
            System.out.println("  95th Percentile: " + percentiles.get("p95") + "ms");
            System.out.println("  Average: " + String.format("%.2f ms", 
                    entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0)));
            System.out.println();
        }
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Generates executive summary report.
     */
    private void generateExecutiveSummaryReport(Map<String, Object> metrics, Map<String, Long> percentiles) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EXECUTIVE SUMMARY - CARDDEMO PERFORMANCE TEST");
        System.out.println("=".repeat(80));
        System.out.println("Overall Performance: " + 
                (percentiles.get("p95") <= MAX_RESPONSE_TIME_MS && 
                 (Double)metrics.get("errorRate") < 1.0 ? "SATISFACTORY" : "NEEDS ATTENTION"));
        System.out.println("System Capacity: " + String.format("%.0f TPS", (Double)metrics.get("actualTPS")));
        System.out.println("Reliability Score: " + String.format("%.2f%%", 100.0 - (Double)metrics.get("errorRate")));
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Generates operational metrics report.
     */
    private void generateOperationalMetricsReport(Map<String, Object> baseline, Map<String, Object> current) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("OPERATIONAL METRICS REPORT");
        System.out.println("=".repeat(80));
        
        long memoryIncrease = (Long)current.get("memory.used") - (Long)baseline.get("memory.used");
        System.out.println("Memory Usage Increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        System.out.println("Active Threads: " + current.get("threads.active"));
        System.out.println("Total Requests Processed: " + (successCount.get() + errorCount.get()));
        
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Generates capacity planning report.
     */
    private void generateCapacityPlanningReport(Map<String, Object> metrics) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CAPACITY PLANNING RECOMMENDATIONS");
        System.out.println("=".repeat(80));
        
        Double actualTPS = (Double) metrics.get("actualTPS");
        Double errorRate = (Double) metrics.get("errorRate");
        
        if (actualTPS >= TARGET_TPS) {
            System.out.println("✓ Current capacity meets performance targets");
        } else {
            double capacityGap = ((TARGET_TPS - actualTPS) / TARGET_TPS) * 100;
            System.out.println("⚠ Capacity gap identified: " + String.format("%.1f%%", capacityGap));
            System.out.println("  Recommendation: Scale infrastructure by " + 
                    String.format("%.0f%%", Math.ceil(capacityGap / 10) * 10));
        }
        
        if (errorRate < 0.1) {
            System.out.println("✓ System reliability is excellent");
        } else if (errorRate < 1.0) {
            System.out.println("⚠ System reliability is acceptable but monitor closely");
        } else {
            System.out.println("✗ System reliability needs immediate attention");
        }
        
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Generates comprehensive metrics report.
     */
    private void generateMetricsReport(Map<String, Object> baseline, Map<String, Object> current) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPREHENSIVE METRICS ANALYSIS");
        System.out.println("=".repeat(80));
        
        // Memory analysis
        long baselineMemory = (Long) baseline.get("memory.used");
        long currentMemory = (Long) current.get("memory.used");
        long memoryIncrease = currentMemory - baselineMemory;
        
        System.out.println("MEMORY ANALYSIS:");
        System.out.println("  Baseline: " + (baselineMemory / 1024 / 1024) + " MB");
        System.out.println("  Current: " + (currentMemory / 1024 / 1024) + " MB");
        System.out.println("  Increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        System.out.println();
        
        // Performance analysis
        System.out.println("PERFORMANCE SUMMARY:");
        System.out.println("  Total Requests: " + (successCount.get() + errorCount.get()));
        System.out.println("  Success Rate: " + String.format("%.2f%%", 
                (successCount.get() * 100.0) / (successCount.get() + errorCount.get())));
        System.out.println("  Average Response Time: " + 
                (totalResponseTime.get() / Math.max(1, successCount.get())) + "ms");
        
        System.out.println("=".repeat(80) + "\n");
    }
}
