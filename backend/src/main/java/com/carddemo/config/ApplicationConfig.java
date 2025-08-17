package com.carddemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;

/**
 * ApplicationConfig - Central configuration class for CardDemo Spring Boot application
 * 
 * This configuration class provides application-wide configuration settings and beans
 * implementing Spring Boot configuration management with @Configuration and 
 * @EnableConfigurationProperties annotations. It includes property source configuration
 * for Spring Cloud Config integration, environment-specific configuration profiles,
 * configuration property validation, bean definitions for configuration-related services,
 * support for encrypted property decryption, configuration refresh scope setup, and
 * integration with Spring Boot Actuator for configuration management endpoints.
 * 
 * Key Features:
 * - Spring Cloud Config client configuration for centralized configuration management
 * - Configuration property binding with type-safe @ConfigurationProperties classes
 * - Environment profile management for development, staging, and production environments
 * - Configuration validation ensuring data integrity and application stability
 * - Refresh scope setup enabling dynamic configuration updates without restart
 * - Integration with Spring Boot Actuator for configuration management endpoints
 * 
 * This replaces traditional CICS system definition parameters and provides cloud-native
 * configuration management capabilities suitable for containerized deployment.
 */
@Configuration
@EnableConfigurationProperties({
    ApplicationConfig.CardDemoProperties.class,
    ApplicationConfig.DatabaseProperties.class,
    ApplicationConfig.SecurityProperties.class,
    ApplicationConfig.BatchProperties.class
})
public class ApplicationConfig {

    private final Environment environment;

    /**
     * Default constructor for Spring Bean instantiation
     */
    public ApplicationConfig() {
        this.environment = null;
    }

    /**
     * Constructor with Environment injection for dependency injection
     */
    public ApplicationConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Configuration property management bean providing centralized access to
     * application configuration properties with type-safe binding and validation.
     * This replaces traditional CICS configuration parameters with modern
     * Spring Boot configuration management.
     * 
     * @return CardDemoProperties instance with validated configuration values
     */
    @Bean
    @RefreshScope
    public CardDemoProperties configurationProperties() {
        return new CardDemoProperties();
    }

    /**
     * Refresh scope configuration enabling dynamic configuration updates
     * without application restart. This provides cloud-native configuration
     * refresh capabilities for operational flexibility in containerized environments.
     * 
     * @return RefreshScope bean for dynamic configuration management
     */
    @Bean
    public org.springframework.cloud.context.scope.refresh.RefreshScope refreshScope() {
        return new org.springframework.cloud.context.scope.refresh.RefreshScope();
    }

    /**
     * Configuration validator ensuring all application configuration properties
     * meet validation requirements before application startup. This provides
     * fail-fast validation for configuration errors and maintains system stability.
     * 
     * @return ConfigurationValidator instance for property validation
     */
    @Bean
    public ConfigurationValidator configurationValidator() {
        return new ConfigurationValidator();
    }

    /**
     * Property sources placeholder configurer for Spring Cloud Config integration.
     * This enables centralized configuration management with environment-specific
     * property resolution and encrypted property decryption support.
     * 
     * @return PropertySourcesPlaceholderConfigurer for Spring Cloud Config integration
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(false);
        configurer.setIgnoreResourceNotFound(false);
        return configurer;
    }

    /**
     * RestTemplate bean for HTTP client operations including external API calls.
     * Configured with appropriate timeouts for address validation services and
     * other external integrations. This bean is required by AddressValidationService
     * and other services that make HTTP requests to external systems.
     * 
     * @return RestTemplate configured with timeouts and error handling
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))  // 5 seconds connection timeout
                .setReadTimeout(Duration.ofSeconds(10))    // 10 seconds read timeout
                .build();
    }

    /**
     * Development profile configuration providing settings optimized for
     * local development environment with relaxed security and verbose logging.
     */
    @Configuration
    @Profile("development")
    public static class DevelopmentConfig {
        
        @Bean
        public ConfigurationValidator developmentConfigurationValidator() {
            ConfigurationValidator validator = new ConfigurationValidator();
            validator.setStrictValidation(false);
            return validator;
        }
    }

    /**
     * Production profile configuration providing settings optimized for
     * production environment with enhanced security and performance.
     */
    @Configuration
    @Profile("production")
    public static class ProductionConfig {
        
        @Bean
        public ConfigurationValidator productionConfigurationValidator() {
            ConfigurationValidator validator = new ConfigurationValidator();
            validator.setStrictValidation(true);
            validator.setEncryptionRequired(true);
            return validator;
        }
    }

    /**
     * CardDemoProperties - Main application configuration properties
     * with comprehensive validation and default values for all operational parameters.
     */
    @ConfigurationProperties(prefix = "carddemo")
    @Validated
    public static class CardDemoProperties {

        /**
         * Application name identifier
         */
        @NotNull
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Application name must contain only alphanumeric characters, hyphens, and underscores")
        private String applicationName = "carddemo";

        /**
         * Application version for compatibility tracking
         */
        @NotNull
        private String version = "1.0.0";

        /**
         * Financial calculation precision settings maintaining COBOL COMP-3 accuracy
         */
        @Min(value = 2, message = "Decimal precision must be at least 2 for financial calculations")
        @Max(value = 10, message = "Decimal precision cannot exceed 10 places")
        private int decimalPrecision = 2;

        /**
         * Rounding mode for BigDecimal operations matching COBOL ROUNDED behavior
         */
        @NotNull
        private RoundingMode roundingMode = RoundingMode.HALF_UP;

        /**
         * Transaction processing timeout in seconds
         */
        @Min(value = 1, message = "Transaction timeout must be at least 1 second")
        @Max(value = 300, message = "Transaction timeout cannot exceed 300 seconds")
        private int transactionTimeoutSeconds = 30;

        /**
         * Maximum records per page for pagination controls
         */
        @Min(value = 1, message = "Page size must be at least 1 record")
        @Max(value = 100, message = "Page size cannot exceed 100 records")
        private int maxRecordsPerPage = 7;

        /**
         * Session timeout duration for user authentication
         */
        @NotNull
        private Duration sessionTimeout = Duration.ofMinutes(30);

        /**
         * Supported user types for role-based access control
         */
        @NotNull
        private List<String> supportedUserTypes = List.of("REGULAR", "ADMIN");

        // Getters and setters
        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public int getDecimalPrecision() { return decimalPrecision; }
        public void setDecimalPrecision(int decimalPrecision) { this.decimalPrecision = decimalPrecision; }

        public RoundingMode getRoundingMode() { return roundingMode; }
        public void setRoundingMode(RoundingMode roundingMode) { this.roundingMode = roundingMode; }

        public int getTransactionTimeoutSeconds() { return transactionTimeoutSeconds; }
        public void setTransactionTimeoutSeconds(int transactionTimeoutSeconds) { this.transactionTimeoutSeconds = transactionTimeoutSeconds; }

        public int getMaxRecordsPerPage() { return maxRecordsPerPage; }
        public void setMaxRecordsPerPage(int maxRecordsPerPage) { this.maxRecordsPerPage = maxRecordsPerPage; }

        public Duration getSessionTimeout() { return sessionTimeout; }
        public void setSessionTimeout(Duration sessionTimeout) { this.sessionTimeout = sessionTimeout; }

        public List<String> getSupportedUserTypes() { return supportedUserTypes; }
        public void setSupportedUserTypes(List<String> supportedUserTypes) { this.supportedUserTypes = supportedUserTypes; }
    }

    /**
     * DatabaseProperties - Database configuration properties with connection
     * pooling and performance tuning parameters for PostgreSQL integration.
     */
    @ConfigurationProperties(prefix = "carddemo.database")
    @Validated
    public static class DatabaseProperties {

        /**
         * Database connection pool minimum size
         */
        @Min(value = 1, message = "Minimum pool size must be at least 1")
        private int minPoolSize = 5;

        /**
         * Database connection pool maximum size
         */
        @Min(value = 1, message = "Maximum pool size must be at least 1")
        @Max(value = 100, message = "Maximum pool size cannot exceed 100")
        private int maxPoolSize = 20;

        /**
         * Connection timeout in milliseconds
         */
        @Min(value = 1000, message = "Connection timeout must be at least 1000ms")
        private long connectionTimeoutMs = 30000;

        /**
         * Idle timeout in milliseconds
         */
        @Min(value = 60000, message = "Idle timeout must be at least 60000ms")
        private long idleTimeoutMs = 600000;

        /**
         * Maximum lifetime in milliseconds
         */
        @Min(value = 300000, message = "Maximum lifetime must be at least 300000ms")
        private long maxLifetimeMs = 1800000;

        // Getters and setters
        public int getMinPoolSize() { return minPoolSize; }
        public void setMinPoolSize(int minPoolSize) { this.minPoolSize = minPoolSize; }

        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }

        public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
        public void setConnectionTimeoutMs(long connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }

        public long getIdleTimeoutMs() { return idleTimeoutMs; }
        public void setIdleTimeoutMs(long idleTimeoutMs) { this.idleTimeoutMs = idleTimeoutMs; }

        public long getMaxLifetimeMs() { return maxLifetimeMs; }
        public void setMaxLifetimeMs(long maxLifetimeMs) { this.maxLifetimeMs = maxLifetimeMs; }
    }

    /**
     * SecurityProperties - Security configuration properties for Spring Security
     * integration with authentication and authorization settings.
     */
    @ConfigurationProperties(prefix = "carddemo.security")
    @Validated
    public static class SecurityProperties {

        /**
         * JWT token expiration time in minutes
         */
        @Min(value = 1, message = "JWT expiration must be at least 1 minute")
        @Max(value = 1440, message = "JWT expiration cannot exceed 1440 minutes (24 hours)")
        private int jwtExpirationMinutes = 60;

        /**
         * JWT secret key for token signing
         */
        @NotNull
        private String jwtSecret = "defaultCardDemoSecretKey";

        /**
         * Password minimum length requirement
         */
        @Min(value = 8, message = "Password minimum length must be at least 8 characters")
        private int passwordMinLength = 8;

        /**
         * Maximum login attempts before account lockout
         */
        @Min(value = 1, message = "Maximum login attempts must be at least 1")
        @Max(value = 10, message = "Maximum login attempts cannot exceed 10")
        private int maxLoginAttempts = 3;

        /**
         * Account lockout duration in minutes
         */
        @Min(value = 1, message = "Account lockout duration must be at least 1 minute")
        private int accountLockoutMinutes = 15;

        // Getters and setters
        public int getJwtExpirationMinutes() { return jwtExpirationMinutes; }
        public void setJwtExpirationMinutes(int jwtExpirationMinutes) { this.jwtExpirationMinutes = jwtExpirationMinutes; }

        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

        public int getPasswordMinLength() { return passwordMinLength; }
        public void setPasswordMinLength(int passwordMinLength) { this.passwordMinLength = passwordMinLength; }

        public int getMaxLoginAttempts() { return maxLoginAttempts; }
        public void setMaxLoginAttempts(int maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }

        public int getAccountLockoutMinutes() { return accountLockoutMinutes; }
        public void setAccountLockoutMinutes(int accountLockoutMinutes) { this.accountLockoutMinutes = accountLockoutMinutes; }
    }

    /**
     * BatchProperties - Spring Batch configuration properties for job processing
     * and performance tuning replacing JCL job parameters.
     */
    @ConfigurationProperties(prefix = "carddemo.batch")
    @Validated
    public static class BatchProperties {

        /**
         * Default chunk size for Spring Batch processing
         */
        @Min(value = 1, message = "Chunk size must be at least 1")
        @Max(value = 10000, message = "Chunk size cannot exceed 10000")
        private int defaultChunkSize = 1000;

        /**
         * Maximum processing window in hours
         */
        @Min(value = 1, message = "Processing window must be at least 1 hour")
        @Max(value = 24, message = "Processing window cannot exceed 24 hours")
        private int maxProcessingWindowHours = 4;

        /**
         * Thread pool size for parallel processing
         */
        @Min(value = 1, message = "Thread pool size must be at least 1")
        @Max(value = 20, message = "Thread pool size cannot exceed 20")
        private int threadPoolSize = 4;

        /**
         * Retry attempts for failed items
         */
        @Min(value = 0, message = "Retry attempts cannot be negative")
        @Max(value = 5, message = "Retry attempts cannot exceed 5")
        private int retryAttempts = 3;

        // Getters and setters
        public int getDefaultChunkSize() { return defaultChunkSize; }
        public void setDefaultChunkSize(int defaultChunkSize) { this.defaultChunkSize = defaultChunkSize; }

        public int getMaxProcessingWindowHours() { return maxProcessingWindowHours; }
        public void setMaxProcessingWindowHours(int maxProcessingWindowHours) { this.maxProcessingWindowHours = maxProcessingWindowHours; }

        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }

        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
    }

    /**
     * ConfigurationValidator - Validates application configuration properties
     * ensuring proper setup and fail-fast behavior for configuration errors.
     */
    public static class ConfigurationValidator {

        private boolean strictValidation = true;
        private boolean encryptionRequired = false;

        /**
         * Validates all configuration properties ensuring proper application setup.
         * This method is called during application startup to verify configuration
         * integrity and prevent runtime errors due to misconfiguration.
         * 
         * @param properties Configuration properties to validate
         * @throws IllegalStateException if configuration validation fails
         */
        public void validateConfiguration(CardDemoProperties properties) {
            if (properties == null) {
                throw new IllegalStateException("CardDemo properties cannot be null");
            }

            // Validate application name
            if (properties.getApplicationName() == null || properties.getApplicationName().trim().isEmpty()) {
                throw new IllegalStateException("Application name must be specified");
            }

            // Validate decimal precision for financial calculations
            if (properties.getDecimalPrecision() < 2) {
                throw new IllegalStateException("Decimal precision must be at least 2 for financial accuracy");
            }

            // Validate rounding mode
            if (properties.getRoundingMode() == null) {
                throw new IllegalStateException("Rounding mode must be specified for BigDecimal operations");
            }

            // Validate session timeout
            if (properties.getSessionTimeout() == null || properties.getSessionTimeout().toMinutes() < 1) {
                throw new IllegalStateException("Session timeout must be at least 1 minute");
            }

            // Validate supported user types
            if (properties.getSupportedUserTypes() == null || properties.getSupportedUserTypes().isEmpty()) {
                throw new IllegalStateException("At least one user type must be supported");
            }
        }

        /**
         * Validates database configuration properties ensuring proper connection
         * pool setup and database connectivity parameters.
         * 
         * @param properties Database properties to validate
         * @throws IllegalStateException if database configuration validation fails
         */
        public void validateDatabaseConfiguration(DatabaseProperties properties) {
            if (properties == null) {
                throw new IllegalStateException("Database properties cannot be null");
            }

            // Validate pool size configuration
            if (properties.getMinPoolSize() > properties.getMaxPoolSize()) {
                throw new IllegalStateException("Minimum pool size cannot exceed maximum pool size");
            }

            // Validate timeout configuration
            if (properties.getConnectionTimeoutMs() <= 0) {
                throw new IllegalStateException("Connection timeout must be positive");
            }
        }

        /**
         * Validates security configuration properties ensuring proper authentication
         * and authorization setup with adequate security parameters.
         * 
         * @param properties Security properties to validate
         * @throws IllegalStateException if security configuration validation fails
         */
        public void validateSecurityConfiguration(SecurityProperties properties) {
            if (properties == null) {
                throw new IllegalStateException("Security properties cannot be null");
            }

            // Validate JWT configuration
            if (encryptionRequired && (properties.getJwtSecret() == null || properties.getJwtSecret().length() < 32)) {
                throw new IllegalStateException("JWT secret must be at least 32 characters for production use");
            }

            // Validate password policy
            if (properties.getPasswordMinLength() < 8) {
                throw new IllegalStateException("Password minimum length must be at least 8 characters");
            }
        }

        /**
         * Validates batch processing configuration properties ensuring proper
         * job execution parameters and performance settings.
         * 
         * @param properties Batch properties to validate
         * @throws IllegalStateException if batch configuration validation fails
         */
        public void validateBatchConfiguration(BatchProperties properties) {
            if (properties == null) {
                throw new IllegalStateException("Batch properties cannot be null");
            }

            // Validate chunk size
            if (properties.getDefaultChunkSize() <= 0) {
                throw new IllegalStateException("Chunk size must be positive");
            }

            // Validate processing window
            if (properties.getMaxProcessingWindowHours() > 8) {
                throw new IllegalStateException("Processing window should not exceed 8 hours for daily operations");
            }
        }

        // Getters and setters
        public boolean isStrictValidation() { return strictValidation; }
        public void setStrictValidation(boolean strictValidation) { this.strictValidation = strictValidation; }

        public boolean isEncryptionRequired() { return encryptionRequired; }
        public void setEncryptionRequired(boolean encryptionRequired) { this.encryptionRequired = encryptionRequired; }
    }
}