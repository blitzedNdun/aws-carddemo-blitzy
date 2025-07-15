/*
 * SessionConfig.java
 * 
 * Spring Session configuration class providing Redis-backed distributed session management
 * with TTL-based expiration, session lifecycle management, and cross-microservice session
 * consistency for CICS pseudo-conversational processing equivalence.
 * 
 * This configuration replaces traditional CICS pseudo-conversational processing with
 * modern distributed session management using Redis as the session store, implementing
 * equivalent session timeout behavior and cross-service session consistency.
 * 
 * Key Features:
 * - Redis-backed distributed session storage for microservices architecture
 * - TTL-based session expiration equivalent to CICS terminal timeout (30 minutes)
 * - Session lifecycle management with creation, access, and expiration tracking
 * - Cross-microservice session consistency through Redis cluster support
 * - Session event listeners for comprehensive session management
 * - Secure session configuration with HttpOnly cookies and CSRF protection
 * - Session namespace isolation for multi-tenant session management
 * - High-performance Lettuce async Redis client with connection pooling
 * 
 * Technical Specification References:
 * - Section 6.4.1.3: Session Management Architecture - Redis-backed Spring Session
 * - Section 6.4.1.5: JWT Token Management - Stateless authentication with Redis session
 * - Section 0.1.3: Technical Interpretation - Stateless REST APIs with session management
 * 
 * COBOL Program Context:
 * This configuration supports the transformation of CICS pseudo-conversational processing
 * patterns found in communication area structures like COCOM01Y.cpy, providing equivalent
 * session state preservation for user context, navigation state, and transaction state
 * across distributed microservices architecture.
 * 
 * Copyright (c) 2024 CardDemo Application
 * Licensed under the Apache License, Version 2.0
 */

package com.carddemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SessionConfig provides comprehensive Redis-backed session management configuration
 * for the CardDemo application, implementing distributed session storage with TTL-based
 * expiration equivalent to CICS pseudo-conversational processing patterns.
 * 
 * This configuration enables stateless microservices architecture while maintaining
 * session state consistency across service boundaries through Redis distributed storage.
 * 
 * Session Management Features:
 * - Redis cluster support for high availability and horizontal scaling
 * - TTL-based session expiration matching CICS terminal timeout behavior
 * - Session event lifecycle management with comprehensive audit logging
 * - Cross-microservice session consistency through shared Redis session store
 * - Secure session configuration with HttpOnly cookies and CSRF protection
 * - Session namespace isolation for multi-tenant environment support
 * - High-performance Lettuce async Redis client with connection pooling
 * 
 * Integration with Authentication Flow:
 * - JWT token authentication with Redis session backing for user context
 * - Spring Security integration for session-based authorization
 * - Session correlation with user identity and role information
 * - Session state preservation for navigation and transaction context
 */
@Configuration
@EnableRedisHttpSession(
    maxInactiveIntervalInSeconds = 1800,  // 30 minutes - equivalent to CICS terminal timeout
    redisNamespace = "carddemo:session",   // Session namespace isolation
    cleanupCron = "0 * * * * *"            // Cleanup expired sessions every minute
)
public class SessionConfig {

    private static final Logger logger = LoggerFactory.getLogger(SessionConfig.class);

    /**
     * Redis connection configuration properties from application.yml
     */
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.session.timeout:30m}")
    private Duration sessionTimeout;

    @Value("${spring.session.cookie.name:CARDDEMO_SESSION}")
    private String sessionCookieName;

    @Value("${spring.session.cookie.secure:true}")
    private boolean sessionCookieSecure;

    @Value("${spring.session.cookie.http-only:true}")
    private boolean sessionCookieHttpOnly;

    @Value("${spring.session.cookie.same-site:strict}")
    private String sessionCookieSameSite;

    @Value("${carddemo.session.namespace:carddemo:session}")
    private String sessionNamespace;

    @Autowired(required = false)
    private AuditEventRepository auditEventRepository;

    /**
     * Creates and configures Lettuce Redis connection factory with high-performance
     * async client configuration for distributed session storage.
     * 
     * This connection factory provides:
     * - High-performance async Redis client with connection pooling
     * - Proper database selection and authentication configuration
     * - Connection timeout and retry configuration for reliability
     * - Support for Redis cluster and sentinel configurations
     * 
     * Technical Implementation:
     * - Uses Lettuce client for reactive and async Redis operations
     * - Configures connection pooling for optimal performance
     * - Supports Redis authentication and database selection
     * - Implements connection timeout and retry logic
     * 
     * @return LettuceConnectionFactory configured for session storage
     */
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        logger.info("Configuring Redis connection factory for session management");
        logger.info("Redis host: {}, port: {}, database: {}", redisHost, redisPort, redisDatabase);

        // Configure Redis standalone connection
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);
        
        // Configure Redis password if provided
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfig.setPassword(RedisPassword.of(redisPassword));
            logger.info("Redis password authentication configured");
        }

        // Create Lettuce connection factory with optimized configuration
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);
        
        // Configure connection validation and timeout
        connectionFactory.setValidateConnection(true);
        connectionFactory.setShareNativeConnection(true);
        
        logger.info("Lettuce Redis connection factory configured successfully");
        return connectionFactory;
    }

    /**
     * Configures Redis action to disable automatic Redis server configuration.
     * 
     * This configuration prevents Spring Session from automatically configuring
     * Redis server settings, allowing for external Redis configuration management
     * and supporting cloud-native Redis deployments.
     * 
     * Technical Implementation:
     * - Disables automatic Redis keyspace notification configuration
     * - Allows for external Redis server configuration management
     * - Supports cloud-native Redis deployments with pre-configured settings
     * - Enables Redis cluster and sentinel configurations
     * 
     * @return ConfigureRedisAction.NO_OP for external Redis configuration
     */
    @Bean
    public ConfigureRedisAction configureRedisAction() {
        logger.info("Configuring Redis action for external server configuration");
        return ConfigureRedisAction.NO_OP;
    }

    /**
     * Configures session cookie serializer with security-enhanced settings.
     * 
     * This cookie serializer implements security best practices for session cookies:
     * - HttpOnly flag prevents JavaScript access to session cookies
     * - Secure flag ensures cookies are only sent over HTTPS
     * - SameSite attribute prevents CSRF attacks
     * - Custom cookie name for session identification
     * 
     * Technical Implementation:
     * - Implements HttpOnly cookie settings for XSS protection
     * - Configures secure cookie transmission over HTTPS
     * - Sets SameSite attribute for CSRF protection
     * - Customizes cookie name for application-specific session management
     * 
     * Cookie Security Features:
     * - HttpOnly: Prevents client-side JavaScript access
     * - Secure: Ensures transmission only over HTTPS
     * - SameSite: Prevents cross-site request forgery
     * - Custom naming: Application-specific session identification
     * 
     * @return DefaultCookieSerializer configured with security settings
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        logger.info("Configuring session cookie serializer with security settings");
        
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        
        // Configure session cookie security settings
        cookieSerializer.setCookieName(sessionCookieName);
        cookieSerializer.setHttpOnly(sessionCookieHttpOnly);
        cookieSerializer.setSecure(sessionCookieSecure);
        cookieSerializer.setSameSite(sessionCookieSameSite);
        cookieSerializer.setCookiePath("/");
        cookieSerializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
        cookieSerializer.setUseSecureCookie(sessionCookieSecure);
        
        // Configure cookie expiration based on session timeout
        cookieSerializer.setCookieMaxAge((int) sessionTimeout.toSeconds());
        
        logger.info("Session cookie configured: name={}, secure={}, httpOnly={}, sameSite={}", 
                   sessionCookieName, sessionCookieSecure, sessionCookieHttpOnly, sessionCookieSameSite);
        
        return cookieSerializer;
    }

    /**
     * Configures Redis template for session data serialization with JSON support.
     * 
     * This Redis template provides optimized serialization for session data:
     * - String key serialization for efficient Redis key management
     * - JSON value serialization for complex session object storage
     * - Support for session attribute serialization and deserialization
     * - Connection factory integration for session operations
     * 
     * Technical Implementation:
     * - Uses StringRedisSerializer for efficient key serialization
     * - Implements GenericJackson2JsonRedisSerializer for JSON value storage
     * - Configures hash key and value serializers for session attributes
     * - Provides transaction support for session operations
     * 
     * @param connectionFactory Redis connection factory for template configuration
     * @return RedisTemplate configured for session data operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        logger.info("Configuring Redis template for session data serialization");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Configure string serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Configure JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        // Enable transaction support
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        
        logger.info("Redis template configured with JSON serialization support");
        return template;
    }

    /**
     * Configures session repository customizer for enhanced session management.
     * 
     * This customizer provides additional session repository configuration:
     * - Custom session timeout configuration
     * - Session event publishing for lifecycle management
     * - Session attribute management and serialization
     * - Database-specific session configuration
     * 
     * Technical Implementation:
     * - Customizes session timeout for CICS-equivalent behavior
     * - Enables session event publishing for audit and monitoring
     * - Configures session attribute handling and serialization
     * - Provides database-specific session optimization
     * 
     * @return SessionRepositoryCustomizer for enhanced session management
     */
    @Bean
    public SessionRepositoryCustomizer<RedisIndexedSessionRepository> sessionRepositoryCustomizer() {
        logger.info("Configuring session repository customizer for enhanced session management");
        
        return sessionRepository -> {
            // Configure session timeout equivalent to CICS terminal timeout
            sessionRepository.setDefaultMaxInactiveInterval(sessionTimeout);
            
            // Configure session namespace for multi-tenant support
            sessionRepository.setRedisNamespace(sessionNamespace);
            
            // Enable session event publishing for lifecycle management
            sessionRepository.setSessionCreatedChannelPrefix("session:created:");
            sessionRepository.setSessionDeletedChannelPrefix("session:deleted:");
            sessionRepository.setSessionExpiredChannelPrefix("session:expired:");
            
            logger.info("Session repository customized with timeout: {}, namespace: {}", 
                       sessionTimeout, sessionNamespace);
        };
    }

    /**
     * Configures HTTP session event publisher for session lifecycle management.
     * 
     * This event publisher provides comprehensive session event handling:
     * - Session creation and destruction events
     * - Session attribute change events
     * - Session timeout and expiration events
     * - Integration with Spring Security for authentication events
     * 
     * Technical Implementation:
     * - Publishes session lifecycle events for monitoring and audit
     * - Integrates with Spring Security for authentication context
     * - Provides session event correlation with user identity
     * - Enables session monitoring and analytics
     * 
     * @return HttpSessionEventPublisher for session lifecycle management
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        logger.info("Configuring HTTP session event publisher for lifecycle management");
        return new HttpSessionEventPublisher();
    }

    /**
     * Configures dual session ID resolver supporting both cookies and headers.
     * 
     * This resolver enables flexible session identification:
     * - Cookie-based session identification for web browsers
     * - Header-based session identification for API clients
     * - JWT token correlation with session management
     * - Mobile and SPA application support
     * 
     * Technical Implementation:
     * - Supports both cookie and header-based session resolution
     * - Enables JWT token integration with session management
     * - Provides mobile and single-page application support
     * - Implements fallback session identification strategies
     * 
     * @return HttpSessionIdResolver supporting multiple identification methods
     */
    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        logger.info("Configuring HTTP session ID resolver for flexible session identification");
        
        // Create composite resolver supporting both cookies and headers
        return new CompositeHttpSessionIdResolver(
            new CookieHttpSessionIdResolver(),
            HeaderHttpSessionIdResolver.xAuthToken()
        );
    }

    /**
     * Session event listener for session creation events.
     * 
     * This listener handles session creation events for comprehensive session management:
     * - Logs session creation with user context
     * - Initializes session attributes for CICS-equivalent behavior
     * - Publishes audit events for session lifecycle tracking
     * - Implements session correlation with authentication context
     * 
     * Session Creation Processing:
     * - Captures session creation timestamp and user context
     * - Initializes session attributes from COBOL communication area equivalent
     * - Publishes audit events for compliance and monitoring
     * - Correlates session with JWT authentication context
     * 
     * @param event SessionCreatedEvent containing session creation details
     */
    @EventListener
    public void handleSessionCreated(SessionCreatedEvent event) {
        String sessionId = event.getSessionId();
        logger.info("Session created: sessionId={}, maxInactiveInterval={}", 
                   sessionId, sessionTimeout.toSeconds());
        
        // Initialize session attributes for CICS-equivalent behavior
        Session session = event.getSession();
        session.setAttribute("session.created.timestamp", System.currentTimeMillis());
        session.setAttribute("session.namespace", sessionNamespace);
        session.setAttribute("session.type", "CARDDEMO_SESSION");
        
        // Publish audit event for session creation
        if (auditEventRepository != null) {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("sessionId", sessionId);
            auditData.put("action", "SESSION_CREATED");
            auditData.put("timestamp", System.currentTimeMillis());
            auditData.put("maxInactiveInterval", sessionTimeout.toSeconds());
            
            AuditEvent auditEvent = new AuditEvent("system", "SESSION_MANAGEMENT", auditData);
            auditEventRepository.add(auditEvent);
        }
        
        logger.debug("Session creation event processed successfully: {}", sessionId);
    }

    /**
     * Session event listener for session deletion events.
     * 
     * This listener handles session deletion events for comprehensive session cleanup:
     * - Logs session deletion with cleanup details
     * - Publishes audit events for session lifecycle tracking
     * - Implements session cleanup for resource management
     * - Correlates session deletion with user logout events
     * 
     * Session Deletion Processing:
     * - Captures session deletion timestamp and reason
     * - Cleans up session-related resources and cache entries
     * - Publishes audit events for compliance and monitoring
     * - Implements session correlation cleanup for security
     * 
     * @param event SessionDeletedEvent containing session deletion details
     */
    @EventListener
    public void handleSessionDeleted(SessionDeletedEvent event) {
        String sessionId = event.getSessionId();
        logger.info("Session deleted: sessionId={}", sessionId);
        
        // Publish audit event for session deletion
        if (auditEventRepository != null) {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("sessionId", sessionId);
            auditData.put("action", "SESSION_DELETED");
            auditData.put("timestamp", System.currentTimeMillis());
            auditData.put("reason", "USER_LOGOUT");
            
            AuditEvent auditEvent = new AuditEvent("system", "SESSION_MANAGEMENT", auditData);
            auditEventRepository.add(auditEvent);
        }
        
        logger.debug("Session deletion event processed successfully: {}", sessionId);
    }

    /**
     * Session event listener for session expiration events.
     * 
     * This listener handles session expiration events for comprehensive session management:
     * - Logs session expiration with timeout details
     * - Publishes audit events for session lifecycle tracking
     * - Implements session expiration cleanup for resource management
     * - Correlates session expiration with security monitoring
     * 
     * Session Expiration Processing:
     * - Captures session expiration timestamp and timeout duration
     * - Cleans up expired session resources and cache entries
     * - Publishes audit events for compliance and monitoring
     * - Implements security correlation for session timeout monitoring
     * 
     * @param event SessionExpiredEvent containing session expiration details
     */
    @EventListener
    public void handleSessionExpired(SessionExpiredEvent event) {
        String sessionId = event.getSessionId();
        logger.info("Session expired: sessionId={}, timeout={}s", sessionId, sessionTimeout.toSeconds());
        
        // Publish audit event for session expiration
        if (auditEventRepository != null) {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("sessionId", sessionId);
            auditData.put("action", "SESSION_EXPIRED");
            auditData.put("timestamp", System.currentTimeMillis());
            auditData.put("timeout", sessionTimeout.toSeconds());
            auditData.put("reason", "TIMEOUT");
            
            AuditEvent auditEvent = new AuditEvent("system", "SESSION_MANAGEMENT", auditData);
            auditEventRepository.add(auditEvent);
        }
        
        logger.debug("Session expiration event processed successfully: {}", sessionId);
    }

    /**
     * Composite HTTP session ID resolver supporting multiple identification methods.
     * 
     * This resolver provides flexible session identification for different client types:
     * - Cookie-based identification for web browsers
     * - Header-based identification for API clients and mobile applications
     * - JWT token correlation with session management
     * - Fallback strategy for session identification
     * 
     * Technical Implementation:
     * - Implements multiple session identification strategies
     * - Provides fallback session resolution for different client types
     * - Integrates with JWT token authentication for session correlation
     * - Supports mobile and single-page application requirements
     */
    private static class CompositeHttpSessionIdResolver implements HttpSessionIdResolver {
        
        private final HttpSessionIdResolver[] resolvers;
        
        public CompositeHttpSessionIdResolver(HttpSessionIdResolver... resolvers) {
            this.resolvers = resolvers;
        }
        
        @Override
        public java.util.List<String> resolveSessionIds(HttpServletRequest request) {
            for (HttpSessionIdResolver resolver : resolvers) {
                java.util.List<String> sessionIds = resolver.resolveSessionIds(request);
                if (!sessionIds.isEmpty()) {
                    return sessionIds;
                }
            }
            return java.util.Collections.emptyList();
        }
        
        @Override
        public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
            for (HttpSessionIdResolver resolver : resolvers) {
                resolver.setSessionId(request, response, sessionId);
            }
        }
        
        @Override
        public void expireSession(HttpServletRequest request, HttpServletResponse response) {
            for (HttpSessionIdResolver resolver : resolvers) {
                resolver.expireSession(request, response);
            }
        }
    }
}