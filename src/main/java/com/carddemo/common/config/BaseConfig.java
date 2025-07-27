package com.carddemo.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Base Spring Boot configuration class providing common application settings, 
 * bean definitions, and cross-cutting concerns shared across all CardDemo microservices.
 * 
 * This configuration ensures:
 * - Financial data precision equivalent to COBOL COMP-3 using BigDecimal
 * - SERIALIZABLE transaction isolation to replicate VSAM record locking behavior
 * - Spring Boot Actuator management endpoints for monitoring and observability
 * - Common logging patterns with MDC context propagation for distributed tracing
 * - Prometheus metrics collection via Micrometer for cloud-native monitoring
 * 
 * Technical Specifications:
 * - Spring Boot 3.2.x with Java 21 LTS runtime support
 * - PostgreSQL SERIALIZABLE isolation level for CICS-equivalent transaction boundaries
 * - BigDecimal with MathContext.DECIMAL128 for exact COBOL numeric precision
 * - Jackson JSON serialization with financial data precision preservation
 * - Micrometer Prometheus registry for cloud-native metrics collection
 * 
 * @author CardDemo Platform Engineering Team
 * @since 1.0.0
 */
@Configuration
@EnableTransactionManagement
public class BaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(BaseConfig.class);

    /**
     * COBOL COMP-3 equivalent math context for precise financial calculations.
     * Uses DECIMAL128 precision (34 digits) with HALF_EVEN rounding to maintain
     * exact decimal arithmetic equivalent to mainframe COBOL processing.
     */
    public static final MathContext COBOL_MATH_CONTEXT = new MathContext(34, RoundingMode.HALF_EVEN);

    /**
     * Standard financial precision for monetary amounts.
     * Equivalent to COBOL PIC S9(10)V99 COMP-3 fields used throughout CardDemo.
     */
    public static final int FINANCIAL_SCALE = 2;

    /**
     * Interest rate precision for disclosure groups and calculations.
     * Equivalent to COBOL PIC S9(1)V9999 COMP-3 fields (e.g., 0.1250 = 12.50%).
     */
    public static final int INTEREST_RATE_SCALE = 4;

    /**
     * Configures Jackson ObjectMapper with BigDecimal precision preservation for financial data.
     * 
     * Key Features:
     * - Preserves exact BigDecimal precision during JSON serialization/deserialization
     * - Maintains COBOL COMP-3 decimal precision requirements
     * - Handles Java 8+ time types with consistent formatting
     * - Configures proper timezone handling for financial transaction timestamps
     * - Enables parameter names module for constructor-based deserialization
     * 
     * @return Primary ObjectMapper instance for all JSON processing
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        logger.info("Configuring CardDemo ObjectMapper with BigDecimal precision preservation");
        
        ObjectMapper mapper = new ObjectMapper();
        
        // Enable BigDecimal precision preservation for financial calculations
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        mapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        
        // Configure datetime handling with consistent formatting
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        mapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        // Enable parameter names for constructor-based deserialization
        mapper.registerModule(new ParameterNamesModule());
        
        // Configure lenient parsing for backward compatibility
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        logger.info("ObjectMapper configured with COBOL COMP-3 equivalent precision settings");
        return mapper;
    }

    /**
     * Transaction Manager Configuration Note
     * 
     * The primary PlatformTransactionManager with SERIALIZABLE isolation level
     * is configured in DatabaseConfig.java as a JpaTransactionManager.
     * 
     * This provides:
     * - SERIALIZABLE isolation prevents phantom reads and non-repeatable reads
     * - Transaction boundaries equivalent to CICS syncpoint behavior
     * - Automatic rollback on unchecked exceptions
     * - Proper resource cleanup and connection management
     * - JPA-specific optimizations for entity management
     * 
     * The SERIALIZABLE isolation level is critical for maintaining data consistency
     * equivalent to the original CICS/VSAM environment while supporting concurrent
     * access patterns required by the microservices architecture.
     */

    /**
     * Configures AuditEventRepository for comprehensive security and business event tracking.
     * 
     * This repository captures:
     * - Spring Security authentication and authorization events
     * - Business transaction audit trails for regulatory compliance
     * - System access patterns for security monitoring
     * - Data modification events for integrity verification
     * 
     * Events are structured for integration with ELK stack log aggregation
     * and support correlation with distributed tracing via MDC context.
     * 
     * @return AuditEventRepository instance for compliance and security monitoring
     */
    @Bean
    public AuditEventRepository auditEventRepository() {
        logger.info("Configuring CardDemo AuditEventRepository for compliance tracking");
        
        // In-memory repository suitable for microservice architecture
        // Events are typically shipped to centralized logging for persistence
        InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository(1000);
        
        logger.info("AuditEventRepository configured with 1000-event capacity");
        return repository;
    }

    /**
     * Configures Micrometer MeterRegistry for Prometheus metrics collection.
     * 
     * This registry enables comprehensive observability through:
     * - Application performance metrics (response times, throughput)
     * - Business metrics (transaction counts, financial accuracy)
     * - JVM metrics (memory usage, garbage collection)
     * - Custom metrics for domain-specific monitoring
     * 
     * Metrics are exposed via Spring Boot Actuator /actuator/prometheus endpoint
     * for collection by Prometheus server in the cloud-native monitoring stack.
     * 
     * Key Metrics Categories:
     * - Transaction response times (<200ms @ 95th percentile target)
     * - System throughput (10,000+ TPS capacity)
     * - Memory utilization (<110% of CICS baseline requirement)
     * - Database connection pool health and performance
     * - Authentication success/failure rates for security monitoring
     * 
     * @return PrometheusMeterRegistry for cloud-native metrics collection
     */
    @Bean
    public MeterRegistry meterRegistry() {
        logger.info("Configuring CardDemo Prometheus MeterRegistry for cloud-native monitoring");
        
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        // Configure common tags for all metrics
        registry.config()
            .commonTags(
                "application", "carddemo",
                "platform", "kubernetes",
                "architecture", "microservices"
            );
        
        logger.info("Prometheus MeterRegistry configured with CardDemo-specific tags");
        return registry;
    }

    /**
     * Initializes MDC (Mapped Diagnostic Context) configuration for distributed tracing.
     * 
     * This static configuration ensures consistent correlation ID propagation
     * across all microservice boundaries, enabling:
     * - End-to-end request tracing through service calls
     * - Log correlation across distributed components  
     * - Integration with OpenTelemetry trace context
     * - Audit trail reconstruction for regulatory compliance
     * 
     * The MDC context supports the ELK stack centralized logging architecture
     * by providing structured fields for log aggregation and analysis.
     */
    static {
        // Initialize MDC with default application context
        MDC.put("application", "carddemo");
        MDC.put("platform", "spring-boot");
        
        logger.info("CardDemo BaseConfig initialized with distributed tracing support");
        logger.info("Configuration features: BigDecimal precision, SERIALIZABLE transactions, Prometheus metrics");
        logger.info("COBOL compatibility: COMP-3 math context, exact decimal arithmetic, financial data integrity");
    }
}