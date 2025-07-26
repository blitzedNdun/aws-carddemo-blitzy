package com.carddemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Test Configuration for CardDemo Application
 * 
 * Provides test-specific bean definitions and configuration overrides
 * for comprehensive unit and integration testing of CardDemo components.
 * 
 * This configuration class ensures that tests run with appropriate
 * database settings, transaction management, and component scanning
 * while excluding production-specific configurations that would
 * interfere with test execution.
 * 
 * @author Blitzy Development Team
 * @version 1.0
 */
@TestConfiguration
@EnableJpaRepositories(basePackages = {
    "com.carddemo.account.repository",
    "com.carddemo.card.repository", 
    "com.carddemo.transaction.repository",
    "com.carddemo.common.repository"
})
@EntityScan(basePackages = {
    "com.carddemo.account.entity",
    "com.carddemo.card.entity",
    "com.carddemo.transaction.entity", 
    "com.carddemo.common.entity"
})
@EnableTransactionManagement
@EnableAutoConfiguration(exclude = {
    BatchAutoConfiguration.class
})
public class CardDemoTestConfiguration {

    /**
     * Test-specific ObjectMapper bean with COBOL-compatible precision settings
     * 
     * Configures Jackson ObjectMapper for test scenarios with proper handling
     * of BigDecimal serialization to maintain COBOL COMP-3 decimal precision
     * equivalency during JSON processing in test cases.
     * 
     * @return ObjectMapper configured for test scenarios
     */
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure BigDecimal handling for COBOL COMP-3 equivalency
        mapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        
        // Add Java 8 time support for LocalDate/LocalDateTime handling
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }

    /**
     * Test-specific MathContext bean for COBOL-equivalent decimal operations
     * 
     * Provides MathContext with DECIMAL128 precision and HALF_UP rounding
     * to ensure that all BigDecimal operations in tests maintain exact
     * COBOL COMP-3 arithmetic behavior and precision.
     * 
     * @return MathContext for COBOL-equivalent operations
     */
    @Bean("cobolMathContext")
    public MathContext cobolMathContext() {
        // DECIMAL128 precision with HALF_UP rounding to match COBOL arithmetic
        return new MathContext(34, RoundingMode.HALF_UP);
    }

    /**
     * Test utility method to create test database URL
     * 
     * Provides consistent H2 in-memory database configuration for test scenarios
     * with PostgreSQL compatibility mode to ensure that SQL queries developed
     * for production PostgreSQL database work correctly in test environment.
     * 
     * @return H2 database URL with PostgreSQL compatibility
     */
    public static String getTestDatabaseUrl() {
        return "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
    }

    /**
     * Test utility method to verify test environment setup
     * 
     * Validates that all required test infrastructure components are properly
     * configured and accessible for comprehensive CardDemo testing scenarios.
     * 
     * @return true if test environment is properly configured
     */
    public static boolean isTestEnvironmentConfigured() {
        try {
            // Verify H2 database driver is available
            Class.forName("org.h2.Driver");
            
            // Verify JPA annotations are available
            Class.forName("jakarta.persistence.Entity");
            
            // Verify Spring Data JPA is available
            Class.forName("org.springframework.data.jpa.repository.JpaRepository");
            
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}