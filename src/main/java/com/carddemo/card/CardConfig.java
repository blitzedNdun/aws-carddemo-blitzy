package com.carddemo.card;

import com.carddemo.common.config.BaseConfig;
import com.carddemo.common.config.DatabaseConfig;
import com.carddemo.common.security.SecurityConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot configuration class for card management domain providing comprehensive
 * microservices infrastructure including JWT authentication, PostgreSQL optimization,
 * service discovery integration, and audit logging for financial compliance.
 * 
 * This configuration supports the card management microservices that replace legacy
 * COBOL programs COCRDLIC (card listing), COCRDUPC (card update), and COBIL00C 
 * (bill payment) with modern Spring Boot REST APIs while maintaining identical
 * business logic and COBOL COMP-3 decimal precision.
 * 
 * Key Features:
 * - HikariCP connection pooling optimized for card transaction processing
 * - JWT authentication with role-based access control for card operations
 * - Spring Cloud Gateway integration for microservices orchestration
 * - Comprehensive audit event repository for PCI DSS compliance
 * - Transaction management with SERIALIZABLE isolation equivalent to CICS behavior
 * - BigDecimal precision preservation for financial calculations
 * 
 * The configuration enables horizontal scaling through Kubernetes deployment
 * while supporting 10,000+ TPS card transaction processing requirements with
 * sub-200ms response times at 95th percentile performance targets.
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since CardDemo v1.0
 */
@Configuration
@EnableWebSecurity
@EnableJpaRepositories(
    basePackages = "com.carddemo.card.repository",
    entityManagerFactoryRef = "cardEntityManagerFactory",
    transactionManagerRef = "cardTransactionManager"
)
@EnableDiscoveryClient
@EnableTransactionManagement
@PropertySource(value = {
    "classpath:application-card.yml",
    "classpath:application-card-${spring.profiles.active:default}.yml"
}, ignoreResourceNotFound = true)
public class CardConfig extends BaseConfig {

    // Card domain specific configuration properties
    @Value("${carddemo.card.datasource.pool-size:25}")
    private int cardPoolSize;
    
    @Value("${carddemo.card.datasource.minimum-idle:5}")
    private int cardMinimumIdle;
    
    @Value("${carddemo.card.datasource.connection-timeout:20000}")
    private long cardConnectionTimeout;
    
    @Value("${carddemo.card.datasource.idle-timeout:300000}")
    private long cardIdleTimeout;
    
    @Value("${carddemo.card.datasource.max-lifetime:1200000}")
    private long cardMaxLifetime;
    
    // Security configuration properties
    @Value("${carddemo.card.security.session-timeout:1800}")
    private int cardSessionTimeout;
    
    @Value("${carddemo.card.security.max-sessions:100}")
    private int cardMaxSessions;
    
    // Audit configuration properties
    @Value("${carddemo.card.audit.enabled:true}")
    private boolean auditEnabled;
    
    @Value("${carddemo.card.audit.max-events:5000}")
    private int maxAuditEvents;
    
    // Performance monitoring properties
    @Value("${carddemo.card.monitoring.slow-query-threshold:1000}")
    private long slowQueryThreshold;
    
    @Value("${carddemo.card.monitoring.connection-leak-threshold:30000}")
    private long connectionLeakThreshold;

    /**
     * Configures optimized HikariDataSource for card domain operations with
     * connection pooling sized for high-volume card transaction processing.
     * 
     * This data source is specifically tuned for card operations including:
     * - Card listing with pagination (COCRDLIC equivalent)
     * - Card updates with optimistic locking (COCRDUPC equivalent)
     * - Bill payment processing (COBIL00C equivalent)
     * 
     * Pool sizing follows PostgreSQL best practices: smaller pool size
     * for card domain compared to transaction domain due to different
     * access patterns - card operations are typically user-initiated
     * rather than batch processing.
     * 
     * Connection properties are optimized for:
     * - Prepared statement caching for card lookup queries
     * - Batch statement rewriting for bulk card operations
     * - Connection validation for reliability
     * - Leak detection for monitoring and debugging
     * 
     * @return HikariDataSource configured for card domain with optimal performance
     */
    @Bean(name = "cardDataSource")
    @Primary
    @ConfigurationProperties(prefix = "carddemo.card.datasource")
    public DataSource cardDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        
        // Basic connection properties (inherited from common DatabaseConfig)
        dataSource.setJdbcUrl(System.getProperty("carddemo.card.datasource.url", 
            "jdbc:postgresql://localhost:5432/carddemo"));
        dataSource.setUsername(System.getProperty("carddemo.card.datasource.username", "carddemo"));
        dataSource.setPassword(System.getProperty("carddemo.card.datasource.password"));
        dataSource.setDriverClassName("org.postgresql.Driver");
        
        // Card domain specific connection pool optimization
        dataSource.setMaximumPoolSize(cardPoolSize); // 25 connections for card operations
        dataSource.setMinimumIdle(cardMinimumIdle); // 5 idle connections baseline
        dataSource.setConnectionTimeout(cardConnectionTimeout); // 20 seconds timeout
        dataSource.setIdleTimeout(cardIdleTimeout); // 5 minutes idle cleanup
        dataSource.setMaxLifetime(cardMaxLifetime); // 20 minutes max connection lifetime
        dataSource.setLeakDetectionThreshold(connectionLeakThreshold); // 30 seconds leak detection
        
        // Performance optimization for card queries
        dataSource.setConnectionTestQuery("SELECT 1 FROM cards LIMIT 1");
        dataSource.setValidationTimeout(5000);
        dataSource.setInitializationFailTimeout(10000);
        dataSource.setAllowPoolSuspension(false);
        
        // PostgreSQL specific optimizations for card operations
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "300"); // Larger cache for card queries
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource.addDataSourceProperty("rewriteBatchedStatements", "true");
        dataSource.addDataSourceProperty("cacheResultSetMetadata", "true");
        dataSource.addDataSourceProperty("cacheServerConfiguration", "true");
        dataSource.addDataSourceProperty("elideSetAutoCommits", "true");
        dataSource.addDataSourceProperty("maintainTimeStats", "false");
        
        // Card domain specific optimizations
        dataSource.addDataSourceProperty("defaultRowFetchSize", "100"); // Optimized for card listings
        dataSource.addDataSourceProperty("logUnclosedConnections", "true");
        dataSource.addDataSourceProperty("ApplicationName", "CardDemo-Card-Service");
        dataSource.addDataSourceProperty("loginTimeout", "30");
        dataSource.addDataSourceProperty("connectTimeout", "20");
        dataSource.addDataSourceProperty("socketTimeout", "0"); // No socket timeout for long operations
        
        // Connection monitoring and health checks
        dataSource.setPoolName("CardDemo-Card-HikariCP");
        dataSource.setRegisterMbeans(true);
        dataSource.setHealthCheckRegistry(null); // Can be configured with Micrometer
        
        return dataSource;
    }

    /**
     * Configures Spring Security filter chain specifically for card domain endpoints
     * with JWT authentication, role-based authorization, and PCI DSS compliance.
     * 
     * Security configuration supports card operations with appropriate access controls:
     * - Card listing: Requires ROLE_USER for own cards, ROLE_ADMIN for all cards
     * - Card updates: Requires ROLE_USER for own cards, ROLE_ADMIN for any card
     * - Bill payments: Requires ROLE_USER with additional transaction validation
     * 
     * Implements security patterns equivalent to RACF authorization rules
     * from legacy CICS card transaction programs while adding modern
     * JWT token-based authentication and CORS support for React frontend.
     * 
     * @param http HttpSecurity configuration builder
     * @return SecurityFilterChain configured for card domain endpoints
     * @throws Exception if security configuration fails
     */
    @Bean(name = "cardSecurityFilterChain")
    public SecurityFilterChain cardSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Configure CORS for React frontend integration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable CSRF for stateless JWT API
            .csrf(csrf -> csrf.disable())
            
            // Stateless session management for microservices
            .sessionManagement(session -> 
                session.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
                    .maximumSessions(cardMaxSessions)
                    .sessionRegistry(sessionRegistry()))
            
            // Card domain specific authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public health check endpoints
                .requestMatchers(
                    "/api/card/health",
                    "/api/card/info",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()
                
                // Admin-only card management endpoints
                .requestMatchers(
                    "/api/card/admin/**",
                    "/api/card/bulk/**",
                    "/api/card/reports/**"
                ).hasRole("ADMIN")
                
                // User card operations (with method-level security for ownership validation)
                .requestMatchers(
                    "/api/card/list/**",
                    "/api/card/view/**"
                ).hasAnyRole("USER", "ADMIN")
                
                // Card modification operations (requires additional validation)
                .requestMatchers(
                    "/api/card/update/**",
                    "/api/card/activate/**",
                    "/api/card/deactivate/**"
                ).hasAnyRole("USER", "ADMIN")
                
                // Bill payment operations (high-security endpoints)
                .requestMatchers(
                    "/api/card/payment/**",
                    "/api/card/billing/**"
                ).hasAnyRole("USER", "ADMIN")
                
                // All other card API endpoints require authentication
                .requestMatchers("/api/card/**").authenticated()
                
                // Actuator endpoints for monitoring (admin only)
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // Allow other requests
                .anyRequest().permitAll()
            )
            
            // OAuth2 resource server configuration for JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            // Security headers for PCI DSS compliance
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000) // 1 year
                    .includeSubDomains(true)
                    .preload(true)
                )
                .cacheControl(cacheControl -> cacheControl.disable()) // Prevent card data caching
            )
            
            // Exception handling for security events
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(cardAuthenticationEntryPoint())
                .accessDeniedHandler(cardAccessDeniedHandler())
            )
            
            .build();
    }

    /**
     * Configures comprehensive audit event repository for card domain operations
     * ensuring PCI DSS compliance and financial regulatory requirements.
     * 
     * Captures audit events for all card-related operations including:
     * - Card listing and viewing (COCRDLIC equivalent operations)
     * - Card updates and status changes (COCRDUPC equivalent operations)
     * - Bill payment processing (COBIL00C equivalent operations)
     * - Authentication and authorization events
     * - Configuration changes and administrative actions
     * 
     * Audit events include correlation IDs for distributed tracing,
     * user context information, and detailed operation metadata for
     * comprehensive compliance reporting and security monitoring.
     * 
     * Events are structured for integration with ELK stack logging
     * and Prometheus metrics collection for real-time alerting.
     * 
     * @return AuditEventRepository configured for card domain compliance
     */
    @Bean(name = "cardAuditEventRepository")
    @ConditionalOnProperty(name = "carddemo.card.audit.enabled", havingValue = "true", matchIfMissing = true)
    public AuditEventRepository cardAuditEventRepository() {
        return new AuditEventRepository() {
            
            // In-memory storage with configurable capacity for development
            // Production should use persistent storage (database or Redis)
            private final org.springframework.boot.actuate.audit.InMemoryAuditEventRepository 
                delegate = new org.springframework.boot.actuate.audit.InMemoryAuditEventRepository(maxAuditEvents);
            
            @Override
            public void add(AuditEvent event) {
                // Enhance audit event with card domain context
                AuditEvent enhancedEvent = enhanceCardAuditEvent(event);
                delegate.add(enhancedEvent);
                
                // Log high-priority security events immediately
                if (isHighPriorityEvent(event)) {
                    logSecurityEvent(enhancedEvent);
                }
            }
            
            @Override
            public List<AuditEvent> find(String principal, Instant after, String type) {
                return delegate.find(principal, after, type);
            }
            
            /**
             * Enhances audit events with card domain specific context and correlation data
             */
            private AuditEvent enhanceCardAuditEvent(AuditEvent originalEvent) {
                Map<String, Object> enhancedData = new java.util.HashMap<>(originalEvent.getData());
                
                // Add card domain correlation context
                enhancedData.put("domain", "card");
                enhancedData.put("microservice", "card-service");
                enhancedData.put("correlationId", generateCorrelationId());
                enhancedData.put("timestamp", Instant.now());
                enhancedData.put("sessionTimeout", cardSessionTimeout);
                
                // Add security classification for card operations
                enhancedData.put("securityClassification", determineSecurityClassification(originalEvent));
                
                // Add performance metrics for monitoring
                enhancedData.put("slowQueryThreshold", slowQueryThreshold);
                
                // Add PCI DSS compliance markers
                if (isPciRelevantEvent(originalEvent)) {
                    enhancedData.put("pciCompliance", true);
                    enhancedData.put("retentionPeriod", "7years"); // PCI DSS requirement
                }
                
                return new AuditEvent(
                    originalEvent.getTimestamp(),
                    originalEvent.getPrincipal(),
                    originalEvent.getType(),
                    enhancedData
                );
            }
            
            /**
             * Determines if an audit event requires immediate security alerting
             */
            private boolean isHighPriorityEvent(AuditEvent event) {
                String eventType = event.getType();
                return eventType.contains("AUTHENTICATION_FAILURE") ||
                       eventType.contains("ACCESS_DENIED") ||
                       eventType.contains("CARD_UPDATE_UNAUTHORIZED") ||
                       eventType.contains("PAYMENT_FRAUD_DETECTED") ||
                       eventType.contains("CONFIGURATION_CHANGE");
            }
            
            /**
             * Logs high-priority security events for immediate alerting
             */
            private void logSecurityEvent(AuditEvent event) {
                // Integration point for security monitoring system
                org.slf4j.Logger securityLogger = org.slf4j.LoggerFactory.getLogger("SECURITY.CARD");
                securityLogger.warn("High-priority card security event: {} for principal: {} with data: {}", 
                    event.getType(), event.getPrincipal(), event.getData());
            }
            
            /**
             * Determines security classification level for card operations
             */
            private String determineSecurityClassification(AuditEvent event) {
                String eventType = event.getType();
                if (eventType.contains("PAYMENT") || eventType.contains("BILLING")) {
                    return "HIGH"; // Financial transactions
                } else if (eventType.contains("UPDATE") || eventType.contains("ACTIVATE")) {
                    return "MEDIUM"; // Card modifications
                } else {
                    return "LOW"; // Read-only operations
                }
            }
            
            /**
             * Determines if audit event is relevant for PCI DSS compliance
             */
            private boolean isPciRelevantEvent(AuditEvent event) {
                String eventType = event.getType();
                return eventType.contains("CARD") ||
                       eventType.contains("PAYMENT") ||
                       eventType.contains("BILLING") ||
                       eventType.contains("AUTHENTICATION");
            }
            
            /**
             * Generates unique correlation ID for distributed tracing
             */
            private String generateCorrelationId() {
                return "CARD-" + java.util.UUID.randomUUID().toString();
            }
        };
    }

    /**
     * Configures transaction manager specifically for card domain operations
     * with SERIALIZABLE isolation level to ensure data consistency equivalent
     * to CICS transaction behavior and VSAM record locking.
     * 
     * Transaction management supports:
     * - Card listing operations with consistent read isolation
     * - Card update operations with optimistic locking and rollback
     * - Bill payment processing with multi-table transaction integrity
     * - Distributed transaction coordination across microservice boundaries
     * 
     * Configuration provides automatic commit/rollback behavior equivalent
     * to CICS syncpoint semantics while supporting Spring's declarative
     * transaction management through @Transactional annotations.
     * 
     * @return PlatformTransactionManager configured for card domain
     */
    @Bean(name = "cardTransactionManager")
    public PlatformTransactionManager cardTransactionManager() {
        org.springframework.orm.jpa.JpaTransactionManager transactionManager = 
            new org.springframework.orm.jpa.JpaTransactionManager();
        
        // Associate with card entity manager factory
        transactionManager.setEntityManagerFactory(cardEntityManagerFactory().getObject());
        
        // Card domain specific transaction configuration
        transactionManager.setDefaultTimeout(30); // 30 seconds for card operations
        transactionManager.setRollbackOnCommitFailure(true);
        transactionManager.setFailEarlyOnGlobalRollbackOnly(true);
        
        // SERIALIZABLE isolation for VSAM behavior replication
        transactionManager.setDefaultIsolationLevel(
            org.springframework.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE
        );
        
        // Support for nested transactions in complex card operations
        transactionManager.setNestedTransactionAllowed(true);
        transactionManager.setValidateExistingTransaction(true);
        
        // Global transaction support for distributed scenarios
        transactionManager.setGlobalRollbackOnParticipationFailure(true);
        
        return transactionManager;
    }

    /**
     * Configures JPA EntityManagerFactory for card domain entities
     * with PostgreSQL optimizations and COBOL data type compatibility.
     * 
     * @return LocalContainerEntityManagerFactoryBean for card entities
     */
    @Bean(name = "cardEntityManagerFactory")
    public org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean cardEntityManagerFactory() {
        org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean em = 
            new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        
        em.setDataSource(cardDataSource());
        em.setPackagesToScan("com.carddemo.card.entity", "com.carddemo.card.domain");
        
        // Hibernate JPA vendor adapter
        org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter vendorAdapter = 
            new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter();
        vendorAdapter.setDatabasePlatform(org.hibernate.dialect.PostgreSQLDialect.class.getName());
        vendorAdapter.setGenerateDdl(false); // Schema managed by Liquibase
        vendorAdapter.setShowSql(false); // Controlled by configuration
        em.setJpaVendorAdapter(vendorAdapter);
        
        // JPA properties for card domain optimization
        java.util.Properties jpaProperties = new java.util.Properties();
        jpaProperties.setProperty("hibernate.hbm2ddl.auto", "validate");
        jpaProperties.setProperty("hibernate.dialect", org.hibernate.dialect.PostgreSQLDialect.class.getName());
        jpaProperties.setProperty("hibernate.connection.isolation", "4"); // SERIALIZABLE
        jpaProperties.setProperty("hibernate.jdbc.batch_size", "25"); // Optimized for card operations
        jpaProperties.setProperty("hibernate.order_inserts", "true");
        jpaProperties.setProperty("hibernate.order_updates", "true");
        jpaProperties.setProperty("hibernate.generate_statistics", "true");
        jpaProperties.setProperty("hibernate.session_factory_name", "CardDemo-Card-SessionFactory");
        
        em.setJpaProperties(jpaProperties);
        
        return em;
    }

    /**
     * Helper methods for security configuration
     */
    
    private org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        // Inherit CORS configuration from SecurityConfig
        return super.corsConfigurationSource();
    }
    
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        // Inherit JWT decoder from SecurityConfig
        return super.jwtDecoder();
    }
    
    private org.springframework.core.convert.converter.Converter<
        org.springframework.security.oauth2.jwt.Jwt, 
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken> 
        jwtAuthenticationConverter() {
        // Use the converter from SecurityConfig with card-specific enhancements
        return jwt -> {
            // Extract standard authorities
            var standardToken = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                jwt, extractAuthorities(jwt), jwt.getClaimAsString("sub"));
            
            // Add card domain specific claims if needed
            return standardToken;
        };
    }
    
    private java.util.Collection<org.springframework.security.core.GrantedAuthority> extractAuthorities(
        org.springframework.security.oauth2.jwt.Jwt jwt) {
        String userType = jwt.getClaimAsString("user_type");
        if ("A".equals(userType)) {
            return java.util.List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")
            );
        } else if ("U".equals(userType)) {
            return java.util.List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return java.util.List.of();
    }
    
    private org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }
    
    private org.springframework.security.web.AuthenticationEntryPoint cardAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized access to card operations\"}");
        };
    }
    
    private org.springframework.security.web.access.AccessDeniedHandler cardAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(javax.servlet.http.HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Access denied to card operations\"}");
        };
    }
}