package com.carddemo.service;

import org.junit.jupiter.api.Assertions;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.carddemo.test.TestConstants;

import java.lang.System;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Map;
import java.util.HashMap;
import java.util.DoubleSummaryStatistics;

/**
 * Comprehensive performance testing utility class for measuring and validating 
 * service performance against the 200ms response time requirement and ensuring
 * COBOL-equivalent precision in financial calculations.
 * 
 * This class provides methods for:
 * - Measuring execution times with nanosecond precision
 * - Validating response times against SLA thresholds
 * - Calculating throughput under load conditions
 * - Analyzing latency percentiles and performance trends
 * - Generating comprehensive performance reports
 * - Supporting warmup and JVM optimization considerations
 * - Tracking performance regression detection
 * 
 * Integrates with Spring Boot testing framework and supports both unit-level
 * and integration-level performance validation scenarios.
 */
public class PerformanceTestUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTestUtils.class);
    
    /**
     * Number of warmup iterations to perform before measurement to ensure JVM optimization.
     * This helps eliminate cold-start effects and provides more accurate performance metrics.
     */
    public static final int WARMUP_ITERATIONS = 1000;
    
    /**
     * Number of measurement iterations to perform for statistical accuracy.
     * Higher iteration count provides more reliable performance statistics.
     */
    public static final int MEASUREMENT_ITERATIONS = 5000;
    
    // Performance thresholds - would normally come from TestConstants
    private static final long RESPONSE_TIME_THRESHOLD_MS = 200L;
    private static final int TARGET_TPS = 10000;
    private static final int BATCH_PROCESSING_WINDOW_HOURS = 4;
    private static final int COBOL_DECIMAL_SCALE = 2;
    private static final java.math.RoundingMode COBOL_ROUNDING_MODE = java.math.RoundingMode.HALF_UP;
    
    // Performance tracking data structures
    private static final List<Long> performanceHistory = new ArrayList<>();
    private static final Map<String, List<Double>> performanceTrends = new HashMap<>();
    
    /**
     * Measures the execution time of a given operation with nanosecond precision.
     * Performs JVM warmup before measurement to ensure accurate results.
     * 
     * @param operation The operation to measure
     * @return Execution time in milliseconds
     * @throws RuntimeException if operation execution fails
     */
    public static long measureExecutionTime(Supplier<Object> operation) {
        Assertions.assertNotNull(operation, "Operation cannot be null for performance measurement");
        
        // Perform JVM warmup
        warmupJvm(operation);
        
        // Record start time with nanosecond precision
        long startNanos = System.nanoTime();
        Instant startTime = Instant.now();
        
        try {
            // Execute the operation
            Object result = operation.get();
            Assertions.assertNotNull(result, "Operation must return a non-null result for valid measurement");
            
        } catch (Exception e) {
            logger.error("Performance measurement failed during operation execution", e);
            Assertions.fail("Performance test operation execution failed: " + e.getMessage());
        }
        
        // Record end time and calculate duration
        long endNanos = System.nanoTime();
        long durationNanos = endNanos - startNanos;
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos);
        
        // Log performance metrics for monitoring
        logPerformanceMetrics("ExecutionTime", durationMillis, startTime);
        
        return durationMillis;
    }
    
    /**
     * Validates that a response time meets the SLA requirement of sub-200ms.
     * Records validation results for trend analysis and regression detection.
     * 
     * @param responseTimeMs The measured response time in milliseconds
     * @return true if response time meets SLA, false otherwise
     */
    public static boolean validateResponseTime(long responseTimeMs) {
        boolean isValid = responseTimeMs < RESPONSE_TIME_THRESHOLD_MS;
        
        // Log validation result with detailed context
        if (isValid) {
            logger.info("Response time validation PASSED: {}ms < {}ms threshold", 
                       responseTimeMs, RESPONSE_TIME_THRESHOLD_MS);
        } else {
            logger.error("Response time validation FAILED: {}ms >= {}ms threshold", 
                        responseTimeMs, RESPONSE_TIME_THRESHOLD_MS);
        }
        
        // Track performance history for trend analysis
        performanceHistory.add(responseTimeMs);
        
        // Assert SLA compliance for test failure on performance regression
        Assertions.assertTrue(isValid, 
            String.format("Response time %dms exceeds SLA threshold of %dms", 
                         responseTimeMs, RESPONSE_TIME_THRESHOLD_MS));
        
        return isValid;
    }
    
    /**
     * Calculates system throughput under load conditions.
     * Measures requests per second (TPS) to validate against target performance.
     * 
     * @param operation The operation to test under load
     * @param concurrentUsers Number of concurrent users to simulate
     * @param testDurationSeconds Duration of the load test in seconds
     * @return Calculated throughput in transactions per second
     */
    public static double calculateThroughput(Supplier<Object> operation, int concurrentUsers, int testDurationSeconds) {
        Assertions.assertNotNull(operation, "Operation cannot be null for throughput calculation");
        Assertions.assertTrue(concurrentUsers > 0, "Concurrent users must be positive");
        Assertions.assertTrue(testDurationSeconds > 0, "Test duration must be positive");
        
        logger.info("Starting throughput test with {} concurrent users for {} seconds", 
                   concurrentUsers, testDurationSeconds);
        
        // Perform JVM warmup for consistent results
        warmupJvm(operation);
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<Future<Long>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (testDurationSeconds * 1000L);
        
        // Submit concurrent operations
        for (int i = 0; i < concurrentUsers; i++) {
            Future<Long> future = executor.submit(() -> {
                long operationCount = 0;
                while (System.currentTimeMillis() < endTime) {
                    try {
                        Object result = operation.get();
                        Assertions.assertNotNull(result, "Operation must return valid result");
                        operationCount++;
                    } catch (Exception e) {
                        logger.warn("Operation failed during throughput test", e);
                    }
                }
                return operationCount;
            });
            futures.add(future);
        }
        
        // Collect results and calculate throughput
        long totalOperations = 0;
        for (Future<Long> future : futures) {
            try {
                totalOperations += future.get();
            } catch (Exception e) {
                logger.error("Failed to collect throughput test results", e);
                Assertions.fail("Throughput test execution failed: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        
        double actualDurationSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        double throughput = totalOperations / actualDurationSeconds;
        
        logger.info("Throughput test completed: {} operations in {:.2f}s = {:.2f} TPS", 
                   totalOperations, actualDurationSeconds, throughput);
        
        return throughput;
    }
    
    /**
     * Analyzes latency distribution and calculates percentiles for performance assessment.
     * Provides detailed statistical analysis of response time characteristics.
     * 
     * @param measurements List of response time measurements in milliseconds
     * @return Map containing percentile analysis (50th, 90th, 95th, 99th percentiles)
     */
    public static Map<String, Double> analyzeLatencyPercentiles(List<Long> measurements) {
        Assertions.assertNotNull(measurements, "Measurements list cannot be null");
        Assertions.assertFalse(measurements.isEmpty(), "Measurements list cannot be empty");
        
        List<Long> sortedMeasurements = new ArrayList<>(measurements);
        Collections.sort(sortedMeasurements);
        
        Map<String, Double> percentiles = new HashMap<>();
        
        // Calculate standard percentiles
        percentiles.put("50th", calculatePercentile(sortedMeasurements, 0.50));
        percentiles.put("90th", calculatePercentile(sortedMeasurements, 0.90));
        percentiles.put("95th", calculatePercentile(sortedMeasurements, 0.95));
        percentiles.put("99th", calculatePercentile(sortedMeasurements, 0.99));
        
        // Calculate basic statistics
        DoubleSummaryStatistics stats = measurements.stream()
            .mapToDouble(Long::doubleValue)
            .summaryStatistics();
        
        percentiles.put("min", stats.getMin());
        percentiles.put("max", stats.getMax());
        percentiles.put("average", stats.getAverage());
        percentiles.put("count", (double) stats.getCount());
        
        logger.info("Latency percentile analysis completed for {} measurements", measurements.size());
        
        return percentiles;
    }
    
    /**
     * Generates a comprehensive performance report including all metrics and analysis.
     * Provides detailed insights into system performance characteristics.
     * 
     * @param testName Name of the performance test
     * @param measurements Performance measurement data
     * @return Formatted performance report string
     */
    public static String generatePerformanceReport(String testName, List<Long> measurements) {
        Assertions.assertNotNull(testName, "Test name cannot be null");
        Assertions.assertNotNull(measurements, "Measurements cannot be null");
        
        if (measurements.isEmpty()) {
            return String.format("Performance Report: %s - No measurements available", testName);
        }
        
        Map<String, Double> percentiles = analyzeLatencyPercentiles(measurements);
        StringBuilder report = new StringBuilder();
        
        report.append(String.format("\n=== Performance Report: %s ===\n", testName));
        report.append(String.format("Test Timestamp: %s\n", Instant.now()));
        report.append(String.format("Total Measurements: %.0f\n", percentiles.get("count")));
        report.append("\nLatency Statistics (ms):\n");
        report.append(String.format("  Minimum: %.2f ms\n", percentiles.get("min")));
        report.append(String.format("  Average: %.2f ms\n", percentiles.get("average")));
        report.append(String.format("  Maximum: %.2f ms\n", percentiles.get("max")));
        
        report.append("\nPercentile Distribution:\n");
        report.append(String.format("  50th percentile: %.2f ms\n", percentiles.get("50th")));
        report.append(String.format("  90th percentile: %.2f ms\n", percentiles.get("90th")));
        report.append(String.format("  95th percentile: %.2f ms\n", percentiles.get("95th")));
        report.append(String.format("  99th percentile: %.2f ms\n", percentiles.get("99th")));
        
        // SLA compliance analysis
        boolean slaCompliant = percentiles.get("95th") < RESPONSE_TIME_THRESHOLD_MS;
        report.append(String.format("\nSLA Compliance Analysis:\n"));
        report.append(String.format("  Target Threshold: %d ms\n", RESPONSE_TIME_THRESHOLD_MS));
        report.append(String.format("  95th Percentile: %.2f ms\n", percentiles.get("95th")));
        report.append(String.format("  SLA Status: %s\n", slaCompliant ? "COMPLIANT" : "NON-COMPLIANT"));
        
        // Performance recommendations
        report.append("\nPerformance Assessment:\n");
        if (percentiles.get("95th") > RESPONSE_TIME_THRESHOLD_MS) {
            report.append("  ⚠️  WARNING: 95th percentile exceeds SLA threshold\n");
            report.append("  📊 Recommendation: Investigate performance bottlenecks\n");
        } else {
            report.append("  ✅ GOOD: Performance meets SLA requirements\n");
        }
        
        if (percentiles.get("max") > (RESPONSE_TIME_THRESHOLD_MS * 2)) {
            report.append("  🔍 Note: Maximum response time indicates potential outliers\n");
        }
        
        report.append("\n" + "=".repeat(50) + "\n");
        
        String reportString = report.toString();
        logger.info("Generated performance report for test: {}", testName);
        
        return reportString;
    }
    
    /**
     * Performs JVM warmup to ensure consistent performance measurements.
     * Eliminates cold-start effects and allows JIT optimization to take effect.
     * 
     * @param operation The operation to warm up
     */
    public static void warmupJvm(Supplier<Object> operation) {
        Assertions.assertNotNull(operation, "Warmup operation cannot be null");
        
        logger.debug("Starting JVM warmup with {} iterations", WARMUP_ITERATIONS);
        
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            try {
                operation.get();
            } catch (Exception e) {
                // Ignore warmup failures but log for debugging
                logger.debug("Warmup iteration {} failed: {}", i, e.getMessage());
            }
        }
        
        // Force garbage collection to stabilize memory
        System.gc();
        
        logger.debug("JVM warmup completed successfully");
    }
    
    /**
     * Tracks performance trends over time for regression detection.
     * Maintains historical performance data for comparison analysis.
     * 
     * @param testName Name of the performance test
     * @param measurement Current performance measurement
     */
    public static void trackPerformanceTrend(String testName, double measurement) {
        Assertions.assertNotNull(testName, "Test name cannot be null for trend tracking");
        Assertions.assertTrue(measurement >= 0, "Performance measurement must be non-negative");
        
        performanceTrends.computeIfAbsent(testName, k -> new ArrayList<>()).add(measurement);
        
        logger.debug("Tracked performance trend for {}: {}", testName, measurement);
    }
    
    /**
     * Validates system compliance with defined SLA thresholds.
     * Provides comprehensive SLA validation across multiple performance dimensions.
     * 
     * @param responseTime Current response time measurement
     * @param throughput Current throughput measurement
     * @return true if all SLA requirements are met
     */
    public static boolean validateSlaCompliance(long responseTime, double throughput) {
        boolean responseTimeCompliant = responseTime < RESPONSE_TIME_THRESHOLD_MS;
        boolean throughputCompliant = throughput >= TARGET_TPS;
        
        logger.info("SLA Compliance Validation:");
        logger.info("  Response Time: {}ms (Target: <{}ms) - {}", 
                   responseTime, RESPONSE_TIME_THRESHOLD_MS, 
                   responseTimeCompliant ? "PASS" : "FAIL");
        logger.info("  Throughput: {:.2f} TPS (Target: >={} TPS) - {}", 
                   throughput, TARGET_TPS, 
                   throughputCompliant ? "PASS" : "FAIL");
        
        boolean overallCompliant = responseTimeCompliant && throughputCompliant;
        
        Assertions.assertTrue(overallCompliant, 
            String.format("SLA compliance failed - Response: %dms, Throughput: %.2f TPS", 
                         responseTime, throughput));
        
        return overallCompliant;
    }
    
    /**
     * Measures concurrent execution performance under load conditions.
     * Tests system behavior with multiple simultaneous operations.
     * 
     * @param operation Operation to execute concurrently
     * @param concurrencyLevel Number of concurrent executions
     * @return Map containing concurrent execution statistics
     */
    public static Map<String, Object> measureConcurrentExecution(Supplier<Object> operation, int concurrencyLevel) {
        Assertions.assertNotNull(operation, "Concurrent operation cannot be null");
        Assertions.assertTrue(concurrencyLevel > 0, "Concurrency level must be positive");
        
        logger.info("Starting concurrent execution test with {} parallel operations", concurrencyLevel);
        
        // Prepare concurrent execution
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        long startTime = System.nanoTime();
        
        // Submit concurrent operations
        for (int i = 0; i < concurrencyLevel; i++) {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long operationStart = System.nanoTime();
                try {
                    Object result = operation.get();
                    Assertions.assertNotNull(result, "Concurrent operation must return valid result");
                } catch (Exception e) {
                    logger.error("Concurrent operation failed", e);
                    Assertions.fail("Concurrent execution failed: " + e.getMessage());
                }
                return System.nanoTime() - operationStart;
            });
            futures.add(future);
        }
        
        // Wait for all operations to complete and collect results
        List<Long> executionTimes = new ArrayList<>();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        for (CompletableFuture<Long> future : futures) {
            try {
                executionTimes.add(TimeUnit.NANOSECONDS.toMillis(future.get()));
            } catch (Exception e) {
                logger.error("Failed to collect concurrent execution result", e);
            }
        }
        
        long totalTime = System.nanoTime() - startTime;
        double totalTimeMs = TimeUnit.NANOSECONDS.toMillis(totalTime);
        
        // Calculate statistics
        Map<String, Object> results = new HashMap<>();
        results.put("concurrencyLevel", concurrencyLevel);
        results.put("totalExecutionTime", totalTimeMs);
        results.put("individualExecutionTimes", executionTimes);
        results.put("averageExecutionTime", executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
        results.put("maxExecutionTime", executionTimes.stream().mapToLong(Long::longValue).max().orElse(0L));
        results.put("minExecutionTime", executionTimes.stream().mapToLong(Long::longValue).min().orElse(0L));
        results.put("effectiveThroughput", (concurrencyLevel * 1000.0) / totalTimeMs);
        
        logger.info("Concurrent execution test completed: {} operations in {:.2f}ms", 
                   concurrencyLevel, totalTimeMs);
        
        return results;
    }
    
    /**
     * Calculates comprehensive performance statistics from measurement data.
     * Provides detailed mathematical analysis of performance characteristics.
     * 
     * @param measurements List of performance measurements
     * @return Map containing calculated statistics
     */
    public static Map<String, Double> calculateStatistics(List<Long> measurements) {
        Assertions.assertNotNull(measurements, "Measurements list cannot be null");
        Assertions.assertFalse(measurements.isEmpty(), "Measurements list cannot be empty");
        
        DoubleSummaryStatistics stats = measurements.stream()
            .mapToDouble(Long::doubleValue)
            .summaryStatistics();
        
        Map<String, Double> statistics = new HashMap<>();
        
        // Basic statistics
        statistics.put("count", (double) stats.getCount());
        statistics.put("sum", stats.getSum());
        statistics.put("min", stats.getMin());
        statistics.put("max", stats.getMax());
        statistics.put("average", stats.getAverage());
        
        // Calculate additional statistics
        double mean = stats.getAverage();
        double variance = measurements.stream()
            .mapToDouble(Long::doubleValue)
            .map(x -> Math.pow(x - mean, 2))
            .average()
            .orElse(0.0);
        
        statistics.put("variance", variance);
        statistics.put("standardDeviation", Math.sqrt(variance));
        
        // Calculate coefficient of variation (relative standard deviation)
        statistics.put("coefficientOfVariation", (Math.sqrt(variance) / mean) * 100);
        
        logger.debug("Calculated statistics for {} measurements", measurements.size());
        
        return statistics;
    }
    
    /**
     * Detects performance regression by comparing current measurements with historical data.
     * Provides automated regression detection based on statistical analysis.
     * 
     * @param testName Name of the test for historical comparison
     * @param currentMeasurements Current performance measurements
     * @param regressionThresholdPercent Acceptable performance degradation percentage
     * @return true if performance regression is detected
     */
    public static boolean detectPerformanceRegression(String testName, List<Long> currentMeasurements, double regressionThresholdPercent) {
        Assertions.assertNotNull(testName, "Test name cannot be null");
        Assertions.assertNotNull(currentMeasurements, "Current measurements cannot be null");
        Assertions.assertTrue(regressionThresholdPercent > 0, "Regression threshold must be positive");
        
        List<Double> historicalData = performanceTrends.get(testName);
        if (historicalData == null || historicalData.isEmpty()) {
            logger.info("No historical data available for regression detection: {}", testName);
            // Store current measurements as baseline
            trackPerformanceTrend(testName, currentMeasurements.stream().mapToLong(Long::longValue).average().orElse(0.0));
            return false;
        }
        
        // Calculate current and historical averages
        double currentAverage = currentMeasurements.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double historicalAverage = historicalData.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Calculate performance change percentage
        double performanceChange = ((currentAverage - historicalAverage) / historicalAverage) * 100;
        boolean isRegression = performanceChange > regressionThresholdPercent;
        
        logger.info("Performance regression analysis for {}:", testName);
        logger.info("  Historical average: {:.2f}ms", historicalAverage);
        logger.info("  Current average: {:.2f}ms", currentAverage);
        logger.info("  Performance change: {:.2f}%", performanceChange);
        logger.info("  Regression threshold: {:.2f}%", regressionThresholdPercent);
        logger.info("  Regression detected: {}", isRegression);
        
        // Update historical data with current measurements
        trackPerformanceTrend(testName, currentAverage);
        
        if (isRegression) {
            logger.error("Performance regression detected for {}: {:.2f}% degradation", testName, performanceChange);
        }
        
        return isRegression;
    }
    
    /**
     * Logs performance metrics to the monitoring system.
     * Integrates with application monitoring for continuous performance tracking.
     * 
     * @param metricName Name of the performance metric
     * @param value Metric value
     * @param timestamp Timestamp of the measurement
     */
    public static void logPerformanceMetrics(String metricName, double value, Instant timestamp) {
        Assertions.assertNotNull(metricName, "Metric name cannot be null");
        Assertions.assertNotNull(timestamp, "Timestamp cannot be null");
        
        // Log metric with structured format for monitoring systems
        logger.info("PERFORMANCE_METRIC | name={} | value={} | timestamp={} | threshold={}", 
                   metricName, value, timestamp, RESPONSE_TIME_THRESHOLD_MS);
        
        // Additional logging for specific metric types
        if ("ExecutionTime".equals(metricName) && value > RESPONSE_TIME_THRESHOLD_MS) {
            logger.warn("Performance threshold exceeded for {}: {}ms > {}ms", 
                       metricName, value, RESPONSE_TIME_THRESHOLD_MS);
        }
    }
    
    // Private helper methods
    
    /**
     * Calculates a specific percentile value from sorted measurements.
     * 
     * @param sortedMeasurements List of measurements sorted in ascending order
     * @param percentile Percentile to calculate (0.0 to 1.0)
     * @return Calculated percentile value
     */
    private static double calculatePercentile(List<Long> sortedMeasurements, double percentile) {
        if (sortedMeasurements.isEmpty()) {
            return 0.0;
        }
        
        int index = (int) Math.ceil(percentile * sortedMeasurements.size()) - 1;
        index = Math.max(0, Math.min(index, sortedMeasurements.size() - 1));
        
        return sortedMeasurements.get(index).doubleValue();
    }
    
    /**
     * Validates BigDecimal precision matches COBOL COMP-3 requirements during performance testing.
     * Ensures financial calculations maintain identical precision to mainframe implementation.
     * 
     * @param actual Actual BigDecimal result from performance test
     * @param expected Expected BigDecimal value for comparison
     * @return true if precision matches COBOL requirements
     */
    public static boolean validateCobolDecimalPrecision(BigDecimal actual, BigDecimal expected) {
        Assertions.assertNotNull(actual, "Actual BigDecimal cannot be null");
        Assertions.assertNotNull(expected, "Expected BigDecimal cannot be null");

        // Validate scale matches COBOL COMP-3 requirements
        if (actual.scale() != expected.scale()) {
            logger.warn("Scale mismatch - Actual: {}, Expected: {}", actual.scale(), expected.scale());
            return false;
        }

        // Validate precision matches using COBOL comparison logic
        int actualPrecision = actual.precision();
        int expectedPrecision = expected.precision();
        
        if (actualPrecision != expectedPrecision) {
            logger.warn("Precision mismatch - Actual: {}, Expected: {}", actualPrecision, expectedPrecision);
            return false;
        }

        // Validate values match exactly using COBOL COMP-3 comparison
        int comparison = actual.compareTo(expected);
        boolean matches = (comparison == 0);
        
        if (!matches) {
            logger.warn("Value mismatch - Actual: {}, Expected: {}", actual, expected);
        } else {
            logger.debug("COBOL precision validation passed - Value: {}, Scale: {}, Precision: {}", 
                        actual, actual.scale(), actual.precision());
        }

        return matches;
    }
    
    /**
     * Compares BigDecimal values using COBOL rounding behavior for performance test validation.
     * Applies HALF_UP rounding mode matching COBOL ROUNDED clause behavior.
     * 
     * @param value1 First BigDecimal value to compare
     * @param value2 Second BigDecimal value to compare
     * @param scale Target scale for rounding comparison
     * @return true if values match after COBOL-equivalent rounding
     */
    public static boolean compareDecimalWithCobolRounding(BigDecimal value1, BigDecimal value2, int scale) {
        Assertions.assertNotNull(value1, "First BigDecimal value cannot be null");
        Assertions.assertNotNull(value2, "Second BigDecimal value cannot be null");
        Assertions.assertTrue(scale >= 0, "Scale must be non-negative");

        // Apply COBOL ROUNDED equivalent rounding (HALF_UP)
        BigDecimal rounded1 = value1.setScale(scale, TestConstants.COBOL_ROUNDING_MODE);
        BigDecimal rounded2 = value2.setScale(scale, TestConstants.COBOL_ROUNDING_MODE);

        // Compare rounded values
        boolean matches = rounded1.compareTo(rounded2) == 0;
        
        if (!matches) {
            logger.warn("COBOL rounding comparison failed - Value1: {} -> {}, Value2: {} -> {}", 
                       value1, rounded1, value2, rounded2);
        } else {
            logger.debug("COBOL rounding comparison passed - Rounded value: {} at scale {}", 
                        rounded1, scale);
        }

        return matches;
    }
}