package com.carddemo.service;

import com.carddemo.repository.ConfigurationRepository;
import com.carddemo.entity.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * Service class providing centralized configuration management capabilities including property loading,
 * environment profile management, dynamic configuration updates, encrypted property handling, and 
 * configuration validation with Spring Cloud Config integration.
 * 
 * This service implements comprehensive configuration management functionality for the modernized
 * CardDemo application including:
 * - Property resolution and loading from multiple sources
 * - Environment-specific configuration profiles (DEV, TEST, PROD)
 * - Dynamic configuration refresh capabilities without application restart
 * - Encrypted property decryption support with version management
 * - Configuration validation and change notifications
 * - Integration with Spring Cloud Config server for centralized management
 * - Property override hierarchy management for flexible configuration
 * - Fallback and default value handling for missing properties
 * - Configuration rollback mechanisms for change safety
 * - Secure handling of sensitive configuration data with audit logging
 * 
 * Integration points:
 * - ConfigurationRepository for persistence and CRUD operations
 * - EncryptionService for secure property handling and sensitive data masking
 * - Spring Cloud Config for centralized configuration management
 * - Spring Environment for property source management
 * - Spring Event framework for configuration change notifications
 * 
 * Supports the migration from COBOL/CICS configuration management to cloud-native
 * configuration services while maintaining identical configuration access patterns
 * and providing enhanced security, audit capabilities, and operational flexibility.
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since Spring Boot 3.2.x
 */
@Service
@RefreshScope
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    // Configuration management constants
    private static final String CONFIG_SOURCE_NAME = "carddemo-config";
    private static final String DEFAULT_PROFILE = "default";
    private static final String ENCRYPTED_PREFIX = "{cipher}";
    private static final String ROLLBACK_SUFFIX = ".rollback";
    private static final Pattern SENSITIVE_PROPERTY_PATTERN = 
        Pattern.compile(".*(?:password|secret|key|token|credential).*", Pattern.CASE_INSENSITIVE);

    // Dependency injection
    private final ConfigurationRepository configurationRepository;
    private final EncryptionService encryptionService;
    private final Environment environment;
    private final ApplicationEventPublisher eventPublisher;

    // Configuration cache and state management
    private final Map<String, Object> configurationCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> configurationTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Object> rollbackSnapshots = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock();

    // Configuration settings
    @Value("${carddemo.config.cache-ttl-minutes:30}")
    private int cacheTtlMinutes;

    @Value("${carddemo.config.enable-audit:true}")
    private boolean auditEnabled;

    @Value("${carddemo.config.validation-enabled:true}")
    private boolean validationEnabled;

    @Value("${carddemo.config.refresh-enabled:true}")
    private boolean refreshEnabled;

    // State tracking
    private volatile boolean initialized = false;
    private volatile LocalDateTime lastRefreshTime;
    private String currentProfile;

    /**
     * Constructor for ConfigurationService with required dependencies.
     * Initializes configuration management with repository and encryption services.
     *
     * @param configurationRepository Repository for configuration data persistence
     * @param encryptionService Service for encryption/decryption operations  
     * @param environment Spring Environment for property access
     * @param eventPublisher Application event publisher for notifications
     */
    @Autowired
    public ConfigurationService(ConfigurationRepository configurationRepository,
                              EncryptionService encryptionService,
                              Environment environment,
                              ApplicationEventPublisher eventPublisher) {
        this.configurationRepository = configurationRepository;
        this.encryptionService = encryptionService;
        this.environment = environment;
        this.eventPublisher = eventPublisher;
        this.lastRefreshTime = LocalDateTime.now();
    }

    /**
     * Initializes the configuration service with default properties and environment setup.
     * Called during application startup to establish baseline configuration state.
     */
    @PostConstruct
    public void initializeConfigurationService() {
        try {
            logger.info("Initializing ConfigurationService...");

            // Determine current active profile
            currentProfile = determineActiveProfile();
            logger.info("Active configuration profile: {}", currentProfile);

            // Load initial configuration properties
            loadProperties();

            // Validate configuration state
            if (validationEnabled) {
                validateConfiguration();
            }

            initialized = true;
            lastRefreshTime = LocalDateTime.now();

            logger.info("ConfigurationService initialized successfully for profile: {}", currentProfile);
            auditConfigurationOperation("SERVICE_INIT", "ConfigurationService", 
                                      "Service initialized for profile: " + currentProfile, true);

        } catch (Exception e) {
            logger.error("Failed to initialize ConfigurationService", e);
            auditConfigurationOperation("SERVICE_INIT", "ConfigurationService", 
                                      "Service initialization failed: " + e.getMessage(), false);
            throw new RuntimeException("ConfigurationService initialization failed", e);
        }
    }

    /**
     * Loads configuration properties from all available sources including database,
     * Spring Cloud Config, and environment variables with proper precedence handling.
     * 
     * @return Map containing all loaded configuration properties
     * @throws RuntimeException if property loading fails critically
     */
    @Transactional(readOnly = true)
    public Map<String, Object> loadProperties() {
        configLock.writeLock().lock();
        try {
            logger.debug("Loading configuration properties for profile: {}", currentProfile);

            Map<String, Object> loadedProperties = new HashMap<>();

            // Load from database repository
            loadDatabaseProperties(loadedProperties);

            // Load from Spring Environment (includes Cloud Config)
            loadEnvironmentProperties(loadedProperties);

            // Process encrypted properties
            processEncryptedProperties(loadedProperties);

            // Update cache with loaded properties
            configurationCache.clear();
            configurationCache.putAll(loadedProperties);

            // Update timestamps
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            for (String key : loadedProperties.keySet()) {
                configurationTimestamps.put(key, LocalDateTime.now());
            }

            logger.info("Loaded {} configuration properties for profile: {}", 
                       loadedProperties.size(), currentProfile);
            auditConfigurationOperation("LOAD_PROPERTIES", currentProfile, 
                                      "Loaded " + loadedProperties.size() + " properties", true);

            return new HashMap<>(loadedProperties);

        } catch (Exception e) {
            logger.error("Failed to load configuration properties", e);
            auditConfigurationOperation("LOAD_PROPERTIES", currentProfile, 
                                      "Property loading failed: " + e.getMessage(), false);
            throw new RuntimeException("Configuration property loading failed", e);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Refreshes configuration from all sources dynamically without application restart.
     * Supports Spring Cloud Config refresh events and manual refresh operations.
     * 
     * @return true if refresh was successful, false otherwise
     * @throws RuntimeException if refresh operation fails critically
     */
    public boolean refreshConfiguration() {
        if (!refreshEnabled) {
            logger.info("Configuration refresh is disabled");
            return false;
        }

        configLock.writeLock().lock();
        try {
            logger.info("Refreshing configuration for profile: {}", currentProfile);

            // Create rollback snapshot before refresh
            createRollbackSnapshot();

            // Reload properties from all sources
            Map<String, Object> refreshedProperties = loadProperties();

            // Validate refreshed configuration
            if (validationEnabled) {
                boolean validationResult = validateConfiguration();
                if (!validationResult) {
                    logger.warn("Configuration validation failed after refresh, rolling back");
                    rollbackConfiguration();
                    return false;
                }
            }

            lastRefreshTime = LocalDateTime.now();

            // Publish refresh event for other components
            notifyConfigurationChange("REFRESH", refreshedProperties.keySet());

            logger.info("Configuration refresh completed successfully. {} properties refreshed", 
                       refreshedProperties.size());
            auditConfigurationOperation("REFRESH", currentProfile, 
                                      "Configuration refreshed successfully", true);

            return true;

        } catch (Exception e) {
            logger.error("Configuration refresh failed", e);
            auditConfigurationOperation("REFRESH", currentProfile, 
                                      "Configuration refresh failed: " + e.getMessage(), false);

            // Attempt rollback on failure
            try {
                rollbackConfiguration();
            } catch (Exception rollbackException) {
                logger.error("Rollback also failed after refresh failure", rollbackException);
            }

            throw new RuntimeException("Configuration refresh failed", e);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Decrypts encrypted property values using the encryption service with version handling.
     * Supports multiple encryption formats and provides fallback for non-encrypted values.
     * 
     * @param encryptedProperty The encrypted property value to decrypt
     * @return The decrypted plaintext value
     * @throws IllegalArgumentException if encrypted property is null or malformed
     * @throws RuntimeException if decryption operation fails
     */
    public String decryptProperty(String encryptedProperty) {
        if (encryptedProperty == null) {
            throw new IllegalArgumentException("Encrypted property cannot be null");
        }

        try {
            // Check if property is actually encrypted
            if (!encryptionService.isEncrypted(encryptedProperty)) {
                logger.debug("Property is not encrypted, returning as-is");
                return encryptedProperty;
            }

            // Perform decryption using encryption service
            String decryptedValue = encryptionService.decrypt(encryptedProperty);

            logger.debug("Successfully decrypted property");
            auditConfigurationOperation("DECRYPT", "property", "Property decrypted successfully", true);

            return decryptedValue;

        } catch (Exception e) {
            logger.error("Failed to decrypt property", e);
            auditConfigurationOperation("DECRYPT", "property", 
                                      "Property decryption failed: " + e.getMessage(), false);
            throw new RuntimeException("Property decryption failed", e);
        }
    }

    /**
     * Validates configuration consistency, security requirements, and business rules.
     * Performs comprehensive validation including type checking and constraint validation.
     * 
     * @return true if all configurations are valid, false otherwise
     * @throws RuntimeException if validation process fails
     */
    public boolean validateConfiguration() {
        configLock.readLock().lock();
        try {
            logger.debug("Validating configuration for profile: {}", currentProfile);

            boolean isValid = true;
            List<String> validationErrors = new ArrayList<>();

            // Validate required properties are present
            isValid &= validateRequiredProperties(validationErrors);

            // Validate property types and formats  
            isValid &= validatePropertyFormats(validationErrors);

            // Validate security requirements
            isValid &= validateSecurityRequirements(validationErrors);

            // Validate business rules and constraints
            isValid &= validateBusinessRules(validationErrors);

            if (!isValid) {
                logger.warn("Configuration validation failed. Errors: {}", validationErrors);
                auditConfigurationOperation("VALIDATE", currentProfile, 
                                          "Validation failed: " + String.join("; ", validationErrors), false);
            } else {
                logger.info("Configuration validation passed for profile: {}", currentProfile);
                auditConfigurationOperation("VALIDATE", currentProfile, "Configuration validation passed", true);
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Configuration validation error", e);
            auditConfigurationOperation("VALIDATE", currentProfile, 
                                      "Validation error: " + e.getMessage(), false);
            throw new RuntimeException("Configuration validation failed", e);
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Rolls back configuration to the previous snapshot state for change safety.
     * Restores configuration from backup snapshot and validates rollback state.
     * 
     * @return true if rollback was successful, false otherwise
     * @throws RuntimeException if rollback operation fails
     */
    public boolean rollbackConfiguration() {
        configLock.writeLock().lock();
        try {
            logger.info("Rolling back configuration for profile: {}", currentProfile);

            if (rollbackSnapshots.isEmpty()) {
                logger.warn("No rollback snapshot available for profile: {}", currentProfile);
                auditConfigurationOperation("ROLLBACK", currentProfile, "No rollback snapshot available", false);
                return false;
            }

            // Restore from rollback snapshot
            configurationCache.clear();
            configurationCache.putAll(rollbackSnapshots);

            // Clear rollback snapshot after use
            rollbackSnapshots.clear();

            // Validate rolled back configuration
            boolean validationResult = true;
            if (validationEnabled) {
                validationResult = validateConfiguration();
            }

            if (validationResult) {
                lastRefreshTime = LocalDateTime.now();
                logger.info("Configuration rollback completed successfully for profile: {}", currentProfile);
                auditConfigurationOperation("ROLLBACK", currentProfile, "Configuration rolled back successfully", true);

                // Notify other components of rollback
                notifyConfigurationChange("ROLLBACK", configurationCache.keySet());
            } else {
                logger.error("Rolled back configuration failed validation");
                auditConfigurationOperation("ROLLBACK", currentProfile, "Rolled back configuration failed validation", false);
            }

            return validationResult;

        } catch (Exception e) {
            logger.error("Configuration rollback failed", e);
            auditConfigurationOperation("ROLLBACK", currentProfile, 
                                      "Configuration rollback failed: " + e.getMessage(), false);
            throw new RuntimeException("Configuration rollback failed", e);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves a configuration property value by key with environment resolution and type conversion.
     * Supports property hierarchy, default values, and automatic decryption of encrypted properties.
     * 
     * @param propertyKey The configuration property key to retrieve
     * @return The property value, or null if not found
     * @throws IllegalArgumentException if property key is null or empty
     */
    public Object getProperty(String propertyKey) {
        if (propertyKey == null || propertyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Property key cannot be null or empty");
        }

        configLock.readLock().lock();
        try {
            logger.debug("Retrieving property: {}", propertyKey);

            // Check cache first
            Object cachedValue = configurationCache.get(propertyKey);
            if (cachedValue != null && !isCacheExpired(propertyKey)) {
                logger.debug("Property found in cache: {}", propertyKey);
                return decryptIfNeeded(cachedValue);
            }

            // Query repository for property
            Optional<Configuration> configEntity = 
                configurationRepository.findByEnvironmentAndName(currentProfile, propertyKey);

            if (configEntity.isPresent()) {
                Object propertyValue = configEntity.get().getValue();
                
                // Update cache
                configurationCache.put(propertyKey, propertyValue);
                configurationTimestamps.put(propertyKey, LocalDateTime.now());

                logger.debug("Property retrieved from repository: {}", propertyKey);
                auditConfigurationOperation("GET_PROPERTY", propertyKey, "Property retrieved successfully", true);

                return decryptIfNeeded(propertyValue);
            }

            // Fallback to Spring Environment
            String envValue = environment.getProperty(propertyKey);
            if (envValue != null) {
                logger.debug("Property retrieved from environment: {}", propertyKey);
                return decryptIfNeeded(envValue);
            }

            logger.debug("Property not found: {}", propertyKey);
            auditConfigurationOperation("GET_PROPERTY", propertyKey, "Property not found", false);
            return null;

        } catch (Exception e) {
            logger.error("Failed to retrieve property: {}", propertyKey, e);
            auditConfigurationOperation("GET_PROPERTY", propertyKey, 
                                      "Property retrieval failed: " + e.getMessage(), false);
            throw new RuntimeException("Property retrieval failed for key: " + propertyKey, e);
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Sets a configuration property value with persistence, validation, and change notification.
     * Supports property encryption, versioning, and audit logging for secure configuration management.
     * 
     * @param propertyKey The configuration property key to set
     * @param propertyValue The value to set for the property
     * @return true if property was set successfully, false otherwise
     * @throws IllegalArgumentException if property key is null or empty
     * @throws RuntimeException if property setting operation fails
     */
    @Transactional
    public boolean setProperty(String propertyKey, Object propertyValue) {
        if (propertyKey == null || propertyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Property key cannot be null or empty");
        }

        configLock.writeLock().lock();
        try {
            logger.debug("Setting property: {}", propertyKey);

            // Validate property before setting
            if (validationEnabled && !validatePropertyValue(propertyKey, propertyValue)) {
                logger.warn("Property validation failed for key: {}", propertyKey);
                auditConfigurationOperation("SET_PROPERTY", propertyKey, "Property validation failed", false);
                return false;
            }

            // Encrypt sensitive properties
            Object valueToStore = propertyValue;
            if (isSensitiveProperty(propertyKey) && propertyValue instanceof String) {
                try {
                    valueToStore = encryptionService.encrypt((String) propertyValue);
                    logger.debug("Property encrypted before storage: {}", propertyKey);
                } catch (Exception e) {
                    logger.warn("Failed to encrypt sensitive property: {}", propertyKey, e);
                    // Continue with unencrypted value if encryption fails
                }
            }

            // Find existing configuration or create new
            Optional<Configuration> existingConfig = 
                configurationRepository.findByEnvironmentAndName(currentProfile, propertyKey);

            Configuration configEntity;
            if (existingConfig.isPresent()) {
                configEntity = existingConfig.get();
                configEntity.setValue(valueToStore.toString());
                configEntity.setLastModified(LocalDateTime.now());
            } else {
                // Create new configuration entity
                configEntity = createNewConfigurationEntity(propertyKey, valueToStore);
            }

            // Save to repository
            configurationRepository.save(configEntity);

            // Update cache
            configurationCache.put(propertyKey, valueToStore);
            configurationTimestamps.put(propertyKey, LocalDateTime.now());

            logger.info("Property set successfully: {}", propertyKey);
            auditConfigurationOperation("SET_PROPERTY", propertyKey, "Property set successfully", true);

            // Notify other components of configuration change
            notifyConfigurationChange("SET_PROPERTY", Collections.singleton(propertyKey));

            return true;

        } catch (Exception e) {
            logger.error("Failed to set property: {}", propertyKey, e);
            auditConfigurationOperation("SET_PROPERTY", propertyKey, 
                                      "Property setting failed: " + e.getMessage(), false);
            throw new RuntimeException("Property setting failed for key: " + propertyKey, e);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the currently active configuration profiles including environment-specific profiles.
     * Returns ordered list of profiles with precedence information for configuration resolution.
     * 
     * @return List of active configuration profiles in precedence order
     */
    public List<String> getActiveProfiles() {
        try {
            logger.debug("Retrieving active configuration profiles");

            List<String> activeProfiles = new ArrayList<>();

            // Add default profile
            activeProfiles.add(DEFAULT_PROFILE);

            // Add Spring active profiles
            String[] springProfiles = environment.getActiveProfiles();
            if (springProfiles.length > 0) {
                activeProfiles.addAll(Arrays.asList(springProfiles));
            } else {
                // Fallback to default profiles
                String[] defaultProfiles = environment.getDefaultProfiles();
                activeProfiles.addAll(Arrays.asList(defaultProfiles));
            }

            // Add current profile if not already included
            if (!activeProfiles.contains(currentProfile)) {
                activeProfiles.add(currentProfile);
            }

            // Remove duplicates while preserving order
            List<String> uniqueProfiles = new ArrayList<>();
            for (String profile : activeProfiles) {
                if (!uniqueProfiles.contains(profile)) {
                    uniqueProfiles.add(profile);
                }
            }

            logger.debug("Active profiles: {}", uniqueProfiles);
            auditConfigurationOperation("GET_PROFILES", "system", 
                                      "Retrieved active profiles: " + uniqueProfiles, true);

            return uniqueProfiles;

        } catch (Exception e) {
            logger.error("Failed to retrieve active profiles", e);
            auditConfigurationOperation("GET_PROFILES", "system", 
                                      "Failed to retrieve active profiles: " + e.getMessage(), false);
            throw new RuntimeException("Failed to retrieve active profiles", e);
        }
    }

    /**
     * Publishes configuration change notifications to other application components.
     * Integrates with Spring event framework for loose coupling and extensibility.
     * 
     * @param changeType The type of configuration change (REFRESH, SET_PROPERTY, etc.)
     * @param changedProperties Set of property keys that were changed
     * @throws RuntimeException if notification publishing fails
     */
    public void notifyConfigurationChange(String changeType, Set<String> changedProperties) {
        try {
            logger.debug("Publishing configuration change notification: {} for {} properties", 
                        changeType, changedProperties.size());

            // Create configuration change event
            ConfigurationChangeEvent changeEvent = new ConfigurationChangeEvent(
                this, changeType, changedProperties, currentProfile, LocalDateTime.now());

            // Publish event through Spring event framework
            eventPublisher.publishEvent(changeEvent);

            // Also publish Spring Cloud refresh event if this is a refresh operation
            if ("REFRESH".equals(changeType)) {
                eventPublisher.publishEvent(new RefreshEvent(this, null, "Configuration refreshed"));
            }

            logger.debug("Configuration change notification published successfully");
            auditConfigurationOperation("NOTIFY_CHANGE", changeType, 
                                      "Change notification published for " + changedProperties.size() + " properties", true);

        } catch (Exception e) {
            logger.error("Failed to publish configuration change notification", e);
            auditConfigurationOperation("NOTIFY_CHANGE", changeType, 
                                      "Change notification failed: " + e.getMessage(), false);
            throw new RuntimeException("Configuration change notification failed", e);
        }
    }

    /**
     * Retrieves default value for a configuration property with type-safe conversion.
     * Supports fallback values and environment-specific defaults with proper type handling.
     * 
     * @param propertyKey The configuration property key
     * @param defaultValue The default value to return if property is not found
     * @param targetType The expected type for type conversion
     * @param <T> The generic type parameter for type safety
     * @return The property value or default value with proper type conversion
     * @throws IllegalArgumentException if property key is null or empty
     */
    @SuppressWarnings("unchecked")
    public <T> T getDefaultValue(String propertyKey, T defaultValue, Class<T> targetType) {
        if (propertyKey == null || propertyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Property key cannot be null or empty");
        }

        try {
            logger.debug("Retrieving property with default: {}", propertyKey);

            // First try to get the actual property value
            Object propertyValue = getProperty(propertyKey);

            if (propertyValue != null) {
                // Convert to target type if necessary
                return convertToType(propertyValue, targetType);
            }

            // Return default value if property not found
            logger.debug("Property not found, returning default value for: {}", propertyKey);
            auditConfigurationOperation("GET_DEFAULT", propertyKey, "Returned default value", true);

            return defaultValue;

        } catch (Exception e) {
            logger.error("Failed to retrieve property with default: {}", propertyKey, e);
            auditConfigurationOperation("GET_DEFAULT", propertyKey, 
                                      "Failed to retrieve property with default: " + e.getMessage(), false);
            
            // Return default value on error
            return defaultValue;
        }
    }

    /**
     * Overloaded method for getDefaultValue without explicit type parameter.
     * 
     * @param propertyKey The configuration property key
     * @param defaultValue The default value to return if property is not found
     * @return The property value or default value
     */
    public Object getDefaultValue(String propertyKey, Object defaultValue) {
        return getDefaultValue(propertyKey, defaultValue, Object.class);
    }

    /**
     * Masks sensitive configuration data for secure display and logging purposes.
     * Implements comprehensive masking patterns for various sensitive data types with audit compliance.
     * 
     * @param configurationData The configuration data to mask
     * @return The masked configuration data with sensitive information obscured
     * @throws IllegalArgumentException if configuration data is null
     */
    public Object maskSensitiveData(Object configurationData) {
        if (configurationData == null) {
            throw new IllegalArgumentException("Configuration data cannot be null");
        }

        try {
            // Handle different data types
            if (configurationData instanceof String) {
                String maskedValue = encryptionService.maskSensitiveProperty((String) configurationData);
                logger.debug("String configuration data masked");
                auditConfigurationOperation("MASK_DATA", "string", "Configuration data masked", true);
                return maskedValue;
            } 
            else if (configurationData instanceof Map) {
                return maskSensitiveMap((Map<?, ?>) configurationData);
            }
            else if (configurationData instanceof Collection) {
                return maskSensitiveCollection((Collection<?>) configurationData);
            }
            else {
                // For other types, convert to string and mask
                String stringValue = configurationData.toString();
                String maskedValue = encryptionService.maskSensitiveProperty(stringValue);
                logger.debug("Object configuration data masked");
                auditConfigurationOperation("MASK_DATA", "object", "Configuration data masked", true);
                return maskedValue;
            }

        } catch (Exception e) {
            logger.error("Failed to mask sensitive configuration data", e);
            auditConfigurationOperation("MASK_DATA", "unknown", 
                                      "Failed to mask configuration data: " + e.getMessage(), false);
            return "***MASKED***"; // Safe fallback
        }
    }

    // ===================== EVENT LISTENERS =====================

    /**
     * Handles application ready events to perform final initialization.
     * 
     * @param event The application ready event
     */
    @EventListener
    public void handleApplicationReadyEvent(ApplicationReadyEvent event) {
        logger.info("Application ready, finalizing configuration service initialization");
        if (!initialized) {
            initializeConfigurationService();
        }
    }

    /**
     * Handles refresh events from Spring Cloud Config.
     * 
     * @param event The refresh event
     */
    @EventListener
    public void handleRefreshEvent(RefreshEvent event) {
        logger.info("Received configuration refresh event");
        if (refreshEnabled) {
            refreshConfiguration();
        }
    }

    // ===================== PRIVATE HELPER METHODS =====================

    /**
     * Determines the active configuration profile from Spring Environment.
     * 
     * @return The active configuration profile
     */
    private String determineActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[0]; // Use first active profile
        }
        
        String[] defaultProfiles = environment.getDefaultProfiles();
        if (defaultProfiles.length > 0) {
            return defaultProfiles[0]; // Use first default profile
        }
        
        return DEFAULT_PROFILE;
    }

    /**
     * Loads configuration properties from database repository.
     * 
     * @param loadedProperties Map to populate with loaded properties
     */
    private void loadDatabaseProperties(Map<String, Object> loadedProperties) {
        try {
            List<Configuration> configurations = 
                configurationRepository.findByEnvironment(currentProfile);
            
            for (com.carddemo.entity.Configuration config : configurations) {
                loadedProperties.put(config.getName(), config.getValue());
            }
            
            logger.debug("Loaded {} properties from database", configurations.size());
        } catch (Exception e) {
            logger.warn("Failed to load properties from database", e);
        }
    }

    /**
     * Loads configuration properties from Spring Environment.
     * 
     * @param loadedProperties Map to populate with loaded properties
     */
    private void loadEnvironmentProperties(Map<String, Object> loadedProperties) {
        try {
            // Get all property sources from environment
            if (environment instanceof ConfigurableEnvironment) {
                ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
                MutablePropertySources propertySources = configurableEnv.getPropertySources();
                
                for (PropertySource<?> propertySource : propertySources) {
                    if (propertySource instanceof MapPropertySource) {
                        MapPropertySource mapPropertySource = (MapPropertySource) propertySource;
                        String[] propertyNames = mapPropertySource.getPropertyNames();
                        
                        for (String propertyName : propertyNames) {
                            Object value = mapPropertySource.getProperty(propertyName);
                            if (value != null) {
                                loadedProperties.put(propertyName, value);
                            }
                        }
                    }
                }
            }
            
            logger.debug("Loaded properties from Spring Environment");
        } catch (Exception e) {
            logger.warn("Failed to load properties from Spring Environment", e);
        }
    }

    /**
     * Processes encrypted properties in the loaded configuration.
     * 
     * @param loadedProperties Map containing properties to process
     */
    private void processEncryptedProperties(Map<String, Object> loadedProperties) {
        loadedProperties.entrySet().removeIf(entry -> {
            try {
                Object value = entry.getValue();
                if (value instanceof String && ((String) value).startsWith(ENCRYPTED_PREFIX)) {
                    String decryptedValue = decryptProperty((String) value);
                    entry.setValue(decryptedValue);
                }
                return false;
            } catch (Exception e) {
                logger.error("Failed to decrypt property: {}", entry.getKey(), e);
                return true; // Remove problematic property
            }
        });
    }

    /**
     * Creates a rollback snapshot of current configuration.
     */
    private void createRollbackSnapshot() {
        rollbackSnapshots.clear();
        rollbackSnapshots.putAll(configurationCache);
        logger.debug("Created rollback snapshot with {} properties", rollbackSnapshots.size());
    }

    /**
     * Validates required properties are present.
     * 
     * @param validationErrors List to collect validation errors
     * @return true if validation passes
     */
    private boolean validateRequiredProperties(List<String> validationErrors) {
        // Define required properties for CardDemo application
        String[] requiredProperties = {
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.redis.host",
            "carddemo.security.jwt.secret"
        };

        boolean isValid = true;
        for (String requiredProperty : requiredProperties) {
            if (getProperty(requiredProperty) == null) {
                validationErrors.add("Required property missing: " + requiredProperty);
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Validates property formats and types.
     * 
     * @param validationErrors List to collect validation errors
     * @return true if validation passes
     */
    private boolean validatePropertyFormats(List<String> validationErrors) {
        boolean isValid = true;

        // Validate database URL format
        Object dbUrl = getProperty("spring.datasource.url");
        if (dbUrl != null && !dbUrl.toString().startsWith("jdbc:postgresql://")) {
            validationErrors.add("Invalid database URL format");
            isValid = false;
        }

        // Validate numeric properties
        Object cacheTimeout = getProperty("carddemo.config.cache-ttl-minutes");
        if (cacheTimeout != null) {
            try {
                int timeout = Integer.parseInt(cacheTimeout.toString());
                if (timeout < 1 || timeout > 1440) {
                    validationErrors.add("Cache timeout must be between 1 and 1440 minutes");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                validationErrors.add("Cache timeout must be a valid integer");
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Validates security requirements.
     * 
     * @param validationErrors List to collect validation errors
     * @return true if validation passes
     */
    private boolean validateSecurityRequirements(List<String> validationErrors) {
        boolean isValid = true;

        // Validate JWT secret strength
        Object jwtSecret = getProperty("carddemo.security.jwt.secret");
        if (jwtSecret != null && jwtSecret.toString().length() < 32) {
            validationErrors.add("JWT secret must be at least 32 characters long");
            isValid = false;
        }

        // Validate encryption is enabled for sensitive properties
        for (String key : configurationCache.keySet()) {
            if (isSensitiveProperty(key)) {
                Object value = configurationCache.get(key);
                if (value instanceof String && !encryptionService.isEncrypted((String) value)) {
                    logger.warn("Sensitive property not encrypted: {}", key);
                    // Don't fail validation, just warn
                }
            }
        }

        return isValid;
    }

    /**
     * Validates business rules and constraints.
     * 
     * @param validationErrors List to collect validation errors
     * @return true if validation passes
     */
    private boolean validateBusinessRules(List<String> validationErrors) {
        boolean isValid = true;

        // Validate credit limits are reasonable
        Object maxCreditLimit = getProperty("carddemo.business.max-credit-limit");
        if (maxCreditLimit != null) {
            try {
                double limit = Double.parseDouble(maxCreditLimit.toString());
                if (limit <= 0 || limit > 1000000) {
                    validationErrors.add("Maximum credit limit must be between 0 and 1,000,000");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                validationErrors.add("Maximum credit limit must be a valid number");
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Checks if a property key represents sensitive data.
     * 
     * @param propertyKey The property key to check
     * @return true if the property is sensitive
     */
    private boolean isSensitiveProperty(String propertyKey) {
        return SENSITIVE_PROPERTY_PATTERN.matcher(propertyKey).matches();
    }

    /**
     * Checks if cache entry has expired.
     * 
     * @param propertyKey The property key to check
     * @return true if cache entry has expired
     */
    private boolean isCacheExpired(String propertyKey) {
        LocalDateTime timestamp = configurationTimestamps.get(propertyKey);
        if (timestamp == null) {
            return true;
        }
        return timestamp.plusMinutes(cacheTtlMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * Decrypts value if it's encrypted.
     * 
     * @param value The value to potentially decrypt
     * @return The decrypted value or original value
     */
    private Object decryptIfNeeded(Object value) {
        if (value instanceof String) {
            String stringValue = (String) value;
            if (encryptionService.isEncrypted(stringValue)) {
                return decryptProperty(stringValue);
            }
        }
        return value;
    }

    /**
     * Validates a property value against rules.
     * 
     * @param propertyKey The property key
     * @param propertyValue The property value
     * @return true if validation passes
     */
    private boolean validatePropertyValue(String propertyKey, Object propertyValue) {
        if (propertyValue == null) {
            return false;
        }

        // Validate string length limits
        if (propertyValue instanceof String) {
            String stringValue = (String) propertyValue;
            if (stringValue.length() > 4000) { // Database column limit
                logger.warn("Property value too long: {}", propertyKey);
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a new configuration entity.
     * 
     * @param propertyKey The property key
     * @param propertyValue The property value
     * @return The new configuration entity
     */
    private Configuration createNewConfigurationEntity(String propertyKey, Object propertyValue) {
        Configuration config = new Configuration();
        config.setEnvironment(currentProfile);
        config.setName(propertyKey);
        config.setConfigKey(propertyKey);
        config.setValue(propertyValue.toString());
        config.setCreatedDate(LocalDateTime.now());
        config.setLastModified(LocalDateTime.now());
        config.setVersion(1);
        config.setActive(true);
        config.setRequiresValidation(false);
        
        // Determine category based on property key
        if (propertyKey.startsWith("spring.datasource")) {
            config.setCategory("DATABASE");
        } else if (propertyKey.startsWith("carddemo.security")) {
            config.setCategory("SECURITY");
        } else if (propertyKey.startsWith("carddemo.business")) {
            config.setCategory("BUSINESS");
        } else {
            config.setCategory("APPLICATION");
        }
        
        return config;
    }

    /**
     * Converts value to target type.
     * 
     * @param value The value to convert
     * @param targetType The target type
     * @param <T> The generic type parameter
     * @return The converted value
     */
    @SuppressWarnings("unchecked")
    private <T> T convertToType(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        // Handle common type conversions
        if (targetType == String.class) {
            return (T) value.toString();
        } else if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(value.toString());
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) Boolean.valueOf(value.toString());
        } else if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(value.toString());
        } else if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(value.toString());
        }

        // Default to returning the value as-is
        return (T) value;
    }

    /**
     * Masks sensitive data in a Map.
     * 
     * @param dataMap The map to mask
     * @return The masked map
     */
    private Map<String, Object> maskSensitiveMap(Map<?, ?> dataMap) {
        Map<String, Object> maskedMap = new HashMap<>();
        
        for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            
            if (isSensitiveProperty(key)) {
                maskedMap.put(key, encryptionService.maskSensitiveProperty(value.toString()));
            } else {
                maskedMap.put(key, value);
            }
        }
        
        return maskedMap;
    }

    /**
     * Masks sensitive data in a Collection.
     * 
     * @param dataCollection The collection to mask
     * @return The masked collection
     */
    private Collection<Object> maskSensitiveCollection(Collection<?> dataCollection) {
        List<Object> maskedList = new ArrayList<>();
        
        for (Object item : dataCollection) {
            if (item instanceof String) {
                maskedList.add(encryptionService.maskSensitiveProperty((String) item));
            } else {
                maskedList.add(item);
            }
        }
        
        return maskedList;
    }

    /**
     * Audits configuration operations.
     * 
     * @param operation The operation type
     * @param target The target of the operation
     * @param description The operation description
     * @param success Whether the operation was successful
     */
    private void auditConfigurationOperation(String operation, String target, String description, boolean success) {
        if (!auditEnabled) {
            return;
        }

        try {
            logger.info("AUDIT - Operation: {}, Target: {}, Description: {}, Success: {}", 
                       operation, target, description, success);
            
            // In a real implementation, this would write to an audit table or external audit system
            // For now, we just log the audit information
            
        } catch (Exception e) {
            logger.error("Failed to audit configuration operation", e);
        }
    }

    /**
     * Cleanup method called during application shutdown.
     */
    @PreDestroy
    public void destroy() {
        logger.info("Shutting down ConfigurationService");
        
        // Clear caches
        configurationCache.clear();
        configurationTimestamps.clear();
        rollbackSnapshots.clear();
        
        initialized = false;
    }

    // ===================== CONFIGURATION CHANGE EVENT =====================

    /**
     * Event class for configuration change notifications.
     */
    public static class ConfigurationChangeEvent {
        private final Object source;
        private final String changeType;
        private final Set<String> changedProperties;
        private final String environment;
        private final LocalDateTime timestamp;

        public ConfigurationChangeEvent(Object source, String changeType, Set<String> changedProperties, 
                                      String environment, LocalDateTime timestamp) {
            this.source = source;
            this.changeType = changeType;
            this.changedProperties = changedProperties;
            this.environment = environment;
            this.timestamp = timestamp;
        }

        // Getters
        public Object getSource() { return source; }
        public String getChangeType() { return changeType; }
        public Set<String> getChangedProperties() { return changedProperties; }
        public String getEnvironment() { return environment; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}