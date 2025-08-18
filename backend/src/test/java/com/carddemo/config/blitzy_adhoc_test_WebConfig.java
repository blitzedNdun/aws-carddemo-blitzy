package com.carddemo.config;

import com.carddemo.converter.BmsMessageConverter;
import com.carddemo.interceptor.CicsTransactionInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive ad-hoc unit tests for WebConfig.java validation.
 * 
 * These tests verify that the Spring Web MVC configuration properly sets up:
 * - CORS configuration for React frontend communication
 * - BmsMessageConverter registration for BMS-to-JSON transformations
 * - TransactionInterceptor registration for CICS transaction code routing
 * - ObjectMapper configuration with COBOL data type precision preservation
 * - Bean definitions for core web infrastructure components
 * 
 * All tests validate exact functional parity with COBOL business logic
 * while ensuring modern web standards compliance.
 */
@SpringBootTest(classes = WebConfigTestConfiguration.class)
@ActiveProfiles("test")
public class blitzy_adhoc_test_WebConfig {

    @Autowired
    private WebConfig webConfig;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private CicsTransactionInterceptor cicsTransactionInterceptor;

    @Autowired(required = false) 
    private BmsMessageConverter bmsMessageConverter;

    private TestCorsRegistry testCorsRegistry;
    private TestInterceptorRegistry testInterceptorRegistry;

    @BeforeEach
    void setUp() {
        testCorsRegistry = new TestCorsRegistry();
        testInterceptorRegistry = new TestInterceptorRegistry();
    }

    @Test
    @DisplayName("Test 1: WebConfig instance creation and autowiring")
    void testWebConfigInstanceCreation() {
        assertNotNull(webConfig, "WebConfig should be properly instantiated and autowired");
        assertTrue(webConfig instanceof org.springframework.web.servlet.config.annotation.WebMvcConfigurer,
                "WebConfig should implement WebMvcConfigurer interface");
    }

    @Test
    @DisplayName("Test 2: CORS configuration for React frontend communication")
    void testCorsConfiguration() {
        // Execute CORS configuration
        webConfig.addCorsMappings(testCorsRegistry);
        
        // Verify CORS mapping was added for API endpoints
        assertFalse(testCorsRegistry.registrations.isEmpty(), 
                "CORS mappings should be configured for API endpoints");
        
        // Verify API pattern mapping
        assertTrue(testCorsRegistry.registrations.stream()
                .anyMatch(reg -> reg.pathPattern.equals("/api/**")),
                "CORS should be configured for /api/** pattern");
    }

    @Test
    @DisplayName("Test 3: CORS allowed origins for React development server")
    void testCorsAllowedOrigins() {
        webConfig.addCorsMappings(testCorsRegistry);
        
        TestCorsRegistration apiRegistration = testCorsRegistry.registrations.stream()
                .filter(reg -> reg.pathPattern.equals("/api/**"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(apiRegistration, "API CORS registration should exist");
        
        // Verify localhost origins for React development
        assertTrue(apiRegistration.allowedOrigins.contains("http://localhost:3000"),
                "Should allow React development server on http://localhost:3000");
        assertTrue(apiRegistration.allowedOrigins.contains("https://localhost:3000"),
                "Should allow HTTPS React development server on https://localhost:3000");
        assertTrue(apiRegistration.allowedOrigins.contains("http://127.0.0.1:3000"),
                "Should allow alternate localhost format http://127.0.0.1:3000");
        assertTrue(apiRegistration.allowedOrigins.contains("https://127.0.0.1:3000"),
                "Should allow HTTPS alternate localhost format https://127.0.0.1:3000");
    }

    @Test
    @DisplayName("Test 4: CORS allowed methods for REST API operations")
    void testCorsAllowedMethods() {
        webConfig.addCorsMappings(testCorsRegistry);
        
        TestCorsRegistration apiRegistration = testCorsRegistry.registrations.stream()
                .filter(reg -> reg.pathPattern.equals("/api/**"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(apiRegistration, "API CORS registration should exist");
        
        // Verify standard REST methods
        assertTrue(apiRegistration.allowedMethods.contains("GET"),
                "Should allow GET for read operations");
        assertTrue(apiRegistration.allowedMethods.contains("POST"),
                "Should allow POST for create operations");
        assertTrue(apiRegistration.allowedMethods.contains("PUT"),
                "Should allow PUT for update operations");
        assertTrue(apiRegistration.allowedMethods.contains("DELETE"),
                "Should allow DELETE for delete operations");
        assertTrue(apiRegistration.allowedMethods.contains("OPTIONS"),
                "Should allow OPTIONS for preflight requests");
    }

    @Test
    @DisplayName("Test 5: CORS allowed headers for frontend communication")
    void testCorsAllowedHeaders() {
        webConfig.addCorsMappings(testCorsRegistry);
        
        TestCorsRegistration apiRegistration = testCorsRegistry.registrations.stream()
                .filter(reg -> reg.pathPattern.equals("/api/**"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(apiRegistration, "API CORS registration should exist");
        
        // Verify essential headers for web applications
        assertTrue(apiRegistration.allowedHeaders.contains("Content-Type"),
                "Should allow Content-Type header for JSON communication");
        assertTrue(apiRegistration.allowedHeaders.contains("Authorization"),
                "Should allow Authorization header for authentication");
        assertTrue(apiRegistration.allowedHeaders.contains("X-XSRF-TOKEN"),
                "Should allow X-XSRF-TOKEN for CSRF protection");
        assertTrue(apiRegistration.allowedHeaders.contains("Accept"),
                "Should allow Accept header for content negotiation");
    }

    @Test
    @DisplayName("Test 6: CORS credentials support for session management")
    void testCorsCredentialsSupport() {
        webConfig.addCorsMappings(testCorsRegistry);
        
        TestCorsRegistration apiRegistration = testCorsRegistry.registrations.stream()
                .filter(reg -> reg.pathPattern.equals("/api/**"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(apiRegistration, "API CORS registration should exist");
        assertTrue(apiRegistration.allowCredentials,
                "CORS should allow credentials for session cookie transmission");
        assertEquals(3600, apiRegistration.maxAge,
                "CORS max age should be 3600 seconds for preflight caching");
    }

    @Test
    @DisplayName("Test 7: Message converters configuration for BMS transformation")
    void testMessageConvertersConfiguration() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        
        // Execute message converter configuration
        webConfig.configureMessageConverters(converters);
        
        // Verify BmsMessageConverter was added
        assertFalse(converters.isEmpty(), "Message converters should be configured");
        assertTrue(converters.stream()
                .anyMatch(converter -> converter instanceof BmsMessageConverter),
                "BmsMessageConverter should be registered in message converters");
    }

    @Test
    @DisplayName("Test 8: Transaction interceptor registration and path patterns")
    void testTransactionInterceptorRegistration() {
        // Execute interceptor configuration
        webConfig.addInterceptors(testInterceptorRegistry);
        
        // Verify interceptor was registered
        assertFalse(testInterceptorRegistry.registrations.isEmpty(),
                "Transaction interceptor should be registered");
        
        TestInterceptorRegistration registration = testInterceptorRegistry.registrations.get(0);
        assertTrue(registration.interceptor instanceof CicsTransactionInterceptor,
                "Registered interceptor should be CicsTransactionInterceptor");
    }

    @Test
    @DisplayName("Test 9: Transaction interceptor path inclusion and exclusion")
    void testTransactionInterceptorPaths() {
        webConfig.addInterceptors(testInterceptorRegistry);
        
        TestInterceptorRegistration registration = testInterceptorRegistry.registrations.get(0);
        
        // Verify included paths
        assertTrue(registration.pathPatterns.contains("/api/**"),
                "Interceptor should apply to all API endpoints");
        
        // Verify excluded paths
        assertTrue(registration.excludePatterns.contains("/api/health"),
                "Interceptor should exclude health check endpoints");
        assertTrue(registration.excludePatterns.contains("/api/actuator/**"),
                "Interceptor should exclude Spring Boot Actuator endpoints");
        assertTrue(registration.excludePatterns.contains("/api/static/**"),
                "Interceptor should exclude static resource endpoints");
    }

    @Test
    @DisplayName("Test 10: ObjectMapper bean configuration with COBOL data support")
    void testObjectMapperBeanConfiguration() {
        // Get ObjectMapper bean from WebConfig
        ObjectMapper mapper = webConfig.objectMapper();
        
        assertNotNull(mapper, "ObjectMapper bean should be created");
        
        // Verify BigDecimal configuration for COBOL precision
        assertTrue(mapper.isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS),
                "ObjectMapper should be configured to use BigDecimal for floats");
        assertTrue(mapper.isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS),
                "ObjectMapper should be configured to use BigInteger for large integers");
        assertTrue(mapper.isEnabled(com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN),
                "ObjectMapper should write BigDecimal as plain string for precision preservation");
    }

    @Test
    @DisplayName("Test 11: CicsTransactionInterceptor bean creation")
    void testCicsTransactionInterceptorBean() {
        CicsTransactionInterceptor interceptor = webConfig.cicsTransactionInterceptor();
        
        assertNotNull(interceptor, "CicsTransactionInterceptor bean should be created");
        assertTrue(interceptor instanceof CicsTransactionInterceptor,
                "Bean should be instance of CicsTransactionInterceptor");
        assertTrue(interceptor instanceof org.springframework.web.servlet.HandlerInterceptor,
                "CicsTransactionInterceptor should implement HandlerInterceptor");
    }

    @Test
    @DisplayName("Test 12: BmsMessageConverter bean creation")
    void testBmsMessageConverterBean() {
        BmsMessageConverter converter = webConfig.bmsMessageConverter();
        
        assertNotNull(converter, "BmsMessageConverter bean should be created");
        assertTrue(converter instanceof BmsMessageConverter,
                "Bean should be instance of BmsMessageConverter");
        assertTrue(converter instanceof org.springframework.http.converter.HttpMessageConverter,
                "BmsMessageConverter should implement HttpMessageConverter");
    }

    @Test
    @DisplayName("Test 13: ObjectMapper COBOL precision with BigDecimal rounding")
    void testObjectMapperCobolPrecision() throws Exception {
        ObjectMapper mapper = webConfig.objectMapper();
        
        // Test BigDecimal serialization/deserialization with COBOL precision
        BigDecimal testAmount = new BigDecimal("123.456").setScale(2, RoundingMode.HALF_UP);
        String json = mapper.writeValueAsString(testAmount);
        BigDecimal deserialized = mapper.readValue(json, BigDecimal.class);
        
        assertEquals(testAmount, deserialized,
                "BigDecimal should maintain COBOL precision during JSON serialization/deserialization");
        assertEquals(2, deserialized.scale(),
                "BigDecimal should maintain scale of 2 for monetary amounts");
    }

    @Test
    @DisplayName("Test 14: Integration test - Complete WebConfig functionality")
    void testCompleteWebConfigIntegration() {
        // Verify all major configuration components work together
        
        // 1. CORS configuration
        webConfig.addCorsMappings(testCorsRegistry);
        assertFalse(testCorsRegistry.registrations.isEmpty(), "CORS should be configured");
        
        // 2. Message converter configuration
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        webConfig.configureMessageConverters(converters);
        assertFalse(converters.isEmpty(), "Message converters should be configured");
        
        // 3. Interceptor configuration
        webConfig.addInterceptors(testInterceptorRegistry);
        assertFalse(testInterceptorRegistry.registrations.isEmpty(), "Interceptors should be configured");
        
        // 4. Bean creation
        assertNotNull(webConfig.objectMapper(), "ObjectMapper bean should be created");
        assertNotNull(webConfig.cicsTransactionInterceptor(), "CicsTransactionInterceptor bean should be created");
        assertNotNull(webConfig.bmsMessageConverter(), "BmsMessageConverter bean should be created");
        
        // Verify WebConfig as a complete configuration unit
        assertTrue(org.springframework.web.servlet.config.annotation.WebMvcConfigurer.class
                .isAssignableFrom(webConfig.getClass()),
                "WebConfig should be a complete WebMvcConfigurer implementation");
    }

    // Test helper classes for CORS and Interceptor configuration testing

    private static class TestCorsRegistry extends CorsRegistry {
        List<TestCorsRegistration> registrations = new ArrayList<>();

        @Override
        public org.springframework.web.servlet.config.annotation.CorsRegistration addMapping(String pathPattern) {
            TestCorsRegistration registration = new TestCorsRegistration(pathPattern);
            registrations.add(registration);
            return registration;
        }
    }

    private static class TestCorsRegistration extends org.springframework.web.servlet.config.annotation.CorsRegistration {
        String pathPattern;
        List<String> allowedOrigins = new ArrayList<>();
        List<String> allowedMethods = new ArrayList<>();
        List<String> allowedHeaders = new ArrayList<>();
        boolean allowCredentials = false;
        long maxAge = -1;

        public TestCorsRegistration(String pathPattern) {
            super(pathPattern);
            this.pathPattern = pathPattern;
        }

        @Override
        public org.springframework.web.servlet.config.annotation.CorsRegistration allowedOrigins(String... origins) {
            this.allowedOrigins.addAll(java.util.Arrays.asList(origins));
            return this;
        }

        @Override
        public org.springframework.web.servlet.config.annotation.CorsRegistration allowedMethods(String... methods) {
            this.allowedMethods.addAll(java.util.Arrays.asList(methods));
            return this;
        }

        @Override
        public org.springframework.web.servlet.config.annotation.CorsRegistration allowedHeaders(String... headers) {
            this.allowedHeaders.addAll(java.util.Arrays.asList(headers));
            return this;
        }

        @Override
        public org.springframework.web.servlet.config.annotation.CorsRegistration allowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
            return this;
        }

        @Override
        public org.springframework.web.servlet.config.annotation.CorsRegistration maxAge(long maxAge) {
            this.maxAge = maxAge;
            return this;
        }
    }

    private static class TestInterceptorRegistry extends InterceptorRegistry {
        List<TestInterceptorRegistration> registrations = new ArrayList<>();

        @Override
        public org.springframework.web.servlet.config.annotation.InterceptorRegistration addInterceptor(
                org.springframework.web.servlet.HandlerInterceptor interceptor) {
            TestInterceptorRegistration registration = new TestInterceptorRegistration(interceptor);
            registrations.add(registration);
            return registration;
        }
    }

    private static class TestInterceptorRegistration extends org.springframework.web.servlet.config.annotation.InterceptorRegistration {
        org.springframework.web.servlet.HandlerInterceptor interceptor;
        List<String> pathPatterns = new ArrayList<>();
        List<String> excludePatterns = new ArrayList<>();

        public TestInterceptorRegistration(org.springframework.web.servlet.HandlerInterceptor interceptor) {
            super(interceptor);
            this.interceptor = interceptor;
        }

        @Override
        public org.springframework.web.servlet.config.annotation.InterceptorRegistration addPathPatterns(String... patterns) {
            this.pathPatterns.addAll(java.util.Arrays.asList(patterns));
            return this;
        }

        @Override
        public org.springframework.web.servlet.config.annotation.InterceptorRegistration excludePathPatterns(String... patterns) {
            this.excludePatterns.addAll(java.util.Arrays.asList(patterns));
            return this;
        }
    }
}