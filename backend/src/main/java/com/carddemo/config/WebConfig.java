/*
 * WebConfig - Spring Web MVC Configuration for CardDemo Application
 * 
 * This configuration class establishes the Spring Web MVC infrastructure
 * required for the COBOL-to-Java migration, providing comprehensive REST API
 * support that maintains functional parity with the original CICS transaction
 * processing system.
 * 
 * Key Responsibilities:
 * - CORS configuration for React frontend communication (localhost:3000)
 * - Custom message converter registration for BMS-to-JSON transformations
 * - Transaction interceptor registration for CICS transaction code routing
 * - ObjectMapper configuration with COBOL data type precision preservation
 * - Bean definitions for core web infrastructure components
 * 
 * This implementation directly supports the requirements specified in Section 0
 * of the technical specification for maintaining exact COBOL business logic
 * while modernizing the technology stack to Spring Boot with REST APIs.
 * 
 * Copyright (C) 2024 CardDemo Application
 */

package com.carddemo.config;

import com.carddemo.converter.BmsMessageConverter;
import com.carddemo.interceptor.CicsTransactionInterceptor;
import com.carddemo.util.CobolDataConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Spring Web MVC configuration class providing comprehensive REST API setup
 * for the CardDemo COBOL-to-Java migration. This configuration ensures seamless
 * integration between the React frontend and Spring Boot backend while maintaining
 * exact functional parity with the original CICS transaction processing system.
 * 
 * The configuration implements modern web standards (CORS, JSON serialization,
 * request interception) while preserving COBOL business logic patterns and
 * data type precision requirements through specialized converters and interceptors.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures Cross-Origin Resource Sharing (CORS) policies for React frontend communication.
     * 
     * This method establishes the CORS configuration required for the React Single Page
     * Application to communicate with the Spring Boot REST API endpoints. The configuration
     * allows requests from the React development server and production deployments while
     * maintaining security through controlled origin, method, and header allowances.
     * 
     * CORS Configuration Details:
     * - Allowed Origins: React development server (localhost:3000) and production origins
     * - Allowed Methods: Standard REST API methods (GET, POST, PUT, DELETE, OPTIONS)
     * - Allowed Headers: Content-Type, Authorization, and XSRF protection headers
     * - Credentials Support: Enabled for session cookie transmission (JSESSIONID, XSRF-TOKEN)
     * - Max Age: 3600 seconds to optimize preflight request caching
     * 
     * @param registry Spring CORS registry for configuring cross-origin policies
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:3000",      // React development server
                    "https://localhost:3000",     // HTTPS development server
                    "http://127.0.0.1:3000",      // Alternate localhost format
                    "https://127.0.0.1:3000"      // HTTPS alternate localhost
                )
                .allowedMethods(
                    "GET",      // Read operations (transaction lists, account views)
                    "POST",     // Create operations (new accounts, sign-on requests)
                    "PUT",      // Update operations (account updates, user profile changes)
                    "DELETE",   // Delete operations (account closure, transaction reversal)
                    "OPTIONS"   // Preflight requests for CORS compliance
                )
                .allowedHeaders(
                    "Content-Type",              // JSON content type specification
                    "Authorization",             // Bearer token or basic auth headers
                    "X-Requested-With",          // AJAX request identification
                    "X-XSRF-TOKEN",             // CSRF protection token
                    "Accept",                    // Content negotiation header
                    "Origin",                    // Origin header for CORS validation
                    "Access-Control-Request-Method",    // Preflight method specification
                    "Access-Control-Request-Headers"    // Preflight header specification
                )
                .allowCredentials(true)          // Enable session cookie transmission
                .maxAge(3600);                   // Cache preflight responses for 1 hour
    }

    /**
     * Configures custom HTTP message converters for BMS-to-JSON transformation.
     * 
     * This method registers the BmsMessageConverter to handle the specialized conversion
     * between COBOL BMS (Basic Mapping Support) screen structures and JSON format
     * required for React frontend consumption. The converter maintains exact field
     * lengths, data types, and formatting rules from the original COBOL implementation.
     * 
     * Converter Registration Strategy:
     * - Priority Position: Added early in converter list for BMS structure detection
     * - Content-Type Support: Handles application/json with BMS field metadata
     * - Precision Preservation: Maintains COBOL COMP-3 decimal precision
     * - Field Mapping: Preserves BMS field names and attributes for frontend compatibility
     * 
     * @param converters List of HTTP message converters to configure
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Add BmsMessageConverter early in the converter chain for proper priority
        converters.add(bmsMessageConverter());
    }

    /**
     * Registers request interceptors for transaction code routing and processing.
     * 
     * This method configures the TransactionInterceptor to handle CICS transaction
     * code-based routing, replicating the original mainframe transaction processing
     * patterns within the Spring Boot REST API framework. The interceptor processes
     * transaction codes and maintains compatibility with legacy CICS behavior.
     * 
     * Interceptor Configuration:
     * - Path Patterns: Applied to all API endpoints (/api/**)
     * - Transaction Routing: Maps CICS transaction codes to appropriate controllers
     * - Session Management: Integrates with Spring Session for COMMAREA equivalent state
     * - Error Handling: Provides COBOL ABEND-style error processing
     * 
     * @param registry Spring interceptor registry for request processing configuration
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cicsTransactionInterceptor())
                .addPathPatterns("/api/**")         // Apply to all API endpoints
                .excludePathPatterns(
                    "/api/health",                  // Exclude health check endpoints
                    "/api/actuator/**",             // Exclude Spring Boot Actuator endpoints
                    "/api/static/**"                // Exclude static resource endpoints
                );
    }

    /**
     * Creates and configures a Jackson ObjectMapper bean with COBOL data type support.
     * 
     * This method produces an ObjectMapper instance configured specifically for handling
     * COBOL data type conversions and maintaining precise decimal calculations that
     * match the original mainframe implementation. The configuration uses the
     * CobolDataConverter utility to ensure exact BigDecimal precision and scale.
     * 
     * ObjectMapper Configuration Features:
     * - BigDecimal Precision: Configured for COBOL COMP-3 packed decimal behavior
     * - Scale Management: Maintains 2-decimal precision for monetary amounts
     * - Rounding Mode: Uses HALF_UP rounding to match COBOL ROUNDED clause
     * - Custom Serializers: Includes specialized serializers for COBOL data types
     * - JSON Generation: Configured for plain BigDecimal output without scientific notation
     * 
     * @return Configured ObjectMapper with COBOL-compatible data type handling
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure ObjectMapper with COBOL data converter settings
        CobolDataConverter.configureObjectMapper(mapper);
        
        // Register BigDecimal module for precise financial calculations
        mapper.registerModule(CobolDataConverter.createBigDecimalModule());
        
        return mapper;
    }

    /**
     * Creates and configures a CicsTransactionInterceptor bean for request processing.
     * 
     * This method produces a CicsTransactionInterceptor instance that handles the routing
     * and processing of REST API requests based on CICS transaction codes. The
     * interceptor maintains compatibility with the original mainframe transaction
     * processing patterns while operating within the Spring Boot framework.
     * 
     * CicsTransactionInterceptor Features:
     * - Transaction Code Mapping: Routes requests based on legacy CICS transaction codes
     * - Session State Management: Integrates with Spring Session for user context
     * - Error Handling: Provides COBOL ABEND-equivalent error processing
     * - Performance Monitoring: Includes request timing and throughput metrics
     * - Security Integration: Works with Spring Security for authentication/authorization
     * 
     * @return Configured CicsTransactionInterceptor for CICS-compatible request processing
     */
    @Bean("cicsTransactionInterceptor")
    public CicsTransactionInterceptor cicsTransactionInterceptor() {
        return new CicsTransactionInterceptor();
    }

    /**
     * Creates and configures a BmsMessageConverter bean for JSON transformation.
     * 
     * This method produces a BmsMessageConverter instance that handles the specialized
     * conversion between COBOL BMS screen structures and JSON format. The converter
     * maintains exact field definitions, validation rules, and formatting requirements
     * from the original BMS mapsets while providing JSON serialization for modern
     * web browser consumption.
     * 
     * BmsMessageConverter Features:
     * - Field Structure Preservation: Maintains BMS field names and attributes
     * - Data Type Conversion: Handles COBOL PIC clauses to JSON type mapping  
     * - Validation Rule Translation: Converts BMS edit rules to JSON schema validation
     * - Precision Maintenance: Preserves numeric precision for financial calculations
     * - Color/Attribute Mapping: Translates BMS display attributes to CSS-friendly formats
     * 
     * @return Configured BmsMessageConverter for BMS-to-JSON bidirectional transformation
     */
    @Bean
    public BmsMessageConverter bmsMessageConverter() {
        return new BmsMessageConverter();
    }
}