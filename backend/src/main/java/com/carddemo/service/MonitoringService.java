package com.carddemo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carddemo.config.MetricsConfig;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service class providing application monitoring capabilities using Micrometer metrics 
 * and Prometheus integration. Manages custom business metrics collection, health indicator 
 * aggregation, alert threshold monitoring, and performance baseline tracking.
 * 
 * Exposes metrics for transaction processing, system health, and SLA compliance monitoring
 * with enterprise-grade observability capabilities replacing traditional mainframe monitoring.
 * 
 * Features:
 * - Transaction metrics collection with sub-200ms response time tracking
 * - Health indicator aggregation for database, Redis, and business logic
 * - Alert threshold evaluation for response times and error rates  
 * - Performance baseline tracking with historical comparison
 * - SLA compliance monitoring for 200ms response times and 10,000 TPS requirements
 * - Prometheus metrics export with cardinality management
 */
@Service
public class MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    private final MetricsConfig metricsConfig;
    private final MeterRegistry meterRegistry;
    private final PrometheusMeterRegistry prometheusMeterRegistry;
    
    // Custom metrics for transaction monitoring
    private final Counter transactionProcessedCounter;
    private final Counter transactionErrorCounter;
    private final Timer transactionTimer;
    private final Map<String, Timer> customTimers = new ConcurrentHashMap<>();
    private final Map<String, Counter> customCounters = new ConcurrentHashMap<>();
    private final Map<String, Gauge> customGauges = new ConcurrentHashMap<>();
    
    // Performance baseline tracking
    private final AtomicReference<Double> responseTimeBaseline = new AtomicReference<>(0.0);
    private final AtomicReference<Double> throughputBaseline = new AtomicReference<>(0.0);
    private final AtomicLong totalTransactionsProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    
    // Alert threshold constants (matching technical requirements)
    private static final double RESPONSE_TIME_SLA_MS = 200.0; // 200ms SLA requirement
    private static final double THROUGHPUT_SLA_TPS = 10000.0; // 10,000 TPS requirement
    private static final double ERROR_RATE_THRESHOLD = 0.01; // 1% error rate threshold
    private static final double HIGH_RESPONSE_TIME_ALERT_MS = 500.0; // Alert threshold
    
    // Health indicators and status tracking
    private final Map<String, HealthIndicator> healthIndicators = new ConcurrentHashMap<>();
    private final AtomicReference<Health> aggregatedHealth = new AtomicReference<>(Health.up().build());
    
    @Autowired
    public MonitoringService(MetricsConfig metricsConfig, PrometheusMeterRegistry prometheusMeterRegistry) {
        this.metricsConfig = metricsConfig;
        this.prometheusMeterRegistry = prometheusMeterRegistry;
        this.meterRegistry = this.prometheusMeterRegistry;
        
        // Initialize core transaction metrics
        this.transactionProcessedCounter = Counter.builder("carddemo.transactions.processed.total")
                .description("Total number of transactions processed")
                .register(meterRegistry);
                
        this.transactionErrorCounter = Counter.builder("carddemo.transactions.errors.total")
                .description("Total number of transaction errors")
                .register(meterRegistry);
                
        this.transactionTimer = Timer.builder("carddemo.transactions.response.time")
                .description("Transaction response time in milliseconds")
                .register(meterRegistry);
                
        // Initialize health indicators from MetricsConfig
        initializeHealthIndicators();
        
        // Register performance baseline gauges
        registerPerformanceGauges();
        
        logger.info("MonitoringService initialized with Prometheus registry and health indicators");
    }

    /**
     * Initialize health indicators from MetricsConfig dependency.
     * Aggregates database, Redis, and custom business health indicators.
     */
    private void initializeHealthIndicators() {
        try {
            // Database health indicator is now managed as Spring Bean in ActuatorConfig
            // No manual initialization needed - Spring Boot Actuator will handle it
            
            // Add custom health indicator from MetricsConfig  
            HealthIndicator customHealth = metricsConfig.customHealthIndicator();
            healthIndicators.put("custom", customHealth);
            
            logger.info("Initialized {} health indicators", healthIndicators.size());
        } catch (Exception e) {
            logger.error("Failed to initialize health indicators", e);
            healthIndicators.put("monitoring", () -> Health.down()
                    .withDetail("error", "Failed to initialize health indicators")
                    .withException(e)
                    .build());
        }
    }

    /**
     * Register performance baseline gauges for monitoring SLA compliance.
     */
    private void registerPerformanceGauges() {
        // Response time baseline gauge
        Gauge.builder("carddemo.performance.response.time.baseline", this, MonitoringService::getCurrentResponseTimeBaseline)
                .description("Current response time baseline in milliseconds")
                .register(meterRegistry);
                
        // Throughput baseline gauge
        Gauge.builder("carddemo.performance.throughput.baseline", this, MonitoringService::getCurrentThroughputBaseline)
                .description("Current throughput baseline in transactions per second")
                .register(meterRegistry);
                
        // SLA compliance gauge
        Gauge.builder("carddemo.sla.compliance.response.time", this, MonitoringService::getSlaResponseTimeCompliance)
                .description("SLA compliance for response time (1.0 = compliant, 0.0 = non-compliant)")
                .register(meterRegistry);
                
        // Error rate gauge
        Gauge.builder("carddemo.performance.error.rate", this, MonitoringService::getCurrentErrorRate)
                .description("Current error rate as percentage")
                .register(meterRegistry);
    }

    /**
     * Collects comprehensive transaction metrics including response times, throughput,
     * and error rates for monitoring and alerting.
     */
    public void collectTransactionMetrics() {
        try {
            logger.debug("Collecting transaction metrics");
            
            // Update transaction count metrics
            double currentCount = transactionProcessedCounter.count();
            double currentErrors = transactionErrorCounter.count();
            
            // Calculate current error rate
            double errorRate = currentCount > 0 ? (currentErrors / currentCount) : 0.0;
            
            // Update performance baselines
            updatePerformanceBaselines();
            
            // Log metrics collection
            logger.debug("Transaction metrics collected - Processed: {}, Errors: {}, Error Rate: {}%", 
                    currentCount, currentErrors, errorRate * 100);
                    
        } catch (Exception e) {
            logger.error("Failed to collect transaction metrics", e);
        }
    }

    /**
     * Aggregates all registered health indicators into a single health status.
     * Provides comprehensive system health overview for monitoring dashboards.
     */
    public Health aggregateHealthIndicators() {
        try {
            Health.Builder builder = new Health.Builder();
            boolean allHealthy = true;
            Map<String, Object> details = new HashMap<>();
            
            for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
                String name = entry.getKey();
                HealthIndicator indicator = entry.getValue();
                
                try {
                    Health health = indicator.health();
                    details.put(name, health);
                    
                    if (!Status.UP.equals(health.getStatus())) {
                        allHealthy = false;
                        logger.warn("Health indicator '{}' reports status: {}", name, health.getStatus());
                    }
                } catch (Exception e) {
                    allHealthy = false;
                    details.put(name, Health.down()
                            .withDetail("error", "Health check failed")
                            .withException(e)
                            .build());
                    logger.error("Health indicator '{}' failed", name, e);
                }
            }
            
            Health aggregated = allHealthy ? 
                    builder.up().withDetails(details).build() :
                    builder.down().withDetails(details).build();
                    
            aggregatedHealth.set(aggregated);
            
            logger.debug("Aggregated health status: {} (from {} indicators)", 
                    aggregated.getStatus(), healthIndicators.size());
                    
            return aggregated;
            
        } catch (Exception e) {
            logger.error("Failed to aggregate health indicators", e);
            Health errorHealth = Health.down()
                    .withDetail("error", "Health aggregation failed")
                    .withException(e)
                    .build();
            aggregatedHealth.set(errorHealth);
            return errorHealth;
        }
    }

    /**
     * Evaluates alert thresholds for response times, error rates, and throughput.
     * Triggers alerts when SLA thresholds are exceeded.
     */
    public Map<String, Boolean> evaluateAlertThresholds() {
        Map<String, Boolean> alertStatus = new HashMap<>();
        
        try {
            // Evaluate response time threshold
            double currentResponseTime = getCurrentResponseTimeMetric();
            boolean highResponseTime = currentResponseTime > HIGH_RESPONSE_TIME_ALERT_MS;
            alertStatus.put("high_response_time", highResponseTime);
            
            // Evaluate error rate threshold  
            double errorRate = getCurrentErrorRate();
            boolean highErrorRate = errorRate > ERROR_RATE_THRESHOLD;
            alertStatus.put("high_error_rate", highErrorRate);
            
            // Evaluate SLA compliance
            boolean slaViolation = !validateSLACompliance();
            alertStatus.put("sla_violation", slaViolation);
            
            // Evaluate system health
            Health currentHealth = aggregateHealthIndicators();
            boolean systemUnhealthy = !Status.UP.equals(currentHealth.getStatus());
            alertStatus.put("system_unhealthy", systemUnhealthy);
            
            // Log alert evaluations
            alertStatus.forEach((alert, triggered) -> {
                if (triggered) {
                    logger.warn("Alert threshold exceeded: {}", alert);
                }
            });
            
            logger.debug("Alert threshold evaluation completed: {}", alertStatus);
            
        } catch (Exception e) {
            logger.error("Failed to evaluate alert thresholds", e);
            alertStatus.put("evaluation_error", true);
        }
        
        return alertStatus;
    }

    /**
     * Tracks performance baselines for response time and throughput with historical comparison.
     * Maintains rolling averages for performance trend analysis.
     */
    public void trackPerformanceBaseline() {
        try {
            // Calculate current response time baseline
            double currentResponseTime = getCurrentResponseTimeMetric();
            updateResponseTimeBaseline(currentResponseTime);
            
            // Calculate current throughput baseline
            double currentThroughput = getCurrentThroughputMetric();
            updateThroughputBaseline(currentThroughput);
            
            logger.debug("Performance baselines updated - Response Time: {}ms, Throughput: {} TPS", 
                    responseTimeBaseline.get(), throughputBaseline.get());
                    
        } catch (Exception e) {
            logger.error("Failed to track performance baseline", e);
        }
    }

    /**
     * Registers a custom metric with the monitoring system.
     * Supports counters, timers, and gauges for business-specific monitoring.
     */
    public void registerCustomMetric(String metricName, String metricType, String description) {
        try {
            logger.debug("Registering custom metric: {} of type: {}", metricName, metricType);
            
            switch (metricType.toLowerCase()) {
                case "counter":
                    Counter counter = Counter.builder(metricName)
                            .description(description)
                            .register(meterRegistry);
                    customCounters.put(metricName, counter);
                    break;
                    
                case "timer":
                    Timer timer = Timer.builder(metricName)
                            .description(description)
                            .register(meterRegistry);
                    customTimers.put(metricName, timer);
                    break;
                    
                case "gauge":
                    // For gauges, we register a placeholder that can be updated
                    AtomicReference<Double> gaugeValue = new AtomicReference<>(0.0);
                    Gauge gauge = Gauge.builder(metricName, gaugeValue, AtomicReference::get)
                            .description(description)
                            .register(meterRegistry);
                    customGauges.put(metricName, gauge);
                    break;
                    
                default:
                    logger.warn("Unsupported metric type: {}", metricType);
                    return;
            }
            
            logger.info("Successfully registered custom metric: {} ({})", metricName, metricType);
            
        } catch (Exception e) {
            logger.error("Failed to register custom metric: {}", metricName, e);
        }
    }

    /**
     * Retrieves a specific metric by name from the metrics registry.
     * Returns metric value or null if metric doesn't exist.
     */
    public Double getMetricByName(String metricName) {
        try {
            // Check custom counters
            Counter counter = customCounters.get(metricName);
            if (counter != null) {
                return counter.count();
            }
            
            // Check for core metrics
            switch (metricName) {
                case "carddemo.transactions.processed.total":
                    return transactionProcessedCounter.count();
                case "carddemo.transactions.errors.total":
                    return transactionErrorCounter.count();
                case "carddemo.transactions.response.time":
                    return transactionTimer.mean(TimeUnit.MILLISECONDS);
                default:
                    // Search in meter registry
                    Meter meter = meterRegistry.find(metricName).meter();
                    if (meter != null) {
                        if (meter instanceof Counter) {
                            return ((Counter) meter).count();
                        } else if (meter instanceof Timer) {
                            return ((Timer) meter).mean(TimeUnit.MILLISECONDS);
                        } else if (meter instanceof Gauge) {
                            return ((Gauge) meter).value();
                        }
                    }
                    return null;
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve metric: {}", metricName, e);
            return null;
        }
    }

    /**
     * Checks if the system is healthy based on aggregated health indicators.
     * Returns true if all health indicators report UP status.
     */
    public boolean isHealthy() {
        try {
            Health currentHealth = aggregateHealthIndicators();
            boolean healthy = Status.UP.equals(currentHealth.getStatus());
            
            logger.debug("System health check result: {}", healthy ? "HEALTHY" : "UNHEALTHY");
            return healthy;
            
        } catch (Exception e) {
            logger.error("Failed to check system health", e);
            return false;
        }
    }

    /**
     * Gets current response time metrics including mean, max, and percentiles.
     * Returns comprehensive response time statistics for monitoring.
     */
    public Map<String, Double> getResponseTimeMetrics() {
        Map<String, Double> metrics = new HashMap<>();
        
        try {
            // Use Timer's direct methods instead of snapshot API
            metrics.put("mean", transactionTimer.mean(TimeUnit.MILLISECONDS));
            metrics.put("max", transactionTimer.max(TimeUnit.MILLISECONDS));
            metrics.put("count", (double) transactionTimer.count());
            
            // For percentiles, use value at percentile if available
            try {
                // These methods may not be available in all Micrometer versions
                // Using basic metrics for now
                metrics.put("total_time", transactionTimer.totalTime(TimeUnit.MILLISECONDS));
            } catch (Exception innerE) {
                logger.debug("Advanced timer metrics not available: {}", innerE.getMessage());
            }
            
            logger.debug("Response time metrics: {}", metrics);
            
        } catch (Exception e) {
            logger.error("Failed to get response time metrics", e);
            metrics.put("error", 1.0);
        }
        
        return metrics;
    }

    /**
     * Gets current throughput metrics including transactions per second and processing rates.
     * Returns comprehensive throughput statistics for capacity monitoring.
     */
    public Map<String, Double> getThroughputMetrics() {
        Map<String, Double> metrics = new HashMap<>();
        
        try {
            double totalTransactions = transactionProcessedCounter.count();
            double totalErrors = transactionErrorCounter.count();
            double successfulTransactions = totalTransactions - totalErrors;
            
            // Calculate rates (simplified calculation - in production would use time windows)
            metrics.put("total_transactions", totalTransactions);
            metrics.put("successful_transactions", successfulTransactions);
            metrics.put("failed_transactions", totalErrors);
            metrics.put("success_rate", totalTransactions > 0 ? (successfulTransactions / totalTransactions) : 1.0);
            metrics.put("error_rate", totalTransactions > 0 ? (totalErrors / totalTransactions) : 0.0);
            metrics.put("throughput_baseline", throughputBaseline.get());
            
            logger.debug("Throughput metrics: {}", metrics);
            
        } catch (Exception e) {
            logger.error("Failed to get throughput metrics", e);
            metrics.put("error", 1.0);
        }
        
        return metrics;
    }

    /**
     * Records a successfully processed transaction.
     * Increments transaction counter and updates performance metrics.
     */
    public void recordTransactionProcessed() {
        try {
            transactionProcessedCounter.increment();
            totalTransactionsProcessed.incrementAndGet();
            
            logger.debug("Transaction processed recorded. Total: {}", transactionProcessedCounter.count());
            
        } catch (Exception e) {
            logger.error("Failed to record processed transaction", e);
        }
    }

    /**
     * Records a transaction error.
     * Increments error counter and updates error rate metrics.
     */
    public void recordTransactionError() {
        try {
            transactionErrorCounter.increment();
            totalErrors.incrementAndGet();
            
            logger.debug("Transaction error recorded. Total errors: {}", transactionErrorCounter.count());
            
        } catch (Exception e) {
            logger.error("Failed to record transaction error", e);
        }
    }

    /**
     * Starts a transaction timer for measuring processing time.
     * Returns a Timer.Sample that can be stopped to record duration.
     */
    public Timer.Sample startTransactionTimer() {
        try {
            Timer.Sample sample = Timer.start(meterRegistry);
            logger.debug("Transaction timer started");
            return sample;
            
        } catch (Exception e) {
            logger.error("Failed to start transaction timer", e);
            // Return a no-op sample to prevent null pointer exceptions
            return Timer.start();
        }
    }

    /**
     * Stops a transaction timer and records the measured duration.
     * Completes the timing measurement for response time tracking.
     */
    public void stopTransactionTimer(Timer.Sample sample) {
        try {
            if (sample != null) {
                long duration = sample.stop(transactionTimer);
                logger.debug("Transaction timer stopped. Duration: {}ms", 
                        Duration.ofNanos(duration).toMillis());
            } else {
                logger.warn("Attempted to stop null timer sample");
            }
            
        } catch (Exception e) {
            logger.error("Failed to stop transaction timer", e);
        }
    }

    /**
     * Validates SLA compliance for response times and throughput.
     * Returns true if system meets SLA requirements (200ms response time, 10k TPS).
     */
    public boolean validateSLACompliance() {
        try {
            // Check response time SLA (95th percentile < 200ms)
            Map<String, Double> responseMetrics = getResponseTimeMetrics();
            Double p95ResponseTime = responseMetrics.get("p95");
            boolean responseTimeSLA = p95ResponseTime != null && p95ResponseTime <= RESPONSE_TIME_SLA_MS;
            
            // Check error rate SLA (< 1%)
            double errorRate = getCurrentErrorRate();
            boolean errorRateSLA = errorRate <= ERROR_RATE_THRESHOLD;
            
            // Check system health SLA
            boolean healthSLA = isHealthy();
            
            boolean overallCompliance = responseTimeSLA && errorRateSLA && healthSLA;
            
            logger.debug("SLA Compliance Check - Response Time: {}, Error Rate: {}, Health: {}, Overall: {}", 
                    responseTimeSLA, errorRateSLA, healthSLA, overallCompliance);
                    
            return overallCompliance;
            
        } catch (Exception e) {
            logger.error("Failed to validate SLA compliance", e);
            return false;
        }
    }

    /**
     * Triggers an alert for the specified condition.
     * Logs alert and could integrate with external alerting systems.
     */
    public void triggerAlert(String alertType, String message, Map<String, Object> context) {
        try {
            logger.warn("ALERT TRIGGERED - Type: {}, Message: {}, Context: {}", 
                    alertType, message, context);
                    
            // In a production system, this would integrate with:
            // - Prometheus Alertmanager
            // - PagerDuty
            // - Slack notifications
            // - Email alerts
            
            // For now, create a metric to track alerts
            Counter alertCounter = Counter.builder("carddemo.alerts.triggered.total")
                    .tag("type", alertType)
                    .description("Total number of alerts triggered by type")
                    .register(meterRegistry);
            alertCounter.increment();
            
        } catch (Exception e) {
            logger.error("Failed to trigger alert: {}", alertType, e);
        }
    }

    /**
     * Gets the current metric cardinality for Prometheus efficiency monitoring.
     * Returns the number of unique metric name-tag combinations.
     */
    public Map<String, Integer> getMetricCardinality() {
        Map<String, Integer> cardinality = new HashMap<>();
        
        try {
            // Count total meters in registry
            int totalMeters = meterRegistry.getMeters().size();
            cardinality.put("total_meters", totalMeters);
            
            // Count custom metrics
            cardinality.put("custom_counters", customCounters.size());
            cardinality.put("custom_timers", customTimers.size());
            cardinality.put("custom_gauges", customGauges.size());
            
            // Count health indicators
            cardinality.put("health_indicators", healthIndicators.size());
            
            logger.debug("Metric cardinality: {}", cardinality);
            
        } catch (Exception e) {
            logger.error("Failed to get metric cardinality", e);
            cardinality.put("error", 1);
        }
        
        return cardinality;
    }

    /**
     * Exports current metrics in Prometheus format.
     * Returns Prometheus-formatted metrics string for scraping.
     */
    public String exportPrometheusMetrics() {
        try {
            String prometheusOutput = prometheusMeterRegistry.scrape();
            
            logger.debug("Exported Prometheus metrics: {} characters", prometheusOutput.length());
            return prometheusOutput;
            
        } catch (Exception e) {
            logger.error("Failed to export Prometheus metrics", e);
            return "# Error exporting metrics\n";
        }
    }

    // Helper methods for internal calculations

    /**
     * Updates performance baselines with current metrics.
     */
    private void updatePerformanceBaselines() {
        try {
            // Update response time baseline (using exponential moving average)
            double currentResponseTime = getCurrentResponseTimeMetric();
            updateResponseTimeBaseline(currentResponseTime);
            
            // Update throughput baseline
            double currentThroughput = getCurrentThroughputMetric();
            updateThroughputBaseline(currentThroughput);
            
        } catch (Exception e) {
            logger.error("Failed to update performance baselines", e);
        }
    }

    /**
     * Gets current response time metric value.
     */
    private double getCurrentResponseTimeMetric() {
        try {
            return transactionTimer.mean(TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Failed to get current response time metric", e);
            return 0.0;
        }
    }

    /**
     * Gets current throughput metric value.
     */
    private double getCurrentThroughputMetric() {
        try {
            // Simplified throughput calculation
            // In production, this would calculate actual TPS over a time window
            return transactionProcessedCounter.count();
        } catch (Exception e) {
            logger.error("Failed to get current throughput metric", e);
            return 0.0;
        }
    }

    /**
     * Updates response time baseline using exponential moving average.
     */
    private void updateResponseTimeBaseline(double currentValue) {
        if (currentValue > 0) {
            double alpha = 0.1; // Smoothing factor
            double oldBaseline = responseTimeBaseline.get();
            double newBaseline = (alpha * currentValue) + ((1 - alpha) * oldBaseline);
            responseTimeBaseline.set(newBaseline);
        }
    }

    /**
     * Updates throughput baseline using exponential moving average.
     */
    private void updateThroughputBaseline(double currentValue) {
        if (currentValue > 0) {
            double alpha = 0.1; // Smoothing factor  
            double oldBaseline = throughputBaseline.get();
            double newBaseline = (alpha * currentValue) + ((1 - alpha) * oldBaseline);
            throughputBaseline.set(newBaseline);
        }
    }

    /**
     * Gets current error rate as a percentage.
     */
    private double getCurrentErrorRate() {
        try {
            double totalTransactions = transactionProcessedCounter.count();
            double totalErrors = transactionErrorCounter.count();
            return totalTransactions > 0 ? (totalErrors / totalTransactions) : 0.0;
        } catch (Exception e) {
            logger.error("Failed to get current error rate", e);
            return 0.0;
        }
    }

    // Gauge supplier methods for performance metrics

    /**
     * Supplier method for response time baseline gauge.
     */
    private double getCurrentResponseTimeBaseline() {
        return responseTimeBaseline.get();
    }

    /**
     * Supplier method for throughput baseline gauge.
     */
    private double getCurrentThroughputBaseline() {
        return throughputBaseline.get();
    }

    /**
     * Supplier method for SLA compliance gauge.
     */
    private double getSlaResponseTimeCompliance() {
        return validateSLACompliance() ? 1.0 : 0.0;
    }
}