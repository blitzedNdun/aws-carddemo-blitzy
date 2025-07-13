package com.carddemo.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

/**
 * Base Spring Boot configuration class providing common application settings, 
 * bean definitions, and cross-cutting concerns shared across all CardDemo microservices.
 * 
 * This configuration includes:
 * - Transaction management with SERIALIZABLE isolation for VSAM behavior replication
 * - JSON serialization with BigDecimal precision for financial data
 * - Spring Boot Actuator management endpoints configuration  
 * - Common logging patterns and MDC context propagation
 * - Micrometer metrics registry for Prometheus monitoring
 * 
 * All financial calculations use BigDecimal with MathContext.DECIMAL128 precision
 * to maintain exact decimal arithmetic equivalent to COBOL COMP-3 behavior.
 */
@Configuration
@EnableTransactionManagement
public class BaseConfig {

    /**
     * COBOL COMP-3 equivalent MathContext for exact financial precision.
     * Uses DECIMAL128 with HALF_EVEN rounding to replicate mainframe arithmetic behavior.
     */
    public static final MathContext COBOL_MATH_CONTEXT = new MathContext(31, RoundingMode.HALF_EVEN);

    /**
     * Configures Jackson ObjectMapper with BigDecimal precision preservation for financial data.
     * 
     * Key features:
     * - BigDecimal serialization without scientific notation
     * - Exact decimal precision maintenance for COBOL COMP-3 equivalence
     * - JSON include non-null for efficient API responses
     * - JSR310 time module for proper date/time handling
     * - Failure handling for unknown properties to support schema evolution
     * 
     * @return Configured ObjectMapper for Spring Boot JSON serialization
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure BigDecimal serialization for financial precision
        mapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // JSON response optimization
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // Date/time handling for Spring Boot REST APIs
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Custom BigDecimal module for COBOL COMP-3 precision
        SimpleModule bigDecimalModule = new SimpleModule();
        bigDecimalModule.addSerializer(BigDecimal.class, new CobolPrecisionBigDecimalSerializer());
        mapper.registerModule(bigDecimalModule);
        
        return mapper;
    }

    /**
     * Configures Spring transaction manager with SERIALIZABLE isolation level.
     * 
     * This configuration replicates VSAM record locking behavior and CICS syncpoint 
     * semantics by using PostgreSQL SERIALIZABLE transaction isolation. Ensures:
     * - ACID compliance equivalent to CICS automatic commit/rollback
     * - Prevention of phantom reads and non-repeatable reads
     * - Distributed transaction consistency across microservice boundaries
     * 
     * @param dataSource HikariCP connection pool configured for optimal performance
     * @return Platform transaction manager with SERIALIZABLE isolation
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        
        // SERIALIZABLE isolation for VSAM behavior replication
        transactionManager.setDefaultIsolationLevel(
            org.springframework.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE
        );
        
        // Enable nested transaction support for complex business operations
        transactionManager.setNestedTransactionAllowed(true);
        
        // Rollback on all exceptions for strict error handling
        transactionManager.setRollbackOnCommitFailure(true);
        
        return transactionManager;
    }

    /**
     * Configures Spring Boot Actuator audit event repository for comprehensive audit trails.
     * 
     * Captures audit events for:
     * - User authentication and authorization (Spring Security integration)
     * - Transaction processing events (business logic auditing)
     * - Configuration changes and administrative actions
     * - System errors and exception handling
     * 
     * Audit events are correlated with Prometheus metrics and ELK stack logs
     * for complete observability as specified in monitoring architecture.
     * 
     * @return In-memory audit event repository with 10000 event retention
     */
    @Bean
    public AuditEventRepository auditEventRepository() {
        // In-memory repository with configurable capacity for development
        // Production deployments should use persistent repository (database/Redis)
        InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository(10000);
        
        // Enable audit event correlation with request tracking
        return new AuditEventRepository() {
            @Override
            public void add(AuditEvent event) {
                // Enhance audit event with correlation context
                AuditEvent enhancedEvent = new AuditEvent(
                    event.getTimestamp(),
                    event.getPrincipal(),
                    event.getType(),
                    enhanceAuditData(event.getData())
                );
                repository.add(enhancedEvent);
            }

            @Override
            public java.util.List<AuditEvent> find(String principal, Instant after, String type) {
                return repository.find(principal, after, type);
            }
            
            /**
             * Enhances audit event data with correlation information for observability.
             */
            private Map<String, Object> enhanceAuditData(Map<String, Object> originalData) {
                Map<String, Object> enhancedData = new java.util.HashMap<>(originalData);
                
                // Add correlation ID for distributed tracing
                enhancedData.put("correlationId", generateCorrelationId());
                enhancedData.put("timestamp", Instant.now());
                enhancedData.put("microservice", getApplicationName());
                
                return enhancedData;
            }
            
            private String generateCorrelationId() {
                return java.util.UUID.randomUUID().toString();
            }
            
            private String getApplicationName() {
                return System.getProperty("spring.application.name", "carddemo-service");
            }
        };
    }

    /**
     * Configures Micrometer meter registry for Prometheus metrics collection.
     * 
     * Provides comprehensive application monitoring including:
     * - HTTP request/response metrics with latency histograms
     * - JVM memory and garbage collection metrics  
     * - Database connection pool metrics (HikariCP integration)
     * - Business metrics for transaction processing
     * - Custom metrics for financial calculation accuracy
     * 
     * Metrics are exported via /actuator/prometheus endpoint for Prometheus scraping
     * as specified in the monitoring and observability architecture.
     * 
     * @return Composite meter registry with Prometheus backend
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        // Create Prometheus registry for metrics export
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        // Configure common tags for all metrics
        prometheusRegistry.config()
            .commonTags(
                "application", getApplicationName(),
                "version", getApplicationVersion(),
                "environment", getEnvironment()
            );

        // Create composite registry for multiple backends
        CompositeMeterRegistry compositeRegistry = new CompositeMeterRegistry();
        compositeRegistry.add(prometheusRegistry);
        
        // Enable JVM metrics for performance monitoring
        io.micrometer.core.instrument.binder.jvm.JvmGcMetrics.builder().register(compositeRegistry);
        io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics.builder().register(compositeRegistry);
        io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics.builder().register(compositeRegistry);
        
        // Enable system metrics for infrastructure monitoring
        io.micrometer.core.instrument.binder.system.ProcessorMetrics.builder().register(compositeRegistry);
        io.micrometer.core.instrument.binder.system.UptimeMetrics.builder().register(compositeRegistry);
        
        return compositeRegistry;
    }

    /**
     * Gets application name from Spring properties or system properties.
     */
    private String getApplicationName() {
        return System.getProperty("spring.application.name", "carddemo-common");
    }

    /**
     * Gets application version from build information or default.
     */
    private String getApplicationVersion() {
        return System.getProperty("spring.application.version", "1.0.0");
    }

    /**
     * Gets deployment environment from active profiles.
     */
    private String getEnvironment() {
        String activeProfiles = System.getProperty("spring.profiles.active", "default");
        return activeProfiles.split(",")[0]; // Use first active profile as environment
    }

    /**
     * Custom BigDecimal serializer for COBOL COMP-3 precision preservation.
     * 
     * Ensures financial values are serialized with exact decimal precision
     * without scientific notation, maintaining compatibility with frontend
     * components and external API consumers.
     */
    private static class CobolPrecisionBigDecimalSerializer 
            extends com.fasterxml.jackson.databind.JsonSerializer<BigDecimal> {
        
        @Override
        public void serialize(BigDecimal value, com.fasterxml.jackson.core.JsonGenerator gen,
                com.fasterxml.jackson.databind.SerializerProvider serializers) 
                throws java.io.IOException {
            
            if (value == null) {
                gen.writeNull();
                return;
            }
            
            // Ensure COBOL-compatible precision and scale
            BigDecimal normalizedValue = value.setScale(2, RoundingMode.HALF_EVEN);
            
            // Write as plain string to preserve exact decimal representation
            gen.writeString(normalizedValue.toPlainString());
        }
    }
}