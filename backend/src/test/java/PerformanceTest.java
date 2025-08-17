package com.carddemo.test;

import org.junit.jupiter.api.Tag;

/**
 * JUnit 5 test category marker interface for performance tests.
 * 
 * <p>Used with @Tag annotation to categorize load tests, stress tests, and performance
 * validation tests that verify response time SLAs and throughput requirements.</p>
 * 
 * <p>Performance tests should:</p>
 * <ul>
 *   <li>Validate sub-200ms response times for transactions</li>
 *   <li>Test system throughput up to 10,000 TPS</li>
 *   <li>Simulate realistic user load patterns</li>
 *   <li>Measure resource utilization and memory usage</li>
 *   <li>Validate batch processing completion within 4-hour window</li>
 *   <li>Test concurrent user scenarios</li>
 *   <li>Verify performance regression detection</li>
 * </ul>
 * 
 * @since 1.0.0
 */
@Tag("performance")
public interface PerformanceTest {
    // Marker interface - no methods needed
}