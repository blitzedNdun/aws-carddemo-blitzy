package com.carddemo.config;

import com.carddemo.service.MonitoringService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.HashMap;

import static org.mockito.Mockito.*;

/**
 * Test configuration providing mock MonitoringService for test environment.
 * This replaces the production MonitoringService which is excluded from tests
 * via @Profile("!test") annotation.
 *
 * Provides a mock implementation that returns safe default values for all
 * monitoring operations without requiring Micrometer/Prometheus infrastructure.
 */
@TestConfiguration
@Profile("test")
public class TestMonitoringConfig {

    /**
     * Provides a mock MonitoringService for testing environment.
     * Returns mock implementation that prevents Spring context loading failures
     * when JwtRequestFilter requires MonitoringService dependency.
     */
    @Bean
    @Primary
    public MonitoringService mockMonitoringService() {
        MonitoringService mockService = mock(MonitoringService.class);
        
        // Setup default mock behavior for common monitoring operations
        when(mockService.isHealthy()).thenReturn(true);
        when(mockService.validateSLACompliance()).thenReturn(true);
        when(mockService.getMetricByName(anyString())).thenReturn(0.0);
        
        // Setup mock response time metrics
        Map<String, Double> responseTimeMetrics = new HashMap<>();
        responseTimeMetrics.put("mean", 50.0);
        responseTimeMetrics.put("max", 100.0);
        responseTimeMetrics.put("count", 0.0);
        when(mockService.getResponseTimeMetrics()).thenReturn(responseTimeMetrics);
        
        // Setup mock throughput metrics
        Map<String, Double> throughputMetrics = new HashMap<>();
        throughputMetrics.put("total_transactions", 0.0);
        throughputMetrics.put("successful_transactions", 0.0);
        throughputMetrics.put("failed_transactions", 0.0);
        throughputMetrics.put("success_rate", 1.0);
        throughputMetrics.put("error_rate", 0.0);
        when(mockService.getThroughputMetrics()).thenReturn(throughputMetrics);
        
        // Setup mock alert evaluation
        Map<String, Boolean> alertStatus = new HashMap<>();
        alertStatus.put("high_response_time", false);
        alertStatus.put("high_error_rate", false);
        alertStatus.put("sla_violation", false);
        alertStatus.put("system_unhealthy", false);
        when(mockService.evaluateAlertThresholds()).thenReturn(alertStatus);
        
        // Setup mock metric cardinality
        Map<String, Integer> cardinality = new HashMap<>();
        cardinality.put("total_meters", 0);
        cardinality.put("custom_counters", 0);
        cardinality.put("custom_timers", 0);
        cardinality.put("custom_gauges", 0);
        cardinality.put("health_indicators", 0);
        when(mockService.getMetricCardinality()).thenReturn(cardinality);
        
        // Setup mock Prometheus export
        when(mockService.exportPrometheusMetrics()).thenReturn("# Test metrics export\n");
        
        return mockService;
    }
}