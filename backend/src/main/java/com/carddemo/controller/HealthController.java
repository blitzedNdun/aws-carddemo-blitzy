/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for application health checks and monitoring endpoints.
 * Provides health status endpoints for load balancers, Kubernetes readiness 
 * and liveness probes, and application metrics collection.
 * 
 * This controller replaces mainframe monitoring capabilities with cloud-native
 * observability patterns, providing comprehensive health validation for the
 * containerized Spring Boot application running on Kubernetes infrastructure.
 * 
 * Health Endpoints:
 * - GET /api/health - Basic application health status
 * - GET /api/health/ready - Kubernetes readiness probe endpoint
 * - GET /api/health/live - Kubernetes liveness probe endpoint
 * - GET /api/health/metrics - Application metrics and performance data
 * 
 * Integration with Spring Boot Actuator:
 * This controller works alongside Spring Boot Actuator to provide custom
 * health endpoints that integrate with HealthService for business-specific
 * health validations and monitoring requirements.
 * 
 * Kubernetes Integration:
 * The readiness and liveness endpoints support Kubernetes container lifecycle
 * management, enabling automated restart and scaling based on application health.
 * Response times are optimized for sub-second probe requirements.
 * 
 * Cloud-Native Monitoring:
 * Health responses include structured data suitable for monitoring systems,
 * alerting frameworks, and operational dashboards in cloud environments.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    private final HealthService healthService;

    /**
     * Constructor for dependency injection of required services.
     * 
     * @param healthService service providing custom health check implementations
     */
    @Autowired
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
        logger.info("HealthController initialized with comprehensive health monitoring capabilities");
    }

    /**
     * Basic application health check endpoint.
     * 
     * Provides fundamental health status information for load balancers and
     * basic monitoring systems. This endpoint performs lightweight health
     * validation to determine if the application is running and responsive.
     * 
     * Health Check Operations:
     * - Application context validation
     * - Service layer availability verification
     * - Basic component health assessment
     * - Response time measurement
     * 
     * @return ResponseEntity containing basic health status with HTTP 200 for healthy,
     *         HTTP 503 for unhealthy application state
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        logger.debug("Processing basic health check request");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Get application health from service layer
            Map<String, Object> applicationHealth = healthService.getApplicationHealth();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Create health response with essential information
            Map<String, Object> healthResponse = new HashMap<>();
            healthResponse.put("status", "UP");
            healthResponse.put("application_health", applicationHealth.get("health_status"));
            healthResponse.put("service_name", "CardDemo Credit Card Management System");
            healthResponse.put("version", "1.0");
            healthResponse.put("response_time_ms", responseTime);
            healthResponse.put("timestamp", System.currentTimeMillis());
            
            // Include basic application metrics
            healthResponse.put("spring_boot_context", applicationHealth.get("spring_boot_context"));
            healthResponse.put("service_layer", applicationHealth.get("service_layer"));
            healthResponse.put("monitoring_capability", applicationHealth.get("monitoring_capability"));
            
            // Determine HTTP status based on application health
            String appHealthStatus = (String) applicationHealth.get("health_status");
            if ("UP".equals(appHealthStatus)) {
                logger.debug("Basic health check completed successfully in {}ms", responseTime);
                return ResponseEntity.ok(healthResponse);
            } else {
                logger.warn("Basic health check failed - application health status: {}", appHealthStatus);
                healthResponse.put("status", "DOWN");
                healthResponse.put("error", "Application health check failed");
                return ResponseEntity.status(503).body(healthResponse);
            }
            
        } catch (Exception e) {
            logger.error("Basic health check failed with exception", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "DOWN");
            errorResponse.put("error", "Health check failed: " + e.getMessage());
            errorResponse.put("exception_type", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("service_name", "CardDemo Credit Card Management System");
            
            return ResponseEntity.status(503).body(errorResponse);
        }
    }

    /**
     * Kubernetes readiness probe endpoint.
     * 
     * Determines if the application is ready to receive traffic by performing
     * comprehensive health validation including database connectivity and
     * service availability. Used by Kubernetes to control traffic routing.
     * 
     * Readiness Validation Operations:
     * - Database connectivity verification via HealthService
     * - Application component readiness assessment
     * - Service layer initialization validation
     * - External dependency availability check
     * - Performance threshold validation
     * 
     * @return ResponseEntity with HTTP 200 if ready to receive traffic,
     *         HTTP 503 if not ready for traffic routing
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        logger.debug("Processing Kubernetes readiness probe request");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Get comprehensive health status for readiness assessment
            Map<String, Object> overallHealth = healthService.getOverallHealth();
            Map<String, Object> databaseHealth = healthService.getDatabaseHealth();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Create readiness response with detailed status
            Map<String, Object> readinessResponse = new HashMap<>();
            readinessResponse.put("readiness_status", "READY");
            readinessResponse.put("overall_health", overallHealth.get("health_status"));
            readinessResponse.put("database_health", databaseHealth.get("health_status"));
            readinessResponse.put("probe_type", "READINESS");
            readinessResponse.put("response_time_ms", responseTime);
            readinessResponse.put("timestamp", System.currentTimeMillis());
            
            // Include detailed health metrics for troubleshooting
            readinessResponse.put("database_status", overallHealth.get("database_status"));
            readinessResponse.put("application_status", overallHealth.get("application_status"));
            readinessResponse.put("memory_status", overallHealth.get("memory_status"));
            readinessResponse.put("system_state", overallHealth.get("system_state"));
            
            // Include database performance metrics
            readinessResponse.put("database_connectivity_ms", databaseHealth.get("connectivity_test_ms"));
            readinessResponse.put("database_transaction_ms", databaseHealth.get("transaction_test_ms"));
            
            // Determine readiness based on critical components
            String overallHealthStatus = (String) overallHealth.get("health_status");
            String databaseHealthStatus = (String) databaseHealth.get("health_status");
            
            if ("UP".equals(overallHealthStatus) && "UP".equals(databaseHealthStatus)) {
                logger.debug("Readiness probe successful - application ready for traffic in {}ms", responseTime);
                return ResponseEntity.ok(readinessResponse);
            } else {
                logger.warn("Readiness probe failed - Overall: {}, Database: {}", 
                           overallHealthStatus, databaseHealthStatus);
                readinessResponse.put("readiness_status", "NOT_READY");
                readinessResponse.put("error", "Critical components not healthy");
                return ResponseEntity.status(503).body(readinessResponse);
            }
            
        } catch (Exception e) {
            logger.error("Readiness probe failed with exception", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("readiness_status", "NOT_READY");
            errorResponse.put("probe_type", "READINESS");
            errorResponse.put("error", "Readiness check failed: " + e.getMessage());
            errorResponse.put("exception_type", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(errorResponse);
        }
    }

    /**
     * Kubernetes liveness probe endpoint.
     * 
     * Determines if the application is alive and functioning by performing
     * basic health validation. Used by Kubernetes to decide if container
     * restart is required. Optimized for minimal resource usage and fast response.
     * 
     * Liveness Validation Operations:
     * - Application context availability verification
     * - Basic service layer responsiveness check
     * - Memory utilization assessment
     * - Critical component status validation
     * - Response time monitoring
     * 
     * @return ResponseEntity with HTTP 200 if application is alive,
     *         HTTP 503 if application should be restarted
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        logger.debug("Processing Kubernetes liveness probe request");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Get application health for liveness assessment
            Map<String, Object> applicationHealth = healthService.getApplicationHealth();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Create liveness response with essential status
            Map<String, Object> livenessResponse = new HashMap<>();
            livenessResponse.put("liveness_status", "ALIVE");
            livenessResponse.put("application_health", applicationHealth.get("health_status"));
            livenessResponse.put("probe_type", "LIVENESS");
            livenessResponse.put("response_time_ms", responseTime);
            livenessResponse.put("timestamp", System.currentTimeMillis());
            
            // Include critical application metrics
            livenessResponse.put("spring_boot_context", applicationHealth.get("spring_boot_context"));
            livenessResponse.put("application_state", applicationHealth.get("application_state"));
            livenessResponse.put("service_layer", applicationHealth.get("service_layer"));
            livenessResponse.put("health_service", applicationHealth.get("health_service"));
            
            // Include basic runtime information
            livenessResponse.put("java_version", applicationHealth.get("java_version"));
            livenessResponse.put("available_processors", applicationHealth.get("available_processors"));
            
            // Determine liveness based on application health
            String appHealthStatus = (String) applicationHealth.get("health_status");
            if ("UP".equals(appHealthStatus)) {
                logger.debug("Liveness probe successful - application alive in {}ms", responseTime);
                return ResponseEntity.ok(livenessResponse);
            } else {
                logger.warn("Liveness probe failed - application health status: {}", appHealthStatus);
                livenessResponse.put("liveness_status", "NOT_ALIVE");
                livenessResponse.put("error", "Application health check failed");
                return ResponseEntity.status(503).body(livenessResponse);
            }
            
        } catch (Exception e) {
            logger.error("Liveness probe failed with exception", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("liveness_status", "NOT_ALIVE");
            errorResponse.put("probe_type", "LIVENESS");
            errorResponse.put("error", "Liveness check failed: " + e.getMessage());
            errorResponse.put("exception_type", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(errorResponse);
        }
    }

    /**
     * Application metrics endpoint for monitoring systems.
     * 
     * Provides comprehensive application metrics including performance data,
     * resource utilization, and business metrics for monitoring systems,
     * alerting frameworks, and operational dashboards.
     * 
     * Metrics Collection Operations:
     * - Database performance metrics via HealthService
     * - Application component performance data
     * - Memory utilization and JVM metrics
     * - Response time measurements
     * - System resource availability metrics
     * - Business logic performance indicators
     * 
     * @return ResponseEntity containing comprehensive metrics data with HTTP 200,
     *         or HTTP 503 if metrics collection fails
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        logger.debug("Processing application metrics request");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Collect comprehensive metrics from health service
            Map<String, Object> overallHealth = healthService.getOverallHealth();
            Map<String, Object> databaseHealth = healthService.getDatabaseHealth();
            Map<String, Object> applicationHealth = healthService.getApplicationHealth();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Create comprehensive metrics response
            Map<String, Object> metricsResponse = new HashMap<>();
            metricsResponse.put("metrics_status", "AVAILABLE");
            metricsResponse.put("collection_time_ms", responseTime);
            metricsResponse.put("timestamp", System.currentTimeMillis());
            metricsResponse.put("metrics_version", "1.0");
            
            // Overall health metrics
            metricsResponse.put("overall_health_status", overallHealth.get("health_status"));
            metricsResponse.put("system_state", overallHealth.get("system_state"));
            metricsResponse.put("overall_status", overallHealth.get("overall_status"));
            
            // Database performance metrics
            Map<String, Object> databaseMetrics = new HashMap<>();
            databaseMetrics.put("health_status", databaseHealth.get("health_status"));
            databaseMetrics.put("connection_pool", databaseHealth.get("connection_pool"));
            databaseMetrics.put("transaction_capability", databaseHealth.get("transaction_capability"));
            databaseMetrics.put("connectivity_test_ms", databaseHealth.get("connectivity_test_ms"));
            databaseMetrics.put("transaction_test_ms", databaseHealth.get("transaction_test_ms"));
            databaseMetrics.put("total_database_test_ms", databaseHealth.get("total_database_test_ms"));
            if (databaseHealth.containsKey("account_records_count")) {
                databaseMetrics.put("account_records_count", databaseHealth.get("account_records_count"));
            }
            metricsResponse.put("database_metrics", databaseMetrics);
            
            // Application performance metrics
            Map<String, Object> applicationMetrics = new HashMap<>();
            applicationMetrics.put("health_status", applicationHealth.get("health_status"));
            applicationMetrics.put("spring_boot_context", applicationHealth.get("spring_boot_context"));
            applicationMetrics.put("service_layer", applicationHealth.get("service_layer"));
            applicationMetrics.put("repository_injection", applicationHealth.get("repository_injection"));
            applicationMetrics.put("application_state", applicationHealth.get("application_state"));
            applicationMetrics.put("health_service", applicationHealth.get("health_service"));
            applicationMetrics.put("monitoring_capability", applicationHealth.get("monitoring_capability"));
            applicationMetrics.put("logging_system", applicationHealth.get("logging_system"));
            metricsResponse.put("application_metrics", applicationMetrics);
            
            // Runtime and memory metrics
            Map<String, Object> runtimeMetrics = new HashMap<>();
            runtimeMetrics.put("java_version", applicationHealth.get("java_version"));
            runtimeMetrics.put("java_vendor", applicationHealth.get("java_vendor"));
            runtimeMetrics.put("available_processors", applicationHealth.get("available_processors"));
            runtimeMetrics.put("max_memory_mb", applicationHealth.get("max_memory_mb"));
            runtimeMetrics.put("total_memory_mb", applicationHealth.get("total_memory_mb"));
            runtimeMetrics.put("free_memory_mb", applicationHealth.get("free_memory_mb"));
            metricsResponse.put("runtime_metrics", runtimeMetrics);
            
            // Memory utilization details from overall health
            if (overallHealth.containsKey("memory_metrics")) {
                metricsResponse.put("memory_metrics", overallHealth.get("memory_metrics"));
            }
            
            // Health status summary
            Map<String, Object> healthSummary = new HashMap<>();
            healthSummary.put("database_status", overallHealth.get("database_status"));
            healthSummary.put("application_status", overallHealth.get("application_status"));
            healthSummary.put("memory_status", overallHealth.get("memory_status"));
            metricsResponse.put("health_summary", healthSummary);
            
            // Service identification
            metricsResponse.put("service_name", "CardDemo Credit Card Management System");
            metricsResponse.put("component", "HealthController");
            metricsResponse.put("environment", "cloud-native");
            
            logger.debug("Application metrics collected successfully in {}ms", responseTime);
            return ResponseEntity.ok(metricsResponse);
            
        } catch (Exception e) {
            logger.error("Application metrics collection failed with exception", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("metrics_status", "ERROR");
            errorResponse.put("error", "Metrics collection failed: " + e.getMessage());
            errorResponse.put("exception_type", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("service_name", "CardDemo Credit Card Management System");
            
            return ResponseEntity.status(503).body(errorResponse);
        }
    }
}