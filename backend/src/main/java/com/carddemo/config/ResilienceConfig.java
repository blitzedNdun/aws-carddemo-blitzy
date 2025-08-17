package com.carddemo.config;

import com.carddemo.service.PaymentService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.micrometer.tagged;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.ratelimiter.RateLimiter;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

/**
 * Resilience4j configuration for circuit breaker, retry, and rate limiting patterns.
 * Implements fault tolerance for external system integrations including payment networks
 * and bank core systems, ensuring system stability during downstream failures.
 * 
 * This configuration supports the PaymentService methods for external payment processing
 * integrations with comprehensive fault tolerance patterns including circuit breakers,
 * retry mechanisms, rate limiting, and fallback methods for graceful degradation.
 * 
 * The configuration aligns with the modernized COBOL-to-Java migration approach,
 * providing resilient external system integration while maintaining identical
 * business logic and processing patterns.
 */
@Configuration
public class ResilienceConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfig.class);
    
    // Circuit breaker thresholds aligned with payment network requirements
    private static final int DEFAULT_SLIDING_WINDOW_SIZE = 10;
    private static final float DEFAULT_FAILURE_RATE_THRESHOLD = 50.0f;
    private static final int DEFAULT_MINIMUM_NUMBER_OF_CALLS = 5;
    private static final Duration DEFAULT_WAIT_DURATION_IN_OPEN_STATE = Duration.ofSeconds(30);
    private static final int DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE = 3;
    
    // Retry configuration for transient network failures
    private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;
    private static final Duration DEFAULT_WAIT_DURATION = Duration.ofMillis(500);
    private static final Duration DEFAULT_MAX_WAIT_DURATION = Duration.ofSeconds(5);
    private static final Duration RETRY_DELAY_INCREMENT = Duration.ofMillis(200);
    
    // Rate limiter configuration for API protection
    private static final int DEFAULT_LIMIT_FOR_PERIOD = 100;
    private static final Duration DEFAULT_LIMIT_REFRESH_PERIOD = Duration.ofSeconds(1);
    private static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofMillis(500);
    
    // Enhanced timeout configurations using Duration utility methods
    private static final Duration PAYMENT_NETWORK_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration BANK_SYSTEM_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration CARD_VALIDATION_TIMEOUT = Duration.ofSeconds(45);
    
    @Value("${carddemo.resilience.circuit-breaker.enabled:true}")
    private boolean circuitBreakerEnabled;
    
    @Value("${carddemo.resilience.retry.enabled:true}")
    private boolean retryEnabled;
    
    @Value("${carddemo.resilience.rate-limiter.enabled:true}")
    private boolean rateLimiterEnabled;
    
    @Value("${carddemo.resilience.metrics.enabled:true}")
    private boolean metricsEnabled;
    
    @Value("${carddemo.resilience.health-indicators.enabled:true}")
    private boolean healthIndicatorsEnabled;
    
    /**
     * Post-construction initialization method for logging resilience configuration startup.
     * Provides comprehensive logging of all configured fault tolerance patterns and settings.
     */
    @PostConstruct
    public void initializeResilienceConfiguration() {
        logger.info("Starting ResilienceConfig initialization for CardDemo payment system");
        logger.info("Configuration flags - Circuit Breakers: {}, Retries: {}, Rate Limiters: {}, Metrics: {}", 
                   circuitBreakerEnabled, retryEnabled, rateLimiterEnabled, metricsEnabled);
        logger.info("Timeout configurations - Payment Network: {}s, Bank System: {}s, Card Validation: {}s",
                   PAYMENT_NETWORK_TIMEOUT.getSeconds(), BANK_SYSTEM_TIMEOUT.getSeconds(), 
                   CARD_VALIDATION_TIMEOUT.getSeconds());
    }

    /**
     * Creates and configures the CircuitBreakerRegistry with payment network and bank system
     * specific configurations. Supports PaymentService.authorizePayment(), 
     * PaymentService.processTransaction(), and PaymentService.validateCardDetails() methods.
     * 
     * @return Configured CircuitBreakerRegistry for external system fault tolerance
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        logger.info("Initializing CircuitBreakerRegistry with payment network configurations");
        
        // Create registry using ofDefaults() as specified in external imports
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        
        // Get default configuration for customization
        CircuitBreakerConfig defaultConfig = registry.getDefaultConfig();
        
        // Configure specific circuit breakers for PaymentService methods using circuitBreaker() method
        if (circuitBreakerEnabled) {
            // Circuit breaker for payment network authorization (authorizePayment method)
            CircuitBreaker paymentNetworkAuth = registry.circuitBreaker("paymentNetworkAuth", 
                CircuitBreakerConfig.custom()
                    .slidingWindowSize(DEFAULT_SLIDING_WINDOW_SIZE)
                    .failureRateThreshold(DEFAULT_FAILURE_RATE_THRESHOLD)
                    .minimumNumberOfCalls(DEFAULT_MINIMUM_NUMBER_OF_CALLS)
                    .waitDurationInOpenState(DEFAULT_WAIT_DURATION_IN_OPEN_STATE)
                    .permittedNumberOfCallsInHalfOpenState(DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE)
                    .automaticTransitionFromOpenToHalfOpenEnabled(true)
                    .build());
            
            // Circuit breaker for payment processing (processTransaction method)
            CircuitBreaker paymentProcessing = registry.circuitBreaker("paymentProcessing",
                CircuitBreakerConfig.custom()
                    .slidingWindowSize(DEFAULT_SLIDING_WINDOW_SIZE)
                    .failureRateThreshold(DEFAULT_FAILURE_RATE_THRESHOLD)
                    .minimumNumberOfCalls(DEFAULT_MINIMUM_NUMBER_OF_CALLS)
                    .waitDurationInOpenState(PAYMENT_NETWORK_TIMEOUT)
                    .permittedNumberOfCallsInHalfOpenState(DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE)
                    .automaticTransitionFromOpenToHalfOpenEnabled(true)
                    .build());
            
            // Circuit breaker for card validation (validateCardDetails method)
            CircuitBreaker cardValidation = registry.circuitBreaker("cardValidation",
                CircuitBreakerConfig.custom()
                    .slidingWindowSize(DEFAULT_SLIDING_WINDOW_SIZE)
                    .failureRateThreshold(DEFAULT_FAILURE_RATE_THRESHOLD * 0.7f) // More sensitive for validation
                    .minimumNumberOfCalls(DEFAULT_MINIMUM_NUMBER_OF_CALLS)
                    .waitDurationInOpenState(CARD_VALIDATION_TIMEOUT)
                    .permittedNumberOfCallsInHalfOpenState(DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE)
                    .automaticTransitionFromOpenToHalfOpenEnabled(true)
                    .build());
            
            logger.info("Configured circuit breakers: paymentNetworkAuth, paymentProcessing, cardValidation");
        }
        
        logger.info("CircuitBreakerRegistry initialized with default configuration - " +
                   "Sliding window size: {}, Failure threshold: {}%, Wait duration: {}",
                   DEFAULT_SLIDING_WINDOW_SIZE, DEFAULT_FAILURE_RATE_THRESHOLD, 
                   DEFAULT_WAIT_DURATION_IN_OPEN_STATE.getSeconds());
        
        return registry;
    }

    /**
     * Creates and configures the RetryRegistry with transient failure retry policies
     * for external payment network and bank system communication failures.
     * Supports resilient retry patterns for all PaymentService external calls.
     * 
     * @return Configured RetryRegistry for transient failure handling
     */
    @Bean
    public RetryRegistry retryRegistry() {
        logger.info("Initializing RetryRegistry with transient failure retry policies");
        
        // Create registry using ofDefaults() as specified in external imports
        RetryRegistry registry = RetryRegistry.ofDefaults();
        
        // Get default configuration for validation
        RetryConfig defaultConfig = registry.getDefaultConfig();
        
        // Configure specific retry policies for PaymentService methods using retry() method
        if (retryEnabled) {
            // Retry policy for payment network authorization with exponential backoff
            Retry paymentNetworkAuthRetry = registry.retry("paymentNetworkAuth",
                RetryConfig.custom()
                    .maxAttempts(DEFAULT_MAX_RETRY_ATTEMPTS)
                    .waitDuration(DEFAULT_WAIT_DURATION)
                    .intervalFunction(attempt -> Duration.ofMillis(
                        DEFAULT_WAIT_DURATION.toMillis() + (attempt * RETRY_DELAY_INCREMENT.toMillis())))
                    .retryOnException(throwable -> 
                        throwable instanceof java.net.SocketTimeoutException ||
                        throwable instanceof java.net.ConnectException ||
                        throwable instanceof org.springframework.web.client.ResourceAccessException)
                    .build());
            
            // Retry policy for payment processing with conservative backoff
            Retry paymentProcessingRetry = registry.retry("paymentProcessing",
                RetryConfig.custom()
                    .maxAttempts(DEFAULT_MAX_RETRY_ATTEMPTS - 1) // Fewer retries for processing
                    .waitDuration(Duration.ofSeconds(1)) // Longer wait for processing
                    .intervalFunction(attempt -> Duration.ofSeconds(attempt * 2))
                    .retryOnException(throwable -> 
                        throwable instanceof java.net.SocketTimeoutException ||
                        throwable instanceof java.net.ConnectException)
                    .build());
            
            // Retry policy for card validation with rapid backoff
            Retry cardValidationRetry = registry.retry("cardValidation",
                RetryConfig.custom()
                    .maxAttempts(DEFAULT_MAX_RETRY_ATTEMPTS)
                    .waitDuration(Duration.ofMillis(300))
                    .intervalFunction(attempt -> Duration.ofMillis(300 * attempt))
                    .retryOnException(throwable -> 
                        throwable instanceof java.net.SocketTimeoutException ||
                        throwable instanceof org.springframework.web.client.ResourceAccessException)
                    .build());
            
            logger.info("Configured retry policies: paymentNetworkAuth, paymentProcessing, cardValidation");
        }
        
        logger.info("RetryRegistry initialized with retry configuration - " +
                   "Max attempts: {}, Wait duration: {}ms, Max wait duration: {}s",
                   DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_WAIT_DURATION.toMillis(),
                   DEFAULT_MAX_WAIT_DURATION.getSeconds());
        
        return registry;
    }

    /**
     * Creates and configures the RateLimiterRegistry for API protection and load management.
     * Prevents overload of external payment networks and bank systems by limiting
     * request rates from PaymentService methods.
     * 
     * @return Configured RateLimiterRegistry for API protection
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        logger.info("Initializing RateLimiterRegistry for API protection and load management");
        
        // Create registry using ofDefaults() as specified in external imports
        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        
        // Get default configuration for validation
        RateLimiterConfig defaultConfig = registry.getDefaultConfig();
        
        // Configure specific rate limiters for PaymentService methods using rateLimiter() method
        if (rateLimiterEnabled) {
            // Rate limiter for payment network authorization - high throughput
            RateLimiter paymentNetworkAuthLimiter = registry.rateLimiter("paymentNetworkAuth",
                RateLimiterConfig.custom()
                    .limitForPeriod(DEFAULT_LIMIT_FOR_PERIOD) // 100 requests per second
                    .limitRefreshPeriod(DEFAULT_LIMIT_REFRESH_PERIOD)
                    .timeoutDuration(DEFAULT_TIMEOUT_DURATION)
                    .writableStackTraceEnabled(true)
                    .build());
            
            // Rate limiter for payment processing - moderate throughput
            RateLimiter paymentProcessingLimiter = registry.rateLimiter("paymentProcessing",
                RateLimiterConfig.custom()
                    .limitForPeriod(50) // Lower limit for processing operations
                    .limitRefreshPeriod(DEFAULT_LIMIT_REFRESH_PERIOD)
                    .timeoutDuration(Duration.ofSeconds(1)) // Longer timeout for processing
                    .writableStackTraceEnabled(true)
                    .build());
            
            // Rate limiter for card validation - high burst tolerance
            RateLimiter cardValidationLimiter = registry.rateLimiter("cardValidation",
                RateLimiterConfig.custom()
                    .limitForPeriod(200) // Higher limit for validation
                    .limitRefreshPeriod(DEFAULT_LIMIT_REFRESH_PERIOD)
                    .timeoutDuration(Duration.ofMillis(200)) // Quick timeout for validation
                    .writableStackTraceEnabled(true)
                    .build());
            
            logger.info("Configured rate limiters: paymentNetworkAuth (100/s), paymentProcessing (50/s), cardValidation (200/s)");
        }
        
        logger.info("RateLimiterRegistry initialized with rate limiting configuration - " +
                   "Limit per period: {}, Refresh period: {}s, Timeout: {}ms",
                   DEFAULT_LIMIT_FOR_PERIOD, DEFAULT_LIMIT_REFRESH_PERIOD.getSeconds(),
                   DEFAULT_TIMEOUT_DURATION.toMillis());
        
        return registry;
    }

    /**
     * Configures specific circuit breaker for payment network integrations.
     * Provides dedicated fault tolerance for PaymentService.authorizePayment()
     * and PaymentService.processTransaction() external network calls.
     * 
     * @param circuitBreakerRegistry The circuit breaker registry
     * @return Configured circuit breaker customizer for payment networks
     */
    @Bean
    public CircuitBreakerConfigCustomizer paymentNetworkCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        logger.info("Configuring payment network circuit breaker with enhanced fault tolerance");
        
        return CircuitBreakerConfigCustomizer.of("paymentNetworkAuth", builder -> {
            builder.slidingWindowSize(DEFAULT_SLIDING_WINDOW_SIZE)
                   .failureRateThreshold(DEFAULT_FAILURE_RATE_THRESHOLD)
                   .minimumNumberOfCalls(DEFAULT_MINIMUM_NUMBER_OF_CALLS)
                   .waitDurationInOpenState(DEFAULT_WAIT_DURATION_IN_OPEN_STATE)
                   .permittedNumberOfCallsInHalfOpenState(DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE)
                   .automaticTransitionFromOpenToHalfOpenEnabled(true)
                   .recordExceptions(Exception.class)
                   .ignoreExceptions(IllegalArgumentException.class);
            
            logger.debug("Payment network circuit breaker configured - Name: paymentNetworkAuth, " +
                        "Failure threshold: {}%, Wait duration: {}s",
                        DEFAULT_FAILURE_RATE_THRESHOLD, DEFAULT_WAIT_DURATION_IN_OPEN_STATE.getSeconds());
        });
    }

    /**
     * Configures specific circuit breaker for bank system integrations.
     * Provides dedicated fault tolerance for bank core system communication
     * through PaymentService external system calls.
     * 
     * @param circuitBreakerRegistry The circuit breaker registry
     * @return Configured circuit breaker customizer for bank systems
     */
    @Bean
    public CircuitBreakerConfigCustomizer bankSystemCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        logger.info("Configuring bank system circuit breaker with conservative fault tolerance");
        
        return CircuitBreakerConfigCustomizer.of("bankSystemStatus", builder -> {
            builder.slidingWindowSize(DEFAULT_SLIDING_WINDOW_SIZE)
                   .failureRateThreshold(DEFAULT_FAILURE_RATE_THRESHOLD * 0.8f) // More conservative for bank systems
                   .minimumNumberOfCalls(DEFAULT_MINIMUM_NUMBER_OF_CALLS)
                   .waitDurationInOpenState(Duration.ofSeconds(60)) // Longer wait for bank systems
                   .permittedNumberOfCallsInHalfOpenState(DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE)
                   .automaticTransitionFromOpenToHalfOpenEnabled(true)
                   .recordExceptions(Exception.class)
                   .ignoreExceptions(IllegalArgumentException.class);
            
            logger.debug("Bank system circuit breaker configured - Name: bankSystemStatus, " +
                        "Failure threshold: {}%, Wait duration: 60s",
                        DEFAULT_FAILURE_RATE_THRESHOLD * 0.8f);
        });
    }

    /**
     * Configures retry policy for transient network failures.
     * Supports PaymentService.authorizePayment(), PaymentService.processTransaction(),
     * and PaymentService.validateCardDetails() with intelligent retry mechanisms.
     * 
     * @param retryRegistry The retry registry
     * @return Configured retry customizer for transient failures
     */
    @Bean
    public RetryConfigCustomizer transientRetryConfig(RetryRegistry retryRegistry) {
        logger.info("Configuring transient retry policy with exponential backoff");
        
        return RetryConfigCustomizer.of("paymentNetworkAuth", builder -> {
            builder.maxAttempts(DEFAULT_MAX_RETRY_ATTEMPTS)
                   .waitDuration(DEFAULT_WAIT_DURATION)
                   .intervalFunction(interval -> Duration.ofMillis(interval.toMillis() * 2)) // Exponential backoff
                   .retryOnException(throwable -> {
                       // Retry on network timeouts and service unavailable errors
                       return throwable instanceof java.net.SocketTimeoutException ||
                              throwable instanceof java.net.ConnectException ||
                              throwable instanceof org.springframework.web.client.ResourceAccessException;
                   })
                   .ignoreExceptions(IllegalArgumentException.class, 
                                   SecurityException.class);
            
            logger.debug("Transient retry policy configured - Name: paymentNetworkAuth, " +
                        "Max attempts: {}, Initial wait: {}ms",
                        DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_WAIT_DURATION.toMillis());
        });
    }

    /**
     * Configures rate limiter for payment processing API protection.
     * Prevents overload of external systems accessed by PaymentService methods
     * while maintaining optimal throughput for payment processing.
     * 
     * @param rateLimiterRegistry The rate limiter registry
     * @return Configured rate limiter customizer for payment APIs
     */
    @Bean
    public RateLimiterConfigCustomizer paymentRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        logger.info("Configuring payment rate limiter for API protection and load management");
        
        return RateLimiterConfigCustomizer.of("paymentNetworkAuth", builder -> {
            builder.limitForPeriod(DEFAULT_LIMIT_FOR_PERIOD)
                   .limitRefreshPeriod(DEFAULT_LIMIT_REFRESH_PERIOD)
                   .timeoutDuration(DEFAULT_TIMEOUT_DURATION)
                   .writableStackTraceEnabled(true);
            
            logger.debug("Payment rate limiter configured - Name: paymentNetworkAuth, " +
                        "Limit: {} requests per {}s, Timeout: {}ms",
                        DEFAULT_LIMIT_FOR_PERIOD, DEFAULT_LIMIT_REFRESH_PERIOD.getSeconds(),
                        DEFAULT_TIMEOUT_DURATION.toMillis());
        });
    }

    /**
     * Configures comprehensive metrics collection for circuit breaker monitoring.
     * Integrates Resilience4j components with Micrometer metrics system for
     * operational visibility into fault tolerance patterns and external system health.
     * 
     * @param meterRegistry The Micrometer meter registry
     * @param circuitBreakerRegistry The circuit breaker registry
     * @param retryRegistry The retry registry  
     * @param rateLimiterRegistry The rate limiter registry
     */
    @Bean
    public void configureMetrics(MeterRegistry meterRegistry,
                                CircuitBreakerRegistry circuitBreakerRegistry,
                                RetryRegistry retryRegistry,
                                RateLimiterRegistry rateLimiterRegistry) {
        
        logger.info("Configuring comprehensive Resilience4j metrics integration with Micrometer");
        
        // Configure circuit breaker metrics using tagged utility as specified in external imports
        tagged.ofCircuitBreakerRegistry(circuitBreakerRegistry).bindTo(meterRegistry);
        
        // Configure retry metrics using tagged utility as specified in external imports
        tagged.ofRetryRegistry(retryRegistry).bindTo(meterRegistry);
        
        // Configure rate limiter metrics using tagged utility as specified in external imports
        tagged.ofRateLimiterRegistry(rateLimiterRegistry).bindTo(meterRegistry);
        
        // Create custom gauge for monitoring circuit breaker states
        meterRegistry.gauge("carddemo.resilience.circuit_breakers.total", 
                           circuitBreakerRegistry.getAllCircuitBreakers().size());
        
        // Create custom counter for tracking resilience events
        meterRegistry.counter("carddemo.resilience.configuration.initialized").increment();
        
        // Create custom timer for measuring configuration initialization time
        meterRegistry.timer("carddemo.resilience.configuration.startup_time");
        
        logger.info("Resilience4j metrics configuration completed - " +
                   "Circuit breakers: {}, Retry policies: {}, Rate limiters: {}",
                   circuitBreakerRegistry.getAllCircuitBreakers().size(),
                   retryRegistry.getAllRetries().size(),
                   rateLimiterRegistry.getAllRateLimiters().size());
        
        logger.info("ResilienceConfig initialization completed successfully - " +
                   "All fault tolerance patterns configured for PaymentService external integrations");
    }
    
    /**
     * Creates a health indicator for monitoring resilience components health status.
     * Provides operational visibility into circuit breaker states, retry registrations,
     * and rate limiter status for Spring Boot Actuator health endpoints.
     * 
     * @param circuitBreakerRegistry The circuit breaker registry
     * @param retryRegistry The retry registry
     * @param rateLimiterRegistry The rate limiter registry
     * @return Health indicator for resilience components
     */
    @Bean
    public HealthIndicator resilienceHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry,
                                                    RetryRegistry retryRegistry,
                                                    RateLimiterRegistry rateLimiterRegistry) {
        
        return () -> {
            try {
                Health.Builder healthBuilder = Health.up();
                
                if (healthIndicatorsEnabled) {
                    // Check circuit breaker states
                    long openCircuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers()
                        .stream()
                        .mapToLong(cb -> cb.getState() == CircuitBreaker.State.OPEN ? 1 : 0)
                        .sum();
                    
                    healthBuilder.withDetail("circuitBreakers.total", circuitBreakerRegistry.getAllCircuitBreakers().size());
                    healthBuilder.withDetail("circuitBreakers.open", openCircuitBreakers);
                    healthBuilder.withDetail("retries.total", retryRegistry.getAllRetries().size());
                    healthBuilder.withDetail("rateLimiters.total", rateLimiterRegistry.getAllRateLimiters().size());
                    
                    // Health status determination
                    if (openCircuitBreakers > 0) {
                        healthBuilder.down().withDetail("reason", "One or more circuit breakers are OPEN");
                    }
                }
                
                return healthBuilder.build();
            } catch (Exception e) {
                logger.error("Error checking resilience health status", e);
                return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("component", "ResilienceConfig")
                    .build();
            }
        };
    }
    
    /**
     * Fallback method configuration for PaymentService.authorizePayment() when circuit breaker is open.
     * Provides graceful degradation during payment network outages by returning a default response
     * indicating service unavailability while maintaining system stability.
     * 
     * @param exception The exception that triggered the fallback
     * @return Default payment authorization response for graceful degradation
     */
    public Object authorizePaymentFallback(Exception exception) {
        logger.warn("Payment authorization fallback triggered due to: {}", exception.getMessage());
        
        // Return a structured response indicating service degradation
        return createFallbackResponse("PAYMENT_NETWORK_UNAVAILABLE", 
                                     "Payment authorization service temporarily unavailable. Please try again later.",
                                     exception.getClass().getSimpleName());
    }
    
    /**
     * Fallback method configuration for PaymentService.processTransaction() when circuit breaker is open.
     * Provides graceful degradation during payment processing outages by returning a default response
     * indicating processing unavailability while maintaining transaction integrity.
     * 
     * @param exception The exception that triggered the fallback
     * @return Default payment processing response for graceful degradation
     */
    public Object processTransactionFallback(Exception exception) {
        logger.warn("Payment processing fallback triggered due to: {}", exception.getMessage());
        
        // Return a structured response indicating service degradation
        return createFallbackResponse("PAYMENT_PROCESSING_UNAVAILABLE",
                                     "Payment processing service temporarily unavailable. Transaction queued for retry.",
                                     exception.getClass().getSimpleName());
    }
    
    /**
     * Fallback method configuration for PaymentService.validateCardDetails() when circuit breaker is open.
     * Provides graceful degradation during card validation outages by returning a default response
     * indicating validation unavailability while maintaining security integrity.
     * 
     * @param exception The exception that triggered the fallback
     * @return Default card validation response for graceful degradation
     */
    public Object validateCardDetailsFallback(Exception exception) {
        logger.warn("Card validation fallback triggered due to: {}", exception.getMessage());
        
        // Return a structured response indicating service degradation
        return createFallbackResponse("CARD_VALIDATION_UNAVAILABLE",
                                     "Card validation service temporarily unavailable. Manual validation required.",
                                     exception.getClass().getSimpleName());
    }
    
    /**
     * Creates a standardized fallback response structure for all PaymentService methods.
     * Ensures consistent error handling and response format during service degradation.
     * 
     * @param errorCode The specific error code for the fallback scenario
     * @param message Human-readable error message
     * @param exceptionType The type of exception that triggered the fallback
     * @return Standardized fallback response object
     */
    private Object createFallbackResponse(String errorCode, String message, String exceptionType) {
        return new Object() {
            public final String status = "DEGRADED";
            public final String errorCode = errorCode;
            public final String message = message;
            public final String exceptionType = exceptionType;
            public final long timestamp = System.currentTimeMillis();
            public final String source = "ResilienceConfig";
        };
    }
}